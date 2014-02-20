package de.cinovo.surveyplatform.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.EnumSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;

import de.cinovo.surveyplatform.bootstrap.configuration.DataSourceConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConnectorType;
import de.cinovo.surveyplatform.bootstrap.configuration.FeatureToggle;
import de.cinovo.surveyplatform.bootstrap.fixture.Fixture;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.help.ContextHelpProvider;
import de.cinovo.surveyplatform.servlets.DownloadServlet;
import de.cinovo.surveyplatform.servlets.LoginServlet;
import de.cinovo.surveyplatform.servlets.MainEntryPoint;
import de.cinovo.surveyplatform.servlets.ParticipateServlet;
import de.cinovo.surveyplatform.servlets.css.DynamicCssServlet;
import de.cinovo.surveyplatform.servlets.dal.ClientDal;
import de.cinovo.surveyplatform.servlets.dal.FeedBackDal;
import de.cinovo.surveyplatform.servlets.dal.HelpDal;
import de.cinovo.surveyplatform.servlets.dal.ParticipantDal;
import de.cinovo.surveyplatform.servlets.dal.QuestionnaireLogicElementDal;
import de.cinovo.surveyplatform.servlets.dal.ReportDal;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.servlets.dal.SystemUserDal;
import de.cinovo.surveyplatform.servlets.dal.TopicDal;
import de.cinovo.surveyplatform.servlets.dal.UserGroupDal;
import de.cinovo.surveyplatform.servlets.helper.AnalyseIDs;
import de.cinovo.surveyplatform.servlets.helper.ClearCache;
import de.cinovo.surveyplatform.servlets.helper.CorrectOptionIDs;
import de.cinovo.surveyplatform.servlets.helper.CorrectQuestionIDs;
import de.cinovo.surveyplatform.servlets.helper.FindLogicElementOrphans;
import de.cinovo.surveyplatform.servlets.helper.ImportS;
import de.cinovo.surveyplatform.servlets.helper.Log;
import de.cinovo.surveyplatform.servlets.helper.ReReadConfig;
import de.cinovo.surveyplatform.servlets.helper.Tasks;
import de.cinovo.surveyplatform.util.CleanupUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.StreamUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * This class loads the environment of jetty and starts jetty
 * 
 * @author yschubert
 * 
 * 
 */
public class JettyLauncher {
	
	/**
	 * 
	 */
	private static final String ARG_DATASOURCE = "data=";
	/**
	 * 
	 */
	private static final String ARG_ENVIRONMENT = "environment=";
	
	/**
	 * 
	 */
	private static final String ARG_NO_DB_CLEAR = "no_db_clear=";
	/**
	 * 
	 */
	private static final String PROPERTIES_FILE_DATASOURCE = Paths.CONFIG + "/datasource.properties";
	/**
	 * 
	 */
	private static final String PROPERTIES_FILE_ENVIRONMENT = Paths.CONFIG + "/environment.properties";
	/**
	 * 
	 */
	private static final String PROPERTIES_FILE_FEATURES = Paths.CONFIG + "/features.properties";
	
	public static String environmentConfigFile;
	public static String datasourceConfigFile;
	public static String featureConfigFile;
	
