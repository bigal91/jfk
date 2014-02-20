package de.cinovo.surveyplatform.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.HttpMethods.Method;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.ui.pages.PageDispatcher;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.SessionManager;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class MainEntryPoint extends HttpServlet {
	
	private static final long serialVersionUID = 6853928598219429669L;
	
	private PageDispatcher dispatcher = new PageDispatcher();
	
	private static final String DEFAULT_ENCODING = "UTF-8";
	
	public static final String SESSION_REQUEST_BEFORE = "requestBefore";
	
	public static final String SESSION_BOT = "sessionBot";
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		// set the map only in the doGet toprevent blowing up the memory from
		// posted form data!
		req.getSession().setAttribute(Constants.ATTR_PARAM_MAP, req.getParameterMap());

		this.doRequest(req, resp, Method.GET);
	}
	
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		this.doRequest(req, resp, Method.POST);
	}
	
	private void doRequest(final HttpServletRequest req, final HttpServletResponse resp, final Method method) throws IOException {
		SessionManager.getInstance().cleanUp();
		
		// set the "RequestBefore" Value for visiting a Container
		// when formula is filled you can access this time to filter BOT-Requests
		req.getSession().setAttribute(SESSION_REQUEST_BEFORE, req.getSession().getLastAccessedTime());
		
		resp.setContentType("text/html; charset=" + DEFAULT_ENCODING);
		resp.setCharacterEncoding(DEFAULT_ENCODING);
		req.setCharacterEncoding(DEFAULT_ENCODING);
		SystemUser currentUser = AuthUtil.checkAuth(req);
		StringBuilder sb = new StringBuilder();
		// PrintWriter out = resp.getWriter();
		
		// provide easy access to the currently visited link
		req.getSession().setAttribute(Constants.ATTR_VISITED_LINK, EnvironmentConfiguration.getHostAndBase() + req.getRequestURI() + (req.getQueryString() == null ? "" : ("?" + req.getQueryString())));
		
		sb.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/header.html", null));
		sb.append(this.dispatcher.getContent(req, currentUser));
		
		Map<String, String> footerReplacements = new HashMap<String, String>();
		if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
			footerReplacements.put("VERSION", "Version: " + EnvironmentConfiguration.VERSION + ". ");
		}
		footerReplacements.put("LOGGED_IN_AS", currentUser == null ? "" : ("Logged in as: " + currentUser.getActualUserName()));
		sb.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/footer.html", footerReplacements));
		
		
		String output = sb.toString();
		resp.getWriter().print(output);
		
	}
}
