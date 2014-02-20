package de.cinovo.surveyplatform.model.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.fileupload.util.Streams;

import de.cinovo.surveyplatform.constants.ChartType;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.ISurvey;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Participation;
import de.cinovo.surveyplatform.model.PersistentProperties;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.AverageNumberQuestion;
import de.cinovo.surveyplatform.model.question.ComboQuestion;
import de.cinovo.surveyplatform.model.question.FreeTextQuestion;
import de.cinovo.surveyplatform.model.question.IMatrixQuestion;
import de.cinovo.surveyplatform.model.question.IMultipleOptionsQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceMatrixQuestion;
import de.cinovo.surveyplatform.model.question.MultipleChoiceQuestion;
import de.cinovo.surveyplatform.model.question.Option;
import de.cinovo.surveyplatform.model.question.PhoneCallerHint;
import de.cinovo.surveyplatform.model.question.RadioMatrixQuestion;
import de.cinovo.surveyplatform.model.question.RadioQuestion;
import de.cinovo.surveyplatform.model.question.SingleLineQuestion;
import de.cinovo.surveyplatform.model.question.TextPart;
import de.cinovo.surveyplatform.model.question.TextfieldQuestion;
import de.cinovo.surveyplatform.ui.views.AbstractQuestionView;
import de.cinovo.surveyplatform.ui.views.AverageNumberQuestionView;
import de.cinovo.surveyplatform.ui.views.ComboQuestionView;
import de.cinovo.surveyplatform.ui.views.FreeTextQuestionView;
import de.cinovo.surveyplatform.ui.views.MultipleChoiceMatrixQuestionView;
import de.cinovo.surveyplatform.ui.views.MultipleChoiceQuestionView;
import de.cinovo.surveyplatform.ui.views.PhoneCallerHintView;
import de.cinovo.surveyplatform.ui.views.RadioMatrixQuestionView;
import de.cinovo.surveyplatform.ui.views.RadioQuestionView;
import de.cinovo.surveyplatform.ui.views.SingleLineQuestionView;
import de.cinovo.surveyplatform.ui.views.TextPartView;
import de.cinovo.surveyplatform.ui.views.TextfieldQuestionView;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.Logger;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * Multipurpose Factory for different model elements of a survey. This
 * class is implemented as Singleton.
 * 
 * @author yschubert
 * 
 */
public final class SurveyElementFactory {
	
	private static SurveyElementFactory instance;
	
	
	private SurveyElementFactory() {
		// singleton class
	}
	
	public static SurveyElementFactory getInstance() {
		if (instance == null) {
			instance = new SurveyElementFactory();
		}
		
		return instance;
	}
	
	public Section createSection(final String sectionTitle) {
		final Section section = new Section();
		section.setSyncId(UUID.randomUUID().toString());
		section.setSectionTitle(sectionTitle);
		return section;
	}
	
	public Page createPage() {
		
		Page page = new Page();
		page.setSyncId(UUID.randomUUID().toString());
		return page;
	}
	
	/**
	 * Creates a participant for the given survey.
	 * Also, if wanted, a Participation element is created and related to the
	 * survey and the participant.
	 * 
	 * @param survey Survey to which the participant shall be related to (set to
	 *            null, if the participant shall not be added)
	 * @param createParticipation If true, creates a participation element for
	 *            the participant and clones the questionnaire of the survey and
	 *            relates it to the participant. (this is ignored when survey ==
	 *            null)
	 * 
	 * @return Participant, related to the given survey
	 */
	public Participant createParticipant(final ISurvey survey, final boolean createParticipation) {
		final Participant participant = new Participant();
		participant.setSyncId(UUID.randomUUID().toString());
		participant.setName("-anonymous-");
		participant.setContactEmail("");
		participant.setContactPhone("");
		
		PersistentProperties pp = new PersistentProperties();
		pp.setSyncId(UUID.randomUUID().toString());
		Map<String, String> properties = new HashMap<String, String>();
		
		
		/** !!! when adding new properties here, also add them to PropertyViewUtil !!! **/
		// TODO externalize these properties!!
		properties.put("a01employerName", "");
		properties.put("a02employerPhone", "");
		properties.put("programmeTitle", "");
		properties.put("nqfLevel", "");
		properties.put("a18_1_18_2", "");
		properties.put("homeLanguage", "");
		properties.put("ethnicgroup", "");
		properties.put("gender", "");
		properties.put("age", "");
		properties.put("province", "");
		pp.setProperties(properties);
		participant.setProperties(pp);
		if (survey != null) {
			if (createParticipation) {
				Participation participation = new Participation();
				participation.setParticipant(participant);
				
				participation.setId(UUID.randomUUID().toString());
				
				// ** do not create questionnaire clone here: questionnaire can
				// still be edited
				// participation.setQuestionnaire(survey.getQuestionnaire().templateClone());
				participant.setParticipation(participation);
				participation.setParticipant(participant);
			}
			
			if (survey.getParticipants() == null) {
				survey.setParticipants(new ArrayList<Participant>());
			}
			survey.getParticipants().add(participant);
			participant.setSurvey((Survey) survey);
		}
		return participant;
	}
	
