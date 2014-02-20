package de.cinovo.surveyplatform.model.question;

import javax.persistence.Entity;

import de.cinovo.surveyplatform.model.IAnalysableQuestion;
import de.cinovo.surveyplatform.model.ILogicApplicableQuestion;


/**
 *
 * Copyright 2010 Cinovo AG
 *
 * @author yschubert
 *
 */
@Entity
public class MultipleChoiceQuestion extends AbstractQuestion implements IMultipleOptionsQuestion, IAlignmentQuestion, ILogicApplicableQuestion, IAnalysableQuestion {
	
	private Alignment alignment = Alignment.HORIZONTAL;
	
	/*
	 * (non-Javadoc)
	 *
	 * @see de.cinovo.surveyplatform.model.question.IAlignmentQuestion#getAlignment()
	 */
	@Override
	public Alignment getAlignment() {
		return alignment;
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.cinovo.surveyplatform.model.question.IAlignmentQuestion#setAlignment(de.cinovo.surveyplatform.model.question
	 * .IAlignmentQuestion.Alignment)
	 */
	@Override
	public void setAlignment(final Alignment alignment) {
		this.alignment = alignment;
	}
	
}
