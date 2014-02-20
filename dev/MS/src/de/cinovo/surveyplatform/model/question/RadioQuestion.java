package de.cinovo.surveyplatform.model.question;

import javax.persistence.Entity;

import de.cinovo.surveyplatform.model.IAnalysableQuestion;
import de.cinovo.surveyplatform.model.ILogicApplicableQuestion;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@Entity
public class RadioQuestion extends AbstractQuestion implements IMultipleOptionsQuestion, IAlignmentQuestion, ILogicApplicableQuestion, IAnalysableQuestion, IDecisionQuestion {
	
	private Alignment alignment = Alignment.HORIZONTAL;
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.model.question.IAlignmentQuestion#getAlignment()
	 */
	@Override
	public Alignment getAlignment() {
		return alignment;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.model.question.IAlignmentQuestion#setAlignment()
	 */
	@Override
	public void setAlignment(final Alignment alignment) {
		this.alignment = alignment;
	}
	
}
