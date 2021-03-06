/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.services;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class RegexHelper{

	private static final String SPLITTER_PATTERN_WITH_DELIMITER = "(?=(?!^)%1$s)(?<!%1$s)|(?!%1$s)(?<=%1$s)";


	private RegexHelper(){}

	public static Pattern pattern(final String pattern){
		return Pattern.compile(pattern);
	}

	public static Pattern pattern(final String pattern, final int flags){
		return Pattern.compile(pattern, flags);
	}

	/**
	 * Returns the delimiters along with the split elements
	 *
	 * @param delimitersRegex	regex stating the delimiters
	 * @return	The pattern to be used to split a string
	 */
	public static Pattern splitterWithDelimiters(final String delimitersRegex){
		return pattern(String.format(SPLITTER_PATTERN_WITH_DELIMITER, delimitersRegex));
	}

	public static String[] split(final CharSequence text, final Pattern pattern){
		return split(text, pattern, 0);
	}

	public static String[] split(final CharSequence text, final Pattern pattern, final int limit){
		return pattern.split(text, limit);
	}

	private static String getNextGroup(final Matcher matcher){
		String component = null;
		int i = 1;
		final int size = matcher.groupCount();
		while(component == null && i <= size)
			component = matcher.group(i ++);
		return component;
	}


	public static Matcher matcher(final CharSequence text, final Pattern pattern){
		return pattern.matcher(text);
	}

	public static boolean find(final CharSequence text, final Pattern pattern){
		return matcher(text, pattern)
			.find();
	}

	public static boolean matches(final CharSequence text, final Pattern pattern){
		return matcher(text, pattern)
			.matches();
	}

	public static boolean contains(final CharSequence text, final Pattern pattern){
		return matcher(text, pattern)
			.find();
	}

	public static String replaceAll(final CharSequence text, final Pattern pattern, final String replacement){
		return matcher(text, pattern)
			.replaceAll(replacement);
	}

	/**
	 * Removes all occurrences of the matching pattern in the input string
	 *
	 * @param pattern	The pattern to look for
	 * @param input	The string to check against
	 * @return	The input string without any parts which matched the pattern
	 */
	public static String removeAll(final String input, final Pattern pattern){
		return replaceAll(input, pattern, "");
	}

	public static String clear(final CharSequence text, final Pattern pattern){
		return replaceAll(text, pattern, StringUtils.EMPTY);
	}

	/**
	 * Returns the first string in the input which match the given pattern.
	 *
	 * @param text	The string to check against.
	 * @param pattern	The pattern to look for.
	 * @return	The first match
	 */
	public static String getFirstMatching(final String text, final Pattern pattern){
		final Matcher m = pattern.matcher(text);
		while(m.find()){
			if(m.group().length() == 0)
				continue;

			return m.group();
		}
		return null;
	}

}
