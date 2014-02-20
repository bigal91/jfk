package de.cinovo.surveyplatform.reporting;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;

@XmlAccessorType(value = XmlAccessType.FIELD)
public class RelevantQuestion {
	
	@XmlAttribute
	private String name;
	
	private int questionId;
	
	@XmlTransient
	private Class<? extends AbstractQuestion> questionType;
	
	
	public String getName() {
		return this.name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	public int getQuestionId() {
		return this.questionId;
	}
	
	public void setQuestionId(final int questionId) {
		this.questionId = questionId;
	}
	
	public Class<? extends AbstractQuestion> getQuestionType() {
		return this.questionType;
	}
	
	public void setQuestionType(final Class<? extends AbstractQuestion> questionType) {
		this.questionType = questionType;
	}
	
}