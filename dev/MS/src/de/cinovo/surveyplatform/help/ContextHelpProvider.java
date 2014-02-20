/**
 *
 */
package de.cinovo.surveyplatform.help;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * This class provides an easy lightweight framework for contextsensitive help.
 * Implemented as singleton.
 * 
 * @author yschubert
 * 
 */
public class ContextHelpProvider {
	
	public static ContextHelpProvider instance;
	
	private Map<String, String> helpRegistry = new HashMap<String, String>();
	
	
	private ContextHelpProvider() {
		// singleton class: use getInstance() instead of instantiating the class
	}
	
	public static ContextHelpProvider getInstance() {
		if (ContextHelpProvider.instance == null) {
			ContextHelpProvider.instance = new ContextHelpProvider();
		}
		return ContextHelpProvider.instance;
	}
	
	/**
	 * Registers help content for a given contextID
	 * 
	 * @param contextID ID of the context. This must be a unique ID.
	 * @param text Content of the Help. This can be any text or HTML
	 */
	public void setHelp(final String contextID, final String text) {
		this.helpRegistry.put(contextID, text);
	}
	
	/**
	 * @param contextID
	 * @return Help content of the given contextID. When no help is available
	 *         for the given ID, an error message is returned.
	 */
	public String getHelp(final String contextID) {
		String help = this.helpRegistry.get(contextID);
		if (help == null) {
			return "<span style=\"color: red;\">!no help available for the topic (contextid:" + contextID + ")!</span>";
		} else {
			return help;
		}
	}
	
	/**
	 * @param contextID ID of the help context
	 * @param toolTip Text for the toolTip of the Link
	 * @param linkText Text of the link. Provide an empty string, if only the
	 *            icon shall be shown
	 * @return HTML Link to open a dialog containing the help with the given
	 *         helpContextID. This link comes with an icon.
	 */
	public String getHelpLink(final String contextID, final String toolTip, final String linkText, final String style) {
		if (this.helpRegistry.containsKey(contextID)) {
			String icon = "gui-icon-";
			if (linkText.isEmpty()) {
				icon += "button-";
			}
			return "<span style=\"vertical-align: middle;" + style + "\"><a style=\"float: none; display: inline-block;\" class=\"" + icon + "\" title=\"" + toolTip + "\" href=\"javascript:openHelp('" + contextID + "');\">" + linkText + "</a></span>";
		}
		return "";
		
	}
}
