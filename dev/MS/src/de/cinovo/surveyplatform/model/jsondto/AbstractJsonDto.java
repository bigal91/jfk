package de.cinovo.surveyplatform.model.jsondto;

import com.google.gson.Gson;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
abstract class AbstractJsonDto {
	
	private Gson gson = new Gson();
	
	public String getJSON() {
		return gson.toJson(this);
	}
}
