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

	public static final String TYPE = "CULTURAL_NORM";
	public static final String ID_PREFIX = "CN";

	public static final String TAG_TITLE = "title";
	public static final String TAG_TYPE = "type";
	public static final String TAG_PLACE = "place";
	public static final String TAG_VALID_FROM = "valid_from";
	public static final String TAG_VALID_TO = "valid_to";
	public static final String TAG_NOTE = "note";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_CONFIDENCE = "confidence";
	public static final String TAG_AUDIT = "audit";

	public static final String TAG_PLACE_EVIDENCE = FLEFRecordHelper.composePath(TAG_PLACE, TAG_EVIDENCE);

	public static final String ENUM_TYPE_AGE_OF_MAJORITY = "age_of_majority";
	public static final String ENUM_TYPE_MARRIAGE_MINIMUM_AGE = "marriage_minimum_age";
	public static final String ENUM_TYPE_BAPTISM_AGE = "baptism_age";
	public static final String ENUM_TYPE_CONFIRMATION_AGE = "confirmation_age";
	public static final String ENUM_TYPE_MILITARY_SERVICE_AGE = "military_service_age";
	public static final String ENUM_TYPE_RETIREMENT_AGE = "retirement_age";
	public static final String ENUM_TYPE_NAMING_CONVENTION = "naming_convention";
	public static final String ENUM_TYPE_SURNAME_TRANSMISSION = "surname_transmission";
	public static final String ENUM_TYPE_PATRONYMIC_SYSTEM = "patronymic_system";
	public static final String ENUM_TYPE_MATRONYMIC_SYSTEM = "matronymic_system";
	public static final String ENUM_TYPE_TITLE_USAGE = "title_usage";
	public static final String ENUM_TYPE_INHERITANCE_RULE = "inheritance_rule";
	public static final String ENUM_TYPE_SUCCESSION_RULE = "succession_rule";
	public static final String ENUM_TYPE_DOWRY_PRACTICE = "dowry_practice";
	public static final String ENUM_TYPE_GUARDIANSHIP_RULE = "guardianship_rule";
	public static final String ENUM_TYPE_ADOPTION_PRACTICE = "adoption_practice";
	public static final String ENUM_TYPE_MARRIAGE_PRACTICE = "marriage_practice";
	public static final String ENUM_TYPE_MARRIAGE_PROHIBITED_DEGREE = "marriage_prohibited_degree";
	public static final String ENUM_TYPE_WIDOWHOOD_RULE = "widowhood_rule";
	public static final String ENUM_TYPE_RESIDENCE_PATTERN = "residence_pattern";
	public static final String ENUM_TYPE_HOUSEHOLD_STRUCTURE = "household_structure";
	public static final String ENUM_TYPE_SOCIAL_CLASSIFICATION = "social_classification";
	public static final String ENUM_TYPE_RELIGIOUS_PRACTICE = "religious_practice";
	public static final String ENUM_TYPE_BURIAL_PRACTICE = "burial_practice";
	public static final String ENUM_TYPE_CITIZENSHIP_RULE = "citizenship_rule";
	public static final String ENUM_TYPE_LEGITIMACY_RULE = "legitimacy_rule";
	public static final String ENUM_TYPE_AGE_DIFFERENCE_CONVENTION = "age_difference_convention";
	public static final String ENUM_TYPE_GENERATIONAL_INTERVAL = "generational_interval";
	public static final String[] TYPES = new String[]{
		// Lifecycle and age-related customs:
		ENUM_TYPE_AGE_OF_MAJORITY, ENUM_TYPE_MARRIAGE_MINIMUM_AGE, ENUM_TYPE_BAPTISM_AGE, ENUM_TYPE_CONFIRMATION_AGE, ENUM_TYPE_MILITARY_SERVICE_AGE,
		ENUM_TYPE_RETIREMENT_AGE,
		// Naming practices:
		ENUM_TYPE_NAMING_CONVENTION, ENUM_TYPE_SURNAME_TRANSMISSION, ENUM_TYPE_PATRONYMIC_SYSTEM, ENUM_TYPE_MATRONYMIC_SYSTEM, ENUM_TYPE_TITLE_USAGE,
		// Family and household customs:
		ENUM_TYPE_INHERITANCE_RULE, ENUM_TYPE_SUCCESSION_RULE, ENUM_TYPE_DOWRY_PRACTICE, ENUM_TYPE_GUARDIANSHIP_RULE, ENUM_TYPE_ADOPTION_PRACTICE,
		// Marriage customs:
		ENUM_TYPE_MARRIAGE_PRACTICE, ENUM_TYPE_MARRIAGE_PROHIBITED_DEGREE, ENUM_TYPE_WIDOWHOOD_RULE,
		// Residence and social organization:
		ENUM_TYPE_RESIDENCE_PATTERN, ENUM_TYPE_HOUSEHOLD_STRUCTURE, ENUM_TYPE_SOCIAL_CLASSIFICATION,
		// Religious and ecclesiastical customs:
		ENUM_TYPE_RELIGIOUS_PRACTICE, ENUM_TYPE_BURIAL_PRACTICE,
		// Legal and citizenship rules:
		ENUM_TYPE_CITIZENSHIP_RULE, ENUM_TYPE_LEGITIMACY_RULE,
		// Genealogical inference rules:
		ENUM_TYPE_AGE_DIFFERENCE_CONVENTION, ENUM_TYPE_GENERATIONAL_INTERVAL
	};


	private CulturalNormReader(){}

	/** @return whether the record is of this type (its tag is the type, ignoring case) */
	public static boolean isOfType(final FLEFRecord record){
		return (record != null && TYPE.equalsIgnoreCase(record.getTag()));
	}



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
