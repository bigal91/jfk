/**
 *
 */
package de.cinovo.surveyplatform.reporting.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
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
import de.cinovo.surveyplatform.model.question.IMultipleOptionsQuestion;
import de.cinovo.surveyplatform.model.question.MatrixQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.PhoneCallerHint;
import de.cinovo.surveyplatform.model.question.RadioQuestion;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.model.reporting.AccessReportFromDB;
import de.cinovo.surveyplatform.model.reporting.GenericReportInfo;
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
import de.cinovo.surveyplatform.ui.views.TextPartView;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil.QuestionType;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.WikiUtil;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class DeviationReportGenerator extends AbstractReportGenerator {
	
	private Map<String, List<AbstractQuestion>> deviationMap;
	
	
	/**
	 * @param baseReport
	 * @param survey
	 */
	public DeviationReportGenerator(final IReport baseReport, final Survey survey) {
		super(baseReport, survey);
		
	}
	
	@SuppressWarnings("unchecked")
	private void createDeviationMap(final String taskID, final Session hibSess) {
		
		// find the decision question
		AbstractQuestion decisionQuestion = null;
		
		// trenne hier folgendermaßen auf:
		// alle fragebögen, die bei der relevantQuestion verschiedene
		// optionen gewählt haben:
		// Select * from option where option->fragebogen<-question<-option = 1
		
		deviationMap = new LinkedHashMap<String, List<AbstractQuestion>>();
		List<Participant> participants = survey.getParticipants();
		Participant firstParticipant = null;
		
		// find the first submitted participant (we know that he has a valid
		// questionnaire)
		for (Participant participant : participants) {
			if (participant.getParticipation().isSubmitted()) {
				firstParticipant = participant;
				break;
			}
			
		}
		
		if (firstParticipant != null) {
			int questionnaireId = firstParticipant.getParticipation().getQuestionnaire().getId();
			
			FeedBackProvider fbp = FeedBackProvider.getInstance();
			
			
			
			// fetch the decisionquestion. i.e. the question with the
			// given localid and correct questionnaire id
			GenericReportInfo genRepObject = AccessReportFromDB.getInstance().getReportInfoById(Integer.parseInt(baseReport.getId()), hibSess);
			{
				Query query = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_LOCALID);
				query.setParameter("1", genRepObject.getQuestions().get(0));
				
				List<?> list = query.list();
				for (Object o : list) {
					if (o instanceof AbstractQuestion) {
						AbstractQuestion q = (AbstractQuestion) o;
						if (q.getQuestionnaireID() == questionnaireId) {
							decisionQuestion = (AbstractQuestion) o;
							break;
						}
					}
					
				}
			}
			if (decisionQuestion == null) {
				Logger.err("Decisionquestion is null. This is a bad consistency error!");
				return;
			}
			int totalWork = decisionQuestion.getOptions().size() * survey.getParticipants().size();
			int currentWork = 0;
			for (Option decisionOption : decisionQuestion.getOptions()) {
				for (Participant p : survey.getParticipants()) {
					currentWork++;
					fbp.setProgress(taskID, (int) Math.floor(((float) currentWork / totalWork) * 70));
					if (p.getSurveySubmitted() != null) {
						int questionnaireID = p.getParticipation().getQuestionnaire().getId();
						
						Query query = hibSess.createQuery("Select q from AbstractQuestion q WHERE q.questionnaireID = " + questionnaireID);
						List<?> list = query.list();
						boolean found = false;
						for (Object o : list) {
							if (o instanceof AbstractQuestion) {
								if (((AbstractQuestion) o).getLocalId() == decisionQuestion.getLocalId()) {
									if (o instanceof IMultipleOptionsQuestion) {
										List<Option> options = ((AbstractQuestion) o).getAllOptions();
										for (Option opt : options) {
											if (opt.getDisplayName().equals(decisionOption.getDisplayName()) && opt.isSelected() && opt.isSubmitted()) {
												
												List<AbstractQuestion> qList = deviationMap.get(opt.getDisplayName());
												if (qList == null) {
													qList = new ArrayList<AbstractQuestion>();
													deviationMap.put(decisionOption.getDisplayName(), qList);
												}
												qList.addAll((List<AbstractQuestion>) list);
												found = true;
												break;
											}
										}
									}
								}
							}
							if (found) {
								break;
							}
						}
					}
				}
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
		createDeviationMap(taskID, hibSess);
		evaluate(survey, question, deviationMap, content, hibSess, targetMedia);
		return content;
	}
	
	@Override
	protected StringBuilder getContent(final QuestionType type, final Session hibSess, final TargetMedia targetMedia, final String taskID) {
		// use surveyID to get the right GenReportInfos
		StringBuilder questionnaireContent = new StringBuilder();
		createDeviationMap(taskID, hibSess);
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		List<Page> pages = survey.getQuestionnaire().getPages();
		int totalWork = pages.size();
		int currentWork = 0;
		// int questionCount = 0;
		for (Page page : pages) {
			currentWork++;
			questionnaireContent.append("<div class=\"questionarePage\">");
			for (Section section : page.getSections()) {
				String sectionTitle = section.getSectionTitle();
				String visible = (sectionTitle == null) || sectionTitle.isEmpty() ? " invisibleSectionTitleForEditor" : "";
				questionnaireContent.append("<div class=\"section\"><p class=\"sectionTitle" + visible + "\">" + WikiUtil.parseToHtml(section.getSectionTitle(), true) + "</p>");
				for (AbstractQuestion question : section.getQuestions()) {
					Map<String, String> replacements = new HashMap<String, String>();
					replacements.put("QUESTIONID", String.valueOf(question.getId()));
					replacements.put("SURVEYID", survey.getId().toString());
					replacements.put("QUESTIONNAME", question.getQuestion());
					
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
					
					questionnaireContent.append("<div class=\"question " + QuestionnaireViewUtil.getQuestionType(question).toString() + "\" style=\"" + displayQuestion + "\" id = \"" + question.getId() + "\">");
					if (type.equals(QuestionType.ALL) || questionType.equals(type)) {
						// questionCount++;
						
						// Open new Session (OutOfMemory Problems)
						Session hibSess2 = HibernateUtil.getSessionFactory().openSession();
						try {
							Transaction tx = hibSess2.beginTransaction();
							evaluate(survey, question, deviationMap, questionnaireContent, hibSess2, targetMedia);
							tx.commit();
						} finally {
							hibSess2.close();
						}
						
					} else {
						questionnaireContent.append(ReportGeneratorUtils.getQuestionText(question));
						questionnaireContent.append("<div style=\"color: #AEAEAE;\">For viewing the results of this question please refer to the " + (type.equals(QuestionType.QUALITATIVE) ? QuestionType.QUANTITATIVE.toString() : QuestionType.QUALITATIVE.toString()).toLowerCase() + " report.</div>");
					}
					// } else {
					// questionnaireContent.append("<div class=\"question " +
					// QuestionnaireViewUtil.getQuestionType(question).toString() + "\">");
					// questionnaireContent.append(question.getQuestion() + " - <i>This question is disabled in this report.</i>");
					
					questionnaireContent.append("</div>");
				}
				questionnaireContent.append("</div>");
			}
			questionnaireContent.append("</div>");
			fbp.setProgress(taskID, (int) Math.floor(((float) currentWork / totalWork) * 30) + 70);
		}
		
		return questionnaireContent;
	}
	
	private void evaluate(final ISurvey selectedSurvey, final AbstractQuestion question, final Map<String, List<AbstractQuestion>> deviationMap, final StringBuilder content, final Session hibSess, final TargetMedia targetMedia) {
		RepresentationMetadata rmd = new RepresentationMetadata();
		rmd.surveyID = selectedSurvey.getId();
		rmd.targetMedia = targetMedia;
		Workbook wb = new SXSSFWorkbook();
		Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(survey.getName()));
		
		AbstractQuestionView questionViewObject = SurveyElementFactory.getInstance().createQuestionViewObject(question);
		if (question instanceof PhoneCallerHint) {
			// do nothing with the hints (also do not show them in the report!)
		} else if (question instanceof TextPart) {
			content.append(new TextPartView((TextPart) question).getAggregatedPrintableRepresentation(null, rmd, question));
		} else if (question instanceof FreeTextQuestion) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			for (Entry<String, List<AbstractQuestion>> entry : deviationMap.entrySet()) {
				List<Answer> answers = new ArrayList<Answer>();
				for (AbstractQuestion q : entry.getValue()) {
					if (q.getOriginQuestionId() == question.getId()) {
						answers.add(((FreeTextQuestion) q).getAnswerObj());
					}
				}
				FreeTextQuestionDataContainer dc = new FreeTextQuestionDataContainer();
				dc.answers = answers;
				dc.topics = addTopicListByCriteria(hibSess, question);
				
				content.append(ReportGeneratorUtils.printAggregationpartContainerTitle(entry.getKey()));
				content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
				if (targetMedia.equals(TargetMedia.SCREEN)) {
					ReportGeneratorUtils.addDownloadExcelLink(content, survey.getId(), question);
				}

				questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
			}
			if (deviationMap.size() == 0) {
				content.append(ReportGeneratorUtils.printNumberOfResponses(0));
			}
			
		} else if (question instanceof TextfieldQuestion) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			
			for (Entry<String, List<AbstractQuestion>> entry : deviationMap.entrySet()) {
				List<List<Answer>> sortedAnswers = new ArrayList<List<Answer>>();
				int numberOfResponse = 0;
				for (AbstractQuestion q : entry.getValue()) {
					if (q.getOriginQuestionId() == question.getId()) {
						List<Answer> answers = ((TextfieldQuestion) q).getAnswer();
						if (answers.size() > 0) {
							numberOfResponse++;
						}
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
				
				content.append(ReportGeneratorUtils.printAggregationpartContainerTitle(entry.getKey()));
				content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
				if (targetMedia.equals(TargetMedia.SCREEN)) {
					ReportGeneratorUtils.addDownloadExcelLink(content, survey.getId(), question);
				}
				questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
				
				content.append(ReportGeneratorUtils.printNumberOfResponses(numberOfResponse, entry.getKey()));
				
			}
			if (deviationMap.size() == 0) {
				content.append(ReportGeneratorUtils.printNumberOfResponses(0));
			}
			
		} else if (question instanceof SingleLineQuestion) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			
			for (Entry<String, List<AbstractQuestion>> entry : deviationMap.entrySet()) {
				List<Answer> answers = new ArrayList<Answer>();
				for (AbstractQuestion q : entry.getValue()) {
					if (q.getOriginQuestionId() == question.getId()) {
						Answer answer = ((SingleLineQuestion) q).getAnswer().get(0);
						if (!answer.getAnswer().isEmpty()) {
							answers.add(answer);
						}
					}
				}
				SingleLineQuestionDataContainer dc = new SingleLineQuestionDataContainer();
				dc.answers = answers;
				dc.topics = addTopicListByCriteria(hibSess, question);
				
				content.append(ReportGeneratorUtils.printAggregationpartContainerTitle(entry.getKey()));
				content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
				questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
				content.append(ReportGeneratorUtils.printNumberOfResponses(answers.size(), entry.getKey()));
			}
			
			if (deviationMap.size() == 0) {
				content.append(ReportGeneratorUtils.printNumberOfResponses(0));
			}
			
		} else if (question instanceof AverageNumberQuestion) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			// put average
			
			for (Entry<String, List<AbstractQuestion>> entry : deviationMap.entrySet()) {
				List<String> answers = new ArrayList<String>();
				for (AbstractQuestion q : entry.getValue()) {
					if (q.getOriginQuestionId() == question.getId()) {
						Answer answerObj = ((AverageNumberQuestion) q).getAnswerObj();
						if (answerObj != null) {
							String textValue = answerObj.getAnswer();
							if (!textValue.isEmpty()) {
								answers.add(textValue);
							}
						}
					}
				}
				AverageNumberQuestionDataContainer dc = new AverageNumberQuestionDataContainer();
				dc.answers = answers;
				dc.topics = addTopicListByCriteria(hibSess, question);
				
				content.append(ReportGeneratorUtils.printAggregationpartContainerTitle(entry.getKey()));
				content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
				questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
				content.append(ReportGeneratorUtils.printNumberOfResponses(dc.numberOfResponses, entry.getKey()));
			}
			
			if (deviationMap.size() == 0) {
				content.append(ReportGeneratorUtils.printNumberOfResponses(0));
			}
			
		} else if ((question instanceof MultipleChoiceQuestion) || (question instanceof RadioQuestion) || (question instanceof ComboQuestion)) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			List<DataSetContainer> containers = new ArrayList<DataSetContainer>();
			List<DataSetContainer> containers2 = new ArrayList<DataSetContainer>();
			List<Integer> numberOfResponses = new ArrayList<Integer>();
			int dCount = 0;
			int optionCount = 0;
			for (Entry<String, List<AbstractQuestion>> entry : deviationMap.entrySet()) {
				int count = 0;
				Map<String, Double> valueMap = new LinkedHashMap<String, Double>();
				
				optionCount = 0;
				for (Option option : question.getOptions()) {
					valueMap.put(option.getDisplayName(), 0.0);
					optionCount++;
				}
				
				List<Integer> ids = new ArrayList<Integer>();
				List<AbstractQuestion> questions = entry.getValue();
				for (AbstractQuestion q : questions) {
					if (q.getOriginQuestionId() == question.getId()) {
						for (Option o : q.getOptions()) {
							ids.add(o.getId());
						}
					}
				}
				
				if (ids.size() > 0) {
					// retrieve all selected options of the question
					Criteria criteria = hibSess.createCriteria(Option.class);
					criteria.add(Restrictions.in("id", ids));
					criteria.add(Restrictions.eq("originQuestionId", question.getId()));
					criteria.add(Restrictions.eq("submitted", true));
					
					List<?> list = criteria.list();
					Option option = null;
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
					}
					
					containers.add(new DataSetContainer(entry.getKey(), valueMap));
					containers2.add(new DataSetContainer(entry.getKey(), valueMap));
					numberOfResponses.add(count / optionCount);
					dCount++;
				} else {
					ReportGeneratorUtils.printNumberOfResponses(0);
				}
			}
			if (optionCount != 0) {
				rmd.useSlideToggleFragment = ReportGeneratorUtils.isSlideToggleFragmentUsedByOptions(optionCount, targetMedia);
				
				// draw chart
				MultipleChoiceQuestionChartDataContainer cdc = new MultipleChoiceQuestionChartDataContainer();
				cdc.dataSets = containers;
				cdc.numberOfResponses = numberOfResponses;
				cdc.filePath = selectedSurvey.getId() + "/" + getIdentifier() + "/" + question.getId() + dCount;
				
				if ((optionCount <= ReportGeneratorUtils.MAXOPTIONS_WITH_GRAPHICAL_REPRESENTATION)) {
					content.append(questionViewObject.getAggregatedPrintableRepresentation(cdc, rmd, question));
					if (rmd.useSlideToggleFragment) {
						showChartButtons(selectedSurvey, content, cdc.filePath);
					}
				}
				
				// draw tables
				{
					if (rmd.targetMedia.equals(TargetMedia.SCREEN)) {
						ReportGeneratorUtils.addDownloadExcelLink(content, survey.getId(), question);
					}
					if (rmd.useSlideToggleFragment) {
						ReportGeneratorUtils.addSimpleInfoButton(content);
						ReportGeneratorUtils.addSlideToggleButton(content, rmd.uuid);
					}
					int index = 0;
					for (DataSetContainer container : containers2) {
						MultipleChoiceQuestionDataContainer dc = new MultipleChoiceQuestionDataContainer();
						dc.numberOfResponses = numberOfResponses.get(index);
						dc.dataSet = container;
						content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
						content.append(ReportGeneratorUtils.printNumberOfResponses(dc.numberOfResponses, container.name));
						questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
						index++;
					}
				}
			}
			
		} else if (question instanceof IMatrixQuestion) {
			content.append(ReportGeneratorUtils.getQuestionText(question));
			
			List<AbstractQuestion> subquestions = question.getSubquestions();
			int subQuestionCount = subquestions.size();
			if (subQuestionCount > 0) {
				List<Option> optionList = subquestions.get(0).getOptions();
				int optionCount = optionList.size();
				for (Entry<String, List<AbstractQuestion>> entry : deviationMap.entrySet()) {
					
					// get an 2D array of n * m
					int[][] valueMap = new int[subQuestionCount][optionCount];
					
					Map<Integer, Integer> relationSubquestionToY = new HashMap<Integer, Integer>();
					Map<String, Integer> relationOptionToX = new HashMap<String, Integer>();
					
					List<Integer> subQuestionIdList = new ArrayList<Integer>();
					int yCount = 0;
					int xCount = 0;
					for (AbstractQuestion subQuestion : subquestions) {
						int id = (subQuestion).getId();
						subQuestionIdList.add(id);
						relationSubquestionToY.put(id, yCount);
						yCount++;
						xCount = 0;
						for (Option option : subQuestion.getOptions()) {
							relationOptionToX.put(option.getDisplayName(), xCount);
							xCount++;
						}
					}
					
					int count = 0;
					for (AbstractQuestion q : entry.getValue()) {
						if (q.getOriginQuestionId() == question.getId()) {
							for (Option option : ((MatrixQuestion) q).getSubquestionOptions()) {
								if (option.isSelected()) {
									valueMap[relationSubquestionToY.get(option.getOriginQuestionId())][relationOptionToX.get(option.getDisplayName())] += 1;
								}
								count++;
							}
						}
					}
					
					int numberOfResponses = (count / (xCount * yCount));
					
					rmd.useSlideToggleFragment = ReportGeneratorUtils.isSlideToggleFragmentUsedByRows(yCount, targetMedia);
					if (yCount <= ReportGeneratorUtils.MAXROWS_WITH_GRAPHICAL_REPRESENTATION) {
						// show charts
						List<DataSetContainer> dataSets = new ArrayList<DataSetContainer>();
						count = 0;
						for (AbstractQuestion subQuestion : subquestions) {
							Map<String, Double> rowValueMap = new LinkedHashMap<String, Double>();
							for (int j = 0; j < xCount; j++) {
								rowValueMap.put(optionList.get(j).getDisplayName(), (double) valueMap[count][j]);
							}
							dataSets.add(new DataSetContainer((subQuestion).getQuestion(), rowValueMap));
							count++;
						}
						
						MatrixQuestionChartDataContainer dc = new MatrixQuestionChartDataContainer();
						dc.numberOfResponses = numberOfResponses;
						dc.dataSets = dataSets;
						dc.filePath = selectedSurvey.getId() + "/" + getIdentifier() + "/" + (question).getId();
						content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
						if (rmd.useSlideToggleFragment) {
							showChartButtons(selectedSurvey, content, dc.filePath);
						}
					}
					
					if (rmd.targetMedia.equals(TargetMedia.SCREEN)) {
						ReportGeneratorUtils.addDownloadExcelLink(content, survey.getId(), question);
					}
					if (rmd.useSlideToggleFragment) {
						ReportGeneratorUtils.addSlideToggleButton(content, rmd.uuid);
					}
					
					MatrixQuestionDataContainer dc = new MatrixQuestionDataContainer();
					dc.numberOfResponses = numberOfResponses;
					dc.valueMap = valueMap;
					dc.datasetName = entry.getKey();
					
					content.append(questionViewObject.getAggregatedPrintableRepresentation(dc, rmd, question));
					questionViewObject.getAggregatedExcelRepresentation(dc, rmd, sheet);
					content.append(ReportGeneratorUtils.printNumberOfResponses(numberOfResponses, entry.getKey()));
				}
			} else {
				content.append(ReportGeneratorUtils.printNumberOfResponses(0));
			}
		} else {
			content.append("---");
		}
		saveExcelWorkbook(wb, question);
	}
	
	/**
	 * @param hibSess the current hibSess declared in evaluate
	 * @param question the current question to evaluate
	 * @return List of Topics
	 */
	@SuppressWarnings("unchecked")
	private List<Topic> addTopicListByCriteria(final Session hibSess, final AbstractQuestion question) {
		Criteria topicCriteria = hibSess.createCriteria(Topic.class);
		topicCriteria.add(Restrictions.eq("refQuestionId", question.getId()));
		return topicCriteria.list();
	}
	
	private void showChartButtons(final ISurvey selectedSurvey, final StringBuilder content, final String graphPath) {
		content.append("<div style=\"height: 32px; margin: 0; text-align:center;\">");
		ReportGeneratorUtils.addViewGraphButton(content, graphPath);
		ReportGeneratorUtils.addGraphDownloadButton(content, graphPath, selectedSurvey.getId());
		// ReportGeneratorUtils.addTableDownloadButton(content,
		// rmd.uuid,
		// ReportGeneratorUtils.getQuestionText(question));
		// ReportGeneratorUtils.addSimpleInfoButton(content);
		content.append("</div>");
	}
}
