package de.cinovo.surveyplatform.importer;

import java.io.File;

public class ImportFromExcelException extends Exception {
	
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String REASON_INVALID_FORMAT = "Invalid format.";
	public static final String REASON_NOT_EXISTING = "The File does not exist.";
	public static final String REASON_NO_PARTICIPANTS_FOUND = "No participants found in the file.";
	public static final String REASON_NOT_VALID_EXCEL = "Not a valid MS-Excel&reg; file.";
	
	private String reason;
	private File file;
	
	
	public ImportFromExcelException(final Throwable t) {
		super(t);
	}
	
	public ImportFromExcelException(final File file, final String reason) {
		this.file = file;
		this.reason = reason;
	}
	
	@Override
	public String getMessage() {
		String message = "Could not import the file: " + this.file.getAbsolutePath();
		message += "\nReason: " + this.reason + "\n" + super.getMessage();
		return message;
	}
	
	/**
	 * @return the reason
	 */
	public String getReason() {
		return this.reason;
	}
}
