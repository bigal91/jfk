package de.cinovo.surveyplatform.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.ExtendedProperties;
import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.IFreeTextQuestion;
import de.cinovo.surveyplatform.model.IQuestionnairePart;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Participation;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.factory.SurveyElementFactory;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.question.IMatrixQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.PhoneCallerHint;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.ui.views.AverageNumberQuestionView;
import de.cinovo.surveyplatform.ui.views.SingleLineQuestionView;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.QuestionValidator;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil;
import de.cinovo.surveyplatform.util.SurveyUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class ParticipateServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final String ID_OF_PART = "idOfPart";
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_PAGENUMBER = "page";
	private static final String PARAM_PARTICIPATION_ID = "pid";
	
	private static final String PARAM_BACKBUTTON = "bButtonBack";
	private static final String PARAM_NEXTBUTTON = "bButtonNext";
	private static final String PARAM_FIRSTPAGE_BUTTON = "bButtonFirstPage";
	private static final String PARAM_SUBMITBUTTON = "bButtonSubmit";
	
	private static final String PARAM_PHONEINTERVIEW = "phoneInterview";
	private static final String PARAM_PREVIEW = "preview";
	private static final String PARAM_RESET = "reset";
	
	private static final String PARAM_SKIPBUTTON = "bSkip";
	
	private static final String PARAM_OVERRIDESUBMITTED = "overridesubmitted";
	
	
	private class ProcessAnswersResult {
		
		List<Integer> unansweredQuestionIds = new ArrayList<Integer>();
		List<Integer> invalidQuestionIds = new ArrayList<Integer>();
		int nextPage;
	}
	
	
	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		this.doRequest(req, resp);
	}
	
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		this.doRequest(req, resp);
	}
	
	private void doRequest(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		
		boolean previewMode = false;
		// the initialVisit decides whether the user visits this Questionnaire initially
		// initally means either: for the first time, or he comes back after he has not finished submission before
		boolean initialVisit = false;
		
		resp.setContentType("text/html; charset=utf-8");
		resp.setCharacterEncoding("UTF-8");
		PrintWriter out = resp.getWriter();
		
		out.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/headerParticipation.html", null));
		
		int pageNr = 0;
		if (ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_PAGENUMBER)) {
			try {
				pageNr = ParamUtil.getSafeIntFromParam(req, ParticipateServlet.PARAM_PAGENUMBER);
				
			} catch (NumberFormatException nfe) {
				pageNr = 0;
			}
		} else {
			// no Page Parameter has been set, the user visits this Questionnaire initially
			initialVisit = true;
		}
		if (pageNr == 0) {
			req.getSession().setAttribute("MEASURE_START", System.currentTimeMillis());
		}
		if ((pageNr > 0) && ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_BACKBUTTON)) {
			pageNr--;
		} else if ((pageNr > 0) && ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_FIRSTPAGE_BUTTON)) {
			pageNr = 0;
		} else if (pageNr <= 0) {
			pageNr = 0;
		}
		
		final StringBuilder htmlOutput = new StringBuilder();
		
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		SystemUser currentUser = AuthUtil.checkAuth(req);
		try {
			// we will get the participation from the given
			// PARAM_PARTICIPATION_ID.
			// If this parameter is not set, we will look for the
			// PARAM_SURVEYID and, if the survey is public, create a new
			// Participation object and persist it immediately.
			Participation participation = null;
			Survey survey = null;
			
			
			// If no participation ID is given and the survey is public, create
			// a new participation through cloning the questionnaire from the
			// survey
			if (ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_PARTICIPATION_ID)) {
				String participationId = req.getParameter(ParticipateServlet.PARAM_PARTICIPATION_ID);
				
				if (participationId.equals(ParticipateServlet.PARAM_PREVIEW)) {
					if (currentUser != null) {
						if (ParamUtil.checkAllParamsSet(req, SurveyDal.PARAM_SURVEYID)) {
							
							survey = (Survey) hibSess.get(Survey.class, ParamUtil.getSafeIntFromParam(req, SurveyDal.PARAM_SURVEYID));
							
							if ((survey != null) && (AuthUtil.isAllowedToOpenPreview(survey, currentUser, hibSess))) {
								previewMode = true;
							}
						}
					}
				} else {
					Transaction tx = hibSess.beginTransaction();
					Query query = hibSess.createQuery(Queries.SURVEY_BY_PARTICIPATION);
					
					query.setParameter("1", participationId);
					survey = (Survey) query.uniqueResult();
					participation = (Participation) hibSess.get(Participation.class, req.getParameter(ParticipateServlet.PARAM_PARTICIPATION_ID));
					
					tx.commit();
				}
				
			} else if (ParamUtil.checkAllParamsSet(req, SurveyDal.PARAM_SURVEYID)) {
				Transaction tx = hibSess.beginTransaction();
				
				survey = (Survey) hibSess.get(Survey.class, ParamUtil.getSafeIntFromParam(req, SurveyDal.PARAM_SURVEYID));
				
				if ((survey != null) && (survey.isPublicSurvey() || ExtendedProperties.get(survey, ExtendedProperties.SURVEY_ISPUBLIC, false))) {
					Participant participant = SurveyElementFactory.getInstance().createParticipant(survey, true);
					participant.setNumber(SurveyUtil.getNextParticipantNumber(hibSess, survey));
					hibSess.save(participant);
					participation = participant.getParticipation();
				}
				tx.commit();
			}
			// test if participant may "jump" to provided pageNr
			if (pageNr > 0) {
				List<Page> pages = participation.getQuestionnaire().getPages();
				if (pageNr > pages.size()) {
					// pageNr not even possible to reach (not in existing pages Array), reset it
					pageNr = 0;
				} else {
					// if pageNr lands on a (yet) invalid page, that may not be seen, reset to "latest" page
					if (pageNr > (getHighestValidPageNr(participation, survey, req, resp) + 1)) {
						// page may not be seen, redirect to first page (someone probably edited the Parameter in the URL)
						pageNr = 0;
					}
					// else do not change the current pageNr
				}
			}
			
			if (participation.getQuestionnaire() != null) {
				// made sure participant is re-visitting this questionnaire (very first visit has no Questionnaire at this point)
				if (initialVisit) {
					resetToHighestPossiblePage(participation, survey, req, resp);
				}
			}
			
			boolean showPhoneHints = false;
			if (currentUser != null) {
				if (ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_PHONEINTERVIEW)) {
					showPhoneHints = true;
				}
			}
			
			if (showPhoneHints) {
				htmlOutput.append(PartsUtil.getPageHeader("Phoneinterview for " + survey.getName(), HelpIDs.PAGE_PHONEINTERVIEW, new String[] {"<a href=\"?page=participants&surveyID=" + survey.getId() + "\">Manage Participants</a>"}));
			}
			
			final Map<String, String> replacements = new HashMap<String, String>();
			
			if (survey != null) {
				// add logo
				if (!EnvironmentConfiguration.isOfflineMode()) {
					String surveyLogo = PartsUtil.getSurveyLogo(survey.getId(), 1, true, true, true);
					String clientLogo = PartsUtil.getClientLogo(survey.getOwner().getClient(), 2, true, surveyLogo.isEmpty(), true);
					htmlOutput.append(clientLogo);
					htmlOutput.append(surveyLogo);
				}
			}
			
			if ((pageNr == 0) && showPhoneHints && !previewMode) {
				replacements.put("PARTICIPANTID", participation.getParticipant().getId() + "");
				htmlOutput.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participantInfoBox.html", replacements));
			}
			
			if (((participation != null) || previewMode) && (survey != null)) {
				
				if ((survey.getState() == SurveyState.RUNNING) || previewMode || EnvironmentConfiguration.isOfflineMode()) {
					
					final Questionnaire questionnaire;
					if (previewMode) {
						questionnaire = survey.getQuestionnaire();
					} else {
						if (participation.getQuestionnaire() == null) {
							Questionnaire templateClone = survey.getQuestionnaire().templateClone();
							SurveyUtil.resetAnswers(templateClone);
							participation.setQuestionnaire(templateClone);
							templateClone.setParticipation(participation);
							reset(templateClone);
						}
						questionnaire = participation.getQuestionnaire();
					}
					
					if (ParamUtil.checkAllParamsSet(req, PARAM_RESET)) {
						reset(questionnaire);
					}
					
					List<Page> pages = questionnaire.getPages();
					
					// Detach list from hibernate context (otherwise,
					// sorting of
					// list causes multiple insert and delete statements)
					pages = new ArrayList<Page>(pages);
					
					final int pageCount = pages.size();
					
					if (pageNr > pageCount) {
						pageNr = 0;
					}
					
					if (showPhoneHints && (ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_SKIPBUTTON) || (ParamUtil.checkAllParamsSet(req, "additionalData") && req.getParameter("additionalData").equals(ParticipateServlet.PARAM_SKIPBUTTON)))) {
						pageNr = pageCount - 1;
					}
					
					int oldPage = pageNr;
					ProcessAnswersResult result = null;
					if (previewMode) {
						Transaction tx = hibSess.beginTransaction();
						this.processAnswers(req, participation, pageNr, hibSess, true, questionnaire);
						this.updateVisibility(questionnaire, true, hibSess);
						tx.commit();
						
						if (ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_NEXTBUTTON)) {
							pageNr = pageNr + 1;
						}
					} else {
						// update the questions according to the data given
						// in the request
						Transaction tx = hibSess.beginTransaction();
						
						this.processAnswers(req, participation, pageNr, hibSess, false, questionnaire);
						
						// update visibility of all parts
						this.updateVisibility(questionnaire, false, hibSess);
						tx.commit();
						
						// check the answers if given and route to the next
						// page in case of all answers are valid, stay on the
						// current page if any question is not valid
						result = this.validateQuestions(participation, pageNr);
						if (ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_NEXTBUTTON)) {
							pageNr = result.nextPage;
						}
					}
					
					boolean valid = true;
					if (result != null) {
						if ((result.invalidQuestionIds.size() > 0) || (result.unansweredQuestionIds.size() > 0)) {
							valid = false;
						}
					}
					
					boolean showThankYouPageWhenSubmitted = (!previewMode && participation.isSubmitted());
					boolean overrideSubmittedAllowed = false;
					if (ParamUtil.checkAllParamsSet(req, PARAM_OVERRIDESUBMITTED) && !survey.getState().equals(SurveyState.CLOSED)) {
						if (AuthUtil.isAllowedToEditQuestionnaireData(currentUser, survey, hibSess)) {
							overrideSubmittedAllowed = true;
							showThankYouPageWhenSubmitted = false;
						}
					}
					if ((valid && ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_SUBMITBUTTON)) || showThankYouPageWhenSubmitted ) {
						if (!previewMode) {
							Transaction tx = hibSess.beginTransaction();
							if (currentUser == null) {
								Logger.info("A participant submitted the questionnaire: " + participation.getQuestionnaire().getId());
							} else {
								Participant participant = participation.getParticipant();
								participation.setSubmittedBy(currentUser.getActualUserName());
								Logger.logUserActivity("Questionnaire submitted: " + participant.getName() + " " + participant.getSurname(), currentUser.getUserName());
							}
							
							participation.setSubmitted(true);
							
							if (participation.getParticipant().getSurveySubmitted() == null) {
								participation.getParticipant().setSurveySubmitted(new Date());
								this.setQuestionnairePartsSubmitted(participation.getQuestionnaire());
							}
							tx.commit();
							if (ExtendedProperties.get(survey, ExtendedProperties.SURVEY_LETPARTICIPANTDOWNLOADPDF, false)) {
								replacements.put("DOWNLOADBUTTON", "<a class=\"gui-icon-PDF_BIG\" style=\"display:block;\" title=\"Download the Questionnaire\" href=\"" + EnvironmentConfiguration.getUrlBase() + "/download?type=questionnaire&pid=" + participation.getId() + "\">Click to download your questionnaire</a>");
							}
						}
						htmlOutput.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/questionnaireSubmitted.html", replacements));
						req.getSession().setAttribute("MEASURE_START", null);
					} else {
						
						if (pageNr >= pageCount) {
							pageNr = pageCount - 1;
						}
						
						Page page = pages.get(pageNr);
						
						if (previewMode) {
							while (!page.isPreviewModeVisible()) {
								if (ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_BACKBUTTON)) {
									pageNr--;
								} else {
									pageNr++;
								}
								if (pageNr >= pageCount) {
									pageNr--;
									break;
								}
								if (pageNr <= 0) {
									pageNr = 0;
									break;
								}
								page = pages.get(pageNr);
							}
						} else {
							while (!page.isVisible()) {
								if (ParamUtil.checkAllParamsSet(req, ParticipateServlet.PARAM_BACKBUTTON)) {
									pageNr--;
								} else {
									pageNr++;
								}
								
								if (pageNr >= pageCount) {
									pageNr--;
									break;
								}
								if (pageNr <= 0) {
									pageNr = 0;
									break;
								}
								page = pages.get(pageNr);
							}
						}
						
						htmlOutput.append("<ul class=\"questionarePage\">");
						
						List<Section> sections = page.getSections();
						
						// Detach list from hibernate context (otherwise,
						// sorting of
						// list causes multiple insert and delete statements)
						sections = new ArrayList<Section>(sections);
						
						
						for (Section section : sections) {
							String sectionType = "section";
							String sessionLocalId = "lid" + sectionType + section.getLocalId();
							
							if (section.getSectionTitle().equals("")) {
								htmlOutput.append("<li class=\"" + sessionLocalId + " section" + (this.isSectionVisible(section, previewMode) ? "" : " invisible") + "\"><p class=\"insivibleSectionTitle\" id=\"sectionTitle" + section.getId() + "\"></p><ul class=\"questionareSection\" id=\"section" + section.getId() + "\">");
							} else {
								htmlOutput.append("<li class=\"" + sessionLocalId + " section" + (this.isSectionVisible(section, previewMode) ? "" : " invisible") + "\"><p class=\"sectionTitle " + sessionLocalId + "\" id=\"sectionTitle" + section.getId() + "\">" + section.getSectionTitle() + "</p><ul class=\"questionareSection\" id=\"section" + section.getId() + "\">");
							}
							
							List<AbstractQuestion> questions = section.getQuestions();
							
							// Detach list from hibernate context (otherwise,
							// sorting of list causes multiple insert and delete
							// statements)
							questions = new ArrayList<AbstractQuestion>(questions);
							
							htmlOutput.append(QuestionnaireViewUtil.getInitOverrideLogicScript("section", String.valueOf(section.getLocalId())));
							
							for (AbstractQuestion question : questions) {
								String message = "";
								// if the user pressed the next button and
								// is redirected to the same page
								
								if ((oldPage == pageNr) && (ParamUtil.checkAllParamsSet(req, PARAM_NEXTBUTTON) || ParamUtil.checkAllParamsSet(req, PARAM_SUBMITBUTTON))) {
									if (!previewMode) {
										if (result.invalidQuestionIds.contains(question.getId())) {
											
											// correct format!
											message = HtmlFormUtil.getErrorMessage("The format you used to answer the question was not valid!<br/>If you do not want to respond, please type in 'no response' into the answer and the system will allow you to continue.");
										}
										if (result.unansweredQuestionIds.contains(question.getId())) {
											
											// to the type of the question
											if (question instanceof TextfieldQuestion) {
												message = HtmlFormUtil.getErrorMessage("Please answer this question.<br/>If you do not want to respond, please type in 'no response' into the first answer option and the system will allow you to continue.");
											} else {
												message = HtmlFormUtil.getErrorMessage("Please answer this question. Enter a text or select at least one option respectively!");
											}
										}
									}
								}
								
								// do not show phone caller hints when this
								// is not a phone interview
								if (!(!showPhoneHints && (question instanceof PhoneCallerHint))) {
									int lastIndexOfDot = AbstractQuestion.class.getName().lastIndexOf(".");
									String questionType = AbstractQuestion.class.getName().substring(lastIndexOfDot + 1).toLowerCase();
									htmlOutput.append("<li class=\"question lid" + questionType + question.getLocalId() + (this.isQuestionVisible(question, previewMode) ? "" : " invisible") + "\" id=\"question" + question.getId() + "\"><div>" + message + QuestionnaireViewUtil.getQuestionHTMLRepresentation(question, false, false) + "</div></li>");
									// " + sessionLocalId + "
								}
								
							}
							htmlOutput.append("</ul></li>");
						}
						htmlOutput.append("</ul>");
						
						if (pageCount > 1) {
							int currentProgress = (int) Math.round(((double) (pageNr + 1) * 100) / pageCount);
							int minutesLeft = pageCount - pageNr - 1;
							htmlOutput.append("<div class=\"innerContainer\">Step " + (pageNr + 1) + " of " + pageCount + ". (" + currentProgress + "%).");
							if ((pageNr > 0) && (pageNr < (pageCount - 1))) {
								htmlOutput.append(" About " + minutesLeft + " minute" + (minutesLeft != 1 ? "s" : "") + " left.");
							}
							htmlOutput.append("<div>");
							htmlOutput.append(PartsUtil.getDynamicProgressBar(currentProgress));
							htmlOutput.append("</div>");
							htmlOutput.append("</div>");
						}
						StringBuilder buttonsRight = new StringBuilder();
						if (showPhoneHints) {
							buttonsRight.append(PartsUtil.getIconButton("RESULTSET_LAST", "Skip all questions and go to the last page", "Skip the rest", "bSkip", "$('#additionalDataContainer').val('" + ParticipateServlet.PARAM_SKIPBUTTON + "'); $('#questionnaireForm').submit();") + " ");
						}
						buttonsRight.append(HtmlFormUtil.getButton(ParticipateServlet.PARAM_NEXTBUTTON, "Continue with next Step"));
						if (pageNr < (pageCount - 1)) {
							
							replacements.put("BUTTONSRIGHT", buttonsRight.toString());
							
							replacements.put("WANTTOEXITHINT", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participateSurveyWantToExit.html", null));
						} else {
							replacements.put("BUTTONSRIGHT", HtmlFormUtil.getButton(ParticipateServlet.PARAM_SUBMITBUTTON, "Submit the Questionnaire"));
						}
						if ((pageNr != 0) && (pageNr != (pageCount - 1))) {
							replacements.put("BUTTONSCENTER", HtmlFormUtil.getButton(ParticipateServlet.PARAM_FIRSTPAGE_BUTTON, "Go To First Page"));
						}
						if (pageNr > 0) {
							replacements.put("BUTTONSLEFT", HtmlFormUtil.getButton(ParticipateServlet.PARAM_BACKBUTTON, "Back"));
						}
					}
					
					StringBuilder questionnairePageReplacements = new StringBuilder();
					if (showPhoneHints) {
						replacements.put("PHONEINTERVIEWPARAM", "&" + ParticipateServlet.PARAM_PHONEINTERVIEW);
					}
					replacements.put("SURVEYID", survey.getId() + "");
					replacements.put("PARTICIPATIONID", previewMode ? ParticipateServlet.PARAM_PREVIEW : participation.getId());
					replacements.put("PAGENR", pageNr + "");
					replacements.put("QUESTIONNARE_PAGE", htmlOutput.toString());
					replacements.put("OVERRIDESUBMITPARAM", overrideSubmittedAllowed ? "&overridesubmitted=1" : "");
					
					questionnairePageReplacements.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/participateSurvey.html", replacements));
					replacements.clear();
					replacements.put("CONTENT", questionnairePageReplacements.toString());
				} else {
					if (survey.getState().ordinal() <= SurveyState.CREATED.ordinal()) {
						replacements.put("CONTENT", "<p style=\"text-align: center;\">The survey " + survey.getName() + " has not started yet. Please contact your survey provider.</p>");
					} else if (survey.getState() == SurveyState.CLOSED) {
						replacements.put("CONTENT", "<p style=\"text-align: center;\">The survey " + survey.getName() + " has already been closed by the survey conductor.</p>");
					}
				}
			} else {
				replacements.put("CONTENT", "<p style=\"text-align: center;\">Uups..something's gone wrong. Please <a href=\"" + EnvironmentConfiguration.getUrlBase() + "/\">login again</a> and make sure you own the rights to this survey.</p>");
			}
			
			if (previewMode) {
				replacements.put("TOPMIDDLETEXT", "<div style=\"color: #1D5987; font-weight: bold; font-size: 15px;\">Participant Preview of " + survey.getName() + "</div>");
			}
			out.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/layoutParticipation.html", replacements));
			out.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/footer.html", null));
			if (req.getSession().getAttribute("MEASURE_START") == null) {
				req.getSession().setAttribute("MEASURE_START", System.currentTimeMillis());
			}
			
		} catch (Exception ex) {
			final Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("CONTENT", "<p style=\"text-align: center;\">Uups..something's gone wrong.</p>");
			out.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/layoutParticipation.html", replacements));
			out.print(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/footer.html", null));
			Logger.errUnexpected(ex, currentUser == null ? null : currentUser.getUserName());
		} finally {
			hibSess.close();
		}
	}
	
	/**
	 * 
	 */
	private void resetToHighestPossiblePage(final Participation participation, final Survey survey, final HttpServletRequest req, final HttpServletResponse resp) {
		// determine last Visited page, by finding (validating) last filled in (NOT SUBMITTED!) question
		
		
		int pageOrderNumber = getHighestValidPageNr(participation, survey, req, resp);
		
		if (pageOrderNumber != 0) {
			try {
				resp.sendRedirect(getServerBaseUrl() + "/participate?surveyID=" + survey.getId() + "&page=" + pageOrderNumber + "&pid=" + participation.getId());
			} catch (IOException e) {
				Logger.err("The page you have requested could not be provided. Please contact the support.", e);
			}
			return;
		}
	}
	
	private int getHighestValidPageNr(final Participation participation, final Survey survey, final HttpServletRequest req, final HttpServletResponse resp) {
		List<AbstractQuestion> answeredQuestions = new ArrayList<AbstractQuestion>();
		List<Page> pages = participation.getQuestionnaire().getPages();
		for (Page page : pages) {
			for (Section sec : page.getSections()) {
				for (AbstractQuestion quest : sec.getQuestions()) {
					if (QuestionValidator.isAnswered(quest)) {
						answeredQuestions.add(quest);
					}
				}
			}
		}
		int highestValidPageNr = 0;
		Page highestPage = getHighestOrderNumberPage(answeredQuestions);
		if (highestPage != null) {
			for (int i = 0; i < pages.size(); i++) {
				if (pages.get(i).equals(highestPage)) {
					highestValidPageNr = i;
				}
			}
		}
		return highestValidPageNr;
		
	}
	
	/**
	 * Gets the highest Ordernumber in a List of Questions
	 * 
	 * @param questions - the list of given questions
	 * @return the one, highest ordernumber
	 */
	private Page getHighestOrderNumberPage(final List<AbstractQuestion> questions) {
		int tmpHighestNum = 0;
		for (AbstractQuestion quest : questions) {
			Page questionPage = quest.getSection().getPage();
			if ((questionPage.getOrderNumber() > tmpHighestNum) && (quest.getOrderNumber() != 0)) {
				tmpHighestNum = questionPage.getOrderNumber();
			}
		}
		for (AbstractQuestion quest : questions) {
			Page questionPage = quest.getSection().getPage();
			if ((tmpHighestNum == questionPage.getOrderNumber()) && (quest.getOrderNumber() != 0)) {
				return quest.getSection().getPage();
			}
		}
		return null;
	}
	
	/**
	 */
	private void reset(final Questionnaire questionnaire) {
		Integer questionnaireID = questionnaire.getId();
		// // map of <localID, List<typeOfPart>>
		// final Map<Integer, List<String>> partMap = new HashMap<Integer, List<String>>();
		for (Page page : questionnaire.getPages()) {
			for (Section section : page.getSections()) {
				for (AbstractQuestion q : section.getQuestions()) {
					// correct the reference to questionnaire
					clearAnswers(q);
					q.setQuestionnaireID(questionnaireID);
					for (Option o : q.getAllOptions()) {
						o.setSelected(false);
						o.setSubmitted(false);
						// correct the reference to the questionnaire
						o.setQuestionnaireID(questionnaireID);
					}
					q.setSubmitted(false);
					// for (QuestionnaireLogicElement qle : q.getLogicElements()) {
					// List<String> list = partMap.get(qle.getIdOfPart());
					// if (list == null) {
					// list = new ArrayList<String>();
					// partMap.put(qle.getIdOfPart(), list);
					// }
					// list.add(qle.getTypeOfPart());
					// }
				}
			}
		}
		// for (Page page : questionnaire.getPages()) {
		// {
		// List<String> list = partMap.get(page.getLocalId());
		// if ((list != null) && list.contains(Page.class.getName())) {
		// page.setVisible(false);
		// }
		// }
		// for (Section section : page.getSections()) {
		// {
		// List<String> list = partMap.get(section.getLocalId());
		// if ((list != null) && list.contains(Section.class.getName())) {
		// section.setVisible(false);
		// }
		// }
		// for (AbstractQuestion q : section.getQuestions()) {
		// {
		// List<String> list = partMap.get(q.getLocalId());
		// if ((list != null) && list.contains(AbstractQuestion.class.getName())) {
		// q.setVisible(false);
		// }
		// }
		// }
		// }
		// }
	}
	
	private void clearAnswers(final AbstractQuestion q) {
		if (q instanceof IFreeTextQuestion) {
			((IFreeTextQuestion) q).setAnswerObj(null);
		} else if (q instanceof TextfieldQuestion) {
			((TextfieldQuestion) q).setAnswers(null);
		}
		
	}
	
	private boolean isSectionVisible(final Section section, final boolean previewMode) {
		if (previewMode) {
			return section.isPreviewModeVisible();
		}
		return section.isVisible();
	}
	
	private boolean isQuestionVisible(final AbstractQuestion question, final boolean previewMode) {
		if (previewMode) {
			return question.isPreviewModeVisible();
		}
		return question.isVisible();
	}
	
	/**
	 * @param questionnaire
	 */
	private void setQuestionnairePartsSubmitted(final Questionnaire questionnaire) {
		int questionnaireID = questionnaire.getId();
		for (Page page : questionnaire.getPages()) {
			if (page.isVisible()) {
				for (Section section : page.getSections()) {
					if (section.isVisible()) {
						for (AbstractQuestion q : section.getQuestions()) {
							// correct the reference to questionnaire
							q.setQuestionnaireID(questionnaireID);
							for (Option o : q.getAllOptions()) {
								if (q.isVisible()) {
									o.setSubmitted(true);
								}
								// correct the reference to the questionnaire
								o.setQuestionnaireID(questionnaireID);
							}
							if (q.isVisible()) {
								q.setSubmitted(true);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @return
	 */
	private ProcessAnswersResult validateQuestions(final Participation participation, final int currentPage) {
		Questionnaire questionnaire = participation.getQuestionnaire();
		ProcessAnswersResult result = new ProcessAnswersResult();
		if (currentPage < questionnaire.getPages().size()) {
			result.nextPage = currentPage;
			// iterate trough each question and find the anwers in the request
			// parameters
			boolean allValid = true;
			List<Page> pages = questionnaire.getPages(); // new
			// ArrayList<Page>();
			
			// for (Page page : questionnaire.getPages()) {
			// Page newPage = new Page();
			// newPage.setDescription(page.getDescription());
			// newPage.setLocalId(page.getLocalId());
			// newPage.setSections(page.getSections());
			// newPage.setTitle(page.getTitle());
			// newPage.setVisible(page.isVisible());
			// newPage.setOrderNumber(page.getOrderNumber());
			// pages.add(newPage);
			// }
			List<Section> sections = pages.get(currentPage).getSections();
			for (Section section : sections) {
				List<AbstractQuestion> questions = section.getQuestions();
				for (AbstractQuestion question : questions) {
					if (question.isVisible()) {
						if (QuestionValidator.isValid(question) == 1) {
							allValid &= false;
							result.unansweredQuestionIds.add(question.getId());
						} else if (QuestionValidator.isValid(question) == 2) {
							allValid &= false;
							result.invalidQuestionIds.add(question.getId());
						}
					}
				}
			}
			if (allValid) {
				result.nextPage = currentPage + 1;
			}
		} else {
			result.nextPage = 0;
		}
		return result;
	}
	
	private void updateVisibility(final Questionnaire questionnaire, final boolean isPreviewMode, final Session hibSess) {
		
		final Map<String, IQuestionnairePart> partMap = new HashMap<String, IQuestionnairePart>();
		// build a map of localid<=>questionnairepart of ALL the questionnaire
		// parts of ALL pages
		for (Page p : questionnaire.getPages()) {
			partMap.put(Page.class.getName().toLowerCase() + p.getLocalId(), p);
			p.setPreviewModeVisible(true);
			for (Section s : p.getSections()) {
				partMap.put(Section.class.getName().toLowerCase() + s.getLocalId(), s);
				s.setPreviewModeVisible(true);
				for (AbstractQuestion q : s.getQuestions()) {
					q.setPreviewModeVisible(true);
					partMap.put(AbstractQuestion.class.getName().toLowerCase() + q.getLocalId(), q);
				}
			}
		}
		for (IQuestionnairePart part : partMap.values()) {
			if (part instanceof AbstractQuestion) {
				Integer surveyId = questionnaire.getParticipation().getParticipant().getSurvey().getId();
				this.checkVisibilityOfQuestionnaireParts(partMap, (AbstractQuestion) part, isPreviewMode, surveyId, hibSess);
			}
		}
	}
	
	private void processAnswers(final HttpServletRequest req, final Participation participation, final int currentPage, final Session hibernateSession, final boolean isPreviewMode, final Questionnaire surveyQuestionnaire) {
		final Questionnaire questionnaire;
		if (isPreviewMode) {
			questionnaire = surveyQuestionnaire;
		} else {
			questionnaire = participation.getQuestionnaire();
		}
		
		// iterate trough each question and find the anwers in the request
		// parameters
		Set<?> entrySet = req.getParameterMap().entrySet();
		if (currentPage < questionnaire.getPages().size()) {
			List<Page> pages = questionnaire.getPages();
			for (Section section : pages.get(currentPage).getSections()) {
				for (AbstractQuestion question : section.getQuestions()) {
					if (question instanceof TextPart) {
						// nothing to do here
					} else if (question instanceof IMatrixQuestion) {
						for (AbstractQuestion subQuestion : question.getSubquestions()) {
							this.checkEntrySet(subQuestion, entrySet, req);
						}
					} else {
						this.checkEntrySet(question, entrySet, req);
					}
					hibernateSession.save(question);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param participation Participation of the questionnaire to look in
	 * @param question Question of which the logical element shall be processed
	 * @param hibSess hibernate session
	 */
	private void checkVisibilityOfQuestionnaireParts(final Map<String, IQuestionnairePart> questionMap, final AbstractQuestion question, final boolean isPreviewMode, final Integer surveyId, final Session hibSess) {
		List<QuestionnaireLogicElement> qleList = question.getLogicElements();
		for (QuestionnaireLogicElement qle : qleList) {
			if (qle != null) {
				boolean partIsVisible = false;
				IQuestionnairePart questionnairePart = questionMap.get(qle.getTypeOfPart().toLowerCase() + qle.getIdOfPart());
				if (questionnairePart != null) {
					List<Answer> refQuestionAnswer = question.getAnswer();
					
					int matchCount = 0;
					List<Answer> answers = qle.getAnswers();
					for (Answer answer : answers) {
						for (Answer answerInRefQuestion : refQuestionAnswer) {
							if (answerInRefQuestion != null) {
								if (answerInRefQuestion.getAnswer().toLowerCase().trim().equals(answer.getAnswer().toLowerCase().trim())) {
									matchCount++;
									break;
								}
							}
						}
					}
					
					
					switch (qle.getOperator()) {
					case AND:
						partIsVisible = matchCount == answers.size();
						break;
					case OR:
						partIsVisible = matchCount >= 1;
						break;
					}
					if (isPreviewMode) {
						questionnairePart.setPreviewModeVisible(partIsVisible);
						if (questionnairePart instanceof Section) {
							Section s = (Section) questionnairePart;
							
							for (AbstractQuestion q : s.getQuestions()) {
								q.setPreviewModeVisible(partIsVisible);
							}
							if (!partIsVisible) {
								this.unsubmit(s);
							}
						} else {
							if (!partIsVisible) {
								if (questionnairePart instanceof Page) {
									this.unsubmit((Page) questionnairePart);
								} else if (questionnairePart instanceof AbstractQuestion) {
									this.unsubmit((AbstractQuestion) questionnairePart);
								}
							}
						}
					} else {
						questionnairePart.setVisible(partIsVisible);
						if (questionnairePart instanceof Section) {
							Section s = (Section) questionnairePart;
							// must be propagated to children
							for (AbstractQuestion q : s.getQuestions()) {
								
								// visibility is always propagated when the section is invisible
								// it is not propagated when the section is visible AND the question is target of any logic => because then
								// the visibility of the question is controlled at another place
								if (!partIsVisible || !isTargetOfLogic(q, surveyId, hibSess)) {
									q.setVisible(partIsVisible);
								}
							}
							if (!partIsVisible) {
								this.unsubmit(s);
							}
						} else {
							if (!partIsVisible) {
								if (questionnairePart instanceof Page) {
									this.unsubmit((Page) questionnairePart);
								} else if (questionnairePart instanceof AbstractQuestion) {
									this.unsubmit((AbstractQuestion) questionnairePart);
								}
							}
						}
						
					}
				}
			}
		}
	}
	
	private boolean isTargetOfLogic(final AbstractQuestion q, final Integer surveyId, final Session hibSess) {
		Criteria c = hibSess.createCriteria(QuestionnaireLogicElement.class);
		c.setMaxResults(1);
		c.add(Restrictions.eq(ID_OF_PART, q.getLocalId()));
		c.add(Restrictions.ilike("typeOfPart", AbstractQuestion.class.getName()));
		c.add(Restrictions.eq("surveyId", surveyId));
		
		List<?> list = c.list();
		return list.size() > 0;
	}
	
	private void unsubmit(final AbstractQuestion q) {
		q.setSubmitted(false);
		List<Option> options = q.getAllOptions();
		for (Option option : options) {
			option.setSubmitted(false);
		}
	}
	
	private void unsubmit(final Section s) {
		List<AbstractQuestion> questions = s.getQuestions();
		for (AbstractQuestion q : questions) {
			this.unsubmit(q);
		}
	}
	
	private void unsubmit(final Page p) {
		List<Section> sections = p.getSections();
		for (Section s : sections) {
			this.unsubmit(s);
		}
	}
	
	/**
	 * Updates a question according to the data given in the HttpServletRequest
	 * object
	 * 
	 * @param question Question to update
	 * @param entrySet Parametermap
	 * @return true, if the question has been answered, false otherwise
	 */
	private boolean checkEntrySet(final AbstractQuestion question, final Set<?> entrySet, final HttpServletRequest req) {
		boolean answerAvailable = false;
		
		for (Object o : entrySet) {
			if (o instanceof Entry<?, ?>) {
				String key = (String) ((Entry<?, ?>) o).getKey();
				if (key.equals("q" + question.getId() + "[]")) { // checkboxinputs
					answerAvailable = true;
					// its an array of options, so fill the options list
					String[] values = req.getParameterValues(key);
					
					// iterate over all options of the question to
					// select the one that is selected and deselect the
					// ones that are not selected
					for (Option option : question.getAllOptions()) {
						boolean found = false;
						for (String optionId : values) {
							if (optionId.equals(option.getId() + "")) {
								found = true;
								break;
							}
						}
						
						option.setSelected(found);
						
					}
				} else if (key.equals("q" + question.getId())) { // radioinputs
					answerAvailable = true;
					String optionId = req.getParameter(key);
					// iterate over all options of the question to
					// select the one that is selected and deselect the
					// ones that are not selected
					for (Option option : question.getAllOptions()) {
						option.setSelected(optionId.equals(option.getId() + ""));
					}
				} else if (key.equals("text" + question.getId())) { // textinputs
					answerAvailable = true;
					// its a free text entry
					String[] text = (String[]) ((Entry<?, ?>) o).getValue();
					Answer answerObj = ((FreeTextQuestion) question).getAnswerObj();
					if (answerObj == null) {
						((FreeTextQuestion) question).setAnswerObj(new Answer(text[0]));
					} else {
						answerObj.setAnswer(text[0]);
					}
					
				} else if (key.equals(SingleLineQuestionView.CONTROLNAME + question.getId())) { // textinputs
					answerAvailable = true;
					// its a free text entry
					String[] text = (String[]) ((Entry<?, ?>) o).getValue();
					Answer answerObj = ((SingleLineQuestion) question).getAnswerObj();
					if (answerObj == null) {
						((SingleLineQuestion) question).setAnswerObj(new Answer(text[0]));
					} else {
						answerObj.setAnswer(text[0]);
					}
				} else if (key.equals(AverageNumberQuestionView.CONTROLNAME + question.getId())) {
					answerAvailable = true;
					// its a free text entry
					String[] text = (String[]) ((Entry<?, ?>) o).getValue();
					Answer answerObj = ((AverageNumberQuestion) question).getAnswerObj();
					if (answerObj == null) {
						((AverageNumberQuestion) question).setAnswerObj(new Answer(text[0]));
					} else {
						answerObj.setAnswer(text[0]);
					}
					
				} else if (key.equals("text" + question.getId() + "[]")) { // textfieldinputs
					String[] values = req.getParameterValues(key);
					List<Answer> answers = ((TextfieldQuestion) question).getAnswers();
					for (int i = 0; i < values.length; i++) {
						if (!values[i].equals("")) {
							answerAvailable = true;
						}
						if (i > (answers.size() - 1)) {
							answers.add(new Answer(values[i]));
						} else {
							if (answers.get(i) == null) {
								answers.set(i, new Answer(values[i]));
							} else {
								answers.get(i).setAnswer(values[i]);
							}
						}
					}
				} else if (key.equals("combo" + question.getId())) { // comboinputs
					answerAvailable = true;
					// its a combo box entry
					String optionId = ((String[]) ((Entry<?, ?>) o).getValue())[0];
					// iterate over all options of the question to
					// select the one that is selected and deselect the
					// ones that are not selected
					for (Option option : question.getAllOptions()) {
						option.setSelected(optionId.equals(option.getId() + ""));
					}
				}
			}
			
		}
		return answerAvailable;
	}
	
	private String getServerBaseUrl() {
		return (String) EnvironmentConfiguration.getConfiguration(ConfigID.HOST) + (String) EnvironmentConfiguration.getConfiguration(ConfigID.URLBASE);
	}
	
}