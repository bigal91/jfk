/**
 *
 */
package de.cinovo.surveyplatform.ui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.cinovo.surveyplatform.model.question.Answer;

/**
 * Copyright 2011 Cinovo AG<br>
 * <br>
 * 
 * @author ablehm
 * 
 *         Holds a filtered List of important words, shown
 *         in the word analysis of the dataMapster.
 */
public class AnswerContainer {
	
	// sorted in alphabetical order, please keep it up
	// @content: 500 most commonly used english words and first numbers
	// @still-to-check: own lists, existing surveys, other...
	private static List<String> blackList = Arrays.asList(new String[] { //
			"", // empty word
			"-", //
			"+", //
			"*", //
			"#", //
			"%", //
			"&", //
			"<", //
			">", //
			"=", //
			"a", "able", "about", "above", "add", "after", "again", "against", "ago", "all", "also", "always", "am", "among", "an", "any", "and", "anyways", "are", "as", "at", //
			"back", "bad", "be", "been", "begin", "began", "behind", "best", "better", "between", "because", "before", "bring", "brought", "but", "by", //
			"came", "can", "can't", "cant", "cause", "caused", "cept", "certain", "close", "closed", "come", "contain", "contained", "could", "cover", "covered", //
			"day", "did", "do", "does", "done", "don't", "dont", "during", //
			"each", "early", "eight", "else", "end", "ended", "enough", "ever", "every", //
			"false", "few", "fine", "five", "for", "four", "from", //
			"gave", "get", "give", "go", "good", "got", "great", //
			"had", "has", "have", "he", "her", "here", "him", "his", "how", //
			"i", "if", "im", "in", "is", "it", "itis", "it's", "itself", //
			"just", //
			"keep", "kind", "knew", "know", "known", //
			"let", "like", "liked", "lot", "lots", //
			"made", "make", "main", "many", "may", "maybe", "me", "might", "more", "most", "much", "must", "my", //
			"never", "new", "next", "nine", "no", "nothing", "now", //
			"of", "off", "often", "oh", "on", "one", "only", "or", "other", "our", "ours", "out", "over", "own", //
			"perhaps", "please", "put", //
			// q is not blacklisted yet
			"ready", "really", //
			"said", "same", "saw", "say", "says", "see", "seem", "self", "set", "seven", "several", "she", "should", "show", "since", "six", "so", "some", "soon", "such", "sure", //
			"take", "ten", "than", "that", "the", "their", "them", "then", "there", "these", "they", "thing", "think", "this", "those", "though", "thought", "three", "through", "to", "too", "took", "total", "totally", "true", "truely", "try", "two", //
			"u", "under", "until", "up", "us", "usual", "use", //
			"various", "very", //
			"want", "was", "we", "well", "went", "were", "what", "when", "where", "which", "while", "who", "whole", "why", "will", "with", "word", "words", "would", //
			// x is not blacklisted yet
			"yay", "yes", "yet", "you", "you're", "your", "youre", "yours", //
			"zero"//
	});
	
	List<Answer> answers = new ArrayList<Answer>();
	Map<String, Integer> wordCount = new HashMap<String, Integer>();
	Map<String, Integer> answerCount = new HashMap<String, Integer>();
	boolean needReprocessingAnswers = false;
	
	
	public AnswerContainer() {
		
	}
	
	public AnswerContainer(final List<Answer> answers) {
		for (Answer iter : answers) {
			this.addAnswer(iter);
		}
	}
	
	public void addAnswer(final Answer answer) {
		if (!answer.getAnswer().isEmpty()) {
			answers.add(answer);
			needReprocessingAnswers = true;
		}
	}
	
	
	/** inner class to do sorting of the map **/
	private class ValueComparer implements Comparator<String> {
		private Map<String, Integer> _data = null;
		
		public ValueComparer(final Map<String, Integer> data) {
			super();
			_data = data;
		}
		
