package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import constants.EnvConfiguration;


public class HTMLUtil {
	
	private final static Map<String, String> templateCache = new ConcurrentHashMap<String, String>();
	
	private final static boolean templateCacheEnabled = true;
	
	
	/**
	 * Clears the cache
	 */
	public static synchronized void clearCache() {
		HTMLUtil.templateCache.clear();
	}
	
	/**
	 * Get the content of template, where the placeholders are replaced by the
	 * data given in replacements. <br>
	 * Hint: The templates are cached in memory with the first call to reduce IO
	 * load of the server. Key of a entry is the given filePath.<br>
	 * 
	 * Limiters of the placeholder are { and } <br>
	 * <br>
	 * This utility supports recursive inclusion of the templates. For
	 * accomplishing this, use e.g. {include:&lt;path&gt;}. Path is a path
	 * expression of the file to include relative to <code>filePath</code>
	 * 
	 * @param filePath Path of the template
	 * @param replacements Map of &lt;placeholder, content&gt; pairs. The
	 *            placeholder's format MUST comply to the regular expression:
	 *            [A-Z0-9_-]+ e.g.: USER_NAME or OPTION1 or TE-ST
	 * @param filePath
	 * @param replacements
	 * @return
	 */
	public static synchronized String getHTMLFile(final String filePath, final Map<String, String> replacements) {
		return HTMLUtil.getHTMLFile(filePath, replacements, "\\{", "\\}");
	}
	