	public MultipleChoiceQuestion createMultipleChoiceQuestion(final String questionText) {
		MultipleChoiceQuestion multipleChoiceQuestion = (MultipleChoiceQuestion) setDefaults(new MultipleChoiceQuestion(), questionText);
		return multipleChoiceQuestion;
	}
	
	public MultipleChoiceMatrixQuestion createMultipleChoiceMatrixQuestion(final String questionText) {
		MultipleChoiceMatrixQuestion multipleChoiceMatrixQuestion = (MultipleChoiceMatrixQuestion) setDefaults(new MultipleChoiceMatrixQuestion(), questionText);
		return multipleChoiceMatrixQuestion;
	}
	
	public RadioQuestion createRadioQuestion(final String questionText) {
		RadioQuestion radioQuestion = (RadioQuestion) setDefaults(new RadioQuestion(), questionText);
		return radioQuestion;
	}
	
	public RadioMatrixQuestion createRadioMatrixQuestion(final String questionText) {
		RadioMatrixQuestion radioMatrixQuestion = (RadioMatrixQuestion) setDefaults(new RadioMatrixQuestion(), questionText);
		return radioMatrixQuestion;
	}
	
	public FreeTextQuestion createFreetTextQuestion(final String questionText) {
		FreeTextQuestion freeTextQuestion = (FreeTextQuestion) setDefaults(new FreeTextQuestion(), questionText);
		return freeTextQuestion;
	}
	
	public ComboQuestion createComboQuestion(final String questionText) {
		ComboQuestion comboQuestion = (ComboQuestion) setDefaults(new ComboQuestion(), questionText);
		return comboQuestion;
	}
	
	public SingleLineQuestion createSingleLineQuestion(final String questionText, final String hint) {
		SingleLineQuestion slq = (SingleLineQuestion) setDefaults(new SingleLineQuestion(), questionText);
		slq.setHint(hint);
		return slq;
	}
	
	public AverageNumberQuestion createAverageNumberQuestion(final String questionText, final String hint) {
		AverageNumberQuestion anq = (AverageNumberQuestion) setDefaults(new AverageNumberQuestion(), questionText);
		anq.setHint(hint);
		return anq;
	}
	
	public TextPart createTextPart(final String name, final String content) {
		final TextPart textPart = (TextPart) setDefaults(new TextPart(), name);
		textPart.setTextValue(content);
		return textPart;
	}
	
	public PhoneCallerHint createPhoneCallerHint(final String name, final String content) {
		final PhoneCallerHint phoneCallerHint = (PhoneCallerHint) setDefaults(new PhoneCallerHint(), name);
		phoneCallerHint.setTextValue(content);
		return phoneCallerHint;
	}
	
	public TextfieldQuestion createTextfieldQuestion(final String questionText) {
		TextfieldQuestion question = (TextfieldQuestion) setDefaults(new TextfieldQuestion(), questionText);
		return question;
	}
	private AbstractQuestion setDefaults(final AbstractQuestion question, final String questionText) {
		question.setQuestion(questionText);
		question.setChartType(ChartType.bar);
		question.setInteresting(true);
		question.setSyncId(UUID.randomUUID().toString());
		return question;
	}
	
