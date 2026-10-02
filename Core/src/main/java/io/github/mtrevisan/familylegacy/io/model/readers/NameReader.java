package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;

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

	public static final String TAG_TYPE = "type";
	public static final String TAG_VALUE = "value";
	public static final String TAG_PART = "part";
	public static final String TAG_LOCALE = "locale";
	public static final String TAG_CULTURAL_NORM = "cultural_norm";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";

	public static final String[] PERSONAL_TYPES = {
		// marital status and origins at birth
		"official", "religious", "birth",
		// changes in marital status and family events
		"married", "maiden", "divorce", "adoption", "fostering",
		// legal, immigration, and naturalization changes
		"legal", "immigrant", "adapted",
		// informal, stage, and social names
		"alias", "nickname", "artistic", "professional", "user",
		// historical and dynastic contexts
		"regnal", "slave_name"
	};

	public static final String[] TYPES = {
		// official and legal names
		"official", "legal",
		// historical naming traditions
		"colonial", "indigenous", "traditional",
		// language and localization variants
		"translated", "transcribed",
		// historical variants
		"historic", "former",
		// common usage
		"common", "colloquial",
		// abbreviated forms
		"abbreviated", "acronym",
		// religious and ecclesiastical forms
		"religious",
		// administrative and archival forms
		"administrative", "archival"
	};


	public static final String TAG_PART_TYPE = "type";
	public static final String TAG_PART_VALUE = "value";
	public static final String TAG_PART_VARIANT = "variant";

	public static final String[] PART_TYPES = {
		"given", "generation",
		"patronymic", "matronymic", "kunya",
		"family", "family_nickname", "lineage", "house", "clan", "tribal", "caste",
		"toponymic",
		"title", "occupational", "prefix", "suffix",
		"nickname", "regnal", "religious", "posthumous"
	};

	public static final Set<String> EXCLUDED_PART_TYPES = Set.of(
		"family_nickname",
		"title", "occupational", "prefix", "suffix",
		"nickname", "regnal", "religious", "posthumous"
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
