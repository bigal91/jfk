/**
 *
 */
package de.cinovo.surveyplatform.reporting.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.InputSource;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Participation;
import de.cinovo.surveyplatform.model.SafeFilenameFile;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.reporting.IReport;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator;
import de.cinovo.surveyplatform.model.reporting.ReportType;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.ui.pages.AnalyseReportsContainer;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil.QuestionType;
import de.cinovo.surveyplatform.util.SessionManager;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.XhtmlEntityResolver;


/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public abstract class AbstractReportGenerator implements IReportGenerator {
	
	protected Survey survey;
	
	protected IReport baseReport;
	
	
	/**
	 * @param baseReport -
	 * @param survey -
	 */
	public AbstractReportGenerator(final IReport baseReport, final Survey survey) {
		this.survey = survey;
		this.baseReport = baseReport;
	}
	
	protected abstract StringBuilder getContent(final QuestionType type, final Session hibSess, final TargetMedia targetMedia, final String taskID);
	
	protected abstract StringBuilder getContent(final AbstractQuestion question, final Session hibSess, final TargetMedia targetMedia, final String taskID);
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.model.reporting.IReportGenerator#evaluate(de.cinovo.surveyplatform.model.question.AbstractQuestion,
	 * de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia, java.lang.String)
	 */
	@Override
	public StringBuilder evaluate(final AbstractQuestion question, final TargetMedia targetMedia, final SubTypeEnum type, final String taskID) {
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		String userName = SessionManager.getInstance().getUserName(taskID.substring(taskID.lastIndexOf(".") + 1));
		fbp.beginTask("Generating partial Report (" + this.getClass().getSimpleName() + ") for question " + question.getId() + " in the survey " + this.survey.getName() + "(ID: " + this.survey.getId() + ")", taskID, userName);
		fbp.setMessage(taskID, "Generating the report for the target media: " + targetMedia);
		StringBuilder content = new StringBuilder();
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			content.append(getContent(question, hibSess, targetMedia, taskID));
			tx.commit();
		} finally {
			hibSess.close();
		}
		fbp.finishTask(taskID);
		
		return content;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @seescc.model.reporting.IReportGenerator#evaluate(de.cinovo.surveyplatform
	 * .model.reporting.
	 * IReportGenerator.TargetMedia)
	 */
	@Override
	public StringBuilder evaluate(final TargetMedia targetMedia, final SubTypeEnum type, final String taskID) {
		
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		String userName = SessionManager.getInstance().getUserName(taskID.substring(taskID.lastIndexOf(".") + 1));
		fbp.beginTask("Generating Report (" + this.getClass().getSimpleName() + ") for: " + this.survey.getName() + "(ID: " + this.survey.getId() + ")", taskID, userName);
		fbp.setMessage(taskID, "Generating the report for the target media: " + targetMedia);
		StringBuilder content = new StringBuilder();
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			
			String baseLink = "?page=" + Pages.PAGE_REPORTS + "&amp;" + SurveyDal.PARAM_SURVEYID + "=" + this.survey.getId() + "&amp;" + AnalyseReportsContainer.PARAM_OPEN + "=" + this.baseReport.getId() + "&amp;" + AnalyseReportsContainer.PARAM_PDF + "=1";
			
			if (TargetMedia.SCREEN.equals(targetMedia)) {
				// offer links to download the PDFs at the top of the online report
				content.append("<div style=\"float: right; width: 160px; height: 32px;\">");
				content.append(PartsUtil.getIconLink(" topOfReportGenerator", "Click to export the collected raw data to an MS-Excel&reg; document", "Export raw data", EnvironmentConfiguration.getUrlBase() + "/download?type=rawdata&" + SurveyDal.PARAM_SURVEYID + "=" + this.survey.getId(), false, null, false));
				if (this.baseReport.getSubTypes() == null) {
					content.append(PartsUtil.getIconLink("PDF_BIG buttonCell", "Download Report as PDF", "", baseLink, false, null, false));
				} else {
					String availableTypes = "__pdfSubTypes = new Array(" + (this.baseReport.getSubTypes().size() > 1 ? "'COMBINED'," : "");
					for (ReportType subType : this.baseReport.getSubTypes()) {
						availableTypes += "'" + subType.getId() + "',";
					}
					availableTypes += "'');";
					content.append("<a class=\"gui-icon-button-PDF_BIG buttonCell\" title=\"Download Report as PDF\" href=\"javascript:void(0);\" onclick=\"" + availableTypes + "__pdfLinkPrefix = '" + baseLink + "'; $('#chooseReportType').dialog('open');\"></a>");
				}
				content.append("</div>");
				content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgChooseReportType.html", null));
			}
			content.append("<div class=\"innerContainer\">");
			content.append("</div>");
			content.append("<h1>" + this.baseReport.getName() + " of " + this.survey.getName() + "</h1>");
			content.append("<p>" + this.survey.getDescription() + "</p>");
			
			StringBuilder questionnaireContent = new StringBuilder();
			
			if (targetMedia.equals(TargetMedia.SCREEN)) {
				questionnaireContent.append("<div class=\"clear\" style=\"margin-top: 10px;\"></div>");
			}
			
			// check if there are any submitted questionnaires
			boolean anySubmission = checkSubmissions();
			
			if (anySubmission) {
				String surveyLogo = PartsUtil.getSurveyLogo(this.survey.getId(), 1, targetMedia.equals(TargetMedia.SCREEN), true, true);
				String clientLogo = PartsUtil.getClientLogo(this.survey.getOwner().getClient(), 2, targetMedia.equals(TargetMedia.SCREEN), surveyLogo.isEmpty(), true);
				questionnaireContent.append(clientLogo);
				questionnaireContent.append(surveyLogo);
			} else {
				questionnaireContent.append("<p style=\"text-align: center;\">No participant has submitted the questionnaire so far, so there is no data available at this time.</p>");
			}
			
			
			// generate report
			if (targetMedia.equals(TargetMedia.PRINTER_REPORT)) {
				
				if (anySubmission) {
					if (SubTypeEnum.COMBINED.equals(type)) {
						questionnaireContent.append(getContent(QuestionType.ALL, hibSess, targetMedia, taskID));
					} else if (SubTypeEnum.QUALITATIVE.equals(type)) {
						questionnaireContent.append(getContent(QuestionType.QUALITATIVE, hibSess, targetMedia, taskID));
					} else if (SubTypeEnum.QUANTITATIVE.equals(type)) {
						questionnaireContent.append(getContent(QuestionType.QUANTITATIVE, hibSess, targetMedia, taskID));
					}
				}
				
				generatePDF(questionnaireContent, type);
			} else {
				
				if (anySubmission) {
					StringBuilder allContent = new StringBuilder();
					allContent.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/reportCommons.html", null));
					allContent.append(getContent(QuestionType.ALL, hibSess, targetMedia, taskID));
					questionnaireContent.append(allContent);
				}
				content.append(questionnaireContent);
			}
			
			tx.commit();
		} finally {
			hibSess.close();
		}
		fbp.finishTask(taskID);
		
		return content;
	}
	
	private boolean checkSubmissions() {
		boolean anySubmission = false;
		List<Participant> participants = this.survey.getParticipants();
		if (participants != null) {
			for (Participant participant : participants) {
				Participation participation = participant.getParticipation();
				if (participation != null) {
					if (participation.isSubmitted()) {
						anySubmission = true;
						break;
					}
				}
			}
		}
		return anySubmission;
	}
	
	protected String getBaseTargetPath() {
		final String baseTargetPath = Paths.REPORTS + "/" + this.survey.getId() + "/" + getIdentifier() + "/";
		File parentFile = new File(baseTargetPath);
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		return baseTargetPath;
	}
	
	protected void generatePDF(final StringBuilder content, final SubTypeEnum subType) {
		final String baseTargetPath = getBaseTargetPath();
		String suffix = "_" + subType.name().toLowerCase();
		generatePDFReport(content, baseTargetPath, suffix);
	}
	
	protected void saveExcelWorkbook(final Workbook wb, final AbstractQuestion question) {
		Sheet sheet = wb.getSheetAt(0);
		sheet.setDefaultColumnWidth(20);
		try {
			String targetFile = "";
			if (question == null) {
				targetFile = Paths.WEBCONTENT + "/raw/" + this.survey.getId() + "/complete.xlsx";
			} else {
				targetFile = Paths.WEBCONTENT + "/raw/" + this.survey.getId() + "/" + question.getId() + ".xlsx";
			}
			File file = new File(targetFile);
			file.getParentFile().mkdirs();
			FileOutputStream fos = new FileOutputStream(file);
			wb.write(fos);
			fos.flush();
			fos.close();
		} catch (Exception ex) {
			Logger.errUnexpected(ex, null);
		}
	}
	
	/**
	 * Generates a PDF from a given html string which is put into a html frame
	 * with according css
	 * 
	 * @param questionnaireContent Content of the questionnaire
	 * @param baseTargetPath Targetpath to put the report to
	 */
	protected void generatePDFReport(final StringBuilder questionnaireContent, final String baseTargetPath, final String fileSuffix) {
		try {
			FileOutputStream fos = new FileOutputStream(new File(baseTargetPath + SafeFilenameFile.getSafeFileName(this.baseReport.getName()) + "_" + this.survey.getId() + fileSuffix + ".pdf"));
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			builder.setEntityResolver(new XhtmlEntityResolver());
			Map<String, String> replacements = new HashMap<String, String>();
			String content = questionnaireContent.toString();
			// let the flying saucer find the images
			content = content.replaceAll("<img([^>]+)src=\"" + EnvironmentConfiguration.getUrlBase(), "<img$1src=\"" + Paths.WEBCONTENT);
			replacements.put("QUESTIONNAIRE", content);
			
			StringBuilder buf = new StringBuilder(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/questionnaireForPrint.html", replacements));
			
			// String cleanedXHTML =
			// SurveyUtil.removeCriticalCharacters(buf.toString());
			String cleanedXHTML = buf.toString();
			Pattern p = Pattern.compile("<!--.*?-->", Pattern.MULTILINE | Pattern.DOTALL);
			cleanedXHTML = p.matcher(cleanedXHTML).replaceAll("");
			// FileWriter fw = new FileWriter(fileSuffix + ".xhtml");
			// fw.write(cleanedXHTML);
			// fw.flush();
			// fw.close();
			org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(cleanedXHTML)));
			ITextRenderer renderer = new ITextRenderer();
			
			final File fontsDir = new File(Paths.FONTS);
			if (fontsDir.exists() && fontsDir.isDirectory()) {
				for (File fontFile : fontsDir.listFiles()) {
					if (fontFile.isFile()) {
						renderer.getFontResolver().addFont(fontFile.getAbsolutePath(), true);
					}
				}
			}
			renderer.setDocument(doc, null);
			renderer.layout();
			renderer.createPDF(fos);
			renderer.finishPDF();
			fos.close();
			
		} catch (Exception ex) {
			Logger.err("Could not generate PDF", ex);
		}
	}
	
	@Override
	public String getIdentifier() {
		return this.baseReport.getId();
	}
	
	/**
	 * Reads a text file
	 * 
	 * @param fileToRead
	 * @return
	 * @throws IOException
	 */
	protected static String readTextFile(final File fileToRead) throws IOException {
		FileInputStream fis = new FileInputStream(fileToRead);
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader br = new BufferedReader(isr);
		
		StringBuilder sb = new StringBuilder();
		String line;
		while (null != (line = br.readLine())) {
			sb.append(line);
			sb.append("\n");
		}
		br.close();
		return sb.toString();
	}
	
}