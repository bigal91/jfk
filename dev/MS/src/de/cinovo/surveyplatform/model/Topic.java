/**
 * 
 */
package de.cinovo.surveyplatform.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import de.cinovo.surveyplatform.model.question.Answer;

/**
 * Copyright 2011 Cinovo AG <br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@Entity
public class Topic {
	
	private long id;
	
	private String title;
	
	private int refQuestionId;
	
	private List<Answer> answers;
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return this.id;
	}
	
	public void setId(final long id) {
		this.id = id;
	}
	
	@OneToMany(cascade = CascadeType.ALL)
	public List<Answer> getAnswers() {
		return this.answers;
	}
	
	public void setAnswers(final List<Answer> answers) {
		this.answers = answers;
	}
	
	public int getRefQuestionId() {
		return this.refQuestionId;
	}
	
	public void setRefQuestionId(final int refQuestionId) {
		this.refQuestionId = refQuestionId;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(final String title) {
		this.title = title;
	}
	
}
