/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.hibernate.Session;
import org.hibernate.Transaction;

import de.cinovo.surveyplatform.bootstrap.configuration.EnvironmentConfiguration;
import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.model.UserGroup;
import de.cinovo.surveyplatform.servlets.dal.SystemUserDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.FileUploadUtil;
import de.cinovo.surveyplatform.util.GroupManager;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.ImageUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.ParamUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;
import de.cinovo.surveyplatform.util.TimeUtil;
import de.cinovo.surveyplatform.util.TreeUtil;
import de.cinovo.surveyplatform.util.TreeUtil.TreeNode;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 *
 * @author yschubert
 *
 */
public class ProfileContainer extends AbstractContainer {
	
	private static final String METHOD_GETID = "getId";
	private static final String METHOD_GETPARENTGROUP = "getParentGroup";
	
	private static final int LOGOMAXWIDTH = 300;
	private static final int LOGOMAXHEIGHT = 150;
	
	/**
	 *
	 */
	private static final String PARAM_REMOVELOGO = "bRemoveLogo";
	
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet
	 * .http.HttpServletRequest
	 * , java.lang.StringBuilder)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, SystemUser user) {
		
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_MY_PROFILE, HelpIDs.PAGE_PROFILE));
		
		if (user != null) {
			
			// load the user from the database to get current values!
			Session hibSess = HibernateUtil.getSessionFactory().openSession();
			try {
				Transaction tx = hibSess.beginTransaction();
				
				// associate user with the current session
				// DO NOT USE hibSess.merge() because this will
				// overwrite data of the user when the user is logged
				// in multiple times!!
				user = (SystemUser) hibSess.load(SystemUser.class, user.getUserName());
				
				Set<UserGroup> allGroups = (Set<UserGroup>) GroupManager.getVisibleGroups(hibSess, user, user);
				TreeNode<UserGroup> rootNode = TreeUtil.convertTableToTree(allGroups, METHOD_GETID, METHOD_GETPARENTGROUP);
				
				tx.commit();
				
				Map<String, String> replacements = new HashMap<String, String>();
				
				replacements.put("USERID", user.getUserName());
				
				Client client = user.getUserGroups().iterator().next().getClient();
				String clientLogo = PartsUtil.getClientLogo(client, 0);
				if (AuthUtil.hasRight(user, UserRights.CLIENT_ADMINISTRATOR)) {
					handlePictureUpload(request, client);
					handleRemoveLogo(request, client);
					clientLogo = PartsUtil.getClientLogo(client, 0);
					
					if (clientLogo.isEmpty()) {
						// placeholder
						replacements.put("CLIENTLOGO", "<img src=\"" +
								EnvironmentConfiguration.getUrlBase() +
								"/gfx/nologo.jpg\" title=\"Here could be your logo\" />");
					} else {
						replacements.put("CLIENTLOGO", clientLogo);
						// Remove_Button only shows up, if a clientLogo is uploaded
						replacements.put("REMOVELOGO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/profileRemoveLogoPart.html", null));
					}
					replacements.put("UPLOADLOGOPART", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/profileUploadLogoPart.html", replacements));
					replacements.put("UPLOADLOGOINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/uploadLogoInfo.html", null));
				} else {
					if (!clientLogo.isEmpty()) {
						replacements.put("UPLOADLOGOPART", clientLogo);
					}
				}
				replacements.put("MANAGEUSERSBUTTON", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/profileManageUsersButton.html", null));
				
				replacements.put("USERNAME_TEXTFIELD", "<b>" + user.getActualUserName() + "</b>");
				
				String actionTemplate = Paths.TEMPLATEPATH + "/savePartOfUserAction.html";
				String actionTemplateForGroupCheckboxes = Paths.TEMPLATEPATH + "/savePartOfUserCheckboxAction.html";
				
				StringStringPair kvpUserID = new StringStringPair("USERID", user.getUserName());
				
				replacements.put("TITLE_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(SystemUserDal.PARAM_TITLE, SystemUserDal.PARAM_TITLE, user.getTitle(), actionTemplate, kvpUserID));
				replacements.put("FIRSTNAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(SystemUserDal.PARAM_FIRST_NAME, SystemUserDal.PARAM_FIRST_NAME, user.getFirstName(), actionTemplate, kvpUserID));
				replacements.put("LASTNAME_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(SystemUserDal.PARAM_LAST_NAME, SystemUserDal.PARAM_LAST_NAME, user.getLastName(), actionTemplate, kvpUserID));
				replacements.put("EMAIL_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(SystemUserDal.PARAM_EMAIL, SystemUserDal.PARAM_EMAIL, user.getEmail(), actionTemplate, kvpUserID));
				replacements.put("ADDRESS_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(SystemUserDal.PARAM_ADDRESS, SystemUserDal.PARAM_ADDRESS, user.getAddress(), actionTemplate, kvpUserID));
				replacements.put("PHONE_TEXTFIELD", HtmlFormUtil.getInplaceEditableTextfield(SystemUserDal.PARAM_PHONE, SystemUserDal.PARAM_PHONE, user.getPhone(), actionTemplate, kvpUserID));
				replacements.put("STATE_COMBO", user.getUserStatus().toString());
				
				List<StringStringPair> options = new ArrayList<StringStringPair>();
				options.add(new StringStringPair(TimeUtil.DATEFORMAT_EN.replace("'", "_"), TimeUtil.DATEFORMAT_EN.replace("'", "")));
				options.add(new StringStringPair(TimeUtil.DATEFORMAT_DE.replace("'", "_"), TimeUtil.DATEFORMAT_DE.replace("'", "")));
				replacements.put("DATEFORMAT_COMBO", HtmlFormUtil.getDynamicEditableComboBox(SystemUserDal.PARAM_DATEFORMAT, SystemUserDal.PARAM_DATEFORMAT, options, user.getTimeFormat().replace("'", "_"), actionTemplate, kvpUserID));
				
				String[] availableTimeZones = TimeUtil.getAvailableTimeZones();
				options.clear();
				for (String zoneID : availableTimeZones) {
					options.add(new StringStringPair(zoneID, zoneID));
				}
				replacements.put("TIMEZONE_COMBO", HtmlFormUtil.getDynamicEditableComboBox(SystemUserDal.PARAM_TIMEZONE, SystemUserDal.PARAM_TIMEZONE, options, user.getTimeZoneID(), actionTemplate, kvpUserID));
				
				options.clear();
				buildGroupTree(options, rootNode, 0);
				List<String> selectedOptions = new ArrayList<String>();
				StringBuilder selectedOptionsSb = new StringBuilder();
				selectedOptionsSb.append("<ul>");
				
				for (UserGroup group : user.getUserGroups()) {
					selectedOptions.add(group.getId() + "");
					selectedOptionsSb.append("<li>");
					selectedOptionsSb.append(group.getName());
					selectedOptionsSb.append("</li>");
				}
				selectedOptionsSb.append("</ul>");
				
				if (AuthUtil.isAllowedToEditSystemUser(user)) {
					String dynamicEditableCheckBoxes = HtmlFormUtil.getDynamicEditableCheckBoxes(SystemUserDal.PARAM_MEMBER_SHIPS, SystemUserDal.PARAM_MEMBER_SHIPS, options, selectedOptions, actionTemplateForGroupCheckboxes, kvpUserID);
					replacements.put("GROUPLIST", dynamicEditableCheckBoxes);
				}
				else {
					replacements.put("GROUPLIST", selectedOptionsSb.toString());
				}
				
				replacements.put("TABLE", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableUserInfo.html", replacements));
				
				replacements.put("CURRENTPASSWORDROW", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgChangePasswordCurrentPwdRow.html", null));
				replacements.put("INFORMATION", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgChangePasswordInformation.html", null));
				replacements.put("DLGCHANGEPASSWORD", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgChangePassword.html", replacements));
				
				content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/profile.html", replacements));
			} finally {
				hibSess.close();
			}
		}
		
	}
	
	/**
	 * @param request
	 * @param user
	 */
	private void handleRemoveLogo(final HttpServletRequest request, final Client client) {
		if (ParamUtil.checkAllParamsSet(request, PARAM_REMOVELOGO)) {
			File targetFile = new File(Paths.CLIENTLOGOS + "/" + client.getId() + ".jpg");
			targetFile.delete();
		}
	}
	
	/**
	 * @param request
	 */
	private void handlePictureUpload(final HttpServletRequest request, final Client client) {
		if (ServletFileUpload.isMultipartContent(request)) {
			try {
				List<File> uploadedFiles = FileUploadUtil.processUpload(request, Paths.WEBCONTENT + "/" + Paths.UPLOAD_TEMP);
				for (File file : uploadedFiles) {
					if ((file != null) && file.exists()) {
						File targetFile = new File(Paths.CLIENTLOGOS + "/" + client.getId() + ".jpg");
						if (targetFile.exists()) {
							targetFile.delete();
						}
						ImageUtil.createThumbnail(file, targetFile, LOGOMAXWIDTH, LOGOMAXHEIGHT);
						file.delete();
						// file.renameTo(targetFile);
						// file.delete();
					}
				}
			} catch (Exception e) {
				Logger.err("Konnte Datei nicht hochladen", e);
			}
		}
	}
	
	private void buildGroupTree(final List<StringStringPair> options, final TreeNode<UserGroup> node, final int depth) {
		Set<TreeNode<UserGroup>> children = node.getChildren();
		if (children != null) {
			for (TreeNode<UserGroup> child : children) {
				options.add(new StringStringPair(child.getData().getId() + "", "<span style=\"margin-left: " + (depth * 20) + "px ;\">" + child.getData().getName() + "</span>"));
				buildGroupTree(options, child, depth + 1);
			}
		}
	}
	
}
