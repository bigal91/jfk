/**
 *
 */
package de.cinovo.surveyplatform.model;

import javax.persistence.Column;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 *         Interface that shall be implemented by all parts of a questionnaire
 */
public interface IQuestionnairePart {
	
	Integer getId();
	
	/**
	 * @param questionIsVisible
	 */
	void setVisible(boolean visible);
	
	/**
	 * @param partIsVisible
	 */
	@Column(columnDefinition = "boolean not null default true")
	void setPreviewModeVisible(boolean previewModeVisible);
	
}
