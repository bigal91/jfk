/**
 * 
 */
package de.cinovo.surveyplatform.model.grouptemplate;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import de.cinovo.surveyplatform.constants.UserRights;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@XmlAccessorType(value = XmlAccessType.FIELD)
public class GroupTemplate {
	
	@XmlAttribute
	private String name;
	
	@XmlElements({@XmlElement(name = "userRight", type = UserRights.class)})
	private Set<UserRights> userRights;
	
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}
	
	/**
	 * @return the userRights
	 */
	public Set<UserRights> getUserRights() {
		return userRights;
	}
	
	/**
	 * @param userRights the userRights to set
	 */
	public void setUserRights(final Set<UserRights> userRights) {
		this.userRights = userRights;
	}
	
}
