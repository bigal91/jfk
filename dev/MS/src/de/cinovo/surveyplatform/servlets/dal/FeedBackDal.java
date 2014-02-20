/**
 *
 */
package de.cinovo.surveyplatform.servlets.dal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.TaskInfo;
import de.cinovo.surveyplatform.model.jsondto.TaskInfoDto;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;



/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class FeedBackDal extends AbstractSccServlet {
	
	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_TASKID = "taskId";
	private static final String PARAM_TASKRESULT = "result";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @seescc.servlets.AbstractSccServlet#processRetrieve(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		if (ParamUtil.checkAllParamsSet(req, PARAM_TASKID)) {
			
			String sessionId = req.getSession().getId();
			String taskId = req.getParameter(PARAM_TASKID);
			if (!taskId.endsWith(sessionId)) {
				taskId += "." + sessionId;
			}
			FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
			
			if (ParamUtil.checkAllParamsSet(req, PARAM_TASKRESULT)) {
				TaskInfo taskInfo = feedBackProvider.getTaskInfo(taskId);
				resp.getWriter().print(taskInfo.getLongTaskResult());
			} else {
				
				if (req.getSession() != null) {
					if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
						TaskInfoDto taskInfoDto = feedBackProvider.getTaskInfoDto(taskId);
						if (taskInfoDto == null) {
							// see if there are system messages
							taskInfoDto = feedBackProvider.getTaskInfoDto("system." + req.getSession().getId());
						}
						if (taskInfoDto != null) {
							resp.getWriter().print(taskInfoDto.getJSON());
						}
					} else {
						TaskInfo taskInfo = feedBackProvider.getTaskInfo(taskId);
						
						if (taskInfo == null) {
							// see if there are system messages
							taskInfo = feedBackProvider.getTaskInfo("system." + req.getSession().getId());
							if (taskInfo != null) {
								taskId = taskInfo.getTaskID();
							}
						}
						
						if (taskInfo == null) {
							resp.getWriter().print("");
						} else {
							Map<String, String> replacements = new HashMap<String, String>();
							replacements.put("STATUS", taskInfo.getStatusCode().name());
							StringBuilder results = new StringBuilder();
							// the currentTimeMillis is for preventing the browser
							// of caching this
							results.append("<span style=\"font-weight: bold;\" id=\"fb" + System.currentTimeMillis() + "\">" + taskInfo.getMessage() + "</span>");
							for (String result : taskInfo.getResults()) {
								results.append(result);
							}
							replacements.put("FEEDBACKITEMS", results.toString());
							resp.getWriter().print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/feedBackContainer.html", replacements));
							feedBackProvider.removeTaskInfo(taskId);
						}
					}
				}
			}
		}
	}
	
}
