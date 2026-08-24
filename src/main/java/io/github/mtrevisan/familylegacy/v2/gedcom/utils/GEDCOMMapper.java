package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Mappa i tag e valori GEDCOM 5.5.1 ai valori enum di FLEF 0.1.2.
 * Tutti i confronti sono case‑insensibili.
 */
public final class GEDCOMMapper{

	// ---- Eventi individuali e familiari ----
	private static final Map<String, String> EVENT_MAP = new HashMap<>();
	// ---- Attributi individuali ----
	private static final Map<String, String> ATTRIBUTE_MAP = new HashMap<>();
	// ---- Sesso ----
	private static final Map<String, String> SEX_MAP = new HashMap<>();
	// ---- Tipo di media (SourceRecord.media_type) ----
	private static final Map<String, String> MEDIA_TYPE_MAP = new HashMap<>();
	// ---- Tipo di relazione (RelationshipRecord.type) ----
	private static final Map<String, String> RELATIONSHIP_TYPE_MAP = new HashMap<>();
	// ---- Ruolo in event_participation ----
	private static final Map<String, String> ROLE_MAP = new HashMap<>();
	// ---- Stato di una relazione ----
	private static final Map<String, String> STATUS_MAP = new HashMap<>();
	// ---- Calendario ----
	private static final Map<String, String> CALENDAR_MAP = new HashMap<>();
	// ---- Parte di secolo (CenturyPart) ----
	private static final Map<String, String> CENTURY_PART_MAP = new HashMap<>();
	// ---- Base di approssimazione (Approximate.basis) ----
	private static final Map<String, String> BASIS_MAP = new HashMap<>();
	// ---- Tipo di fonte (EvidenceQualifiers.source_type) ----
	private static final Map<String, String> SOURCE_TYPE_MAP = new HashMap<>();
	// ---- Tipo di informazione (EvidenceQualifiers.information_type) ----
	private static final Map<String, String> INFO_TYPE_MAP = new HashMap<>();
	// ---- Tipo di evidenza (EvidenceQualifiers.evidence_type) ----
	private static final Map<String, String> EVIDENCE_TYPE_MAP = new HashMap<>();
	// ---- Livello privacy ----
	private static final Map<String, String> PRIVACY_LEVEL_MAP = new HashMap<>();
	// ---- Activity type (ResearchActivityRecord.activity_type) ----
	private static final Map<String, String> ACTIVITY_TYPE_MAP = new HashMap<>();
	// ---- Result (ResearchActivityRecord.result) ----
	private static final Map<String, String> RESULT_MAP = new HashMap<>();

