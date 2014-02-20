/**
 * 
 */
package de.cinovo.surveyplatform.sync;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author cinovo
 * 
 */
public interface SyncPostSyncCallback {
	
	public void postSync(Object left, Object leftFieldValue, Object right, Object rightFieldValue, String fieldName);
	
}
