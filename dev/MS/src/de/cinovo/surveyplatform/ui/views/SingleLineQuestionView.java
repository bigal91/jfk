/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.reporting.container.SingleLineQuestionDataContainer;
import de.cinovo.surveyplatform.util.ExcelUtil.CellPointer;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.WikiUtil;



/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class SingleLineQuestionView extends AbstractQuestionView {
	
	public static final String CONTROLNAME = "singleLine";
	
	
	/**
	 *
	 */
	public SingleLineQuestionView(final SingleLineQuestion question) {
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
		Cell c = p.jumpLastRow();
		writeQuestionToCell(p);
		
		if (dataContainer instanceof SingleLineQuestionDataContainer) {
			SingleLineQuestionDataContainer dc = (SingleLineQuestionDataContainer) dataContainer;
			Collections.sort(dc.answers);
			c = p.nextRow();
			// c = p.jumpColumn(1);
			for (Answer answerObj : dc.answers) {
				final String answer = answerObj.getAnswer();
				if (!answer.isEmpty()) {
					c.setCellValue(answer);
					c = p.nextRow();
					// c = p.jumpColumn(1);
				}
			}
			c.setCellValue("n = " + dc.answers.size());
			c = p.nextRow();
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
		rep.append(getAdditionalInfoHtml(question, targetMedia));
		rep.append("<p>");
		rep.append(getQuestionInline(question, targetMedia));
		rep.append("<input type=\"text\" name=\"" + CONTROLNAME + "");
		rep.append(question.getId());
		rep.append("\" style=\"width: 40%;\" value=\"");
		List<Answer> answers = ((SingleLineQuestion) question).getAnswer();
		if (answers.size() > 0) {
			rep.append(HtmlFormUtil.escapeHtmlFull(((SingleLineQuestion) question).getAnswer().get(0).getAnswer()));
		}
		String hint = ((SingleLineQuestion) question).getHint();
		rep.append("\" />");
		if ((hint != null) && !hint.isEmpty()) {
			rep.append("<span style=\"font-style: italic;margin: 0px 0px 0px 5px;\">(");
			rep.append(hint);
			rep.append(")</span>");
		}
		rep.append("</p>");
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
		if (dataContainer instanceof SingleLineQuestionDataContainer) {
			return createHTMLTable((SingleLineQuestionDataContainer) dataContainer, representationMetadata);
		} else {
			return "Don't know how to handle: " + dataContainer.getClass().getName();
		}
	}
	
	/**
	 * @param dataContainer
	 * @return
	 */
	private String createHTMLTable(final SingleLineQuestionDataContainer dc, final RepresentationMetadata representationMetadata) {
		StringBuilder rep = new StringBuilder();
		
		FreeTextFilterUtil u = new FreeTextFilterUtil(question.getId());
		
		// rep.append("<div style=\"padding: 10px; border: 1px #D5D5D5 solid;\">");
		rep.append("<div style=\"padding: 10px;\">");
		
		int index = 0;
		
		Collections.sort(dc.answers);
		
		for (Answer answerObj : dc.answers) {
			final String answer = answerObj.getAnswer();
			if (!answer.isEmpty()) {
				rep.append("<div class=\"answerContainerContainer\"><div class=\"answerContainer\" data-datamapster=\"" + u.getId() + "\">");
				rep.append("<p id=\"q" + question.getId() + "a" + index + "\" data-answerid=\"" + answerObj.getId() + "\" class=\"id" + answerObj.getId() + " answer answer" + u.getId() + " " + u.getMd5FromAnswer(answer) + "\">" + HtmlFormUtil.escapeHtmlFull(answer) + "</p>");
				rep.append("</div></div>");
				index++;
			}
		}
		rep.append("</div>");
		
		if (representationMetadata.targetMedia.equals(TargetMedia.SCREEN)) {
			u.setTopics(dc.topics);
			u.setAnswers(new AnswerContainer(dc.answers));
			rep.insert(0, u.getFilterBox(dc.answers.size()));
		}
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
			Answer answerObj = ((SingleLineQuestion) question).getAnswerObj();
			if (answerObj != null) {
				rep.append(answerObj.getAnswer());
			}
		} else {
			rep.append("<span>_________________________________________________</span>");
		}
		String hint = ((SingleLineQuestion) question).getHint();
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
