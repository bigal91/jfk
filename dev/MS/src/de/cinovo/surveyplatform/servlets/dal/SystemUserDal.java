package de.cinovo.surveyplatform.servlets.dal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.mail.EmailException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.UserStatus;
import de.cinovo.surveyplatform.model.factory.DtoFactory;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.EmailManager;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.HyperLinkUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;
import de.cinovo.surveyplatform.util.TreeUtil;
import de.cinovo.surveyplatform.util.TreeUtil.TreeNode;

/**
 *
 * Copyright 2010 Cinovo AG<br>
 * <br>
 *
 * @author yschubert
 *
 */
public class SystemUserDal extends AbstractSccServlet {
	
	/**
	 *
	 */
	public static final String PARAM_MEMBER_SHIPS = "userGroups[]";
	
	public static final String PARAM_PHONE = "phone";
	
	public static final String PARAM_ADDRESS = "address";
	
	public static final String PARAM_LAST_NAME = "lastName";
	
	public static final String PARAM_FIRST_NAME = "firstName";
	
	public static final String PARAM_TITLE = "title";
	
	public static final String PARAM_EMAIL = "email";
	
	private static final String PARAM_USERNAME = "userName";
	
	private static final String PARAM_USERID = "userID";
	
	private static final String PARAM_STATE = "userStatus";
	
	private static final String PARAM_SENDCREATEDUSERMAIL = "sendUserCreatedMail";
	
	private static final String PARAM_PASSWORDOLD = "passwordOld";
	private static final String PARAM_PASSWORD1 = "password1";
	private static final String PARAM_PASSWORD2 = "password2";
	
	public static final String PARAM_TIMEZONE = "timeZone";
	
	public static final String PARAM_DATEFORMAT = "dateFormat";
	
	private static final long serialVersionUID = -3625470421106761561L;
	
	public static final int MSG_REGISTRATION_SUCCESS = 0x1;
	
	public static final int ERR_PASSWORDS_NOT_EQUAL = 0x2;
	
	public static final int ERR_MISSING_USERNAME = 0x4;
	
	public static final int ERR_MISSING_PASSWORD = 0x8;
	
	public static final int ERR_MISSING_EMAIL = 0x10;
	
	public static final int ERR_USER_ALREADY_EXISTS = 0x20;
	
	public static final int ERR_USERNAME_ILLEGAL = 0x40;
	
	public static final int ERR_NO_GROUP = 0x80;
	
	private static final String METHOD_GETID = "getId";
	private static final String METHOD_GETPARENTGROUP = "getParentGroup";
	
	private static final String PARAM_CHECKUSERNAME = "checkName";
	
	private static final String PARAM_TYPED_NAME = "typedName";
	
	
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
		
