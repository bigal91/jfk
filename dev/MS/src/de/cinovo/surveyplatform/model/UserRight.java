/**
 *
 */
package de.cinovo.surveyplatform.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@Entity
public class UserRight implements Cloneable {
	
	private String name;
	
	private boolean internal;
	
	private boolean inherited;
	
	
	/**
	 * for hibernate
	 */
	public UserRight() {
		
	}
	
	/**
	 * for convenience
	 */
	public UserRight(final String name) {
		this.name = name;
	}
	
	/**
	 * @return the name
	 */
	@Id
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
	 * @return the internal
	 */
	public boolean isInternal() {
		return this.internal;
	}
	
	/**
	 * @param internal the internal to set
	 */
	public void setInternal(final boolean internal) {
		this.internal = internal;
	}
	
	/**
	 * @return the inherited
	 */
	@Transient
	@XmlTransient
	public boolean isInherited() {
		return this.inherited;
	}
	
	/**
	 * @param inherited the inherited to set
	 */
	public void setInherited(final boolean inherited) {
		this.inherited = inherited;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.name.hashCode();
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
		return this.name.equals(((UserRight) obj).name);
	}
	
	@Override
	public UserRight clone() {
		UserRight clone = null;
		try {
			clone = (UserRight) super.clone();
			
		} catch (CloneNotSupportedException e) {
			Logger.errUnexpected(e, null);
		}
		
		return clone;
	}
}
