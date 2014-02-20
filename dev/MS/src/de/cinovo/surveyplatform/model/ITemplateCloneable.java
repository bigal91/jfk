package de.cinovo.surveyplatform.model;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public interface ITemplateCloneable {
	
	/**
	 * Makes a clone of the object, but sets properties which will make make a
	 * concrete copy of a template
	 * 
	 * @return
	 */
	Object templateClone();
	
}
