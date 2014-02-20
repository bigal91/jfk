package de.cinovo.surveyplatform.model.chart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.plot.PlotOrientation;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class BarChartInfo {
	
	private String title;
	private String xAxisLabel;
	private String yAxisLabel;
	private List<Integer> numberOfResponses;
	
	private int height;
	private int width;
	
	private PlotOrientation orientation = PlotOrientation.VERTICAL;
	
	private List<DataSetContainer> dataSets;
	
	private boolean isPercentage = false;
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(final String title) {
		this.title = title;
	}
	
	public String getxAxisLabel() {
		return xAxisLabel;
	}
	
	public void setxAxisLabel(final String xAxisLabel) {
		this.xAxisLabel = xAxisLabel;
	}
	
	public String getyAxisLabel() {
		return yAxisLabel;
	}
	
	public void setyAxisLabel(final String yAxisLabel) {
		this.yAxisLabel = yAxisLabel;
	}
	
	public PlotOrientation getOrientation() {
		return orientation;
	}
	
	public void setOrientation(final PlotOrientation orientation) {
		this.orientation = orientation;
	}
	
	
	public List<DataSetContainer> getDataSets() {
		return dataSets;
	}
	
	public void addDataSet(final Map<String, Double> dataSet, final String name) {
		if (dataSets == null) {
			dataSets = new ArrayList<DataSetContainer>();
		}
		DataSetContainer newSet = new DataSetContainer(name, dataSet);
		this.dataSets.add(newSet);
	}
	
	public void addDataSet(final DataSetContainer dataSet) {
		if (dataSets == null) {
			dataSets = new ArrayList<DataSetContainer>();
		}
		this.dataSets.add(dataSet);
	}
	
	
	public int getHeight() {
		return height;
	}
	
	public void setHeight(final int height) {
		this.height = height;
	}
	
	public List<Integer> getNumberOfResponses() {
		return numberOfResponses;
	}
	
	public void setNumberOfResponses(final List<Integer> numberOfResponses) {
		this.numberOfResponses = numberOfResponses;
	}
	
	public int getWidth() {
		return width;
	}
	
	public void setWidth(final int width) {
		this.width = width;
	}
	
	public void convertToPercentage() {
		if (!isPercentage) {
			int length = dataSets.size();
			for (int i = 0; i < length; i++) {
				Map<String, Double> dataSet = dataSets.get(i).dataSet;
				Map<String, Double> newDataSet = new LinkedHashMap<String, Double>();
				int number = 0;
				if (numberOfResponses.size() > i) {
					number = numberOfResponses.get(i);
				} else {
					number = numberOfResponses.get(0);
				}
				
				int sum = 0;
				if (number == 0) {
					// find maximum value
					for (Double value : dataSet.values()) {
						sum += value;
					}
				} else {
					sum = number;
				}
				for (Entry<String, Double> entry : dataSet.entrySet()) {
					newDataSet.put(entry.getKey(), (entry.getValue() / sum));
				}
				dataSets.get(i).dataSet = newDataSet;
			}
			setPercentage(true);
		}
	}
	
	public boolean isPercentage() {
		return isPercentage;
	}
	
	public void setPercentage(final boolean isPercentage) {
		this.isPercentage = isPercentage;
	}
	
	
	
}
