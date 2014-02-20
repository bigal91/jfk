package de.cinovo.surveyplatform.ui.views;


import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public interface IQuestionView {
	
	public static final String DEFAULT_DATASETNAME = "_default_";
	
	
	/**
	 * @return Creates a visible representation of the question in HTML
	 */
	String getHTMLRepresentation(TargetMedia targetMedia);
	
	// /**
	// * @return Creates the iText representation of the question
	// */
	// Element getITextRepresentation();
	
	/**
	 * @param result When <code>true</code>, the values of the question answers
	 *            are printed. When <code>false</code>, placeholders are printed
	 * @return a printable representation of this question
	 */
	String getPrintableRepresentation(boolean result);
	
	/**
	 * 
	 * @param dataContainer Container which holds the aggregated values
	 * @param representationMetadata data which holds information about the representation
	 * @param sheet the excel sheet object
	 * @return a printable representation of the question analysis (aggregated values)
	 */
	void getAggregatedExcelRepresentation(IReportDataContainer dataContainer, RepresentationMetadata representationMetadata, Sheet sheet);
	
	/**
	 * 
	 * @param dataContainer Container which holds the aggregated values
	 * @param representationMetadata data which holds information about the
	 *            representation
	 * @return a printable representation of the question analysis (aggregated
	 *         values)
	 */
	String getAggregatedPrintableRepresentation(IReportDataContainer dataContainer, RepresentationMetadata representationMetadata, AbstractQuestion question);
}
