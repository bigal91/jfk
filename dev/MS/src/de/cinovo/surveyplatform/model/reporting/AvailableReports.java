package de.cinovo.surveyplatform.model.reporting;

import java.util.List;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * 
 * This class is used to persist and load the list of available reports
 * for the platform over javax.xml
 * 
 * @author yschubert
 * 
 * 
 */
// @XmlRootElement()
// @XmlAccessorType(value = XmlAccessType.FIELD)
public class AvailableReports {
	
	private List<AbstractReport> reports;
	
	private static AvailableReports instance;
	
	
	// @XmlElements( {@XmlElement(name = "SummaryReport", type =
	// SummaryReport.class), //
	// @XmlElement(name = "PerformanceIndexReport", type =
	// PerformanceIndexReport.class), //
	// @XmlElement(name = "ProvincialReport", type = ProvincialReport.class), //
	// @XmlElement(name = "EthnicGroupReport", type = EthnicGroupReport.class),
	// //
	// @XmlElement(name = "GenderReport", type = GenderReport.class), //
	// @XmlElement(name = "Learnership181_182Report", type =
	// Learnership181_182Report.class), //
	// @XmlElement(name = "NQFLevelReport", type = NQFLevelReport.class), //
	// @XmlElement(name = "AgeGroupReport", type = AgeGroupReport.class), //
	// @XmlElement(name = "TrainingProviderReport", type =
	// TrainingProviderReport.class), //
	// @XmlElement(name = "GenericReport", type = GenericReport.class), //
	// })
	
	public static AvailableReports getInstance() {
		if (instance == null) {
			instance = new AvailableReports();
		}
		return instance;
	}
	
	public List<AbstractReport> getReports() {
		return reports;
	}
	
	public void setReports(final List<AbstractReport> reports) {
		this.reports = reports;
	}
}