		if (method.equals(Method.UPDATE)) {
			// every user is allowed to update his data
			String id = req.getParameter(PARAM_USERID);
			if ((id != null) && id.equals(currentUser.getUserName())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void processCreate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		String userName = ParamUtil.getSafeParam(req, PARAM_USERNAME);
		String password = ParamUtil.getSafeParam(req, PARAM_PASSWORD1);
		String password2 = ParamUtil.getSafeParam(req, PARAM_PASSWORD2);
		String email = ParamUtil.getSafeParam(req, PARAM_EMAIL);
		String title = ParamUtil.getSafeParam(req, PARAM_TITLE);
		String firstName = ParamUtil.getSafeParam(req, PARAM_FIRST_NAME);
		String lastName = ParamUtil.getSafeParam(req, PARAM_LAST_NAME);
		String address = ParamUtil.getSafeParam(req, PARAM_ADDRESS);
		String phone = ParamUtil.getSafeParam(req, PARAM_PHONE);
		Boolean sendCreatedMail = false;
		if (ParamUtil.checkAllParamsSet(req, PARAM_SENDCREATEDUSERMAIL)) {
			if (ParamUtil.getSafeParam(req, PARAM_SENDCREATEDUSERMAIL).equals("userCreatedMail")) {
				sendCreatedMail = true;
			}
		}
		
		long groupID = ParamUtil.getSafeLongFromParam(req, UserGroupDal.PARAM_GROUPID);
		
		String timeZone = ParamUtil.getSafeParam(req, PARAM_TIMEZONE);
		String dateFormat = ParamUtil.getSafeParam(req, PARAM_DATEFORMAT);
		
		Map<String, String> formData = new HashMap<String, String>();
		
		int msgFlag = 0;
		
		msgFlag |= checkParam(PARAM_USERNAME, userName, formData, ERR_MISSING_USERNAME);
		msgFlag |= checkParam(PARAM_EMAIL, email, formData, ERR_MISSING_EMAIL);
		msgFlag |= groupID > 0 ? 0 : ERR_NO_GROUP;
		
		if ((password != null) && (password2 != null) && !password.equals("") && !password2.equals("")) {
			if (!password.equals(password2)) {
				msgFlag |= ERR_PASSWORDS_NOT_EQUAL;
			}
		} else {
			msgFlag |= ERR_MISSING_PASSWORD;
		}
		
		msgFlag |= checkUserName(userName);
		
		String taskID = "page." + Pages.PAGE_USERADMIN + "." + req.getSession().getId();
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		
		String actualUserName = currentUser.getActualUserName();
		if (msgFlag == 0) {
			
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Transaction tx = hibSess.beginTransaction();
				
				UserGroup group = (UserGroup) hibSess.get(UserGroup.class, groupID);
				SystemUser user = SurveyElementFactory.getInstance().createSystemUser(userName, email, password, group);
				
				user.setAddress(address);
				user.setTitle(title);
				user.setFirstName(firstName);
				user.setLastName(lastName);
				user.setPhone(phone);
				user.setTimeZoneID(timeZone);
				user.setTimeFormat(dateFormat.replace("_", "'"));
				
				hibSess.save(user);
				tx.commit();
				fbp.setMessage(taskID, "User " + userName + " successfully created.", actualUserName);
				
				msgFlag |= MSG_REGISTRATION_SUCCESS;
				
				if (sendCreatedMail) {
					try {
						sendRegistrationEmail(currentUser, password, user);
					} catch (EmailException ee) {
						Logger.err("Error while sending a registration email to " + user.getEmail(), ee);
						fbp.addFeedback(taskID, "The user has been created but no registration email could be sent. You have to inform the new user about his account data manually.", Status.WARNING);
					}
				}
				
				if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
					resp.getWriter().print(DtoFactory.getInstance().createDto(currentUser, user).getJSON());
				}
				fbp.finishTask(taskID);
				
			} catch (Exception e) {
				Logger.err("Benutzer konnte nicht angelegt werden: " + userName, e);
				if ((e instanceof ConstraintViolationException) || (e instanceof NonUniqueObjectException)) {
					fbp.setMessage(taskID, "Could not create user!", actualUserName);
					fbp.addFeedback(taskID, "The user with the username '" + userName + "' already exists. Please choose another one!", Status.ERROR, actualUserName);
				}
				fbp.finishTask(taskID, true);
			} finally {
				hibSess.close();
			}
			
		} else {
			fbp.setMessage(taskID, "Could not create user!", actualUserName);
			if ((msgFlag & ERR_MISSING_USERNAME) > 0) {
				fbp.addFeedback(taskID, "The field 'Username' is mandatory! Please provide a username.", Status.ERROR, actualUserName);
			}
			if ((msgFlag & ERR_MISSING_PASSWORD) > 0) {
				fbp.addFeedback(taskID, "Please enter a password for the user and confirm the password in the 'Repeat password' field!", Status.ERROR, actualUserName);
			}
			if ((msgFlag & ERR_MISSING_EMAIL) > 0) {
				fbp.addFeedback(taskID, "The field 'Email' is mandatory. Please enter an email address for the user!", Status.ERROR, actualUserName);
			}
			if ((msgFlag & ERR_PASSWORDS_NOT_EQUAL) > 0) {
				fbp.addFeedback(taskID, "Passwords are not equal. You must enter the password for the user twice to prevent typing errors!", Status.ERROR, actualUserName);
			}
			if ((msgFlag & ERR_USERNAME_ILLEGAL) > 0) {
				fbp.addFeedback(taskID, "The name " + userName + " is not a valid username.", Status.ERROR, actualUserName);
			}
			if ((msgFlag & ERR_NO_GROUP) > 0) {
				fbp.addFeedback(taskID, "No group given. You must provide a group to add the user to.", Status.ERROR, actualUserName);
			}
			fbp.finishTask(taskID, true);
		}
		
