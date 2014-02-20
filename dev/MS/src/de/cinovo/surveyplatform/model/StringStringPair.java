package de.cinovo.surveyplatform.model;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class StringStringPair {
	
	private String key;
	
	private String value;
	
	private Object meta;
	
	
	public StringStringPair(final String key, final String value) {
		this.key = key;
		this.value = value;
	}
	
	/**
	 * @return the key
	 */
	public String getKey() {
		return this.key;
	}
	
	/**
	 * @param key
	 *            the key to set
	 */
	public void setKey(final String key) {
		this.key = key;
	}
	
	/**
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}
	
	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(final String value) {
		this.value = value;
	}
	
	/**
	 * @param meta optional meta information for the pair
	 */
	public void setMeta(final Object meta) {
		this.meta = meta;
	}
	
	/**
	 * @return optional meta information for the pair
	 */
	public Object getMeta() {
		return this.meta;
	}
	
	@Override
	public boolean equals(final Object kvp) {
		return ((StringStringPair) kvp).getKey().equals(this.key) && ((StringStringPair) kvp).getValue().equals(this.value);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.key + "=" + this.value + (this.meta != null ? (" (" + String.valueOf(this.meta) + ")") : "");
	}
}
