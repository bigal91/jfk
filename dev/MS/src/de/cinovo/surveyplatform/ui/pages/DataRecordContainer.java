/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.help.ContextHelpProvider;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.servlets.dal.ParticipantDal;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.SurveyUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class DataRecordContainer extends AbstractContainer {
	
	private final static String STATE_NEW = "stateNew";
	private final static String STATE_INVITED = "stateInvited";
	private final static String STATE_REMINDED = "stateReminded";
	private final static String STATE_SUBMITTED = "stateSubmitted";
	
	
	/* (non-Javadoc)
	 * @see de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet.http.HttpServletRequest, java.lang.StringBuilder, de.cinovo.surveyplatform.model.SystemUser)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		
		if (ParamUtil.checkAllParamsSet(request, SurveyDal.PARAM_SURVEYID)) {
			showSurveyParticipantsList(request, content, currentUser);
		} else {
			showActiveSurveyOverview(request, content, currentUser);
		}
		
	}
	
	private void showSurveyParticipantsList(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			int surveyID = ParamUtil.getSafeIntFromParam(request, SurveyDal.PARAM_SURVEYID);
			
			Survey survey = (Survey) hibSess.get(Survey.class, surveyID);
			
			content.append(PartsUtil.getPageHeader("Participants of " + survey.getName(), HelpIDs.PAGE_ADMINISTRATE_PARTICIPANTS));
			
			boolean altLeastOneParticipantMarkedForPhoneInterview = false;
			if (survey.getState() != SurveyState.SYSTEMTEMPLATE) {
				Query query = null;
				query = hibSess.createQuery(Queries.PARTICIPANT_BY_SURVEY);
				query.setParameter("1", surveyID);
				List<?> participantList = query.list();
				
				Map<String, String> replacements = new HashMap<String, String>();
				Map<String, String> rowReplacements = new HashMap<String, String>();
				
				StringBuilder tableRows = new StringBuilder();
				
				for (Object obj : participantList) {
					if (obj instanceof Participant) {
						Participant participant = (Participant) obj;
						
						String icon = "";
						String state = STATE_NEW;
						
						if (participant.getInvitationSent() != null) {
							rowReplacements.put("INVITED", TimeUtil.getLocalTime(currentUser, participant.getInvitationSent()));
							icon += PartsUtil.getIcon("EMAIL", "Participant has been invited");
							state = STATE_INVITED;
						} else {
							rowReplacements.put("INVITED", "");
						}
						if (participant.getReminderSent() != null) {
							rowReplacements.put("REMINDED", TimeUtil.getLocalTime(currentUser, participant.getReminderSent()));
							icon += PartsUtil.getIcon("EMAIL_ERROR", "Participant has been reminded");
							state = STATE_REMINDED;
						} else {
							rowReplacements.put("REMINDED", "");
						}
						if (participant.getSurveySubmitted() != null) {
							rowReplacements.put("SUBMITTED", TimeUtil.getLocalTime(currentUser, participant.getSurveySubmitted()));
							icon += PartsUtil.getIcon("ACCEPT", "Participant has submitted the questionnaire");
							state = STATE_SUBMITTED;
						} else {
							icon += PartsUtil.getIcon("ACCEPT invisible submittedIcon" + participant.getId(), "Participant has submitted the questionnaire");
							icon = "<a class=\"gui-icon-button-CONTROL_PLAY_BLUE startR" + participant.getId() + "\" title=\"Start recording data\" href=\"" + EnvironmentConfiguration.getUrlBase() + "/participate?pid=" + participant.getParticipation().getId() + "\" target=\"_blank\"></a>" + icon;
							rowReplacements.put("SUBMITTED", "");
						}
						
						if (participant.isEmailInQueue()) {
							icon += PartsUtil.getIcon("EMAIL_GO", "E-Mail is going to be sent. Please wait.");
						}
						
						if (participant.isAskByPhone()) {
							altLeastOneParticipantMarkedForPhoneInterview = true;
							if (survey.getState().equals(SurveyState.RUNNING)) {
								icon += PartsUtil.getIconLink("PHONE", "Click to start phone interview", "", participant.getParticipation() != null ? (EnvironmentConfiguration.getUrlBase() + "/participate?pid=" + participant.getParticipation().getId() + "&phoneInterview") : "");
							} else {
								icon += PartsUtil.getIcon("PHONE", "This participant is interviewed by phone");
							}
						}
						
						rowReplacements.put("NAME", "<a href=\"javascript:void(0);\" title=\"Show Details\" onclick=\"showParticipantInfo(" + participant.getId() + ")\"><span id=\"" + ParticipantDal.PARAM_NAME + participant.getId() + "\">" + participant.getName() + " " + participant.getSurname() + "</span></a>");
						
						rowReplacements.put("NUMBER", participant.getNumber() + "");
						rowReplacements.put("PARTICIPANTID", participant.getId() + "");
						rowReplacements.put("ICON", icon);
						rowReplacements.put("STATE", state);
						tableRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantTable_rowRecorder.html", rowReplacements));
					}
				}
				replacements.put("SURVEYID", surveyID + "");
				
				replacements.put("TEXT_EDITDISABLED", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantTable_textEditDisabled.html", null));
				
				replacements.put("ROWS", tableRows.toString());
				replacements.put("DEFAULTICON", "");
				replacements.put("SPECIALPROPERTIES", AdministrateParticipantsContainer.getSpecialPropertyRows());
				
				replacements.put("DLGPARTICIPANTINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgParticipantInfo.html", replacements));
				if (altLeastOneParticipantMarkedForPhoneInterview && (survey.getState().ordinal() >= SurveyState.CREATED.ordinal())) {
					replacements.put("BUTTON_DOWNLOADPHONELIST", "<button id=\"bDownloadPhonelist\" title=\"Click to download the list of participants that shall be interviewed by phone (PDF)\">Download Phonelist</button>");
				}
				
				// String actionTemplate = Paths.TEMPLATEPATH +
				// "/savePartOfSurveyAction.html";
				// StringStringPair kvpSurveyID = new
				// StringStringPair("SURVEYID", survey.getId() + "");
				// replacements.put("EDIT_EMAILSENDER_TEXTFIELD",
				// HtmlFormUtil.getInplaceEditableTextfield("eMailSender",
				// "eMailSender", survey.getEmailSender(), actionTemplate,
				// "e.g. john.smith@example.com", kvpSurveyID));
				// replacements.put("EDIT_SENDERNAME_TEXTFIELD",
				// HtmlFormUtil.getInplaceEditableTextfield("senderName",
				// "senderName", survey.getSenderName(), actionTemplate,
				// "e.g. John Smith", kvpSurveyID));
				
				// content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH +
				// "/dlgParticipantsCopy.html", replacements));
				content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantTableRecorder.html", replacements));
			} else {
				content.append("<div class=\"innerContainer\">The survey <b>" + survey.getName() + "</b> is a Systemtemplate. You cannot edit the participants of a Systemtemplate. You first have to create a template or a concrete survey from it.</div>");
			}
			tx.commit();
		} finally {
			hibSess.close();
		}
	}
	
	private void showActiveSurveyOverview(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_DATA_RECORD, HelpIDs.PAGE_DATA_RECORD));
		
		if (currentUser != null) {
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("SURVEYTABLE", getActiveSurveys(request, currentUser, replacements));
			
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/recordDataSurveyOverview.html", replacements));
		}
	}
	
	private String getActiveSurveys(final HttpServletRequest request, final SystemUser currentUser, final Map<String, String> replacements) {
		boolean atLeastOneSurveyExists = false;
		
		StringBuilder content = new StringBuilder();
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			
			Criteria criteria = hibSess.createCriteria(Survey.class);
			
			criteria.add(Restrictions.eq("deleted", false));
			criteria.add(Restrictions.or(Restrictions.eq("state", SurveyState.RUNNING), Restrictions.eq("state", SurveyState.CREATED)));
			criteria.addOrder(Order.asc("name"));
			
			Map<String, String> rowReplacements = new HashMap<String, String>();
			
			StringBuilder tableRowsRunning = new StringBuilder();
			
			List<?> list = criteria.list();
			Collection<UserGroup> visibleGroups = GroupManager.getVisibleGroups(hibSess, currentUser, currentUser);
			
			for (Object obj : list) {
				if (obj instanceof Survey) {
					Survey survey = (Survey) obj;
					UserGroup owner = survey.getOwner();
					if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR) || visibleGroups.contains(owner)) {
						atLeastOneSurveyExists = true;
						rowReplacements.put("NAME", "<td><a href=\"javascript:void(0);\" onclick=\"showsurveyInfo(" + survey.getId() + ");\"><span id=\"" + SurveyDal.PARAM_SURVEYNAME + survey.getId() + "\">" + survey.getName() + "</span></a></td>");
						rowReplacements.put("OWNER", "<td>" + (owner == null ? "---" : owner.getName()) + "</td>");
						
						// rowReplacements.put("CREATED", "<td><span class=\"invisible\">" + survey.getCreationDate().getTime() + "</span>"
						// + TimeUtil.getLocalTime(currentUser, survey.getCreationDate()) + "</td>");
						rowReplacements.put("STATE", "<td>" + survey.getStateDisplayname() + "</td>");
						rowReplacements.put("HIGHLIGHT", "<td></td>");
						
						int progress = 0;
						progress = SurveyUtil.calculateReturnRate(currentUser, survey);
						rowReplacements.put("PROGRESS", "<td>" + PartsUtil.getProgressBar(progress) + "</td>");
						
						rowReplacements.put("SURVEYID", survey.getId() + "");
						
						StringBuilder buttons = new StringBuilder();
						buttons.append(PartsUtil.getIconLink("CONTROL_PLAY_BLUE", "Record Data", "", "?page=" + Pages.PAGE_DATA_RECORD + "&surveyID=" + survey.getId()));
						
						rowReplacements.put("BUTTONS", buttons.toString());
						rowReplacements.put("WIDTH", "30");
						
						tableRowsRunning.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_row.html", rowReplacements));
					}
				}
			}
			replacements.put("RUNNINGSTR", SurveyState.getDisplayName(SurveyState.RUNNING));
			
			replacements.put("BUTTONCOLUMNWIDTH", (50) + "");
			replacements.put("DIALOGSURVEYINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfo.html", replacements));
			
			if (tableRowsRunning.length() > 0) {
				replacements.put("SECTIONHEADER", "Active Surveys");
				replacements.put("HELPLINK", ContextHelpProvider.getInstance().getHelpLink("table.running", "Click to get detailed info", "", ""));
				replacements.put("ROWS_RUNNING", tableRowsRunning.toString());
				replacements.put("SHOW_ALL_STYLE", ",\"sDom\": 'fprt<\"bottom\"i>'");
				replacements.put("TABLE_RUNNING_SURVEYS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_running.html", replacements));
			}
			
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable.html", replacements));
			tx.commit();
		} finally {
			hibSess.close();
		}
		
		if (atLeastOneSurveyExists) {
			replacements.put("INFO_SURVEYEXISTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/recordDataInfo.html", replacements));
		} else {
			if (EnvironmentConfiguration.isOfflineMode()) {
				replacements.put("INFO_SURVEYEXISTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/recordDataFirstImportInfo.html", replacements));
			} else {
				replacements.put("INFO_SURVEYEXISTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/recordDataNoSurveysInfo.html", replacements));
			}
		}
		
		return content.toString();
	}
	
}
