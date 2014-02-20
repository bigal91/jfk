package de.cinovo.surveyplatform.constants;

import de.cinovo.surveyplatform.model.question.AbstractQuestion;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class Queries {
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>surveyID (Integer)</li>
	 * </ol>
	 */
	public static final String PARTICIPANT_BY_SURVEY = "Select p from Participant p, Survey s WHERE p.id in (SELECT id FROM s.participants WHERE s.id = ?1)";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>surveyID (Integer)</li>
	 * </ol>
	 */
	public static final String PARTICIPANT_BY_SURVEYDEMO = "Select p from Participant p, SurveyDemo s WHERE p.id in (SELECT id FROM s.participants WHERE s.id = ?1)";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>pageID (Integer)</li>
	 * </ol>
	 */
	public static final String SURVEY_BY_PAGE = "Select s From Survey s, Questionnaire q WHERE s.questionnaire = q.id AND ?1 in (Select id FROM q.pages)";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>participantID (Integer)</li>
	 * </ol>
	 */
	public static final String SURVEY_BY_PARTICIPANT = "Select s From Survey s WHERE ?1 in (Select id FROM s.participants)";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>participantID (Integer)</li>
	 * </ol>
	 */
	public static final String SURVEYDEMO_BY_PARTICIPANT = "Select s From SurveyDemo s WHERE ?1 in (Select id FROM s.participants)";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>participationID (Integer)</li>
	 * </ol>
	 */
	public static final String SURVEY_BY_PARTICIPATION = "Select s from Survey s JOIN s.participants as participant WHERE participant.participation.id = ?1";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>participationID (Integer)</li>
	 * </ol>
	 */
	public static final String SURVEYDEMO_BY_PARTICIPATION = "Select s from SurveyDemo s JOIN s.participants as participant WHERE participant.participation.id = ?1";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>sectionID (Integer)</li>
	 * </ol>
	 */
	// public static final String SURVEY_BY_SECTION =
	// "Select s from Survey s JOIN s.pages as page JOIN page.sections as section WHERE section.id = ?";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>questionID (Integer)</li>
	 * </ol>
	 */
	public static final String ABSTRACTQUESTION_BY_ID = "Select q From " + AbstractQuestion.class.getName() + " q WHERE q.id = ?1";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>questionID (Integer)</li>
	 * </ol>
	 */
	public static final String ABSTRACTQUESTION_BY_LOCALID = "Select q From " + AbstractQuestion.class.getName() + " q WHERE q.localId = ?1";
	
	// /**
	// * Parameters:
	// * <ol>
	// * <li>questionID (Integer)</li>
	// * </ol>
	// */
	// public static final String SECTION_BY_QUESTIONID =
	// "Select section_id FROM survey." + AbstractQuestion.class.getSimpleName()
	// + " WHERE questions_id = ?";
	//
	// /**
	// * Parameters:
	// * <ol>
	// * <li>sectionID (Integer)</li>
	// * </ol>
	// */
	// public static final String PAGE_BY_SECTIONID =
	// "Select page_id FROM survey." + Section.class.getSimpleName() +
	// " WHERE sections_id = ?";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>pageID (Integer)</li>
	 * </ol>
	 */
	public static final String QUESTIONNAIRE_BY_PAGE = "Select q From Questionnaire q Where ?1 in (Select id From q.pages)";
	
	/**
	 * No parameters
	 */
	public static final String GET_MAX_QUESTION_ORDERNUMBER = "Select Max(q.orderNumber) From AbstractQuestion q";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>survey id (Integer)</li>
	 * </ol>
	 */
	public static final String GET_MAX_PARTICIPANT_NUMBER = "Select Max(p.number) From Participant p, Survey s Where s.id = ?1 and p.id in (Select id FROM s.participants)";
	
	/**
	 * No parameters
	 */
	public static final String GET_MAX_SECTION_ORDERNUMBER = "Select Max(s.orderNumber) From Section s";
	
	/**
	 * No parameters
	 */
	public static final String GET_MAX_PAGE_ORDERNUMBER = "Select Max(p.orderNumber) From Page p";
	
	/**
	 * !Do session.createSQLQuery() here!
	 * Parameters:
	 * <ol>
	 * <li>pageID (Integer)</li>
	 * <li>sectionID (Integer)</li>
	 * </ol>
	 */
	public static final String RECONNECT_SECTION_TO_PAGE = "Update survey.section s Set page_id = ?1 WHERE s.id = ?2";
	
	/**
	 * !Do session.createSQLQuery() here!
	 * Parameters:
	 * <ol>
	 * <li>pageID (Integer)</li>
	 * <li>sectionID (Integer)</li>
	 * </ol>
	 */
	public static final String RECONNECT_QUESTION_TO_SECTION = "Update survey.abstractquestion q Set section_id = ?1 WHERE q.id = ?2";
	
	/**
	 * <ol>
	 * <li>list (List&lt;Integer&gt;)</li>
	 * </ol>
	 */
	public static final String OPTIONS_BY_QUESTIONID = "Select o from Option o Where o.submitted = true AND o.originQuestionId in (:list) AND o.questionnaireID in (:questionnaires) Order by o.id";
	
	/**
	 * Parameters:
	 * <ol>
	 * <li>survey id (Integer)</li>
	 * </ol>
	 */
	public static final String SURVEYS_BY_CLIENT = "Select s from Survey s, UserGroup g, Client c Where c = g.client AND g = s.owner AND c.id = ?1";
	
}
