package de.cinovo.surveyplatform.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

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
@XmlRootElement
public class Survey implements ISurvey, Cloneable, SyncIdentifiable<String> {
	
	private Integer id;
	
	private String syncId;
	
	private SystemUser creator;
	
	private UserGroup owner;
	
	private Project project;
	
	@Sync(filter = {SyncFilter.SURVEY}, recurse = true)
	private Questionnaire questionnaire;
	
	private List<Participant> participants;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String name = "";
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String description = "";
	
	@Sync(filter = {SyncFilter.SURVEY})
	private SurveyState state = SurveyState.TEMPLATE;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private boolean publicSurvey = false;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private Date creationDate = new Date();
	
	@Sync(filter = {SyncFilter.SURVEY})
	private Date runningSinceDate = null;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private Date closedAtDate = null;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private boolean deleted = false;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String emailTextInvite;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String emailTextRemind;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String emailSubjectInvite;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String emailSubjectRemind;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String emailSender;
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String senderName;
	
	
	@Sync(filter = {SyncFilter.SURVEY})
	private String extendedProperties;
	
	
	@Override
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return this.id;
	}
	
	/**
	 * @return -
	 */
	@XmlID
	@Transient
	public String getXMLID() {
		return this.getClass().getSimpleName() + this.getId();
	}
	
	/**
	 * @param id .
	 **/
	public void setId(final Integer id) {
		this.id = id;
	}
	
	@Override
	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}
	
	@Override
	public boolean isDeleted() {
		return this.deleted;
	}
	
	@Override
	@OneToOne(cascade = CascadeType.ALL, optional = true)
	@XmlIDREF
	public Questionnaire getQuestionnaire() {
		return this.questionnaire;
	}
	
	@Override
	public void setQuestionnaire(final Questionnaire questionnaire) {
		this.questionnaire = questionnaire;
	}
	
	@Override
	@Column(columnDefinition = "text")
	public String getDescription() {
		return this.description;
	}
	
	@Override
	public void setDescription(final String description) {
		this.description = description;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setName(final String name) {
		this.name = name;
	}
	
	@Override
	@OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@XmlIDREF
	public SystemUser getCreator() {
		return this.creator;
	}
	
	@Override
	public void setCreator(final SystemUser creator) {
		this.creator = creator;
	}
	
	@Override
	public SurveyState getState() {
		return this.state;
	}
	
	@Override
	public void setState(final SurveyState state) {
		this.state = state;
	}
	
	@Override
	public boolean isPublicSurvey() {
		return this.publicSurvey;
	}
	
	/**
	 * @param publicSurvey k
	 */
	public void setPublicSurvey(final boolean publicSurvey) {
		this.publicSurvey = publicSurvey;
	}
	
	@Override
	public void setCreationDate(final Date creationDate) {
		this.creationDate = creationDate;
	}
	
	@Override
	public Date getCreationDate() {
		return this.creationDate;
	}
	
	@Override
	public Date getRunningSinceDate() {
		return this.runningSinceDate;
	}
	
	@Override
	public void setRunningSinceDate(final Date runningSinceDate) {
		this.runningSinceDate = runningSinceDate;
	}
	
	@Override
	public Date getClosedAtDate() {
		return this.closedAtDate;
	}
	
	@Override
	public void setClosedAtDate(final Date closedAtDate) {
		this.closedAtDate = closedAtDate;
	}
	
	@Override
	@Column(columnDefinition = "text")
	public String getEmailTextInvite() {
		return this.emailTextInvite;
	}
	
	@Override
	public void setEmailTextInvite(final String emailTextInvite) {
		this.emailTextInvite = emailTextInvite;
	}
	
	@Override
	@Column(columnDefinition = "text")
	public String getEmailTextRemind() {
		return this.emailTextRemind;
	}
	
	@Override
	public void setEmailTextRemind(final String emailTextRemind) {
		this.emailTextRemind = emailTextRemind;
	}
	
	@Override
	public void setEmailSender(final String eMailSender) {
		this.emailSender = eMailSender;
	}
	
	@Override
	public String getEmailSender() {
		return this.emailSender;
	}
	
	@Override
	public void setSenderName(final String senderName) {
		this.senderName = senderName;
	}
	
	@Override
	public String getSenderName() {
		return this.senderName;
	}
	
	/**
	 * @return the participants
	 */
	@Override
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "survey", fetch = FetchType.LAZY)
	public List<Participant> getParticipants() {
		return this.participants;
	}
	
	/**
	 * @param participants the participants to set
	 */
	@Override
	public void setParticipants(final List<Participant> participants) {
		this.participants = participants;
	}
	
	@Override
	@Transient
	public String getStateDisplayname() {
		return SurveyState.getDisplayName(this.state);
	}
	
	/**
	 * @return the owner
	 */
	@OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, optional = true)
	@XmlIDREF
	public UserGroup getOwner() {
		return this.owner;
	}
	
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(final UserGroup owner) {
		this.owner = owner;
	}
	
	/**
	 * @return the extendedProperties
	 */
	public String getExtendedProperties() {
		return this.extendedProperties;
	}
	
	/**
	 * @param extendedProperties the extendedProperties to set
	 */
	public void setExtendedProperties(final String extendedProperties) {
		this.extendedProperties = extendedProperties;
	}
	
	/**
	 * @return the emailSubjectInvite
	 */
	public String getEmailSubjectInvite() {
		return this.emailSubjectInvite;
	}
	
	/**
	 * @param emailSubjectInvite the emailSubjectInvite to set
	 */
	public void setEmailSubjectInvite(final String emailSubjectInvite) {
		this.emailSubjectInvite = emailSubjectInvite;
	}
	
	/**
	 * @return the emailSubjectRemind
	 */
	public String getEmailSubjectRemind() {
		return this.emailSubjectRemind;
	}
	
	/**
	 * @param emailSubjectRemind the emailSubjectRemind to set
	 */
	public void setEmailSubjectRemind(final String emailSubjectRemind) {
		this.emailSubjectRemind = emailSubjectRemind;
	}
	
	/**
	 * @return the project
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@XmlIDREF
	public Project getProject() {
		return this.project;
	}
	
	/**
	 * @param project the project to set
	 */
	public void setProject(final Project project) {
		this.project = project;
	}
	
	@Override
	public Survey clone() {
		Survey clone = null;
		try {
			clone = (Survey) super.clone();
			clone.setId(0);
			if (this.creator != null) {
				clone.setCreator(this.creator.clone());
			}
			if (this.owner != null) {
				clone.setOwner(this.owner.clone());
			}
			if (this.questionnaire != null) {
				clone.setQuestionnaire(this.questionnaire.clone());
			}
			
			if (this.participants != null) {
				List<Participant> newParticipants = new ArrayList<Participant>();
				for (Participant p : this.participants) {
					newParticipants.add(p.clone());
				}
				clone.setParticipants(newParticipants);
			}
			
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return clone;
	}
	
	/**
	 * @return -
	 */
	public Survey cloneWithId() {
		Survey clone = null;
		try {
			clone = (Survey) super.clone();
			clone.setId(this.id);
			if (this.creator != null) {
				clone.setCreator(this.creator.cloneWithId());
			}
			if (this.owner != null) {
				clone.setOwner(this.owner.cloneWithId());
			}
			if (this.questionnaire != null) {
				clone.setQuestionnaire(this.questionnaire.cloneWithId());
			}
			
			if (this.participants != null) {
				List<Participant> newParticipants = new ArrayList<Participant>();
				for (Participant p : this.participants) {
					newParticipants.add(p.cloneWithId());
				}
				clone.setParticipants(newParticipants);
			}
			
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return clone;
	}
	
	/**
	 * @return -
	 */
	public Survey cloneForExport() {
		Survey clone = null;
		try {
			clone = (Survey) super.clone();
			clone.setId(this.id);
			clone.setCreator(null);
			clone.setOwner(null);
			if (this.questionnaire != null) {
				clone.setQuestionnaire(this.questionnaire.clone());
			}
			clone.setParticipants(null);
			
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return clone;
		
	}
	
	/**
	 * @param syncId the syncId to set
	 */
	public void setSyncId(final String syncId) {
		this.syncId = syncId;
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
	
}
