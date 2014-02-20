/**
 * 
 */
package de.cinovo.surveyplatform.servlets.helper;

import java.io.IOException;
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
public class CorrectQuestionIDs extends HttpServlet {
	
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
				int optionCount = 0;
				for (Object o : list) {
					if (o instanceof Survey) {
						Survey survey = (Survey) o;
						Questionnaire questionnaire = survey.getQuestionnaire();
						for (Page p : questionnaire.getPages()) {
							for (Section s : p.getSections()) {
								for (AbstractQuestion q : s.getQuestions()) {
									resp.getWriter().println(q.getQuestion());
									if (q instanceof IMatrixQuestion) {
										List<AbstractQuestion> subquestions = q.getSubquestions();
										for (AbstractQuestion sq : subquestions) {
											String questionText = (sq).getQuestion();
											resp.getWriter().println(questionText);
											Criteria criteria = hibSess.createCriteria(AbstractQuestion.class);
											criteria.add(Restrictions.eq("question", questionText));
											List<?> questions = criteria.list();
											for (Object obj : questions) {
												AbstractQuestion subq = (AbstractQuestion) obj;
												for (Option opt : subq.getAllOptions()) {
													int id = (sq).getId();
													optionCount++;
													opt.setOriginQuestionId(id);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				tx.commit();
				hibSess.close();
				resp.getWriter().println(optionCount + " Options corrected.");
			}
		}
	}
	
}
