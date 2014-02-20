/**
 *
 */
package de.cinovo.surveyplatform.constants;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * This enum describes the existing userrights. It is beeing kept synchronous
 * with the database through the fixture.class
 * 
 * @author yschubert
 * 
 */
public enum UserRights {
	
	// enum und id MÜSSEN identisch sein!
	ADMINISTRATOR("administrator", "Is a system Administrator", true), CLIENT_ADMINISTRATOR(
			
			"client_administrator",
			"Client Administrator (No restrictions, all rights including group generation)",
			false), GROUP_ADMINISTRATOR(
					
					"group_administrator",
					"Group Administrator (Rights to manage a specific group including user management)",
					false), SURVEY_MANAGER(
							
							"survey_manager",
							"Survey Manager (Rights to administrate surveys and generate reports)",
 false), DATA_VIEWER("data_viewer",
									"Report Viewer (Restricted rights to view reports only)", false), DATA_RECORDER(
											"data_recorder",
			"Data Recorder (Restricted rights to record data)", false), NONE("none", "None (Use this for containers)", false);
	
	private final String id;
	private final String description;
	private final boolean internal;
	
	
	UserRights(final String id, final String description, final boolean internal) {
		this.id = id;
		this.description = description;
		this.internal = internal;
	}
	
	public String id() {
		return this.id;
	}
	
	public String decription() {
		return this.description;
	}
	
	public boolean internal() {
		return this.internal;
	}
	
}
