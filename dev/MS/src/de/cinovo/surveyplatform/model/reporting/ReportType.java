/**
 *
 */
package de.cinovo.surveyplatform.model.reporting;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */

public class ReportType {
	
	private SubTypeEnum id;
	private String displayName = "";
	
	
	public enum SubTypeEnum {
		QUALITATIVE, QUANTITATIVE, COMBINED
	}
	
	
	public ReportType() {
		// empty constructor needed for JAXB
	}
	
	public ReportType(final SubTypeEnum id, final String displayName) {
		this.id = id;
		this.displayName = displayName;
	}
	
	public SubTypeEnum getId() {
		return id;
	}
	
	public void setId(final SubTypeEnum id) {
		this.id = id;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}
	
}
