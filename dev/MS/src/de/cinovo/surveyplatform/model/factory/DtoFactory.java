package de.cinovo.surveyplatform.model.factory;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.Page;
import de.cinovo.surveyplatform.model.Participant;
import de.cinovo.surveyplatform.model.Questionnaire;
import de.cinovo.surveyplatform.model.QuestionnaireLogicElement;
import de.cinovo.surveyplatform.model.Section;
import de.cinovo.surveyplatform.model.Survey;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.Topic;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.model.UserRight;
import de.cinovo.surveyplatform.model.jsondto.ClientDto;
import de.cinovo.surveyplatform.model.jsondto.PageDto;
import de.cinovo.surveyplatform.model.jsondto.ParticipantDto;
import de.cinovo.surveyplatform.model.jsondto.QuestionDto;
import de.cinovo.surveyplatform.model.jsondto.QuestionnaireLogicElementDto;
import de.cinovo.surveyplatform.model.jsondto.SectionDto;
import de.cinovo.surveyplatform.model.jsondto.SurveyDto;
import de.cinovo.surveyplatform.model.jsondto.SystemUserDto;
import de.cinovo.surveyplatform.model.jsondto.TopicDto;
import de.cinovo.surveyplatform.model.jsondto.UserGroupDto;
import de.cinovo.surveyplatform.model.question.AbstractQuestion;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.servlets.dal.SurveyDal;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.QuestionnaireViewUtil;
import de.cinovo.surveyplatform.util.TimeUtil;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * Transforms Objects into their DTO representation if possible
 * Implemented as singleton, so use <code>DtoFactory.getInstance()</code> to
 * access.
 * 
 * @author yschubert
 * 
 */
public final class DtoFactory {
	
	private static DtoFactory instance;
	
	
	/**
	 * @return the factory
	 */
	public static DtoFactory getInstance() {
		if (instance == null) {
			instance = new DtoFactory();
		}
		return instance;
	}
	
	private DtoFactory() {
		// singleton class
	}
	
	/**
	 * @param page -
	 * @return the dto
	 */
	public PageDto createDto(final Page page) {
		final PageDto dto = new PageDto();
		dto.id = page.getId();
		return dto;
	}
	
	/**
	 * @param section -
	 * @return the dto
	 */
	public SectionDto createDto(final Section section) {
		final SectionDto dto = new SectionDto();
		dto.id = section.getId();
		dto.title = section.getSectionTitle();
		return dto;
	}
	
	/**
	 * @param question -
	 * @return the dto
	 */
	public QuestionDto createDto(final AbstractQuestion question) {
		final QuestionDto dto = new QuestionDto();
		dto.id = question.getId();
		dto.htmlRepresentation = QuestionnaireViewUtil.getQuestionHTMLRepresentation(question, true, true);
		return dto;
	}
	
	/**
	 * @param user -
	 * @param survey -
	 * @return the dto
	 */
	public SurveyDto createDto(final SystemUser user, final Survey survey) {
		final SurveyDto dto = new SurveyDto();
		dto.id = survey.getId();
		dto.description = survey.getDescription();
		dto.name = survey.getName();
		Questionnaire questionnaire = survey.getQuestionnaire();
		UserGroup owner = survey.getOwner();
		if (questionnaire != null) {
			dto.questionnaireId = questionnaire.getId();
		}
		SystemUser creator = survey.getCreator();
		if (creator != null) {
			dto.creator = creator.getActualUserName();
		}
		if (owner != null) {
			if (AuthUtil.hasRight(user, UserRights.ADMINISTRATOR)) {
				dto.owner = owner.getClient().getOrganization() + " - " + owner.getName();
			} else {
				dto.owner = owner.getName();
			}
		}
		
		dto.state = survey.getStateDisplayname();
		dto.eMailSender = survey.getEmailSender();
		dto.senderName = survey.getSenderName();
		
		if (survey.getParticipants() != null) {
			dto.totalParticipants = survey.getParticipants().size();
			int submittedCount = 0;
			for (Participant p : survey.getParticipants()) {
				if (p.getSurveySubmitted() != null) {
					submittedCount++;
				}
			}
			dto.submittedQuestionnaires = submittedCount;
		}
		
		if (survey.getClosedAtDate() == null) {
			dto.closingDate = "";
		} else {
			dto.closingDate = TimeUtil.getLocalTime(user, survey.getClosedAtDate());
		}
		
		if (survey.getRunningSinceDate() == null) {
			dto.runningDate = "";
		} else {
			dto.runningDate = TimeUtil.getLocalTime(user, survey.getRunningSinceDate());
		}
		
		if (survey.getCreationDate() == null) {
			dto.creationDate = "";
		} else {
			dto.creationDate = TimeUtil.getLocalTime(user, survey.getCreationDate());
		}
		
		return dto;
	}
	
