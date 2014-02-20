/**
 * 
 */
package de.cinovo.surveyplatform.reporting.container;

import de.cinovo.surveyplatform.ui.views.IQuestionView;

/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class MatrixQuestionDataContainer implements IReportDataContainer {
	
	public int numberOfResponses;
	public int[][] valueMap;
	public String datasetName = IQuestionView.DEFAULT_DATASETNAME;
}
