/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.servlet.ServletFileUpload;

import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.feedback.FeedBackProvider;
import de.cinovo.surveyplatform.feedback.FeedBackProvider.Status;
import de.cinovo.surveyplatform.importer.ParticipantImporter;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.FileUploadUtil;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class DataImportContainer extends AbstractContainer {
	
	public static final String PARAM_IMPORT_FILE = "file";
	
	
	/* (non-Javadoc)
	 * @see de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet.http.HttpServletRequest, java.lang.StringBuilder, de.cinovo.surveyplatform.model.SystemUser)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_DATA_IMPORT, HelpIDs.PAGE_DATA_IMPORT));
		
		if (currentUser != null) {
			
			if (ServletFileUpload.isMultipartContent(request)) {
				String taskID = "page." + Pages.PAGE_DATA_IMPORT + "." + request.getSession().getId();
				FeedBackProvider fbp = FeedBackProvider.getInstance();
				fbp.beginTask("Import of Participants", taskID, currentUser.getActualUserName());
				try {
					List<File> uploadedFiles = FileUploadUtil.processUpload(request, Paths.WEBCONTENT + "/" + Paths.UPLOAD_TEMP);
					for (File file : uploadedFiles) {
						ParticipantImporter pi = new ParticipantImporter(taskID);
						pi.importFromXml(file, currentUser);
					}
					fbp.addFeedback(taskID, "Data imported", Status.OK);
					fbp.finishTask(taskID);
				} catch (Exception ex) {
					Logger.err("Error during import of a xml file", ex);
					fbp.addFeedback(taskID, "Could not import data: " + ex.getMessage(), Status.ERROR);
					fbp.finishTask(taskID, true);
				}
			}
			
			content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/importData.html", null));
		} else {
			content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
		}
	}
	
}
