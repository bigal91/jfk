/**
 *
 */
package de.cinovo.surveyplatform.reporting.reports;

import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.reporting.AbstractReport;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;
import de.cinovo.surveyplatform.reporting.generator.SummaryReportGenerator;
import de.cinovo.surveyplatform.reporting.reports.GenericReport.Type;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class SummaryReport extends AbstractReport {
	
	private String id = "report_summary";
	private String name = "Summary Report";
	
	
	public SummaryReport() {
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public StringBuilder evaluate(final Survey survey, final TargetMedia targetMedia, final String taskID, final Type reportType, final SubTypeEnum subType) {
		return new SummaryReportGenerator(this, survey).evaluate(targetMedia, subType, taskID);
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
		return new SummaryReportGenerator(this, survey).evaluate(question, targetMedia, subType, taskID);
	}
}
