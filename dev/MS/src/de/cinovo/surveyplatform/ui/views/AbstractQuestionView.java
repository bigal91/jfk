/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jfree.chart.plot.PlotOrientation;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.chart.SccChart;
import de.cinovo.surveyplatform.constants.ChartType;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement;
import de.cinovo.surveyplatform.model.chart.BarChartInfo;
import de.cinovo.surveyplatform.model.chart.DataSetContainer;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.ComboQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.RadioMatrixQuestion;
import de.cinovo.surveyplatform.model.question.RadioQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.ExcelUtil.CellPointer;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil;
import de.cinovo.surveyplatform.util.WikiUtil;


/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public abstract class AbstractQuestionView implements IQuestionView {
	
	protected AbstractQuestion question;
	
	private final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#0.00");
	
	
	public enum OutputMode {
		PERCENT, ABSOLUTE
	}
	
	public AbstractQuestionView(final AbstractQuestion question) {
		this.question = question;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.ui.views.IQuestionView#getHTMLRepresentation()
	 */
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		return "No HTML Representation available!";
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.ui.views.IQuestionView#getPrintableRepresentation
	 * ()
	 */
	@Override
	public String getPrintableRepresentation(final boolean result) {
		return "No printable representation available!";
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.ui.views.IQuestionView#getAggregatedExcelRepresentation(de.cinovo.surveyplatform.reporting.container.
	 * IReportDataContainer, de.cinovo.surveyplatform.reporting.RepresentationMetadata,
	 * de.cinovo.surveyplatform.model.question.AbstractQuestion)
	 */
	@Override
	public void getAggregatedExcelRepresentation(final IReportDataContainer dataContainer, final RepresentationMetadata representationMetadata, final Sheet sheet) {
		{
			Row row = sheet.createRow(sheet.getPhysicalNumberOfRows() + 2);
			Cell cell = row.createCell(0);
			cell.setCellValue(WikiUtil.parseToHtml(question.getQuestion()));
		}
		{
			Row row = sheet.createRow(sheet.getPhysicalNumberOfRows() + 1);
			Cell cell = row.createCell(0);
			cell.setCellValue("No excel representation available for the questiontype " + this.getClass().getSimpleName() + "!");
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.ui.views.IQuestionView#
	 * getAggregatedPrintableRepresentation(java.
	 * lang.Object)
	 */
	@Override
	public String getAggregatedPrintableRepresentation(final IReportDataContainer dataContainer, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion) {
		return "No printable representation available!";
	}
	
	
	protected String getDefaultChart(final String filePath, final int surveyID, final List<Integer> numberOfResponses, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion, final DataSetContainer... dataSets) {
		BarChartInfo barChartInfo = new BarChartInfo();
		BarChartInfo bigbarChartInfo = new BarChartInfo();
		barChartInfo.setTitle("");
		barChartInfo.setxAxisLabel("");
		barChartInfo.setyAxisLabel("");
		barChartInfo.setOrientation(PlotOrientation.VERTICAL);
		bigbarChartInfo.setTitle("");
		bigbarChartInfo.setxAxisLabel("");
		bigbarChartInfo.setyAxisLabel("");
		bigbarChartInfo.setOrientation(PlotOrientation.VERTICAL);
		if (dataSets.length > 0) {
			for (DataSetContainer dataSet : dataSets) {
				barChartInfo.addDataSet(dataSet);
				bigbarChartInfo.addDataSet(new DataSetContainer(dataSet.name, dataSet.dataSet));
			}
			barChartInfo.setNumberOfResponses(numberOfResponses);
			barChartInfo.setHeight(200);
			bigbarChartInfo.setNumberOfResponses(numberOfResponses);
			int width = 50 + (int) Math.round(120 * dataSets.length * dataSets[0].dataSet.size() * Math.pow(0.90, dataSets.length));
			barChartInfo.setWidth(width > SccChart.MAX_WIDTH ? SccChart.MAX_WIDTH : width);
			barChartInfo.convertToPercentage();
			bigbarChartInfo.convertToPercentage();
			
			bigbarChartInfo.setWidth(width * 2);
			bigbarChartInfo.setHeight(barChartInfo.getHeight() * 2);
			
			if (SccChart.OUTPUTTYPE.equals("PNG")) {
				String ChartContentLinkAdd = "<a href=\"" + EnvironmentConfiguration.getUrlBase() + "/download?type=graph&surveyID=" + surveyID + "&filePath=" + Paths.CHARTS + "/" + filePath + ".png\">";
				
				ChartType chartType = QuestionnaireViewUtil.getChartType(currentQuestion);
				if (representationMetadata.targetMedia.equals(TargetMedia.SCREEN)) {
					return "<div style=\"text-align: center; margin: 10px;\">" + ChartContentLinkAdd + "<img title=\"Save or Copy this graph into any Document: Right-Click / Copy... Paste\" src=\"" + (new SccChart().createChart(filePath, chartType, barChartInfo)) + "?" + System.currentTimeMillis() + "\" /></a></div>";
				} else {
					return "<div style=\"text-align: center; margin: 10px;\"><img title=\"Save or Copy this graph into any Document: Right-Click / Copy... Paste\" src=\"" + (new SccChart().createChart(filePath, chartType, barChartInfo)) + "\" /></div>";
				}
				
			} else {
				return "";
			}
		} else {
			return "";
		}
		
	}
	
	/**
	 * Gets an OptionLogicScript for Radio- and
	 * MultipleChoice Questions, by reading their Logic-Elements
	 * 
	 * @param option the current option to put script in
	 * @return String the OptionScript
	 */
	protected StringBuilder getOptionScriptWithoutEventListener(final Option option) {
		
		List<QuestionnaireLogicElement> logicElements = question.getLogicElements();
		Set<String> openParts = new HashSet<String>();
		Set<String> closeParts = new HashSet<String>();
		
		final String currentOption = option.getDisplayName().toLowerCase();
		for (QuestionnaireLogicElement logicElement : logicElements) {
			
			String partTypePrefix = logicElement.getTypeOfPart();
			partTypePrefix = partTypePrefix.substring(partTypePrefix.lastIndexOf(".") + 1).toLowerCase();
			
			String partId = String.valueOf(logicElement.getIdOfPart());
			String partInfo = partTypePrefix + partId;
			if (!partTypePrefix.equals("page")) {
				for (Answer answer : logicElement.getAnswers()) {
					final String selectedOption = answer.getAnswer().toLowerCase();
					
					if (selectedOption.equals(currentOption)) {
						openParts.add(partInfo);
						if (closeParts.contains(partInfo)) {
							closeParts.remove(partInfo);
						}
					} else {
						if (!openParts.contains(partInfo)) {
							closeParts.add(partTypePrefix + partId);
						}
					}
				}
			}
		}
		
		
		if (((question instanceof RadioMatrixQuestion) || (question instanceof RadioQuestion) || (question instanceof ComboQuestion))) {
			StringBuilder script = new StringBuilder();
			for (String partInfo :openParts) {
				if (partInfo.startsWith("section")) {
					script.append("$('.lid").append(partInfo).append("').find('.question').show();");
				}
				script.append("$('.lid").append(partInfo).append(" .invisible').show(); $('.lid").append(partInfo).append("').slideDown();");
			}
			for (String partInfo :closeParts) {
				script.append("$('.lid").append(partInfo).append("').slideUp();");
			}
			
			return script;
		} else {
			
			StringBuilder script = new StringBuilder();
			script.append("if ($(':animated').size() > 0){ return false; } else {");
			script.append("if ($('.checkbox" + question.getId() + "." + AuthUtil.md5(option.getDisplayName()) + "').attr('checked')) {");
			for (String partInfo : openParts) {
				script.append("_override_" + partInfo + " += 1; ");
				if (partInfo.startsWith("section")) {
					script.append("$('.lid").append(partInfo).append("').find('.question').show();");
				}
				script.append("  $('.lid").append(partInfo).append("').slideDown();");
				
			}
			script.append("} else {");
			for (String partInfo : openParts) {
				script.append("_override_" + partInfo + " -= 1; ");
				script.append("if (_override_" + partInfo + " <= 0) {");
				// only slideup, when no override
				script.append("_override_" + partInfo + " = 0; ");
				script.append("$('.lid").append(partInfo).append("').slideUp();");
				script.append("}");
			}
			script.append("}"); // end of "if checkbox=checked then ... else ..."
			script.append("}"); // end of "if animated then ... else ..."
			
			// disable checkbox for a moment, to avoid multiple clicks
			// script.append("var check = $(this); check.attr('disabled', 'disabled');  window.setTimeout(function() { check.removeAttr('disabled'); }, 200);");
			
			return script;
		}
		
		
	}
	
	protected StringBuilder getOptionScript(final Option option) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("onclick=\"");
		stringBuilder.append(getOptionScriptWithoutEventListener(option));
		stringBuilder.append("\"");
		return stringBuilder;
	}
	
	protected void startSlideToggleContainer(final StringBuilder content, final String uuid) {
		content.append("<div style=\"display:none; margin: 0px 5px 0px 5px;\" class=\"c" + uuid + "\">");
	}
	
	protected void endSlideToggleFragment(final StringBuilder content) {
		content.append("</div>");
	}
	
	protected StringBuilder getAdditionalInfoHtml(final AbstractQuestion question, final TargetMedia targetMedia) {
		StringBuilder addInfoHtml = new StringBuilder();
		if (TargetMedia.PRINTER_REPORT.equals(targetMedia)) {
			return addInfoHtml;
		}
		String addedInfo = question.getAdditionalInfo();
		if ((addedInfo == null) || (addedInfo.equals(""))) {
			// no info shall be shown
		} else {
			addInfoHtml.append("<div class=\"additionalInfoContainer\"><div>");
			addInfoHtml.append("<div");
			if (TargetMedia.SCREEN.equals(targetMedia)) {
				addInfoHtml.append(" style=\"display:none;\"");
			}
			addInfoHtml.append(" class=\"addInfo" + question.getId() + "\">");
			addInfoHtml.append(addedInfo);
			addInfoHtml.append("</div></div></div>");
		}
		return addInfoHtml;
	}
	
	protected StringBuilder getAdditionalInfoButton(final AbstractQuestion question, final TargetMedia targetMedia) {
		StringBuilder addInfoHtml = new StringBuilder();
		if (TargetMedia.SCREEN.equals(targetMedia)) {
			String addedInfo = question.getAdditionalInfo();
			if ((addedInfo == null) || (addedInfo.equals(""))) {
				// no info shall be shown
			} else {
				addInfoHtml.append("&nbsp;<span onClick=\"$('.addInfo" + question.getId() + "').slideToggle('fast');$('.saic" + question.getId() + "').toggle();\" title=\"View Explanation\" class=\"gui-icon-HELP additionalInfoIcon\" style=\"cursor: pointer;\" href=\"javascript:void(0);\"></span>&nbsp;");
			}
		}
		return addInfoHtml;
	}
	
	protected StringBuilder getQuestionBlock(final AbstractQuestion question, final TargetMedia targetMedia) {
		StringBuilder questionBlock = new StringBuilder();
		if (TargetMedia.SCREEN.equals(targetMedia)) {
			questionBlock.append(QuestionnaireViewUtil.getInitOverrideLogicScript("abstractquestion", String.valueOf(question.getLocalId())));
		}
		
		questionBlock.append("<p class=\"questionText\">");
		questionBlock.append(WikiUtil.parseToHtml(question.getQuestion(), true));
		questionBlock.append(getAdditionalInfoButton(question, targetMedia));
		questionBlock.append("</p>");
		
		return questionBlock;
	}
	
	protected StringBuilder getQuestionInline(final AbstractQuestion question, final TargetMedia targetMedia) {
		StringBuilder questionInline = new StringBuilder();
		if (TargetMedia.SCREEN.equals(targetMedia)) {
			questionInline.append(QuestionnaireViewUtil.getInitOverrideLogicScript("abstractquestion", String.valueOf(question.getLocalId())));
		}
		questionInline.append("<span class=\"questionText\" style=\"margin: 0px 5px 0px 0px;\">");
		questionInline.append(WikiUtil.parseToHtml(question.getQuestion(), true));
		questionInline.append(getAdditionalInfoButton(question, targetMedia));
		questionInline.append("</span>");
		return questionInline;
	}
	
	protected String postProcess(String input) {
		// adds image functionality
		input = input.replaceAll("image\\:([A-Za-z0-9/\\:\\=\\?\\&\\.]*)", "<img src=\"$1\" />");
		return input;
	}
	
	protected String getPercentage(final double count, final int numberOfResponses) {
		return getPercentage(count, numberOfResponses, true, " ", true);
	}
	
	protected String getPercentage(final double count, final int numberOfResponses, final boolean withAbsoluteValue) {
		return getPercentage(count, numberOfResponses, withAbsoluteValue, " ", true);
	}
	
	protected String getPercentage(final double count, final int numberOfResponses, final boolean withAbsoluteValue, final boolean withPercentSymbol) {
		return getPercentage(count, numberOfResponses, withAbsoluteValue, " ", withPercentSymbol);
	}
	
	protected String getPercentage(final double count, final int numberOfResponses, final boolean withAbsoluteValue, final String breakString, final boolean withPercentSymbol) {
		double percentage = 0;
		if (numberOfResponses != 0) {
			percentage = (count / numberOfResponses) * 100;
		}
		
		if (withAbsoluteValue) {
			int value = (int) Math.round(count);
			return PERCENTAGE_FORMAT.format(percentage) + (withPercentSymbol ? "%" : "") + breakString + "(" + value + ")";
		}
		return PERCENTAGE_FORMAT.format(percentage) + (withPercentSymbol ? "%" : "");
	}
	
	protected void writeQuestionToCell(final CellPointer cp) {
		Cell currentCell = cp.currentCell();
		Workbook workbook = currentCell.getSheet().getWorkbook();
		{
			CellStyle style = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setItalic(true);
			style.setFont(font);
			currentCell.setCellStyle(style);
			currentCell.setCellValue(question.getAlias());
		}
		currentCell = cp.nextRow();
		{
			CellStyle style = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			style.setFont(font);
			currentCell.setCellStyle(style);
			currentCell.setCellValue(question.getQuestion());
		}
		
	}
	
}
