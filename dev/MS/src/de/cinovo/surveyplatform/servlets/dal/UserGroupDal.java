/**
 *
 */
package de.cinovo.surveyplatform.servlets.dal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.mail.EmailException;
import org.hibernate.Criteria;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.CallBack;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.Token;
import de.cinovo.surveyplatform.model.Token.SaveTokenCallback;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.UserRight;
import de.cinovo.surveyplatform.model.factory.DtoFactory;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.EmailManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class UserGroupDal extends AbstractSccServlet {
	
	/**
	 *
	 */
	private static final String PARAM_GROUP_RIGHTS = "groupRight";
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String PARAM_GROUPID = "groupID";
	public static final String PARAM_GROUPS = "groups[]";
	
	public static final String PARAM_NAME = "name";
	public static final String PARAM_PARENT_GROUP = "parentGroup";
	
	private static final String PARAM_MAILS = "mails";
	private static final String PARAM_MESSAGE = "message";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#isAccessAllowed(
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse,
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet.Method)
	 */
	@Override
	public boolean isAccessAllowed(final HttpServletRequest req, final Method method, final SystemUser currentUser) {
		
		if (method.equals(Method.RETRIEVE) || AuthUtil.isAllowedToEditSystemUser(currentUser)) {
			return true;
		}
		
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#processCreate(javax
	 * .servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processCreate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			String name = ParamUtil.getSafeParam(req, PARAM_NAME);
			long clientID = ParamUtil.getSafeLongFromParam(req, ClientDal.PARAM_CLIENTID);
			long parentGroupId = ParamUtil.getSafeLongFromParam(req, PARAM_PARENT_GROUP);
			
			String taskID = "page." + Pages.PAGE_ADMINISTRATE_GROUPS + "." + req.getSession().getId();
			FeedBackProvider fbp = FeedBackProvider.getInstance();
			
			boolean allOkay = true;
			
			if ((name == null) || name.trim().isEmpty()) {
				fbp.addFeedback(taskID, "Please enter a non empty name.", Status.ERROR);
				allOkay = false;
			} else {
				// check uniqueness
				Criteria c = hibSess.createCriteria(UserGroup.class);
				c.add(Restrictions.eq("client.id", clientID));
				c.add(Restrictions.ilike("name", name, MatchMode.EXACT));
				if (parentGroupId != 0) {
					c.add(Restrictions.eq("parentGroup.id", parentGroupId));
				}
				Object result = c.uniqueResult();
				if (result == null) {
					allOkay &= true;
				} else {
					allOkay = false;
					fbp.addFeedback(taskID, "There is already a group with this name: " + name, Status.ERROR);
				}
			}
			
			if (clientID <= 0) {
				fbp.addFeedback(taskID, "Don't know to which client the group applies to.", Status.ERROR);
				allOkay = false;
			} else {
				allOkay &= true;
			}
			
			if (allOkay) {
				
				try {
					Transaction tx = hibSess.beginTransaction();
					
					Client client = (Client) hibSess.get(Client.class, clientID);
					
					if (AuthUtil.isAllowedToEditGroup(currentUser, client, parentGroupId, hibSess)) {
						
						UserGroup group = new UserGroup();
						group.setName(name);
						group.setClient(client);
						
						if (parentGroupId == 0) {
							group.setParentGroup(null);
						} else {
							UserGroup parentGroup = (UserGroup) hibSess.get(UserGroup.class, parentGroupId);
							if (parentGroup != null) {
								group.setParentGroup(parentGroup);
							}
						}
						
						hibSess.save(group);
						tx.commit();
						fbp.setMessage(taskID, "Group " + name + " successfully created.", currentUser.getActualUserName());
						if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
							resp.getWriter().print(DtoFactory.getInstance().createDto(group).getJSON());
						}
						fbp.finishTask(taskID);
					} else {
						tx.rollback();
						fbp.setMessage(taskID, "You are not allowed to create the group " + name, currentUser.getActualUserName());
						fbp.finishTask(taskID, true);
						handlePermissionDenied(req, resp);
					}
					
				} catch (Exception e) {
					fbp.setMessage(taskID, "Could not create group!", currentUser.getActualUserName());
					Logger.err("Gruppe konnte nicht angelegt werden: " + name, e);
					if ((e instanceof ConstraintViolationException) || (e instanceof NonUniqueObjectException)) {
						fbp.addFeedback(taskID, "There is already a group with this name: " + name, Status.ERROR);
					} else {
						fbp.addFeedback(taskID, e.getMessage(), Status.ERROR);
					}
					fbp.finishTask(taskID, true);
				}
				
			} else {
				fbp.setMessage(taskID, "Could not create group!", currentUser.getActualUserName());
				fbp.finishTask(taskID, true);
			}
			
			if (!ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
				resp.sendRedirect(getStandardRedirectLocation(req));
			}
		} finally {
			hibSess.close();
		}
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#processRetrieve(
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		if (ParamUtil.checkAllParamsSet(req, PARAM_GROUPID)) {
			
			final long groupID = ParamUtil.getSafeLongFromParam(req, PARAM_GROUPID);
			if (groupID > 0) {
				
				PrintWriter writer = resp.getWriter();
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					UserGroup group = (UserGroup) hibSess.load(UserGroup.class, groupID);
					Token token = group.getToken();
					if (token == null) {
						SaveTokenCallback cb = new SaveTokenCallback() {
							
							@Override
							public boolean save(final Token token, final Session hibSess) {
								UserGroup g = (UserGroup) hibSess.load(UserGroup.class, groupID);
								g.setToken(token);
								hibSess.save(g);
								return true;
							}
						};
						token = Token.createToken(7, cb);
						group.setToken(token);
						hibSess.save(group);
					}
					tx.commit();
					
					if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_HTML)) {
						Map<String, String> replacements = new HashMap<String, String>();
						
						String actionTemplate = Paths.TEMPLATEPATH + "/savePartOfGroupAction.html";
						StringStringPair kvpClientID = new StringStringPair("GROUPID", group.getId() + "");
						replacements.put("GROUPID", group.getId() + "");
						
						replacements.put("NAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_NAME, PARAM_NAME, group.getName(), actionTemplate, kvpClientID));
						
						replacements.put("USERRIGHTS_LIST", createUserRightsList(group));
						
						replacements.put("GROUP_TOKEN", group.getToken().toString());
						
						if (group.getParentGroup() != null) {
							replacements.put("PARENTGROUP_NAME", group.getParentGroup().getName());
						} else {
							replacements.put("PARENTGROUP_NAME", "---");
						}
						
						replacements.put("RIGHTS_ALLOWED_TO_CHANGE", getRightsAllowedToChange(currentUser));
						replacements.put("ACTIONS_EDITGROUP", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/actionsEditGroup.html", null));
						
						replacements.put("HINT_INHERITED", "<p class=\"info\" id=\"inheritedHint\" style=\"display: none;\"><sup>1</sup>Inherited groups are set by a parent group and cannot be edited within this group</p>");
						
						writer.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableGroupInfo.html", replacements));
					} else if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
						writer.print(DtoFactory.getInstance().createDto(group).getJSON());
					}
				} finally {
					hibSess.close();
				}
			}
		}
	}
	
	/**
	 * @param currentUser
	 * @return
	 */
	private String getRightsAllowedToChange(final SystemUser currentUser) {
		UserRights[] effectiveRights = AuthUtil.getEffectiveRights(currentUser);
		StringBuilder builder = new StringBuilder();
		
		for (UserRights right : effectiveRights) {
			builder.append("|| input.val() == '" + right.id() + "'");
		}
		return builder.toString();
	}
	
	/**
	 * @param group
	 * @param currentUser
	 * @return
	 */
	private String createUserRightsList(final UserGroup group) {
		StringBuilder list = new StringBuilder();
		list.append("<ul>");
		Set<UserRight> groupRights = group.getUserRights();
		UserGroup parentGroup = group.getParentGroup();
		while (parentGroup != null) {
			for (UserRight r : parentGroup.getUserRights()) {
				r.setInherited(true);
				groupRights.add(r);
			}
			parentGroup = parentGroup.getParentGroup();
		}
		
		for (UserRights rightEnum : UserRights.values()) {
			if (!rightEnum.internal()) {
				boolean inherited = false;
				boolean checked = false;
				for (UserRight r : groupRights) {
					if (r.getName().equals(rightEnum.id())) {
						checked = true;
						if (r.isInherited()) {
							inherited = true;
						}
						break;
					}
				}
				list.append("<li class=\"rightItem disabled" + (inherited ? " inherited" : "") + "\"><input type=\"radio\" name=\"groupRight\" id=\"ur_" + rightEnum.id() + "\" value=\"" + rightEnum.id() + "\" disabled=\"disabled\"");
				if (checked) {
					list.append(" checked=\"checked\"");
				}
				list.append(" />");
				list.append(rightEnum.decription());
				if (inherited) {
					list.append("(inherited<sup>1</sup>)");
				}
				list.append("</li>");
			}
		}
		list.append("</ul>");
		return list.toString();
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#processUpdate(javax
	 * .servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processUpdate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		// TODO handle new invited User
		long groupId = ParamUtil.getSafeLongFromParam(req, PARAM_GROUPID);
		if (groupId > 0) {
			// if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			// differentiate, if edit Group or just sendEmail for Group invitation
			if (ParamUtil.checkAllParamsSet(req, PARAM_MAILS) && !ParamUtil.getSafeParam(req, PARAM_MAILS).equals("")) {
				String mailsGiven = ParamUtil.getSafeParam(req, PARAM_MAILS);
				String[] mails = mailsGiven.split(",");
				String invitationText = ParamUtil.getSafeParam(req, PARAM_MESSAGE);
				// get Group Token
				StringBuilder invitationFormat = new StringBuilder();
				invitationFormat.append("Personal Invitation Message: \"");
				UserGroup group = (UserGroup) hibSess.get(UserGroup.class, groupId);
				String token = group.getToken().toString();
				String registrationLink = getServerBaseUrl() + "/main?page=register&regUserClientToken=" + token;
				Map<String, String> replacements = new HashMap<String, String>();
				replacements.put("INVITATION_LINK", registrationLink);
				replacements.put("CLIENT", group.getClient().getOrganization());
				replacements.put("GROUP", group.getName());
				if (!invitationText.equals(null)) {
					invitationFormat.append(invitationText);
					invitationFormat.append("\" ");
					replacements.put("INVITATION_MESSAGE", invitationFormat.toString());
				}
				// now send E-Mail to every mail provided in "mails"
				final String taskID = "page." + Pages.PAGE_ADMINISTRATE_GROUPS + "." + req.getSession().getId();
				CallBack cb = new CallBack() {
					
					@Override
					public void doCallBack() {
						FeedBackProvider fbp = FeedBackProvider.getInstance();
						fbp.addFeedback(taskID, "The Invitation Mail(s) has(have) been sent.", Status.OK);
					}
					
					@Override
					public void doCallBackFailure(final Exception e) {
						FeedBackProvider fbp = FeedBackProvider.getInstance();
						fbp.addFeedback(taskID, "The E-Mail could not be sent. There was an error.", Status.ERROR);
					}
				};
				String surveyPlatformMail = EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER).toString();
				for (String mail : mails) {
					try {
						EmailManager.getInstance().sendEmail(mail.trim(), null, surveyPlatformMail, "Metior Solutions Survey Platform", "Invitation Surveyplatform", replacements, Paths.TEMPLATEPATH + "/invitationMail.txt", cb);
					} catch (EmailException e) {
						Logger.errUnexpected(e, currentUser.getUserName());
					}
				}
			}
			try {
				Transaction tx = hibSess.beginTransaction();
				
				UserGroup groupToChange = (UserGroup) hibSess.get(UserGroup.class, groupId);
				
				if (AuthUtil.isAllowedToEditGroup(currentUser, groupToChange.getClient(), groupToChange.getId(), hibSess)) {
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_NAME)) {
						String param = ParamUtil.getSafeParam(req, PARAM_NAME);
						groupToChange.setName(param);
					}
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_GROUP_RIGHTS)) {
						String sentRightID = req.getParameter(PARAM_GROUP_RIGHTS);
						Set<UserRight> groupRights = groupToChange.getUserRights();
						Criteria c = hibSess.createCriteria(UserRight.class);
						
						List<UserRight> notSent = new ArrayList<UserRight>();
						for (Object o : c.list()) {
							UserRight right = (UserRight) o;
							boolean sent = false;
							
							if (sentRightID.equals(right.getName())) {
								sent = true;
								if (groupRights.contains(right)) {
									// nüscht
								} else {
									if (AuthUtil.hasRight(currentUser, UserRights.valueOf(sentRightID.toUpperCase()))) {
										groupRights.add(right);
									}
								}
							}
							
							if (!sent) {
								notSent.add(right);
							}
						}
						for (UserRight right : notSent) {
							groupRights.remove(right);
						}
					}
					
					hibSess.save(groupToChange);
					
					tx.commit();
					if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
						resp.getWriter().print(DtoFactory.getInstance().createDto(groupToChange).getJSON());
					} else {
						resp.sendRedirect(getStandardRedirectLocation(req));
					}
				}
			} finally {
				hibSess.close();
			}
		} else {
			super.handlePermissionDenied(req, resp);
		}
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#processDelete(javax
	 * .servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processDelete(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (ParamUtil.checkAllParamsSet(req, PARAM_GROUPS)) {
			
			String taskID = "page." + Pages.PAGE_ADMINISTRATE_GROUPS + "." + req.getSession().getId();
			FeedBackProvider fbp = FeedBackProvider.getInstance();
			
			String groupIDs[] = req.getParameterValues(PARAM_GROUPS);
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			
			try {
				Transaction tx = hibSess.beginTransaction();
				int removedCount = 0;
				for (String groupIDStr : groupIDs) {
					try {
						long groupID = Long.parseLong(groupIDStr);
						UserGroup groupToChange = (UserGroup) hibSess.load(UserGroup.class, groupID);
						UserGroup parentGroup = groupToChange.getParentGroup();
						long parentGroupId = 0;
						if (parentGroup != null) {
							parentGroupId = parentGroup.getId();
						}
						if (AuthUtil.isAllowedToEditGroup(currentUser, groupToChange.getClient(), parentGroupId, hibSess)) {
							int size = groupToChange.getMembers().size();
							if (size == 0) {
								if (groupToChange.getUserRights() != null) {
									groupToChange.getUserRights().clear();
								}
								
								hibSess.delete(groupToChange);
								removedCount++;
								fbp.addFeedback(taskID, "Deleted the group " + groupToChange.getName() + ".", Status.OK);
							} else {
								fbp.addFeedback(taskID, "Cannot delete the group " + groupToChange.getName() + " as it still has existing members. Regroup all " + size + " members into another group before you delete this group.", Status.ERROR);
							}
						} else {
							fbp.addFeedback(taskID, "You do not have sufficient rights to delete the group " + groupToChange.getName() + ".", Status.ERROR);
							
						}
					} catch (NumberFormatException nfe) {
						// ignore those ids which cannot converted to
						// integer
					}
				}
				tx.commit();
				fbp.setMessage(taskID, removedCount + " Groups deleted.", currentUser.getActualUserName());
				fbp.finishTask(taskID);
			} catch (Exception e) {
				Logger.errUnexpected(e, currentUser.getUserName());
				resp.getWriter().write("Error: " + e);
				fbp.setMessage(taskID, "No Groups deleted. Following error occured: " + e.getMessage(), currentUser.getActualUserName());
				fbp.finishTask(taskID, true);
			} finally {
				hibSess.close();
			}
			
		} else {
			super.handlePermissionDenied(req, resp);
		}
	}
	
	private String getServerBaseUrl() {
		return (String) EnvironmentConfiguration.getConfiguration(ConfigID.HOST) + (String) EnvironmentConfiguration.getConfiguration(ConfigID.URLBASE);
	}
}
