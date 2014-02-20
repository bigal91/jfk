package de.cinovo.surveyplatform.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import de.cinovo.surveyplatform.sync.Sync;
import de.cinovo.surveyplatform.sync.SyncFilter;
import de.cinovo.surveyplatform.sync.SyncIdentifiable;
import de.cinovo.surveyplatform.util.Logger;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * Participant of a survey
 * 
 * @author yschubert
 * 
 */
@Entity
public class Participant implements Cloneable, SyncIdentifiable<String> {
	
	private int id;
	
	private String syncId;
	
	private int number;
	
	private String contactEmail = "";
	
	private String name = "";
	private String surname = "";
	private String contactPhone = "";
	private PersistentProperties properties;
	
	private Date invitationSent;
	private Date reminderSent;
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private Date surveySubmitted;
	
	private boolean askByPhone = false;
	
	private boolean emailInQueue = false;
	
	@Sync(filter = SyncFilter.PARTICIPANT, recurse = true)
	private Participation participation;
	
	private Survey survey;
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return this.id;
	}
	
	@XmlID
	@Transient
	public String getXMLID() {
		return this.getClass().getSimpleName() + this.getId();
	}
	
	private void setId(final int id) {
		this.id = id;
	}
	
	/**
	 * @return the survey
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "survey_id", nullable = true)
	@XmlIDREF
	public Survey getSurvey() {
		return this.survey;
	}
	
	/**
	 * @param survey the survey to set
	 */
	public void setSurvey(final Survey survey) {
		this.survey = survey;
	}
	
	public int getNumber() {
		return this.number;
	}
	
	public void setNumber(final int number) {
		this.number = number;
	}
	
	/**
	 * @param properties the properties to set
	 */
	public void setProperties(final PersistentProperties properties) {
		this.properties = properties;
	}
	
	/**
	 * @return the properties
	 */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
	@XmlIDREF
	public PersistentProperties getProperties() {
		return this.properties;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	public String getContactEmail() {
		return this.contactEmail;
	}
	
	public void setContactEmail(final String contactEmail) {
		this.contactEmail = contactEmail;
	}
	
	public Date getInvitationSent() {
		return this.invitationSent;
	}
	
	public void setInvitationSent(final Date invitationSent) {
		this.invitationSent = invitationSent;
	}
	
	public Date getReminderSent() {
		return this.reminderSent;
	}
	
	public void setReminderSent(final Date reminderSent) {
		this.reminderSent = reminderSent;
	}
	
	public Date getSurveySubmitted() {
		return this.surveySubmitted;
	}
	
	public void setSurveySubmitted(final Date surveySubmitted) {
		this.surveySubmitted = surveySubmitted;
	}
	
	public void setParticipation(final Participation participation) {
		this.participation = participation;
	}
	
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@XmlIDREF
	public Participation getParticipation() {
		return this.participation;
	}
	
	public void setAskByPhone(final boolean askByPhone) {
		this.askByPhone = askByPhone;
	}
	
	public boolean isAskByPhone() {
		return this.askByPhone;
	}
	
	public String getSurname() {
		return this.surname;
	}
	
	public void setSurname(final String surname) {
		this.surname = surname;
	}
	
	public String getContactPhone() {
		return this.contactPhone;
	}
	
	public void setContactPhone(final String contactPhone) {
		this.contactPhone = contactPhone;
	}
	
	public boolean isEmailInQueue() {
		return this.emailInQueue;
	}
	
	public void setEmailInQueue(final boolean emailInQueue) {
		this.emailInQueue = emailInQueue;
	}
	
	@Override
	public Participant clone() {
		Participant newParticipant = null;
		try {
			newParticipant = (Participant) super.clone();
			newParticipant.setId(0);
			if (this.participation != null) {
				newParticipant.setParticipation(this.getParticipation().clone());
				newParticipant.getParticipation().setParticipant(newParticipant);
			}
			newParticipant.setSurvey(new Survey());
			newParticipant.getSurvey().setSyncId(UUID.randomUUID().toString());
			newParticipant.getSurvey().setId(this.getSurvey().getId());
			newParticipant.setProperties(this.getProperties().clone());
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return newParticipant;
	}
	
	public Participant cloneWithId() {
		Participant newParticipant = null;
		try {
			newParticipant = (Participant) super.clone();
			newParticipant.setId(this.getId());
			if (this.participation != null) {
				newParticipant.setParticipation(this.getParticipation().cloneWithId());
				newParticipant.getParticipation().setParticipant(newParticipant);
			}
			newParticipant.setSurvey(this.getSurvey().cloneWithId());
			newParticipant.setProperties(this.getProperties().cloneWithId());
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return newParticipant;
	}
	
	public Participant cloneForExport() {
		Participant newParticipant = null;
		try {
			newParticipant = (Participant) super.clone();
			newParticipant.setId(this.getId());
			if (this.participation != null) {
				newParticipant.setParticipation(this.getParticipation().cloneWithId());
				newParticipant.getParticipation().setParticipant(newParticipant);
			}
			newParticipant.setSurvey(this.getSurvey().cloneForExport());
			newParticipant.setProperties(this.getProperties().clone());
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return newParticipant;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.sync.SyncIdentifiable#getSyncId()
	 */
	@Override
	public String getSyncId() {
		return this.syncId;
	}
	
	/**
	 * @param syncId the syncId to set
	 */
	public void setSyncId(final String syncId) {
		this.syncId = syncId;
	}
	
}
