package de.cinovo.surveyplatform.ui.pages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class ManageSurveysContainer extends AbstractContainer {
	
	private SurveyListContainer surveyListContainer = new SurveyListContainer();
	
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_MANAGE_SURVEYS, HelpIDs.PAGE_MANAGE_SURVEYS));
		
		boolean atLeastOneSurveyExists = false;
		if (currentUser != null) {
			Map<String, String> replacements = new HashMap<String, String>();
			StringBuilder commands = new StringBuilder();
			commands.append("<select class=\"commandSelector\">");
			commands.append("<option value=\"\">-- Select a command--</option>");
			commands.append("<option value=\"delete\">Remove selected surveys from view</option>");
			commands.append("</select><a class=\"runCommandButton\" href=\"javascript:void(0);\" onclick=\"sendCommand($(this).prev().val());\">Go</a>");
			replacements.put("COMMANDS", commands.toString());
			replacements.put("SURVEYTABLE", this.surveyListContainer.getContent(request, replacements, currentUser));
			
			if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
				replacements.put("SYSTEMPLATEOPTION", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgCreateSurvey_typeSystemTemplate.html", null));
			}
			replacements.put("TYPEPART", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgCreateSurvey_type.html", replacements));
			
			StringBuilder surveyNameList = new StringBuilder();
			
			surveyNameList.append("<option value=\"-1\">---</option>");
			
			List<ISurvey> surveys = this.surveyListContainer.getSurveys();
			for (ISurvey survey : surveys) {
				surveyNameList.append("<option value=\"" + survey.getId() + "\">" + survey.getName() + "</option>");
			}
			StringBuilder participantCopysurveyNameList = new StringBuilder();
			participantCopysurveyNameList.append("<option value=\"-1\">---</option>");
			for (ISurvey survey : surveys) {
				if (!survey.getState().equals(SurveyState.SYSTEMTEMPLATE)) {
					atLeastOneSurveyExists = true;
					participantCopysurveyNameList.append("<option value=\"" + survey.getId() + "\">" + survey.getName() + "</option>");
				}
			}
			
			replacements.put("SURVEYNAMELIST", surveyNameList.toString());
			replacements.put("PARTICIPANT_COPY_SURVEYNAMELIST", participantCopysurveyNameList.toString());
			
			replacements.put("SESSIONID", request.getSession().getId());
			
			if (atLeastOneSurveyExists) {
				replacements.put("INFO_SURVEYEXISTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/manageSurveysInfoExists.html", replacements));
			} else {
				replacements.put("INFO_SURVEYEXISTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/manageSurveysInfoNotExists.html", replacements));
			}
			
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/manageSurveys.html", replacements));
		}
	}
	
}
