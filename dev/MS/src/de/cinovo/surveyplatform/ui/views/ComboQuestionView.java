/**
 *
 */
package de.cinovo.surveyplatform.ui.views;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement;
import de.cinovo.surveyplatform.model.chart.DataSetContainer;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.ComboQuestion;
import de.cinovo.surveyplatform.model.question.Option;
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
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class ComboQuestionView extends AbstractQuestionView {
	
	private ComboQuestion question;
	
	
	public ComboQuestionView(final ComboQuestion question) {
		super(question);
		this.question = question;
	}
	
	/* (non-Javadoc)
	 * @see de.cinovo.surveyplatform.ui.views.IQuestionView#getHTMLRepresentation()
	 */
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		rep.append(getQuestionBlock(question, targetMedia));
		rep.append(getAdditionalInfoHtml(question, targetMedia));
		
		String actionScript = getOnChangeScriptForComboBox();
		
		rep.append("<select onchange=\"" + actionScript + "\" input type=\"text\" name=\"combo" + question.getId() + "\">");
		rep.append("<option value=\"-1\">- please select one option -</option>");
		for (Option option : question.getOptions()) {
			rep.append("<option value=\"" + option.getId() + "\" style=\"padding: 2px;\" " + (option.isSelected() ? "selected=\"selected\"" : "") + " >" + WikiUtil.parseToHtml(option.getDisplayName(), true) + "</option>");
		}
		rep.append("</select>");
		return rep.toString();
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
		Cell c = p.jumpLastRow();
		writeQuestionToCell(p);
		if (dataContainer instanceof MultipleChoiceQuestionDataContainer) {
			MultipleChoiceQuestionDataContainer dc = (MultipleChoiceQuestionDataContainer) dataContainer;
			DataSetContainer valueMap = dc.dataSet;
			for (OutputMode mode : OutputMode.values()) {
				for (Entry<String, Double> entry : valueMap.dataSet.entrySet()) {
					c = p.nextRow();
					// c = p.jumpColumn(1);
					c.setCellValue(entry.getKey());
					c = p.nextColumn();
					if (mode.equals(OutputMode.ABSOLUTE)) {
						c.setCellValue(entry.getValue());
					} else {
						c.setCellValue(entry.getValue() / dc.numberOfResponses);
						ExcelUtil.createPercentStyle(c);
					}
				}
				c = p.nextRow();
				// c = p.jumpColumn(1);
				c.setCellValue("n = " + dc.numberOfResponses);
				c = p.jumpRow(2);
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
		
		if (dataContainer instanceof MultipleChoiceQuestionChartDataContainer) {
			MultipleChoiceQuestionChartDataContainer dc = (MultipleChoiceQuestionChartDataContainer) dataContainer;
			return getDefaultChart(dc.filePath, representationMetadata.surveyID, dc.numberOfResponses, representationMetadata, currentQuestion, dc.dataSets.toArray(new DataSetContainer[dc.dataSets.size()]));
		} else if (dataContainer instanceof MultipleChoiceQuestionDataContainer) {
			MultipleChoiceQuestionDataContainer dc = (MultipleChoiceQuestionDataContainer) dataContainer;
			
			StringBuilder rep = new StringBuilder();
			DataSetContainer valueMap = dc.dataSet;
			if (representationMetadata.useSlideToggleFragment) {
				startSlideToggleContainer(rep, representationMetadata.uuid);
			}
			
			if (!valueMap.name.equals(IQuestionView.DEFAULT_DATASETNAME)) {
				rep.append(ReportGeneratorUtils.printAggregationpartContainerTitle(valueMap.name));
			}
			
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
			
			rep.append("<table class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
			rep.append("<tr style=\"background: #eaeaea;\">");
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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.ui.views.AbstractQuestionView#
	 * getPrintableRepresentation(boolean)
	 */
	@Override
	public String getPrintableRepresentation(final boolean result) {
		StringBuilder rep = new StringBuilder();
		rep.append("<div class=\"questionText\">" + WikiUtil.parseToHtml(question.getQuestion()) + "</div>");
		rep.append(getAdditionalInfoHtml(question, TargetMedia.PRINTER_QUESTIONNAIRE));
		
		if (result) {
			for (Option option : question.getOptions()) {
				if (option.isSelected()) {
					rep.append("Answer: " + WikiUtil.parseToHtml(option.getDisplayName(), true));
					break;
				}
			}
		} else {
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
	
	/**
	 * This method returns the OnChangeActionScript for the ComboBox
	 * 
	 * @return ActionScript
	 */
	protected String getOnChangeScriptForComboBox() {
		
		// This String will be returned
		String actionScriptString = "";
		
		// All LogicElements of the Question
		List<QuestionnaireLogicElement> questionLogicElements = question.getLogicElements();
		
		// This map contains for every answer the affected LogicElements
		HashMap<String, List<QuestionnaireLogicElement>> answersWithAssociatedLogicElements = new HashMap<String, List<QuestionnaireLogicElement>>();
		
		// Create the answersWithAssociatedLogicElements List: Iterate over all
		// LogicElements of the question and map their "ActivationAnswers" to
		// the respective LogicElement
		for (QuestionnaireLogicElement logicElement : questionLogicElements) {
			
			// Iterate over the "ActivationAnswers" of the LogicElement
			for (Answer answer : logicElement.getAnswers()) {
				
				// Put every LogicElement to the corresponding
				// "ActivationAnswer" in the
				// answersWithAssociatedLogicElements-Map
				if (answersWithAssociatedLogicElements.containsKey(answer.getAnswer())) {
					answersWithAssociatedLogicElements.get(answer.getAnswer()).add(logicElement);
				} else {
					
					List<QuestionnaireLogicElement> newLogicElementsList = new ArrayList<QuestionnaireLogicElement>();
					newLogicElementsList.add(logicElement);
					answersWithAssociatedLogicElements.put(answer.getAnswer(), newLogicElementsList);
					
				}
			}
		}
		
		// Get all PartIds of Parts affected by LogicElements
		List<Integer> affectedPartIdsOfAllLogicElementsOfTheQuestion = new ArrayList<Integer>();
		List<String> affectedTypes = new ArrayList<String>();
		{
			for (QuestionnaireLogicElement logicElement : question.getLogicElements()) {
				affectedPartIdsOfAllLogicElementsOfTheQuestion.add(logicElement.getIdOfPart());
				int lastIndexOfDot = logicElement.getTypeOfPart().lastIndexOf(".");
				affectedTypes.add(logicElement.getTypeOfPart().substring(lastIndexOfDot + 1).toLowerCase());
			}
			
		}
		
		// Iterate over all options of the questions
		for (Option option : question.getOptions()) {
			
			String displayText = option.getDisplayName();
			
			actionScriptString += "if (this.options[this.selectedIndex].text == '" + displayText + "') {";
			
			// Get all slideDownElementIds for the current option
			List<Integer> slideDownElementIds = new ArrayList<Integer>();
			List<String> slideDownElementTypes = new ArrayList<String>();
			{
				List<QuestionnaireLogicElement> slideDownElements = answersWithAssociatedLogicElements.get(displayText);
				if ((slideDownElements != null) && !slideDownElements.isEmpty()) {
					for (QuestionnaireLogicElement logicElement : slideDownElements) {
						slideDownElementIds.add(logicElement.getIdOfPart());
						int lastIndexOfDot = logicElement.getTypeOfPart().lastIndexOf(".");
						slideDownElementTypes.add(logicElement.getTypeOfPart().substring(lastIndexOfDot + 1).toLowerCase());
					}
				}
			}
			
			// SlideUpElementIds contains all elementIds associated with ANY
			// LogicElement of the question WITHOUT the SlideDownElements of
			// the current option
			List<Integer> slideUpElementIds = new ArrayList<Integer>(affectedPartIdsOfAllLogicElementsOfTheQuestion);
			slideUpElementIds.removeAll(slideDownElementIds);
			
			for (int i = 0; i < slideDownElementIds.size(); i++) {
				actionScriptString += "$('.lid" + slideDownElementTypes.get(i).toLowerCase() + slideDownElementIds.get(i) + "').slideDown();";
			}
			
			for (int i = 0; i < slideUpElementIds.size(); i++) {
				actionScriptString += "$('.lid" + affectedTypes.get(i).toLowerCase() + slideUpElementIds.get(i) + "').slideUp();";
			}
			
			actionScriptString += "};";
			
		}
		
		return actionScriptString;
	}
}
