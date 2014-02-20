/**
 *
 */
package de.cinovo.surveyplatform.exporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.tools.ant.util.DateUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.Topic;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.util.ExcelUtil;
import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class TopicExporter {
	
	public void exportToExcel(final int refQuestionId, final FileOutputStream outputStream, final SystemUser currentUser, final String count) throws IOException {
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			
			Query query = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_ID);
			query.setParameter("1", refQuestionId);
			AbstractQuestion question = (AbstractQuestion) query.uniqueResult();
			
			SXSSFWorkbook wb = new SXSSFWorkbook();
			
			// simpleCenterAlignment: a style for all other cells created.
			CellStyle simpleCenterStyle = wb.createCellStyle();
			simpleCenterStyle.setAlignment(CellStyle.ALIGN_CENTER);
			simpleCenterStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
			
			// centerGreyStyle: Style for the last row
			CellStyle centerGreyStyle = wb.createCellStyle();
			centerGreyStyle.setFillForegroundColor(new HSSFColor.GREY_25_PERCENT().getIndex());
			centerGreyStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
			centerGreyStyle.setAlignment(CellStyle.ALIGN_CENTER);
			
			Sheet sheet = createSheet(question, wb);
			sheet.setMargin((short) 4, 1.2);
			
			Cell cell;
			Cell firstRowCell;
			
			Row headerRow = sheet.createRow(0);
			headerRow.setHeightInPoints(30);
			// header: topic
			firstRowCell = headerRow.createCell(0);
			
			CellStyle blueStyle = ExcelUtil.createBlueStyle(firstRowCell);
			
			firstRowCell.setCellValue("Topic");
			firstRowCell.setCellStyle(blueStyle);
			
			// header: Itemcount
			firstRowCell = headerRow.createCell(1);
			firstRowCell.setCellValue("Number of assigned \n statements");
			firstRowCell.setCellStyle(blueStyle);
			
			// header: percentage of Answers
			firstRowCell = headerRow.createCell(2);
			firstRowCell.setCellValue("Percentage \n %");
			firstRowCell.setCellStyle(blueStyle);
			
			String taskID = "topicExport" + UUID.randomUUID();
			FeedBackProvider fbp = FeedBackProvider.getInstance();
			fbp.beginTask("Exporting Topic Excel Sheet of Question: " + question.getAlias() + " (" + question.getId() + ")", taskID, currentUser.getActualUserName());
			
			Criteria criteria = hibSess.createCriteria(Topic.class);
			criteria.add(Restrictions.eq("refQuestionId", refQuestionId));
			List<?> list = criteria.list();
			
			int currentTopic = 1;
			int totalTopics = list.size();
			int totalAssignedStatements = 0;
			int totalPercentage = 0;
			double actualPercentage = 0;
			
			int totalAnswers = 0;
			for (Object o : list) {
				Topic topic = (Topic) o;
				totalAnswers += topic.getAnswers().size();
			}
			
			// we want to sort the topics according their answer sizes
			TreeMap<String, Topic> topics = new TreeMap<String, Topic>();
			for (Object o : list) {
				Topic topic = (Topic) o;
				topics.put(String.format("%020d", topic.getAnswers().size()) + "_" + topic.getTitle() + topic.getId(), topic);
			}
			
			for (Topic topic : topics.descendingMap().values()) {
				
				fbp.setProgress(taskID, (int) Math.round(((double) currentTopic * 100) / totalTopics));
				
				fbp.addFeedback(taskID, DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN) + ": Exporting row " + currentTopic + " (total: " + totalTopics + ")", Status.OK);
				Row row = sheet.createRow(currentTopic);
				
				// column: topic
				cell = row.createCell(0);
				cell.setCellValue(topic.getTitle());
				cell.setCellStyle(simpleCenterStyle);
				
				// column: Assigned Statements
				cell = row.createCell(1);
				int answersSize = topic.getAnswers().size();
				cell.setCellValue(answersSize);
				totalAssignedStatements += answersSize;
				cell.setCellStyle(simpleCenterStyle);
				
				// column: percentage of answers in a topic
				cell = row.createCell(2);
				actualPercentage += ((double) answersSize * 100) / totalAnswers;
				cell.setCellValue(Math.round(((double) answersSize * 100) / totalAnswers) + " %");
				cell.setCellStyle(simpleCenterStyle);
				
				currentTopic++;
				
			}
			
			totalPercentage = (int) Math.round(actualPercentage);
			
			// the "Total" Row (last row in table)
			Row lastRow = sheet.createRow(currentTopic);
			Cell lastRowCell = lastRow.createCell(0);
			lastRowCell.setCellValue("Total");
			lastRowCell.setCellStyle(centerGreyStyle);
			lastRowCell = lastRow.createCell(1);
			lastRowCell.setCellValue(totalAssignedStatements);
			lastRowCell.setCellStyle(centerGreyStyle);
			lastRowCell = lastRow.createCell(2);
			lastRowCell.setCellValue(totalPercentage + " %");
			lastRowCell.setCellStyle(centerGreyStyle);
			
			// width: auto-size width of the existing columns
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
			
			tx.commit();
			fbp.setMessage(taskID, "Export successfull.");
			fbp.finishTask(taskID, false);
			
			wb.write(outputStream);
		} finally {
			hibSess.close();
		}
		
	}
	
	private Sheet createSheet(final AbstractQuestion question, final Workbook workbook) {
		Sheet sheet;
		String sheetTitle;
		if (question.getAlias() == null) {
			sheetTitle = question.getQuestion();
		} else {
			sheetTitle = question.getAlias();
		}
		sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(sheetTitle));
		return sheet;
	}
	
	public void exportToFreemind(final int refQuestionId, final FileOutputStream outputStream, final SystemUser currentUser, final String count) {
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		// TODO
		// add modified attribute
		// only add POSITION attribute for first child-nodes
		// iterate Left-Right-Left-Right...
		try {
			Transaction tx = hibSess.beginTransaction();
			Query query = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_ID);
			query.setParameter("1", refQuestionId);
			AbstractQuestion question = (AbstractQuestion) query.uniqueResult();
			
			Criteria criteria = hibSess.createCriteria(Topic.class);
			criteria.add(Restrictions.eq("refQuestionId", refQuestionId));
			List<?> list = criteria.list();
			
			Element map = new Element("map");
			map.setAttribute("version", "0.9.0");
			
			String tStamp = "" + new Date().getTime();
			double totalAnswers = 0;
			for (Object o : list) {
				Topic topic = (Topic) o;
				totalAnswers += topic.getAnswers().size();
			}
			
			Element root = new Element("node");
			root.setAttribute("CREATED", tStamp);
			root.setAttribute("ID", "node_0");
			root.setAttribute("MODIFIED", tStamp);
			if (question.getAlias() == null) {
				root.setAttribute("TEXT", question.getQuestion());
			} else {
				root.setAttribute("TEXT", question.getAlias());
			}
			
			int topicCounter = 0;
			int answerCounter = 0;
			boolean leftPosition = true;
			for (Object o : list) {
				Topic topic = (Topic) o;
				Element node = new Element("node");
				node.setAttribute("CREATED", tStamp);
				node.setAttribute("ID", "node_0_" + topicCounter);
				node.setAttribute("MODIFIED", tStamp);
				if (leftPosition){
					node.setAttribute("POSITION", "left");
				} else {
					node.setAttribute("POSITION", "right");
				}
				node.setAttribute("TEXT", topic.getTitle());
				root.addContent(node);
				double topicAnswers = topic.getAnswers().size();
				int percentage = (int) ((topicAnswers / totalAnswers) * 100);
				Element subNode = new Element("node");
				subNode.setAttribute("CREATED", tStamp);
				subNode.setAttribute("ID", "node_0_" + topicCounter + "_i");
				subNode.setAttribute("MODIFIED", tStamp);
				subNode.setAttribute("TEXT", topicAnswers + " (" + percentage + "%)");
				node.addContent(subNode);
				// iterate answers
				for (Answer ans : topic.getAnswers()) {
					Element answerNode = new Element("node");
					answerNode.setAttribute("CREATED", tStamp);
					answerNode.setAttribute("ID", "node_0_" + topicCounter + "_i_" + answerCounter);
					answerNode.setAttribute("MODIFIED", tStamp);
					answerNode.setAttribute("TEXT", ans.getAnswer());
					subNode.addContent(answerNode);
					answerCounter++;
				}
				leftPosition = !leftPosition;
				topicCounter++;
			}
			map.addContent(root);
			Document doc = new Document(map);
			// serialize it onto outputStream
			try {
				XMLOutputter serializer = new XMLOutputter(Format.getCompactFormat().setOmitDeclaration(true));
				serializer.output(doc, outputStream);
			} catch (IOException e) {
				
				Logger.err("Error during serialisation", e);
			}
			// JDOM stuff here
			
			tx.commit();
		} finally {
			hibSess.close();
		}
	}
}
