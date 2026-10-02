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
package io.github.mtrevisan.familylegacy.io.model.readers.names;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Extracts the name anatomy of an individual or a group from a FLEF model.
 * <p>
 * <b>Personal names.</b> For an individual, each {@code name} child is a
 * {@code PersonalNameStructure} with an ordered list of {@code part}
 * children, each carrying a {@code type}, a {@code value} and optional
 * {@code variant} children. The service preserves the order of the parts
 * and normalizes empty fields to empty strings.
 * <p>
 * <b>Generic names.</b> For a group (and for any other entity that uses
 * the generic {@code NameStructure}), each {@code name} child carries a
 * single {@code value} plus optional variants on the name itself, with no
 * parts.
 * <p>
 * The service is stateless and not thread-safe; use it from the Swing
 * Event Dispatch Thread.
 */
public final class NameAnatomyService{

	private NameAnatomyService(){}


	/**
	 * Extracts the anatomy of every name of the given individual.
	 *
	 * @param individual the individual
	 * @return the ordered list of names, never {@code null}
	 */
	public static List<Name> extractForIndividual(final FLEFRecord individual){
		final List<Name> result = new ArrayList<>();
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_NAME);
		for(final FLEFRecord name : names)
			result.add(parsePersonalName(name));
		return result;
	}

	/**
	 * Extracts the anatomy of every name of the given group or place.
	 *
	 * @param group the group
	 * @return the ordered list of names, never {@code null}
	 */
	public static List<Name> extractForGeneric(final FLEFRecord group){
		final List<Name> result = new ArrayList<>();
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(group, GroupReader.TAG_NAME);
		for(final FLEFRecord name : names)
			result.add(parseGenericName(name));
		return result;
	}

	/**
	 * Extracts the anatomy of every name of the given source.
	 *
	 * @param source the source
	 * @return the ordered list of names, never {@code null}
	 */
	public static List<Name> extractForSource(final FLEFRecord source){
		final List<Name> result = new ArrayList<>();
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(source, SourceReader.TAG_TITLE);
		for(final FLEFRecord name : names)
			result.add(parseGenericName(name));
		return result;
	}


	/* ======================================================================
	 *                          Personal name
	 * ====================================================================== */

	private static Name parsePersonalName(final FLEFRecord nameRecord){
		final String type = NameReader.extractType(nameRecord);

		final List<FLEFRecord> partStructs = FLEFRecordHelper.findChildren(nameRecord, NameReader.TAG_PART);
		final List<NamePart> parts = new ArrayList<>();
		for(final FLEFRecord partRecord : partStructs)
			parts.add(NamePart.parsePart(partRecord));

		final String locale = NameReader.extractLocale(nameRecord);

		final List<String> culturalNormIds = nameRecord.extractReferenceIds(NameReader.TAG_CULTURAL_NORM);

		final List<FLEFRecord> sources = FLEFRecordHelper.findChildren(nameRecord, NameReader.TAG_SOURCE);

		final List<FLEFRecord> notes = FLEFRecordHelper.findChildren(nameRecord, NameReader.TAG_NOTE);

		return new Name(type, parts, locale, StringUtils.EMPTY, List.of(), culturalNormIds, sources, notes, nameRecord);
	}

	/* ======================================================================
	 *                          Generic name
	 * ====================================================================== */

	private static Name parseGenericName(final FLEFRecord nameRecord){
		final String type = NameReader.extractType(nameRecord);

		final String value = NameReader.extractValue(nameRecord);

		final String locale = NameReader.extractLocale(nameRecord);

		final List<TextValueVariant> variants = NamePart.extractVariants(nameRecord);

		final List<String> culturalNormIds = nameRecord.extractReferenceIds(NameReader.TAG_CULTURAL_NORM);

		final List<FLEFRecord> sources = FLEFRecordHelper.findChildren(nameRecord, NameReader.TAG_SOURCE);

		final List<FLEFRecord> notes = FLEFRecordHelper.findChildren(nameRecord, NameReader.TAG_NOTE);

		return new Name(type, List.of(), locale, value, variants, culturalNormIds, sources, notes, nameRecord);
	}

}
