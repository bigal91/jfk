package de.cinovo.surveyplatform.servlets.dal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.mail.EmailException;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.google.gson.Gson;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.ChartType;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.ExtendedProperties;
import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.help.ContextHelpProvider;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISortable;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement.Operator;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.factory.DtoFactory;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.model.jsondto.SurveyDto;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.question.ComboQuestion;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.question.IAlignmentQuestion;
import de.cinovo.surveyplatform.model.question.IAlignmentQuestion.Alignment;
import de.cinovo.surveyplatform.model.question.IMatrixQuestion;
import de.cinovo.surveyplatform.model.question.IMultipleOptionsQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceMatrixQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.RadioMatrixQuestion;
import de.cinovo.surveyplatform.model.question.RadioQuestion;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.model.reporting.IReportGenerator.TargetMedia;
import de.cinovo.surveyplatform.model.reporting.ReportType.SubTypeEnum;
import de.cinovo.surveyplatform.reporting.generator.SummaryReportGenerator;
import de.cinovo.surveyplatform.reporting.reports.SummaryReport;
import de.cinovo.surveyplatform.servlets.AbstractSccServlet;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.EmailManager;
import de.cinovo.surveyplatform.util.FileUploadUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil;
import de.cinovo.surveyplatform.util.SurveyUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;
import de.cinovo.surveyplatform.util.TreeUtil;
import de.cinovo.surveyplatform.util.TreeUtil.TreeNode;

/**
 *
 * Copyright 2010 Cinovo AG
 *
 * @author yschubert
 *
 */
public class SurveyDal extends AbstractSccServlet {
	
	private static final long serialVersionUID = 1L;
	public static final String PARTNAME_SECTION = "section";
	public static final String PARTNAME_QUESTION = "question";
	public static final String PARTNAME_PAGE = "page";
	public static final String PARTNAME_SURVEY = "survey";
	
	private static final String SURVEY_TYPE_CONCRETE = "concrete";
	private static final String SURVEY_TYPE_TEMPLATE = "templ";
	private static final String SURVEY_TYPE_SYSTEMTEMPLATE = "systempl";
	
	private static final String PARTNAME_EMAIL_INVITE = "emailInvite";
	private static final String PARTNAME_EMAIL_REMIND = "emailRemind";
	private static final String PARTNAME_EMAIL_SUBJECT_INVITE = "emailSubjectInvite";
	private static final String PARTNAME_EMAIL_SUBJECT_REMIND = "emailSubjectRemind";
	
	public static final String PARTNAME_QUESTION_EDITTABLE = "questionEditTable";
	
	private static final String PARAM_SENDER_NAME = "senderName";
	private static final String PARAM_TEST_MAIL_RECEIVER = "testMailReceiver";
	private static final String PARAM_SEND_EMAIL_TEST = "sendEmailTest";
	public static final String PARAM_SURVEYNAME = "name";
	public static final String PARAM_SURVEYID = "surveyID";
	public static final String PARAM_PAGEID = "pageID";
	public static final String PARAM_SECTIONID = "sectionID";
	public static final String PARAM_QUESTIONID = "questionID";
	
	public static final String PARAM_SOURCE_QUESTION_ID = "sourceQuestId";
	
	public static final String PARAM_QUESTION_TYPE = "questionType";
	public static final String PARAM_QUESTION_TEXT = "questionText";
	private static final String PARAM_QUESTION_ADD_INFO = "addInfo";
	
	public static final String PARAM_QUESTION_ALIGNMENT = "alignment";
	
	public static final String PARAM_SECTION_TITLE = "sectionTitle";
	
	private static final String PARAM_PART = "part";
	
	public static final String PARAM_SORTING = "sorting[]";
	
	private static final String PARAM_OPTIONS = "options[]";
	
	private static final String PARAM_SURVEYS = "surveys[]";
	
	private static final String PARAM_SUBQUESTIONS = "subQuestions[]";
	
	private static final String PARAM_SINGLELINE_HINT = "singleLineHint";
	private static final String PARAM_AVERAGE_NUM_HINT = "averageNumHint";
	
	private static final String PARAM_SURVEYTEMPLATE = "surveyTemplateId";
	private static final String PARAM_TYPE = "type";
	private static final String PARAM_DESCRIPTION = "description";
	private static final String PARAM_INCLUDEPARTICIPANTS = "includeParticipants";
	
	private static final String PARAM_TEXTPART = "editTextPartArea";
	
	private static final String PARAM_VISIBILITY = "visibility";
	
	private static final String PARAM_ANSWERS = "answers";
	private static final String PARAM_QUESTION_IDS = "questions";
	private static final String PARAM_SECTION_IDS = "sections";
	private static final String PARAM_PAGE_IDS = "pages";
	private static final String PARAM_LOGIC_CONDITION_COUNTER = "logicConditionCounter";
	
	private static final String PARAM_CREATELOGIC_REQUESTED = "createLogic";
	private static final String PARAM_QUESTION_ALIAS = "alias";
	private static final String PARAM_STATE = "state";
	
	private static final String PARAM_OPERATION = "operation";
	private static final String OPERATION_MOVE = "move";
	private static final String OPERATION_COPY = "copy";
	private static final String PARAM_POSITION = "position";
	
	private static final String PARAM_INSERTAT = "insertAt";
	
	private static final String PARAM_EMAILTEXT = "emailText";
	private static final String PARAM_EMAILSUBJECT = "emailSubject";
	private static final String PARAM_EMAILTEXT_TYPE = "textType";
	private static final String PARAM_E_MAIL_SENDER = "eMailSender";
	
	private static final String PARAM_CHECKED = "checked";
	private static final String PARAM_BAR_CHECKED = "barChecked";
	private static final String PARAM_PIE_CHECKED = "pieChecked";
	
	private static final String PARAM_REMOVE_PICTURE = "removePicture";
	
	private static final String PARAM_EXTENDEDPROPERTIES = "extendedProperties";
	private static final String PARAM_OWNER = "owner";
	
	private static final String PARAM_GROUPID = "groupId";
	
