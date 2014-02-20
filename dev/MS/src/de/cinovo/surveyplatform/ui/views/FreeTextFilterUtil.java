/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import de.cinovo.surveyplatform.constants.Paths;
import de.cinovo.surveyplatform.model.Topic;
import de.cinovo.surveyplatform.model.question.Answer;
import de.cinovo.surveyplatform.model.question.TopicCombination;
import de.cinovo.surveyplatform.util.AuthUtil;
import de.cinovo.surveyplatform.util.Logger;
import de.cinovo.surveyplatform.util.PartsUtil;
import de.cinovo.surveyplatform.util.TemplateUtil;


/**
 * Copyright 2011 Cinovo AG<br><br>
 * @author ubreitenbuecher
 *
 */
public class FreeTextFilterUtil {
	
	private static final String UTF_8 = "UTF-8";
	private String id;
	private AnswerContainer container;
	private List<Topic> topics;
	private int questionRefId;
	
	public FreeTextFilterUtil(final int questionRefId) {
		this.questionRefId = questionRefId;
		this.id = UUID.randomUUID().toString().replace("-", "");
		
	}
	
	public FreeTextFilterUtil(final int questionRefId, final String boxId) {
		this.questionRefId = questionRefId;
		this.id = boxId;
	}
	
	public void setAnswers(final AnswerContainer answerContainer) {
		this.container = answerContainer;
	}
	
	public void setTopics(final List<Topic> topics) {
		this.topics = topics;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getFilterBox(final int count) {
		
		Map<String, String> replacements = new HashMap<String, String>();
		StringBuilder str = new StringBuilder();
		
		double max = container.getMaximumCount();
		double halfMax = max / 2;
		
		for (Entry<String, Integer> entry : container.getWordCountHashMap().entrySet()) {
			double val = entry.getValue();
			// compare "equals" by ints, not doubles
			int val_integer = (int) val;
			int halfMax_integer = (int) halfMax;
			int frequency = -(int) (70 - ((val / max) * 70));
			String freqBarColor = "frequencyBar_white";
			if (val < halfMax) {
				freqBarColor = "frequencyBar_blue";
			} else if (val_integer == halfMax_integer) {
				freqBarColor = "frequencyBar_darkblue";
			} else if (val > halfMax) {
				freqBarColor = "frequencyBar_white";
			}
			str.append("<a class='item" + this.id + "' href='javascript:void(0);'><span class='" + freqBarColor + " frequencyBar' style='background-position: " + frequency + "px 0px;'>" + entry.getValue() + "</span><span class='itemText itemText" + this.id + "'>" + entry.getKey() + "</span></a>");
		}
		
		StringBuilder topicsStr = new StringBuilder();
		if (topics != null) {
			for (Topic topic : topics) {
				topicsStr.append(getTopicItem(topic));
			}
		}
		replacements.put("TOPICS", topicsStr.toString());
		replacements.put("ITEMS", str.toString());
		replacements.put("DATAMAPSTERLOGO", PartsUtil.getIcon("DATAMAPSTERICON", ""));
		replacements.put("QUESTIONID", questionRefId + "");
		replacements.put("ID", this.id);
		replacements.put("COUNT", count + "");
		
		replacements.put("TOPIC_DROP_TARGET", "<div class=\"topicDropTarget createTopic" + this.id + "\">DRAG AND DROP INDIVIDUAL STATEMENTS HERE TO CREATE A NEW TOPIC</div>");
		
		return TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/freeTextFilterBox.html", replacements);
	}
	
	public StringBuilder getTopicItem(final Topic topic) {
		StringBuilder topicItem = new StringBuilder();
		List<Answer> answerList = topic.getAnswers();
		StringBuilder answerComboIds = new StringBuilder();
		for (Answer ans : answerList) {
			for (TopicCombination c : ans.getTopicCombinations()) {
				answerComboIds.append("::" + c.getAnswerId() + "_" + c.getCutBegin() + "_" + c.getCutEnd() + "_" + ans.getId());
			}
		}
		
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("ANSWER_COUNT", answerList.size() + "");
		replacements.put("ID", this.id);
		replacements.put("COMBINATIONIDS", answerComboIds.toString());
		replacements.put("TOPIC_TITLE", topic.getTitle());
		replacements.put("TOPIC_ID", topic.getId() + "");
		topicItem.append(TemplateUtil.getTemplate(Paths.TEMPLATEPATH + "/topicItem.html", replacements));
		topicItem.append("<script type=\"text/javascript\">$(function() {");
		topicItem.append("$('#topic" + topic.getId() + "').parent().droppable({");
		topicItem.append("hoverClass: 'ui-state-active',");
		topicItem.append("drop: function(event, ui) {");
		topicItem.append("addAnswerToTopic" + this.id + "(event, ui);");
		topicItem.append("}");
		topicItem.append("});");
		
		topicItem.append("});");
		topicItem.append("</script>");
		return topicItem;
	}
	
	public String getMd5FromAnswer(final String answer) {
		try {
			return "h" + AuthUtil.md5(URLDecoder.decode(answer.replace("%", "__PROZENT__").replace("\n", "").replace("\r", ""), UTF_8).replace("__PROZENT__", "%"));
		} catch (UnsupportedEncodingException uee) {
			Logger.err("Unknown encoding: " + UTF_8, uee);
		}
		return "";
	}
}
