package de.cinovo.surveyplatform.reporting.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.InputSource;

import com.lowagie.text.DocumentException;

import de.cinovo.surveyplatform.constants.ChartType;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.Topic;
import de.cinovo.surveyplatform.model.chart.DataSetContainer;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.question.ComboQuestion;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.question.IMatrixQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.PhoneCallerHint;
import de.cinovo.surveyplatform.model.question.RadioQuestion;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.model.reporting.IReport;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.AverageNumberQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.container.FreeTextQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.container.MatrixQuestionChartDataContainer;
import de.cinovo.surveyplatform.reporting.container.MatrixQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.container.MultipleChoiceQuestionChartDataContainer;
import de.cinovo.surveyplatform.reporting.container.MultipleChoiceQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.container.SingleLineQuestionDataContainer;
import de.cinovo.surveyplatform.reporting.container.TextfieldQuestionDataContainer;
import de.cinovo.surveyplatform.ui.views.AbstractQuestionView;
import de.cinovo.surveyplatform.ui.views.IQuestionView;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil.QuestionType;
import de.cinovo.surveyplatform.util.TemplateUtil;
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
public class SummaryReportGenerator extends AbstractReportGenerator {
	
	ArrayList<Integer> questionnaireIdList = new ArrayList<Integer>();
	
	
	// public static SummaryReportGenerator instance = new
	// SummaryReportGenerator(new SummaryReport(), null);
	
