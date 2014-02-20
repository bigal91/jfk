/**
 * 
 */
package de.cinovo.surveyplatform.servlets.helper;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.bootstrap.JettyLauncher;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.TemplateUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class ClearCache extends HttpServlet {
	
	private static final long serialVersionUID = 3743250907576412606L;
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (AuthUtil.hasRight(AuthUtil.checkAuth(req), UserRights.ADMINISTRATOR)) {
			Logger.info("Clearcache requested");
			resp.getWriter().print("Clearing Template cache...");
			TemplateUtil.clearCache();
			resp.getWriter().println("Done.");
			resp.getWriter().print("Reregistering contextsensitive help...");
			JettyLauncher.initContextSensitiveHelp();
			resp.getWriter().println("Done.");
		}
		
	}
	
}
