/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;

import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.TaskInfo;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.UserStatus;
import de.cinovo.surveyplatform.servlets.dal.SystemUserDal;
import de.cinovo.surveyplatform.servlets.dal.UserGroupDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.SessionManager;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;



/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class AdministrateUserContainer extends AbstractContainer {
	
	private static final String PARAM_USERID = "userID";
	
	
	/* (non-Javadoc)
	 * @see de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet.http.HttpServletRequest, java.lang.StringBuilder)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		
		if (AuthUtil.isAllowedToEditSystemUser(currentUser)) {
			if (ParamUtil.checkAllParamsSet(request, PARAM_USERID)) {
				showInfo(request.getParameter(PARAM_USERID), content);
			} else {
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					
					UserGroup group = null;
					long groupID = ParamUtil.getSafeLongFromParam(request, UserGroupDal.PARAM_GROUPID);
					if (groupID > 0) {
						group = (UserGroup) hibSess.load(UserGroup.class, groupID);
					}
					
					if ((group == null) && !AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
						content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
						return;
					}
					
					showUserTable(request, content, currentUser, group);
					
					tx.commit();
				} finally {
					hibSess.close();
				}
			}
		} else {
			content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
		}
		
	}
	
	private void showUserTable(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser, final UserGroup group) {
		if (group == null) {
			content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_ADMINISTRATE_USERS, HelpIDs.PAGE_USERADMIN));
		} else {
			content.append(PartsUtil.getPageHeader(group.getName(), HelpIDs.PAGE_USERADMIN, new String[] {"<a href=\"?page=" + Pages.PAGE_ADMINISTRATE_CLIENTS + "\">" + group.getClient().getOrganization() + "</a>", "<a href=\"?page=" + Pages.PAGE_ADMINISTRATE_GROUPS + "&clientID=" + group.getClient().getId() + "\">" + Pages.PAGE_HEADER_ADMINISTRATE_GROUPS + "</a>"}));
		}
		
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("USERTABLE", getUserTable(request, group));
		
		Map<String, String> comboReplacements = new HashMap<String, String>();
		List<StringStringPair> options = new ArrayList<StringStringPair>();
		options.add(new StringStringPair(TimeUtil.DATEFORMAT_EN.replace("'", "_"), TimeUtil.DATEFORMAT_EN.replace("'", "")));
		options.add(new StringStringPair(TimeUtil.DATEFORMAT_DE.replace("'", "_"), TimeUtil.DATEFORMAT_DE.replace("'", "")));
		comboReplacements.put("DATEFORMAT_COMBO", HtmlFormUtil.getComboBox(SystemUserDal.PARAM_DATEFORMAT, SystemUserDal.PARAM_DATEFORMAT, options, currentUser.getTimeFormat().replace("'", "_")));
		
		String[] availableTimeZones = TimeUtil.getAvailableTimeZones();
		options.clear();
		for (String zoneID : availableTimeZones) {
			options.add(new StringStringPair(zoneID, zoneID));
		}
		comboReplacements.put("TIMEZONE_COMBO", HtmlFormUtil.getComboBox(SystemUserDal.PARAM_TIMEZONE, SystemUserDal.PARAM_TIMEZONE, options, currentUser.getTimeZoneID()));
		
		if (group != null) {
			replacements.put("GROUPID", group.getId() + "");
		}
		
		replacements.put("TABLE_EDITUSER", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableEditUser.html", comboReplacements));
		replacements.put("DLGCREATEUSER", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgCreateUser.html", replacements));
		replacements.put("DLGUSERINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgUserInfo.html", null));
		replacements.put("DLGCHANGEPASSWORD", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgChangePassword.html", replacements));
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateUser.html", replacements));
	}
	
	private void showInfo(final String userId, final StringBuilder content) {
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_CLIENTINFO, HelpIDs.PAGE_CLIENTINFO, new String[] {"<a href=\"?page=" + Pages.PAGE_USERADMIN + "\">" + Pages.PAGE_HEADER_ADMINISTRATE_CLIENTS + "</a>"}));
		Map<String, String> replacements = new HashMap<String, String>();
		
		Set<String> users = new HashSet<String>();
		users.add(userId);
		StringBuilder log = new StringBuilder();
		StringBuilder tasks = new StringBuilder();
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		Set<Entry<String, TaskInfo>> entrySet = fbp.getTaskMap().entrySet();
		for (Entry<String, TaskInfo> entry : entrySet) {
			TaskInfo taskInfo = entry.getValue();
			long ttl = (FeedBackProvider.MAX_TASKINFO_AGE - taskInfo.getAgeByFinishTime());
			if (ttl > 0) {
				if (users.contains(taskInfo.getCreatedBy())) {
					tasks.append("<div style=\"margin-bottom: 10px; padding-bottom: 10px;\">");
					tasks.append("<h2>");
					tasks.append("Task: " + taskInfo.getTaskName());
					tasks.append("</h2>");
					
					tasks.append("<p>Created by: ");
					tasks.append(taskInfo.getCreatedBy());
					tasks.append("</p>");
					
					tasks.append("<p>Message: ");
					tasks.append(taskInfo.getMessage());
					tasks.append("</p>");
					
					tasks.append("<p>Status: ");
					tasks.append(taskInfo.getStatusCode());
					tasks.append("</p>");
					
					tasks.append("<div style=\"width: 200px;\">Progress:");
					tasks.append(PartsUtil.getProgressBar(taskInfo.getProgress()));
					tasks.append("</div>");
					tasks.append("</div>");
				}
			}
		}
		if (tasks.length() == 0) {
			tasks.append("There are no tasks currently running.");
		}
		
		for (String user : users) {
			log.append("<div>");
			log.append("Activities of the user: " + user);
			log.append("<div style=\"height: 300px; overflow: auto; border: #D5D5D5 1px solid; padding: 10px;\">");
			fileToStringBuilder(new File("logs/" + user + "_out.log"), log);
			log.append("</div>");
			log.append("</div>");
		}
		
		replacements.put("LOG", log.toString());
		replacements.put("CURRENTTASKS", tasks.toString());
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/userInfo.html", replacements));
	}
	
	/**
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String getUserTable(final HttpServletRequest request, final UserGroup group) {
		
		Set<SystemUser> userList = null;
		
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		Map<String, String> replacements = new HashMap<String, String>();
		
		try {
			if (group == null) {
				Transaction tx = hibSess.beginTransaction();
				
				Criteria criteria = hibSess.createCriteria(SystemUser.class);
				criteria.addOrder(Order.asc("userName"));
				
				userList = new LinkedHashSet<SystemUser>(criteria.list());
				tx.commit();
			} else {
				userList = group.getMembers();
			}
			
			Map<String, String> rowReplacements = new HashMap<String, String>();
			
			StringBuilder tableRows = new StringBuilder();
			
			for (SystemUser iterUser : userList) {
				if (!iterUser.getUserStatus().equals(UserStatus.Deleted)) {
					rowReplacements.put("USERNAME", "<a href=\"javascript:void(0);\" onclick=\"showClientInfo('" + iterUser.getUserName() + "');\">" + iterUser.getActualUserName() + "</a>");
					rowReplacements.put("USERID", iterUser.getUserName());
					
					rowReplacements.put("NAME", (iterUser.getFirstName() == null ? "" : (iterUser.getFirstName() + " ")) + (iterUser.getLastName() == null ? "" : iterUser.getLastName()));
					
					rowReplacements.put("ORGANIZATION", iterUser.getUserGroups().iterator().next().getClient().getOrganization());
					
					HttpSession session = SessionManager.getInstance().getSession(iterUser.getUserName());
					if (iterUser.getUserStatus().equals(UserStatus.Disabled)) {
						rowReplacements.put("BUTTONS", PartsUtil.getIcon("BULLET_DELETE", "This account is DISABLED"));
					} else {
						if ((session == null) || !SessionManager.getInstance().isSessionValid(session)) {
							rowReplacements.put("BUTTONS", PartsUtil.getIcon("BULLET_BLACK", ""));
						} else {
							rowReplacements.put("BUTTONS", PartsUtil.getIcon("BULLET_GREEN", "Logged in since: " + TimeUtil.getLocalTime(request, new Date(session.getCreationTime())) + "\nLast Activity: " + TimeUtil.getLocalTime(request, new Date(session.getLastAccessedTime()))));
						}
					}
					
					tableRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateUserTable_row.html", rowReplacements));
				}
			}
			
			replacements.put("ROWS", tableRows.toString());
			replacements.put("BUTTONCOLUMNWIDTH", "20");
		} finally {
			hibSess.close();
		}
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateUserTable.html", replacements);
	}
	
	private void fileToStringBuilder(final File file, final StringBuilder stringBuilder) {
		if (file.exists()) {
			try {
				FileInputStream fr = new FileInputStream(file);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fr));
				String line = null;
				
				while ((line = bufferedReader.readLine()) != null) {
					stringBuilder.append(line + "<br />");
				}
				
				bufferedReader.close();
			} catch (Exception ex) {
				Logger.err("Could not read the file " + file.getAbsolutePath(), ex);
			}
		} else {
			stringBuilder.append("-No activities logged so far-");
		}
	}
	
}
