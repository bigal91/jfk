/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.reporting.container.TextfieldQuestionDataContainer;
import de.cinovo.surveyplatform.util.ExcelUtil;
import de.cinovo.surveyplatform.util.ExcelUtil.CellPointer;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.WikiUtil;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class TextfieldQuestionView extends AbstractQuestionView {
	
	private static final int MINIMUM_FIELD_COUNT = 3;
	
	private TextfieldQuestion question;
	
	
	public TextfieldQuestionView(final TextfieldQuestion question) {
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
		Cell c = p.jumpLastRow();
		writeQuestionToCell(p);
		
		if (dataContainer instanceof TextfieldQuestionDataContainer) {
			TextfieldQuestionDataContainer dc = (TextfieldQuestionDataContainer) dataContainer;
			c = p.nextRow();
			// c = p.jumpColumn(1);
			for (int i = 0; i < dc.sortedAnswers.size(); i++) {
				c.setCellStyle(ExcelUtil.createBlueStyle(c));
				c.setCellValue("Answer " + (i + 1));
				c = p.nextColumn();
			}
			c = p.nextRow();
			// c = p.jumpColumn(1);
			int matrixHeight = dc.sortedAnswers.get(0).size();
			int matrixWidth = dc.sortedAnswers.size();
			
			for (int y = 0; y < matrixHeight; y++) {
				boolean hasAnswer = false;
				for (int x = 0; x < matrixWidth; x++) {
					Answer answerObj = dc.sortedAnswers.get(x).get(y);
					String answer = answerObj.getAnswer();
					if (!answer.trim().isEmpty()) {
						hasAnswer = true;
						c.setCellValue(answer);
					}
					c = p.nextColumn();
				}
				if (hasAnswer) {
					c = p.nextRow();
					// c = p.jumpColumn(1);
				}
			}
			c.setCellValue("n = " + matrixHeight);
			c = p.nextRow();
			
		}
	}
	
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		
		List<Answer> answers = question.getAnswer();
		rep.append(getQuestionBlock(question, targetMedia));
		rep.append(getAdditionalInfoHtml(question, targetMedia));
		int fieldCount = MINIMUM_FIELD_COUNT;
		if (answers.size() > fieldCount) {
			fieldCount = answers.size();
		}
		for (int i = 0; i < fieldCount; i++) {
			rep.append("<p>" + (i + 1) + ". <input type=\"text\" name=\"text" + question.getId() + "[]\" value=\"" + (answers.size() > i ? answers.get(i).getAnswer() : "") + "\" /></p>");
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
		
		List<Answer> answers = question.getAnswer();
		
		rep.append("<div class=\"questionText\">" + WikiUtil.parseToHtml(question.getQuestion()) + "</div>");
		rep.append(getAdditionalInfoHtml(question, TargetMedia.PRINTER_QUESTIONNAIRE));
		
		int fieldCount = MINIMUM_FIELD_COUNT;
		if (answers.size() > fieldCount) {
			fieldCount = answers.size();
		}
		for (int i = 0; i < fieldCount; i++) {
			rep.append("<p>" + (i + 1) + ". " + ((answers.size() > i) && result ? answers.get(i).getAnswer() : "") + "</p>");
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
		if (dataContainer instanceof TextfieldQuestionDataContainer) {
			return createHTMLTable((TextfieldQuestionDataContainer) dataContainer, representationMetadata);
		} else {
			return "Don't know how to handle: " + dataContainer.getClass().getName();
		}
	}
	
	/**
	 * @param dataContainer
	 * @return
	 */
	private String createHTMLTable(final TextfieldQuestionDataContainer dc, final RepresentationMetadata representationMetadata) {
		StringBuilder rep = new StringBuilder();
		
		FreeTextFilterUtil u = new FreeTextFilterUtil(question.getId());
		AnswerContainer answerContainer = new AnswerContainer();
		rep.append("<br />");
		int matrixWidth = dc.sortedAnswers.size();
		int index = 0;
		if (matrixWidth == 0) {
			rep.append("No answers given");
		} else {
			
			rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
			rep.append("<tr style=\"background: #eaeaea;\">");
			for (int i = 0; i < dc.sortedAnswers.size(); i++) {
				rep.append("<td>");
				rep.append("<p style=\"text-align: center;\">Answer " + (i + 1) + "</p>");
				rep.append("</td>");
			}
			rep.append("</tr>");
			int matrixHeight = dc.sortedAnswers.get(0).size();
			for (int y = 0; y < matrixHeight; y++) {
				StringBuilder row = new StringBuilder();
				row.append("<tr>");
				boolean rowHasAnwser = false;
				for (int x = 0; x < matrixWidth; x++) {
					Answer answer = dc.sortedAnswers.get(x).get(y);
					if (!answer.getAnswer().trim().equals("")) {
						rowHasAnwser = true;
					}
					answerContainer.addAnswer(answer);
					row.append("<td style=\"vertical-align: top; padding: 0; width:" + ((100 / matrixWidth) + "%;\" class=\"answerContainerContainer\">"));
					row.append("<div style=\"vertical-align:top;\" class=\"answerContainer noborder\" data-datamapster=\"" + u.getId() + "\">");
					row.append("<p id=\"q" + question.getId() + "a" + index + "\" data-answerid=\"" + answer.getId() + "\" class=\"id" + answer.getId() + " answer answer" + u.getId() + " " + u.getMd5FromAnswer(answer.getAnswer()) + "\">" + HtmlFormUtil.escapeHtmlFull(answer.getAnswer()) + "</p>");
					row.append("</div>");
					row.append("</td>");
					index++;
				}
				row.append("</tr>");
				if (rowHasAnwser) {
					rep.append(row);
				}
			}
		}
		rep.append("</table>");
		if (representationMetadata.targetMedia.equals(TargetMedia.SCREEN)) {
			u.setTopics(dc.topics);
			u.setAnswers(answerContainer);
			rep.insert(0, u.getFilterBox(index));
		}
		
		return rep.toString();
		
	}
}
