/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.servlets.dal.ClientDal;
import de.cinovo.surveyplatform.servlets.dal.UserGroupDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TreeUtil;
import de.cinovo.surveyplatform.util.TreeUtil.TreeNode;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class AdministrateGroupsContainer extends AbstractContainer {
	
	/**
	 *
	 */
	private static final String SPACE = " ";
	private static final String METHOD_GETID = "getId";
	private static final String METHOD_GETPARENTGROUP = "getParentGroup";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet
	 * .http.HttpServletRequest, java.lang.StringBuilder,
	 * de.cinovo.surveyplatform.model.SystemUser)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		showGroupTable(request, content, currentUser);
	}
	
	private void showGroupTable(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		
		long clientID = ParamUtil.getSafeLongFromParam(request, ClientDal.PARAM_CLIENTID);
		
		if (clientID == 0) {
			clientID = currentUser.getUserGroups().iterator().next().getClient().getId();
		}
		
		if (clientID > 0) {
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Transaction tx = hibSess.beginTransaction();
				Client client = (Client) hibSess.load(Client.class, clientID);
				if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
					content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_ADMINISTRATE_GROUPS + ": " + client.getOrganization(), HelpIDs.PAGE_ADMINISTRATE_GROUPS, new String[] {"<a href=\"?page=" + Pages.PAGE_ADMINISTRATE_CLIENTS + "\">" + Pages.PAGE_HEADER_ADMINISTRATE_CLIENTS + "</a>"}));
				} else {
					content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_ADMINISTRATE_GROUPS, HelpIDs.PAGE_ADMINISTRATE_GROUPS));
				}
				Map<String, String> replacements = new HashMap<String, String>();
				replacements.put("CLIENTID", clientID + "");
				
				Collection<UserGroup> visibleGroups = GroupManager.getVisibleGroups(hibSess, client, currentUser);
				replacements.put("GROUPSTABLE", getGroupsTable(visibleGroups));
				replacements.put("GROUP_TREE_SELECTABLE", getGroupSelectionTree(visibleGroups));
				
				replacements.put("TABLE_EDITGROUP", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableEditGroup.html", replacements));
				replacements.put("DLGCREATEGROUP", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgCreateGroup.html", replacements));
				replacements.put("DLGGROUPINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgGroupInfo.html", null));
				content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateGroups.html", replacements));
				tx.commit();
			} finally {
				hibSess.close();
			}
		}
		
	}
	
	private String getGroupSelectionTree(final Collection<UserGroup> groups) {
		
		Map<String, String> replacements = new HashMap<String, String>();
		StringBuilder tableRows = new StringBuilder();
		
		Set<UserGroup> result = new TreeSet<UserGroup>(groups);
		TreeNode<UserGroup> rootNode = TreeUtil.convertTableToTree(result, METHOD_GETID, METHOD_GETPARENTGROUP);
		
		printSelectableRows(rootNode, 0, tableRows);
		
		replacements.put("ROWS", tableRows.toString());
		
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/selectGroupTree.html", replacements);
	}
	
	private void printSelectableRows(final TreeNode<UserGroup> node, final int depth, final StringBuilder tableRows) {
		Map<String, String> rowReplacements = new HashMap<String, String>();
		
		UserGroup userGroup = node.getData();
		if (userGroup != null) {
			rowReplacements.put("WIDTH", String.valueOf(20 * (depth - 1)));
			rowReplacements.put("GROUPID", userGroup.getId() + "");
			rowReplacements.put("NAME", userGroup.getName());
			tableRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/selectGroupTree_row.html", rowReplacements));
		}
		
		Set<TreeNode<UserGroup>> children = node.getChildren();
		if (children != null) {
			
			for (TreeNode<UserGroup> child : children) {
				printSelectableRows(child, depth + 1, tableRows);
			}
		}
	}
	
	private String getGroupsTable(final Collection<UserGroup> groups) {
		
		Map<String, String> replacements = new HashMap<String, String>();
		StringBuilder tableRows = new StringBuilder();
		
		Set<UserGroup> result = new TreeSet<UserGroup>(groups);
		
		TreeNode<UserGroup> rootNode = TreeUtil.convertTableToTree(result, METHOD_GETID, METHOD_GETPARENTGROUP);
		
		printTableRows(rootNode, 0, tableRows);
		
		replacements.put("ROWS", tableRows.toString());
		
		replacements.put("BUTTONCOLUMNWIDTH", "20");
		
		if (result.size() > 1) {
			StringBuilder commands = new StringBuilder();
			commands.append("<select class=\"commandSelector\">");
			commands.append("<option value=\"\">-- Select a command--</option>");
			commands.append("<option value=\"delete\">Remove selected groups</option>");
			commands.append("</select><a class=\"runCommandButton\" href=\"javascript:void(0);\" onclick=\"sendCommand($(this).prev().val());\">Go</a>");
			replacements.put("COMMANDS", commands.toString());
		}
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateGroupTable.html", replacements);
		
	}
	
	private void printTableRows(final TreeNode<UserGroup> node, final int depth, final StringBuilder tableRows) {
		Map<String, String> rowReplacements = new HashMap<String, String>();
		
		UserGroup userGroup = node.getData();
		if (userGroup != null) {
			rowReplacements.put("WIDTH", String.valueOf((20 * (depth - 1)) + 5));
			rowReplacements.put("CHECKBOX", "<input class=\"groupCheckBox\" type=\"checkbox\" value=\"" + userGroup.getId() + "\" name=\"groups[]\" />");
			
			rowReplacements.put("GROUPID", userGroup.getId() + "");
			rowReplacements.put("NAME", "<a href=\"javascript:void(0);\" onclick=\"showGroupInfo('" + userGroup.getId() + "');\"><span id=\"" + UserGroupDal.PARAM_NAME + userGroup.getId() + "\">" + userGroup.getName() + "</span></a>");
			
			rowReplacements.put("MEMBERLIST", getMemberList(userGroup) + "");
			
			StringBuilder buttons = new StringBuilder();
			buttons.append(PartsUtil.getIconLink("MANAGE_USERS", "Manage Users", "", "?page=" + Pages.PAGE_USERADMIN + "&groupID=" + userGroup.getId()));
			buttons.append("<a class=\"bCreateSubGroup gui-icon-button-SUBGROUP\" data-groupid=\"" + userGroup.getId() + "\" title=\"Create Sub-Group\" href=\"javascript:void(0);\"></a>");
			rowReplacements.put("BUTTONS", buttons.toString());
			
			tableRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateGroupTable_row.html", rowReplacements));
		}
		Set<TreeNode<UserGroup>> children = node.getChildren();
		if (children != null) {
			for (TreeNode<UserGroup> child : children) {
				printTableRows(child, depth + 1, tableRows);
			}
		}
	}
	
	private String getMemberList(final UserGroup userGroup) {
		StringBuilder sb = new StringBuilder();
		Set<SystemUser> members = userGroup.getMembers();
		if ((members == null) || (members.size() == 0)) {
			sb.append("<a class=\"inTableAction\" href=\"?page=" + Pages.PAGE_USERADMIN + "&groupID=");
			sb.append(userGroup.getId());
			sb.append("#create\" title=\"Click to create a new user\">Create new User</a>");
			sb.append("");
		} else {
			for (SystemUser member : members) {
				sb.append("<a href=\"?page=" + Pages.PAGE_USERADMIN + "&groupID=");
				sb.append(userGroup.getId());
				sb.append("#open=");
				sb.append(member.getUserName());
				sb.append("\" title=\"Click to open the users details\" >");
				sb.append(member.getUserName());
				sb.append("</a>");
				sb.append(SPACE);
			}
		}
		return sb.toString();
	}
	
}
