/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.util.List;

import de.cinovo.surveyplatform.model.question.Answer;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public interface IFreeTextQuestion {
	
	/**
	 * 
	 * @return the answered text
	 */
	public List<Answer> getAnswer();
	
	/**
	 * @param textValue the answered text
	 */
	public void setAnswerObj(Answer answer);
	
	/**
	 * the answer object
	 */
	public Answer getAnswerObj();
}