		public int compare(final String o1, final String o2) {
			Integer e1 = _data.get(o1);
			Integer e2 = _data.get(o2);
			
			if (e1.compareTo(e2) == 0) {
				return -1;
			}
			return -1 * e1.compareTo(e2);
		}
	}
	
	
	public Map<String, Integer> getAnswerCountHashMap() {
		if (needReprocessingAnswers) {
			answerCount.clear();
			
			for (Answer answer : answers) {
				if (answerCount.containsKey(answer)) {
					answerCount.put(answer.getAnswer(), answerCount.get(answer) + 1);
				} else {
					answerCount.put(answer.getAnswer(), 1);
				}
			}
		}
		// Sort by value
		SortedMap<String, Integer> sortedData = new TreeMap<String, Integer>(new ValueComparer(answerCount));
		sortedData.putAll(answerCount);
		
		return sortedData;
		
	}
	
	/**
	 * Creates and returns a sorted HashMap from the given
	 * text-type answers in the form: <String, Integer>, where
	 * String is an important word and Integer the amount of times
	 * it was found in the answers List.
	 * A smart-word-filter takes care of bugs.
	 * 
	 * @return the sorted HashMap
	 */
	public Map<String, Integer> getWordCountHashMap() {
		int wordCountLimit = 0;
		if (needReprocessingAnswers) {
			wordCount.clear();
			
			// calculate the "striking-word-factor" (a percantage deciding
			// whether to show or not so show a word in list)
			double showWordFactor = getStrikingFactor();
			wordCountLimit = (int) (showWordFactor * answers.size());
			
			// Build Temp_HashMap of all Words (no filter yet)
			for (Answer answer : answers) {
				String[] singleWordArray = answer.getAnswer().split(" ");
				for (String singleWord : singleWordArray) {
					String singleWordNormalized = normalizeSingleWord(singleWord);
					if (wordCount.containsKey(singleWordNormalized)) {
						wordCount.put(singleWordNormalized, wordCount.get(singleWordNormalized) + 1);
					} else {
						wordCount.put(singleWordNormalized, 1);
					}
				}
			}
			// Now check if any importantWord groups are split
			// e.g. job and jobs should not be in two seperate spots
			// instead put job and jobs in one word-class "job" and add
			// their word counts together...
			List<String> alreadyRemovedWords = new ArrayList<String>();
			Map<String, Integer> tempCount = new HashMap<String, Integer>();
			for (Object iter : wordCount.keySet().toArray()) {
				String iteratedString = (String) iter;
				// the for loop works with an old copy of wordCount,
				// while elements of wordCount are removed. the array:
				// alrdyRemovedWords helps to prevent NullPointerExceptions
				if (!alreadyRemovedWords.contains(iter)) {
					if (wordCount.get(iter) >= wordCountLimit) {
						Map<String, Integer> matchedMap = new HashMap<String, Integer>();
						switch (iteratedString.length()) {
						case 0:
						case 1:
						case 2:
							// cant be an important word
							break;
						case 3:
							// nothing to do, since min-length
							// of a meaningful word is 3 (no cuts
							// allowed)
							break;
						case 4:
							// try cut-off up to: last letter
							matchedMap = getOtherMatches(iteratedString, 1, wordCount);
							readMatchedMap(matchedMap, tempCount, alreadyRemovedWords, iteratedString);
							break;
						case 5:
							// try cut-off up to: last two letters
							matchedMap = getOtherMatches(iteratedString, 2, wordCount);
							readMatchedMap(matchedMap, tempCount, alreadyRemovedWords, iteratedString);
							break;
						case 6:
							// try cut-off up to: last three letters
							matchedMap = getOtherMatches(iteratedString, 3, wordCount);
							readMatchedMap(matchedMap, tempCount, alreadyRemovedWords, iteratedString);
							break;
						default:
							// try cut-off up to: last four letters
							// (maxlength of a suffix)
							matchedMap = getOtherMatches(iteratedString, 4, wordCount);
							readMatchedMap(matchedMap, tempCount, alreadyRemovedWords, iteratedString);
							break;
						}
					}
				}
			}
			// tempCount is now the complement of wordCount
			// it contains all words that needed to be
			// re-united in one class, while those single
			// word-classes, we have been uniting int tempCount
			// have been removed from wordCount
			wordCount.putAll(tempCount);
		}
		
		// Remove Black-Listed-Words
		for (Object iter : wordCount.keySet().toArray()) {
			if (blackList.contains(iter)) {
				wordCount.remove(iter);
			}
		}
		
		// Remove unimportant words
		for (Object iter : wordCount.keySet().toArray()) {
			if (wordCount.get(iter) < wordCountLimit) {
				wordCount.remove(iter);
			}
		}
		
		// Sort by value
		SortedMap<String, Integer> sortedData = new TreeMap<String, Integer>(new ValueComparer(wordCount));
		sortedData.putAll(wordCount);
		
		return sortedData;
	}
	
