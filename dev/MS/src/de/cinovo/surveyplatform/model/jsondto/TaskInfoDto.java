/**
 * 
 */
package de.cinovo.surveyplatform.model.jsondto;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class TaskInfoDto extends AbstractJsonDto {
	
	public String taskID;
	
	public String taskName;
	
	public String[] results;
	
	public String statusCode;
	
	public int progress;
	
	public long estimatedFinishTime;
}
