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

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * What {@link FullNameParser} understood of a name: the parts of a {@code PersonalNameStructure}, in the order they were written.
 * <p>
 * It is a proposal, not an interpretation: it can be shown to the user, corrected (the list of parts is just data) and then turned into
 * the structure of the file with {@link #toRecord()}.
 *
 * @param parts	The parts of the name, in order.
 */
public record ParsedName(List<Part> parts){

	/**
	 * A part of a name.
	 *
	 * @param type	The type of the part (one of {@link NameReader#PART_TYPES}).
	 * @param value	The text of the part (not blank).
	 */
	public record Part(String type, String value){}


	/** The name that has no part. */
	public static final ParsedName EMPTY = new ParsedName(List.of());


	public ParsedName{
		parts = List.copyOf(parts);
	}


	/** @return	Whether the name has no part. */
	public boolean isEmpty(){
		return parts.isEmpty();
	}

	/** @return	The text of the parts that make the name the way it is shown (the nicknames, the titles... left out), separated by a space. */
	public String fullName(){
		final StringBuilder sb = new StringBuilder();
		for(final Part part : parts)
			if(!NameReader.isPartTypeExcludedFromPersonalName(part.type())){
				if(!sb.isEmpty())
					sb.append(StringUtils.SPACE);
				sb.append(part.value());
			}
		return sb.toString();
	}

	/**
	 * @return	The {@code name} structure of an individual that holds the parts (without type, as the name that was given), {@code null}
	 * 	if there is no part.
	 */
	public FLEFRecord toRecord(){
		if(parts.isEmpty())
			return null;

		final FLEFRecord name = FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME);
		for(final Part part : parts)
			name.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_TYPE, part.type()))
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_VALUE, part.value())));
		return name;
	}

}
