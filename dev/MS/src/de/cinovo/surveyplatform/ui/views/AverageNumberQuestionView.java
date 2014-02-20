/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.AverageNumberQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.util.ExcelUtil;
import de.cinovo.surveyplatform.util.ExcelUtil.CellPointer;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.WikiUtil;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author ablehm
 *
 */
public class AverageNumberQuestionView extends AbstractQuestionView {
	
	public static final String CONTROLNAME = "averageNumber";
	
	
	/**
	 *
	 */
	public AverageNumberQuestionView(final AverageNumberQuestion question) {
		super(question);
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
		rep.append("<p>");
		rep.append(getQuestionInline(question, targetMedia));
		rep.append("<input type=\"text\" name=\"" + CONTROLNAME + "");
		rep.append(question.getId());
		rep.append("\" style=\"width: 40%;\" value=\"");
		Answer answerObj = ((AverageNumberQuestion) question).getAnswerObj();
		if (answerObj == null) {
			rep.append("");
		} else {
			rep.append(HtmlFormUtil.escapeHtmlFull(answerObj.getAnswer()));
		}
		rep.append("\" />");
		String hint = ((AverageNumberQuestion) question).getHint();
		if ((hint != null) && !hint.isEmpty()) {
			rep.append("<span style=\"font-style: italic;margin: 0px 0px 0px 5px;\">(");
			rep.append(hint);
			rep.append(")</span>");
		}
		rep.append("</p>");
		rep.append(getAdditionalInfoHtml(question, targetMedia));
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
		c.setCellValue(question.getQuestion());
		c = p.nextRow();
		if (dataContainer instanceof AverageNumberQuestionDataContainer) {
			AverageNumberQuestionDataContainer dc = (AverageNumberQuestionDataContainer) dataContainer;
			List<String> answerList = dc.answers;
			// c = p.jumpColumn(1);
			c.setCellStyle(ExcelUtil.createBlueStyle(c));
			c.setCellValue("Average");
			p.nextColumn();
			c.setCellValue(calculateAverageNumber(answerList, dc));
			ExcelUtil.createPercentStyle(c);
			c = p.nextRow();
			// c = p.jumpColumn(1);
			c.setCellValue("n = " + dc.numberOfResponses);
		}
		c = p.nextRow();
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
		if (dataContainer instanceof AverageNumberQuestionDataContainer) {
			List<String> answerList = ((AverageNumberQuestionDataContainer) dataContainer).answers;
			double average = calculateAverageNumber(answerList, ((AverageNumberQuestionDataContainer) dataContainer));
			((AverageNumberQuestionDataContainer) dataContainer).answers.clear();
			((AverageNumberQuestionDataContainer) dataContainer).answers.add(String.valueOf(average));
			return createHTML((AverageNumberQuestionDataContainer) dataContainer, representationMetadata);
		} else {
			return "Don't know how to handle: " + dataContainer.getClass().getName();
		}
	}
	
	/**
	 * Calculates the average Number. Is also used for test cases.
	 * 
	 * @param answerList - the list of all given answers in a double form to the avrg number question
	 */
	private double calculateAverageNumber(final List<String> answerList, final AverageNumberQuestionDataContainer dc) {
		double average = 0.00;
		int numOfSubmissions = 0;
		double sumOfSubmissions = 0;
		for (String currentAnswer : answerList) {
			if (!currentAnswer.isEmpty()) {
				try {
					sumOfSubmissions += Double.parseDouble(currentAnswer);
					numOfSubmissions++;
				} catch (NumberFormatException nfe) {
					// ignore that answer
				}
			}
		}
		
		average = sumOfSubmissions / numOfSubmissions;
		average = (double) Math.round(average * 100) / 100;
		dc.numberOfResponses = numOfSubmissions;
		return average;
	}
	
	private String createHTML(final AverageNumberQuestionDataContainer dc, final RepresentationMetadata representationMetadata) {
		StringBuilder rep = new StringBuilder();
		
		FreeTextFilterUtil u = new FreeTextFilterUtil(question.getId());
		rep.append("<div style=\"padding: 10px;\">");
		
		for (String answer : dc.answers) {
			if ((answer != null) && !answer.isEmpty()) {
				rep.append("<div class=\"answerContainerContainer\"><div class=\"answerContainer\" data-datamapster=\"" + u.getId() + "\">");
				rep.append("<p id=\"q" + question.getId() + "a0" + "\" class='answer answer" + u.getId() + " " + u.getMd5FromAnswer(answer) + "'>Average: " + HtmlFormUtil.escapeHtmlFull(answer) + "</p>");
				rep.append("</div></div>");
				break;
			}
		}
		rep.append("</div>");
		
		return rep.toString();
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
		String formattedQuestionText = WikiUtil.parseToHtml(question.getQuestion());
		
		rep.append("<p><span class=\"questionText\" style=\"margin: 0px 5px 0px 0px;\">");
		rep.append(formattedQuestionText.substring(3, formattedQuestionText.length() - 4));
		rep.append("</span>");
		if (result) {
			Answer answerObj = ((AverageNumberQuestion) question).getAnswerObj();
			rep.append(answerObj == null ? "" : answerObj.getAnswer());
		} else {
			rep.append("<span>_________________________________________________</span>");
		}
		
		String hint = ((AverageNumberQuestion) question).getHint();
		if ((hint != null) && !hint.isEmpty()) {
			rep.append("<span style=\"font-style: italic;margin: 0px 0px 0px 5px;\">(");
			rep.append(hint);
			rep.append(")</span>");
		}
		rep.append("</p>");
		
		rep.append(getAdditionalInfoHtml(question, TargetMedia.PRINTER_QUESTIONNAIRE));
		return rep.toString();
	}
}
