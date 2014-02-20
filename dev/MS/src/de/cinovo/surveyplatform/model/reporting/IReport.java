/**
 *
 */
package de.cinovo.surveyplatform.model.reporting;

import java.util.List;

import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;
import de.cinovo.surveyplatform.reporting.reports.GenericReport.Type;



/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public interface IReport {
	
	/**
	 * @return global identifier of this report
	 */
	String getId();
	
	/**
	 * @return display name of this report
	 */
	String getName();
	
	/**
	 * Evaluates the survey
	 * 
	 * @param survey The survey to evaluate
	 * @param targetMedia the media to which the report is generated
	 * @param taskID ID to track the unique progress of the report generation task
	 * @param reportType the enum Type for genericReports to generate in the correct generator
	 * @param subType the subtype of the report (qualitative, quantitative, combined)
	 * @return Visual representation of the report
	 */
	StringBuilder evaluate(Survey survey, TargetMedia targetMedia, String taskID, Type reportType, SubTypeEnum subType);
	
	/**
	 * Evaluates a single question
	 * 
	 * @param survey The survey to evaluate
	 * @param question The question to evaluate
	 * @param targetMedia the media to which the report is generated
	 * @param taskID ID to track the unique progress of the report generation task
	 * @param reportType the enum Type for genericReports to generate in the correct generator
	 * @param subType the subtype of the report (qualitative, quantitative, combined)
	 * @return Visual representation of the report
	 */
	StringBuilder evaluate(Survey survey, AbstractQuestion question, TargetMedia targetMedia, String taskID, Type reportType, SubTypeEnum subType);
	
	/**
	 * Get the list of subtypes of this report (returns null if there are no
	 * subtypes)
	 */
	List<ReportType> getSubTypes();
}
