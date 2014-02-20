package de.cinovo.surveyplatform.util;

import java.util.List;
import java.util.regex.Pattern;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.question.IMatrixQuestion;
import de.cinovo.surveyplatform.model.question.IMultipleOptionsQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * Utility class for check if an answer is valid.
 * 
 * @author yschubert
 * 
 */
public class QuestionValidator {
	
	private QuestionValidator() {
		// utility class. not meant to be instantiated
	}
	
	/**
	 * Checks if the given question is answered (but not submitted!), which means <br>
	 * that if the question is filled at "participation" time it is already answered.<br>
	 * Additionaly freeText Questions are only seen as answered, if they contain text.
	 * 
	 * @param question
	 * @return true, if the question is answered at "participation" time
	 */
	public static boolean isAnswered(final AbstractQuestion question) {
		if ((question instanceof FreeTextQuestion)) {
			FreeTextQuestion freeText = (FreeTextQuestion) question;
			if (freeText.getAnswerObj() != null) {
				if (freeText.getAnswerObj().getAnswer() != null) {
					if (!freeText.getAnswerObj().getAnswer().equals("")) {
						// freeText Question is answered
						return true;
					}
				}
			}
		} else if (question instanceof TextPart) {
			// is always "answered"
			return true;
		} else if (question instanceof AverageNumberQuestion) {
			if (((AverageNumberQuestion) question).getAnswerObj() != null) {
				String answer = ((AverageNumberQuestion) question).getAnswerObj().getAnswer();
				if (answer != null) {
					if (!answer.equals("")) {
						// question is Answered
						return true;
					}
				}
			}
		} else if ((question instanceof SingleLineQuestion) || (question instanceof TextfieldQuestion)) {
			if (QuestionValidator.atLeastOneAnswerIsGiven(question)) {
				// question is answered
				return true;
			}
		} else if (question instanceof IMatrixQuestion) {
			for (AbstractQuestion subQuestion : question.getSubquestions()) {
				if (!QuestionValidator.atLeastOneOptionSelected(subQuestion)) {
					return false;
				}
			}
			// all options for this kind of question selected? Question is answered!
			return true;
		} else if (question instanceof IMultipleOptionsQuestion) {
			if (QuestionValidator.atLeastOneOptionSelected(question)) {
				// question is answered
				return true;
			}
		}
		// no possible question answered? So it is not seen as answered.
		return false;
	}
	
	/**
	 * Checks if the given question is valid by Integers 0, 1 and 2 If 0 is returned, the question is valid If 1 is returned, the question
	 * was not answered If 2 is returned, the format of the question was invalid
	 * 
	 * @param question The question to validate
	 * @return true in case of a {@link FreeTextQuestion} or {@link TextPart}. true if at least one option of a
	 *         {@link IMultipleOptionsQuestion} is selected.
	 */
	public static int isValid(final AbstractQuestion question) {
		// only valid, when validInteger == 0 (or not (2 or 3))
		int validInteger = 0;
		if ((question instanceof FreeTextQuestion) || (question instanceof TextPart)) {
			validInteger = 0; // text is always valid
		} else if ((question instanceof AverageNumberQuestion) || (question instanceof SingleLineQuestion)) {
			if (!QuestionValidator.atLeastOneAnswerIsGiven(question)) {
				validInteger = 1;
			} else {
				if (question instanceof AverageNumberQuestion) {
					if (((AverageNumberQuestion) question).getAnswerObj() != null) {
						String answer = ((AverageNumberQuestion) question).getAnswerObj().getAnswer();
						if (answer == null) {
							validInteger = 1;
						} else if (answer.toLowerCase().trim().replaceAll("\\s", "").equals("noresponse")) {
							validInteger = 0;
						} else if (!Pattern.matches("[0-9\\,\\.]+", answer)) {
							validInteger = 2;
						}
					}
				}
			}
			// validInteger = 0; // text is always valid
		} else if (question instanceof TextfieldQuestion) {
			if (!QuestionValidator.atLeastOneAnswerIsGiven(question)) {
				validInteger = 1;
			}
		} else if (question instanceof IMatrixQuestion) {
			for (AbstractQuestion subQuestion : question.getSubquestions()) {
				if (!QuestionValidator.atLeastOneOptionSelected(subQuestion)) {
					validInteger = 1;
					break;
				}
			}
		} else if (question instanceof IMultipleOptionsQuestion) {
			if (!QuestionValidator.atLeastOneOptionSelected(question)) {
				validInteger = 1;
			}
		}
		return validInteger;
	}
	
	private static boolean atLeastOneAnswerIsGiven(final AbstractQuestion question) {
		List<Answer> answers = question.getAnswer();
		boolean valid = false;
		for (Answer answer : answers) {
			if (!answer.getAnswer().equals("")) {
				valid = true;
				break;
			}
		}
		return valid;
	}
	
	private static boolean atLeastOneOptionSelected(final AbstractQuestion question) {
		if (question.getOptions() != null) {
			for (Option option : question.getAllOptions()) {
				if (option.isSelected()) {
					return true;
				}
			}
		}
		return false;
	}
	
}
