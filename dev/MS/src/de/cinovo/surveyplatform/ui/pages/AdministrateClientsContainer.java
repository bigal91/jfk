/**
 *
 */
package de.cinovo.surveyplatform.ui.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;

import de.cinovo.surveyplatform.constants.HelpIDs;
import de.cinovo.surveyplatform.constants.Pages;
import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.constants.UserRights;
import de.cinovo.surveyplatform.hibernate.HibernateUtil;
import de.cinovo.surveyplatform.model.Client;
import de.cinovo.surveyplatform.model.PaymentModel;
import de.cinovo.surveyplatform.model.StringStringPair;
import de.cinovo.surveyplatform.model.SystemUser;
import de.cinovo.surveyplatform.servlets.dal.ClientDal;
import de.cinovo.surveyplatform.ui.AbstractContainer;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.HtmlFormUtil;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class AdministrateClientsContainer extends AbstractContainer {
	
	/* (non-Javadoc)
	 * @see de.cinovo.surveyplatform.ui.AbstractContainer#provideContent(javax.servlet.http.HttpServletRequest, java.lang.StringBuilder, de.cinovo.surveyplatform.model.SystemUser)
	 */
	@Override
	public void provideContent(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		if (AuthUtil.hasRight(currentUser, UserRights.ADMINISTRATOR)) {
			showClientTable(request, content, currentUser);
		} else {
			content.append(HtmlFormUtil.getErrorMessage(PartsUtil.getUnsufficientRightsMessage()));
		}
	}
	
	private void showClientTable(final HttpServletRequest request, final StringBuilder content, final SystemUser currentUser) {
		content.append(PartsUtil.getPageHeader(Pages.PAGE_HEADER_ADMINISTRATE_CLIENTS, HelpIDs.PAGE_ADMINISTRATE_CLIENTS));
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("CLIENTTABLE", getClientTable(request));
		
		Map<String, String> comboReplacements = new HashMap<String, String>();
		List<StringStringPair> options = new ArrayList<StringStringPair>();
		options.add(new StringStringPair(PaymentModel.Trial.toString(), PaymentModel.Trial.toString()));
		options.add(new StringStringPair(PaymentModel.Monthly.toString(), PaymentModel.Monthly.toString()));
		options.add(new StringStringPair(PaymentModel.Yearly.toString(), PaymentModel.Yearly.toString()));
		options.add(new StringStringPair(PaymentModel.NONE.toString(), PaymentModel.NONE.toString()));
		comboReplacements.put("ACCOUNT_COMBO", HtmlFormUtil.getComboBox(ClientDal.PARAM_ACCOUNT, ClientDal.PARAM_ACCOUNT, options, PaymentModel.Trial.toString()));
		
		replacements.put("TABLE_EDITCLIENT", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/tableEditClient.html", comboReplacements));
		replacements.put("DLGCREATECLIENT", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgCreateClient.html", replacements));
		replacements.put("DLGCLIENTINFO", TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dlgClientInfo.html", null));
		content.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateClients.html", replacements));
	}
	
	private String getClientTable(final HttpServletRequest request) {
		Map<String, String> replacements = new HashMap<String, String>();
		Session hibSess = HibernateUtil.getSessionFactory().openSession();
		try {
			Transaction tx = hibSess.beginTransaction();
			
			Criteria criteria = hibSess.createCriteria(Client.class);
			
			criteria.addOrder(Order.asc("organization"));
			
			Map<String, String> rowReplacements = new HashMap<String, String>();
			
			StringBuilder tableRows = new StringBuilder();
			
			List<?> list = criteria.list();
			for (Object obj : list) {
				if (obj instanceof Client) {
					Client iterClient = (Client) obj;
					rowReplacements.put("CLIENTID", iterClient.getId() + "");
					rowReplacements.put("ORGANIZATION", "<a href=\"javascript:void(0);\" onclick=\"showClientInfo('" + iterClient.getId() + "');\"><span id=\"" + ClientDal.PARAM_ORGANIZATION + iterClient.getId() + "\">" + iterClient.getOrganization() + "</span></a>");
					rowReplacements.put("PAYMENTMODEL", "<span id=\"" + ClientDal.PARAM_ACCOUNT + iterClient.getId() + "\">" + (iterClient.getInternal() ? "---" : iterClient.getPaymentModel().toString()) + "</span>");
					
					rowReplacements.put("PAYMENTINFO", "---");
					
					rowReplacements.put("BUTTONS", PartsUtil.getIconLink("MANAGE_CLIENTS", "Edit Groups", "", "?page=" + Pages.PAGE_ADMINISTRATE_GROUPS + "&clientID=" + iterClient.getId()));
					
					tableRows.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateClientTable_row.html", rowReplacements));
				}
			}
			tx.commit();
			replacements.put("ROWS", tableRows.toString());
			
			replacements.put("BUTTONCOLUMNWIDTH", "20");
		} finally {
			hibSess.close();
		}
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/administrateClientTable.html", replacements);
	}
	
}
