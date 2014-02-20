package de.cinovo.surveyplatform.hibernate;

import java.io.File;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import de.cinovo.surveyplatform.constants.Paths;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class HibernateUtil {
	
	private static SessionFactory SESSIONFACTORY = HibernateUtil.buildSessionFactory();
	
	
	private static SessionFactory buildSessionFactory() {
		try {
			
			Configuration configuration = new Configuration();
			configuration.configure(new File(Paths.CONFIG + "/hibernate.cfg.xml"));
			ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
			SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
			
			return sessionFactory;
			
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			System.out.print("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}
	
	public static SessionFactory getSessionFactory() {
		return HibernateUtil.SESSIONFACTORY;
	}
	
	public static Class<?> getRealClass(final Object obj) {
		Class<?> clazz;
		if (obj.getClass().getName().indexOf("$$_javassist") >= 0) {
			clazz = obj.getClass().getSuperclass();
		} else {
			clazz = obj.getClass();
		}
		return clazz;
	}
	
	public static void destroySessionFactory() {
		HibernateUtil.SESSIONFACTORY.close();
		HibernateUtil.SESSIONFACTORY = HibernateUtil.buildSessionFactory();
	}
}
