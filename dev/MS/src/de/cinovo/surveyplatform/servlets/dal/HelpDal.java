/**
 * 
 */
package de.cinovo.surveyplatform.servlets.dal;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.help.ContextHelpProvider;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.util.ParamUtil;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class HelpDal extends AbstractSccServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_HELPID = "helpId";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @seescc.servlets.AbstractSccServlet#processRetrieve(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		if (ParamUtil.checkAllParamsSet(req, PARAM_HELPID)) {
			resp.getWriter().print(ContextHelpProvider.getInstance().getHelp(req.getParameter(PARAM_HELPID)));
		}
	}
	
}
