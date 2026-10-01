/**
 * Copyright (c) 2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public final class StringHelper{

	private static final Pattern PLURAL_PATTERN_S = Pattern.compile(".*?(s|sh|ch|x|z)$");
	private static final Pattern PLURAL_PATTERN_VOWEL_Y = Pattern.compile(".*?[aeiou]y$");
	private static final Pattern PLURAL_PATTERN_CONSONANT_Y = Pattern.compile(".*?[^aeiou]y$");
	private static final Pattern PLURAL_PATTERN_F = Pattern.compile(".*?fe?$");
	private static final Pattern PLURAL_PATTERN_CONSONANT_O = Pattern.compile(".*?[^aeiou]o$");

	private static final Set<String> UNCOUNTABLE = new HashSet<>(List.of("media"));
	private static final Set<String> F_EXCEPTIONS = new HashSet<>(List.of("roof", "cliff", "chief", "belief", "chef"));
	private static final Set<String> CONSONANT_O_EXCEPTIONS = new HashSet<>(List.of("piano", "halo", "photo"));


	private StringHelper(){}


	public static String composeTextFilter(String text){
		try{
			Pattern.compile(text);
		}
		catch(final PatternSyntaxException ignored){
			text = Pattern.quote(text);
		}
		return "(?i)(?:" + text + ")";
	}


	/**
	 * Transforms English words from singular to plural form.
	 * <p>
	 * Examples:
	 * <pre>
	 *    English.plural("word") = "words";
	 *
	 *    English.plural("cat", 1) = "cat";
	 *    English.plural("cat", 2) = "cats";
	 * </pre>
	 * </p>
	 * <p>
	 * Based on <a href="http://www.csse.monash.edu.au/~damian/papers/HTML/Plurals.html">An algorithmic approach to english pluralization</a>
	 * by Damian Conway.
	 * </p>
	 *
	 * @see <a href="https://github.com/atteo/evo-inflector/blob/master/src/main/java/org/atteo/evo/inflector/English.java">English.java</a>
	 */
	public static String pluralize(String word){
		word = StringUtils.replaceChars(word, '_', ' ');
		if(isUncountable(word))
			return word;

		if(PLURAL_PATTERN_S.matcher(word).matches())
			//FIXME words that end in Z sometimes add an extra Z to the plural form of the word
			word += "es";
		else if(PLURAL_PATTERN_VOWEL_Y.matcher(word).matches())
			word += "s";
		else if(PLURAL_PATTERN_CONSONANT_Y.matcher(word).matches())
			word = word.substring(0, word.length() - 1) + "ies";
		else if(PLURAL_PATTERN_F.matcher(word).matches() && !isFException(word))
			word = word.substring(0, word.length() - (word.endsWith("fe")? 2: 1)) + "ves";
		else if(PLURAL_PATTERN_CONSONANT_O.matcher(word).matches() && !isConsonantOException(word))
			word += "es";
		else
			word += "s";
		return word;
	}

	private static boolean isUncountable(final String word){
		return UNCOUNTABLE.contains(word.toLowerCase(Locale.ROOT));
	}

	private static boolean isFException(final String word){
		return F_EXCEPTIONS.contains(word.toLowerCase(Locale.ROOT));
	}

	private static boolean isConsonantOException(final String word){
		return CONSONANT_O_EXCEPTIONS.contains(word.toLowerCase(Locale.ROOT));
	}

}
