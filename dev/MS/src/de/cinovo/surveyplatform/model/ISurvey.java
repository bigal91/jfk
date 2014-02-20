/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.util.Date;
import java.util.List;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public interface ISurvey {
	
	/*
	 * 
	 * DO NOT CHANGE THE ORDERING OF THE VALUES AS THE CODE RELIES ON THAT IN SOME PLACES!
	 */
	public enum SurveyState {
		/**
		 * A Survey in this state can be used for creating a template for a non
		 * admin
		 */
		SYSTEMTEMPLATE,
		/**
		 * A Survey in this state can be used for creating another template or a
		 * new concrete survey
		 */
		TEMPLATE,
		/**
		 * A Survey in this state is a concrete Survey, that is not yet running.
		 * Editing the Questionnaire is prohibited in this state.
		 */
		CREATED,
		/**
		 * A survey in this state is a running survey
		 */
		RUNNING,
		/**
		 * A survey in this state is closed. The data of the questionnaire can
		 * be
		 * used for generating reports.
		 */
		CLOSED;
		
		public static String getDisplayName(final SurveyState state) {
			switch (state) {
			case TEMPLATE:
				return "Template";
			case CLOSED:
				return "Closed";
			case RUNNING:
				return "Running";
			case CREATED:
				return "Created";
			default:
				return "Public Template";
			}
		}
	}
	
	
	Date getClosedAtDate();
	
	Date getCreationDate();
	
	SystemUser getCreator();
	
	String getDescription();
	
	String getEmailTextInvite();
	
	String getEmailTextRemind();
	
	Integer getId();
	
	String getName();
	
	List<Participant> getParticipants();
	
	Questionnaire getQuestionnaire();
	
	Date getRunningSinceDate();
	
	SurveyState getState();
	
	String getStateDisplayname();
	
	String getEmailSender();
	
	String getSenderName();
	
	void setSenderName(String senderName);
	
	void setEmailSender(String eMailSender);
	
	void setClosedAtDate(Date closedAt);
	
	void setCreationDate(Date createdAt);
	
	void setCreator(SystemUser creator);
	
	void setDeleted(boolean deleted);
	
	void setDescription(String dscription);
	
	void setEmailTextInvite(String emailTextInvite);
	
	void setEmailTextRemind(String emailTextRemind);
	
	void setName(String name);
	
	void setParticipants(List<Participant> participants);
	
	void setQuestionnaire(Questionnaire clone);
	
	void setRunningSinceDate(Date runningSince);
	
	void setState(SurveyState state);
	
	boolean isPublicSurvey();
	
	boolean isDeleted();
	
}
