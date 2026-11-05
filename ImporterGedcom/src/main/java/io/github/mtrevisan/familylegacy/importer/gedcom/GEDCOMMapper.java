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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.TextValueVariantReader;


/**
 * Mappa i tag e valori GEDCOM 5.5.1 e 7 ai valori enum di FLEF 0.2.0.
 * Tutti i confronti sono case‑insensibili.
 */
public final class GEDCOMMapper{

	/** Type of the generic event (EVEN) and fact (FACT) tags, whose actual type is given by their TYPE. */
	public static final String TAG_GENERIC_EVENT = "EVEN";
	public static final String TAG_GENERIC_FACT = "FACT";

	// ---- Tag degli eventi (INDIVIDUAL_EVENT_STRUCTURE, FAMILY_EVENT_STRUCTURE, LDS ordinances) ----
	public static final Set<String> INDIVIDUAL_EVENT_TAGS = Set.of("BIRT", "CHR", "DEAT", "BURI", "CREM", "ADOP", "BAPM",
		"BARM", "BASM", "BLES", "CHRA", "CONF", "FCOM", "ORDN", "NATU", "EMIG", "IMMI", "CENS", "PROB", "WILL", "GRAD", "RETI",
		"EVEN", "BAPL", "CONL", "ENDL", "INIL", "SLGC");
	public static final Set<String> FAMILY_EVENT_TAGS = Set.of("ANUL", "CENS", "DIV", "DIVF", "ENGA", "MARB", "MARC", "MARR",
		"MARL", "MARS", "EVEN", "SLGS");

