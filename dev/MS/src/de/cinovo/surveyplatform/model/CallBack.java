/**
 * 
 */
package de.cinovo.surveyplatform.model;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public abstract class CallBack {
	
	/**
	 * Called in case of success
	 */
	public abstract void doCallBack();
	
	/**
	 * Called in case of failure
	 */
	public void doCallBackFailure(final Exception e) {
		// optional
	}
	
}
