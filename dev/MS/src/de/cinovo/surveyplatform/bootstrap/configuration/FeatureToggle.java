package de.cinovo.surveyplatform.bootstrap.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.cinovo.surveyplatform.util.Base64Util;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * Reads the environment.properties file and configures the environment
 * 
 * @author yschubert
 * 
 * 
 */
public class FeatureToggle {
	
	
	
	/**
	 * List of available features
	 * 
	 */
	public static enum Feature {
		/**
		 * Registering for extern users allowed
		 */
		REGISTER
	}
	
	
	private static final Map<Feature, Object> configuration = new HashMap<Feature, Object>();
	
	
	/**
	 * @param propertiesFile the prop file
	 * @throws Exception in case of error
	 */
	public static void configure(final String propertiesFile) throws Exception {
		Properties properties = new Properties();
		FileInputStream stream;
		// load properties file
		if (new File(new File(propertiesFile).getParent() + "/offline").exists()) {
			StringReader sr = new StringReader(Base64Util.decodeFile(propertiesFile));
			properties.load(sr);
			sr.close();
		} else {
			stream = new FileInputStream(propertiesFile);
			properties.load(stream);
			stream.close();
		}
		
		for (Feature f : Feature.values()) {
			configuration.put(f, Boolean.parseBoolean(properties.getProperty(f.name(), "false")));
		}
		
	}
	
	/**
	 * @param feature The feature to check
	 * @return true if given feature is enabled, false otherwise
	 */
	public static Boolean isEnabled(final Feature feature) {
		if (configuration == null) {
			return false;
		}
		Boolean result = (Boolean) configuration.get(feature);
		if (result == null) {
			return false;
		}
		return result;
	}
	
	
	// public static void main(final String[] args) throws Exception {
	// configure("config/features.properties");
	// System.out.println(isEnabled(Feature.REGISTER));
	// }
	
}