	// ---- Tipi di evento definiti da FLEF (EventRecord.type) ----
	private static final Set<String> FLEF_EVENT_TYPES = Set.of(EventReader.ENUM_TYPE_BIRTH, EventReader.ENUM_TYPE_ADOPTION, EventReader.ENUM_TYPE_DEATH, EventReader.ENUM_TYPE_CREMATION, EventReader.ENUM_TYPE_BURIAL,
		EventReader.ENUM_TYPE_CORONER_REPORT, EventReader.ENUM_TYPE_ILLNESS, EventReader.ENUM_TYPE_HOSPITALIZATION, EventReader.ENUM_TYPE_MEDICAL_PROCEDURE, EventReader.ENUM_TYPE_ENGAGEMENT, EventReader.ENUM_TYPE_MARRIAGE_BANN, EventReader.ENUM_TYPE_MARRIAGE_CONTRACT,
		EventReader.ENUM_TYPE_MARRIAGE_LICENSE, EventReader.ENUM_TYPE_MARRIAGE_SETTLEMENT, EventReader.ENUM_TYPE_MARRIAGE, EventReader.ENUM_TYPE_SEPARATION, EventReader.ENUM_TYPE_DIVORCE_FILED, EventReader.ENUM_TYPE_DIVORCE_DECREE, EventReader.ENUM_TYPE_DIVORCE, EventReader.ENUM_TYPE_ANNULMENT, EventReader.ENUM_TYPE_EDUCATION,
		EventReader.ENUM_TYPE_GRADUATION, EventReader.ENUM_TYPE_RETIREMENT, EventReader.ENUM_TYPE_MILITARY_INDUCTION, EventReader.ENUM_TYPE_MILITARY_MUSTER_ROLL, EventReader.ENUM_TYPE_MILITARY_SERVICE, EventReader.ENUM_TYPE_MILITARY_AWARD,
		EventReader.ENUM_TYPE_MILITARY_RELEASE, EventReader.ENUM_TYPE_MILITARY_DISCHARGE, EventReader.ENUM_TYPE_MILITARY_RESIGNATION, EventReader.ENUM_TYPE_MILITARY_RETIREMENT, EventReader.ENUM_TYPE_PRISON, EventReader.ENUM_TYPE_PARDON, EventReader.ENUM_TYPE_JURY_DUTY,
		EventReader.ENUM_TYPE_HONOR, EventReader.ENUM_TYPE_BANKRUPTCY, EventReader.ENUM_TYPE_IMMIGRATION, EventReader.ENUM_TYPE_NATURALIZATION, EventReader.ENUM_TYPE_EMIGRATION, EventReader.ENUM_TYPE_DEPORTATION, EventReader.ENUM_TYPE_INTERNMENT, EventReader.ENUM_TYPE_LIBERATION,
		EventReader.ENUM_TYPE_EMANCIPATION, EventReader.ENUM_TYPE_RELOCATION, EventReader.ENUM_TYPE_CENSUS, EventReader.ENUM_TYPE_DEED, EventReader.ENUM_TYPE_ESCROW, EventReader.ENUM_TYPE_CHANCERY, EventReader.ENUM_TYPE_WILL, EventReader.ENUM_TYPE_PROBATE, EventReader.ENUM_TYPE_GUARDIANSHIP);
	// ---- Sinonimi del TYPE di EVEN verso i tipi di evento FLEF ----
	private static final Map<String, String> EVENT_TYPE_SYNONYMS = Map.ofEntries(
		Map.entry("move", "relocation"),
		Map.entry("moved", "relocation"),
		Map.entry("military_enlistment", "military_induction"),
		Map.entry("enlistment", "military_induction"),
		Map.entry("military", "military_service"),
		Map.entry("hospitalisation", EventReader.ENUM_TYPE_HOSPITALIZATION),
		Map.entry("medical", EventReader.ENUM_TYPE_MEDICAL_PROCEDURE),
		Map.entry("surgery", EventReader.ENUM_TYPE_MEDICAL_PROCEDURE),
		Map.entry("imprisonment", EventReader.ENUM_TYPE_PRISON),
		Map.entry("banns", EventReader.ENUM_TYPE_MARRIAGE_BANN),
		Map.entry("marriage_banns", EventReader.ENUM_TYPE_MARRIAGE_BANN),
		Map.entry("engaged", EventReader.ENUM_TYPE_ENGAGEMENT),
		Map.entry("divorce_filing", EventReader.ENUM_TYPE_DIVORCE_FILED),
		Map.entry("separated", EventReader.ENUM_TYPE_SEPARATION),
		Map.entry("award", EventReader.ENUM_TYPE_HONOR),
		Map.entry("decoration", EventReader.ENUM_TYPE_HONOR),
		Map.entry("incarceration", "prison"),
		Map.entry("degree", "graduation"),
		Map.entry("probate", "probate"));
	// ---- TYPE di EVEN/FACT che FLEF rappresenta altrove ----
	/** EVEN/FACT types that are names of the individual: they become a {@code PersonalNameStructure} with this name part type. */
	public static final Map<String, String> NAME_PART_FACT_TYPES = Map.of(
		"family_nickname", "family_nickname",
		"nickname", "nickname");
	/**
	 * EVEN/FACT types that are attributes of the individual (the FLEF types of IndividualAttributeRecord, and their synonyms):
	 * they become an {@code IndividualAttributeRecord} of this type.
	 */
	public static final Map<String, String> ATTRIBUTE_FACT_TYPES = Map.ofEntries(
		Map.entry("family_address", IndividualAttributeReader.ENUM_TYPE_RESIDENCE),
		Map.entry("address", IndividualAttributeReader.ENUM_TYPE_RESIDENCE),
		Map.entry("residence", IndividualAttributeReader.ENUM_TYPE_RESIDENCE),
		Map.entry("characteristic", IndividualAttributeReader.ENUM_TYPE_CHARACTERISTIC),
		Map.entry("physical_description", IndividualAttributeReader.ENUM_TYPE_CHARACTERISTIC),
		Map.entry("description", IndividualAttributeReader.ENUM_TYPE_CHARACTERISTIC),
		Map.entry("occupation", IndividualAttributeReader.ENUM_TYPE_OCCUPATION),
		Map.entry("profession", IndividualAttributeReader.ENUM_TYPE_OCCUPATION),
		Map.entry("possession", IndividualAttributeReader.ENUM_TYPE_POSSESSION),
		Map.entry("property", IndividualAttributeReader.ENUM_TYPE_POSSESSION),
		Map.entry("military_rank", IndividualAttributeReader.ENUM_TYPE_MILITARY_RANK),
		Map.entry("caste", IndividualAttributeReader.ENUM_TYPE_CASTE),
		Map.entry("social_class", IndividualAttributeReader.ENUM_TYPE_SOCIAL_CLASS),
		Map.entry("ethnicity", IndividualAttributeReader.ENUM_TYPE_ETHNICITY),
		Map.entry("citizenship", IndividualAttributeReader.ENUM_TYPE_CITIZENSHIP),
		Map.entry("nationality", IndividualAttributeReader.ENUM_TYPE_NATIONALITY),
		Map.entry("national_id", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID),
		Map.entry("title", IndividualAttributeReader.ENUM_TYPE_TITLE),
		Map.entry("nobility_title", IndividualAttributeReader.ENUM_TYPE_TITLE),
		Map.entry("children_count", IndividualAttributeReader.ENUM_TYPE_CHILDREN_COUNT),
		Map.entry("marriages_count", IndividualAttributeReader.ENUM_TYPE_MARRIAGES_COUNT),
		Map.entry("religion", IndividualAttributeReader.ENUM_TYPE_RELIGION),
		Map.entry("language", IndividualAttributeReader.ENUM_TYPE_LANGUAGE),
		Map.entry("literacy", IndividualAttributeReader.ENUM_TYPE_LITERACY),
		Map.entry("apprenticeship", IndividualAttributeReader.ENUM_TYPE_APPRENTICESHIP));
	/** EVEN types of a family that are attributes of the family: they become a {@code GroupAttributeRecord} of this type. */
	public static final Map<String, String> GROUP_ATTRIBUTE_FACT_TYPES = Map.of(
		"family_address", IndividualAttributeReader.ENUM_TYPE_RESIDENCE,
		"address", IndividualAttributeReader.ENUM_TYPE_RESIDENCE,
		"residence", IndividualAttributeReader.ENUM_TYPE_RESIDENCE,
		"religion", IndividualAttributeReader.ENUM_TYPE_RELIGION,
		"language", IndividualAttributeReader.ENUM_TYPE_LANGUAGE,
		"social_class", "social_class");

