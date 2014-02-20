/**
 * 
 */
package de.cinovo.surveyplatform.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * Copyright 2013 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@Entity
public class Project {
	
	private long id;
	
	private String name;
	
	private Client owner;
	
	private boolean deleted = false;
	
	
	/**
	 * @return -
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return this.id;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(final long id) {
		this.id = id;
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
	 * @return the owner
	 */
	@OneToOne
	public Client getOwner() {
		return this.owner;
	}
	
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(final Client owner) {
		this.owner = owner;
	}
	
	/**
	 * @return the deleted
	 */
	public boolean isDeleted() {
		return this.deleted;
	}
	
	/**
	 * @param deleted the deleted to set
	 */
	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}
	
}
