/**
 * 
 */
package de.cinovo.surveyplatform.reporting.container;

import de.cinovo.surveyplatform.model.chart.DataSetContainer;


/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class MultipleChoiceQuestionDataContainer implements IReportDataContainer {
	
	public int numberOfResponses;
	public DataSetContainer dataSet;
}
