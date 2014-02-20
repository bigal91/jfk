/**
 *
 */
package de.cinovo.surveyplatform.model.question;

import javax.persistence.Column;
import javax.persistence.Entity;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
@Entity
public class PhoneCallerHint extends TextPart {
	
	private String textValue = "";
	
	@Override
	@Column(columnDefinition = "text")
	public String getTextValue() {
		return textValue;
	}
	
	@Override
	public void setTextValue(final String textValue) {
		this.textValue = textValue;
	}

}
