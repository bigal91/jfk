package util;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;


public class ParamUtil {
	
	private static Pattern HTML_COMMENT_BLOCK = Pattern.compile("<!--.*?-->", Pattern.MULTILINE | Pattern.DOTALL);
	
	public static String stripTags(final String input, final String... whiteList) {
		String strippedInput = input;
		// TODO not working currently!
		// if (whiteList != null) {
		// for (String doNotStrip : whiteList) {
		// strippedInput = strippedInput.replaceAll("\\<" + doNotStrip + "", "TAG__OPEN__" + doNotStrip);
		// strippedInput = strippedInput.replaceAll("\\</" + doNotStrip + "\\>", "TAG__CLOSE__" + doNotStrip);
		// }
		// }
		// strippedInput = strippedInput.replaceAll("\\<[^\\>]*\\>", "");
		// if (whiteList != null) {
		// for (String doNotStrip : whiteList) {
		// strippedInput = strippedInput.replaceAll("TAG__OPEN__" + doNotStrip, "<" + doNotStrip + "");
		// strippedInput = strippedInput.replaceAll("TAG__CLOSE__" + doNotStrip, "</" + doNotStrip + ">");
		// }
		// }
		
		return strippedInput;
	}
	
	public static String getSafeParam(final HttpServletRequest request, final String param, final String... whiteList) {
		String text = HTML_COMMENT_BLOCK.matcher(request.getParameter(param)).replaceAll("");
		return text;
	}
	
	/**
	 * Iterates through each param given and ensures that all params are set in
	 * the request
	 * 
	 * @param request HttpServletRequest object
	 * @param params List of parameternames to check
	 * @return true if all parameters are set in the request. false if any of
	 *         the parameters is not set in the request
	 */
	public static boolean checkAllParamsSet(final HttpServletRequest request, final String... params) {
		for (String param : params) {
			if (request.getParameter(param) == null) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Iterates through each param given and ensures that all params are set and
	 * non empty in the request
	 * 
	 * @param request HttpServletRequest object
	 * @param params List of parameternames to check
	 * @return true if all parameters are set in the request and are not empty
	 *         (paramValue != ""). false if any of the parameters is not set in
	 *         the request or is empty.
	 */
	public static boolean ensureAllParamsSetAndNotEmpty(final HttpServletRequest request, final String... params) {
		for (String param : params) {
			if (request.getParameter(param) == null) {
				return false;
			} else if (request.getParameter(param).equals("")) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Converts a parameter to an integer value.<br>
	 * <br>
	 * Only use this method, when expecting values other than 0 (zero) from the
	 * parameter (see explanation in "Returns")!
	 * 
	 * @param request HttpServletRequest object
	 * @param param Name of the parameter to convert
	 * @return The integer representation of the given parameter. 0, if the
	 *         parameter could not be gathered from the request or the
	 *         Integer.parse(s) throws a {@link NumberFormatException}. (The
	 *         exception is not rethrown by this method, instead 0 is returned)
	 */
	public static int getSafeIntFromParam(final HttpServletRequest request, final String param) {
		int intValue = 0;
		try {
			String stringValue = request.getParameter(param);
			intValue = Integer.parseInt(stringValue);
		} catch (NumberFormatException nfe) {
			// ignore
		}
		return intValue;
	}
	
	/**
	 * Converts a parameter to a long value.<br>
	 * <br>
	 * Only use this method, when expecting values other than 0 (zero) from the
	 * parameter (see explanation in "Returns")!
	 * 
	 * @param request HttpServletRequest object
	 * @param param Name of the parameter to convert
	 * @return The long representation of the given parameter. 0, if the
	 *         parameter could not be gathered from the request or the
	 *         InLongteger.parse(s) throws a {@link NumberFormatException}. (The
	 *         exception is not rethrown by this method, instead 0 is returned)
	 */
	public static long getSafeLongFromParam(final HttpServletRequest request, final String param) {
		long longValue = 0;
		try {
			String stringValue = request.getParameter(param);
			longValue = Long.parseLong(stringValue);
		} catch (NumberFormatException nfe) {
			// ignore
		}
		return longValue;
	}
}