	static{
		// ------------------------------
		// Eventi (EVENT_MAP)
		// ------------------------------
		EVENT_MAP.put("BIRT", "birth");
		EVENT_MAP.put("DEAT", "death");
		EVENT_MAP.put("BURI", "burial");
		EVENT_MAP.put("CREM", "cremation");
		EVENT_MAP.put("ADOP", "adoption");
		EVENT_MAP.put("BAPM", "baptism");
		EVENT_MAP.put("BARM", "bar_mitzvah");
		EVENT_MAP.put("BASM", "bat_mitzvah");
		EVENT_MAP.put("BLES", "blessing");
		EVENT_MAP.put("CHRA", "chrismation");
		EVENT_MAP.put("CONF", "confirmation");
		EVENT_MAP.put("FCOM", "first_communion");
		EVENT_MAP.put("ORDN", "ordination");
		EVENT_MAP.put("NATU", "naturalization");
		EVENT_MAP.put("EMIG", "emigration");
		EVENT_MAP.put("IMMI", "immigration");
		EVENT_MAP.put("CENS", "census");
		EVENT_MAP.put("PROB", "probate");
		EVENT_MAP.put("WILL", "will");
		EVENT_MAP.put("GRAD", "graduation");
		EVENT_MAP.put("RETI", "retirement");
		EVENT_MAP.put("MARR", "marriage");
		EVENT_MAP.put("DIV", "divorce");
		EVENT_MAP.put("ANUL", "annulment");
		EVENT_MAP.put("ENGA", "engagement");
		EVENT_MAP.put("MARB", "marriage_bann");
		EVENT_MAP.put("MARC", "marriage_contract");
		EVENT_MAP.put("MARL", "marriage_license");
		EVENT_MAP.put("MARS", "marriage_settlement");
		EVENT_MAP.put("RESI", "residence");
		EVENT_MAP.put("EVEN", "other");

		// ------------------------------
		// Attributi (ATTRIBUTE_MAP)
		// ------------------------------
		ATTRIBUTE_MAP.put("CAST", "caste");
		ATTRIBUTE_MAP.put("DSCR", "characteristic");
		ATTRIBUTE_MAP.put("EDUC", "education");
		ATTRIBUTE_MAP.put("IDNO", "ssn");
		ATTRIBUTE_MAP.put("NATI", "nationality");
		ATTRIBUTE_MAP.put("NCHI", "children_count");
		ATTRIBUTE_MAP.put("NMR", "marriages_count");
		ATTRIBUTE_MAP.put("OCCU", "occupation");
		ATTRIBUTE_MAP.put("PROP", "possession");
		ATTRIBUTE_MAP.put("RELI", "religion");
		ATTRIBUTE_MAP.put("RESI", "residence");
		ATTRIBUTE_MAP.put("SSN", "ssn");
		ATTRIBUTE_MAP.put("TITL", "title");
		ATTRIBUTE_MAP.put("FACT", "other");

		// ------------------------------
		// Sesso
		// ------------------------------
		SEX_MAP.put("M", "male");
		SEX_MAP.put("F", "female");
		SEX_MAP.put("U", "unknown");
		SEX_MAP.put("X", "unknown");

		// ------------------------------
		// Media type
		// ------------------------------
		MEDIA_TYPE_MAP.put("AUDIO", "audio");
		MEDIA_TYPE_MAP.put("BOOK", "book");
		MEDIA_TYPE_MAP.put("CARD", "card");
		MEDIA_TYPE_MAP.put("ELECTRONIC", "electronic");
		MEDIA_TYPE_MAP.put("FICHE", "fiche");
		MEDIA_TYPE_MAP.put("FILM", "film");
		MEDIA_TYPE_MAP.put("MAGAZINE", "magazine");
		MEDIA_TYPE_MAP.put("MANUSCRIPT", "manuscript");
		MEDIA_TYPE_MAP.put("MAP", "map");
		MEDIA_TYPE_MAP.put("NEWSPAPER", "newspaper");
		MEDIA_TYPE_MAP.put("PHOTO", "photo");
		MEDIA_TYPE_MAP.put("TOMBSTONE", "tombstone");
		MEDIA_TYPE_MAP.put("VIDEO", "video");

		// ------------------------------
		// Relationship types
		// ------------------------------
		RELATIONSHIP_TYPE_MAP.put("biological_child", "biological_child");
		RELATIONSHIP_TYPE_MAP.put("adoptive_child", "adoptive_child");
		RELATIONSHIP_TYPE_MAP.put("foster_child", "foster_child");
		RELATIONSHIP_TYPE_MAP.put("guarded_child", "guarded_child");
		RELATIONSHIP_TYPE_MAP.put("step_child", "step_child");
		RELATIONSHIP_TYPE_MAP.put("civil_spouse", "civil_spouse");
		RELATIONSHIP_TYPE_MAP.put("religious_spouse", "religious_spouse");
		RELATIONSHIP_TYPE_MAP.put("customary_spouse", "customary_spouse");
		RELATIONSHIP_TYPE_MAP.put("cohabiting_partner", "cohabiting_partner");
		RELATIONSHIP_TYPE_MAP.put("engaged_partner", "engaged_partner");
		RELATIONSHIP_TYPE_MAP.put("group_member", "group_member");
		RELATIONSHIP_TYPE_MAP.put("associate", "associate");

		// ------------------------------
		// Roles
		// ------------------------------
		ROLE_MAP.put("CHILD", "child");
		ROLE_MAP.put("PARENT", "parent");
		ROLE_MAP.put("SPOUSE", "spouse");
		ROLE_MAP.put("POWER_OF_ATTORNEY", "power_of_attorney");
		ROLE_MAP.put("PRISONER", "prisoner");
		ROLE_MAP.put("WITNESS", "witness");
		ROLE_MAP.put("OFFICIANT", "officiant");
		ROLE_MAP.put("INFORMANT", "informant");
		ROLE_MAP.put("EXECUTOR", "executor");
		ROLE_MAP.put("GRANTOR", "grantor");
		ROLE_MAP.put("GRANTEE", "grantee");
		ROLE_MAP.put("LANDLORD", "landlord");
		ROLE_MAP.put("TENANT", "tenant");
		ROLE_MAP.put("SOLDIER", "soldier");
		ROLE_MAP.put("COMMANDER", "commander");
		ROLE_MAP.put("VICTIM", "victim");
		ROLE_MAP.put("SURVIVOR", "survivor");
		ROLE_MAP.put("ACCUSED", "accused");
		ROLE_MAP.put("JUDGE", "judge");

		// ------------------------------
		// Relationship status
		// ------------------------------
		STATUS_MAP.put("ACTIVE", "active");
		STATUS_MAP.put("ENDED", "ended");
		STATUS_MAP.put("UNKNOWN", "unknown");

		// ------------------------------
		// Calendar
		// ------------------------------
		CALENDAR_MAP.put("GREGORIAN", "gregorian");
		CALENDAR_MAP.put("JULIAN", "julian");
		CALENDAR_MAP.put("ISLAMIC", "islamic");
		CALENDAR_MAP.put("HEBREW", "hebrew");
		CALENDAR_MAP.put("CHINESE", "chinese");
		CALENDAR_MAP.put("INDIAN", "indian");
		CALENDAR_MAP.put("BUDDHIST", "buddhist");
		CALENDAR_MAP.put("FRENCH_REPUBLICAN", "french_republican");
		CALENDAR_MAP.put("COPTIC", "coptic");
		CALENDAR_MAP.put("SOVIET_ETERNAL", "soviet_eternal");
		CALENDAR_MAP.put("ETHIOPIAN", "ethiopian");
		CALENDAR_MAP.put("MAYAN", "mayan");

		// ------------------------------
		// Century part
		// ------------------------------
		CENTURY_PART_MAP.put("FIRST_QUARTER", "first_quarter");
		CENTURY_PART_MAP.put("SECOND_QUARTER", "second_quarter");
		CENTURY_PART_MAP.put("THIRD_QUARTER", "third_quarter");
		CENTURY_PART_MAP.put("FOURTH_QUARTER", "fourth_quarter");
		CENTURY_PART_MAP.put("FIRST_HALF", "first_half");
		CENTURY_PART_MAP.put("SECOND_HALF", "second_half");
		CENTURY_PART_MAP.put("EARLY", "early");
		CENTURY_PART_MAP.put("MID", "mid");
		CENTURY_PART_MAP.put("LATE", "late");

		// ------------------------------
		// Basis for approximate dates
		// ------------------------------
		BASIS_MAP.put("STATED", "stated");
		BASIS_MAP.put("CALCULATED", "calculated");
		BASIS_MAP.put("CONVENTIONAL", "conventional");
		BASIS_MAP.put("UNSPECIFIED", "unspecified");

		// ------------------------------
		// Evidence qualifiers
		// ------------------------------
		SOURCE_TYPE_MAP.put("ORIGINAL", "original");
		SOURCE_TYPE_MAP.put("DERIVED", "derived");

		INFO_TYPE_MAP.put("PRIMARY", "primary");
		INFO_TYPE_MAP.put("SECONDARY", "secondary");
		INFO_TYPE_MAP.put("UNDETERMINED", "undetermined");

		EVIDENCE_TYPE_MAP.put("DIRECT", "direct");
		EVIDENCE_TYPE_MAP.put("INDIRECT", "indirect");
		EVIDENCE_TYPE_MAP.put("NEGATIVE", "negative");

		// ------------------------------
		// Privacy level
		// ------------------------------
		PRIVACY_LEVEL_MAP.put("PUBLIC", "public");
		PRIVACY_LEVEL_MAP.put("RESTRICTED", "restricted");
		PRIVACY_LEVEL_MAP.put("CONFIDENTIAL", "confidential");

		// ------------------------------
		// Research activity types (non ancora usati ma utili per estensione)
		// ------------------------------
		ACTIVITY_TYPE_MAP.put("SEARCH", "search");
		ACTIVITY_TYPE_MAP.put("REVIEW", "review");
		ACTIVITY_TYPE_MAP.put("ANALYSIS", "analysis");
		ACTIVITY_TYPE_MAP.put("CORRESPONDENCE", "correspondence");
		ACTIVITY_TYPE_MAP.put("INTERVIEW", "interview");
		ACTIVITY_TYPE_MAP.put("HYPOTHESIS", "hypothesis");

		RESULT_MAP.put("POSITIVE", "positive");
		RESULT_MAP.put("NEGATIVE", "negative");
		RESULT_MAP.put("INCONCLUSIVE", "inconclusive");
		RESULT_MAP.put("CONFLICTING", "conflicting");
		RESULT_MAP.put("UNAVAILABLE", "unavailable");
	}