	public SummaryReportGenerator(final IReport baseReport, final Survey survey) {
		super(baseReport, survey);
		for (Participant p : survey.getParticipants()) {
			if ((p.getParticipation() != null) && (p.getParticipation().getQuestionnaire() != null)) {
				questionnaireIdList.add(p.getParticipation().getQuestionnaire().getId());
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.reporting.generator.AbstractReportGenerator#getContent(de.cinovo.surveyplatform.model.question.AbstractQuestion
	 * , org.hibernate.Session, de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia, java.lang.String)
	 */
	@Override
	protected StringBuilder getContent(final AbstractQuestion question, final Session hibSess, final TargetMedia targetMedia, final String taskID) {
		StringBuilder content = new StringBuilder();
		evaluate(survey, question, content, hibSess, targetMedia);
		return content;
	}
	
	/**
	 * @param type
	 *            The current questionType
	 * @param hibSess
	 *            the currently used Hibernate Session
	 * @param targetMedia
	 *            the Media so show the analysed data in
	 * @param taskID
	 *            a generated taskID
	 * 
	 *            Creates a Stringbuilder, with the content of the submitted
	 *            questions
	 *            in the report. It also calls an own "evaluate" method, to
	 *            evaluate
	 *            answers.
	 * 
	 * @return StringBuilder
	 *         The analysed content of the summary report.
	 */
	@Override
	protected StringBuilder getContent(final QuestionType type, final Session hibSess, final TargetMedia targetMedia, final String taskID) {
		
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		StringBuilder questionnaireContent = new StringBuilder();
		int questionCount = 0;
		List<Page> pages = survey.getQuestionnaire().getPages();
		int totalWork = pages.size();
		
		int currentWork = 1;
		
		for (Page page : pages) {
			questionnaireContent.append("<div class=\"questionarePage\">");
			for (Section section : page.getSections()) {
				String sectionTitle = section.getSectionTitle();
				String visible = (sectionTitle == null) || sectionTitle.isEmpty() ? " invisibleSectionTitleForEditor" : "";
				
				questionnaireContent.append("<div class=\"section\"><p class=\"sectionTitle" + visible + "\">" + WikiUtil.parseToHtml(sectionTitle, true) + "</p>");
				for (AbstractQuestion question : section.getQuestions()) {
					Map<String, String> replacements = new HashMap<String, String>();
					replacements.put("QUESTIONID", String.valueOf(question.getId()));
					replacements.put("SURVEYID", survey.getId().toString());
					replacements.put("QUESTIONNAME", question.getQuestion());
					replacements.put("INCLUDED", "1");
					
					QuestionType questionType = QuestionnaireViewUtil.getQuestionType(question);
					if (targetMedia.equals(TargetMedia.SCREEN)) {
						if (question.getInteresting()) {
							replacements.put("INCLUDED", "1");
						} else {
							replacements.put("INCLUDED", "0");
						}
						questionnaireContent.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/markAsInteresting.html", replacements));
					}
					
					String displayQuestion = "display: block;";
					if ((question.getInteresting() == false) && TargetMedia.SCREEN.equals(targetMedia)) {
						displayQuestion = "display: none;";
					}
					
					questionnaireContent.append("<div class=\"question " + QuestionnaireViewUtil.getQuestionType(question).toString() + "\" id=\"" + question.getId() + "\" style=\"" + displayQuestion + "\">");
					questionnaireContent.append("<span style=\"display: none;\">" + question.getClass().getSimpleName() + " " + question.getId() + " " + question.getLocalId() + "</span>");
					if (type.equals(QuestionType.ALL) || questionType.equals(type)) {
						Session hibSess2 = HibernateUtil.getSessionFactory().openSession();
						try {
							Transaction tx = hibSess2.beginTransaction();
							evaluate(survey, question, questionnaireContent, hibSess2, targetMedia);
							tx.commit();
						} finally {
							hibSess2.close();
						}
					} else {
						questionnaireContent.append(ReportGeneratorUtils.getQuestionText(question));
						questionnaireContent.append("<div style=\"color: #AEAEAE;\">For viewing the results of this question please refer to the " + (type.equals(QuestionType.QUALITATIVE) ? QuestionType.QUANTITATIVE.toString() : QuestionType.QUALITATIVE.toString()).toLowerCase() + " report.</div>");
					}
					questionCount++;
					
					questionnaireContent.append("</div>");
					
				}
				questionnaireContent.append("</div>");
			}
			questionnaireContent.append("</div>");
			fbp.setProgress(taskID, Math.round(((float) currentWork++ / totalWork) * 100));
		}
		if (questionCount == 0) {
			questionnaireContent = new StringBuilder("");
		}
		
		return questionnaireContent;
	}
	
	/**
	 * Generates Questionnaire PDF for each participation (with results)
	 * 
	 * @param baseTargetPath Targetpath to put the pdfs to
	 */
	@SuppressWarnings("unused")
	// yes, currently not used. but maybe in future ;)
	private void generateQuestionnaires(final String baseTargetPath) {
		for (Participant p : survey.getParticipants()) {
			if (p.getSurveySubmitted() != null) {
				try {
					FileOutputStream fos = new FileOutputStream(new File(baseTargetPath + "questionnaire_" + p.getParticipation().getId() + ".pdf"));
					try {
						// parse the markup into an xml Document
						DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						builder.setEntityResolver(new XhtmlEntityResolver());
						StringBuilder questionnaireContent = new StringBuilder();
						
						questionnaireContent.append(PartsUtil.getClientLogo(survey.getOwner().getClient(), 2, false, false, true));
						questionnaireContent.append(PartsUtil.getSurveyLogo(survey.getId(), 1, false, true, true));
						
						Questionnaire questionnaire = p.getParticipation().getQuestionnaire();
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
						
					} catch (DocumentException de) {
						Logger.err("Error during creation of the questionnaire pdf: " + de.getMessage(), de);
					}
					
				} catch (Exception ex) {
					Logger.err("", ex);
				}
			}
		}
	}
	
	private void evaluate(final ISurvey selectedSurvey, final AbstractQuestion question, final StringBuilder content, final Session hibSess, final TargetMedia targetMedia) {
		
		int numberOfResponses = 0;
		RepresentationMetadata rmd = new RepresentationMetadata();
		rmd.targetMedia = targetMedia;
		rmd.surveyID = selectedSurvey.getId();
		
		String graphPath = selectedSurvey.getId() + "/" + getIdentifier() + "/" + question.getId();
		
		Workbook wb = new SXSSFWorkbook();
		Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(survey.getName()));
		
		// checkboxxed here
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("QUESTIONID", String.valueOf(question.getId()));
		replacements.put("SURVEYID", String.valueOf(survey.getId()));
		
		// del_line
		replacements.put("CHECKED", "checked = \"checked\"");
		
		AbstractQuestionView questionViewObject = SurveyElementFactory.getInstance().createQuestionViewObject(question);
		
		if (question instanceof PhoneCallerHint) {
			// do nothing with the hints (also do not show them in the
			// report!)
		} else if (question instanceof TextPart) {
			content.append(questionViewObject.getAggregatedPrintableRepresentation(null, rmd, question));
		} else if (question instanceof FreeTextQuestion) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			
			List<?> list = getListByCriteriaOfClass(FreeTextQuestion.class, hibSess, question);
			if ((list != null) && (list.size() > 0)) {
				List<Answer> answers = new ArrayList<Answer>();
				for (Object obj : list) {
					if (obj instanceof FreeTextQuestion) {
						// dont add empty answers into answers list
						if ((((FreeTextQuestion) obj).getAnswerObj() != null) && (!((FreeTextQuestion) obj).getAnswerObj().equals(""))) {
							answers.add(((FreeTextQuestion) obj).getAnswerObj());
						}
					}
				}
				FreeTextQuestionDataContainer dc = new FreeTextQuestionDataContainer();
				dc.answers = answers;
				dc.topics = addTopicListByCriteria(hibSess, question);
				content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
				if (targetMedia.equals(TargetMedia.SCREEN)) {
					ReportGeneratorUtils.addDownloadExcelLink(content, survey.getId(), question);
				}
				questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
				
				numberOfResponses = answers.size();
			}
		} else if (question instanceof TextfieldQuestion) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			
			List<?> list = getListByCriteriaOfClass(TextfieldQuestion.class, hibSess, question);
			if ((list != null) && (list.size() > 0)) {
				List<List<Answer>> sortedAnswers = new ArrayList<List<Answer>>();
				// divides the answers into sublists
				for (Object obj : list) {
					if (obj instanceof TextfieldQuestion) {
						List<Answer> answers = ((TextfieldQuestion) obj).getAnswers();
						
						for (int i = 0; i < answers.size(); i++) {
							if (sortedAnswers.size() < answers.size()) {
								sortedAnswers.add(new ArrayList<Answer>());
							}
							List<Answer> innerList = sortedAnswers.get(i);
							innerList.add(answers.get(i));
						}
					}
				}
				TextfieldQuestionDataContainer dc = new TextfieldQuestionDataContainer();
				dc.sortedAnswers = sortedAnswers;
				dc.topics = addTopicListByCriteria(hibSess, question);
				
				content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
				if (targetMedia.equals(TargetMedia.SCREEN)) {
					ReportGeneratorUtils.addDownloadExcelLink(content, survey.getId(), question);
				}
				questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
				
				numberOfResponses = list.size();
			}
		} else if (question instanceof SingleLineQuestion) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			
			List<?> list = getListByCriteriaOfClass(SingleLineQuestion.class, hibSess, question);
			if ((list != null) && (list.size() > 0)) {
				List<Answer> answers = new ArrayList<Answer>();
				for (Object obj : list) {
					if (obj instanceof SingleLineQuestion) {
						// dont add empty answers into answers list
						if ((((SingleLineQuestion) obj).getAnswer().get(0) != null) && (!((SingleLineQuestion) obj).getAnswer().get(0).getAnswer().equals(""))) {
							answers.add(((SingleLineQuestion) obj).getAnswer().get(0));
						}
					}
				}
				SingleLineQuestionDataContainer dc = new SingleLineQuestionDataContainer();
				dc.answers = answers;
				dc.topics = addTopicListByCriteria(hibSess, question);
				
				content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
				if (targetMedia.equals(TargetMedia.SCREEN)) {
					ReportGeneratorUtils.addDownloadExcelLink(content, survey.getId(), question);
				}
				questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
				
				numberOfResponses = answers.size();
			}
		} else if (question instanceof AverageNumberQuestion) {
			// put average
			content.append(ReportGeneratorUtils.getQuestionText(question));
			
			List<?> list = getListByCriteriaOfClass(AverageNumberQuestion.class, hibSess, question);
			if ((list != null) && (list.size() > 0)) {
				List<String> answers = new ArrayList<String>();
				for (Object obj : list) {
					if (obj instanceof AverageNumberQuestion) {
						Answer answerObj = ((AverageNumberQuestion) obj).getAnswerObj();
						if (answerObj != null) {
							answers.add(answerObj.getAnswer());
						}
					}
				}
				AverageNumberQuestionDataContainer dc = new AverageNumberQuestionDataContainer();
				dc.answers = answers;
				dc.topics = addTopicListByCriteria(hibSess, question);
				
				content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
				if (targetMedia.equals(TargetMedia.SCREEN)) {
					ReportGeneratorUtils.addDownloadExcelLink(content, survey.getId(), question);
				}
				questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
				
				numberOfResponses = dc.numberOfResponses;
			}
		} else {
			ChartType chartType = QuestionnaireViewUtil.getChartType(question);
			if ((question instanceof MultipleChoiceQuestion) || (question instanceof RadioQuestion) || (question instanceof ComboQuestion)) {
				
				content.append(ReportGeneratorUtils.getQuestionText(question));
				
				Map<String, Double> valueMap = new LinkedHashMap<String, Double>();
				
				int optionCount = 0;
				for (Option option : question.getOptions()) {
					valueMap.put(option.getDisplayName(), 0.0);
					optionCount++;
				}
				
				// retrieve all selected options of the question
				
				List<?> list = getListByCriteriaOfClass(Option.class, hibSess, question);
				Option option = null;
				int count = 0;
				if ((list != null) && (list.size() > 0)) {
					for (Object obj : list) {
						if (obj instanceof Option) {
							option = (Option) obj;
							if (option.isSelected()) {
								valueMap.put(option.getDisplayName(), valueMap.get(option.getDisplayName()) + 1);
							}
							count++;
						}
					}
					numberOfResponses = count / optionCount;
					
					
					MultipleChoiceQuestionChartDataContainer cdc = new MultipleChoiceQuestionChartDataContainer();
					cdc.numberOfResponses = Arrays.asList(new Integer[] {numberOfResponses});
					List<DataSetContainer> dscList = new ArrayList<DataSetContainer>();
					dscList.add(new DataSetContainer(IQuestionView.DEFAULT_DATASETNAME, valueMap));
					cdc.dataSets = dscList;
					cdc.filePath = graphPath;
					
					if ((chartType == null) || chartType.equals(ChartType.bar)) {
						// draw Bar-chart
						if ((optionCount <= ReportGeneratorUtils.MAXOPTIONS_WITH_GRAPHICAL_REPRESENTATION)) {
							content.append(questionViewObject.getAggregatedPrintableRepresentation(cdc, rmd, question));
						} else {
							content.append("<p style=\"text-align:center; margin: 40px 0px 40px 0px;\">Cannot show the chart as the data is either empty or the question has too many or too long options.</p>");
						}
					} else if (chartType.equals(ChartType.pie)) {
						// draw Pie-chart
						content.append(questionViewObject.getAggregatedPrintableRepresentation(cdc, rmd, question));
					}
					
					// draw table
					if (targetMedia.equals(TargetMedia.SCREEN)) {
						showChartButtons(selectedSurvey, content, graphPath, replacements, chartType, optionCount);
						if ((optionCount <= ReportGeneratorUtils.MAXOPTIONS_WITH_GRAPHICAL_REPRESENTATION) || chartType.equals(ChartType.pie)) {
							ReportGeneratorUtils.addSimpleInfoButton(content);
							ReportGeneratorUtils.addSlideToggleButton(content, rmd.uuid);
							rmd.useSlideToggleFragment = true;
						}
					}
					{
						MultipleChoiceQuestionDataContainer dc = new MultipleChoiceQuestionDataContainer();
						dc.numberOfResponses = numberOfResponses;
						dc.dataSet = new DataSetContainer(IQuestionView.DEFAULT_DATASETNAME, valueMap);
						content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
						questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
					}
				}
			} else if (question instanceof IMatrixQuestion) {
				content.append(ReportGeneratorUtils.getQuestionText(question));
				
				List<AbstractQuestion> subquestions = question.getSubquestions();
				int subQuestionCount = subquestions.size();
				if (subQuestionCount > 0) {
					List<Option> optionList = subquestions.get(0).getOptions();
					int optionCount = optionList.size();
					
					// get an 2D array of n * m
					int[][] valueMap = new int[subQuestionCount][optionCount];
					
					Map<Integer, Integer> relationSubquestionToY = new HashMap<Integer, Integer>();
					Map<String, Integer> relationOptionToX = new HashMap<String, Integer>();
					
					List<Integer> subQuestionIdList = new ArrayList<Integer>();
					int yCount = 0;
					int xCount = 0;
					for (AbstractQuestion subQuestion : subquestions) {
						int id = subQuestion.getId();
						subQuestionIdList.add(id);
						relationSubquestionToY.put(id, yCount);
						yCount++;
						xCount = 0;
						for (Option option : subQuestion.getOptions()) {
							relationOptionToX.put(option.getDisplayName(), xCount);
							xCount++;
						}
					}
					
					Query query = hibSess.createQuery(Queries.OPTIONS_BY_QUESTIONID);
					
					query.setParameterList("list", subQuestionIdList);
					query.setParameterList("questionnaires", questionnaireIdList);
					List<?> list = query.list();
					int count = 0;
					if ((list != null) && (list.size() > 0)) {
						for (Object obj : list) {
							if (obj instanceof Option) {
								Option option = (Option) obj;
								if (option.isSelected()) {
									try {
										valueMap[relationSubquestionToY.get(option.getOriginQuestionId())][relationOptionToX.get(option.getDisplayName())] += 1;
									} catch (Exception ex) {
										Logger.err("Database corrupt!", ex);
									}
								}
							}
							count++;
						}
					}
					
					numberOfResponses = (count / (xCount * yCount));
					// show charts
					List<DataSetContainer> dataSets = new ArrayList<DataSetContainer>();
					count = 0;
					for (AbstractQuestion subQuestion : subquestions) {
						Map<String, Double> rowValueMap = new LinkedHashMap<String, Double>();
						for (int j = 0; j < xCount; j++) {
							rowValueMap.put(optionList.get(j).getDisplayName(), (double) valueMap[count][j]);
						}
						dataSets.add(new DataSetContainer(subQuestion.getQuestion(), rowValueMap));
						count++;
					}
					
					MatrixQuestionChartDataContainer cdc = new MatrixQuestionChartDataContainer();
					cdc.numberOfResponses = numberOfResponses;
					cdc.dataSets = dataSets;
					cdc.filePath = selectedSurvey.getId() + "/" + getIdentifier() + "/" + question.getId();
					if ((yCount <= ReportGeneratorUtils.MAXROWS_WITH_GRAPHICAL_REPRESENTATION) && (optionCount <= ReportGeneratorUtils.MAXOPTIONS_WITH_GRAPHICAL_REPRESENTATION) && chartType.equals(ChartType.bar)) {
						content.append(questionViewObject.getAggregatedPrintableRepresentation(cdc, rmd, question));
					} else if (chartType.equals(ChartType.pie)) {
						content.append(questionViewObject.getAggregatedPrintableRepresentation(cdc, rmd, question));
					}
					
					MatrixQuestionDataContainer dc = new MatrixQuestionDataContainer();
					dc.numberOfResponses = numberOfResponses;
					dc.valueMap = valueMap;
					if (targetMedia.equals(TargetMedia.SCREEN)) {
						if (((yCount <= ReportGeneratorUtils.MAXROWS_WITH_GRAPHICAL_REPRESENTATION) && (optionCount <= ReportGeneratorUtils.MAXOPTIONS_WITH_GRAPHICAL_REPRESENTATION))) {
							showChartButtons(selectedSurvey, content, cdc.filePath, replacements, chartType, optionCount);
							ReportGeneratorUtils.addSlideToggleButton(content, rmd.uuid);
							rmd.useSlideToggleFragment = true;
						}
					}
					
					content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
					questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
				}
			}
			
		}
		
		if (question.isVisible() && !((question instanceof TextPart) || (question instanceof PhoneCallerHint))) {
			content.append(ReportGeneratorUtils.printNumberOfResponses(numberOfResponses));
		}
		
		saveExcelWorkbook(wb, question);
	}
	
	private void showChartButtons(final ISurvey selectedSurvey, final StringBuilder content, final String graphPath, final Map<String, String> replacements, final ChartType chartType, final int optionCount) {
		content.append("<div style=\"height: 32px;width:435px; margin: 0 auto; text-align:center;\">");
		if (ChartType.pie.equals(chartType)) {
			replacements.put("CHECKBAR", "");
			replacements.put("CHECKPIE", "checked");
		} else {
			replacements.put("CHECKBAR", "checked");
			replacements.put("CHECKPIE", "");
		}
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/chartTypeSelection.html", replacements));
		
		if ((optionCount <= ReportGeneratorUtils.MAXOPTIONS_WITH_GRAPHICAL_REPRESENTATION) || chartType.equals(ChartType.pie)) {
			ReportGeneratorUtils.addViewGraphButton(content, graphPath);
			ReportGeneratorUtils.addGraphDownloadButton(content, graphPath, selectedSurvey.getId());
			// ReportGeneratorUtils.addTableDownloadButton(content,
			// rmd.uuid,
			// ReportGeneratorUtils.getQuestionText(question));
		}
		// ReportGeneratorUtils.addSimpleInfoButton(content);
		content.append("</div>");
	}
	
	/**
	 * @param hibSess
	 *            the current hibSess declared in evaluate
	 * @param question
	 *            the current question to evaluate
	 * @return List of Topics
	 */
	@SuppressWarnings("unchecked")
	private List<Topic> addTopicListByCriteria(final Session hibSess, final AbstractQuestion question) {
		Criteria topicCriteria = hibSess.createCriteria(Topic.class);
		topicCriteria.add(Restrictions.eq("refQuestionId", question.getId()));
		return topicCriteria.list();
	}
	
	/**
	 * @param criteriaClass
	 *            The Class, which is used to create a criteria
	 * @param hibSess
	 *            the current hibSess declared in evaluate
	 * @param question
	 *            the current question to evaluate
	 * @return List (obj)
	 *         List of Answers answered in question
	 */
	private List<?> getListByCriteriaOfClass(final Class<?> criteriaClass, final Session hibSess, final AbstractQuestion question) {
		Criteria criteria = hibSess.createCriteria(criteriaClass);
		criteria.add(Restrictions.eq("submitted", true));
		criteria.add(Restrictions.eq("originQuestionId", question.getId()));
		List<?> list = criteria.list();
		return list;
	}
	
	
}
