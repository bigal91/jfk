/**
 *
 */
package de.cinovo.surveyplatform.reporting.container;

import java.util.List;

import de.cinovo.surveyplatform.model.Topic;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author ablehm
 *
 */
public class AverageNumberQuestionDataContainer implements IReportDataContainer {
	
	public List<String> answers;
	public List<Topic> topics;
	public int numberOfResponses;
}
