/**
 *
 */
package de.cinovo.surveyplatform.model.jsondto;




/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class QuestionnaireLogicElementDto extends AbstractJsonDto {
	
	public int id;
	
	public int surveyId;

	public String operator;
	
	public String answers;
	
	public String typeOfPart;
	
	public int idOfPart;
	
	public long questionIdWithLogic;
	
}
