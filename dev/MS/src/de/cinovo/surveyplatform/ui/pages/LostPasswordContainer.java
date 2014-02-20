/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

import java.util.Calendar;
import java.util.HashMap;
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
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.Token;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.EmailManager;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author ablehm
 *
 */
public class LostPasswordContainer extends AbstractContainer {
	
	private static final String PARAM_TOKEN = "token";
	
	private static final String PARAM_TOKENMAIL = "tokenMail";
	
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		// in case longer
		int hoursTokenCanLast = 1;
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			if (ParamUtil.checkAllParamsSet(request, PARAM_TOKENMAIL)) {
				String userName = request.getParameter(PARAM_TOKENMAIL);
				
				// schauen ob es einen benutzer mit der email gibt
				Transaction tx = hibSess.beginTransaction();
				SystemUser systemUser = new SystemUser();
				systemUser = (SystemUser) hibSess.load(SystemUser.class, userName);
				
				if (systemUser.getEmail() == null) {
					content.append("<p style=\"text-align: center;\"> The UserName you typed in has provided no E-Mail, please check for spelling mistakes</p>");
				} else {
					// create token entity in database
					Token token = Token.createToken(7, null);
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.HOUR, hoursTokenCanLast);
					
					token.setValidUntil(cal.getTimeInMillis());
					token.setUserName(systemUser.getUserName());
					
					hibSess.save(token);
					tx.commit();
					
					Map<String, String> replacements = new HashMap<String, String>();
					replacements.put("TOKENLINK", getVisitedLink(request) + "&token=" + token.getToken());
					try {
						EmailManager.getInstance().sendEmail(systemUser.getEmail(), systemUser.getUserName(), (String) EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER), "Metior Solutions Survey Platform", "Password Reset", replacements, Paths.TEMPLATEPATH + "/userLostPwMail.txt", null);
					}catch (Exception e){
						Logger.err("Could not send email to: " + systemUser.getEmail(), e);
					}
					content.append("The E-Mail has been sent, please check your E-Mail Inbox.");
				}
			} else if (ParamUtil.checkAllParamsSet(request, "newPassword")) {
				String newPassword = request.getParameter("newPassword");
				String tokenValue = request.getParameter(PARAM_TOKEN);
				
				Transaction tx = hibSess.beginTransaction();
				Criteria criteria = hibSess.createCriteria(Token.class);
				criteria.add(Restrictions.eq(PARAM_TOKEN, tokenValue));
				
				Token token = (Token) criteria.uniqueResult();
				// Delete Token
				hibSess.delete(token);
				
				SystemUser user = (SystemUser) hibSess.load(SystemUser.class, token.getUserName());
				
				user.setPassword(AuthUtil.scramblePassword(newPassword));
				tx.commit();
				Logger.logUserActivity("User changed his password.", user.getUserName());
				content.append("<p style=\"text-align: center;\"> Your password has been changed, you may now login with the new password.</p>");
				// Mail to let user know WHAT EXACTLY has been changed
				Map<String, String> replacements = new HashMap<String, String>();
				replacements.put("USERNAME", user.getUserName());
				replacements.put("NEWPASSWORD", request.getParameter("newPassword"));
				try {
					EmailManager.getInstance().sendEmail(user.getEmail(), user.getUserName(), (String) EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER), "Metior Solutions Survey Platform", "Password Changed", replacements, Paths.TEMPLATEPATH + "/changePassword_confirmation.txt", null);
				} catch (EmailException e) {
					Logger.err("Could not send email to: " + user.getEmail(), e);
				}
			} else if (ParamUtil.checkAllParamsSet(request, PARAM_TOKEN)) {
				String tokenValue = request.getParameter(PARAM_TOKEN);
				Transaction tx = hibSess.beginTransaction();
				Criteria criteria = hibSess.createCriteria(Token.class);
				criteria.add(Restrictions.eq(PARAM_TOKEN, tokenValue));
				
				Token token = (Token)criteria.uniqueResult();
				
				tx.commit();
				if (token == null) {
					content.append("<p style=\"text-align: center;\">Sorry, but the token is not valid anymore. Please resend another Token-Email to change your Password.</p>");
				} else {
					if (token.isValid(token.getValidUntil())) {
						// The user may now change his password here
						Map<String, String> replacements = new HashMap<String, String>();
						replacements.put("TOKENVALUE", request.getParameter(PARAM_TOKEN));
						content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/changePassword.html", replacements));
					} else {
						content.append("<p style=\"text-align: center;\">Error: The Token does not match, or is not up-to-date anymore. A token can last " + Integer.toString(hoursTokenCanLast) + " hour(s).</p>");
					}
				}
			}
			else{
				content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/lostPassword.html", null));
				
			}
		} catch (Exception e) {
			content.append("<p style=\"text-align: center; margin: 10px;\"> The UserName you typed in does not exist, please check for spelling mistakes.</p>");
			content.append("<p style=\"text-align: center; margin: 50px;\">You can go back, by clicking <a href=\"?page=lostPassword\">here</a></p>");
			Logger.err(e.toString());
		} finally {
			hibSess.close();
		}
		
	}
	
}
