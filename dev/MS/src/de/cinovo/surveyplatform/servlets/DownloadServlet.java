package de.cinovo.surveyplatform.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.googlecode.htmlcompressor.compressor.XmlCompressor;
import com.lowagie.text.Anchor;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.exporter.ParticipationExporter;
import de.cinovo.surveyplatform.exporter.TopicExporter;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Participation;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.SafeFilenameFile;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.reporting.AbstractReport;
import de.cinovo.surveyplatform.model.reporting.AccessReportFromDB;
import de.cinovo.surveyplatform.model.reporting.GenericReportInfo;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.model.reporting.ReportType;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;
import de.cinovo.surveyplatform.reporting.reports.GenericReport;
import de.cinovo.surveyplatform.reporting.reports.GenericReport.Type;
import de.cinovo.surveyplatform.reporting.reports.SummaryReport;
import de.cinovo.surveyplatform.servlets.dal.ParticipantDal;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.SessionManager;
import de.cinovo.surveyplatform.util.SurveyUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;
import de.cinovo.surveyplatform.util.WikiUtil;
import de.cinovo.surveyplatform.util.XhtmlEntityResolver;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class DownloadServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final String DOWNLOADTYPE_REPORT = "report";
	
	private static final String DOWNLOADTYPE_QUESTIONNAIRE = "questionnaire";
	
	private static final String DOWNLOADTYPE_PHONELIST = "phonelist";
	
	private static final String DOWNLOADTYPE_TOPICS = "topics";
	
	private static final String DOWNLOADTYPE_RAWDATA = "rawdata";
	
	private static final String DOWNLOADTYPE_GRAPH = "graph";
	
	private static final String DOWNLOADTYPE_FREEMIND = "freemind";
	
	private static final String DOWNLOADTYPE_PARTICIPATIONS = "participations";
	
	private static final String PARAM_COUNT = "count";
	
	public static final String PARAM_DOWNLOADTYPE = "type";
	
	public static final String PARAM_REPORTID = "reportId";
	
	private static final String PARAM_REPORTSUBTYPE = "subtype";
	
	private static final String PARAM_REFQUESTIONID = "refQuestionId";
	
	private static final String PARAM_FILEPATH = "filePath";
	
	private static final String PARAM_OVERRIDECREATE = "overrideCreate";
	
	
	
	private static final String PARAM_PARTICIPATIONID = "pid";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		this.doGet(req, resp);
	}
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.setCharacterEncoding("UTF-8");
		SystemUser user = AuthUtil.checkAuth(req);
		File outputFile = null;
		boolean deleteOutputFile = false;
		if (ParamUtil.checkAllParamsSet(req, DownloadServlet.PARAM_DOWNLOADTYPE)) {
			String downloadType = req.getParameter(DownloadServlet.PARAM_DOWNLOADTYPE);
			
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				if (user == null) {
					if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_QUESTIONNAIRE)) {
						String participationID = req.getParameter(DownloadServlet.PARAM_PARTICIPATIONID);
						outputFile = createFilledQuestionnaire(participationID, hibSess);
						deleteOutputFile = true;
					} else {
						Logger.err("Download not possible.");
						throw new SecurityException("Permission denied. Please ensure you are logged in and have appropriate rights to start the download");
					}
					
				} else {
					Survey survey = null;
					if (ParamUtil.checkAllParamsSet(req, SurveyDal.PARAM_SURVEYID)) {
						int surveyId = ParamUtil.getSafeIntFromParam(req, SurveyDal.PARAM_SURVEYID);
						Transaction tx = hibSess.beginTransaction();
						
						survey = (Survey) hibSess.get(Survey.class, surveyId);
						
						if (!AuthUtil.isAllowedToViewReports(user, survey, hibSess)) {
							survey = null;
						}
						tx.commit();
					}
					
					// Create File
					if (survey == null) {
						Logger.logUserActivity("Download initiated: " + downloadType, user.getUserName());
						if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_TOPICS) && ParamUtil.checkAllParamsSet(req, DownloadServlet.PARAM_REFQUESTIONID)) {
							int refQuestionId = ParamUtil.getSafeIntFromParam(req, DownloadServlet.PARAM_REFQUESTIONID);
							outputFile = this.createTopicDocument(refQuestionId, req, user);
							deleteOutputFile = true;
						} else if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_FREEMIND) && ParamUtil.checkAllParamsSet(req, DownloadServlet.PARAM_REFQUESTIONID)) {
							int refQuestionId = ParamUtil.getSafeIntFromParam(req, DownloadServlet.PARAM_REFQUESTIONID);
							outputFile = this.createTopicDocument(refQuestionId, req, user);
							deleteOutputFile = true;
						} else if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_QUESTIONNAIRE)) {
							String participationID = req.getParameter(DownloadServlet.PARAM_PARTICIPATIONID);
							outputFile = createFilledQuestionnaire(participationID, hibSess);
							deleteOutputFile = true;
						}
					} else {
						Logger.logUserActivity("Download initiated: " + downloadType + "(" + survey.getName() + " [id:" + survey.getId() + "])", user.getUserName());
						if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_PHONELIST)) {
							outputFile = this.createPhoneList(survey, req, user);
							deleteOutputFile = true;
						} else if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_QUESTIONNAIRE)) {
							outputFile = this.createQuestionnaire(survey, req, user);
							deleteOutputFile = true;
						} else if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_PARTICIPATIONS)) {
							outputFile = this.createParticipationXML(survey, req, user);
							deleteOutputFile = true;
						} else if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_RAWDATA)) {
							outputFile = this.createRawDataDocument(survey, req, user);
							Logger.info("Rawdata Download: " + user + " - Survey: " + survey.getName());
							deleteOutputFile = true;
						} else if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_REPORT) && ParamUtil.checkAllParamsSet(req, DownloadServlet.PARAM_REPORTID)) {
							String reportId = req.getParameter(DownloadServlet.PARAM_REPORTID);
							SubTypeEnum type = SubTypeEnum.COMBINED;
							Boolean overrideCreate = Boolean.parseBoolean(req.getParameter(DownloadServlet.PARAM_OVERRIDECREATE));
							if (ParamUtil.checkAllParamsSet(req, DownloadServlet.PARAM_REPORTSUBTYPE)) {
								type = SubTypeEnum.valueOf(req.getParameter(DownloadServlet.PARAM_REPORTSUBTYPE).toUpperCase());
							}
							String taskID = "generator_" + type.name().toLowerCase() + "." + reportId + "." + survey.getId() + "." + req.getSession().getId();
							Logger.info("Report Download: " + user + " - Report: " + reportId + "_" + type.name().toLowerCase());
							
							outputFile = DownloadServlet.createReport(survey, reportId, type, taskID, overrideCreate);
						} else if (downloadType.equals(DownloadServlet.DOWNLOADTYPE_GRAPH)) {
							outputFile = new File(Paths.WEBCONTENT + "/" + req.getParameter(DownloadServlet.PARAM_FILEPATH));
						}
						
					}
				}
			} finally {
				hibSess.close();
			}
			// Send File
			if (outputFile != null) {
				if (outputFile.exists()) {
					this.setHeaderForStreaming(resp, outputFile);
					
					try {
						this.streamFile(resp, outputFile, deleteOutputFile);
					} catch (Exception ex) {
						Logger.err("Could not stream the PDF " + outputFile.getAbsolutePath() + " to HTTP Response:", ex);
					}
				} else {
					Logger.err("Cannot download the file " + outputFile.getAbsolutePath() + ". It does not exist.");
					resp.getWriter().print("Sorry, but the file (" + outputFile.getAbsolutePath() + ") you are trying to download does not exist.");
				}
			}
		} else {
			throw new SecurityException("Permission denied. Please ensure you are logged in and have appropriate rights to start the download");
		}
		
	}
	
	
	private void streamFile(final HttpServletResponse resp, final File outputFile, final boolean deleteOutputFile) throws IOException {
		FileInputStream fis = new FileInputStream(outputFile);
		OutputStream out = resp.getOutputStream();
		byte[] buffer = new byte[256 * 1024];
		int contentLength;
		while ((contentLength = fis.read(buffer)) > 0) {
			out.write(buffer, 0, contentLength);
		}
		out.close();
		fis.close();
		if (deleteOutputFile) {
			if (!outputFile.delete()) {
				outputFile.deleteOnExit();
			}
		}
	}
	
	private void setHeaderForStreaming(final HttpServletResponse resp, final File outputFile) {
		resp.setContentType("application/" + outputFile.getName().substring(outputFile.getName().lastIndexOf(".") + 1) + "; charset=utf-8");
		resp.setHeader("Content-Disposition", "attachment; filename=" + outputFile.getName());
		resp.setContentLength((int) outputFile.length());
		resp.setHeader("Cache-Control", "no-cache");
		resp.setDateHeader("Expires", 0);
		resp.setHeader("Pragma", "No-cache");
	}
	
	
	public static File createReport(final Survey survey, final String reportId, final SubTypeEnum type, final String taskID, final boolean overrideCreate) {
		File outputPdf = null;
		
		GenericReportInfo genRepInfo = new GenericReportInfo();
		AbstractReport report;
		boolean summary = false;
		if (!reportId.equals("report_summary")) {
			genRepInfo = AccessReportFromDB.getInstance().getReportInfoById(Integer.parseInt(reportId), null);
			report = new GenericReport();
			((GenericReport) report).setName(genRepInfo.getName());
			((GenericReport) report).setId(reportId);
			((GenericReport) report).setReportType(genRepInfo.getRepInfoType());
		} else {
			summary = true;
			report = new SummaryReport();
		}
		List<ReportType> subTypes = new ArrayList<ReportType>();
		subTypes.add(new ReportType(SubTypeEnum.QUANTITATIVE, "Quantitative Report"));
		if (!Type.PERFORMANCE.equals(genRepInfo.getRepInfoType())) {
			subTypes.add(new ReportType(SubTypeEnum.QUALITATIVE, "Qualitative Report"));
		}
		report.setSubTypes(subTypes);
		if (report != null) {
			String baseTargetPath = Paths.REPORTS + "/" + survey.getId() + "/" + reportId + "/";
			
			outputPdf = new File(baseTargetPath + SafeFilenameFile.getSafeFileName(report.getName()) + "_" + survey.getId() + "_" + type.name().toLowerCase() + ".pdf");
			
			if ((overrideCreate && outputPdf.exists()) || (outputPdf.exists() && survey.getState().equals(SurveyState.CLOSED) && (outputPdf.lastModified() > survey.getClosedAtDate().getTime()))) {
				String userName = SessionManager.getInstance().getUserName(taskID.substring(taskID.lastIndexOf(".") + 1));
				FeedBackProvider.getInstance().beginTask("Generating Report for: " + survey.getName(), taskID, userName);
				FeedBackProvider.getInstance().finishTask(taskID);
				
				// do not generate the pdf as it is already created and was
				// created after the survey has been closed (so no change of the
				// data took place)
			} else {
				if (summary) {
					report.evaluate(survey, TargetMedia.PRINTER_REPORT, taskID, null, type);
				} else {
					report.evaluate(survey, TargetMedia.PRINTER_REPORT, taskID, ((GenericReport) report).getReportType(), type);
				}
			}
		}
		return outputPdf;
	}
	
	private File createFilledQuestionnaire(final String participationID, final Session hibSess) {
		
		File outputFile = null;
		Participation participation = (Participation) hibSess.load(Participation.class, participationID);
		Survey survey = participation.getParticipant().getSurvey();
		
		String baseTargetPath = Paths.REPORTS + "/" + survey.getId() + "/" ;
		
		try {
			outputFile = new File(baseTargetPath + "questionnaire_" + participation.getId() + ".pdf");
			FileOutputStream fos = new FileOutputStream(outputFile);
			// parse the markup into an xml Document
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			builder.setEntityResolver(new XhtmlEntityResolver());
			StringBuilder questionnaireContent = new StringBuilder();
			
			questionnaireContent.append(PartsUtil.getClientLogo(survey.getOwner().getClient(), 2, false, false, true));
			questionnaireContent.append(PartsUtil.getSurveyLogo(survey.getId(), 1, false, true, true));
			
			Questionnaire questionnaire = participation.getQuestionnaire();
			for (Page page : questionnaire.getPages()) {
				if (page.isVisible()) {
					questionnaireContent.append("<ul class=\"questionarePage\">");
					for (Section section : page.getSections()) {
						if (section.isVisible()) {
							String sectionTitle = section.getSectionTitle();
							String visible = (sectionTitle == null) || sectionTitle.isEmpty() ? " invisibleSectionTitleForEditor" : "";
							questionnaireContent.append("<li class=\"section\"><p class=\"sectionTitle" + visible + "\" id=\"sectionTitle" + section.getId() + "\">" + WikiUtil.parseToHtml(sectionTitle, true) + "</p><ul class=\"questionareSection\" id=\"section" + section.getId() + "\">");
							for (AbstractQuestion question : section.getQuestions()) {
								if (question.isVisible()) {
									questionnaireContent.append("<li class=\"question\" id=\"question" + question.getId() + "\"><div>" + SurveyElementFactory.getInstance().createQuestionViewObject(question).getPrintableRepresentation(true) + "</div></li>");
								}
							}
							questionnaireContent.append("</ul></li>");
						}
					}
					questionnaireContent.append("</ul>");
				}
			}
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("QUESTIONNAIRE", questionnaireContent.toString());
			StringBuilder buf = new StringBuilder(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/questionnaireForPrint.html", replacements));
			// org.w3c.dom.Document doc = builder.parse(new
			// InputSource(new
			// StringReader(SurveyUtil.removeCriticalCharacters(buf.toString()))));
			org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(buf.toString())));
			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocument(doc, null);
			
			renderer.layout();
			renderer.createPDF(fos);
			fos.close();
			
		} catch (Exception de) {
			Logger.err("Error during creation of the questionnaire pdf: " + de.getMessage(), de);
		}
		
		return outputFile;
	}
	
	private File createQuestionnaire(final Survey survey, final HttpServletRequest req, final SystemUser currentUser) {
		
		File outputPdf = null;
		
		StringBuilder questionnaireContent = new StringBuilder();
		
		String surveyLogo = PartsUtil.getSurveyLogo(survey.getId(), 1, false, true, true);
		String clientLogo = PartsUtil.getClientLogo(survey.getOwner().getClient(), 2, false, surveyLogo.isEmpty(), true);
		questionnaireContent.append(clientLogo);
		questionnaireContent.append(surveyLogo);
		
		List<Page> pages = survey.getQuestionnaire().getPages();
		for (Page page : pages) {
			questionnaireContent.append("<div class=\"questionarePage\">");
			for (Section section : page.getSections()) {
				String sectionTitle = section.getSectionTitle();
				String visible = (sectionTitle == null) || sectionTitle.isEmpty() ? " invisibleSectionTitleForEditor" : "";
				questionnaireContent.append("<div class=\"section\"><p class=\"sectionTitle" + visible + "\">" + WikiUtil.parseToHtml(sectionTitle, true) + "</p>");
				for (AbstractQuestion question : section.getQuestions()) {
					questionnaireContent.append("<div class=\"question\">");
					questionnaireContent.append(SurveyElementFactory.getInstance().createQuestionViewObject(question).getPrintableRepresentation(false));
					questionnaireContent.append("</div>");
				}
				questionnaireContent.append("</div>");
			}
			questionnaireContent.append("</div>");
		}
		
		try {
			outputPdf = new File(this.getSurveyFilesBasePath(survey) + "Questionnaire_" + survey.getId() + ".pdf");
			FileOutputStream fos = new FileOutputStream(outputPdf);
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			builder.setEntityResolver(new XhtmlEntityResolver());
			Map<String, String> replacements = new HashMap<String, String>();
			String content = questionnaireContent.toString();
			// let the flying saucer find the images
			content = content.replaceAll("<img([^>]+)src=\"" + EnvironmentConfiguration.getUrlBase(), "<img$1src=\"" + Paths.WEBCONTENT);
			replacements.put("QUESTIONNAIRE", content);
			StringBuilder buf = new StringBuilder(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/questionnaireForPrint.html", replacements));
			try {
				XmlCompressor c = new XmlCompressor();
				String xml = c.compress(buf.toString());
				// FileWriter fw = new FileWriter(new File("a.xhtml"));
				// fw.write(xml);
				// fw.flush();
				// fw.close();
				
				org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(xml)));
				ITextRenderer renderer = new ITextRenderer();
				renderer.setDocument(doc, null);
				renderer.layout();
				renderer.createPDF(fos);
			} catch (SAXParseException spe) {
				// when we cannot parse, remove bad characters
				Logger.err("Had a parse exception. But trying again. You will need to act when another exception follows!", spe);
				org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(SurveyUtil.removeCriticalCharacters(buf.toString()))));
				ITextRenderer renderer = new ITextRenderer();
				renderer.setDocument(doc, null);
				renderer.layout();
				renderer.createPDF(fos);
			} finally {
				fos.close();
			}
			
		} catch (Exception ex) {
			Logger.err("Could not create PDF for survey: " + survey.getName() + "(" + survey.getId() + ")", ex);
			String taskID = "system." + req.getSession().getId();
			FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
			feedBackProvider.setMessage(taskID, "Export of Questionnaire", currentUser.getActualUserName());
			feedBackProvider.addFeedback(taskID, "Could not export the questionnaire for the survey " + survey.getName() + ". Please contact your support.", Status.ERROR, currentUser.getActualUserName());
			feedBackProvider.finishTask(taskID, true);
		}
		return outputPdf;
	}
	
	private File createPhoneList(final Survey survey, final HttpServletRequest req, final SystemUser currentUser) {
		File outputPdf = null;
		if (survey.getState().equals(SurveyState.CREATED) || survey.getState().equals(SurveyState.RUNNING) || survey.getState().equals(SurveyState.CLOSED)) {
			
			outputPdf = new File(this.getSurveyFilesBasePath(survey) + "Phonelist_" + SafeFilenameFile.getSafeFileName(survey.getName()).replace(" ", "") + ".pdf");
			// create temp directory if not exists
			File parentFile = outputPdf.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			
			Document document = new Document(PageSize.A4.rotate());
			try {
				FileOutputStream fos = new FileOutputStream(outputPdf);
				PdfWriter.getInstance(document, fos);
				this.createHeaderAndFooter(document, survey);
				document.open();
				this.createLogo(document, survey);
				this.createParticipantTable(document, survey, req);
				this.createHint(document);
				
			} catch (Exception de) {
				Logger.err("Cannot create phone list", de);
				String taskID = "system." + req.getSession().getId();
				FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
				feedBackProvider.setMessage(taskID, "Phonelist export", currentUser.getActualUserName());
				feedBackProvider.addFeedback(taskID, "Could not export the phone list. Please contact your support.", Status.ERROR, currentUser.getActualUserName());
				feedBackProvider.finishTask(taskID, true);
			}
			document.close();
			
		}
		return outputPdf;
	}
	
	private File createRawDataDocument(final Survey survey, final HttpServletRequest req, final SystemUser currentUser) {
		ParticipationExporter exporter = new ParticipationExporter();
		File outputFile = new File(this.getSurveyFilesBasePath(survey) + "Rawdata_" + SafeFilenameFile.getSafeFileName(survey.getName()).replace(" ", "") + ".xlsx");
		try {
			FileOutputStream stream = new FileOutputStream(outputFile);
			exporter.exportToExcel(survey, stream, currentUser);
			stream.flush();
			stream.close();
		} catch (IOException ex) {
			Logger.err("Cannot export rawdata to " + outputFile.getAbsolutePath(), ex);
			String taskID = "system." + req.getSession().getId();
			FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
			feedBackProvider.setMessage(taskID, "Export of Raw Data", currentUser.getActualUserName());
			feedBackProvider.addFeedback(taskID, "Could not export the raw data for the survey " + survey.getName() + ". Please contact your support.", Status.ERROR, currentUser.getActualUserName());
			feedBackProvider.finishTask(taskID, true);
		}
		return outputFile;
	}
	
	private File createParticipationXML(final Survey survey, final HttpServletRequest req, final SystemUser currentUser) {
		
		ParticipationExporter exporter = new ParticipationExporter();
		File outputFile = new File(this.getSurveyFilesBasePath(survey) + "Records_" + SafeFilenameFile.getSafeFileName(survey.getName()).replace(" ", "") + ".xml");
		try {
			FileOutputStream stream = new FileOutputStream(outputFile);
			
			// fetch the participants from the parameters
			// null means: export ALL participants
			String[] participants = req.getParameterValues(ParticipantDal.PARAM_PARTICIPANTS);
			List<Integer> participantIds = null;
			if (participants != null) {
				participantIds = new ArrayList<Integer>();
				for (String participantId : participants) {
					participantIds.add(Integer.parseInt(participantId));
				}
			}
			exporter.exportToXml(survey, participantIds, stream, currentUser);
			stream.flush();
			stream.close();
		} catch (IOException ex) {
			Logger.err("Cannot export participations to " + outputFile.getAbsolutePath(), ex);
			String taskID = "system." + req.getSession().getId();
			FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
			feedBackProvider.setMessage(taskID, "Export of Questionnaire", currentUser.getActualUserName());
			feedBackProvider.addFeedback(taskID, "Could not export the raw data for the survey " + survey.getName() + ". Please contact your support.", Status.ERROR, currentUser.getActualUserName());
			feedBackProvider.finishTask(taskID, true);
		}
		return outputFile;
	}
	
	private File createTopicDocument(final int refQuestionId, final HttpServletRequest req, final SystemUser currentUser) {
		TopicExporter exporter = new TopicExporter();
		if (req.getParameter("type").equals("topics")) {
			File outputFile = new File(Paths.TEMP + "/Topics_" + refQuestionId + ".xlsx");
			try {
				FileOutputStream stream = new FileOutputStream(outputFile);
				exporter.exportToExcel(refQuestionId, stream, currentUser, req.getParameter(DownloadServlet.PARAM_COUNT));
				stream.flush();
				stream.close();
			} catch (Exception ex) {
				Logger.err("Cannot export topics to " + outputFile.getAbsolutePath(), ex);
				String taskID = "system." + req.getSession().getId();
				FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
				feedBackProvider.setMessage(taskID, "Export of Topics", currentUser.getActualUserName());
				feedBackProvider.addFeedback(taskID, "Could not export the topics for the question " + refQuestionId + ". Please contact your support.", Status.ERROR, currentUser.getActualUserName());
				feedBackProvider.finishTask(taskID, true);
			}
			return outputFile;
		} else {
			File outputFile = new File(Paths.TEMP + "/Topics_" + refQuestionId + ".mm");
			try {
				FileOutputStream stream = new FileOutputStream(outputFile);
				exporter.exportToFreemind(refQuestionId, stream, currentUser, req.getParameter(DownloadServlet.PARAM_COUNT));
				stream.flush();
				stream.close();
			} catch (IOException ex) {
				Logger.err("Cannot export topics to " + outputFile.getAbsolutePath(), ex);
				String taskID = "system." + req.getSession().getId();
				FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
				feedBackProvider.setMessage(taskID, "Export of Topics", currentUser.getActualUserName());
				feedBackProvider.addFeedback(taskID, "Could not export the topics for the question " + refQuestionId + ". Please contact your support.", Status.ERROR, currentUser.getActualUserName());
				feedBackProvider.finishTask(taskID, true);
			}
			return outputFile;
		}
		
	}
	
	
	private void createHint(final Document document) throws DocumentException {
		Paragraph phoneHint = new Paragraph(new Chunk("Click on the phone icon"));
		phoneHint.add(new Chunk(this.getPhoneIcon(), 0.0f, 0.0f, true));
		phoneHint.add(new Chunk("to open the phone interview in your webbrowser."));
		document.add(phoneHint);
	}
	
	private void createLogo(final Document document, final Survey survey) throws DocumentException {
		
		String clientLogoImagePath = Paths.CLIENTLOGOS + "/" + survey.getOwner().getClient().getId() + ".jpg";
		Image logoPicture = null;
		if (new File(clientLogoImagePath).exists()) {
			try {
				logoPicture = Image.getInstance(clientLogoImagePath);
			} catch (Exception e) {
				Logger.err(clientLogoImagePath + " is not the correct path to the logo of the client!", e);
			}
		}
		
		Paragraph logo = new Paragraph(new Chunk(logoPicture, 0.0f, 0.0f, true));
		logo.setAlignment(Element.ALIGN_RIGHT);
		document.add(logo);
		
	}
	
	private void createParticipantTable(final Document document, final ISurvey survey, final HttpServletRequest request) throws DocumentException {
		PdfPTable table = new PdfPTable(new float[] {0.4f, 3f, 1f, 3f, 0.6f, 4f});
		table.setSpacingBefore(10);
		table.setWidthPercentage(100.0f);
		document.add(new Paragraph());
		table.addCell("No.");
		table.addCell("Name");
		table.addCell("Language");
		table.addCell("Phone");
		PdfPCell cell = new PdfPCell(new Phrase("Link"));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);
		table.addCell("Comment");
		table.setHeaderRows(1);
		
		int count = 0;
		for (Participant participant : survey.getParticipants()) {
			if (participant.isAskByPhone()) {
				count++;
				table.addCell(count + "");
				table.addCell(participant.getName() + " " + participant.getSurname());
				table.addCell(participant.getProperties().getProperty("homeLanguage"));
				table.addCell(participant.getContactPhone());
				Paragraph p = new Paragraph();
				p.add(new Chunk(this.getPhoneIcon(), 0.5f, 0.0f));
				p.add(" ");
				Anchor link = new Anchor(p);
				link.setReference(this.getServerBaseUrl(request) + "/participate?phoneInterview=1&pid=" + participant.getParticipation().getId());
				PdfPCell cell2 = new PdfPCell(link);
				cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
				table.addCell(cell2);
				table.addCell("");
			}
		}
		
		document.add(table);
		
	}
	
	private Image getPhoneIcon() {
		String phoneIconPath = Paths.WEBCONTENT + "/gfx/" + Paths.ICONS + "/phone.png";
		try {
			return Image.getInstance(phoneIconPath);
		} catch (Exception e) {
			Logger.err(phoneIconPath + " is not the correct path to the icon!", e);
			return null;
		}
	}
	
	private void createHeaderAndFooter(final Document document, final ISurvey survey) throws DocumentException, IOException {
		BaseFont helvetica = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
		Font defaultFont = new Font(helvetica, 8, Font.NORMAL);
		Font defaultFontBold = new Font(helvetica, 10, Font.NORMAL);
		Phrase phrase = new Phrase("Phonelist for Survey: " + survey.getName(), defaultFontBold);
		Phrase phrase2 = new Phrase("Downloaded on " + TimeUtil.getLocalTime(survey.getCreator(), new Date()) + ". Page ", defaultFont);
		HeaderFooter header = new HeaderFooter(phrase, false);
		HeaderFooter footer = new HeaderFooter(phrase2, true);
		header.setAlignment(Element.ALIGN_CENTER);
		footer.setAlignment(Element.ALIGN_RIGHT);
		header.setBorder(0);
		footer.setBorder(0);
		document.setHeader(header);
		document.setFooter(footer);
		
	}
	
	private String getSurveyFilesBasePath(final ISurvey survey) {
		final String baseTargetPath = "Reports/" + survey.getId() + "/";
		File parentFile = new File(baseTargetPath);
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		return baseTargetPath;
	}
	
	private String getServerBaseUrl(final HttpServletRequest req) {
		return (String) EnvironmentConfiguration.getConfiguration(ConfigID.HOST) + EnvironmentConfiguration.getUrlBase();
	}
}
