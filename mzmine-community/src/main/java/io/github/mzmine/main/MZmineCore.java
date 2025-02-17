/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.main;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.Semver.SemverType;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.HeadLessDesktop;
import io.github.mzmine.gui.MZmineDesktop;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.mainwindow.UsersTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.mzio.events.AuthRequiredEvent;
import io.mzio.events.EventService;
import io.mzio.users.gui.fx.UsersController;
import io.mzio.users.user.CurrentUserService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MZmine main class
 */
public final class MZmineCore {

  private static final Logger logger = Logger.getLogger(MZmineCore.class.getName());

  private static final MZmineCore instance = new MZmineCore();

  // the default headless desktop is returned if no other desktop is set (e.g., during start up)
  // it is also used in headless mode
  private final Map<String, MZmineModule> initializedModules = new HashMap<>();
  private boolean tdfPseudoProfile = false;
  private boolean tsfProfile = false;

  private MZmineCore() {
    init();
  }

  /**
   * Main method
   */
  public static void main(final String[] args) {
    try {
      Semver version = getMZmineVersion();
      logger.info("Starting MZmine " + version);
      /*
       * Dump the MZmine and JVM arguments for debugging purposes
       */
      final String mzmineArgsString = String.join(" ", args);
      final List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
      final String jvmArgsString = String.join(" ", jvmArgs);
      final String classPathString = System.getProperty("java.class.path");
      logger.finest("MZmine arguments: " + mzmineArgsString);
      logger.finest("Java VM arguments: " + jvmArgsString);
      logger.finest("Java class path: " + classPathString);

      /*
       * Report current working and temporary directory
       */
      final String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
      logger.finest("Working directory is " + cwd);
      logger.finest("Default temporary directory is " + System.getProperty("java.io.tmpdir"));

      // Remove old temporary files on a new thread
      Thread cleanupThread = new Thread(new TmpFileCleanup()); // check regular temp dir
      cleanupThread.setPriority(Thread.MIN_PRIORITY);
      cleanupThread.start();

      MZmineArgumentParser argsParser = new MZmineArgumentParser();
      argsParser.parse(args);
      getInstance().tdfPseudoProfile = argsParser.isLoadTdfPseudoProfile();
      getInstance().tsfProfile = argsParser.isLoadTsfProfile();

      // override preferences file by command line argument pref
      final File prefFile = Objects.requireNonNullElse(argsParser.getPreferencesFile(),
          MZmineConfiguration.CONFIG_FILE);

      boolean updateTempDir = false;
      // Load configuration
      if (prefFile.exists() && prefFile.canRead()) {
        try {
          ConfigService.getConfiguration().loadConfiguration(prefFile, true);
          updateTempDir = true;
        } catch (Exception e) {
          logger.log(Level.WARNING,
              "Error while reading configuration " + prefFile.getAbsolutePath(), e);
        }
      } else {
        logger.log(Level.WARNING, "Cannot read configuration " + prefFile.getAbsolutePath());
      }

      MZminePreferences preferences = ConfigService.getPreferences();
      // override temp directory
      final File tempDirectory = argsParser.getTempDirectory();
      if (tempDirectory != null) {
        // needs to be accessible
        if (FileAndPathUtil.createDirectory(tempDirectory)) {
          preferences.setParameter(MZminePreferences.tempDirectory, tempDirectory);
          updateTempDir = true;
        } else {
          logger.log(Level.WARNING,
              "Cannot create or access temp file directory that was set via program argument: "
              + tempDirectory.getAbsolutePath());
        }
      }

      // listen for user changes so that the latest user is saved
      CurrentUserService.addListener(user -> {
        var nickname = user == null ? null : user.getNickname();
        ConfigService.getPreferences().setParameter(MZminePreferences.username, nickname);
      });
      String username = ConfigService.getPreference(MZminePreferences.username);
      // this will set the current user to CurrentUserService
      // loads all users already logged in from the user folder
      if (StringUtils.hasValue(username)) {
        new UsersController().setCurrentUserByName(username);
      }

      // add event listener
      EventService.subscribe(mzEvent -> {
        if (mzEvent instanceof AuthRequiredEvent) {
          if (DesktopService.isGUI()) {
            getDesktop().addTab(new UsersTab());
          } else {
            getDesktop().displayMessage("Requires user login. Open MZmine and login to a user");
          }
        }
      });

      // set temp directory
      if (updateTempDir) {
        setTempDirToPreference();
      }

      KeepInMemory keepInMemory = argsParser.isKeepInMemory();
      if (keepInMemory != null) {
        // set to preferences
        preferences.setParameter(MZminePreferences.memoryOption, keepInMemory);
      } else {
        keepInMemory = preferences.getParameter(MZminePreferences.memoryOption).getValue();
      }

      String numCores = argsParser.getNumCores();
      setNumThreadsOverride(numCores);

      // after loading the config and numCores
      TaskService.init(ConfigService.getConfiguration().getNumOfThreads());

      // apply memory management option
      keepInMemory.enforceToMemoryMapping();

      // batch mode defined by command line argument
      File batchFile = argsParser.getBatchFile();
      File[] overrideDataFiles = argsParser.getOverrideDataFiles();
      File[] overrideSpectralLibraryFiles = argsParser.getOverrideSpectralLibrariesFiles();
      boolean keepRunningInHeadless = argsParser.isKeepRunningAfterBatch();

      boolean headLessMode = (batchFile != null || keepRunningInHeadless);
      // If we have no arguments, run in GUI mode, otherwise run in batch mode
      if (!headLessMode) {
        try {
          logger.info("Starting MZmine GUI");
          FxThread.setIsFxInitialized(true);
          Application.launch(MZmineGUI.class, args);
        } catch (Throwable e) {
          logger.log(Level.SEVERE, "Could not initialize GUI", e);
          System.exit(1);
        }
      } else {
        // set headless desktop globally
        DesktopService.setDesktop(new HeadLessDesktop());

        Task batchTask = null;
        if (batchFile != null) {
          // load batch
          if ((!batchFile.exists()) || (!batchFile.canRead())) {
            logger.severe("Cannot read batch file " + batchFile);
            System.exit(1);
          }

          // run batch file
          batchTask = BatchModeModule.runBatch(ProjectService.getProject(), batchFile,
              overrideDataFiles, overrideSpectralLibraryFiles, Instant.now());
        }

        // option to keep MZmine running after the batch is finished
        // currently used to test - maybe useful to provide an API to access more data or to run other modules on demand
        if (!keepRunningInHeadless) {
          exit(batchTask);
        }
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Error during MZmine start up", ex);
      exit(null);
    }
  }

  /**
   * Set number of cores to automatic or to fixed number
   *
   * @param numCores "auto" for automatic or integer
   */
  public static void setNumThreadsOverride(@Nullable final String numCores) {
    if (numCores != null) {
      // set to preferences
      var parameter = ConfigService.getPreferences().getParameter(MZminePreferences.numOfThreads);
      if (numCores.equalsIgnoreCase("auto") || numCores.equalsIgnoreCase("automatic")) {
        parameter.setAutomatic(true);
      } else {
        try {
          parameter.setValue(Integer.parseInt(numCores));
        } catch (Exception ex) {
          logger.log(Level.SEVERE,
              "Cannot parse command line argument threads (int) set to " + numCores);
          throw new IllegalArgumentException("numCores was set to " + numCores, ex);
        }
      }
    }
  }

  public static void openTempPreferences() {
    MZminePreferences pref = getConfiguration().getPreferences();
    pref.showSetupDialog(true, "temp");
  }

  public static MZmineCore getInstance() {
    return instance;
  }

  /**
   * Exit MZmine (usually used in headless mode)
   */
  public static void exit(final @Nullable Task batchTask) {
    if (isHeadLessMode() && FxThread.isFxInitialized()) {
      // fx might be initialized for graphics export in headless mode - shut it down
      // in GUI mode it is shut down automatically
      Platform.exit();
    }
    if (batchTask == null || batchTask.isFinished()) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }

  @NotNull
  public static TaskController getTaskController() {
    return TaskService.getController();
  }

  /**
   * The current desktop or a default headless desktop (e.g., during app startup).
   *
   * @return the current desktop or the default headless desktop if still during app startup
   */
  @NotNull
  public static MZmineDesktop getDesktop() {
    if (DesktopService.getDesktop() instanceof MZmineDesktop mZmineDesktop) {
      return mZmineDesktop;
    }
    throw new IllegalStateException("Desktop was not initialized. Requires MZmineDesktop");
  }

  @NotNull
  public static MZmineConfiguration getConfiguration() {
    return ConfigService.getConfiguration();
  }

  /**
   * Returns the instance of a module of given class
   */
  @SuppressWarnings("unchecked")
  public synchronized static <ModuleType extends MZmineModule> ModuleType getModuleInstance(
      Class<ModuleType> moduleClass) {
    if (moduleClass == null) {
      return null;
    }

    ModuleType module = (ModuleType) getInstance().initializedModules.get(moduleClass.getName());

    if (module == null) {

      try {

        logger.finest("Creating an instance of the module " + moduleClass.getName());

        // Create instance and init module
        module = moduleClass.getDeclaredConstructor().newInstance();

        // Add to the module list
        getInstance().initializedModules.put(moduleClass.getName(), module);

      } catch (Throwable e) {
        logger.log(Level.SEVERE, "Could not start module " + moduleClass, e);
        return null;
      }
    }

    return module;
  }

  public static Collection<MZmineModule> getAllModules() {
    return getInstance().initializedModules.values();
  }

  /**
   * Show setup dialog and run module if okay
   *
   * @param moduleClass the module class
   */
  public static ExitCode setupAndRunModule(
      final Class<? extends MZmineRunnableModule> moduleClass) {
    return setupAndRunModule(moduleClass, null, null);
  }

  /**
   * Show setup dialog and run module if okay
   *
   * @param moduleClass the module class
   * @param onFinish    callback for all tasks finished
   * @param onError     callback for error
   */
  public static ExitCode setupAndRunModule(final Class<? extends MZmineRunnableModule> moduleClass,
      @Nullable Runnable onFinish, @Nullable Runnable onError) {
    return setupAndRunModule(moduleClass, onFinish, onError, null);
  }

  /**
   * Show setup dialog and run module if okay
   *
   * @param moduleClass the module class
   * @param onFinish    callback for all tasks finished
   * @param onError     callback for error
   * @param onCancel    callback for cancelled tasks
   */
  public static ExitCode setupAndRunModule(final Class<? extends MZmineRunnableModule> moduleClass,
      @Nullable Runnable onFinish, @Nullable Runnable onError, @Nullable Runnable onCancel) {
    // throw exception on headless mode
    if (isHeadLessMode()) {
      throw new IllegalStateException(
          "Cannot setup parameters in headless mode. This needs the parameter setup dialog");
    }

    MZmineModule module = MZmineCore.getModuleInstance(moduleClass);

    if (module == null) {
      MZmineCore.getDesktop().displayMessage("Cannot find module of class " + moduleClass);
      return ExitCode.ERROR;
    }

    ParameterSet moduleParameters = ConfigService.getConfiguration()
        .getModuleParameters(moduleClass);

    logger.info("Setting parameters for module " + module.getName());
    moduleParameters.setModuleNameAttribute(module.getName());

    try {
      ExitCode exitCode = moduleParameters.showSetupDialog(true);
      if (exitCode != ExitCode.OK) {
        return exitCode;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
    logger.finest("Starting module " + module.getName() + " with parameters " + parametersCopy);
    List<Task> tasks = MZmineCore.runMZmineModule(moduleClass, parametersCopy);

    if (onError != null || onFinish != null || onCancel != null) {
      AllTasksFinishedListener.registerCallbacks(tasks, true, onFinish, onError, onCancel);
    }
    return ExitCode.OK;
  }

  /**
   * Creates a new empty raw data file with the same type and name+suffix like raw
   *
   * @param raw     defines base name and type of raw data file, standard, IMS, or imaging
   * @param suffix  add a suffix to the raw.name
   * @param storage the storage to store data on disk for this file
   * @return new data file of the same type
   * @throws IOException
   */
  public static RawDataFile createNewFile(@NotNull RawDataFile raw, @NotNull final String suffix,
      @Nullable final MemoryMapStorage storage) throws IOException {
    String newName = raw.getName() + " " + suffix;
    String absPath = raw.getAbsolutePath();
    if (raw instanceof IMSRawDataFile) {
      return createNewIMSFile(newName, absPath, storage);
    } else if (raw instanceof ImagingRawDataFileImpl) {
      return createNewImagingFile(newName, absPath, storage);
    } else {
      return new RawDataFileImpl(newName, absPath, storage);
    }
  }

  public static RawDataFile createNewFile(@NotNull final String name,
      @Nullable final String absPath, @Nullable final MemoryMapStorage storage) throws IOException {
    return new RawDataFileImpl(name, absPath, storage);
  }

  public static IMSRawDataFile createNewIMSFile(@NotNull final String name,
      @Nullable final String absPath, @Nullable final MemoryMapStorage storage) throws IOException {
    return new IMSRawDataFileImpl(name, absPath, storage);
  }

  public static ImagingRawDataFile createNewImagingFile(@NotNull final String name,
      @Nullable final String absPath, @Nullable final MemoryMapStorage storage) throws IOException {
    return new ImagingRawDataFileImpl(name, absPath, storage);
  }

  @NotNull
  public static Semver getMZmineVersion() {
    try {
      ClassLoader myClassLoader = MZmineCore.class.getClassLoader();
      InputStream inStream = myClassLoader.getResourceAsStream("mzmineversion.properties");
      if (inStream == null) {
        return new Semver("3-SNAPSHOT", SemverType.LOOSE);
      }
      Properties properties = new Properties();
      properties.load(inStream);
      String versionString = properties.getProperty("version.semver");
      if ((versionString == null) || (versionString.startsWith("$"))) {
        return new Semver("3-SNAPSHOT", SemverType.LOOSE);
      }
      return new Semver(versionString, SemverType.LOOSE);
    } catch (Exception e) {
      return new Semver("3-SNAPSHOT", SemverType.LOOSE);
    }
  }

  /**
   * Standard method to run modules in MZmine
   *
   * @param moduleClass the module class to run
   * @param parameters  the parameter set
   * @return a list of created tasks that were added to the controller
   */
  public static List<Task> runMZmineModule(
      @NotNull Class<? extends MZmineRunnableModule> moduleClass,
      @NotNull ParameterSet parameters) {

    MZmineRunnableModule module = getModuleInstance(moduleClass);

    // Run the module
    final List<Task> newTasks = new ArrayList<>();
    final MZmineProject currentProject = ProjectService.getProject();
    final Instant date = Instant.now();
    logger.finest(() -> "Module " + module.getName() + " called at " + date.toString());
    module.runModule(currentProject, parameters, newTasks, date);
    TaskService.getController().addTasks(newTasks.toArray(new Task[0]));

    return newTasks;
    // Log module run in audit log
    // AuditLogEntry auditLogEntry = new AuditLogEntry(module, parameters,
    // newTasks);
    // currentProject.logProcessingStep(auditLogEntry);
  }

  private static void setTempDirToPreference() {
    final File tempDir = getConfiguration().getPreferences()
        .getParameter(MZminePreferences.tempDirectory).getValue();
    if (tempDir == null) {
      logger.warning(() -> "Invalid temporary directory.");
      return;
    }

    if (!tempDir.exists()) {
      if (!tempDir.mkdirs()) {
        logger.warning(() -> "Could not create temporary directory " + tempDir.getAbsolutePath());
        return;
      }
    }

    if (tempDir.isDirectory()) {
      FileAndPathUtil.setTempDir(tempDir.getAbsoluteFile());
      logger.finest(() -> "Default temporary directory is " + System.getProperty("java.io.tmpdir"));
      logger.finest(
          () -> "Working temporary directory is " + FileAndPathUtil.getTempDir().toString());
      // check the new temp dir for old files.
      Thread cleanupThread2 = new Thread(new TmpFileCleanup());
      cleanupThread2.setPriority(Thread.MIN_PRIORITY);
      cleanupThread2.start();
    }
  }

  /**
   * @return headless mode or JavaFX GUI
   */
  public static boolean isHeadLessMode() {
    return DesktopService.isHeadLess();
  }

  /**
   * @return currently just negates {@link #isHeadLessMode()}
   */
  public static boolean isGUI() {
    return !isHeadLessMode();
  }

  public static @NotNull MetadataTable getProjectMetadata() {
    return ProjectService.getProject().getProjectMetadata();
  }

  private void init() {
    // In the beginning, set the default locale to English, to avoid
    // problems with conversion of numbers etc. (e.g. decimal separator may
    // be . or , depending on the locale)
    Locale.setDefault(new Locale("en", "US"));
    // initialize by default with all in memory
    MemoryMapStorage.setStoreAllInRam(true);

    logger.fine("Initializing core classes..");
  }

  public boolean isTdfPseudoProfile() {
    return tdfPseudoProfile;
  }

  public boolean isTsfProfile() {
    return tsfProfile;
  }
}
