/**
 * 
 */
package de.cinovo.surveyplatform.bootstrap.fixture;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.Queries;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.IFreeTextQuestion;
import de.cinovo.surveyplatform.model.ILogicApplicableQuestion;
import de.cinovo.surveyplatform.model.ISurvey.SurveyState;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Participation;
import de.cinovo.surveyplatform.model.Project;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemSettings;
import de.cinovo.surveyplatform.model.Topic;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.model.question.TopicCombination;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class DatabaseMigration {
	
	/**
	 * @param s -
	 * @throws Exception -
	 */
	@SuppressWarnings("null")
	public static boolean migrate(final Session s) throws Exception {
		
		SystemSettings settings = SystemSettings.load();
		Integer dbVersion = settings.getDbVersion();
		int newDBVersion = dbVersion;
		if ((dbVersion == null) || (dbVersion == 0)) {
			s.createSQLQuery("UPDATE survey.page SET previewModeVisible=true;").executeUpdate();
			newDBVersion++;
		}
		
		if (dbVersion <= 1) {
			System.out.println("DB UPDATE: Creating random sync UUIDs for all questionnaire parts");
			s.createSQLQuery("UPDATE survey.AbstractQuestion set syncId=RANDOM_UUID()").executeUpdate();
			s.createSQLQuery("UPDATE survey.Option set syncId=RANDOM_UUID()").executeUpdate();
			s.createSQLQuery("UPDATE survey.Page set syncId=RANDOM_UUID()").executeUpdate();
			s.createSQLQuery("UPDATE survey.Participant set syncId=RANDOM_UUID()").executeUpdate();
			s.createSQLQuery("UPDATE survey.PersistentProperties set syncId=RANDOM_UUID()").executeUpdate();
			s.createSQLQuery("UPDATE survey.Questionnaire set syncId=RANDOM_UUID()").executeUpdate();
			s.createSQLQuery("UPDATE survey.Section set syncId=RANDOM_UUID()").executeUpdate();
			s.createSQLQuery("UPDATE survey.Survey set syncId=RANDOM_UUID()").executeUpdate();
			newDBVersion++;
		}
		if (dbVersion <= 2) {
			System.out.println("DB UPDATE: Setting the initial ordernumbers of all options");
			Criteria l = s.createCriteria(AbstractQuestion.class);
			List<?> list = l.list();
			for (Object obj : list) {
				int orderNumber = 0;
				for (Option o : ((AbstractQuestion) obj).getOptions()) {
					Session s2 = HibernateUtil.getSessionFactory().openSession();
					Transaction t = s2.beginTransaction();
					Option opt = (Option) s2.load(Option.class, o.getId());
					opt.setOrderNumber(++orderNumber);
					s2.saveOrUpdate(opt);
					t.commit();
					s2.close();
				}
			}
			
			newDBVersion++;
		}
		
		if (dbVersion <= 3) {
			// System.out.println("DB UPDATE: Setting the inital combination IDs for Type Answer");
			// s.createSQLQuery("UPDATE survey.Answer set combinationId=RANDOM_UUID()").executeUpdate();
			newDBVersion++;
		}
		
		if (dbVersion <= 4) {
			System.out.println("DB UPDATE: Setting values of textValue (Question) to Answer (Question).");
			{
				System.err.println(" - disabled - ");
				// Criteria cr = s.createCriteria(FreeTextQuestion.class);
				// List<?> list = cr.list();
				// for (FreeTextQuestion q : ((List<FreeTextQuestion>) list)) {
				//
				// Answer ans = new Answer();
				// ans.setAnswer(q.getTextValue());
				// q.setAnswerObj(ans);
				// s.save(q);
				// }
			}
			
			newDBVersion++;
		}
		
		if (dbVersion <= 5) {
			
			// {
			// Criteria cr = s.createCriteria(SingleLineQuestion.class);
			// List<?> list = cr.list();
			// for (SingleLineQuestion q : ((List<SingleLineQuestion>) list)) {
			//
			// Answer ans = new Answer();
			// ans.setAnswer(q.getTextValue());
			// q.setAnswerObj(ans);
			// s.save(q);
			// }
			// }
			
			Criteria cr = s.createCriteria(Participation.class);
			List<?> list = cr.list();
			int count = list.size();
			System.out.println("Checking " + count + " participations for correct topic assignments");
			for (Object o : list) {
				System.out.println("                          " + (count--) + " participations to go...");
				Participation p = (Participation) o;
				if (p.getQuestionnaire() != null) {
					for (Page page : p.getQuestionnaire().getPages()) {
						for (Section section : page.getSections()) {
							for (AbstractQuestion q : section.getQuestions()) {
								
								Criteria topicsCriteria = s.createCriteria(Topic.class);
								topicsCriteria.add(Restrictions.eq("refQuestionId", q.getOriginQuestionId()));
								List<?> topics = topicsCriteria.list();
								
								for (Object topicObj : topics) {
									Topic t = (Topic) topicObj;
									
									List<Integer> toRemove = new ArrayList<Integer>();
									int index = 0;
									for (Answer topicAnswer : t.getAnswers()) {
										if (topicAnswer.equals("")) {
											// do not allow empty topic answers!
											toRemove.add(index);
										} else {
											for (Answer questionAnswer : q.getAnswer()) {
												// System.out.println(questionAnswer.getAnswer() + " <=> " + topicAnswer.getAnswer());
												if (questionAnswer.getAnswer().replace("\n", "").replace("\r", "").equals(topicAnswer.getAnswer().replace("\n", "").replace("\r", ""))) {
													if (topicAnswer.getTopicCombinations() == null) {
														topicAnswer.setTopicCombinations(new ArrayList<TopicCombination>());
													}
													// System.out.println(topicAnswer.getId());
													TopicCombination tc = new TopicCombination(questionAnswer.getId(), -1, -1);
													topicAnswer.getTopicCombinations().add(tc);
												} else {
													// System.out.println(".");
												}
											}
										}
										index++;
									}
									for (Integer i : toRemove) {
										t.getAnswers().remove((int) i);
									}
									
								}
								
							}
						}
					}
				}
				
			}
			newDBVersion++;
		}
		
		if (dbVersion <= 6) {
			System.out.println("Settings the length of the additionalInfo field in the abstractquestion table");
			s.createSQLQuery("ALTER TABLE survey.abstractquestion ALTER COLUMN additionalInfo TYPE text;").executeUpdate();
			newDBVersion++;
		}
		
		if (dbVersion <= 7) {
			System.out.println("Correcting the local IDs of the questions");
			
			Criteria cr = s.createCriteria(Survey.class);
			cr.add(Restrictions.le("state", SurveyState.TEMPLATE));
			List<?> list = cr.list();
			for (Object o : list) {
				Survey survey = (Survey) o;
				List<Answer> toRemove = new ArrayList<Answer>();
				for (Page page : survey.getQuestionnaire().getPages()) {
					for (Section section : page.getSections()) {
						for (AbstractQuestion q : section.getQuestions()) {
							List<Answer> answers = q.getAnswer();
							List<Answer> toRemoveFromQuestion = new ArrayList<Answer>();
							for (Answer a : answers) {
								String answer = a.getAnswer();
								if (answer != null) {
									if (answer.isEmpty()) {
										if (q instanceof IFreeTextQuestion) {
											((IFreeTextQuestion) q).setAnswerObj(null);
										} else if (q instanceof TextfieldQuestion) {
											toRemoveFromQuestion.add(a);
										}
										toRemove.add(a);
									}
								}
							}
							for (Answer a : toRemoveFromQuestion) {
								q.getAnswer().remove(a);
							}
						}
					}
				}
				for (Answer a : toRemove) {
					s.delete(a);
				}
			}
			
			newDBVersion++;
		}
		
		if (dbVersion <= 8) {
			System.out.println("Correcting the local IDs of the questions");
			
			Criteria cr = s.createCriteria(Survey.class);
			cr.add(Restrictions.le("state", SurveyState.TEMPLATE));
			List<?> list = cr.list();
			for (Object o : list) {
				Survey survey = (Survey) o;
				List<Answer> toRemove = new ArrayList<Answer>();
				for (Page page : survey.getQuestionnaire().getPages()) {
					for (Section section : page.getSections()) {
						for (AbstractQuestion q : section.getQuestions()) {
							List<Answer> answers = q.getAnswer();
							List<Answer> toRemoveFromQuestion = new ArrayList<Answer>();
							for (Answer a : answers) {
								String answer = a.getAnswer();
								if (answer == null) {
									if (q instanceof IFreeTextQuestion) {
										((IFreeTextQuestion) q).setAnswerObj(null);
									} else if (q instanceof TextfieldQuestion) {
										toRemoveFromQuestion.add(a);
									}
									toRemove.add(a);
								}
							}
							for (Answer a : toRemoveFromQuestion) {
								q.getAnswer().remove(a);
							}
						}
					}
				}
				for (Answer a : toRemove) {
					s.delete(a);
				}
			}
			
			newDBVersion++;
		}
		
		if (dbVersion <= 9) {
			if (!EnvironmentConfiguration.isTestEnvironment()) {
				System.out.println("Correcting the average Number Question AnswerObjects for old AverageNumberQuestions");
				List<?> list = s.createSQLQuery("SELECT textvalue,id FROM survey.AVERAGENUMBERQUESTION").list();
				for (Object o : list) {
					Object[] oArray = (Object[]) o;
					Clob cl = (Clob) (oArray)[0];
					int questionId = (Integer) (oArray)[1];
					if (cl != null) {
						try {
							// the length will be cut in perfectly for the existing String characters
							String clobString = cl.getSubString(1, Integer.MAX_VALUE);
							if (!clobString.equals("")) {
								Answer answ = new Answer(clobString);
								s.save(answ);
								s.createSQLQuery("UPDATE survey.AVERAGENUMBERQUESTION set answerobj_id=" + answ.getId() + " WHERE id='" + questionId + "' AND answerobj_id IS NULL").executeUpdate();
							}
							
						} catch (SQLException sqe) {
							sqe.printStackTrace();
						}
					}
				}
			}
			newDBVersion++;
		}
		if (dbVersion <= 10) {
			System.out.println("Connecting Logicelements with surveys...");
			{
				Criteria cr = s.createCriteria(Survey.class);
				List<?> list = cr.list();
				for (Object o : list) {
					Survey survey = (Survey) o;
					for (Page page : survey.getQuestionnaire().getPages()) {
						for (Section section : page.getSections()) {
							for (AbstractQuestion q : section.getQuestions()) {
								if (q instanceof ILogicApplicableQuestion) {
									List<QuestionnaireLogicElement> logicElements = q.getLogicElements();
									for (QuestionnaireLogicElement le : logicElements) {
										le.setSurveyId(survey.getId());
									}
								}
							}
						}
					}
					for (Participant p : survey.getParticipants()) {
						if ((p.getParticipation() != null) && (p.getParticipation().getQuestionnaire() != null)) {
							for (Page page : p.getParticipation().getQuestionnaire().getPages()) {
								for (Section section : page.getSections()) {
									for (AbstractQuestion q : section.getQuestions()) {
										if (q instanceof ILogicApplicableQuestion) {
											List<QuestionnaireLogicElement> logicElements = q.getLogicElements();
											for (QuestionnaireLogicElement le : logicElements) {
												le.setSurveyId(survey.getId());
											}
										}
									}
								}
							}
							
						}
					}
				}
			}
			newDBVersion++;
		}
		
		if (dbVersion <= 11) {
			// update old Database Token_Id (integer) to the actual new Token_Id (String)
			System.out.println("Fixing old DataBase Column Token_Id (Integer) to new Token_Id (String)...");
			
			try {
				s.createSQLQuery("DROP TABLE survey.Token").executeUpdate();
			} catch (Exception ex) {
				// ignore
			}
			
			// TODO IMPORTANT: Tell Developer to RESTART the Server, since hibernate has to Re-Create the survey.TOKEN table!
			System.out.println("TOKEN_Table updated. PLEASE RESTART THE PLATFORM NOW, DO NOT USE IT BEFORE RE-START!");
			newDBVersion++;
			System.out.println("Migrated database to Version: " + newDBVersion);
			s.createSQLQuery("UPDATE survey.systemsettings SET dbVersion=" + newDBVersion + ";").executeUpdate();
			return false;
		}
		
		if (dbVersion <= 12) {
			System.out.println("Creating Projects for the clients...");
			Criteria criteria = s.createCriteria(Client.class);
			List<?> list = criteria.list();
			for (Object o : list) {
				Client c = (Client) o;
				Project p = new Project();
				p.setName("My Survey Project");
				p.setOwner(c);
				s.save(p);
				Query query = s.createQuery(Queries.SURVEYS_BY_CLIENT);
				query.setParameter("1", c.getId());
				List<?> surveys = query.list();
				
				for (Object obj : surveys) {
					Survey survey = (Survey) obj;
					survey.setProject(p);
					s.save(survey);
				}
				s.save(p);
				
			}
			newDBVersion++;
		}
		
		// USE THIS TEMPLATE FOR THE NEXT QUERY
		// if ( dbVersion <= 13) {
		// s.createSQLQuery(QUERY_GOES_HERE).executeUpdate();
		// newDBVersion++;
		// }
		
		if (dbVersion != newDBVersion) {
			System.out.println("Migrated database to Version: " + newDBVersion);
			s.createSQLQuery("UPDATE survey.systemsettings SET dbVersion=" + newDBVersion + ";").executeUpdate();
		}
		return true;
	}
}

