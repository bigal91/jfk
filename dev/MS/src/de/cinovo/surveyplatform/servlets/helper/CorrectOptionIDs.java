/**
 * 
 */
package de.cinovo.surveyplatform.servlets.helper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.IMatrixQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.ParamUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class CorrectOptionIDs extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		
		if (AuthUtil.hasRight(AuthUtil.checkAuth(req), UserRights.ADMINISTRATOR)) {
			int surveyID = ParamUtil.getSafeIntFromParam(req, "surveyID");
			if (surveyID != 0) {
				
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				Transaction tx = hibSess.beginTransaction();
				
				Criteria createCriteria = hibSess.createCriteria(Survey.class);
				createCriteria.add(Restrictions.eq("id", surveyID));
				PrintWriter w = resp.getWriter();
				List<?> list = createCriteria.list();
				for (Object o : list) {
					if (o instanceof Survey) {
						Survey survey = (Survey) o;
						{
							Questionnaire questionnaire = survey.getQuestionnaire();
							for (Page p : questionnaire.getPages()) {
								w.println("Page " + p.getLocalId());
								for (Section s : p.getSections()) {
									w.println("- Section " + s.getLocalId());
									for (AbstractQuestion q : s.getQuestions()) {
										w.println("-- Question " + q.getId() + " origin: " + q.getLocalId() + " - " + q.getQuestion());
										if (q instanceof IMatrixQuestion) {
											List<AbstractQuestion> subquestions = q.getSubquestions();
											for (AbstractQuestion sq : subquestions) {
												w.println("--- SubQuestion " + sq.getId() + " origin: " + sq.getOriginQuestionId() + " - " + sq.getQuestion());
												for (Option opt : sq.getAllOptions()) {
													w.print("---- Option: " + opt.getDisplayName() + ", origin: " + opt.getOriginQuestionId());
													if (opt.getOriginQuestionId() != sq.getId()) {
														opt.setOriginQuestionId(sq.getId());
														w.print(" -> CORRECTED TO " + sq.getId());
													}
													w.println();
												}
											}
										}
									}
								}
							}
						}
						w.println("--------------------------------------------------------------------------------");
						{
							for (Participant participant : survey.getParticipants()) {
								
								Questionnaire questionnaire = participant.getParticipation().getQuestionnaire();
								if (questionnaire != null) {
									w.println(participant.getName() + " Questionnaire: " + questionnaire.getId());
									for (Page p : questionnaire.getPages()) {
										w.println("Page " + p.getLocalId());
										for (Section s : p.getSections()) {
											w.println("- Section " + s.getLocalId());
											for (AbstractQuestion q : s.getQuestions()) {
												w.println("-- Question " + q.getId() + " origin: " + q.getLocalId() + " - " + q.getQuestion());
												if (q instanceof IMatrixQuestion) {
													List<AbstractQuestion> subquestions = q.getSubquestions();
													for (AbstractQuestion sq : subquestions) {
														w.println("--- SubQuestion " + sq.getId() + " origin: " + sq.getOriginQuestionId() + " - " + sq.getQuestion());
														
														for (Option opt : sq.getAllOptions()) {
															w.print("---- Option: " + opt.getDisplayName() + ", origin: " + opt.getOriginQuestionId());
															if (opt.getOriginQuestionId() != sq.getOriginQuestionId()) {
																opt.setOriginQuestionId(sq.getOriginQuestionId());
																w.print(" -> CORRECTED TO " + sq.getOriginQuestionId());
															}
															w.println();
														}
														
													}
												}
											}
										}
										
									}
								}
							}
						}
					}
					break;
				}
				tx.commit();
				hibSess.close();
			}
		}
	}
	
}
