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

package io.github.mtrevisan.familylegacy.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * The text typed by the user, parsed into <b>terms</b>.
 * <p>
 * Terms are separated by whitespace, and there are three kinds of them:
 * <ul>
 *   <li>a <b>word</b>: {@code gall}, matched as seen in {@link SearchMatcher} (substring, whole word, fuzzy);</li>
 *   <li>a <b>phrase</b>: {@code "Bortolo Gallinaro"}, text between double quotes, that must be found exactly as written
 *       (ignoring capitals), with its spaces and punctuation; neither the fuzzy mode nor the wildcards apply to it, and
 *       the whole word mode requires it to start and end on a word boundary;</li>
 *   <li>a <b>wildcard</b> word: {@code gall*}, {@code b?rtolo}, where {@code *} stands for any run of characters (even none) and
 *       {@code ?} for exactly one character; it must match a whole word of the text.</li>
 * </ul>
 * A backslash makes the next character literal: {@code \"} is a double quote, {@code \*} an asterisk, {@code \?} a question mark
 * and {@code \\} a backslash, both inside and outside a phrase (inside a phrase, {@code *} and {@code ?} are literal anyway, escaped or not).
 * A backslash before anything else is a plain backslash. A quote that is never closed ends with the text, so that the query is
 * meaningful while it is being typed. A word that contains a character that is neither a letter nor a digit (an escaped quote,
 * an apostrophe) can never equal a word of the text, so it is looked for like a phrase.
 */
public final class SearchQuery{

	/** The kinds of term. */
	public enum Kind{

		WORD,
		PHRASE,
		WILDCARD

	}


	/** In a wildcard pattern, stands for {@code *}: a character that no text can contain, so that a literal {@code *} is not mistaken. */
	static final char ANY_RUN = '';
	/** In a wildcard pattern, stands for {@code ?}. */
	static final char ANY_ONE = '';


	/**
	 * A term of the query.
	 *
	 * @param kind	What kind of term it is.
	 * @param text	The text to look for, in lower case. For a wildcard, the pattern, with the wildcards replaced by markers.
	 */
	public record Term(Kind kind, String text){

		/** @return	The term as the user would type it, to show it back. */
		public String display(){
			return switch(kind){
				case WORD -> text;
				case PHRASE -> '"' + text + '"';
				case WILDCARD -> text.replace(ANY_RUN, '*')
					.replace(ANY_ONE, '?');
			};
		}

	}


	private final List<Term> terms;


	private SearchQuery(final List<Term> terms){
		this.terms = List.copyOf(terms);
	}


	/** @return	The terms of the query, in the order they were typed. */
	public List<Term> terms(){
		return terms;
	}

	/** @return	Whether the query has no term at all (blank, or only empty quotes): everything passes. */
	public boolean isEmpty(){
		return terms.isEmpty();
	}


	/**
	 * Parses the text typed by the user.
	 *
	 * @param query	The text, possibly {@code null}.
	 * @return	The parsed query, never {@code null}.
	 */
	public static SearchQuery parse(final String query){
		final List<Term> terms = new ArrayList<>();
		if(query == null)
			return new SearchQuery(terms);

		final StringBuilder current = new StringBuilder();
		boolean wildcard = false;
		boolean inPhrase = false;
		final int length = query.length();
		for(int i = 0; i < length; i ++){
			final char c = query.charAt(i);
			if(inPhrase){
				if(c == '\\' && i + 1 < length && isEscapable(query.charAt(i + 1)))
					current.append(query.charAt(++ i));
				else if(c == '"'){
					addTerm(terms, current, Kind.PHRASE);
					inPhrase = false;
				}
				else
					current.append(c);
			}
			else if(Character.isWhitespace(c)){
				addTerm(terms, current, (wildcard? Kind.WILDCARD: Kind.WORD));
				wildcard = false;
			}
			else if(c == '"'){
				addTerm(terms, current, (wildcard? Kind.WILDCARD: Kind.WORD));
				wildcard = false;
				inPhrase = true;
			}
			else if(c == '\\' && i + 1 < length && isEscapable(query.charAt(i + 1)))
				current.append(query.charAt(++ i));
			else if(c == '*'){
				current.append(ANY_RUN);
				wildcard = true;
			}
			else if(c == '?'){
				current.append(ANY_ONE);
				wildcard = true;
			}
			else
				current.append(c);
		}
		addTerm(terms, current, (inPhrase? Kind.PHRASE: (wildcard? Kind.WILDCARD: Kind.WORD)));
		return new SearchQuery(terms);
	}

	private static boolean isEscapable(final char c){
		return (c == '"' || c == '\\' || c == '*' || c == '?');
	}

	/** Adds the term collected so far, if any, and starts a new one. */
	private static void addTerm(final List<Term> terms, final StringBuilder current, final Kind kind){
		if(current.isEmpty())
			return;

		final String text = current.toString().toLowerCase(Locale.ROOT);
		current.setLength(0);
		if(kind == Kind.WORD && !isWord(text))
			terms.add(new Term(Kind.PHRASE, text));
		else
			terms.add(new Term(kind, text));
	}

	private static boolean isWord(final String text){
		for(int i = 0, length = text.length(); i < length; i ++)
			if(!Character.isLetterOrDigit(text.charAt(i)))
				return false;
		return true;
	}

}
