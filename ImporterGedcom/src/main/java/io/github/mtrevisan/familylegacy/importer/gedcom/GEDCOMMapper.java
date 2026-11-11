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

import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.TextValueVariantReader;
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
	public static final String TAG_NOTE = "NOTE";
	public static final String TAG_SOURCE = "SOUR";
	public static final String TAG_DATE = "DATE";
	public static final String TAG_CENSUS = "CENS";
	public static final String TAG_TYPE = "TYPE";
	public static final String TAG_MULTIMEDIA = "OBJE";
	public static final String TAG_PLACE = "PLAC";
	public static final String TAG_LANGUAGE = "LANG";
	public static final String TAG_WIFE = "WIFE";
	public static final String TAG_HUSBAND = "HUSB";
	public static final String TAG_ROLE = "ROLE";
	public static final String TAG_REPOSITORY = "REPO";
	public static final String TAG_INDIVIDUAL = "INDI";
	public static final String TAG_ADOPTION = "ADOP";
	public static final String TAG_VERSION = "VERS";
	public static final String TAG_TRANSLATION = "TRAN";
	public static final String TAG_TITLE = "TITL";
	public static final String TAG_NAME = "NAME";
	public static final String TAG_FAMILY_CHILD = "FAMC";
	public static final String TAG_ASSOCIATION = "ASSO";
	public static final String TAG_AGE = "AGE";
	public static final String TAG_ADDRESS = "ADDR";
	public static final String TAG_HEADER = "HEAD";
	public static final String TAG_FAMILY = "FAM";
	public static final String TAG_FORM = "FORM";
	public static final String TAG_DESCRIPTION = "DESC";
	public static final String TAG_SEALING_CHILD = "SLGC";
	/** Custom tag: a task to do. */
	public static final String TAG_TODO = "_TODO";
	/** Custom tag: the married name. */
	public static final String TAG_MARRIED_NAME = "_MARNM";
	/** Custom tag: an alias ("also known as"). */
	public static final String TAG_AKA = "_AKA";
	/** The prefix of the links to the records of the file in the notes. */
	public static final String FLEF_LINK_PREFIX = "flef:";
	/** The media type of a note with links. */
	public static final String MIME_MARKDOWN = "text/markdown";

	public static final String TAG_REFERENCE_NUMBER = "REFN";
	public static final String TAG_UID = "UID";
	public static final String TAG_CUSTOM_UID = "_UID";
	public static final String TAG_EXTERNAL_ID = "EXID";
	public static final String TAG_RECORD_FILE_NUMBER = "RFN";
	public static final String TAG_ANCESTRAL_FILE_NUMBER = "AFN";
	public static final String TAG_FAMILY_SEARCH_ID = "_FSFTID";
	public static final String TAG_POSTAL_CODE = "POST";
	public static final String TAG_PHONE = "PHON";
	public static final String TAG_EMAIL = "EMAIL";
	public static final String TAG_FAX = "FAX";
	public static final String TAG_WEB = "WWW";
	public static final String TAG_CUSTOM_EMAIL = "_EMAIL";
	public static final String TAG_CUSTOM_WEB = "_WWW";
	public static final String TAG_MAP = "MAP";
	public static final String TAG_CROP = "CROP";
	public static final String TAG_BLOB = "BLOB";
	public static final String TAG_STATUS = "STAT";
	public static final String TAG_SEX = "SEX";
	public static final String TAG_DATA = "DATA";
	public static final String TAG_CHANGE = "CHAN";
	public static final String TAG_CREATION = "CREA";
	public static final String TAG_RESTRICTION = "RESN";
	public static final String TAG_SPOUSE_FAMILY = "FAMS";
	public static final String TAG_CHILD = "CHIL";
	public static final String TAG_NON_EVENT = "NO";
	public static final String TAG_ALIAS = "ALIA";
	public static final String TAG_SUBMITTER = "SUBM";
	public static final String TAG_ABBREVIATION = "ABBR";
	public static final String TAG_AUTHOR = "AUTH";
	public static final String TAG_PUBLICATION = "PUBL";
	public static final String TAG_TEXT = "TEXT";
	public static final String TAG_FILE = "FILE";
	public static final String TAG_NUMBER_OF_CHILDREN = "NCHI";
	public static final String TAG_CAUSE = "CAUS";
	public static final String TAG_AGENCY = "AGNC";
	public static final String TAG_RELIGION = "RELI";
	public static final String TAG_TEMPLE = "TEMP";
	public static final String TAG_CONCATENATION = "CONC";
	public static final String TAG_CONTINUATION = "CONT";
	public static final String TAG_MIME = "MIME";
	public static final String TAG_CUSTOM_DATE = "_DATE";
	public static final String TAG_CUSTOM_PUBLISHER = "_PUBL";
	public static final String TAG_CUSTOM_PRIMARY = "_PRIMARY";
	public static final String TAG_CUSTOM_PREFERRED = "_PREF";
	public static final String TAG_CUSTOM_CUT = "_CUTD";
	public static final String TAG_LATITUDE = "LATI";
	public static final String TAG_LONGITUDE = "LONG";
	public static final String TAG_PHONETIC = "FONE";
	public static final String TAG_ROMANIZED = "ROMN";
	public static final String TAG_NAME_PREFIX = "NPFX";
	public static final String TAG_GIVEN_NAME = "GIVN";
	public static final String TAG_NICKNAME = "NICK";
	public static final String TAG_SURNAME_PREFIX = "SPFX";
	public static final String TAG_SURNAME = "SURN";
	public static final String TAG_NAME_SUFFIX = "NSFX";

	/** The identifiers that FLEF cannot store: reported as discarded. */
	public static final Set<String> IDENTIFIER_TAGS = Set.of(TAG_REFERENCE_NUMBER, TAG_UID, TAG_CUSTOM_UID, TAG_EXTERNAL_ID,
		TAG_RECORD_FILE_NUMBER, TAG_ANCESTRAL_FILE_NUMBER, TAG_FAMILY_SEARCH_ID);
	/**
	 * The technical tags (metadata about the file or about the program that wrote it, links redundant with other records): their
	 * loss is not reported.
	 */
	public static final Set<String> TECHNICAL_TAGS = Set.of("RIN", "SUBN", "SDATE", "ANCI", "DESI", TAG_SPOUSE_FAMILY, TAG_SUBMITTER,
		"CHAR", "GEDC", "DEST", TAG_FILE, "SCHMA", "_CRE", "_UPD", TAG_CHANGE, TAG_CREATION, TAG_CONCATENATION, TAG_CONTINUATION);
	/** The contact tags of an address. */
	public static final Set<String> CONTACT_TAGS = Set.of(TAG_PHONE, TAG_EMAIL, TAG_FAX, TAG_WEB, TAG_CUSTOM_EMAIL, TAG_CUSTOM_WEB);

	/** The tags of an individual read by the converter (apart from the events and the attributes). */
	public static final Set<String> INDIVIDUAL_CONSUMED_TAGS = Set.of(TAG_NAME, TAG_SEX, TAG_SOURCE, TAG_NOTE, TAG_MULTIMEDIA,
		TAG_FAMILY_CHILD, TAG_ALIAS, TAG_ASSOCIATION, TAG_RESTRICTION);
	/** The tags of a family read by the converter (apart from the events and the attributes). */
	public static final Set<String> FAMILY_CONSUMED_TAGS = Set.of(TAG_HUSBAND, TAG_WIFE, TAG_CHILD, TAG_NUMBER_OF_CHILDREN, TAG_SOURCE,
		TAG_NOTE, TAG_NON_EVENT, TAG_ASSOCIATION, TAG_MULTIMEDIA, TAG_RESTRICTION);
	/** The tags of an event or of an attribute read by the converter. */
	public static final Set<String> EVENT_CONSUMED_TAGS = Set.of(TAG_TYPE, TAG_DATE, TAG_PLACE, TAG_ADDRESS, TAG_CAUSE, TAG_AGENCY,
		TAG_RELIGION, TAG_TEMPLE, TAG_STATUS, TAG_FAMILY_CHILD, TAG_SOURCE, TAG_NOTE, TAG_MULTIMEDIA, TAG_RESTRICTION, TAG_AGE,
		TAG_HUSBAND, TAG_WIFE, TAG_ASSOCIATION, TAG_POSTAL_CODE, TAG_PHONE, TAG_EMAIL, TAG_FAX, TAG_WEB, TAG_CUSTOM_EMAIL, TAG_CUSTOM_WEB);
	/** The tags of a source read by the converter. */
	public static final Set<String> SOURCE_CONSUMED_TAGS = Set.of(TAG_TITLE, TAG_ABBREVIATION, TAG_AUTHOR, TAG_PUBLICATION, TAG_DATA,
		TAG_TEXT, TAG_REPOSITORY, TAG_MULTIMEDIA, TAG_NOTE, TAG_RESTRICTION);
	/** The tags of a repository read by the converter. */
	public static final Set<String> REPOSITORY_CONSUMED_TAGS = Set.of(TAG_NAME, TAG_PLACE, TAG_ADDRESS, TAG_PHONE, TAG_EMAIL, TAG_FAX,
		TAG_WEB, TAG_CUSTOM_EMAIL, TAG_CUSTOM_WEB, TAG_NOTE, TAG_RESTRICTION);
	/** The tags of a multimedia record read by the converter. */
	public static final Set<String> MULTIMEDIA_CONSUMED_TAGS = Set.of(TAG_FILE, TAG_FORM, TAG_TITLE, TAG_CUSTOM_DATE, TAG_CUSTOM_PUBLISHER,
		TAG_CUSTOM_PRIMARY, TAG_CUSTOM_PREFERRED, TAG_CUSTOM_CUT, TAG_CROP, TAG_NOTE, TAG_RESTRICTION, TAG_SOURCE, TAG_BLOB);
	/** The tags of a submitter read by the converter (or reported apart). */
	public static final Set<String> SUBMITTER_CONSUMED_TAGS = Set.of(TAG_NAME, TAG_ADDRESS, TAG_PHONE, TAG_EMAIL, TAG_FAX, TAG_WEB,
		TAG_CUSTOM_EMAIL, TAG_CUSTOM_WEB, TAG_LANGUAGE, TAG_RECORD_FILE_NUMBER, TAG_NOTE, TAG_MULTIMEDIA);
	/** The tags of a note record read by the converter. */
	public static final Set<String> NOTE_CONSUMED_TAGS = Set.of(TAG_SOURCE, TAG_MIME, TAG_LANGUAGE, TAG_TRANSLATION);
	/** The tags of a personal name read by the converter. */
	public static final Set<String> NAME_CONSUMED_TAGS = Set.of(TAG_TYPE, TAG_NAME_PREFIX, TAG_GIVEN_NAME, TAG_NICKNAME,
		TAG_SURNAME_PREFIX, TAG_SURNAME, TAG_NAME_SUFFIX, TAG_PHONETIC, TAG_ROMANIZED, TAG_TRANSLATION, TAG_SOURCE, TAG_NOTE);

	/** Custom tags (underscore tags) that never are an event, even with a date: they are about the record itself. */
	private static final Set<String> CUSTOM_NON_EVENT_TAGS = Set.of("_UID", "_FSFTID", "_CRE", "_UPD");

	/**
	 * Whether a custom tag (underscore tag) of an individual or of a family is an event: it holds a date or a place.
	 * <p>
	 * The custom tags without data (flags, identifiers, creation and update stamps) are not.
	 */
	public static boolean isCustomEvent(final GEDCOMNode node){
		final String tag = node.getTag();
		if(!StringUtils.startsWith(tag, "_") || CUSTOM_NON_EVENT_TAGS.contains(tag.toUpperCase(Locale.ROOT))
				|| TAG_TODO.equalsIgnoreCase(tag) || TAG_MARRIED_NAME.equalsIgnoreCase(tag) || TAG_AKA.equalsIgnoreCase(tag))
			return false;

		return (GEDCOMHelper.findFirstChild(node, TAG_DATE) != null || GEDCOMHelper.findFirstChild(node, TAG_PLACE) != null);
	}

	// ---- Tag degli eventi (INDIVIDUAL_EVENT_STRUCTURE, FAMILY_EVENT_STRUCTURE, LDS ordinances) ----
	public static final Set<String> INDIVIDUAL_EVENT_TAGS = Set.of("BIRT", "CHR", "DEAT", "BURI", "CREM", TAG_ADOPTION, "BAPM",
		"BARM", "BASM", "BLES", "CHRA", "CONF", "FCOM", "ORDN", "NATU", "EMIG", "IMMI", "CENS", "PROB", "WILL", "GRAD", "RETI",
		TAG_GENERIC_EVENT, "BAPL", "CONL", "ENDL", "INIL", "SLGC");
	/** The events of an individual that are about the individual itself: whoever it is told of is the principal of them, not a witness. */
	public static final Set<String> PRINCIPAL_ONLY_EVENT_TAGS = Set.of("BIRT", "CHR", "BAPM", "DEAT", "BURI", "CREM");
	public static final Set<String> FAMILY_EVENT_TAGS = Set.of("ANUL", "CENS", "DIV", "DIVF", "ENGA", "MARB", "MARC", "MARR",
		"MARL", "MARS", TAG_GENERIC_EVENT, "SLGS");

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
		Map.entry("move", EventReader.ENUM_TYPE_RELOCATION),
		Map.entry("moved", EventReader.ENUM_TYPE_RELOCATION),
		Map.entry("military_enlistment", EventReader.ENUM_TYPE_MILITARY_INDUCTION),
		Map.entry("enlistment", EventReader.ENUM_TYPE_MILITARY_INDUCTION),
		Map.entry("military", EventReader.ENUM_TYPE_MILITARY_SERVICE),
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
		Map.entry("incarceration", EventReader.ENUM_TYPE_PRISON),
		Map.entry("degree", EventReader.ENUM_TYPE_GRADUATION),
		Map.entry("probate", EventReader.ENUM_TYPE_PROBATE));
	// ---- TYPE di EVEN/FACT che FLEF rappresenta altrove ----
	/** EVEN/FACT types that are names of the individual: they become a {@code PersonalNameStructure} with this name part type. */
	public static final Map<String, String> NAME_PART_FACT_TYPES = Map.of(
		NameReader.ENUM_PART_TYPE_FAMILY_NICKNAME, NameReader.ENUM_PART_TYPE_FAMILY_NICKNAME,
		NameReader.ENUM_PART_TYPE_NICKNAME, NameReader.ENUM_PART_TYPE_NICKNAME);
	/**
	 * EVEN/FACT types that are attributes of the individual (the FLEF types of IndividualAttributeRecord, and their synonyms):
	 * they become an {@code IndividualAttributeRecord} of this type.
	 */
	public static final Map<String, String> ATTRIBUTE_FACT_TYPES = Map.ofEntries(
		Map.entry("family_address", IndividualAttributeReader.ENUM_TYPE_RESIDENCE),
		Map.entry("address", IndividualAttributeReader.ENUM_TYPE_RESIDENCE),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_RESIDENCE, IndividualAttributeReader.ENUM_TYPE_RESIDENCE),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_CHARACTERISTIC, IndividualAttributeReader.ENUM_TYPE_CHARACTERISTIC),
		Map.entry("physical_description", IndividualAttributeReader.ENUM_TYPE_CHARACTERISTIC),
		Map.entry("description", IndividualAttributeReader.ENUM_TYPE_CHARACTERISTIC),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_OCCUPATION, IndividualAttributeReader.ENUM_TYPE_OCCUPATION),
		Map.entry("profession", IndividualAttributeReader.ENUM_TYPE_OCCUPATION),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_POSSESSION, IndividualAttributeReader.ENUM_TYPE_POSSESSION),
		Map.entry("property", IndividualAttributeReader.ENUM_TYPE_POSSESSION),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_MILITARY_RANK, IndividualAttributeReader.ENUM_TYPE_MILITARY_RANK),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_CASTE, IndividualAttributeReader.ENUM_TYPE_CASTE),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_SOCIAL_CLASS, IndividualAttributeReader.ENUM_TYPE_SOCIAL_CLASS),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_ETHNICITY, IndividualAttributeReader.ENUM_TYPE_ETHNICITY),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_CITIZENSHIP, IndividualAttributeReader.ENUM_TYPE_CITIZENSHIP),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_NATIONALITY, IndividualAttributeReader.ENUM_TYPE_NATIONALITY),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_TITLE, IndividualAttributeReader.ENUM_TYPE_TITLE),
		Map.entry("nobility_title", IndividualAttributeReader.ENUM_TYPE_TITLE),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_CHILDREN_COUNT, IndividualAttributeReader.ENUM_TYPE_CHILDREN_COUNT),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_MARRIAGES_COUNT, IndividualAttributeReader.ENUM_TYPE_MARRIAGES_COUNT),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_RELIGION, IndividualAttributeReader.ENUM_TYPE_RELIGION),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_LANGUAGE, IndividualAttributeReader.ENUM_TYPE_LANGUAGE),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_LITERACY, IndividualAttributeReader.ENUM_TYPE_LITERACY),
		Map.entry(IndividualAttributeReader.ENUM_TYPE_APPRENTICESHIP, IndividualAttributeReader.ENUM_TYPE_APPRENTICESHIP));
	/** EVEN types of a family that are attributes of the family: they become a {@code GroupAttributeRecord} of this type. */
	public static final Map<String, String> GROUP_ATTRIBUTE_FACT_TYPES = Map.of(
		"family_address", IndividualAttributeReader.ENUM_TYPE_RESIDENCE,
		"address", IndividualAttributeReader.ENUM_TYPE_RESIDENCE,
		IndividualAttributeReader.ENUM_TYPE_RESIDENCE, IndividualAttributeReader.ENUM_TYPE_RESIDENCE,
		IndividualAttributeReader.ENUM_TYPE_RELIGION, IndividualAttributeReader.ENUM_TYPE_RELIGION,
		IndividualAttributeReader.ENUM_TYPE_LANGUAGE, IndividualAttributeReader.ENUM_TYPE_LANGUAGE,
		IndividualAttributeReader.ENUM_TYPE_SOCIAL_CLASS, IndividualAttributeReader.ENUM_TYPE_SOCIAL_CLASS);

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
		EVENT_MAP.put(TAG_ADOPTION, EventReader.ENUM_TYPE_ADOPTION);
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
		ATTRIBUTE_MAP.put(TAG_TITLE, IndividualAttributeReader.ENUM_TYPE_TITLE);
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
		MEDIA_TYPE_MAP.put("AUDIO", SourceReader.ENUM_MEDIA_TYPE_AUDIO);
		MEDIA_TYPE_MAP.put("BOOK", SourceReader.ENUM_MEDIA_TYPE_BOOK);
		MEDIA_TYPE_MAP.put("CARD", SourceReader.ENUM_MEDIA_TYPE_CARD);
		MEDIA_TYPE_MAP.put("ELECTRONIC", SourceReader.ENUM_MEDIA_TYPE_ELECTRONIC);
		MEDIA_TYPE_MAP.put("FICHE", SourceReader.ENUM_MEDIA_TYPE_FICHE);
		MEDIA_TYPE_MAP.put("FILM", SourceReader.ENUM_MEDIA_TYPE_FILM);
		MEDIA_TYPE_MAP.put("MAGAZINE", SourceReader.ENUM_MEDIA_TYPE_MAGAZINE);
		MEDIA_TYPE_MAP.put("MANUSCRIPT", SourceReader.ENUM_MEDIA_TYPE_MANUSCRIPT);
		MEDIA_TYPE_MAP.put("MAP", SourceReader.ENUM_MEDIA_TYPE_MAP);
		MEDIA_TYPE_MAP.put("NEWSPAPER", SourceReader.ENUM_MEDIA_TYPE_NEWSPAPER);
		MEDIA_TYPE_MAP.put("PHOTO", SourceReader.ENUM_MEDIA_TYPE_PHOTO);
		MEDIA_TYPE_MAP.put("TOMBSTONE", SourceReader.ENUM_MEDIA_TYPE_TOMBSTONE);
		MEDIA_TYPE_MAP.put("VIDEO", SourceReader.ENUM_MEDIA_TYPE_VIDEO);

		// ------------------------------
		// Roles
		// ------------------------------
		ROLE_MAP.put("CHILD", EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		ROLE_MAP.put("PARENT", EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		ROLE_MAP.put("SPOUSE", EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		ROLE_MAP.put("SPONSOR", EventParticipationReader.ENUM_ROLE_SPONSOR);
		ROLE_MAP.put("POWER_OF_ATTORNEY", EventParticipationReader.ENUM_ROLE_POWER_OF_ATTORNEY);
		ROLE_MAP.put("PRISONER", EventParticipationReader.ENUM_ROLE_PRISONER);
		ROLE_MAP.put("WITNESS", EventParticipationReader.ENUM_ROLE_WITNESS);
		ROLE_MAP.put("OFFICIANT", EventParticipationReader.ENUM_ROLE_OFFICIANT);
		ROLE_MAP.put("DECLARANT", EventParticipationReader.ENUM_ROLE_INFORMANT);
		ROLE_MAP.put("INFORMANT", EventParticipationReader.ENUM_ROLE_INFORMANT);
		ROLE_MAP.put("EXECUTOR", EventParticipationReader.ENUM_ROLE_EXECUTOR);
		ROLE_MAP.put("GRANTOR", EventParticipationReader.ENUM_ROLE_GRANTOR);
		ROLE_MAP.put("GRANTEE", EventParticipationReader.ENUM_ROLE_GRANTEE);
		ROLE_MAP.put("LANDLORD", EventParticipationReader.ENUM_ROLE_LANDLORD);
		ROLE_MAP.put("TENANT", EventParticipationReader.ENUM_ROLE_TENANT);
		ROLE_MAP.put("SOLDIER", EventParticipationReader.ENUM_ROLE_SOLDIER);
		ROLE_MAP.put("COMMANDER", EventParticipationReader.ENUM_ROLE_COMMANDER);
		ROLE_MAP.put("VICTIM", EventParticipationReader.ENUM_ROLE_VICTIM);
		ROLE_MAP.put("SURVIVOR", EventParticipationReader.ENUM_ROLE_SURVIVOR);
		ROLE_MAP.put("ACCUSED", EventParticipationReader.ENUM_ROLE_ACCUSED);
		ROLE_MAP.put("JUDGE", EventParticipationReader.ENUM_ROLE_JUDGE);
		ROLE_MAP.put("PROPERTY", EventParticipationReader.ENUM_ROLE_PROPERTY);

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
		//a custom tag (e.g. _MILT) gives a custom type, without the leading underscore
		if(StringUtils.startsWith(gedcomTag, "_")){
			final String customToken = toTypeToken(gedcomTag);
			if(customToken != null)
				return EVENT_TYPE_SYNONYMS.getOrDefault(customToken, customToken);
		}
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
		return ("aka".equals(token)? NameReader.ENUM_PERSONAL_TYPE_ALIAS: token);
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

		final String role = gedcomRole.trim()
			.toUpperCase(Locale.ROOT);
		return switch(role){
			case "WITN" -> EventParticipationReader.ENUM_ROLE_WITNESS;
			case "OFFICIATOR", "CLERGY" -> EventParticipationReader.ENUM_ROLE_OFFICIANT;
			case "GODP" -> EventParticipationReader.ENUM_ROLE_SPONSOR;
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
			case TAG_HUSBAND -> "Husband";
			case "MOTH" -> "Mother";
			case "MULTIPLE" -> "Multiple";
			case "NGHBR" -> "Neighbor";
			case "OFFICIATOR" -> "Officiator";
			case "PARENT" -> "Parent";
			case "SPOU" -> "Spouse";
			case TAG_WIFE -> "Wife";
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
				case SourceReader.ENUM_MEDIA_TYPE_PHOTO -> {
					return MediaReader.ENUM_KIND_IMAGE;
				}
				case SourceReader.ENUM_MEDIA_TYPE_AUDIO -> {
					return MediaReader.ENUM_KIND_AUDIO;
				}
				case SourceReader.ENUM_MEDIA_TYPE_VIDEO -> {
					return MediaReader.ENUM_KIND_VIDEO;
				}
				default -> {}
			}

		if(StringUtils.isNotBlank(gedcomFormat)){
			final String format = gedcomFormat.trim()
				.toLowerCase(Locale.ROOT);
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
