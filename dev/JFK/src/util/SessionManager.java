package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

public class SessionManager {
private Map<String, List<HttpSession>> sessions = new HashMap<String, List<HttpSession>>();
	
	private static SessionManager instance = new SessionManager();
	
	
	public static SessionManager getInstance() {
		return SessionManager.instance;
	}
	
	/**
	 * Registers a session in the SessionManager
	 * 
	 * @param userName
	 * @param session
	 */
	public synchronized void register(final String userName, final HttpSession session) {
		this.cleanUp();
		List<HttpSession> list = this.sessions.get(userName);
		if (list == null) {
			list = new ArrayList<HttpSession>();
			this.sessions.put(userName, list);
		}
		list.add(session);
	}
	
	/**
	 * 
	 * @param userName
	 * @return null if user has no session. Or, if the the user has a session it
	 *         will return the first valid session found in the session list.
	 */
	public HttpSession getSession(final String userName) {
		List<HttpSession> httpSessionList = this.sessions.get(userName);
		if ((httpSessionList == null) || (httpSessionList.size() == 0)) {
			return null;
		}
		for (HttpSession session : httpSessionList) {
			if (this.isSessionValid(session)) {
				return session;
			}
		}
		return null;
	}
	
	public String getUserName(final String sessionId) {
		for (Entry<String, List<HttpSession>> entry : this.sessions.entrySet()) {
			for (HttpSession session : entry.getValue()) {
				if (session.getId().equals(sessionId)) {
					return entry.getKey();
				}
			}
		}
		return "";
	}
	
	/**
	 * 
	 * @param userName
	 * @return null if user has no session. Or, if the the user has a session it
	 *         will return a list of all registered sessions (this list may
	 *         include invalid sessions)
	 */
	public List<HttpSession> getSessions(final String userName) {
		List<HttpSession> httpSessionList = this.sessions.get(userName);
		if ((httpSessionList == null) || (httpSessionList.size() == 0)) {
			return null;
		}
		return httpSessionList;
	}
	
	/**
	 * Clean the SessionManagers list from invalid sessions
	 */
	public synchronized void cleanUp() {
		List<String> usersToRemove = new ArrayList<String>();
		for (Entry<String, List<HttpSession>> entry : this.sessions.entrySet()) {
			List<HttpSession> list = entry.getValue();
			List<HttpSession> sessionsToRemove = new ArrayList<HttpSession>();
			for (HttpSession session : list) {
				if (!this.isSessionValid(session)) {
					sessionsToRemove.add(session);
				}
			}
			for (HttpSession session : sessionsToRemove) {
				list.remove(session);
			}
			if (list.size() == 0) {
				usersToRemove.add(entry.getKey());
			}
		}
		for (String userName : usersToRemove) {
			this.sessions.remove(userName);
		}
	}
	
	public boolean isSessionValid(final HttpSession session) {
		try {
			session.getLastAccessedTime();
			return true;
		} catch (IllegalStateException ise) {
			return false;
		}
	}
	
}
