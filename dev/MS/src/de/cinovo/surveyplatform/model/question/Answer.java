/**
 *
 */
package de.cinovo.surveyplatform.model.question;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import de.cinovo.surveyplatform.util.Logger;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
@Entity
public class Answer implements Cloneable, Comparable<Answer> {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;
	
	@Column(columnDefinition = "text")
	private String answer;
	
	@OneToMany(cascade = CascadeType.ALL)
	private List<TopicCombination> topicCombinations;
	
	
	/**
	 * empty constructor needed for hibernate
	 */
	public Answer() {
		
	}
	
	public Answer(final String answerText) {
		this.answer = answerText;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(final int id) {
		this.id = id;
	}
	
	
	public String getAnswer() {
		return this.answer;
	}
	
	public void setAnswer(final String answer) {
		this.answer = answer;
	}
	
	
	/**
	 * @return the topicCombinations
	 */
	public List<TopicCombination> getTopicCombinations() {
		return topicCombinations;
	}
	
	/**
	 * @param topicCombinations the topicCombinations to set
	 */
	public void setTopicCombinations(final List<TopicCombination> topicCombinations) {
		this.topicCombinations = topicCombinations;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.answer;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	/**
	 * Caution: This clone sets the ID of the cloned Answer to 0
	 */
	@Override
	public Answer clone() {
		try {
			Answer newAnswer = (Answer) super.clone();
			newAnswer.setId(0);
			
			if (topicCombinations != null) {
				newAnswer.setTopicCombinations(new ArrayList<TopicCombination>());
				for (TopicCombination tc : topicCombinations) {
					newAnswer.getTopicCombinations().add(tc.clone());
				}
			}
			
			return newAnswer;
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final Answer o) {
		return this.getAnswer().trim().compareTo(o.getAnswer().trim());
	}
	
	/**
	 * @return
	 */
	public Answer cloneWithId() {
		try {
			Answer newAnswer = (Answer) super.clone();
			newAnswer.setId(getId());
			
			if (topicCombinations != null) {
				newAnswer.setTopicCombinations(new ArrayList<TopicCombination>());
				for (TopicCombination tc : topicCombinations) {
					newAnswer.getTopicCombinations().add(tc.cloneWithId());
				}
			}
			
			return newAnswer;
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return null;
	}
	
}
