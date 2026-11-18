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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.io.merger.TextSimilarity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.Set;


/**
 * How alike two personal names are, with no list of names: the letters decide.
 * <p>
 * The measure is the Jaro-Winkler similarity, the one the record linkage literature found best for personal names (short strings,
 * typing and transcription errors, more weight on the beginning of the name), taken with the threshold of the automatic merge
 * ({@link TextSimilarity}); a second route accepts a lower similarity when the two names have the same consonant skeleton, which catches
 * the spellings that differ in the first letter or in the vowels (Catarina and Katarina, Philip and Filip) and that no prefix would put
 * together. A name with a single letter is an initial: it agrees with the names that begin with it, but weakly.
 * <p>
 * Names shorter than {@value #MIN_FUZZY_LENGTH} letters agree only if they are equal (Ugo and Ugu are two names): there is too little
 * to tell a typing error from another name. Texts are expected normalized (lower case, no accents or punctuation).
 */
public final class NameSimilarity{

	/** How well two names agree, the weakest first. */
	public enum Level{
		NONE, INITIAL, SIMILAR, EXACT;

		boolean isAtLeast(final Level other){
			return (ordinal() >= other.ordinal());
		}
	}

	/** Two names that agree, and how well. */
	public record Match(Level level, String first, String second){

		/** @return	Whether the names agree (even if only as an initial). */
		public boolean agrees(){
			return (level != Level.NONE);
		}
	}


	/** The shortest name that is allowed to agree by similarity of the letters. */
	static final int MIN_FUZZY_LENGTH = 4;
	/** The Jaro-Winkler similarity from which two names are spelling variants of each other. */
	static final double SIMILAR = 0.92;
	/** The lower similarity accepted when the two names have the same consonant skeleton. */
	static final double SIMILAR_SAME_SKELETON = 0.85;

	private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();
	private static final String VOWELS = "aeiou";


	private NameSimilarity(){}


	/**
	 * @param a	A name token, normalized.
	 * @param b	Another one.
	 * @return	How well they agree.
	 */
	public static Level level(final String a, final String b){
		if(a.isEmpty() || b.isEmpty())
			return Level.NONE;
		if(a.equals(b))
			return Level.EXACT;
		if(a.length() == 1 || b.length() == 1)
			return (a.charAt(0) == b.charAt(0)? Level.INITIAL: Level.NONE);
		if(a.length() < MIN_FUZZY_LENGTH || b.length() < MIN_FUZZY_LENGTH)
			return Level.NONE;

		final double similarity = JARO_WINKLER.apply(a, b);
		if(similarity >= SIMILAR || similarity >= SIMILAR_SAME_SKELETON && skeleton(a).equals(skeleton(b)))
			return Level.SIMILAR;
		return Level.NONE;
	}

	/**
	 * @param first	The names of the first record.
	 * @param second	The names of the second record.
	 * @return	The best agreement between a name of the first and a name of the second (the first one found among the best).
	 */
	public static Match best(final Set<String> first, final Set<String> second){
		Match best = new Match(Level.NONE, null, null);
		for(final String a : first)
			for(final String b : second){
				final Level level = level(a, b);
				if(level.isAtLeast(best.level()) && level != best.level()){
					best = new Match(level, a, b);
					if(level == Level.EXACT)
						return best;
				}
			}
		return best;
	}

	/**
	 * The consonants of a name, with the letters that sound alike written the same: {@code catarina} and {@code katarina} are both
	 * {@code ktrn}. It is not a phonetic code of a language, only a key that puts together the spellings that differ in the vowels or
	 * in a letter that stands for the same sound; it is used to block, and to accept a lower similarity.
	 *
	 * @param name	A name, normalized.
	 * @return	The skeleton: the first letter (changed as the others), then the consonants without repetitions.
	 */
	public static String skeleton(final String name){
		final StringBuilder sb = new StringBuilder(name.length());
		final String text = StringUtils.replaceEach(name, new String[]{"ph", "ck", "ch", "sh", "x"},
			new String[]{"f", "k", "k", "s", "ks"});
		char last = 0;
		for(int i = 0; i < text.length(); i ++){
			char c = text.charAt(i);
			c = switch(c){
				case 'c', 'q' -> 'k';
				case 'w' -> 'v';
				case 'y' -> 'i';
				case 'z' -> 's';
				default -> c;
			};
			//the vowels and the h are dropped, but for the first letter
			if(i > 0 && (VOWELS.indexOf(c) >= 0 || c == 'h'))
				continue;
			if(c != last)
				sb.append(c);
			last = c;
		}
		return sb.toString();
	}

}
