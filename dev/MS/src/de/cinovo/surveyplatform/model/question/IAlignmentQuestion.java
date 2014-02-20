/**
 * 
 */
package de.cinovo.surveyplatform.model.question;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * Implementors of this interface allow to define the alignment of the question
 * 
 * @author yschubert
 * 
 */
public interface IAlignmentQuestion {
	
	public enum Alignment {
		HORIZONTAL, VERTICAL
	}
	
	Alignment getAlignment();
	
	void setAlignment(Alignment alignment);
}
