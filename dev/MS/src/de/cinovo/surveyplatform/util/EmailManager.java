package de.cinovo.surveyplatform.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.model.CallBack;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * Helper Class for sending emails
 * 
 * @author yschubert
 * 
 */
public class EmailManager {
	
	private static final int MILLIS_DEFERRED = 5000;
	
	private final ConcurrentLinkedQueue<SimpleEmailWithCallBack> queue = new ConcurrentLinkedQueue<SimpleEmailWithCallBack>();
	
	private Object lock = new Object();
	
	private DeferredEmailSender des = null;
	
	private static EmailManager instance;
	
	
	private EmailManager() {
		// singleton
	}
	
	public static EmailManager getInstance() {
		if (EmailManager.instance == null) {
			EmailManager.instance = new EmailManager();
		}
		return EmailManager.instance;
	}
	
	/**
	 * Sends the email with the given data. Each call is deferred there is only
	 * sent one email in a specific time period (5000ms)
	 * 
	 * @param toEmail Receivers Email
	 * @param toName Receivers Name
	 * @param fromEmail Senders Email
	 * @param fromName Senders Name
	 * @param subject Subject of the email
	 * @param replacements Replacements for the Text
	 * @param template FilePath or Content of a Template
	 * @param callBack {@link CallBack} Object for doing after send actions
	 * @throws EmailException When there is something wrong with the email
	 */
	public void sendEmail(final String toEmail, final String toName, final String fromEmail, final String fromName, final String subject, final Map<String, String> replacements, final String template, final CallBack callBack) throws EmailException {
		// First look if in Test-Environment
		if (EnvironmentConfiguration.isTestEnvironment()) {
			// you are testing, so instead of sending E-Mail, write a file
			// the file will be tested as specific as possible
			try {
				String tmpFileDirectory = System.getProperty("java.io.tmpdir");
				File testFile = new File(tmpFileDirectory + "/eMailTest.txt");
				if (testFile.exists()) {
					testFile.delete();
				}
				FileWriter outFile = new FileWriter(testFile);
				PrintWriter out = new PrintWriter(outFile);
				
				out.println(toEmail);
				if (toName == null) {
					out.println("Receiver");
				} else {
					out.println(toName);
				}
				out.println(fromEmail);
				out.println(fromName);
				out.println(subject);
				// TODO nachsehen, was hier eigentlich rauskommt und dann im Test vergleichen, wenn plausibel
				// always add a <p> as Prefix for test Reasons (to cover all TINY_MCE and NON_TINY_MCE generated Mails)
				if (template.startsWith("Templates/invitationMail")) {
					out.print(TemplateUtil.directReplacement("<p>{INVITATION_MESSAGE}::{CLIENT}::{GROUP}::{INVITATION_LINK}", replacements, "\\{", "\\}"));
				} else if (template.startsWith("Templates/userCreatedEMail")) {
					out.print(TemplateUtil.directReplacement("<p>{USERNAME}::{PASSWORD}", replacements, "\\{", "\\}"));
				} else if (template.startsWith("Templates/activationMail")) {
					out.print(TemplateUtil.directReplacement("<p>{HOURS}::{ACTIVATION_LINK}", replacements, "\\{", "\\}"));
				} else if (template.startsWith("Templates/registrationConfirmed")) {
					out.print(TemplateUtil.directReplacement("<p>{LOGIN_LINK}", replacements, "\\{", "\\}"));
				} else if (template.startsWith("Templates/userLostPwMail")) {
					out.print(TemplateUtil.directReplacement("<p>{TOKENLINK}", replacements, "\\{", "\\}"));
				} else if (template.startsWith("Templates/changePassword_confirmation")) {
					out.print(TemplateUtil.directReplacement("<p>{USERNAME}::{NEWPASSWORD}", replacements, "\\{", "\\}"));
				} else {
					// generated (tiny_mce) Mails. No need to add a <p> here.
					out.print(TemplateUtil.directReplacement(template, replacements, "\\{", "\\}"));
				}
				out.close();
			} catch (IOException e) {
				Logger.err(e.getMessage());
				e.printStackTrace();
			}
			
		} else {
			SimpleEmail email = new SimpleEmail();
			String host = (String) EnvironmentConfiguration.getConfiguration(ConfigID.SMTP_HOST);
			if ((host == null) || host.isEmpty()) {
				// do nothing if there is no host configured
				Logger.warn("No smpt_host configured in environment.properties! No emails will be sent!");
				return;
			}
			email.setCharset("UTF-8");
			
			if ((Boolean) EnvironmentConfiguration.getConfiguration(ConfigID.SMTP_SSL_USE)) {
				email.setSSL(true);
				email.setSslSmtpPort((String) EnvironmentConfiguration.getConfiguration(ConfigID.SMTP_SSL_PORT));
			}
			
			email.addHeader("Content-Type", "text/html; charset=UTF-8;");
			email.setHostName(host);
			email.addTo(toEmail, toName);
			email.setAuthentication((String) EnvironmentConfiguration.getConfiguration(ConfigID.SMTP_USERNAME), (String) EnvironmentConfiguration.getConfiguration(ConfigID.SMTP_PASSWORD));
			email.setFrom(fromEmail, fromName);
			email.setSubject(subject);
			if (new File(template).exists()) {
				email.setMsg(TemplateUtil.getTemplate(template, replacements));
			} else {
				email.setMsg(TemplateUtil.directReplacement(template, replacements, "\\{", "\\}"));
			}
			this.addMailToQueue(email, callBack);
		}
	}
	
	private void addMailToQueue(final SimpleEmail email, final CallBack cb) {
		synchronized (this.lock) {
			this.queue.add(new SimpleEmailWithCallBack(email, cb));
			if ((this.des == null) || !this.des.isRunning) {
				this.des = new DeferredEmailSender();
				this.des.start();
			}
		}
	}
	
	
	public class DeferredEmailSender extends Thread {
		
		boolean isRunning = true;
		
		
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			this.isRunning = true;
			while (EmailManager.this.queue.size() > 0) {
				// deferr sending the next email
				try {
					Thread.sleep(EmailManager.MILLIS_DEFERRED);
				} catch (InterruptedException e) {
					// -
				}
				try {
					synchronized (queue) {
						if (!queue.isEmpty()) {
							SimpleEmailWithCallBack simpleEmail = EmailManager.this.queue.poll();
							try {
								simpleEmail.email.send();
								if (simpleEmail.callBack != null) {
									simpleEmail.callBack.doCallBack();
								}
							} catch (EmailException e) {
								if (simpleEmail.callBack == null) {
									throw e;
								} else {
									simpleEmail.callBack.doCallBackFailure(e);
								}
							}
						}
					}
				} catch (Exception e) {
					Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
				}
				
			}
			this.isRunning = false;
		}
		
	}
	
	public class SimpleEmailWithCallBack {
		
		SimpleEmail email;
		CallBack callBack;
		
		
		public SimpleEmailWithCallBack(final SimpleEmail email, final CallBack callBack) {
			this.email = email;
			this.callBack = callBack;
		}
	}
}
