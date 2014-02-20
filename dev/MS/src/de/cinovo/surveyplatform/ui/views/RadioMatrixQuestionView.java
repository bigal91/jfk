package de.cinovo.surveyplatform.ui.views;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.RadioMatrixQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.util.WikiUtil;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class RadioMatrixQuestionView extends AbstractMatrixQuestionView {
	
	private RadioMatrixQuestion question;
	
	
	public RadioMatrixQuestionView(final RadioMatrixQuestion question) {
		super(question);
		this.question = question;
	}
	
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		rep.append(getQuestionBlock(question, targetMedia));
		rep.append(getAdditionalInfoHtml(question, targetMedia));
		rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
		boolean titleDrawn = false;
		
		if (question.getSubquestions().size() > 0) {
			int width = (int) Math.floor((double) 100 / (question.getSubquestions().get(0).getOptions().size() + 1));
			
			for (AbstractQuestion subQuestion : question.getSubquestions()) {
				
				// make a column with the optiondisplay names of the subquestions at
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
				int size = subQuestion.getOptions().size();
				for (Option option : subQuestion.getOptions()) {
					size--;
					rep.append("<td onclick=\"$(this).children('input[type=radio]').attr('checked', 'checked');\" style=\"text-align:center; cursor: pointer;\">");
					rep.append("\n<input title=\"" + option.getDisplayName() + "\" type=\"radio\" name=\"q" + subQuestion.getId() + "\" value=\"" + option.getId() + "\"" + (option.isSelected() ? "checked=\"checked\"" : "") + " " + getOptionScript(option) + " />");
					if (size == 0) {
						// this hidden field is necessarry to always make this
						// question present in the request parameter (as it would
						// not be, if no option of the question is selected)
						rep.append("<input type=\"hidden\" name=\"q" + subQuestion.getId() + "\" value=\"-1\" />");
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
