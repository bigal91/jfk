package de.cinovo.surveyplatform.ui.pages;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.help.ContextHelpProvider;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
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
public class SurveyListContainer extends AbstractContainer {
	
	private final static String PARAM_BUTTON_CONDUCTSURVEY = "bConductSurvey";
	private final static String PARAM_BUTTON_CLOSESURVEY = "bCloseSurvey";
	
	private List<ISurvey> surveys = new ArrayList<ISurvey>();
	
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		content.append(getContent(request, null, currentUser));
	}
	
	/**
	 * @param request -
	 * @param overrideReplacements -
	 * @param currentUser -
	 * @return -
	 */
	public String getContent(final HttpServletRequest request, final Map<String, String> overrideReplacements, final SystemUser currentUser) {
		StringBuilder content = new StringBuilder();
		if (currentUser != null) {
			
			if (AuthUtil.isAllowedToListSurveys(currentUser)) {
				
				Session hibSess = HibernateUtil.getSessionFactory().openSession();
				try {
					Transaction tx = hibSess.beginTransaction();
					
					if (ParamUtil.checkAllParamsSet(request, SurveyDal.PARAM_SURVEYID)) {
						int surveyId = ParamUtil.getSafeIntFromParam(request, SurveyDal.PARAM_SURVEYID);
						if (ParamUtil.checkAllParamsSet(request, PARAM_BUTTON_CONDUCTSURVEY)) {
							conductSurvey(currentUser, surveyId, hibSess);
						} else if (ParamUtil.checkAllParamsSet(request, PARAM_BUTTON_CLOSESURVEY)) {
							closeSurvey(currentUser, surveyId, hibSess);
						}
					}
					
					Criteria criteria = hibSess.createCriteria(Survey.class);
					
					criteria.add(Restrictions.eq("deleted", false));
					criteria.addOrder(Order.asc("name"));
					
					Map<String, String> replacements = new HashMap<String, String>();
					Map<String, String> rowReplacements = new HashMap<String, String>();
					
					StringBuilder tableRowsTemplate = new StringBuilder();
					StringBuilder tableRowsRunning = new StringBuilder();
					StringBuilder tableRowsClosed = new StringBuilder();
					
					List<?> list = criteria.list();
					synchronized (this) {
						this.surveys.clear();
					}
					Collection<UserGroup> visibleGroups = GroupManager.getVisibleGroups(hibSess, currentUser, currentUser);
					
					for (Object obj : list) {
						if (obj instanceof Survey) {
							Survey survey = (Survey) obj;
							
							// only show the survey if the current user has created
							// it or has the right to edit all surveys
							// hint: this condition is not restriction through the
							// critera above, because systemtemplates shall occur in
							// the selection of templates anyway
							if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR) || visibleGroups.contains(survey.getOwner())) {
								synchronized (this) {
									this.surveys.add(survey);
								}
								
								String highLight = "";
								String highlightClass = "";
								Calendar calendar = Calendar.getInstance();
								calendar.add(Calendar.HOUR, -3);
								if (survey.getCreationDate().compareTo(calendar.getTime()) > 0) {
									highlightClass = "class=\"highLightNewSurvey\"";
									highLight = "<td><span class=\"invisible\">0</span></td>";
								} else {
									highLight = "<td><span class=\"invisible\">1</span></td>";
								}
								
								rowReplacements.put("HIGHLIGHT", highLight);
								rowReplacements.put("NAME", "<td><a href=\"javascript:void(0);\" onclick=\"showsurveyInfo(" + survey.getId() + ");\"><span id=\"" + SurveyDal.PARAM_SURVEYNAME + survey.getId() + "\" " + highlightClass + ">" + survey.getName() + "</span></a></td>");
								if (AuthUtil.isAdmin(currentUser)) {
									rowReplacements.put("OWNER", "<td>" + survey.getOwner().getClient().getOrganization() + " - " + survey.getOwner().getName() + "</td>");
								} else {
									rowReplacements.put("OWNER", "<td>" + survey.getOwner().getName() + "</td>");
								}
								if (survey.getState() != SurveyState.CLOSED) {
									// rowReplacements.put("CREATED", "<td><span class=\"invisible\">" + survey.getCreationDate().getTime()
									// + "</span>" + TimeUtil.getLocalTime(currentUser, survey.getCreationDate()) + "</td>");
									rowReplacements.put("STATE", "<td>" + survey.getStateDisplayname() + "</td>");
								} else {
									rowReplacements.put("STATE", "");
									// rowReplacements.put("CREATED", "");
								}
								
								int progress = 0;
								if (survey.getState().ordinal() > SurveyState.CREATED.ordinal()) {
									progress = SurveyUtil.calculateReturnRate(currentUser, survey);
									rowReplacements.put("PROGRESS", "<td><span class=\"invisible\">" + String.format("%03d", progress) + "</span>" + PartsUtil.getProgressBar(progress) + "</td>");
								} else {
									if (survey.getState().equals(SurveyState.CREATED)) {
										String linkProgress = "<td style=\"text-align: right;\"><div style=\"float: right; width: 130px; text-align:center;\">";
										if (survey.getParticipants().size() == 0) {
											linkProgress += "<a class=\"inTableAction\" href=\"?page=participants&surveyID=" + survey.getId() + "\" title=\"Click to administrate the participants of this survey\">Add participants</a>";
										} else {
											linkProgress += "<a class=\"inTableAction\" href=\"javascript:void(0);\" title=\"Click to open the survey information, where you can release the survey\" onclick=\"showsurveyInfo(" + survey.getId() + ");\">Release survey</a>";
										}
										linkProgress += "</td></div>";
										rowReplacements.put("PROGRESS", linkProgress);
									} else {
										rowReplacements.put("PROGRESS", "");
									}
								}
								
								rowReplacements.put("SURVEYID", survey.getId() + "");
								
								StringBuilder buttons = new StringBuilder();
								if (survey.getState().equals(SurveyState.CLOSED)) {
									buttons.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTableButton_analyseSurvey.html", rowReplacements));
								}
								if (!survey.getState().equals(SurveyState.SYSTEMTEMPLATE)) {
									// participants cannot be added to a
									// systemtemplate
									buttons.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTableButton_administrateParticipants.html", rowReplacements));
								}
								if (!survey.getState().equals(SurveyState.CLOSED)) {
									if ((survey.getState() == SurveyState.SYSTEMTEMPLATE) || ((survey.getState() == SurveyState.TEMPLATE) || (survey.getState() == SurveyState.CREATED))) {
										// the questionnaire can only be edited in
										// templates
										buttons.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTableButton_editQuestionnare.html", rowReplacements));
									}
								}
								buttons.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTableButton_downloadQuestionnare.html", rowReplacements));
								
								rowReplacements.put("BUTTONS", buttons.toString());
								rowReplacements.put("WIDTH", "30");
								
								rowReplacements.put("CHECKBOX", "<input class=\"surveyCheckBox\" type=\"checkbox\" value=\"" + survey.getId() + "\" name=\"surveys[]\" />");
								if ((survey.getState() == SurveyState.SYSTEMTEMPLATE) || (survey.getState() == SurveyState.TEMPLATE)) {
									tableRowsTemplate.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_row.html", rowReplacements));
								} else if (survey.getState() == SurveyState.CLOSED) {
									tableRowsClosed.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_row.html", rowReplacements));
								} else {
									tableRowsRunning.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_row.html", rowReplacements));
								}
							} else if (survey.getState() == SurveyState.SYSTEMTEMPLATE) {
								
								// this will add each system template to the list of
								// suveys (could be improved by introducing user
								// groups, for providing different types of surveys
								// to different types of users)
								synchronized (this) {
									this.surveys.add(survey);
								}
							}
						}
					}
					replacements.put("SYSTEMPLATESTR", SurveyState.getDisplayName(SurveyState.SYSTEMTEMPLATE));
					replacements.put("TEMPLATESTR", SurveyState.getDisplayName(SurveyState.TEMPLATE));
					replacements.put("CREATEDSTR", SurveyState.getDisplayName(SurveyState.CREATED));
					replacements.put("RUNNINGSTR", SurveyState.getDisplayName(SurveyState.RUNNING));
					replacements.put("CLOSEDSTR", SurveyState.getDisplayName(SurveyState.CLOSED));
					
					replacements.put("ENABLEDIALOGBUTTONS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfoEnableDialogButtons.html", null));
					// replacements.put("HANDLEDIALOGBUTTONS",
					// TemplateUtil.getTemplate(Paths.TEMPLATEPATH +
					// "/dlgSurveyInfoHandleDialogButtonsManage.html",
					// replacements));
					
					replacements.put("BUTTONCOLUMNWIDTH", (90) + "");
					replacements.put("DIALOGSURVEYINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgSurveyInfo.html", replacements));
					if (overrideReplacements != null) {
						replacements.putAll(overrideReplacements);
					}
					if (tableRowsTemplate.length() > 0) {
						replacements.put("ROWS_TEMPLATES", tableRowsTemplate.toString());
						replacements.put("TABLE_TEMPLATE_SURVEYS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_templates.html", replacements));
					}
					if (tableRowsRunning.length()> 0) {
						replacements.put("SECTIONHEADER", "Active Surveys");
						replacements.put("HELPLINK", ContextHelpProvider.getInstance().getHelpLink("table.running", "Click to get detailed info", "", ""));
						replacements.put("ROWS_RUNNING", tableRowsRunning.toString());
						replacements.put("SHOW_ALL_STYLE", ",\"sDom\": 'fprt<\"bottom\"<\"manageSurveysOverall\"i>'");
						replacements.put("TABLE_RUNNING_SURVEYS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_running.html", replacements));
					}
					if (tableRowsClosed.length() > 0) {
						replacements.put("ROWS_CLOSED", tableRowsClosed.toString());
						replacements.put("TABLE_CLOSED_SURVEYS", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable_closed.html", replacements));
					}
					
					content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/surveyTable.html", replacements));
					tx.commit();
				} finally {
					hibSess.close();
				}
				
			} else {
				content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
			}
		}
		
		return content.toString();
	}
	
	private void closeSurvey(final SystemUser currentUser, final int surveyId, final Session hibSess) {
		Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
		
		survey.setClosedAtDate(new Date());
		survey.setState(SurveyState.CLOSED);
		hibSess.save(survey);
		Logger.logUserActivity("Closed a survey: " + survey.getName() + "(" + survey.getId() + ")", currentUser.getUserName());
	}
	
	private void conductSurvey(final SystemUser currentUser, final int surveyId, final Session hibSess) {
		Survey survey = (Survey) hibSess.get(Survey.class, surveyId);
		
		survey.setRunningSinceDate(new Date());
		survey.setState(SurveyState.RUNNING);
		hibSess.save(survey);
		Logger.logUserActivity("Starts a survey: " + survey.getName() + "(" + survey.getId() + ")", currentUser.getUserName());
	}
	
	/**
	 * @return the surveys
	 */
	public synchronized List<ISurvey> getSurveys() {
		return Collections.unmodifiableList(this.surveys);
	}
}
