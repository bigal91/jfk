package de.cinovo.surveyplatform.ui.views;

import java.util.HashMap;
import java.util.Map;

import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceMatrixQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.WikiUtil;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class MultipleChoiceMatrixQuestionView extends AbstractMatrixQuestionView {
	
	private MultipleChoiceMatrixQuestion question;
	
	
	public MultipleChoiceMatrixQuestionView(final MultipleChoiceMatrixQuestion question) {
		super(question);
		this.question = question;
	}
	
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		rep.append(getQuestionBlock(question, targetMedia));
		rep.append(getAdditionalInfoHtml(question, targetMedia));
		
		if (question.getSubquestions().size() > 0) {
			rep.append("<table style=\"width: 100%;\" class=\"questionTable\" cellspacing=\"0\" cellpadding=\"0\">");
			boolean titleDrawn = false;
			
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
					String displayNameHash = AuthUtil.md5(option.getDisplayName());
					rep.append("<td class=\"checkboxCell" + question.getId() + "\" style=\"text-align:center; cursor:pointer;\">");
					rep.append("\n<input title=\"" + option.getDisplayName() + "\"  class=\"checkbox" + question.getId() + "_" + displayNameHash + " checkbox" + question.getId() + "\" type=\"checkbox\" name=\"q" + subQuestion.getId() + "[]\" value=\"" + option.getId() + "\"" + (option.isSelected() ? "checked=\"checked\"" : "") + " " + getOptionScript(option) + " />");
					if (size == 0) {
						// this hidden field is necessarry to always make this
						// question present in the request parameter (as it would
						// not be, if no option of the question is selected)
						rep.append("<input type=\"hidden\" name=\"q" + subQuestion.getId() + "[]\" value=\"-1\" />");
					}
					rep.append("</td>");
				}
				rep.append("</tr>");
			}
			rep.append("</table>");
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("QUESTIONID", question.getId() + "");
			rep.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/checkBoxCellClickScript.html", replacements));
		}
		return rep.toString();
	}
}
