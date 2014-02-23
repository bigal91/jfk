package servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.User;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import constants.Constants;
import constants.EnvConfiguration;
import util.AuthorizationUtil;
import util.HibernateUtil;
import util.Logger;
import util.ParamUtil;
import util.SessionManager;



public class LoginServlet  extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1456475L;
	
	private static String USERNAME = "uname";
	private static String PASSWORD = "pword";

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (ParamUtil.checkAllParamsSet(req, LoginServlet.USERNAME, LoginServlet.PASSWORD)){
			String userName = ParamUtil.getSafeParam(req, LoginServlet.USERNAME);
			String passWord = ParamUtil.getSafeParam(req, LoginServlet.PASSWORD);
			Session s = HibernateUtil.getSessionFactory().openSession();
			Transaction tx = s.beginTransaction();
			boolean loginSucceded = false;
			try{
				Criteria criteria = s.createCriteria(User.class);
				criteria.add(Restrictions.eq("userName", userName.toLowerCase()));
				criteria.add(Restrictions.eq("userPassword", AuthorizationUtil.scramblePassword(passWord)));
				User loggedUser = (User) criteria.uniqueResult();
				if (loggedUser != null){
					Logger.log("User " + loggedUser.getUserName() + " hat sich eingeloggt.");
					req.getSession().setAttribute(Constants.ATTR_AUTH_AUTHENTICATED, true);
					req.getSession().setAttribute(Constants.ATTR_AUTH_USER, loggedUser);
					SessionManager.getInstance().register(loggedUser.getUserName(), req.getSession());
					loginSucceded = true;
				}
			} catch (Exception e){
				Logger.err("Es ist etwas beim Login schiefgegangen.");
			} finally {
				s.close();

			}
			if (loginSucceded){
				resp.sendRedirect("/main?page=start");
				//resp.getWriter().write("Login succeded");
			} else {
				resp.sendRedirect("/main?page=login");
			}
		} else {
			Logger.err("Es ist etwas bei der Parameterübergabe im Login schiefgegangen.");
		}
	}
}
