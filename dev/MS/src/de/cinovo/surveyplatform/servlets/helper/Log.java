/**
 * 
 */
package de.cinovo.surveyplatform.servlets.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.StreamUtil;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class Log extends HttpServlet {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 6542947630920137376L;
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (AuthUtil.hasRight(AuthUtil.checkAuth(req), UserRights.ADMINISTRATOR)) {
			File fileToRead = null;
			if (ParamUtil.checkAllParamsSet(req, "err")) {
				fileToRead = new File("logs/log_error.log");
			} else if (ParamUtil.checkAllParamsSet(req, "out")) {
				fileToRead = new File("logs/log_out.log");
			}
			if (fileToRead != null) {
				if (fileToRead.exists()) {
					try {
						FileInputStream fr = new FileInputStream(fileToRead);
						resp.getWriter().print("<html><body>" + StreamUtil.inputStreamToString(fr).replace("\n", "<br>") + "</body></html>");
						fr.close();
					} catch (Exception ex) {
						resp.getWriter().write("Could not read the file: " + fileToRead.getAbsolutePath());
						ex.printStackTrace(resp.getWriter());
					}
				} else {
					resp.getWriter().write("The file " + fileToRead.getAbsolutePath() + " does not exist!");
				}
			} else {
				resp.sendRedirect(EnvironmentConfiguration.getHostAndBase());
			}
		} else {
			resp.sendRedirect(EnvironmentConfiguration.getHostAndBase());
		}
	}
	
}