	public Option createOption(final String displayName) {
		Option option = new Option(displayName);
		option.setSyncId(UUID.randomUUID().toString());
		return option;
	}
	
	public SystemUser createSystemUser(final String userName, final String email, final String password, final UserGroup group) {
		final SystemUser user = new SystemUser();
		user.setEmail(email);
		user.setUserName(userName.toLowerCase());
		user.setPassword(AuthUtil.scramblePassword(password));
		user.setCreationDate(new Date());
		user.setUserStatus(de.cinovo.surveyplatform.model.UserStatus.Active);
		GroupManager.addMember(group, user);
		return user;
	}
	
	
	
	public AbstractQuestionView createQuestionViewObject(final AbstractQuestion question) {
		if (question instanceof ComboQuestion) {
			return new ComboQuestionView((ComboQuestion) question);
		} else if (question instanceof SingleLineQuestion) {
			return new SingleLineQuestionView((SingleLineQuestion) question);
		} else if (question instanceof AverageNumberQuestion) {
			return new AverageNumberQuestionView((AverageNumberQuestion) question);
		} else if (question instanceof FreeTextQuestion) {
			return new FreeTextQuestionView((FreeTextQuestion) question);
		} else if (question instanceof MultipleChoiceQuestion) {
			return new MultipleChoiceQuestionView((MultipleChoiceQuestion) question);
		} else if (question instanceof MultipleChoiceMatrixQuestion) {
			return new MultipleChoiceMatrixQuestionView((MultipleChoiceMatrixQuestion) question);
		} else if (question instanceof RadioMatrixQuestion) {
			return new RadioMatrixQuestionView((RadioMatrixQuestion) question);
		} else if (question instanceof RadioQuestion) {
			return new RadioQuestionView((RadioQuestion) question);
		} else if (question instanceof TextfieldQuestion) {
			return new TextfieldQuestionView((TextfieldQuestion) question);
		} else if (question instanceof PhoneCallerHint) {
			return new PhoneCallerHintView((PhoneCallerHint) question);
		} else if (question instanceof TextPart) {
			return new TextPartView((TextPart) question);
		}
		return null;
	}
	
