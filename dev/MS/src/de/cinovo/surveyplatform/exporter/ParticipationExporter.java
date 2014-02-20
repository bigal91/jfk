/**
 *
 */
package de.cinovo.surveyplatform.exporter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.tools.ant.util.DateUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.model.IAnalysableQuestion;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Participation;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.IMatrixQuestion;
import de.cinovo.surveyplatform.model.question.IMultipleOptionsQuestion;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.util.TimeUtil;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class ParticipationExporter {
	
	/**
	 *
	 */
	// private static final String XML_VERSION_1_0 = "<?xml version=\"1.0\" ?>";
	
	public void exportToXml(final Survey survey, final List<Integer> toExport, final OutputStream outputStream, final SystemUser currentUser) throws IOException {
		
		String taskID = "participationExport" + UUID.randomUUID();
		
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		
		fbp.beginTask("Exporting Participants of: " + survey.getName(), taskID, currentUser.getActualUserName());
		
		final XStream xstream = new XStream(new StaxDriver());
		
		List<Participant> participantsToExport = new ArrayList<Participant>();
		
		List<Participant> allParticipants = survey.getParticipants();
		for (Participant participant : allParticipants) {
			if ((toExport == null) || toExport.contains(participant.getId())) {
				Participant cloneForExport = participant.cloneForExport();
				if (cloneForExport.getParticipation().getQuestionnaire() == null) {
					Questionnaire templateClone = survey.getQuestionnaire().templateClone();
					cloneForExport.getParticipation().setQuestionnaire(templateClone);
					templateClone.setParticipation(cloneForExport.getParticipation());
				}
				participantsToExport.add(cloneForExport);
			}
		}
		
		PrintStream ps = new PrintStream(outputStream);
		ExportContainer ec = new ExportContainer();
		ec.exports.add(participantsToExport);
		// ps.print(XML_VERSION_1_0);
		// ps.print("<export>");
		// ps.print("<survey>");
		// //
		// ps.print(xstream.toXML(survey.cloneWithId()).replace(XML_VERSION_1_0,
		// // ""));
		// ps.print("</survey>");
		// ps.print("<participants>");
		// String xml = ;
		// ps.print(xml.replace(XML_VERSION_1_0, ""));
		// ps.print("</participants>");
		// ps.print("</export>");
		ps.print(xstream.toXML(ec));
		fbp.finishTask(taskID);
	}
	
	
	public void exportToExcel(final Survey survey, final OutputStream outputStream, final SystemUser currentUser) throws IOException {
		Workbook wb = new SXSSFWorkbook();
		Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(survey.getName()));
		int participantIndex = 0;
		
		Cell cell;
		Cell firstRowCell;
		Row headerRow = sheet.createRow(0);
		// header: id
		firstRowCell = headerRow.createCell(0);
		firstRowCell.setCellValue("id");
		
		// header: date of submission
		firstRowCell = headerRow.createCell(1);
		firstRowCell.setCellValue("date of submission");
		
		String taskID = "participationExport" + UUID.randomUUID();
		
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		
		fbp.beginTask("Exporting RAW Data of: " + survey.getName(), taskID, currentUser.getActualUserName());
		
		//
		// Map<Integer, List<Page>> questionnairePages = new HashMap<Integer,
		// List<Page>>();
		// Map<Integer, List<Section>> pageSections = new HashMap<Integer,
		// List<Section>>();
		// Map<Integer, List<AbstractQuestion>> sectionQuestions = new
		// HashMap<Integer, List<AbstractQuestion>>();
		// Map<Integer, List<AbstractQuestion>> questionSubquestions = new
		// HashMap<Integer, List<AbstractQuestion>>();
		// Map<Integer, List<Option>> questionOptions = new HashMap<Integer,
		// List<Option>>();
		// List<Participant> allParticipants = new ArrayList<Participant>();
		// List<Questionnaire> allQuestionnaires = new
		// ArrayList<Questionnaire>();
		// List<Section> allSections = new ArrayList<Section>();
		// List<Page> allPages = new ArrayList<Page>();
		// List<AbstractQuestion> allQuestions = new
		// ArrayList<AbstractQuestion>();
		// List<Option> allOptions = new ArrayList<Option>();
		//
		// Session hibSess = HibernateUtil.getSessionFactory().openSession();
		// Transaction tx = hibSess.beginTransaction();
		//
		// // fetch participants
		// Criteria crit = hibSess.createCriteria(Participant.class);
		// if (survey instanceof SurveyDemo) {
		// crit.add(Restrictions.eq("surveyDemo", survey));
		// } else {
		// crit.add(Restrictions.isNull("surveyDemo"));
		// crit.add(Restrictions.eq("survey", survey));
		// }
		// crit.createAlias("participation", "part");
		// crit.add(Restrictions.eq("part.submitted", true));
		// System.out.println(survey.getId());
		// int totalParticipants = 0;
		// for (Object o : crit.list()) {
		// if (o instanceof Participant) {
		// allParticipants.add(((Participant) o));
		// totalParticipants++;
		// }
		// }
		//
		// // fetch participations
		// fbp.setProgress(taskID, 5);
		// crit = hibSess.createCriteria(Participation.class);
		// crit.add(Restrictions.in("participant", allParticipants));
		// for (Object o : crit.list()) {
		// if (o instanceof Participation) {
		// allQuestionnaires.add(((Participation) o).getQuestionnaire());
		// }
		// }
		//
		// // fetch pages
		// fbp.setProgress(taskID, 10);
		// crit = hibSess.createCriteria(Page.class);
		// crit.add(Restrictions.in("questionnaire", allQuestionnaires));
		// for (Object o : crit.list()) {
		// if (o instanceof Page) {
		// Page p = (Page) o;
		// List<Page> list =
		// questionnairePages.get(p.getQuestionnaire().getId());
		// if (list == null) {
		// list = new ArrayList<Page>();
		// questionnairePages.put(p.getQuestionnaire().getId(), list);
		// }
		// list.add(p);
		// allPages.add(p);
		// }
		// }
		//
		// // fetch sections
		// fbp.setProgress(taskID, 15);
		// crit = hibSess.createCriteria(Section.class);
		// crit.add(Restrictions.in("page", allPages));
		// for (Object o : crit.list()) {
		// if (o instanceof Section) {
		// Section s = (Section) o;
		// List<Section> list = pageSections.get(s.getPage().getId());
		// if (list == null) {
		// list = new ArrayList<Section>();
		// pageSections.put(s.getPage().getId(), list);
		// }
		// list.add(s);
		// allSections.add(s);
		// }
		// }
		//
		// // fetch questions
		// fbp.setProgress(taskID, 20);
		// crit = hibSess.createCriteria(AbstractQuestion.class);
		// crit.add(Restrictions.in("section", allSections));
		// for (Object o : crit.list()) {
		// if (o instanceof AbstractQuestion) {
		// AbstractQuestion q = (AbstractQuestion) o;
		// List<AbstractQuestion> list =
		// sectionQuestions.get(q.getSection().getId());
		// if (list == null) {
		// list = new ArrayList<AbstractQuestion>();
		// sectionQuestions.put(q.getSection().getId(), list);
		// }
		// list.add(q);
		// allQuestions.add(q);
		// }
		// }
		//
		// // fetch subquestions
		// fbp.setProgress(taskID, 30);
		// crit = hibSess.createCriteria(AbstractQuestion.class);
		// crit.add(Restrictions.or(Restrictions.in("parentMultipleChoiceQuestion",
		// allQuestions), Restrictions.in("parentRadioQuestion",
		// allQuestions)));
		// for (Object o : crit.list()) {
		// if (o instanceof AbstractQuestion) {
		// AbstractQuestion q = (AbstractQuestion) o;
		// List<AbstractQuestion> list =
		// questionSubquestions.get(q.getParentQuestion().getId());
		// if (list == null) {
		// list = new ArrayList<AbstractQuestion>();
		// questionSubquestions.put(q.getParentQuestion().getId(), list);
		// }
		// list.add(q);
		// allQuestions.add(q);
		// }
		// }
		//
		// // fetch options
		// fbp.setProgress(taskID, 40);
		// crit = hibSess.createCriteria(Option.class);
		// crit.add(Restrictions.in("question", allQuestions));
		// for (Object o : crit.list()) {
		// if (o instanceof Option) {
		// Option opt = (Option) o;
		// List<Option> list = questionOptions.get(opt.getQuestion().getId());
		// if (list == null) {
		// list = new ArrayList<Option>();
		// questionOptions.put(opt.getQuestion().getId(), list);
		// }
		// list.add(opt);
		// allOptions.add(opt);
		// }
		// }
		
		List<Participant> participants = survey.getParticipants();
		int totalParticipants = participants.size();
		int columnIndex = 0;
		int rowIndex = 0;
		for (Participant p : participants) {
			participantIndex++;
			Participation participation = p.getParticipation();
			Questionnaire questionnaire = participation.getQuestionnaire();
			fbp.setProgress(taskID, 50 + (int) Math.round(((double) participantIndex * 50) / totalParticipants));
			if ((questionnaire != null) && participation.isSubmitted()) {
				// System.out.println(questionnaire.getId());
				rowIndex++;
				columnIndex = 0;
				
				Row row = sheet.createRow(rowIndex);
				// column: id
				cell = row.createCell(columnIndex++);
				cell.setCellValue(p.getNumber());
				
				// column: date of submission
				cell = row.createCell(columnIndex++);
				cell.setCellValue(TimeUtil.getLocalTime(survey.getCreator(), p.getSurveySubmitted()));
				
				for (Page page : questionnaire.getPages()) { // questionnairePages.get(questionnaire.getId()))
					// {
					for (Section section : page.getSections()) { // pageSections.get(page.getId()))
						// {
						for (AbstractQuestion question : section.getQuestions()) { // sectionQuestions.get(section.getId()))
							// {
							if (question instanceof IAnalysableQuestion) {
								if (question instanceof IMatrixQuestion) {
									// matrixquestion
									for (AbstractQuestion subQuestion : question.getSubquestions()) { // questionSubquestions.get(question.getId()))
										// {
										if (rowIndex == 1) {
											firstRowCell = headerRow.createCell(columnIndex);
											if ((question.getAlias() == null) || question.getAlias().isEmpty()) {
												firstRowCell.setCellValue(question.getId() + "_" + (subQuestion).getId());
											} else {
												firstRowCell.setCellValue(question.getAlias());
											}
										}
										if (question.isSubmitted()) {
											cell = row.createCell(columnIndex++);
											cell.setCellValue(this.getFormattedAnswer((subQuestion).getAnswer(subQuestion.getOptions()))); // questionOptions.get(subQuestion.getId()))));
										} else {
											columnIndex++;
										}
									}
								} else if (question instanceof TextfieldQuestion) {
									int count = 0;
									for (Answer answer : question.getAnswer()) {
										count++;
										if (rowIndex == 1) {
											firstRowCell = headerRow.createCell(columnIndex);
											firstRowCell.setCellValue(question.getId() + "_" + count);
										}
										if (question.isSubmitted()) {
											cell = row.createCell(columnIndex++);
											cell.setCellValue(answer.getAnswer());
										} else {
											columnIndex++;
										}
										
									}
								} else {
									// simple question
									if (rowIndex == 1) {
										firstRowCell = headerRow.createCell(columnIndex);
										if ((question.getAlias() == null) || (question.getAlias() == "")) {
											firstRowCell.setCellValue(question.getId());
										} else {
											firstRowCell.setCellValue(question.getAlias());
										}
									}
									if (question.isSubmitted()) {
										cell = row.createCell(columnIndex++);
										if (question instanceof IMultipleOptionsQuestion) {
											cell.setCellValue(this.getFormattedAnswer(question.getAnswer(question.getOptions()))); // questionOptions.get(question.getId()))));
										} else {
											cell.setCellValue(this.getFormattedAnswer(question.getAnswer()));
										}
									} else {
										columnIndex++;
									}
								}
							}
						}
					}
				}
				fbp.addFeedback(taskID, DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN) + ": Exporting participant " + p.getNumber() + ", " + participantIndex + " of " + totalParticipants, Status.OK);
				
			} else {
				fbp.addFeedback(taskID, DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN) + ": Participant " + p.getNumber() + " has not submitted yet", Status.WARNING);
			}
			
		}
		
		// tx.commit();
		// hibSess.close();
		fbp.setMessage(taskID, "Export successfull.");
		fbp.finishTask(taskID, false);
		
		wb.write(outputStream);
	}
	
	/**
	 * @param subQuestion
	 * @return
	 */
	private String getFormattedAnswer(final List<Answer> answers) {
		
		String formattedAnswers = "";
		for (int i = 0; i < answers.size(); i++) {
			formattedAnswers += answers.get(i);
			if (!(i == (answers.size() - 1))) {
				formattedAnswers += ", ";
			}
		}
		
		return formattedAnswers;
	}
	
}
