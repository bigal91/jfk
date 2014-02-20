package de.cinovo.surveyplatform.ui;

import javax.servlet.http.HttpServletRequest;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.model.SystemUser;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * Each container of the site must extend this class
 * 
 * @author yschubert
 * 
 */
public abstract class AbstractContainer implements IContainer {
	
	protected final String getVisitedLink(final HttpServletRequest request) {
		
		Object visitedLink = request.getSession().getAttribute(Constants.ATTR_VISITED_LINK);
		if (visitedLink != null) {
			return (String) visitedLink;
		}
		return EnvironmentConfiguration.getHostAndBase();
	}
	
	@Override
	public String getContent(final HttpServletRequest request, final SystemUser currentUser) {
		StringBuilder content = new StringBuilder();
		this.provideContent(request, content, currentUser);
		return content.toString();
	}
	
	@Override
	public abstract void provideContent(HttpServletRequest request, StringBuilder content, SystemUser currentUser);
}
