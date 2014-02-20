package de.cinovo.surveyplatform.ui.views;

import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.model.chart.DataSetContainer;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.IAlignmentQuestion.Alignment;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.RadioQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.reporting.container.MultipleChoiceQuestionChartDataContainer;
import de.cinovo.surveyplatform.reporting.container.MultipleChoiceQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.generator.ReportGeneratorUtils;
import de.cinovo.surveyplatform.util.ExcelUtil;
import de.cinovo.surveyplatform.util.ExcelUtil.CellPointer;
import de.cinovo.surveyplatform.util.WikiUtil;

/**
 *
 * Copyright 2010 Cinovo AG
 *
 * @author yschubert
 *
 */
public class RadioQuestionView extends AbstractQuestionView {
	
	private RadioQuestion question;
	
	public RadioQuestionView(final RadioQuestion question) {
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
		CellPointer p = new CellPointer(sheet);
		p.jumpLastRow();
		writeQuestionToCell(p);
		
		if (dataContainer instanceof MultipleChoiceQuestionDataContainer) {
			MultipleChoiceQuestionDataContainer dc = (MultipleChoiceQuestionDataContainer) dataContainer;
			DataSetContainer valueMap = dc.dataSet;
			
			for (OutputMode mode : OutputMode.values()) {
				Cell c = p.nextRow();
				// c = p.jumpColumn(1);
				if (question.getAlignment() == Alignment.HORIZONTAL) {
					for (String option : valueMap.dataSet.keySet()) {
						c.setCellStyle(ExcelUtil.createBlueStyle(c));
						c.setCellValue(option);
						c = p.nextColumn();
					}
					c = p.nextRow();
					// c = p.jumpColumn(1);
					for (Double count : valueMap.dataSet.values()) {
						if (mode.equals(OutputMode.ABSOLUTE)) {
							c.setCellValue(count);
						} else {
							c.setCellValue((count / dc.numberOfResponses));
							ExcelUtil.createPercentStyle(c);
						}
						c = p.nextColumn();
					}
					c = p.nextRow();
				} else {
					for (Entry<String, Double> entry : valueMap.dataSet.entrySet()) {
						c.setCellStyle(ExcelUtil.createBlueStyle(c));
						c.setCellValue(entry.getKey());
						c = p.nextColumn();
						if (mode.equals(OutputMode.ABSOLUTE)) {
							c.setCellValue(entry.getValue());
						} else {
							c.setCellValue((entry.getValue() / dc.numberOfResponses));
							ExcelUtil.createPercentStyle(c);
						}
						c = p.nextRow();
						// c = p.jumpColumn(1);
					}
				}
				c.setCellValue("n = " + dc.numberOfResponses);
				p.jumpRow(2);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.cinovo.surveyplatform.ui.views.AbstractQuestionView#getHTMLRepresentation
	 * ()
	 */
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		rep.append(getQuestionBlock(question, targetMedia));
		rep.append(getAdditionalInfoHtml(question, targetMedia));
		if (question.getAlignment() == Alignment.HORIZONTAL) {
			rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
			int width = (int) Math.floor((double) 100 / (question.getOptions().size()));
			
			rep.append("<tr style=\"background: #eaeaea;\">");
			for (Option option : question.getOptions()) {
				rep.append("<td style=\"text-align:center; width:" + width + "%;\">");
				rep.append(WikiUtil.parseToHtml(option.getDisplayName(), true));
				rep.append("</td>");
			}
			rep.append("</tr>");
			rep.append("<tr>");
			for (Option option : question.getOptions()) {
				rep.append("<td onclick=\"$(this).children('input[type=radio]').attr('checked', 'checked'); " + getOptionScriptWithoutEventListener(option) + "\" style=\"text-align:center; cursor:pointer;\">");
				rep.append("<input style=\"margin: 3px;\"type=\"radio\" name=\"q" + question.getId() + "\" value=\"" + option.getId() + "\"" + (option.isSelected() ? "checked=\"checked\"" : "") + " " + getOptionScript(option) + " />");
				rep.append("</td>");
			}
			rep.append("</tr>");
			rep.append("</table>");
		} else if (question.getAlignment() == Alignment.VERTICAL) {
			rep.append("<ul>");
			for (Option option : question.getOptions()) {
				rep.append("<li style=\"margin: 8px 0px 3px 0px;\">");
				rep.append("<input style=\"margin: 0px 5px 0px 5px; float:left; vertical-align: middle;\" type=\"radio\" name=\"q" + question.getId() + "\" value=\"" + option.getId() + "\"" + (option.isSelected() ? "checked=\"checked\"" : "") + " " + getOptionScript(option) + " /><span style=\"margin: 0 0 0 25px;\" class=\"questionItemIEFIX\">" + WikiUtil.parseToHtml(option.getDisplayName(), true) + "</span><div style=\"clear: both;\"></div>");
				rep.append("</li>");
			}
			rep.append("</ul>");
		}
		
		// this hidden field is necessarry to always make this
		// question present in the request parameter (as it would
		// not be, if no option of the question is selected)
		rep.append("<input type=\"hidden\" name=\"q" + question.getId() + "\" value=\"-1\" />");
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
		if (question.getAlignment() == Alignment.HORIZONTAL) {
			rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
			int width = (int) Math.floor((double) 100 / (question.getOptions().size()));
			
			rep.append("<tr style=\"background: #eaeaea;\">");
			for (Option option : question.getOptions()) {
				rep.append("<td style=\"text-align:center; width:" + width + "%;\">");
				rep.append(WikiUtil.parseToHtml(option.getDisplayName(), true));
				rep.append("</td>");
			}
			rep.append("</tr>");
			rep.append("<tr>");
			for (Option option : question.getOptions()) {
				rep.append("<td style=\"text-align:center;\">");
				if (result && option.isSelected()) {
					rep.append("<img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/radio_checked.png\" />");
				} else {
					rep.append("<img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/radio.png\" />");
				}
				rep.append("</td>");
			}
			rep.append("</tr>");
			rep.append("</table>");
		} else if (question.getAlignment() == Alignment.VERTICAL) {
			rep.append("<ul>");
			for (Option option : question.getOptions()) {
				rep.append("<li style=\"list-style: none;\">");
				if (result && option.isSelected()) {
					rep.append("<div style=\"float: left;\"><img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/radio_checked.png\" /></div><span style=\"display: block; margin-left: 25px;\">" + WikiUtil.parseToHtml(option.getDisplayName(), true) + "</span>");
				} else {
					rep.append("<div style=\"float: left;\"><img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/radio.png\" /></div><span style=\"display: block; margin-left: 25px;\">" + WikiUtil.parseToHtml(option.getDisplayName(), true) + "</span>");
				}
				rep.append("</li>");
			}
			rep.append("</ul>");
		}
		return rep.toString();
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
		if (dataContainer instanceof MultipleChoiceQuestionChartDataContainer) {
			return createChart((MultipleChoiceQuestionChartDataContainer) dataContainer, representationMetadata, currentQuestion);
		} else if (dataContainer instanceof MultipleChoiceQuestionDataContainer) {
			return createHTMLTable((MultipleChoiceQuestionDataContainer) dataContainer, representationMetadata, currentQuestion);
		}
		return "Don't know how to handle: " + dataContainer.getClass().getName();
	}
	
	private String createHTMLTable(final MultipleChoiceQuestionDataContainer dc, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion) {
		StringBuilder rep = new StringBuilder();
		DataSetContainer valueMap = dc.dataSet;
		// find maximum
		int max = -1;
		boolean moreThanOneMaxValue = false;
		for (Double count : valueMap.dataSet.values()) {
			if (count == max) {
				moreThanOneMaxValue = true;
			}
			if (count > max) {
				max = (int) Math.round(count);
			}
		}
		if (representationMetadata.useSlideToggleFragment) {
			startSlideToggleContainer(rep, representationMetadata.uuid);
		}
		if (!valueMap.name.equals(IQuestionView.DEFAULT_DATASETNAME)) {
			rep.append(ReportGeneratorUtils.printAggregationpartContainerTitle(valueMap.name));
		}
		if (question.getAlignment() == Alignment.HORIZONTAL) {
			rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
			int width = (int) Math.floor((double) 100 / (question.getOptions().size()));
			rep.append("<tr>");
			for (String option : valueMap.dataSet.keySet()) {
				rep.append("<td style=\"text-align:center; width:" + width + "%;\">");
				rep.append(WikiUtil.parseToHtml(option, true));
				rep.append("</td>");
			}
			rep.append("</tr>");
			rep.append("<tr>");
			for (Double count : valueMap.dataSet.values()) {
				int value = (int) Math.round(count);
				rep.append("<td class=\"");
				rep.append(value == max ? "maxTrend" : "");
				rep.append((value == max) && moreThanOneMaxValue ? "More" : "");
				rep.append("\" style=\"text-align:center;\">");
				rep.append(getPercentage(count, dc.numberOfResponses));
				rep.append("</td>");
			}
			rep.append("</tr>");
			rep.append("</table>");
		} else if (question.getAlignment() == Alignment.VERTICAL) {
			rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
			rep.append("<tr>");
			rep.append("<td>Option</td><td style=\"width: 90px;\">Count</td>");
			rep.append("</tr>");
			for (Entry<String, Double> entry : valueMap.dataSet.entrySet()) {
				int value = (int) Math.round(entry.getValue());
				rep.append("<tr>");
				rep.append("<td>");
				rep.append(WikiUtil.parseToHtml(entry.getKey(), true));
				rep.append("</td>");
				rep.append("<td class=\"");
				rep.append(value == max ? "maxTrend" : "");
				rep.append((value == max) && moreThanOneMaxValue ? "More" : "");
				rep.append("\">");
				rep.append(getPercentage(entry.getValue(), dc.numberOfResponses));
				rep.append("</td>");
				rep.append("</tr>");
			}
			rep.append("</table>");
		}
		if (representationMetadata.targetMedia.equals(TargetMedia.SCREEN)) {
			ReportGeneratorUtils.addDownloadExcelLink(rep, representationMetadata.surveyID, currentQuestion);
		}
		
		if (representationMetadata.useSlideToggleFragment) {
			endSlideToggleFragment(rep);
		}
		return rep.toString();
	}
	
	/**
	 * @param dataContainer
	 * @return
	 */
	private String createChart(final MultipleChoiceQuestionChartDataContainer dc, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion) {
		return getDefaultChart(dc.filePath, representationMetadata.surveyID, dc.numberOfResponses, representationMetadata, currentQuestion, dc.dataSets.toArray(new DataSetContainer[dc.dataSets.size()]));
	}
}