	private static final String[] HTML_TAG_WHITELIST = {"strong", "em", "span", "p", "a", "hr", "address", "pre", "h1", "h2", "h3", "h4", "h5", "h6", "u"};
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet#isAccessAllowed(
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse,
	 * de.cinovo.surveyplatform.servlets.AbstractSccServlet.Method)
	 */
	@Override
	public boolean isAccessAllowed(final HttpServletRequest req, final Method method, final SystemUser currentUser) {
		if (method.equals(Method.RETRIEVE) | AuthUtil.isAllowedToEditSurveys(currentUser)) {
			return true;
		}
		return false;
	}
	
	@Override
	public void processCreate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		if (ParamUtil.checkAllParamsSet(req, PARAM_SURVEYNAME, PARAM_SURVEYTEMPLATE, PARAM_TYPE, PARAM_DESCRIPTION)) {
			
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Transaction tx = hibSess.beginTransaction();
				
				Survey survey = new Survey();
				survey.setSyncId(UUID.randomUUID().toString());
				if (checkParamsValid(req, false)) {
					Survey originSurvey = null;
					int surveyTemplateId = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYTEMPLATE);
					if (surveyTemplateId > 0) {
						try {
							originSurvey = getSurvey(surveyTemplateId, hibSess);
						} catch (EntityNotFoundException enf) {
							originSurvey = null;
							// just create an empty questionnaire if the
							// originSurvey is not found
						}
					}
					// associate user with the current session
					// DO NOT USE hibSess.merge() because this will
					// overwrite data of the user when the user is logged
					// in multiple times!!
					currentUser = (SystemUser) hibSess.load(SystemUser.class, currentUser.getUserName());
					
					if (AuthUtil.isAllowedToEditSurveys(currentUser)) {
						
						long groupId = ParamUtil.getSafeIntFromParam(req, PARAM_GROUPID);
						UserGroup ownerGroup;
						if (groupId == 0) {
							// take the default group of the user (the first)
							ownerGroup = currentUser.getUserGroups().iterator().next();
						} else {
							ownerGroup = (UserGroup) hibSess.load(UserGroup.class, groupId);
						}
						
						// ownerGroup is mandatory
						if (ownerGroup != null) {
							
							String type = ParamUtil.getSafeParam(req, PARAM_TYPE);
							
							// create a survey containing one page and an empty
							// section
							if (originSurvey == null) {
								createDefaultQuestionnaire(survey);
								survey.setEmailSender(currentUser.getEmail());
								survey.setSenderName(ownerGroup.getClient().getOrganization());
							} else {
								SurveyElementFactory.getInstance().correctIds(originSurvey);
								Questionnaire questionnaireClone = originSurvey.getQuestionnaire().clone();
								SurveyUtil.resetAnswers(questionnaireClone);
								survey.setQuestionnaire(questionnaireClone);
								survey.setClosedAtDate(null);
								survey.setRunningSinceDate(null);
								survey.setCreationDate(new Date());
								survey.setEmailTextInvite(originSurvey.getEmailTextInvite());
								survey.setEmailTextRemind(originSurvey.getEmailTextRemind());
								survey.setEmailSender(originSurvey.getEmailSender());
								survey.setSenderName(originSurvey.getSenderName());
							}
							
							survey.setCreator(currentUser);
							survey.setOwner(ownerGroup);
							survey.setName(ParamUtil.getSafeParam(req, PARAM_SURVEYNAME));
							survey.setDescription(ParamUtil.getSafeParam(req, PARAM_DESCRIPTION));
							
							// save must be triggered here, that ids are set.
							// this is needed for creating the participants of
							// which each gets a clone of the questionnaire
							// where each question has an entry for the origin
							// question ID
							hibSess.save(survey);
							
							// copy the configuration xml files to the target
							SurveyElementFactory.getInstance().copySurveyMetaData(originSurvey, survey);
							
							// options have the id of the origin question to
							// improve performance on analysing reports
							SurveyElementFactory.getInstance().correctIds(survey);
							
							// set SYSTEMTEMPLATE state only if parameter is set
							// and the
							// user has according rights
							if (type.equals(SURVEY_TYPE_SYSTEMTEMPLATE)) {
								survey.setState(SurveyState.SYSTEMTEMPLATE);
								survey.setParticipants(null);
								Logger.logUserActivity("Created a public template: " + survey.getName() + "(" + survey.getId() + ")", currentUser.getUserName());
							} else {
								if (type.equals(SURVEY_TYPE_TEMPLATE)) {
									survey.setState(SurveyState.TEMPLATE);
									Logger.logUserActivity("Created a private template: " + survey.getName() + "(" + survey.getId() + ")", currentUser.getUserName());
								} else {
									survey.setState(SurveyState.CREATED);
									Logger.logUserActivity("Created a concrete survey: " + survey.getName() + "(" + survey.getId() + ")", currentUser.getUserName());
								}
								if (originSurvey != null) {
									boolean createParticipation = type.equals(SURVEY_TYPE_CONCRETE);
									if (ParamUtil.checkAllParamsSet(req, PARAM_INCLUDEPARTICIPANTS)) {
										Survey participantSourceSurvey = getSurvey(ParamUtil.getSafeIntFromParam(req, PARAM_INCLUDEPARTICIPANTS), hibSess);
										if (participantSourceSurvey != null) {
											
											for (Participant p : participantSourceSurvey.getParticipants()) {
												int participantNumber = SurveyUtil.getNextParticipantNumber(hibSess, survey);
												Participant participant = SurveyElementFactory.getInstance().createParticipant(survey, createParticipation);
												participant.setNumber(participantNumber);
												participant.setContactEmail(p.getContactEmail());
												participant.setName(p.getName());
												participant.setContactPhone(p.getContactPhone());
												participant.setSurname(p.getSurname());
												if (p.getProperties() != null) {
													for (Entry<String, String> entry : p.getProperties().getProperties().entrySet()) {
														participant.getProperties().setProperty(entry.getKey(), entry.getValue());
													}
												}
												hibSess.save(participant);
											}
										}
									}
								}
							}
							tx.commit();
						} else {
							handlePermissionDenied(req, resp);
						}
					} else {
						handlePermissionDenied(req, resp);
					}
				}
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
			
		} else if (ParamUtil.checkAllParamsSet(req, PARAM_PART)) {
			String part = ParamUtil.getSafeParam(req, PARAM_PART);
			Serializable newId = null;
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Survey survey = getSurvey(ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID), hibSess);
				if (survey == null) {
					if (ParamUtil.checkAllParamsSet(req, PARAM_PAGEID)) {
						Page page = (Page) hibSess.load(Page.class, ParamUtil.getSafeIntFromParam(req, PARAM_PAGEID));
						survey = (Survey) hibSess.createQuery(Queries.SURVEY_BY_PAGE).setParameter("1", page.getId()).uniqueResult();
					} else if (ParamUtil.checkAllParamsSet(req, PARAM_SECTIONID)) {
						Section section = (Section) hibSess.load(Section.class, ParamUtil.getSafeIntFromParam(req, PARAM_SECTIONID));
						survey = (Survey) hibSess.createQuery(Queries.SURVEY_BY_PAGE).setParameter("1", section.getPage().getId()).uniqueResult();
					}
				}
				
				if (AuthUtil.isAllowedToEditThisSurvey(currentUser, survey, hibSess)) {
					Transaction tx = hibSess.beginTransaction();
					if (part.equals(PARTNAME_PAGE) && ParamUtil.checkAllParamsSet(req, PARAM_SURVEYID)) {
						
						Page page = SurveyElementFactory.getInstance().createPage();
						List<Page> pages = survey.getQuestionnaire().getPages();
						page.setTitle("Page " + pages.size() + 1);
						if (ParamUtil.checkAllParamsSet(req, PARAM_INSERTAT)) {
							// page shall be inserted:
							// first multiply all ordernumbers by 10
							// then set the new page to currentPage.orderNumber
							// - 1
							int currentPageId = ParamUtil.getSafeIntFromParam(req, PARAM_INSERTAT);
							int orderNumberCurrentPage = 0;
							Iterator<Page> iterator = pages.iterator();
							int count = 0;
							while (iterator.hasNext()) {
								Page pageIter = iterator.next();
								pageIter.setOrderNumber((count + 1) * 10);
								if (pageIter.getId() == currentPageId) {
									orderNumberCurrentPage = pageIter.getOrderNumber();
								}
								count++;
							}
							if (orderNumberCurrentPage > 0) {
								page.setOrderNumber(orderNumberCurrentPage - 1);
							}
						} else {
							page.setOrderNumber(getNextPageOrderNumber(hibSess));
						}
						pages.add(page);
						page.setQuestionnaire(survey.getQuestionnaire());
						newId = hibSess.save(page);
						page.setLocalId(page.getId());
						hibSess.save(survey);
						Logger.logUserActivity("Created page: " + page.getId(), currentUser.getUserName());
					} else if (part.equals(PARTNAME_SECTION) && ParamUtil.checkAllParamsSet(req, PARAM_PAGEID)) {
						Page page = (Page) hibSess.load(Page.class, ParamUtil.getSafeIntFromParam(req, PARAM_PAGEID));
						Section section = SurveyElementFactory.getInstance().createSection("New Section");
						section.setOrderNumber(getNextSectionOrderNumber(hibSess));
						page.getSections().add(section);
						section.setPage(page);
						newId = hibSess.save(section);
						section.setLocalId(section.getId());
						hibSess.save(section);
						hibSess.save(page);
						Logger.logUserActivity("Created section: " + section.getId(), currentUser.getUserName());
					} else if (part.equals(PARTNAME_QUESTION) && ParamUtil.checkAllParamsSet(req, PARAM_SECTIONID, PARAM_QUESTION_TEXT, PARAM_QUESTION_TYPE)) {
						Section section = (Section) hibSess.load(Section.class, ParamUtil.getSafeIntFromParam(req, PARAM_SECTIONID));
						AbstractQuestion question = createQuestion(req, hibSess);
						if (question != null) {
							question.setOrderNumber(getNextQuestionOrderNumber(hibSess));
							section.getQuestions().add(question);
							question.setSection(section);
							newId = hibSess.save(question);
							question.setLocalId(question.getId());
							
							// options have the id of the origin question to
							// improve performance on analysing reports
							SurveyElementFactory.getInstance().correctOptionIds(question, 0);
							hibSess.save(section);
							Logger.logUserActivity("Created question: " + question.getId(), currentUser.getUserName());
						}
					}
					tx.commit();
				} else {
					handlePermissionDenied(req, resp);
				}
			} catch (Exception e) {
				Logger.errUnexpected(e, currentUser.getUserName());
				resp.getWriter().write("Error: " + e);
			} finally {
				hibSess.close();
			}
			if (newId != null) {
				resp.getWriter().write(newId.toString());
			}
		} else {
			resp.sendRedirect(getStandardRedirectLocation(req));
		}
	}
	
	private Survey getSurvey(final int surveyID, final Session hibSess) {
		return (Survey) hibSess.get(Survey.class, surveyID);
	}
	
	/**
	 * @param survey
	 */
	private void createDefaultQuestionnaire(final Survey survey) {
		Questionnaire questionare = new Questionnaire();
		questionare.setSyncId(UUID.randomUUID().toString());
		Page page = SurveyElementFactory.getInstance().createPage();
		Section section = SurveyElementFactory.getInstance().createSection("Welcome");
		
		page.getSections().add(section);
		section.setPage(page);
		questionare.getPages().add(page);
		page.setQuestionnaire(questionare);
		
		TextPart welcomeTextQuestion = SurveyElementFactory.getInstance().createTextPart("", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/welcomePage.html", null));
		section.setQuestions(new ArrayList<AbstractQuestion>());
		section.getQuestions().add(welcomeTextQuestion);
		welcomeTextQuestion.setSection(section);
		
		Page firstPage = SurveyElementFactory.getInstance().createPage();
		section = SurveyElementFactory.getInstance().createSection("Section A");
		firstPage.getSections().add(section);
		section.setPage(firstPage);
		questionare.getPages().add(firstPage);
		firstPage.setQuestionnaire(questionare);
		
		Page lastPage = SurveyElementFactory.getInstance().createPage();
		section = SurveyElementFactory.getInstance().createSection("THANK YOU!");
		lastPage.getSections().add(section);
		section.setPage(lastPage);
		questionare.getPages().add(lastPage);
		lastPage.setQuestionnaire(questionare);
		
		TextPart thankYouTextQuestion = SurveyElementFactory.getInstance().createTextPart("", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/thankYouPage.html", null));
		section.setQuestions(new ArrayList<AbstractQuestion>());
		section.getQuestions().add(thankYouTextQuestion);
		thankYouTextQuestion.setSection(section);
		survey.setQuestionnaire(questionare);
		
	}
	
	/**
	 * @param req
	 * @return
	 */
	private boolean checkParamsValid(final HttpServletRequest req, final boolean editOnly) {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		String taskID = "page." + Pages.PAGE_MANAGE_SURVEYS + "." + req.getSession().getId();
		FeedBackProvider fbp = FeedBackProvider.getInstance();
		
		// PARAM_SURVEYNAME, PARAM_SURVEYTEMPLATE, PARAM_TYPE, PARAM_DESCRIPTION
		String surveyName = null;
		try {
			if (ParamUtil.checkAllParamsSet(req, PARAM_SURVEYNAME)) {
				surveyName = ParamUtil.getSafeParam(req, PARAM_SURVEYNAME);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}
		
		// if (ParamUtil.checkAllParamsSet(req, PARAM_DESCRIPTION)) {
		// surveyString = ParamUtil.getSafeParam(req, PARAM_DESCRIPTION);
		// }
		
		
		boolean valid = true;
		boolean validChars = true;
		if (surveyName != null) {
			if (surveyName.trim().isEmpty()) {
				fbp.addFeedback(taskID, "Please enter a name for the survey!", Status.ERROR, currentUser.getActualUserName());
				valid = false;
			}
			
			String[] surveyChars = surveyName.split("");
			for (String surveyChar : surveyChars) {
				if (surveyChar.equals("<") || surveyChar.equals(">") || surveyChar.equals("\\")) {
					validChars = false;
					valid = false;
				}
			}
		}
		
		
		// if (req.getParameter(PARAM_TYPE).equals(SURVEY_TYPE_CONCRETE)) {
		// if ((req.getParameter(PARAM_SURVEYTEMPLATE) == null) ||
		// req.getParameter(PARAM_SURVEYTEMPLATE).equals("-1")) {
		// fbp.addFeedback(taskID,
		// "You cannot create a survey without making a copy of an existing template or creating an entirely new template. Please choose to make a copy of a template (Type: Survey) or select Type 'Template' to create a new template.",
		// Status.ERROR, currentUser.getActualUserName());
		// valid = false;
		// }
		// } else
		if (ParamUtil.checkAllParamsSet(req, PARAM_TYPE)) {
			if (req.getParameter(PARAM_TYPE).equals(SURVEY_TYPE_SYSTEMTEMPLATE)) {
				if (!AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
					fbp.addFeedback(taskID, "You do not have sufficient rights to create an empty survey template. Please choose to make a copy of a template.", Status.ERROR, currentUser.getActualUserName());
					valid = false;
				}
			}
			
		}
		
		if (valid && !editOnly) {
			fbp.setMessage(taskID, "Survey " + surveyName + " successfully created!", currentUser.getActualUserName());
		} else if (valid && editOnly) {
			if (surveyName != null) {
				fbp.setMessage(taskID, "Survey " + surveyName + " successfully edited!", currentUser.getActualUserName());
			}
		} else {
			if (!validChars) {
				fbp.setMessage(taskID, "Could not accept the survey name (invalid characters). Invalid Characters are: \"<\", backslash, \">\".", currentUser.getActualUserName());
			} else {
				fbp.setMessage(taskID, "Could not create the survey!", currentUser.getActualUserName());
			}
		}
		fbp.finishTask(taskID, !valid);
		return valid;
	}
	
	private int getNextQuestionOrderNumber(final Session hibSess) {
		Query query = hibSess.createQuery(Queries.GET_MAX_QUESTION_ORDERNUMBER);
		Object o = query.uniqueResult();
		if (o instanceof Integer) {
			return ((Integer) o).intValue() + 1;
		}
		return 0;
	}
	
	private int getNextSectionOrderNumber(final Session hibSess) {
		Query query = hibSess.createQuery(Queries.GET_MAX_SECTION_ORDERNUMBER);
		Object o = query.uniqueResult();
		if (o instanceof Integer) {
			return ((Integer) o).intValue() + 1;
		}
		return 0;
	}
	
	private int getNextPageOrderNumber(final Session hibSess) {
		Query query = hibSess.createQuery(Queries.GET_MAX_PAGE_ORDERNUMBER);
		Object o = query.uniqueResult();
		if (o instanceof Integer) {
			return ((Integer) o).intValue() + 1;
		}
		return 0;
	}
	
	@Override
	public void processRetrieve(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			if (ParamUtil.checkAllParamsSet(req, PARAM_SURVEYID)) {
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					// See if there is given a part parameter, which signals the
					// DAL
					// only to geht the specific part.
					// Notice that given the surveyID AND partID param is
					// overdetermined
					if (ParamUtil.checkAllParamsSet(req, PARAM_PART)) {
						String part = ParamUtil.getSafeParam(req, PARAM_PART);
						if (part.equals(PARTNAME_QUESTION) && ParamUtil.checkAllParamsSet(req, PARAM_QUESTIONID)) {
							// return specific question
							try {
								Transaction tx = hibSess.beginTransaction();
								
								// cannot use session.load() here because it
								// would
								// make cast impossible
								// not working => p = (AbstractQuestion)hibSess.load(AbstractQuestion.class,
								// Integer.parseInt());
								Query query = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_ID);
								query.setParameter("1", ParamUtil.getSafeIntFromParam(req, PARAM_QUESTIONID));
								Object o = query.uniqueResult();
								tx.commit();
								if (o != null) {
									resp.getWriter().write(DtoFactory.getInstance().createDto((AbstractQuestion) o).getJSON());
								} else {
									resp.getWriter().write("{}");
								}
							} catch (Exception e) {
								Logger.err("Ein unerwarteter Fehler ist aufgetreten", e);
								resp.getWriter().write("Error: " + e);
							} finally {
								hibSess.close();
							}
						} else if (part.equals(PARTNAME_QUESTION_EDITTABLE)) {
							Map<String, String> replacements = new HashMap<String, String>();
							if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTIONID)) {
								Transaction tx = hibSess.beginTransaction();
								Query query = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_ID);
								query.setParameter("1", ParamUtil.getSafeIntFromParam(req, PARAM_QUESTIONID));
								AbstractQuestion question = (AbstractQuestion) query.uniqueResult();
								tx.commit();
								replacements = getDataFromQuestion(question);
								replacements.put("DISABLED", "disabled=\"disabled\"");
							}
							replacements.put("SURVEYID", ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID) + "");
							replacements.put("HELPLINK", ContextHelpProvider.getInstance().getHelpLink(HelpIDs.DLG_CREATEQUESTION, "Show examples", "", "float: none; background-position-y: 5px;"));
							
							resp.getWriter().write(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableEditQuestion.html", replacements));
							
						} else {
							Transaction tx = hibSess.beginTransaction();
							Survey survey = getSurvey(ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID), hibSess);
							String output = "";
							if (part.equals(PARTNAME_EMAIL_INVITE)) {
								output = SurveyUtil.getEmailTextInvite(survey);
							} else if (part.equals(PARTNAME_EMAIL_REMIND)) {
								output = SurveyUtil.getEmailTextRemind(survey);
							} else if (part.equals(PARTNAME_EMAIL_SUBJECT_INVITE)) {
								if (survey.getEmailSubjectInvite() != null) {
									output = survey.getEmailSubjectInvite();
								}
							} else if (part.equals(PARTNAME_EMAIL_SUBJECT_REMIND)) {
								if (survey.getEmailSubjectRemind() != null) {
									output = survey.getEmailSubjectRemind();
								}
							}
							resp.getWriter().write(output);
							tx.commit();
						}
						
					} else {
						// return complete survey
						final PrintWriter writer = resp.getWriter();
						try {
							Transaction tx = hibSess.beginTransaction();
							Survey survey = getSurvey(ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID), hibSess);
							tx.commit();
							
							if (survey != null) {
								if (ParamUtil.checkAllParamsSet(req, Constants.PARAM_HTML)) {
									Map<String, String> replacements = new HashMap<String, String>();
									
									String actionTemplate = Paths.TEMPLATEPATH + "/savePartOfSurveyAction.html";
									StringStringPair kvpSurveyID = new StringStringPair("SURVEYID", survey.getId() + "");
									replacements.put("ENTITY", "survey");
									replacements.put("ENTITYID", survey.getId() + "");
									boolean allowedToEditThisSurvey = AuthUtil.isAllowedToEditThisSurvey(currentUser, survey, hibSess);
									if (allowedToEditThisSurvey) {
										replacements.put("UPLOAD_LINK_TITLE", "Upload a logo for this survey");
										replacements.put("REMOVE_LINK_TITLE", "Remove the logo from this survey");
										
										if (new File(Paths.SURVEYLOGOS + "/" + survey.getId() + ".jpg").exists()) {
											replacements.put("SURVEYIMAGE", PartsUtil.getSurveyLogo(survey.getId(), 0, true, true, true) + "<br />" + TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/removeEntityPictureLink.html", replacements));
										} else if (!EnvironmentConfiguration.isOfflineMode()) {
											replacements.put("SURVEYIMAGE", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/uploadEntityPictureLink.html", replacements));
										}
									} else {
										if (new File(Paths.SURVEYLOGOS + "/" + survey.getId() + ".jpg").exists()) {
											replacements.put("SURVEYIMAGE", PartsUtil.getSurveyLogo(survey.getId(), 0, true, true, true));
										}
									}
									replacements.put("BUTTONCELL", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/buttonCell.html", replacements));
									UserGroup owner = survey.getOwner();
									if (!EnvironmentConfiguration.isOfflineMode() && AuthUtil.isAllowedToEditSurveys(currentUser)) {
										
										if (survey.getState().equals(SurveyState.CREATED) || survey.getState().equals(SurveyState.RUNNING) || survey.getState().equals(SurveyState.CLOSED)) {
											Map<String, String> buttonReplacements = new HashMap<String, String>();
											buttonReplacements.put("SURVEYID", survey.getId() + "");
											if (survey.getState().equals(SurveyState.CREATED)) {
												if (survey.getParticipants().size() == 0) {
													buttonReplacements.put("BUTTON_ADDPARTICIPANTS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfoDialogButtonAddParticipants.html", buttonReplacements));
												} else {
													buttonReplacements.put("BUTTON_CONDUCTSURVEY", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfoDialogButtonConductSurvey.html", buttonReplacements));
												}
											} else if (survey.getState().equals(SurveyState.RUNNING)) {
												buttonReplacements.put("BUTTON_CLOSESURVEY", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfoDialogButtonCloseSurvey.html", null));
											} else if (survey.getState().equals(SurveyState.CLOSED)) {
												buttonReplacements.put("BUTTON_ANALYSESURVEY", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfoDialogButtonsAnalyse.html", buttonReplacements));
											}
											if (buttonReplacements.size() > 0) {
												replacements.put("DIALOGBUTTONS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfoDialogButtonsManage.html", buttonReplacements));
											}
										}
										
										replacements.put("NAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_SURVEYNAME, PARAM_SURVEYNAME, survey.getName(), actionTemplate, kvpSurveyID));
										replacements.put("DESCRIPTION_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(PARAM_DESCRIPTION, PARAM_DESCRIPTION, survey.getDescription(), actionTemplate, kvpSurveyID));
										List<StringStringPair> options = new ArrayList<StringStringPair>();
										if ((survey.getState().ordinal() < SurveyState.RUNNING.ordinal())) {
											
											if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
												options.add(new StringStringPair(SurveyState.SYSTEMTEMPLATE.toString(), SurveyState.getDisplayName(SurveyState.SYSTEMTEMPLATE)));
											}
											options.add(new StringStringPair(SurveyState.TEMPLATE.toString(), SurveyState.getDisplayName(SurveyState.TEMPLATE)));
											if (!survey.getState().equals(SurveyState.TEMPLATE) && !survey.getState().equals(SurveyState.SYSTEMTEMPLATE)) {
												options.add(new StringStringPair(SurveyState.CREATED.toString(), SurveyState.getDisplayName(SurveyState.CREATED)));
											}
										} else {
											options.add(new StringStringPair(SurveyState.RUNNING.toString(), SurveyState.getDisplayName(SurveyState.RUNNING)));
											options.add(new StringStringPair(SurveyState.CLOSED.toString(), SurveyState.getDisplayName(SurveyState.CLOSED)));
										}
										replacements.put("STATE_TEXTFIELD", HtmlFormUtil.getDynamicEditableComboBox(PARAM_STATE, PARAM_STATE, options, survey.getState().toString(), actionTemplate, kvpSurveyID));
										
										options = new ArrayList<StringStringPair>();
										{
											// Collection<UserGroup> groups =
											// null;
											if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
												@SuppressWarnings("unchecked")
												Set<UserGroup> allGroups = new LinkedHashSet<UserGroup>(hibSess.createCriteria(UserGroup.class).list());
												TreeNode<UserGroup> rootNode = TreeUtil.convertTableToTree(allGroups, "getId", "getParentGroup");
												buildGroupTree(options, rootNode, 0, true);
											} else {
												Set<UserGroup> groups = (Set<UserGroup>) GroupManager.getVisibleGroups(hibSess, currentUser, currentUser);
												TreeNode<UserGroup> rootNode = TreeUtil.convertTableToTree(groups, "getId", "getParentGroup");
												buildGroupTree(options, rootNode, 0, false);
												// for (UserGroup group :
												// groups) {
												// options.add(new
												// StringStringPair(String.valueOf(group.getId()),
												// group.getName()));
												// }
											}
										}
										replacements.put("OWNER_TEXTFIELD", HtmlFormUtil.getDynamicEditableComboBox(PARAM_OWNER, PARAM_OWNER, options, String.valueOf(owner.getId()), actionTemplate, kvpSurveyID));
										
									} else {
										replacements.put("NAME_TEXTFIELD", survey.getName());
										replacements.put("DESCRIPTION_TEXTFIELD", survey.getDescription());
										replacements.put("STATE_TEXTFIELD", survey.getStateDisplayname());
										replacements.put("OWNER_TEXTFIELD", owner == null ? "---" : owner.getName());
									}
									
									replacements.put("DATECREATION_TEXTFIELD", survey.getCreationDate() != null ? TimeUtil.getLocalTime(currentUser, survey.getCreationDate()) : "-");
									replacements.put("DATERUNNING_TEXTFIELD", survey.getRunningSinceDate() != null ? TimeUtil.getLocalTime(currentUser, survey.getRunningSinceDate()) : "-");
									replacements.put("DATECLOSED_TEXTFIELD", survey.getClosedAtDate() != null ? TimeUtil.getLocalTime(currentUser, survey.getClosedAtDate()) : "-");
									
									if (survey.getParticipants() != null) {
										replacements.put("TOTALPARTICIPANTS_TEXTFIELD", survey.getParticipants().size() + "");
										int submittedCount = 0;
										for (Participant p : survey.getParticipants()) {
											if (p.getSurveySubmitted() != null) {
												submittedCount++;
											}
										}
										replacements.put("SUBMITTEDQUESTIONNARES_TEXTFIELD", submittedCount + "");
										replacements.put("RETURNRATE_TEXTFIELD", SurveyUtil.calculateReturnRate(currentUser, survey) + "%");
									}
									
									if (survey.getDescription().toUpperCase().contains("EXTENDED_PROPERTIES=YES")) {
										replacements.put("EXTENDED_PROPERTIES_CONTENT", survey.getExtendedProperties());
										replacements.put("ADDITIONAL_STUFF", getAdditionalStuff(survey));
										replacements.put("EXTENDED_PROPERTIES", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/extendedSurveyProperties.html", replacements));
									}
									
									writer.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableSurveyInfo.html", replacements));
									writer.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgUploadPicture.html", replacements));
								} else {
									writer.write(DtoFactory.getInstance().createDto(currentUser, survey).getJSON());
								}
							} else {
								writer.write("{}");
							}
						} catch (Exception e) {
							Logger.errUnexpected(e, currentUser.getUserName());
							writer.write("Error: " + e);
						}
					}
				} finally {
					hibSess.close();
				}
				
			} else {
				// return all surveys
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					
					Criteria criteria = hibSess.createCriteria(Survey.class);
					criteria.add(Restrictions.in("owner", GroupManager.getVisibleGroups(hibSess, currentUser, currentUser)));
					criteria.addOrder(Order.asc("name"));
					List<SurveyDto> surveyDtoList = new ArrayList<SurveyDto>();
					List<?> list = criteria.list();
					tx.commit();
					if ((list != null) && (list.size() > 0)) {
						for (Object obj : list) {
							if (obj instanceof Survey) {
								surveyDtoList.add(DtoFactory.getInstance().createDto(currentUser, (Survey) obj));
							}
						}
						// Create a JSON Representation of the List
						Gson gson = new Gson();
						String jsonRepresentation = gson.toJson(surveyDtoList);
						resp.getWriter().write(jsonRepresentation);
						
					} else {
						resp.getWriter().write("{}");
					}
				} catch (Exception e) {
					Logger.errUnexpected(e, currentUser.getUserName());
					resp.getWriter().write("Error: " + e);
				} finally {
					hibSess.close();
				}
			}
		} else {
			super.handlePermissionDenied(req, resp);
		}
	}
	
	
	private String getAdditionalStuff(final Survey survey) {
		StringBuilder stuff = new StringBuilder();
		
		if (ExtendedProperties.get(survey, ExtendedProperties.SURVEY_ISPUBLIC, false)) {
			stuff.append("<a href=\"" + EnvironmentConfiguration.getUrlBase() + "/participate?surveyID=" + survey.getId() + "\">Link to questionnaire</a>");
		}
		
		return stuff.toString();
	}
	
	private void buildGroupTree(final List<StringStringPair> options, final TreeNode<UserGroup> node, final int depth, final boolean showClients) {
		Set<TreeNode<UserGroup>> children = node.getChildren();
		if (children != null) {
			for (TreeNode<UserGroup> child : children) {
				UserGroup group = child.getData();
				StringBuilder displayName = new StringBuilder();
				if (showClients) {
					displayName.append(group.getClient().getOrganization() + " - ");
				}
				
				for (int i = 0; i < (depth * 2); i++) {
					displayName.append("&#160;");
				}
				
				displayName.append(group.getName());
				StringStringPair pair = new StringStringPair(child.getData().getId() + "", displayName.toString());
				pair.setMeta((depth * 20));
				options.add(pair);
				buildGroupTree(options, child, depth + 1, showClients);
			}
		}
	}
	
	/**
	 * @param question
	 * @return
	 */
	private Map<String, String> getDataFromQuestion(final AbstractQuestion question) {
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("QUESTIONTEXT", question.getQuestion());
		replacements.put("QUESTIONALIAS", question.getAlias());
		replacements.put("QUESTIONADDINFO", question.getAdditionalInfo());
		
		StringBuilder optionsString = new StringBuilder();
		
		if (question instanceof IMatrixQuestion) {
			StringBuilder subQuestionsString = new StringBuilder();
			List<? extends AbstractQuestion> subquestions = question.getSubquestions();
			for (AbstractQuestion subQuestion : subquestions) {
				subQuestionsString.append("<li class=\"optionListItem\" style=\"vertical-align: middle;\"><span class=\"optionRemoveButton\"></span><span class=\"optionMovableButton\"></span><input style=\"width: 550px;\" class=\"gui-dialog-field-width\" type=\"text\" name=\"" + PARAM_SUBQUESTIONS + "\" value=\"" + subQuestion.getQuestion() + "\" /></li>");
				
			}
			replacements.put("SUBQUESTIONS", subQuestionsString.toString());
			if (subquestions.size() > 0) {
				List<Option> options = subquestions.get(0).getOptions();
				for (Option option : options) {
					optionsString.append("<li class=\"optionListItem\"><span class=\"optionRemoveButton\" style=\"vertical-align: middle;\"></span><span class=\"optionMovableButton\"></span><input style=\"width: 550px;\" class=\"gui-dialog-field-width\" type=\"text\" name=\"" + PARAM_OPTIONS + "\" value=\"" + option.getDisplayName() + "\" /></li>");
				}
				replacements.put("OPTIONS", optionsString.toString());
			}
			
		} else if (question instanceof IMultipleOptionsQuestion) {
			for (Option option : question.getOptions()) {
				optionsString.append("<li class=\"optionListItem\"><span class=\"optionRemoveButton\" style=\"vertical-align: middle;\"></span><span class=\"optionMovableButton\"></span><input style=\"width: 550px;\" class=\"gui-dialog-field-width\" type=\"text\" name=\"options[]\" value=\"" + option.getDisplayName() + "\" /></li>");
			}
			replacements.put("OPTIONS", optionsString.toString());
		}
		
		if (question instanceof IAlignmentQuestion) {
			Alignment alignment = ((IAlignmentQuestion) question).getAlignment();
			if (alignment == Alignment.HORIZONTAL) {
				replacements.put("SELECTED_HORIZONTAL", " checked=\"checked\"");
			} else if (alignment == Alignment.VERTICAL) {
				replacements.put("SELECTED_VERTICAL", " checked=\"checked\"");
			}
		}
		
		if (question instanceof ComboQuestion) {
			replacements.put("SELECTED_COMBO", " selected=\"selected\"");
		} else if (question instanceof SingleLineQuestion) {
			replacements.put("SELECTED_SINGLELINE", " selected=\"selected\"");
			replacements.put("SINGLELINE_HINT", ((SingleLineQuestion) question).getHint());
		} else if (question instanceof AverageNumberQuestion) {
			replacements.put("SELECTED_AVERAGE_NUM", " selected=\"selected\"");
			replacements.put("AVERAGE_NUM_HINT", ((AverageNumberQuestion) question).getHint());
		} else if (question instanceof FreeTextQuestion) {
			replacements.put("SELECTED_FREETEXT", " selected=\"selected\"");
		} else if (question instanceof MultipleChoiceMatrixQuestion) {
			replacements.put("SELECTED_MULTIPLEMATRIX", " selected=\"selected\"");
		} else if (question instanceof MultipleChoiceQuestion) {
			replacements.put("SELECTED_MULTIPLE", " selected=\"selected\"");
		} else if (question instanceof RadioMatrixQuestion) {
			replacements.put("SELECTED_RADIOMATRIX", " selected=\"selected\"");
		} else if (question instanceof RadioQuestion) {
			replacements.put("SELECTED_RADIO", " selected=\"selected\"");
		} else if (question instanceof TextfieldQuestion) {
			replacements.put("SELECTED_TEXTFIELD", " selected=\"selected\"");
		} else if (question instanceof TextPart) {
			replacements.put("SELECTED_TEXTPART", " selected=\"selected\"");
			replacements.put("TEXTVALUE", ((TextPart) question).getTextValue());
		}
		return replacements;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @seescc.servlets.AbstractSccServlet#processUpdate(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processUpdate(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		
		SystemUser currentUser = AuthUtil.checkAuth(req);
		
		if (currentUser != null) {
			
			if (ServletFileUpload.isMultipartContent(req)) {
				handlePictureUpload(req);
				resp.sendRedirect(getStandardRedirectLocation(req) + "#open=" + ((String) req.getAttribute(PARAM_SURVEYID)));
				return;
			}
			
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTIONID) && ParamUtil.checkAllParamsSet(req, PARAM_BAR_CHECKED) && ParamUtil.checkAllParamsSet(req, PARAM_PIE_CHECKED) && ParamUtil.checkAllParamsSet(req, PARAM_SURVEYID)) {
					Query chartTypeQuery = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_ID);
					Transaction tx = hibSess.beginTransaction();
					chartTypeQuery.setParameter("1", ParamUtil.getSafeIntFromParam(req, PARAM_QUESTIONID));
					int surveyID = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
					Survey survey = getSurvey(surveyID, hibSess);
					StringBuilder divContent = new StringBuilder();
					SummaryReportGenerator sumRepGenObject = new SummaryReportGenerator(new SummaryReport(), survey);
					Object o = chartTypeQuery.uniqueResult();
					if ((o != null) && (o instanceof AbstractQuestion)) {
						AbstractQuestion q = (AbstractQuestion) o;
						ChartType chartType = QuestionnaireViewUtil.getChartType(q);
						if (req.getParameter(PARAM_BAR_CHECKED).equals("true") && chartType.equals(ChartType.pie)) {
							q.setChartType(ChartType.bar);
						} else if (req.getParameter(PARAM_PIE_CHECKED).equals("true") && chartType.equals(ChartType.bar)) {
							q.setChartType(ChartType.pie);
						} else {
							Logger.err("An error occurred, while transferring the HTML request.");
						}
						divContent.append(sumRepGenObject.evaluate(q, TargetMedia.SCREEN, SubTypeEnum.COMBINED, "changeChartType"));
						resp.getWriter().write(divContent.toString());
					}
					tx.commit();
					return;
				}
				
				if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTIONID) && ParamUtil.checkAllParamsSet(req, PARAM_CHECKED)) {
					Query interestingQuestionQuery = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_ID);
					Transaction tx = hibSess.beginTransaction();
					interestingQuestionQuery.setParameter("1", ParamUtil.getSafeIntFromParam(req, PARAM_QUESTIONID));
					Object o = interestingQuestionQuery.uniqueResult();
					if ((o != null) && (o instanceof AbstractQuestion)) {
						AbstractQuestion q = (AbstractQuestion) o;
						if (req.getParameter(PARAM_CHECKED).equals("true")) {
							q.setInteresting(true);
						} else {
							q.setInteresting(false);
						}
						
					}
					tx.commit();
					return;
				}
				
				if (ParamUtil.checkAllParamsSet(req, PARAM_SEND_EMAIL_TEST)) {
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_EMAILTEXT, PARAM_EMAILTEXT_TYPE)) {
						
						String emailText = ParamUtil.getSafeParam(req, PARAM_EMAILTEXT);
						String emailSubject = ParamUtil.getSafeParam(req, PARAM_EMAILSUBJECT);
						String emailReceiver = ParamUtil.getSafeParam(req, PARAM_TEST_MAIL_RECEIVER);
						
						int surveyID = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
						Survey survey = getSurvey(surveyID, hibSess);
						Map<String, String> replacements = new HashMap<String, String>();
						replacements.put("NAME_OF_PARTICIPANT", currentUser.getFirstName() + " " + currentUser.getLastName());
						replacements.put("ORGANIZATION", survey.getOwner().getClient().getOrganization());
						String link = getServerBaseUrl() + "/participate?pid=preview&reset=1&surveyID=" + surveyID;
						replacements.put("LINK", link);
						
						String sender = survey.getEmailSender();
						String senderName = survey.getSenderName();
						if (sender == null) {
							sender = String.valueOf(EnvironmentConfiguration.getConfiguration(ConfigID.EMAIL_SENDER));
						}
						if (senderName == null) {
							senderName = "Metior Solutions Survey Platform";
						}
						try {
							EmailManager.getInstance().sendEmail(emailReceiver, currentUser.getFirstName() + " " + currentUser.getLastName(), sender, senderName, emailSubject, replacements, emailText, null);
						} catch (EmailException e) {
							Logger.err("Could not send email to: " + emailReceiver, e);
						}
					}
					
				} else {
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_SURVEYID) && (req.getParameter(PARAM_PART) == null) && !ParamUtil.checkAllParamsSet(req, PARAM_BAR_CHECKED) && !ParamUtil.checkAllParamsSet(req, PARAM_CHECKED)) {
						
						int surveyID = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
						
						if (surveyID > 0) {
							
							Transaction tx = hibSess.beginTransaction();
							
							Survey surveyToChange = getSurvey(surveyID, hibSess);
							
							if (surveyToChange != null) {
								
								if (AuthUtil.isAllowedToEditThisSurvey(currentUser, surveyToChange, hibSess)) {
									if (checkParamsValid(req, true)) {
										
										if (ParamUtil.checkAllParamsSet(req, PARAM_REMOVE_PICTURE)) {
											File pictureFile = new File(Paths.SURVEYLOGOS + "/" + surveyToChange.getId() + ".jpg");
											if (pictureFile.exists()) {
												pictureFile.delete();
											}
										}
										
										if (ParamUtil.checkAllParamsSet(req, PARAM_SURVEYNAME)) {
											String param = ParamUtil.getSafeParam(req, PARAM_SURVEYNAME);
											surveyToChange.setName(param);
										}
										if (ParamUtil.checkAllParamsSet(req, PARAM_DESCRIPTION)) {
											String param = ParamUtil.getSafeParam(req, PARAM_DESCRIPTION);
											surveyToChange.setDescription(param);
										}
										if (ParamUtil.checkAllParamsSet(req, PARAM_EXTENDEDPROPERTIES)) {
											String param = ParamUtil.getSafeParam(req, PARAM_EXTENDEDPROPERTIES);
											surveyToChange.setExtendedProperties(param);
										}
										if (ParamUtil.checkAllParamsSet(req, PARAM_SENDER_NAME)) {
											String senderName = ParamUtil.getSafeParam(req, PARAM_SENDER_NAME);
											surveyToChange.setSenderName(senderName);
										}
										if (ParamUtil.checkAllParamsSet(req, PARAM_E_MAIL_SENDER)) {
											String eMailSender = ParamUtil.getSafeParam(req, PARAM_E_MAIL_SENDER);
											surveyToChange.setEmailSender(eMailSender);
										}
										if (ParamUtil.checkAllParamsSet(req, PARAM_STATE)) {
											String param = ParamUtil.getSafeParam(req, PARAM_STATE);
											surveyToChange.setState(SurveyState.valueOf(param));
										}
										if (ParamUtil.checkAllParamsSet(req, PARAM_OWNER)) {
											long groupId = ParamUtil.getSafeLongFromParam(req, PARAM_OWNER);
											if (groupId != 0) {
												UserGroup group = (UserGroup) hibSess.get(UserGroup.class, groupId);
												if (group != null) {
													surveyToChange.setOwner(group);
												}
											}
										}
										if (ParamUtil.checkAllParamsSet(req, PARAM_EMAILTEXT, PARAM_EMAILTEXT_TYPE)) {
											String newText = ParamUtil.getSafeParam(req, PARAM_EMAILTEXT);
											String emailType = ParamUtil.getSafeParam(req, PARAM_EMAILTEXT_TYPE);
											String emailSubject = ParamUtil.getSafeParam(req, PARAM_EMAILSUBJECT);
											
											if (emailType.equals(PARTNAME_EMAIL_INVITE)) {
												surveyToChange.setEmailTextInvite(newText);
												surveyToChange.setEmailSubjectInvite(emailSubject);
											} else if (emailType.equals(PARTNAME_EMAIL_REMIND)) {
												surveyToChange.setEmailTextRemind(newText);
												surveyToChange.setEmailSubjectRemind(emailSubject);
											}
										}
										
										hibSess.save(surveyToChange);
										tx.commit();
										
										Logger.logUserActivity("Survey updated: " + surveyToChange.getName() + " (" + surveyToChange.getId() + ")", currentUser.getUserName());
										
										resp.getWriter().print(DtoFactory.getInstance().createDto(currentUser, surveyToChange).getJSON());
									}
									
								} else {
									super.handlePermissionDenied(req, resp);
								}
							}
						} else {
							super.handlePermissionDenied(req, resp);
						}
					} else if (ParamUtil.checkAllParamsSet(req, PARAM_PART)) {
						String part = ParamUtil.getSafeParam(req, PARAM_PART);
						try {
							Transaction tx = hibSess.beginTransaction();
							if (part.equals(PARTNAME_SURVEY) && ParamUtil.checkAllParamsSet(req, PARAM_SURVEYID)) {
								Survey targetSurvey = getSurvey(ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID), hibSess);
								
								// perform copy and move of pages
								if (ParamUtil.checkAllParamsSet(req, PARAM_OPERATION, PARAM_PAGEID, PARAM_POSITION)) {
									Page pageToHandle = (Page) hibSess.get(Page.class, ParamUtil.getSafeIntFromParam(req, PARAM_PAGEID));
									int position = ParamUtil.getSafeIntFromParam(req, PARAM_POSITION);
									if (req.getParameter(PARAM_OPERATION).equals(OPERATION_COPY)) {
										copyPage(pageToHandle, targetSurvey.getQuestionnaire(), position, hibSess);
									} else if (req.getParameter(PARAM_OPERATION).equals(OPERATION_MOVE)) {
										movePage(pageToHandle, targetSurvey.getQuestionnaire(), position, hibSess);
									}
								}
								Logger.logUserActivity("Survey updated (page copy/move): " + targetSurvey.getId(), currentUser.getUserName());
							} else if (part.equals(PARTNAME_PAGE) && ParamUtil.checkAllParamsSet(req, PARAM_PAGEID)) {
								Page targetPage = (Page) hibSess.get(Page.class, ParamUtil.getSafeIntFromParam(req, PARAM_PAGEID));
								
								// perform copy and move of sections
								if (ParamUtil.checkAllParamsSet(req, PARAM_OPERATION, PARAM_SECTIONID, PARAM_POSITION)) {
									Section sectionToHandle = (Section) hibSess.get(Section.class, ParamUtil.getSafeIntFromParam(req, PARAM_SECTIONID));
									int position = ParamUtil.getSafeIntFromParam(req, PARAM_POSITION);
									if (req.getParameter(PARAM_OPERATION).equals(OPERATION_COPY)) {
										copySection(sectionToHandle, targetPage, position, hibSess);
									} else if (req.getParameter(PARAM_OPERATION).equals(OPERATION_MOVE)) {
										moveSection(sectionToHandle, targetPage, position, hibSess);
									}
								}
								Logger.logUserActivity("Page updated (section copy/move): " + targetPage.getId(), currentUser.getUserName());
								
							} else if (part.equals(PARTNAME_SECTION) && ParamUtil.checkAllParamsSet(req, PARAM_SECTIONID)) {
								Section section = (Section) hibSess.load(Section.class, ParamUtil.getSafeIntFromParam(req, PARAM_SECTIONID));
								if (ParamUtil.checkAllParamsSet(req, PARAM_SECTION_TITLE)) {
									section.setSectionTitle(ParamUtil.getSafeParam(req, PARAM_SECTION_TITLE));
								}
								
								// perform copy and move of questions
								if (ParamUtil.checkAllParamsSet(req, PARAM_OPERATION, PARAM_QUESTIONID, PARAM_POSITION)) {
									Query query = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_ID);
									query.setParameter("1", ParamUtil.getSafeIntFromParam(req, PARAM_QUESTIONID));
									AbstractQuestion questionToHandle = (AbstractQuestion) query.uniqueResult();
									int position = ParamUtil.getSafeIntFromParam(req, PARAM_POSITION);
									if (req.getParameter(PARAM_OPERATION).equals(OPERATION_COPY)) {
										copyQuestion(questionToHandle, section, position, hibSess);
									} else if (req.getParameter(PARAM_OPERATION).equals(OPERATION_MOVE)) {
										moveQuestion(questionToHandle, section, position, hibSess);
									}
								}
								
								hibSess.save(section);
								Logger.logUserActivity("Section updated: " + section.getId(), currentUser.getUserName());
								resp.getWriter().write(DtoFactory.getInstance().createDto(section).getJSON());
							} else if (part.equals(PARTNAME_QUESTION) && ParamUtil.checkAllParamsSet(req, PARAM_QUESTIONID)) {
								Query query = hibSess.createQuery(Queries.ABSTRACTQUESTION_BY_ID);
								query.setParameter("1", ParamUtil.getSafeIntFromParam(req, PARAM_QUESTIONID));
								Object o = query.uniqueResult();
								if ((o != null) && (o instanceof AbstractQuestion)) {
									AbstractQuestion q = (AbstractQuestion) o;
									
									// Reset all logic, because question is
									// beeing edited. (no logic may be kept)
									q.setLogicElements(new ArrayList<QuestionnaireLogicElement>());
									
									if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTION_TEXT)) {
										q.setQuestion(ParamUtil.getSafeParam(req, PARAM_QUESTION_TEXT, HTML_TAG_WHITELIST));
									}
									
									if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTION_ALIAS)) {
										q.setAlias(ParamUtil.getSafeParam(req, PARAM_QUESTION_ALIAS, HTML_TAG_WHITELIST));
									}
									
									if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTION_ADD_INFO)) {
										q.setAdditionalInfo(ParamUtil.getSafeParam(req, PARAM_QUESTION_ADD_INFO, HTML_TAG_WHITELIST));
									}
									
									if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTION_ALIGNMENT)) {
										if (q instanceof IAlignmentQuestion) {
											try {
												((IAlignmentQuestion) q).setAlignment(Alignment.valueOf(req.getParameter(PARAM_QUESTION_ALIGNMENT)));
											} catch (Exception e) {
												((IAlignmentQuestion) q).setAlignment(Alignment.HORIZONTAL);
											}
										}
									}
									
									if (ParamUtil.checkAllParamsSet(req, PARAM_OPTIONS) || ParamUtil.checkAllParamsSet(req, PARAM_SUBQUESTIONS)) {
										createQuestionOptions(q, req, hibSess);
									}
									
									if (q instanceof TextPart) {
										if (ParamUtil.checkAllParamsSet(req, PARAM_TEXTPART)) {
											
											((TextPart) q).setTextValue(ParamUtil.getSafeParam(req, PARAM_TEXTPART, HTML_TAG_WHITELIST));
										}
									}
									
									if (q instanceof SingleLineQuestion) {
										if (ParamUtil.checkAllParamsSet(req, PARAM_SINGLELINE_HINT)) {
											((SingleLineQuestion) q).setHint(ParamUtil.getSafeParam(req, PARAM_SINGLELINE_HINT));
										}
									}
									
									if (q instanceof AverageNumberQuestion) {
										if (ParamUtil.checkAllParamsSet(req, PARAM_AVERAGE_NUM_HINT)) {
											((AverageNumberQuestion) q).setHint(ParamUtil.getSafeParam(req, PARAM_AVERAGE_NUM_HINT));
										}
									}
									
									if (ParamUtil.checkAllParamsSet(req, PARAM_SOURCE_QUESTION_ID, PARAM_CREATELOGIC_REQUESTED)) {
										// update or create logic for the
										// question
										createOrUpdateLogic(req, q, hibSess, resp);
									} else if (ParamUtil.checkAllParamsSet(req, PARAM_CREATELOGIC_REQUESTED)) {
										// logic element button pressed, but no
										// data available
										q.setLogicElements(null);
									}
									
									if (ParamUtil.checkAllParamsSet(req, PARAM_VISIBILITY)) {
										// update visibility of the question
										q.setVisible(req.getParameter(PARAM_VISIBILITY).equals("true"));
									}
									
									hibSess.save(q);
									Logger.logUserActivity("Question updated: " + q.getId(), currentUser.getUserName());
									if (q instanceof IMultipleOptionsQuestion) {
										SurveyElementFactory.getInstance().correctOptionIds(q, 0);
									}
								}
							}
							tx.commit();
							
						} catch (Exception e) {
							Logger.errUnexpected(e, currentUser.getUserName());
							resp.getWriter().write("Error: " + e);
						}
						// } else {
						// handlePermissionDenied(req, resp);
						// }
					}
				}
			} finally {
				hibSess.close();
			}
		} else {
			super.handlePermissionDenied(req, resp);
		}
		
	}
	
	/**
	 * @param sectionToHandle
	 * @param targetSection
	 * @param position
	 * @param hibSess
	 */
	private void movePage(final Page pageToMove, final Questionnaire targetQuestionnaire, final int position, final Session hibSess) {
		List<Page> pages = targetQuestionnaire.getPages();
		final List<ISortable> sortableElements = new ArrayList<ISortable>();
		sortableElements.addAll(pages);
		provideNewOrderNumbers(pageToMove, position, sortableElements, hibSess);
		hibSess.save(pageToMove);
	}
	
	/**
	 * @param sectionToHandle
	 * @param targetPage
	 * @param hibSess
	 */
	private void copyPage(final Page pageToCopy, final Questionnaire targetQuestionnaire, final int position, final Session hibSess) {
		// Page pageClone = pageToCopy.clone();
		// List<Page> pages = targetQuestionnaire.getPages();
		// final List<ISortable> sortableElements = new ArrayList<ISortable>();
		// sortableElements.addAll(pages);
		// provideNewOrderNumbers(pageClone, position, sortableElements, hibSess);
		// hibSess.save(pageClone);
		// pages.add(pageClone);
		// pageClone.setQuestionnaire(targetQuestionnaire);
		// pageClone.setLocalId(pageClone.getId());
		// hibSess.save(targetQuestionnaire);
		throw new RuntimeException("copy Page not implemented");
	}
	
	/**
	 * @param sectionToHandle
	 * @param targetPage
	 * @param position
	 * @param hibSess
	 */
	private void moveSection(final Section sectionToMove, final Page targetPage, final int position, final Session hibSess) {
		// 1. change order numbers
		// 2. get page of sectionToHandle
		// see if this page is equal to the targetPage
		// if yes: already done through providing of new order numbers
		// if no: put the section
		// in the targetPage and remove it from the
		// sourcePage
		
		List<Section> sections = targetPage.getSections();
		final List<ISortable> sortableElements = new ArrayList<ISortable>();
		sortableElements.addAll(sections);
		provideNewOrderNumbers(sectionToMove, position, sortableElements, hibSess);
		hibSess.save(sectionToMove);
		if (sectionToMove.getPage().getId() != targetPage.getId()) {
			sectionToMove.getPage().getSections().remove(sectionToMove);
			sectionToMove.setPage(targetPage);
		}
	}
	
	
	private void copySection(final Section sectionToCopy, final Page targetPage, final int position, final Session hibSess) {
		Section sectionClone = sectionToCopy.clone();
		List<Section> sections = targetPage.getSections();
		final List<ISortable> sortableElements = new ArrayList<ISortable>();
		sortableElements.addAll(sections);
		provideNewOrderNumbers(sectionClone, position, sortableElements, hibSess);
		hibSess.save(sectionClone);
		sectionClone.setLocalId(sectionClone.getId());
		for (AbstractQuestion q : sectionClone.getQuestions()) {
			q.setLocalId(q.getId());
			if (q instanceof MultipleChoiceQuestion) {
				if (q.getSubquestions() != null) {
					for (AbstractQuestion sq : q.getSubquestions()) {
						sq.setLocalId(sq.getId());
						sq.setOriginQuestionId(q.getId());
						for (Option o : sq.getOptions()) {
							o.setOriginQuestionId(q.getId());
						}
					}
				}
			}
			if (q.getLogicElements() != null) {
				for (QuestionnaireLogicElement qle : q.getLogicElements()) {
					qle.setQuestionIdWithLogic(q.getLocalId());
				}
			}
		}
		sections.add(sectionClone);
		sectionClone.setPage(targetPage);
		hibSess.save(targetPage);
	}
	
	
	private void moveQuestion(final AbstractQuestion questionToMove, final Section targetSection, final int position, final Session hibSess) {
		List<AbstractQuestion> questions = targetSection.getQuestions();
		
		// Detach list from hibernate context (otherwise, sorting of list causes
		// multiple insert and delete statements)
		questions = new ArrayList<AbstractQuestion>(questions);
		
		final List<ISortable> sortableElements = new ArrayList<ISortable>();
		sortableElements.addAll(questions);
		provideNewOrderNumbers(questionToMove, position, sortableElements, hibSess);
		hibSess.save(questionToMove);
		if (questionToMove.getSection().getId() != targetSection.getId()) {
			questionToMove.getSection().getQuestions().remove(questionToMove);
			questionToMove.setSection(targetSection);
		}
	}
	
	
	private void copyQuestion(final AbstractQuestion questionToCopy, final Section targetSection, final int position, final Session hibSess) {
		AbstractQuestion questionClone = questionToCopy.clone();
		List<AbstractQuestion> questions = targetSection.getQuestions();
		final List<ISortable> sortableElements = new ArrayList<ISortable>();
		sortableElements.addAll(questions);
		provideNewOrderNumbers(questionClone, position, sortableElements, hibSess);
		hibSess.save(questionClone);
		questionClone.setLocalId(questionClone.getId());
		if (questionClone.getLogicElements() != null) {
			for (QuestionnaireLogicElement qle : questionClone.getLogicElements()) {
				qle.setQuestionIdWithLogic(questionClone.getLocalId());
			}
		}
		questions.add(questionClone);
		questionClone.setSection(targetSection);
		hibSess.save(targetSection);
	}
	
	/**
	 * @param hibSess
	 * @param position
	 * @param elementToInsert
	 * @param sections
	 */
	private ISortable provideNewOrderNumbers(final ISortable elementToInsert, final int position, final List<ISortable> sortableList, final Session hibSess) {
		int orderNumberNewSection = 0;
		int currentPosition = 0;
		int lastOrderNumber = 0;
		final Iterator<? extends ISortable> iterator = sortableList.iterator();
		while (iterator.hasNext()) {
			ISortable sortableIter = iterator.next();
			lastOrderNumber = (currentPosition + 1) * 10;
			sortableIter.setOrderNumber(lastOrderNumber);
			if (currentPosition == position) {
				orderNumberNewSection = sortableIter.getOrderNumber();
			}
			hibSess.save(sortableIter);
			currentPosition++;
		}
		if (orderNumberNewSection > 0) {
			elementToInsert.setOrderNumber(orderNumberNewSection - 1);
		} else {
			elementToInsert.setOrderNumber(lastOrderNumber + 1);
		}
		return elementToInsert;
	}
	
	/**
	 * Creates a {@link QuestionnaireLogicElement} object and adds it to the
	 * given
	 * Question.
	 * By now, only one logicelement per question is supported.
	 * 
	 * @param req HttpServletRequest
	 * @param question Question to add the logic
	 */
	private void createOrUpdateLogic(final HttpServletRequest req, final AbstractQuestion question, final Session hibSess, final HttpServletResponse resp) {
		
		int surveyId = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
		Survey currentSurvey = getSurvey(surveyId, hibSess);
		SystemUser currentUser = AuthUtil.checkAuth(req);
		int logicConditionCounter = Integer.parseInt(req.getParameter(PARAM_LOGIC_CONDITION_COUNTER));
		
		// Reset LogicElements-List
		question.setLogicElements(new ArrayList<QuestionnaireLogicElement>());
		
		// loop through conditionCounterNumber
		// create new arrays each time of:
		// answers, and either
		// questions, sections or pages.
		
		for (int condition = 1; condition <= logicConditionCounter; condition++) {
			
			String[] answers = req.getParameterValues(PARAM_ANSWERS + "[" + condition + "]");
			
			String[] targetQuestions = new String[0];
			String[] targetSections = new String[0];
			String[] targetPages = new String[0];
			List<Page> logicPages = new ArrayList<Page>();
			
			String partType = "";
			if (ParamUtil.checkAllParamsSet(req, PARAM_QUESTION_IDS + "[" + condition + "]")) {
				targetQuestions = req.getParameterValues(PARAM_QUESTION_IDS + "[" + condition + "]");
				partType = AbstractQuestion.class.getName().toLowerCase();
			} else if (ParamUtil.checkAllParamsSet(req, PARAM_SECTION_IDS + "[" + condition + "]")) {
				targetSections = req.getParameterValues(PARAM_SECTION_IDS + "[" + condition + "]");
				partType = Section.class.getName().toLowerCase();
			} else if (ParamUtil.checkAllParamsSet(req, PARAM_PAGE_IDS + "[" + condition + "]")) {
				targetPages = req.getParameterValues(PARAM_PAGE_IDS + "[" + condition + "]");
				partType = Page.class.getName().toLowerCase();
				for (String targetPage : targetPages) {
					for (Page page : question.getSection().getPage().getQuestionnaire().getPages()) {
						if (page.getLocalId() == Integer.parseInt(targetPage)) {
							logicPages.add(page);
							break;
						}
					}
				}
			}
			// for now, the ONLY operator used, is OR.
			String[] abstractTarget = new String[0];
			if (partType.equals(AbstractQuestion.class.getName().toLowerCase())) {
				abstractTarget = targetQuestions;
			} else if (partType.equals(Section.class.getName().toLowerCase())) {
				abstractTarget = targetSections;
			} else if (partType.equals(Page.class.getName().toLowerCase())) {
				abstractTarget = targetPages;
			}
			if (abstractTarget.length >= 1) {
				for (int i = 0; i < abstractTarget.length; i++) {
					int partId = Integer.parseInt(abstractTarget[i]);
					// before building the LogicElement, check if targetPart
					// isn't already a target of ANY LOGIC
					// loop through all Survey Elements and check it
					List<Page> pageList = currentSurvey.getQuestionnaire().getPages();
					boolean isLogicalTarget = false;
					boolean isErrorTarget = false;
					for (Page page : pageList) {
						for (Section section : page.getSections()) {
							for (AbstractQuestion q : section.getQuestions()) {
								// question targetting its own section/page, assuming localIds of q and s/p are always different.
								if ((question.getSection().getLocalId() == partId) || (question.getSection().getPage().getLocalId() == partId)) {
									isErrorTarget = true;
									break;
								}
								if (logicPages.size() > 0) {
									if ((question.getSection().getPage().getOrderNumber() >= logicPages.get(i).getOrderNumber())) {
										isErrorTarget = true;
										break;
									}
								}
								for (QuestionnaireLogicElement e : q.getLogicElements()) {
									if (e.getIdOfPart() == partId) {
										// found a targetelement that is already
										// in logic; might cause conflicts
										if (q.getLocalId() == question.getLocalId()) {
											// do nothing, same question Options on the same target is allowed
										} else {
											isLogicalTarget = true;
											break;
										}
										
									}
								}
							}
						}
					}
					if (isLogicalTarget) {
						// tell it is not possible to create this logic and send
						// a mini-Tutorial as response (instead)
						try {
							resp.getWriter().write("Sorry, the element with id: " + partId + ", you have selected as a target is already a target of another Logic Condition (of another Question). To keep logic simple, such operations are forbidden.");
							// break here! Do not allow any other changes within this save operation. This save Operation is aborted!
							break;
						} catch (IOException e) {
							Logger.errUnexpected(e, currentUser.getUserName());
						}
						
					} else if (isErrorTarget) {
						// tell this operation will cause errors and is forbidden in general
						try {
							resp.getWriter().write("Sorry, the operation targetting the element with id: " + partId + " is forbidden, because it might cause Errors. E.g. a question targetting its own section/page, so that when Logic is once triggered, the section/page will disappear and action cannot be undone.");
							// break here! Do not allow any other changes within this save operation. This save Operation is aborted!
							break;
						} catch (IOException e) {
							Logger.errUnexpected(e, currentUser.getUserName());
						}
					} else {
						if (!partType.equals("")) {
							// new LogicElement
							// foreach part
							// set partType Element as "isTargetOfLogic"
							for (Page page : pageList) {
								for (Section section : page.getSections()) {
									for (AbstractQuestion q : section.getQuestions()) {
										if ((q.getLocalId() == partId) && partType.equals(AbstractQuestion.class.getName())) {
											q.setTargetOfLogic(true);
										}
									}
									if ((section.getLocalId() == partId) && partType.equals(Section.class.getName())) {
										section.setTargetOfLogic(true);
									}
								}
								if ((page.getLocalId() == partId) && partType.equals(Page.class.getName())) {
									page.setTargetOfLogic(true);
								}
							}
							
							QuestionnaireLogicElement logicElement = new QuestionnaireLogicElement();
							for (String answer : answers) {
								logicElement.getAnswers().add(new Answer(answer));
							}
							logicElement.setIdOfPart(partId);
							logicElement.setSurveyId(surveyId);
							// only "OR" for now
							logicElement.setOperator(Operator.OR);
							
							logicElement.setTypeOfPart(partType);
							logicElement.setQuestionIdWithLogic(question.getLocalId());
							hibSess.save(logicElement);
							question.getLogicElements().add(logicElement);
						}
					}
				}
			} else {
				// The target-Part-Information was not sent/received well.
				Logger.err("The target-Part-Information was not received properly.");
			}
			
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @seescc.servlets.AbstractSccServlet#processDelete(javax.servlet.http.
	 * HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void processDelete(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		
		SystemUser currentUser = AuthUtil.checkAuth(req);
		if (currentUser != null) {
			if (ParamUtil.checkAllParamsSet(req, PARAM_SURVEYID)) {
				
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					int surveyID = ParamUtil.getSafeIntFromParam(req, PARAM_SURVEYID);
					Survey surveyToChange = getSurvey(surveyID, hibSess);
					
					// complete deletions is only allowed for admins
					if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
						
						hibSess.delete(surveyToChange);
						tx.commit();
						
						File surveyLogo = new File(Paths.SURVEYLOGOS + "/" + surveyToChange.getId() + ".jpg");
						if (surveyLogo.exists()) {
							surveyLogo.delete();
						}
						
					} else {
						super.handlePermissionDenied(req, resp);
					}
				} catch (Exception e) {
					Logger.errUnexpected(e, currentUser.getUserName());
					resp.getWriter().write("Error: " + e);
				} finally {
					hibSess.close();
				}
				
			} else if (ParamUtil.checkAllParamsSet(req, PARAM_SURVEYS)) {
				String surveyIds[] = req.getParameterValues(PARAM_SURVEYS);
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					for (String surveyIdStr : surveyIds) {
						try {
							int surveyID = Integer.parseInt(surveyIdStr);
							Survey surveyToChange = getSurvey(surveyID, hibSess);
							if (AuthUtil.isAllowedToEditThisSurvey(currentUser, surveyToChange, hibSess)) {
								surveyToChange.setDeleted(true);
							} else {
								super.handlePermissionDenied(req, resp);
							}
						} catch (NumberFormatException nfe) {
							// ignore those ids which cannot converted to
							// integer
						}
					}
					tx.commit();
				} catch (Exception e) {
					Logger.errUnexpected(e, currentUser.getUserName());
					resp.getWriter().write("Error: " + e);
				} finally {
					hibSess.close();
				}
			} else if (ParamUtil.checkAllParamsSet(req, PARAM_PART)) {
				String part = req.getParameter(PARAM_PART);
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					if (part.equals(PARTNAME_PAGE) && ParamUtil.checkAllParamsSet(req, PARAM_PAGEID)) {
						Query query = hibSess.createQuery(Queries.QUESTIONNAIRE_BY_PAGE);
						int pageID = ParamUtil.getSafeIntFromParam(req, PARAM_PAGEID);
						query.setParameter("1", pageID);
						Object o = query.uniqueResult();
						if (o != null) {
							if (o instanceof Questionnaire) {
								Questionnaire q = ((Questionnaire) o);
								Page toRemove = null;
								for (Page p : q.getPages()) {
									if (p.getId() == pageID) {
										toRemove = p;
										break;
									}
								}
								if (toRemove != null) {
									q.getPages().remove(toRemove);
								}
								hibSess.save(q);
								hibSess.delete(toRemove);
							}
						}
					} else if (part.equals(PARTNAME_SECTION) && ParamUtil.checkAllParamsSet(req, PARAM_SECTIONID)) {
						int sectionID = ParamUtil.getSafeIntFromParam(req, PARAM_SECTIONID);
						Section section = (Section) hibSess.load(Section.class, sectionID);
						if (section != null) {
							Page page = section.getPage();
							page.getSections().remove(section);
							hibSess.save(page);
							hibSess.delete(section);
						}
					} else if (part.equals(PARTNAME_QUESTION) && ParamUtil.checkAllParamsSet(req, PARAM_QUESTIONID)) {
						int questionID = ParamUtil.getSafeIntFromParam(req, PARAM_QUESTIONID);
						AbstractQuestion question = (AbstractQuestion) hibSess.load(AbstractQuestion.class, questionID);
						if (question != null) {
							Section section = question.getSection();
							section.getQuestions().remove(question);
							hibSess.save(section);
							hibSess.delete(question);
							
						}
					}
					tx.commit();
				} catch (Exception e) {
					Logger.errUnexpected(e, currentUser.getUserName());
					resp.getWriter().write("Error: " + e);
				} finally {
					hibSess.close();
				}
			}
		} else {
			super.handlePermissionDenied(req, resp);
		}
	}
	
	/**
	 * Creates the question from the data given in the request
	 * 
	 * @param req HttpServletRequest
	 * @return Question object according to the data given in the request
	 */
	private AbstractQuestion createQuestion(final HttpServletRequest req, final Session hibSess) {
		AbstractQuestion question = null;
		String text = ParamUtil.getSafeParam(req, PARAM_QUESTION_TEXT);
		String questionType = ParamUtil.getSafeParam(req, PARAM_QUESTION_TYPE);
		String alias = ParamUtil.getSafeParam(req, PARAM_QUESTION_ALIAS);
		String addInfo = ParamUtil.getSafeParam(req, PARAM_QUESTION_ADD_INFO, HTML_TAG_WHITELIST);
		
		
		
		if (questionType.equals("freetext")) {
			question = SurveyElementFactory.getInstance().createFreetTextQuestion(text);
		} else {
			if (questionType.equals("multiple")) {
				question = SurveyElementFactory.getInstance().createMultipleChoiceQuestion(text);
				createQuestionOptions(question, req, hibSess);
			} else if (questionType.equals("singleline")) {
				question = SurveyElementFactory.getInstance().createSingleLineQuestion(text, ParamUtil.getSafeParam(req, PARAM_SINGLELINE_HINT));
			} else if (questionType.equals("averageNumber")) {
				question = SurveyElementFactory.getInstance().createAverageNumberQuestion(text, ParamUtil.getSafeParam(req, PARAM_AVERAGE_NUM_HINT));
			} else if (questionType.equals("radio")) {
				question = SurveyElementFactory.getInstance().createRadioQuestion(text);
				createQuestionOptions(question, req, hibSess);
			} else if (questionType.equals("combo")) {
				question = SurveyElementFactory.getInstance().createComboQuestion(text);
				createQuestionOptions(question, req, hibSess);
			} else if (questionType.equals("multiplematrix")) {
				question = SurveyElementFactory.getInstance().createMultipleChoiceMatrixQuestion(text);
				createQuestionOptions(question, req, hibSess);
			} else if (questionType.equals("radiomatrix")) {
				question = SurveyElementFactory.getInstance().createRadioMatrixQuestion(text);
				createQuestionOptions(question, req, hibSess);
			} else if (questionType.equals("textfield")) {
				question = SurveyElementFactory.getInstance().createTextfieldQuestion(text);
			} else if (questionType.equals("textpart")) {
				question = SurveyElementFactory.getInstance().createTextPart(text, ParamUtil.getSafeParam(req, PARAM_TEXTPART, HTML_TAG_WHITELIST));
			} else if (questionType.equals("textpart_phone")) {
				question = SurveyElementFactory.getInstance().createPhoneCallerHint(text, ParamUtil.getSafeParam(req, PARAM_TEXTPART, HTML_TAG_WHITELIST));
			}
			question.setChartType(ChartType.bar);
			if (question instanceof IAlignmentQuestion) {
				try {
					((IAlignmentQuestion) question).setAlignment(Alignment.valueOf(req.getParameter(PARAM_QUESTION_ALIGNMENT)));
				} catch (Exception e) {
					((IAlignmentQuestion) question).setAlignment(Alignment.HORIZONTAL);
				}
			}
		}
		question.setAdditionalInfo(addInfo);
		question.setInteresting(true);
		question.setAlias(alias);
		
		return question;
	}
	
	
	/**
	 * @param question
	 * @param req
	 */
	private void createQuestionOptions(final AbstractQuestion question, final HttpServletRequest req, final Session hibSess) {
		
		// first clear old stuff
		for (Option option : question.getOptions()) {
			hibSess.delete(option);
		}
		question.getOptions().clear();
		
		if (question.getSubquestions() != null) {
			for (AbstractQuestion subQuestion : question.getSubquestions()) {
				hibSess.delete(subQuestion);
			}
			question.getSubquestions().clear();
		}
		
		if (question instanceof RadioMatrixQuestion) {
			RadioMatrixQuestion q = (RadioMatrixQuestion) question;
			String[] subQuestions = req.getParameterValues(PARAM_SUBQUESTIONS);
			int orderNumber = 0;
			if (subQuestions != null) {
				q.setSubquestions(new ArrayList<AbstractQuestion>());
				for (String subQuestionText : subQuestions) {
					RadioQuestion subQuestion = SurveyElementFactory.getInstance().createRadioQuestion(ParamUtil.stripTags(subQuestionText));
					subQuestion.setOrderNumber(++orderNumber);
					q.getSubquestions().add(subQuestion);
					subQuestion.setParentQuestion(q);
				}
			}
		} else if (question instanceof MultipleChoiceMatrixQuestion) {
			MultipleChoiceMatrixQuestion q = (MultipleChoiceMatrixQuestion) question;
			String[] subQuestions = req.getParameterValues(PARAM_SUBQUESTIONS);
			int orderNumber = 0;
			if (subQuestions != null) {
				q.setSubquestions(new ArrayList<AbstractQuestion>());
				for (String subQuestionText : subQuestions) {
					MultipleChoiceQuestion subQuestion = SurveyElementFactory.getInstance().createMultipleChoiceQuestion(ParamUtil.stripTags(subQuestionText));
					subQuestion.setOrderNumber(++orderNumber);
					subQuestion.setQuestion(ParamUtil.stripTags(subQuestionText));
					q.getSubquestions().add(subQuestion);
					subQuestion.setParentQuestion(q);
				}
			}
		}
		
		// now provide the options if it is a multipleoptions question or
		// matrixquestion
		if (question != null) {
			if (question instanceof IMultipleOptionsQuestion) {
				String[] options = req.getParameterValues(PARAM_OPTIONS);
				if (options != null) {
					int orderNumber = 0;
					if (question instanceof IMatrixQuestion) {
						// provide the options to each subquestion
						for (AbstractQuestion subQuestion : question.getSubquestions()) {
							for (String option : options) {
								Option newOption = SurveyElementFactory.getInstance().createOption(ParamUtil.stripTags(option));
								newOption.setOrderNumber(++orderNumber);
								subQuestion.getOptions().add(newOption);
								newOption.setQuestion(subQuestion);
							}
						}
					} else {
						// provide the options to the question
						for (String option : options) {
							Option newOption = SurveyElementFactory.getInstance().createOption(ParamUtil.stripTags(option));
							question.getOptions().add(newOption);
							newOption.setQuestion(question);
							newOption.setOrderNumber(++orderNumber);
						}
					}
				}
			}
		}
	}
	
	private String getServerBaseUrl() {
		return (String) EnvironmentConfiguration.getConfiguration(ConfigID.HOST) + (String) EnvironmentConfiguration.getConfiguration(ConfigID.URLBASE);
	}
	
	private String handlePictureUpload(final HttpServletRequest request) {
		
		String uploadedFileURL = "";
		try {
			List<File> uploadedFiles = FileUploadUtil.processUpload(request, Paths.WEBCONTENT + "/" + Paths.UPLOAD_TEMP);
			String surveyID = (String) request.getAttribute(PARAM_SURVEYID);
			for (File file : uploadedFiles) {
				if ((file != null) && file.exists()) {
					File targetFile = new File(Paths.SURVEYLOGOS + "/" + surveyID + ".jpg");
					
					if (targetFile.exists()) {
						targetFile.delete();
					} else {
						targetFile.getParentFile().mkdirs();
					}
					file.renameTo(targetFile);
					
					// ImageUtil.createThumbnail(file, targetFile, 200, 100);
					uploadedFileURL = EnvironmentConfiguration.getUrlBase() + Paths.SURVEYLOGOS.substring(Paths.WEBCONTENT.length()) + "/" + surveyID + ".jpg";
					file.delete();
				}
			}
		} catch (Exception e) {
			Logger.err("Konnte Datei nicht hochladen", e);
		}
		return uploadedFileURL;
	}
	
}

