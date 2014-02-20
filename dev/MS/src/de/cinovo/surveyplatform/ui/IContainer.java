package de.cinovo.surveyplatform.ui;

import javax.servlet.http.HttpServletRequest;

import de.cinovo.surveyplatform.model.SystemUser;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public interface IContainer {
	
	String getContent(HttpServletRequest request, SystemUser currentUser);
	
	/**
	 * The container will put its visual representation (html) in the given
	 * {@link StringBuilder}. This method is meant to be used when building the
	 * page out
	 * of building blocks which also may be nested.
	 * 
	 * @param request The {@link HttpServletRequest} from the servlet
	 * @param content A non null Stringbuilder object to which the container
	 *            will append its content
	 * @param currentUser the current user. <code>null</code> if no user is
	 *            logged on
	 */
	void provideContent(HttpServletRequest request, StringBuilder content, SystemUser currentUser);
}
