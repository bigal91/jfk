/**
 * 
 */
package de.cinovo.surveyplatform.model;

import java.util.LinkedList;
import java.util.Queue;


/**
 * Copyright 2013 Cinovo AG<br><br>
 * @author ablehm
 *
 */
public class SessionRequestQueue {
	
	private Queue<Long> requestStamps;
	
	private int length;
	
	
	public SessionRequestQueue(final int length) {
		requestStamps = new LinkedList<Long>();
		this.length = length;
	}
	
	/**
	 * @return the requestStamps
	 */
	public Queue<Long> getRequestStamps() {
		return requestStamps;
	}
	
	/**
	 * @param requestStamps the requestStamps to set
	 */
	public void setRequestStamps(final Queue<Long> requestStamps) {
		this.requestStamps = requestStamps;
	}
	
	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * @param length the length to set
	 */
	public void setLength(final int length) {
		this.length = length;
	}
	
	public boolean enqueue(final Long timeStamp) {
		if (requestStamps.size() >= length) {
			requestStamps.poll();
		}
		return requestStamps.offer(timeStamp);
	}
	
	public Long dequeue() {
		return requestStamps.poll();
	}
}
