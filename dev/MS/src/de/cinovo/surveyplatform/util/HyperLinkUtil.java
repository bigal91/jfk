package de.cinovo.surveyplatform.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.model.StringStringPair;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class HyperLinkUtil {
	
	public static String getServerBaseLink(final HttpServletRequest req) {
		return req.getScheme() + "://" + req.getServerName() + (req.getLocalPort() == 80 ? "" : (":" + req.getLocalPort()));
	}
	
	public static String getHtmlLink(final String title, final String cssClass, final String text, final HttpServletRequest request, final StringStringPair... pairs) {
		return "<a href=\"" + HyperLinkUtil.getLinkURI(request, pairs) + "\" title=\"" + title + "\" class=\"" + cssClass + "\">" + text + "</a>";
	}
	
	public static String getLinkURI(final HttpServletRequest request, final StringStringPair... pairs) {
		final StringBuilder link = new StringBuilder();
		link.append(request.getRequestURI());
		if ((request.getParameterMap().size() + pairs.length) > 0) {
			link.append("?");
			Map<String, String> newParams = new HashMap<String, String>();
			
			for (Object keyO : request.getParameterMap().keySet()) {
				String key = keyO.toString();
				Object valueO = request.getParameter(key);
				// only process string values (not arrays)
				if (valueO instanceof String) {
					newParams.put(key, valueO.toString());
				}
			}
			// now overwrite if applicable
			for (StringStringPair kvp : pairs) {
				newParams.put(kvp.getKey().toString(), kvp.getValue().toString());
			}
			
			// build the querystring
			for (String key : newParams.keySet()) {
				link.append(key);
				link.append("=");
				link.append(newParams.get(key));
				link.append("&");
			}
			link.deleteCharAt(link.length() - 1);
		}
		return link.toString();
	}
	
	public static String removeParamFromRequest(final String requestURI, final String... params) {
		String newRequestURI = requestURI;
		for (String paramToRemove : params) {
			newRequestURI = newRequestURI.replaceAll(paramToRemove + "=[^&]*", "").replaceAll("&&", "&").replaceAll("&$", "");
		}
		return newRequestURI;
	}
	
	public static String setParamInRequest(final String requestURI, final StringStringPair... pairs) {
		String newRequestURI = requestURI;
		for (StringStringPair pair : pairs) {
			String param = pair.getKey();
			String newValue = pair.getValue();
			
			Pattern pattern = Pattern.compile(param + "=[^&]*", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(newRequestURI);
			if (matcher.find()) {
				newRequestURI = matcher.replaceAll(param + "=" + newValue);
			} else {
				// if param not found
				if (newRequestURI.contains("?")) {
					newRequestURI += "&" + param + "=" + newValue;
				} else {
					newRequestURI += "?" + param + "=" + newValue;
				}
			}
		}
		return newRequestURI;
	}
	
	public static String encodeURLWithSession(final HttpServletResponse response, final String URL) {
		int paramsIndex = URL.indexOf("?");
		if (paramsIndex == 0) {
			return URL;
		} else if (paramsIndex > 0) {
			return response.encodeURL(URL.substring(0, paramsIndex)) + URL.substring(paramsIndex);
		}
		return response.encodeURL(URL);
	}
	
	public static String buildHyperLink(final String url, final String content, final String title, final String cssClass, final StringStringPair... pairs) {
		StringBuilder link = new StringBuilder();
		
		link.append("<a href=\"");
		link.append(url);
		if (pairs.length > 0) {
			link.append("?");
		}
		for (StringStringPair pair : pairs) {
			if (pair != null) {
				link.append(pair.getKey());
				link.append("=");
				link.append(pair.getValue());
				link.append("&");
			}
		}
		if (pairs.length > 0) {
			// delete & at the end if any parameter added or the ? if no
			// parameter was added
			link.deleteCharAt(link.length() - 1);
		}
		link.append("\" class=\"");
		link.append(cssClass);
		link.append("\" title=\"");
		link.append(title);
		link.append("\">");
		link.append(content);
		link.append("</a>");
		return link.toString();
	}
}
