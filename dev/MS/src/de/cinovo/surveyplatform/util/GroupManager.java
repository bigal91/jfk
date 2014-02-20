/**
 *
 */
package de.cinovo.surveyplatform.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.PaymentModel;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class GroupManager {
	
	private GroupManager() {
		// utililty class
	}
	
	/**
	 * @param group -
	 * @param systemUser -
	 */
	public static void addMember(final UserGroup group, final SystemUser systemUser) {
		Set<SystemUser> members = group.getMembers();
		if (members == null) {
			members = new HashSet<SystemUser>();
			group.setMembers(members);
		}
		members.add(systemUser);
		Set<UserGroup> userGroups = systemUser.getUserGroups();
		if (userGroups == null) {
			userGroups = new HashSet<UserGroup>();
			systemUser.setUserGroups(userGroups);
		}
		userGroups.add(group);
	}
	
	/**
	 * @param group -
	 * @param systemUser -
	 */
	public static void removeMember(final UserGroup group, final SystemUser systemUser) {
		final Set<SystemUser> members = group.getMembers();
		if (members != null) {
			members.remove(systemUser);
		}
		final Set<UserGroup> userGroups = systemUser.getUserGroups();
		if (userGroups != null) {
			userGroups.remove(group);
		}
	}
	
	/**
	 * @param user -
	 * @param survey -
	 * @return -
	 */
	public static boolean isGroupOwner(final SystemUser user, final Survey survey) {
		for (UserGroup group : user.getUserGroups()) {
			if (group.equals(survey.getOwner())) {
				return true;
			}
		}
		return false;
	}
	
	// public static boolean isVisibleGroup(final Session hibSess, final
	// SystemUser user) {
	// for(UserGroup group:getChildGroups(hibSess, user)) {
	// if ()
	// }
	// }
	
	/**
	 * @param user -
	 * @return -
	 */
	public static PaymentModel getPaymentModel(final SystemUser user) {
		PaymentModel maxModel = PaymentModel.Trial;
		for (UserGroup group : user.getUserGroups()) {
			PaymentModel paymentModel = group.getClient().getPaymentModel();
			if (paymentModel.ordinal() > maxModel.ordinal()) {
				maxModel = paymentModel;
			}
		}
		return maxModel;
	}
	
	/**
	 * @param hibSess -
	 * @param client -
	 * @return -
	 */
	public static Collection<UserGroup> getClientGroups(final Session hibSess, final Client client) {
		Criteria cAllGroups = hibSess.createCriteria(UserGroup.class);
		cAllGroups.add(Restrictions.eq("client", client));
		cAllGroups.addOrder(Order.asc("name"));
		Collection<UserGroup> allGroups = new LinkedHashSet<UserGroup>(cAllGroups.list());
		return allGroups;
	}
	
	/**
	 * @param hibSess -
	 * @param user -
	 * @param currentUser -
	 * @return -
	 */
	public static Collection<UserGroup> getVisibleGroups(final Session hibSess, final SystemUser user, final SystemUser currentUser) {
		Collection<UserGroup> groups = GroupManager.getParentGroups(hibSess, user);
		if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
			groups.addAll(GroupManager.getClientGroups(hibSess, getClient(user)));
		} else if (AuthUtil.hasRight(currentUser, UserRights.CLIENT_ADMINISTRATOR)) {
			groups.addAll(GroupManager.getClientGroups(hibSess, getClient(currentUser)));
		} else {
			groups.addAll(GroupManager.getChildGroups(hibSess, user));
		}
		return groups;
	}
	
	/**
	 * @param hibSess -
	 * @param client -
	 * @param user -
	 * @return -
	 */
	public static Collection<UserGroup> getVisibleGroups(final Session hibSess, final Client client, final SystemUser user) {
		Collection<UserGroup> groups = null;
		if (AuthUtil.hasRight(user, UserRights.ADMINISTRATOR)) {
			groups = GroupManager.getClientGroups(hibSess, client);
		} else {
			if (client.getId() == getClient(user).getId()) {
				if (AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR)) {
					groups = GroupManager.getClientGroups(hibSess, client);
				} else {
					groups = GroupManager.getParentGroups(hibSess, user);
					groups.addAll(GroupManager.getChildGroups(hibSess, user));
				}
			}
		}
		if (groups == null) {
			return new ArrayList<UserGroup>();
		}
		return groups;
	}
	
	/**
	 * @param hibSess -
	 * @param user -
	 * @return -
	 */
	public static Collection<UserGroup> getChildGroups(final Session hibSess, final SystemUser user) {
		Collection<UserGroup> allGroups = new LinkedHashSet<UserGroup>();
		for (UserGroup child : user.getUserGroups()) {
			allGroups.add(child);
			GroupManager.getChildGroups(hibSess, child.getId(), allGroups);
		}
		return allGroups;
	}
	
	/**
	 * @param hibSess -
	 * @param user -
	 * @return -
	 */
	public static Collection<UserGroup> getParentGroups(final Session hibSess, final SystemUser user) {
		Collection<UserGroup> allGroups = new LinkedHashSet<UserGroup>();
		for (UserGroup group : user.getUserGroups()) {
			group = (UserGroup) hibSess.load(UserGroup.class, group.getId());
			allGroups.add(group);
			GroupManager.getParentGroups(group, allGroups);
		}
		return allGroups;
	}
	
	/**
	 * @param user the user
	 * @return -
	 */
	public static Client getClient(final SystemUser user) {
		return user.getUserGroups().iterator().next().getClient();
	}
	
	private static void getChildGroups(final Session hibSess, final long groupId, final Collection<UserGroup> allGroups) {
		UserGroup group = (UserGroup) hibSess.load(UserGroup.class, groupId);
		for (UserGroup child : group.getChildGroups()) {
			allGroups.add(child);
			GroupManager.getChildGroups(hibSess, child.getId(), allGroups);
		}
	}
	
	private static void getParentGroups(final UserGroup group, final Collection<UserGroup> allGroups) {
		UserGroup parentGroup = group.getParentGroup();
		if (parentGroup != null) {
			allGroups.add(parentGroup);
			GroupManager.getParentGroups(parentGroup, allGroups);
		}
	}
	
}
