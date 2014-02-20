package de.cinovo.surveyplatform.ui.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.TaskInfo;
import de.cinovo.surveyplatform.feedback.TaskInfo.StatusCode;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.IDecisionQuestion;
import de.cinovo.surveyplatform.model.question.IMultipleOptionsQuestion;
import de.cinovo.surveyplatform.model.reporting.AbstractReport;
import de.cinovo.surveyplatform.model.reporting.AccessReportFromDB;
import de.cinovo.surveyplatform.model.reporting.GenericReportInfo;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.model.reporting.ReportType;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;
import de.cinovo.surveyplatform.reporting.reports.GenericReport;
import de.cinovo.surveyplatform.reporting.reports.GenericReport.Type;
import de.cinovo.surveyplatform.reporting.reports.SummaryReport;
import de.cinovo.surveyplatform.servlets.DownloadServlet;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.SurveyUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 *
 * Copyright 2010 Cinovo AG<br>
 * <br>
 *
 * @author yschubert
 *
 */
public class AnalyseReportsContainer extends AbstractContainer {
	
	public static final String PARAM_OPEN = "open";
	public static final String PARAM_PDF = "pdf";
	public static final String PARAM_REPORTSUBTYPE = "subtype";
	
	private static final ExecutorService threadExecutor = Executors.newCachedThreadPool();
	private static final List<String> currentRunning = new ArrayList<String>();
	
	
	private static final String PARAM_DELETE = "delete";
	
	
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		
		if (currentUser != null) {
			
			if (AuthUtil.isAllowedToViewReports(currentUser)) {
				if (ParamUtil.checkAllParamsSet(request, SurveyDal.PARAM_SURVEYID)) {
					int surveyId = ParamUtil.getSafeIntFromParam(request, SurveyDal.PARAM_SURVEYID);
					if (surveyId > 0) {
						showSurveyReports(surveyId, request, content);
					} else {
						showSurveyOverview(request, content);
					}
				} else {
					showSurveyOverview(request, content);
				}
			} else {
				content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
			}
		}
	}
	
	private void showSurveyOverview(final HttpServletRequest request, final StringBuilder content) {
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_ANALYSE_REPORTS, HelpIDs.PAGE_ANALYSE));
		
		Map<String, String> replacements = new HashMap<String, String>();
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			// only show surveys the user has created or all if user may
			// edit all surveys
			SystemUser currentUser = AuthUtil.checkAuth(request);
			
			Criteria criteria = hibSess.createCriteria(Survey.class);
			
			// do not show templates
			criteria.add(Restrictions.ge("state", SurveyState.RUNNING));
			
			if (!AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
				criteria.add(Restrictions.in("owner", GroupManager.getVisibleGroups(hibSess, currentUser, currentUser)));
			}
			
			criteria.addOrder(Order.asc("name"));
			
			Map<String, String> rowReplacements = new HashMap<String, String>();
			StringBuilder tableRows = new StringBuilder();
			
			Map<String, String> rowDeletedReplacements = new HashMap<String, String>();
			StringBuilder tableDeletedRows = new StringBuilder();
			
			List<?> list = criteria.list();
			for (Object obj : list) {
				if (obj instanceof Survey) {
					Survey survey = (Survey) obj;
					Map<String, String> currentRowMap = null;
					if (survey.isDeleted()) {
						currentRowMap = rowDeletedReplacements;
					} else {
						currentRowMap = rowReplacements;
					}
					currentRowMap.put("NAME", "<td><a href=\"javascript:void(0);\" onclick=\"showsurveyInfo(" + survey.getId() + ");\">" + survey.getName() + "</a></td>");
					currentRowMap.put("OWNER", "<td>" + survey.getOwner().getName() + "</td>");
					// currentRowMap.put("STATE", "<td>" + survey.getStateDisplayname() + "</td>");
					currentRowMap.put("STATE", "<td>" + survey.getStateDisplayname() + "</td>");
					currentRowMap.put("HIGHLIGHT", "<td></td>");
					int progress = 0;
					if (survey.getState().ordinal() > SurveyState.CREATED.ordinal()) {
						progress = SurveyUtil.calculateReturnRate(currentUser, survey);
						currentRowMap.put("PROGRESS", "<td>" + PartsUtil.getProgressBar(progress) + "</td>");
					} else {
						currentRowMap.put("PROGRESS", "<td></td>");
					}
					
					currentRowMap.put("SURVEYID", survey.getId() + "");
					
					StringBuilder buttons = new StringBuilder();
					buttons.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTableButton_analyseSurvey.html", currentRowMap));
					buttons.append(PartsUtil.getIconLink(" inTableAction", "Click to export the collected raw data to an MS-Excel&reg; document", "Export", EnvironmentConfiguration.getUrlBase() + "/download?type=rawdata&" + SurveyDal.PARAM_SURVEYID + "=" + survey.getId(), false, null, false));
					currentRowMap.put("BUTTONS", buttons.toString());
					currentRowMap.put("WIDTH", "1");
					if (survey.isDeleted()) {
						tableDeletedRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_row.html", rowDeletedReplacements));
					} else {
						tableRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_row.html", rowReplacements));
					}
				}
			}
			tx.commit();
			if ((tableRows.length() == 0) && (tableDeletedRows.length() == 0)) {
				replacements.put("INFORMATION", "<p class=\"innerContainer info\">There are currently no surveys to analyse. The surveys can be analysed once they have been started.</p>");
			}
			
			if (tableRows.length() > 0) {
				replacements.put("SECTIONHEADER", "Survey Reports");
				replacements.put("ROWS_RUNNING", tableRows.toString());
				replacements.put("SHOW_ALL_STYLE", ",\"sDom\": 'fprt<\"bottom\"i>'");
				replacements.put("TABLE_RUNNING_SURVEYS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_running.html", replacements));
			}
			if (tableDeletedRows.length() > 0) {
				replacements.put("SECTIONHEADER", "Deleted Surveys");
				replacements.put("ROWS_RUNNING", tableDeletedRows.toString());
				replacements.put("IDSUFFIX", "Deleted");
				
				StringBuilder deletedContainer = new StringBuilder();
				deletedContainer.append("<div style=\"float:right;\"><a class=\"link\" href=\"javascript:void(0);\" onclick=\"$('#deletedSurveysContainer').slideToggle(); $('#showDeletedSurveysContainer').toggle(); $('#hideDeletedSurveysContainer').toggle();\">" + PartsUtil.getIcon("BIN_EMPTY", "") + " <span id=\"showDeletedSurveysContainer\">Show</span><span id=\"hideDeletedSurveysContainer\" style=\"display:none;\">Hide</span> Deleted Surveys</a></div>");
				deletedContainer.append("<div style=\"clear: both; height: 5px;\"></div>");
				deletedContainer.append("<div style=\"display:none;\" id=\"deletedSurveysContainer\">");
				deletedContainer.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_running.html", replacements));
				deletedContainer.append("</div>");
				
				replacements.put("TABLE_CLOSED_SURVEYS", deletedContainer.toString());
			}
			
			replacements.put("BUTTONCELL", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/buttonCell.html", null));
			replacements.put("ENABLEDIALOGBUTTONS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfoEnableDialogButtons.html", null));
			replacements.put("DIALOGSURVEYINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfo.html", replacements));
			replacements.put("BUTTONCOLUMNWIDTH", "80");
			replacements.put("SURVEYTABLE", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable.html", replacements));
			
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/analyseReports.html", replacements));
			
		} finally {
			hibSess.close();
		}
	}
	
	private void showSurveyReports(final int surveyId, final HttpServletRequest request, final StringBuilder content) {
		
		SystemUser currentUser = AuthUtil.checkAuth(request);
		
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			
			Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
			
			// associate user with the current session
			// DO NOT USE hibSess.merge() because this will
			// overwrite data of the user when the user is logged
			// in multiple times!!
			currentUser = (SystemUser) hibSess.load(SystemUser.class, currentUser.getUserName());
			
			if (AuthUtil.isAllowedToViewReports(currentUser, survey, hibSess)) {
				
				if (ParamUtil.checkAllParamsSet(request, PARAM_OPEN)) {
					String reportID = request.getParameter(PARAM_OPEN);
					
					if (ParamUtil.checkAllParamsSet(request, PARAM_PDF)) {
						SubTypeEnum subType = SubTypeEnum.COMBINED;
						if (ParamUtil.checkAllParamsSet(request, PARAM_REPORTSUBTYPE)) {
							subType = SubTypeEnum.valueOf(request.getParameter(PARAM_REPORTSUBTYPE).toUpperCase());
						}
						String taskID = "generator_" + subType.name().toLowerCase() + "." + reportID + "." + survey.getId() + "." + request.getSession().getId();
						createPDFReport(reportID, subType, survey, currentUser, content, taskID);
					} else if (ParamUtil.checkAllParamsSet(request, PARAM_DELETE)) {
						deleteReport(reportID, survey, currentUser, content);
						buildShowReportsPage(currentUser, content, survey, surveyId);
					} else {
						String taskID = "generator." + reportID + "." + survey.getId() + "." + request.getSession().getId();
						
						showReportDetails(reportID, survey, currentUser, content, taskID);
					}
				} else {
					buildShowReportsPage(currentUser, content, survey, surveyId);
					
				}
				
			} else {
				content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
			}
			tx.commit();
		} finally {
			hibSess.close();
		}
	}
	
	/**
	 * @param content
	 * @param survey
	 */
	private void buildShowReportsPage(final SystemUser currentUser, final StringBuilder content, final Survey survey, final Integer surveyId) {
		content.append(PartsUtil.getPageHeader(survey.getName(), HelpIDs.PAGE_ANALYSE_DETAIL, new String[] {"<a href=\"?page=" + Pages.PAGE_REPORTS + "\">" + Pages.PAGE_HEADER_ANALYSE_REPORTS + "</a>"}));
		
		Map<String, String> replacements = new HashMap<String, String>();
		StringBuilder tableReports = new StringBuilder();
		// StringBuilder tableAvailable = new StringBuilder();
		
		// --------------------------------------------------------------------------------------------------------------------
		// get available reports from the registry
		List<AbstractReport> reports = new ArrayList<AbstractReport>();
		// adding default Report: SummaryReport
		SummaryReport sumRep = new SummaryReport();
		List<ReportType> repType = new ArrayList<ReportType>();
		repType.add(new ReportType(SubTypeEnum.QUANTITATIVE, "Quantitative Report"));
		repType.add(new ReportType(SubTypeEnum.QUALITATIVE, "Qualitative Report"));
		sumRep.setSubTypes(repType);
		reports.add(sumRep);
		
		try {
			
			Session hibReportListSess = HibernateUtil.getSessionFactory().openSession();
			try {
				// add default: new SummaryReport in list
				Criteria criteria = hibReportListSess.createCriteria(GenericReportInfo.class);
				criteria.addOrder(Order.asc("name"));
				
				List<?> list = criteria.list();
				
				for (Object obj : list) {
					if (obj instanceof GenericReportInfo) {
						GenericReportInfo currentReport = (GenericReportInfo) obj;
						GenericReport actualReport = new GenericReport();
						actualReport.setId(String.valueOf((currentReport.getId())));
						actualReport.setName(currentReport.getName());
						List<ReportType> subTypes = new ArrayList<ReportType>();
						if (currentReport.getRepInfoType().equals(Type.PERFORMANCE)) {
							subTypes.add(new ReportType(SubTypeEnum.QUANTITATIVE, "Quantitative Report"));
							actualReport.setSubTypes(subTypes);
						} else {
							subTypes.add(new ReportType(SubTypeEnum.QUANTITATIVE, "Quantitative Report"));
							subTypes.add(new ReportType(SubTypeEnum.QUALITATIVE, "Qualitative Report"));
							actualReport.setSubTypes(subTypes);
						}
						if (currentReport.getDescription() != null) {
							actualReport.setDescription(currentReport.getDescription());
						} else {
							actualReport.setDescription("");
						}
						// only add reports from database, that belong to
						// the current Survey
						if (currentReport.getSurveyId() == surveyId) {
							reports.add(actualReport);
						}
					}
				}
			} finally {
				hibReportListSess.close();
			}
		} catch (Exception e) {
			Logger.err("Could not read access GenericReportInfo from database", e);
		}
		
		tableReports.append("<div class=\"innerContainer\">" + PartsUtil.getPageSectionDivider("Available Reports", false) + "</div>");
		if ((reports != null) && (reports.size() > 0)) {
			StringBuilder rows = new StringBuilder();
			for (AbstractReport currentReport : reports) {
				
				replacements.put("REPORT_NAME", currentReport.getName());
				replacements.put("REPORT_DESCRIPTION", currentReport.getDescription());
				StringBuilder buttons = new StringBuilder();
				String taskID = "generator." + currentReport.getId() + "." + survey.getId();
				String baseLink = "?page=" + Pages.PAGE_REPORTS + "&amp;" + SurveyDal.PARAM_SURVEYID + "=" + surveyId + "&amp;" + PARAM_OPEN + "=" + currentReport.getId();
				String baseUrl = getServerBaseUrl();
				buttons.append(PartsUtil.getIconLink("OPEN_REPORT", "Open report", "", baseLink, false, taskID, true));
				if (currentReport.getSubTypes() == null) {
					buttons.append(PartsUtil.getIconLink("PDF", "Download Report as PDF", "", baseLink + "&amp;" + PARAM_PDF + "=1", false, null, false));
				} else {
					
					String availableTypes = "__pdfSubTypes = new Array(" + (currentReport.getSubTypes().size() > 1 ? "'COMBINED'," : "");
					for (ReportType subType : currentReport.getSubTypes()) {
						availableTypes += "'" + subType.getId() + "',";
					}
					availableTypes += "'');";
					buttons.append("<a id=\"rep_" + currentReport.getId() + "\" class=\"gui-icon-button-PDF\" title=\"Download Report as PDF\" href=\"javascript:void(0);\" onclick=\"" + availableTypes + "__pdfLinkPrefix = '" + baseLink + "&amp;" + PARAM_PDF + "=1" + "'; $('#chooseReportType').dialog('open');\"></a>");
				}
				replacements.put("BASEURL", baseUrl);
				if (!currentReport.getId().equals("report_summary")) {
					buttons.append(PartsUtil.getIconLink("BIN_EMPTY", "Delete this report", "", "javascript:confirmDeleteReport('" + currentReport.getId() + "')", false, null, false));
				}
				replacements.put("BUTTONS", buttons.toString());
				rows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/reportTableRow.html", replacements));
				
			}
			replacements.put("ROWS", rows.toString());
			tableReports.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgChooseReportType.html", null));
			
			tableReports.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableReports.html", replacements));
		} else {
			tableReports.append("No reports available.");
		}
		
		replacements.put("TABLE_REPORTS", tableReports.toString());
		
		// build possible performance questions
		StringBuilder perfQuestions = new StringBuilder();
		for (int pageIndex = 0; pageIndex < survey.getQuestionnaire().getPages().size(); pageIndex++) {
			for (int sectionIndex = 0; sectionIndex < survey.getQuestionnaire().getPages().get(pageIndex).getSections().size(); sectionIndex++) {
				for (AbstractQuestion aq : survey.getQuestionnaire().getPages().get(pageIndex).getSections().get(sectionIndex).getQuestions()) {
					
					AbstractQuestion currentQuestion = aq;
					if (aq instanceof IMultipleOptionsQuestion) {
						perfQuestions.append("<option value=\"qs" + currentQuestion.getLocalId() + "\" data-order=\"" + currentQuestion.getOrderNumber() + "\">" + currentQuestion.getQuestion() + "</option>");
					}
				}
			}
		}
		// build possible deviation questions
		StringBuilder devQuestions = new StringBuilder();
		for (int pageIndex = 0; pageIndex < survey.getQuestionnaire().getPages().size(); pageIndex++) {
			for (int sectionIndex = 0; sectionIndex < survey.getQuestionnaire().getPages().get(pageIndex).getSections().size(); sectionIndex++) {
				for (AbstractQuestion aq : survey.getQuestionnaire().getPages().get(pageIndex).getSections().get(sectionIndex).getQuestions()) {
					
					AbstractQuestion currentQuestion = aq;
					if (aq instanceof IDecisionQuestion) {
						devQuestions.append("<option value=\"qs" + currentQuestion.getLocalId() + "\" data-order=\"" + currentQuestion.getOrderNumber() + "\">" + currentQuestion.getQuestion() + "</option>");
					}
				}
			}
		}
		if (!devQuestions.toString().equals("")) {
			replacements.put("DEVIATION_QUESTIONS_DROPDOWN", devQuestions.toString());
		} else {
			replacements.put("DEVIATION_QUESTIONS_DROPDOWN", "<option value=\"empty\">There are no questions for this Report-Type available in this survey.</option>");
		}
		if (!perfQuestions.toString().equals("")) {
			replacements.put("PERFORMANCE_QUESTIONS_DROPDOWN", perfQuestions.toString());
		} else {
			replacements.put("PERFORMANCE_QUESTIONS_DROPDOWN", "<option value=\"empty\">There are no questions for this Report-Type available in this survey.</option>");
		}
		
		replacements.put("SURVEYID", String.valueOf(surveyId));
		if (AuthUtil.isAllowedToCreateReports(currentUser)) {
			replacements.put("DLGCREATEREPORT", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgCreateReport.html", replacements));
			String createReportButton = "<button id=\"bCreateReportType\">Create Report</button>";
			replacements.put("CREATE_REPORT", createReportButton);
		}
		
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/reports.html", replacements));
	}
	
	private void deleteReport(final String reportID, final Survey survey, final SystemUser currentUser, final StringBuilder content) {
		// Session hibSess = HibernateUtil.getSessionFactory().openSession();
		if (currentUser != null) {
			Session hibSess = HibernateUtil.getSessionFactory().getCurrentSession();
			Transaction tx = hibSess.beginTransaction();
			
			GenericReportInfo reportToDelete = (GenericReportInfo) hibSess.load(GenericReportInfo.class, Integer.parseInt(reportID));
			hibSess.delete(reportToDelete);
			
			tx.commit();
			hibSess.close();
		}
	}
	
	private void createPDFReport(final String reportId, final SubTypeEnum subType, final ISurvey survey, final SystemUser currentUser, final StringBuilder content, final String taskID) {
		
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Criteria genericCriteria = hibSess.createCriteria(GenericReportInfo.class);
			
			GenericReportInfo report = null;
			List<?> genRepInfoList = genericCriteria.list();
			for (Object obj : genRepInfoList) {
				if (obj instanceof GenericReportInfo) {
					GenericReportInfo repInfo = (GenericReportInfo) obj;
					if (String.valueOf(repInfo.getId()).equals(reportId)) {
						report = repInfo;
						break;
					}
				}
			}
			
			if (report == null) {
				content.append(PartsUtil.getPageHeader("<a href=\"?page=" + Pages.PAGE_REPORTS + "&" + SurveyDal.PARAM_SURVEYID + "=" + survey.getId() + "\">" + survey.getName() + "</a>: Summary Report", HelpIDs.PAGE_ANALYSE_DETAIL, new String[] {"<a href=\"?page=" + Pages.PAGE_REPORTS + "\">" + Pages.PAGE_HEADER_ANALYSE_REPORTS + "</a>"}));
			} else {
				content.append(PartsUtil.getPageHeader("<a href=\"?page=" + Pages.PAGE_REPORTS + "&" + SurveyDal.PARAM_SURVEYID + "=" + survey.getId() + "\">" + survey.getName() + "</a>: " + report.getName(), HelpIDs.PAGE_ANALYSE_DETAIL, new String[] {"<a href=\"?page=" + Pages.PAGE_REPORTS + "\">" + Pages.PAGE_HEADER_ANALYSE_REPORTS + "</a>"}));
			}
			
			if (!currentRunning.contains(taskID)) {
				currentRunning.add(taskID);
				Runnable generatorRunnable = new Runnable() {
					
					public void run() {
						
						Session hibSess = HibernateUtil.getSessionFactory().openSession();
						try {
							Survey s = (Survey) hibSess.get(Survey.class, survey.getId());
							try {
								TaskInfo taskInfo = FeedBackProvider.getInstance().getTaskInfo(taskID);
								if (taskInfo != null) {
									taskInfo.setStatusCode(StatusCode.IDLE);
								}
								DownloadServlet.createReport(s, reportId, subType, taskID, false);
								String link = EnvironmentConfiguration.getUrlBase() + "/download?type=report&amp;subtype=" + subType.name().toLowerCase() + "&amp;" + SurveyDal.PARAM_SURVEYID + "=" + s.getId() + "&amp;reportId=" + reportId + "&amp;overrideCreate=true";
								FeedBackProvider.getInstance().getTaskInfo(taskID).setLongTaskResult(link);
							} catch (Exception ex) {
								Logger.err("Cannot evaluate the report " + reportId + " for: " + s.getName(), ex);
							}
						} finally {
							currentRunning.remove(taskID);
							hibSess.close();
						}
					}
					
				};
				threadExecutor.execute(generatorRunnable);
			}
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("TASKID", taskID);
			replacements.put("MESSAGE", "Generating PDF Report for " + survey.getName());
			replacements.put("MD5TASKID", AuthUtil.md5(taskID));
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/downloadResultPoller.html", replacements));
			
		} finally {
			hibSess.close();
		}
	}
	
	private void showReportDetails(final String reportId, final ISurvey survey, final SystemUser currentUser, final StringBuilder content, final String taskID) {
		GenericReportInfo reportInfo = new GenericReportInfo();
		final GenericReport report = new GenericReport();
		final SummaryReport sumReport = new SummaryReport();
		if (reportId.equals("report_summary")) {
			content.append(PartsUtil.getPageHeader("<a href=\"?page=" + Pages.PAGE_REPORTS + "&" + SurveyDal.PARAM_SURVEYID + "=" + survey.getId() + "\">" + survey.getName() + "</a>: Summary_Report", HelpIDs.PAGE_ANALYSE_DETAIL, new String[] {"<a href=\"?page=" + Pages.PAGE_REPORTS + "\">" + Pages.PAGE_HEADER_ANALYSE_REPORTS + "</a>"}));
		} else {
			reportInfo = AccessReportFromDB.getInstance().getReportInfoById(Integer.parseInt(reportId), null);
			report.setId(String.valueOf(reportInfo.getId()));
			report.setName(reportInfo.getName());
			report.setReportType(reportInfo.getRepInfoType());
			content.append(PartsUtil.getPageHeader("<a href=\"?page=" + Pages.PAGE_REPORTS + "&" + SurveyDal.PARAM_SURVEYID + "=" + survey.getId() + "\">" + survey.getName() + "</a>: " + reportInfo.getName(), HelpIDs.PAGE_ANALYSE_DETAIL, new String[] {"<a href=\"?page=" + Pages.PAGE_REPORTS + "\">" + Pages.PAGE_HEADER_ANALYSE_REPORTS + "</a>"}));
		}
		
		if (report != null) {
			Logger.info("Report Open (" + reportId + "): " + currentUser + " - " + survey.getName());
			Logger.logUserActivity("Report Open (" + reportId + "): " + survey.getName(), currentUser.getUserName());
			if (!currentRunning.contains(taskID)) {
				currentRunning.add(taskID);
				Runnable generatorRunnable = new Runnable() {
					
					public void run() {
						Session hibSess = HibernateUtil.getSessionFactory().openSession();
						try {
							Survey s = (Survey) hibSess.get(Survey.class, survey.getId());
							
							try {
								// if the task was, to open a summary report,
								// open the new summary report instead of
								// generic report
								if (reportId.equals("report_summary")) {
									List<ReportType> subTypes = new ArrayList<ReportType>();
									subTypes.add(new ReportType(SubTypeEnum.QUANTITATIVE, "Quantitative Report"));
									subTypes.add(new ReportType(SubTypeEnum.QUALITATIVE, "Qualitative Report"));
									sumReport.setSubTypes(subTypes);
									StringBuilder evaluate = sumReport.evaluate(s, TargetMedia.SCREEN, taskID, report.getReportType(), SubTypeEnum.COMBINED);
									FeedBackProvider.getInstance().getTaskInfo(taskID).setLongTaskResult(evaluate.toString());
								} else {
									List<ReportType> subTypes = new ArrayList<ReportType>();
									subTypes.add(new ReportType(SubTypeEnum.QUANTITATIVE, "Quantitative Report"));
									if (!Type.PERFORMANCE.equals(report.getReportType())) {
										subTypes.add(new ReportType(SubTypeEnum.QUALITATIVE, "Qualitative Report"));
									}
									sumReport.setSubTypes(subTypes);
									
									StringBuilder evaluate = report.evaluate(s, TargetMedia.SCREEN, taskID, report.getReportType(), SubTypeEnum.COMBINED);
									FeedBackProvider.getInstance().getTaskInfo(taskID).setLongTaskResult(evaluate.toString());
								}
							} catch (Exception ex) {
								Logger.err("Cannot evaluate the report " + reportId + " for: " + s.getName(), ex);
							}
							
						} finally {
							currentRunning.remove(taskID);
							hibSess.close();
						}
					}
				};
				threadExecutor.execute(generatorRunnable);
			}
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("TASKID", taskID);
			replacements.put("MD5TASKID", AuthUtil.md5(taskID));
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/reportResultPoller.html", replacements));
		}
	}
	
	private String getServerBaseUrl() {
		return (String) EnvironmentConfiguration.getConfiguration(ConfigID.HOST) + (String) EnvironmentConfiguration.getConfiguration(ConfigID.URLBASE);
	}
}
