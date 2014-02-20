/**
 * 
 */
package de.cinovo.surveyplatform.bootstrap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import de.cinovo.surveyplatform.util.TimeUtil;

/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class LogFormatter extends Formatter {
	
	/* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	@Override
	public String format(final LogRecord record) {
		StringBuilder sb = new StringBuilder();
		sb.append(TimeUtil.getFormattedLogTime(record.getMillis()) + " " + record.getLevel().getName() + ": " + record.getMessage() + "\n");
		if (record.getThrown() != null) {
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			record.getThrown().printStackTrace(printWriter);
			sb.append(result);
		}
		
		return sb.toString();
	}
	
	
}
