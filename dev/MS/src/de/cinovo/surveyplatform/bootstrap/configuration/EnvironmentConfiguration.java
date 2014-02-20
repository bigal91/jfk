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
public class EnvironmentConfiguration {
	
	public static String VERSION = "development";
	
	
	public static enum EnvironmentType {
		PRODUCTIVE,
		DEVELOPMENT, TEST
	}
	
	public static enum ConnectorType {
		SELECTCHANNEL, SOCKET
	}
	
	public static enum ConfigID {
		PORT, ENVIRONMENT_TYPE, SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD, SMTP_SSL_USE, SMTP_SSL_PORT, SERVER_BASE, EMAIL_SUPPORT, SEND_SUPPORT_MAIL, EMAIL_SENDER, URLBASE, CONNECTORTYPE, HOST, USE_STATISTICS, REGISTER_URL, CONTACT_URL, ACCESSFROM, OFFLINE_MODE, LOGGER, TEMPLATE_CACHE_ENABLED
	}
	
	
	private static final Map<ConfigID, Object> configuration = new HashMap<ConfigID, Object>();
	
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
		
		configuration.put(ConfigID.PORT, Integer.parseInt(properties.getProperty("port", "80")));
		configuration.put(ConfigID.URLBASE, properties.getProperty("url_base", ""));
		configuration.put(ConfigID.HOST, properties.getProperty("host", "/"));
		configuration.put(ConfigID.ACCESSFROM, properties.getProperty("accessfrom", "0.0.0.0"));
		configuration.put(ConfigID.ENVIRONMENT_TYPE, EnvironmentType.valueOf(properties.getProperty("environment", EnvironmentType.DEVELOPMENT.name()).toUpperCase()));
		configuration.put(ConfigID.SMTP_HOST, properties.getProperty("smtp_host", "localhost"));
		configuration.put(ConfigID.SMTP_SSL_USE, Boolean.parseBoolean(properties.getProperty("smtp_ssl_enabled", "true")));
		configuration.put(ConfigID.SMTP_SSL_PORT, properties.getProperty("smtp_ssl_port", "465"));
		configuration.put(ConfigID.SMTP_USERNAME, properties.getProperty("smtp_username", ""));
		configuration.put(ConfigID.SMTP_PASSWORD, properties.getProperty("smtp_password", ""));
		configuration.put(ConfigID.EMAIL_SUPPORT, properties.getProperty("email_support", ""));
		configuration.put(ConfigID.SEND_SUPPORT_MAIL, Boolean.parseBoolean(properties.getProperty("send_support_mail", "false")));
		configuration.put(ConfigID.EMAIL_SENDER, properties.getProperty("email_sender", ""));
		configuration.put(ConfigID.REGISTER_URL, properties.getProperty("register_url", ""));
		configuration.put(ConfigID.CONTACT_URL, properties.getProperty("contact_url", ""));
		configuration.put(ConfigID.OFFLINE_MODE, Boolean.parseBoolean(properties.getProperty("offline_mode", "false")));
		configuration.put(ConfigID.CONNECTORTYPE, ConnectorType.valueOf(properties.getProperty("connector_type", ConnectorType.SELECTCHANNEL.name().toUpperCase())));
		configuration.put(ConfigID.USE_STATISTICS, Boolean.parseBoolean(properties.getProperty("use_statistics", "false")));
		configuration.put(ConfigID.LOGGER, properties.getProperty("logger", "console"));
		configuration.put(ConfigID.TEMPLATE_CACHE_ENABLED, Boolean.parseBoolean(properties.getProperty("templateCacheEnabled", "true")));
		
	}
	
	public static Object getConfiguration(final ConfigID configID) {
		return configuration.get(configID);
	}
	
	public static boolean isDevelopmentEnvironment() {
		return configuration.get(ConfigID.ENVIRONMENT_TYPE) == EnvironmentType.DEVELOPMENT;
	}
	
	public static boolean isTestEnvironment() {
		return configuration.get(ConfigID.ENVIRONMENT_TYPE) == EnvironmentType.TEST;
	}
	
	public static boolean isOfflineMode() {
		return Boolean.TRUE.equals(configuration.get(ConfigID.OFFLINE_MODE));
	}
	
	public static String getUrlBase() {
		return (String) configuration.get(ConfigID.URLBASE);
	}
	
	public static String getHostAndBase() {
		return (String) configuration.get(ConfigID.HOST) + (String) configuration.get(ConfigID.URLBASE);
	}
	
	public static boolean isTemplateCacheEnabled() {
		return Boolean.TRUE.equals(configuration.get(ConfigID.TEMPLATE_CACHE_ENABLED));
	}
}