	/**
	 * Get the content of template, where the placeholders are replaced by the
	 * data given in replacements. <br>
	 * Hint: The templates are cached in memory with the first call to reduce IO
	 * load of the server. Key of a entry is the given filePath.<br>
	 * <br>
	 * This utility supports recursive inclusion of the templates. For
	 * accomplishing this, use e.g. {include:&lt;path&gt;}. Path is a path
	 * expression of the file to include relative to <code>filePath</code>
	 * 
	 * @param filePath Path of the template
	 * @param replacements Map of &lt;placeholder, content&gt; pairs. The
	 *            placeholder's format MUST comply to the regular expression:
	 *            [A-Z0-9_-]+ e.g.: USER_NAME or OPTION1 or TE-ST
	 * @param limiterLeft left side of the placeholder's limiter
	 * @param limiterRight right side of the placeholder's limiter
	 * @return
	 */
	public static synchronized String getHTMLFile(final String filePath, final Map<String, String> replacements, final String limiterLeft, final String limiterRight) {
		String content = "";
		try {
			
			if (HTMLUtil.templateCacheEnabled && HTMLUtil.templateCache.containsKey(filePath)) {
				content = HTMLUtil.templateCache.get(filePath);
				// logger.debug("Template cache match: " + filePath);
			} else {
				final StringBuilder fileContent = new StringBuilder();
				final BufferedReader br = new BufferedReader(new FileReader(filePath));
				String line = "";
				while ((line = br.readLine()) != null) {
					fileContent.append(line + "\n");
				}
				br.close();
				content = fileContent.toString();
				
				Map<String, String> includeMap = new HashMap<String, String>();
				// ** process include instruction: {include:filename}
				{
					Pattern pattern = Pattern.compile(limiterLeft + "include:([^" + limiterRight + "]+)" + limiterRight);
					Matcher matcher = pattern.matcher(content);
					File currentFile = new File(filePath);
					
					while (matcher.find()) {
						String fileToInclude = matcher.group(1);
						includeMap.put(limiterLeft + "include:" + fileToInclude + limiterRight, HTMLUtil.getHTMLFile(currentFile.getParentFile().getAbsolutePath() + File.separator + fileToInclude, null));
					}
				}
				// **************
				
				// ** process feature include instruction: {feature:FEATURE:filename}
//				{
//					Pattern pattern = Pattern.compile(limiterLeft + "feature:([^:]+):([^" + limiterRight + "]+)" + limiterRight);
//					Matcher matcher = pattern.matcher(content);
//					File currentFile = new File(filePath);
//					
//					while (matcher.find()) {
//						String featureToCheck = matcher.group(1);
//						String fileToInclude = matcher.group(2);
//						String replacement = limiterLeft + "feature:" + featureToCheck + ":" + fileToInclude + limiterRight;
//						if (FeatureToggle.isEnabled(Feature.valueOf(featureToCheck))) {
//							includeMap.put(replacement, HTMLUtil.getTemplate(currentFile.getParentFile().getAbsolutePath() + File.separator + fileToInclude, null));
//						} else {
//							includeMap.put(replacement, "");
//						}
//					}
//				}
				// **************
				
				// ** process help instruction: {help:helpID}
//				{
//					Pattern pattern = Pattern.compile(limiterLeft + "help:([^" + limiterRight + "]+)" + limiterRight);
//					Matcher matcher = pattern.matcher(content);
//					while (matcher.find()) {
//						String helpID = matcher.group(1);
//						includeMap.put(limiterLeft + "help:" + helpID + limiterRight, ContextHelpProvider.getInstance().getHelpLink(helpID, "Click to get detailed info", "", ""));
//					}
//				}
				// **************
				
				// TODO (URL BASE as second param)
				content = content.replaceAll("\\{\\$BASEURL\\$\\}", "");
				
				for (Entry<String, String> entry : includeMap.entrySet()) {
					String replacement = entry.getValue().replaceAll("\\$", "UP_DOLLAR_UP");
					content = content.replaceAll(entry.getKey(), replacement);
				}
				// **************
				
//				if (HTMLUtil.templateCacheEnabled) {
//					String xmlcomp = compressor.compress(content);
//					HTMLUtil.templateCache.put(filePath, xmlcomp);
//					
//					//TemplateUtil.templateCache.put(filePath, content);
//					// logger.debug("Template cache push: " + filePath +
//					// ", Now " + templateCache.size() + " entries.");
//				}
				
			}
			
			if (replacements != null) {
				String replacement = "";
				for (Entry<String, String> entry : replacements.entrySet()) {
					// $ has to be replaced, as it will be recognized by the
					// regular expression engine as reference, what is not
					// wanted here
					// Pattern.quote(s) does not work in this case!
					replacement = entry.getValue();
					if (replacement != null) {
						replacement = replacement.replaceAll("\\$", "UP_DOLLAR_UP");
					} else {
						replacement = "";
					}
					
					content = content.replaceAll(limiterLeft + entry.getKey().toUpperCase() + limiterRight, replacement);
				}
				// undo the replacement of the $
				content = content.replaceAll("UP_DOLLAR_UP", "\\$");
			}
		} catch (Exception e) {
			// logger.error(e.getMessage(), e);
			return e.getMessage();
		}
		// clear content from not replaced placeholders
		Pattern pattern = Pattern.compile(limiterLeft + "[A-Z0-9_-]+" + limiterRight);
		Matcher matcher = pattern.matcher(content);
		content = matcher.replaceAll("");
		
		return content;
	}
	
	/**
	 * Makes a direct replacement in a string. Should not be used as there is no
	 * caching.
	 * 
	 * @param replacements
	 * @param content
	 * @param limiterLeft
	 * @param limiterRight
	 * @return replaced string
	 */
	public static synchronized String directReplacement(String content, final Map<String, String> replacements, final String limiterLeft, final String limiterRight) {
		if (replacements != null) {
			String replacement = "";
			for (Entry<String, String> entry : replacements.entrySet()) {
				// $ has to be replaced, as it will be recognized by the regular
				// expression engine as reference, what is not wanted here
				// Pattern.quote(s) does not work in this case!
				replacement = entry.getValue();
				if (replacement != null) {
					replacement = replacement.replaceAll("\\$", "UP_DOLLAR_UP");
				} else {
					replacement = "";
				}
				content = content.replaceAll(limiterLeft + entry.getKey().toUpperCase() + limiterRight, replacement);
			}
			// undo the replacement of the $
			content = content.replaceAll("UP_DOLLAR_UP", "\\$");
		}
		
		return content;
	}
}
