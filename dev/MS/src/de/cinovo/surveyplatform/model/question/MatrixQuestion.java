/**
 *
 */
package de.cinovo.surveyplatform.model.question;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;

import de.cinovo.surveyplatform.model.IAnalysableQuestion;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class MatrixQuestion extends AbstractQuestion implements IMatrixQuestion, IAnalysableQuestion {
	
	@Override
	public AbstractQuestion clone() {
		
		MatrixQuestion question = (MatrixQuestion) super.clone();
		question.setSubquestions(new ArrayList<AbstractQuestion>());
		List<AbstractQuestion> subquestions = getSubquestions();
		if (subquestions != null) {
			for (AbstractQuestion subQuestion : subquestions) {
				AbstractQuestion clonedSQ = subQuestion.clone();
				clonedSQ.setOriginQuestionId(subQuestion.getId());
				question.getSubquestions().add(clonedSQ);
				clonedSQ.setParentQuestion(question);
			}
		}
		
		return question;
	}
	
	@Override
	public AbstractQuestion cloneWithId() {
		MatrixQuestion question = (MatrixQuestion) super.cloneWithId();
		question.setSubquestions(new ArrayList<AbstractQuestion>());
		List<AbstractQuestion> subquestions = getSubquestions();
		if (subquestions != null) {
			for (AbstractQuestion subQuestion : subquestions) {
				AbstractQuestion clonedSQ = subQuestion.cloneWithId();
				clonedSQ.setOriginQuestionId(subQuestion.getId());
				question.getSubquestions().add(clonedSQ);
				clonedSQ.setParentQuestion(question);
			}
		}
		
		return question;
	}
	
	/**
	 * Retrieves all options of the subquestions
	 * 
	 * @return Return list of Options
	 */
	@Transient
	public List<Option> getSubquestionOptions() {
		List<Option> options = new ArrayList<Option>();
		List<AbstractQuestion> subquestions = getSubquestions();
		if (subquestions != null) {
			for (AbstractQuestion sq : subquestions) {
				for (Option opt : sq.getOptions()) {
					options.add(opt);
				}
			}
		}
		return options;
	}
}
