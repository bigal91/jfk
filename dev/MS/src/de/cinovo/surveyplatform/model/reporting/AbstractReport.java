package de.cinovo.surveyplatform.model.reporting;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractReport implements IReport {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	private List<ReportType> subTypes;
	
	private String description = "";
	
	/**
	 * needed for serialisation
	 */
	public AbstractReport() {
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public List<ReportType> getSubTypes() {
		return subTypes;
	}

	public void setSubTypes(final List<ReportType> subTypes) {
		this.subTypes = subTypes;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(final String description) {
		this.description = description;
	}

}
