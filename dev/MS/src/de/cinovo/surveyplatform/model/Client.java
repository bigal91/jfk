/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlID;

import de.cinovo.surveyplatform.util.Logger;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
@Entity
@Table(schema = "survey")
public class Client implements Cloneable {
	
	private long id;
	
	private String organization;
	
	private PaymentModel paymentModel;
	
	private List<Payment> payments;
	
	boolean internal = false;
	
	private UserStatus clientStatus = UserStatus.Disabled;
	
	
	/**
	 * @return -
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
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
	
	/**
	 * @param id the id to set
	 */
	public void setId(final long id) {
		this.id = id;
	}
	
	/**
	 * @return -
	 */
	@Column(columnDefinition = "text")
	public String getOrganization() {
		return this.organization;
	}
	
	/**
	 * @param organization -
	 */
	public void setOrganization(final String organization) {
		this.organization = organization;
	}
	
	/**
	 * @return the paymentModel
	 */
	public PaymentModel getPaymentModel() {
		return this.paymentModel;
	}
	
	/**
	 * @param paymentModel the paymentModel to set
	 */
	public void setPaymentModel(final PaymentModel paymentModel) {
		this.paymentModel = paymentModel;
	}
	
	/**
	 * @return the internal
	 */
	public boolean getInternal() {
		return this.internal;
	}
	
	/**
	 * @param internal the internal to set
	 */
	public void setInternal(final boolean internal) {
		this.internal = internal;
	}
	
	/**
	 * 
	 * @return -
	 */
	public UserStatus getClientStatus() {
		return this.clientStatus;
	}
	
	/**
	 * 
	 * @param clientStatus -
	 */
	public void setClientStatus(final UserStatus clientStatus) {
		this.clientStatus = clientStatus;
	}
	
	/**
	 * @return the payments
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "client", fetch = FetchType.LAZY)
	@OrderBy("dueDate")
	public List<Payment> getPayments() {
		return this.payments;
	}
	
	/**
	 * @param payments the payments to set
	 */
	public void setPayments(final List<Payment> payments) {
		this.payments = payments;
	}
	
	@Override
	public Client clone() {
		Client clone = null;
		try {
			clone = (Client) super.clone();
			clone.setId(0);
		} catch (CloneNotSupportedException e) {
			Logger.err("", e);
		}
		
		return clone;
	}
	
	/**
	 * @return -
	 */
	public Client cloneWithId() {
		Client clone = this.clone();
		clone.setId(this.id);
		return clone;
	}
	
}
