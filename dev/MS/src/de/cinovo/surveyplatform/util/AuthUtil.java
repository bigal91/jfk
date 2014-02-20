package de.cinovo.surveyplatform.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;

import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.IAuthRestrictable;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.UserRight;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class AuthUtil {
	
	/**
	 * Checks, if the user of the session given with the HttpServletRequest is authenticated. If so, the SystemUser object is returned.
	 * 
	 * @param req HttpServletRequest object
	 * @return The SystemUser object, when user of the given Session is authenticated, null otherwise
	 */
	public static SystemUser checkAuth(final HttpServletRequest req) {
		if (req.getSession() != null) {
			boolean loggedIn = false;
			if (req.getSession().getAttribute(Constants.ATTR_AUTH_AUTHENTICATED) != null) {
				loggedIn = true;
			}
			if (loggedIn && (req.getSession().getAttribute(Constants.ATTR_AUTH_USER) != null)) {
				return (SystemUser) req.getSession().getAttribute(Constants.ATTR_AUTH_USER);
			}
		}
		return null;
	}
	
	/**
	 * Checks, if the user is impersonating another user
	 * 
	 * @param req HttpServletRequest object
	 * @return true if impersonating, false otherwise
	 */
	public static boolean isImpersonating(final HttpServletRequest req) {
		if (req.getSession() != null) {
			if ((req.getSession().getAttribute(Constants.ATTR_IMPERSONATE_USER) != null) && req.getSession().getAttribute(Constants.ATTR_IMPERSONATE_USER).equals(Boolean.TRUE)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isAdmin(final SystemUser user) {
		return AuthUtil.hasRight(user, UserRights.ADMINISTRATOR);
	}
	
	/**
	 * Checks, if the user is in demo mode
	 * 
	 * @param req HttpServletRequest object
	 * @return true if the user is in demo mode, false otherwise
	 */
	public static boolean isInDemoMode(final HttpServletRequest req) {
		if (req.getSession() != null) {
			if ((req.getSession().getAttribute(Constants.ATTR_DEMO_MODE) != null) && req.getSession().getAttribute(Constants.ATTR_DEMO_MODE).equals(Boolean.TRUE)) {
				return true;
			}
		}
		return false;
	}
	
	// public static Logger getUserLogger(final HttpServletRequest req) {
	// SystemUser user = AuthUtil.checkAuth(req);
	// if (user == null) {
	// return null;
	// }
	// Logger logger = Logger.create(user.getActualUserName());
	// return logger;
	// }
	//
	// public static Logger getUserLogger(final SystemUser user) {
	// Logger logger = Logger.create(user.getActualUserName());
	// return logger;
	// }
	
	/**
	 * Convenient method to test, if a given user has a given right
	 * 
	 * @param user SystemUser object to test
	 * @param ruserRightName the name of the right
	 * @return true, if user is not null and SystemUser has the given right in his UserRights list, false otherwise
	 */
	public static boolean hasRight(final IAuthRestrictable ar, final UserRights... userRights) {
		if ((ar == null) || (ar.getUserRights() == null)) {
			return false;
		}
		
		UserRight userRightToCheck = new UserRight(UserRights.ADMINISTRATOR.id());
		if (ar.getUserRights().contains(userRightToCheck)) {
			return true;
		}
		
		for (UserRights userRight : userRights) {
			userRightToCheck = new UserRight(userRight.id());
			
			if (ar.getUserRights().contains(userRightToCheck)) {
				return true;
			}
			
			// see if any group of the user has the right
			if (ar instanceof SystemUser) {
				for (UserGroup group : ((SystemUser) ar).getUserGroups()) {
					UserGroup currentGroup = group;
					while (currentGroup != null) {
						Set<UserRight> groupRights = currentGroup.getUserRights();
						if ((userRights != null) && groupRights.contains(userRightToCheck)) {
							return true;
						}
						currentGroup = currentGroup.getParentGroup();
					}
				}
			} else if (ar instanceof UserGroup) {
				UserGroup currentGroup = (UserGroup) ar;
				while (currentGroup != null) {
					Set<UserRight> groupRights = currentGroup.getUserRights();
					if ((userRights != null) && groupRights.contains(userRightToCheck)) {
						return true;
					}
					currentGroup = currentGroup.getParentGroup();
				}
			}
		}
		return false;
	}
	
	public static UserRights[] getEffectiveRights(final IAuthRestrictable ar) {
		if ((ar == null) || (ar.getUserRights() == null)) {
			return null;
		}
		
		if (ar.getUserRights().contains(UserRights.ADMINISTRATOR)) {
			return UserRights.values();
		}
		
		List<UserRights> effectiveRights = new ArrayList<UserRights>();
		
		for (UserRights userRight : UserRights.values()) {
			if (AuthUtil.hasRight(ar, userRight)) {
				effectiveRights.add(userRight);
			}
		}
		return effectiveRights.toArray(new UserRights[effectiveRights.size()]);
	}
	
	public static void setUserRight(final UserRights userRight, final IAuthRestrictable ar) {
		
		Session hibSess = HibernateUtil.getSessionFactory().getCurrentSession();
		
		Set<UserRight> userRights = ar.getUserRights();
		if (userRights == null) {
			userRights = new HashSet<UserRight>();
		}
		UserRight right = (UserRight) hibSess.load(UserRight.class, userRight.name());
		if (right != null) {
			userRights.add(right);
		}
		hibSess.save(ar);
		hibSess.close();
	}
	
	public static void removeUserRight(final String name, final IAuthRestrictable ar) {
		Session hibSess = HibernateUtil.getSessionFactory().getCurrentSession();
		
		Set<UserRight> userRights = ar.getUserRights();
		if (userRights != null) {
			userRights.remove(new UserRight(name));
		}
		hibSess.save(ar);
		hibSess.close();
	}
	
	/**
	 * Create a hashed representation of a given plaintext password string. The password string is salted with a prefix and suffix.
	 * 
	 * @param password Plaintext password
	 * @return hash of the password
	 */
	public static String scramblePassword(final String password) {
		return AuthUtil.sha512(AuthUtil.salt(password));
	}
	
	public static String salt(final String password) {
		return "UPSALT::" + password + "::TLASPU";
	}
	
	/**
	 * Create a MD5 hashed representation of a given string.
	 * 
	 * @param stringToHash String to hash
	 * @return MD5 Representation
	 */
	public static String md5(final String stringToHash) {
		return AuthUtil.getHash(stringToHash, "MD5");
	}
	
	/**
	 * Create a SHA-512 hashed representation of a given string.
	 * 
	 * @param stringToHash String to hash
	 * @return SHA-512 Representation
	 */
	public static String sha512(final String stringToHash) {
		return AuthUtil.getHash(stringToHash, "SHA-512");
	}
	
	public static String getHash(final String stringToHash, final String algorithm) {
		StringBuffer hexString = new StringBuffer();
		try {
			/* Berechnung */
			MessageDigest md5 = MessageDigest.getInstance(algorithm);
			md5.reset();
			md5.update(stringToHash.getBytes());
			byte[] result = md5.digest();
			
			/* Ausgabe */
			for (byte element : result) {
				if ((element <= 15) && (element >= 0)) {
					hexString.append("0");
				}
				hexString.append(Integer.toHexString(0xFF & element));
			}
		} catch (NoSuchAlgorithmException e) {
			Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
		}
		return hexString.toString();
	}
	
	/**
	 * @param user -
	 * @return true if the user is allowed to list surveys
	 */
	public static boolean isAllowedToListSurveys(final SystemUser user) {
		return AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR, UserRights.GROUP_ADMINISTRATOR, UserRights.DATA_VIEWER, UserRights.SURVEY_MANAGER, UserRights.DATA_RECORDER);
	}
	
	/**
	 * @param user -
	 * @return true if the user is allowed to create reports
	 */
	public static boolean isAllowedToCreateReports(final SystemUser user) {
		return AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR, UserRights.GROUP_ADMINISTRATOR, UserRights.SURVEY_MANAGER);
	}
	
	/**
	 * @param user -
	 * @return true if the user is allowed to edit surveys
	 */
	public static boolean isAllowedToEditSurveys(final SystemUser user) {
		return AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR, UserRights.GROUP_ADMINISTRATOR, UserRights.SURVEY_MANAGER);
	}
	
	/**
	 * @param user -
	 * @return true if the user is allowed to edit systemusers
	 */
	public static boolean isAllowedToEditSystemUser(final SystemUser user) {
		return AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR, UserRights.GROUP_ADMINISTRATOR);
	}
	
	/**
	 * @param user -
	 * @return true if the user is allowed to manage the participants
	 */
	public static boolean isAllowedToManageParticipants(final SystemUser user) {
		return AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR, UserRights.GROUP_ADMINISTRATOR, UserRights.DATA_RECORDER, UserRights.SURVEY_MANAGER);
	}
	
	/**
	 * @param user -
	 * @return true if the user is allowed to view reports
	 */
	public static boolean isAllowedToViewReports(final SystemUser user) {
		return AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR, UserRights.GROUP_ADMINISTRATOR, UserRights.SURVEY_MANAGER, UserRights.DATA_VIEWER);
	}
	
	/**
	 * @param user
	 * @return true if the user is allowed to edit questionnaire data after the questionnaire is submitted
	 */
	public static boolean isAllowedToEditQuestionnaireData(final SystemUser user) {
		return AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR);
	}
	
	/**
	 * @param user
	 * @return true if the user is allowed to edit questionnaire data after the questionnaire is submitted
	 */
	public static boolean isAllowedToEditQuestionnaireData(final SystemUser user, final Survey survey, final Session hibSess) {
		if (isAdmin(user)) {
			return true;
		}
		if (isAllowedToEditQuestionnaireData(user)) {
			Collection<UserGroup> clientGroups = GroupManager.getClientGroups(hibSess, user.getUserGroups().iterator().next().getClient());
			if (clientGroups.contains(survey.getOwner())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param user -
	 * @param survey -
	 * @return true if the user is allowed to edit the given survey
	 */
	public static boolean isAllowedToEditThisSurvey(final SystemUser user, final Survey survey, final Session hibSess) {
		if (AuthUtil.isAdmin(user)) {
			return true;
		}
		
		if (AuthUtil.hasRight(user, UserRights.SURVEY_MANAGER)) {
			Collection<UserGroup> visibleGroups = GroupManager.getVisibleGroups(hibSess, user, user);
			if (visibleGroups.contains(survey.getOwner())) {
				return true;
			}
		}
		
		if (AuthUtil.hasRight(user, UserRights.GROUP_ADMINISTRATOR)) {
			Collection<UserGroup> childGroups = GroupManager.getChildGroups(hibSess, user);
			if (childGroups.contains(survey.getOwner())) {
				return true;
			}
		}
		
		if (AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR)) {
			Collection<UserGroup> clientGroups = GroupManager.getClientGroups(hibSess, user.getUserGroups().iterator().next().getClient());
			if (clientGroups.contains(survey.getOwner())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @param user -
	 * @param client -
	 * @return true if the user is allowed to create a group for the given client
	 */
	public static boolean isAllowedToEditGroup(final SystemUser user, final Client client, final long parentGroupId, final Session hibSess) {
		if (AuthUtil.isAdmin(user)) {
			return true;
		}
		if (client.getId() != user.getUserGroups().iterator().next().getClient().getId()) {
			return false;
		}
		if (AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR)) {
			return true;
		}
		
		if (AuthUtil.hasRight(user, UserRights.GROUP_ADMINISTRATOR)) {
			Collection<UserGroup> childGroups = GroupManager.getChildGroups(hibSess, user);
			UserGroup testGroup = new UserGroup();
			testGroup.setId(parentGroupId);
			if (childGroups.contains(parentGroupId)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @param user -
	 * @param survey -
	 * @param hibSess -
	 * @return true if the user is allowed to manage participants of the given survey
	 */
	public static boolean isAllowedToManageParticipants(final SystemUser user, final Survey survey, final Session hibSess) {
		if (AuthUtil.isAdmin(user)) {
			return true;
		}
		
		if (!AuthUtil.isAllowedToManageParticipants(user)) {
			return false;
		}
		
		Client usersClient = user.getUserGroups().iterator().next().getClient();
		Client surveysClient = survey.getOwner().getClient();
		
		if (usersClient.getId() != surveysClient.getId()) {
			return false;
		}
		
		if (AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR)) {
			return true;
		}
		
		if (GroupManager.getVisibleGroups(hibSess, user, user).contains(survey.getOwner())) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param user
	 * @param survey
	 * @return true if the user is allowed to see reports of the given survey
	 */
	public static boolean isAllowedToViewReports(final SystemUser user, final Survey survey, final Session hibSess) {
		if (AuthUtil.isAdmin(user)) {
			return true;
		}
		
		if (!AuthUtil.isAllowedToViewReports(user)) {
			return false;
		}
		Client usersClient = user.getUserGroups().iterator().next().getClient();
		Client surveysClient = survey.getOwner().getClient();
		
		if (usersClient.getId() != surveysClient.getId()) {
			return false;
		}
		
		if (AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR)) {
			return true;
		}
		
		if (GroupManager.getVisibleGroups(hibSess, user, user).contains(survey.getOwner())) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param survey -
	 * @param user -
	 * @return true, if the user is allowed to open the preview of the given survey
	 */
	public static boolean isAllowedToOpenPreview(final Survey survey, final SystemUser user, final Session hibSess) {
		return AuthUtil.isAllowedToEditThisSurvey(user, survey, hibSess);
	}
	
}
