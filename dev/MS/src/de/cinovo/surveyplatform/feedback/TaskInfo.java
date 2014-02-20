/**
 *
 */
package de.cinovo.surveyplatform.feedback;

import java.util.List;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class TaskInfo {
	
	public enum StatusCode {
		IDLE, RUNNING, FINISHED, ERROR
	}
	
	
	private String taskID;
	
	private String taskName;
	
	private String createdBy;
	
	private List<String> results;
	
	private String longTaskResult;
	
	private StatusCode statusCode = StatusCode.IDLE;
	
	private long creationTime = -1;
	private long finishTime = -1;
	private long startRunningTime = -1;
	private int progress;
	
	private String message;
	
	private boolean hasWarnings;
	
	private boolean hasErrors;
	
	
	public TaskInfo() {
		this.creationTime = System.currentTimeMillis();
	}
	
	public String getTaskID() {
		return this.taskID;
	}
	
	public void setTaskID(final String taskID) {
		this.taskID = taskID;
	}
	
	public String getTaskName() {
		return this.taskName;
	}
	
	public void setTaskName(final String taskName) {
		this.taskName = taskName;
	}
	
	public StatusCode getStatusCode() {
		return this.statusCode;
	}
	
	public void setStatusCode(final StatusCode statusCode) {
		this.statusCode = statusCode;
		if (StatusCode.ERROR.equals(statusCode) || StatusCode.FINISHED.equals(statusCode)) {
			this.finishTime = System.currentTimeMillis();
		} else if (StatusCode.RUNNING.equals(statusCode)) {
			startRunningTime = System.currentTimeMillis();
		}
	}
	
	public long getAgeByCreationTime() {
		return System.currentTimeMillis() - this.creationTime;
	}
	
	public long getAgeByFinishTime() {
		if (StatusCode.ERROR.equals(this.statusCode) || StatusCode.FINISHED.equals(this.statusCode)) {
			return System.currentTimeMillis() - this.finishTime;
		}
		return 0;
	}
	
	public long getEstimatedFinishTime() {
		if (StatusCode.RUNNING.equals(this.statusCode)) {
			if (progress == 0) {
				return -1;
			} else {
				return (Math.round(System.currentTimeMillis() - startRunningTime) * 100) / progress;
			}
		} else {
			return 0;
		}
	}

	public int getProgress() {
		return this.progress;
	}
	
	public void setProgress(final int progress) {
		this.progress = progress;
	}
	
	public List<String> getResults() {
		return this.results;
	}
	
	public void setResults(final List<String> results) {
		this.results = results;
	}
	
	/**
	 * @param message
	 */
	public void setMessage(final String message) {
		this.message = message;
	}
	
	/**
	 * @return the message
	 */
	public String getMessage() {
		return this.message;
	}
	
	public String getCreatedBy() {
		return this.createdBy;
	}
	
	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}
	
	public String getLongTaskResult() {
		return this.longTaskResult;
	}
	
	public void setLongTaskResult(final String longTaskResult) {
		this.longTaskResult = longTaskResult;
	}
	
	/**
	 * @return true if any message contains an error message
	 */
	public boolean hasErrors() {
		return this.hasErrors;
	}
	
	public boolean hasWarnings() {
		return this.hasWarnings;
	}
	
	/**
	 * @param hasWarnings -
	 */
	public void setHasWarnings(boolean hasWarnings) {
		hasWarnings = true;
		
	}
	
	/**
	 * @param hasWarnings -
	 */
	public void setHasErrors(boolean hasErrors) {
		hasErrors = true;
	}
	
}
