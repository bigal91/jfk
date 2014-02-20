/**
 *
 */
package de.cinovo.surveyplatform.reporting.generator;

import java.util.HashMap;
import java.util.Map;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.WikiUtil;

/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class ReportGeneratorUtils {
	
	static final int MAXROWS_WITH_GRAPHICAL_REPRESENTATION = 3;
	static final int MAXOPTIONS_WITH_GRAPHICAL_REPRESENTATION = 7;
	
	private ReportGeneratorUtils() {
		// -
	}
	
	public static boolean isSlideToggleFragmentUsedByRows(final int yCount, final TargetMedia targetMedia) {
		return (yCount <= MAXROWS_WITH_GRAPHICAL_REPRESENTATION) && targetMedia.equals(TargetMedia.SCREEN);
	}
	
	public static boolean isSlideToggleFragmentUsedByOptions(final int optionCount, final TargetMedia targetMedia) {
		return (optionCount <= MAXOPTIONS_WITH_GRAPHICAL_REPRESENTATION) && targetMedia.equals(TargetMedia.SCREEN);
	}
	
	public static void addSlideToggleButton(final StringBuilder content, final String uuid) {
		content.append("<div style=\"float:right; margin: 3px 0px 3px 0px; width:140px\"><a style=\"color: #86B9E4;\" class=\"link underlinedLink\" href=\"javascript:void(0);\" onclick=\"$('.c" + uuid + "').slideToggle(); $('#show" + uuid + "').toggle(); $('#hide" + uuid + "').toggle();\">" + " <span style=\"color: #86B9E4;\" id=\"show" + uuid + "\" class=\"link underlinedLink\">Show</span><span id=\"hide" + uuid + "\" style=\"display:none; color: #86B9E4;\" class=\"link underlinedLink\">Hide</span> Descriptive Table</a></div><div class=\"clear\"></div>");
	}
	
	public static void addSimpleInfoButton(final StringBuilder content) {
		// content.append("<span style=\"float:right; margin: 3px\"><a class=\"link\" href=\"javascript:void(0);\" title =\" Save or Copy this chart into any Document: Right-Click / Copy... Paste\">"
		// + PartsUtil.getIcon("HELP", "") + "</a></span>");
	}
	
	public static void addGraphDownloadButton(final StringBuilder content, final String graphPath, final int surveyID) {
		content.append("<a class=\"button smallButton\" href=\"" + EnvironmentConfiguration.getUrlBase() + "/download?type=graph&surveyID=" + surveyID + "&filePath=" + Paths.CHARTS + "/" + graphPath + ".png\"  title=\"Download the chart\">Download Chart</a>");
		
	}
	
	public static void addTableDownloadButton(final StringBuilder content, final String uuid, final String titleOfQuestion) {
		String temp;
		temp = titleOfQuestion.substring(29);
		temp = temp.replaceFirst("</p></div>", "");
		
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("UUIDREPLACED", uuid.replace("-", ""));
		replacements.put("TEMPTITLE", temp);
		replacements.put("UUID", uuid);
		replacements.put("TABLEICON", PartsUtil.getIcon("TABLE_GO", ""));
		
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_titleConverter.html", replacements));
	}
	
	public static void addDownloadExcelLink(final StringBuilder content, final int surveyID, final AbstractQuestion question) {
		content.append("<div style=\"text-align: right; margin: 5px 5px 5px 0px;\"><a class=\"link underlinedLink\" style=\"color: #86B9E4\" href=\"" + EnvironmentConfiguration.getUrlBase() + "/download?type=graph&surveyID=" + surveyID + "&filePath=raw/" + surveyID + "/" + question.getId() + ".xlsx\">Export to MS-Excel&reg;</a></div>");
	}
	
	public static void addViewGraphButton(final StringBuilder content, final String graphPath) {
		content.append("<a class=\"button smallButton\" href=\"" + EnvironmentConfiguration.getUrlBase() + Paths.CHARTS + "/" + graphPath + "_xl.png\"  target=\"_blank\" title=\"View Chart Only\">View Chart Only</a>");
	}
	
	public static String getQuestionText(final AbstractQuestion question) {
		return "<div class=\"questionText\">" + WikiUtil.parseToHtml(question.getQuestion()) + "</div>";
	}
	
	public static String printNumberOfResponses(final int number, final String detail) {
		return "<div style=\"margin-top: 10px;\">n" + (((detail == null) || detail.equals("")) ? "" : " (" + detail + ")") + " = " + number + "</div>";
	}
	
	public static String printNumberOfResponses(final int number) {
		return "<div style=\"margin-top: 10px;\">n = " + number + "</div>";
	}
	
	public static String printAggregationpartContainerTitle(final String title) {
		return "<p class=\"aggregationContainerTitle\">" + title + "</p>";
	}
}
