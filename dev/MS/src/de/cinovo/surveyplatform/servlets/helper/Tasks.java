/**
 * 
 */
package de.cinovo.surveyplatform.servlets.helper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.TaskInfo;
import de.cinovo.surveyplatform.util.AuthUtil;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class Tasks extends HttpServlet {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (AuthUtil.hasRight(AuthUtil.checkAuth(req), UserRights.ADMINISTRATOR)) {
			FeedBackProvider fbp = FeedBackProvider.getInstance();
			Map<String, TaskInfo> taskMap = fbp.getTaskMap();
			PrintWriter writer = resp.getWriter();
			writer.print("<html><head><meta http-equiv=\"refresh\" content=\"1;\" /></head><body><pre>");
			for (TaskInfo taskInfo : taskMap.values()) {
				
				writer.println("-------------------------------------------------------");
				writer.println("TaskID: " + taskInfo.getTaskID());
				writer.println("Task name: " + taskInfo.getTaskName());
				writer.println("Message: " + taskInfo.getMessage());
				writer.println("Statuscode: " + taskInfo.getStatusCode());
				writer.println("Age: " + taskInfo.getAgeByCreationTime());
				long ttl = (FeedBackProvider.MAX_TASKINFO_AGE - taskInfo.getAgeByFinishTime());
				if (ttl < 0) {
					writer.print("<span style=\"color: red;\">");
				}
				writer.print("TTL: " + (ttl > 0 ? ttl : "gets removed with next task"));
				if (ttl < 0) {
					writer.print("</span>");
				}
				writer.println("");
				writer.print("Progress: ");
				for (int i = 0; i < (int) Math.round((double) taskInfo.getProgress() / 2); i++) {
					writer.print("#");
				}
				writer.println(" " + taskInfo.getProgress() + "%");
				synchronized (taskInfo) {
					for (String feedBack : taskInfo.getResults()) {
						writer.println("  " + feedBack);
					}
				}
			}
			writer.print("</pre></body></html>");
		} else {
			resp.sendRedirect(EnvironmentConfiguration.getHostAndBase());
		}
	}
	
}
