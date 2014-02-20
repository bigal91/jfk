/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class TextPartView extends AbstractQuestionView {
	
	private TextPart question;
	
	
	public TextPartView(final TextPart question) {
		super(question);
		this.question = question;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.ui.views.IQuestionView#getAggregatedExcelRepresentation(de.cinovo.surveyplatform.reporting.container.
	 * IReportDataContainer, de.cinovo.surveyplatform.reporting.RepresentationMetadata,
	 * de.cinovo.surveyplatform.model.question.AbstractQuestion, org.apache.poi.ss.usermodel.Sheet)
	 */
	@Override
	public void getAggregatedExcelRepresentation(final IReportDataContainer dataContainer, final RepresentationMetadata representationMetadata, final Sheet sheet) {
		Row row = sheet.createRow(sheet.getPhysicalNumberOfRows() + 1);
		Cell cell = row.createCell(0);
		cell.setCellValue("No excel representation available!");
	}
	
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		if (question.getTextValue() != null) {
			// rep.append(WikiUtil.parseToHtml(question.getTextValue()));
			rep.append(postProcess(question.getTextValue()));
		}
		return rep.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.ui.views.AbstractQuestionView#
	 * getPrintableRepresentation()
	 */
	@Override
	public String getPrintableRepresentation(final boolean result) {
		return getHTMLRepresentation(TargetMedia.PRINTER_QUESTIONNAIRE);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.ui.views.AbstractQuestionView#
	 * getAggregatedPrintableRepresentation
	 * (java.lang.Object)
	 */
	@Override
	public String getAggregatedPrintableRepresentation(final IReportDataContainer dataContainer, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion) {
		return getHTMLRepresentation(representationMetadata.targetMedia);
	}
}