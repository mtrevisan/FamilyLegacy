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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Splits the full name typed in one line into the parts of a {@code PersonalNameStructure}.
 * <p>
 * The parser is a convenience, not an authority: it applies the usual conventions and gives a proposal, which the user sees and may
 * correct (see {@link ParsedName}). A name it cannot understand is a given name. It recognizes:
 * <ul>
 * 	<li>the plain order, <i>given family</i>: the last word is the family name, together with the particles that precede it (<i>de la</i>,
 * 		<i>van der</i>, <i>von</i>... see {@link NameParticles}), which are a part of their own ({@code family_prefix}), as the importers do;</li>
 * 	<li>two family names joined by <i>y</i>, <i>e</i> or <i>i</i> (<i>Ortega y Gasset</i>), kept as one family name;</li>
 * 	<li>the inverted order, <i>family, given</i> (<i>Rossi, Giovanni</i>), written in the plain order in the result;</li>
 * 	<li>the family name between slashes, as GEDCOM does (<i>Giovanni /Rossi/</i>), which settles any doubt;</li>
 * 	<li>the family name in capital letters in front of the given names (<i>ROSSI Giovanni</i>), kept in that order;</li>
 * 	<li>the honorifics in front (<i>Dr.</i>, {@code prefix}), the generation marks at the end (<i>Jr.</i>, <i>III</i>, {@code generation}),
 * 		the nickname between quotes (<i>Giovanni "Gianni" Rossi</i>, {@code nickname}).</li>
 * </ul>
 * A family name that is more than the last word (<i>Calder&oacute;n de la Barca</i>, <i>Garc&iacute;a M&aacute;rquez</i>) cannot be told from
 * a middle given name: it is the user who writes it between slashes or corrects the proposal.
 */
public final class FullNameParser{

	private static final String PART_PREFIX = NameReader.ENUM_PART_TYPE_PREFIX;
	private static final String PART_GIVEN = NameReader.ENUM_PART_TYPE_GIVEN;
	private static final String PART_FAMILY_PREFIX = NameReader.ENUM_PART_TYPE_FAMILY_PREFIX;
	private static final String PART_FAMILY = NameReader.ENUM_PART_TYPE_FAMILY;
	private static final String PART_GENERATION = NameReader.ENUM_PART_TYPE_GENERATION;
	private static final String PART_SUFFIX = NameReader.ENUM_PART_TYPE_SUFFIX;
	private static final String PART_NICKNAME = NameReader.ENUM_PART_TYPE_NICKNAME;

	private static final char SLASH = '/';
	private static final char COMMA = ',';


	/** A word of the name; a quoted one is a nickname. */
	private record Token(String text, boolean quoted){}

	/** The parts being built: the consecutive words of the same type are a single part (but each nickname is a part of its own). */
	private static final class Parts{

		private final List<ParsedName.Part> parts = new ArrayList<>();


		void add(final String type, final String value){
			final int last = parts.size() - 1;
			if(last >= 0 && !PART_NICKNAME.equals(type) && parts.get(last).type().equals(type))
				parts.set(last, new ParsedName.Part(type, parts.get(last).value() + StringUtils.SPACE + value));
			else
				parts.add(new ParsedName.Part(type, value));
		}

		ParsedName build(){
			return (parts.isEmpty()? ParsedName.EMPTY: new ParsedName(parts));
		}

	}


	private FullNameParser(){}


