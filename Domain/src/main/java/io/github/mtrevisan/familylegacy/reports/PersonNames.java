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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.NameAnatomyService;
import io.github.mtrevisan.familylegacy.io.model.readers.NamePart;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;


/** How the name of a person is written in a report: all the given names or the first and the initials, and what a person with no name is called. */
final class PersonNames{

	private static final String UNKNOWN_FALLBACK = "?";


	private PersonNames(){}


	/**
	 * @param individual	An individual.
	 * @param style	How the given names are written.
	 * @param unknown	What is written for an individual with no name; empty for its identifier.
	 * @return	The name as the sentences of a report tell it (all the parts of the first name that has some), never empty.
	 */
	static String plain(final FLEFRecord individual, final GivenNameStyle style, final String unknown){
		for(final Name name : NameAnatomyService.extractForIndividual(individual)){
			final String text = compose(name.parts(), style, false);
			if(!text.isEmpty())
				return text;
		}
		return unnamed(individual, unknown);
	}

	/**
	 * @param individual	An individual.
	 * @param style	How the given names are written.
	 * @param unknown	What is written for an individual with no name; empty for nothing but the identifier.
	 * @return	The text that identifies the individual, written as {@link IndividualReader#displayText(FLEFRecord)} does ({@code Giovanni B. Rossi [I1]}).
	 */
	static String display(final FLEFRecord individual, final GivenNameStyle style, final String unknown){
		String text = StringUtils.EMPTY;
		for(final Name name : NameAnatomyService.extractForIndividual(individual)){
			text = compose(name.parts(), style, true);
			if(!text.isEmpty())
				break;
		}
		if(text.isEmpty())
			text = unknown;
		return (text.isEmpty()? StringUtils.EMPTY: text + StringUtils.SPACE) + '[' + individual.getId() + ']';
	}

	private static String unnamed(final FLEFRecord individual, final String unknown){
		if(!unknown.isEmpty())
			return unknown;
		return (individual.getId() != null? individual.getId(): UNKNOWN_FALLBACK);
	}

	private static String compose(final List<NamePart> parts, final GivenNameStyle style, final boolean personalOnly){
		final StringJoiner joiner = new StringJoiner(StringUtils.SPACE);
		boolean givenSeen = false;
		for(final NamePart part : parts){
			final String value = part.value();
			if(value == null || value.isBlank() || (personalOnly && NameReader.isPartTypeExcludedFromPersonalName(part.type())))
				continue;

			if(style == GivenNameStyle.FIRST_AND_INITIALS && NameReader.ENUM_PART_TYPE_GIVEN.equals(part.type())){
				joiner.add(abbreviated(value.strip(), givenSeen));
				givenSeen = true;
			}
			else
				joiner.add(value.strip());
		}
		return joiner.toString();
	}

	/**
	 * @param given	The value of a given-name part ({@code Giovanni Battista}), that may be more than one name.
	 * @param afterGiven	Whether a given name came before this part (then its first name is abbreviated too), or this is the first given name
	 * 	of the person (then it is kept in full).
	 * @return	The part with the names but the first one (if kept) reduced to the initial ({@code Giovanni B.}).
	 */
	private static String abbreviated(final String given, final boolean afterGiven){
		final String[] words = StringUtils.split(given, ' ');
		final StringJoiner joiner = new StringJoiner(StringUtils.SPACE);
		for(int i = 0; i < words.length; i ++)
			joiner.add(i == 0 && !afterGiven? words[i]: initial(words[i]));
		return joiner.toString();
	}

	/** @return	The initial of a name ({@code B.}, {@code J.-P.} for a compound one); an initial is left as it is. */
	private static String initial(final String word){
		if(word.endsWith(".") && word.length() <= 3)
			return word;

		final StringJoiner joiner = new StringJoiner("-");
		for(final String piece : StringUtils.split(word, '-'))
			joiner.add(new String(Character.toChars(piece.codePointAt(0))) + '.');
		return joiner.toString();
	}

}
