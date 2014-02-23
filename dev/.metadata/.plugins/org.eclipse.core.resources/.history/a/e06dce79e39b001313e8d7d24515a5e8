package util;

import java.io.File;
import java.util.Set;

import javax.persistence.Entity;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.reflections.Reflections;

import resources.ResourcePaths;



public class HibernateUtil {
	
	private static final SessionFactory SESSIONFACTORY = HibernateUtil.buildSessionFactory();
	
	private static SessionFactory buildSessionFactory() {
		try {
			
			Configuration configuration = new Configuration();
			configuration.configure(new File(ResourcePaths.CONFIG + "/hibernate.cfg.xml"));
			
			Reflections reflections = new Reflections("");
			
			Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Entity.class);
			
			for (Class<?> clazz : annotated) {
				configuration.addAnnotatedClass(clazz);
			}
			
			ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
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
	
}
