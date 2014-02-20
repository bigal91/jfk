/**
 *
 */
package de.cinovo.surveyplatform.model.reporting;

import java.util.List;

/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author yschubert
 *
 */
// @XmlRootElement
// @XmlAccessorType(value = XmlAccessType.FIELD)
public class AllowedReports {
	
	private List<String> allowed;
	
	private int surveyId;
	
	
	public List<String> getAllowed() {
		return allowed;
	}
	
	public void setAllowed(final List<String> allowed) {
		this.allowed = allowed;
	}

	public void setSurveyId(int surveyId) {
		this.surveyId = surveyId;
	}

	public int getSurveyId() {
		return surveyId;
	}
	
	// public static void main(final String[] args) {
	// AllowedReports ar = new AllowedReports();
	//
	// ar.setAllowed(new ArrayList<String>());
	// ar.getAllowed().add("6070");
	//
	// System.out.println(XMLUtil.obj2XmlFile(ar, AllowedReports.class, new
	// File("Reports/6070/allowedReports.xml")));
	// }
}
