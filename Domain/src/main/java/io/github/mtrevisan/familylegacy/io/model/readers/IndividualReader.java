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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.awt.Rectangle;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;


/**
 * Handler for INDIVIDUAL records.
 * <p>
 * Structure:
 * <pre>
 * // A representation of a historical individual together with the facts, events, relationships, and evidence believed to apply to that individual.
 * record IndividualRecord {
 *   name*: PersonalNameStructure   // names associated with this individual (birth name, married name, alias, etc.)
 *   source*: SourceCitation        // the evidence that this individual existed; facts about the individual (its sex included, as a SEX IndividualAttributeRecord) are cited on their own records
 *   note*: NoteStructure           // free-text notes about this individual
 *   preferred_image?: struct {
 *     media: Xref&lt;MediaRecord&gt;
 *     crop?: CropRect   // normalized rectangle identifying the region of interest within the image
 *   }
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class IndividualReader{

	public static final String TYPE = "INDIVIDUAL";
	public static final String ID_PREFIX = "I";

	public static final String TAG_NAME = "name";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PREFERRED_IMAGE = "preferred_image";
	private static final String TAG_PREFERRED_IMAGE_DOCUMENT = FLEFRecordHelper.composePath(TAG_PREFERRED_IMAGE, CropReader.TAG_MEDIA);
	private static final String TAG_PREFERRED_IMAGE_CROP = FLEFRecordHelper.composePath(TAG_PREFERRED_IMAGE, CropReader.TAG_CROP);
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	/** The values of a SEX individual attribute (see {@link IndividualAttributeReader#ENUM_TYPE_SEX} and {@link PreferredSex}). */
	public static final String ENUM_SEX_MALE = "male";
	public static final String ENUM_SEX_FEMALE = "female";
	public static final String ENUM_SEX_UNKNOWN = "unknown";
	public static final String[] SEXES = {
		ENUM_SEX_MALE,
		ENUM_SEX_FEMALE,
		ENUM_SEX_UNKNOWN
	};


	private IndividualReader(){}

	/** @return whether the record is of this type (its tag is the type, ignoring case) */
	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}



	public static boolean isSexMale(final String sex){
		return Strings.CI.equals(sex, ENUM_SEX_MALE);
	}

	public static boolean isSexFemale(final String sex){
		return Strings.CI.equals(sex, ENUM_SEX_FEMALE);
	}

	public static boolean isSexUnknown(final String sex){
		return Strings.CI.equals(sex, ENUM_SEX_UNKNOWN);
	}

	/**
	 * Returns whether the two individuals are of opposite sex.
	 * Used to decide whether systems that only concern man-woman marriage
	 * apply to this pair.
	 */
	public static boolean areOppositeSex(final String sexA, final String sexB){
		return (ENUM_SEX_MALE.equals(sexA) && !ENUM_SEX_MALE.equals(sexB)
			|| ENUM_SEX_FEMALE.equals(sexA) && !ENUM_SEX_FEMALE.equals(sexB));
	}

	public static String getOppositeSex(final String sex){
		if(isSexMale(sex))
			return ENUM_SEX_FEMALE;
		else if(isSexFemale(sex))
			return ENUM_SEX_MALE;
		return ENUM_SEX_UNKNOWN;
	}


	public static String extractPreferredImageUri(final FLEFRecord record, final RecordLookup model){
		final String documentId = FLEFRecordHelper.getChildValue(record, TAG_PREFERRED_IMAGE_DOCUMENT);
		final FLEFRecord document = model.getRecordById(documentId);
		return MediaReader.extractUri(document);
	}

	public static Rectangle extractPreferredImageCrop(final FLEFRecord record){
		final FLEFRecord crop = FLEFRecordHelper.findChild(record, TAG_PREFERRED_IMAGE_CROP);
		return CropReader.extractPreferredImageCrop(crop);
	}


	/**
	 * Text that identifies an individual to the user: the first full name followed by the ID in brackets.
	 *
	 * @param individual the IndividualRecord (may be {@code null})
	 * @return the text, {@code "--"} if the record is {@code null}
	 */
	public static String displayText(final FLEFRecord individual){
		if(individual == null)
			return "--";

		final List<String> names = extractFullNames(individual);
		final String name = (!names.isEmpty()? names.getFirst(): null);

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(name))
			sb.append(name)
				.append(' ');
		sb.append('[')
			.append(individual.getId())
			.append(']');
		return sb.toString();
	}

	/**
	 * Extracts full names from an IndividualRecord, excluding additional,
	 * assumed, or non-birth name parts (e.g. nicknames, titles, regnal names).
	 *
	 * @param individual the IndividualRecord
	 * @return a list of full name strings with excluded parts omitted
	 */
	public static List<String> extractFullNames(final FLEFRecord individual){
		final List<Name> names = NameAnatomyService.extractForIndividual(individual);
		return names.stream()
			.map(IndividualReader::parsePersonalName)
			.filter(StringUtils::isNotEmpty)
			.toList();
	}

	/**
	 * The name to show for an individual: the preferred one among its names, by their type. The protocol has no preferred assertion
	 * for names (a conclusion cannot point to one), so the choice is by type: the {@code birth} name, then the {@code official} one, then
	 * the {@code legal} one; otherwise the first name that is not an alias or a nickname; otherwise the first one. A name with no
	 * personal part (only a nickname, a title...) is not a candidate.
	 *
	 * @param individual the IndividualRecord
	 * @return the full name, or {@code null} if the individual has none
	 */
	public static String extractPrimaryFullname(final FLEFRecord individual){
		final List<Name> names = NameAnatomyService.extractForIndividual(individual).stream()
			.filter(name -> StringUtils.isNotEmpty(parsePersonalName(name)))
			.toList();
		return (!names.isEmpty()? parsePersonalName(preferredName(names)): null);
	}

	/** @return the name to show among the names of an individual (not empty): by type, then the first that is not informal, then the first */
	static Name preferredName(final List<Name> names){
		for(final String preferredType : PREFERRED_NAME_TYPES)
			for(final Name name : names)
				if(preferredType.equalsIgnoreCase(name.type()))
					return name;
		for(final Name name : names)
			if(!INFORMAL_NAME_TYPES.contains(StringUtils.lowerCase(name.type())))
				return name;
		return names.getFirst();
	}

	/** The types of name that are the name to show, in order of preference. */
	private static final List<String> PREFERRED_NAME_TYPES = List.of(NameReader.ENUM_PERSONAL_TYPE_BIRTH, NameReader.ENUM_PERSONAL_TYPE_OFFICIAL,
		NameReader.ENUM_PERSONAL_TYPE_LEGAL);
	/** The types of name that are not shown if another name is there: informal names. */
	private static final Set<String> INFORMAL_NAME_TYPES = Set.of(NameReader.ENUM_PERSONAL_TYPE_ALIAS,
		NameReader.ENUM_PERSONAL_TYPE_NICKNAME);

	private static String parsePersonalName(final Name name){
		final StringJoiner fullName = new StringJoiner(StringUtils.SPACE);
		final List<NamePart> parts = name.parts();
		for(final NamePart part : parts){
			final String value = part.value();
			if(value != null && !NameReader.isPartTypeExcludedFromPersonalName(part.type()))
				fullName.add(value);
		}
		return fullName.toString();
	}

}
