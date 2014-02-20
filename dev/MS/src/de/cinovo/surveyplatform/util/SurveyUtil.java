package de.cinovo.surveyplatform.util;

import java.util.ArrayList;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.factory.DtoFactory;
import de.cinovo.surveyplatform.model.jsondto.SurveyDto;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.question.IMultipleOptionsQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class SurveyUtil {
	
	public static int calculateReturnRate(final SystemUser user, final Survey survey) {
		// create a temporary surveyDto, where the needed values are gathered
		// already
		SurveyDto surveyDto = DtoFactory.getInstance().createDto(user, survey);
		if (surveyDto.totalParticipants > 0) {
			return (int) (Math.round((((double) surveyDto.submittedQuestionnaires / surveyDto.totalParticipants) * 100)));
		}
		return 0;
	}
	
	public static int getNextParticipantNumber(Session hibSess, final ISurvey survey) {
		boolean usedAnOwnSession = false;
		Transaction tx = null;
		if (hibSess == null) {
			hibSess = HibernateUtil.getSessionFactory().openSession();
			usedAnOwnSession = true;
			tx = hibSess.beginTransaction();
		}
		
		Query query = hibSess.createQuery(Queries.GET_MAX_PARTICIPANT_NUMBER);
		query.setParameter("1", survey.getId());
		Object o = query.uniqueResult();
		if (usedAnOwnSession) {
			tx.commit();
			hibSess.close();
		}
		if (o instanceof Integer) {
			return ((Integer) o).intValue() + 1;
		}
		
		return 1;
	}
	
	public static String removeCriticalCharacters(final String input) {
		String stringWithoutUnicodeEntities = input //
				.replace("&#9;", " ") //
				.replace("&#32;", " ") //
				.replace("&#8220;", "\"") //
				.replace("&#8222;", "\"") //
				.replace("&#8221;", "\"") //
				.replace("&#8211;", "-") //
				.replaceAll("\\&\\#([0-8]+\\;)", "__AMP__$1") //
				.replace("&amp;lt;", "__AMP__lt;") //
				.replace("&amp;gt;", "__AMP__gt;") //
				.replace("&ndash;", "__AMP__ndash;") //
				.replace("&amp;", "__AMP__amp;") //
				.replace("&", "&amp;") //
				.replace("__AMP__", "&") //
				;
		// try {
		// FileWriter fw = new FileWriter(new File("out.html"));
		// fw.write(stringWithoutUnicodeEntities);
		// fw.flush();
		// fw.close();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		return stringWithoutUnicodeEntities;
	}
	
	public static String getEmailTextInvite(final ISurvey survey) {
		String emailText;
		if (survey.getEmailTextInvite() == null) {
			emailText = (TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/emailInvitationDefault.html", null, "\\{\\{", "\\}\\}"));
		} else {
			emailText = survey.getEmailTextInvite();
		}
		return emailText;
	}
	
	public static String getEmailTextRemind(final ISurvey survey) {
		String emailText;
		if (survey.getEmailTextRemind() == null) {
			emailText = (TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/emailReminderDefault.html", null, "\\{\\{", "\\}\\}"));
		} else {
			emailText = survey.getEmailTextRemind();
		}
		return emailText;
	}
	
	/**
	 * Resets all selected options and question answers of a questionnaire
	 * 
	 * @param questionnaire the quesionnaire
	 */
	public static void resetAnswers(final Questionnaire questionnaire) {
		for (Page p : questionnaire.getPages()) {
			for (Section s : p.getSections()) {
				for (AbstractQuestion q : s.getQuestions()) {
					
					if (q instanceof IMultipleOptionsQuestion) {
						for (Option o : q.getAllOptions()) {
							o.setSubmitted(false);
							o.setSelected(false);
						}
					} else if (q instanceof AverageNumberQuestion) {
						((AverageNumberQuestion) q).setAnswerObj(new Answer(""));
					} else if (q instanceof FreeTextQuestion) {
						((FreeTextQuestion) q).setAnswerObj(new Answer(""));
					} else if (q instanceof SingleLineQuestion) {
						((SingleLineQuestion) q).setAnswerObj(new Answer(""));
					} else if (q instanceof TextfieldQuestion) {
						((TextfieldQuestion) q).setAnswers(new ArrayList<Answer>());
					}
				}
			}
		}
		
	}
}
