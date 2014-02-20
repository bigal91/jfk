/**
 * 
 */
package de.cinovo.surveyplatform.servlets.helper;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.XMLUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class ImportS extends HttpServlet {
	

	/**
	 *
	 */
	private static final long serialVersionUID = 6542947630920137376L;
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (AuthUtil.hasRight(AuthUtil.checkAuth(req), UserRights.ADMINISTRATOR)) {
			if (ParamUtil.checkAllParamsSet(req, "file")) {
				File fileToImport = new File(req.getParameter("file"));
				if (fileToImport.exists()) {
					try {
						Session s = HibernateUtil.getSessionFactory().openSession();
						Transaction t = s.beginTransaction();
						Survey xmlFile2obj = (Survey) XMLUtil.xmlFile2obj(fileToImport, Survey.class);
						s.save(xmlFile2obj);
						t.commit();
						s.close();
						resp.getWriter().write("The survey <b>" + xmlFile2obj.getName() + "</b> has been imported successfully.");
					} catch (Exception ex) {
						resp.getWriter().write("Could not import the file: " + fileToImport.getAbsolutePath());
						ex.printStackTrace(resp.getWriter());
					}
				} else {
					resp.getWriter().write("The file " + fileToImport.getAbsolutePath() + " does not exist!");
				}
			} else {
				resp.sendRedirect(EnvironmentConfiguration.getHostAndBase());
			}
		} else {
			resp.sendRedirect(EnvironmentConfiguration.getHostAndBase());
		}
	}
}
