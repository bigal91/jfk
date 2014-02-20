package de.cinovo.surveyplatform.ui.pages;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class LoginContainer extends AbstractContainer {
	
	public static final int ERR_MISSING_USERNAME = 0x1;
	
	public static final int ERR_MISSING_PASSWORD = 0x2;
	
	public static final int ERR_ACCOUNT_LOCKED = 0x4;
	
	public static final int ERR_REGISTRATION_PENDING = 0x8;
	
	public static final int ERR_WRONG_DATA = 0x10;
	
	private static final String PARAM_PRE_USERNAME = "uname";
	
	private static Map<Integer, String> MESSAGES = new HashMap<Integer, String>();
	
	static {
		MESSAGES.put(ERR_ACCOUNT_LOCKED, "Your account is disabled. Please contact the adminstrators!");
		MESSAGES.put(ERR_REGISTRATION_PENDING, "Your account is not yet enabled. Please wait until it has been enabled by an administrator.");
		MESSAGES.put(ERR_MISSING_USERNAME, "Please enter your username!");
		MESSAGES.put(ERR_MISSING_PASSWORD, "Please enter your password!");
		MESSAGES.put(ERR_WRONG_DATA, "Username or password wrong.");
	}
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		
		// content.append(PartsUtil.getPageHeader("Login", "", new String[] {}, true));
		
		Map<String, String> replacements = new HashMap<String, String>();
		if (request.getParameter(Constants.FLAG_MESSAGES_LOGIN_PARAM) != null) {
			int messageFlag = ParamUtil.getSafeIntFromParam(request, Constants.FLAG_MESSAGES_LOGIN_PARAM);
			for (Entry<Integer, String> entry : MESSAGES.entrySet()) {
				if ((messageFlag & entry.getKey()) != 0) {
					content.append(HtmlFormUtil.getErrorMessage(entry.getValue()) + "<br />");
				}
			}
		}
		if (ParamUtil.checkAllParamsSet(request, PARAM_PRE_USERNAME)) {
			String preFillUsername = ParamUtil.getSafeParam(request, PARAM_PRE_USERNAME);
			replacements.put("USERNAME", preFillUsername);
		} else {
			replacements.put("USERNAME", "");
		}
		replacements.put("PASSWORD", "");
		replacements.put("QUERYSTRING", (request.getQueryString() == null ? "" : ("?" + request.getQueryString())).replaceAll("page=logout", ""));
		if (!EnvironmentConfiguration.isOfflineMode()) {
			replacements.put("PASSWORD_FIELD", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/loginFormPassword.html", replacements));
			replacements.put("LOST_PASSWORD_LINK", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/loginFormLostPassword.html", replacements));
		}
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/loginForm.html", replacements));
	}
}