		if (!ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
			resp.sendRedirect(HyperLinkUtil.setParamInRequest(getStandardRedirectLocation(req), new StringStringPair(Constants.FLAG_MESSAGES_REGISTER_PARAM, msgFlag + "")));
		}
	}
	private int checkUserName(final String userName) {
		if (userName.toLowerCase().matches("[a-z]+[a-z0-9-_+]*")) {
			return 0;
		}
		return ERR_USERNAME_ILLEGAL;
	}
	
	private void sendRegistrationEmail(final SystemUser currentUser, final String password, final SystemUser user) throws EmailException {
		// EMail to created User
		String subject = "Welcome to the Metior Solutions Survey Platform";
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("USERNAME", user.getUserName());
		replacements.put("PASSWORD", password);
		replacements.put("FIRSTNAME", user.getFirstName());
		replacements.put("LASTNAME", user.getLastName());
		replacements.put("TELEPHONE", user.getPhone());
		
		EmailManager.getInstance().sendEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName(), (String) EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER), "Metior Solutions Survey Platform", subject, replacements, Paths.TEMPLATEPATH + "/userCreatedEMail.txt", null);
	}
	
	private int checkParam(final String paramName, final String paramValue, final Map<String, String> formData, final int errorFlag) {
		if ((paramValue != null) && !paramValue.equals("")) {
			formData.put(paramName, paramValue);
		} else {
			return errorFlag;
		}
		return 0;
	}
	
	@Override
	public void processUpdate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		String id = req.getParameter(PARAM_USERID);
		String taskID = "page." + Pages.PAGE_USERADMIN + "." + req.getSession().getId();
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		fbp.beginTask("Update user:" + id, taskID, currentUser.getUserName());
		fbp.setMessage(taskID, "Updating user");
		if ((id != null) && !id.isEmpty() && (currentUser != null)) {
			
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Transaction tx = hibSess.beginTransaction();
				
				SystemUser userToChange = (SystemUser) hibSess.get(SystemUser.class, id);
				
				// password edited by another user
				if (currentUser.getUserName().equals(id)) {
					// password edited by the user itself (here he must confirm
					// with the old password)
					if (ParamUtil.checkAllParamsSet(req, PARAM_PASSWORD1, PARAM_PASSWORD2, PARAM_PASSWORDOLD)) {
						if (userToChange.getPassword().equals(AuthUtil.scramblePassword(req.getParameter(PARAM_PASSWORDOLD)))) {
							if (req.getParameter(PARAM_PASSWORD1).equals(req.getParameter(PARAM_PASSWORD2))) {
								userToChange.setPassword(AuthUtil.scramblePassword(req.getParameter(PARAM_PASSWORD1)));
								
								// log the user out
								req.getSession().invalidate();
							}
						}
					}
				} else {
					if (ParamUtil.checkAllParamsSet(req, PARAM_PASSWORD1, PARAM_PASSWORD2)) {
						if (req.getParameter(PARAM_PASSWORD1).equals(req.getParameter(PARAM_PASSWORD2))) {
							userToChange.setPassword(AuthUtil.scramblePassword(req.getParameter(PARAM_PASSWORD1)));
						}
					}
				}
				
				// email
				if (ParamUtil.checkAllParamsSet(req, PARAM_ADDRESS)) {
					String param = ParamUtil.getSafeParam(req, PARAM_ADDRESS);
					userToChange.setAddress(param);
				}
				if (ParamUtil.checkAllParamsSet(req, PARAM_STATE)) {
					String param = req.getParameter(PARAM_STATE);
					try {
						userToChange.setUserStatus(UserStatus.valueOf(param));
					} catch (Exception e) {
						// paranoia: set user disabled if a false string is
						// provided
						userToChange.setUserStatus(UserStatus.Disabled);
					}
				}
				
				if (ParamUtil.checkAllParamsSet(req, PARAM_EMAIL)) {
					String param = ParamUtil.getSafeParam(req, PARAM_EMAIL);
					userToChange.setEmail(param);
				}
				if (ParamUtil.checkAllParamsSet(req, PARAM_FIRST_NAME)) {
					String param = ParamUtil.getSafeParam(req, PARAM_FIRST_NAME);
					userToChange.setFirstName(param);
				}
				if (ParamUtil.checkAllParamsSet(req, PARAM_LAST_NAME)) {
					String param = ParamUtil.getSafeParam(req, PARAM_LAST_NAME);
					userToChange.setLastName(param);
				}
				if (ParamUtil.checkAllParamsSet(req, PARAM_PHONE)) {
					String param = ParamUtil.getSafeParam(req, PARAM_PHONE);
					userToChange.setPhone(param);
				}
				if (ParamUtil.checkAllParamsSet(req, PARAM_TITLE)) {
					String param = ParamUtil.getSafeParam(req, PARAM_TITLE);
					userToChange.setTitle(param);
				}
				if (ParamUtil.checkAllParamsSet(req, PARAM_TIMEZONE)) {
					String param = ParamUtil.getSafeParam(req, PARAM_TIMEZONE);
					userToChange.setTimeZoneID(param);
				}
				if (ParamUtil.checkAllParamsSet(req, PARAM_DATEFORMAT)) {
					String param = ParamUtil.getSafeParam(req, PARAM_DATEFORMAT);
					userToChange.setTimeFormat(param.replace("_", "'"));
				}
				if (ParamUtil.checkAllParamsSet(req, PARAM_MEMBER_SHIPS)) {
					String groupIDs[] = req.getParameterValues(PARAM_MEMBER_SHIPS);
					Collection<UserGroup> possibleGroups = GroupManager.getVisibleGroups(hibSess, userToChange, currentUser);
					List<UserGroup> notSent = new ArrayList<UserGroup>();
					Set<UserGroup> userGroups = userToChange.getUserGroups();
					for (UserGroup group : possibleGroups) {
						boolean sent = false;
						for (String sentGroupID : groupIDs) {
							if (sentGroupID.equals(String.valueOf(group.getId()))) {
								sent = true;
								if (userGroups.contains(group)) {
									// nüscht
								} else {
									userGroups.add(group);
								}
							}
						}
						if (!sent) {
							notSent.add(group);
						}
					}
					for (UserGroup group : notSent) {
						// if (userGroups.size() > 1) {
						userGroups.remove(group);
						// } else {
						// fbp.addFeedback(taskID, "User " +
						// userToChange.getUserName() +
						// " must remain in at least one group. So I will not remove the group "
						// + group.getName() + " from the user.", Status.ERROR);
						// }
					}
				}
				
				hibSess.save(userToChange);
				if (userToChange.equals(currentUser)) {
					req.getSession().setAttribute(Constants.ATTR_AUTH_USER, userToChange);
				}
				
				tx.commit();
				if (!fbp.getTaskInfo(taskID).hasErrors()) {
					fbp.addFeedback(taskID, "User " + userToChange.getUserName() + " updated.", Status.OK);
				}
				
				fbp.finishTask(taskID);
				if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
					resp.getWriter().print(DtoFactory.getInstance().createDto(currentUser, userToChange).getJSON());
				} else {
					resp.sendRedirect(getStandardRedirectLocation(req));
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
	 * @seescc.servlets.AbstractSccServlet#processRetrieve(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		if (ParamUtil.checkAllParamsSet(req, PARAM_CHECKUSERNAME)) {
			// check UserName Uniqueness for registration
			String typedName = ParamUtil.getSafeParam(req, PARAM_TYPED_NAME);
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				SystemUser user = (SystemUser) hibSess.get(SystemUser.class, typedName.toLowerCase());
				if (user != null) {
					resp.getWriter().write("This Username already exists.");
				}
			} catch (Exception e) {
				Logger.err(e.getMessage());
			} finally {
				hibSess.close();
			}
		}
		
		if (currentUser != null) {
			// retrieve UserData
			if (ParamUtil.checkAllParamsSet(req, PARAM_USERID)) {
				PrintWriter writer = resp.getWriter();
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					SystemUser user = (SystemUser) hibSess.load(SystemUser.class, req.getParameter(PARAM_USERID));
					
					Set<UserGroup> allGroups = (Set<UserGroup>) GroupManager.getVisibleGroups(hibSess, user, currentUser);
					TreeNode<UserGroup> rootNode = TreeUtil.convertTableToTree(allGroups, METHOD_GETID, METHOD_GETPARENTGROUP);
					
					tx.commit();
					if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_HTML)) {
						Map<String, String> replacements = new HashMap<String, String>();
						
						// username is readonly
						replacements.put("USERNAME_TEXTFIELD", user.getUserName());
						
						String actionTemplate = Paths.TEMPLATEPATH + "/savePartOfUserAction.html";
						String actionTemplateForNameAndSurname = Paths.TEMPLATEPATH + "/savePartOfUserActionForNameAndSurname.html";
						String actionTemplateForGroupCheckboxes = Paths.TEMPLATEPATH + "/savePartOfUserCheckboxAction.html";
						
						StringStringPair kvpUserID = new StringStringPair("USERID", user.getUserName());
						replacements.put("USERID", user.getUserName());
						
						replacements.put("TITLE_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_TITLE, PARAM_TITLE, user.getTitle(), actionTemplate, kvpUserID));
						replacements.put("FIRSTNAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_FIRST_NAME, PARAM_FIRST_NAME, user.getFirstName(), actionTemplateForNameAndSurname, kvpUserID));
						replacements.put("LASTNAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_LAST_NAME, PARAM_LAST_NAME, user.getLastName(), actionTemplateForNameAndSurname, kvpUserID));
						replacements.put("EMAIL_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_EMAIL, PARAM_EMAIL, user.getEmail(), actionTemplate, kvpUserID));
						replacements.put("ADDRESS_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_ADDRESS, PARAM_ADDRESS, user.getAddress(), actionTemplate, kvpUserID));
						replacements.put("PHONE_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_PHONE, PARAM_PHONE, user.getPhone(), actionTemplate, kvpUserID));
						List<StringStringPair> options = new ArrayList<StringStringPair>();
						if (user.equals(currentUser) || !AuthUtil.isAllowedToEditSystemUser(currentUser)) {
							replacements.put("STATE_COMBO", user.getUserStatus().toString());
						} else {
							options.add(new StringStringPair(UserStatus.Active.toString(), UserStatus.Active.toString()));
							options.add(new StringStringPair(UserStatus.Disabled.toString(), UserStatus.Disabled.toString()));
							replacements.put("STATE_COMBO", HtmlFormUtil.getDynamicEditableComboBox(SystemUserDal.PARAM_STATE, SystemUserDal.PARAM_STATE, options, user.getUserStatus().toString(), actionTemplate, kvpUserID));
						}
						
						String[] availableTimeZones = TimeUtil.getAvailableTimeZones();
						options.clear();
						for (String zoneID : availableTimeZones) {
							options.add(new StringStringPair(zoneID, zoneID));
						}
						replacements.put("TIMEZONE_COMBO", HtmlFormUtil.getDynamicEditableComboBox(SystemUserDal.PARAM_TIMEZONE + "editCombo", SystemUserDal.PARAM_TIMEZONE, options, user.getTimeZoneID(), actionTemplate, kvpUserID));
						options.clear();
						options.add(new StringStringPair(TimeUtil.DATEFORMAT_EN.replace("'", "_"), TimeUtil.DATEFORMAT_EN.replace("'", "")));
						options.add(new StringStringPair(TimeUtil.DATEFORMAT_DE.replace("'", "_"), TimeUtil.DATEFORMAT_DE.replace("'", "")));
						replacements.put("DATEFORMAT_COMBO", HtmlFormUtil.getDynamicEditableComboBox(SystemUserDal.PARAM_DATEFORMAT + "editCombo", SystemUserDal.PARAM_DATEFORMAT, options, user.getTimeFormat().replace("'", "_"), actionTemplate, kvpUserID));
						
						options.clear();
						
						buildGroupTree(options, rootNode, 0);
						
						List<String> selectedOptions = new ArrayList<String>();
						for (UserGroup group : user.getUserGroups()) {
							selectedOptions.add(group.getId() + "");
						}
						
						String dynamicEditableCheckBoxes = HtmlFormUtil.getDynamicEditableCheckBoxes(PARAM_MEMBER_SHIPS, PARAM_MEMBER_SHIPS, options, selectedOptions, actionTemplateForGroupCheckboxes, kvpUserID);
						replacements.put("GROUPLIST", dynamicEditableCheckBoxes);
						
						writer.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableUserInfo.html", replacements));
					} else if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
						writer.print(DtoFactory.getInstance().createDto(currentUser, user).getJSON());
					}
				} finally {
					hibSess.close();
				}
				
			}
			
		}
	}
	
	private void buildGroupTree(final List<StringStringPair> options, final TreeNode<UserGroup> node, final int depth) {
		Set<TreeNode<UserGroup>> children = node.getChildren();
		if (children != null) {
			for (TreeNode<UserGroup> child : children) {
				options.add(new StringStringPair(child.getData().getId() + "", "<span style=\"margin-left: " + (depth * 20) + "px ;\">" + child.getData().getName() + "</span>"));
				buildGroupTree(options, child, depth + 1);
			}
		}
	}
	
}
