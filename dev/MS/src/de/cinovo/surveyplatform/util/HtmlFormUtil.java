package de.cinovo.surveyplatform.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.StringStringPair;

/**
 * 
 * Copyright 2010 Cinovo AG
 * 
 * Utility class for generating HTML input fields
 * 
 * @author yschubert
 * 
 * 
 */
public class HtmlFormUtil {
	
	public enum Type {
		TEXTAREA, TEXTFIELD, PASSWORD, BOOLEAN, EMAIL, HIDDEN, URL, DATE, DATETIME, LIST
	}
	
	
	/**
	 * Create a inputfield or text representation of a field.
	 * 
	 * @param editable if true, a inputfield is generated according the given
	 *            type, to let the user edit the value of the field
	 * @param name name of the input field (this name is used when sending the
	 *            form)
	 * @param type Type of the inputfield.
	 * @param cssClass Use this to add a custom css to the input
	 * @param value value of the input
	 * @return HTML represenation of a field, controlled by the editable field
	 */
	@SuppressWarnings("unchecked")
	public static String getEditableProperty(final boolean editable, final String name, final Type type, final String cssClass, final Object value) {
		
		if (editable) {
			String editableValue = "";
			switch (type) {
			case TEXTFIELD:
				editableValue = "<input type=\"text\" name=\"" + name + "\" id=\"" + name + "\" value=\"" + (String) value + "\" class=\"" + cssClass + "\" />";
				break;
			case TEXTAREA:
				editableValue = "<a href=\"http://textism.com/tools/textile/?sample=2\" style=\"display:block; text-align: right;\">Bearbeitungshinweise (Textile)</a><textarea class=\"tinyEditor\" name=\"" + name + "\" id=\"" + name + "\" class=\"" + cssClass + "\">" + (String) value + "</textarea>";
				break;
			case LIST:
				editableValue = "<select name=\"" + name + "\" id=\"" + name + "\" class=\"" + cssClass + "\">";
				for (String item : (List<String>) value) {
					if (item.startsWith("_selected_")) {
						editableValue += "<option selected=\"selected\">" + item.substring(10) + "</option>";
					} else {
						editableValue += "<option>" + item + "</option>";
					}
				}
				editableValue += "</select>";
				break;
			case PASSWORD:
				editableValue = "<input type=\"password\" name=\"" + name + "\" id=\"" + name + "\" value=\"" + (String) value + "\" class=\"" + cssClass + "\" />";
				break;
			case HIDDEN:
				editableValue = "<input type=\"hidden\" name=\"" + name + "\" id=\"" + name + "\" value=\"" + (String) value + "\"/>";
				break;
			case BOOLEAN:
				StringStringPair checkBoxValuePair = (StringStringPair) value;
				String checkBoxValue = checkBoxValuePair.getKey();
				String checkedState = "";
				if (checkBoxValue.startsWith("_selected_")) {
					checkBoxValue = checkBoxValue.substring(10);
					checkedState = " checked=\"checked\"";
				}
				
				editableValue = "<input type=\"checkbox\" name=\"" + name + "\" id=\"" + name + "\" value=\"" + checkBoxValue + "\" class=\"" + cssClass + "\"" + checkedState + " />" + checkBoxValuePair.getValue();
				break;
			default:
				editableValue = (String) value;
				throw new RuntimeException("Type " + type + " not known or not implemented!");
			}
			return editableValue;
		} else {
			if (type.equals(Type.TEXTAREA)) {
				return WikiUtil.parseToHtml((String) value);
			}
			return (String) value;
		}
	}
	
	/**
	 * Creates a Textfield that is editable inplace by clicking on an icon
	 * besides the text
	 * 
	 * @param id id of the text element
	 * @param name name of the textfield
	 * @param value value of the text element
	 * @param saveActionTemplate Template of javascriptcode that is executed
	 *            when clicking on save. You can use the replacements {ID},
	 *            {NAME} and {VALUE} for the template.
	 * @param pairs Theys are &gt;String, String&lt; Pairs to add to the
	 *            replacement of the saveActionTemplate
	 * @return Fragment of HTML containing the markup for the textelement and
	 *         related javascript snippet
	 */
	public static String getInplaceEditableTextfield(final String id, final String name, final String value, final String saveActionTemplate, final String hint, final StringStringPair... pairs) {
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("ID", id);
		replacements.put("NAME", name);
		replacements.put("VALUE", value);
		
		for (StringStringPair kvp : pairs) {
			replacements.put(kvp.getKey(), kvp.getValue());
		}
		if (saveActionTemplate != null) {
			replacements.put("SAVEACTION", TemplateUtil.getTemplate(saveActionTemplate, replacements));
		}
		
		String waterMark = "";
		if ((value == null) && (hint != null)) {
			waterMark = "<i id=\"" + id + "inplaceEditHint\">" + hint + "</i>";
		}
		
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dynamicEditableTextField.html", replacements) + waterMark;
	}
	
