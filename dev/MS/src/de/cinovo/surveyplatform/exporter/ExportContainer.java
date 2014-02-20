/**
 *
 */
package de.cinovo.surveyplatform.exporter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.relational.Exportable;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * Content of the exports should be {@link Exportable} or List of Exportables
 * 
 * @author yschubert
 * 
 */
public class ExportContainer {
	
	List<Object> exports = new ArrayList<Object>();
	
	
	public List<Object> getContent() {
		return this.exports;
	}
}
