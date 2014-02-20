/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@Entity
public class QuestionnaireLogicElement implements Cloneable {
	
	public enum Operator {
		AND, OR
	}
	
	
	private int id;
	
	private Integer surveyId;
	
	private Operator operator;
	
	private List<Answer> answers = new ArrayList<Answer>();
	
	private String typeOfPart;
	
	private int idOfPart;
	
	private int questionIdWithLogic;
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public int getId() {
		return this.id;
	}
	
	private void setId(final int id) {
		this.id = id;
	}
	
	public Operator getOperator() {
		return this.operator;
	}
	
	public void setOperator(final Operator operator) {
		this.operator = operator;
	}
	
	@OneToMany(cascade = CascadeType.ALL)
	public List<Answer> getAnswers() {
		return this.answers;
	}
	
	public void setAnswers(final List<Answer> answers) {
		this.answers = answers;
	}
	
	public int getIdOfPart() {
		return this.idOfPart;
	}
	
	public void setIdOfPart(final int idOfPart) {
		this.idOfPart = idOfPart;
	}
	
	public int getQuestionIdWithLogic() {
		return this.questionIdWithLogic;
	}
	
	public void setQuestionIdWithLogic(final int questionIdWithLogic) {
		this.questionIdWithLogic = questionIdWithLogic;
	}
	
	/**
	 * @param typeOfPart the typeOfPart to set
	 */
	public void setTypeOfPart(final String typeOfPart) {
		this.typeOfPart = typeOfPart;
	}
	
	/**
	 * @return the surveyId
	 */
	@Column(nullable = true)
	public Integer getSurveyId() {
		return surveyId;
	}
	
	/**
	 * @param surveyId the surveyId to set
	 */
	public void setSurveyId(final Integer surveyId) {
		this.surveyId = surveyId;
	}
	
	/**
	 * @return the typeOfPart
	 */
	@Column(columnDefinition = "text")
	public String getTypeOfPart() {
		return this.typeOfPart;
	}
	
	@SuppressWarnings("unchecked")
	@Transient
	public Class<IQuestionnairePart> getClassOfPart() throws ClassNotFoundException {
		return (Class<IQuestionnairePart>) Class.forName(this.typeOfPart);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public QuestionnaireLogicElement clone() {
		QuestionnaireLogicElement logicElement = null;
		try {
			logicElement = (QuestionnaireLogicElement) super.clone();
			logicElement.setId(0);
			
			if (this.answers != null) {
				logicElement.setAnswers(new ArrayList<Answer>());
				for (Answer answer : this.answers) {
					logicElement.getAnswers().add(new Answer(answer.getAnswer()));
				}
			}
			
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
			// clone IS supported
		}
		return logicElement;
	}
	
	/**
	 * @return
	 */
	public QuestionnaireLogicElement cloneWithId() {
		QuestionnaireLogicElement clone = this.clone();
		clone.setId(this.getId());
		return clone;
	}
}
