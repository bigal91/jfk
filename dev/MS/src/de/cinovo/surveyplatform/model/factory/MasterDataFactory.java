/**
 * 
 */
package de.cinovo.surveyplatform.model.factory;

import java.util.HashSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.PaymentModel;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.UserRight;


/**
 * Copyright 2013 Cinovo AG<br><br>
 * @author ablehm
 *
 */
public class MasterDataFactory {
	
	private static MasterDataFactory instance = new MasterDataFactory();
	
	
	public static MasterDataFactory getInstance() {
		return instance;
	}
	
	/**
	 * @param organization
	 * @return
	 */
	public Client createClient(final String organization) {
		Client client = new Client();
		client.setOrganization(organization);
		return client;
	}
	
	/**
	 * Creates a Client and his standard groups, with the given organization name. <br>
	 * It also returns the client-Administrator Group of the Client, for a default user, who has been newly created in the system.
	 * 
	 * @param organization - the Organization name of the client
	 * @param accountType - the accountType of the Client
	 * @param hibSess - the current Hibernate Session to save the data in the database
	 * @return the Client-Administrator Group, which also contains the Client itself
	 */
	public UserGroup createClientAndStandardGroups(final String organization, final PaymentModel paymentModel, final Session hibSess) {
		
		Client client = MasterDataFactory.getInstance().createClient(organization);
		
		Transaction tx = hibSess.beginTransaction();
		
		hibSess.save(client);
		
		Criteria c = hibSess.createCriteria(UserRight.class);
		UserGroup caGroup = null;
		for (Object o : c.list()) {
			boolean isClientAdmin = false;
			UserRight right = (UserRight) o;
			UserGroup group = new UserGroup();
			if (right.getName().equals(UserRights.ADMINISTRATOR.id())) {
				// !DO NOT ADD ADMINISTRATORS GROUP!
				continue;
			} else if (right.getName().equals(UserRights.CLIENT_ADMINISTRATOR.id())) {
				group.setName("Client Administrators");
				isClientAdmin = true;
			} else if (right.getName().equals(UserRights.GROUP_ADMINISTRATOR.id())) {
				group.setName("Group Administrators");
			} else if (right.getName().equals(UserRights.DATA_RECORDER.id())) {
				group.setName("Data Recorder");
			} else if (right.getName().equals(UserRights.DATA_VIEWER.id())) {
				group.setName("Report Viewer");
			} else if (right.getName().equals(UserRights.SURVEY_MANAGER.id())) {
				group.setName("Survey Manager");
			} else {
				group.setName(right.getName());
			}
			group.setClient(client);
			group.setUserRights(new HashSet<UserRight>());
			group.getUserRights().add(right);
			hibSess.save(group);
			if (isClientAdmin) {
				caGroup = group;
			}
		}
		
		tx.commit();
		return caGroup;
	}
	
}