	/**
	 * Creates a Textfield that is editable inplace by clicking on an icon
	 * besides the text
	 * 
	 * @param id id of the text element
	 * @param name name of the textfield
	 * @param value value of the text element
	 * @param saveActionTemplate Template of javascriptcode that is executed
	 *            when clicking on save. You can use the replacements {ID},
	 *            {NAME} and {VALUE} for the template.
	 * @param pairs Theys are &gt;String, String&lt; Pairs to add to the
	 *            replacement of the saveActionTemplate
	 * @return Fragment of HTML containing the markup for the textelement and
	 *         related javascript snippet
	 */
	public static String getInplaceEditableTextfield(final String id, final String name, final String value, final String saveActionTemplate, final StringStringPair... pairs) {
		return HtmlFormUtil.getInplaceEditableTextfield(id, name, value, saveActionTemplate, null, pairs);
	}
	
	/**
	 * Creates a Textfield that is editable inplace by clicking on an icon
	 * besides the text
	 * 
	 * @param id id of the text element
	 * @param name name of the textfield
	 * @param value value of the text element
	 * @param saveActionTemplate Template of javascriptcode that is executed
	 *            when clicking on save. You can use the replacements {ID},
	 *            {NAME} and {VALUE} for the template.
	 * @param pairs Theys are &gt;String, String&lt; Pairs to add to the
	 *            replacement of the saveActionTemplate
	 * @return Fragment of HTML containing the markup for the textelement and
	 *         related javascript snippet
	 */
	public static String getDynamicEditableComboBox(final String id, final String name, final List<StringStringPair> options, final String selectedOption, final String saveActionTemplate, final StringStringPair... pairs) {
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("ID", id);
		replacements.put("NAME", name);
		
		StringBuilder optionsSb = new StringBuilder();
		for (StringStringPair pair : options) {
			optionsSb.append("<option value=\"" + pair.getKey() + "\"" + (pair.getKey().equals(selectedOption) ? "selected=\"selected\"" : "") + ">" + pair.getValue() + "</option>");
			if (pair.getKey().equals(selectedOption)) {
				replacements.put("VALUE", pair.getValue());
			}
		}
		replacements.put("OPTIONS", optionsSb.toString());
		
		for (StringStringPair kvp : pairs) {
			replacements.put(kvp.getKey(), kvp.getValue());
		}
		if (saveActionTemplate != null) {
			replacements.put("SAVEACTION", TemplateUtil.getTemplate(saveActionTemplate, replacements));
		}
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dynamicEditableComboBox.html", replacements);
	}
	
	/**
	 * Creates a Textfield that is editable inplace by clicking on an icon
	 * besides the text
	 * 
	 * @param id id of the text element
	 * @param name name of the textfield
	 * @param value value of the text element
	 * @param saveActionTemplate Template of javascriptcode that is executed
	 *            when clicking on save. You can use the replacements {ID},
	 *            {NAME} and {VALUE} for the template.
	 * @param pairs Theys are &gt;String, String&lt; Pairs to add to the
	 *            replacement of the saveActionTemplate
	 * @return Fragment of HTML containing the markup for the textelement and
	 *         related javascript snippet
	 */
	public static String getDynamicEditableCheckBoxes(final String id, final String name, final List<StringStringPair> options, final List<String> selectedOptions, final String saveActionTemplate, final StringStringPair... pairs) {
		
		String sanitizedID = id.replace("[]", "");
		
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("ID", sanitizedID);
		replacements.put("NAME", name);
		
		StringBuilder optionsSb = new StringBuilder();
		StringBuilder selectedOptionsSb = new StringBuilder();
		selectedOptionsSb.append("<ul>");
		for (StringStringPair pair : options) {
			boolean selected = false;
			if (selectedOptions.contains(pair.getKey())) {
				selectedOptionsSb.append("<li>" + pair.getValue() + "</li>");
				selected = true;
			}
			optionsSb.append("<li><input type=\"checkbox\" class=\"" + sanitizedID + "Item\" name=\"" + name + "\" value=\"" + pair.getKey() + "\"" + (selected ? "checked=\"checked\"" : "") + " />" + pair.getValue() + "</li>");
		}
		selectedOptionsSb.append("</ul>");
		
		replacements.put("VALUE", selectedOptionsSb.toString());
		replacements.put("OPTIONS", optionsSb.toString());
		
		for (StringStringPair kvp : pairs) {
			replacements.put(kvp.getKey(), kvp.getValue());
		}
		if (saveActionTemplate != null) {
			replacements.put("SAVEACTION", TemplateUtil.getTemplate(saveActionTemplate, replacements));
		}
		
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/dynamicEditableCheckBoxes.html", replacements);
	}
	
