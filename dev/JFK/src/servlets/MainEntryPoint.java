package servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import constants.Constants;
import constants.HttpMethods.Method;
import model.User;
import resources.ResourcePaths;
import ui.Dispatcher;
import util.AuthorizationUtil;
import util.HTMLUtil;
import util.SessionManager;

public class MainEntryPoint  extends HttpServlet {
	
	private static final String DEFAULT_ENCODING = "UTF-8";
	
	private Dispatcher dispatcher = new Dispatcher();
	
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
	
	private void doRequest(HttpServletRequest req, HttpServletResponse resp,
			Method post) throws IOException{
		SessionManager.getInstance().cleanUp();
		
		// set the "RequestBefore" Value for visiting a Container
		// when formula is filled you can access this time to filter BOT-Requests
		
		resp.setContentType("text/html; charset=" + DEFAULT_ENCODING);
		resp.setCharacterEncoding(DEFAULT_ENCODING);
		req.setCharacterEncoding(DEFAULT_ENCODING);

		StringBuilder sb = new StringBuilder();
		
		req.getSession().setAttribute(Constants.ATTR_VISITED_LINK, "/" + req.getRequestURI() + (req.getQueryString() == null ? "" : ("?" + req.getQueryString())));
		
		sb.append(HTMLUtil.getHTMLFile(ResourcePaths.HTML_FILE_PATH + "/header.html", null));
		// TODO create AUTH UTIL and get the CURRENT_USER
		User currentUser = AuthorizationUtil.checkAuthorization(req);
		sb.append(this.dispatcher.getContent(req, currentUser));
		 
		// TODO
//		Map<String, String> footerReplacements = new HashMap<String, String>();
//		if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
//			footerReplacements.put("VERSION", "Version: " + EnvironmentConfiguration.VERSION + ". ");
//		}
//		footerReplacements.put("LOGGED_IN_AS", currentUser == null ? "" : ("Logged in as: " + currentUser.getActualUserName()));
		
		// TODO can add footer Replacements instead of null later
		sb.append(HTMLUtil.getHTMLFile(ResourcePaths.HTML_FILE_PATH + "/footer.html", null));
		
		
		String output = sb.toString();
		resp.getWriter().print(output);
	}
}
