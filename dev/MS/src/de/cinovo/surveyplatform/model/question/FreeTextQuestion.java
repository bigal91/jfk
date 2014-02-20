package de.cinovo.surveyplatform.model.question;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import de.cinovo.surveyplatform.model.IAnalysableQuestion;
import de.cinovo.surveyplatform.model.IFreeTextQuestion;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
@Entity
public class FreeTextQuestion extends AbstractQuestion implements IAnalysableQuestion, IFreeTextQuestion {
	
	private Answer answer;
	private boolean multiLine = true;
	
	
	@OneToOne(cascade = CascadeType.ALL)
	public Answer getAnswerObj() {
		return answer;
	}
	
	public void setAnswerObj(final Answer anwerObj) {
		this.answer = anwerObj;
	}
	
	/**
	 * @param multiLine the multiLine to set
	 */
	public void setMultiLine(final boolean multiLine) {
		this.multiLine = multiLine;
	}
	
	/**
	 * @return the multiLine
	 */
	public boolean isMultiLine() {
		return multiLine;
	}
	
	@Override
	@Transient
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
		FreeTextQuestion question = (FreeTextQuestion) super.clone();
		if (answer != null) {
			question.setAnswerObj(answer.clone());
		}
		
		return question;
	}
	
	@Override
	public AbstractQuestion cloneWithId() {
		FreeTextQuestion question = (FreeTextQuestion) super.cloneWithId();
		if (answer != null) {
			question.setAnswerObj(answer.cloneWithId());
		}
		
		return question;
	}
}
