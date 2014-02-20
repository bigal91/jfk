/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@Entity
public class UserGroup implements IAuthRestrictable, Comparable<UserGroup>, Cloneable, ITokenable {
	
	private long id;
	
	private String name;
	
	private Set<SystemUser> members;
	
	private Set<UserRight> userRights;
	
	private Client client;
	
	private UserGroup parentGroup;
	
	private Set<UserGroup> childGroups;
	
	private Token token;
	
	
	/**
	 * @return -
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return this.id;
	}
	
	/**
	 * @return -
	 */
	@XmlID
	@Transient
	public String getXMLID() {
		return this.getClass().getSimpleName() + this.getId();
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}
	
	/**
	 * @return the users
	 */
	@ManyToMany(mappedBy = "userGroups", fetch = FetchType.LAZY)
	public Set<SystemUser> getMembers() {
		return this.members;
	}
	
	/**
	 * @param members the users to set
	 */
	public void setMembers(final Set<SystemUser> members) {
		this.members = members;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(final long id) {
		this.id = id;
	}
	
	/**
	 * @return the userRights
	 */
	@Override
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "group_rights", joinColumns = {@JoinColumn(name = "usergroup_id")}, inverseJoinColumns = {@JoinColumn(name = "userright_id")})
	public Set<UserRight> getUserRights() {
		return this.userRights;
	}
	
	/**
	 * @param userRights the userRights to set
	 */
	@Override
	public void setUserRights(final Set<UserRight> userRights) {
		this.userRights = userRights;
	}
	
	/**
	 * @param client the client to set
	 */
	public void setClient(final Client client) {
		this.client = client;
	}
	
	/**
	 * @return the client
	 */
	@ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "client_id", nullable = false)
	@XmlIDREF
	public Client getClient() {
		return this.client;
	}
	
	/**
	 * @return the parentGroup
	 */
	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "parent_group", nullable = true)
	@XmlIDREF
	public UserGroup getParentGroup() {
		return this.parentGroup;
	}
	
	/**
	 * @param parentGroup the parentGroup to set
	 */
	public void setParentGroup(final UserGroup parentGroup) {
		this.parentGroup = parentGroup;
	}
	
	/**
	 * @return the childGroups
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parentGroup", fetch = FetchType.LAZY)
	public Set<UserGroup> getChildGroups() {
		return this.childGroups;
	}
	
	/**
	 * @param childGroups the childGroups to set
	 */
	public void setChildGroups(final Set<UserGroup> childGroups) {
		this.childGroups = childGroups;
	}
	
	@Override
	@OneToOne(cascade = CascadeType.ALL, optional = true)
	public Token getToken() {
		return this.token;
	}
	
	@Override
	public void setToken(final Token token) {
		this.token = token;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = (this.id + this.name).hashCode();
		// System.out.println(this.getName() + "(" + id + "): " + hashCode);
		return hashCode;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		return this.id == ((UserGroup) obj).getId();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final UserGroup o) {
		// System.out.println(this.getName() + " => " + o.getName());
		int nameCompareResult = this.getName().compareTo(o.getName());
		if (nameCompareResult != 0) {
			// System.out.println("result: " + nameCompareResult);
			return nameCompareResult;
		}
		// System.out.println(getId() + " => " + o.getId());
		
		nameCompareResult = this.getId() == o.getId() ? 0 : this.getId() < o.getId() ? -1 : 1;
		// System.out.println("result: " + nameCompareResult);
		
		return nameCompareResult;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.name + " (" + this.id + ")";
	}
	
	@Override
	public UserGroup clone() {
		UserGroup clone = null;
		try {
			clone = (UserGroup) super.clone();
			clone.setClient(this.client.clone());
			clone.setMembers(this.members);
			clone.setParentGroup(null);
			clone.setId(0);
			if (this.childGroups != null) {
				Set<UserGroup> newUserGroups = new HashSet<UserGroup>();
				for (UserGroup ug : this.childGroups) {
					newUserGroups.add(ug.clone());
				}
				clone.setChildGroups(newUserGroups);
			}
			
			if (this.userRights != null) {
				Set<UserRight> newUserRights = new HashSet<UserRight>();
				for (UserRight ur : this.userRights) {
					newUserRights.add(ur.clone());
				}
				clone.setUserRights(newUserRights);
			}
			
		} catch (CloneNotSupportedException e) {
			Logger.errUnexpected(e, null);
		}
		
		return clone;
	}
	
	/**
	 * @return -
	 */
	public UserGroup cloneWithId() {
		UserGroup clone = null;
		try {
			clone = (UserGroup) super.clone();
			clone.setId(this.id);
			clone.setClient(this.client.cloneWithId());
			clone.setMembers(this.members);
			clone.setParentGroup(null);
			if (this.childGroups != null) {
				Set<UserGroup> newUserGroups = new HashSet<UserGroup>();
				for (UserGroup ug : this.childGroups) {
					newUserGroups.add(ug.cloneWithId());
				}
				clone.setChildGroups(newUserGroups);
			}
			
			if (this.userRights != null) {
				Set<UserRight> newUserRights = new HashSet<UserRight>();
				for (UserRight ur : this.userRights) {
					newUserRights.add(ur.clone());
				}
				clone.setUserRights(newUserRights);
			}
			
		} catch (CloneNotSupportedException e) {
			Logger.errUnexpected(e, null);
		}
		
		return clone;
	}
	
	
	
}
