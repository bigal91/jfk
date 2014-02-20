package de.cinovo.surveyplatform.model.jsondto;

import java.util.Map;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class ParticipantDto extends AbstractJsonDto {
	
	public int id;
	public int number;
	public String name;
	public String surname;
	public String email;
	public String phone;
	public String participationId;
	public String invitedDateString;
	public String remindedDateString;
	public String submittedDateString;
	
	public Map<String, String> properties;
	
}
