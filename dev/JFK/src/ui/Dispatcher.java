package ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import resources.ResourcePaths;

import util.HTMLUtil;

import constants.Pages;

import model.User;

public class Dispatcher extends AbstractContainer{

	private StartContainer startContainer = new StartContainer();
	
	@Override
	public void provideContent(HttpServletRequest request,
			StringBuilder content, User currentUser) {
		final StringBuilder innerContent = new StringBuilder();
		final Map<String, String> replacements = new HashMap<String, String>();
		String page = request.getParameter(Pages.PAGE_MAIN_PARAM);
		
		try {
			if (page == null){
				if (currentUser == null){
					page = Pages.NEWS;
				} else {
					page = Pages.DEFAULT_PAGE;
				}
			} else {
				if (currentUser == null){
					page = Pages.NEWS;
				}
			}
			
			if (page.equals(Pages.LOGOUT)){
				request.getSession().invalidate();
				page = Pages.NEWS;
				if (currentUser != null) {
					// TODO log: user logged out
				}
				currentUser = null;
			}
			
			if (page.equals(Pages.NEWS)){
				startContainer.provideContent(request, innerContent, currentUser);
			}			
			replacements.put("CONTENT", innerContent.toString());
			// TODO put a nice header instead of "This is the HEADER"
			replacements.put("HEADMENU", HTMLUtil.getHTMLFile(ResourcePaths.HTML_FILE_PATH + "/headerName.html", null));
			
			if (currentUser != null) {
				// TODO replacements.put("LOGOUT_LINK", "<a href=\"?page=logout\">logout</a>");
			}
		}catch (Exception e) {
			replacements.put("CONTENT", "<p style=\"text-align: center;\"> the DISPATCHER caused an Error. (" + new SimpleDateFormat().format(new Date()) + ")<br /><br /></p>");
		}
		
		content.append(HTMLUtil.getHTMLFile(ResourcePaths.HTML_FILE_PATH + "/layout.html", replacements));
		
	}

}
