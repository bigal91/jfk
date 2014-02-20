/**
 * 
 */
package de.cinovo.surveyplatform.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlIDREF;

/**
 * Copyright 2013 Cinovo AG<br><br>
 * @author yschubert
 *
 */
@Entity
public class Payment {
	
	private long id;
	private Client client = null;
	private Date dueDate = null;
	private boolean payed = false;
	private BigDecimal amount = BigDecimal.ZERO;
	
	
	/**
	 * @return -
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return this.id;
	}
	
	
	/**
	 * @param id the id to set
	 */
	public void setId(final long id) {
		this.id = id;
	}
	
	/**
	 * @return the client
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id", nullable = false)
	@XmlIDREF
	public Client getClient() {
		return this.client;
	}
	
	/**
	 * @param client the client to set
	 */
	public void setClient(final Client client) {
		this.client = client;
	}
	
	/**
	 * @return the dueDate
	 */
	public Date getDueDate() {
		return this.dueDate;
	}
	
	/**
	 * @param dueDate the dueDate to set
	 */
	public void setDueDate(final Date dueDate) {
		this.dueDate = dueDate;
	}
	
	/**
	 * @return the payed
	 */
	public boolean isPayed() {
		return this.payed;
	}
	
	/**
	 * @param payed the payed to set
	 */
	public void setPayed(final boolean payed) {
		this.payed = payed;
	}
	
	/**
	 * @return the amount
	 */
	public BigDecimal getAmount() {
		return this.amount;
	}
	
	/**
	 * @param amount the amount to set
	 */
	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}
	
}
