package de.cinovo.surveyplatform.servlets.dal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.mail.EmailException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.gson.Gson;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.importer.ImportFromExcelException;
import de.cinovo.surveyplatform.importer.ParticipantImporter;
import de.cinovo.surveyplatform.model.CallBack;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Participation;
import de.cinovo.surveyplatform.model.PaymentModel;
import de.cinovo.surveyplatform.model.PersistentProperties;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.factory.DtoFactory;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.model.jsondto.SmallParticipantDto;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.EmailManager;
import de.cinovo.surveyplatform.util.FileUploadUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.PropertyViewUtil;
import de.cinovo.surveyplatform.util.SurveyUtil;
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
public class ParticipantDal extends AbstractSccServlet {
	
	public static final String PARAM_NAME = "name";
	public static final String PARAM_SURNAME = "surname";
	private static final String PARAM_PHONE = "phone";
	public static final String PARAM_EMAIL = "email";
	
	public static final String PARAM_PARTICIPANTS = "participant[]";
	
	public static final String PARAM_COMMAND = "cmd";
	public static final String PARAM_E_MAIL_SENDER = "eMailSender";
	public static final String COMMAND_INVITE = "invite";
	public static final String COMMAND_REMIND = "remind";
	public static final String COMMAND_REINVITE = "reinvite";
	public static final String COMMAND_MARKASPHONE = "markasphone";
	
	public static final String PARAM_WRITEABLE = "rw";
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_STATE = "state";
	private static final String PARAM_SURVEYID = "surveyID";
	private static final String PARAM_PARTICIPANT_ID = "participantID";
	private static final String PARAM_INCLUDEPARTICIPANTS = "includeParticipants";
	private static final String PARAM_SHOWLIST = "showlist";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.servlets.AbstractSccServlet#isAccessAllowed( javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public boolean isAccessAllowed(final HttpServletRequest req, final Method method, final SystemUser currentUser) {
		
