package de.cinovo.surveyplatform.bootstrap.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;

import org.eclipse.jetty.jndi.NamingUtil;
import org.eclipse.jetty.plus.jndi.NamingEntryUtil;
import org.eclipse.jetty.plus.jndi.Resource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import de.cinovo.surveyplatform.util.Base64Util;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * Reads the datasource.properties file and configures the data source
 * 
 * @author yschubert
 * 
 * 
 */
public class DataSourceConfiguration {
	
	public static void configure(final String configFile, final boolean clearDatabase) throws Exception {
		Properties properties = new Properties();
		FileInputStream stream;
		
		// load properties file
		if (EnvironmentConfiguration.isOfflineMode() && new File(new File(configFile).getParent() + "/offline").exists()) {
			StringReader sr = new StringReader(Base64Util.decodeFile(configFile));
			properties.load(sr);
			sr.close();
		} else {
			stream = new FileInputStream(configFile);
			properties.load(stream);
			stream.close();
		}
		
		// create datasource
		ComboPooledDataSource dataSource = new ComboPooledDataSource();
		// BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUser(properties.getProperty("username"));
		dataSource.setPassword(properties.getProperty("password", ""));
		dataSource.setJdbcUrl("jdbc:" + properties.getProperty("url")); // mysql://" + properties.getProperty("host") + "/" + properties.getProperty("dbname"));
		dataSource.setDriverClass(properties.getProperty("driver")); // com.mysql.jdbc.Driver
		dataSource.setPreferredTestQuery("SELECT 1");
		dataSource.setTestConnectionOnCheckin(true);
		// dataSource.set
		
		if (EnvironmentConfiguration.isTestEnvironment() && clearDatabase) {
			System.out.println("Cleaning Test database...");
			Connection conn = dataSource.getConnectionPoolDataSource().getPooledConnection().getConnection();
			conn.createStatement().executeUpdate("DROP SCHEMA IF EXISTS SURVEY;");
			conn.createStatement().executeUpdate("CREATE SCHEMA SURVEY;");
			conn.commit();
			System.out.println("Done.");
			// java:/comp/env/jdbc/scc
		}

		// init naming context
		// Hashtable<String, String> env = new Hashtable<String, String>();
		// env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
		// "javax.naming.spi.InitialContextFactory");
		
		InitialContext ctx = new InitialContext();
		javax.naming.Context compCtx;
		javax.naming.Context envCtx;
		
		// init jndi resource
		Resource res = new Resource("jdbc/" + properties.getProperty("resourcename"), dataSource);
		compCtx = (javax.naming.Context) ctx.lookup("java:comp");
		try {
			compCtx.destroySubcontext("env");
		} catch (NameNotFoundException nnfe) {
			// ignore that
		}
		envCtx = compCtx.createSubcontext("env");
		res.bindToENC(res.getJndiName());
		Name namingEntryName = NamingEntryUtil.makeNamingEntryName(null, res);
		
		// bind datasource to jndi
		NamingUtil.bind(envCtx, namingEntryName.toString(), res);
		
	}
}
