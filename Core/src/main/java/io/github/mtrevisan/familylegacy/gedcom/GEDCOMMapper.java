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
package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


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
	private static final Set<String> FLEF_EVENT_TYPES = Set.of("birth", "adoption", "death", "cremation", "burial",
		"coroner_report", "illness", "hospitalization", "medical_procedure", "engagement", "marriage_bann", "marriage_contract",
		"marriage_license", "marriage_settlement", "marriage", "divorce_filed", "divorce_decree", "divorce", "annulment", "education",
		"graduation", "retirement", "military_induction", "military_muster_roll", "military_service", "military_award",
		"military_release", "military_discharge", "military_resignation", "military_retirement", "prison", "pardon", "jury_duty",
		"honor", "bankruptcy", "immigration", "naturalization", "emigration", "deportation", "internment", "liberation",
		"emancipation", "relocation", "census", "deed", "escrow", "chancery", "will", "probate", "guardianship");
	// ---- Sinonimi del TYPE di EVEN verso i tipi di evento FLEF ----
	private static final Map<String, String> EVENT_TYPE_SYNONYMS = Map.ofEntries(
		Map.entry("move", "relocation"),
		Map.entry("moved", "relocation"),
		Map.entry("military_enlistment", "military_induction"),
		Map.entry("enlistment", "military_induction"),
		Map.entry("military", "military_service"),
		Map.entry("hospitalisation", "hospitalization"),
		Map.entry("medical", "medical_procedure"),
		Map.entry("surgery", "medical_procedure"),
		Map.entry("imprisonment", "prison"),
		Map.entry("banns", "marriage_bann"),
		Map.entry("marriage_banns", "marriage_bann"),
		Map.entry("engaged", "engagement"),
		Map.entry("divorce_filing", "divorce_filed"),
		Map.entry("award", "honor"),
		Map.entry("decoration", "honor"),
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
		Map.entry("family_address", "residence"),
		Map.entry("address", "residence"),
		Map.entry("residence", "residence"),
		Map.entry("characteristic", "characteristic"),
		Map.entry("physical_description", "characteristic"),
		Map.entry("description", "characteristic"),
		Map.entry("occupation", "occupation"),
		Map.entry("profession", "occupation"),
		Map.entry("possession", "possession"),
		Map.entry("property", "possession"),
		Map.entry("military_rank", "military_rank"),
		Map.entry("caste", "caste"),
		Map.entry("social_class", "social_class"),
		Map.entry("ethnicity", "ethnicity"),
		Map.entry("citizenship", "citizenship"),
		Map.entry("nationality", "nationality"),
		Map.entry("national_id", "national_id"),
		Map.entry("title", "title"),
		Map.entry("nobility_title", "title"),
		Map.entry("children_count", "children_count"),
		Map.entry("marriages_count", "marriages_count"),
		Map.entry("religion", "religion"),
		Map.entry("language", "language"),
		Map.entry("literacy", "literacy"));
	/** EVEN types of a family that are attributes of the family: they become a {@code GroupAttributeRecord} of this type. */
	public static final Map<String, String> GROUP_ATTRIBUTE_FACT_TYPES = Map.of(
		"family_address", "residence",
		"address", "residence",
		"residence", "residence",
		"religion", "religion",
		"language", "language",
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
		EVENT_MAP.put("BIRT", "birth");
		EVENT_MAP.put("CHR", "christening");
		EVENT_MAP.put("DEAT", "death");
		EVENT_MAP.put("BURI", "burial");
		EVENT_MAP.put("CREM", "cremation");
		EVENT_MAP.put("ADOP", "adoption");
		EVENT_MAP.put("BAPM", "baptism");
		EVENT_MAP.put("BARM", "bar_mitzvah");
		EVENT_MAP.put("BASM", "bat_mitzvah");
		EVENT_MAP.put("BLES", "blessing");
		EVENT_MAP.put("CHRA", "adult_christening");
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
		EVENT_MAP.put("DIVF", "divorce_filed");
		EVENT_MAP.put("ANUL", "annulment");
		EVENT_MAP.put("ENGA", "engagement");
		EVENT_MAP.put("MARB", "marriage_bann");
		EVENT_MAP.put("MARC", "marriage_contract");
		EVENT_MAP.put("MARL", "marriage_license");
		EVENT_MAP.put("MARS", "marriage_settlement");
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
		ATTRIBUTE_MAP.put("CAST", "caste");
		ATTRIBUTE_MAP.put("DSCR", "characteristic");
		ATTRIBUTE_MAP.put("EDUC", "education");
		ATTRIBUTE_MAP.put("IDNO", "national_id");
		ATTRIBUTE_MAP.put("NATI", "nationality");
		ATTRIBUTE_MAP.put("NCHI", "children_count");
		ATTRIBUTE_MAP.put("NMR", "marriages_count");
		ATTRIBUTE_MAP.put("OCCU", "occupation");
		ATTRIBUTE_MAP.put("PROP", "possession");
		ATTRIBUTE_MAP.put("RELI", "religion");
		ATTRIBUTE_MAP.put("RESI", "residence");
		ATTRIBUTE_MAP.put("SSN", "national_id");
		ATTRIBUTE_MAP.put("TITL", "title");
		ATTRIBUTE_MAP.put(TAG_GENERIC_FACT, "fact");

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
		// Roles
		// ------------------------------
		ROLE_MAP.put("CHILD", "principal");
		ROLE_MAP.put("PARENT", "principal");
		ROLE_MAP.put("SPOUSE", "principal");
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
		PRIVACY_LEVEL_MAP.put("PUBLIC", "public");
		PRIVACY_LEVEL_MAP.put("RESTRICTED", "restricted");
		PRIVACY_LEVEL_MAP.put("PRIVACY", "restricted");         // RESN privacy: dati personali riservati
		PRIVACY_LEVEL_MAP.put("PRIVATE", "restricted");         // RESN privacy: dati personali riservati
		// RESN locked impedisce le modifiche, non la diffusione: non ha corrispondente in FLEF
		PRIVACY_LEVEL_MAP.put("CONFIDENTIAL", "confidential"); // RESN confidential (estensione standard GEDCOM)
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
			return "biological_child";

		return switch(gedcomPedigree.trim().toUpperCase(Locale.ROOT)){
			case "ADOPTED" -> "adoptive_child";
			case "FOSTER" -> "foster_child";
			//a sealing does not tell the nature of the relationship
			case "SEALING", "SEALED" -> "child";
			default -> "biological_child";
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
			case "CHALLENGED", "DISPROVEN" -> "low";
			case "PROVEN" -> "high";
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
				case "latn" -> "romanized";
				case "cyrl" -> "cyrillized";
				case "arab" -> "arabized";
				case "hebr" -> "hebraized";
				case "grek" -> "hellenized";
				default -> null;
			};
			if(script != null)
				return script;
		}
		return switch(subtags[0].toLowerCase(Locale.ROOT)){
			case "en" -> "anglicized";
			case "fr" -> "francized";
			case "de" -> "germanized";
			case "it" -> "italianized";
			case "es" -> "hispanicized";
			case "pt" -> "lusitanized";
			case "la" -> "latinized";
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
			return "unknown";
		}
		return SEX_MAP.getOrDefault(gedcomSex.toUpperCase(Locale.ROOT), "unknown");
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
			if(mapped != null && (level == null || "confidential".equals(mapped)))
				level = mapped;
		}
		return level;
	}

	private GEDCOMMapper(){
	}

}