		if (method.equals(Method.RETRIEVE) || AuthUtil.isAllowedToManageParticipants(currentUser)) {
			return true;
		}
		return false;
	}
	
	@Override
	public void processCreate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (ParamUtil.checkAllParamsSet(req, PARAM_EMAIL, PARAM_PHONE) && ParamUtil.ensureAllParamsSetAndNotEmpty(req, PARAM_NAME, PARAM_SURVEYID)) {
			
			final int surveyId = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
			
			final String participantName = req.getParameter(PARAM_NAME);
			final String participantSurname = req.getParameter(PARAM_SURNAME);
			final String participantEmail = req.getParameter(PARAM_EMAIL);
			final String participantPhone = req.getParameter(PARAM_PHONE);
			
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Transaction tx = hibSess.beginTransaction();
				
				Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
				
				String taskID = "page." + Pages.PAGE_ADMINISTRATE_PARTICIPANTS + "." + req.getSession().getId();
				FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
				
				if (AuthUtil.isAllowedToManageParticipants(currentUser, survey, hibSess)) {
					
					if ((PaymentModel.Trial.equals(GroupManager.getPaymentModel(currentUser)) && (survey.getParticipants().size() >= Constants.TRIAL_PARTICIPANTS_LIMIT))) {
						feedBackProvider.setMessage(taskID, "Participant NOT added", currentUser.getActualUserName());
						feedBackProvider.addFeedback(taskID, "Did not add the participants, because the maximum count of participants for this survey is limited to " + Constants.TRIAL_PARTICIPANTS_LIMIT, Status.ERROR);
					} else {
						
						boolean createParticipation = false;
						// Only create participations if this is a concrete
						// survey
						if ((survey.getState().ordinal() > SurveyState.TEMPLATE.ordinal())) {
							createParticipation = true;
						}
						
						Participant participant = SurveyElementFactory.getInstance().createParticipant(survey, createParticipation);
						participant.setNumber(SurveyUtil.getNextParticipantNumber(hibSess, survey));
						participant.setName(participantName);
						participant.setContactEmail(participantEmail);
						participant.setContactPhone(participantPhone);
						participant.setSurname(participantSurname);
						
						// handle special properties
						setSpecialProperties(participant, req);
						
						hibSess.save(survey);
						hibSess.save(participant);
						
						tx.commit();
						Logger.logUserActivity("Created a participant in '" + survey.getName() + "': " + participant.getName() + " " + participant.getSurname(), currentUser.getUserName());
						String jsonRepresentation = DtoFactory.getInstance().createDto(currentUser, participant).getJSON();
						resp.getWriter().write(jsonRepresentation);
						feedBackProvider.setMessage(taskID, "Participant added", currentUser.getActualUserName());
						feedBackProvider.addFeedback(taskID, "Successfully added the participant: " + participant.getSurname() + " " + participant.getName(), Status.OK);
						
					}
				} else {
					feedBackProvider.setMessage(taskID, "Participant NOT added", currentUser.getActualUserName());
					feedBackProvider.addFeedback(taskID, "You do not have sufficient rights to add participants to this survey!", Status.ERROR);
				}
				feedBackProvider.finishTask(taskID);
				
			} catch (Exception e) {
				Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
				resp.getWriter().write("Error: " + e);
			} finally {
				hibSess.close();
			}
		} else {
			try {
				if (ServletFileUpload.isMultipartContent(req)) {
					List<File> uploadedFiles = FileUploadUtil.processUpload(req, Paths.WEBCONTENT + "/" + Paths.UPLOAD_TEMP);
					final int surveyId = Integer.parseInt(req.getAttribute(PARAM_SURVEYID).toString());
					Session hibSess = HibernateUtil.getSessionFactory().openSession();
					try {
						Transaction tx = hibSess.beginTransaction();
						
						Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
						
						if (survey != null) {
							
							boolean createParticipation = true;
							if ((survey.getState() == SurveyState.SYSTEMTEMPLATE) || (survey.getState() == SurveyState.TEMPLATE)) {
								createParticipation = false;
							}
							String taskID = "page." + Pages.PAGE_ADMINISTRATE_PARTICIPANTS + "." + req.getSession().getId();
							FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
							for (File file : uploadedFiles) {
								final String fileName = FileUploadUtil.getRealFileName(file);
								
								ParticipantImporter importer = new ParticipantImporter(taskID);
								if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {
									try {
										List<Participant> participants = importer.importFromExcel(file, currentUser);
										boolean limitParticipants = false;
										if (PaymentModel.Trial.equals(GroupManager.getPaymentModel(currentUser))) {
											limitParticipants = true;
										}
										int participantsAlready = survey.getParticipants().size();
										int pCount = 0;
										int participantNumber = SurveyUtil.getNextParticipantNumber(hibSess, survey);
										for (Participant p : participants) {
											Participant participant = SurveyElementFactory.getInstance().createParticipant(survey, createParticipation);
											participant.setNumber(participantNumber++);
											participant.setName(p.getName());
											participant.setContactEmail(p.getContactEmail());
											participant.setContactPhone(p.getContactPhone());
											participant.setSurname(p.getSurname());
											participant.setProperties(p.getProperties());
											// hibSess.save(participant);
											pCount++;
											if (limitParticipants && ((pCount + participantsAlready) >= Constants.TRIAL_PARTICIPANTS_LIMIT)) {
												int diff = participants.size() - pCount;
												if (diff > 0) {
													feedBackProvider.addFeedback(taskID, "Did not import " + diff + " participants, because the maximum count of participants for this survey is limited to " + Constants.TRIAL_PARTICIPANTS_LIMIT, Status.WARNING, currentUser.getActualUserName());
												}
												break;
											}
										}
										hibSess.save(survey);
										Logger.logUserActivity("Imported participants to '" + survey.getName() + "': " + pCount, currentUser.getUserName());
										
										feedBackProvider.setMessage(taskID, "Participants imported", currentUser.getActualUserName());
										feedBackProvider.addFeedback(taskID, "Imported " + pCount + " participants from the excel file " + fileName, Status.OK);
									} catch (ImportFromExcelException ifee) {
										Logger.err("There was an error, reading the file " + fileName, ifee);
										feedBackProvider.setMessage(taskID, "Participants NOT imported", currentUser.getActualUserName());
										feedBackProvider.addFeedback(taskID, ifee.getReason(), Status.ERROR);
										feedBackProvider.addFeedback(taskID, "There was an error, reading the file " + fileName + ". Ensure that your file complies to the format of <a href=\"../excel/Excel_File_Template.xls\">the Template MS-Excel&reg; file</a> and try again.<br/>If you still encounter the error, please contact your support.", Status.ERROR);
									}
								} else {
									feedBackProvider.setMessage(taskID, "No participants imported", currentUser.getActualUserName());
									feedBackProvider.addFeedback(taskID, "You must select a valid excel file (.xls or .xlsx). " + fileName + " is not a valid MS-Excel&reg; file.", Status.ERROR);
								}
								
								if (!file.delete()) {
									file.deleteOnExit();
								}
							}
							
							tx.commit();
							feedBackProvider.finishTask(taskID);
							
							resp.sendRedirect(getStandardRedirectLocation(req));
							
						}
					} catch (Exception e) {
						Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
						resp.getWriter().write("Error: " + e);
					} finally {
						hibSess.close();
					}
					
				}
			} catch (Exception e) {
				Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
			}
		}
		if (ParamUtil.checkAllParamsSet(req, PARAM_INCLUDEPARTICIPANTS)) {
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			final int surveyId = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
			
			try {
				Transaction tx = hibSess.beginTransaction();
				
				Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
				if (survey != null) {
					boolean createParticipation = false;
					SurveyState currentState = survey.getState();
					if (currentState.compareTo(SurveyState.CREATED) < 0) {
						createParticipation = true;
					}
					Survey participantSourceSurvey = (Survey) hibSess.get(Survey.class, ParamUtil.getSafeIntFromParam(req, PARAM_INCLUDEPARTICIPANTS));
					
					int participantNumber = SurveyUtil.getNextParticipantNumber(hibSess, survey);
					if (participantSourceSurvey != null) {
						for (Participant p : participantSourceSurvey.getParticipants()) {
							Participant participant = SurveyElementFactory.getInstance().createParticipant(survey, createParticipation);
							participant.setNumber(participantNumber);
							participant.setContactEmail(p.getContactEmail());
							participant.setName(p.getName());
							participant.setContactPhone(p.getContactPhone());
							participant.setSurname(p.getSurname());
							// Create new Participation to avoid non-unique
							// queries
							Participation participation = new Participation();
							participation.setParticipant(participant);
							participation.setId(UUID.randomUUID().toString());
							
							participant.setParticipation(participation);
							participation.setParticipant(participant);
							// --
							participant.setSurvey(survey);
							if (p.getProperties() != null) {
								for (Entry<String, String> entry : p.getProperties().getProperties().entrySet()) {
									participant.getProperties().setProperty(entry.getKey(), entry.getValue());
								}
							}
							hibSess.save(participant);
							participantNumber++;
						}
					}
				}
				tx.commit();
				if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
					resp.getWriter().print(DtoFactory.getInstance().createDto(currentUser, survey).getJSON());
				} else {
					resp.sendRedirect(getStandardRedirectLocation(req));
				}
				
			} catch (Exception e) {
				Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
				resp.getWriter().write("Error: " + e);
			} finally {
				hibSess.close();
			}
		}
		
	}
	
	/**
	 * @param participant
	 * @param req
	 */
	private void setSpecialProperties(final Participant participant, final HttpServletRequest req) {
		Set<String> keySet = null;
		if (participant.getProperties() == null) {
			Participant p = SurveyElementFactory.getInstance().createParticipant(null, false);
			PersistentProperties persistentProperties = new PersistentProperties();
			persistentProperties.setSyncId(UUID.randomUUID().toString());
			participant.setProperties(persistentProperties);
			participant.getProperties().setProperties(p.getProperties().getProperties());
		}
		
		keySet = participant.getProperties().getProperties().keySet();
		
		String[] properties = keySet.toArray(new String[keySet.size()]);
		for (String propertyName : properties) {
			String value = req.getParameter(PropertyViewUtil.format(propertyName));
			if (value != null) {
				participant.getProperties().setProperty(propertyName, value);
			}
		}
	}
	
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				if (ParamUtil.ensureAllParamsSetAndNotEmpty(req, PARAM_PARTICIPANT_ID)) {
					
					final int participantId = ParamUtil.getSafeIntFromParam(req, PARAM_PARTICIPANT_ID);
					final PrintWriter writer = resp.getWriter();
					
					Transaction tx = hibSess.beginTransaction();
					
					Participant participant = (Participant) hibSess.get(Participant.class, participantId);
					tx.commit();
					if (participant != null) {
						
						if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_HTML)) {
							Map<String, String> replacements = new HashMap<String, String>();
							
							String actionTemplate = Paths.TEMPLATEPATH + "/savePartOfParticipantAction.html";
							String actionTemplateForNameAndSurname = Paths.TEMPLATEPATH + "/savePartOfParticipantActionForNameAndSurname.html";
							
							StringStringPair kvpUserID = new StringStringPair("PARTICIPANTID", participant.getId() + "");
							
							if (!EnvironmentConfiguration.isOfflineMode() && ParamUtil.checkAllParamsSet(req, PARAM_WRITEABLE)) {
								replacements.put("NAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_NAME, PARAM_NAME, participant.getName(), actionTemplateForNameAndSurname, kvpUserID));
								replacements.put("SURNAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_SURNAME, PARAM_SURNAME, participant.getSurname(), actionTemplateForNameAndSurname, kvpUserID));
								replacements.put("EMAIL_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_EMAIL, PARAM_EMAIL, participant.getContactEmail(), actionTemplate, kvpUserID));
								replacements.put("PHONE_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_PHONE, PARAM_PHONE, participant.getContactPhone(), actionTemplate, kvpUserID));
								replacements.put("SPECIALPROPERTIES", getSpecialPropertyRows(participant, true, actionTemplate, kvpUserID));
							} else {
								replacements.put("NAME_TEXTFIELD", participant.getName());
								replacements.put("SURNAME_TEXTFIELD", participant.getSurname());
								replacements.put("EMAIL_TEXTFIELD", participant.getContactEmail());
								replacements.put("PHONE_TEXTFIELD", participant.getContactPhone());
								replacements.put("SPECIALPROPERTIES", getSpecialPropertyRows(participant, false));
							}
							if (participant.getSurveySubmitted() == null) {
								replacements.put("START_LABEL", "Start recording:");
								replacements.put("LINK_TEXTFIELD", participant.getParticipation() != null ? "<a style=\"float: right;\" class=\"gui-icon-button-CONTROL_PLAY_BLUE_BIG\" title=\"Start recording data\" href=\"" + EnvironmentConfiguration.getUrlBase() + "/participate?pid=" + participant.getParticipation().getId() + "\" target=\"_blank\"></a>" : "not available");
							} else {
								String editLink = "";
								replacements.put("START_LABEL", PartsUtil.getIcon("ACCEPT_BIG", "Participant has submitted the questionnaire"));
								if (AuthUtil.isAllowedToEditQuestionnaireData(currentUser) && !participant.getSurvey().getState().equals(SurveyState.CLOSED)) {
									if (participant.getParticipation() != null) {
										editLink = "<a class=\"inTableAction\" title=\"This is NOT recommended!\" href=\"" + EnvironmentConfiguration.getUrlBase() + "/participate?overridesubmitted=1&pid=" + participant.getParticipation().getId() + "\" target=\"_blank\">Edit questionnaire data</a>";
									}
								}
								replacements.put("LINK_TEXTFIELD", "<span>Participant has submitted the questionnaire</span>" + editLink);
							}
							
							replacements.put("DATEINVITATION_TEXTFIELD", participant.getInvitationSent() != null ? TimeUtil.getLocalTime(currentUser, participant.getInvitationSent()) : "");
							replacements.put("DATEREMINDING_TEXTFIELD", participant.getReminderSent() != null ? TimeUtil.getLocalTime(currentUser, participant.getReminderSent()) : "");
							replacements.put("DATESUBMISSION_TEXTFIELD", participant.getSurveySubmitted() != null ? TimeUtil.getLocalTime(currentUser, participant.getSurveySubmitted()) : "");
							
							writer.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableParticipantInfo.html", replacements));
						} else if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_JSON)) {
							String jsonRepresentation = DtoFactory.getInstance().createDto(currentUser, participant).getJSON();
							writer.write(jsonRepresentation);
						}
						
					}
				} else if (ParamUtil.checkAllParamsSet(req, PARAM_SHOWLIST)) {
					int surveyId = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
					Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
					if (survey != null) {
						StringBuilder participantList = new StringBuilder();
						participantList.append("<p style=\"margin: 0px 0px 20px 0px;\"><b>Preview of Participants to add: </b></p>");
						participantList.append("<ul>");
						Survey participantSourceSurvey = (Survey) hibSess.get(Survey.class, ParamUtil.getSafeIntFromParam(req, PARAM_INCLUDEPARTICIPANTS));
						if (participantSourceSurvey != null) {
							List<Participant> participants = participantSourceSurvey.getParticipants();
							if ((participants == null) || (participants.size() == 0)) {
								participantList.append("<li style=\"margin: 3px 0px 3px 0px;\">No participant in this survey yet</li>");
							} else {
								for (Participant p : participantSourceSurvey.getParticipants()) {
									participantList.append("<li style=\"margin: 3px 0px 3px 0px;\">" + p.getName() + "</li>");
								}
							}
						}
						participantList.append("</ul>");
						resp.getWriter().print(participantList.toString());
					}
				} else if (ParamUtil.checkAllParamsSet(req, PARAM_STATE)) {
					int surveyId = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
					Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
					List<Participant> participants = survey.getParticipants();
					List<SmallParticipantDto> dtos = new ArrayList<SmallParticipantDto>();
					for (Participant participant : participants) {
						SmallParticipantDto dto = new SmallParticipantDto();
						dto.id = participant.getId();
						if (participant.getSurveySubmitted() == null) {
							dto.submittedDateString = "";
						} else {
							dto.submittedDateString = TimeUtil.getLocalTime(currentUser, participant.getSurveySubmitted());
						}
						dtos.add(dto);
					}
					resp.getWriter().print(new Gson().toJson(dtos));
				}
			} catch (Exception e) {
				Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
				resp.getWriter().write("Error: " + e);
			} finally {
				hibSess.close();
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @seescc.servlets.AbstractSccServlet#processUpdate(javax.servlet.http. HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processUpdate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		if (ServletFileUpload.isMultipartContent(req)) {
			
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				List<File> uploadedFiles = FileUploadUtil.processUpload(req, Paths.WEBCONTENT + "/" + Paths.UPLOAD_TEMP);
				final int surveyId = Integer.parseInt(req.getAttribute(PARAM_SURVEYID).toString());
				Transaction tx = hibSess.beginTransaction();
				
				Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
				
				if (survey != null) {
					
					String taskID = "page." + Pages.PAGE_ADMINISTRATE_PARTICIPANTS + "." + req.getSession().getId();
					FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
					for (File file : uploadedFiles) {
						final String fileName = FileUploadUtil.getRealFileName(file);
						
						ParticipantImporter importer = new ParticipantImporter(taskID);
						if (file.getName().endsWith(".xml")) {
							try {
								importer.importFromXml(file, currentUser);
								
							} catch (Exception ex) {
								Logger.err("There was an error, reading the file " + fileName, ex);
								feedBackProvider.setMessage(taskID, "Error during import", currentUser.getActualUserName());
								feedBackProvider.addFeedback(taskID, ex.getMessage(), Status.ERROR);
								feedBackProvider.addFeedback(taskID, "There was an error, reading the file " + fileName + ". Ensure that your file has been exported by an offline client.", Status.ERROR);
							}
						} else {
							feedBackProvider.setMessage(taskID, "No participants imported", currentUser.getActualUserName());
							feedBackProvider.addFeedback(taskID, "You must select a valid xml file (.xml). " + fileName + " is not a valid xml file.", Status.ERROR);
						}
						
						if (!file.delete()) {
							file.deleteOnExit();
						}
					}
					
					tx.commit();
					feedBackProvider.finishTask(taskID);
					
					resp.sendRedirect(getStandardRedirectLocation(req));
					
				}
			} catch (Exception e) {
				Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
				resp.getWriter().write("Error: " + e);
			} finally {
				hibSess.close();
			}
			return;
			
		}
		
		int participantID = ParamUtil.getSafeIntFromParam(req, PARAM_PARTICIPANT_ID);
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			
			if (participantID > 0) {
				Participant participantToChange = (Participant) hibSess.get(Participant.class, participantID);
				
				Query query = hibSess.createQuery(Queries.SURVEY_BY_PARTICIPANT);
				
				if (participantToChange == null) {
					Logger.err("Strange access: User tried to get a survey from the participant: 'null' (ID:" + participantID + ")");
					handlePermissionDenied(req, resp);
					return;
				}
				query.setParameter("1", participantToChange.getId());
				Survey survey = (Survey) query.uniqueResult();
				
				if (survey == null) {
					Logger.err("Strange access: User tried to get a survey from the participant: '" + participantToChange.getName() + "' (ID:" + participantID + ")");
					handlePermissionDenied(req, resp);
					return;
				}
				
				if (AuthUtil.isAllowedToManageParticipants(currentUser, survey, hibSess)) {
					
					// email
					if (ParamUtil.checkAllParamsSet(req, PARAM_NAME)) {
						String param = req.getParameter(PARAM_NAME);
						participantToChange.setName(param);
					}
					if (ParamUtil.checkAllParamsSet(req, PARAM_EMAIL)) {
						String param = req.getParameter(PARAM_EMAIL);
						participantToChange.setContactEmail(param);
					}
					if (ParamUtil.checkAllParamsSet(req, PARAM_PHONE)) {
						String param = req.getParameter(PARAM_PHONE);
						participantToChange.setContactPhone(param);
					}
					if (ParamUtil.checkAllParamsSet(req, PARAM_SURNAME)) {
						String param = req.getParameter(PARAM_SURNAME);
						participantToChange.setSurname(param);
					}
					
					// handle special properties
					setSpecialProperties(participantToChange, req);
					hibSess.save(participantToChange);
					tx.commit();
					Logger.logUserActivity("Updated a participant in '" + survey.getName() + "': " + participantToChange.getName() + " " + participantToChange.getSurname(), currentUser.getUserName());
					resp.getWriter().print(DtoFactory.getInstance().createDto(currentUser, participantToChange).getJSON());
				} else {
					super.handlePermissionDenied(req, resp);
				}
			} else if (ParamUtil.checkAllParamsSet(req, PARAM_COMMAND, PARAM_SURVEYID)) {
				
				Survey survey = (Survey) hibSess.get(Survey.class, ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID));
				
				String command = req.getParameter(PARAM_COMMAND);
				String[] participants = req.getParameterValues(PARAM_PARTICIPANTS);
				if (participants != null) {
					if (command.equals(COMMAND_INVITE)) {
						int emailsSentOk = 0;
						int alreadySentORSubmitted = 0;
						for (String participantId : participants) {
							Participant participant = (Participant) hibSess.get(Participant.class, Integer.parseInt(participantId));
							// only invite users that are not invited and did not
							// submit the questionnaire
							if ((participant.getInvitationSent() == null) && (participant.getSurveySubmitted() == null)) {
								try {
									sendInvitationMail(participant, req, survey);
									hibSess.save(participant);
									emailsSentOk++;
								} catch (Exception e) {
									Logger.err("Could not send email", e);
								}
							} else {
								emailsSentOk++;
								alreadySentORSubmitted++;
							}
						}
						String taskID = "page." + Pages.PAGE_ADMINISTRATE_PARTICIPANTS + "." + req.getSession().getId();
						FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
						if (emailsSentOk < participants.length) {
							feedBackProvider.setMessage(taskID, "Not all Invitations sent!", currentUser.getActualUserName());
							feedBackProvider.addFeedback(taskID, (participants.length - emailsSentOk) + " of " + participants.length + " e-mails could not be sent. Please check, if the addresses are spelled correctly.", Status.ERROR);
							feedBackProvider.finishTask(taskID, true);
						} else {
							// Re-Invite here
							feedBackProvider.setMessage(taskID, "Invitations sent", currentUser.getActualUserName());
							feedBackProvider.addFeedback(taskID, "Invitations are sent to " + (emailsSentOk - alreadySentORSubmitted) + " participants. The e-mails are sent deferred so that it may take some time until all emails reach the receiver.", Status.OK);
							if (alreadySentORSubmitted > 0) {
								feedBackProvider.addFeedback(taskID, "Did not send e-mails to " + alreadySentORSubmitted + " participants as they are already invited or have submitted the questionnaire.", Status.WARNING);
							}
							feedBackProvider.finishTask(taskID, false);
						}
						
					} else if (command.equals(COMMAND_REMIND)) {
						int emailsSentOk = 0;
						int notSentOrAlreadySubmitted = 0;
						for (String participantId : participants) {
							Participant participant = (Participant) hibSess.load(Participant.class, Integer.parseInt(participantId));
							// only send reminder if the user has been invited and
							// did not submit the questionnaire
							
							if ((participant.getInvitationSent() != null) && (participant.getSurveySubmitted() == null)) {
								try {
									sendReminderEmail(participant, req, survey);
									hibSess.save(participant);
									emailsSentOk++;
								} catch (EmailException e) {
									Logger.err("Could not send email", e);
								}
							} else {
								emailsSentOk++;
								notSentOrAlreadySubmitted++;
							}
						}
						String taskID = "page." + Pages.PAGE_ADMINISTRATE_PARTICIPANTS + "." + req.getSession().getId();
						FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
						if (emailsSentOk < participants.length) {
							feedBackProvider.setMessage(taskID, "Not all reminders sent!", currentUser.getActualUserName());
							feedBackProvider.addFeedback(taskID, (participants.length - emailsSentOk) + " of " + participants.length + " e-mails could not be sent. Please check, if the addresses are spelled correctly.", Status.ERROR);
							feedBackProvider.finishTask(taskID, true);
						} else {
							feedBackProvider.setMessage(taskID, "Reminders sent", currentUser.getActualUserName());
							feedBackProvider.addFeedback(taskID, "Reminders are sent to " + (participants.length - notSentOrAlreadySubmitted) + " participants. The e-mails are sent deferred so that it may take some time until all emails reach the receiver.", Status.OK);
							if (notSentOrAlreadySubmitted > 0) {
								feedBackProvider.addFeedback(taskID, "Did not sent e-mails to " + notSentOrAlreadySubmitted + " participants as they are not invited yet or have already submitted the questionnaire.", Status.WARNING);
							}
							feedBackProvider.finishTask(taskID, false);
						}
						
					} else if (command.equals(COMMAND_REINVITE)) {
						int emailsSentOk = 0;
						for (String participantId : participants) {
							Participant participant = (Participant) hibSess.get(Participant.class, Integer.parseInt(participantId));
							// Re-Invite (after confirmation) all users
							try {
								sendInvitationMail(participant, req, survey);
								hibSess.save(participant);
								emailsSentOk++;
							} catch (Exception e) {
								Logger.err("Could not send email", e);
							}
						}
						String taskID = "page." + Pages.PAGE_ADMINISTRATE_PARTICIPANTS + "." + req.getSession().getId();
						FeedBackProvider feedBackProvider = FeedBackProvider.getInstance();
						// Re-Invite here
						feedBackProvider.setMessage(taskID, "Invitations sent", currentUser.getActualUserName());
						feedBackProvider.addFeedback(taskID, "Invitations are sent to " + emailsSentOk + " participants. The e-mails are sent deferred so that it may take some time until all emails reach the receiver.", Status.OK);
						feedBackProvider.finishTask(taskID, false);
						
					}
					if (command.equals(COMMAND_MARKASPHONE)) {
						for (String participantId : participants) {
							Participant participant = (Participant) hibSess.load(Participant.class, Integer.parseInt(participantId));
							// only mark as phone if the user has not submitted the
							// questionnaire yet
							if (participant.getSurveySubmitted() == null) {
								participant.setAskByPhone(true);
								hibSess.save(participant);
							}
						}
					}
				}
				tx.commit();
			} else {
				super.handlePermissionDenied(req, resp);
			}
		} finally {
			hibSess.close();
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @seescc.servlets.AbstractSccServlet#processDelete(javax.servlet.http. HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processDelete(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (ParamUtil.checkAllParamsSet(req, PARAM_PARTICIPANTS)) {
			String participantIds[] = req.getParameterValues(PARAM_PARTICIPANTS);
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Transaction tx = hibSess.beginTransaction();
				if (participantIds != null) {
					Survey survey = (Survey) hibSess.get(Survey.class, ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID));
					
					for (String participantId : participantIds) {
						try {
							Participant participant = (Participant) hibSess.load(Participant.class, Integer.parseInt(participantId));
							survey.getParticipants().remove(participant);
							hibSess.delete(participant);
							
						} catch (NumberFormatException nfe) {
							// ignore numberformat exception and go on with the next
						}
					}
					hibSess.save(survey);
					Logger.logUserActivity("Deleted participants: " + Arrays.toString(participantIds), currentUser.getUserName());
					
				}
				tx.commit();
			} finally {
				hibSess.close();
			}
		}
	}
	
	/**
	 * @param participant
	 * @param survey
	 * @throws EmailException
	 */
	private void sendReminderEmail(final Participant participant, final HttpServletRequest req, final Survey survey) throws EmailException {
		if (participant.getContactEmail() != "") {
			if ((participant.getContactEmail() != "") && (participant.getParticipation() != null)) {
				Map<String, String> replacements = new HashMap<String, String>();
				
				replacements.put("NAME_OF_PARTICIPANT", participant.getName() + " " + participant.getSurname());
				replacements.put("ORGANIZATION", survey.getOwner().getClient().getOrganization());
				String link = getServerBaseUrl() + "/participate?pid=" + participant.getParticipation().getId();
				replacements.put("LINK", link);
				final int participantID = participant.getId();
				
				CallBack cb = new CallBack() {
					
					@Override
					public void doCallBack() {
						Session hibSess = HibernateUtil.getSessionFactory().openSession();
						Transaction tx = hibSess.beginTransaction();
						Participant participant = (Participant) hibSess.load(Participant.class, participantID);
						participant.setReminderSent(new Date());
						participant.setEmailInQueue(false);
						tx.commit();
						hibSess.close();
					}
					
					/*
					 * (non-Javadoc)
					 * 
					 * @see de.cinovo.surveyplatform.model.CallBack#doCallBackFailure( java.lang.Exception)
					 */
					@Override
					public void doCallBackFailure(final Exception e) {
						Session hibSess = HibernateUtil.getSessionFactory().openSession();
						Transaction tx = hibSess.beginTransaction();
						Participant participant = (Participant) hibSess.load(Participant.class, participantID);
						participant.setEmailInQueue(false);
						tx.commit();
						hibSess.close();
						Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
					}
				};
				
				String emailSender = survey.getEmailSender() == null ? EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER).toString() : survey.getEmailSender();
				String emailSenderName = survey.getSenderName() == null ? "Metior Solutions Survey Platform" : survey.getSenderName();
				String emailSubject = survey.getEmailSubjectRemind();
				if (emailSubject == null) {
					emailSubject = "Reminder";
				}
				
				EmailManager.getInstance().sendEmail(participant.getContactEmail(), participant.getName(), emailSender, emailSenderName, emailSubject, replacements, SurveyUtil.getEmailTextRemind(survey), cb);
				participant.setEmailInQueue(true);
				
			}
			
		}
	}
	
	/**
	 * @param participant
	 * @param survey
	 * @throws EmailException
	 */
	private void sendInvitationMail(final Participant participant, final HttpServletRequest req, final Survey survey) throws EmailException {
		if ((participant.getContactEmail() != "") && (participant.getParticipation() != null)) {
			Map<String, String> replacements = new HashMap<String, String>();
			
			replacements.put("NAME_OF_PARTICIPANT", participant.getName() + " " + participant.getSurname());
			replacements.put("ORGANIZATION", survey.getOwner().getClient().getOrganization());
			String link = getServerBaseUrl() + "/participate?pid=" + participant.getParticipation().getId();
			replacements.put("LINK", link);
			final int participantID = participant.getId();
			
			CallBack cb = new CallBack() {
				
				@Override
				public void doCallBack() {
					Session hibSess = HibernateUtil.getSessionFactory().openSession();
					try {
						Transaction tx = hibSess.beginTransaction();
						Participant participant = (Participant) hibSess.load(Participant.class, participantID);
						participant.setInvitationSent(new Date());
						participant.setEmailInQueue(false);
						tx.commit();
					} finally {
						hibSess.close();
					}
				}
				
				/*
				 * (non-Javadoc)
				 * 
				 * @see de.cinovo.surveyplatform.model.CallBack#doCallBackFailure(java .lang.Exception)
				 */
				@Override
				public void doCallBackFailure(final Exception e) {
					Session hibSess = HibernateUtil.getSessionFactory().openSession();
					try {
						Transaction tx = hibSess.beginTransaction();
						Participant participant = (Participant) hibSess.load(Participant.class, participantID);
						participant.setEmailInQueue(false);
						tx.commit();
					} finally {
						hibSess.close();
					}
					Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
				}
			};
			
			String emailSender = survey.getEmailSender() == null ? EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER).toString() : survey.getEmailSender();
			String emailSenderName = survey.getSenderName() == null ? "Metior Solutions Survey Platform" : survey.getSenderName();
			String emailSubject = survey.getEmailSubjectInvite();
			if (emailSubject == null) {
				emailSubject = "Invitation to survey";
			}
			EmailManager.getInstance().sendEmail(participant.getContactEmail(), participant.getName(), emailSender, emailSenderName, emailSubject, replacements, SurveyUtil.getEmailTextInvite(survey), cb);
			participant.setEmailInQueue(true);
			
		}
	}
	
	private String getSpecialPropertyRows(final Participant participant, final boolean writeable, final String actionTemplate, final StringStringPair... pairs) {
		StringBuilder sb = new StringBuilder();
		if (participant.getProperties() != null) {
			Map<String, String> properties = participant.getProperties().getProperties();
			Set<String> keySet = participant.getProperties().getProperties().keySet();
			// Array
			List<String> list = asSortedList(keySet);
			
			for (String key : list) {
				String formattedKey = PropertyViewUtil.format(key);
				sb.append("<tr><td>");
				sb.append(PropertyViewUtil.getPropertyView(key));
				sb.append(":</td><td id=\"" + formattedKey + "Field\">");
				if (writeable) {
					sb.append(HtmlFormUtil.getInplaceEditableTextfield(formattedKey, formattedKey, properties.get(key), actionTemplate, pairs));
				} else {
					sb.append(properties.get(key));
				}
				sb.append("</td></tr>");
			}
		}
		return sb.toString();
	}
	
	private String getSpecialPropertyRows(final Participant participant, final boolean writeable) {
		return getSpecialPropertyRows(participant, writeable, "");
	}
	
	private static <T extends Comparable<? super T>> List<T> asSortedList(final Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}
	
	private String getServerBaseUrl() {
		return (String) EnvironmentConfiguration.getConfiguration(ConfigID.HOST) + (String) EnvironmentConfiguration.getConfiguration(ConfigID.URLBASE);
	}
	
}
