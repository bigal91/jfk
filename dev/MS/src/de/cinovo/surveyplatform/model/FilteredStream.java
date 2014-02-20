/**
 * 
 */
package de.cinovo.surveyplatform.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.fileupload.util.Streams;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class FilteredStream extends FilterOutputStream {
	
	private long maxFileSize = 1000000;
	
	private final File logFile;
	
	
	public FilteredStream(final OutputStream aStream, final File logFile) {
		super(aStream);
		this.logFile = logFile;
	}
	
	@Override
	public void write(final byte b[]) throws IOException {
		String aString = new String(b);
		this.log(aString);
	}
	
	@Override
	public void write(final byte b[], final int off, final int len) throws IOException {
		String aString = new String(b, off, len);
		this.log(aString);
	}
	
	private void log(final String stringToLog) throws IOException {
		boolean append = true;
		if (this.logFile.length() > this.maxFileSize) {
			Streams.copy(new FileInputStream(this.logFile), new FileOutputStream(new File(this.logFile.getName().substring(0, this.logFile.getName().lastIndexOf(".")) + "_" + System.currentTimeMillis() + this.logFile.getName().substring(this.logFile.getName().lastIndexOf(".")))), true);
			append = false;
		}
		FileWriter aWriter = new FileWriter(this.logFile, append);
		aWriter.write(stringToLog);
		
		aWriter.close();
	}
}
