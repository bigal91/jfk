package de.cinovo.surveyplatform.ui.pages;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration.ConfigID;
import de.cinovo.surveyplatform.constants.Constants;
import de.cinovo.surveyplatform.constants.ExtendedProperties;
import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.PaymentModel;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.HyperLinkUtil;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
public class EditQuestionnaireContainer extends AbstractContainer {
	
	private static final String PARAM_ALLPAGES = "allPages";
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		
		if (currentUser != null) {
			
			if (ParamUtil.checkAllParamsSet(request, SurveyDal.PARAM_SURVEYID)) {
				int surveyId = ParamUtil.getSafeIntFromParam(request, SurveyDal.PARAM_SURVEYID);
				
				StringBuilder questionnaire = new StringBuilder();
				
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					
					Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
					
					if (survey != null) {
						
						if (AuthUtil.isAllowedToEditThisSurvey(currentUser, survey, hibSess)) {
							
							final Map<String, String> replacements = new LinkedHashMap<String, String>();
							
							content.append(PartsUtil.getPageHeader("Questionnaire of " + survey.getName(), HelpIDs.PAGE_SURVEY, new String[] {"<a href=\"?page=" + Pages.PAGE_MANAGE_SURVEYS + "\">" + Pages.PAGE_HEADER_MANAGE_SURVEYS + "</a>"}, false));
							List<Page> pages = survey.getQuestionnaire().getPages();
							int pageCount = pages.size();
							
							boolean showAllPages = false;
							int pageNumber = ParamUtil.getSafeIntFromParam(request, SurveyDal.PARAM_PAGEID);
							if (ParamUtil.checkAllParamsSet(request, PARAM_ALLPAGES)) {
								showAllPages = true;
								pageNumber = 0;
							}
							
							if (pageNumber > (pageCount - 1)) {
								pageNumber = pageCount - 1;
							}
							
							// build control buttons
							StringBuilder buttonNextPage = new StringBuilder();
							StringBuilder buttonPreview = new StringBuilder();
							StringBuilder buttonPreviousPage = new StringBuilder();
							StringBuilder buttonInsertPageInFront = new StringBuilder();
							StringBuilder buttonInsertPageAfter = new StringBuilder();
							StringBuilder buttonAddPage = new StringBuilder();
							StringBuilder linkShowAllPages = new StringBuilder();
							StringBuilder directNavigation = new StringBuilder();
							
							buttonPreview.append("<div style=\"margin-left: 10px;\">");
							buttonPreview.append("<a class=\"gui-icon-button-PAGE_WHITE_MAGNIFY_BIG\" title=\"Preview Questionnaire\" href=\"" + EnvironmentConfiguration.getUrlBase() + "/participate?pid=preview&reset=1&page=" + pageNumber + "&surveyID=" + survey.getId() + "\" target=\"_blank\"></a>");
							buttonPreview.append("<a class=\"gui-icon-button-PDF_BIG\" title=\"Download the empty Questionnaire\" href=\"/download?type=questionnaire&surveyID=" + survey.getId() + "\"></a>");
							buttonPreview.append("</div>");
							if (pageNumber > 0) {
								buttonPreviousPage.append("<a class=\"button navButton\" href=\"" + HyperLinkUtil.getLinkURI(request, new StringStringPair(SurveyDal.PARAM_PAGEID, (pageNumber - 1) + "")) + "\">Previous Page</a>");
							}
							if (pageNumber < (pageCount - 1)) {
								buttonNextPage.append("<a class=\"button navButton\" href=\"" + HyperLinkUtil.getLinkURI(request, new StringStringPair(SurveyDal.PARAM_PAGEID, (pageNumber + 1) + "")) + "\">Next Page</a>");
							}
							if (showAllPages || (pageNumber == (pageCount - 1))) {
								buttonAddPage.append("<a href=\"javascript:void(0);\" title=\"Add a new page at the end of the questionnaire\" class=\"addPageButton button\">Add New Page</a>");
							}
							if (pageNumber <= (pageCount - 1)) {
								if (pages.size() > 0) {
									buttonInsertPageInFront.append(getInsertPageInFrontButton(pages.get(pageNumber).getId()));
									if (!showAllPages&&(pageNumber < (pageCount - 1))) {
										buttonInsertPageAfter.append("<a href=\"javascript:void(0);\" title=\"Insert a new page after the current page\" class=\"insertPageButton button\" onclick=\"insertEmptyPage(" + pages.get(pageNumber + 1).getId() + ", true);\">Insert Page</a>");
									}
								}
							}
							if (pageCount > 1) {
								if (showAllPages) {
									linkShowAllPages.append(HyperLinkUtil.buildHyperLink(HyperLinkUtil.removeParamFromRequest(HyperLinkUtil.getLinkURI(request), "allPages"), "Show pages separately", "Show each page separately", "link"));
								} else {
									linkShowAllPages.append("<div style=\"text-align: center; position: absolute; left: 50%; width: 100px;\"><div style=\"margin:0px 0px 0px -60px; width: 100px;\">" + HyperLinkUtil.getHtmlLink("Show all pages of the questionnaire on one page", "link", "Show all pages", request, new StringStringPair("allPages", "1")) + "</div></div>");
								}
								fillDirectNavigation(directNavigation, pageCount, pageNumber, request);
							}
							
							replacements.put("NEXTPAGELINK", HyperLinkUtil.getLinkURI(request, new StringStringPair(SurveyDal.PARAM_PAGEID, (pageNumber + 1) + "")));
							
							boolean canAddQuestions = true;
							if (PaymentModel.Trial.equals(GroupManager.getPaymentModel(currentUser))) {
								int qCount = 0;
								for (Page page : pages) {
									for (Section section : page.getSections()) {
										qCount += section.getQuestions().size();
									}
								}
								if (qCount >= Constants.TRIAL_QUESTION_LIMIT) {
									canAddQuestions = false;
								}
							}
							
							// show all pages or one page
							StringBuilder pageIds = new StringBuilder();
							StringBuilder sectionIds = new StringBuilder();
							StringBuilder questionIds = new StringBuilder();
							TreeMap<Integer, Page> sortedPagesByLocalID = new TreeMap<Integer, Page>();
							TreeMap<Integer, Section> sortedSectionsByLocalID = new TreeMap<Integer, Section>();
							TreeMap<Integer, AbstractQuestion> sortedQuestionsByLocalID = new TreeMap<Integer, AbstractQuestion>();
							
							for (Page p : pages) {
								sortedPagesByLocalID.put(p.getLocalId(), p);
								List<Section> sections = p.getSections();
								Iterator<Section> iterator = sections.iterator();
								while (iterator.hasNext()) {
									Section section = iterator.next();
									sortedSectionsByLocalID.put(section.getLocalId(), section);
									List<AbstractQuestion> questions = section.getQuestions();
									for (AbstractQuestion question : questions) {
										sortedQuestionsByLocalID.put(question.getLocalId(), question);
									}
								}
							}
							for (Page page : sortedPagesByLocalID.values()) {
								pageIds.append("<option value=\"" + page.getLocalId() + "\">" + page.getLocalId() + "</option>");
							}
							for (Section section : sortedSectionsByLocalID.values()) {
								sectionIds.append("<option value=\"" + section.getLocalId() + "\">" + section.getLocalId() + "</option>");
							}
							for (AbstractQuestion question : sortedQuestionsByLocalID.values()) {
								questionIds.append("<option value=\"" + question.getLocalId() + "\">" + question.getLocalId() + "</option>");
							}
							replacements.put("QUESTIONIDS", questionIds.toString());
							replacements.put("SECTIONIDS", sectionIds.toString());
							replacements.put("PAGEIDS", pageIds.toString());
							
							boolean overrideLastPage = false;
							if (!ExtendedProperties.get(survey, ExtendedProperties.SURVEY_LASTPAGEISMANDATORY, true)) {
								overrideLastPage = true;
							}
							
							if (showAllPages) {
								StringBuilder infoPageNumber = new StringBuilder(getInfoPageNumber(pageNumber, pageCount));
								replacements.put("BUTTONSTOP", getButtonBox(true, floatPart(buttonPreview, true), floatPart(linkShowAllPages, true), infoPageNumber, floatPart(buttonInsertPageInFront, false)));
								pageNumber = 0;
								questionnaire.append(getCopyMovePlaceholder(surveyId, "Page", pageNumber, request));
								
								for (Page p : pages) {
									
									buttonInsertPageInFront.delete(0, buttonInsertPageInFront.length());
									buttonInsertPageInFront.append(getInsertPageInFrontButton(pages.get(pageNumber).getId()));
									infoPageNumber.delete(0, infoPageNumber.length());
									infoPageNumber.append(getInfoPageNumber(pageNumber, pageCount));
									if (pageNumber > 0) {
										questionnaire.append("<li>" + getButtonBox(false, infoPageNumber, floatPart(buttonInsertPageInFront, false), floatPart(linkShowAllPages, true)) + "</li>");
									}
									questionnaire.append(getPageContent(p, replacements, "", showAllPages, !overrideLastPage && (pageNumber == (pageCount - 1)), canAddQuestions, request, sectionIds, questionIds));
									pageNumber++;
									if (pageNumber < pageCount) {
										questionnaire.append(getCopyMovePlaceholder(surveyId, "Page", pageNumber, request));
									}
								}
								if (pageNumber < pageCount) {
									questionnaire.append("<li>" + getButtonBox(false, buttonAddPage) + "</li>");
								}
								
							} else {
								StringBuilder infoPageNumber = new StringBuilder(getInfoPageNumber(pageNumber, pageCount));
								directNavigation.insert(0, infoPageNumber);
								if (pageNumber >= 0) {
									Page p = pages.get(pageNumber);
									replacements.put("BUTTONSTOP", getButtonBox(true, floatPart(buttonPreview, true), floatPart(buttonNextPage, true), floatPart(buttonPreviousPage, false), floatPart(buttonInsertPageInFront, false), linkShowAllPages));
									questionnaire.append(getPageContent(p, replacements, directNavigation.toString(), showAllPages, !overrideLastPage && (pageNumber == (pageCount - 1)), canAddQuestions, request, sectionIds, questionIds));
									if (pageNumber < (pageCount - 1)) {
										replacements.put("BUTTONSBOTTOM", getButtonBox(false, floatPart(buttonPreviousPage, false), floatPart(buttonInsertPageAfter, false), floatPart(buttonAddPage, false), floatPart(buttonNextPage, true)));
									} else {
										replacements.put("BUTTONSBOTTOM", getButtonBox(false, floatPart(buttonPreviousPage, false)));
									}
								}
							}
							tx.commit();
							
							// do the hucklebuck
							replacements.put("SURVEYID", String.valueOf(surveyId));
							replacements.put("QUESTIONNARE", questionnaire.toString());
							
							content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/editSurvey.html", replacements));
						} else {
							content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
						}
					} else {
						content.append(HtmlFormUtil.getErrorMessage("The Survey with the ID " +surveyId + " does not exist!"));
					}
				} finally {
					hibSess.close();
				}
			} else {
				content.append("No id provided.");
			}
		} else {
			content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
		}
	}
	
	private String getInsertPageInFrontButton(final int id) {
		return "<a href=\"javascript:void(0);\" title=\"Insert a new page in front of the current page\" class=\"insertPageButton button\" onclick=\"insertEmptyPage(" + id + ");\">Insert Page</a>";
	}
	
	private void fillDirectNavigation(final StringBuilder navigation, final int pageCount, final int pageNumber, final HttpServletRequest request) {
		boolean dotsShown = false;
		for (int i = 0; i < pageCount; i++) {
			boolean showButton = false;
			boolean showDots = false;
			if (pageCount < 10) {
				showButton = true;
			} else if ((i == 0) || (i == (pageCount - 1))) {
				showButton = true;
			} else if ((i < 6) && (pageNumber < 4)) {
				showButton = true;
			} else if ((i > (pageCount - 6)) && (pageNumber > (pageCount - 4))) {
				showButton = true;
			} else if ((pageNumber < 4) && (i == (pageCount - 2))) {
				showDots = true;
			} else if ((pageNumber > (pageCount - 4)) && (i == 2)) {
				showDots = true;
			} else if ((i > (pageNumber - 3)) && (i < (pageNumber + 3))) {
				showButton = true;
			} else if ((i == (pageNumber - 4)) || (i == (pageNumber + 4))) {
				showDots = true;
			} else {
				showDots = true;
				showButton = false;
			}
			
			if (showButton) {
				dotsShown = false;
				navigation.append("<a " + (i == (pageCount - 1) ? "style=\"color: DarkOrange;\" title=\"Jump to last page\"" : "") + " class=\"button navButton small" + (i == pageNumber ? " selected" : "") + "\" title=\"Jump to page " + (i + 1) + "\" href=\"" + HyperLinkUtil.getLinkURI(request, new StringStringPair(SurveyDal.PARAM_PAGEID, i + "")) + "\">" + (i + 1) + "</a>");
			} else if (showDots && !dotsShown) {
				dotsShown = true;
				navigation.append("...");
			}
		}
	}
	
	private String getInfoPageNumber(final int pageNumber, final int pageCount) {
		return "<div class=\"infoPageNumber\"><span>Page " + (pageNumber + 1) + " of " + pageCount + "</span></div>";
	}
	
	private String getButtonBox(final boolean top, final StringBuilder... parts) {
		StringBuilder buttonBox = new StringBuilder();
		buttonBox.append("<div class=\"buttonBox" + (top ? "" : " buttonBoxBottom") + "\">");
		for (StringBuilder part : parts) {
			buttonBox.append(part);
		}
		buttonBox.append("</div>");
		return buttonBox.toString();
	}
	
	private StringBuilder floatPart(final StringBuilder part, final boolean right) {
		part.insert(0, "<div style=\"float: " + (right ? "right" : "left") + ";\">");
		part.append("</div>");
		return part;
	}
	
	private String getPageContent(final Page p, final Map<String, String> replacements, final String navigation, final boolean showAllPages, final boolean isLastPage, final boolean canAddQuestions, final HttpServletRequest request, final StringBuilder sectionIds, final StringBuilder questionIds) {
		StringBuilder pageContent = new StringBuilder();
		
		pageContent.append("<li><a name=\"page" + p.getId() + "\" class=\"anchor\"></a>");
		pageContent.append(QuestionnaireViewUtil.getPageControls(p, navigation, showAllPages, !isLastPage) + "<ul class=\"questionarePage lid" + p.getLocalId() + "\" id=\"page" + p.getId() + "\">");
		
		List<Section> sections = p.getSections();
		
		// Detach list from hibernate context (otherwise,
		// sorting of list causes multiple insert and delete
		// statements)
		sections = new ArrayList<Section>(sections);
		
		
		String addSectionPart = "";
		if (!isLastPage && canAddQuestions) {
			addSectionPart += "<input type=\"button\" class=\"addSectionButton\" id=\"asb" + p.getId() + "\" name=\"addSectionButton" + p.getId() + "\" value=\"Add Section\" />";
		}
		int pagePosition = 0;
		pageContent.append(getCopyMovePlaceholder(p.getId(), "Section", pagePosition, request));
		
		if (sections.size() > 0) {
			Iterator<Section> iterator = sections.iterator();
			while (iterator.hasNext()) {
				Section section = iterator.next();
				String sectionType = "section";
				
				if ((section.getSectionTitle() != null) && !section.getSectionTitle().equals("")) {
					pageContent.append("<li class=\"section\" id=\"section" + section.getId() + "\"><a name=\"section" + section.getId() + "\"></a>" + QuestionnaireViewUtil.getSectionControls(section, isLastPage) + "<p class=\"sectionTitle\" id=\"sectionTitle" + section.getId() + "\">" + section.getSectionTitle() + "</p><ul class=\"questionareSection lid" + sectionType + section.getLocalId() + "\">");
				} else {
					pageContent.append("<li class=\"section\" id=\"section" + section.getId() + "\"><a name=\"section" + section.getId() + "\"></a>" + QuestionnaireViewUtil.getSectionControls(section, isLastPage) + "<p class=\"invisibleSectionTitleForEditor\" id=\"sectionTitle" + section.getId() + "\"></p><ul class=\"questionareSection lid" + sectionType + section.getLocalId() + "\">");
				}
				
				List<AbstractQuestion> questions = section.getQuestions();
				int sectionPosition = 0;
				
				pageContent.append(getCopyMovePlaceholder(section.getId(), "Question", sectionPosition, request));
				for (AbstractQuestion question : questions) {
					int lastIndexOfDot = AbstractQuestion.class.getName().lastIndexOf(".");
					String questionType = AbstractQuestion.class.getName().substring(lastIndexOfDot + 1).toLowerCase();
					pageContent.append("<li class=\"question" + (question.isVisible() ? "" : " invisibleQuestion") + " lid" + questionType + question.getLocalId() + "\" id=\"question" + question.getId() + "\">");
					pageContent.append("<div style=\"margin: 0px 5px 10px 5px;\">");
					//addLogicSource as hidden
					StringBuilder answersOfQuestion = new StringBuilder();
					for (Option option : question.getOptions()){
						answersOfQuestion.append("<option class=\"" + option.getId() + "\" value=\"" + option.getDisplayName() + "\">" + option.getDisplayName() + "</option>");
					}
					replacements.put("ANSWEROPTIONS", answersOfQuestion.toString());
					replacements.put("QUESTID", String.valueOf(question.getLocalId()));
					pageContent.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/buildLogicCondition.html", replacements));
					pageContent.append(QuestionnaireViewUtil.getQuestionHTMLRepresentation(question, true, !isLastPage) + "</div></li>");
					
					sectionPosition++;
					pageContent.append(getCopyMovePlaceholder(section.getId(), "Question", sectionPosition, request));
				}
				if (!isLastPage) {
					pageContent.append("<li>");
					if (canAddQuestions) {
						pageContent.append("<input type=\"button\" class=\"addQuestionButton\" id=\"aqb" + section.getId() + "\" name=\"addQuestionButton" + section.getId() + "\" value=\"Add Question\" />");
					} else {
						pageContent.append("You can add up to " + Constants.TRIAL_QUESTION_LIMIT + " questions in the trial account. <a style=\"color: #2244ca;\" href=\"" + EnvironmentConfiguration.getConfiguration(ConfigID.CONTACT_URL) + "\">Contact us here</a> to upgrade your account and remove this limit!");
					}
				}
				if (!iterator.hasNext()) {
					pageContent.append(addSectionPart);
				}
				if (!isLastPage && canAddQuestions) {
					pageContent.append("</li>");
				}
				pageContent.append("</ul></li>");
				pagePosition++;
				pageContent.append(getCopyMovePlaceholder(p.getId(), "Section", pagePosition, request));
			}
		} else {
			pageContent.append(addSectionPart);
		}
		replacements.put("CURRENTPAGE", p.getId() + "");
		pageContent.append("</ul></li>");
		return pageContent.toString();
	}
	
	private String getCopyMovePlaceholder(final int parentPartId, final String partType, final int position, final HttpServletRequest request) {
		StringBuilder placeHolder = new StringBuilder("<li><div class=\"insertPartPlaceHolder insert");
		placeHolder.append(partType);
		placeHolder.append("PlaceHolder\"></div><div class=\"insertPart insert");
		placeHolder.append(partType);
		placeHolder.append("\">");
		placeHolder.append(PartsUtil.getIconLink("pasteLink" + parentPartId + "", "Click to insert the " + partType + " you want to move or copy", "Insert " + partType + " here", "javascript:insert" + partType + "(" + parentPartId + "," + position + ");"));
		placeHolder.append("</div></li>");
		return placeHolder.toString();
	}
	
}
