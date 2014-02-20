/**
 * 
 */
package de.cinovo.surveyplatform.util;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ITokenable;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.Token;
import de.cinovo.surveyplatform.model.UserGroup;

/**
 * Copyright 2013 Cinovo AG<br><br>
 * @author ablehm
 *
 */
public class CleanupUtil implements Runnable {
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	private Set<Class<?>> classesToCleanUp = new HashSet<Class<?>>();
	
	
	public CleanupUtil() {
		classesToCleanUp.add(UserGroup.class);
		classesToCleanUp.add(SystemUser.class);
	}
	
	
	@Override
	public void run() {
		while (true) {
			try {
				// cleanup invalid (old) tokens, every 24 hours = 86400000 ms
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					for (Class<?> currentClass : classesToCleanUp) {
						deleteTokenFrom(currentClass, hibSess);
					}
				} catch (Exception e) {
					Logger.errUnexpected(e, null);
				} finally {
					hibSess.close();
				}
				Thread.sleep(86400000);
			} catch (InterruptedException e) {
				Logger.err("Cleanup Service (Util) threw Exception");
				Logger.err(e.getMessage());
			}
		}
		
	}
	
	/**
	 * @param currentClass
	 * @param hibSess
	 */
	private void deleteTokenFrom(final Class<?> currentClass, final Session hibSess) throws Exception {
		
		Transaction tx = hibSess.beginTransaction();
		Criteria tokenCrit = hibSess.createCriteria(currentClass);
		List<?> tokenList = tokenCrit.list();
		Long now = new Date().getTime();
		for (Object o : tokenList) {
			if (o instanceof ITokenable) {
				ITokenable obj = (ITokenable) o;
				// if token is not valid until "now", delete it
				if (obj.getToken() != null) {
					if (!obj.getToken().isValid(now)) {
						Token token = obj.getToken();
						obj.setToken(null);
						hibSess.save(obj);
						hibSess.delete(token);
					}
				}
			}
		}
		tx.commit();
		
	}
	
}