	/**
	 * Creates a Textfield that is editable inplace by clicking on an icon
	 * besides the text
	 * 
	 * @param id id of the text element
	 * @param name name of the textfield
	 * @param value value of the text element
	 * @param saveActionTemplate Template of javascriptcode that is executed
	 *            when clicking on save. You can use the replacements {ID},
	 *            {NAME} and {VALUE} for the template.
	 * @param pairs Theys are &gt;String, String&lt; Pairs to add to the
	 *            replacement of the saveActionTemplate
	 * @return Fragment of HTML containing the markup for the textelement and
	 *         related javascript snippet
	 */
	public static String getComboBox(final String id, final String name, final List<StringStringPair> options, final String selectedOption) {
		StringBuilder optionsSb = new StringBuilder();
		for (StringStringPair pair : options) {
			optionsSb.append("<option value=\"" + pair.getKey() + "\"" + (pair.getKey().equals(selectedOption) ? "selected=\"selected\"" : "") + ">" + pair.getValue() + "</option>");
		}
		return "<select name=\"" + name + "\" id=\"" + id + "\">" + optionsSb.toString() + "</select>";
	}
	
	/**
	 * Creates a HTML representation of a button
	 * 
	 * @param name Name of the Button
	 * @param value Text value of the button
	 * @return
	 */
	public static String getButton(final String name, final String value) {
		return "<input class=\"styledButtonMedium form\" type=\"submit\" name=\"" + name + "\" id=\"" + name + "\" value=\"" + value + "\" />";
	}
	
	/**
	 * Creates the begin tag of a HTML form. Keep in mind to use
	 * <code>endForm()</code> to close the form again!
	 * 
	 * @param target
	 * @return &lt;form method="post" action="target"&gt;
	 */
	public static String beginPostForm(final String target) {
		return "<form method=\"post\" action=\"" + target + "\">";
	}
	
	/**
	 * End a HTML form. Keep in mind that a form must be open BEFORE using the
	 * result of this method!
	 * 
	 * @return &lt;/form&gt;
	 */
	public static String endForm() {
		return "</form>";
	}
	
	/**
	 * @param message
	 * @return A span containing an error message
	 */
	public static String getErrorMessage(final String message) {
		return "<span style=\"color: red; font-weight: bold;\">" + message + "</span>";
	}
	
	/**
	 * This method will escape all html characters
	 * 
	 * @param s string to escape
	 * @return escaped string
	 */
	public static String escapeHtmlFull(final String s) {
		if (s == null) {
			return "";
		}
		StringBuilder b = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z')) || ((ch >= '0') && (ch <= '9'))) {
				// safe
				b.append(ch);
			} else if (Character.isWhitespace(ch)) {
				// paranoid version: whitespaces are unsafe - escape
				// conversion of (int)ch is naive
				b.append("&#").append((int) ch).append(";");
			} else if (Character.isISOControl(ch)) {
				// paranoid version:isISOControl which are not isWhitespace
				// removed !
				// do nothing do not include in output !
			} else if (Character.isHighSurrogate(ch)) {
				int codePoint;
				if (((i + 1) < s.length()) && Character.isSurrogatePair(ch, s.charAt(i + 1)) && Character.isDefined(codePoint = (Character.toCodePoint(ch, s.charAt(i + 1))))) {
					b.append("&#").append(codePoint).append(";");
				} else {
					// log("bug:isHighSurrogate");
				}
				i++; // in both ways move forward
			} else if (Character.isLowSurrogate(ch)) {
				// log("bug:isLowSurrogate");
				i++; // move forward,do nothing do not include in output !
			} else {
				if (Character.isDefined(ch)) {
					// paranoid version
					// the rest is unsafe, including <127 control chars
					b.append("&#").append((int) ch).append(";");
				}
				// do nothing do not include undefined in output!
			}
		}
		return b.toString();
	}
}
