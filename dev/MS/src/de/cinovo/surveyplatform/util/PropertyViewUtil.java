/**
 *
 */
package de.cinovo.surveyplatform.util;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class PropertyViewUtil {
	
	public static String format(final String propertyKey) {
		return propertyKey.replaceAll("[\\.\\/]", "_");
	}
	
	public static String getPropertyView(final String propertyKey) {
		
		if (propertyKey.equals("programmeTitle")) {
			return "Programme title";
		} else if (propertyKey.equals("nqfLevel")) {
			return "NQF level";
		} else if (propertyKey.equals("a18_1_18_2")) {
			return "18.1/18.2";
		} else if (propertyKey.equals("homeLanguage")) {
			return "Homelanguage";
		} else if (propertyKey.equals("gender")) {
			return "Gender";
		} else if (propertyKey.equals("age")) {
			return "AGE";
		} else if (propertyKey.equals("a01employerName")) {
			return "Employer name";
		} else if (propertyKey.equals("a02employerPhone")) {
			return "Employer phone";
		} else if (propertyKey.equals("province")) {
			return "Province";
		} else if (propertyKey.equals("ethnicgroup")) {
			return "Ethnic Group";
		}
		return "";
	}
}