	/**
	 * @param matchedMap
	 * @param wordCount2
	 * @param tempCount
	 * 
	 *            Reads the info from the matchedMap. if the map is empty,
	 *            no changes will be done. If not empty: the content of the
	 *            matchedMap is read, removed from the global wordCount and
	 *            added to the local tempCount.
	 *            Finally tempCount is the complement of wordCount and holds
	 *            the complete, united representation of a single word-class.
	 */
	private void readMatchedMap(final Map<String, Integer> matchedMap, final Map<String, Integer> tempCount, final List<String> alrdyRemoved, final String currentIterString) {
		// iterate matchedMap and get content
		// united into tempCount
		int addWordCounts = 0;
		if (!matchedMap.isEmpty()) {
			for (Object iterator : matchedMap.keySet().toArray()) {
				addWordCounts += matchedMap.get(iterator);
				wordCount.remove(iterator);
				alrdyRemoved.add((String) iterator);
			}
			tempCount.put(currentIterString, addWordCounts);
		}
	}
	
	/**
	 * @param singleWordNormalized
	 *            The sourceWord to cut
	 * @param cutNumber
	 *            the number of letters to slice-off from right side
	 * @param tempCount
	 * 
	 *            Gets other matches of an existing, important word in analysis,
	 *            by cutting any possible suffix of length 4 to 1.
	 * 
	 * @return matchedMap
	 *         The map, that holds the single elements, that match the
	 *         "singleWordNormalized" String
	 */
	private Map<String, Integer> getOtherMatches(final String singleWordNormalized, final int cutNumber, final Map<String, Integer> tempCount) {
		boolean currentWordMeaning = true;
		// set the meaning of the "search" word to either positive or negative
		// e.g. skilled and unskilled are put in different word-classes
		// since only suffixes are cut, exceptions like "understand", "demand"..
		// that are not negative, but start with a "seemingly" negative
		// prefix, will still go in the correct word-class
		if (singleWordNormalized.startsWith("un") || singleWordNormalized.startsWith("de") || singleWordNormalized.startsWith("ab") || singleWordNormalized.startsWith("non") || singleWordNormalized.startsWith("dis")) {
			currentWordMeaning = false;
		}
		Map<String, Integer> matchedMap = new HashMap<String, Integer>();
		String otherMatch = singleWordNormalized.substring(0, singleWordNormalized.length() - cutNumber);
		String maybeSuffix = singleWordNormalized.substring(singleWordNormalized.length() - cutNumber, singleWordNormalized.length());
		if (maybeSuffix.equals("s") || maybeSuffix.equals("ed") || maybeSuffix.equals("en") || maybeSuffix.equals("er") || maybeSuffix.equals("est") || maybeSuffix.equals("ing") || maybeSuffix.equals("ies") || maybeSuffix.equals("able") || maybeSuffix.equals("ment")) {
			// try find matches for otherMatch
			for (Object iter : tempCount.keySet().toArray()) {
				String iterString = (String) iter;
				Pattern pattern = Pattern.compile(otherMatch);
				Matcher matcher = pattern.matcher(iterString);
				while (matcher.find()) {
					// decide, whether the word meaning is negative/positive
					if (iterString.startsWith("un") || iterString.startsWith("de") || iterString.startsWith("ab") || iterString.startsWith("non") || iterString.startsWith("dis")) {
						// only put in negative words, if the starting "search"
						// word was ment to be negative, too
						if (!currentWordMeaning) {
							if (matchedMap.containsKey(iterString)) {
								matchedMap.put(iterString, matchedMap.get(iterString) + tempCount.get(iterString));
							} else {
								matchedMap.put(iterString, tempCount.get(iterString));
							}
						}
					} else {
						// only put in positive words, if the starting "search"
						// word was ment to be positive, too
						if (currentWordMeaning) {
							if (matchedMap.containsKey(iterString)) {
								matchedMap.put(iterString, matchedMap.get(iterString) + tempCount.get(iterString));
							} else {
								matchedMap.put(iterString, tempCount.get(iterString));
							}
						}
					}
				}
			}
			if (maybeSuffix.equals("ies")) {
				// if the suffix is "-ies", its a special case
				// return it instantly, otherwise the case of
				// the suffix "-s" will be counted as well later
				return matchedMap;
			}
			if (cutNumber == 1) {
				// we build a list of matches, return it now
				return matchedMap;
			} else {
				// getOtherMatch will be executed with cutNum - 1
				// recursivly until cutNum reaches == 1
				matchedMap.putAll(getOtherMatches(singleWordNormalized, cutNumber - 1, tempCount));
				return matchedMap;
			}
		} else {
			if (cutNumber == 1) {
				// dont look for other matches, since
				// we just cut a word itself (no suffix was cut before)
				return new HashMap<String, Integer>();
			} else {
				matchedMap.putAll(getOtherMatches(singleWordNormalized, cutNumber - 1, tempCount));
				return matchedMap;
			}
		}
	}
	
