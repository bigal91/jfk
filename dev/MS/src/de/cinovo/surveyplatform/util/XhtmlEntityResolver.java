/**
 *
 */
package de.cinovo.surveyplatform.util;

import java.io.FileReader;
import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class XhtmlEntityResolver implements EntityResolver {
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
		String fileName = systemId.substring(systemId.lastIndexOf("/") + 1);
		return new InputSource(new FileReader("./dtd/" + fileName));
	}
	
}
