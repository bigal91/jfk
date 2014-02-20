/**
 *
 */
package de.cinovo.surveyplatform.reporting;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
@Deprecated
public class RelevantQuestionsContainer {
	
	@XmlAttribute(required = false)
	private int surveyID;
	
	@XmlElements({@XmlElement(name = "question", type = RelevantQuestion.class)})
	private List<RelevantQuestion> relevantQuestions;
	
	
	public List<RelevantQuestion> getRelevantQuestions() {
		return this.relevantQuestions;
	}
	
	public void setRelevantQuestions(final List<RelevantQuestion> relevantQuestions) {
		this.relevantQuestions = relevantQuestions;
	}
	
	public int getSurveyID() {
		return this.surveyID;
	}
	
	public void setSurveyID(final int surveyID) {
		this.surveyID = surveyID;
	}
	
}