	public static void main(final String[] args) throws Exception {
		
		
		createFolders();
		environmentConfigFile = JettyLauncher.PROPERTIES_FILE_ENVIRONMENT;
		datasourceConfigFile = JettyLauncher.PROPERTIES_FILE_DATASOURCE;
		featureConfigFile = PROPERTIES_FILE_FEATURES;
		boolean clearDatabase = true;
		for (String arg : args) {
			if (arg.startsWith(JettyLauncher.ARG_DATASOURCE)) {
				datasourceConfigFile = arg.substring(JettyLauncher.ARG_DATASOURCE.length());
			}
			if (arg.startsWith(JettyLauncher.ARG_ENVIRONMENT)) {
				environmentConfigFile = arg.substring(JettyLauncher.ARG_ENVIRONMENT.length());
			}
			if (arg.startsWith(ARG_NO_DB_CLEAR)) {
				clearDatabase = !Boolean.parseBoolean(arg.substring(ARG_NO_DB_CLEAR.length()));
			}
		}
		
		
		// initialize environment
		EnvironmentConfiguration.configure(environmentConfigFile);
		FeatureToggle.configure(featureConfigFile);
		
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");
		for (Handler handler : logger.getHandlers()) {
			logger.removeHandler(handler);
		}
		
		if ("file".equals(EnvironmentConfiguration.getConfiguration(ConfigID.LOGGER))) {
			FileHandler fileHandler = new FileHandler(Paths.LOGS + "/syso%u.log", 5000000, 5, true);
			fileHandler.setFormatter(new LogFormatter());
			logger.addHandler(fileHandler);
		} else {
			ConsoleHandler consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(new LogFormatter());
			logger.addHandler(consoleHandler);
		}
		
		
		
		InputStream stream = JettyLauncher.class.getResourceAsStream("/version");
		
		if (stream != null) {
			EnvironmentConfiguration.VERSION = StreamUtil.inputStreamToString(stream);
			
			String header = "\n---------------------------------------------------------\n";
			header += " Metior Solutions Survey Platform   " + EnvironmentConfiguration.VERSION;
			header += "---------------------------------------------------------\n";
			Logger.info(header);
			System.out.println(header);
			stream.close();
		}
		
		if (EnvironmentConfiguration.isTestEnvironment()) {
			String header = "\n---------------------------------------------------------\n";
			header += " TEST ENVIRONMENT \n";
			header += "---------------------------------------------------------\n";
			Logger.info(header);
		}
		
		Integer serverPort = (Integer) EnvironmentConfiguration.getConfiguration(ConfigID.PORT);
		
		// test port
		try {
			ServerSocket socket = new ServerSocket(serverPort);
			socket.close();
		} catch (IOException ioe) {
			String alreadyStarted = "Server already started on port: " + serverPort;
			System.out.println(alreadyStarted);
			Logger.warn(alreadyStarted);
			JettyLauncher.checkToOpenBrowser(args);
			return;
		}
		
		// initialize datasource
		DataSourceConfiguration.configure(datasourceConfigFile, clearDatabase);
		
		
		// Webserver erstellen
		final Server server = new Server();
		AbstractConnector connector;
		if (EnvironmentConfiguration.getConfiguration(ConfigID.CONNECTORTYPE).equals(ConnectorType.SOCKET)) {
			connector = new SocketConnector();
		} else {
			connector = new SelectChannelConnector();
		}
		connector.setReuseAddress(false);
		connector.setPort(serverPort);
		String allowFrom = (String) EnvironmentConfiguration.getConfiguration(ConfigID.ACCESSFROM);
		connector.setHost(allowFrom);
		connector.setAcceptQueueSize(30);
		connector.setRequestHeaderSize(16384);
		
		server.setSendServerVersion(false);
		server.setSendDateHeader(false);
		
		
		final StatisticsHandler statisticsHandler = new StatisticsHandler();
		if ((Boolean) EnvironmentConfiguration.getConfiguration(ConfigID.USE_STATISTICS)) {
			statisticsHandler.setHandler(new RequestLogHandler());
			server.setHandler(statisticsHandler);
		}
		
		server.setConnectors(new Connector[] {connector});
		
		// Context für die Seite erstellen
		ServletContextHandler context = new ServletContextHandler(server, EnvironmentConfiguration.getUrlBase().isEmpty() ? "/" : EnvironmentConfiguration.getUrlBase());
		context.setResourceBase("./" + Paths.WEBCONTENT);
		
		// session Manager
		Logger.info("Creating Session Manager");
		SessionManager sm = new HashSessionManager();
		sm.setMaxInactiveInterval(3600);
		
		SessionHandler sh = new SessionHandler(sm);
		
		context.setSessionHandler(sh);
		context.setWelcomeFiles(new String[] {"index.jsp"});
		
		Logger.info("Adding GzipFilter");
		context.addFilter(GzipFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		
		Logger.info("Adding Servlets");
		
		context.addServlet(new ServletHolder(new DefaultServlet()), "/");
		context.addServlet(new ServletHolder(new MainEntryPoint()), "/main");
		
		context.addServlet(new ServletHolder(new LoginServlet()), "/login");
		
		context.addServlet(new ServletHolder(new DownloadServlet()), "/download");
		
		DynamicCssServlet dcss = new DynamicCssServlet();
		
		File cssFolder = new File(Paths.TEMPLATEPATH + "/css");
		if (cssFolder.exists() && cssFolder.isDirectory()) {
			for (File cssFile : cssFolder.listFiles()) {
				if (cssFile.isFile()) {
					context.addServlet(new ServletHolder(dcss), "/css/" + cssFile.getName());
					Logger.info("Adding Dynamic CSS:" + cssFile.getName());
				}
			}
		}
		
		context.addServlet(new ServletHolder(new ParticipateServlet()), "/participate");
		HelpDal helpDal = new HelpDal();
		context.addServlet(new ServletHolder(helpDal), "/help/retrieve");
		
		FeedBackDal feedBackDal = new FeedBackDal();
		context.addServlet(new ServletHolder(feedBackDal), "/feedback/retrieve");
		
		QuestionnaireLogicElementDal qleDal = new QuestionnaireLogicElementDal();
		context.addServlet(new ServletHolder(qleDal), "/questionnairelogicelement/retrieve");
		
		SystemUserDal systemUserDal = new SystemUserDal();
		context.addServlet(new ServletHolder(systemUserDal), "/user/retrieve");
		context.addServlet(new ServletHolder(systemUserDal), "/user/create");
		context.addServlet(new ServletHolder(systemUserDal), "/user/update");
		
		ClientDal clientDal = new ClientDal();
		context.addServlet(new ServletHolder(clientDal), "/client/create");
		context.addServlet(new ServletHolder(clientDal), "/client/retrieve");
		context.addServlet(new ServletHolder(clientDal), "/client/update");
		
		UserGroupDal userGroupDal = new UserGroupDal();
		context.addServlet(new ServletHolder(userGroupDal), "/group/create");
		context.addServlet(new ServletHolder(userGroupDal), "/group/retrieve");
		context.addServlet(new ServletHolder(userGroupDal), "/group/update");
		context.addServlet(new ServletHolder(userGroupDal), "/group/delete");
		
		SurveyDal surveyDal = new SurveyDal();
		context.addServlet(new ServletHolder(surveyDal), "/survey/create");
		context.addServlet(new ServletHolder(surveyDal), "/survey/retrieve");
		context.addServlet(new ServletHolder(surveyDal), "/survey/update");
		context.addServlet(new ServletHolder(surveyDal), "/survey/delete");
		
		ParticipantDal participantDal = new ParticipantDal();
		context.addServlet(new ServletHolder(participantDal), "/participant/create");
		context.addServlet(new ServletHolder(participantDal), "/participant/retrieve");
		context.addServlet(new ServletHolder(participantDal), "/participant/update");
		context.addServlet(new ServletHolder(participantDal), "/participant/delete");
		
		TopicDal topicDal = new TopicDal();
		context.addServlet(new ServletHolder(topicDal), "/topic/create");
		context.addServlet(new ServletHolder(topicDal), "/topic/retrieve");
		context.addServlet(new ServletHolder(topicDal), "/topic/update");
		context.addServlet(new ServletHolder(topicDal), "/topic/delete");
		
		ReportDal reportDal = new ReportDal();
		context.addServlet(new ServletHolder(reportDal), "/report/create");
		context.addServlet(new ServletHolder(reportDal), "/report/retrieve");
		context.addServlet(new ServletHolder(reportDal), "/report/update");
		context.addServlet(new ServletHolder(reportDal), "/report/delete");
		
		if (EnvironmentConfiguration.isDevelopmentEnvironment()) {
			addHelpServlets(context);
		}
		
		// create fixture data
		Fixture.create(clearDatabase);
		
		// init
		JettyLauncher.initContextSensitiveHelp();
		
		try {
			// Server starten
			server.start();
			Logger.info("Server is running.");
			Thread cleanUp = new Thread(new CleanupUtil());
			cleanUp.run();
			Logger.info("Cleanup-Service is Running");
			System.out.println("Server is running and listening on " + allowFrom + ":" + serverPort);
			JettyLauncher.checkToOpenBrowser(args);
			server.join();
			Logger.info("Server stopped.");
			System.out.println("Server stopped.");
		} catch (Exception e) {
			server.stop();
			Logger.err("Error during Server Startup: " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * 
	 */
	private static void createFolders() {
		{
			File dir = new File(Paths.REPORTS);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		{
			File dir = new File(Paths.TEMP);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		{
			File dir = new File(Paths.LOGS);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		{
			File dir = new File(Paths.CLIENTLOGOS);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		{
			File dir = new File(Paths.SURVEYLOGOS);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		{
			File dir = new File(Paths.WEBCONTENT + "/" + Paths.UPLOAD_TEMP);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		
	}
	
	private static void checkToOpenBrowser(final String[] args) {
		for (String s : args) {
			if ("launchbrowser".equals(s.toLowerCase())) {
				JettyLauncher.openBrowser();
			}
		}
	}
	
	/**
	 *
	 */
	private static void openBrowser() {
		
		if (!java.awt.Desktop.isDesktopSupported()) {
			Logger.warn("Cannot open the Browser as the Desktop API is not supported.");
			return;
		}
		
		java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
		
		if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
			Logger.warn("Cannot open the Browser as there is no default Browser installed.");
			return;
		}
		
		try {
			
			java.net.URI uri = new java.net.URI("http://localhost:" + EnvironmentConfiguration.getConfiguration(ConfigID.PORT) + "/main");
			desktop.browse(uri);
		} catch (Exception e) {
			
			System.err.println(e.getMessage());
		}
		
	}
	
	/**
	 *
	 */
	public static void initContextSensitiveHelp() {
		File helpTemplateFolder = new File(Paths.TEMPLATEPATH + "/help");
		if (helpTemplateFolder.exists() && helpTemplateFolder.isDirectory()) {
			for (File helpFile : helpTemplateFolder.listFiles()) {
				if (helpFile.isFile()) {
					String fileName = helpFile.getName();
					String contextID = fileName;
					if (fileName.contains(".")) {
						contextID = helpFile.getName().substring(0, helpFile.getName().lastIndexOf("."));
					}
					ContextHelpProvider.getInstance().setHelp(contextID, TemplateUtil.getTemplate(helpFile.getAbsolutePath(), null));
					Logger.info("Adding Helpcontext: " + contextID);
				}
			}
		}
	}
	
	
	
	
	private static void addHelpServlets(final ServletContextHandler context) {
		context.addServlet(new ServletHolder(new ImportS()), "/" + ImportS.class.getSimpleName());
		context.addServlet(new ServletHolder(new Log()), "/" + Log.class.getSimpleName());
		context.addServlet(new ServletHolder(new ClearCache()), "/" + ClearCache.class.getSimpleName());
		context.addServlet(new ServletHolder(new CorrectQuestionIDs()), "/" + CorrectQuestionIDs.class.getSimpleName());
		context.addServlet(new ServletHolder(new CorrectOptionIDs()), "/" + CorrectOptionIDs.class.getSimpleName());
		context.addServlet(new ServletHolder(new AnalyseIDs()), "/" + AnalyseIDs.class.getSimpleName());
		context.addServlet(new ServletHolder(new Tasks()), "/" + Tasks.class.getSimpleName());
		context.addServlet(new ServletHolder(new FindLogicElementOrphans()), "/" + FindLogicElementOrphans.class.getSimpleName());
		context.addServlet(new ServletHolder(new ReReadConfig()), "/" + ReReadConfig.class.getSimpleName());

	}
	
}
