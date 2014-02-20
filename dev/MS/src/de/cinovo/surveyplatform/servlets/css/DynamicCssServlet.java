package de.cinovo.surveyplatform.servlets.css;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class DynamicCssServlet extends HttpServlet {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, String> replacements = new HashMap<String, String>();
	
	@Override
	public void init() throws ServletException {
		
		StringBuilder iconStyles = new StringBuilder();
		
		File iconsFolder = new File(Paths.WEBCONTENT + "/gfx/" + Paths.ICONS);
		if (iconsFolder.exists() && iconsFolder.isDirectory()) {
			for (File iconFile : iconsFolder.listFiles()) {
				if (iconFile.isFile()) {
					String fileName = iconFile.getName();
					String iconName = fileName.substring(0, fileName.lastIndexOf(".")).replace(".", "").toUpperCase();
					if (iconName.endsWith("_BIG")) {
						iconStyles.append(".gui-icon-" + iconName + "{ background: url(" + Paths.ICONS + "/" + fileName + ") no-repeat; padding-left: 35px; line-height: 32px; height: 32px; }\n\t");
						iconStyles.append(".gui-icon-button-" + iconName + "{ background: url(" + Paths.ICONS + "/" + fileName + ") no-repeat; width: 32px; line-height: 32px; height: 32px; float:left; border: 0px; margin-right: 4px;}\n\t");
					} else {
						iconStyles.append(".gui-icon-" + iconName + "{ background: url(" + Paths.ICONS + "/" + fileName + ") no-repeat; padding-left: 19px; line-height: 16px; height: 16px; }\n\t");
						iconStyles.append(".gui-icon-button-" + iconName + "{ background: url(" + Paths.ICONS + "/" + fileName + ") no-repeat; width: 22px; line-height: 22px; height: 22px; float:left; border: 0px; margin-right: 2px;}\n\t");
					}
				}
			}
		}
		
		replacements.put("ICONS", iconStyles.toString());
	}
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/css");
		String cssFile = req.getRequestURI().substring(EnvironmentConfiguration.getUrlBase().length());
		
		SystemUser u = AuthUtil.checkAuth(req);
		
		if (u == null) {
			replacements.put("LOGGEDSTATE", "display: none !important;");
		}
		// resp.getWriter().print(c.compress(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + cssFile, replacements)));
		resp.getWriter().print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + cssFile, replacements));
	}
	
}
