/**
 *
 */
package de.cinovo.surveyplatform.servlets.dal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.PaymentModel;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.Token;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.UserStatus;
import de.cinovo.surveyplatform.model.factory.DtoFactory;
import de.cinovo.surveyplatform.model.factory.MasterDataFactory;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.servlets.MainEntryPoint;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.EmailManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class ClientDal extends AbstractSccServlet {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public static final String PARAM_CLIENTID = "clientID";
	public static final String PARAM_ORGANIZATION = "organization";
	public static final String PARAM_ACCOUNT = "account";
	
	private static final String PARAM_REG_USERNAME = "regUserName";
	private static final String PARAM_REG_USERPASSWORD = "regUserPassword";
	private static final String PARAM_REG_USERPASSWORD_REPEAT = "regUserPasswordRepeat";
	private static final String PARAM_REG_USEREMAIL = "regUserEmail";
	private static final String PARAM_REG_USERTITLE = "regUserTitle";
	private static final String PARAM_REG_USERFORENAME = "regUserForename";
	private static final String PARAM_REG_USERSURNAME = "regUserSurname";
	private static final String PARAM_REG_USER_CLIENTTOKEN = "regUserClientToken";
	private static final String PARAM_REG_USERADRESS = "regUserAdress";
	private static final String PARAM_REG_USERTELEPHONE = "regUserTelephone";
	private static final String PARAM_OTHERDATA = "otherData";
	private static final String PARAM_REG_TIMEZONE = "timeZone";
	private static final String PARAM_REG_DATEFORMAT = "dateFormat";
	
	private int hoursTokenCanLast = 2;
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.servlets.AbstractSccServlet#processRetrieve( javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
			if (ParamUtil.checkAllParamsSet(req, PARAM_CLIENTID)) {
				
				long clientId = ParamUtil.getSafeLongFromParam(req, PARAM_CLIENTID);
				if (clientId > 0) {
					
					PrintWriter writer = resp.getWriter();
					Session hibSess = HibernateUtil.getSessionFactory().openSession();
					try {
						Transaction tx = hibSess.beginTransaction();
						Client client = (Client) hibSess.load(Client.class, clientId);
						
						if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_HTML)) {
							Map<String, String> replacements = new HashMap<String, String>();
							
							replacements.put("LOGO", PartsUtil.getClientLogo(client, 2));
							
							String actionTemplate = Paths.TEMPLATEPATH + "/savePartOfClientAction.html";
							StringStringPair kvpClientID = new StringStringPair("CLIENTID", client.getId() + "");
							
							replacements.put("ORGANISATION_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_ORGANIZATION, PARAM_ORGANIZATION, client.getOrganization(), actionTemplate, kvpClientID));
							if (client.getInternal()) {
								replacements.put("ACCOUNT_COMBO", "Internal");
							} else {
								List<StringStringPair> options = new ArrayList<StringStringPair>();
								options.add(new StringStringPair(PaymentModel.Trial.toString(), PaymentModel.Trial.toString()));
								options.add(new StringStringPair(PaymentModel.Monthly.toString(), PaymentModel.Monthly.toString()));
								options.add(new StringStringPair(PaymentModel.Yearly.toString(), PaymentModel.Yearly.toString()));
								options.add(new StringStringPair(PaymentModel.NONE.toString(), PaymentModel.NONE.toString()));
								replacements.put("ACCOUNT_COMBO", HtmlFormUtil.getDynamicEditableComboBox(PARAM_ACCOUNT + "editCombo", PARAM_ACCOUNT, options, client.getPaymentModel().toString(), actionTemplate, kvpClientID));
							}
							
							replacements.put("ACTIONS_EDITCLIENT", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/actionsEditClient.html", null));
							
							writer.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableClientInfo.html", replacements));
						} else if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
							writer.print(DtoFactory.getInstance().createDto(client).getJSON());
						}
						
						tx.commit();
					} finally {
						hibSess.close();
					}
					
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.servlets.AbstractSccServlet#processCreate(javax .servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processCreate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		boolean validRegistration = false;
		try {
			if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
				if (ParamUtil.checkAllParamsSet(req, PARAM_ORGANIZATION)) {
					String organization = ParamUtil.getSafeParam(req, PARAM_ORGANIZATION);
					PaymentModel accountType = getPaymentModel(ParamUtil.getSafeParam(req, PARAM_ACCOUNT));
					
					String taskID = "page." + Pages.PAGE_ADMINISTRATE_CLIENTS + "." + req.getSession().getId();
					FeedBackProvider fbp = FeedBackProvider.getInstance();
					
					boolean allOkay = false;
					
					// check organization
					if ((organization == null) || organization.trim().isEmpty()) {
						fbp.addFeedback(taskID, "Please enter a non empty organization name.", Status.ERROR);
					} else {
						// check uniqueness
						Criteria c = hibSess.createCriteria(Client.class);
						c.add(Restrictions.ilike("organization", organization, MatchMode.EXACT));
						Object result = c.uniqueResult();
						if (result == null) {
							allOkay = true;
						} else {
							fbp.addFeedback(taskID, "There is already an organization with this name: " + organization, Status.ERROR);
						}
					}
					if (allOkay) {
						try {
							UserGroup resultGroup = MasterDataFactory.getInstance().createClientAndStandardGroups(organization, accountType, hibSess);
							fbp.setMessage(taskID, "Client " + organization + " successfully created.", currentUser.getActualUserName());
							if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
								resp.getWriter().print(DtoFactory.getInstance().createDto(resultGroup.getClient()).getJSON());
							}
							fbp.finishTask(taskID);
						} catch (Exception e) {
							Logger.err("Client konnte nicht angelegt werden: " + organization, e);
							if ((e instanceof ConstraintViolationException) || (e instanceof NonUniqueObjectException)) {
								fbp.setMessage(taskID, "Could not create client!", currentUser.getActualUserName());
								fbp.addFeedback(taskID, "There is already an organization with this name: " + organization, Status.ERROR);
							} else {
								fbp.setMessage(taskID, "Could not create client!", currentUser.getActualUserName());
								fbp.addFeedback(taskID, "Unexpected Error: " + e.getMessage(), Status.ERROR);
							}
							fbp.finishTask(taskID, true);
						}
						
					} else {
						fbp.setMessage(taskID, "Could not create client!", currentUser.getActualUserName());
						fbp.finishTask(taskID, true);
					}
					
				}
				
			}
			if (ParamUtil.checkAllParamsSet(req, PARAM_REG_USERNAME, PARAM_REG_USERPASSWORD, PARAM_REG_USERPASSWORD_REPEAT, PARAM_REG_USEREMAIL, PARAM_REG_USERFORENAME, PARAM_REG_USERSURNAME)) {
				// Register new User
				// test if bot or human
				if (isHumanRequest(req)) {
					String regUserName = ParamUtil.getSafeParam(req, PARAM_REG_USERNAME);
					String regUserPassword = ParamUtil.getSafeParam(req, PARAM_REG_USERPASSWORD);
					String regUserPasswordRepeat = ParamUtil.getSafeParam(req, PARAM_REG_USERPASSWORD_REPEAT);
					String regUserEMail = ParamUtil.getSafeParam(req, PARAM_REG_USEREMAIL);
					String regUserTitle = ParamUtil.getSafeParam(req, PARAM_REG_USERTITLE);
					String regUserForeName = ParamUtil.getSafeParam(req, PARAM_REG_USERFORENAME);
					String regUserSurName = ParamUtil.getSafeParam(req, PARAM_REG_USERSURNAME);
					String regUserAdress = ParamUtil.getSafeParam(req, PARAM_REG_USERADRESS);
					String regUserTelephone = ParamUtil.getSafeParam(req, PARAM_REG_USERTELEPHONE);
					String timeZone = ParamUtil.getSafeParam(req, PARAM_REG_TIMEZONE);
					String dateFormat = ParamUtil.getSafeParam(req, PARAM_REG_DATEFORMAT);
					String password = null;
					if (regUserPassword.equals(regUserPasswordRepeat)) {
						password = regUserPassword;
					}
					SystemUser user = null;
					// create token entity in database
					Token token = Token.createToken(7, null);
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.HOUR, hoursTokenCanLast);
					token.setValidUntil(cal.getTimeInMillis());
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_REG_USER_CLIENTTOKEN) && !ParamUtil.getSafeParam(req, PARAM_REG_USER_CLIENTTOKEN).equals("")) {
						String tokenReceived = ParamUtil.getSafeParam(req, PARAM_REG_USER_CLIENTTOKEN);
						UserGroup targetGroup = getGroupFromToken(tokenReceived, hibSess);
						if (targetGroup != null) {
							user = SurveyElementFactory.getInstance().createSystemUser(regUserName, regUserEMail, password, targetGroup);
							if (SystemUser.isUserUnique(user, hibSess)) {
								// Assign new User to existing Client
								Transaction tx = hibSess.beginTransaction();
								user.setAddress(regUserAdress);
								user.setTitle(regUserTitle);
								user.setFirstName(regUserForeName);
								user.setLastName(regUserSurName);
								user.setPhone(regUserTelephone);
								user.setTimeZoneID(timeZone);
								user.setTimeFormat(dateFormat.replace("_", "'"));
								token.setUserName(user.getUserName());
								user.setToken(token);
								hibSess.save(user);
								tx.commit();
								validRegistration = true;
							} else {
								// User already exists. Please chose another name
								validRegistration = false;
							}
						} else {
							// Invalid Token for Group (either too old, or just invalid)
							validRegistration = false;
						}
						
					} else {
						// Create new Default Client and groups for User (with UserName)
						UserGroup resultGroup = MasterDataFactory.getInstance().createClientAndStandardGroups(regUserName, PaymentModel.Trial, hibSess);
						
						// Create SystemUser and assign to this group
						
						if (password != null) {
							user = SurveyElementFactory.getInstance().createSystemUser(regUserName, regUserEMail, password, resultGroup);
							if (SystemUser.isUserUnique(user, hibSess)) {
								Transaction tx = hibSess.beginTransaction();
								user.setAddress(regUserAdress);
								user.setTitle(regUserTitle);
								user.setFirstName(regUserForeName);
								user.setLastName(regUserSurName);
								user.setPhone(regUserTelephone);
								user.setTimeZoneID(timeZone);
								user.setTimeFormat(dateFormat.replace("_", "'"));
								user.setUserStatus(UserStatus.Disabled);
								token.setUserName(user.getUserName());
								user.setToken(token);
								hibSess.save(user);
								tx.commit();
								validRegistration = true;
							} else {
								// User already exists. Please chose another name
								validRegistration = false;
							}
							
						} else {
							// Passwords are not equal!
							Logger.err("The Passwords a new User tried to provide, are not Equal.");
						}
					}
					// After registering a user (in any way) send him an activation-Mail
					
					Map<String, String> replacements = new HashMap<String, String>();
					replacements.put("ACTIVATION_LINK", getServerBaseUrl() + "/main?page=register&activationToken=" + token.getToken());
					replacements.put("HOURS", String.valueOf(hoursTokenCanLast));
					try {
						EmailManager.getInstance().sendEmail(user.getEmail(), user.getUserName(), (String) EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER), "Metior Solutions Survey Platform", "Account Activation", replacements, Paths.TEMPLATEPATH + "/activationMail.txt", null);
					} catch (Exception e) {
						Logger.err("Could not send email to: " + user.getEmail(), e);
					}
				} else {
					// a bot was sending the last Request, ignore it
					// reset the "requestBefore" value with the lastAccessedTime
					req.getSession().setAttribute(MainEntryPoint.SESSION_REQUEST_BEFORE, req.getSession().getLastAccessedTime());
					// also set Bot-Flag
					req.getSession().setAttribute(MainEntryPoint.SESSION_BOT, true);
				}
			}
		} catch (Exception e) {
			System.out.println("Invalid Registration. Registration: Failed.");
			validRegistration = false;
		} finally {
			hibSess.close();
		}
		
		if (!ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON) && !ParamUtil.checkAllParamsSet(req, PARAM_REG_USERNAME)) {
			resp.sendRedirect(getStandardRedirectLocation(req));
		} else if (!validRegistration) {
			resp.getWriter().write("<p>Invalid Registration. Maybe the Registration failed. Please contact the support.</p>");
		} else {
			resp.sendRedirect(getServerBaseUrl() + "/main?page=register_success");
		}
	}
	
	private PaymentModel getPaymentModel(final String modelName) {
		try {
			return PaymentModel.valueOf(modelName);
		} catch (Exception e) {
			// fallback: use trial account
			return PaymentModel.Trial;
		}
	}
	
	/**
	 * Checks if Bot Flag is set, and if requests were not sent too fast.
	 * 
	 * @param req - the current request
	 * @return true, if requests seemed to be sent by a human beeing
	 */
	private boolean isHumanRequest(final HttpServletRequest req) {
		if (req.getSession().getAttribute(MainEntryPoint.SESSION_BOT) != null) {
			if ((Boolean) req.getSession().getAttribute(MainEntryPoint.SESSION_BOT)) {
				return false;
			}
		}
		String otherData = "";
		otherData = ParamUtil.getSafeParam(req, PARAM_OTHERDATA);
		if (otherData != "") {
			return false;
		}
		Long lastRequest = req.getSession().getLastAccessedTime();
		Long requestBefore = (Long) req.getSession().getAttribute(MainEntryPoint.SESSION_REQUEST_BEFORE);
		// assume a human needs 3 (or any) seconds to fill in the formula
		Long seconds = 3000L;
		if (lastRequest < (requestBefore + seconds)) {
			// lastRequest came in under x seconds! This must be a bot.
			return false;
		}
		
		return true;
	}
	
	/**
	 * Gets the UserGroup, that matches for the token in DataBase.<br>
	 * Returns the Group, or null, if there are no matches.<br>
	 * 
	 * @param token - the token to match against tokens in a database
	 * @param hibSess - the current DataBase Session
	 * @return the UserGroup, that matches for the given token; else null
	 */
	private UserGroup getGroupFromToken(final String token, final Session hibSess) {
		Criteria crit = hibSess.createCriteria(UserGroup.class);
		List<?> groupList = crit.list();
		UserGroup resultGroup = null;
		for (Object gr : groupList) {
			UserGroup group = (UserGroup) gr;
			if (group.getToken() != null) {
				if (group.getToken().toString().equals(token)) {
					resultGroup = group;
				}
			}
		}
		return resultGroup;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.servlets.AbstractSccServlet#processUpdate(javax .servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processUpdate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		long clientId = ParamUtil.getSafeLongFromParam(req, PARAM_CLIENTID);
		if (clientId > 0) {
			if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					
					Client clientToChange = (Client) hibSess.get(Client.class, clientId);
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_ORGANIZATION)) {
						String param = ParamUtil.getSafeParam(req, PARAM_ORGANIZATION);
						clientToChange.setOrganization(param);
					}
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_ACCOUNT)) {
						String param = ParamUtil.getSafeParam(req, PARAM_ACCOUNT);
						PaymentModel newModel = null;
						try {
							newModel = PaymentModel.valueOf(param);
						} catch (Exception e) {
							newModel = PaymentModel.Trial;
						}
						
						clientToChange.setPaymentModel(newModel);
					}
					
					hibSess.save(clientToChange);
					
					tx.commit();
					if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
						resp.getWriter().print(DtoFactory.getInstance().createDto(clientToChange).getJSON());
					} else {
						resp.sendRedirect(getStandardRedirectLocation(req));
					}
				} finally {
					hibSess.close();
				}
			} else {
				super.handlePermissionDenied(req, resp);
			}
		} else {
			super.handlePermissionDenied(req, resp);
		}
	}
	
	private String getServerBaseUrl() {
		return (String) EnvironmentConfiguration.getConfiguration(ConfigID.HOST) + (String) EnvironmentConfiguration.getConfiguration(ConfigID.URLBASE);
	}
	
}
