/**
 *
 */
package de.cinovo.surveyplatform.model.question;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
@Entity
public class TextPart extends AbstractQuestion {
	
	/**
	 * 
	 */
	private static final ArrayList<Answer> EMPTY_ARRAY_LIST = new ArrayList<Answer>();

	private String textValue = "";
	
	
	@Column(columnDefinition = "text")
	public String getTextValue() {
		return textValue;
	}
	
	public void setTextValue(final String textValue) {
		this.textValue = textValue;
	}
	
	/* (non-Javadoc)
	 * @see de.cinovo.surveyplatform.model.question.AbstractQuestion#getAnswer()
	 */
	@Override
	@Transient
	public List<Answer> getAnswer() {
		return EMPTY_ARRAY_LIST;
	}
	
}
