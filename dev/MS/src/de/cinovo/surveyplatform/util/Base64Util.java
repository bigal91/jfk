/**
 *
 */
package de.cinovo.surveyplatform.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

/**
 * Copyright 2012 Cinovo AG<br>
 * <br>
 * 
 * @author yschubert
 * 
 */
public class Base64Util {
	
	private static final int BUFFER_SIZE = 4096;
	
	
	public static void encodeFile(final String inputFilePath, final String outputFilePath) throws IOException {
		InputStream input = null;
		OutputStream output = null;
		try {
			byte[] buffer = new byte[Base64Util.BUFFER_SIZE];
			input = new FileInputStream(inputFilePath);
			output = new Base64OutputStream(new FileOutputStream(outputFilePath));
			int n = input.read(buffer, 0, Base64Util.BUFFER_SIZE);
			while (n >= 0) {
				output.write(buffer, 0, n);
				n = input.read(buffer, 0, Base64Util.BUFFER_SIZE);
			}
		} finally {
			if (input != null) {
				input.close();
			}
			if (output != null) {
				output.close();
			}
		}
	}
	
	public static String encodeFile(final String filePath) throws IOException {
		InputStream input = null;
		OutputStream output = null;
		try {
			byte[] buffer = new byte[Base64Util.BUFFER_SIZE];
			input = new FileInputStream(filePath);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			output = new Base64OutputStream(baos);
			int n = input.read(buffer, 0, Base64Util.BUFFER_SIZE);
			while (n >= 0) {
				output.write(buffer, 0, n);
				n = input.read(buffer, 0, Base64Util.BUFFER_SIZE);
			}
			return baos.toString("UTF-8");
		} finally {
			if (input != null) {
				input.close();
			}
			if (output != null) {
				output.close();
			}
		}
	}
	
	public static void decodeFile(final String inputFilePath, final String outputFilePath) throws IOException {
		InputStream input = null;
		OutputStream output = null;
		try {
			byte[] buffer = new byte[Base64Util.BUFFER_SIZE];
			input = new Base64InputStream(new FileInputStream(inputFilePath));
			output = new FileOutputStream(outputFilePath);
			int n = input.read(buffer, 0, Base64Util.BUFFER_SIZE);
			while (n >= 0) {
				output.write(buffer, 0, n);
				n = input.read(buffer, 0, Base64Util.BUFFER_SIZE);
			}
		} finally {
			if (input != null) {
				input.close();
			}
			if (output != null) {
				output.close();
			}
		}
	}
	
	public static String decodeFile(final String filePath) throws IOException {
		InputStream input = null;
		Reader reader = null;
		
		try {
			char[] buffer = new char[Base64Util.BUFFER_SIZE];
			input = new Base64InputStream(new FileInputStream(filePath));
			reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
			
			StringWriter sw = new StringWriter();
			int n;
			while ((n = reader.read(buffer)) != -1) {
				sw.write(buffer, 0, n);
			}
			return sw.toString();
		} finally {
			if (input != null) {
				input.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
	}
	
	public static void main(final String[] args) throws IOException {
		if (args.length == 2) {
			if (args[0].equals("decodeFile")) {
				System.out.println(Base64Util.decodeFile(args[1]));
			}
			if (args[0].equals("encodeFile")) {
				System.out.println(Base64Util.encodeFile(args[1]));
			}
		}
		if (args.length == 3) {
			if (args[0].equals("decodeFile")) {
				Base64Util.decodeFile(args[1], args[2]);
			}
			if (args[0].equals("encodeFile")) {
				Base64Util.encodeFile(args[1], args[2]);
			}
		}
	}
	
}
