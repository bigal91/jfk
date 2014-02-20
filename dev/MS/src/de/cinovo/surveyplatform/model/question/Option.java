package de.cinovo.surveyplatform.model.question;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import de.cinovo.surveyplatform.sync.Sync;
import de.cinovo.surveyplatform.sync.SyncFilter;
import de.cinovo.surveyplatform.sync.SyncIdentifiable;
import de.cinovo.surveyplatform.util.Logger;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * An option represents an option one survey participant can select from
 * a multiplechoice, radio or combo question
 * 
 * @author yschubert
 * 
 */
@Entity
public class Option implements Cloneable, SyncIdentifiable<String> {
	
	private int id;
	private String displayName = "";
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private boolean selected = false;
	
	private int originQuestionId = 0;
	
	private int questionnaireID = 0;
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private boolean submitted = false;
	
	private AbstractQuestion question;
	private String syncId;
	
	private int orderNumber = 0;
	
	
	public Option() {
		// for hibernate
	}
	
	public Option(final String displayName) {
		this.displayName = displayName;
	}
	
	public Option(final String displayName, final boolean selected) {
		this.displayName = displayName;
		this.selected = selected;
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}
	
	@XmlID
	@Transient
	public String getXMLID() {
		return this.getClass().getSimpleName() + getId();
	}
	
	private void setId(final int id) {
		this.id = id;
	}
	
	/**
	 * @return the question
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id", nullable = false)
	@XmlIDREF
	public AbstractQuestion getQuestion() {
		return question;
	}
	
	/**
	 * @param question the question to set
	 */
	public void setQuestion(final AbstractQuestion question) {
		this.question = question;
	}
	
	/**
	 * @return This value represents the name of the option
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}
	
	public void setSelected(final boolean selected) {
		this.selected = selected;
	}
	
	/**
	 * @return true, if the option is selected in the questionnaire
	 */
	public boolean isSelected() {
		return selected;
	}
	
	/**
	 * Options have the id of the origin question to improve performance on
	 * analysing reports.
	 * 
	 * @param originQuestionId Id of the question containing this option
	 */
	public void setOriginQuestionId(final int originQuestionId) {
		this.originQuestionId = originQuestionId;
	}
	
	public int getOriginQuestionId() {
		return originQuestionId;
	}
	
	/**
	 * @param submitted the submitted to set
	 */
	public void setSubmitted(final boolean submitted) {
		this.submitted = submitted;
	}
	
	/**
	 * @return the submitted
	 */
	public boolean isSubmitted() {
		return submitted;
	}
	
	public int getQuestionnaireID() {
		return questionnaireID;
	}
	
	public void setQuestionnaireID(final int questionnaireID) {
		this.questionnaireID = questionnaireID;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.sync.SyncIdentifiable#getSyncId()
	 */
	@Override
	public String getSyncId() {
		return syncId;
	}
	
	/**
	 * @param syncId the syncId to set
	 */
	public void setSyncId(final String syncId) {
		this.syncId = syncId;
	}
	
	/**
	 * @return the orderNumber
	 */
	@Column(columnDefinition = "integer not null default 0")
	public int getOrderNumber() {
		return orderNumber;
	}
	
	/**
	 * @param orderNumber the orderNumber to set
	 */
	public void setOrderNumber(final int orderNumber) {
		this.orderNumber = orderNumber;
	}
	
	/**
	 * Caution: This clone sets the ID of the cloned Option to 0
	 */
	@Override
	public Option clone() {
		try {
			Option newOption = (Option) super.clone();
			newOption.setId(0);
			// newOption.setSubmitted(false);
			return newOption;
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		return null;
	}
	
	/**
	 * @return
	 */
	public Option cloneWithId() {
		Option clone = clone();
		clone.setId(getId());
		clone.setQuestion(null);
		return clone;
	}
	
}