	/**
	 * @param user -
	 * @param participant -
	 * @return the dto
	 */
	public ParticipantDto createDto(final SystemUser user, final Participant participant) {
		final ParticipantDto dto = new ParticipantDto();
		dto.id = participant.getId();
		dto.number = participant.getNumber();
		
		dto.email = participant.getContactEmail();
		dto.name = participant.getName();
		dto.surname = participant.getSurname();
		
		if (participant.getParticipation() == null) {
			dto.participationId = "";
		} else {
			dto.participationId = participant.getParticipation().getId();
		}
		dto.phone = participant.getContactPhone();
		if (participant.getInvitationSent() == null) {
			dto.invitedDateString = "";
		} else {
			dto.invitedDateString = TimeUtil.getLocalTime(user, participant.getInvitationSent());
		}
		if (participant.getReminderSent() == null) {
			dto.remindedDateString = "";
		} else {
			dto.remindedDateString = TimeUtil.getLocalTime(user, participant.getReminderSent());
		}
		if (participant.getSurveySubmitted() == null) {
			dto.submittedDateString = "";
		} else {
			dto.submittedDateString = TimeUtil.getLocalTime(user, participant.getSurveySubmitted());
		}
		
		if (participant.getProperties() != null) {
			dto.properties = participant.getProperties().getProperties();
		}
		
		return dto;
	}
	
	/**
	 * @param client -
	 * @return the dto
	 */
	public ClientDto createDto(final Client client) {
		ClientDto dto = new ClientDto();
		dto.paymentmodel = client.getPaymentModel().name();
		dto.organization = client.getOrganization();
		dto.account = String.valueOf(client.getPaymentModel());
		dto.status = String.valueOf(client.getClientStatus());
		return dto;
	}
	
	/**
	 * @param currentUser -
	 * @param systemUser -
	 * @return the dto
	 */
	public SystemUserDto createDto(final SystemUser currentUser, final SystemUser systemUser) {
		final SystemUserDto dto = new SystemUserDto();
		dto.address = systemUser.getAddress();
		dto.creationDate = TimeUtil.getLocalTime(currentUser, systemUser.getCreationDate());
		dto.email = systemUser.getEmail();
		dto.firstName = systemUser.getFirstName();
		dto.lastName = systemUser.getLastName();
		dto.phone = systemUser.getPhone();
		dto.title = systemUser.getTitle();
		dto.userName = systemUser.getUserName();
		dto.userStatus = String.valueOf(systemUser.getUserStatus());
		dto.timeZone = systemUser.getTimeZoneID();
		dto.dateFormat = systemUser.getTimeFormat().replace("'", "");
		Set<UserRight> userRights = systemUser.getUserRights();
		if (userRights == null) {
			dto.userRights = new String[0];
		} else {
			dto.userRights = new String[userRights.size()];
			int i = 0;
			Iterator<UserRight> iterator = userRights.iterator();
			while (iterator.hasNext()) {
				dto.userRights[i++] = iterator.next().getName();
			}
		}
		Set<UserGroup> userGroups = systemUser.getUserGroups();
		if (userGroups == null) {
			dto.userGroups = new String[0];
		} else {
			dto.userGroups = new String[userGroups.size()];
			int i = 0;
			Iterator<UserGroup> iterator = userGroups.iterator();
			while (iterator.hasNext()) {
				dto.userGroups[i++] = iterator.next().getName();
			}
		}
		return dto;
	}
	
	/**
	 * @param logicElements -
	 * @return the dto
	 */
	public QuestionnaireLogicElementDto[] createDto(final List<QuestionnaireLogicElement> logicElements) {
		final QuestionnaireLogicElementDto[] dtoArray = new QuestionnaireLogicElementDto[logicElements.size()];
		for (int i = 0; i < logicElements.size(); i++) {
			QuestionnaireLogicElement logicElement = logicElements.get(i);
			final QuestionnaireLogicElementDto dto = new QuestionnaireLogicElementDto();
			dto.id = logicElement.getId();
			dto.answers = "";
			dto.surveyId = logicElement.getSurveyId();
			final List<Answer> answers = logicElement.getAnswers();
			int size = answers.size();
			for (int j = 0; j < size; j++) {
				dto.answers += answers.get(j).getAnswer();
				if (j < (size - 1)) {
					dto.answers += ", ";
				}
			}
			dto.idOfPart = logicElement.getIdOfPart();
			dto.operator = logicElement.getOperator().name();
			dto.questionIdWithLogic = logicElement.getQuestionIdWithLogic();
			dto.typeOfPart = SurveyDal.PARTNAME_QUESTION;
			if (logicElement.getTypeOfPart().equals(AbstractQuestion.class.getName().toLowerCase())) {
				dto.typeOfPart = SurveyDal.PARTNAME_QUESTION;
			} else if (logicElement.getTypeOfPart().equals(Section.class.getName().toLowerCase())) {
				dto.typeOfPart = SurveyDal.PARTNAME_SECTION;
			} else if (logicElement.getTypeOfPart().equals(Page.class.getName().toLowerCase())) {
				dto.typeOfPart = SurveyDal.PARTNAME_PAGE;
			}
			dtoArray[i] = dto;
		}
		
		return dtoArray;
	}
	
	/**
	 * @param topic -
	 * @return the dto
	 */
	public TopicDto createDto(final Topic topic) {
		TopicDto dto = new TopicDto();
		dto.id = topic.getId();
		dto.title = topic.getTitle();
		List<Answer> answers = topic.getAnswers();
		int size = answers.size();
		String[] answsersArray = new String[size];
		for (int i = 0; i < size; i++) {
			answsersArray[i] = answers.get(i).getAnswer();
		}
		dto.answers = answsersArray;
		return dto;
	}
	
	/**
	 * @param group -
	 * @return the dto
	 */
	public UserGroupDto createDto(final UserGroup group) {
		UserGroupDto dto = new UserGroupDto();
		dto.id = group.getId();
		dto.name = group.getName();
		return dto;
	}
}
