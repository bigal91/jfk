package de.cinovo.surveyplatform.ui.pages;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.bootstrap.configuration.FeatureToggle;
import de.cinovo.surveyplatform.bootstrap.configuration.FeatureToggle.Feature;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.servlets.dal.UserGroupDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 *
 * Copyright 2010 Cinovo AG
 *
 * This is the main entry page for the site. This is where dispatching
 * according to the GET parameters of the HTTP request happens.
 *
 * @author yschubert
 *
 *
 */
public class PageDispatcher extends AbstractContainer {
	
	private LoginContainer loginContainer = new LoginContainer();
	
	private LostPasswordContainer lostPasswordContainer = new LostPasswordContainer();
	
	private RegisterContainer registerContainer = new RegisterContainer();
	
	private StartPageContainer startPageContainer = new StartPageContainer();
	
	private DataRecordContainer dataRecordContainer = new DataRecordContainer();
	
	private DataImportContainer dataImportContainer = new DataImportContainer();
	
	private DataExportContainer dataExportContainer = new DataExportContainer();
	
	private EditQuestionnaireContainer surveyContainer = new EditQuestionnaireContainer();
	
	private ManageSurveysContainer manageSurveysContainer = new ManageSurveysContainer();
	
	private AnalyseReportsContainer reportsContainer = new AnalyseReportsContainer();
	
	private AdministrateParticipantsContainer administrateParticipants = new AdministrateParticipantsContainer();
	
	private AdministrateUserContainer administrateUserContainer = new AdministrateUserContainer();
	
	private ProfileContainer profileContainer = new ProfileContainer();
	
	private AdministrateClientsContainer administrateClients = new AdministrateClientsContainer();
	
	private AdministrateGroupsContainer administrateGroups = new AdministrateGroupsContainer();
	
