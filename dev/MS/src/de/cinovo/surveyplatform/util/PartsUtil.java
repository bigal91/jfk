package de.cinovo.surveyplatform.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.help.ContextHelpProvider;
import de.cinovo.surveyplatform.model.Client;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class PartsUtil {
	
	private static final Map<String, int[]> sizeCache = new HashMap<String, int[]>();
	private static final Map<String, Long> sizeCacheIntegrity = new HashMap<String, Long>();
	
	private static final int LOGOMAXWIDTH = 300;
	private static final int LOGOMAXHEIGHT = 150;
	
	
	public static String getPageHeader(final String title, final String helpContextID) {
		return PartsUtil.getPageHeader(title, helpContextID, new String[] {}, true);
	}
	
	public static String getPageHeader(final String title, final String helpContextID, final String[] path) {
		return PartsUtil.getPageHeader(title, helpContextID, path, true);
	}
	
	public static String getPageHeader(final String title, final String helpContextID, final String[] path, final boolean withLogo) {
		final Map<String, String> replacements = new HashMap<String, String>();
		StringBuilder pathElements = new StringBuilder();
		for (String element : path) {
			pathElements.append(element);
			pathElements.append(" &raquo; ");
		}
		String helpLink = "";
		if (!helpContextID.isEmpty()) {
			helpLink = ContextHelpProvider.getInstance().getHelpLink(helpContextID, "Show Help for the current page", "", "float: right;");
		}
		replacements.put("BREADCRUMB", helpLink + "<a href=\"" + EnvironmentConfiguration.getUrlBase() + "/main\">Home</a>" + (title == null ? "" : (" &raquo; " + pathElements + title)));
		if (withLogo) {
			replacements.put("HEAD_LOGO", "<div class=\"marginHeadLogo\" style=\"float:left;\"><img src=\"./gfx/ms_blue-box_logo.png\" alt=\"Metior Solutions\" /></div>");
			replacements.put("MARGIN_BECAUSE_OF_HEAD_LOGO", " style=\"margin-left: 119px;\"");
		}
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/pageHeader.html", replacements);
	}
	
	/**
	 * Creates a divider which divides sections of a page
	 * 
	 * @param title The Title of the section
	 * @param fixedWidth500 set to True if the divider is in a div container
	 *            with a fixed width of 500px
	 * @return HTML representation of the divider
	 */
	public static String getPageSectionDivider(final String title, final boolean fixedWidth500) {
		final Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("TITLE", title);
		if (fixedWidth500) {
			replacements.put("ADDITIONALCLASS", "500");
		}
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/pageSectionDivider.html", replacements);
	}
	
	public static String getIcon(final String name, final String title) {
		return "<span class=\"gui-icon-button-" + name + "\" title=\"" + title + "\"></span>";
	}
	
	public static String getIconLink(final String name, final String title, final String linkText, final String href, final boolean showWait, final String taskID, final boolean openInNewWindow) {
		String targetBlank = "target=\"_blank\"";
		if (openInNewWindow) {
			if (linkText.isEmpty()) {
				return "<a class=\"gui-icon-button-" + name + "\" title=\"" + title + "\" " + (showWait ? "onclick=\"showWait(" + ((taskID != null) ? "'" + taskID + "'" : "") + ");\"" : "") + " href=\"" + href + "\" " + targetBlank + "></a>";
			} else {
				return "<a class=\"gui-icon-" + name + "\" title=\"" + title + "\" " + (showWait ? "onclick=\"showWait(" + ((taskID != null) ? "'" + taskID + "'" : "") + ");\"" : "") + " href=\"" + href + "\" " + targetBlank + ">" + linkText + "</a>";
			}
		} else {
			if (linkText.isEmpty()) {
				return "<a class=\"gui-icon-button-" + name + "\" title=\"" + title + "\" " + (showWait ? "onclick=\"showWait(" + ((taskID != null) ? "'" + taskID + "'" : "") + ");\"" : "") + " href=\"" + href + "\"></a>";
			} else {
				return "<a class=\"gui-icon-" + name + "\" title=\"" + title + "\" " + (showWait ? "onclick=\"showWait(" + ((taskID != null) ? "'" + taskID + "'" : "") + ");\"" : "") + " href=\"" + href + "\">" + linkText + "</a>";
			}
		}
	}
	
	public static String getIconLink(final String name, final String title, final String linkText, final String href) {
		return PartsUtil.getIconLink(name, title, linkText, href, true, null, false);
	}
	
	public static String getIconButton(final String name, final String title, final String id, final String clickJavaScript) {
		return PartsUtil.getIconButton(name, title, "", id, clickJavaScript, "");
	}
	
	public static String getIconButton(final String name, final String title, final String id, final String clickJavascript, final String classAttr) {
		return PartsUtil.getIconButton(name, title, "", id, clickJavascript, classAttr);
	}
	
	public static String getIconButton(final String name, final String title, final String text, final String id, final String clickJavaScript, final String classAttr) {
		String buttonHtml = "<span class=\"" + classAttr + " gui-icon-" + (text.isEmpty() ? "button-" : "") + name + (text.isEmpty() ? "" : " link") + "\" title=\"" + title + "\" id=\"" + id + "\" style=\"cursor: pointer;\">" + text + "</span>";
		if (!clickJavaScript.isEmpty()) {
			buttonHtml += "<script type=\"text/javascript\">/* <![CDATA[*/\n" + "$('#" + id + "').click(function () {" + clickJavaScript + "});\n/* ]]> */</script>";
		}
		return buttonHtml;
	}
	
	/**
	 * @param client the client
	 * @param floatType 0 - none, 1 - left, 2 - right
	 * @return HTML construct that will show the logo of the client or an empty
	 *         String if no logo exists for the user
	 */
	public static String getClientLogo(final Client client, final int floatType) {
		return PartsUtil.getClientLogo(client, floatType, true, true, true);
	}
	
	/**
	 * @param client the client
	 * @param floatType 0 - none, 1 - left, 2 - right
	 * @param additionalStyle Additional styles for the image element
	 * @param addUniqueId true, when you want to add an random number to the
	 *            image url, to prevent the browser from caching the image
	 * @return HTML construct that will show the logo of the client or an empty
	 *         String if no logo exists for the user
	 */
	public static String getClientLogo(final Client client, final int floatType, final boolean addUniqueId, final boolean clearFloat, final boolean restrictDimensions) {
		
		File imageFile = new File(Paths.CLIENTLOGOS + "/" + client.getId() + ".jpg");
		if (imageFile.exists()) {
			
			String title = ""; // effectiveUser.getOrganization() == null ? "" :
			// effectiveUser.getOrganization();
			String uniqueId = addUniqueId ? "?id=" + System.currentTimeMillis() : "";
			
			String dimensions = "";
			if (restrictDimensions) {
				String cacheKey = "client" + client.getId();
				Long lastModifiedLogoDate = PartsUtil.sizeCacheIntegrity.get(cacheKey);
				if ((lastModifiedLogoDate != null) && (lastModifiedLogoDate != imageFile.lastModified())) {
					PartsUtil.sizeCache.remove(cacheKey);
					PartsUtil.sizeCacheIntegrity.remove(cacheKey);
				}
				
				int[] size = PartsUtil.sizeCache.get(cacheKey);
				if (size == null) {
					size = ImageUtil.getSize(imageFile);
					size = ImageUtil.resize(size[0], size[1], PartsUtil.LOGOMAXWIDTH, PartsUtil.LOGOMAXHEIGHT, true);
					PartsUtil.sizeCache.put(cacheKey, size);
					PartsUtil.sizeCacheIntegrity.put(cacheKey, imageFile.lastModified());
				}
				dimensions = " width=\"" + size[0] + "\" height=\"" + size[1] + "\"";
			}
			String img = "<img style=\"margin: 0px 10px 0px 10px\" src=\"" + EnvironmentConfiguration.getUrlBase() + Paths.CLIENTLOGOS.substring(Paths.WEBCONTENT.length()) + "/" + client.getId() + ".jpg" + uniqueId + "\" title=\"" + title + "\"" + dimensions + " />";
			switch (floatType) {
			case 0:
				return img;
			case 1:
				return "<div style=\"float:left;\">" + img + "</div>" + (clearFloat ? ("<div style=\"clear:both;\"></div>") : "");
			case 2:
				return "<div style=\"float:right;\">" + img + "</div>" + (clearFloat ? ("<div style=\"clear:both;\"></div>") : "");
			}
		}
		return "";
	}
	
	/**
	 * Replaces all unwanted characters in a given bad string. Used e.g. as
	 * filename prefix
	 * 
	 * @param badString the bad string
	 * @return string where all unwanted characters (i.e. [^a-zA-Z0-9]) are
	 *         replaced by _
	 */
	public static String convertToValidString(final String badString) {
		// replaces all invalid chars by "_"
		return badString.replaceAll("[^a-zA-Z0-9]", "_");
	}
	
	/**
	 * @param clientName name of the client
	 * @param floatType 0 - none, 1 - left, 2 - right
	 * @param addUniqueId true, when you want to add an random number to the
	 *            image url, to prevent the browser from caching the image
	 * @return HTML construct that will show the logo of the client or an empty
	 *         String if no logo exists for the user
	 */
	public static String getSurveyLogo(final int surveyId, final int floatType, final boolean addUniqueId, final boolean clearFloat, final boolean restrictDimensions) {
		String surveyIdStr = String.valueOf(surveyId);
		File imageFile = new File(Paths.SURVEYLOGOS + "/" + surveyIdStr + ".jpg");
		
		if (imageFile.exists()) {
			
			String dimensions = "";
			if (restrictDimensions) {
				Long lastModifiedLogoDate = PartsUtil.sizeCacheIntegrity.get(surveyIdStr);
				if ((lastModifiedLogoDate != null) && (lastModifiedLogoDate != imageFile.lastModified())) {
					PartsUtil.sizeCache.remove(surveyIdStr);
					PartsUtil.sizeCacheIntegrity.remove(surveyIdStr);
				}
				
				int[] size = PartsUtil.sizeCache.get(surveyIdStr);
				if (size == null) {
					size = ImageUtil.getSize(imageFile);
					size = ImageUtil.resize(size[0], size[1], PartsUtil.LOGOMAXWIDTH, PartsUtil.LOGOMAXHEIGHT, true);
					PartsUtil.sizeCache.put(surveyIdStr, size);
					PartsUtil.sizeCacheIntegrity.put(surveyIdStr, imageFile.lastModified());
				}
				dimensions = " width=\"" + size[0] + "\" height=\"" + size[1] + "\"";
			}
			String uniqueId = addUniqueId ? "?id=" + System.currentTimeMillis() : "";
			String img = "<img style=\"margin: 0px 10px 0px 10px\" src=\"" + EnvironmentConfiguration.getUrlBase() + Paths.SURVEYLOGOS.substring(Paths.WEBCONTENT.length()) + "/" + surveyId + ".jpg" + uniqueId + "\" title=\"\"" + dimensions + " />";
			switch (floatType) {
			case 0:
				return img;
			case 1:
				return "<div style=\"float:left;\">" + img + "</div>" + (clearFloat ? ("<div style=\"clear:both;\"></div>") : "");
			case 2:
				return "<div style=\"float:right;\">" + img + "</div>" + (clearFloat ? ("<div style=\"clear:both;\"></div>") : "");
			}
		}
		return "";
	}
	
	public static String getProgressBar(final int value) {
		final Map<String, String> replacements = new HashMap<String, String>();
		final int maxwidth = 80;
		replacements.put("VALUE", String.valueOf(value));
		replacements.put("MAXWIDTH", String.valueOf(maxwidth));
		replacements.put("WIDTH", String.valueOf((int) Math.floor(((double) value * maxwidth) / 100)));
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/progressBar.html", replacements);
	}
	
	public static String getDynamicProgressBar(final int value) {
		final Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("VALUE", value + "");
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/progressBarDynamic.html", replacements);
	}
	
	public static String getUnsufficientRightsMessage() {
		return "<div class=\"innerContainer\">You do not have sufficient rights to access this page!</div>";
	}
}
