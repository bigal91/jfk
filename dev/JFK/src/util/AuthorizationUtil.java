package util;

import javax.servlet.http.HttpServletRequest;

import constants.Constants;

import model.User;



public class AuthorizationUtil {
	
	/**
	 * Tests, if the User is in the Session. If there is a user registered in it, return the User  Object.
	 * 
	 * @param req HttpServletRequest object
	 * @return The SystemUser object, when user of the given Session is authenticated, null otherwise
	 */
	public static User checkAuthorization(final HttpServletRequest req) {
		if (req.getSession() != null) {
			boolean loggedIn = false;
			if (req.getSession().getAttribute(Constants.ATTR_AUTH_CHECKED) != null) {
				loggedIn = true;
			}
			if (loggedIn && (req.getSession().getAttribute(Constants.ATTR_AUTH_USER) != null)) {
				return (User) req.getSession().getAttribute(Constants.ATTR_AUTH_USER);
			}
		}
		return null;
	}
	
	
	
}
