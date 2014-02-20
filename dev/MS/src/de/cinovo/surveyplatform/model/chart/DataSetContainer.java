package de.cinovo.surveyplatform.model.chart;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DataSetContainer {
	
	public String name;
	public Map<String, Double> dataSet;
	
	
	public DataSetContainer(final String name, final Map<String, Double> dataSet) {
		this.name = name;
		// make a real clone of the map
		this.dataSet = new LinkedHashMap<String, Double>();
		for (Entry<String, Double> entry : dataSet.entrySet()) {
			this.dataSet.put(entry.getKey(), entry.getValue());
		}
	}
}