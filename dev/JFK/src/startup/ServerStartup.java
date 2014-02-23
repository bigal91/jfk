package startup;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import constants.EnvConfiguration;
import resources.ResourcePaths;
import servlets.LoginServlet;
import servlets.MainEntryPoint;
import servlets.helper.ClearCache;

public class ServerStartup
{
	private final static Integer serverPort = 12300;
	
	public static String environmentConfigFile;
	
    public static void main(String[] args) throws Exception
    {
    	environmentConfigFile = ResourcePaths.CONFIG + "/env.properties";
    	EnvConfiguration.configure(environmentConfigFile);
    	
    	/* Pre-Server Management */
		try {
			// test the port
			ServerSocket socket = new ServerSocket(serverPort);
			socket.close();
		} catch (IOException ioe) {
			System.out.println("Server läuft bereits auf: " + serverPort);
			return;
		}
    	
    	/* Main Server Management */
        Server server = new Server(serverPort);

        ServletContextHandler context = new ServletContextHandler(server, "/");
        context.setResourceBase("./" + ResourcePaths.WC);
        
        /* Session Management */
        SessionManager sessionManager = new HashSessionManager();
        sessionManager.setMaxInactiveInterval(3600);
        SessionHandler sessionHandler = new SessionHandler(sessionManager);
        context.setSessionHandler(sessionHandler);
        
        /* Servlet Context Management */
        context.setWelcomeFiles(new String[] {"index.html"});
        
        /* Servlets */
        
        // Use the following template for servlets:
        // context.addServlet(new ServletHolder(new NameServlet()), "/name");
        /* DefaultServlet for files */
        context.addServlet(new ServletHolder(new DefaultServlet()), "/");
		context.addServlet(new ServletHolder(new MainEntryPoint()), "/main");
        context.addServlet(new ServletHolder(new LoginServlet()), "/login");
        
        /* Helper Servlets */
		context.addServlet(new ServletHolder(new ClearCache()), "/" + ClearCache.class.getSimpleName());
        
        Init.initialize();
		
        try{
        	server.start();
        	System.out.println("Server ist gestartet unter: " + serverPort);
        	server.join();
        	System.out.println("Server wurde angehalten.");
        }catch (Exception e) {
			server.stop();
			System.out.println("Unerwarteter Fehler beim Serverstart");
		}
    }

}