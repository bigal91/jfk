/**
 *
 */
package de.cinovo.surveyplatform.servlets.dal;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.google.gson.Gson;

import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.factory.DtoFactory;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.ParamUtil;

/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class QuestionnaireLogicElementDal extends AbstractSccServlet {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @seescc.servlets.AbstractSccServlet#processRetrieve(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		if (currentUser != null) {
			
			if (ParamUtil.checkAllParamsSet(req, SurveyDal.PARAM_QUESTIONID)) {
				int questionId = ParamUtil.getSafeIntFromParam(req, SurveyDal.PARAM_QUESTIONID);
				
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					Criteria criteria = hibSess.createCriteria(AbstractQuestion.class);
					criteria.add(Restrictions.eq("id", questionId));
					AbstractQuestion question = (AbstractQuestion) criteria.uniqueResult();
					if (question != null) {
						if ((question.getLogicElements() != null) && (question.getLogicElements().size() > 0)) {
							if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
								Gson gson = new Gson();
								resp.getWriter().print(gson.toJson(DtoFactory.getInstance().createDto(question.getLogicElements())));
							} else {
								resp.getWriter().print("only json supported");
							}
						}
					}
					tx.commit();
				} finally {
					hibSess.close();
				}
			}
		}
	}
	
}
