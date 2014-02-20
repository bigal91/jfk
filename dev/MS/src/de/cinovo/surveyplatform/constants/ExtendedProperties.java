/**
 * 
 */
package de.cinovo.surveyplatform.constants;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import de.cinovo.surveyplatform.model.Survey;


/**
 * Copyright 2013 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class ExtendedProperties {
	
	public static String SURVEY_LETPARTICIPANTDOWNLOADPDF = "letParticipantDownloadPDF";
	public static String SURVEY_ISPUBLIC = "isPublic";
	public static String SURVEY_LASTPAGEISMANDATORY = "lastPageMandatory";
	
	private static Properties readProperties(final String packedProperties) {
		Properties properties = new Properties();
		try {
			properties.load(new StringReader(packedProperties));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T get(final Survey survey, final String key, final T defaultValue) {
		String extendedProperties = survey.getExtendedProperties();
		String value = "";
		if (extendedProperties != null) {
			value = (readProperties(extendedProperties).getProperty(key, String.valueOf(defaultValue)));
		}
		
		if (defaultValue instanceof Boolean) {
			return (T) ((Boolean) Boolean.parseBoolean(value));
		} else if (defaultValue instanceof String) {
			return (T) (value);
		} else if (defaultValue instanceof Integer) {
			return (T) (Integer) (Integer.parseInt(value));
		}
		
		return null;
	}
	
}
