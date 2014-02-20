package de.cinovo.surveyplatform.model.jsondto;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class SurveyDto extends AbstractJsonDto {
	
	public int id;
	
	public int questionnaireId;
	
	public int totalParticipants;
	
	public int submittedQuestionnaires;
	
	public String owner;
	
	public String name;
	
	public String description;
	
	public String state;
	
	public String creationDate;
	
	public String runningDate;
	
	public String closingDate;
	
	public String creator;
	
	public String eMailSender;
	
	public String senderName;
}
