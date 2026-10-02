package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


/**
 * Handler for CULTURAL NORM records.
 * <p>
 * Structure:
 * <pre>
 * // Genealogical events and individual characteristics at various times and places are influenced by customs, practices, and conditions of their
 * // culture. This affects the interpretation of recorded information and the assumptions made about probable genealogical events when there is no or
 * // little known information. For example, a birth year can be estimated by knowing the date of a religious ceremony which normally involves a child
 * // of 12 years old.
 * record CulturalNormRecord {
 *   title?: Text                 // the title of the rule
 *   type?: enum {           // Broad category of cultural rule. Detailed interpretation and parameters are expressed through notes, citations, and external documentation.
 *     // lifecycle and age-related customs
 *     age_of_majority, marriage_minimum_age, baptism_age, confirmation_age, military_service_age, retirement_age,
 *     // naming practices
 *     naming_convention, surname_transmission, patronymic_system, matronymic_system, title_usage,
 *     // family and household customs
 *     inheritance_rule, succession_rule, dowry_practice, guardianship_rule, adoption_practice,
 *     // marriage customs
 *     marriage_practice, marriage_prohibited_degree, widowhood_rule,
 *     // residence and social organization
 *     residence_pattern, household_structure, social_classification,
 *     // religious and ecclesiastical customs
 *     religious_practice, burial_practice,
 *     // legal and citizenship rules
 *     citizenship_rule, legitimacy_rule,
 *     // genealogical inference rules
 *     age_difference_convention, generational_interval
 *   } | Text
 *   place?: PlaceCitation        // the location in which this cultural norm was in effect
 *   valid_from?: DateStructure   // the date from which this cultural norm was in effect
 *   valid_to?: DateStructure     // the date until which this cultural norm was in effect
 *   note*: NoteStructure
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class CulturalNormReader{

	public static final String TAG_TITLE = "title";
	public static final String TAG_TYPE = "type";
	public static final String TAG_PLACE = "place";
	public static final String TAG_VALID_FROM = "valid_from";
	public static final String TAG_VALID_TO = "valid_to";
	public static final String TAG_NOTE = "note";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_AUDIT = "audit";

	public static final String TAG_PLACE_EVIDENCE = FLEFRecordHelper.composePath(TAG_PLACE, TAG_EVIDENCE);

	public static final String[] TYPES = new String[]{
		// Lifecycle and age-related customs:
		"age_of_majority", "marriage_minimum_age", "baptism_age", "confirmation_age", "military_service_age",
		"retirement_age",
		// Naming practices:
		"naming_convention", "surname_transmission", "patronymic_system", "matronymic_system", "title_usage",
		// Family and household customs:
		"inheritance_rule", "succession_rule", "dowry_practice", "guardianship_rule", "adoption_practice",
		// Marriage customs:
		"marriage_practice", "marriage_prohibited_degree", "widowhood_rule",
		// Residence and social organization:
		"residence_pattern", "household_structure", "social_classification",
		// Religious and ecclesiastical customs:
		"religious_practice", "burial_practice",
		// Legal and citizenship rules:
		"citizenship_rule", "legitimacy_rule",
		// Genealogical inference rules:
		"age_difference_convention", "generational_interval"
	};


	private CulturalNormReader(){}


	public static String extractTitle(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TITLE);
	}

	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}

//	public static String extractName(final FLEFRecord record){
//		return FLEFRecordHelper.getChildValue(record, TAG_NAME);
//	}

}
