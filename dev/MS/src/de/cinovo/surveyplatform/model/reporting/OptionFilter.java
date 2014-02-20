package de.cinovo.surveyplatform.model.reporting;

import java.util.ArrayList;
import java.util.List;

import de.cinovo.surveyplatform.model.question.Option;


/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class OptionFilter {
	
	private int questionId;
	
	private List<Option> exampleOptions = new ArrayList<Option>();
	
	
	public int getQuestionId() {
		return questionId;
	}
	
	public void setQuestionId(final int questionId) {
		this.questionId = questionId;
	}
	
	public List<Option> getExampleOptions() {
		return exampleOptions;
	}
	
	public void setExampleOptions(final List<Option> exampleOptions) {
		this.exampleOptions = exampleOptions;
	}
	
}
