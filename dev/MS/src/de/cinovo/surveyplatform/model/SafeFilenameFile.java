/**
 *
 */
package de.cinovo.surveyplatform.model;

import java.io.File;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * Subclass of {@link File}, which provides a secure (paranoid) filename, that
 * should be valid for the most operating systems around.
 * 
 * @author yschubert
 * 
 */
public class SafeFilenameFile extends File {
	
	private static final long serialVersionUID = -3887471477656613625L;
	
	private static char[] CHARACTERWHITELIST = {'.', ' ', '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '-'};
	
	private static final char FALLBACKCHARACTER = '_';
	
	
	/**
	 * Creates a new File instance by converting the given pathname string into
	 * an abstract pathname. If the given string is the empty string, then the
	 * result is the empty abstract pathname. <br>
	 * <br>
	 * In addition to {@link File}, all 'bad' characters in the pathname are
	 * replaced by a '_'.<br>
	 * <br>
	 * Only allowed characters are a-z, A-Z, 0-9, ., blank, -, _
	 * 
	 * @param pathname A pathname string
	 * @throws NullPointerException - If the pathname argument is null
	 */
	public SafeFilenameFile(final String pathname) {
		super(SafeFilenameFile.getSafeFileName(pathname));
	}
	
	public static String getSafeFileName(final String pathname) {
		String newPathname = pathname;
		for (int i = 0; i < newPathname.length(); i++) {
			char c = newPathname.charAt(i);
			if (!SafeFilenameFile.containedInCharacterWhiteList(c)) {
				newPathname = newPathname.replace(c, SafeFilenameFile.FALLBACKCHARACTER);
			}
		}
		return newPathname;
	}
	
	private static boolean containedInCharacterWhiteList(final char c) {
		for (char element : SafeFilenameFile.CHARACTERWHITELIST) {
			if (c == element) {
				return true;
			}
		}
		return false;
	}
	
	// this is used to generate the character whitelist string
	public static void main(final String[] args) {
		StringBuilder whitelist = new StringBuilder("private static char[] CHARACTERWHITELIST =  {");
		whitelist.append("'");
		whitelist.append(" ");
		whitelist.append("',");
		
		whitelist.append("'");
		whitelist.append("_");
		whitelist.append("',");
		
		for (int i = 'a'; i <= 'z'; i++) {
			whitelist.append("'");
			whitelist.append(String.valueOf((char) i));
			whitelist.append("',");
		}
		for (int i = '0'; i <= '9'; i++) {
			whitelist.append("'");
			whitelist.append(String.valueOf((char) i));
			whitelist.append("',");
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			whitelist.append("'");
			whitelist.append(String.valueOf((char) i));
			whitelist.append("',");
		}
		whitelist.append("'");
		whitelist.append("-");
		whitelist.append("'");
		
		whitelist.append("};");
		
		System.out.println(whitelist);
		
		// StringBuilder testString = new StringBuilder();
		// for (int i = 0; i <= 255; i++) {
		// testString.append(String.valueOf((char) i));
		// }
		//
		// File f = new SafeFilenameFile(testString.toString());
		//
		// System.out.println(f.getAbsoluteFile());
		
	}
}
