/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.helpers;

import org.slf4j.helpers.MessageFormatter;


/**
 * A collection of convenience methods for working with {@link String} objects.
 */
public final class StringHelper{

	/** An empty {@code String} array. */
	private static final String[] EMPTY_STRING_ARRAY = new String[0];


	private StringHelper(){}


	/**
	 * Performs argument substitution.
	 *
	 * @param message	The message pattern which will be parsed and formatted (see {@link MessageFormatter}).
	 * @param parameters	The arguments to be substituted in place of the formatting anchors.
	 * @return	The formatted message.
	 */
	public static String format(final String message, final Object... parameters){
		return MessageFormatter.arrayFormat(message, parameters)
			.getMessage();
	}

	/**
	 * Split the given text into an array, separator specified.
	 * <p>
	 * The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * </p>
	 *
	 * @param text	The text to parse.
	 * @param separatorChar	The character used as the delimiter.
	 * @return	A list of parsed strings.
	 */
	public static String[] split(final String text, final char separatorChar){
		return split(text, 0, separatorChar, null);
	}

	/**
	 * Split the given text into an array, separator specified.
	 * <p>
	 * The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * </p>
	 *
	 * @param text	The text to parse.
	 * @param separatorChar	The character used as the delimiter.
	 * @param escapeChar	The character used to escape a delimiter.
	 * @return	A list of parsed strings.
	 */
	public static String[] split(final String text, final char separatorChar, final char escapeChar){
		return split(text, 0, separatorChar, escapeChar);
	}

	/**
	 * Split the given text into an array, separator specified.
	 * <p>
	 * The separator is not included in the returned String array.
	 * </p>
	 *
	 * @param text	The text to parse.
	 * @param fromIndex	Index in text to start from.
	 * @param separatorChar	The character used as the delimiter.
	 * @param escapeChar	The character used to escape a delimiter.
	 * @return	A list of parsed strings.
	 */
	public static String[] split(final String text, final int fromIndex, final char separatorChar, final Character escapeChar){
		final int length = text.length();
		if(length == 0)
			return EMPTY_STRING_ARRAY;

		int count = 1;
		for(int i = fromIndex; i < length; i ++)
			if(text.charAt(i) == separatorChar)
				count ++;
		final String[] result = new String[count];

		char previousChar = 0;
		int currentIndex = 0;
		int start = fromIndex;
		for(int i = fromIndex; i < length; i ++){
			char currentChar = text.charAt(i);
			if(currentChar == separatorChar && escapeChar != null && i > 0 && previousChar != escapeChar){
				if(start != i)
					result[currentIndex] = text.substring(start, i);
				currentIndex ++;

				start = i + 1;
			}

			previousChar = currentChar;
		}
		if(start != length)
			result[currentIndex] = text.substring(start);

		return result;
	}

	private static String[] createSplitResult(final byte[] bytes, final int fromIndex, final char separatorChar){
		final int length = bytes.length;
		int count = (bytes[fromIndex] == separatorChar? 0: 1);
		for(int i = fromIndex; i < length; i ++)
			if(bytes[i] == separatorChar)
				count ++;
		return new String[count];
	}

}
