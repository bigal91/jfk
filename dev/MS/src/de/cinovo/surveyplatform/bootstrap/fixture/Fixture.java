package de.cinovo.surveyplatform.bootstrap.fixture;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.PaymentModel;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.UserRight;
import de.cinovo.surveyplatform.model.UserStatus;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.Logger;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class Fixture {
	
	/**
	 *
	 */
	private static final String USERNAME_ADMIN = "admin";
	/**
	 *
	 */
	private static final String USERNAME_OFFLINE_USER = "offline_user";
	
	
	public static void create(final boolean clearDatabase) throws Exception {
		Session s = HibernateUtil.getSessionFactory().openSession();
		Transaction t = s.beginTransaction();
		boolean migrationSucceeded = false;
		
		syncUserRights(s);
		
		createInternalClientIfNeccessary(s);
		if (EnvironmentConfiguration.isTestEnvironment() && clearDatabase) {
			createTestEnvironment(s);
			
		} else {
			createAdminUserIfNeccessaryAndRestoreData(s, "admin");
			if (EnvironmentConfiguration.isOfflineMode()) {
				createOfflineUserIfNeccessaryAndRestoreData(s);
			}
			migrationSucceeded = DatabaseMigration.migrate(s);
			
		}
		
		t.commit();
		if (!migrationSucceeded) {
			throw new Exception("DataBaseMigration failed.");
		}
		s.close();
	}
	
	private static void createInternalClientIfNeccessary(final Session s) {
		Criteria criteria = s.createCriteria(Client.class);
		criteria.add(Restrictions.eq("internal", true));
		List<?> list = criteria.list();
		if (list.size() == 0) {
			Client c = new Client();
			c.setInternal(true);
			c.setOrganization("Internal");
			c.setPaymentModel(PaymentModel.NONE);
			c.setClientStatus(UserStatus.Active);
			s.save(c);
		}
	}
	
	private static void syncUserRights(final Session s) {
		
		Criteria criteria = s.createCriteria(UserRight.class);
		
		@SuppressWarnings("unchecked")
		List<UserRight> persistedUserRights = criteria.list();
		UserRights[] values = UserRights.values();
		for (UserRights userRight : values) {
			UserRight right = new UserRight(userRight.id());
			right.setInternal(userRight.internal());
			if (persistedUserRights.contains(right)) {
				// ok
			} else {
				System.out.println("Persisting UserRight: " + right.getName());
				s.save(right);
			}
		}
		Set<UserRight> toRemove = new HashSet<UserRight>();
		for (UserRight userRight : persistedUserRights) {
			try {
				UserRights.valueOf(userRight.getName().toUpperCase());
			} catch (IllegalArgumentException iae) {
				toRemove.add(userRight);
			}
		}
		
		for (UserRight doomedRight : toRemove) {
			criteria = s.createCriteria(UserGroup.class);
			List<?> list = criteria.list();
			for (Object o : list) {
				UserGroup g = (UserGroup) o;
				g.getUserRights().remove(doomedRight);
			}
			criteria = s.createCriteria(SystemUser.class);
			list = criteria.list();
			for (Object o : list) {
				SystemUser su = (SystemUser) o;
				su.getUserRights().remove(doomedRight);
			}
			System.out.println("Deleting UserRight: " + doomedRight.getName());
			s.delete(doomedRight);
		}
	}
	
	private static void createTestEnvironment(final Session s) {
		
		createAdminUserIfNeccessaryAndRestoreData(s, "test");
		
		Client testClient = new Client();
		testClient.setInternal(false);
		testClient.setOrganization("Test AG");
		testClient.setPaymentModel(PaymentModel.NONE);
		testClient.setClientStatus(UserStatus.Active);
		
		s.save(testClient);
		UserGroup surveyA = addGroup(s, "Survey A", testClient, null, null);
		UserGroup surveyB = addGroup(s, "Survey B", testClient, null, null);
		
		UserGroup groupClientAdmins = addGroup(s, "Client Admins", testClient, hashSetOf(s, EnumSet.of(UserRights.CLIENT_ADMINISTRATOR)), null);
		addUser(s, "clientAdmin", "test", groupClientAdmins);
		
		UserGroup groupSurveyManager = addGroup(s, "Survey Manager", testClient, hashSetOf(s, EnumSet.of(UserRights.SURVEY_MANAGER)), null);
		SystemUser sm1 = addUser(s, "surveyManager1", "test", groupSurveyManager);
		GroupManager.addMember(surveyA, sm1);
		
		SystemUser sm2 = addUser(s, "surveyManager2", "test", groupSurveyManager);
		GroupManager.addMember(surveyB, sm2);
		
		UserGroup groupGroupAdmins = addGroup(s, "Group Admins", testClient, hashSetOf(s, EnumSet.of(UserRights.GROUP_ADMINISTRATOR)), null);
		addUser(s, "groupAdmin", "test", groupGroupAdmins);
		
		UserGroup groupDataRecorder = addGroup(s, "Data Recorder", testClient, hashSetOf(s, EnumSet.of(UserRights.DATA_RECORDER)), null);
		addUser(s, "dataRecorder", "test", groupDataRecorder);
		
		UserGroup groupDataViewer = addGroup(s, "Data Viewer", testClient, hashSetOf(s, EnumSet.of(UserRights.DATA_VIEWER)), null);
		addUser(s, "dataViewer", "test", groupDataViewer);
		
	}
	
	private static SystemUser addUser(final Session s, final String userName, final String password, final UserGroup group) {
		SystemUser user = new SystemUser();
		user.setCreationDate(new Date(System.currentTimeMillis()));
		user.setUserName(userName);
		user.setPassword(AuthUtil.scramblePassword(password));
		user.setEmail("");
		GroupManager.addMember(group, user);
		s.save(user);
		return user;
	}
	
	private static HashSet<UserRight> hashSetOf(final Session s, final EnumSet<UserRights> userRightSet) {
		Criteria cUserRights = s.createCriteria(UserRight.class);
		
		@SuppressWarnings("unchecked")
		List<UserRight> userRights = cUserRights.list();
		
		HashSet<UserRight> set = new HashSet<UserRight>();
		for (UserRight right : userRights) {
			for (UserRights rightEnum : userRightSet) {
				if (rightEnum.id().equals(right.getName())) {
					set.add(right);
				}
			}
		}
		return set;
	}
	
	private static UserGroup addGroup(final Session s, final String groupName, final Client client, final Set<UserRight> rights, final UserGroup parentGroup) {
		UserGroup group = new UserGroup();
		group.setClient(client);
		group.setName(groupName);
		group.setUserRights(rights);
		group.setParentGroup(parentGroup);
		s.save(group);
		return group;
	}
	
	private static void createOfflineUserIfNeccessaryAndRestoreData(final Session s) {
		Logger.info("Maintaining admin group and admin user...");
		
		Criteria cUserRights = s.createCriteria(UserRight.class);
		
		@SuppressWarnings("unchecked")
		List<UserRight> userRights = cUserRights.list();
		
		// fix the offline users group
		UserGroup offlineGroup = getOrCreateGroup("Offline Users", s);
		
		if (offlineGroup.getUserRights() == null) {
			offlineGroup.setUserRights(new HashSet<UserRight>());
		}
		
		offlineGroup.getUserRights().clear();
		for (UserRight right : userRights) {
			if (UserRights.DATA_RECORDER.id().equals(right.getName())) {
				offlineGroup.getUserRights().add(right);
			}
		}
		
		// fix the admin user
		SystemUser offlineUser = (SystemUser) s.get(SystemUser.class, USERNAME_OFFLINE_USER);
		if (offlineUser == null) {
			offlineUser = new SystemUser();
			offlineUser.setCreationDate(new Date(System.currentTimeMillis()));
			offlineUser.setUserName(USERNAME_OFFLINE_USER);
			offlineUser.setPassword(AuthUtil.scramblePassword(UUID.randomUUID().toString()));
			offlineUser.setEmail("");
		}
		
		GroupManager.addMember(offlineGroup, offlineUser);
		
		if (offlineUser.getUserRights() == null) {
			offlineUser.setUserRights(new HashSet<UserRight>());
		}
		
		offlineUser.getUserRights().clear();
		for (UserRight right : userRights) {
			offlineUser.getUserRights().add(right);
		}
		
		if (!offlineUser.getUserStatus().equals(de.cinovo.surveyplatform.model.UserStatus.Active)) {
			offlineUser.setUserStatus(de.cinovo.surveyplatform.model.UserStatus.Active);
		}
		
		s.save(offlineUser);
		
	}
	
	private static void createAdminUserIfNeccessaryAndRestoreData(final Session s, final String adminPassword) {
		
		Logger.info("Maintaining admin group and admin user...");
		
		Criteria cUserRights = s.createCriteria(UserRight.class);
		
		@SuppressWarnings("unchecked")
		List<UserRight> userRights = cUserRights.list();
		
		// fix the administrators group
		UserGroup adminGroup = getOrCreateGroup("Administrators", s);
		
		if (adminGroup.getUserRights() == null) {
			adminGroup.setUserRights(new HashSet<UserRight>());
		}
		
		adminGroup.getUserRights().clear();
		for (UserRight right : userRights) {
			adminGroup.getUserRights().add(right);
		}
		
		// fix the admin user
		SystemUser adminUser = (SystemUser) s.get(SystemUser.class, USERNAME_ADMIN);
		if (adminUser == null) {
			adminUser = new SystemUser();
			adminUser.setCreationDate(new Date(System.currentTimeMillis()));
			adminUser.setUserName(USERNAME_ADMIN);
			if (EnvironmentConfiguration.isOfflineMode()) {
				adminUser.setPassword(AuthUtil.scramblePassword(UUID.randomUUID().toString()));
			} else {
				adminUser.setPassword(AuthUtil.scramblePassword(adminPassword));
			}
			adminUser.setEmail("");
		}
		
		GroupManager.addMember(adminGroup, adminUser);
		
		if (adminUser.getUserRights() == null) {
			adminUser.setUserRights(new HashSet<UserRight>());
		}
		
		adminUser.getUserRights().clear();
		for (UserRight right : userRights) {
			adminUser.getUserRights().add(right);
		}
		
		if (!adminUser.getUserStatus().equals(UserStatus.Active)) {
			adminUser.setUserStatus(UserStatus.Active);
		}
		
		s.save(adminUser);
		
	}
	
	private static UserGroup getOrCreateGroup(final String name, final Session s) {
		UserGroup adminGroup = null;
		try {
			Criteria cGroups = s.createCriteria(UserGroup.class);
			cGroups.add(Restrictions.eq("name", name));
			List<?> list = cGroups.list();
			if ((list == null) || (list.size() == 0)) {
				adminGroup = new UserGroup();
				adminGroup.setName(name);
				
				Criteria cClients = s.createCriteria(Client.class);
				cClients.add(Restrictions.eq("internal", true));
				Client client = (Client) cClients.uniqueResult();
				adminGroup.setClient(client);
				
			} else {
				adminGroup = (UserGroup) list.get(0);
			}
			s.save(adminGroup);
			
		} catch (HibernateException e) {
			System.out.println("Exc");
		}
		return adminGroup;
	}
	
	// private static void createDemoUser(final Session s) {
	// SystemUser demoUser = (SystemUser) s.get(SystemUser.class, "demo");
	// UserGroup demoGroup = getOrCreateGroup("Demo", s);
	// AuthUtil.setUserRight(UserRights.DEMO, demoGroup);
	// if (demoUser == null) {
	// Logger.log("Creating 'demo' user");
	// demoUser = new SystemUser();
	// demoUser.setCreationDate(new Date(System.currentTimeMillis()));
	// demoUser.setUserName("demo");
	// demoUser.setPassword(AuthUtil.scramblePassword("demo"));
	// demoUser.setEmail("");
	// }
	//
	// GroupManager.addMember(demoGroup, demoUser);
	//
	// demoUser.setUserStatus(UserStatus.Active);
	// s.save(demoUser);
	// }
}
