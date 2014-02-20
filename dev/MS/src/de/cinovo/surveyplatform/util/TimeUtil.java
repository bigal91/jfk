/**
 *
 */
package de.cinovo.surveyplatform.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import de.cinovo.surveyplatform.model.SystemUser;

/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class TimeUtil {
	
	public static final String DATEFORMAT_DE = new String("dd.MM.yyyy 'at' HH:mm");
	public static final String DATEFORMAT_EN = new String("MM/dd/yyyy 'at' HH:mm");
	public static final String DATEFORMAT_FILE = new String("yyyy-MM-dd-HH-mm");
	public static final String DATEFORMAT_LOG = new String("dd.MM.yyyy HH:mm:ss");

	/**
	 * 
	 */
	private static final SimpleDateFormat HTML_HEADER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT_EN = new SimpleDateFormat(TimeUtil.DATEFORMAT_EN);
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT_LOG = new SimpleDateFormat(TimeUtil.DATEFORMAT_LOG);
	
	static {
		HTML_HEADER_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public static String getLocalTime(final SystemUser user, final Date date) {
		
		final TimeZone timeZone = TimeZone.getTimeZone(user.getTimeZoneID());
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(user.getTimeFormat());
		simpleDateFormat.setTimeZone(timeZone);
		
		return simpleDateFormat.format(date);
	}
	
	public static String getLocalTime(final HttpServletRequest request, final Date date) {
		
		SystemUser currentUser = AuthUtil.checkAuth(request);
		
		SimpleDateFormat simpleDateFormat = null;
		TimeZone timeZone = null;
		if (currentUser != null) {
			timeZone = TimeZone.getTimeZone(currentUser.getTimeZoneID());
			simpleDateFormat = new SimpleDateFormat(currentUser.getTimeFormat());
		} else {
			simpleDateFormat = SIMPLE_DATE_FORMAT_EN;
			timeZone = TimeZone.getDefault();
		}
		simpleDateFormat.setTimeZone(timeZone);
		
		return simpleDateFormat.format(date);
	}
	
	public static String getFormattedFileTime() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TimeUtil.DATEFORMAT_FILE);
		return simpleDateFormat.format(new Date());
	}
	
	public static String getFormattedLogTime(final long millis) {
		return SIMPLE_DATE_FORMAT_LOG.format(new Date(millis));
	}
	
	public static String getLocalTime(final TimeZone timeZone, final String format, final Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		simpleDateFormat.setTimeZone(timeZone);
		return simpleDateFormat.format(date);
	}
	
	public static String[] getAvailableTimeZones() {
		String[] availableIDs = TimeZone.getAvailableIDs();
		Arrays.sort(availableIDs);
		return availableIDs;
	}
	
	public static String htmlHeaderFormat(final Date date) {
		return HTML_HEADER_DATE_FORMAT.format(date);
	}
	
}
