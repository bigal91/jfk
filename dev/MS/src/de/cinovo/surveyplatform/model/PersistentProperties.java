/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;

import de.cinovo.surveyplatform.sync.SyncIdentifiable;
import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@Entity
public class PersistentProperties implements Cloneable, SyncIdentifiable<String> {
	
	private int id;
	
	private String syncId;
	
	private Map<String, String> properties = new HashMap<String, String>();
	
	
	@ElementCollection
	@MapKeyColumn(name = "key")
	@Column(name = "value", nullable = false)
	@CollectionTable(name = "properties", joinColumns = {@JoinColumn(name = "entityid")})
	public Map<String, String> getProperties() {
		return this.properties;
	}
	
	public void setProperties(final Map<String, String> properties) {
		this.properties = properties;
	}
	
	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public int getId() {
		return this.id;
	}
	
	@XmlID
	@Transient
	public String getXMLID() {
		return this.getClass().getSimpleName() + this.getId();
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(final int id) {
		this.id = id;
	}
	
	public void setProperty(final String key, final String value) {
		this.properties.put(key, value);
	}
	
	@Transient
	public String getProperty(final String key) {
		return this.properties.get(key);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public PersistentProperties clone() {
		PersistentProperties clone = null;
		try {
			clone = (PersistentProperties) super.clone();
			clone.setId(0);
			clone.setProperties(new HashMap<String, String>());
			for (Entry<String, String> entry : this.getProperties().entrySet()) {
				clone.getProperties().put(entry.getKey(), entry.getValue());
			}
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		return clone;
	}
	
	public PersistentProperties cloneWithId() {
		PersistentProperties clone = this.clone();
		clone.setId(this.getId());
		return clone;
	}
	
	/**
	 * @param syncId the syncId to set
	 */
	public void setSyncId(final String syncId) {
		this.syncId = syncId;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.sync.SyncIdentifiable#getSyncId()
	 */
	@Override
	public String getSyncId() {
		return this.syncId;
	}
}
