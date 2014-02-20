/**
 *
 */
package de.cinovo.surveyplatform.sync;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public interface SyncIdentifiable<T> {
	
	/**
	 * 
	 * @return a "worldwide" unique identifier for this object
	 */
	public T getSyncId();
}
