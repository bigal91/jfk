package de.cinovo.surveyplatform.util;

import java.io.StringWriter;

import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.textile.core.TextileLanguage;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * 
 * Util for providing wiki text formatting
 * 
 * @author yschubert
 * 
 */
public class WikiUtil {
	
	private static MarkupParser parser = new MarkupParser(new TextileLanguage());
	
	
	/**
	 * Converts textile dialect to XHTML
	 * 
	 * @param input
	 * @return XHTML representation of the given textile input
	 * @see http://textile.thresholdstate.com/
	 */
	public static synchronized String parseToHtml(final String input, final boolean noParagraph) {
		return WikiUtil.getHtml(input, noParagraph);
	}
	
	public static synchronized String parseToHtml(final String input) {
		return WikiUtil.getHtml(input, false);
	}
	
	private static String getHtml(final String input, final boolean noParagraph) {
		// synchronized to safe parser from changeings of other threads
		
		StringWriter writer = new StringWriter();
		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
		
		// avoid the <html> and <body> tags
		builder.setEmitAsDocument(false);
		
		builder.setEncoding("utf-8");
		builder.setXhtmlStrict(true);
		String cleanedInput = input.replaceAll("<br>", "<br/>");
		WikiUtil.parser.setBuilder(builder);
		WikiUtil.parser.parse(cleanedInput);
		String string = writer.toString();
		if (noParagraph && string.startsWith("<p>")) {
			return string.substring(3, string.length() - 4);
		}
		return string;
		
	}
	
	public static void main(final String[] args) {
		System.out.println(WikiUtil.getHtml("test & test", true));
	}
}
