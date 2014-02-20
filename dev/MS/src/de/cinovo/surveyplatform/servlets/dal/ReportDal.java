package de.cinovo.surveyplatform.servlets.dal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.reporting.GenericReportInfo;
import de.cinovo.surveyplatform.reporting.reports.GenericReport.Type;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;

/**
 *
 * Copyright 2010 Cinovo AG<br>
 * <br>
 *
 * @author yschubert
 *
 */
public class ReportDal extends AbstractSccServlet {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_REPORTNAME = "reportName";
	private static final String PARAM_DEVIATIONCHECK = "deviationCheck";
	private static final String PARAM_PERFORMANCECHECK = "performanceCheck";
	private static final String PARAM_QUESTIONS = "questions[]";
	private static final String PARAM_SURVEYID = "surveyId";
	private static final String PARAM_DECISIONQUESTIONID = "decisionQuestionId";
	private static final String PARAM_REPORTDESCRIPTION = "reportDescription";
	private static final String DELETEREPORT = "deleteReport";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#isAccessAllowed(
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse,
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet.Method)
	 */
	@Override
	public boolean isAccessAllowed(final HttpServletRequest req, final Method method, final SystemUser currentUser) {
		if (AuthUtil.isAllowedToCreateReports(currentUser)) {
			return true;
		}
		return false;
	}
	/**
	 *
	 */
	@Override
	public void processCreate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Transaction tx = hibSess.beginTransaction();
				Survey survey = getSurvey(ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID), hibSess);
				
				if (AuthUtil.isAllowedToEditThisSurvey(currentUser, survey, hibSess)) {
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_DEVIATIONCHECK)) {
						
						if (ParamUtil.checkAllParamsSet(req, PARAM_DECISIONQUESTIONID)) {
							
							// 1.) Create new GenericReportInfo (Entity)
							// 2.) Iterate through given questionIds
							// 2.1) Save QuestionId's in Info-List
							// 3.) Save "generic" in hibernate
							
							GenericReportInfo generic = new GenericReportInfo();
							if (survey != null) {
								generic.setSurveyId(survey.getId());
							} else {
								Logger.err("surveyId was not set.");
							}
							
							List<Integer> questionIds = new ArrayList<Integer>();
							String[] questionIdsAsString = req.getParameterValues(PARAM_DECISIONQUESTIONID);
							for (String element : questionIdsAsString) {
								element = element.replaceFirst("q", "");
								element = element.replaceFirst("s", "");
								questionIds.add(Integer.parseInt(element));
							}
							String reportName = req.getParameter(PARAM_REPORTNAME);
							generic.setName(reportName);
							generic.setQuestions(questionIds);
							generic.setRepInfoType(Type.DEVIATION);
							if (ParamUtil.checkAllParamsSet(req, PARAM_REPORTDESCRIPTION)){
								generic.setDescription(req.getParameter(PARAM_REPORTDESCRIPTION));
							}
							
							hibSess.save(generic);
						}
					} else if (ParamUtil.checkAllParamsSet(req, PARAM_PERFORMANCECHECK)) {
						if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTIONS)) {
							
							// 1.) Create new GenericReportInfo (Entity)
							// 2.) Iterate through given questionIds
							// 2.1) Save QuestionId's in Info-List
							// 3.) Save "generic" in hibernate
							GenericReportInfo generic = new GenericReportInfo();
							if (survey != null) {
								generic.setSurveyId(survey.getId());
							} else {
								System.out.println("SurveyID not set!!!");
							}
							
							List<Integer> questionIds = new ArrayList<Integer>();
							String[] questionIdsAsString = req.getParameterValues(PARAM_QUESTIONS);
							for (String element : questionIdsAsString) {
								element = element.replaceFirst("q", "");
								element = element.replaceFirst("s", "");
								questionIds.add(Integer.parseInt(element));
							}
							String reportName = req.getParameter(PARAM_REPORTNAME);
							generic.setName(reportName);
							generic.setQuestions(questionIds);
							generic.setRepInfoType(Type.PERFORMANCE);
							if (ParamUtil.checkAllParamsSet(req, PARAM_REPORTDESCRIPTION)) {
								generic.setDescription(req.getParameter(PARAM_REPORTDESCRIPTION));
							}
							
							hibSess.save(generic);
						}
					}
					tx.commit();
				} else {
					handlePermissionDenied(req, resp);
				}
			} catch (Exception e) {
				Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
			} finally {
				hibSess.close();
			}
			
		}
		resp.sendRedirect(getStandardRedirectLocation(req));
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @seescc.servlets.AbstractSccServlet#processRetrieve(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processDelete(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			if (ParamUtil.checkAllParamsSet(req, DELETEREPORT)) {
				int idToRemove = Integer.parseInt(req.getParameter(DELETEREPORT));
				// List <?> list =
				// AccessReportFromDB.getInstance().getCurrentReportListFromDB();
				Session hibReportListSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibReportListSess.beginTransaction();
					Criteria criteria = hibReportListSess.createCriteria(GenericReportInfo.class);
					
					List<?> list = criteria.list();
					
					for (Object o : list) {
						if (o instanceof GenericReportInfo) {
							GenericReportInfo genericInfo = (GenericReportInfo) o;
							if ((genericInfo != null) && (genericInfo.getId() == idToRemove)) {
								hibReportListSess.delete(genericInfo);
								break;
							}
						}
					}
					tx.commit();
				} finally {
					hibReportListSess.close();
				}
				
			}
		}
		resp.sendRedirect(getStandardRedirectLocation(req));
	}
	
	private Survey getSurvey(final int surveyID, final Session hibSess) {
		return (Survey) hibSess.get(Survey.class, surveyID);
	}
	
}
