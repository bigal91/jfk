/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.help.ContextHelpProvider;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.SurveyUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class DataExportContainer extends AbstractContainer {
	
	/* (non-Javadoc)
	 * @see de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet.http.HttpServletRequest, java.lang.StringBuilder, de.cinovo.surveyplatform.model.SystemUser)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		showActiveSurveyOverview(request, content, currentUser);
	}
	
	private void showActiveSurveyOverview(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_DATA_EXPORT, HelpIDs.PAGE_DATA_EXPORT));
		
		if (currentUser != null) {
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("SURVEYTABLE", getActiveSurveys(request, currentUser, replacements));
			
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/recordDataSurveyOverview.html", replacements));
		}
	}
	
	private String getActiveSurveys(final HttpServletRequest request, final SystemUser currentUser, final Map<String, String> replacements) {
		boolean atLeastOneSurveyExists = false;
		
		StringBuilder content = new StringBuilder();
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			
			Criteria criteria = hibSess.createCriteria(Survey.class);
			
			criteria.add(Restrictions.eq("deleted", false));
			criteria.add(Restrictions.or(Restrictions.eq("state", SurveyState.RUNNING), Restrictions.eq("state", SurveyState.CREATED)));
			criteria.addOrder(Order.asc("name"));
			
			Map<String, String> rowReplacements = new HashMap<String, String>();
			
			StringBuilder tableRowsRunning = new StringBuilder();
			
			List<?> list = criteria.list();
			
			Collection<UserGroup> visibleGroups = GroupManager.getVisibleGroups(hibSess, currentUser, currentUser);
			
			for (Object obj : list) {
				if (obj instanceof Survey) {
					Survey survey = (Survey) obj;
					atLeastOneSurveyExists = true;
					UserGroup owner = survey.getOwner();
					if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR) || visibleGroups.contains(owner)) {
						rowReplacements.put("NAME", "<td><a href=\"javascript:void(0);\" onclick=\"showsurveyInfo(" + survey.getId() + ");\"><span id=\"" + SurveyDal.PARAM_SURVEYNAME + survey.getId() + "\">" + survey.getName() + "</span></a></td>");
						rowReplacements.put("OWNER", "<td>" + (owner == null ? "---" : owner.getName()) + "</td>");
						
						// rowReplacements.put("CREATED", "<td><span class=\"invisible\">" + survey.getCreationDate().getTime() + "</span>"
						// + TimeUtil.getLocalTime(currentUser, survey.getCreationDate()) + "</td>");
						rowReplacements.put("STATE", "<td>" + survey.getStateDisplayname() + "</td>");
						rowReplacements.put("HIGHLIGHT", "<td></td>");
						
						int progress = 0;
						progress = SurveyUtil.calculateReturnRate(currentUser, survey);
						rowReplacements.put("PROGRESS", "<td>" + PartsUtil.getProgressBar(progress) + "</td>");
						
						rowReplacements.put("SURVEYID", survey.getId() + "");
						
						StringBuilder buttons = new StringBuilder();
						buttons.append(PartsUtil.getIconLink("PDF", "Export Data", "", "/download?type=participations&surveyID=" + survey.getId(), false, null, false));
						
						rowReplacements.put("BUTTONS", buttons.toString());
						rowReplacements.put("WIDTH", "30");
						
						tableRowsRunning.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_row.html", rowReplacements));
					}
				}
			}
			replacements.put("RUNNINGSTR", SurveyState.getDisplayName(SurveyState.RUNNING));
			
			replacements.put("BUTTONCOLUMNWIDTH", (50) + "");
			replacements.put("DIALOGSURVEYINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfo.html", replacements));
			
			if (tableRowsRunning.length() > 0) {
				replacements.put("SECTIONHEADER", "Active Surveys");
				replacements.put("HELPLINK", ContextHelpProvider.getInstance().getHelpLink("table.running", "Click to get detailed info", "", ""));
				replacements.put("ROWS_RUNNING", tableRowsRunning.toString());
				replacements.put("SHOW_ALL_STYLE", ",\"sDom\": 'fprt<\"bottom\"i>'");
				replacements.put("TABLE_RUNNING_SURVEYS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_running.html", replacements));
			}
			
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable.html", replacements));
			tx.commit();
		} finally {
			hibSess.close();
		}
		
		if (atLeastOneSurveyExists) {
			replacements.put("INFO_SURVEYEXISTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/recordDataInfo.html", replacements));
		} else {
			replacements.put("INFO_SURVEYEXISTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/recordDataFirstImportInfo.html", replacements));
		}
		
		return content.toString();
	}
	
}
