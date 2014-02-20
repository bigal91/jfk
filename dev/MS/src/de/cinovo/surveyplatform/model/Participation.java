package de.cinovo.surveyplatform.model;

import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;

import de.cinovo.surveyplatform.sync.Sync;
import de.cinovo.surveyplatform.sync.SyncFilter;
import de.cinovo.surveyplatform.sync.SyncIdentifiable;
import de.cinovo.surveyplatform.util.Logger;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
@Entity
public class Participation implements Cloneable, SyncIdentifiable<String> {
	
	private String id;
	
	private Participant participant;
	
	@Sync(filter = SyncFilter.PARTICIPANT, recurse = true)
	private Questionnaire questionnaire;
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private boolean submitted = false;
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private String submittedBy;
	
	
	@Id
	public String getId() {
		return this.id;
	}
	
	@XmlID
	@Transient
	public String getXMLID() {
		return this.getClass().getSimpleName() + this.getId();
	}
	
	public void setId(final String id) {
		this.id = id;
	}
	
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@XmlIDREF
	public Participant getParticipant() {
		return this.participant;
	}
	
	public void setParticipant(final Participant participant) {
		this.participant = participant;
	}
	
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@XmlIDREF
	public Questionnaire getQuestionnaire() {
		return this.questionnaire;
	}
	
	public void setQuestionnaire(final Questionnaire questionnaire) {
		this.questionnaire = questionnaire;
	}
	
	public boolean isSubmitted() {
		return this.submitted;
	}
	
	public void setSubmitted(final boolean submitted) {
		this.submitted = submitted;
	}
	
	public String getSubmittedBy() {
		return this.submittedBy;
	}
	
	public void setSubmittedBy(final String submittedBy) {
		this.submittedBy = submittedBy;
	}
	
	@Override
	public Participation clone() {
		Participation clone = null;
		try {
			clone = (Participation) super.clone();
			clone.setId(UUID.randomUUID().toString());
			clone.setParticipant(null);
			if (this.questionnaire != null) {
				clone.setQuestionnaire(this.questionnaire.clone());
				clone.getQuestionnaire().setParticipation(clone);
			}
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return clone;
	}
	
	public Participation cloneWithId() {
		Participation clone = null;
		try {
			clone = (Participation) super.clone();
			clone.setId(this.getId());
			clone.setParticipant(null);
			Questionnaire tmpQ = this.getQuestionnaire();
			if (tmpQ != null) {
				Questionnaire cloneQ = tmpQ.cloneWithId();
				cloneQ.setParticipation(clone);
				clone.setQuestionnaire(cloneQ);
			}
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return clone;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.sync.SyncIdentifiable#getSyncId()
	 */
	@Override
	@Transient
	@XmlTransient
	public String getSyncId() {
		return this.id;
	}
	
}
