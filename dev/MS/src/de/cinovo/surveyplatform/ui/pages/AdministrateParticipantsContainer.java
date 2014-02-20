package de.cinovo.surveyplatform.ui.pages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.PaymentModel;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.servlets.dal.ParticipantDal;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.PropertyViewUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class AdministrateParticipantsContainer extends AbstractContainer {
	
	private final SurveyListContainer surveyListContainer = new SurveyListContainer();
	
	private final static String STATE_NEW = "stateNew";
	private final static String STATE_INVITED = "stateInvited";
	private final static String STATE_REMINDED = "stateReminded";
	private final static String STATE_SUBMITTED = "stateSubmitted";
	
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		if ((currentUser != null) && ParamUtil.checkAllParamsSet(request, SurveyDal.PARAM_SURVEYID)) {
			
			if (AuthUtil.isAllowedToManageParticipants(currentUser)) {
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					int surveyID = ParamUtil.getSafeIntFromParam(request, SurveyDal.PARAM_SURVEYID);
					
					Survey survey = (Survey) hibSess.get(Survey.class, surveyID);
					
					if (survey != null) {
						if (AuthUtil.isAllowedToManageParticipants(currentUser, survey, hibSess)) {
							
							content.append(PartsUtil.getPageHeader("Participants of " + survey.getName(), HelpIDs.PAGE_ADMINISTRATE_PARTICIPANTS, new String[] {"<a href=\"?page=" + Pages.PAGE_MANAGE_SURVEYS + "\">" + Pages.PAGE_HEADER_MANAGE_SURVEYS + "</a>"}));
							
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
										
										String icon = ""; // PartsUtil.getIcon("new",
										// "New");
										String state = STATE_NEW;
										
										if (participant.getInvitationSent() != null) {
											rowReplacements.put("INVITED", TimeUtil.getLocalTime(currentUser, participant.getInvitationSent()));
											icon = PartsUtil.getIcon("EMAIL", "Participant has been invited");
											state = STATE_INVITED;
										} else {
											rowReplacements.put("INVITED", "");
										}
										if (participant.getReminderSent() != null) {
											rowReplacements.put("REMINDED", TimeUtil.getLocalTime(currentUser, participant.getReminderSent()));
											icon = PartsUtil.getIcon("EMAIL_ERROR", "Participant has been reminded");
											state = STATE_REMINDED;
										} else {
											rowReplacements.put("REMINDED", "");
										}
										if (participant.getSurveySubmitted() != null) {
											rowReplacements.put("SUBMITTED", TimeUtil.getLocalTime(currentUser, participant.getSurveySubmitted()));
											icon = PartsUtil.getIcon("ACCEPT", "Participant has submitted the questionnaire");
											state = STATE_SUBMITTED;
										} else {
											rowReplacements.put("SUBMITTED", "");
										}
										
										if (participant.isEmailInQueue()) {
											icon = PartsUtil.getIcon("EMAIL_GO", "E-Mail is going to be sent. Please wait.");
										}
										
										if (participant.isAskByPhone()) {
											altLeastOneParticipantMarkedForPhoneInterview = true;
											if (survey.getState().equals(SurveyState.RUNNING)) {
												icon += PartsUtil.getIconLink("PHONE", "Click to start phone interview", "", participant.getParticipation() != null ? (EnvironmentConfiguration.getUrlBase() + "/participate?pid=" + participant.getParticipation().getId() + "&phoneInterview") : "");
											} else {
												icon += PartsUtil.getIcon("PHONE", "This participant is interviewed by phone");
											}
										}
										
										rowReplacements.put("NAME", getInfoLink(participant, participant.getName() + " " + participant.getSurname(), ParticipantDal.PARAM_NAME));
										rowReplacements.put("NUMBER", participant.getNumber() + "");
										// rowReplacements.put("EMAIL",
										// getInfoLink(participant,
										// participant.getContactEmail(),
										// ParticipantDal.PARAM_EMAIL));
										rowReplacements.put("PARTICIPANTID", participant.getId() + "");
										rowReplacements.put("ICON", icon);
										rowReplacements.put("STATE", state);
										tableRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantTable_row.html", rowReplacements));
									}
								}
								tx.commit();
								replacements.put("SURVEYID", surveyID + "");
								
								if (survey.getState() != SurveyState.CLOSED) {
									StringBuilder commands = new StringBuilder();
									commands.append("<select id=\"commandSelector\" class=\"commandSelector\">");
									commands.append("<option value=\"\">-- Select a command--</option>");
									StringBuilder selectOptions = new StringBuilder();
									selectOptions.append("Select:&nbsp;");
									selectOptions.append("<a class=\"underlinedLink\" style=\"margin-right: 10px;\" onclick=\"$('.stateNew').attr('checked', 'checked');\">New</a>");
									if (!(survey.getState().equals(SurveyState.TEMPLATE) || survey.getState().equals(SurveyState.SYSTEMTEMPLATE))) {
										commands.append("<option value=\"" + ParticipantDal.COMMAND_INVITE + "\">Invite selected</option>");
										commands.append("<option value=\"" + ParticipantDal.COMMAND_REMIND + "\">Remind selected</option>");
										selectOptions.append("<a class=\"underlinedLink gui-icon-EMAIL\" style=\"margin-right: 10px;\" onclick=\"$('.stateInvited').attr('checked', 'checked');\">Invited</a>");
										selectOptions.append("<a class=\"underlinedLink gui-icon-EMAIL_ERROR\" style=\"margin-right: 10px;\" onclick=\"$('.stateReminded').attr('checked', 'checked');\">Reminded</a>");
									}
									commands.append("<option value=\"" + ParticipantDal.COMMAND_MARKASPHONE + "\">Mark selected as phone interview</option>");
									if (survey.getState() == SurveyState.RUNNING) {
										commands.append("<option value=\"export\">Export for offline recording</option>");
									}
									commands.append("<option value=\"delete\">Remove selected</option>");
									// if
									// (survey.getState().equals(SurveyState.RUNNING))
									// {
									// replacements.put("BUTTONSTARTPHONECALLS",
									// "<button id=\"bStartPhonecalls\">Start phone interviews</button>");
									// }
									selectOptions.append("<a class=\"underlinedLink gui-icon-GROUP\" style=\"margin-right: 10px;\" onclick=\"$('.participantCheckBox').attr('checked', true);\">All</a>");
									selectOptions.append("<a class=\"underlinedLink gui-icon-GROUP_DELETE\" style=\"margin-right: 10px;\" onclick=\"$('.participantCheckBox').removeAttr('checked');\">None</a>");
									commands.append("</select><a class=\"runCommandButton\" href=\"javascript:void(0);\" onclick=\"sendCommand($('#commandSelector').val())\";\">Go</a>");
									
									replacements.put("COMMANDS", commands.toString());
									replacements.put("SELECTOPTIONS", selectOptions.toString());
									
									replacements.put("DLGPARTICIPANTADD", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgParticipantAdd.html", replacements));
									replacements.put("DLGPARTICIPANTSIMPORT", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgParticipantsImport.html", replacements));
									replacements.put("DLGPARTICIPANTSCOPY", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgParticipantsCopy.html", replacements));
									
									StringBuilder participantCopysurveyNameList = new StringBuilder();
									participantCopysurveyNameList.append("<option value=\"-1\">---</option>");
									surveyListContainer.getContent(request, replacements, currentUser);
									List<ISurvey> surveys = surveyListContainer.getSurveys();
									for (ISurvey currentSurvey : surveys) {
										if (!currentSurvey.getState().equals(SurveyState.SYSTEMTEMPLATE) && (currentSurvey.getId() != survey.getId()) && !currentSurvey.equals(survey) && !currentSurvey.getName().equals(survey.getName())) {
											participantCopysurveyNameList.append("<option value=\"" + currentSurvey.getId() + "\">" + currentSurvey.getName() + "</option>");
										}
									}
									replacements.put("PARTICIPANT_COPY_SURVEYNAMELIST", participantCopysurveyNameList.toString());
									
									boolean canAddParticipants = true;
									if (PaymentModel.Trial.equals(GroupManager.getPaymentModel(currentUser))) {
										if (participantList.size() >= Constants.TRIAL_PARTICIPANTS_LIMIT) {
											canAddParticipants = false;
										}
									}
									if (canAddParticipants) {
										replacements.put("BUTTON_EDITLIST", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantTable_buttonsEdit.html", replacements));
									} else {
										replacements.put("BUTTON_EDITLIST", "You can only add up to " + Constants.TRIAL_PARTICIPANTS_LIMIT + " participants in the trial account. <a style=\"color: #2244ca;\" href=\"" + EnvironmentConfiguration.getConfiguration(ConfigID.CONTACT_URL) + "\">Upgrade your account here</a> to remove this limit!");
									}
									
									replacements.put("TEXT_EDITENABLED", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantTable_textEditEnabled.html", null));
								} else {
									replacements.put("TEXT_EDITDISABLED", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantTable_textEditDisabled.html", null));
								}
								replacements.put("SYSUSERMAIL", currentUser.getEmail());
								replacements.put("DLGEDITEMAILTEXT", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgEditEmailText.html", replacements));
								
								if (survey.getState().ordinal() > SurveyState.CREATED.ordinal()) {
									replacements.put("DLGDATARECORDIMPORT", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgDataRecordsImport.html", replacements));
									replacements.put("IMPORTRECORDSSECTION", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/importRecordsSection.html", replacements));
								}
								
								replacements.put("ROWS", tableRows.toString());
								replacements.put("DEFAULTICON", "");
								replacements.put("SPECIALPROPERTIES", getSpecialPropertyRows());
								
								replacements.put("DLGPARTICIPANTINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgParticipantInfo.html", replacements));
								if (altLeastOneParticipantMarkedForPhoneInterview && (survey.getState().ordinal() >= SurveyState.CREATED.ordinal())) {
									replacements.put("BUTTON_DOWNLOADPHONELIST", "<button id=\"bDownloadPhonelist\" title=\"Click to download the list of participants that shall be interviewed by phone (PDF)\">Download Phonelist</button>");
								}
								if (survey.getState() == SurveyState.CREATED) {
									replacements.put("ADDITIONALINFO", "<a id=\"clickToReleaseSurveyButton\" class=\"button\"" + ((survey.getParticipants().size() == 0) ? " style=\"display:none;\"" : "") + " title=\"Click here to release the survey now\" href=\"?page=" + Pages.PAGE_MANAGE_SURVEYS + "#open=" + survey.getId() + "\">Release the Survey now</a>");
								}
								
								String actionTemplate = Paths.TEMPLATEPATH + "/savePartOfSurveyAction.html";
								StringStringPair kvpSurveyID = new StringStringPair("SURVEYID", survey.getId() + "");
								replacements.put("EDIT_EMAILSENDER_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield("eMailSender", "eMailSender", survey.getEmailSender(), actionTemplate, "e.g. john.smith@example.com", kvpSurveyID));
								replacements.put("EDIT_SENDERNAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield("senderName", "senderName", survey.getSenderName(), actionTemplate, "e.g. John Smith", kvpSurveyID));
								
								content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgParticipantsCopy.html", replacements));
								content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantTable.html", replacements));
							} else {
								content.append("<div class=\"innerContainer\">The survey <b>" + survey.getName() + "</b> is a Systemtemplate. You cannot edit the participants of a Systemtemplate. You first have to create a template or a concrete survey from it.</div>");
							}
						} else {
							content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
						}
					} else {
						content.append("Whoops...no survey with the ID " + surveyID + " exists.");
					}
				} finally {
					hibSess.close();
				}
			} else {
				content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
			}
		}
	}
	
	/**
	 * @return rows for special properties
	 */
	public static String getSpecialPropertyRows() {
		StringBuilder sb = new StringBuilder();
		Participant p = SurveyElementFactory.getInstance().createParticipant(null, false);
		for (String key : p.getProperties().getProperties().keySet()) {
			sb.append("<tr><td>");
			sb.append(PropertyViewUtil.getPropertyView(key));
			sb.append(":</td><td><input type=\"text\" name=\"");
			sb.append(PropertyViewUtil.format(key));
			sb.append("\" id=\"");
			sb.append(PropertyViewUtil.format(key));
			sb.append("TextField\" value=\"\" class=\"gui-dialog-element\" /></td></tr>");
		}
		return sb.toString();
	}
	
	private String getInfoLink(final Participant participant, final String label, final String param) {
		return "<a href=\"javascript:void(0);\" title=\"Show Details\" onclick=\"showParticipantInfo(" + participant.getId() + ")\"><span id=\"" + param + participant.getId() + "\">" + label + "</span></a>";
	}
}
