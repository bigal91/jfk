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
public class ComboQuestion extends AbstractQuestion implements IMultipleOptionsQuestion, ILogicApplicableQuestion, IAnalysableQuestion {
	
}
