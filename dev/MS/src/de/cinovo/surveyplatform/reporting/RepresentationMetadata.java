/**
 *
 */
package de.cinovo.surveyplatform.reporting;

import java.util.UUID;

import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class RepresentationMetadata {
	
	public boolean useSlideToggleFragment = false;
	public String uuid = UUID.randomUUID().toString();
	public TargetMedia targetMedia;
	public int surveyID;
}
