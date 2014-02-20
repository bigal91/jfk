package de.cinovo.surveyplatform.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.question.ComboQuestion;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceMatrixQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceQuestion;
import de.cinovo.surveyplatform.model.question.RadioMatrixQuestion;
import de.cinovo.surveyplatform.model.question.RadioQuestion;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.sync.Sync;
import de.cinovo.surveyplatform.sync.SyncFilter;
import de.cinovo.surveyplatform.sync.SyncIdentifiable;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
@Entity
public class Section implements Cloneable, ITemplateCloneable, IQuestionnairePart, Comparable<Section>, ISortable, SyncIdentifiable<String> {
	
	private int id;
	
	private int orderNumber = 0;
	
	private String sectionTitle = "";
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private List<AbstractQuestion> questions;
	
	private String syncId;
	
	private boolean visible = true;
	
	private boolean previewModeVisible;
	
	private boolean isTargetOfLogic;
	
	private int localId;
	
	private Page page;
	
	
	@Override
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
	 * @return the page
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "page_id", nullable = false)
	@XmlIDREF
	public Page getPage() {
		return this.page;
	}
	
	/**
	 * @param page the page to set
	 */
	public void setPage(final Page page) {
		this.page = page;
	}
	
	@Override
	public int getOrderNumber() {
		return this.orderNumber;
	}
	
	@Override
	public void setOrderNumber(final int orderNumber) {
		this.orderNumber = orderNumber;
	}
	
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "section", fetch = FetchType.LAZY)
	@XmlElements({@XmlElement(name = "SingleLineQuestion", type = SingleLineQuestion.class), @XmlElement(name = "ComboQuestion", type = ComboQuestion.class), @XmlElement(name = "FreeTextQuestion", type = FreeTextQuestion.class), @XmlElement(name = "MultipleChoiceMatrixQuestion", type = MultipleChoiceMatrixQuestion.class), @XmlElement(name = "MultipleChoiceQuestion", type = MultipleChoiceQuestion.class), @XmlElement(name = "RadioMatrixQuestion", type = RadioMatrixQuestion.class), @XmlElement(name = "RadioQuestion", type = RadioQuestion.class), @XmlElement(name = "TextfieldQuestion", type = TextfieldQuestion.class), @XmlElement(name = "TextPart", type = TextPart.class), @XmlElement(name = "AverageNumberQuestion", type = AverageNumberQuestion.class)})
	@OrderBy("orderNumber")
	public List<AbstractQuestion> getQuestions() {
		return this.questions;
	}
	
	public void setQuestions(final List<AbstractQuestion> questions) {
		this.questions = questions;
	}
	
	@Column(columnDefinition = "text")
	public String getSectionTitle() {
		return this.sectionTitle;
	}
	
	public void setSectionTitle(final String sectionTitle) {
		this.sectionTitle = sectionTitle;
	}
	
	// @OneToOne(optional = true)
	// public AbstractQuestion getMainQuestion() {
	// return mainQuestion;
	// }
	//
	// public void setMainQuestion(final AbstractQuestion mainQuestion) {
	// this.mainQuestion = mainQuestion;
	// }
	
	public boolean isVisible() {
		return this.visible;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.model.IQuestionnairePart#setVisible(boolean)
	 */
	@Override
	public void setVisible(final boolean visible) {
		this.visible = visible;
		
		// // inherit visibility to childs
		// if (questions != null) {
		// for (AbstractQuestion q : questions) {
		// q.setVisible(visible);
		// }
		// }
	}
	
	public void setLocalId(final int localId) {
		this.localId = localId;
	}
	
	public int getLocalId() {
		return this.localId;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final Section o) {
		if (this.orderNumber < o.getOrderNumber()) {
			return -1;
		} else if (this.orderNumber > o.getOrderNumber()) {
			return 1;
		}
		return 0;
	}
	
	@Override
	public Section clone() {
		Section newSection = null;
		try {
			newSection = (Section) super.clone();
			newSection.setId(0);
			newSection.setQuestions(new ArrayList<AbstractQuestion>());
			// if (this.mainQuestion != null) {
			// newSection.setMainQuestion(null);
			// }
			if (this.questions != null) {
				AbstractQuestion[] questionsArray = this.questions.toArray(new AbstractQuestion[this.questions.size()]);
				for (AbstractQuestion question : questionsArray) {
					AbstractQuestion newQuestion = question.clone();
					newSection.getQuestions().add(newQuestion);
					newQuestion.setSection(newSection);
				}
			}
		} catch (CloneNotSupportedException e) {
			// clone IS supported
		}
		return newSection;
	}
	
	@Override
	public Section templateClone() {
		Section newSection = null;
		try {
			newSection = (Section) super.clone();
			newSection.setId(0);
			newSection.setQuestions(new ArrayList<AbstractQuestion>());
			// if (this.mainQuestion != null) {
			// newSection.setMainQuestion(null);
			// }
			if (this.questions != null) {
				AbstractQuestion[] questionsArray = this.questions.toArray(new AbstractQuestion[this.questions.size()]);
				for (AbstractQuestion question : questionsArray) {
					AbstractQuestion templateClone = question.templateClone();
					newSection.getQuestions().add(templateClone);
					templateClone.setSection(newSection);
				}
			}
		} catch (CloneNotSupportedException e) {
			// clone IS supported
		}
		return newSection;
	}
	
	/**
	 * @return
	 */
	public Section cloneWithId() {
		Section newSection = null;
		try {
			newSection = (Section) super.clone();
			newSection.setId(this.getId());
			newSection.setPage(null);
			newSection.setQuestions(new ArrayList<AbstractQuestion>());
			if (this.questions != null) {
				AbstractQuestion[] questionsArray = this.questions.toArray(new AbstractQuestion[this.questions.size()]);
				for (AbstractQuestion question : questionsArray) {
					AbstractQuestion newQuestion = question.cloneWithId();
					newSection.getQuestions().add(newQuestion);
					newQuestion.setSection(newSection);
				}
			}
		} catch (CloneNotSupportedException e) {
			// clone IS supported
		}
		return newSection;
	}
	
	/**
	 * @param isTargetOfLogic the isTargetOfLogic to set
	 */
	public void setTargetOfLogic(final boolean isTargetOfLogic) {
		this.isTargetOfLogic = isTargetOfLogic;
	}
	
	/**
	 * @return the isTargetOfLogic
	 */
	@Column(columnDefinition = "boolean not null default false")
	public boolean isTargetOfLogic() {
		return this.isTargetOfLogic;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.model.IQuestionnairePart#setPreviewModeVisible
	 * (boolean)
	 */
	@Override
	public void setPreviewModeVisible(final boolean previewModeVisible) {
		this.previewModeVisible = previewModeVisible;
	}
	
	@Column(columnDefinition = "boolean not null default true")
	public boolean isPreviewModeVisible() {
		return this.previewModeVisible;
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