	// ---- Metodi pubblici ----
	public static String mapEvent(String gedcomTag, String customType){
		if(customType != null && !customType.isEmpty()){
			return customType.toLowerCase(Locale.ROOT);
		}
		return EVENT_MAP.getOrDefault(gedcomTag.toUpperCase(Locale.ROOT), "other");
	}

	public static String mapAttribute(String gedcomTag, String customType){
		if(customType != null && !customType.isEmpty()){
			return customType.toLowerCase(Locale.ROOT);
		}
		return ATTRIBUTE_MAP.getOrDefault(gedcomTag.toUpperCase(Locale.ROOT), "other");
	}

	public static String mapSex(String gedcomSex){
		if(gedcomSex == null) return "unknown";
		return SEX_MAP.getOrDefault(gedcomSex.toUpperCase(Locale.ROOT), "unknown");
	}

	public static String mapMediaType(String gedcomMedia){
		if(gedcomMedia == null) return null;
		return MEDIA_TYPE_MAP.getOrDefault(gedcomMedia.toUpperCase(Locale.ROOT), gedcomMedia.toLowerCase(Locale.ROOT));
	}

	public static String mapRelationshipType(String gedcomType){
		if(gedcomType == null) return "associate";
		return RELATIONSHIP_TYPE_MAP.getOrDefault(gedcomType.toLowerCase(Locale.ROOT), gedcomType.toLowerCase(Locale.ROOT));
	}

