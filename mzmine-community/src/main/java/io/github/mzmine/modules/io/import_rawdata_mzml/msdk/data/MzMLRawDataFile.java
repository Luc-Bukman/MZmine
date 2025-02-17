/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import com.google.common.collect.ImmutableList;
import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.FileType;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Only used during import as temporary object
 */
public class MzMLRawDataFile {

  private static final @NotNull FileType fileType = FileType.MZML;

  private final File sourceFile;

  private final @NotNull List<String> msFunctions;
  private final @NotNull List<BuildingMzMLMsScan> msScans;
  private final @NotNull List<Chromatogram> chromatograms;

  private @NotNull String defaultInstrumentConfiguration;
  private @NotNull String defaultDataProcessingScan;
  private @NotNull String defaultDataProcessingChromatogram;
  private @NotNull String startTimeStamp;

  private @NotNull String name;

  /**
   *
   * @param sourceFile a {@link File} object.
   * @param msFunctions a {@link List} object.
   * @param msScans a {@link List} object.
   * @param chromatograms a {@link List} object.
   */
  @SuppressWarnings("null")
  public MzMLRawDataFile(File sourceFile, List<String> msFunctions, List<BuildingMzMLMsScan> msScans,
      List<Chromatogram> chromatograms) {
    this.sourceFile = sourceFile;
    this.startTimeStamp = "";
    this.name = sourceFile != null ? sourceFile.getName() : null;
    this.msFunctions = msFunctions;
    this.msScans = msScans;
    this.chromatograms = chromatograms;
    this.defaultInstrumentConfiguration = "unknown";
    this.defaultDataProcessingScan = "unknown";
    this.defaultDataProcessingChromatogram = "unknown";
  }

  @NotNull
  public String getName() {
    return name;
  }

  public Optional<File> getOriginalFile() {
    return Optional.ofNullable(sourceFile);
  }

  @NotNull
  public FileType getRawDataFileType() {
    return fileType;
  }

  @NotNull
  public List<String> getMsFunctions() {
    return ImmutableList.copyOf(msFunctions);
  }

  @NotNull
  public List<BuildingMzMLMsScan> getScans() {
    return ImmutableList.copyOf(msScans);
  }

  @NotNull
  public List<Chromatogram> getChromatograms() {
    return ImmutableList.copyOf(chromatograms);
  }

  public String getDefaultInstrumentConfiguration() {
    return defaultInstrumentConfiguration;
  }

  public void setDefaultInstrumentConfiguration(String defaultInstrumentConfiguration) {
    this.defaultInstrumentConfiguration = defaultInstrumentConfiguration;
  }

  public @NotNull String getStartTimeStamp() {
    return startTimeStamp;
  }

  public void setStartTimeStamp(String startTimeStamp) {
    this.startTimeStamp = startTimeStamp;
  }

  public String getDefaultDataProcessingScan() {
    return defaultDataProcessingScan;
  }

  public void setDefaultDataProcessingScan(String defaultDataProcessingScan) {
    this.defaultDataProcessingScan = defaultDataProcessingScan;
  }

  public String getDefaultDataProcessingChromatogram() {
    return defaultDataProcessingChromatogram;
  }

  public void setDefaultDataProcessingChromatogram(String defaultDataProcessingChromatogram) {
    this.defaultDataProcessingChromatogram = defaultDataProcessingChromatogram;
  }

}