	private static final Pattern PATTERN_NON_TOKEN = Pattern.compile("[^a-z0-9]+");
	private static final Pattern PATTERN_DIACRITICS = Pattern.compile("\\p{M}+");
	/** Maximum number of words of a TYPE that is still a type and not a description. */
	private static final int MAX_TYPE_WORDS = 4;

	// ---- Eventi individuali e familiari ----
	public static final Map<String, String> EVENT_MAP = new HashMap<>();
	// ---- Attributi individuali ----
	public static final Map<String, String> ATTRIBUTE_MAP = new HashMap<>();
	// ---- Sesso ----
	private static final Map<String, String> SEX_MAP = new HashMap<>();
	// ---- Tipo di media (SourceRecord.media_type) ----
	private static final Map<String, String> MEDIA_TYPE_MAP = new HashMap<>();
	// ---- Ruolo in event_participation ----
	public static final Map<String, String> ROLE_MAP = new HashMap<>();
	// ---- Livello privacy ----
	private static final Map<String, String> PRIVACY_LEVEL_MAP = new HashMap<>();

	static{
		// ------------------------------
		// Eventi (EVENT_MAP)
		// ------------------------------
		// religious events have no FLEF type: they are written as custom types
		EVENT_MAP.put("BIRT", EventReader.ENUM_TYPE_BIRTH);
		EVENT_MAP.put("CHR", "christening");
		EVENT_MAP.put("DEAT", EventReader.ENUM_TYPE_DEATH);
		EVENT_MAP.put("BURI", EventReader.ENUM_TYPE_BURIAL);
		EVENT_MAP.put("CREM", EventReader.ENUM_TYPE_CREMATION);
		EVENT_MAP.put("ADOP", EventReader.ENUM_TYPE_ADOPTION);
		EVENT_MAP.put("BAPM", "baptism");
		EVENT_MAP.put("BARM", "bar_mitzvah");
		EVENT_MAP.put("BASM", "bat_mitzvah");
		EVENT_MAP.put("BLES", "blessing");
		EVENT_MAP.put("CHRA", "adult_christening");
		EVENT_MAP.put("CONF", "confirmation");
		EVENT_MAP.put("FCOM", "first_communion");
		EVENT_MAP.put("ORDN", "ordination");
		EVENT_MAP.put("NATU", EventReader.ENUM_TYPE_NATURALIZATION);
		EVENT_MAP.put("EMIG", EventReader.ENUM_TYPE_EMIGRATION);
		EVENT_MAP.put("IMMI", EventReader.ENUM_TYPE_IMMIGRATION);
		EVENT_MAP.put("CENS", EventReader.ENUM_TYPE_CENSUS);
		EVENT_MAP.put("PROB", EventReader.ENUM_TYPE_PROBATE);
		EVENT_MAP.put("WILL", EventReader.ENUM_TYPE_WILL);
		EVENT_MAP.put("GRAD", EventReader.ENUM_TYPE_GRADUATION);
		EVENT_MAP.put("RETI", EventReader.ENUM_TYPE_RETIREMENT);
		EVENT_MAP.put("MARR", EventReader.ENUM_TYPE_MARRIAGE);
		EVENT_MAP.put("DIV", EventReader.ENUM_TYPE_DIVORCE);
		EVENT_MAP.put("DIVF", EventReader.ENUM_TYPE_DIVORCE_FILED);
		EVENT_MAP.put("ANUL", EventReader.ENUM_TYPE_ANNULMENT);
		EVENT_MAP.put("ENGA", EventReader.ENUM_TYPE_ENGAGEMENT);
		EVENT_MAP.put("MARB", EventReader.ENUM_TYPE_MARRIAGE_BANN);
		EVENT_MAP.put("MARC", EventReader.ENUM_TYPE_MARRIAGE_CONTRACT);
		EVENT_MAP.put("MARL", EventReader.ENUM_TYPE_MARRIAGE_LICENSE);
		EVENT_MAP.put("MARS", EventReader.ENUM_TYPE_MARRIAGE_SETTLEMENT);
		// LDS ordinances
		EVENT_MAP.put("BAPL", "lds_baptism");
		EVENT_MAP.put("CONL", "lds_confirmation");
		EVENT_MAP.put("ENDL", "lds_endowment");
		EVENT_MAP.put("INIL", "lds_initiatory");
		EVENT_MAP.put("SLGC", "lds_child_sealing");
		EVENT_MAP.put("SLGS", "lds_spouse_sealing");
		EVENT_MAP.put(TAG_GENERIC_EVENT, "event");

		// ------------------------------
		// Attributi (ATTRIBUTE_MAP)
		// ------------------------------
		ATTRIBUTE_MAP.put("CAST", IndividualAttributeReader.ENUM_TYPE_CASTE);
		ATTRIBUTE_MAP.put("DSCR", IndividualAttributeReader.ENUM_TYPE_CHARACTERISTIC);
		ATTRIBUTE_MAP.put("EDUC", IndividualAttributeReader.ENUM_TYPE_EDUCATION);
		ATTRIBUTE_MAP.put("IDNO", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID);
		ATTRIBUTE_MAP.put("NATI", IndividualAttributeReader.ENUM_TYPE_NATIONALITY);
		ATTRIBUTE_MAP.put("NCHI", IndividualAttributeReader.ENUM_TYPE_CHILDREN_COUNT);
		ATTRIBUTE_MAP.put("NMR", IndividualAttributeReader.ENUM_TYPE_MARRIAGES_COUNT);
		ATTRIBUTE_MAP.put("OCCU", IndividualAttributeReader.ENUM_TYPE_OCCUPATION);
		ATTRIBUTE_MAP.put("PROP", IndividualAttributeReader.ENUM_TYPE_POSSESSION);
		ATTRIBUTE_MAP.put("RELI", IndividualAttributeReader.ENUM_TYPE_RELIGION);
		ATTRIBUTE_MAP.put("RESI", IndividualAttributeReader.ENUM_TYPE_RESIDENCE);
		ATTRIBUTE_MAP.put("SSN", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID);
		ATTRIBUTE_MAP.put("TITL", IndividualAttributeReader.ENUM_TYPE_TITLE);
		ATTRIBUTE_MAP.put(TAG_GENERIC_FACT, "fact");

		// ------------------------------
		// Sesso
		// ------------------------------
		SEX_MAP.put("M", IndividualReader.ENUM_SEX_MALE);
		SEX_MAP.put("F", IndividualReader.ENUM_SEX_FEMALE);
		SEX_MAP.put("U", IndividualReader.ENUM_SEX_UNKNOWN);
		SEX_MAP.put("X", IndividualReader.ENUM_SEX_UNKNOWN);

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
		// Roles
		// ------------------------------
		ROLE_MAP.put("CHILD", EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		ROLE_MAP.put("PARENT", EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		ROLE_MAP.put("SPOUSE", EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		ROLE_MAP.put("SPONSOR", "sponsor");
		ROLE_MAP.put("POWER_OF_ATTORNEY", "power_of_attorney");
		ROLE_MAP.put("PRISONER", "prisoner");
		ROLE_MAP.put("WITNESS", "witness");
		ROLE_MAP.put("OFFICIANT", "officiant");
		ROLE_MAP.put("DECLARANT", "informant");
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
		ROLE_MAP.put("PROPERTY", "property");

		// ------------------------------
		// Privacy level
		// ------------------------------
		PRIVACY_LEVEL_MAP.put("PUBLIC", PrivacyReader.ENUM_LEVEL_PUBLIC);
		PRIVACY_LEVEL_MAP.put("RESTRICTED", PrivacyReader.ENUM_LEVEL_RESTRICTED);
		PRIVACY_LEVEL_MAP.put("PRIVACY", PrivacyReader.ENUM_LEVEL_RESTRICTED);         // RESN privacy: dati personali riservati
		PRIVACY_LEVEL_MAP.put("PRIVATE", PrivacyReader.ENUM_LEVEL_RESTRICTED);         // RESN privacy: dati personali riservati
		// RESN locked impedisce le modifiche, non la diffusione: non ha corrispondente in FLEF
		PRIVACY_LEVEL_MAP.put("CONFIDENTIAL", PrivacyReader.ENUM_LEVEL_CONFIDENTIAL); // RESN confidential (estensione standard GEDCOM)
	}

	// ---- Metodi pubblici ----
	public static String mapEvent(String gedcomTag, String defaultValue){
		if(gedcomTag == null)
			return defaultValue;

		if(defaultValue != null && !defaultValue.isEmpty()){
			defaultValue = defaultValue.toLowerCase(Locale.ROOT);
		}
		else{
			defaultValue = "other";
		}
		return EVENT_MAP.getOrDefault(gedcomTag.toUpperCase(Locale.ROOT), defaultValue);
	}

	/**
	 * Maps a GEDCOM event to a FLEF event type.
	 * <p>
	 * The type of a generic event (EVEN) is given by its TYPE: a FLEF type (directly or through a synonym) if one matches, a
	 * custom type otherwise. A TYPE too long to be a type gives the generic type {@code event}.
	 *
	 * @param gedcomTag	The GEDCOM tag (e.g. {@code BIRT}, {@code EVEN}).
	 * @param gedcomType	The value of its TYPE, may be {@code null}.
	 * @return	The FLEF event type.
	 */
	public static String mapEventType(final String gedcomTag, final String gedcomType){
		if(!TAG_GENERIC_EVENT.equalsIgnoreCase(gedcomTag))
			return mapEvent(gedcomTag, gedcomTag);

		final String token = toTypeToken(gedcomType);
		if(token == null)
			return EVENT_MAP.get(TAG_GENERIC_EVENT);
		if(FLEF_EVENT_TYPES.contains(token))
			return token;
		return EVENT_TYPE_SYNONYMS.getOrDefault(token, token);
	}

	/**
	 * Converts a free-text GEDCOM type into a token valid as a FLEF custom type ({@code [a-z0-9_]+}).
	 *
	 * @param gedcomType	The type (e.g. {@code Military Service}).
	 * @return	The token (e.g. {@code military_service}), or {@code null} if the text is empty or is a description rather than
	 * 	a type.
	 */
	public static String toTypeToken(final String gedcomType){
		if(StringUtils.isBlank(gedcomType) || StringUtils.split(gedcomType).length > MAX_TYPE_WORDS)
			return null;

		final String ascii = PATTERN_DIACRITICS.matcher(Normalizer.normalize(gedcomType, Normalizer.Form.NFD))
			.replaceAll(StringUtils.EMPTY);
		final String token = StringUtils.strip(PATTERN_NON_TOKEN.matcher(ascii.toLowerCase(Locale.ROOT))
			.replaceAll("_"), "_");
		return (!token.isEmpty()? token: null);
	}

	/**
	 * Maps a GEDCOM NAME_TYPE to a FLEF {@code PersonalNameStructure} type.
	 *
	 * @param gedcomNameType	The name type (aka, birth, immigrant, maiden, married, or user defined).
	 * @return	The FLEF type, or {@code null} if it cannot be expressed as a type.
	 */
	public static String mapNameType(final String gedcomNameType){
		final String token = toTypeToken(gedcomNameType);
		return ("aka".equals(token)? "alias": token);
	}

	/**
	 * Maps a GEDCOM PEDIGREE_LINKAGE_TYPE (FAMC.PEDI) to a FLEF relationship type.
	 * <p>
	 * A missing pedigree is taken as a birth one, as genealogical programs do.
	 *
	 * @param gedcomPedigree	The pedigree (adopted, birth, foster, sealing), may be {@code null}.
	 * @return	The FLEF relationship type.
	 */
	public static String mapPedigree(final String gedcomPedigree){
		if(StringUtils.isBlank(gedcomPedigree))
			return RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD;

		return switch(gedcomPedigree.trim().toUpperCase(Locale.ROOT)){
			case "ADOPTED" -> RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD;
			case "FOSTER" -> RelationshipReader.ENUM_TYPE_FOSTER_CHILD;
			//a sealing does not tell the nature of the relationship
			case "SEALING", "SEALED" -> RelationshipReader.ENUM_TYPE_CHILD;
			default -> RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD;
		};
	}

	/**
	 * Maps a GEDCOM CHILD_LINKAGE_STATUS (FAMC.STAT) to a FLEF confidence.
	 *
	 * @param gedcomStatus	The status (challenged, disproven, proven), may be {@code null}.
	 * @return	The FLEF confidence, or {@code null} if not determinable.
	 */
	public static String mapChildLinkageStatus(final String gedcomStatus){
		if(StringUtils.isBlank(gedcomStatus))
			return null;

		return switch(gedcomStatus.trim().toUpperCase(Locale.ROOT)){
			case "CHALLENGED", "DISPROVEN" -> ConfidenceReader.ENUM_LOW;
			case "PROVEN" -> ConfidenceReader.ENUM_HIGH;
			default -> null;
		};
	}

	/**
	 * Maps the ROLE of an individual associated with an event (GEDCOM 7) to a FLEF participation role.
	 *
	 * @param gedcomRole	The role (e.g. {@code WITN}, {@code GODP}, or the phrase of {@code OTHER}), may be {@code null}.
	 * @return	The FLEF role, or {@code null} if the role is not a function in the event (e.g. a kinship, as {@code FATH}).
	 */
	public static String mapEventRole(final String gedcomRole){
		if(StringUtils.isBlank(gedcomRole))
			return null;

		final String role = gedcomRole.trim().toUpperCase(Locale.ROOT);
		return switch(role){
			case "WITN" -> "witness";
			case "OFFICIATOR", "CLERGY" -> "officiant";
			case "GODP" -> "sponsor";
			// the principals of an event are linked by the event itself, not as associates
			case "CHILD", "PARENT", "SPOUSE" -> null;
			default -> ROLE_MAP.get(role);
		};
	}

	/**
	 * Returns the readable form of the ROLE of an associate (GEDCOM 7).
	 *
	 * @param gedcomRole	The role (e.g. {@code GODP}, or the phrase of {@code OTHER}), may be {@code null}.
	 * @return	The readable role (e.g. {@code Godparent}), the role itself if not an enumerated one, or {@code null} if blank.
	 */
	public static String mapAssociationRoleLabel(final String gedcomRole){
		if(StringUtils.isBlank(gedcomRole))
			return null;

		return switch(gedcomRole.trim().toUpperCase(Locale.ROOT)){
			case "CHIL" -> "Child";
			case "CLERGY" -> "Clergy";
			case "FATH" -> "Father";
			case "FRIEND" -> "Friend";
			case "GODP" -> "Godparent";
			case "HUSB" -> "Husband";
			case "MOTH" -> "Mother";
			case "MULTIPLE" -> "Multiple";
			case "NGHBR" -> "Neighbor";
			case "OFFICIATOR" -> "Officiator";
			case "PARENT" -> "Parent";
			case "SPOU" -> "Spouse";
			case "WIFE" -> "Wife";
			case "WITN" -> "Witness";
			case "OTHER" -> "Other";
			default -> gedcomRole.trim();
		};
	}

	/**
	 * Maps the language of a translated name (GEDCOM 7 TRAN.LANG, a BCP 47 tag) to the type of a FLEF transcription.
	 *
	 * @param language	The language (e.g. {@code en}, {@code ja-Latn}), may be {@code null}.
	 * @return	The type (e.g. {@code anglicized}, {@code romanized}), or {@code null} if not determinable.
	 */
	public static String mapTranslationType(final String language){
		if(StringUtils.isBlank(language))
			return null;

		final String[] subtags = StringUtils.split(language.trim(), "-_");
		// the script, when given, tells the writing system the name was converted to
		if(subtags.length > 1 && subtags[1].length() == 4){
			final String script = switch(subtags[1].toLowerCase(Locale.ROOT)){
				case "latn" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_ROMANIZED;
				case "cyrl" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_CYRILLIZED;
				case "arab" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_ARABIZED;
				case "hebr" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_HEBRAIZED;
				case "grek" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_HELLENIZED;
				default -> null;
			};
			if(script != null)
				return script;
		}
		return switch(subtags[0].toLowerCase(Locale.ROOT)){
			case "en" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_ANGLICIZED;
			case "fr" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_FRANCIZED;
			case "de" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_GERMANIZED;
			case "it" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_ITALIANIZED;
			case "es" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_HISPANICIZED;
			case "pt" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_LUSITANIZED;
			case "la" -> TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_LATINIZED;
			default -> null;
		};
	}

	public static String mapAttribute(String gedcomTag, String defaultValue){
		if(gedcomTag == null)
			return defaultValue;

		if(defaultValue != null && !defaultValue.isEmpty()){
			defaultValue = defaultValue.toLowerCase(Locale.ROOT);
		}
		else{
			defaultValue = "other";
		}
		return ATTRIBUTE_MAP.getOrDefault(gedcomTag.toUpperCase(Locale.ROOT), defaultValue);
	}

	public static String mapSex(String gedcomSex){
		if(gedcomSex == null){
			return IndividualReader.ENUM_SEX_UNKNOWN;
		}
		return SEX_MAP.getOrDefault(gedcomSex.toUpperCase(Locale.ROOT), IndividualReader.ENUM_SEX_UNKNOWN);
	}

	public static String mapMediaType(String gedcomMedia){
		if(gedcomMedia == null){
			return null;
		}
		return MEDIA_TYPE_MAP.getOrDefault(gedcomMedia.toUpperCase(Locale.ROOT), gedcomMedia.toLowerCase(Locale.ROOT));
	}

	/**
	 * Determines the kind of a FLEF MediaRecord.
	 *
	 * @param gedcomMediaType	The SOURCE_MEDIA_TYPE (FORM.TYPE or FORM.MEDI, e.g. {@code photo}), may be {@code null}.
	 * @param gedcomFormat	The MULTIMEDIA_FORMAT (FORM, e.g. {@code jpg}, or a media type as {@code image/jpeg}), may be
	 * 	{@code null}.
	 * @param uri	The file, may be {@code null}.
	 * @return	The kind ({@code image}, {@code document}, {@code audio}, {@code video}).
	 */
	public static String mapMediaKind(final String gedcomMediaType, final String gedcomFormat, final String uri){
		if(gedcomMediaType != null)
			switch(gedcomMediaType.trim().toLowerCase(Locale.ROOT)){
				case "photo" -> {
					return MediaReader.ENUM_KIND_IMAGE;
				}
				case "audio" -> {
					return MediaReader.ENUM_KIND_AUDIO;
				}
				case "video" -> {
					return MediaReader.ENUM_KIND_VIDEO;
				}
				default -> {}
			}

		if(StringUtils.isNotBlank(gedcomFormat)){
			final String format = gedcomFormat.trim().toLowerCase(Locale.ROOT);
			if(format.startsWith("image/"))
				return MediaReader.ENUM_KIND_IMAGE;
			if(format.startsWith("audio/"))
				return MediaReader.ENUM_KIND_AUDIO;
			if(format.startsWith("video/"))
				return MediaReader.ENUM_KIND_VIDEO;
			if(!format.contains("/"))
				return MediaReader.inferKind("file." + format);
		}
		return MediaReader.inferKind(uri);
	}

	/**
	 * Maps GEDCOM RESN (restriction notice) tag values to FLEF privacy levels.
	 *
	 * @param gedcomLevel the raw value of the RESN tag (e.g., "locked", "privacy")
	 * @return the mapped FLEF privacy level string ("restricted" or "confidential"), or {@code null} if the restriction does
	 * 	not limit the diffusion of the data (e.g. "locked")
	 */
	public static String mapPrivacyLevel(String gedcomLevel){
		if(StringUtils.isEmpty(gedcomLevel)){
			return null;
		}

		// Gestione di valori multipli separati da spazio o virgola (es. "privacy, locked"): vince il più restrittivo
		String level = null;
		for(final String value : StringUtils.split(gedcomLevel.trim().toUpperCase(Locale.ROOT), ", ")){
			final String mapped = PRIVACY_LEVEL_MAP.get(value);
			if(mapped != null && (level == null || PrivacyReader.ENUM_LEVEL_CONFIDENTIAL.equals(mapped)))
				level = mapped;
		}
		return level;
	}

	private GEDCOMMapper(){
	}

}
