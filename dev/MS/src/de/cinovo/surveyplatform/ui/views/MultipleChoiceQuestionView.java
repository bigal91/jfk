package de.cinovo.surveyplatform.ui.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.chart.DataSetContainer;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.IAlignmentQuestion.Alignment;
import de.cinovo.surveyplatform.model.question.MultipleChoiceQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.reporting.container.MultipleChoiceQuestionChartDataContainer;
import de.cinovo.surveyplatform.reporting.container.MultipleChoiceQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.generator.ReportGeneratorUtils;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.ExcelUtil;
import de.cinovo.surveyplatform.util.ExcelUtil.CellPointer;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.WikiUtil;

/**
 *
 * Copyright 2010 Cinovo AG
 *
 * @author yschubert
 *
 */
public class MultipleChoiceQuestionView extends AbstractQuestionView {
	
	private MultipleChoiceQuestion question;
	
	
	public MultipleChoiceQuestionView(final MultipleChoiceQuestion question) {
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
							c.setCellValue(count / dc.numberOfResponses);
							ExcelUtil.createPercentStyle(c);
						}
						c = p.nextColumn();
					}
					c = p.nextRow();
					// c = p.jumpColumn(1);
				} else {
					for (Entry<String, Double> entry : valueMap.dataSet.entrySet()) {
						c.setCellStyle(ExcelUtil.createBlueStyle(c));
						c.setCellValue(entry.getKey());
						c = p.nextColumn();
						if (mode.equals(OutputMode.ABSOLUTE)) {
							c.setCellValue(entry.getValue());
						} else {
							c.setCellValue(entry.getValue() / dc.numberOfResponses);
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
	
	
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		rep.append(getQuestionBlock(question, targetMedia));
		rep.append(getAdditionalInfoHtml(question, targetMedia));
		if (question.getAlignment() == Alignment.HORIZONTAL) {
			rep.append("<table class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
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
				String displayNameHash = AuthUtil.md5(option.getDisplayName());
				
				rep.append("<td class=\"checkboxCell" + question.getId() + "\" style=\"text-align:center; cursor:pointer;\">");
				rep.append("<input class=\"checkbox" + question.getId() + " " + displayNameHash + "\" style=\"margin: 3px;\" type=\"checkbox\" name=\"q" + question.getId() + "[]\" value=\"" + option.getId() + "\"" + (option.isSelected() ? "checked=\"checked\"" : "") + " " + getOptionScript(option) + " />");
				rep.append("</td>");
			}
			rep.append("</tr>");
			rep.append("</table>");
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("QUESTIONID", question.getId() + "");
			rep.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/checkBoxCellClickScript.html", replacements));
			
		} else if (question.getAlignment() == Alignment.VERTICAL) {
			rep.append("<ul>");
			for (Option option : question.getOptions()) {
				String displayNameHash = AuthUtil.md5(option.getDisplayName());
				rep.append("<li style=\"margin: 8px 0px 3px 0px;\">");
				rep.append("<input class=\"checkbox" + question.getId() + " " + displayNameHash + "\" style=\"margin: 0px 5px 0px 5px; float:left; vertical-align: middle;\" type=\"checkbox\" name=\"q" + question.getId() + "[]\" value=\"" + option.getId() + "\"" + (option.isSelected() ? "checked=\"checked\"" : "") + " " + getOptionScript(option) + " /><span style=\"margin: 0 0 0 25px;\" class=\"questionItemIEFIX\">" + WikiUtil.parseToHtml(option.getDisplayName(), true) + "</span><div style=\"clear: both;\"></div>");
				rep.append("</li>");
			}
			rep.append("</ul>");
		}
		// this hidden field is necessary to always make this
		// question present in the request parameter (as it would
		// not be, if no option of the question is selected)
		rep.append("<input type=\"hidden\" name=\"q" + question.getId() + "[]\" value=\"-1\" />");
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
			rep.append("<table class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
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
					rep.append("<img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/checkbox_checked.png\" />");
				} else {
					rep.append("<img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/checkbox.png\" />");
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
					rep.append("<div style=\"float: left;\"><img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/checkbox_checked.png\" /></div><span style=\"display: block; margin-left: 25px;\">" + WikiUtil.parseToHtml(option.getDisplayName(), true) + "</span>");
				} else {
					rep.append("<div style=\"float: left;\"><img src=\"" + EnvironmentConfiguration.getUrlBase() + "/gfx/icons/checkbox.png\" /></div><span style=\"display: block; margin-left: 25px;\">" + WikiUtil.parseToHtml(option.getDisplayName(), true) + "</span>");
				}
				
				rep.append("</li>");
			}
			rep.append("</ul>");
		}
		return rep.toString();
	}
	
	/**
	 *
	 */
	@Override
	public String getAggregatedPrintableRepresentation(final IReportDataContainer dataContainer, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion) {
		
		if (dataContainer instanceof MultipleChoiceQuestionChartDataContainer) {
			MultipleChoiceQuestionChartDataContainer dc = (MultipleChoiceQuestionChartDataContainer) dataContainer;
			return getDefaultChart(dc.filePath, representationMetadata.surveyID, dc.numberOfResponses, representationMetadata, currentQuestion, dc.dataSets.toArray(new DataSetContainer[dc.dataSets.size()]));
		} else if (dataContainer instanceof MultipleChoiceQuestionDataContainer) {
			MultipleChoiceQuestionDataContainer dc = (MultipleChoiceQuestionDataContainer) dataContainer;
			StringBuilder rep = new StringBuilder();
			DataSetContainer valueMap = dc.dataSet;
			
			// find maximum
			int max = -1;
			boolean moreThanOneMaxValue = false;
			for (Double count : valueMap.dataSet.values()) {
				if ((int) Math.round(count) == max) {
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
				rep.append("<table class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
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
				rep.append("<table class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
				rep.append("<tr>");
				rep.append("<td>Option</td><td style=\"width: 90px;\">Count</td>");
				rep.append("</tr>");
				
				for (Entry<String, Double> entry : valueMap.dataSet.entrySet()) {
					rep.append("<tr>");
					rep.append("<td>");
					rep.append(WikiUtil.parseToHtml(entry.getKey(), true));
					rep.append("</td>");
					int value = (int) Math.round(entry.getValue());
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
		
		return "Don't know how to handle: " + dataContainer.getClass().getName();
		
	}
}