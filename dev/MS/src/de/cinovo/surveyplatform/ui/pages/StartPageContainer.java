package de.cinovo.surveyplatform.ui.pages;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * This is the start page of the site
 * 
 * @author yschubert
 * 
 */
public class StartPageContainer extends AbstractContainer {
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser user) {
		// Session hibSess = HibernateUtil.getSessionFactory().openSession();
		// try {
		// if (hasSurveys(hibSess, user)) {
		// ManageSurveysContainer msc = new ManageSurveysContainer();
		// msc.provideContent(request, content, user);
		// return;
		// }
		// } finally {
		// hibSess.close();
		// }
		
		SystemUser currentUser = user;
		
		Map<String, String> replacements = new HashMap<String, String>();
		
		replacements.put("USERNAME", currentUser.getActualUserName());
		
		
		if (!EnvironmentConfiguration.isOfflineMode()) {
			replacements.put("SSL", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/sslSign.html", null));
		}
		
		StringBuilder news = new StringBuilder();
		
		String logo = PartsUtil.getClientLogo(currentUser.getUserGroups().iterator().next().getClient(), 0);
		if ("".equals(logo)) {
			news.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/startPageUploadLogoButton.html", null));
		}
		content.append(PartsUtil.getPageHeader("Welcome!", ""));
		
		news.append("<div style=\"margin: 0 0 10px 0;float:right;\">" + logo + "</div>");
		if ((currentUser.getFirstName() == null) || (currentUser.getLastName() == null)) {
			news.append("<div style=\"font-size: 20px;padding: 20px 0 0 155px;\">Welcome!</div>");
		} else {
			news.append("<div style=\"font-size: 20px;padding: 20px 0 0 155px;\">Welcome, " + currentUser.getFirstName() + " " + currentUser.getLastName() + "!</div>");
		}
		Date lastLogin = (Date) request.getSession().getAttribute(Constants.ATTR_LASTLOGIN);
		if (lastLogin != null) {
			news.append("<div style=\"font-size: 15px;padding: 20px 0 0 155px;\"><em>Last login: " + TimeUtil.getLocalTime(currentUser, lastLogin) + "</em></div>");
		}
		
		replacements.put("STARTCONTENT", news.toString());
		//
		
		if (AuthUtil.isAllowedToEditSurveys(currentUser)) {
			replacements.put("BUTTON_MANAGESURVEYS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/welcomePageButtonManageSurveys.html", null));
		}
		if (AuthUtil.isAllowedToViewReports(currentUser)) {
			replacements.put("BUTTON_ANALYSEREPORTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/welcomePageButtonAnalyseReports.html", null));
		}
		
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/mainMenu.html", replacements));
		
	}
	//
	// private boolean hasSurveys(final Session hibSess, final SystemUser
	// currentUser) {
	// Criteria surveysCriteria = hibSess.createCriteria(Survey.class);
	//
	// surveysCriteria.add(Restrictions.eq("deleted", false));
	// surveysCriteria.add(Restrictions.in("owner",
	// GroupManager.getVisibleGroups(hibSess, currentUser, currentUser)));
	// surveysCriteria.setMaxResults(1);
	// List<?> list = surveysCriteria.list();
	// if ((list == null) || (list.size() == 0)) {
	// return false;
	// }
	// return true;
	// }
	
}
