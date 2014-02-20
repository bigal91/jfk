/**
 *
 */
package de.cinovo.surveyplatform.feedback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.cinovo.surveyplatform.feedback.TaskInfo.StatusCode;
import de.cinovo.surveyplatform.model.jsondto.TaskInfoDto;
import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * This class provides a framework for providing progress and feedback.
 * Implemented as singleton.
 * 
 * @author yschubert
 * 
 */
public class FeedBackProvider {
	
	public final static long MAX_TASKINFO_AGE = 18000000l; // 5 hrs
	
	private static final int MINMIMUM_PERCENT = 5;
	private static final int MAXIMUM_PERCENT = 100;
	
	
	public enum Status {
		OK, WARNING, ERROR
	}
	
	
	private static FeedBackProvider instance;
	
	private Map<String, TaskInfo> taskMap = new HashMap<String, TaskInfo>();
	
	
	private FeedBackProvider() {
		// singleton class: use getInstance() instead of instantiating the class
	}
	
	/**
	 * cleans the taskmap from old tasks
	 */
	private synchronized void clean() {
		List<String> tasksToRemove = new ArrayList<String>();
		for (Entry<String, TaskInfo> entry : this.taskMap.entrySet()) {
			if (entry.getValue().getAgeByFinishTime() > FeedBackProvider.MAX_TASKINFO_AGE) {
				tasksToRemove.add(entry.getKey());
			}
		}
		for (String taskID : tasksToRemove) {
			this.removeTaskInfo(taskID);
		}
	}
	
	public static FeedBackProvider getInstance() {
		if (FeedBackProvider.instance == null) {
			FeedBackProvider.instance = new FeedBackProvider();
		}
		return FeedBackProvider.instance;
	}
	
	public void addFeedback(final String taskID, final String feedBack, final Status status) {
		this.addFeedback(taskID, feedBack, status, "-");
	}
	
	public void addFeedback(final String taskID, final String feedBack, final Status status, final String userID) {
		TaskInfo taskInfo = this.getTaskInfo(taskID);
		if (taskInfo == null) {
			taskInfo = this.beginTask("", taskID, userID);
		}
		taskInfo.getResults().add("<span class=\"feedBack" + status.name() + "\">" + feedBack + "</span>");
		
		if (!status.equals(Status.OK)) {
			if (status.equals(Status.WARNING)) {
				taskInfo.setHasWarnings(true);
			}
			if (status.equals(Status.ERROR)) {
				taskInfo.setHasErrors(true);
			}
			Logger.err(status.name() + " Feedback (task: " + taskID + "): " + feedBack);
		}
	}
	
	public void setMessage(final String taskID, final String message) {
		this.setMessage(taskID, message, "-");
	}
	
	public void setMessage(final String taskID, final String message, final String userID) {
		TaskInfo taskInfo = this.getTaskInfo(taskID);
		if (taskInfo == null) {
			taskInfo = this.beginTask("", taskID, userID);
		}
		taskInfo.setMessage(message);
	}
	
	public void removeTaskInfo(final String taskID) {
		this.taskMap.remove(taskID);
	}
	
	public TaskInfo beginTask(final String taskName, final String taskID, final String userID) {
		this.clean();
		TaskInfo taskInfo = new TaskInfo();
		taskInfo.setCreatedBy(userID);
		taskInfo.setProgress(MINMIMUM_PERCENT);
		taskInfo.setResults(new ArrayList<String>());
		taskInfo.setStatusCode(StatusCode.RUNNING);
		taskInfo.setTaskID(taskID);
		taskInfo.setTaskName(taskName);
		this.taskMap.put(taskID, taskInfo);
		return taskInfo;
	}
	
	public void setProgress(final String taskID, int percent) {
		if (percent < MINMIMUM_PERCENT) {
			percent = MINMIMUM_PERCENT;
		}
		if (percent > MAXIMUM_PERCENT) {
			percent = MAXIMUM_PERCENT;
		}
		TaskInfo taskInfo = this.getTaskInfo(taskID);
		if (taskInfo != null) {
			taskInfo.setProgress(percent);
		}
	}
	
	public void finishTask(final String taskID) {
		TaskInfo taskInfo = this.getTaskInfo(taskID);
		if (taskInfo != null) {
			taskInfo.setProgress(MAXIMUM_PERCENT);
			boolean errorOccured = false;
			for (String result : taskInfo.getResults()) {
				if (result.contains("feedBack" + Status.ERROR)) {
					errorOccured = true;
					break;
				}
			}
			if (errorOccured) {
				taskInfo.setStatusCode(StatusCode.ERROR);
			} else {
				taskInfo.setStatusCode(StatusCode.FINISHED);
			}
		}
	}
	
	public void finishTask(final String taskID, final boolean errorOccured) {
		TaskInfo taskInfo = this.getTaskInfo(taskID);
		if (taskInfo != null) {
			taskInfo.setProgress(MAXIMUM_PERCENT);
			if (errorOccured) {
				taskInfo.setStatusCode(StatusCode.ERROR);
			} else {
				taskInfo.setStatusCode(StatusCode.FINISHED);
			}
		}
	}
	
	public TaskInfo getTaskInfo(final String taskID) {
		TaskInfo taskInfo = this.taskMap.get(taskID);
		return taskInfo;
	}
	
	public TaskInfoDto getTaskInfoDto(final String taskID) {
		TaskInfo taskInfo = this.getTaskInfo(taskID);
		if (taskInfo != null) {
			TaskInfoDto dto = new TaskInfoDto();
			dto.results = taskInfo.getResults().toArray(new String[taskInfo.getResults().size()]);
			dto.taskID = taskInfo.getTaskID();
			dto.taskName = taskInfo.getTaskName();
			dto.statusCode = taskInfo.getStatusCode().name();
			dto.progress = taskInfo.getProgress();
			dto.estimatedFinishTime = taskInfo.getEstimatedFinishTime();
			return dto;
		}
		return null;
	}
	
	public boolean isRunning(final String taskID) {
		TaskInfo taskInfo = this.getTaskInfo(taskID);
		if (taskInfo != null) {
			return taskInfo.getStatusCode() != StatusCode.FINISHED;
		}
		return false;
	}
	
	/**
	 * @return the taskMap
	 */
	public Map<String, TaskInfo> getTaskMap() {
		return Collections.unmodifiableMap(this.taskMap);
	}
}
