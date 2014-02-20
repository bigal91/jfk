/**
 *
 */
package de.cinovo.surveyplatform.model.reporting;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;

import de.cinovo.surveyplatform.hibernate.HibernateUtil;



/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author ablehm
 *
 */
public class AccessReportFromDB {
	
	private static AccessReportFromDB instance;
	
	
	public static AccessReportFromDB getInstance() {
		if (instance == null) {
			instance = new AccessReportFromDB();
		}
		return instance;
	}
	
	public List<?> getCurrentReportListFromDB() {
		Session hibReportListSess = HibernateUtil.getSessionFactory().openSession();
		Criteria criteria = hibReportListSess.createCriteria(GenericReportInfo.class);
		List<?> list = criteria.list();
		hibReportListSess.close();
		return list;
	}
	
	public GenericReportInfo getReportInfoById(final int reportId, Session session) {
		boolean haveMyOwnSession = false;
		if (session == null) {
			session = HibernateUtil.getSessionFactory().openSession();
			haveMyOwnSession = true;
		}
		try {
			Criteria criteria = session.createCriteria(GenericReportInfo.class);
			criteria.addOrder(Order.asc("name"));
			
			List<?> list = criteria.list();
			for (Object o : list) {
				if (o instanceof GenericReportInfo) {
					GenericReportInfo currentReportInfo = (GenericReportInfo) o;
					if (currentReportInfo.getId() == reportId) {
						return currentReportInfo;
					}
				}
			}
		} finally {
			if (haveMyOwnSession) {
				session.close();
			}
		}
		return new GenericReportInfo();
	}
	
	public List<Integer> getQuestionsFromReportInfo(final GenericReportInfo genRepInfo) {
		List<Integer> questions = genRepInfo.getQuestions();
		return questions;
	}
}
