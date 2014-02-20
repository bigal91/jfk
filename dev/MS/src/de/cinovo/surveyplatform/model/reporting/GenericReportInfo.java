/**
 *
 */
package de.cinovo.surveyplatform.model.reporting;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import de.cinovo.surveyplatform.reporting.reports.GenericReport.Type;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author ablehm
 *
 */
@Entity
public class GenericReportInfo implements Cloneable {
	
	private String name;
	
	private String description;
	
	private List<Integer> questions;
	
	private int id;
	
	private int surveyId;
	
	private Type reportType;
	
	
	public String getName() {
		return name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	@ElementCollection
	public List<Integer> getQuestions() {
		return questions;
	}
	
	public void setQuestions(final List<Integer> questions) {
		this.questions = questions;
	}
	
	public void setId(final int id) {
		this.id = id;
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public int getId() {
		return id;
	}
	
	public void setSurveyId(final int surveyId) {
		this.surveyId = surveyId;
	}
	
	public int getSurveyId() {
		return surveyId;
	}
	
	public void setRepInfoType(final Type reportType) {
		this.reportType = reportType;
	}
	
	public Type getRepInfoType() {
		return reportType;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected GenericReportInfo clone() {
		try {
			GenericReportInfo newGenericReportInfo = (GenericReportInfo) super.clone();
			newGenericReportInfo.setId(0);
			if (this.questions != null) {
				newGenericReportInfo.setQuestions(new ArrayList<Integer>());
				newGenericReportInfo.getQuestions().addAll(questions);
			}
			
			return newGenericReportInfo;
		} catch (CloneNotSupportedException cns) {
			// clone IS supported
		}
		return null;
	}
	
	public void setDescription(final String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
}
