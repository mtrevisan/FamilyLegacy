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

import java.util.Set;


/**
 * Handler for NAME structures.
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
 *   locale?: LocaleCode
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
 *   locale?: LocaleCode
 *   variant*: TextValueVariant    // alternative phonetic, transliterated, or transcribed representations
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;   // Unlike ContextImpactRecord, CULTURAL_NORM here describes the naming system that governs the structure of the name itself.
 *   source*: SourceCitation
 *   note*: NoteStructure
 * }
 *
 * struct NamePartStructure {
 *   type: enum {
 *     // personal and birth names
 *     given, generation,
 *     // direct family relationships (descent)
 *     patronymic, matronymic, kunya (كُنيَة),
 *     // extended family and social belonging
 *     family, family_nickname, lineage, house, clan, tribal, caste,
 *     // geographical and territorial origin
 *     toponymic,
 *     // titles, roles and professions
 *     title, occupational, prefix, suffix,
 *     // assumed names, nicknames and contextual
 *     nickname, regnal, religious, posthumous
 *   } | Text
 *   value: Text                  // the textual value of this individual name component
 *   variant*: TextValueVariant   // alternative phonetic, transliterated, or transcribed representations of this name component
 * }
 * </pre>
 */
public final class NameReader{

	public static final String TYPE = "NAME_STRUCTURE";

	public static final String TAG_TYPE = "type";
	public static final String TAG_VALUE = "value";
	public static final String TAG_PART = "part";
	public static final String TAG_LOCALE = "locale";
	public static final String TAG_CULTURAL_NORM = "cultural_norm";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";

	public static final String ENUM_PERSONAL_TYPE_OFFICIAL = "official";
	public static final String ENUM_PERSONAL_TYPE_RELIGIOUS = "religious";
	public static final String ENUM_PERSONAL_TYPE_BIRTH = "birth";
	public static final String ENUM_PERSONAL_TYPE_MARRIED = "married";
	public static final String ENUM_PERSONAL_TYPE_MAIDEN = "maiden";
	public static final String ENUM_PERSONAL_TYPE_DIVORCE = "divorce";
	public static final String ENUM_PERSONAL_TYPE_ADOPTION = "adoption";
	public static final String ENUM_PERSONAL_TYPE_FOSTERING = "fostering";
	public static final String ENUM_PERSONAL_TYPE_LEGAL = "legal";
	public static final String ENUM_PERSONAL_TYPE_IMMIGRANT = "immigrant";
	public static final String ENUM_PERSONAL_TYPE_ADAPTED = "adapted";
	public static final String ENUM_PERSONAL_TYPE_ALIAS = "alias";
	public static final String ENUM_PERSONAL_TYPE_NICKNAME = "nickname";
	public static final String ENUM_PERSONAL_TYPE_ARTISTIC = "artistic";
	public static final String ENUM_PERSONAL_TYPE_PROFESSIONAL = "professional";
	public static final String ENUM_PERSONAL_TYPE_REGNAL = "regnal";
	public static final String ENUM_PERSONAL_TYPE_SLAVE_NAME = "slave_name";
	public static final String[] PERSONAL_TYPES = {
		// marital status and origins at birth
		ENUM_PERSONAL_TYPE_OFFICIAL, ENUM_PERSONAL_TYPE_RELIGIOUS, ENUM_PERSONAL_TYPE_BIRTH,
		// changes in marital status and family events
		ENUM_PERSONAL_TYPE_MARRIED, ENUM_PERSONAL_TYPE_MAIDEN, ENUM_PERSONAL_TYPE_DIVORCE, ENUM_PERSONAL_TYPE_ADOPTION, ENUM_PERSONAL_TYPE_FOSTERING,
		// legal, immigration, and naturalization changes
		ENUM_PERSONAL_TYPE_LEGAL, ENUM_PERSONAL_TYPE_IMMIGRANT, ENUM_PERSONAL_TYPE_ADAPTED,
		// informal, stage, and social names
		ENUM_PERSONAL_TYPE_ALIAS, ENUM_PERSONAL_TYPE_NICKNAME, ENUM_PERSONAL_TYPE_ARTISTIC, ENUM_PERSONAL_TYPE_PROFESSIONAL,
		// historical and dynastic contexts
		ENUM_PERSONAL_TYPE_REGNAL, ENUM_PERSONAL_TYPE_SLAVE_NAME
	};

