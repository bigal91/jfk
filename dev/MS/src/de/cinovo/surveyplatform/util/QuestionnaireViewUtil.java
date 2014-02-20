package de.cinovo.surveyplatform.util;

import java.util.HashMap;
import java.util.Map;

import de.cinovo.surveyplatform.constants.ChartType;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.ILogicApplicableQuestion;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.question.IMultipleOptionsQuestion;
import de.cinovo.surveyplatform.model.question.PhoneCallerHint;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class QuestionnaireViewUtil {
	
	public enum QuestionType {
		QUALITATIVE, QUANTITATIVE, NONE, ALL
	}
	
	
	public static String getQuestionHTMLRepresentation(final AbstractQuestion question, final boolean editable, final boolean showCopyMoveControls) {
		StringBuilder questionView = new StringBuilder();
		
		// controls for the question
		if (editable) {
			final Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("ID", question.getId() + "");
			replacements.put("LOCALID", question.getLocalId() + "");
			replacements.put("PART", SurveyDal.PARTNAME_QUESTION);
			replacements.put("VISIBILITY", Boolean.toString(!question.isVisible()));
			
			questionView.append("<div class=\"controlBoxWrapper\"><div class=\"controlBox\"><span class=\"controlBoxCaption\">Edit Question " + question.getLocalId() + ":</span>");
			
			questionView.append(PartsUtil.getIconButton("PAGE_WHITE_EDIT editQuestionButton", "Edit Question", "beq" + question.getId(), ""));
			
			if ((question instanceof PhoneCallerHint)) {
				questionView.append(PartsUtil.getIcon("PHONE", "This is a hint for a phone caller"));
			} else {
				String scriptChangeVisibility = TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/makeQuestionVisible.js", replacements);
				if (showCopyMoveControls) {
					questionView.append(PartsUtil.getIconButton("EYE", "Change Visibility", "bmi" + question.getId(), scriptChangeVisibility));
				}
			}
			
			if (showCopyMoveControls) {
				questionView.append(QuestionnaireViewUtil.getCopyMoveIcons(question.getId(), "Question"));
			}
			
			// Logic-Button-Visibility
			if (question instanceof ILogicApplicableQuestion) {
				String grayStrIfQuestionHasLogic = "_GRAY";
				if ((question.getLogicElements() != null) && (question.getLogicElements().size() > 0)) {
					grayStrIfQuestionHasLogic = "";
				}
				questionView.append(PartsUtil.getIconButton("ARROW_BRANCH" + grayStrIfQuestionHasLogic + " addLogicButton", "Add Logic", "bal" + question.getId(), "", question.getLocalId() + ""));
			}
			
			String scriptRemove = TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/removePartScript.js", replacements);
			
			if (showCopyMoveControls) {
				questionView.append(PartsUtil.getIconButton("BIN_EMPTY", "Remove", "brq" + question.getId(), scriptRemove));
			}
			
			questionView.append("</div>");
			questionView.append("<div class=\"questionAlias\">" + (question.getAlias() == null ? "" : question.getAlias()) + "</div>");
			questionView.append("</div>");
		}
		
		questionView.append("<div style=\"clear: both;\"></div>");
		
		// html representation of the question
		questionView.append(SurveyElementFactory.getInstance().createQuestionViewObject(question).getHTMLRepresentation(TargetMedia.SCREEN));
		return questionView.toString();
	}
	
	public static QuestionType getQuestionType(final AbstractQuestion question) {
		if (question instanceof TextPart) {
			return QuestionType.NONE;
		} else if (question instanceof PhoneCallerHint) {
			return QuestionType.NONE;
		} else if (question instanceof FreeTextQuestion) {
			return QuestionType.QUALITATIVE;
		} else if (question instanceof IMultipleOptionsQuestion) {
			return QuestionType.QUANTITATIVE;
		} else if (question instanceof SingleLineQuestion) {
			return QuestionType.QUALITATIVE;
		} else if (question instanceof AverageNumberQuestion) {
			return QuestionType.QUALITATIVE;
		} else if (question instanceof TextfieldQuestion) {
			return QuestionType.QUALITATIVE;
		}
		return QuestionType.NONE;
	}
	
	public static ChartType getChartType(final AbstractQuestion question) {
		ChartType chartType = question.getChartType();
		if (chartType == null) {
			chartType = ChartType.bar;
		}
		return chartType;
	}
	
	/**
	 * @param string
	 * @return
	 */
	private static String getCopyMoveIcons(final int partId, final String partType) {
		final Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("ID", partId + "");
		replacements.put("PART", partType);
		String scriptCopyPart = TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/copyPart.js", replacements);
		String scriptMovePart = TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/movePart.js", replacements);
		StringBuilder sb = new StringBuilder();
		if (partType.equals("Question")) {
			sb.append(PartsUtil.getIconButton("PAGE_COPY", "Copy", "cpi_q" + partId, scriptCopyPart));
			sb.append(PartsUtil.getIconButton("MOVE", "Move", "mpi_q" + partId, scriptMovePart));
		} else if (partType.equals("Section")) {
			sb.append(PartsUtil.getIconButton("PAGE_COPY", "Copy", "cpi_s" + partId, scriptCopyPart));
			sb.append(PartsUtil.getIconButton("MOVE", "Move", "mpi_s" + partId, scriptMovePart));
		}
		return sb.toString();
	}
	
	public static String getPageControls(final Page page, final String navigation, final boolean showCopyMoveControls, final boolean showRemoveControls) {
		final Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("ID", page.getId() + "");
		replacements.put("LOCALID", page.getLocalId() + "");
		replacements.put("PART", SurveyDal.PARTNAME_PAGE);
		String scriptRemove = TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/removePartScript.js", replacements);
		// String enableSorting = TemplateUtil.getTemplate(Paths.TEMPLATEPATH +
		// "/enableSorting.js", replacements);
		
		StringBuilder pageControls = new StringBuilder();
		pageControls.append("<div class=\"controlBoxWrapper\">" + navigation);
		if (showCopyMoveControls || showRemoveControls) {
			pageControls.append("<div class=\"controlBox\"><span class=\"controlBoxCaption\">Edit Page " + page.getLocalId() + ":</span>");
		}
		// pageControls.append(PartsUtil.getIconButton("MOVE",
		// "Click to sort the Parts of this Page", "sqp" + page.getId(),
		// enableSorting));
		if (showCopyMoveControls) {
			pageControls.append(QuestionnaireViewUtil.getCopyMoveIcons(page.getId(), "Page"));
		}
		if (showRemoveControls) {
			pageControls.append(PartsUtil.getIconButton("BIN_EMPTY", "Remove", "brp" + page.getId(), scriptRemove));
		}
		if (showCopyMoveControls || showRemoveControls) {
			pageControls.append("</div>");
		}
		pageControls.append("</div>");
		
		return pageControls.toString();
	}
	
	public static String getSectionControls(final Section section, final boolean readonly) {
		final Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("ID", section.getId() + "");
		replacements.put("LOCALID", section.getLocalId() + "");
		replacements.put("PART", SurveyDal.PARTNAME_SECTION);
		String scriptRemove = TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/removePartScript.js", replacements);
		String scriptEditTitle = TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/openEditSectionTitleDialog.js", replacements);
		
		StringBuilder sectionControls = new StringBuilder();
		sectionControls.append("<div class=\"controlBoxWrapper\"><div class=\"controlBox\"><span class=\"controlBoxCaption\">Edit Section " + section.getLocalId() + ":</span>");
		sectionControls.append(PartsUtil.getIconButton("PAGE_WHITE_EDIT", "Edit Section Title", "est" + section.getId(), scriptEditTitle));
		if (!readonly) {
			sectionControls.append(QuestionnaireViewUtil.getCopyMoveIcons(section.getId(), "Section"));
			sectionControls.append(PartsUtil.getIconButton("BIN_EMPTY", "Remove", "brs" + section.getId(), scriptRemove));
		}
		sectionControls.append("</div></div>");
		
		return sectionControls.toString();
	}
	
	public static String getInitOverrideLogicScript(final String type, final String localId) {
		StringBuilder script = new StringBuilder();
		script.append("<script type=\"text/javascript\">");
		script.append("/* <![CDATA[*/ ");
		script.append("var _override_" + type + localId + " = 0;");
		script.append("/* ]]> */ ");
		script.append("</script> ");
		return script.toString();
	}
	
	public static String formatLineBreaks(final String text) {
		return text.replaceAll("\n\r|\r\n|\r|\n", "<br>");
	}
	
}
