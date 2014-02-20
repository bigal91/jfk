/**
 *
 */
package de.cinovo.surveyplatform.chart;

import java.awt.Color;
import java.util.List;

import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author ablehm
 * 
 */
public class PieRenderer {
	
	private Color[] color;
	
	
	public PieRenderer(final Color[] color) {
		this.color = color;
	}
	
	@SuppressWarnings("rawtypes")
	public void setColor(final PiePlot plot, final DefaultPieDataset dataset) {
		@SuppressWarnings("unchecked")
		List<Comparable> keys = dataset.getKeys();
		int aInt;
		
		for (int i = 0; i < keys.size(); i++) {
			aInt = i % this.color.length;
			plot.setSectionPaint(keys.get(i), this.color[aInt]);
		}
	}
}
