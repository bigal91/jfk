package de.cinovo.surveyplatform.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.TimeUtil;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
@Entity
public class SystemUser implements IAuthRestrictable, Cloneable, ITokenable {
	
	
	private String userName;
	
	private String alias;
	
	private String password;
	
	private String email;
	
	private Date creationDate;
	
	private Date lastLogin;
	
	private String title;
	
	private String firstName;
	
	private String lastName;
	
	private String address;
	
	private String phone;
	
	private UserStatus userStatus = UserStatus.Disabled;
	
	private String timeZoneID = "UTC";
	
	private String timeFormat = TimeUtil.DATEFORMAT_EN;
	
	private Set<UserGroup> userGroups;
	
	private Set<UserRight> userRights;
	
	private Token token;
	
	/**
	 * @return the userName
	 */
	@Id
	@XmlID
	public String getUserName() {
		return this.userName;
	}
	
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(final String userName) {
		this.userName = userName;
	}
	
	@Transient
	@XmlTransient
	public String getActualUserName() {
		if ((this.alias == null) || "".equals(this.alias)) {
			return this.userName;
		}
		return this.alias;
	}
	
	/**
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}
	
	/**
	 * @param password the password to set
	 */
	public void setPassword(final String password) {
		this.password = password;
	}
	
	/**
	 * @return the email
	 */
	public String getEmail() {
		return this.email;
	}
	
	/**
	 * @param email the email to set
	 */
	public void setEmail(final String email) {
		this.email = email;
	}
	
	/**
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return this.creationDate;
	}
	
	/**
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(final Date creationDate) {
		this.creationDate = creationDate;
	}
	
	/**
	 * @return the userStatus
	 */
	public UserStatus getUserStatus() {
		return this.userStatus;
	}
	
	/**
	 * @param userStatus the userStatus to set
	 */
	public void setUserStatus(final UserStatus userStatus) {
		this.userStatus = userStatus;
	}
	
	@Override
	public String toString() {
		return this.userName;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(final String title) {
		this.title = title;
	}
	
	@Column(columnDefinition = "text")
	public String getFirstName() {
		return this.firstName;
	}
	
	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}
	
	@Column(columnDefinition = "text")
	public String getLastName() {
		return this.lastName;
	}
	
	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}
	
	@Column(columnDefinition = "text")
	public String getAddress() {
		return this.address;
	}
	
	public void setAddress(final String address) {
		this.address = address;
	}
	
	public String getPhone() {
		return this.phone;
	}
	
	public void setPhone(final String phone) {
		this.phone = phone;
	}
	
	public void setTimeZoneID(final String timeZoneID) {
		this.timeZoneID = timeZoneID;
	}
	
	public String getTimeZoneID() {
		return this.timeZoneID;
	}
	
	public void setTimeFormat(final String timeFormat) {
		this.timeFormat = timeFormat;
	}
	
	public String getTimeFormat() {
		return this.timeFormat;
	}
	
	/**
	 * @return the alias
	 */
	public String getAlias() {
		return this.alias;
	}
	
	/**
	 * @param alias the alias to set
	 */
	public void setAlias(final String alias) {
		this.alias = alias;
	}
	
	// /**
	// * @return the userGroup
	// */
	// @ManyToOne(fetch = FetchType.LAZY)
	// @JoinColumn(name = "userGroup_id", nullable = false)
	// public List<UserGroup> getUserGroup() {
	// return userGroup;
	// }
	//
	// /**
	// * @param userGroup the userGroup to set
	// */
	// public void setUserGroup(final UserGroup userGroup) {
	// this.userGroup = userGroup;
	// }
	/**
	 * @return the userGroups
	 */
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "group_memberships", joinColumns = {@JoinColumn(name = "systemuser_id")}, inverseJoinColumns = {@JoinColumn(name = "usergroup_id")})
	public Set<UserGroup> getUserGroups() {
		return this.userGroups;
	}
	
	/**
	 * @param userGroups the userGroups to set
	 */
	public void setUserGroups(final Set<UserGroup> userGroups) {
		this.userGroups = userGroups;
	}
	
	/**
	 * @return the userRights
	 */
	@Override
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "systemuser_rights", joinColumns = {@JoinColumn(name = "systemuser_id")}, inverseJoinColumns = {@JoinColumn(name = "userright_id")})
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
	 * @return the lastLogin
	 */
	public Date getLastLogin() {
		return this.lastLogin;
	}
	
	/**
	 * @param lastLogin the lastLogin to set
	 */
	public void setLastLogin(final Date lastLogin) {
		this.lastLogin = lastLogin;
	}
	
	@OneToOne(cascade = CascadeType.ALL)
	public Token getToken() {
		return token;
	}
	
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
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if ((obj == null) || !(obj instanceof SystemUser)) {
			return false;
		}
		return obj.toString().equals(this.toString());
	}
	
	@Override
	public SystemUser clone() {
		SystemUser clone = null;
		try {
			clone = (SystemUser) super.clone();
			
			if (this.userGroups != null) {
				Set<UserGroup> newUserGroups = new HashSet<UserGroup>();
				for (UserGroup ug : this.userGroups) {
					newUserGroups.add(ug.clone());
				}
				clone.setUserGroups(newUserGroups);
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
	
	public SystemUser cloneWithId() {
		SystemUser clone = null;
		try {
			clone = (SystemUser) super.clone();
			
			if (this.userGroups != null) {
				Set<UserGroup> newUserGroups = new HashSet<UserGroup>();
				for (UserGroup ug : this.userGroups) {
					newUserGroups.add(ug.cloneWithId());
				}
				clone.setUserGroups(newUserGroups);
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
	 * @param user
	 * @return
	 */
	public static boolean isUserUnique(final SystemUser user, final Session hibSess) {
		Criteria crit = hibSess.createCriteria(SystemUser.class);
		crit.add(Restrictions.eq("userName", user.getUserName()));
		SystemUser someUser = (SystemUser) crit.uniqueResult();
		if (someUser!= null){
			return false;
		}
		return true;
	}
}
