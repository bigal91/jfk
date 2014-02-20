/**
 *
 */
package de.cinovo.surveyplatform.servlets.dal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.Topic;
import de.cinovo.surveyplatform.model.factory.DtoFactory;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.TopicCombination;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.ui.views.FreeTextFilterUtil;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;



/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class TopicDal extends AbstractSccServlet {
	
	/**
	 *
	 */
	private static final String ANSWER_SPLITTER = "::";
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String PARAM_TOPIC_ID = "topic";
	public static final String PARAM_TOPIC_TITLE = "title";
	public static final String PARAM_FILTERBOX_ID = "boxId";
	public static final String PARAM_TOPIC_QUESTIONREF = "qref";
	public static final String PARAM_CMD_REMOVEANSWER = "rmv";
	public static final String PARAM_CMD_ADDANSWER = "add";
	
	private static final String PARAM_ANSWER_ID_LIST = "answerIds";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#processCreate(javax
	 * .servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processCreate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			if (ParamUtil.checkAllParamsSet(req, PARAM_TOPIC_TITLE, PARAM_TOPIC_QUESTIONREF)) {
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				Transaction tx = hibSess.beginTransaction();
				String title = ParamUtil.getSafeParam(req, PARAM_TOPIC_TITLE);
				FeedBackProvider fbp = FeedBackProvider.getInstance();
				String taskID = createTaskID(req);
				try {
					// initiate Topic
					Topic newTopic = new Topic();
					newTopic.setRefQuestionId(Integer.parseInt(req.getParameter(PARAM_TOPIC_QUESTIONREF)));
					newTopic.setTitle(title);
					createAnswers(req, hibSess, newTopic);
					hibSess.save(newTopic);
					tx.commit();
					fbp.setMessage(taskID, "Created new topic " + title, currentUser.getActualUserName());
					fbp.finishTask(taskID, false);
					if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
						resp.getWriter().print(DtoFactory.getInstance().createDto(newTopic));
					} else {
						if (ParamUtil.checkAllParamsSet(req, PARAM_FILTERBOX_ID)) {
							FreeTextFilterUtil ftfu = new FreeTextFilterUtil(newTopic.getRefQuestionId(), ParamUtil.getSafeParam(req, PARAM_FILTERBOX_ID));
							resp.getWriter().print(ftfu.getTopicItem(newTopic));
						}
					}
				} catch (Exception ex) {
					fbp.setMessage(taskID, "There was an error creating the new topic " + title, currentUser.getActualUserName());
					fbp.addFeedback(taskID, ex.getMessage(), Status.ERROR);
					fbp.finishTask(taskID, true);
					Logger.err("Cannot create Topic!", ex);
					tx.rollback();
				} finally {
					hibSess.close();
				}
			}
		}
	}
	
	private void createAnswers(final HttpServletRequest req, final Session hibSess, final Topic topic) {
		List<Answer> answers = topic.getAnswers();
		if (answers == null) {
			answers = new ArrayList<Answer>();
		}
		
		if (ParamUtil.checkAllParamsSet(req, PARAM_CMD_ADDANSWER, PARAM_ANSWER_ID_LIST)) {
			String answerIDsParam = ParamUtil.getSafeParam(req, PARAM_ANSWER_ID_LIST);
			String[] splittedAnswerIdsParam = answerIDsParam.split(ANSWER_SPLITTER);
			
			for (String current : splittedAnswerIdsParam) {
				if (current.contains("_")) {
					String[] parts = current.split("_");
					int answerId = Integer.parseInt(parts[0]);
					int cutBegin = Integer.parseInt(parts[1]);
					int cutEnd = Integer.parseInt(parts[2]);
					
					Answer answer = (Answer) hibSess.load(Answer.class, answerId);
					if (!answer.equals("")) {
						Answer finalAnswer = new Answer(answer.getAnswer());
						createTopicCombination(cutEnd, cutBegin, answerId, finalAnswer);
						answers.add(finalAnswer);
					}
					
				}
			}
		}
		topic.setAnswers(answers);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#processDelete(javax
	 * .servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processDelete(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			if (ParamUtil.checkAllParamsSet(req, PARAM_TOPIC_ID)) {
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				Transaction tx = hibSess.beginTransaction();
				FeedBackProvider fbp = FeedBackProvider.getInstance();
				String taskID = createTaskID(req);
				try {
					Topic topic = (Topic) hibSess.load(Topic.class, (long) ParamUtil.getSafeIntFromParam(req, PARAM_TOPIC_ID));
					String title = topic.getTitle();
					hibSess.delete(topic);
					tx.commit();
					fbp.setMessage(taskID, "Removed the topic " + title, currentUser.getActualUserName());
					fbp.finishTask(taskID, false);
				} catch (Exception ex) {
					fbp.setMessage(taskID, "Could not remove the topic", currentUser.getActualUserName());
					fbp.addFeedback(taskID, ex.getMessage(), Status.ERROR);
					fbp.finishTask(taskID, true);
					Logger.err("Cannot remove topic!", ex);
					tx.rollback();
				} finally {
					hibSess.close();
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#processRetrieve(
	 * javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			if (ParamUtil.checkAllParamsSet(req, PARAM_TOPIC_ID)) {
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				Transaction tx = hibSess.beginTransaction();
				try {
					Topic topic = (Topic) hibSess.load(Topic.class, ParamUtil.getSafeIntFromParam(req, PARAM_TOPIC_ID));
					tx.commit();
					if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
						resp.getWriter().print(DtoFactory.getInstance().createDto(topic));
					} else {
						FreeTextFilterUtil ftfu = new FreeTextFilterUtil(topic.getRefQuestionId());
						resp.getWriter().print(ftfu.getTopicItem(topic));
					}
					
				} catch (Exception ex) {
					Logger.err("There was an error retrieving the topic!", ex);
					tx.rollback();
				} finally {
					hibSess.close();
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#processUpdate(javax
	 * .servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processUpdate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			if (ParamUtil.checkAllParamsSet(req, PARAM_TOPIC_ID)) {
				
				long topicID = ParamUtil.getSafeIntFromParam(req, PARAM_TOPIC_ID);
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				FeedBackProvider fbp = FeedBackProvider.getInstance();
				String taskID = createTaskID(req);
				try {
					Transaction tx = hibSess.beginTransaction();
					
					// get existing Topic
					Topic topic = (Topic) hibSess.load(Topic.class, topicID);
					String title = topic.getTitle();
					if (ParamUtil.checkAllParamsSet(req, PARAM_TOPIC_TITLE)) {
						topic.setTitle(ParamUtil.getSafeParam(req, PARAM_TOPIC_TITLE));
						title = topic.getTitle();
					}
					if (ParamUtil.checkAllParamsSet(req, PARAM_CMD_ADDANSWER, PARAM_ANSWER_ID_LIST)) {
						createAnswers(req, hibSess, topic);
					}
					if (ParamUtil.checkAllParamsSet(req, PARAM_CMD_REMOVEANSWER)) {
						int answerID = ParamUtil.getSafeIntFromParam(req, PARAM_CMD_REMOVEANSWER);
						List<Answer> answers = topic.getAnswers();
						if (answers != null) {
							Answer toRemove = null;
							for (Answer answer : answers) {
								if (answer.getId() == answerID) {
									toRemove = answer;
									break;
								}
							}
							if (toRemove != null) {
								answers.remove(toRemove);
							}
						}
					}
					
					hibSess.save(topic);
					tx.commit();
					fbp.setMessage(taskID, "Saved the topic " + title, currentUser.getActualUserName());
					fbp.finishTask(taskID, false);
					if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
						resp.getWriter().print(DtoFactory.getInstance().createDto(topic));
					} else {
						if (ParamUtil.checkAllParamsSet(req, PARAM_FILTERBOX_ID)) {
							FreeTextFilterUtil ftfu = new FreeTextFilterUtil(topic.getRefQuestionId(), ParamUtil.getSafeParam(req, PARAM_FILTERBOX_ID));
							resp.getWriter().print(ftfu.getTopicItem(topic));
						}
					}
					
				} catch (Exception ex) {
					fbp.setMessage(taskID, "There was an error updating the topic", currentUser.getActualUserName());
					fbp.addFeedback(taskID, ex.getMessage(), Status.ERROR);
					fbp.finishTask(taskID, true);
					Logger.err("Cannot create Topic!", ex);
				} finally {
					hibSess.close();
				}
			}
		}
	}
	
	private void createTopicCombination(final int cutEnd, final int cutBegin, final int answerId, final Answer answer) {
		if (answer.getTopicCombinations() == null) {
			answer.setTopicCombinations(new ArrayList<TopicCombination>());
		}
		answer.getTopicCombinations().add(new TopicCombination(answerId, cutBegin, cutEnd));
	}
	
	private String createTaskID(final HttpServletRequest req) {
		return "page.analyse." + req.getSession().getId();
	}
}