	// public SurveyDemo createSurveyDemo(final Survey survey, final String
	// sessionId, final Session hibSess) {
	//
	// // make a clone of the survey
	// SurveyDemo surveyDemo = new SurveyDemo();
	// surveyDemo.setSessionId(sessionId);
	// if (survey.getClosedAtDate() != null) {
	// surveyDemo.setClosedAtDate(new Date(survey.getClosedAtDate().getTime()));
	// }
	// if (survey.getCreationDate() != null) {
	// surveyDemo.setCreationDate(new Date(survey.getCreationDate().getTime()));
	// }
	// if (survey.getRunningSinceDate() != null) {
	// surveyDemo.setRunningSinceDate(new
	// Date(survey.getRunningSinceDate().getTime()));
	// }
	// surveyDemo.setCreator(survey.getCreator());
	// surveyDemo.setDeleted(survey.isDeleted());
	// surveyDemo.setDescription(survey.getDescription());
	// surveyDemo.setEmailTextInvite(survey.getEmailTextInvite());
	// surveyDemo.setEmailTextRemind(survey.getEmailTextRemind());
	// surveyDemo.setName(survey.getName());
	// surveyDemo.setPublicSurvey(survey.isPublicSurvey());
	// // clone the questionnaire
	// if (survey.getQuestionnaire() != null) {
	// surveyDemo.setQuestionnaire(survey.getQuestionnaire().clone());
	// }
	// surveyDemo.setState(survey.getState());
	//
	// // save to fill entities with IDs
	// hibSess.save(surveyDemo);
	// correctIds(surveyDemo);
	//
	// // create participations if neccessary
	// boolean createParticipation = survey.getState().ordinal() >=
	// SurveyState.CREATED.ordinal();
	//
	// if (survey.getParticipants() != null) {
	// for (Participant p : survey.getParticipants()) {
	// final Participant participant =
	// SurveyElementFactory.getInstance().createParticipant(surveyDemo,
	// createParticipation);
	// Participation origParticipation = p.getParticipation();
	// if (origParticipation != null) {
	//
	// if (origParticipation.isSubmitted()) {
	//
	// Participation newParticipation = participant.getParticipation();
	// Questionnaire questionnaire =
	// origParticipation.getQuestionnaire().clone();
	// newParticipation.setQuestionnaire(questionnaire);
	// questionnaire.setParticipation(newParticipation);
	// // copy the selections and answers of the participant
	// newParticipation.setSubmitted(true);
	// newParticipation.setSubmittedBy(origParticipation.getSubmittedBy());
	//
	// for (final Page page : origParticipation.getQuestionnaire().getPages()) {
	// for (final Section section : page.getSections()) {
	// for (final AbstractQuestion question : section.getQuestions()) {
	// final AbstractQuestion newQuestion = findQuestion(questionnaire,
	// page.getLocalId(), section.getLocalId(), question.getLocalId());
	// if (newQuestion != null) {
	// newQuestion.setSubmitted(true);
	// if (newQuestion instanceof TextfieldQuestion) {
	// for (Answer answer : ((TextfieldQuestion) question).getAnswers()) {
	// ((TextfieldQuestion) newQuestion).getAnswers().add(answer.clone());
	// }
	// } else if (newQuestion instanceof FreeTextQuestion) {
	// ((FreeTextQuestion) newQuestion).setTextValue(((FreeTextQuestion)
	// question).getTextValue());
	// } else if (newQuestion instanceof SingleLineQuestion) {
	// ((SingleLineQuestion) newQuestion).setTextValue(((SingleLineQuestion)
	// question).getTextValue());
	// }
	// int optionNr = 0;
	// List<Option> newQuestionOptions = newQuestion.getAllOptions();
	// if (newQuestionOptions.size() != question.getAllOptions().size()) {
	// Logger.warn("Cloned question is messed up! Might be a strange behavior!");
	// } else {
	// for (final Option option : question.getAllOptions()) {
	// final Option newOption = newQuestionOptions.get(optionNr);
	// newOption.setSelected(option.isSelected());
	// newOption.setSubmitted(option.isSubmitted());
	// optionNr++;
	// }
	// }
	// correctOptionIds(newQuestion, questionnaire.getId());
	// }
	// }
	// }
	// }
	//
	// }
	// participant.setNumber(p.getNumber());
	// participant.setContactEmail(p.getContactEmail());
	// participant.setName(p.getName());
	// participant.setContactPhone(p.getContactPhone());
	// participant.setSurname(p.getSurname());
	// participant.setAskByPhone(p.isAskByPhone());
	// participant.setEmailInQueue(p.isEmailInQueue());
	// if (p.getInvitationSent() != null) {
	// participant.setInvitationSent(new Date(p.getInvitationSent().getTime()));
	// }
	// if (p.getReminderSent() != null) {
	// participant.setReminderSent(new Date(p.getReminderSent().getTime()));
	// }
	// if (p.getSurveySubmitted() != null) {
	// participant.setSurveySubmitted(new
	// Date(p.getSurveySubmitted().getTime()));
	// }
	// if (p.getProperties() != null) {
	// for (Entry<String, String> entry :
	// p.getProperties().getProperties().entrySet()) {
	// participant.getProperties().setProperty(entry.getKey(),
	// entry.getValue());
	// }
	// }
	// }
	// }
	// }
	// return surveyDemo;
	// }
	//
	// private AbstractQuestion findQuestion(final Questionnaire questionnaire,
	// final int pageLocalId, final int sectionLocalId, final int
	// questionLocalId) {
	// AbstractQuestion result = null;
	// for (Page page : questionnaire.getPages()) {
	// if (page.getLocalId() == pageLocalId) {
	// for (Section section : page.getSections()) {
	// if (section.getLocalId() == sectionLocalId) {
	// for (AbstractQuestion question : section.getQuestions()) {
	// if (question.getLocalId() == questionLocalId) {
	// return question;
	// }
	// }
	// }
	// }
	// }
	// }
	// return result;
	// }
	
