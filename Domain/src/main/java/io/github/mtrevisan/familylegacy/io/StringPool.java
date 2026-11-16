/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.io;

import java.util.Locale;


/**
 * A small table that gives always the same instance to the strings that are read many times (the tags, and the short values that
 * repeat, like the types), so that a big file does not keep a copy of "name" for each of its millions of nodes. The string is found
 * in the text without creating it, so a word already seen makes no garbage either.
 * <p>
 * The table has a fixed size, and a word replaces the one that has the same slot: the memory kept is bounded whatever the file.
 * </p>
 * <p>It is not thread-safe: each parser has its own.</p>
 */
final class StringPool{

	private final String[] table;
	private final int mask;


	/** @param size the number of slots, a power of two */
	StringPool(final int size){
		table = new String[size];
		mask = size - 1;
	}


	/**
	 * @param text  the text
	 * @param start the start of the word (inclusive)
	 * @param end   the end of the word (exclusive)
	 * @return the word in lowercase, the same instance for the same word
	 */
	String lowercase(final String text, final int start, final int end){
		int hash = 0;
		for(int i = start; i < end; i ++){
			final char c = text.charAt(i);
			if(c >= 'A' && c <= 'Z' || c > 127)
				//a word that is not lowercase ASCII is rare: it takes the slow way
				return exact(text.substring(start, end).toLowerCase(Locale.ROOT));

			hash = 31 * hash + c;
		}
		return lookup(text, start, end, hash);
	}

	/**
	 * @param text  the text
	 * @param start the start of the word (inclusive)
	 * @param end   the end of the word (exclusive)
	 * @return the word, the same instance for the same word
	 */
	String exact(final String text, final int start, final int end){
		int hash = 0;
		for(int i = start; i < end; i ++)
			hash = 31 * hash + text.charAt(i);
		return lookup(text, start, end, hash);
	}

	private String lookup(final String text, final int start, final int end, final int hash){
		final int slot = (hash ^ (hash >>> 16)) & mask;
		final String known = table[slot];
		if(known != null && known.length() == end - start && text.startsWith(known, start))
			return known;

		final String word = text.substring(start, end);
		table[slot] = word;
		return word;
	}

	private String exact(final String word){
		return exact(word, 0, word.length());
	}

}