	/**
	 * 
	 * Calculates the strikingFactor, for an amount of answers given.
	 * This factor will be multiplied to answers.size() and returns
	 * a "good" value for a wordLimit.
	 * E.g. if 50 Answers are given, that means about 25 People answered
	 * and the factor is zero, which means for 25 people, every single
	 * analysed word is shown in the Analysis and considered as
	 * "possibly important"
	 * if 50 < answers < 100, one percent is hidden,
	 * if 100 < answers < 1000, two percent are hidden...
	 * 
	 * @return strikingFactor
	 */
	private double getStrikingFactor() {
		double strikingFactor;
		if (answers.size() < 50) {
			strikingFactor = 0.0;
		} else if (answers.size() < 100) {
			strikingFactor = 0.01;
		} else if (answers.size() < 1000) {
			strikingFactor = 0.02;
		} else if (answers.size() < 1300) {
			strikingFactor = 0.026;
		} else {
			// A formula for greater amounts of answers,
			// of over 1300 answer Strings
			strikingFactor = (answers.size() * 2) / 100000;
		}
		// never hide more, than 10%
		if (strikingFactor > 0.10) {
			return 0.1;
		}
		return strikingFactor;
	}
	
	/**
	 * @param toNormalize
	 * 
	 *            "Normalizes" a single word by putting it toLowerCase()
	 *            and removing critical Characters from it (like dots, etc...)
	 * 
	 * @return normalized String
	 */
	private String normalizeSingleWord(String toNormalize) {
		toNormalize = toNormalize.toLowerCase();
		
		toNormalize = toNormalize.replace(".", "");
		toNormalize = toNormalize.replace("!", "");
		toNormalize = toNormalize.replace("?", "");
		toNormalize = toNormalize.replace(",", "");
		toNormalize = toNormalize.replace("(", "");
		toNormalize = toNormalize.replace(")", "");
		toNormalize = toNormalize.replace(";", "");
		toNormalize = toNormalize.replace(":", "");
		toNormalize = toNormalize.replace("/", "");
		toNormalize = toNormalize.replace("\\", "");
		return toNormalize;
	}
	
	public Integer getMaximumCount() {
		
		this.getWordCountHashMap();
		
		int max = 0;
		
		for (Integer val : wordCount.values()) {
			if (val > max) {
				max = val;
			}
		}
		
		return max;
	}
	
}
