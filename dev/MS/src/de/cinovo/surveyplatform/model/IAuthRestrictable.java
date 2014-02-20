/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.util.Set;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public interface IAuthRestrictable {
	
	Set<UserRight> getUserRights();
	
	void setUserRights(Set<UserRight> userRights);
	
}
