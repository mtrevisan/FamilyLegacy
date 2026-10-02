package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.names.NameAnatomyService;
import io.github.mtrevisan.familylegacy.io.model.readers.names.NamePart;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.awt.Rectangle;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;


/**
 * Handler for INDIVIDUAL records.
 * <p>
 * Structure:
 * <pre>
 * // A representation of a historical individual together with the facts, events, relationships, and evidence believed to apply to that individual.
 * record IndividualRecord {
 *   name*: PersonalNameStructure   // names associated with this individual (birth name, married name, alias, etc.)
 *   sex?: enum {                   // Biological sex as recorded in sources or inferred from evidence. This represents a genealogical conclusion and may be revised as additional evidence becomes available.
 *     male, female, unknown
 *   }
 *   source*: SourceCitation        // source citations supporting facts about this individual
 *   note*: NoteStructure           // free-text notes about this individual
 *   preferred_image?: struct {
 *     uri: Uri          // Resource URI pointing to a preferred image of this individual.
 *     crop?: CropRect   // normalized rectangle identifying the region of interest within the image
 *   }
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class IndividualReader{

	public static final String TAG_NAME = "name";
	public static final String TAG_SEX = "sex";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PREFERRED_IMAGE = "preferred_image";
	private static final String TAG_PREFERRED_IMAGE_URI = FLEFRecordHelper.composePath(TAG_PREFERRED_IMAGE, "uri");
	private static final String TAG_PREFERRED_IMAGE_CROP = FLEFRecordHelper.composePath(TAG_PREFERRED_IMAGE, "crop");
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	static final String ENUM_SEX_MALE = "male";
	static final String ENUM_SEX_FEMALE = "female";
	static final String ENUM_SEX_UNKNOWN = "unknown";
	public static final String[] SEXES = {
		ENUM_SEX_MALE,
		ENUM_SEX_FEMALE,
		ENUM_SEX_UNKNOWN
	};


	private IndividualReader(){}


	public static String extractRawSex(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_SEX);
	}

	public static SexType extractSex(final FLEFRecord individual){
		final String targetRawSex = extractRawSex(individual);
		return (targetRawSex != null
			? Enum.valueOf(SexType.class, targetRawSex.toUpperCase(Locale.ROOT))
			: SexType.UNKNOWN);
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


	public static String extractPreferredImageUri(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PREFERRED_IMAGE_URI);
	}

	public static Rectangle extractPreferredImageCrop(final FLEFRecord record){
		final FLEFRecord crop = FLEFRecordHelper.findChild(record, TAG_PREFERRED_IMAGE_CROP);
		return CropReader.extractPreferredImageCrop(crop);
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
			// Skip parts having a type explicitly classified as acquired or contextual
			.filter(name -> name.parts().stream()
				.noneMatch(part -> NameReader.isPartTypeExcludedFromPersonalName(part.type())))
			.map(IndividualReader::parsePersonalName)
			.filter(StringUtils::isNotEmpty)
			.toList();
	}

	public static String extractPrimaryFullname(final FLEFRecord individual){
		final List<String> fullnames = extractFullNames(individual);
		return (!fullnames.isEmpty()? fullnames.getFirst(): null);
	}

	private static String parsePersonalName(final Name name){
		final StringJoiner fullName = new StringJoiner(StringUtils.SPACE);
		for(final NamePart part : name.parts()){
			final String value = part.value();
			if(value != null)
				fullName.add(value);
		}
		return fullName.toString();
	}

}
