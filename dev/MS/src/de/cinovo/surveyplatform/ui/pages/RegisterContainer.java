/**
 * 
 */
package de.cinovo.surveyplatform.ui.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.mail.EmailException;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.Token;
import de.cinovo.surveyplatform.model.UserStatus;
import de.cinovo.surveyplatform.servlets.dal.SystemUserDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.EmailManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;

/**
 * Copyright 2013 Cinovo AG<br><br>
 * @author ablehm
 *
 */
public class RegisterContainer extends AbstractContainer {
	
	private final static String CLIENT_TOKEN = "regUserClientToken";
	// TODO register User by token
	// TODO provide User Information in the link before!
	private final static String ACTIVATION_TOKEN = "activationToken";
	private static final String PARAM_REGISTER_SUCCESS = "register_success";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet.http.HttpServletRequest, java.lang.StringBuilder,
	 * de.cinovo.surveyplatform.model.SystemUser)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		if (ParamUtil.checkAllParamsSet(request, "page") && (request.getAttribute(PARAM_REGISTER_SUCCESS) == null)) {
			String page = ParamUtil.getSafeParam(request, "page");
			if (page.equals("register") && !ParamUtil.checkAllParamsSet(request, ACTIVATION_TOKEN)) {
				Map<String, String> comboReplacements = new HashMap<String, String>();
				List<StringStringPair> options = new ArrayList<StringStringPair>();
				options.add(new StringStringPair(TimeUtil.DATEFORMAT_EN.replace("'", "_"), TimeUtil.DATEFORMAT_EN.replace("'", "")));
				options.add(new StringStringPair(TimeUtil.DATEFORMAT_DE.replace("'", "_"), TimeUtil.DATEFORMAT_DE.replace("'", "")));
				comboReplacements.put("DATEFORMAT_COMBO", HtmlFormUtil.getComboBox(SystemUserDal.PARAM_DATEFORMAT, SystemUserDal.PARAM_DATEFORMAT, options, null));
				
				String[] availableTimeZones = TimeUtil.getAvailableTimeZones();
				options.clear();
				for (String zoneID : availableTimeZones) {
					options.add(new StringStringPair(zoneID, zoneID));
				}
				comboReplacements.put("TIMEZONE_COMBO", HtmlFormUtil.getComboBox(SystemUserDal.PARAM_TIMEZONE, SystemUserDal.PARAM_TIMEZONE, options, null));
				String token = null;
				if (ParamUtil.checkAllParamsSet(request, CLIENT_TOKEN)) {
					token = ParamUtil.getSafeParam(request, CLIENT_TOKEN);
				}
				comboReplacements.put("CLIENT_TOKEN", token);
				content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/choosePaymentPlan.html", comboReplacements));
			} else if (ParamUtil.checkAllParamsSet(request, "page", ACTIVATION_TOKEN)) {
				Session hibSess = null;
				try{
					hibSess = HibernateUtil.getSessionFactory().openSession();
					String tokenValue = request.getParameter(ACTIVATION_TOKEN);
					Transaction tx = hibSess.beginTransaction();
					Criteria criteria = hibSess.createCriteria(Token.class);
					criteria.add(Restrictions.eq("token", tokenValue));
					Token token = (Token) criteria.uniqueResult();
					Criteria userCriteria = hibSess.createCriteria(SystemUser.class);
					userCriteria.add(Restrictions.eq("token", token));
					SystemUser user = (SystemUser) userCriteria.uniqueResult();
					
					if (token == null) {
						content.append("<p style=\"text-align: center;\">Sorry, but the token is not valid anymore. Please resend another Token-Email to change your Password.</p>");
					} else {
						if (token.isValid(token.getValidUntil())) {
							// The user may now login with username and password
							user.setUserStatus(UserStatus.Active);
							user.setToken(null);
							hibSess.save(user);
							hibSess.delete(token);
							Map<String, String> replacements = new HashMap<String, String>();
							replacements.put("LOGIN_LINK", getServerBaseUrl() + "/main?page=login&uname=" + user.getUserName());
							replacements.put("REGISTER_SUCCESS_CONTENT", "<p>Thank you. You can now login with your username and password.</p>");
							content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/register_success.html", replacements));
							
							EmailManager.getInstance().sendEmail(user.getEmail(), user.getUserName(), (String) EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER), "Metior Solutions Survey Platform", "Account Confirmed", replacements, Paths.TEMPLATEPATH + "/registrationConfirmed.txt", null);
						} else {
							content.append("<p style=\"text-align: center;\">Error: The Token does not match, or is not up-to-date anymore. Please register with a new Account (you can use the same data as before)</p>");
						}
					}
					
					tx.commit();
				} catch (EmailException e) {
					Logger.err("Eine Confirmation-Mail konnte nicht gesendet werden.", e);
				} finally{
					if (hibSess != null) {
						hibSess.close();
					}
				}
			}
		} else if (request.getAttribute(PARAM_REGISTER_SUCCESS) != null) {
			// provide content for successfully registered User
			
			// Thank you for your payment. Your transaction has been completed, and a receipt for your purchase has been emailed to you. You
			// may log into your account at www.paypal.com/za to view details of this transaction.
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("REGISTER_SUCCESS_CONTENT", "<p>Your registration has been successfull, please check your E-Mail Inbox.");
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/register_success.html", replacements));
		}
	}
	
	private String getServerBaseUrl() {
		return (String) EnvironmentConfiguration.getConfiguration(ConfigID.HOST) + (String) EnvironmentConfiguration.getConfiguration(ConfigID.URLBASE);
	}
}
