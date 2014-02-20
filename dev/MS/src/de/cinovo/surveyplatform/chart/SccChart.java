package de.cinovo.surveyplatform.chart;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.TextAnchor;

import com.lowagie.text.Font;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.ChartType;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.chart.BarChartInfo;
import de.cinovo.surveyplatform.model.chart.DataSetContainer;
import de.cinovo.surveyplatform.util.Logger;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class SccChart {
	
	public static final int MAX_WIDTH = 715;
	
	private static final Color RANGE_GRID_LINE_COLOR = new Color(150, 150, 150);
	private static final Color GRID_LINE_COLOR = new Color(200, 200, 200);
	
	private static final Color[] COLORS = {new Color(79, 129, 189), new Color(192, 80, 77), new Color(155, 187, 89), new Color(247, 150, 70), new Color(128, 100, 162), new Color(165, 165, 165)};
	
	private static final java.awt.Font DEFAULT_FONT = new java.awt.Font("Arial", Font.NORMAL, 11);
	
	public static final String CHART_BASE_PATH = Paths.WEBCONTENT + "/" + Paths.CHARTS + "/";
	
	// public static final String OUTPUTTYPE = "SVG";
	
	public static final String OUTPUTTYPE = "PNG";
	
	public int maxLineNumbers = 1;
	public int currentLineNumbers = 0;
	private boolean hasData = true;
	
	public int lowCharLimit = 35;
	public int highCharLimit = 85;
	
	
	public String createChart(final String target, final ChartType type, final BarChartInfo barChartInfo) {
		
		if (SccChart.OUTPUTTYPE.equals("SVG")) {
			throw new RuntimeException("not implemented");
			// File targetFile = new File(CHART_BASE_PATH + target + ".svg");
			// barChartInfo.convertToPercentage();
			// // if (!targetFile.exists()) {
			// JFreeChart chart = null;
			// if (type.equals("bar")) {
			// chart = makeBarChart(barChartInfo);
			// }
			//
			// DOMImplementation domImpl =
			// GenericDOMImplementation.getDOMImplementation();
			// // Create an instance of org.w3c.dom.Document
			// Document document = domImpl.createDocument(null, "svg", null);
			// // Create an instance of the SVG Generator
			// SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
			// // set the precision to avoid a null pointer exception in Batik
			// 1.5
			// svgGenerator.getGeneratorContext().setPrecision(6);
			// // Ask the chart to render into the SVG Graphics2D implementation
			// chart.draw(svgGenerator, new Rectangle2D.Double(0, 0,
			// barChartInfo.getWidth(), barChartInfo.getHeight()), null);
			// // Finally, stream out SVG to a file using UTF-8 character to
			// // byte encoding
			// boolean useCSS = true;
			//
			// try {
			//
			// Writer out = new OutputStreamWriter(new
			// FileOutputStream(targetFile), "UTF-8");
			// svgGenerator.stream(out, useCSS);
			//
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// return EnvironmentConfiguration.getUrlBase() +
			// targetFile.getPath().substring(Paths.WEBCONTENT.length()).replace(File.separator,
			// "/");
		} else if (SccChart.OUTPUTTYPE.equals("PNG")) {
			File targetFile = new File(SccChart.CHART_BASE_PATH + target + ".png");
			File targetFileXL = new File(SccChart.CHART_BASE_PATH + target + "_xl.png");
			// if (!targetFile.exists()) {
			JFreeChart chart = null;
			this.maxLineNumbers = 1;
			this.currentLineNumbers = 1;
			BufferedImage bufferedImage;
			BufferedImage bufferedImageXL;
			// edit Chart Dimension here
			if (type.equals(ChartType.bar)) {
				chart = this.makeBarChart(barChartInfo);
				bufferedImage = chart.createBufferedImage(barChartInfo.getWidth(), Math.round(barChartInfo.getHeight() + (13 * this.maxLineNumbers)));
				bufferedImageXL = chart.createBufferedImage(barChartInfo.getWidth() * 2, Math.round(barChartInfo.getHeight() + (13 * this.maxLineNumbers)) * 2);
			} else if (type.equals(ChartType.pie)) {
				chart = this.makePieChart(barChartInfo, false);
				if (this.hasData) {
					bufferedImage = chart.createBufferedImage(545, 320);
				} else {
					bufferedImage = chart.createBufferedImage(545, 50);
				}
				chart = this.makePieChart(barChartInfo, true);
				bufferedImageXL = chart.createBufferedImage(800, 600);
				this.hasData = true;
			} else {
				bufferedImage = null;
				bufferedImageXL = null;
			}
			// no more 3 minute filter
			try {
				// ensure, that parent Path exists
				File parentFile = targetFile.getParentFile();
				if (!parentFile.exists()) {
					parentFile.mkdirs();
				}
				File parentFileXL = targetFileXL.getParentFile();
				if (!parentFileXL.exists()) {
					parentFileXL.mkdirs();
				}
				
				ImageIO.write(bufferedImage, "png", targetFile);
				ImageIO.write(bufferedImageXL, "png", targetFileXL);
			} catch (IOException e) {
				Logger.err("Konnte Bild nicht erstellen: " + targetFile, e);
			}
			return EnvironmentConfiguration.getUrlBase() + targetFile.getPath().substring(Paths.WEBCONTENT.length()).replace(File.separator, "/");
		}
		return "";
	}
	
	// BARCHART
	private JFreeChart makeBarChart(final BarChartInfo barChartInfo) {
		
		// yes, getHeight is correct here!
		int characters = (int) Math.round((double) barChartInfo.getHeight() / 11);
		
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		// int series = 0;
		for (DataSetContainer container : barChartInfo.getDataSets()) {
			for (Entry<String, Double> entry : container.dataSet.entrySet()) {
				String catName = entry.getKey();
				catName = this.stripHtml(catName);
				this.currentLineNumbers = 1;
				dataset.addValue(entry.getValue(), container.name, this.wrap(catName, characters));
			}
			// series++;
		}
		
		JFreeChart chart = ChartFactory.createBarChart(barChartInfo.getTitle(), barChartInfo.getxAxisLabel(), barChartInfo.getyAxisLabel(), dataset, barChartInfo.getOrientation(), barChartInfo.getDataSets().size() > 1, false, false);
		
		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);
		
		chart.setTextAntiAlias(true);
		chart.setAntiAlias(true);
		// get a reference to the plot for further customisation...
		
		// chart.setPadding(new RectangleInsets(0.0, 0.0, 0.0, 10.0));
		
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		
		// set the range axis to display integers only...
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		
		if (barChartInfo.isPercentage()) {
			rangeAxis.setTickUnit(new NumberTickUnit(.2, new DecimalFormat("##0%")));
		}
		
		rangeAxis.setUpperMargin(10.0);
		rangeAxis.setLowerMargin(10.0);
		
		if (barChartInfo.isPercentage()) {
			rangeAxis.setUpperBound(1.2);
			rangeAxis.setUpperMargin(0.05);
		} else {
			rangeAxis.setUpperBound(5.0);
			rangeAxis.setUpperMargin(1.0);
		}
		
		// disable bar outlines...
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		
		renderer.setDrawBarOutline(true);
		renderer.setShadowVisible(false);
		StandardCategoryItemLabelGenerator labelGenerator;
		if (barChartInfo.isPercentage()) {
			labelGenerator = new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat("##0%"));
		} else {
			labelGenerator = new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat("0.0"));
		}
		renderer.setBaseItemLabelGenerator(labelGenerator);
		renderer.setBaseItemLabelsVisible(true);
		renderer.setItemLabelAnchorOffset(20D);
		renderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.TOP_CENTER));
		
		renderer.setBaseItemLabelFont(SccChart.DEFAULT_FONT);
		
		plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(1.5f);
		plot.getDomainAxis().setMaximumCategoryLabelLines(20);
		plot.getDomainAxis().setCategoryMargin(0.5);
		
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(SccChart.GRID_LINE_COLOR);
		plot.setRangeGridlinePaint(SccChart.RANGE_GRID_LINE_COLOR);
		plot.setOutlinePaint(Color.white);
		
		GradientPaint gp0 = new GradientPaint(5.0f, 0.0f, SccChart.COLORS[0], 0.0f, 0.0f, new Color(109, 159, 229));
		renderer.setSeriesPaint(0, gp0);
		gp0 = new GradientPaint(5.0f, 0.0f, SccChart.COLORS[1], 0.0f, 0.0f, new Color(212, 100, 97));
		renderer.setSeriesPaint(1, gp0);
		
		plot.getDomainAxis().setTickLabelFont(SccChart.DEFAULT_FONT);
		plot.getRangeAxis().setTickLabelFont(SccChart.DEFAULT_FONT);
		
		renderer.setBaseFillPaint(SccChart.COLORS[0]);
		renderer.setBasePaint(SccChart.COLORS[0]);
		
		for (int i = 2; i < SccChart.COLORS.length; i++) {
			renderer.setSeriesPaint(i, SccChart.COLORS[i]);
			renderer.setSeriesFillPaint(i, SccChart.COLORS[i]);
		}
		
		return chart;
	}
	
	// PIECHART
	private JFreeChart makePieChart(final BarChartInfo barChartInfo, final boolean isXL) {
		
		this.lowCharLimit = 35;
		this.highCharLimit = 85;
		if (isXL) {
			this.lowCharLimit = 70;
			this.highCharLimit = 170;
		}
		
		int maxChar = 0;
		int options = 0;
		
		for (DataSetContainer container : barChartInfo.getDataSets()) {
			for (Entry<String, Double> entry : container.dataSet.entrySet()) {
				String catName = entry.getKey();
				if (catName.length() > maxChar) {
					maxChar = catName.length();
				}
				options++;
			}
		}
		boolean shortMatches = this.lookForShortMatches(barChartInfo, options);
		boolean hideLabels = false;
		boolean hideLegend = false;
		boolean hideWholeGraph = false;
		DefaultPieDataset dataset = new DefaultPieDataset();
		// int series = 0;
		for (DataSetContainer container : barChartInfo.getDataSets()) {
			for (Entry<String, Double> entry : container.dataSet.entrySet()) {
				String catName = entry.getKey();
				catName = this.stripHtml(catName);
				// if (entry.getValue() < 0.05) {
				// continue;
				// } else {
				catName = NumberFormat.getPercentInstance().format(entry.getValue()) + ": " + catName;
				// }
				this.currentLineNumbers = 1;
				if (((maxChar < this.lowCharLimit) && (options <= 6)) || ((maxChar < this.highCharLimit) && (options <= 4))) {
					dataset.setValue(catName, entry.getValue());
				} else if ((maxChar < this.highCharLimit) && (options <= 6)) {
					dataset.setValue(this.cutStrings(catName), entry.getValue());
				} else if ((maxChar > this.highCharLimit) && (options <= 6)) {
					if (shortMatches) {
						hideLabels = true;
						dataset.setValue(catName, entry.getValue());
					} else {
						// show strings as cut
						dataset.setValue(this.cutStrings(catName), entry.getValue());
					}
				} else if (((options > 4) && (options <= 10))) {
					if (shortMatches) {
						hideWholeGraph = true;
					} else {
						// show strings as cut
						dataset.setValue(this.cutStrings(catName), entry.getValue());
						hideLegend = true;
					}
				} else {
					// hide graph, PieChart on its limit.
					hideWholeGraph = true;
				}
				
			}
			// series++;
		}
		
		JFreeChart chart = ChartFactory.createPieChart(barChartInfo.getTitle(), dataset, true, true, false);
		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);
		
		// chart.setTextAntiAlias(true);
		chart.setAntiAlias(true);
		
		// get a reference to the plot for further customisation...
		
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setNoDataMessage("Cannot show the chart as the data is either empty or the question has too many or too long options.");
		plot.setBackgroundPaint(Color.white);
		plot.setOutlinePaint(Color.white);
		if (hideWholeGraph) {
			dataset = new DefaultPieDataset();
			plot.setDataset(dataset);
			this.hasData = false;
		} else {
			// plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
			plot.setCircular(true);
			plot.setLabelGap(0.04);
			plot.setLabelBackgroundPaint(Color.WHITE);
			plot.setMaximumLabelWidth(0.22);
			if (hideLabels) {
				plot.setLabelGenerator(null);
			}
			if (hideLegend) {
				chart.removeLegend();
			}
			PieRenderer pieRend = new PieRenderer(SccChart.COLORS);
			pieRend.setColor(plot, dataset);
		}
		
		return chart;
	}
	
	/**
	 * @param catName
	 * @return
	 */
	private String cutStrings(final String catName) {
		String temp = "";
		if (catName.length() > this.lowCharLimit) {
			temp = catName.substring(0, this.lowCharLimit - 3);
			temp = temp.concat("...");
		} else {
			temp = catName;
		}
		return temp;
	}
	
	/**
	 * @param catName
	 * @return
	 */
	private boolean lookForShortMatches(final BarChartInfo barChartInfo, final int options) {
		String[] temp = new String[options];
		int counter = 0;
		for (DataSetContainer container : barChartInfo.getDataSets()) {
			for (Entry<String, Double> entry : container.dataSet.entrySet()) {
				String catName = entry.getKey();
				if (catName.length() > this.lowCharLimit) {
					temp[counter] = catName.substring(0, this.lowCharLimit + 1);
				} else {
					temp[counter] = catName;
				}
				counter++;
			}
		}
		boolean matches = false;
		for (int i = 0; i < (temp.length - 1); i++) {
			String temp_2 = temp[i];
			for (int j = 0; j < (temp.length - i); j++) {
				if (temp_2.equals(temp[j]) && (i != j)) {
					matches = true;
					if (matches) {
						break;
					}
				}
			}
			if (matches) {
				break;
			}
		}
		return matches;
	}
	
	/**
	 * @param ca
	 * @return
	 */
	private String stripHtml(final String textWithHtml) {
		
		return textWithHtml.replaceAll("\\<[^\\>]*\\>", " ");
	}
	
	/**
	 * @param key
	 * @return
	 */
	private String wrap(String key, int position) {
		if (key.length() > position) {
			this.currentLineNumbers++;
			if (this.currentLineNumbers > this.maxLineNumbers) {
				this.maxLineNumbers = this.currentLineNumbers;
			}
			int index = 0;
			for (index = position - 1; index > 0; index--) {
				char c = key.charAt(index);
				if (c == ' ') {
					index++;
					break;
				}
			}
			
			if (index <= 0) {
				if (isNonBreakableCharacter(key.charAt(position))) {
					position++;
				}
				if (position < key.length()) {
					if (isBreakableCharacter(key.charAt(position))) {
						position++;
					}
				}
				key = key.substring(0, position) + "\n" + this.wrap(key.substring(position), position);
			} else {
				char c = key.charAt(index - 1);
				if (isNonBreakableCharacter(c)) {
					index++;
					if (isBreakableCharacter(key.charAt(index))) {
						index++;
					}
				}
				key = key.substring(0, index) + "\n" + this.wrap(key.substring(index), position);
			}
		}
		return key;
	}
	
	private boolean isNonBreakableCharacter(final char c) {
		return (c == '/') || (c == '-') || (c == '+') || (c == '.') || (c == ',') || (c == '(') || (c == ')') || (c == ';') || (c == ':') || (c == '_') || ((c == '"') || (c == '\'') || (c == '=') || (c == '?') || (c == '!') || (c == '*') || (c == '&') || (c == '%') || (c == '$') || (c == '§') || (c == '<') || (c == '>') || (c == '|') || (c == '°') || (c == '~'));
	}
	
	private boolean isBreakableCharacter(final char c) {
		return (c == ' ') || (c == '\t');
	}
}
