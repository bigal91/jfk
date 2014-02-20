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
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
@Entity
public class SingleLineQuestion extends AbstractQuestion implements IAnalysableQuestion, IFreeTextQuestion {
	
	private Answer answer;
	private String hint;
	
	/**
	 * @param hint the hint to set
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
	 * @param answer the answer to set
	 */
	public void setAnswerObj(final Answer answer) {
		this.answer = answer;
	}
	
	@OneToOne(cascade = CascadeType.ALL)
	public Answer getAnswerObj() {
		return answer;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.model.question.AbstractQuestion#getAnswer()
	 */
	
	@Transient
	@Override
	public List<Answer> getAnswer() {
		List<Answer> answerList = new ArrayList<Answer>();
		if (answer != null) {
			answerList.add(answer);
		}
		return answerList;
	}
	
	// @Column(columnDefinition = "text")
	// public String getTextValue() {
	// if (answer == null) {
	// return null;
	// }
	// return answer.getAnswer();
	// }
	//
	// public void setTextValue(final String textValue) {
	// if (answer == null) {
	// answer = new Answer();
	// }
	// answer.setAnswer(textValue);
	// }
	
	@Override
	public AbstractQuestion clone() {
		SingleLineQuestion question = (SingleLineQuestion) super.clone();
		if (answer != null) {
			question.setAnswerObj(answer.clone());
		}
		
		return question;
	}
	
	@Override
	public AbstractQuestion cloneWithId() {
		SingleLineQuestion question = (SingleLineQuestion) super.cloneWithId();
		if (answer != null) {
			question.setAnswerObj(answer.cloneWithId());
		}
		
		return question;
	}
	
}



