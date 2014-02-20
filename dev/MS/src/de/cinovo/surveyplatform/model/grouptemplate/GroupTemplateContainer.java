/**
 * 
 */
package de.cinovo.surveyplatform.model.grouptemplate;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.util.XMLUtil;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)
public class GroupTemplateContainer {
	
	@XmlElements({@XmlElement(name = "template", type = GroupTemplate.class)})
	private Set<GroupTemplate> templates;
	
	
	/**
	 * @return the templates
	 */
	public Set<GroupTemplate> getTemplates() {
		return templates;
	}
	
	/**
	 * @param templates the templates to set
	 */
	public void setTemplates(final Set<GroupTemplate> templates) {
		this.templates = templates;
	}
	
	public static void main(final String[] args) {
		GroupTemplate groupTemplate = new GroupTemplate();
		GroupTemplateContainer gtc = new GroupTemplateContainer();
		gtc.setTemplates(new HashSet<GroupTemplate>());
		gtc.getTemplates().add(groupTemplate);
		groupTemplate.setName("Administration");
		groupTemplate.setUserRights(new HashSet<UserRights>());
		for (UserRights right : UserRights.values()) {
			groupTemplate.getUserRights().add(right);
		}
		
		XMLUtil.obj2XmlFile(gtc, GroupTemplateContainer.class, new File(Paths.TEMPLATEPATH + "/groups.xml"));
		
	}
	
}
