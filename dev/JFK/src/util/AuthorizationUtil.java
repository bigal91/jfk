package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
			if (req.getSession().getAttribute(Constants.ATTR_AUTH_AUTHENTICATED) != null) {
				loggedIn = true;
			}
			if (loggedIn && (req.getSession().getAttribute(Constants.ATTR_AUTH_USER) != null)) {
				return (User) req.getSession().getAttribute(Constants.ATTR_AUTH_USER);
			}
		}
		return null;
	}
	
	/**
	 * Create a hashed representation of a given plaintext password string. The password string is salted with a prefix and suffix.
	 * 
	 * @param password Plaintext password
	 * @return hash of the password
	 */
	public static String scramblePassword(final String password) {
		return AuthorizationUtil.sha512(AuthorizationUtil.salt(password));
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
		return AuthorizationUtil.getHash(stringToHash, "MD5");
	}
	
	/**
	 * Create a SHA-512 hashed representation of a given string.
	 * 
	 * @param stringToHash String to hash
	 * @return SHA-512 Representation
	 */
	public static String sha512(final String stringToHash) {
		return AuthorizationUtil.getHash(stringToHash, "SHA-512");
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
			Logger.err("Ein unerwarteter Fehler ist aufgetreten");
		}
		return hexString.toString();
	}
	
}
