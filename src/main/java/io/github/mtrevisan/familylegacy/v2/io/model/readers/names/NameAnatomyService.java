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
package io.github.mtrevisan.familylegacy.v2.io.model.readers.names;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.IndividualReader;
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
 * <p>
 * Structure:
 * <pre>
 * // A structured personal name composed of one or more name parts. This structure is intended to support naming systems from all cultures, including given
 * // names, family names, patronymics, matronymics, clan names, lineage names, titles, and other naming components. The order of PART elements is
 * // significant and should reflect the historical or culturally appropriate representation of the name.
 * struct PersonalNameStructure {
 *   type?: enum {
 *     // marital status and origins at birth
 *     official, religious, birth,
 *     // changes in marital status and family events
 *     married, maiden, divorce, adoption, fostering,
 *     // legal, immigration, and naturalization changes
 *     legal, immigrant, adapted,
 *     // informal, stage, and social names
 *     alias, nickname, artistic, professional, user,
 *     // historical and dynastic contexts
 *     regnal, slave_name
 *   } | Text
 *   part+: PartStructure   // The order of PART elements is significant and reflects the culturally appropriate representation of the full name (e.g., 'given' then 'family' for Western names, 'family' then 'given' for East Asian names). No semantic ordering is implied by the individual part types. Consumers must preserve the original order.
 *   locale?: LocaleCode | Text
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;   // Unlike ContextImpactRecord, CULTURAL_NORM here describes the naming system that governs the structure of the name itself.
 *   source*: SourceCitation
 *   note*: NoteStructure
 * }
 *
 * // A textual designation associated with an entity, source, place, organization, repository, group, or other object.
 * // This structure represents a complete textual expression and may be used for names, titles, labels, or similar identifying text.
 * // Unlike PersonalNameStructure, this structure does not decompose the text into culturally-specific components such as given names, family names,
 * // patronymics, titles, or lineage elements.
 * struct NameStructure {
 *   type?: enum {
 *     // official and legal names
 *     official, legal,
 *     // historical naming traditions
 *     colonial, indigenous, traditional,
 *     // language and localization variants
 *     translated, transcribed,
 *     // historical variants
 *     historic, former,
 *     // common usage
 *     common, colloquial,
 *     // abbreviated forms
 *     abbreviated, acronym,
 *     // religious and ecclesiastical forms
 *     religious,
 *     // administrative and archival forms
 *     administrative, archival
 *   } | Text
 *   value: Text                   // the primary textual value
 *   locale?: LocaleCode | Text
 *   variant*: TextValueVariant    // alternative phonetic, transliterated, or transcribed representations
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;   // Unlike ContextImpactRecord, CULTURAL_NORM here describes the naming system that governs the structure of the name itself.
 *   source*: SourceCitation
 *   note*: NoteStructure
 * }
 * </pre>
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


	/* ======================================================================
	 *                          Personal name
	 * ====================================================================== */

	private static Name parsePersonalName(final FLEFRecord nameRecord){
		final String type = FLEFRecordHelper.getChildValue(nameRecord, Name.TAG_TYPE);

		final List<FLEFRecord> partStructs = FLEFRecordHelper.findChildren(nameRecord, Name.TAG_PART);
		final List<NamePart> parts = new ArrayList<>();
		for(final FLEFRecord partRecord : partStructs)
			parts.add(NamePart.parsePart(partRecord));

		final String locale = FLEFRecordHelper.getChildValue(nameRecord, Name.TAG_LOCALE);

		final List<String> culturalNormIds = nameRecord.extractReferenceIds(Name.TAG_CULTURAL_NORM);

		final List<FLEFRecord> sources = FLEFRecordHelper.findChildren(nameRecord, Name.TAG_SOURCE);

		final List<FLEFRecord> notes = FLEFRecordHelper.findChildren(nameRecord, Name.TAG_NOTE);

		return new Name(type, parts, locale, StringUtils.EMPTY, List.of(), culturalNormIds, sources, notes, nameRecord);
	}

	/* ======================================================================
	 *                          Generic name
	 * ====================================================================== */

	private static Name parseGenericName(final FLEFRecord nameRecord){
		final String type = FLEFRecordHelper.getChildValue(nameRecord, Name.TAG_TYPE);

		final String value = FLEFRecordHelper.getChildValue(nameRecord, Name.TAG_VALUE);

		final String locale = FLEFRecordHelper.getChildValue(nameRecord, Name.TAG_LOCALE);

		final List<TextValueVariant> variants = NamePart.extractVariants(nameRecord);

		final List<String> culturalNormIds = nameRecord.extractReferenceIds(Name.TAG_CULTURAL_NORM);

		final List<FLEFRecord> sources = FLEFRecordHelper.findChildren(nameRecord, Name.TAG_SOURCE);

		final List<FLEFRecord> notes = FLEFRecordHelper.findChildren(nameRecord, Name.TAG_NOTE);

		return new Name(type, List.of(), locale, value, variants, culturalNormIds, sources, notes, nameRecord);
	}

}
