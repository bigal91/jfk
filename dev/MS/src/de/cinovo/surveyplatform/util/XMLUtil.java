package de.cinovo.surveyplatform.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * 
 * Copyright 2010 Cinovo AG<br>
 * <br>
 * Marshalling and unmarshalling of objects into and from XML. Uses JAXB
 * Marshaller.
 * 
 * @author yschubert
 * 
 */
public class XMLUtil {
	
	public static String obj2xml(final Object obj, final Class<?> clazz) {
		
		StringWriter stringWriter = new StringWriter();
		try {
			JAXBContext JContext = JAXBContext.newInstance(clazz);
			
			Marshaller marshaller = JContext.createMarshaller();
			
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
			marshaller.marshal(obj, stringWriter);
			
		} catch (Exception e) {
			Logger.errUnexpected(e, null);
		}
		return stringWriter.toString();
	}
	
	public static Object xml2obj(final String xmlString, final Class<?> clazz) {
		try {
			JAXBContext JContext = JAXBContext.newInstance(clazz);
			
			Unmarshaller unmarshaller = JContext.createUnmarshaller();
			
			StringReader stringReader = new StringReader(xmlString);
			return unmarshaller.unmarshal(stringReader);
			
		} catch (Exception e) {
			Logger.errUnexpected(e, null);
		}
		return null;
	}
	
	public static Object xmlFile2obj(final File sourceFile, final Class<?> clazz) {
		try {
			return XMLUtil.xml2obj(XMLUtil.readTextFile(sourceFile), clazz);
		} catch (IOException e) {
			Logger.errUnexpected(e, null);
		}
		return null;
	}
	
	public static boolean obj2XmlFile(final Object obj, final Class<?> clazz, final File targetFile) {
		try {
			XMLUtil.writeTextFile(XMLUtil.obj2xml(obj, clazz), targetFile);
			return true;
		} catch (IOException e) {
			Logger.errUnexpected(e, null);
		}
		
		return false;
	}
	
	private static String readTextFile(final File fileToRead) throws IOException {
		FileInputStream fis = new FileInputStream(fileToRead);
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader br = new BufferedReader(isr);
		
		StringBuilder sb = new StringBuilder();
		String line;
		while (null != (line = br.readLine())) {
			sb.append(line);
			sb.append("\n");
		}
		br.close();
		return sb.toString();
	}
	
	/**
	 * Helper method to write a text file from a string
	 * 
	 * @param content
	 * @param fileToSave
	 * @throws IOException
	 */
	private static void writeTextFile(final String content, final File fileToSave) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileToSave);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);
		bw.write(content);
		bw.flush();
		bw.close();
	}
}
