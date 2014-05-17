/**
 *
 */
package util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import model.User;


/**
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class TimeUtil {
	
	/**
	 * 
	 */
	public static final String DATEFORMAT_DE = ("dd.MM.yyyy 'at' HH:mm");
	/**
	 * 
	 */
	public static final String DATEFORMAT_EN = ("MM/dd/yyyy 'at' HH:mm");
	/**
	 * 
	 */
	public static final String DATEFORMAT_FILE = ("yyyy-MM-dd-HH-mm");
	/**
	 * 
	 */
	public static final String DATEFORMAT_LOG = ("dd.MM.yyyy HH:mm:ss");
	
	/**
	 * 
	 */
	private static final SimpleDateFormat HTML_HEADER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT_EN = new SimpleDateFormat(TimeUtil.DATEFORMAT_EN);
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT_LOG = new SimpleDateFormat(TimeUtil.DATEFORMAT_LOG);
	
	static {
		HTML_HEADER_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}


	
	/**
	 * @return -
	 */
	public static String getFormattedFileTime() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TimeUtil.DATEFORMAT_FILE);
		return simpleDateFormat.format(new Date());
	}
	
	/**
	 * @param millis -
	 * @return -
	 */
	public static String getFormattedLogTime(final long millis) {
		return SIMPLE_DATE_FORMAT_LOG.format(new Date(millis));
	}
	
	/**
	 * @return -
	 */
	public static String getFormattedLogTime() {
		return SIMPLE_DATE_FORMAT_LOG.format(new Date());
	}
	
	/**
	 * @param timeZone -
	 * @param format -
	 * @param date -
	 * @return -
	 */
	public static String getLocalTime(final TimeZone timeZone, final String format, final Date date) {
		if ((date == null)) {
			return "---";
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		simpleDateFormat.setTimeZone(timeZone);
		return simpleDateFormat.format(date);
	}
	
	/**
	 * @return -
	 */
	public static String[] getAvailableTimeZones() {
		String[] availableIDs = TimeZone.getAvailableIDs();
		Arrays.sort(availableIDs);
		return availableIDs;
	}
	
	/**
	 * @param date -
	 * @return -
	 */
	public static String htmlHeaderFormat(final Date date) {
		return HTML_HEADER_DATE_FORMAT.format(date);
	}
	
	
}
