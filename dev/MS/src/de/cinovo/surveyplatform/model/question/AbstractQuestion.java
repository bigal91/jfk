package de.cinovo.surveyplatform.model.question;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;

import de.cinovo.surveyplatform.constants.ChartType;
import de.cinovo.surveyplatform.model.IQuestionnairePart;
import de.cinovo.surveyplatform.model.ISortable;
import de.cinovo.surveyplatform.model.ITemplateCloneable;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement;
import de.cinovo.surveyplatform.model.Section;
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
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractQuestion implements Cloneable, ITemplateCloneable, IQuestionnairePart, Comparable<AbstractQuestion>, ISortable, SyncIdentifiable<String> {
	
	private int id;
	
	private String syncId;
	
	private int localId = 0;
	
	private int questionnaireID;
	
	private int orderNumber = 0;
	
	private String question = "";
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private boolean visible = true;
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private boolean previewModeVisible = true;
	
	private int originQuestionId;
	
	private String alias;
	
	private String additionalInfo;
	
	private String activeLogic;
	
	private List<QuestionnaireLogicElement> logicElements;
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private boolean submitted;
	
	private boolean interesting = true;
	
	private boolean isTargetOfLogic;
	
	private Section section;
	
	@Sync(filter = SyncFilter.PARTICIPANT, recurse = true)
	private List<Option> options = new ArrayList<Option>();
	
	@Sync(filter = SyncFilter.PARTICIPANT, recurse = true)
	private List<AbstractQuestion> subquestions = new ArrayList<AbstractQuestion>();
	
	private AbstractQuestion parentQuestion;
	
	private ChartType chartType = ChartType.bar;
	
	
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
	
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "question", fetch = FetchType.LAZY)
	@XmlElement(name = "options")
	@OrderBy("orderNumber")
	public List<Option> getOptions() {
		return options;
	}
	
	public void setOptions(final List<Option> options) {
		this.options = options;
	}
	
	/**
	 * @return the subquestions
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parentQuestion", fetch = FetchType.LAZY)
	@OrderBy("orderNumber")
	public List<AbstractQuestion> getSubquestions() {
		return subquestions;
	}
	
	/**
	 * @param subquestions the subquestions to set
	 */
	public void setSubquestions(final List<AbstractQuestion> subquestions) {
		this.subquestions = subquestions;
	}
	
	/**
	 * @return the parentQuestion
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parentQuestion_id", nullable = true)
	@XmlIDREF
	public AbstractQuestion getParentQuestion() {
		return parentQuestion;
	}
	
	/**
	 * @param parentQuestion the parentQuestion to set
	 */
	public void setParentQuestion(final AbstractQuestion parentQuestion) {
		this.parentQuestion = parentQuestion;
	}
	
	/**
	 * @return the section
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "section_id", nullable = true)
	@XmlIDREF
	public Section getSection() {
		return section;
	}
	
	/**
	 * @param section the section to set
	 */
	public void setSection(final Section section) {
		this.section = section;
	}
	
	/**
	 * @return This value contains the order in which the question must appear
	 *         in the questionnaire
	 */
	public int getOrderNumber() {
		return orderNumber;
	}
	
	public void setOrderNumber(final int orderNumber) {
		this.orderNumber = orderNumber;
	}
	
	/**
	 * @return Text of the question
	 */
	@Column(columnDefinition = "text")
	public String getQuestion() {
		return question;
	}
	
	public void setQuestion(final String question) {
		this.question = question;
	}
	
	@Column(columnDefinition = "text")
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(final String alias) {
		this.alias = alias;
	}
	
	/**
	 *
	 * @return true, if the question is visible in the questionnaire
	 */
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(final boolean visible) {
		this.visible = visible;
	}
	
	@Column(columnDefinition = "boolean not null default true")
	public boolean isPreviewModeVisible() {
		return previewModeVisible;
	}
	
	public void setPreviewModeVisible(final boolean previewVisible) {
		this.previewModeVisible = previewVisible;
	}
	
	/**
	 * @return If this value is > 0 then it contains the ID of the question of
	 *         the original questionnaire template
	 */
	public int getOriginQuestionId() {
		return originQuestionId;
	}
	
	/** <strong>CAUTION: Be careful with setting this value!</strong> **/
	public void setOriginQuestionId(final int originQuestionId) {
		this.originQuestionId = originQuestionId;
	}
	
	@Transient
	public List<Answer> getAnswer() {
		return getAnswer(getAllOptions());
	}
	
	
	@Transient
	public List<Answer> getAnswer(final List<Option> options) {
		List<Answer> answer = new ArrayList<Answer>();
		if (options != null) {
			List<Answer> selectedOptions = new ArrayList<Answer>();
			for (Option option : options) {
				if (option.isSelected()) {
					selectedOptions.add(new Answer(option.getDisplayName()));
				}
			}
			answer.addAll(selectedOptions);
		}
		return answer;
	}
	
	public void setLogicElements(final List<QuestionnaireLogicElement> logicElements) {
		this.logicElements = logicElements;
	}
	
	@OneToMany(cascade = CascadeType.ALL)
	public List<QuestionnaireLogicElement> getLogicElements() {
		return logicElements;
	}
	
	public void setLocalId(final int localId) {
		this.localId = localId;
	}
	
	public int getLocalId() {
		return localId;
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
	
	@Override
	public AbstractQuestion clone() {
		AbstractQuestion newQuestion = null;
		try {
			newQuestion = (AbstractQuestion) super.clone();
			newQuestion.setId(0);
			newQuestion.setOriginQuestionId(0);
			newQuestion.setVisible(true);
			
			newQuestion.setLogicElements(new ArrayList<QuestionnaireLogicElement>());
			for (QuestionnaireLogicElement qle : this.getLogicElements()) {
				QuestionnaireLogicElement clone = qle.clone();
				newQuestion.getLogicElements().add(clone);
			}
			
			newQuestion.setSubquestions(null);
			newQuestion.setParentQuestion(null);
			
			List<Option> optionList = getOptions();
			if (optionList == null) {
				newQuestion.setOptions(null);
			} else {
				newQuestion.setOptions(new ArrayList<Option>());
				for (Option option : optionList) {
					Option newOption = option.clone();
					newQuestion.getOptions().add(newOption);
					newOption.setQuestion(newQuestion);
				}
			}
			
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		return newQuestion;
	}
	
	@Override
	public AbstractQuestion templateClone() {
		AbstractQuestion newQuestion = null;
		newQuestion = this.clone();
		newQuestion.setId(0);
		newQuestion.setSubmitted(false);
		
		// this will propagate the origin questionId (is overwritten by clone)
		newQuestion.setOriginQuestionId(this.id);
		if (getParentQuestion() != null) {
			newQuestion.getParentQuestion().setOriginQuestionId(getParentQuestion().getOriginQuestionId());
		}
		return newQuestion;
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final AbstractQuestion o) {
		if (this.orderNumber < o.getOrderNumber()) {
			return -1;
		} else if (this.orderNumber > o.getOrderNumber()) {
			return 1;
		}
		return 0;
	}
	
	/**
	 * @return
	 */
	@Transient
	@XmlTransient
	public List<Option> getAllOptions() {
		List<Option> options = new ArrayList<Option>();
		options.addAll(getOptions());
		if (subquestions != null) {
			for (AbstractQuestion sq : subquestions) {
				for (Option opt : sq.getOptions()) {
					options.add(opt);
				}
			}
		}
		return options;
	}
	
	public void setInteresting(final boolean isInteresting) {
		this.interesting = isInteresting;
	}
	
	public boolean getInteresting() {
		return this.interesting;
	}
	
	public void setChartType(final ChartType chartType) {
		this.chartType = chartType;
	}
	
	@Enumerated(EnumType.ORDINAL)
	public ChartType getChartType() {
		return chartType;
	}
	
	/**
	 * @return
	 */
	public AbstractQuestion cloneWithId() {
		AbstractQuestion newQuestion = null;
		try {
			newQuestion = (AbstractQuestion) super.clone();
			newQuestion.setId(getId());
			newQuestion.setOriginQuestionId(getOriginQuestionId());
			newQuestion.setLogicElements(new ArrayList<QuestionnaireLogicElement>());
			for (QuestionnaireLogicElement qle : this.getLogicElements()) {
				QuestionnaireLogicElement qleClone = qle.cloneWithId();
				newQuestion.getLogicElements().add(qleClone);
			}
			
			newQuestion.setSubquestions(null);
			newQuestion.setParentQuestion(null);
			
			List<Option> optionList = getOptions();
			if (optionList == null) {
				newQuestion.setOptions(null);
			} else {
				newQuestion.setOptions(new ArrayList<Option>());
				for (Option option : optionList) {
					Option newOption = option.cloneWithId();
					newQuestion.getOptions().add(newOption);
					newOption.setQuestion(newQuestion);
				}
			}
			
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		return newQuestion;
	}
	
	/**
	 * @param activeLogic the activeLogic to set
	 */
	public void setActiveLogic(final String activeLogic) {
		this.activeLogic = activeLogic;
	}
	
	/**
	 * @return the activeLogic
	 */
	public String getActiveLogic() {
		return activeLogic;
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
		return isTargetOfLogic;
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
	
	@Column(columnDefinition = "text")
	public String getAdditionalInfo() {
		return additionalInfo;
	}
	
	public void setAdditionalInfo(final String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	
}
