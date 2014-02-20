package de.cinovo.surveyplatform.ui.views;

import java.util.Collections;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.FreeTextQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.util.ExcelUtil.CellPointer;
import de.cinovo.surveyplatform.util.HtmlFormUtil;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class FreeTextQuestionView extends AbstractQuestionView {
	
	private FreeTextQuestion question;
	
	
	public FreeTextQuestionView(final FreeTextQuestion question) {
		super(question);
		this.question = question;
	}
	
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		rep.append(getQuestionBlock(question, targetMedia));
		rep.append(getAdditionalInfoHtml(question, targetMedia));
		Answer answerObj = question.getAnswerObj();
		rep.append("<p><textarea name=\"text" + question.getId() + "\">" + (answerObj != null ? answerObj.getAnswer() : "") + "</textarea></p>");
		
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
		
		if (dataContainer instanceof FreeTextQuestionDataContainer) {
			FreeTextQuestionDataContainer dc = (FreeTextQuestionDataContainer) dataContainer;
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
	 * @see de.cinovo.surveyplatform.ui.views.AbstractQuestionView#
	 * getPrintableRepresentation()
	 */
	@Override
	public String getPrintableRepresentation(final boolean result) {
		StringBuilder rep = new StringBuilder();
		rep.append(getQuestionBlock(question, TargetMedia.PRINTER_QUESTIONNAIRE));
		rep.append(getAdditionalInfoHtml(question, TargetMedia.PRINTER_QUESTIONNAIRE));
		Answer answerObj = question.getAnswerObj();
		rep.append("<p><textarea style=\"border: 0;height:auto;min-height:200px;\" name=\"text" + question.getId() + "\">" + (answerObj == null ? "" : HtmlFormUtil.escapeHtmlFull(answerObj.getAnswer()).replace("&#10;", "<br />")) + "</textarea></p>");
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
		if (dataContainer instanceof FreeTextQuestionDataContainer) {
			FreeTextQuestionDataContainer dc = (FreeTextQuestionDataContainer) dataContainer;
			StringBuilder rep = new StringBuilder();
			
			FreeTextFilterUtil u = new FreeTextFilterUtil(question.getId());
			
			// rep.append("<div style=\"border-top: 1px #D5D5D5 solid;border-left: 1px #D5D5D5 solid;border-right: 1px #D5D5D5 solid;\">");
			rep.append("<div>");
			int index = 0;
			
			Collections.sort(dc.answers);
			
			for (Answer answerObj : dc.answers) {
				final String answer = answerObj.getAnswer();
				if (!answer.isEmpty()) {
					rep.append("<div class=\"answerContainerContainer\"><div class=\"answerContainer\" data-datamapster=\"" + u.getId() + "\">");
					rep.append("<p id=\"q" + question.getId() + "a" + index + "\" data-answerid=\"" + answerObj.getId() + "\" class=\"id" + answerObj.getId() + " answer answer" + u.getId() + " " + u.getMd5FromAnswer(answer) + "\">" + HtmlFormUtil.escapeHtmlFull(answer).replace("&#10;", "<br />") + "</p>");
					rep.append("</div></div>");
					index++;
				}
			}
			rep.append("</div>");
			
			u.setAnswers(new AnswerContainer(dc.answers));
			u.setTopics(dc.topics);
			if (representationMetadata.targetMedia.equals(TargetMedia.SCREEN)) {
				rep.insert(0, u.getFilterBox(dc.answers.size()));
			}
			
			return rep.toString();
		} else {
			return "Dont know how to handle: " + dataContainer.getClass().getName();
			
		}
	}
}
