package de.cinovo.surveyplatform.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

import de.cinovo.surveyplatform.sync.Sync;
import de.cinovo.surveyplatform.sync.SyncFilter;
import de.cinovo.surveyplatform.sync.SyncIdentifiable;
import de.cinovo.surveyplatform.util.Logger;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
@Entity
@XmlRootElement
public class Questionnaire implements Cloneable, ITemplateCloneable, SyncIdentifiable<String> {
	
	private int id;
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private List<Page> pages = new ArrayList<Page>();
	
	private Participation participation;
	
	private String syncId;
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return this.id;
	}
	
	@XmlID
	@Transient
	public String getXMLID() {
		return this.getClass().getSimpleName() + this.getId();
	}
	
	private void setId(final int id) {
		this.id = id;
	}
	
	/**
	 * @return the participation
	 */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
	@XmlIDREF
	public Participation getParticipation() {
		return this.participation;
	}
	
	/**
	 * @param participation the participation to set
	 */
	public void setParticipation(final Participation participation) {
		this.participation = participation;
	}
	
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "questionnaire", fetch = FetchType.LAZY)
	@OrderBy("orderNumber")
	public List<Page> getPages() {
		return this.pages;
	}
	
	public void setPages(final List<Page> pages) {
		this.pages = pages;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.cinovo.surveyplatform.sync.SyncIdentifiable#getSyncId()
	 */
	@Override
	public String getSyncId() {
		return this.syncId;
	}
	
	/**
	 * @param syncId the syncId to set
	 */
	public void setSyncId(final String syncId) {
		this.syncId = syncId;
	}
	
	@Override
	public Questionnaire clone() {
		Questionnaire newQuestionnaire = null;
		try {
			newQuestionnaire = (Questionnaire) super.clone();
			newQuestionnaire.setId(0);
			newQuestionnaire.setPages(new ArrayList<Page>());
			if (this.pages != null) {
				Page[] pageArray = this.pages.toArray(new Page[this.pages.size()]);
				for (Page page : pageArray) {
					Page newPage = page.clone();
					newQuestionnaire.pages.add(newPage);
					newPage.setQuestionnaire(newQuestionnaire);
				}
			}
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return newQuestionnaire;
	}
	
	@Override
	public Questionnaire templateClone() {
		Questionnaire newQuestionnaire = null;
		try {
			newQuestionnaire = (Questionnaire) super.clone();
			newQuestionnaire.setId(0);
			newQuestionnaire.setPages(new ArrayList<Page>());
			if (this.pages != null) {
				Page[] pageArray = this.pages.toArray(new Page[this.pages.size()]);
				for (Page page : pageArray) {
					Page newPage = page.templateClone();
					newQuestionnaire.pages.add(newPage);
					newPage.setQuestionnaire(newQuestionnaire);
				}
			}
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		return newQuestionnaire;
	}
	
	public Questionnaire cloneWithId() {
		Questionnaire newQuestionnaire = null;
		try {
			newQuestionnaire = (Questionnaire) super.clone();
			newQuestionnaire.setId(this.getId());
			newQuestionnaire.setParticipation(null);
			newQuestionnaire.setPages(new ArrayList<Page>());
			if (this.pages != null) {
				Page[] pageArray = this.pages.toArray(new Page[this.pages.size()]);
				for (Page page : pageArray) {
					Page newPage = page.cloneWithId();
					newQuestionnaire.pages.add(newPage);
					newPage.setQuestionnaire(newQuestionnaire);
				}
			}
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return newQuestionnaire;
	}
	
}
