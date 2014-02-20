/**
 * 
 */
package de.cinovo.surveyplatform.reporting.container;

import java.util.List;

import de.cinovo.surveyplatform.model.chart.DataSetContainer;


/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class MatrixQuestionChartDataContainer implements IReportDataContainer {
	
	public int numberOfResponses;
	public List<DataSetContainer> dataSets;
	public String filePath;
}
