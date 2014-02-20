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
public interface SyncPreSyncCallback {
	
	public void preSync(Object left, Object leftFieldValue, Object right, Object rightFieldValue, String fieldName);
}