	/**
	 * @param fullName	The name, as typed (may be {@code null}).
	 * @return	The parts of the name, in order ({@link ParsedName#EMPTY} if the text is blank).
	 */
	public static ParsedName parse(final String fullName){
		final String text = StringUtils.normalizeSpace(fullName);
		if(StringUtils.isEmpty(text))
			return ParsedName.EMPTY;

		final Parts parts = new Parts();
		final int open = text.indexOf(SLASH);
		if(open >= 0){
			//the family name is marked: what comes before is the given names, what comes after the suffixes
			final int close = text.indexOf(SLASH, open + 1);
			final String before = text.substring(0, open);
			final String after = (close >= 0? text.substring(close + 1): StringUtils.EMPTY);
			emitGiven(tokenize(before), parts);
			emitFamily(tokenize(close < 0? text.substring(open + 1): text.substring(open + 1, close)), parts);
			//with nothing in front, what follows is the given names (family name first), otherwise the suffixes
			if(StringUtils.isBlank(before))
				emitGiven(tokenize(after), parts);
			else
				emitTrailing(tokenize(after), parts);
			return parts.build();
		}

		final List<String> segments = new ArrayList<>();
		for(final String segment : StringUtils.split(text, COMMA))
			if(StringUtils.isNotBlank(segment))
				segments.add(segment.strip());
		String generation = null;
		if(segments.size() > 1){
			final List<Token> last = tokenize(segments.getLast());
			if(last.size() == 1 && !last.getFirst().quoted() && NameParticles.isGeneration(last.getFirst().text())){
				generation = StringUtils.stripEnd(last.getFirst().text(), String.valueOf(COMMA));
				segments.removeLast();
			}
		}

		if(segments.size() > 1){
			//the inverted order, family first: it is written in the plain order
			emitGiven(tokenize(String.join(StringUtils.SPACE, segments.subList(1, segments.size()))), parts);
			emitFamily(tokenize(segments.getFirst()), parts);
		}
		else{
			final List<Token> single = tokenize(segments.isEmpty()? StringUtils.EMPTY: segments.getFirst());
			if(single.size() == 1 && text.indexOf(COMMA) >= 0)
				//a lone name with a comma tells which one it is: the family name if the comma follows (Rossi,), else the given name (, Giovanni)
				if(text.charAt(0) == COMMA)
					emitGiven(single, parts);
				else
					emitFamily(single, parts);
			else
				emitPlain(single, parts);
		}
		if(generation != null)
			parts.add(PART_GENERATION, generation);
		return parts.build();
	}


	/** Splits the text into words, taking apart the ones between quotes (they are nicknames). */
	private static List<Token> tokenize(final String text){
		final List<Token> tokens = new ArrayList<>();
		final int length = text.length();
		int i = 0;
		while(i < length){
			final char c = text.charAt(i);
			if(Character.isWhitespace(c)){
				i ++;
				continue;
			}

			final char closing = closingQuote(c);
			if(closing != 0){
				final int end = findClosingQuote(text, i + 1, closing);
				if(end > 0){
					final String nickname = text.substring(i + 1, end).strip();
					if(!nickname.isEmpty())
						tokens.add(new Token(nickname, true));
					i = end + 1;
					continue;
				}
			}

			int j = i;
			while(j < length && !Character.isWhitespace(text.charAt(j)))
				j ++;
			tokens.add(new Token(text.substring(i, j), false));
			i = j;
		}
		return tokens;
	}

	/** @return	The quote that closes the one given, 0 if the character is not an opening quote. */
	private static char closingQuote(final char opening){
		return switch(opening){
			case '"' -> '"';
			case '“' -> '”';
			case '„' -> '“';
			case '«' -> '»';
			case '‘' -> '’';
			default -> 0;
		};
	}