	public static String mapRole(String gedcomRole){
		if(gedcomRole == null) return null;
		return ROLE_MAP.getOrDefault(gedcomRole.toUpperCase(Locale.ROOT), gedcomRole.toLowerCase(Locale.ROOT));
	}

	public static String mapStatus(String gedcomStatus){
		if(gedcomStatus == null) return null;
		return STATUS_MAP.getOrDefault(gedcomStatus.toUpperCase(Locale.ROOT), null);
	}

	public static String mapCalendar(String gedcomCalendar){
		if(gedcomCalendar == null) return "gregorian";
		return CALENDAR_MAP.getOrDefault(gedcomCalendar.toUpperCase(Locale.ROOT), gedcomCalendar.toLowerCase(Locale.ROOT));
	}

	public static String mapCenturyPart(String gedcomPart){
		if(gedcomPart == null) return null;
		return CENTURY_PART_MAP.getOrDefault(gedcomPart.toUpperCase(Locale.ROOT), null);
	}

	public static String mapBasis(String gedcomBasis){
		if(gedcomBasis == null) return "unspecified";
		return BASIS_MAP.getOrDefault(gedcomBasis.toUpperCase(Locale.ROOT), "unspecified");
	}

	public static String mapSourceType(String gedcomSourceType){
		if(gedcomSourceType == null) return null;
		return SOURCE_TYPE_MAP.getOrDefault(gedcomSourceType.toUpperCase(Locale.ROOT), null);
	}

	public static String mapInfoType(String gedcomInfoType){
		if(gedcomInfoType == null) return null;
		return INFO_TYPE_MAP.getOrDefault(gedcomInfoType.toUpperCase(Locale.ROOT), null);
	}

	public static String mapEvidenceType(String gedcomEvidenceType){
		if(gedcomEvidenceType == null) return null;
		return EVIDENCE_TYPE_MAP.getOrDefault(gedcomEvidenceType.toUpperCase(Locale.ROOT), null);
	}

	public static String mapPrivacyLevel(String gedcomLevel){
		if(gedcomLevel == null) return "public";
		return PRIVACY_LEVEL_MAP.getOrDefault(gedcomLevel.toUpperCase(Locale.ROOT), "public");
	}

	private GEDCOMMapper(){
	}

}
