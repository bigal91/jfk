package de.cinovo.surveyplatform.constants;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class Constants {
	
	public static final int TRIAL_QUESTION_LIMIT = 10;
	public static final int TRIAL_PARTICIPANTS_LIMIT = 100;
	
	public static final String PARAM_JSON = "json";
	public static final String PARAM_HTML = "html";
	
	public static final String DEFAULT_PAGE = Pages.PAGE_STARTPAGE;
	
	public final static String ATTR_AUTH_AUTHENTICATED = "authenticated";
	public final static String ATTR_AUTH_USER = "user";
	public static final String ATTR_IMPERSONATE_USER = "impersonate";
	public static final String ATTR_DEMO_MODE = "demo";
	
	public static final String ATTR_LASTLOGIN = "lastLogin";
	
	public final static String ATTR_VISITED_LINK = "visitedlink";
	
	public final static String ATTR_PARAM_MAP = "parammap";
	
	public static final String ATTR_FORM_DATA_MAP = "formData";
	
	public static final String CSS_HEADMENUITEM_SELECTED = "headMenuItemSelected";
	
	public static final String FLAG_MESSAGES_LOGIN_PARAM = "lmf";
	
	public static final String FLAG_MESSAGES_REGISTER_PARAM = "mf";
	
	public static final String FLAG_MESSAGES_CHANGEPASSWORD_PARAM = "cpmf";
	
	/**
	 * Amount of days the recently period is set (x days from today, where x
	 * must be negative)
	 **/
	public static final int RECENT_PERIOD = -7;
	
	public static final String BREADCRUMB_ELEMENTS = "breadCrumbElements";
	
}
