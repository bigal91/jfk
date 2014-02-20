package de.cinovo.surveyplatform.servlets;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.SessionRequestQueue;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserStatus;
import de.cinovo.surveyplatform.ui.pages.LoginContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.HyperLinkUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.SessionManager;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class LoginServlet extends HttpServlet {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 4226443826670143506L;
	
	private static final String SESSION_LOGIN_REQUEST_QUEUE = "loginRequestListBefore";
	
	private static final String SESSION_BOT = "sessionBot";
	
	
	// private static Map<String, SecurityToken> tokens;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		
		int messageFlag = 0;
		
		String queryString = EnvironmentConfiguration.getUrlBase() + "/main" + (req.getQueryString() == null ? "" : ("?" + req.getQueryString()));
		
		if (isHumanRequest(req)) {
			// this is not a bot, its a human, proceed
			if (EnvironmentConfiguration.isOfflineMode()) {
				if (ParamUtil.checkAllParamsSet(req, "uname")) {
					try {
						String userNameParam = req.getParameter("uname");
						
						Session hibSess = HibernateUtil.getSessionFactory().openSession();
						try {
							Transaction tx = hibSess.beginTransaction();
							SystemUser loggedUser = null;
							Criteria criteria = hibSess.createCriteria(SystemUser.class);
							criteria.add(Restrictions.eq("userName", "offline_user"));
							loggedUser = (SystemUser) criteria.uniqueResult();
							loggedUser.setAlias(userNameParam.toLowerCase());
							
							if (loggedUser.getUserStatus().ordinal() >= de.cinovo.surveyplatform.model.UserStatus.Disabled.ordinal()) {
								messageFlag |= LoginContainer.ERR_ACCOUNT_LOCKED;
							} else if (loggedUser.getUserStatus().equals(UserStatus.Register_Pending)) {
								messageFlag |= LoginContainer.ERR_REGISTRATION_PENDING;
								
							} else if (loggedUser.getUserStatus().equals(UserStatus.Active)) {
								req.getSession().setAttribute(Constants.ATTR_AUTH_AUTHENTICATED, true);
								req.getSession().setAttribute(Constants.ATTR_AUTH_USER, loggedUser);
								req.getSession().setAttribute(Constants.ATTR_LASTLOGIN, loggedUser.getLastLogin());
								
								loggedUser.setLastLogin(new Date());
								SessionManager.getInstance().register(loggedUser.getUserName(), req.getSession());
								Logger.info("Login: " + loggedUser.getActualUserName());
								Logger.logUserActivity("\r\n\r\nLogin\r\n", loggedUser.getActualUserName());
								
							}
							
							if (messageFlag != 0) {
								queryString = HyperLinkUtil.setParamInRequest(queryString, new StringStringPair(Constants.FLAG_MESSAGES_LOGIN_PARAM, messageFlag + ""));
							}
							tx.commit();
							
						} finally {
							hibSess.close();
						}
						
						resp.sendRedirect(EnvironmentConfiguration.getHostAndBase() + HyperLinkUtil.encodeURLWithSession(resp, queryString));
						
					} catch (Exception e) {
						Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
						resp.getWriter().write("Error: " + e);
					}
				} else {
					resp.getWriter().write("");
				}
			} else {
				if (ParamUtil.checkAllParamsSet(req, "uname", "pword")) {
					try {
						String userNameParam = req.getParameter("uname");
						boolean impersonate = userNameParam.startsWith("@");
						
						Session hibSess = HibernateUtil.getSessionFactory().openSession();
						try {
							Transaction tx = hibSess.beginTransaction();
							List<SystemUser> users = null;
							{
								Criteria criteria = hibSess.createCriteria(SystemUser.class);
								if (impersonate) {
									userNameParam = userNameParam.substring(1);
								}
								criteria.add(Restrictions.eq("userName", userNameParam.toLowerCase()));
								if (impersonate) {
									// this is only possible with admin password
									users = criteria.list();
									
									Criteria getAdminPasswdCriteria = hibSess.createCriteria(SystemUser.class);
									getAdminPasswdCriteria.add(Restrictions.eq("userName", "admin"));
									getAdminPasswdCriteria.add(Restrictions.eq("password", AuthUtil.scramblePassword(req.getParameter("pword"))));
									if (getAdminPasswdCriteria.list().size() <= 0) {
										users.clear();
										impersonate = false;
									}
									
								} else {
									criteria.add(Restrictions.or(Restrictions.eq("password", AuthUtil.scramblePassword(req.getParameter("pword"))), //
											// for backward compatibility check md5
											// hash, too
											// (all new
											// passwords are stored with sha-512)
											Restrictions.eq("password", AuthUtil.md5(AuthUtil.salt(req.getParameter("pword"))))));
									users = criteria.list();
								}
								
							}
							if (users != null) {
								
								if (users.size() > 0) {
									SystemUser loggedUser = users.get(0);
									
									if (loggedUser.getUserStatus().ordinal() >= UserStatus.Disabled.ordinal()) {
										messageFlag |= LoginContainer.ERR_ACCOUNT_LOCKED;
									} else if (loggedUser.getUserStatus().equals(UserStatus.Register_Pending)) {
										messageFlag |= LoginContainer.ERR_REGISTRATION_PENDING;
										
									} else if (loggedUser.getUserStatus().equals(UserStatus.Active)) {
										req.getSession().setAttribute(Constants.ATTR_AUTH_AUTHENTICATED, true);
										req.getSession().setAttribute(Constants.ATTR_AUTH_USER, loggedUser);
										if (impersonate) {
											req.getSession().setAttribute(Constants.ATTR_IMPERSONATE_USER, true);
										}
										req.getSession().setAttribute(Constants.ATTR_LASTLOGIN, loggedUser.getLastLogin());
										
										loggedUser.setLastLogin(new Date());
										SessionManager.getInstance().register(loggedUser.getUserName(), req.getSession());
										Logger.info("Login: " + loggedUser.getActualUserName());
										Logger.logUserActivity("\r\n\r\nLogin\r\n", loggedUser.getActualUserName());
									}
									
								} else {
									messageFlag |= LoginContainer.ERR_WRONG_DATA;
								}
							} else {
								messageFlag |= LoginContainer.ERR_WRONG_DATA;
							}
							if (messageFlag != 0) {
								queryString = HyperLinkUtil.setParamInRequest(queryString, new StringStringPair(Constants.FLAG_MESSAGES_LOGIN_PARAM, messageFlag + ""));
							}
							tx.commit();
							
						} finally {
							hibSess.close();
						}
						
						resp.sendRedirect(EnvironmentConfiguration.getHostAndBase() + HyperLinkUtil.encodeURLWithSession(resp, queryString));
						
					} catch (Exception e) {
						Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
						resp.getWriter().write("Error: " + e);
					}
				} else {
					resp.getWriter().write("");
				}
			}
		} else {
			// TODO HTTP EXCEPTION oder SECURITY EXCEPTION
		}
	}
	
	/**
	 * Checks if Bot Flag is set, and if requests were not sent too fast.
	 * 
	 * @param req - the current request
	 * @return true, if requests seemed to be sent by a human beeing
	 */
	private boolean isHumanRequest(final HttpServletRequest req) {
		if (req.getSession().getAttribute(SESSION_BOT) != null){
			if ((Boolean) req.getSession().getAttribute(SESSION_BOT)) {
				return false;
			}
		}
		// remember last x requests from this Session. If they were sent within time y, set BOT flag
		SessionRequestQueue requests = (SessionRequestQueue) req.getSession().getAttribute(SESSION_LOGIN_REQUEST_QUEUE);
		if (requests == null) {
			int queueMaxLength = 10;
			requests = new SessionRequestQueue(queueMaxLength);
		}
		requests.enqueue(req.getSession().getLastAccessedTime());
		req.getSession().setAttribute(SESSION_LOGIN_REQUEST_QUEUE, requests);
		// Say you have 10 Requests, within (10 / 2) * 1000 = 5000 Milliseconds = 5 Seconds. It must be a bot.
		int queueSize = requests.getRequestStamps().size();
		Long[] queue = requests.getRequestStamps().toArray(new Long[requests.getRequestStamps().size()]);
		Long milliSeconds = (queueSize / 2L) * 1000;
		if (((queue[queueSize - 1] - queue[0]) > milliSeconds) || (queueSize == 1)) {
			// time is long enough between first and last request. This is a human request.
			return true;
		} else {
			// requests were sent too fast, this is a bot.
			return false;
		}
	}
}


