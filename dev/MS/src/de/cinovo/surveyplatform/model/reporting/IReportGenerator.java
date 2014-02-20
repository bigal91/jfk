package de.cinovo.surveyplatform.model.reporting;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public interface IReportGenerator {
	
	public enum TargetMedia {
		SCREEN, PRINTER_REPORT, PRINTER_QUESTIONNAIRE
	}
	
	
	String getIdentifier();
	
	StringBuilder evaluate(TargetMedia targetMedia, SubTypeEnum type, String taskID);
	
	StringBuilder evaluate(AbstractQuestion question, TargetMedia targetMedia, SubTypeEnum type, String taskID);
}
