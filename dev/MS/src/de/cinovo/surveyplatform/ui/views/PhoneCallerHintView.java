/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.PhoneCallerHint;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.reporting.RepresentationMetadata;
import de.cinovo.surveyplatform.reporting.container.IReportDataContainer;
import de.cinovo.surveyplatform.util.PartsUtil;

/**
 * Copyright 2010 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class PhoneCallerHintView extends AbstractQuestionView {
	
	private PhoneCallerHint question;
	
	
	public PhoneCallerHintView(final PhoneCallerHint question) {
		super(question);
		this.question = question;
	}
	
	@Override
	public String getHTMLRepresentation(final TargetMedia targetMedia) {
		StringBuilder rep = new StringBuilder();
		if (question.getTextValue() != null) {
			rep.append("<div class=\"phoneCallerHint\">" + PartsUtil.getIcon("PHONE_BIG", "Hint for the phone interviewer") + "<b>Hint for the phone interviewer: </b>");
			rep.append(question.getTextValue());
			rep.append("<div style=\"clear: both;\"></div>");
			rep.append("</div>");
		}
		return rep.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.ui.views.AbstractQuestionView#
	 * getPrintableRepresentation()
	 */
	@Override
	public String getPrintableRepresentation(final boolean result) {
		return getHTMLRepresentation(TargetMedia.PRINTER_QUESTIONNAIRE);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.ui.views.AbstractQuestionView#
	 * getAggregatedPrintableRepresentation
	 * (java.lang.Object)
	 */
	@Override
	public String getAggregatedPrintableRepresentation(final IReportDataContainer dataContainer, final RepresentationMetadata representationMetadata, final AbstractQuestion currentQuestion) {
		return getHTMLRepresentation(representationMetadata.targetMedia);
	}
}