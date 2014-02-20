/**
 *
 */
package de.cinovo.surveyplatform.model.question;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import de.cinovo.surveyplatform.model.IAnalysableQuestion;
import de.cinovo.surveyplatform.model.IFreeTextQuestion;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * A question that reads only digits / numbers on the plattform and calculates the average after each submission.
 * 
 * @author ablehm
 * 
 * 
 */
@Entity
public class AverageNumberQuestion extends AbstractQuestion implements IAnalysableQuestion, IFreeTextQuestion {
	
	private Answer answer;
	private String hint;
	
	
	/**
	 * @param hint
	 *            the hint to set, it is displayed
	 *            next to the text-input-box to the
	 *            participant
	 */
	public void setHint(final String hint) {
		this.hint = hint;
	}
	
	/**
	 * @return the hint
	 */
	@Column(columnDefinition = "text")
	public String getHint() {
		return hint;
	}
	
	/**
	 * @param textValue
	 *            the textValue to set
	 */
	public void setAnswerObj(final Answer textValue) {
		this.answer = textValue;
	}
	
	/**
	 * @return the textValue
	 */
	@OneToOne(cascade = CascadeType.ALL)
	public Answer getAnswerObj() {
		return answer;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.model.question.AbstractQuestion#getAnswer()
	 */
	@Override
	@Transient
	public List<Answer> getAnswer() {
		List<Answer> answerList = new ArrayList<Answer>();
		if (answer != null) {
			answerList.add(answer);
		}
		return answerList;
	}
	
	@Override
	public AbstractQuestion clone() {
		AverageNumberQuestion question = (AverageNumberQuestion) super.clone();
		if (answer != null) {
			question.setAnswerObj(answer.clone());
		}
		
		return question;
	}
	
	@Override
	public AbstractQuestion cloneWithId() {
		AverageNumberQuestion question = (AverageNumberQuestion) super.cloneWithId();
		if (answer != null) {
			question.setAnswerObj(answer.cloneWithId());
		}
		
		return question;
	}
	
}
