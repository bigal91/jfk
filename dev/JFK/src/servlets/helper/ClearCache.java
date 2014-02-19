package servlets.helper;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import startup.ServerStartup;
import util.HTMLUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class ClearCache extends HttpServlet {
	
	private static final long serialVersionUID = 3743250907576412606L;
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		// TODO rights
		//if (AuthUtil.hasRight(AuthUtil.checkAuth(req), UserRights.ADMINISTRATOR)) {
			resp.getWriter().print("Clearing Template cache...");
			HTMLUtil.clearCache();
			resp.getWriter().println("Done.");
			resp.getWriter().print("Reregistering contextsensitive help...");
			resp.getWriter().println("Done.");
		//}
		
	}
	
}
