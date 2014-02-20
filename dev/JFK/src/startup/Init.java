package startup;

import org.hibernate.Session;
import org.hibernate.Transaction;

import util.HibernateUtil;

public class Init {
	public static void initialize(){
		Session s = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = s.beginTransaction();
		
		// create admin user
		User user = new User();
	}
}
