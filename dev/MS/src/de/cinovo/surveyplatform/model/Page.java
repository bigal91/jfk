package de.cinovo.surveyplatform.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import de.cinovo.surveyplatform.sync.Sync;
import de.cinovo.surveyplatform.sync.SyncFilter;
import de.cinovo.surveyplatform.sync.SyncIdentifiable;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * @author yschubert
 * 
 */
@Entity
public class Page implements Cloneable, ITemplateCloneable, IQuestionnairePart, Comparable<Page>, ISortable, SyncIdentifiable<String> {
	
	private int id;
	
	private int orderNumber = 0;
	
	@Sync(filter = SyncFilter.PARTICIPANT)
	private List<Section> sections = new ArrayList<Section>();
	
	private String title = "";
	
	private String description = "";
	
	private boolean visible = true;
	
	private boolean previewModeVisible = true;
	
	private boolean isTargetOfLogic;
	
	private int localId;
	
	private Questionnaire questionnaire;
	
	private String syncId;
	
	
	@Override
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return this.id;
	}
	
	/**
	 * @return -
	 */
	@XmlID
	@Transient
	public String getXMLID() {
		return this.getClass().getSimpleName() + this.getId();
	}
	
	private void setId(final int id) {
		this.id = id;
	}
	
	/**
	 * @return the questionnaire
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "questionnaire_id", nullable = false)
	@XmlIDREF
	public Questionnaire getQuestionnaire() {
		return this.questionnaire;
	}
	
	/**
	 * @param questionnaire the questionnaire to set
	 */
	public void setQuestionnaire(final Questionnaire questionnaire) {
		this.questionnaire = questionnaire;
	}
	
	@Override
	public int getOrderNumber() {
		return this.orderNumber;
	}
	
	@Override
	public void setOrderNumber(final int order) {
		this.orderNumber = order;
	}
	
	/**
	 * @return -
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "page", fetch = FetchType.LAZY)
	@OrderBy("orderNumber")
	public List<Section> getSections() {
		return this.sections;
	}
	
	/**
	 * @param sections -
	 */
	public void setSections(final List<Section> sections) {
		this.sections = sections;
	}
	
	/**
	 * @return -
	 */
	public String getTitle() {
		return this.title;
	}
	
	/**
	 * @param title -
	 */
	public void setTitle(final String title) {
		this.title = title;
	}
	
	/**
	 * @return -
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * @param description -
	 */
	public void setDescription(final String description) {
		this.description = description;
	}
	
	/**
	 * @return the visible
	 */
	public boolean isVisible() {
		return this.visible;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.model.IQuestionnairePart#setVisible(boolean)
	 */
	@Override
	public void setVisible(final boolean visible) {
		this.visible = visible;
		
		// // inherit visibility to childs
		// if (sections != null) {
		// for (Section s : sections) {
		// s.setVisible(visible);
		// }
		// }
		
	}
	
	/**
	 * @param localId -
	 */
	public void setLocalId(final int localId) {
		this.localId = localId;
	}
	
	/**
	 * @return -
	 */
	public int getLocalId() {
		return this.localId;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final Page page) {
		if (this.orderNumber < page.getOrderNumber()) {
			return -1;
		} else if (this.orderNumber > page.getOrderNumber()) {
			return 1;
		}
		return 0;
	}
	
	@Override
	public Page clone() {
		Page newPage = null;
		try {
			newPage = (Page) super.clone();
			newPage.setId(0);
			newPage.setSections(new ArrayList<Section>());
			if (this.sections != null) {
				final Section[] sectionArray = this.sections.toArray(new Section[this.sections.size()]);
				for (Section section : sectionArray) {
					Section newSection = section.clone();
					newPage.sections.add(newSection);
					newSection.setPage(newPage);
				}
			}
		} catch (CloneNotSupportedException e) {
			// clone IS supported
		}
		return newPage;
	}
	
	@Override
	public Page templateClone() {
		Page newPage = null;
		try {
			newPage = (Page) super.clone();
			newPage.setId(0);
			newPage.setSections(new ArrayList<Section>());
			if (this.sections != null) {
				final Section[] sectionArray = this.sections.toArray(new Section[this.sections.size()]);
				for (Section section : sectionArray) {
					Section newSection = section.templateClone();
					newPage.sections.add(newSection);
					newSection.setPage(newPage);
				}
			}
		} catch (CloneNotSupportedException e) {
			// clone IS supported
		}
		return newPage;
	}
	
	/**
	 * @return -
	 */
	public Page cloneWithId() {
		Page newPage = null;
		try {
			newPage = (Page) super.clone();
			newPage.setId(this.getId());
			newPage.setQuestionnaire(null);
			newPage.setSections(new ArrayList<Section>());
			if (this.sections != null) {
				final Section[] sectionArray = this.sections.toArray(new Section[this.sections.size()]);
				for (Section section : sectionArray) {
					Section newSection = section.cloneWithId();
					newPage.sections.add(newSection);
					newSection.setPage(newPage);
				}
			}
		} catch (CloneNotSupportedException e) {
			// clone IS supported
		}
		return newPage;
	}
	
	/**
	 * @param isTargetOfLogic the isTargetOfLogic to set
	 */
	public void setTargetOfLogic(final boolean isTargetOfLogic) {
		this.isTargetOfLogic = isTargetOfLogic;
	}
	
	/**
	 * @return the isTargetOfLogic
	 */
	@Column(columnDefinition = "boolean not null default false")
	public boolean isTargetOfLogic() {
		return this.isTargetOfLogic;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.model.IQuestionnairePart#setPreviewModeVisible
	 * (boolean)
	 */
	@Override
	public void setPreviewModeVisible(final boolean previewModeVisible) {
		this.previewModeVisible = previewModeVisible;
	}
	
	/**
	 * @return -
	 */
	@Column(columnDefinition = "boolean not null default true")
	public boolean isPreviewModeVisible() {
		return this.previewModeVisible;
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
	
}
