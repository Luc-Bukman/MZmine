/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.io.impl;


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.Task;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.query.Predicate;

/**
 * 
 */
public class ProjectSavingTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File projectDir;
    private TaskStatus status;
    private String errorMessage;
    private MZmineProjectImpl project;
    private float finished=(float)0.0;
    private static final float FINISHED_STARTED=0.1f;
    private static final float FINISHED_OBJECT_FREEZED=0.3f;
    private static final float FINISHED_START_ZIPPING=0.4f;
    private static final float FINISHED_COMPLETE=1.0f;
    /**
     * 
     */
    public ProjectSavingTask(File projectDir) {

        this.projectDir = projectDir;
        status = TaskStatus.WAITING;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Saving project to " + projectDir;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        return finished;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        // Update task status
        logger.info("Started saving project" + projectDir);
        status = TaskStatus.PROCESSING;
        finished=FINISHED_STARTED;
        try {        	
            //Store all project in db4o database
        	File dbFile = new File(projectDir,"mzmine.db4o");
        	ObjectContainer db = Db4o.openFile(dbFile.toString());
        	//first remove existing project
        	List <MZmineProjectImpl>result_project= db.query(new Predicate<MZmineProjectImpl>() {
        	    public boolean match(MZmineProjectImpl mzmineProject) {
        	     return true;	
        	    }
        	});
        	for (MZmineProjectImpl oldProject :result_project){
        		db.delete(oldProject);
        	}
        	
        	db.set(MZmineCore.getCurrentProject());
        	db.commit();
        	db.close();

		} catch (Throwable e) {
            logger.log(Level.SEVERE, "Could not save project "
                    + projectDir.getPath(), e);
            errorMessage = e.toString();
            status = TaskStatus.ERROR;
            return;
        }

        logger.info("Finished saving " + projectDir);

        status = TaskStatus.FINISHED;
        finished=FINISHED_COMPLETE;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        logger.info("Cancelling saving of project" + projectDir);
        status = TaskStatus.CANCELED;
    }

    public File getResult(){
    	return projectDir;
    }
}
