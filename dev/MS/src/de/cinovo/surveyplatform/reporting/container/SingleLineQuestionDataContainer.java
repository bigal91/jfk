/**
 * 
 */
package de.cinovo.surveyplatform.reporting.container;

import java.util.List;

import de.cinovo.surveyplatform.model.Topic;
import de.cinovo.surveyplatform.model.question.Answer;


/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class SingleLineQuestionDataContainer implements IReportDataContainer {
	
	public List<Answer> answers;
	public List<Topic> topics;
	
}