	public void copySurveyMetaData(final ISurvey originSurvey, final ISurvey survey) {
		
		File reportsDir = new File(Paths.REPORTS + "/" + survey.getId());
		reportsDir.mkdirs();
		if (originSurvey != null) {
			
			File surveyLogo = new File(Paths.SURVEYLOGOS + "/" + originSurvey.getId() + ".jpg");
			if (surveyLogo.exists()) {
				try {
					Streams.copy(new FileInputStream(surveyLogo), new FileOutputStream(new File(Paths.SURVEYLOGOS + "/" + survey.getId() + ".jpg")), true);
				} catch (Exception e) {
					Logger.err("Could not copy the survey meta data.", e);
				}
			}
			
			// copy xml files
			File originReportsDir = new File(Paths.REPORTS + "/" + originSurvey.getId());
			if (!originReportsDir.exists()) {
				originReportsDir.mkdirs();
			}
			File[] xmlFiles = originReportsDir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(final File dir, final String name) {
					return name.toLowerCase().endsWith(".xml");
				}
			});
			for (File xmlFile : xmlFiles) {
				try {
					Streams.copy(new FileInputStream(xmlFile), new FileOutputStream(new File(reportsDir + "/" + xmlFile.getName())), true);
				} catch (Exception e) {
					Logger.err("Could not copy the survey meta data.", e);
				}
			}
		}
	}
	
	/**
	 * Options have the id of the origin question to improve performance on
	 * analysing reports. This method will set the origin ID of each option of a
	 * {@link IMultipleOptionsQuestion} Question. Also the questionnaire Id and
	 * the survey Ids are propagated to the childs
	 * 
	 * @param survey survey in which the ids of all
	 *            questions and options shall be set
	 */
	public void correctIds(final Survey survey) {
		Questionnaire questionnaire = survey.getQuestionnaire();
		for (Page page : questionnaire.getPages()) {
			if (page.getLocalId() == 0) {
				page.setLocalId(page.getId());
			}
			for (Section section : page.getSections()) {
				if (section.getLocalId() == 0) {
					section.setLocalId(section.getId());
				}
				if (section.getQuestions() != null) {
					for (AbstractQuestion question : section.getQuestions()) {
						if (question.getLocalId() == 0) {
							question.setLocalId(question.getId());
						}
						question.setQuestionnaireID(0);
						correctOptionIds(question, 0);
					}
				}
			}
		}
	}
	
	/**
	 * Options have the id of the origin question to improve performance on
	 * analysing reports. This method will set the origin ID of each option of a
	 * {@link IMultipleOptionsQuestion} Question. Also the questionnaire Id and
	 * the survey Ids are propagated to the childs
	 * 
	 * @param questionnaire questionnaire in which the ids of all
	 *            questions and options shall be set
	 */
	public void correctIds(final Questionnaire questionnaire, final int questionnaireID) {
		for (Page page : questionnaire.getPages()) {
			if (page.getLocalId() == 0) {
				page.setLocalId(page.getId());
			}
			for (Section section : page.getSections()) {
				if (section.getLocalId() == 0) {
					section.setLocalId(section.getId());
				}
				if (section.getQuestions() != null) {
					for (AbstractQuestion question : section.getQuestions()) {
						if (question.getLocalId() == 0) {
							question.setLocalId(question.getId());
						}
						question.setQuestionnaireID(questionnaireID);
						correctOptionIds(question, questionnaireID);
					}
				}
			}
		}
	}
	
	/**
	 * Options have the id of the origin question to improve performance on
	 * analysing reports. This method will set the origin ID of each option of a
	 * {@link IMultipleOptionsQuestion} Question.
	 * 
	 * @param question Question to set the ids
	 * @param survey Survey to set
	 */
	public void correctOptionIds(final AbstractQuestion question, final int questionnaireID) {
		if (question instanceof IMatrixQuestion) {
			List<AbstractQuestion> subquestions = question.getSubquestions();
			for (AbstractQuestion subQuestion : subquestions) {
				correctOptionIds(subQuestion, questionnaireID);
			}
		} else if (question instanceof IMultipleOptionsQuestion) {
			for (Option option : question.getOptions()) {
				option.setOriginQuestionId(question.getId());
				option.setQuestionnaireID(questionnaireID);
			}
		}
	}
	
}
