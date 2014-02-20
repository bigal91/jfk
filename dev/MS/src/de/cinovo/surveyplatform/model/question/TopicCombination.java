/**
 * 
 */
package de.cinovo.surveyplatform.model.question;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
@Entity
public class TopicCombination implements Cloneable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	private int cutBegin;
	private int cutEnd;
	private int answerId;
	
	
	/**
	 * for hibernate
	 */
	public TopicCombination() {
		
	}
	
	public TopicCombination(final int answerId, final int cutBegin, final int cutEnd) {
		this.answerId = answerId;
		this.cutBegin = cutBegin;
		this.cutEnd = cutEnd;
	}
	
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(final long id) {
		this.id = id;
	}
	
	
	/**
	 * @return the cutBegin
	 */
	public int getCutBegin() {
		return cutBegin;
	}
	
	/**
	 * @param cutBegin the cutBegin to set
	 */
	public void setCutBegin(final int cutBegin) {
		this.cutBegin = cutBegin;
	}
	
	/**
	 * @return the cutEnd
	 */
	public int getCutEnd() {
		return cutEnd;
	}
	
	/**
	 * @param cutEnd the cutEnd to set
	 */
	public void setCutEnd(final int cutEnd) {
		this.cutEnd = cutEnd;
	}
	
	/**
	 * @return the answerId
	 */
	public int getAnswerId() {
		return answerId;
	}
	
	/**
	 * @param answerId the answerId to set
	 */
	public void setAnswerId(final int answerId) {
		this.answerId = answerId;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public TopicCombination clone() {
		try {
			TopicCombination clone = (TopicCombination) super.clone();
			clone.setId(0);
			return clone;
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		return null;
	}
	
	public TopicCombination cloneWithId() {
		try {
			TopicCombination clone = (TopicCombination) super.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		return null;
	}
	
	
}
