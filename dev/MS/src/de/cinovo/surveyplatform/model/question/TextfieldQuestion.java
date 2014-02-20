/**
 *
 */
package de.cinovo.surveyplatform.model.question;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import de.cinovo.surveyplatform.model.IAnalysableQuestion;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
@Entity
public class TextfieldQuestion extends AbstractQuestion implements IAnalysableQuestion {
	
	private List<Answer> answers = new ArrayList<Answer>();
	
	/**
	 * @return the answers
	 */
	@OneToMany(cascade = CascadeType.ALL)
	public List<Answer> getAnswers() {
		return answers;
	}
	
	/**
	 * @param answers the answers to set
	 */
	public void setAnswers(final List<Answer> answers) {
		this.answers = answers;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.model.question.AbstractQuestion#getAnswer()
	 */
	@Override
	@Transient
	public List<Answer> getAnswer() {
		if (this.answers == null) {
			return new ArrayList<Answer>();
		}
		
		return answers;
	}
	
	@Override
	public AbstractQuestion clone() {
		TextfieldQuestion question = (TextfieldQuestion) super.clone();
		question.setAnswers(new ArrayList<Answer>());
		for (Answer answer : this.answers) {
			question.getAnswers().add(answer.clone());
		}
		
		return question;
	}
	
	@Override
	public AbstractQuestion cloneWithId() {
		TextfieldQuestion question = (TextfieldQuestion) super.clone();
		question.setAnswers(new ArrayList<Answer>());
		for (Answer answer : this.answers) {
			question.getAnswers().add(answer.cloneWithId());
		}
		
		return question;
	}
}
