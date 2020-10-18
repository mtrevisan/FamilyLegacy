/**
 * Copyright (c) 2020 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package ch.thn;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class StringUtil{

	/**
	 * Determines on which side the string should be clipped.
	 */
	private enum ClippingMode{
		/**
		 * Clipped on the left side of the string.
		 */
		LEFT,
		/**
		 * Clipped on the right side of the string.
		 */
		RIGHT,
	}

	private enum ClippingModeCenter{
		/**
		 * Clips in the middle of the string, more towards the beginning of the string.
		 */
		LEFT,
		/**
		 * Clips in the middle of the string, more towards the end of the string.
		 */
		RIGHT,
		/**
		 * Clips in the middle of the string.
		 */
		CENTER;
	}

	/**
	 * Special characters, from ASCII #33 to #126, without minus and underline:
	 * !"#$%&'()*+,./:;<=>?@[\]^`{|}~
	 **/
	private static final String specialChars = "!\"#$%&\'()*+,./:;<=>?@[\\]^`{|}~";
	private static final String numbers = "0123456789";
	private static final String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String lowercase = "abcdefghijklmnopqrstuvwxyz";

	/**
	 * Matches single string ranges, like a-z and A-Z in "a-zA-z"
	 */
	private static Pattern stringRangePattern = Pattern.compile(".-.");
	/**
	 * Matches a range group with one or more ranges, like "[a-z]" or "[a-zA-z]"
	 */
	private static Pattern stringRangePatternGroup = Pattern.compile("\\[(.-.)+\\]");
	/**
	 * Matches a whole string only if there are only pattern groups, like "[a-zA-z][0-9]" but not
	 * "[a-zA-z]abc[0-999]"
	 */
	private static Pattern stringRangePatternGroupsOnly = Pattern.compile("(\\[(.-.)+\\])*");
	/**
	 * Matches the beginning or a range group with multiple ranges, like "[a-xA" in "[a-xA-X1-9]"
	 */
	private static Pattern stringRangePatternGroupStart = Pattern.compile("(\\[.-.)([^\\]])");

	private static final SecureRandom random = new SecureRandom();

	/**
	 * Null-save equal check for two strings. Returns true if both are <code>null</code>,
	 * <code>false</code> if one of them is <code>null</code> and compares them otherwise using the
	 * equals method.
	 *
	 * @param string1 The first string
	 * @param string2 The second string
	 * @return The result of the comparison
	 */
	public static boolean equals(String string1, String string2){
		// return (str1 == null ? str2 == null : str1.equals(str2));
		// Now uses Objects.equals from Java7
		return Objects.equals(string1, string2);
	}

	/**
	 * Clips a string so that the total number of characters does not exceed
	 * <code>maxCharacterLength</code>.<br>
	 * This method clips the string on the left or the right side, replacing excessive characters with
	 * "...".
	 *
	 * @param str                The string to clip
	 * @param maxCharacterLength The maximum number of characters the resulting string should contain
	 * @param clipMode           The mode of the clipping
	 * @return The clipped string, or the unmodified string if the maximum characters defined is
	 * higher than the actual string
	 */
	public static String clipString(String str, int maxCharacterLength, ClippingMode clipMode){
		if(str == null || str.length() <= maxCharacterLength){
			return str;
		}

		if(maxCharacterLength < 0){
			return "...";
		}

		String substitute = "...";

		if(str.length() > maxCharacterLength){

			switch(clipMode){
				case LEFT:
					int start = str.length() - maxCharacterLength + substitute.length();

					if(start > str.length()){
						start = str.length();
					}

					return substitute + str.substring(start, str.length());
				case RIGHT:
					int end = maxCharacterLength - substitute.length();

					if(end < 0){
						end = 0;
					}

					return str.substring(0, end) + substitute;
			}
		}

		return str;

	}

	/**
	 * This method clips the string s in the center of the string, replacing excessive characters with
	 * "...". The length of the string can be defined by number of characters.<br>
	 *
	 * @param str                The string to clip
	 * @param maxCharacterLength The maximum number of characters the resulting string should contain
	 * @return The clipped string, or the unmodified string if the maximum characters defined is
	 * higher than the actual string
	 */
	public static String clipStringCenter(String str, int maxCharacterLength){
		return clipStringCenter(str, maxCharacterLength, ClippingModeCenter.CENTER, 0, 0, 0, 0);
	}

	/**
	 * This method clips the string s within the string, replacing excessive characters with "...".
	 * The length of the string can be defined by number of characters.<br>
	 * The clipping is shifted to the left, leaving at least leftMin and maximum leftMax characters on
	 * the left side. leftMax can be set to 0 for no maximum which results in a centered clipping.
	 *
	 * @param str                The string to clip
	 * @param maxCharacterLength The maximum number of characters the resulting string should contain
	 * @param leftMin            The minimum number of characters to remain on the left side of the string
	 * @param leftMax            The maximum number of characters on the left side of the string
	 * @return The clipped string, or the unmodified string if the maximum characters defined is
	 * higher than the actual string
	 */
	public static String clipStringCenterLeft(String str, int maxCharacterLength, int leftMin, int leftMax){
		if(leftMin > leftMax){
			throw new IllegalArgumentException("leftMin is greater than leftMax. Only leftMin <= leftMax is allowed.");
		}

		if(leftMin < 0 || leftMax < 0){
			throw new IllegalArgumentException("leftMin < 0 or leftMax < 0. Only values >= 0 allowed.");
		}

		return clipStringCenter(str, maxCharacterLength, ClippingModeCenter.LEFT, leftMin, leftMax, 0, 0);
	}

	/**
	 * This method clips the string s within the string, replacing excessive characters with "...".
	 * The length of the string can be defined by number of characters.<br>
	 * The clipping is shifted to the right, leaving at least rightMin and maximum rightMax characters
	 * on the right side. rightMax can be set to 0 for no maximum which results in a centered
	 * clipping.
	 *
	 * @param str                The string to clip
	 * @param maxCharacterLength The maximum number of characters the resulting string should contain
	 * @param rightMin           The minimum number of characters to remain on the right side of the string
	 * @param rightMax           The maximum number of characters on the right side of the string
	 * @return The clipped string, or the unmodified string if the maximum characters defined is
	 * higher than the actual string
	 */
	public static String clipStringCenterRight(String str, int maxCharacterLength, int rightMin, int rightMax){
		if(rightMin > rightMax){
			throw new IllegalArgumentException("rightMin is greater than rightMax. Only rightMin <= rightMax is allowed.");
		}

		if(rightMin < 0 || rightMax < 0){
			throw new IllegalArgumentException("rightMin < 0 or rightMax < 0. Only values >= 0 allowed.");
		}

		return clipStringCenter(str, maxCharacterLength, ClippingModeCenter.RIGHT, 0, 0, rightMin, rightMax);
	}

	/**
	 * The core method for clipping strings with a defined character length.<br>
	 * This method clips the string s within the string, replacing excessive characters with "...".
	 * The length of the string can be defined by the number of characters.
	 *
	 * @param str                The string to clip
	 * @param maxCharacterLength The maximum characters (including the "...")
	 * @param clipCenterMode     The mode for clipping in the center
	 * @param leftMin            The minimum characters on the left side, if the clipping mode is
	 *                           ClippingMode.CLIP_CENTER_LEFT
	 * @param leftMax            The maximum characters on the left side, if the clipping mode is
	 *                           ClippingMode.CLIP_CENTER_LEFT. If set to 0, the maximum value is not used.
	 * @param rightMin           The minimum characters on the right side, if the clipping mode is
	 *                           ClippingMode.CLIP_CENTER_RIGHT
	 * @param rightMax           The maximum characters on the right side, if the clipping mode is
	 *                           ClippingMode.CLIP_CENTER_RIGHT. If set to 0, the maximum value is not used.
	 * @return The clipped string, or the unmodified string if the maximum characters defined is
	 * higher than the actual string
	 */
	public static String clipStringCenter(String str, int maxCharacterLength, ClippingModeCenter clipCenterMode, int leftMin, int leftMax, int rightMin, int rightMax){

		if(str == null || str.length() <= maxCharacterLength){
			return str;
		}

		if(maxCharacterLength <= 3){
			return "...";
		}

		String substitute = "...";

		// Get the center
		int left = (int)Math.ceil(maxCharacterLength / 2.0) - (int)Math.ceil(substitute.length() / 2.0);
		int right = str.length() - (int)Math.floor(maxCharacterLength / 2.0) + (int)Math.floor(substitute.length() / 2.0);

		// Adjust the left and right min/max bounds if necessary
		switch(clipCenterMode){
			case LEFT:
				if(left < leftMin){
					// It is too far to the left -> shift it to the right
					right += leftMin - left;
					left = leftMin;
				}
				else if(left > leftMax && leftMax != 0){
					// It is too far to the right -> shift it to the left
					right -= left - leftMax;
					left = leftMax;
				}
				break;
			case RIGHT:
				if(str.length() - right < rightMin){
					left -= str.length() - right - rightMin;
					right = str.length() - rightMin;
				}
				else if(str.length() - right > rightMax && rightMax != 0){
					left += str.length() - right - rightMax;
					right = str.length() - rightMax;
				}
				break;
			case CENTER:
				// Do nothing
				break;
		}

		// Adjust left and right if they are out of bounds

		if(left < 0){
			left = 0;
		}

		if(right > str.length()){
			right = str.length();
		}

		// Get the substrings and put the substitute in between
		return str.substring(0, left) + substitute + str.subSequence(right, str.length());

	}

	/**
	 * Checks if the pattern is present in the given input string.
	 *
	 * @param pattern The pattern to look for
	 * @param input   The string to check against
	 * @return <code>true</code> if the pattern is found the input string at least once
	 */
	public static boolean contains(Pattern pattern, String input){
		return pattern.matcher(input).find();
	}

	/**
	 * Checks if the pattern is present in the given input string the given number of times
	 *
	 * @param pattern         The pattern to look for
	 * @param input           The string to check against
	 * @param numberOfMatches The number of matches to count
	 * @return <code>true</code> if the input string contains the given number of matches
	 */
	public static boolean contains(Pattern pattern, String input, int numberOfMatches){
		if(numberOfMatches < 0){
			throw new IllegalArgumentException("Invalid value range for numberOfMatches. Only values >= 0 allowed.");
		}

		Matcher m = pattern.matcher(input);
		int count = 0;
		while(m.find()){
			count++;

			if(count == numberOfMatches){
				return true;
			}
		}

		if(count == 0){
			return true;
		}

		return false;
	}

	/**
	 * Checks if the pattern matches the entire input string. It is equivalent to String.matches but
	 * gives the possibility to use a precompiled pattern.
	 *
	 * @param pattern The pattern to look for
	 * @param input   The string to check against
	 * @return <code>true</code> if the pattern matched the entire input string
	 */
	public static boolean matches(Pattern pattern, String input){
		return pattern.matcher(input).matches();
	}

	/**
	 * Counts the number of matches in the input string.<br>
	 * <br>
	 * If you want to know if a pattern is present in a string a certain number of times, use
	 * {@link #contains(Pattern, String, int)} for better efficiency.
	 *
	 * @param pattern The pattern to look for
	 * @param input   The string to check against
	 * @return The number of matches
	 */
	public static int matchesCount(Pattern pattern, String input){
		Matcher m = pattern.matcher(input);
		int count = 0;
		while(m.find()){
			count++;
		}
		return count;
	}

	/**
	 * Removes all occurrences of the matching pattern in the input string
	 *
	 * @param pattern The pattern to look for
	 * @param input   The string to check against
	 * @return The input string without any parts which matched the pattern
	 */
	public static String removeAll(Pattern pattern, String input){
		return replaceAll(pattern, input, "", false);
	}


	/**
	 * Replaces all pattern matches with the replacement string. If the result of the replacement
	 * creates another pattern match, that match is not replaced. Use
	 * {@link #replaceAll(Pattern, String, String, boolean)} for that case.
	 *
	 * @param pattern     The pattern to look for
	 * @param input       The string to check against
	 * @param replaceWith The replacement string
	 * @return The input string which has all matches replaced with the replacement string
	 */
	public static String replaceAll(Pattern pattern, String input, String replaceWith){
		return replaceAll(pattern, input, replaceWith, false);
	}

	/**
	 * Replaces all occurrences of the pattern in the input string with the given replaceWith string.
	 * It is equivalent to String.replaceAll, but gives the possibility to also look through the
	 * result of the replacement result and replace any patterns which might have been created through
	 * a previous replacement.<br>
	 *
	 * @param pattern         The pattern to look for
	 * @param input           The string to check against
	 * @param replaceWith     The replacement string
	 * @param replaceReplaced If set to <code>true</code>, also matches which are created by the
	 *                        replacement are replaced, until there are no matches any more (e.g. replacing "abc" with
	 *                        "cba" in "abcbcxyz" needs two steps: "abcbcxyz" -> has one match -> "cbabcxyz" -> this
	 *                        created anothermatch -> "cbcbaxyz"). <i>Warning:</i> If this is set to
	 *                        <code>true</code>, an error is thrown if the pattern matches the replacement string
	 *                        because this would result in an infinite loop.
	 * @return The input string whose pattern matches are replaced with the replaceWith string
	 */
	public static String replaceAll(Pattern pattern, String input, String replaceWith, boolean replaceReplaced){
		if(replaceReplaced){
			Matcher m = pattern.matcher(input);
			String ret = input;
			while(m.find()){
				ret = m.replaceAll(replaceWith);
				m = pattern.matcher(ret);

				// If the pattern matches the replacement string, an infinite loop
				// would be created -> show error
				if(contains(pattern, replaceWith)){
					throw new IllegalArgumentException("Pattern '" + pattern.toString() + "' matches the replacement string '" + replaceWith + "' which would create an infinite loop when using replaceReplaced=true.");
				}
			}
			return ret;
		}
		else{
			Matcher m = pattern.matcher(input);
			if(m.find()){
				return m.replaceAll(replaceWith);
			}
			else{
				return input;
			}
		}
	}

	/**
	 * Simple search and replace in a string builder.<br>
	 * Replaces all occurrences of <code>search</code> in <code>sb</code> with <code>replace</code>
	 *
	 * @param sb      The string builder to search through
	 * @param search  The string to search for
	 * @param replace The replacement string
	 */
	public static void replaceAll(StringBuilder sb, String search, String replace){
		replaceAll(sb, search, replace, false);
	}

	/**
	 * Simple search and replace in a string builder.<br>
	 * Replaces all occurrences of <code>search</code> in <code>sb</code> with
	 * <code>replace</code><br>
	 * Choose to start at the front or at the tail with the search.
	 *
	 * @param sb            The string builder to search through
	 * @param search        The string to search for
	 * @param replace       The replacement string
	 * @param startWithTail if <code>true</code>, the search starts from the end of the string builder
	 */
	public static void replaceAll(StringBuilder sb, String search, String replace, boolean startWithTail){

		int index = 0;
		int start = 0;
		int end = 0;

		while(index < sb.length()){

			if(startWithTail){
				start = sb.lastIndexOf(search);
			}
			else{
				start = sb.indexOf(search, index);
			}

			if(start == - 1){
				// No more occurrences
				break;
			}

			end = start + search.length();

			sb.replace(start, end, replace);
			index = start + replace.length();
		}


		// //Could also be done like this. Faster?
		// Pattern p = Pattern.compile("cat");
		// Matcher m = p.matcher("one cat two cats in the yard");
		// StringBuffer sb = new StringBuffer();
		// while (m.find()) {
		// m.appendReplacement(sb, "dog");
		// }
		// m.appendTail(sb);
		// System.out.println(sb.toString());

	}

	/**
	 * Returns all the strings in the input which match the given pattern.
	 *
	 * @param pattern The pattern to look for
	 * @param input   The string to check against
	 * @return An ordered list of all matches
	 */
	public static List<String> getMatching(Pattern pattern, String input){
		return getMatching(pattern, input, 0);
	}

	/**
	 * Returns the first string in the input which match the given pattern.
	 *
	 * @param pattern The pattern to look for
	 * @param input   The string to check against
	 * @return The first match
	 */
	public static String getMatchingFirst(Pattern pattern, String input){
		List<String> l = getMatching(pattern, input, 1);
		if(l.size() > 0){
			return l.get(0);
		}
		else{
			return null;
		}
	}

	/**
	 * Returns all the strings in the input which match the given pattern, but stops looking for more
	 * matches when <code>numberOfMatching</code> is reached. If <code>numberOfMatching=0</code>, it
	 * returns all the matches.
	 *
	 * @param pattern          The pattern to look for
	 * @param input            The string to check against
	 * @param numberOfMatching After how many matches the method should return. <code>0</code> returns
	 *                         all matches
	 * @return The found matches, depending on <code>numberOfMatching</code>
	 */
	private static List<String> getMatching(Pattern pattern, String input, int numberOfMatching){
		List<String> l = new ArrayList<>();
		Matcher m = pattern.matcher(input);

		if(numberOfMatching == 0){
			// Return all the matches

			while(m.find()){
				if(m.group().length() == 0){
					continue;
				}

				l.add(m.group());
			}
		}
		else{
			while(m.find() && l.size() < numberOfMatching){
				if(m.group().length() == 0){
					continue;
				}

				l.add(m.group());
			}
		}

		return l;
	}

	/**
	 * Extracts all the "ranges" out of the given string. A range has a start and an end character,
	 * enclosed in [] and separated by "-" (e.g. "abc[a-z]def" has one range "a-z"). There can also be
	 * range groups like "[a-zA-Z0-9]".
	 *
	 * @param rangesString An input string like "a-z" or "a-zA-Z0-9#-/"
	 * @return An ordered list of all ranges
	 */
	public static List<String> rangesExtract(String rangesString){
		List<String> groups = getMatching(stringRangePatternGroup, rangesString);
		List<String> ranges = new ArrayList<>();
		for(String rangeGroup : groups){
			ranges.addAll(getMatching(stringRangePattern, rangeGroup));
		}
		return ranges;
	}

	/**
	 * Builds a map with all the ranges which are within the rangesString. The map key is the range
	 * string (e.g. "[a-z]"), the map value is the expanded range (e.g. "abc...xyz")
	 *
	 * @param rangesString The string to look for ranges
	 * @return The map with the expanded ranges
	 */
	public static Map<String, String> rangesExtractMap(String rangesString){
		List<String> ranges = rangesExtract(rangesString);

		// <range, built range>
		Map<String, String> builtRanges = new HashMap<String, String>();

		// Build all ranges
		for(String range : ranges){
			// Do not build it again if already built
			if(! builtRanges.containsKey(range)){
				builtRanges.put(range, rangeExpand(range));
			}
		}

		return builtRanges;
	}

	/**
	 * Builds a string out of one single range. A range can be increasing or decreasing (reverse),
	 * thus [a-z] will go from a to z and [z-a] will go from z to a. The resulting string would be
	 * "abcdefg...xyz" or "zyx...gfedcba".<br>
	 * <br>
	 * For multiple ranges, use {@link #rangesExpand(String)}.<br>
	 * For one or multiple ranges within a string, use {@link #rangesReplace(String)}.
	 *
	 * @param rangeString An input string like "a-z" or "[#-/]", with or without the enclosing
	 *                     brackets []
	 * @return The expanded range
	 */
	public static String rangeExpand(String rangeString){
		// Get rid of the enclosing brackets if there are any
		if(rangeString != null && rangeString.length() == 5){
			rangeString = rangeString.substring(1);
			rangeString = rangeString.substring(0, 3);
		}

		if(rangeString == null || ! matches(stringRangePattern, rangeString)){
			throw new IllegalArgumentException("Invalid range string '" + rangeString + "'. A range string can only have 3 characters (plus the opening " + "and closing brackets [ and ]), e.g. a-z, [A-Z] etc.");
		}

		int start = rangeString.charAt(0);
		int end = rangeString.charAt(2);
		boolean reverse = start > end;

		// The output string will have the size of all the including start
		// and end characters
		StringBuilder sb = null;

		if(reverse){
			sb = new StringBuilder(start - end + 1);
			for(int i = start; i >= end; i--){
				sb.append((char)i);
			}
		}
		else{
			sb = new StringBuilder(end - start + 1);
			for(int i = start; i <= end; i++){
				sb.append((char)i);
			}
		}

		return sb.toString();
	}

	/**
	 * Builds the string from one or more ranges. The given string can only contain ranges (single
	 * ranges or groups), like [a-zA-z][0-9] etc. and must include the opening and closing brackets []
	 *
	 * @param rangesString A string with only ranges
	 * @return The expanded ranges
	 */
	public static String rangesExpand(String rangesString){
		if(rangesString == null || ! matches(stringRangePatternGroupsOnly, rangesString)){
			throw new IllegalArgumentException("Ranges string does not only contain ranges. A range needs to be between opening and closing brackets [].");
		}

		List<String> ranges = rangesExtract(rangesString);

		// <range, built range>
		HashMap<String, String> builtRanges = new HashMap<String, String>();

		// Build all ranges
		for(String range : ranges){
			// Do not build it again if already built
			if(! builtRanges.containsKey(range)){
				builtRanges.put(range, rangeExpand(range));
			}
		}

		StringBuilder sb = new StringBuilder(ranges.size() * 2);

		for(String range : ranges){
			sb.append(builtRanges.get(range));
		}

		return sb.toString();
	}

	/**
	 * Looks for all the ranges within the given string, builds the ranges and replaces the
	 * range-strings [a-z] etc. with the built range.<br>
	 * See {@link #rangeExpand(String)} for more information about allowed ranges and their format.
	 *
	 * @param rangesString A string which contains ranges
	 * @return The string, with all ranges expanded
	 */
	public static String rangesReplace(String rangesString){
		Map<String, String> builtRanges = rangesExtractMap(rangesString);

		// Convert all the group ranges to single ranges. This is necessary for
		// the later replacement of the ranges.
		rangesString = replaceAll(stringRangePatternGroupStart, rangesString, "$1\\]\\[$2", true);

		String output = rangesString;

		Iterator<String> iterator = builtRanges.keySet().iterator();

		// Replace all ranges (a-z, A-Z, etc.) with their built ranges
		while(iterator.hasNext()){
			String range = iterator.next();
			output = output.replaceAll(Pattern.quote("[" + range + "]"), Matcher.quoteReplacement(builtRanges.get(range)));
		}

		return output;
	}

	/**
	 * Generates a random string with the given length, the numbers 0-9 and the letters A-Z and a-z.
	 * If needed, special characters can be included.
	 *
	 * @param len                      The length of the string to produce
	 * @param includeSpecialCharacters Whether to include special characters
	 * @return The random string
	 */
	public static String randomString(int len, boolean includeSpecialCharacters){
		return randomString(len, true, true, true, false, includeSpecialCharacters, includeSpecialCharacters, includeSpecialCharacters);
	}

	/**
	 * Generates a random string with the given length and the selected characters
	 *
	 * @param len          The character length of the random string
	 * @param numbers      All numbers 0-9
	 * @param uppercase    All uppercase Characters A-Z
	 * @param lowercase    All lowercase characters a-z
	 * @param space        The space character " "
	 * @param minus        The minus character "-"
	 * @param underline    The underline character "_"
	 * @param specialChars Special characters, from ASCII #33 to #126, without minus and underline:
	 *                     !"#$%&'()*+,./:;<=>?@[\]^`{|}~
	 * @return The random string
	 */
	public static String randomString(int len, boolean numbers, boolean uppercase, boolean lowercase, boolean space, boolean minus, boolean underline, boolean specialChars){

		StringBuilder sb = new StringBuilder();

		if(numbers){
			sb.append(StringUtil.numbers);
		}

		if(uppercase){
			sb.append(StringUtil.uppercase);
		}

		if(lowercase){
			sb.append(StringUtil.lowercase);
		}

		if(space){
			sb.append(" ");
		}

		if(minus){
			sb.append("-");
		}

		if(underline){
			sb.append("_");
		}

		return randomString(sb.toString(), len, specialChars);
	}

	/**
	 * Generates a random string with the given length and the characters given in the
	 * characterString. If needed, the special characters from ASCII #33 to #126 can be included
	 * (defined in {@link #specialChars}).
	 *
	 * @param characterString          The characters to use to build the random string
	 * @param len                      The length of the string to produce
	 * @param includeSpecialCharacters Wheter to include special characters
	 * @return The random string
	 */
	public static String randomString(String characterString, int len, boolean includeSpecialCharacters){
		if(characterString == null || characterString.length() == 0 || len < 0){
			return null;
		}

		int nextInt = 0;
		StringBuilder sb = new StringBuilder(len);

		if(includeSpecialCharacters){
			characterString = characterString + StringUtil.specialChars;
		}

		for(int i = 0; i < len; i++){
			nextInt = random.nextInt(characterString.length());
			sb.append(characterString.charAt(nextInt));
		}

		return sb.toString();

	}

}
