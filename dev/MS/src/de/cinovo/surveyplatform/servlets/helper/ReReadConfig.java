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
import de.cinovo.surveyplatform.bootstrap.configuration.DataSourceConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.FeatureToggle;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.Logger;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class ReReadConfig extends HttpServlet {
	
	private static final long serialVersionUID = 3743250907576412606L;
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (AuthUtil.hasRight(AuthUtil.checkAuth(req), UserRights.ADMINISTRATOR)) {
			try {
				Logger.info("Rereading Configuration");
				DataSourceConfiguration.configure(JettyLauncher.datasourceConfigFile, false);
				EnvironmentConfiguration.configure(JettyLauncher.environmentConfigFile);
				FeatureToggle.configure(JettyLauncher.featureConfigFile);
				resp.getWriter().println("Done.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
}
