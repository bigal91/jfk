/**
 *
 */
package de.cinovo.surveyplatform.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.mail.EmailException;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.CallBack;
import de.cinovo.surveyplatform.model.FilteredStream;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class Logger {
	
	// private static Logger instance;
	// private PrintStream errPrintStream;
	// private PrintStream outPrintStream;
	// private static File errLogFile;
	// private static File outLogFile;
	
	private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss ");
	
	private static final java.util.logging.Logger SYSTEMLOGGER = java.util.logging.Logger.getLogger("LOG");
	
	private static CallBack callBack = new CallBack() {
		
		@Override
		public void doCallBack() {
			warn("Sent email to support: " + EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SUPPORT));
		}
	};
	
	// public static void init(final String logFilePrefix) {
	// Logger.instance = new Logger();
	//
	// Logger.errLogFile = new File(Paths.LOGS + "/" + logFilePrefix + "error.log");
	// Logger.outLogFile = new File(Paths.LOGS + "/" + logFilePrefix + "out.log");
	//
	// Logger.instance.errPrintStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream(), Logger.errLogFile));
	// Logger.instance.outPrintStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream(), Logger.outLogFile));
	// }
	
	public static void info(final String toLog) {
		SYSTEMLOGGER.info(toLog);
		
	}
	
	public static void warn(final String toLog) {
		SYSTEMLOGGER.warning(toLog);
	}
	
	public static void err(final String toLog) {
		SYSTEMLOGGER.severe(toLog);
	}
	
	public static void err(final String toLog, final Throwable t) {
		SYSTEMLOGGER.log(Level.SEVERE, toLog, t);
		t.printStackTrace();
		sendSupportMail(toLog, t, null);
	}
	
	public static void errUnexpected(final Throwable t, final String user) {
		SYSTEMLOGGER.log(Level.SEVERE, "Ein unerwarteter Fehler ist aufgetreten", t);
		t.printStackTrace();
		sendSupportMail("Ein unerwarteter Fehler ist aufgetreten", t, user);
	}
	
	public static void sendSupportMail(final String toLog, final Throwable t, String user) {
		
		String supportMailReceiver = (String) EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SUPPORT);
		if (supportMailReceiver.isEmpty()) {
			warn("Property email_support is not set. No email will be sent");
			return;
		}
		if (!(Boolean) EnvironmentConfiguration.getConfiguration(ConfigID.SEND_SUPPORT_MAIL)) {
			warn("Property send_support_mail is set to 'false' => No email will be sent to " + supportMailReceiver);
			return;
		}
		
		final Writer tAsString = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(tAsString);
		t.printStackTrace(printWriter);
		
		Map<String, String> replacements = new HashMap<String, String>();
		if (user == null) {
			user = "-not specified-";
		}
		replacements.put("USER", user);
		replacements.put("MESSAGE", toLog);
		replacements.put("EXCEPTION", tAsString.toString().replace("\n", "<br />"));
		try {
			EmailManager.getInstance().sendEmail(supportMailReceiver, "Survey Platform Support", (String) EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER), "Survey Platform", "Error occured", replacements, Paths.TEMPLATEPATH + "/emailSupport.txt", callBack);
		} catch (EmailException ee) {
			warn("Could not send email to support: " + supportMailReceiver + ": " + ee.getMessage());
		}
	}
	
	public static void logUserActivity(final String toLog, final String userName) {
		userActivity(toLog, userName, Level.INFO);
	}
	
	public static void errUserActivity(final String toLog, final Throwable t, final String userName) {
		userActivity(toLog, userName, Level.SEVERE);
		err(toLog, t);
	}
	
	private static void userActivity(final String toLog, final String userName, final Level level) {
		
		File logFile = new File(Paths.LOGS + "/" + userName + (level == Level.SEVERE ? "_error.log" : "_out.log"));
		
		if (!sizeOfFileIsValid(logFile)) {
			saveExistingFile(logFile);
			logFile.delete();
		}
		
		PrintStream printStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream(), logFile));
		printStream.print(DATEFORMAT.format(System.currentTimeMillis()) + toLog + "\r\n");
		printStream.flush();
		printStream.close();
	}
	
	
	
	private static boolean sizeOfFileIsValid(final File file) {
		if (file.length() > 1000000) {
			return false;
		} else {
			return true;
		}
	}
	
	private static void saveExistingFile(final File file) {
		// remove last .log to get TimeStamp in between for copied log-file
		String newPath = file.getAbsolutePath().replaceFirst("\\.log", "");
		try {
			Streams.copy(new FileInputStream(file), new FileOutputStream(new File(newPath + TimeUtil.getFormattedFileTime() + ".log")), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// private static PrintStream createNewLogFile(final File logFile) {
	// return new PrintStream(new FilteredStream(new ByteArrayOutputStream(), logFile));
	// }
	
}
