/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.hibernate.HibernateUtil;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * Put all system settings in here
 * 
 * @author yschubert
 * 
 */
@Entity
public class SystemSettings {
	
	@Id
	@Column(nullable = true)
	private Integer dbVersion = null;
	
	
	/**
	 * @return the dbVersion
	 */
	public Integer getDbVersion() {
		return this.dbVersion;
	}
	
	/**
	 * @param dbVersion the dbVersion to set
	 */
	public void setDbVersion(final Integer dbVersion) {
		this.dbVersion = dbVersion;
	}
	
	/**
	 * @return the systemsettings
	 */
	public static SystemSettings load() {
		Session s = HibernateUtil.getSessionFactory().openSession();
		Transaction t = s.beginTransaction();
		Criteria c = s.createCriteria(SystemSettings.class);
		c.setMaxResults(1);
		List<?> list = c.list();
		SystemSettings settings = null;
		if (list.isEmpty()) {
			settings = new SystemSettings();
			settings.setDbVersion(0);
			s.save(settings);
		} else {
			settings = (SystemSettings) list.iterator().next();
		}
		t.commit();
		return settings;
	}
	
}
