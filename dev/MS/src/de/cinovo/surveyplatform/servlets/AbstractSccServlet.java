package de.cinovo.surveyplatform.servlets;

import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.TimeUtil;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public abstract class AbstractSccServlet extends HttpServlet {
	
	private static final long serialVersionUID = 6227251814560547895L;
	
	private static final String URL_CREATE = "/create";
	private static final String URL_RETRIEVE = "/retrieve";
	private static final String URL_UPDATE = "/update";
	private static final String URL_DELETE = "/delete";
	
	
	public enum Method {
		CREATE, RETRIEVE, UPDATE, DELETE
	}
	
	
	/** can be overriden through a child class **/
	public void processCreate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	/** can be overriden through a child class **/
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	/** can be overriden through a child class **/
	public void processUpdate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	/** can be overriden through a child class **/
	public void processDelete(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	/** can be overriden through a child class **/
	public boolean isAccessAllowed(final HttpServletRequest req, final Method method, final SystemUser currentUser) {
		return true;
	}
	
	protected String getStandardRedirectLocation(final HttpServletRequest req) {
		
		Object visitedLink = req.getSession().getAttribute(Constants.ATTR_VISITED_LINK);
		if (visitedLink != null) {
			return (String) visitedLink;
		}
		return EnvironmentConfiguration.getHostAndBase();
	}
	
	@Override
	protected final void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		this.route(req, resp);
	}
	
	@Override
	protected final void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		this.route(req, resp);
	}
	
	private final void route(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		try {
			req.setCharacterEncoding("UTF-8");
			resp.setCharacterEncoding("UTF-8");
			resp.setContentType("text/html");
			
			// disable caching (this is mainly for IE)
			resp.setHeader("Cache-Control", "no-cache");
			String now = TimeUtil.htmlHeaderFormat(Calendar.getInstance().getTime());
			resp.setHeader("Last-Modified", now);
			resp.setHeader("Expires", now);
			resp.setHeader("Etag", UUID.randomUUID().toString());
			
			
			Method method = this.getMethod(req);
			if (this.isAccessAllowed(req, method, AuthUtil.checkAuth(req))) {
				if (EnvironmentConfiguration.isOfflineMode()) {
					if (method == Method.RETRIEVE) {
						this.processRetrieve(req, resp);
					} else {
						resp.sendRedirect(EnvironmentConfiguration.getHostAndBase());
					}
				} else {
					if (method == Method.CREATE) {
						this.processCreate(req, resp);
					} else if (method == Method.RETRIEVE) {
						this.processRetrieve(req, resp);
					} else if (method == Method.UPDATE) {
						this.processUpdate(req, resp);
					} else if (method == Method.DELETE) {
						this.processDelete(req, resp);
					} else {
						resp.sendRedirect(EnvironmentConfiguration.getHostAndBase());
					}
				}
			} else {
				this.handlePermissionDenied(req, resp);
			}
			
		} catch (Exception ex) {
			
			String logInAgainMessage = "";
			if (AuthUtil.checkAuth(req) == null) {
				logInAgainMessage = " click <a href=\"" + (EnvironmentConfiguration.getUrlBase().isEmpty() ? "/" : EnvironmentConfiguration.getUrlBase()) + "\">here</a> try to log in again. If the problem still occurs, ";
			}
			
			// final fallback
			Logger.err("Unexpected Exception", ex);
			resp.getWriter().print("Something has gone wrong: " + ex.getMessage() + ". Please " + logInAgainMessage + "contact your support. Sorry for any inconvenience caused.");
		}
	}
	
	protected void handlePermissionDenied(final HttpServletRequest req, final HttpServletResponse resp) {
		// redirect does not work for ajax calls!
		// resp.sendRedirect(getStandardRedirectLocation(req));
		throw new SecurityException("Permission denied.");
	}
	
	private Method getMethod(final HttpServletRequest req) {
		String servletPath = req.getServletPath();
		if (servletPath.endsWith(AbstractSccServlet.URL_CREATE)) {
			return Method.CREATE;
		} else if (servletPath.endsWith(AbstractSccServlet.URL_RETRIEVE)) {
			return Method.RETRIEVE;
		} else if (servletPath.endsWith(AbstractSccServlet.URL_UPDATE)) {
			return Method.UPDATE;
		} else if (servletPath.endsWith(AbstractSccServlet.URL_DELETE)) {
			return Method.DELETE;
		}
		
		this.handlePermissionDenied(req, null);
		return null;
	}
	
}