	private AdministrateProjectsContainer administrateProjects = new AdministrateProjectsContainer();
	
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		dispatch(request, request.getParameter(Pages.PAGE_MAIN_PARAM), content, currentUser);
	}
	
	private void dispatch(final HttpServletRequest request, final String aPage, final StringBuilder content, SystemUser currentUser) {
		String page = aPage;
		final StringBuilder innerContent = new StringBuilder();
		final Map<String, String> replacements = new HashMap<String, String>();
		try {
			
			if (page == null) {
				if (currentUser == null) {
					page = Pages.PAGE_LOGIN;
				} else {
					page = Constants.DEFAULT_PAGE;
				}
			} else {
				if ((currentUser == null) && !page.equals("lostPassword") && !page.equals("register") && !page.equals("register_success")) {
					page = Pages.PAGE_LOGIN;
				}
			}
			
			if (page.equals("logout")) {
				request.getSession().invalidate();
				// Setze Page auf "LOGIN" nach logout Befehl
				page = Pages.PAGE_LOGIN;
				if (currentUser != null) {
					Logger.logUserActivity("Logged out", currentUser.getUserName());
				}
				currentUser = null;
			}
			boolean userIsRecorder = userIsRecorder(currentUser);
			if (EnvironmentConfiguration.getConfiguration(ConfigID.OFFLINE_MODE).equals(true) || userIsRecorder) {
				if (page.equals(Pages.PAGE_LOGIN) && (currentUser == null)) {
					loginContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_STARTPAGE) || (page.equals(Pages.PAGE_LOGIN) && (currentUser != null))) {
					dataRecordContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_DATA_RECORD)) {
					dataRecordContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_DATA_IMPORT)) {
					dataImportContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_DATA_EXPORT)) {
					dataExportContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_PROFILE)) {
					profileContainer.provideContent(request, innerContent, currentUser);
				}
			} else {
				
				// PAGE_LOGIN kann nur aufgerufen werden, wenn User == null ist.
				if (page.equals(Pages.PAGE_LOGIN) && (currentUser == null)) {
					loginContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_LOST_PW) && (currentUser == null)) {
					lostPasswordContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_REGISTER) && (currentUser == null) && FeatureToggle.isEnabled(Feature.REGISTER)) {
					registerContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_REGISTER_SUCCESS)) {
					request.setAttribute(Pages.PAGE_REGISTER_SUCCESS, "register_success");
					registerContainer.provideContent(request, innerContent, currentUser);
				}
				
				// PAGE_STARTPAGE kann auch dann aufgerufen werden, wenn PAGE_LOGIN
				// zuletzt offen war UND User eingeloggt ist.
				else if (page.equals(Pages.PAGE_STARTPAGE) || (page.equals(Pages.PAGE_LOGIN) && (currentUser != null))) {
					startPageContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_SURVEY)) {
					surveyContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_MANAGE_SURVEYS)) {
					manageSurveysContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_ADMINISTRATE_PARTICIPANTS)) {
					administrateParticipants.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_REPORTS)) {
					reportsContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_USERADMIN)) {
					administrateUserContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_ADMINISTRATE_CLIENTS)) {
					administrateClients.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_ADMINISTRATE_GROUPS)) {
					administrateGroups.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_ADMINISTRATE_PROJECTS)) {
					administrateProjects.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_DATA_IMPORT)) {
					dataImportContainer.provideContent(request, innerContent, currentUser);
				} else if (page.equals(Pages.PAGE_PROFILE)) {
					profileContainer.provideContent(request, innerContent, currentUser);
				} else {
					String fileName = Paths.HTMLPAGES + "/" + page + ".html";
					if ((new File(fileName)).exists()) {
						innerContent.append(TemplateUtil.getTemplate(fileName, null));
					} else {
						innerContent.append("Page '" + page + "' does not exist.");
					}
				}
			}
			
			replacements.put("CONTENT", innerContent.toString());
			replacements.put("HEADMENU", createHeadMenu(page, currentUser, request, userIsRecorder));
			
			if (currentUser != null) {
				replacements.put("LOGOUT_LINK", "<a href=\"?page=logout\">logout</a>");
			}
			
		} catch (Exception e) {
			replacements.put("CONTENT", "<p style=\"text-align: center;\">Sorry, there was an unexpected error. Please contact your support! (" + new SimpleDateFormat().format(new Date()) + ")<br /><br /></p>");
			Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
		}
		
		if (AuthUtil.isImpersonating(request)) {
			SystemUser user = AuthUtil.checkAuth(request);
			if (user != null) {
				replacements.put("TOPMIDDLETEXT", "<div style=\"color: #1D5987; font-weight: bold; font-size: 15px;\">Impersonating " + user.getUserName() + "</div>");
			}
		} else if (AuthUtil.isInDemoMode(request)) {
			replacements.put("TOPMIDDLETEXT", "<div style=\"color: #1D5987; font-weight: bold; font-size: 15px;\">You are in the demonstration mode.<br /><br /></div>");
		}
		
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/layout.html", replacements));
	}
	
	/**
	 * @param currentUser
	 * @return
	 */
	private boolean userIsRecorder(final SystemUser currentUser) {
		if (currentUser == null) {
			return false;
		}
		if (AuthUtil.hasRight(currentUser, UserRights.CLIENT_ADMINISTRATOR, UserRights.DATA_VIEWER, UserRights.GROUP_ADMINISTRATOR, UserRights.SURVEY_MANAGER)) {
			return false;
		}
		if (AuthUtil.hasRight(currentUser, UserRights.DATA_RECORDER)) {
			return true;
		}
		return false;
	}
	
	private String createHeadMenu(final String page, final SystemUser currentUser, final HttpServletRequest request, final boolean userIsRecorder) {
		StringBuilder headMenu = new StringBuilder();
		
		if (page.equals(Pages.PAGE_REGISTER) || (currentUser == null)) {
			headMenu.append(getMenuItem(page, Pages.PAGE_LOGIN, Pages.PAGE_HEADER_LOGIN));
			if (!EnvironmentConfiguration.isOfflineMode() && FeatureToggle.isEnabled(Feature.REGISTER)) {
				headMenu.append(getMenuItem(page, Pages.PAGE_REGISTER, "Create Account"));
			}
		} else {
			if (EnvironmentConfiguration.isOfflineMode()) {
				headMenu.append(getMenuItem(page, Pages.PAGE_DATA_RECORD, Pages.PAGE_HEADER_DATA_RECORD));
				headMenu.append(getMenuItem(page, Pages.PAGE_DATA_IMPORT, Pages.PAGE_HEADER_DATA_IMPORT));
				headMenu.append(getMenuItem(page, Pages.PAGE_DATA_EXPORT, Pages.PAGE_HEADER_DATA_EXPORT));
			} else if (userIsRecorder) {
				headMenu.append(getMenuItem(page, Pages.PAGE_DATA_RECORD, Pages.PAGE_HEADER_DATA_RECORD));
				headMenu.append(getMenuItem(page, Pages.PAGE_PROFILE, Pages.PAGE_HEADER_MY_PROFILE, (page.equals(Pages.PAGE_PROFILE)) ? Constants.CSS_HEADMENUITEM_SELECTED : ""));
				
			} else {
				
				if (AuthUtil.hasRight(currentUser, UserRights.SURVEY_MANAGER, UserRights.CLIENT_ADMINISTRATOR, UserRights.GROUP_ADMINISTRATOR)) {
					headMenu.append(getMenuItem(page, Pages.PAGE_MANAGE_SURVEYS, Pages.PAGE_HEADER_MANAGE_SURVEYS, (page.equals(Pages.PAGE_MANAGE_SURVEYS) || page.equals(Pages.PAGE_ADMINISTRATE_PARTICIPANTS) || page.equals(Pages.PAGE_SURVEY)) ? Constants.CSS_HEADMENUITEM_SELECTED : ""));
				}
				headMenu.append(getMenuItem(page, Pages.PAGE_REPORTS, Pages.PAGE_HEADER_ANALYSE_REPORTS));
				
				if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
					boolean selected = page.equals(Pages.PAGE_ADMINISTRATE_CLIENTS) || page.equals(Pages.PAGE_ADMINISTRATE_GROUPS) || (page.equals(Pages.PAGE_USERADMIN) && (request.getParameter(UserGroupDal.PARAM_GROUPID) != null));
					headMenu.append(getMenuItem(page, Pages.PAGE_ADMINISTRATE_CLIENTS, Pages.PAGE_HEADER_ADMINISTRATE_CLIENTS, (selected) ? Constants.CSS_HEADMENUITEM_SELECTED : ""));
				} else {
					if (AuthUtil.isAllowedToEditSystemUser(currentUser)) {
						headMenu.append(getMenuItem(page, Pages.PAGE_ADMINISTRATE_GROUPS, Pages.PAGE_HEADER_ADMINISTRATE_GROUPS, (page.equals(Pages.PAGE_ADMINISTRATE_GROUPS)) ? Constants.CSS_HEADMENUITEM_SELECTED : ""));
					}
				}
				
				headMenu.append(getMenuItem(page, Pages.PAGE_PROFILE, Pages.PAGE_HEADER_MY_PROFILE, (page.equals(Pages.PAGE_PROFILE)) ? Constants.CSS_HEADMENUITEM_SELECTED : ""));
			}
		}
		
		return headMenu.toString();
	}
	
	private String getMenuItem(final String currentPage, final String page, final String label) {
		final Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("LINK", "?page=" + page);
		replacements.put("CLASS", currentPage.equals(page) ? Constants.CSS_HEADMENUITEM_SELECTED : "");
		replacements.put("LABEL", label);
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/headMenuItem.html", replacements);
	}
	
	private String getMenuItem(final String currentPage, final String page, final String label, final String className) {
		final Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("LINK", "?page=" + page);
		replacements.put("CLASS", className);
		replacements.put("LABEL", label);
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/headMenuItem.html", replacements);
	}
	
}
