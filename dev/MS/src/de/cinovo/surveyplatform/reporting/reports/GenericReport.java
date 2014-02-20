/**
 *
 */
package de.cinovo.surveyplatform.reporting.reports;

import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.reporting.AbstractReport;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;
import de.cinovo.surveyplatform.reporting.generator.DeviationReportGenerator;
import de.cinovo.surveyplatform.reporting.generator.PerformanceReportGenerator;

/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class GenericReport extends AbstractReport {
	
	private String id = "";
	
	private String name = "";
	
	private Type reportType;
	
	
	public enum Type {
		DEVIATION, PERFORMANCE
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see de.cinovo.surveyplatform.model.reporting.IReport#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see de.cinovo.surveyplatform.model.reporting.IReport#getId()
	 */
	@Override
	public String getId() {
		return this.id;
	}
	
	public void setId(final String id) {
		this.id = id;
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see de.cinovo.surveyplatform.model.reporting.IReport#evaluate(de.cinovo.
	 * surveyplatform.model.Survey)
	 */
	@Override
	public StringBuilder evaluate(final Survey survey, final TargetMedia targetMedia, final String taskID, final Type reportType, final SubTypeEnum subType) {
		if (Type.DEVIATION.equals(reportType)) {
			return new DeviationReportGenerator(this, survey).evaluate(targetMedia, subType, taskID);
		} else if (Type.PERFORMANCE.equals(reportType)) {
			return new PerformanceReportGenerator(this, survey).evaluate(targetMedia, subType, taskID);
		} else {
			return new StringBuilder();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.model.reporting.IReport#evaluate(de.cinovo.surveyplatform.model.Survey,
	 * de.cinovo.surveyplatform.model.question.AbstractQuestion, de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia,
	 * java.lang.String, de.cinovo.surveyplatform.reporting.reports.GenericReport.Type)
	 */
	@Override
	public StringBuilder evaluate(final Survey survey, final AbstractQuestion question, final TargetMedia targetMedia, final String taskID, final Type reportType, final SubTypeEnum subType) {
		if (Type.DEVIATION.equals(reportType)) {
			return new DeviationReportGenerator(this, survey).evaluate(question, targetMedia, subType, taskID);
		} else if (Type.PERFORMANCE.equals(reportType)) {
			return new PerformanceReportGenerator(this, survey).evaluate(question, targetMedia, subType, taskID);
		} else {
			return new StringBuilder();
		}
	}
	
	public void setReportType(final Type reportType) {
		this.reportType = reportType;
	}
	
	public Type getReportType() {
		return reportType;
	}
}