	/** @return	The index of the closing quote that ends a word (an apostrophe inside a word, like in {@code D'Angelo}, does not), -1 if none. */
	private static int findClosingQuote(final String text, final int from, final char closing){
		for(int i = from; i < text.length(); i ++){
			if(text.charAt(i) != closing)
				continue;

			if(i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1)) || text.charAt(i + 1) == COMMA || text.charAt(i + 1) == SLASH)
				return i;
		}
		return -1;
	}


	private static List<Integer> unquoted(final List<Token> tokens){
		final List<Integer> indices = new ArrayList<>(tokens.size());
		for(int i = 0; i < tokens.size(); i ++)
			if(!tokens.get(i).quoted())
				indices.add(i);
		return indices;
	}

	/** @return	The types of the words, in order: the nicknames are known, the others are to be set. */
	private static String[] newTypes(final List<Token> tokens){
		final String[] types = new String[tokens.size()];
		for(int i = 0; i < types.length; i ++)
			if(tokens.get(i).quoted())
				types[i] = PART_NICKNAME;
		return types;
	}

	private static void flush(final List<Token> tokens, final String[] types, final Parts parts){
		for(int i = 0; i < types.length; i ++)
			if(types[i] != null)
				parts.add(types[i], tokens.get(i).text());
	}


	/** The words that are the given names (there may be honorifics in front). */
	private static void emitGiven(final List<Token> tokens, final Parts parts){
		final List<Integer> words = unquoted(tokens);
		final String[] types = newTypes(tokens);
		int start = 0;
		while(start < words.size() - 1 && NameParticles.isHonorific(tokens.get(words.get(start)).text()))
			types[words.get(start ++)] = PART_PREFIX;
		for(int i = start; i < words.size(); i ++)
			types[words.get(i)] = PART_GIVEN;
		flush(tokens, types, parts);
	}

	/** The words that are known to be the family name (the particles in front are a part of their own). */
	private static void emitFamily(final List<Token> tokens, final Parts parts){
		final List<Integer> words = unquoted(tokens);
		final String[] types = newTypes(tokens);
		labelFamily(tokens, words, 0, words.size(), types);
		flush(tokens, types, parts);
	}

	/** The words after the family name: the generation marks, and the suffixes. */
	private static void emitTrailing(final List<Token> tokens, final Parts parts){
		final String[] types = newTypes(tokens);
		for(int i = 0; i < types.length; i ++)
			if(types[i] == null)
				types[i] = (NameParticles.isGeneration(tokens.get(i).text())? PART_GENERATION: PART_SUFFIX);
		flush(tokens, types, parts);
	}

	/** Labels the words {@code from} (included) to {@code to} (excluded): the particles in front are the prefix, the rest is the family name. */
	private static void labelFamily(final List<Token> tokens, final List<Integer> words, final int from, final int to, final String[] types){
		int i = from;
		while(i < to - 1 && NameParticles.isParticle(tokens.get(words.get(i)).text()))
			types[words.get(i ++)] = PART_FAMILY_PREFIX;
		for(; i < to; i ++)
			types[words.get(i)] = PART_FAMILY;
	}

	/** The name in one piece: the honorifics, the given names, the family name (the last word and what goes with it), the generation. */
	private static void emitPlain(final List<Token> tokens, final Parts parts){
		final List<Integer> words = unquoted(tokens);
		final String[] types = newTypes(tokens);
		final int count = words.size();
		if(count == 0){
			flush(tokens, types, parts);
			return;
		}

		int start = 0;
		while(start < count - 1 && NameParticles.isHonorific(tokens.get(words.get(start)).text()))
			types[words.get(start ++)] = PART_PREFIX;
		final boolean honorific = (start > 0);
		int end = count;
		if(end - start > 1 && NameParticles.isGeneration(tokens.get(words.get(end - 1)).text()))
			types[words.get(-- end)] = PART_GENERATION;

		if(end - start == 1)
			//a lone word is the given name, but after an honorific (Dr. Rossi) it is the family name
			types[words.get(start)] = (honorific? PART_FAMILY: PART_GIVEN);
		else{
			int upperEnd = start;
			while(upperEnd < end && isUpperWord(tokens.get(words.get(upperEnd)).text()))
				upperEnd ++;
			if(upperEnd > start && upperEnd < end){
				//the family name in capital letters, first (ROSSI Giovanni)
				labelFamily(tokens, words, start, upperEnd, types);
				for(int i = upperEnd; i < end; i ++)
					types[words.get(i)] = PART_GIVEN;
			}
			else{
				int familyStart = end - 1;
				//the family names joined by a connector (Ortega y Gasset) are one
				while(familyStart - 2 >= start && NameParticles.isConnector(tokens.get(words.get(familyStart - 1)).text()))
					familyStart -= 2;
				while(familyStart - 1 >= start && NameParticles.isParticle(tokens.get(words.get(familyStart - 1)).text()))
					familyStart --;
				//a word that is a given name as well (Al Pacino) is a particle only after a given name
				while(familyStart == start && familyStart < end - 1 && NameParticles.isWeakParticle(tokens.get(words.get(familyStart)).text()))
					familyStart ++;
				for(int i = start; i < familyStart; i ++)
					types[words.get(i)] = PART_GIVEN;
				labelFamily(tokens, words, familyStart, end, types);
			}
		}
		flush(tokens, types, parts);
	}

	/** @return	Whether the word is written in capital letters (at least two letters, as {@code J} is an initial). */
	private static boolean isUpperWord(final String word){
		int letters = 0;
		for(int i = 0; i < word.length(); i ++){
			final char c = word.charAt(i);
			if(!Character.isLetter(c))
				continue;

			if(!Character.isUpperCase(c))
				return false;
			letters ++;
		}
		return (letters >= 2);
	}

}
