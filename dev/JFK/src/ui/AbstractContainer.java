package ui;

import javax.servlet.http.HttpServletRequest;

import model.User;

import constants.Constants;

/**
 * <br>
 * Each container of the site must extend this class

 * 
 */
public abstract class AbstractContainer implements IContainer {
	
	protected final String getVisitedLink(final HttpServletRequest request) {
		
		Object visitedLink = request.getSession().getAttribute(Constants.ATTR_VISITED_LINK);
		if (visitedLink != null) {
			return (String) visitedLink;
		}
		// TODO "getHostAndBase()"
		return "/";
	}
	
	@Override
	public String getContent(final HttpServletRequest request, final User currentUser) {
		StringBuilder content = new StringBuilder();
		this.provideContent(request, content, currentUser);
		return content.toString();
	}
	
	@Override
	public abstract void provideContent(HttpServletRequest request, StringBuilder content, User currentUser);
}
