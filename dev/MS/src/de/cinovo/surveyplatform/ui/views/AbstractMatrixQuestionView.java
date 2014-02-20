/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.model.chart.DataSetContainer;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.MatrixQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.RadioQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.reporting.container.MatrixQuestionChartDataContainer;
import de.cinovo.surveyplatform.reporting.container.MatrixQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.generator.ReportGeneratorUtils;
import de.cinovo.surveyplatform.util.ExcelUtil;
import de.cinovo.surveyplatform.util.ExcelUtil.CellPointer;
import de.cinovo.surveyplatform.util.WikiUtil;



/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public abstract class AbstractMatrixQuestionView extends AbstractQuestionView {
	
	public AbstractMatrixQuestionView(final MatrixQuestion question) {
		super(question);
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
		CellPointer p = new CellPointer(sheet);
		p.jumpLastRow();
		writeQuestionToCell(p);
		Cell c = p.nextRow();
		if (dataContainer instanceof MatrixQuestionDataContainer) {
			MatrixQuestionDataContainer dc = (MatrixQuestionDataContainer) dataContainer;
			
			List<AbstractQuestion> subquestions = question.getSubquestions();
			for (OutputMode mode : OutputMode.values()) {
				c = p.nextColumn();
				for (Option option : subquestions.get(0).getOptions()) {
					c.setCellStyle(ExcelUtil.createBlueStyle(c));
					c.setCellValue(option.getDisplayName());
					c = p.nextColumn();
				}
				
				int subQuestionCount = subquestions.size();
				if (subQuestionCount > 0) {
					int optionCount = subquestions.get(0).getOptions().size();
					
					for (int y = 0; y < subQuestionCount; y++) {
						c = p.nextRow();
						c.setCellStyle(ExcelUtil.createBlueStyle(c));
						c.setCellValue((subquestions.get(y)).getQuestion());
						for (int x = 0; x < optionCount; x++) {
							c = p.nextColumn();
							int count = dc.valueMap[y][x];
							if (mode.equals(OutputMode.ABSOLUTE)) {
								c.setCellValue(count);
							} else {
								c.setCellValue(((double) count / dc.numberOfResponses) * 100);
								ExcelUtil.createPercentStyle(c);
							}
						}
					}
				}
				c = p.nextRow();
				c.setCellValue("n = " + dc.numberOfResponses);
				p.jumpRow(2);
			}
		}
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
		
		if (dataContainer instanceof MatrixQuestionDataContainer) {
			return createHTMLTable((MatrixQuestionDataContainer) dataContainer, representationMetadata, currentQuestion);
		}else if (dataContainer instanceof MatrixQuestionChartDataContainer) {
			return createChart((MatrixQuestionChartDataContainer) dataContainer, representationMetadata, currentQuestion);
		}
		return "Don't know how to handle: " + dataContainer.getClass().getName();
	}
	
	/**
	 * @param dataContainer
	 */
	private String createChart(final MatrixQuestionChartDataContainer dc, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion) {
		return getDefaultChart(dc.filePath, representationMetadata.surveyID, Arrays.asList(new Integer[] {dc.numberOfResponses}), representationMetadata, currentQuestion, dc.dataSets.toArray(new DataSetContainer[dc.dataSets.size()]));
	}
	
	/**
	 * @param dataContainer
	 */
	private String createHTMLTable(final MatrixQuestionDataContainer dc, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion) {
		StringBuilder rep = new StringBuilder();
		
		if (representationMetadata.useSlideToggleFragment) {
			startSlideToggleContainer(rep, representationMetadata.uuid);
		}
		if (!dc.datasetName.equals(IQuestionView.DEFAULT_DATASETNAME)) {
			rep.append(ReportGeneratorUtils.printAggregationpartContainerTitle(dc.datasetName));
		}
		
		rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
		if (question.getSubquestions().size() > 0) {
			int width = (int) Math.floor((double) 100 / (question.getSubquestions().get(0).getOptions().size() + 1));
			
			// make a column with the optiondisplay names of the
			// subquestions at
			// the first iteration
			List<AbstractQuestion> subquestions = question.getSubquestions();
			rep.append("<tr style=\"background: #eaeaea;\">");
			rep.append("<td style=\"width:" + width + "%;\"></td>");
			for (Option option : subquestions.get(0).getOptions()) {
				rep.append("<td style=\"width:" + width + "%; text-align:center;\">");
				rep.append(WikiUtil.parseToHtml(option.getDisplayName(), true));
				rep.append("</td>");
			}
			rep.append("</tr>");
			
			int subQuestionCount = subquestions.size();
			if (subQuestionCount > 0) {
				int optionCount = subquestions.get(0).getOptions().size();
				
				for (int y = 0; y < subQuestionCount; y++) {
					// find maximum
					int max = -1;
					boolean moreThanOneMaxValue = false;
					for (int x = 0; x < optionCount; x++) {
						int count = dc.valueMap[y][x];
						
						if (count == max) {
							moreThanOneMaxValue = true;
						}
						if (count > max) {
							max = Math.round(count);
						}
					}
					
					rep.append("<tr>");
					rep.append("<td>" + WikiUtil.parseToHtml((subquestions.get(y)).getQuestion(), true) + "</td>");
					for (int x = 0; x < optionCount; x++) {
						int count = dc.valueMap[y][x];
						rep.append("<td class=\"");
						rep.append(count == max ? "maxTrend" : "");
						rep.append((count == max) && moreThanOneMaxValue ? "More" : "");
						rep.append("\" style=\"text-align:center;\">");
						rep.append(getPercentage(count, dc.numberOfResponses, true, "<br />", true));
						rep.append("</td>");
					}
					rep.append("</tr>");
				}
			}
		}
		rep.append("</table>");
		
		if (representationMetadata.targetMedia.equals(TargetMedia.SCREEN)) {
			ReportGeneratorUtils.addDownloadExcelLink(rep, representationMetadata.surveyID, currentQuestion);
		}
		
		if (representationMetadata.useSlideToggleFragment) {
			endSlideToggleFragment(rep);
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
		StringBuilder rep = new StringBuilder();
		
		rep.append("<div class=\"questionText\">" + WikiUtil.parseToHtml(question.getQuestion()) + "</div>");
		rep.append(getAdditionalInfoHtml(question, TargetMedia.PRINTER_QUESTIONNAIRE));
		rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
		boolean titleDrawn = false;
		if (question.getSubquestions().size() > 0) {
			int width = (int) Math.floor((double) 100 / (question.getSubquestions().get(0).getOptions().size() + 1));
			
			for (AbstractQuestion subQuestion : question.getSubquestions()) {
				
				// make a column with the optiondisplay names of the
				// subquestions at
				// the first iteration
				if (!titleDrawn) {
					rep.append("<tr style=\"background: #eaeaea;\">");
					rep.append("<td style=\"width:" + width + "%;\"></td>");
					for (Option option : subQuestion.getOptions()) {
						rep.append("<td style=\"width:" + width + "%; text-align:center;\">");
						rep.append(WikiUtil.parseToHtml(option.getDisplayName(), true));
						rep.append("</td>");
					}
					rep.append("</tr>");
					titleDrawn = true;
				}
				rep.append("<tr>");
				rep.append("<td>" + WikiUtil.parseToHtml(subQuestion.getQuestion(), true) + "</td>");
				for (Option option : subQuestion.getOptions()) {
					rep.append("<td style=\"text-align:center;\">");
					if (result && option.isSelected()) {
						if (subQuestion instanceof RadioQuestion) {
							rep.append("<img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/radio_checked.png\" />");
						} else if (subQuestion instanceof MultipleChoiceQuestion) {
							rep.append("<img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/checkbox_checked.png\" />");
						}
					} else {
						if (subQuestion instanceof RadioQuestion) {
							rep.append("<img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/radio.png\" />");
						} else if (subQuestion instanceof MultipleChoiceQuestion) {
							rep.append("<img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/checkbox.png\" />");
						}
					}
					rep.append("</td>");
				}
				
				rep.append("</tr>");
			}
		}
		rep.append("</table>");
		return rep.toString();
	}
	
	
}
