/**
 * 
 */
package de.cinovo.surveyplatform.servlets.helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import de.cinovo.surveyplatform.model.ILogicApplicableQuestion;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.ParamUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class FindLogicElementOrphans extends HttpServlet {
	
	private static final long serialVersionUID = 3743250907576412606L;
	
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		
		if (AuthUtil.hasRight(AuthUtil.checkAuth(req), UserRights.ADMINISTRATOR)) {
			int surveyID = ParamUtil.getSafeIntFromParam(req, "surveyID");
			if (surveyID != 0) {
				
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				Transaction tx = hibSess.beginTransaction();
				
				Criteria createCriteria = hibSess.createCriteria(Survey.class);
				createCriteria.add(Restrictions.eq("id", surveyID));
				
				List<?> list = createCriteria.list();
				Map<String, QuestionnaireLogicElement> boundElements = new HashMap<String, QuestionnaireLogicElement>();
				Map<String, QuestionnaireLogicElement> pointerElements = new HashMap<String, QuestionnaireLogicElement>();
				
				Map<Integer, QuestionnaireLogicElement> allQles = new HashMap<Integer, QuestionnaireLogicElement>();
				
				{
					Criteria c = hibSess.createCriteria(QuestionnaireLogicElement.class);
					c.add(Restrictions.eq("surveyId", surveyID));
					List<?> qles = c.list();
					for (Object o : qles) {
						QuestionnaireLogicElement qle = (QuestionnaireLogicElement) o;
						allQles.put(qle.getId(), qle);
						
					}
				}
				
				for (Object o : list) {
					if (o instanceof Survey) {
						Survey survey = (Survey) o;
						List<Participant> participants = survey.getParticipants();
						for (Participant participant : participants) {
							if (participant.getParticipation() != null) {
								Questionnaire questionnaire = participant.getParticipation().getQuestionnaire();
								if (questionnaire != null) {
									for (Page p : questionnaire.getPages()) {
										resp.getWriter().println("P " + p.getLocalId());
										{
											for (QuestionnaireLogicElement qle : allQles.values()) {
												if (qle.getIdOfPart() == p.getLocalId()) {
													pointerElements.put(qle.getId() + "_" + qle.getIdOfPart() + "_" + qle.getQuestionIdWithLogic() + "_" + qle.getTypeOfPart(), qle);
												}
											}
										}
										
										for (Section s : p.getSections()) {
											resp.getWriter().println(" S " + s.getLocalId());
											{
												for (QuestionnaireLogicElement qle : allQles.values()) {
													if (qle.getIdOfPart() == s.getLocalId()) {
														pointerElements.put(qle.getId() + "_" + qle.getIdOfPart() + "_" + qle.getQuestionIdWithLogic() + "_" + qle.getTypeOfPart(), qle);
													}
												}
												
											}
											for (AbstractQuestion q : s.getQuestions()) {
												if (q instanceof ILogicApplicableQuestion) {
													resp.getWriter().println("  Q " + q.getLocalId());
													List<QuestionnaireLogicElement> logicElements = q.getLogicElements();
													for (QuestionnaireLogicElement qle : logicElements) {
														boundElements.put(qle.getId() + "_" + qle.getIdOfPart() + "_" + qle.getQuestionIdWithLogic() + "_" + qle.getTypeOfPart(), qle);
													}
													
													{
														for (QuestionnaireLogicElement qle : allQles.values()) {
															if (qle.getIdOfPart() == q.getLocalId()) {
																pointerElements.put(qle.getId() + "_" + qle.getIdOfPart() + "_" + qle.getQuestionIdWithLogic() + "_" + qle.getTypeOfPart(), qle);
															}
														}
														
													}
												}
											}
											resp.getWriter().flush();
										}
									}
									
								}
							}
						}
					}
				}
				resp.getWriter().println();
				resp.getWriter().println();
				
				for (String bound : boundElements.keySet()) {
					pointerElements.remove(bound);
				}
				for (QuestionnaireLogicElement orphan : pointerElements.values()) {
					resp.getWriter().println(orphan.getId());
					hibSess.delete(orphan);
				}
				
				tx.commit();
				hibSess.close();
				resp.getWriter().println("Done.");
			}
		}
	}
	
}
