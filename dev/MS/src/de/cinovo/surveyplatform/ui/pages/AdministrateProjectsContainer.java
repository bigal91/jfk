/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

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
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Project;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.servlets.dal.ProjectDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class AdministrateProjectsContainer extends AbstractContainer {
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet.http.HttpServletRequest, java.lang.StringBuilder,
	 * de.cinovo.surveyplatform.model.SystemUser)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
			showProjectTable(content, currentUser);
		} else {
			content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
		}
	}
	
	private void showProjectTable(final StringBuilder content, final SystemUser currentUser) {
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_ADMINISTRATE_PROJECTS, HelpIDs.PAGE_ADMINISTRATE_PROJECTS));
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("PROJECTTABLE", getProjectTable(currentUser));
		
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/manageProjects.html", replacements));
	}
	
	private String getProjectTable(final SystemUser currentUser) {
		Map<String, String> replacements = new HashMap<String, String>();
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			
			Criteria criteria = hibSess.createCriteria(Project.class);
			criteria.add(Restrictions.eq("owner", GroupManager.getClient(currentUser)));
			criteria.addOrder(Order.asc("name"));
			
			Map<String, String> rowReplacements = new HashMap<String, String>();
			
			StringBuilder tableRows = new StringBuilder();
			
			List<?> list = criteria.list();
			for (Object obj : list) {
				if (obj instanceof Project) {
					Project iterProject = (Project) obj;
					rowReplacements.put("PROJECTID", iterProject.getId() + "");
					rowReplacements.put("NAME", "<a href=\"javascript:void(0);\" onclick=\"showClientInfo('" + iterProject.getId() + "');\"><span id=\"" + ProjectDal.PARAM_NAME + iterProject.getId() + "\">" + iterProject.getName() + "</span></a>");
					
					rowReplacements.put("SURVEYS", "---");
					
					rowReplacements.put("BUTTONS", PartsUtil.getIconLink("MANAGE_SURVEYS", "Manage Surveys", "", "?page=" + Pages.PAGE_MANAGE_SURVEYS + "&projectID=" + iterProject.getId()));
					
					tableRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateProjectTable_row.html", rowReplacements));
				}
			}
			tx.commit();
			replacements.put("ROWS", tableRows.toString());
			
			replacements.put("BUTTONCOLUMNWIDTH", "20");
		} finally {
			hibSess.close();
		}
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateProjectTable.html", replacements);
	}
	
}