	public static final String ENUM_TYPE_OFFICIAL = "official";
	public static final String ENUM_TYPE_LEGAL = "legal";
	public static final String ENUM_TYPE_COLONIAL = "colonial";
	public static final String ENUM_TYPE_INDIGENOUS = "indigenous";
	public static final String ENUM_TYPE_TRADITIONAL = "traditional";
	public static final String ENUM_TYPE_TRANSLATED = "translated";
	public static final String ENUM_TYPE_TRANSCRIBED = "transcribed";
	public static final String ENUM_TYPE_HISTORIC = "historic";
	public static final String ENUM_TYPE_COMMON = "common";
	public static final String ENUM_TYPE_COLLOQUIAL = "colloquial";
	public static final String ENUM_TYPE_ABBREVIATED = "abbreviated";
	public static final String ENUM_TYPE_ACRONYM = "acronym";
	public static final String ENUM_TYPE_RELIGIOUS = "religious";
	public static final String ENUM_TYPE_ADMINISTRATIVE = "administrative";
	public static final String ENUM_TYPE_ARCHIVAL = "archival";
	public static final String[] TYPES = {
		// official and legal names
		ENUM_TYPE_OFFICIAL, ENUM_TYPE_LEGAL,
		// historical naming traditions
		ENUM_TYPE_COLONIAL, ENUM_TYPE_INDIGENOUS, ENUM_TYPE_TRADITIONAL,
		// language and localization variants
		ENUM_TYPE_TRANSLATED, ENUM_TYPE_TRANSCRIBED,
		// historical variants
		ENUM_TYPE_HISTORIC,
		// common usage
		ENUM_TYPE_COMMON, ENUM_TYPE_COLLOQUIAL,
		// abbreviated forms
		ENUM_TYPE_ABBREVIATED, ENUM_TYPE_ACRONYM,
		// religious and ecclesiastical forms
		ENUM_TYPE_RELIGIOUS,
		// administrative and archival forms
		ENUM_TYPE_ADMINISTRATIVE, ENUM_TYPE_ARCHIVAL
	};


	public static final String TAG_PART_TYPE = "type";
	public static final String TAG_PART_VALUE = "value";
	public static final String TAG_PART_VARIANT = "variant";
	public static final String TAG_VALID_FROM = "valid_from";
	public static final String TAG_VALID_TO = "valid_to";

	public static final String ENUM_PART_TYPE_GIVEN = "given";
	public static final String ENUM_PART_TYPE_GENERATION = "generation";
	public static final String ENUM_PART_TYPE_PATRONYMIC = "patronymic";
	public static final String ENUM_PART_TYPE_MATRONYMIC = "matronymic";
	public static final String ENUM_PART_TYPE_KUNYA = "kunya";
	public static final String ENUM_PART_TYPE_FAMILY = "family";
	public static final String ENUM_PART_TYPE_FAMILY_NICKNAME = "family_nickname";
	public static final String ENUM_PART_TYPE_LINEAGE = "lineage";
	public static final String ENUM_PART_TYPE_HOUSE = "house";
	public static final String ENUM_PART_TYPE_CLAN = "clan";
	public static final String ENUM_PART_TYPE_TRIBAL = "tribal";
	public static final String ENUM_PART_TYPE_CASTE = "caste";
	public static final String ENUM_PART_TYPE_TOPONYMIC = "toponymic";
	public static final String ENUM_PART_TYPE_TITLE = "title";
	public static final String ENUM_PART_TYPE_OCCUPATIONAL = "occupational";
	public static final String ENUM_PART_TYPE_PREFIX = "prefix";
	public static final String ENUM_PART_TYPE_SUFFIX = "suffix";
	public static final String ENUM_PART_TYPE_NICKNAME = "nickname";
	public static final String ENUM_PART_TYPE_REGNAL = "regnal";
	public static final String ENUM_PART_TYPE_RELIGIOUS = "religious";
	public static final String ENUM_PART_TYPE_POSTHUMOUS = "posthumous";
	public static final String[] PART_TYPES = {
		ENUM_PART_TYPE_GIVEN, ENUM_PART_TYPE_GENERATION,
		ENUM_PART_TYPE_PATRONYMIC, ENUM_PART_TYPE_MATRONYMIC, ENUM_PART_TYPE_KUNYA,
		ENUM_PART_TYPE_FAMILY, ENUM_PART_TYPE_FAMILY_NICKNAME, ENUM_PART_TYPE_LINEAGE, ENUM_PART_TYPE_HOUSE, ENUM_PART_TYPE_CLAN, ENUM_PART_TYPE_TRIBAL, ENUM_PART_TYPE_CASTE,
		ENUM_PART_TYPE_TOPONYMIC,
		ENUM_PART_TYPE_TITLE, ENUM_PART_TYPE_OCCUPATIONAL, ENUM_PART_TYPE_PREFIX, ENUM_PART_TYPE_SUFFIX,
		ENUM_PART_TYPE_NICKNAME, ENUM_PART_TYPE_REGNAL, ENUM_PART_TYPE_RELIGIOUS, ENUM_PART_TYPE_POSTHUMOUS
	};

	public static final Set<String> EXCLUDED_PART_TYPES = Set.of(
		ENUM_PART_TYPE_FAMILY_NICKNAME,
		ENUM_PART_TYPE_TITLE, ENUM_PART_TYPE_OCCUPATIONAL, ENUM_PART_TYPE_PREFIX, ENUM_PART_TYPE_SUFFIX,
		ENUM_PART_TYPE_NICKNAME, ENUM_PART_TYPE_REGNAL, ENUM_PART_TYPE_RELIGIOUS, ENUM_PART_TYPE_POSTHUMOUS
	);


	private NameReader(){}


	public static String extractValue(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_VALUE);
	}

	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}

	public static String extractLocale(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_LOCALE);
	}


	public static String extractPartValue(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PART_VALUE);
	}

	public static String extractPartType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PART_TYPE);
	}

	public static boolean isPartTypeExcludedFromPersonalName(final String partType){
		return EXCLUDED_PART_TYPES.contains(partType);
	}

}
