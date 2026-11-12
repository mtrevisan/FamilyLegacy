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
package io.github.mtrevisan.familylegacy.importer.gramps;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.Deduplicator;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.PlaceCache;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HeaderReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.util.SoftwareInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Converts a Gramps XML document (version 1.7) into a FLEF model, directly: nothing is lost on the way (as it is when the file is
 * rewritten as GEDCOM), and what Gramps keeps as records on its own stays so.
 * <ul>
 *   <li>an event is a single event, whatever the number of its participants, each one with the role it has (principal, officiant,
 *   witness, ...) and the evidence of its own;</li>
 *   <li>a place is a place on its own, with its names (and their languages and dates), its type and its coordinates, linked to the
 *   place that contains it by a place relationship;</li>
 *   <li>a family is a group, whose members are linked to it; each parent of a child is related to the child with what Gramps says
 *   of that parent (birth, adopted, stepchild, ...);</li>
 *   <li>the dates keep what they mean (before, after, about, between, from-to), with their calendar;</li>
 *   <li>a citation is written where it is cited, with its page, its date and its confidence; the formatting of a note is kept as
 *   Markdown.</li>
 * </ul>
 * Gramps has no counterpart in FLEF for the bookmarks and the formats of the names; the regions of the images whose file cannot be
 * read, and the dates that cannot be normalized (a text, a new-year rule), are kept as notes.
 */
public final class GrampsXMLToFLEFConverter{

	private static final String PROTOCOL_NAME = "Family LEgacy Format";
	private static final String PROTOCOL_VERSION = "0.2.0";

	private static final String ROLE_PRINCIPAL = EventParticipationReader.ENUM_ROLE_PRINCIPAL;
	private static final String PRIVACY_RESTRICTED = PrivacyReader.ENUM_LEVEL_RESTRICTED;
	private static final String MIME_MARKDOWN = "text/markdown";
	/** The size of an image that cannot be read. */
	private static final int[] NO_SIZE = new int[0];
	private static final String TYPE_UNKNOWN = "unknown";
	/** The tag of the header, as it is written in a file. */
	private static final String TAG_HEADER = HeaderReader.TYPE.toLowerCase(Locale.ROOT);
	/** The roles of the parents in the family they form (the free text of the role of the group member). */
	private static final String ROLE_FATHER = "father";
	private static final String ROLE_MOTHER = "mother";

	/** Names of the elements and of the attributes of Gramps XML that are used many times. */
	private static final String GRAMPS_TYPE = "type";
	private static final String GRAMPS_HANDLE = "handle";
	private static final String GRAMPS_HLINK = "hlink";
	private static final String GRAMPS_VALUE = "value";
	private static final String GRAMPS_DESCRIPTION = "description";
	private static final String GRAMPS_NAME = "name";
	private static final String GRAMPS_TEXT = "text";
	private static final String GRAMPS_PERSON = "person";
	private static final String GRAMPS_PLACEOBJ = "placeobj";
	private static final String GRAMPS_OBJECT = "object";
	private static final String GRAMPS_FAMILY = "family";
	private static final String GRAMPS_REPOSITORY = "repository";
	private static final String GRAMPS_EVENT = "event";
	private static final String GRAMPS_SOURCE = "source";
	private static final String GRAMPS_ATTRIBUTE = "attribute";
	private static final String GRAMPS_EVENTREF = "eventref";
	private static final String GRAMPS_NOTEREF = "noteref";
	private static final String GRAMPS_NOTE = "note";
	private static final String GRAMPS_OBJREF = "objref";
	private static final String GRAMPS_URL = "url";
	private static final String GRAMPS_PHONE = "phone";
	private static final String GRAMPS_STREET = "street";
	private static final String GRAMPS_LOCALITY = "locality";
	private static final String GRAMPS_CITY = "city";
	private static final String GRAMPS_COUNTY = "county";
	private static final String GRAMPS_STATE = "state";
	private static final String GRAMPS_COUNTRY = "country";
	private static final String GRAMPS_POSTAL = "postal";
	private static final String GRAMPS_FATHER = "father";
	private static final String GRAMPS_MOTHER = "mother";
	private static final String GRAMPS_PLACE = "place";
	private static final String GRAMPS_PLACES = "places";
	private static final String GRAMPS_PEOPLE = "people";
	private static final String GRAMPS_FAMILIES = "families";
	private static final String GRAMPS_PARISH = "parish";
	private static final String GRAMPS_REPOSITORY_WEB_SITE = "web site";
	private static final String GRAMPS_REPOSITORY_SAFE = "safe";
	private static final String GRAMPS_ADDRESS = "address";
	private static final String GRAMPS_CITATIONREF = "citationref";
	private static final String GRAMPS_AGE = "age";
	private static final String GRAMPS_NEWYEAR = "newyear";
	private static final String GRAMPS_PRIMARY = "prim";
	private static final String GRAMPS_TITLE = "title";
	private static final String GRAMPS_SUFFIX = "suffix";
	/** The types of event of Gramps (lowercase). */
	private static final String GRAMPS_EVENT_BIRTH = "birth";
	private static final String GRAMPS_EVENT_DEATH = "death";
	private static final String GRAMPS_EVENT_BURIAL = "burial";
	private static final String GRAMPS_EVENT_CREMATION = "cremation";
	private static final String GRAMPS_EVENT_NATURALIZATION = "naturalization";
	private static final String GRAMPS_EVENT_EMIGRATION = "emigration";
	private static final String GRAMPS_EVENT_IMMIGRATION = "immigration";
	private static final String GRAMPS_EVENT_CENSUS = "census";
	private static final String GRAMPS_EVENT_PROBATE = "probate";
	private static final String GRAMPS_EVENT_WILL = "will";
	private static final String GRAMPS_EVENT_GRADUATION = "graduation";
	private static final String GRAMPS_EVENT_RETIREMENT = "retirement";
	private static final String GRAMPS_EVENT_OCCUPATION = "occupation";
	private static final String GRAMPS_EVENT_RESIDENCE = "residence";
	private static final String GRAMPS_EVENT_EDUCATION = "education";
	private static final String GRAMPS_EVENT_MARRIAGE = "marriage";
	private static final String GRAMPS_EVENT_DIVORCE = "divorce";
	private static final String GRAMPS_EVENT_ANNULMENT = "annulment";
	private static final String GRAMPS_EVENT_ENGAGEMENT = "engagement";

	/** The identifiers that can be kept: they must begin with a letter, and have only letters and digits. */
	private static final Pattern ID_FORMAT = Pattern.compile("[A-Za-z][A-Za-z0-9]*");
	private static final Pattern FAMILY_ID = Pattern.compile("F(\\d+)");

	/** The GEDCOM tag of the types of event of Gramps (lowercase), whose FLEF type is given by the mapper of GEDCOM. */
	private static final Map<String, String> EVENT_TAGS = Map.ofEntries(
		Map.entry(GRAMPS_EVENT_BIRTH, "BIRT"), Map.entry(GRAMPS_EVENT_DEATH, "DEAT"), Map.entry("baptism", "BAPM"), Map.entry(GRAMPS_EVENT_BURIAL, "BURI"),
		Map.entry("christening", "CHR"), Map.entry(GRAMPS_EVENT_CREMATION, "CREM"), Map.entry("adopted", "ADOP"),
		Map.entry("bar mitzvah", "BARM"), Map.entry("bas mitzvah", "BASM"), Map.entry("blessing", "BLES"),
		Map.entry("adult christening", "CHRA"), Map.entry("confirmation", "CONF"), Map.entry("first communion", "FCOM"),
		Map.entry("ordination", "ORDN"), Map.entry(GRAMPS_EVENT_NATURALIZATION, "NATU"), Map.entry(GRAMPS_EVENT_EMIGRATION, "EMIG"),
		Map.entry(GRAMPS_EVENT_IMMIGRATION, "IMMI"), Map.entry(GRAMPS_EVENT_CENSUS, "CENS"), Map.entry(GRAMPS_EVENT_PROBATE, "PROB"), Map.entry(GRAMPS_EVENT_WILL, "WILL"),
		Map.entry(GRAMPS_EVENT_GRADUATION, "GRAD"), Map.entry(GRAMPS_EVENT_RETIREMENT, "RETI"),
		Map.entry(GRAMPS_EVENT_OCCUPATION, "OCCU"), Map.entry(GRAMPS_EVENT_RESIDENCE, "RESI"), Map.entry(GRAMPS_EVENT_EDUCATION, "EDUC"),
		Map.entry("religion", "RELI"), Map.entry("nationality", "NATI"), Map.entry("property", "PROP"),
		Map.entry("caste", "CAST"), Map.entry("physical description", "DSCR"), Map.entry("number of children", "NCHI"),
		Map.entry("number of marriages", "NMR"), Map.entry("social security number", "SSN"),
		Map.entry("nobility title", "TITL"), Map.entry("identification number", "IDNO"),
		Map.entry(GRAMPS_EVENT_MARRIAGE, "MARR"), Map.entry(GRAMPS_EVENT_DIVORCE, "DIV"), Map.entry(GRAMPS_EVENT_ANNULMENT, "ANUL"),
		Map.entry(GRAMPS_EVENT_ENGAGEMENT, "ENGA"), Map.entry("marriage banns", "MARB"), Map.entry("marriage contract", "MARC"),
		Map.entry("marriage license", "MARL"), Map.entry("marriage settlement", "MARS"), Map.entry("divorce filing", "DIVF"));
	/** The tags that are attributes of an individual (they are not events). */
	private static final Set<String> ATTRIBUTE_TAGS = Set.of("OCCU", "RESI", "EDUC", "RELI", "NATI", "PROP", "CAST", "DSCR",
		"NCHI", "NMR", "SSN", "TITL", "IDNO");
	/** The GEDCOM tag of the attributes of Gramps (lowercase). */
	private static final Map<String, String> ATTRIBUTE_TAG_BY_TYPE = Map.of(
		"caste", "CAST", "description", "DSCR", "identification number", "IDNO", "national origin", "NATI",
		"number of children", "NCHI", "social security number", "SSN");
	private static final Map<String, String> LDS_TAGS = Map.of(
		"baptism", "BAPL", "confirmation", "CONL", "endowment", "ENDL", "initiatory", "INIL", "sealed to parents", "SLGC",
		"sealed to spouse", "SLGS", "sealed_to_parents", "SLGC", "sealed_to_spouse", "SLGS");
	private static final Map<String, String> NAME_TYPES = Map.of(
		"birth name", NameReader.ENUM_PERSONAL_TYPE_BIRTH, "also known as", NameReader.ENUM_PERSONAL_TYPE_ALIAS, "married name", NameReader.ENUM_PERSONAL_TYPE_MARRIED);
	/** The type of name part that a derivation of the surname says. */
	private static final Map<String, String> SURNAME_DERIVATIONS = Map.of(
		"patronymic", NameReader.ENUM_PART_TYPE_PATRONYMIC, "matronymic", NameReader.ENUM_PART_TYPE_MATRONYMIC, "location", NameReader.ENUM_PART_TYPE_TOPONYMIC, "occupation", NameReader.ENUM_PART_TYPE_OCCUPATIONAL,
		"feudal", NameReader.ENUM_PART_TYPE_LINEAGE);
	private static final Map<String, String> EVENT_ROLES = Map.ofEntries(
		Map.entry("primary", ROLE_PRINCIPAL), Map.entry("family", ROLE_PRINCIPAL), Map.entry("bride", ROLE_PRINCIPAL),
		Map.entry("groom", ROLE_PRINCIPAL), Map.entry("clergy", EventParticipationReader.ENUM_ROLE_OFFICIANT), Map.entry("celebrant", EventParticipationReader.ENUM_ROLE_OFFICIANT),
		Map.entry("witness", EventParticipationReader.ENUM_ROLE_WITNESS), Map.entry("informant", EventParticipationReader.ENUM_ROLE_INFORMANT), Map.entry("godparent", EventParticipationReader.ENUM_ROLE_SPONSOR));
	private static final Map<String, String> CHILD_RELATIONSHIPS = Map.of(
		"birth", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "adopted", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD, "stepchild", RelationshipReader.ENUM_TYPE_STEP_CHILD,
		"foster", RelationshipReader.ENUM_TYPE_FOSTER_CHILD);

	/** The way an event is written. */
	private record EventKind(String type, String description, String attributeType, String typeNote){}

	/** Who has an event, and what the reference to the event says. */
	private record Owner(boolean isFamily, String handle, Element reference){}



	private final FLEFModel model = new FLEFModel();
	private final Element root;
	private final Map<String, Element> handles = new HashMap<>();
	private final Map<String, String> ids = new HashMap<>();
	private final Set<String> usedIds = new HashSet<>();
	private final Map<String, FLEFRecord> individuals = new HashMap<>();
	private final Map<String, List<Owner>> owners = new LinkedHashMap<>();
	private final Set<String> familyEvents = new HashSet<>();
	/** The media of every source, that its citations refer to. */
	private final Map<String, Set<String>> sourceMedia = new HashMap<>();
	/** The pseudo-sources that cite a media, by media. */
	private final Map<String, FLEFRecord> mediaSources = new HashMap<>();
	private final Map<String, String> tagNames = new HashMap<>();
	/** The identifiers of the places by name and type, to find the ones that are already there. */
	private final Map<String, String> placeIds = new HashMap<>();
	/** The keys of the places that more than one place has: an address does not belong to any of them. */
	private final Set<String> ambiguousPlaceKeys = new HashSet<>();
	/** The individuals that have a birth with a date. */
	private final Set<String> datedBirths = new HashSet<>();
	/** The size of the images by media. */
	private final Map<String, int[]> imageSizes = new HashMap<>();
	private String mediaPath;
	private final String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS));


	private final ImportReport report;
	/** Where the media of a package were extracted, {@code null} if there is no such place. */
	private final Path mediaDirectory;


	private GrampsXMLToFLEFConverter(final Document document, final ImportReport report, final Path mediaDirectory){
		root = document.getDocumentElement();
		this.report = report;
		this.mediaDirectory = mediaDirectory;
	}


	/**
	 * @param xml	The Gramps XML document.
	 * @param listener	Told of the progress of the conversion.
	 * @return	The model.
	 * @throws IOException	If the document is not a valid XML, or is not a Gramps one.
	 */
	public static FLEFModel convert(final byte[] xml, final GEDCOMToFLEFConverter.ProgressListener listener) throws IOException{
		return convert(xml, listener, new ImportReport());
	}

	/**
	 * @param xml	The Gramps XML document.
	 * @param listener	Told of the progress of the conversion.
	 * @param report	Where the important information that is not kept is collected.
	 * @return	The model.
	 * @throws IOException	If the document is not a valid XML, or is not a Gramps one.
	 */
	public static FLEFModel convert(final byte[] xml, final GEDCOMToFLEFConverter.ProgressListener listener, final ImportReport report)
			throws IOException{
		return convert(xml, listener, report, null);
	}

	/**
	 * @param xml	The Gramps XML document.
	 * @param listener	Told of the progress of the conversion.
	 * @param report	Where the important information that is not kept is collected.
	 * @param mediaDirectory	Where the media files with a relative path are (the one the package was extracted into), {@code null}
	 * 	to use the media path of the document.
	 * @return	The model.
	 * @throws IOException	If the document is not a valid XML, or is not a Gramps one.
	 */
	public static synchronized FLEFModel convert(final byte[] xml, final GEDCOMToFLEFConverter.ProgressListener listener,
			final ImportReport report, final Path mediaDirectory) throws IOException{
		final Document document;
		try{
			document = GrampsXML.parse(xml);
		}
		catch(final SAXException | ParserConfigurationException e){
			throw new IOException("Not a valid XML document: " + e.getMessage(), e);
		}
		if(!"database".equals(GrampsXML.name(document.getDocumentElement())))
			throw new IOException("Not a Gramps XML document");

		Deduplicator.reset();
		IDGenerator.reset();
		return new GrampsXMLToFLEFConverter(document, report, mediaDirectory).build(listener);
	}


	private FLEFModel build(final GEDCOMToFLEFConverter.ProgressListener listener){
		listener.onProgress(0, t("progress.indexing"));
		index();
		allocateIds();
		collectOwners();
		collectDatedBirths();

		listener.onProgress(10, t("progress.header"));
		header();
		listener.onProgress(20, t("progress.places"));
		for(final Element place : items(GRAMPS_PLACES, GRAMPS_PLACEOBJ))
			place(place);
		for(final Element place : items(GRAMPS_PLACES, GRAMPS_PLACEOBJ))
			placeRelationships(place);
		listener.onProgress(30, t("progress.repositories_media"));
		for(final Element repository : items("repositories", GRAMPS_REPOSITORY))
			repository(repository);
		for(final Element object : items("objects", GRAMPS_OBJECT))
			media(object);
		listener.onProgress(40, t("progress.individuals"));
		for(final Element person : items(GRAMPS_PEOPLE, GRAMPS_PERSON))
			person(person);
		listener.onProgress(60, t("progress.families"));
		for(final Element family : items(GRAMPS_FAMILIES, GRAMPS_FAMILY))
			family(family);
		listener.onProgress(75, t("progress.events"));
		for(final Element event : items("events", GRAMPS_EVENT))
			event(event);
		listener.onProgress(90, t("progress.sources"));
		for(final Element source : items("sources", GRAMPS_SOURCE))
			source(source);
		orphanNotes();
		listener.onProgress(100, t("progress.done"));
		return model;
	}


	/* ======================================================================
	 *                          Preparation
	 * ====================================================================== */

	private static String t(final String key){
		return I18N.t("import.gramps." + key);
	}

	private static String tf(final String key, final Object... arguments){
		return I18N.tf("import.gramps." + key, arguments);
	}

	/** Tells the notes that no record refers to: FLEF keeps a note only inside the record it is about, so they are not kept. */
	private void orphanNotes(){
		final Set<String> referenced = new HashSet<>();
		final NodeList references = root.getElementsByTagNameNS("*", GRAMPS_NOTEREF);
		for(int i = 0; i < references.getLength(); i ++)
			referenced.add(GrampsXML.attribute((Element)references.item(i), GRAMPS_HLINK));
		for(final Element note : items("notes", GRAMPS_NOTE))
			if(!referenced.contains(GrampsXML.attribute(note, GRAMPS_HANDLE)))
				discard(ImportReasons.NOTE_DETAIL, tf("discard.note_unattached", GrampsXML.attribute(note, "id"),
					StringUtils.abbreviate(StringUtils.defaultString(GrampsXML.childText(note, GRAMPS_TEXT)), 80)), note);
	}

	private List<Element> items(final String collection, final String item){
		return GrampsXML.children(GrampsXML.child(root, collection), item);
	}

	private void index(){
		for(final Element collection : GrampsXML.children(root))
			for(final Element item : GrampsXML.children(collection)){
				final String handle = GrampsXML.attribute(item, GRAMPS_HANDLE);
				if(handle != null)
					handles.put(handle, item);
			}
		mediaPath = GrampsXML.childText(GrampsXML.child(root, "header"), "mediapath");
		for(final Element tag : items("tags", "tag"))
			tagNames.put(GrampsXML.attribute(tag, GRAMPS_HANDLE), GrampsXML.attribute(tag, GRAMPS_NAME));
	}

	/** The element a reference points to, if it is of the kind. */
	private Element target(final Element reference, final String kind){
		final String handle = (reference != null? GrampsXML.attribute(reference, GRAMPS_HLINK): null);
		final Element element = (handle != null? handles.get(handle): null);
		return (element != null && kind.equals(GrampsXML.name(element))? element: null);
	}

	private String id(final Element element){
		return ids.get(GrampsXML.attribute(element, GRAMPS_HANDLE));
	}

	/** Tells that something of the element is not kept; where it is told is the record that contains the element. */
	private void discard(final String reason, final String item, final Element at){
		report.discard(reason, item, where(at));
	}

	/** @return	The identifier of the record that contains the element (or that is it), {@code null} if there is none. */
	private String where(final Element element){
		Node node = element;
		while(node instanceof Element current){
			if(GrampsXML.attribute(current, GRAMPS_HANDLE) != null)
				return id(current);

			node = node.getParentNode();
		}
		return null;
	}

	/** Tells that the new-year rule of a date is not kept (the default ones say nothing). */
	private void newYear(final Element date, final Element about){
		if(date == null)
			return;

		final String quality = GrampsDates.ignoredQuality(date);
		if(quality != null)
			discard(ImportReasons.DATE_DETAIL, "quality=" + quality, about);
		final String newYear = GrampsXML.attribute(date, GRAMPS_NEWYEAR);
		if(newYear != null && !"0".equals(newYear) && !"Jan1".equalsIgnoreCase(newYear) && !GrampsDates.newYearKept(date))
			discard(ImportReasons.DATE_DETAIL, GRAMPS_NEWYEAR + "=" + newYear, about);
	}

	/** Tells that an attribute that has only the type or only the value, so that it cannot be written, is not kept. */
	private void incompleteAttribute(final Element attribute, final Element about){
		final String type = GrampsXML.attribute(attribute, GRAMPS_TYPE);
		final String value = GrampsXML.attribute(attribute, GRAMPS_VALUE);
		if(type != null || value != null)
			discard(ImportReasons.OTHER, GRAMPS_ATTRIBUTE + StringUtils.SPACE + Objects.toString(type, "?") + "=" + Objects.toString(value, "?"), about);
	}

	/** Tells that the data entries of a source or a citation are not kept. */
	private void dataEntries(final Element holder){
		for(final String name : List.of("datamap", "data_item"))
			for(final Element entry : GrampsXML.children(holder, name)){
				final String key = GrampsXML.attribute(entry, "key");
				final String value = GrampsXML.attribute(entry, GRAMPS_VALUE);
				if(key != null || value != null)
					discard(ImportReasons.SOURCE_DETAIL, name + StringUtils.SPACE + Objects.toString(key, "?") + "=" + Objects.toString(value, "?"), holder);
				for(final Element item : GrampsXML.children(entry))
					discard(ImportReasons.SOURCE_DETAIL, name + StringUtils.SPACE + Objects.toString(GrampsXML.attribute(item, "key"), "?") + "="
						+ Objects.toString(GrampsXML.attribute(item, GRAMPS_VALUE), "?"), holder);
			}
	}

	/** Identifiers of the records: the ones of Gramps if they can be kept, otherwise new ones. */
	private void allocateIds(){
		final Map<Element, String> prefixes = new LinkedHashMap<>();
		for(final Element person : items(GRAMPS_PEOPLE, GRAMPS_PERSON))
			prefixes.put(person, IndividualReader.ID_PREFIX);
		for(final Element family : items(GRAMPS_FAMILIES, GRAMPS_FAMILY))
			prefixes.put(family, GroupReader.ID_PREFIX);
		for(final Element event : items("events", GRAMPS_EVENT))
			prefixes.put(event, EventReader.ID_PREFIX);
		for(final Element place : items(GRAMPS_PLACES, GRAMPS_PLACEOBJ))
			prefixes.put(place, PlaceReader.ID_PREFIX);
		for(final Element source : items("sources", GRAMPS_SOURCE))
			prefixes.put(source, SourceReader.ID_PREFIX);
		for(final Element repository : items("repositories", GRAMPS_REPOSITORY))
			prefixes.put(repository, RepositoryReader.ID_PREFIX);
		for(final Element object : items("objects", GRAMPS_OBJECT))
			prefixes.put(object, MediaReader.ID_PREFIX);

		for(final Map.Entry<Element, String> entry : prefixes.entrySet()){
			String id = GrampsXML.attribute(entry.getKey(), "id");
			//the groups are the records that begin with G (the families of Gramps begin with F)
			if(id != null && entry.getValue().equals(GroupReader.ID_PREFIX) && FAMILY_ID.matcher(id).matches())
				id = GroupReader.ID_PREFIX + id.substring(1);
			if(id != null && ID_FORMAT.matcher(id).matches() && usedIds.add(id.toLowerCase(Locale.ROOT))){
				ids.put(GrampsXML.attribute(entry.getKey(), GRAMPS_HANDLE), id);
				IDGenerator.registerExistingId(id);
			}
		}
		for(final Map.Entry<Element, String> entry : prefixes.entrySet())
			if(!ids.containsKey(GrampsXML.attribute(entry.getKey(), GRAMPS_HANDLE)))
				ids.put(GrampsXML.attribute(entry.getKey(), GRAMPS_HANDLE), nextId(entry.getValue()));
	}

	private String nextId(final String prefix){
		String id;
		do{
			id = IDGenerator.nextId(prefix);
		}while(!usedIds.add(id.toLowerCase(Locale.ROOT)));
		return id;
	}

	/** Who has each event. */
	private void collectOwners(){
		for(final Element person : items(GRAMPS_PEOPLE, GRAMPS_PERSON))
			for(final Element reference : GrampsXML.children(person, GRAMPS_EVENTREF)){
				final String event = GrampsXML.attribute(reference, GRAMPS_HLINK);
				if(event != null)
					owners.computeIfAbsent(event, k -> new ArrayList<>())
						.add(new Owner(false, GrampsXML.attribute(person, GRAMPS_HANDLE), reference));
			}
		for(final Element family : items(GRAMPS_FAMILIES, GRAMPS_FAMILY))
			for(final Element reference : GrampsXML.children(family, GRAMPS_EVENTREF)){
				final String event = GrampsXML.attribute(reference, GRAMPS_HLINK);
				if(event != null){
					owners.computeIfAbsent(event, k -> new ArrayList<>())
						.add(new Owner(true, GrampsXML.attribute(family, GRAMPS_HANDLE), reference));
					familyEvents.add(event);
				}
			}
	}


	/** The individuals that have a birth whose date can be written: the age at an event does not give them another one. */
	private void collectDatedBirths(){
		for(final Element person : items(GRAMPS_PEOPLE, GRAMPS_PERSON))
			for(final Element reference : GrampsXML.children(person, GRAMPS_EVENTREF)){
				final Element event = target(reference, GRAMPS_EVENT);
				if(event != null && isBirth(event) && ROLE_PRINCIPAL.equals(roleOf(GrampsXML.attribute(reference, "role")))
						&& GrampsDates.structure(EventReader.TAG_DATE, dateOf(event)) != null)
					datedBirths.add(id(person));
			}
	}

	private static boolean isBirth(final Element event){
		return GRAMPS_EVENT_BIRTH.equalsIgnoreCase(GrampsXML.childText(event, GRAMPS_TYPE));
	}


	/* ======================================================================
	 *                          Common structures
	 * ====================================================================== */

	private FLEFRecord record(final Element element, final String type){
		return FLEFRecord.createMainRecord(id(element), type);
	}

	/** @return	The field with the value, {@code null} (that is not added) if there is no value. */
	private static FLEFRecord child(final String tag, final String value){
		return (StringUtils.isNotBlank(value)? FLEFRecord.createChildWithTagAndValue(tag, value): null);
	}

	/** The date and time of the last change of a record of Gramps, in the format of FLEF. */
	private String isoOf(final Element element){
		final String change = GrampsXML.attribute(element, "change");
		if(change != null)
			try{
				return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(Long.parseLong(change)));
			}
			catch(final NumberFormatException ignored){
				//no usable date
			}
		return now;
	}

	/**
	 * The creation is the import (that is when the record was made here), the date of the last change of Gramps is the update.
	 *
	 * @param iso	The date of the last change, {@code now} if Gramps says none.
	 */
	private FLEFRecord audit(final String iso){
		final FLEFRecord audit = FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT)
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
				.addChild(child(AuditReader.TAG_DATE, now)));
		if(!now.equals(iso))
			audit.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_UPDATE)
				.addChild(child(AuditReader.TAG_DATE, iso)));
		return audit;
	}

	private static boolean isPrivate(final Element element){
		return "1".equals(GrampsXML.attribute(element, "priv"));
	}

	private static FLEFRecord restricted(){
		return FLEFRecord.createChildWithTag(PrivacyReader.TAG_PRIVACY)
			.addChild(child(PrivacyReader.TAG_LEVEL, PRIVACY_RESTRICTED));
	}

	private void privacy(final FLEFRecord parent, final Element element){
		if(isPrivate(element))
			parent.addChild(restricted());
	}

	private FLEFRecord noteStructure(final String title, final String text, final String mime, final String iso){
		return noteStructure(title, text, mime, iso, false);
	}

	private FLEFRecord noteStructure(final String title, final String text, final String mime, final String iso, final boolean isRestricted){
		if(StringUtils.isBlank(text))
			return null;

		return FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
			.addChild(StringUtils.isNotBlank(title)? child(NoteReader.TAG_TITLE, title): null)
			.addChild(child(NoteReader.TAG_TEXT, text.trim()))
			.addChild(StringUtils.isNotBlank(mime)? child(NoteReader.TAG_MIME, mime): null)
			.addChild(isRestricted? restricted(): null)
			.addChild(audit(iso));
	}

	/** A note about something that FLEF has no field for. */
	private FLEFRecord note(final String title, final String text, final Element about){
		return noteStructure(title, text, null, isoOf(about));
	}

	/** The notes that a record refers to, and its tags. */
	private void notes(final FLEFRecord parent, final Element holder){
		for(final Element reference : GrampsXML.children(holder, GRAMPS_NOTEREF)){
			final Element note = target(reference, GRAMPS_NOTE);
			if(note != null)
				parent.addChild(noteOf(note));
		}
		final List<String> tags = new ArrayList<>();
		for(final Element reference : GrampsXML.children(holder, "tagref")){
			final String name = tagNames.get(GrampsXML.attribute(reference, GRAMPS_HLINK));
			if(name != null)
				tags.add(name);
		}
		if(!tags.isEmpty())
			parent.addChild(note(t("title.tags"), String.join(", ", tags), holder));
	}

	/** A note of Gramps; its formatting (bold, italic, links) is kept as Markdown. */
	private FLEFRecord noteOf(final Element note){
		//the styles are positions in the text as it is written: it is not trimmed before they are applied
		final String text = GrampsXML.rawText(GrampsXML.child(note, GRAMPS_TEXT));
		final String markdown = ("1".equals(GrampsXML.attribute(note, "format"))? null: markdown(text, note));
		final String type = GrampsXML.attribute(note, GRAMPS_TYPE);
		//the types that only say what the note is about are of no use
		final String title = (type != null && !type.endsWith(" Note") && !"General".equalsIgnoreCase(type)
			&& !"Unknown".equalsIgnoreCase(type)? type: null);
		return (markdown != null
			? noteStructure(title, markdown, MIME_MARKDOWN, isoOf(note), isPrivate(note))
			: noteStructure(title, text, null, isoOf(note), isPrivate(note)));
	}

	/** @return	The text with the styles of the note written as Markdown, or {@code null} if it has none. */
	private static String markdown(final String text, final Element note){
		if(text == null)
			return null;

		final int length = text.codePointCount(0, text.length());
		final List<int[]> insertions = new ArrayList<>();
		final List<String> marks = new ArrayList<>();
		for(final Element style : GrampsXML.children(note, "style")){
			final String name = GrampsXML.attribute(style, GRAMPS_NAME);
			final String value = GrampsXML.attribute(style, GRAMPS_VALUE);
			final String open;
			final String close;
			if("bold".equals(name)){
				open = "**";
				close = "**";
			}
			else if("italic".equals(name)){
				open = "*";
				close = "*";
			}
			else if("link".equals(name) && value != null){
				open = "[";
				close = "](" + Strings.CS.replace(Strings.CS.replace(value, StringUtils.SPACE, "%20"), ")", "%29") + ")";
			}
			else
				continue;

			for(final Element range : GrampsXML.children(style, "range"))
				try{
					//Gramps counts the characters of the text, not the units of UTF-16
					final int start = Integer.parseInt(GrampsXML.attribute(range, "start"));
					final int end = Integer.parseInt(GrampsXML.attribute(range, "end"));
					if(start >= 0 && end <= length && start < end){
						int from = text.offsetByCodePoints(0, start);
						int to = text.offsetByCodePoints(0, end);
						//a mark is not valid next to a blank
						while(from < to && Character.isWhitespace(text.charAt(from)))
							from ++;
						while(to > from && Character.isWhitespace(text.charAt(to - 1)))
							to --;
						if(from < to){
							marks.add(open);
							insertions.add(new int[]{from, 1, marks.size() - 1});
							marks.add(close);
							insertions.add(new int[]{to, 0, marks.size() - 1});
						}
					}
				}
				catch(final NumberFormatException ignored){
					//not a range
				}
		}
		if(insertions.isEmpty())
			return null;

		//at the same position the marks that close come before the ones that open
		insertions.sort((a, b) -> (a[0] != b[0]? Integer.compare(a[0], b[0]): Integer.compare(a[1], b[1])));
		final StringBuilder sb = new StringBuilder();
		int position = 0;
		for(final int[] insertion : insertions){
			sb.append(text, position, insertion[0]).append(marks.get(insertion[2]));
			position = insertion[0];
		}
		return sb.append(text, position, text.length()).toString();
	}

	/**
	 * Writes a date as a structure; a date that cannot be (a text, or a calendar that FLEF cannot write here) is kept as a note
	 * of the parent.
	 */
	private void date(final FLEFRecord parent, final String tag, final Element date, final Element about){
		if(date == null)
			return;

		newYear(date, about);
		final FLEFRecord structure = GrampsDates.structure(tag, date);
		if(structure != null)
			parent.addChild(structure);
		else
			parent.addChild(note(t("title.date"), GrampsDates.text(date), about));
	}

	/** Writes the validity of a fact observed at a date. */
	private void validity(final FLEFRecord parent, final Element date, final Element about){
		if(date == null)
			return;

		newYear(date, about);
		final FLEFRecord[] validity = GrampsDates.validity(date);
		if(validity != null){
			parent.addChild(validity[0]);
			parent.addChild(validity[1]);
		}
		else
			parent.addChild(note(t("title.date"), GrampsDates.text(date), about));
	}

	private static Element dateOf(final Element parent){
		for(final String name : List.of("dateval", "daterange", "datespan", "datestr")){
			final Element date = GrampsXML.child(parent, name);
			if(date != null)
				return date;
		}
		return null;
	}

	/** The place of the reference, as a place citation. */
	private FLEFRecord placeCitation(final Element reference){
		final Element place = target(reference, GRAMPS_PLACEOBJ);
		return (place != null
			? FLEFRecord.createChildWithTag(PlaceCitationReader.TAG_PLACE).addChild(child(PlaceCitationReader.TAG_PLACE, id(place)))
			: null);
	}


	/* ======================================================================
	 *                          Citations and media
	 * ====================================================================== */

	/**
	 * Writes the citations that the holder has: the source, the page, the date, the media, and the confidence of each one.
	 * <p>
	 * What the citation says besides that (its notes, its attributes) goes into the remark of its first extract, or into a note of
	 * the parent when there is no extract.
	 */
	private void citations(final FLEFRecord parent, final Element holder){
		citations(parent, holder, false);
	}

	/** @param indirect	Whether the citations are indirect evidence (what they say is not what is asserted, but implies it). */
	private void citations(final FLEFRecord parent, final Element holder, final boolean indirect){
		for(final Element reference : GrampsXML.children(holder, GRAMPS_CITATIONREF)){
			final Element citation = target(reference, "citation");
			final Element source = (citation != null? target(GrampsXML.child(citation, "sourceref"), GRAMPS_SOURCE): null);
			if(source == null){
				if(citation != null)
					discard(ImportReasons.SOURCE_DETAIL, tf("discard.citation", Objects.toString(GrampsXML.childText(citation, "page"), t("discard.without_source"))), holder);
				continue;
			}

			dataEntries(citation);
			final String sourceId = id(source);
			//the date of the citation is not where it is located: it goes with the remarks
			final List<String> remarks = new ArrayList<>();
			final List<String> locator = new ArrayList<>();
			final String page = GrampsXML.childText(citation, "page");
			if(page != null)
				locator.add(page);
			newYear(dateOf(citation), citation);
			final String date = GrampsDates.text(dateOf(citation));
			if(date != null)
				remarks.add(tf("text.date", date));

			final List<FLEFRecord> extracts = new ArrayList<>();
			for(final Element objectReference : GrampsXML.children(citation, GRAMPS_OBJREF)){
				final Element object = target(objectReference, GRAMPS_OBJECT);
				if(object == null)
					continue;

				sourceMedia.computeIfAbsent(GrampsXML.attribute(source, GRAMPS_HANDLE), k -> new LinkedHashSet<>())
					.add(id(object));
				extracts.add(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EXTRACT)
					.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_MEDIA_PART)
						.addChild(child(SourceCitationReader.TAG_MEDIA, id(object)))));
			}

			for(final Element noteReference : GrampsXML.children(citation, GRAMPS_NOTEREF)){
				final Element note = target(noteReference, GRAMPS_NOTE);
				final String text = (note != null? GrampsXML.childText(note, GRAMPS_TEXT): null);
				if(text != null)
					remarks.add(text);
			}
			for(final Element attribute : GrampsXML.children(citation, "srcattribute"))
				if(GrampsXML.attribute(attribute, GRAMPS_TYPE) != null || GrampsXML.attribute(attribute, GRAMPS_VALUE) != null)
					remarks.add(Objects.toString(GrampsXML.attribute(attribute, GRAMPS_TYPE), "?") + ": "
						+ Objects.toString(GrampsXML.attribute(attribute, GRAMPS_VALUE), "?"));
			if(!remarks.isEmpty())
				if(extracts.isEmpty())
					parent.addChild(note(tf("title.citation", sourceId), String.join(StringUtils.LF, remarks), citation));
				else
					extracts.getFirst()
						.addChild(child(SourceCitationReader.TAG_REMARK, String.join(StringUtils.LF, remarks)));

			final FLEFRecord structure = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
				.addChild(child(SourceCitationReader.TAG_SOURCE, sourceId));
			if(!locator.isEmpty())
				structure.addChild(child(SourceCitationReader.TAG_LOCATOR, String.join(", ", locator)));
			for(final FLEFRecord extract : extracts)
				structure.addChild(extract);
			structure.addChild(child(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS))
				.addChild(indirect? indirectEvidence(): evidence(GrampsXML.childText(citation, "confidence")));
			privacy(structure, citation);
			parent.addChild(structure);
		}
	}

	private static FLEFRecord indirectEvidence(){
		return FLEFRecord.createChildWithTag(EvidenceQualifiersReader.TAG_EVIDENCE)
			.addChild(child(EvidenceQualifiersReader.TAG_INFORMATION_TYPE, EvidenceQualifiersReader.INFORMATION_TYPE_UNDETERMINED))
			.addChild(child(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_INDIRECT));
	}

	/** Gramps has five levels of confidence: from very low (0) to very high (4). */
	private static FLEFRecord evidence(final String confidence){
		if(confidence == null)
			return null;

		final FLEFRecord evidence = FLEFRecord.createChildWithTag(EvidenceQualifiersReader.TAG_EVIDENCE);
		switch(confidence){
			case "3", "4" -> evidence.addChild(child(EvidenceQualifiersReader.TAG_INFORMATION_TYPE, EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY))
				.addChild(child(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT));
			case "2" -> evidence.addChild(child(EvidenceQualifiersReader.TAG_INFORMATION_TYPE, EvidenceQualifiersReader.INFORMATION_TYPE_SECONDARY));
			default -> evidence.addChild(child(EvidenceQualifiersReader.TAG_INFORMATION_TYPE, EvidenceQualifiersReader.INFORMATION_TYPE_UNDETERMINED));
		}
		return evidence;
	}

	/**
	 * Writes the media of a record: the first one is its preferred image (for an individual and a family), and each one is cited
	 * as a source made of the media.
	 */
	private void media(final FLEFRecord parent, final Element holder){
		boolean first = true;
		for(final Element reference : GrampsXML.children(holder, GRAMPS_OBJREF)){
			final Element object = target(reference, GRAMPS_OBJECT);
			if(object == null)
				continue;

			final String mediaId = id(object);
			final boolean cropped = (cropOrNull(CropReader.TAG_CROP, object, reference) != null);
			if(first && (IndividualReader.isOfType(parent) || GroupReader.isOfType(parent)))
				parent.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_PREFERRED_IMAGE)
					.addChild(child(CropReader.TAG_MEDIA, mediaId))
					.addChild(cropOrNull(CropReader.TAG_CROP, object, reference)));
			first = false;

			final FLEFRecord source = mediaSources.computeIfAbsent(mediaId, k -> {
				final String description = GrampsXML.attribute(GrampsXML.child(object, "file"), GRAMPS_DESCRIPTION);
				final FLEFRecord record = FLEFRecord.createMainRecord(nextId(SourceReader.ID_PREFIX), SourceReader.TYPE)
					.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
						.addChild(child(NameReader.TAG_VALUE, description != null? description: tf("name.media", mediaId))))
					.addChild(child(SourceReader.TAG_MEDIA, mediaId))
					.addChild(audit(isoOf(object)));
				model.addRecord(record);
				return record;
			});
			//what the reference says besides the region (its notes, its attributes) is a remark of the extract
			final FLEFRecord extract = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_EXTRACT)
				.addChild(FLEFRecord.createChildWithTag(SourceCitationReader.TAG_MEDIA_PART)
					.addChild(child(SourceCitationReader.TAG_MEDIA, mediaId))
					.addChild(cropOrNull(SourceCitationReader.TAG_CROP, object, reference)));
			for(final String remark : referenceRemarks(reference))
				extract.addChild(child(SourceCitationReader.TAG_REMARK, remark));
			final FLEFRecord structure = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
				.addChild(child(SourceCitationReader.TAG_SOURCE, source.getId()))
				.addChild(extract)
				.addChild(child(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS));
			privacy(structure, reference);
			parent.addChild(structure);
			//the citations of the reference are evidence of the record that has the media
			citations(parent, reference);

			if(!cropped){
				final String region = regionOf(reference);
				if(region != null){
					parent.addChild(note(tf("title.region", mediaId), region, holder));
					discard(ImportReasons.MEDIA_DETAIL, tf("discard.region", mediaId, region), object);
				}
			}
		}
	}

	/** The notes and the attributes of a reference to a media, as text. */
	private List<String> referenceRemarks(final Element reference){
		final List<String> remarks = new ArrayList<>();
		for(final Element noteReference : GrampsXML.children(reference, GRAMPS_NOTEREF)){
			final Element note = target(noteReference, GRAMPS_NOTE);
			final String text = (note != null? GrampsXML.childText(note, GRAMPS_TEXT): null);
			if(text != null)
				remarks.add(text);
		}
		for(final Element attribute : GrampsXML.children(reference, GRAMPS_ATTRIBUTE)){
			final String type = GrampsXML.attribute(attribute, GRAMPS_TYPE);
			final String value = GrampsXML.attribute(attribute, GRAMPS_VALUE);
			if(type != null && value != null)
				remarks.add(type + ": " + value);
		}
		return remarks;
	}

	/**
	 * The region of an image that a reference selects, as a crop in pixels (Gramps keeps it in percent of the image).
	 * <p>
	 * IMPORT DISCARD POINT: it needs the size of the image, that is read from the file (found through the media path of the Gramps
	 * file); when the file cannot be found or read (or the region is not a valid one) there is no crop, and the caller keeps the
	 * region, in percent, as a note.
	 *
	 * @param tag	The tag of the crop.
	 * @return	The crop, {@code null} if there is no region, or it cannot be converted.
	 */
	private FLEFRecord cropOrNull(final String tag, final Element object, final Element reference){
		final Element region = GrampsXML.child(reference, "region");
		if(region == null)
			return null;

		final int[] size = imageSize(object);
		if(size == null)
			return null;

		final double left;
		final double top;
		final double right;
		final double bottom;
		try{
			left = Double.parseDouble(GrampsXML.attribute(region, "corner1_x"));
			top = Double.parseDouble(GrampsXML.attribute(region, "corner1_y"));
			right = Double.parseDouble(GrampsXML.attribute(region, "corner2_x"));
			bottom = Double.parseDouble(GrampsXML.attribute(region, "corner2_y"));
		}
		catch(final NumberFormatException | NullPointerException ignored){
			return null;
		}

		final int[] horizontal = span(left, right, size[0]);
		final int[] vertical = span(top, bottom, size[1]);
		return FLEFRecord.createChildWithTag(tag)
			.addChild(child(CropReader.TAG_X, Integer.toString(horizontal[0])))
			.addChild(child(CropReader.TAG_Y, Integer.toString(vertical[0])))
			.addChild(child(CropReader.TAG_WIDTH, Integer.toString(horizontal[1])))
			.addChild(child(CropReader.TAG_HEIGHT, Integer.toString(vertical[1])));
	}

	/** @return	The start and the length in pixels of a span between two percentages of a size, within the size (length at least 1). */
	private static int[] span(final double first, final double second, final int size){
		final int start = (int)Math.clamp(size - 1, 0, Math.round(Math.min(first, second) * size / 100.));
		final int end = (int)Math.clamp(size, start + 1, Math.round(Math.max(first, second) * size / 100.));
		return new int[]{start, end - start};
	}

	/** @return	The width and the height of the image of the media, {@code null} if it cannot be read. */
	private int[] imageSize(final Element object){
		final int[] size = imageSizes.computeIfAbsent(id(object),
			k -> readImageSize(GrampsXML.attribute(GrampsXML.child(object, "file"), "src")));
		return (size.length == 2? size: null);
	}

	/** Reads the size of an image from its header, without decoding it. */
	private int[] readImageSize(final String source){
		if(source == null)
			return NO_SIZE;

		try{
			final Path file = Path.of(location(source));
			if(!Files.isRegularFile(file))
				return NO_SIZE;

			try(final ImageInputStream in = ImageIO.createImageInputStream(file.toFile())){
				final Iterator<ImageReader> readers = (in != null? ImageIO.getImageReaders(in): null);
				if(readers == null || !readers.hasNext())
					return NO_SIZE;

				final ImageReader reader = readers.next();
				try{
					reader.setInput(in);
					return new int[]{reader.getWidth(0), reader.getHeight(0)};
				}
				finally{
					reader.dispose();
				}
			}
		}
		catch(final IOException | RuntimeException ignored){
			return NO_SIZE;
		}
	}

	/** The region of an image that a reference selects, in percent (FLEF wants pixels, whose size Gramps does not keep). */
	private static String regionOf(final Element reference){
		final Element region = GrampsXML.child(reference, "region");
		if(region == null)
			return null;

		return tf("text.region", GrampsXML.attribute(region, "corner1_x"), GrampsXML.attribute(region, "corner1_y"),
			GrampsXML.attribute(region, "corner2_x"), GrampsXML.attribute(region, "corner2_y"));
	}


	/* ======================================================================
	 *                          Header
	 * ====================================================================== */

	private void header(){
		final Element headerElement = GrampsXML.child(root, "header");
		final Element created = GrampsXML.child(headerElement, "created");

		final FLEFRecord header = FLEFRecord.createChildWithTag(TAG_HEADER)
			.addChild(FLEFRecord.createChildWithTag(HeaderReader.TAG_PROTOCOL)
				.addChild(child(HeaderReader.TAG_NAME, PROTOCOL_NAME))
				.addChild(child(HeaderReader.TAG_VERSION, PROTOCOL_VERSION)))
			//the software that writes this file is this one, whatever wrote the Gramps file
			.addChild(FLEFRecord.createChildWithTag(HeaderReader.TAG_SOURCE)
				.addChild(child(HeaderReader.TAG_NAME, SoftwareInfo.NAME))
				.addChild(child(HeaderReader.TAG_VERSION, SoftwareInfo.version()))
				.addChild(child(HeaderReader.TAG_SOURCE_ORGANIZATION, SoftwareInfo.ORGANIZATION)));

		final String date = GrampsXML.attribute(created, "date");
		header.addChild(child(HeaderReader.TAG_DATE, (date != null && date.length() == 10? date + "T00:00:00Z": now)));

		final Element researcher = GrampsXML.child(headerElement, "researcher");
		if(researcher != null){
			final FLEFRecord submitter = FLEFRecord.createChildWithTag(HeaderReader.TAG_SUBMITTER);
			final String name = GrampsXML.childText(researcher, "resname");
			final String address = addressText(GrampsXML.childText(researcher, "resaddr"),
				GrampsXML.childText(researcher, "reslocality"), GrampsXML.childText(researcher, "rescity"), null,
				GrampsXML.childText(researcher, "resstate"), GrampsXML.childText(researcher, "rescountry"),
				GrampsXML.childText(researcher, "respostal"));
			submitter.addChild(contact(address, ContactReader.ENUM_TYPE_POSTAL, name, null))
				.addChild(contact(GrampsXML.childText(researcher, "resphone"), ContactReader.ENUM_TYPE_PHONE, name, null))
				.addChild(contact(GrampsXML.childText(researcher, "resemail"), ContactReader.ENUM_TYPE_EMAIL, name, null));
			if(name != null)
				submitter.addChild(child(ContactReader.TAG_REMARK, StringUtils.strip(name, ", ")));
			header.addChild(submitter);
		}
		model.setHeader(header);
	}

	private FLEFRecord contact(final String value, final String type, final String name, final String remark){
		if(value == null)
			return null;

		return FLEFRecord.createChildWithTag(RepositoryReader.TAG_CONTACT)
			.addChild(child(ContactReader.TAG_VALUE, value))
			.addChild(child(ContactReader.TAG_TYPE, type))
			.addChild(name != null? FLEFRecord.createChildWithTag(ContactReader.TAG_NAME).addChild(child(ContactReader.TAG_VALUE, StringUtils.strip(name, ", "))): null)
			.addChild(remark != null? child(ContactReader.TAG_REMARK, remark): null)
			.addChild(audit(now));
	}

	/** @return	The lines of an address element, one over the other ({@code null} if it has none). */
	private static String addressText(final Element address){
		return addressText(GrampsXML.childText(address, GRAMPS_STREET), GrampsXML.childText(address, GRAMPS_LOCALITY),
			GrampsXML.childText(address, GRAMPS_CITY), GrampsXML.childText(address, GRAMPS_COUNTY), GrampsXML.childText(address, GRAMPS_STATE),
			GrampsXML.childText(address, GRAMPS_COUNTRY), GrampsXML.childText(address, GRAMPS_POSTAL));
	}

	/** @return	The lines of an address, one over the other ({@code null} if it has none). */
	private static String addressText(final String street, final String locality, final String city, final String county,
			final String state, final String country, final String postal){
		final List<String> lines = new ArrayList<>();
		for(final String part : new String[]{street, locality, city, county})
			if(part != null)
				lines.add(part);
		final String region = StringUtils.trimToNull(StringUtils.defaultString(state) + StringUtils.SPACE + StringUtils.defaultString(postal));
		if(region != null)
			lines.add(region);
		if(country != null)
			lines.add(country);
		return (!lines.isEmpty()? String.join(StringUtils.LF, lines): null);
	}


	/* ======================================================================
	 *                          Places
	 * ====================================================================== */

	private void place(final Element element){
		final FLEFRecord place = record(element, PlaceReader.TYPE);

		for(final Element name : GrampsXML.children(element, "pname")){
			final String value = GrampsXML.attribute(name, GRAMPS_VALUE);
			if(value == null)
				continue;

			final FLEFRecord structure = FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(child(NameReader.TAG_VALUE, value))
				.addChild(child(NameReader.TAG_LOCALE, GrampsXML.attribute(name, "lang")));
			validity(structure, dateOf(name), element);
			place.addChild(structure);
		}
		if(FLEFRecordHelper.findChildren(place, PlaceReader.TAG_NAME).isEmpty()){
			final String title = GrampsXML.childText(element, "ptitle");
			place.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(child(NameReader.TAG_VALUE, title != null? title: tf("name.place", id(element)))));
		}

		final String type = GrampsXML.attribute(element, GRAMPS_TYPE);
		if(type != null && !TYPE_UNKNOWN.equalsIgnoreCase(type))
			place.addChild(child(PlaceReader.TAG_TYPE, GEDCOMMapper.toTypeToken(type)));
		for(final FLEFRecord name : FLEFRecordHelper.findChildren(place, PlaceReader.TAG_NAME))
			registerPlace(placeKey(FLEFRecordHelper.getChildValue(name, NameReader.TAG_VALUE),
				FLEFRecordHelper.getChildValue(place, PlaceReader.TAG_TYPE)), place.getId());

		final Element coordinates = GrampsXML.child(element, "coord");
		final String iso = PlaceCache.toISO6709(GrampsXML.attribute(coordinates, "lat"), GrampsXML.attribute(coordinates, "long"));
		if(iso != null)
			place.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_LOCATION)
				.addChild(child(PlaceReader.TAG_COORDINATES, iso)));
		else if(coordinates != null)
			discard(ImportReasons.PLACE_DETAIL, tf("discard.coordinates", GrampsXML.attribute(coordinates, "lat"), GrampsXML.attribute(coordinates, "long")), element);

		citations(place, element);
		final String code = GrampsXML.childText(element, "code");
		if(code != null)
			place.addChild(note(t("title.code"), code, element));
		for(final Element location : GrampsXML.children(element, "location"))
			place.addChild(note(t("title.location"), locationText(location), element));
		for(final Element url : GrampsXML.children(element, GRAMPS_URL))
			place.addChild(note(t("title.url"), urlText(url), element));
		notes(place, element);
		media(place, element);
		privacy(place, element);
		place.addChild(audit(isoOf(element)));
		model.addRecord(place);
	}

	private static String locationText(final Element location){
		final List<String> parts = new ArrayList<>();
		for(final String name : List.of(GRAMPS_STREET, GRAMPS_LOCALITY, GRAMPS_CITY, GRAMPS_PARISH, GRAMPS_COUNTY, GRAMPS_STATE, GRAMPS_COUNTRY, GRAMPS_POSTAL, GRAMPS_PHONE)){
			final String value = GrampsXML.attribute(location, name);
			if(value != null)
				parts.add(name + ": " + value);
		}
		return String.join(", ", parts);
	}

	private static String urlText(final Element url){
		final String type = GrampsXML.attribute(url, GRAMPS_TYPE);
		final String description = GrampsXML.attribute(url, GRAMPS_DESCRIPTION);
		return GrampsXML.attribute(url, "href") + (description != null? " (" + description + ")": StringUtils.EMPTY)
			+ (type != null? " [" + type + "]": StringUtils.EMPTY);
	}

	/** A place is part of the places that contain it, for the time that Gramps says. */
	private void placeRelationships(final Element element){
		for(final Element reference : GrampsXML.children(element, "placeref")){
			final Element container = target(reference, GRAMPS_PLACEOBJ);
			if(container == null)
				continue;

			//a parish, a diocese or an archdiocese is an ecclesiastical jurisdiction, the other places are administrative ones
			final FLEFRecord relationship = placeRelationship(id(element), id(container),
				(isEcclesiastical(element) || isEcclesiastical(container)
					? PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF
					: PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF));
			validity(relationship, dateOf(reference), element);
			relationship.addChild(audit(isoOf(element)));
			model.addRecord(relationship);
		}
	}

	private FLEFRecord placeRelationship(final String subjectId, final String targetId, final String type){
		return FLEFRecord.createMainRecord(nextId(PlaceRelationshipReader.ID_PREFIX), PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
				.addChild(child(PlaceRelationshipReader.TAG_PLACE, subjectId)))
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
				.addChild(child(PlaceRelationshipReader.TAG_PLACE, targetId)))
			.addChild(child(PlaceRelationshipReader.TAG_TYPE, type));
	}

	private static boolean isEcclesiastical(final Element place){
		final String type = GEDCOMMapper.toTypeToken(GrampsXML.attribute(place, GRAMPS_TYPE));
		return (PlaceReader.ENUM_TYPE_PARISH.equalsIgnoreCase(type) || PlaceReader.ENUM_TYPE_DIOCESE.equalsIgnoreCase(type)
			|| PlaceReader.ENUM_TYPE_ARCHDIOCESE.equalsIgnoreCase(type));
	}

	private void registerPlace(final String key, final String id){
		final String known = placeIds.putIfAbsent(key, id);
		if(known != null && !known.equals(id))
			ambiguousPlaceKeys.add(key);
	}

	private static String placeKey(final String name, final String type){
		return StringUtils.defaultString(name).toLowerCase(Locale.ROOT) + "|" + StringUtils.defaultString(type).toLowerCase(Locale.ROOT);
	}

	/**
	 * The place of an address: a place of type address, named by the address, that is part of the place of its locality, city,
	 * county, state and country (the ones that are already there are used, the others are made).
	 *
	 * @return	The identifier of the place, {@code null} if the address says nothing.
	 */
	private String addressPlace(final Element address){
		final String text = addressText(address);
		if(text == null)
			return null;

		final String name = Strings.CS.replace(text, StringUtils.LF, ", ");
		final String addressKey = placeKey(name, PlaceReader.ENUM_TYPE_ADDRESS);
		final String known = (!ambiguousPlaceKeys.contains(addressKey)? placeIds.get(addressKey): null);
		if(known != null)
			return known;

		final String addressId = newPlace(name, PlaceReader.ENUM_TYPE_ADDRESS);
		final String[][] containers = {
			{GrampsXML.childText(address, GRAMPS_LOCALITY), null},
			{GrampsXML.childText(address, GRAMPS_CITY), PlaceReader.ENUM_TYPE_CITY},
			{GrampsXML.childText(address, GRAMPS_COUNTY), PlaceReader.ENUM_TYPE_COUNTY},
			{GrampsXML.childText(address, GRAMPS_STATE), PlaceReader.ENUM_TYPE_REGION},
			{GrampsXML.childText(address, GRAMPS_COUNTRY), PlaceReader.ENUM_TYPE_COUNTRY}};
		String contained = addressId;
		for(final String[] container : containers){
			if(container[0] == null || container[0].equalsIgnoreCase(name))
				continue;

			final String containerKey = placeKey(container[0], container[1]);
			//a name that many places have says nothing of which one it is
			final String existing = (!ambiguousPlaceKeys.contains(containerKey)? placeIds.get(containerKey): null);
			final String containerId = (existing != null? existing: newPlace(container[0], container[1]));
			addPlaceRelationship(contained, containerId);
			//the container that was there has its own containers
			if(existing != null)
				break;

			contained = containerId;
		}
		return addressId;
	}

	private String newPlace(final String name, final String type){
		final FLEFRecord place = FLEFRecord.createMainRecord(nextId(PlaceReader.ID_PREFIX), PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(child(NameReader.TAG_VALUE, name)))
			.addChild(child(PlaceReader.TAG_TYPE, type))
			.addChild(audit(now));
		model.addRecord(place);
		registerPlace(placeKey(name, type), place.getId());
		return place.getId();
	}

	private void addPlaceRelationship(final String subjectId, final String targetId){
		model.addRecord(placeRelationship(subjectId, targetId, PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF)
			.addChild(audit(now)));
	}


	/* ======================================================================
	 *                          Repositories, media, sources
	 * ====================================================================== */

	private void repository(final Element element){
		final FLEFRecord repository = record(element, RepositoryReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RepositoryReader.TAG_NAME)
				.addChild(child(NameReader.TAG_VALUE, Objects.toString(GrampsXML.childText(element, "rname"),
					tf("name.repository", id(element))))));

		for(final Element address : GrampsXML.children(element, GRAMPS_ADDRESS)){
			repository.addChild(contact(addressText(address), ContactReader.ENUM_TYPE_POSTAL, null, null));
			repository.addChild(contact(GrampsXML.childText(address, GRAMPS_PHONE), ContactReader.ENUM_TYPE_PHONE, null, null));
			repository.addChild(addressNote(address, element));
		}
		for(final Element url : GrampsXML.children(element, GRAMPS_URL))
			repository.addChild(contact(GrampsXML.attribute(url, "href"), ContactReader.ENUM_TYPE_WEBSITE, null,
				Objects.toString(GrampsXML.attribute(url, GRAMPS_DESCRIPTION), GrampsXML.attribute(url, GRAMPS_TYPE))));

		final String type = repositoryType(GrampsXML.childText(element, GRAMPS_TYPE));
		if(type != null)
			repository.addChild(child(RepositoryReader.TAG_TYPE, type));
		notes(repository, element);
		privacy(repository, element);
		repository.addChild(audit(isoOf(element)));
		model.addRecord(repository);
	}

	/** @return the FLEF type of a Gramps repository type: an enum member when there is one, else a custom token; {@code null} for none */
	static String repositoryType(final String grampsType){
		if(StringUtils.isBlank(grampsType))
			return null;

		final String key = grampsType.strip()
			.toLowerCase(Locale.ROOT);
		if(TYPE_UNKNOWN.equals(key))
			return null;
		for(final String member : RepositoryReader.TYPES)
			if(member.equals(key))
				return member;
		if(GRAMPS_REPOSITORY_WEB_SITE.equals(key))
			return RepositoryReader.ENUM_TYPE_WEBSITE;
		//a safe holds the family papers of a private person
		if(GRAMPS_REPOSITORY_SAFE.equals(key))
			return RepositoryReader.ENUM_TYPE_PRIVATE_COLLECTION;
		return GEDCOMMapper.toTypeToken(grampsType);
	}

	/**
	 * What the address of a repository says besides the address: the period it was valid, the citations and the notes (as a note of
	 * the repository, whose sources are the citations).
	 */
	private FLEFRecord addressNote(final Element address, final Element repository){
		newYear(dateOf(address), repository);
		final String period = GrampsDates.text(dateOf(address));
		final List<String> texts = new ArrayList<>();
		for(final Element noteReference : GrampsXML.children(address, GRAMPS_NOTEREF)){
			final Element note = target(noteReference, GRAMPS_NOTE);
			final String text = (note != null? GrampsXML.childText(note, GRAMPS_TEXT): null);
			if(text != null)
				texts.add(text);
		}
		if(period == null && texts.isEmpty() && GrampsXML.children(address, GRAMPS_CITATIONREF).isEmpty())
			return null;

		final String text = Strings.CS.replace(Objects.toString(addressText(address), t("title.address")), StringUtils.LF, ", ")
			+ (period != null? " (" + period + ")": StringUtils.EMPTY);
		texts.addFirst(text);
		final FLEFRecord note = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
			.addChild(child(NoteReader.TAG_TITLE, t("title.address")))
			.addChild(child(NoteReader.TAG_TEXT, String.join(StringUtils.LF, texts)));
		citations(note, address);
		privacy(note, address);
		return note.addChild(audit(isoOf(repository)));
	}

	private void media(final Element element){
		final Element file = GrampsXML.child(element, "file");
		final String source = GrampsXML.attribute(file, "src");
		final FLEFRecord media = record(element, MediaReader.TYPE)
			.addChild(child(MediaReader.TAG_URI, (source != null? location(source): "unknown")))
			.addChild(child(MediaReader.TAG_KIND, GEDCOMMapper.mapMediaKind(null, GrampsXML.attribute(file, "mime"), source)))
			.addChild(child(MediaReader.TAG_DESCRIPTION, GrampsXML.attribute(file, GRAMPS_DESCRIPTION)));

		//the date the media was created or captured (a text, or a calendar that FLEF cannot write, is kept as a note)
		date(media, MediaReader.TAG_DATE, dateOf(element), element);
		final String checksum = GrampsXML.attribute(file, "checksum");
		if(checksum != null)
			media.addChild(note(t("title.checksum"), checksum, element));
		for(final Element attribute : GrampsXML.children(element, GRAMPS_ATTRIBUTE))
			media.addChild(note(GrampsXML.attribute(attribute, GRAMPS_TYPE), GrampsXML.attribute(attribute, GRAMPS_VALUE), element));
		notes(media, element);
		privacy(media, element);
		media.addChild(audit(isoOf(element)));
		model.addRecord(media);
	}

	/** The location of a media file: relative to the media path of the file, unless it is one that Gramps resolves by itself. */
	private String location(final String source){
		final boolean relative = !source.startsWith("/") && !source.contains(":") && !source.startsWith("\\");
		if(relative && mediaDirectory != null)
			try{
				return mediaDirectory.toAbsolutePath()
					.resolve(source)
					.normalize()
					.toString();
			}
			catch(final InvalidPathException ignored){
				return source;
			}
		if(!relative || mediaPath == null || mediaPath.contains("{"))
			return source;

		return Strings.CS.removeEnd(mediaPath, "/") + "/" + source;
	}

	private void source(final Element element){
		final FLEFRecord source = record(element, SourceReader.TYPE);
		dataEntries(element);

		final String title = GrampsXML.childText(element, "stitle");
		source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
			.addChild(child(NameReader.TAG_VALUE, title != null? title: tf("name.source", id(element)))));
		final String abbreviation = GrampsXML.childText(element, "sabbrev");
		if(abbreviation != null)
			source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(child(NameReader.TAG_TYPE, NameReader.ENUM_TYPE_ABBREVIATED))
				.addChild(child(NameReader.TAG_VALUE, abbreviation)));
		final String author = GrampsXML.childText(element, "sauthor");
		if(author != null)
			source.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_AUTHOR)
				.addChild(child(AgentReader.TAG_TEXT, author)));
		source.addChild(child(SourceReader.TAG_PUBLISHER, GrampsXML.childText(element, "spubinfo")));

		String mediaType = null;
		final List<FLEFRecord> repositories = new ArrayList<>();
		for(final Element reference : GrampsXML.children(element, "reporef")){
			final Element repository = target(reference, GRAMPS_REPOSITORY);
			if(repository == null)
				continue;

			final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceReader.TAG_REPOSITORY)
				.addChild(child(RepositoryCitationReader.TAG_REPOSITORY, id(repository)))
				.addChild(child(RepositoryCitationReader.TAG_LOCATOR, GrampsXML.attribute(reference, "callno")));
			for(final Element noteReference : GrampsXML.children(reference, GRAMPS_NOTEREF)){
				final Element note = target(noteReference, GRAMPS_NOTE);
				final String text = (note != null? GrampsXML.childText(note, GRAMPS_TEXT): null);
				if(text != null)
					citation.addChild(child(RepositoryCitationReader.TAG_REMARK, text));
			}
			repositories.add(citation);

			final String medium = GEDCOMMapper.toTypeToken(GrampsXML.attribute(reference, "medium"));
			if(mediaType == null && medium != null && !TYPE_UNKNOWN.equals(medium))
				mediaType = medium;
		}
		source.addChild(child(SourceReader.TAG_MEDIA_TYPE, mediaType));
		for(final FLEFRecord repository : repositories)
			source.addChild(repository);

		final Set<String> media = new LinkedHashSet<>(sourceMedia.getOrDefault(GrampsXML.attribute(element, GRAMPS_HANDLE), Set.of()));
		for(final Element reference : GrampsXML.children(element, GRAMPS_OBJREF)){
			final Element object = target(reference, GRAMPS_OBJECT);
			if(object != null)
				media.add(id(object));
		}
		for(final String mediaId : media)
			source.addChild(child(SourceReader.TAG_MEDIA, mediaId));

		for(final Element attribute : GrampsXML.children(element, "srcattribute"))
			source.addChild(note(GrampsXML.attribute(attribute, GRAMPS_TYPE), GrampsXML.attribute(attribute, GRAMPS_VALUE), element));
		notes(source, element);
		privacy(source, element);
		source.addChild(audit(isoOf(element)));
		model.addRecord(source);
	}


	/* ======================================================================
	 *                          Individuals
	 * ====================================================================== */

	private void person(final Element element){
		final FLEFRecord individual = record(element, IndividualReader.TYPE);
		individuals.put(GrampsXML.attribute(element, GRAMPS_HANDLE), individual);

		final List<Element> names = GrampsXML.children(element, GRAMPS_NAME);
		Element primary = null;
		for(final Element name : names)
			if(!"1".equals(GrampsXML.attribute(name, "alt"))){
				primary = name;

				break;
			}
		if(primary == null && !names.isEmpty())
			primary = names.getFirst();
		if(primary != null)
			individual.addChild(name(primary));
		for(final Element name : names)
			if(name != primary)
				individual.addChild(name(name));

		final String gender = StringUtils.defaultString(GrampsXML.childText(element, "gender"))
			.toUpperCase(Locale.ROOT);
		switch(gender){
			case "M" -> sex(element, individual, IndividualReader.ENUM_SEX_MALE);
			case "F" -> sex(element, individual, IndividualReader.ENUM_SEX_FEMALE);
			case "U" -> sex(element, individual, IndividualReader.ENUM_SEX_UNKNOWN);
			default -> {}
		}

		citations(individual, element);
		notes(individual, element);
		for(final Element url : GrampsXML.children(element, GRAMPS_URL))
			individual.addChild(note(t("title.url"), urlText(url), element));
		media(individual, element);
		privacy(individual, element);
		individual.addChild(audit(isoOf(element)));
		model.addRecord(individual);

		for(final Element attribute : GrampsXML.children(element, GRAMPS_ATTRIBUTE))
			personAttribute(element, attribute);
		for(final Element address : GrampsXML.children(element, GRAMPS_ADDRESS))
			residence(element, address);
		for(final Element association : GrampsXML.children(element, "personref"))
			association(element, association);
		for(final Element lds : GrampsXML.children(element, "lds_ord"))
			lds(lds, false, element);
	}

	private FLEFRecord name(final Element name){
		final FLEFRecord structure = FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME);
		final String type = GrampsXML.attribute(name, GRAMPS_TYPE);
		if(type != null && !TYPE_UNKNOWN.equalsIgnoreCase(type) && !"Other Name".equalsIgnoreCase(type))
			structure.addChild(child(NameReader.TAG_TYPE, NAME_TYPES.getOrDefault(type.toLowerCase(Locale.ROOT), GEDCOMMapper.toTypeToken(type))));

		part(structure, NameReader.ENUM_PART_TYPE_TITLE, GrampsXML.childText(name, GRAMPS_TITLE));
		part(structure, NameReader.ENUM_PART_TYPE_GIVEN, GrampsXML.childText(name, "first"));
		part(structure, NameReader.ENUM_PART_TYPE_NICKNAME, GrampsXML.childText(name, "nick"));
		final List<Element> surnames = GrampsXML.children(name, "surname");
		for(final Element surname : surnames){
			final String prefix = StringUtils.trimToNull(GrampsXML.attribute(surname, "prefix"));
			final String text = StringUtils.trimToNull(GrampsXML.text(surname));
			final String value = StringUtils.trimToNull(StringUtils.defaultString(prefix) + StringUtils.SPACE + StringUtils.defaultString(text));
			final String derivation = StringUtils.defaultString(GrampsXML.attribute(surname, "derivation"))
				.toLowerCase(Locale.ROOT);
			final String connector = GrampsXML.attribute(surname, "connector");
			if(StringUtils.isNotBlank(connector))
				discard(ImportReasons.NAME_DETAIL, tf("discard.surname_connector", connector, value), name);
			if(!derivation.isEmpty() && !SURNAME_DERIVATIONS.containsKey(derivation) && !"unknown".equals(derivation) && !"inherited".equals(derivation))
				discard(ImportReasons.NAME_DETAIL, tf("discard.surname_derivation", derivation, value), name);
			//the prefix is its own part, just before the surname it belongs to
			part(structure, NameReader.ENUM_PART_TYPE_FAMILY_PREFIX, prefix);
			part(structure, SURNAME_DERIVATIONS.getOrDefault(derivation, NameReader.ENUM_PART_TYPE_FAMILY), text);
		}
		//the surname that is the primary one is not the first
		for(int i = 0; i < surnames.size(); i ++){
			final Element surname = surnames.get(i);
			if(!"0".equals(GrampsXML.attribute(surname, GRAMPS_PRIMARY)) && !"0".equals(GrampsXML.attribute(surname, "primary"))){
				if(i > 0)
					discard(ImportReasons.NAME_DETAIL, tf("discard.primary_surname", GrampsXML.text(surname)), name);
				break;
			}
		}
		part(structure, NameReader.ENUM_PART_TYPE_FAMILY_NICKNAME, GrampsXML.childText(name, "familynick"));
		part(structure, NameReader.ENUM_PART_TYPE_SUFFIX, GrampsXML.childText(name, GRAMPS_SUFFIX));
		if(FLEFRecordHelper.findChildren(structure, NameReader.TAG_PART).isEmpty()){
			for(final String detail : List.of("call", "group"))
				if(GrampsXML.childText(name, detail) != null)
					discard(ImportReasons.NAME_DETAIL, tf("discard.name_without_parts", detail, GrampsXML.childText(name, detail)), name);
			return null;
		}

		validity(structure, dateOf(name), name);
		citations(structure, name);
		notes(structure, name);
		final String call = GrampsXML.childText(name, "call");
		if(call != null)
			structure.addChild(note(t("title.call_name"), call, name));
		final String group = GrampsXML.childText(name, "group");
		if(group != null)
			structure.addChild(note(t("title.grouped_as"), group, name));
		return structure;
	}

	private static void part(final FLEFRecord name, final String type, final String value){
		if(value != null)
			name.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
				.addChild(child(NameReader.TAG_PART_TYPE, type))
				.addChild(child(NameReader.TAG_PART_VALUE, value)));
	}

	/** The gender of a person is the sex assertion of the file: an attribute of the individual. */
	private void sex(final Element person, final FLEFRecord individual, final String value){
		model.addRecord(FLEFRecord.createMainRecord(nextId(IndividualAttributeReader.ID_PREFIX), IndividualAttributeReader.TYPE)
			.addChild(child(IndividualAttributeReader.TAG_INDIVIDUAL, individual.getId()))
			.addChild(child(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(child(IndividualAttributeReader.TAG_VALUE, value))
			.addChild(audit(isoOf(person))));
	}

	private void personAttribute(final Element person, final Element attribute){
		final String type = GrampsXML.attribute(attribute, GRAMPS_TYPE);
		final String value = GrampsXML.attribute(attribute, GRAMPS_VALUE);
		if(type == null || value == null){
			incompleteAttribute(attribute, person);
			return;
		}

		final String tag = ATTRIBUTE_TAG_BY_TYPE.get(type.toLowerCase(Locale.ROOT));
		final FLEFRecord record = FLEFRecord.createMainRecord(nextId(IndividualAttributeReader.ID_PREFIX),
				IndividualAttributeReader.TYPE)
			.addChild(child(IndividualAttributeReader.TAG_INDIVIDUAL, id(person)))
			.addChild(child(IndividualAttributeReader.TAG_TYPE, (tag != null? GEDCOMMapper.mapAttribute(tag, tag): GEDCOMMapper.toTypeToken(type))))
			.addChild(child(IndividualAttributeReader.TAG_VALUE, value));
		citations(record, attribute);
		notes(record, attribute);
		privacy(record, attribute);
		record.addChild(audit(isoOf(person)));
		model.addRecord(record);
	}

	/** An address of an individual is a residence, whose place is the place of type address (the value is the place). */
	private void residence(final Element person, final Element address){
		final String placeId = addressPlace(address);
		final FLEFRecord record = FLEFRecord.createMainRecord(nextId(IndividualAttributeReader.ID_PREFIX),
				IndividualAttributeReader.TYPE)
			.addChild(child(IndividualAttributeReader.TAG_INDIVIDUAL, id(person)))
			.addChild(child(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_RESIDENCE));
		if(placeId != null)
			record.addChild(FLEFRecord.createChildWithTag(IndividualAttributeReader.TAG_PLACE)
				.addChild(child(PlaceCitationReader.TAG_PLACE, placeId)));
		//the date of the address is the validity of the residence
		validity(record, dateOf(address), person);
		citations(record, address);
		final String phone = GrampsXML.childText(address, GRAMPS_PHONE);
		if(phone != null)
			record.addChild(note(t("title.phone"), phone, person));
		notes(record, address);
		privacy(record, address);
		record.addChild(audit(isoOf(person)));
		model.addRecord(record);
	}

	/**
	 * An association is a relationship between the associate and the individual that has it, whose role is the one of the associate
	 * (a godfather ...).
	 */
	private void association(final Element person, final Element reference){
		final Element associate = target(reference, GRAMPS_PERSON);
		if(associate == null)
			return;

		final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(child(RelationshipReader.TAG_INDIVIDUAL, id(associate))))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(child(RelationshipReader.TAG_INDIVIDUAL, id(person))))
			.addChild(child(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_ASSOCIATE))
			.addChild(child(RelationshipReader.TAG_ROLE, GrampsXML.attribute(reference, "rel")));
		citations(relationship, reference);
		notes(relationship, reference);
		privacy(relationship, reference);
		relationship.addChild(audit(isoOf(person)));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}


	/* ======================================================================
	 *                          Families
	 * ====================================================================== */

	private void family(final Element element){
		final FLEFRecord group = record(element, GroupReader.TYPE);
		final Element father = target(GrampsXML.child(element, GRAMPS_FATHER), GRAMPS_PERSON);
		final Element mother = target(GrampsXML.child(element, GRAMPS_MOTHER), GRAMPS_PERSON);

		final String fatherName = familyName(father);
		final String motherName = familyName(mother);
		final String name = (father == null && mother == null
			? tf("name.family", id(element))
			: tf("name.family_of", (fatherName != null? fatherName: t("name.unknown_member")),
				(motherName != null? motherName: t("name.unknown_member"))));
		group.addChild(FLEFRecord.createChildWithTag(GroupReader.TAG_NAME)
			.addChild(child(NameReader.TAG_VALUE, name)));
		group.addChild(child(GroupReader.TAG_TYPE, GroupReader.ENUM_TYPE_FAMILY));
		citations(group, element);
		notes(group, element);
		media(group, element);
		privacy(group, element);
		group.addChild(audit(isoOf(element)));
		model.addRecord(group);

		if(father != null)
			membership(father, group, ROLE_FATHER, element);
		if(mother != null)
			membership(mother, group, ROLE_MOTHER, element);
		for(final Element reference : GrampsXML.children(element, "childref")){
			final Element offspring = target(reference, GRAMPS_PERSON);
			if(offspring == null)
				continue;

			membership(offspring, group, RelationshipReader.ENUM_TYPE_CHILD, element);
			if(father != null)
				parentChild(offspring, father, GrampsXML.attribute(reference, "frel"), reference, element);
			if(mother != null)
				parentChild(offspring, mother, GrampsXML.attribute(reference, "mrel"), reference, element);
		}
		if(father != null && mother != null)
			spouses(element, father, mother);

		for(final Element attribute : GrampsXML.children(element, GRAMPS_ATTRIBUTE))
			groupAttribute(element, group, GEDCOMMapper.toTypeToken(GrampsXML.attribute(attribute, GRAMPS_TYPE)),
				GrampsXML.attribute(attribute, GRAMPS_VALUE), attribute);
		for(final Element lds : GrampsXML.children(element, "lds_ord"))
			lds(lds, true, element);
	}

	/** @return	The family name of the individual (the first one it has), {@code null} if there is none. */
	private String familyName(final Element person){
		final FLEFRecord individual = (person != null? individuals.get(GrampsXML.attribute(person, GRAMPS_HANDLE)): null);
		if(individual == null)
			return null;

		for(final FLEFRecord name : FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_NAME))
			for(final FLEFRecord part : FLEFRecordHelper.findChildren(name, NameReader.TAG_PART))
				if(NameReader.ENUM_PART_TYPE_FAMILY.equals(FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_TYPE)))
					return FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_VALUE);
		return null;
	}

	private void membership(final Element person, final FLEFRecord group, final String role, final Element family){
		final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(child(RelationshipReader.TAG_INDIVIDUAL, id(person))))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(child(RelationshipReader.TAG_GROUP, group.getId())))
			.addChild(child(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_GROUP_MEMBER))
			.addChild(child(RelationshipReader.TAG_ROLE, role))
			.addChild(audit(isoOf(family)));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/**
	 * The relationship of a child with one of the parents: what Gramps says of that parent (the relationship with the mother can be
	 * different from the one with the father).
	 */
	private void parentChild(final Element child, final Element parent, final String kind, final Element reference,
			final Element family){
		final String key = Objects.toString(kind, "Birth")
			.toLowerCase(Locale.ROOT);
		final String type = CHILD_RELATIONSHIPS.getOrDefault(key, RelationshipReader.ENUM_TYPE_CHILD);
		final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(child(RelationshipReader.TAG_INDIVIDUAL, id(child))))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(child(RelationshipReader.TAG_INDIVIDUAL, id(parent))))
			.addChild(child(RelationshipReader.TAG_TYPE, type));
		if(RelationshipReader.ENUM_TYPE_CHILD.equals(type) && !CHILD_RELATIONSHIPS.containsKey(key) && !TYPE_UNKNOWN.equals(key))
			relationship.addChild(note(t("title.relationship"), kind, family));
		citations(relationship, reference);
		notes(relationship, reference);
		privacy(relationship, reference);
		relationship.addChild(audit(isoOf(family)));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/** The relationship between the spouses: it lasts from the marriage to the divorce or the annulment. */
	private void spouses(final Element family, final Element father, final Element mother){
		final String kind = Objects.toString(GrampsXML.attribute(GrampsXML.child(family, "rel"), GRAMPS_TYPE), TYPE_UNKNOWN);
		final String type = switch(kind.toLowerCase(Locale.ROOT)){
			case "civil union" -> RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE;
			case "unmarried" -> RelationshipReader.ENUM_TYPE_COHABITING_PARTNER;
			default -> RelationshipReader.ENUM_TYPE_SPOUSE;
		};

		final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(child(RelationshipReader.TAG_INDIVIDUAL, id(father))))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(child(RelationshipReader.TAG_INDIVIDUAL, id(mother))))
			.addChild(child(RelationshipReader.TAG_TYPE, type));

		for(final Element reference : GrampsXML.children(family, GRAMPS_EVENTREF)){
			final Element event = target(reference, GRAMPS_EVENT);
			final String eventType = (event != null? StringUtils.defaultString(GrampsXML.childText(event, GRAMPS_TYPE)).toLowerCase(Locale.ROOT): "");
			if(GRAMPS_EVENT_MARRIAGE.equals(eventType) && FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM) == null)
				date(relationship, RelationshipReader.TAG_VALID_FROM, dateOf(event), event);
			else if((GRAMPS_EVENT_DIVORCE.equals(eventType) || GRAMPS_EVENT_ANNULMENT.equals(eventType)) && FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO) == null)
				date(relationship, RelationshipReader.TAG_VALID_TO, dateOf(event), event);
		}
		if(RelationshipReader.ENUM_TYPE_SPOUSE.equals(type) && !List.of("married", TYPE_UNKNOWN).contains(kind.toLowerCase(Locale.ROOT)))
			relationship.addChild(note(t("title.relationship_type"), kind, family));
		relationship.addChild(audit(isoOf(family)));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	private void groupAttribute(final Element family, final FLEFRecord group, final String type, final String value,
			final Element holder){
		if(type == null){
			incompleteAttribute(holder, family);
			return;
		}

		final FLEFRecord record = FLEFRecord.createMainRecord(nextId(GroupAttributeReader.ID_PREFIX), GroupAttributeReader.TYPE)
			.addChild(child(GroupAttributeReader.TAG_GROUP, group.getId()))
			.addChild(child(GroupAttributeReader.TAG_TYPE, type))
			.addChild(child(GroupAttributeReader.TAG_VALUE, value));
		citations(record, holder);
		notes(record, holder);
		privacy(record, holder);
		record.addChild(audit(isoOf(family)));
		model.addRecord(record);
	}


	/* ======================================================================
	 *                          Events
	 * ====================================================================== */

	/**
	 * How an event is written: the type of the event of FLEF that the type of Gramps has, or an attribute (an occupation is not an
	 * event) when the type is one and the event is not about a family.
	 */
	private static EventKind kindOf(final String type, final boolean ofFamily, final String description){
		final String tag = (type != null? EVENT_TAGS.get(type.toLowerCase(Locale.ROOT)): null);
		if(tag != null){
			if(ATTRIBUTE_TAGS.contains(tag))
				return new EventKind(GEDCOMMapper.mapAttribute(tag, tag), description,
					(!ofFamily? GEDCOMMapper.mapAttribute(tag, tag): null), null);
			return new EventKind(GEDCOMMapper.mapEvent(tag, tag), description, null, null);
		}

		//a custom type: if it is too long to be a type it is the description of the event
		final boolean named = (type != null && !TYPE_UNKNOWN.equalsIgnoreCase(type) && !"Custom".equalsIgnoreCase(type));
		final String token = (named? GEDCOMMapper.toTypeToken(type): null);
		if(token != null)
			return new EventKind(GEDCOMMapper.mapEventType(GEDCOMMapper.TAG_GENERIC_EVENT, type), description, null, null);
		return new EventKind(GEDCOMMapper.mapEventType(GEDCOMMapper.TAG_GENERIC_EVENT, null),
			(description != null? description: (named? type: null)), null, (description != null && named? type: null));
	}

	private void event(final Element element){
		final List<Owner> eventOwners = owners.getOrDefault(GrampsXML.attribute(element, GRAMPS_HANDLE), List.of());
		final boolean ofFamily = familyEvents.contains(GrampsXML.attribute(element, GRAMPS_HANDLE));
		final EventKind kind = kindOf(GrampsXML.childText(element, GRAMPS_TYPE), ofFamily, GrampsXML.childText(element, GRAMPS_DESCRIPTION));

		final List<Owner> principals = new ArrayList<>();
		for(final Owner owner : eventOwners)
			if(!owner.isFamily() && ROLE_PRINCIPAL.equals(roleOf(GrampsXML.attribute(owner.reference(), "role"))))
				principals.add(owner);
		//an attribute of the individuals that have it
		if(kind.attributeType() != null && !principals.isEmpty()){
			if(kind.description() == null){
				discard(ImportReasons.OTHER, tf("discard.attribute_without_value", kind.attributeType()), element);
				return;
			}

			if(eventOwners.size() > principals.size())
				discard(ImportReasons.OTHER, tf("discard.participants", kind.description()), element);
			if(GrampsXML.childText(element, "cause") != null || !GrampsXML.children(element, GRAMPS_ATTRIBUTE).isEmpty())
				discard(ImportReasons.OTHER, tf("discard.cause_attributes", kind.description()), element);
			for(final Owner owner : principals)
				individualAttribute(element, kind, owner);
			return;
		}

		final FLEFRecord event = record(element, EventReader.TYPE)
			.addChild(child(EventReader.TAG_TYPE, kind.type()))
			.addChild(child(EventReader.TAG_DESCRIPTION, kind.description()));
		date(event, EventReader.TAG_DATE, dateOf(element), element);
		event.addChild(placeCitation(GrampsXML.child(element, GRAMPS_PLACE)));
		final List<String> notes = new ArrayList<>();
		final String cause = GrampsXML.childText(element, "cause");
		FLEFRecord causeStructure = (cause != null? FLEFRecord.createChildWithTag(EventReader.TAG_CAUSE).addChild(child(EventReader.TAG_REASON, cause)): null);
		for(final Element attribute : GrampsXML.children(element, GRAMPS_ATTRIBUTE)){
			final String type = GrampsXML.attribute(attribute, GRAMPS_TYPE);
			final String value = GrampsXML.attribute(attribute, GRAMPS_VALUE);
			if(type == null || value == null){
				incompleteAttribute(attribute, element);
				continue;
			}

			if("cause".equalsIgnoreCase(type) && causeStructure == null)
				causeStructure = FLEFRecord.createChildWithTag(EventReader.TAG_CAUSE).addChild(child(EventReader.TAG_REASON, value));
			else
				notes.add(type + ": " + value);
		}
		event.addChild(causeStructure);
		citations(event, element);
		media(event, element);
		if(kind.typeNote() != null)
			event.addChild(note(t("title.type"), kind.typeNote(), element));
		if(!notes.isEmpty())
			event.addChild(note(t("title.attributes"), String.join(StringUtils.LF, notes), element));
		notes(event, element);
		privacy(event, element);
		event.addChild(audit(isoOf(element)));
		model.addRecord(event);

		participations(event, eventOwners, element);
	}

	private void individualAttribute(final Element eventElement, final EventKind kind, final Owner owner){
		final FLEFRecord record = FLEFRecord.createMainRecord(nextId(IndividualAttributeReader.ID_PREFIX),
				IndividualAttributeReader.TYPE)
			.addChild(child(IndividualAttributeReader.TAG_INDIVIDUAL, id(handles.get(owner.handle()))))
			.addChild(child(IndividualAttributeReader.TAG_TYPE, kind.attributeType()))
			.addChild(child(IndividualAttributeReader.TAG_VALUE, kind.description()));
		validity(record, dateOf(eventElement), eventElement);
		record.addChild(placeCitation(GrampsXML.child(eventElement, GRAMPS_PLACE)));
		citations(record, eventElement);
		citations(record, owner.reference());
		media(record, eventElement);
		notes(record, eventElement);
		notes(record, owner.reference());
		privacy(record, eventElement);
		record.addChild(audit(isoOf(eventElement)));
		model.addRecord(record);
	}

	/** The function of a participant in the event: the role of Gramps, as FLEF names it. */
	private static String roleOf(final String role){
		if(role == null)
			return ROLE_PRINCIPAL;

		final String key = role.toLowerCase(Locale.ROOT);
		if(EVENT_ROLES.containsKey(key))
			return EVENT_ROLES.get(key);
		return ("unknown".equals(key) || "custom".equals(key)? null: GEDCOMMapper.toTypeToken(role));
	}

	/**
	 * Writes who took part in the event, with the evidence of each one. A family event is about the family, and about each parent.
	 */
	private void participations(final FLEFRecord event, final List<Owner> eventOwners, final Element element){
		final Set<String> principals = new HashSet<>();
		for(final Owner owner : eventOwners){
			final Element holder = handles.get(owner.handle());
			final String participantTag = (owner.isFamily()? EventParticipationReader.TAG_PARTICIPANT_GROUP: EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL);
			final String role = (owner.isFamily()? ROLE_PRINCIPAL: roleOf(GrampsXML.attribute(owner.reference(), "role")));
			if(role == null)
				discard(ImportReasons.OTHER, tf("discard.role", GrampsXML.attribute(owner.reference(), "role"), id(holder)), element);

			participation(event, participantTag, id(holder), role, owner.reference(), element);
			if(owner.isFamily())
				for(final String parentTag : List.of(GRAMPS_FATHER, GRAMPS_MOTHER)){
					final Element parent = target(GrampsXML.child(holder, parentTag), GRAMPS_PERSON);
					if(parent != null && principals.add(id(parent)))
						participation(event, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, id(parent), ROLE_PRINCIPAL, null, element);
				}
			else if(ROLE_PRINCIPAL.equals(role))
				principals.add(id(holder));
		}
	}

	private void participation(final FLEFRecord event, final String participantTag, final String participantId, final String role,
			final Element reference, final Element about){
		final FLEFRecord participation = FLEFRecord.createMainRecord(nextId(EventParticipationReader.ID_PREFIX),
				EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(child(participantTag, participantId)))
			.addChild(child(EventParticipationReader.TAG_EVENT, event.getId()))
			.addChild(child(EventParticipationReader.TAG_ROLE, role));
		if(reference != null){
			citations(participation, reference);
				for(final Element attribute : GrampsXML.children(reference, GRAMPS_ATTRIBUTE)){
				final String type = GrampsXML.attribute(attribute, GRAMPS_TYPE);
				final String value = GrampsXML.attribute(attribute, GRAMPS_VALUE);
				if(type == null || value == null){
					incompleteAttribute(attribute, about);
					continue;
				}

				final boolean age = GRAMPS_AGE.equalsIgnoreCase(type);
				//the age of an individual says when it was born, if the individual has no birth with a date; otherwise it is a note
				if(!(age && EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL.equals(participantTag)
						&& birthFromAge(event, about, participantId, value)))
					participation.addChild(note((age? t("title.age"): type), value, about));
			}
			notes(participation, reference);
			privacy(participation, reference);
		}
		participation.addChild(audit(isoOf(about)));
		Deduplicator.getDeduplicatedRecordId(model, participation);
	}

	/**
	 * Writes the birth of an individual that the age at an event says: its date is calculated (the year of the event minus the age),
	 * its evidence is the one of the event, taken as indirect, and its confidence is low.
	 *
	 * @return	Whether the birth was written ({@code false} if the individual has a birth with a date, or the age or the date of the
	 * 	event cannot be used).
	 */
	private boolean birthFromAge(final FLEFRecord event, final Element eventElement, final String individualId, final String age){
		if(datedBirths.contains(individualId) || isBirth(eventElement))
			return false;

		final FLEFRecord date = GrampsDates.birthDate(EventReader.TAG_DATE, dateOf(eventElement), age);
		if(date == null)
			return false;

		final FLEFRecord birth = FLEFRecord.createMainRecord(nextId(EventReader.ID_PREFIX), EventReader.TYPE)
			.addChild(child(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(date);
		citations(birth, eventElement, true);
		//without a source, where the age comes from is said by a note
		if(FLEFRecordHelper.findChildren(birth, EventReader.TAG_SOURCE).isEmpty())
			birth.addChild(noteStructure(t("title.age"), tf("text.birth_from_age", event.getId()),
				MIME_MARKDOWN, isoOf(eventElement)));
		birth.addChild(child(EventReader.TAG_CONFIDENCE, ConfidenceReader.ENUM_LOW))
			.addChild(audit(isoOf(eventElement)));
		model.addRecord(birth);
		datedBirths.add(individualId);

		participation(birth, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, individualId, ROLE_PRINCIPAL, null, eventElement);
		return true;
	}

	/** An ordinance of the Church of Jesus Christ of Latter-day Saints is an event of the individual (or of the spouses). */
	private void lds(final Element lds, final boolean ofFamily, final Element holder){
		final String tag = LDS_TAGS.get(StringUtils.defaultString(GrampsXML.attribute(lds, GRAMPS_TYPE)).toLowerCase(Locale.ROOT));
		//the sealing of spouses is told by the family, the other ordinances by the individual
		if(tag == null || ofFamily != "SLGS".equals(tag)){
			discard(ImportReasons.OTHER, tf("discard.lds", GrampsXML.attribute(lds, GRAMPS_TYPE)), holder);
			return;
		}

		final FLEFRecord event = FLEFRecord.createMainRecord(nextId(EventReader.ID_PREFIX), EventReader.TYPE)
			.addChild(child(EventReader.TAG_TYPE, GEDCOMMapper.mapEvent(tag, tag)));
		date(event, EventReader.TAG_DATE, dateOf(lds), holder);
		event.addChild(placeCitation(GrampsXML.child(lds, GRAMPS_PLACE)));
		citations(event, lds);
		final String temple = GrampsXML.attribute(GrampsXML.child(lds, "temple"), "val");
		if(temple != null)
			event.addChild(note(t("title.temple"), temple, holder));
		final String status = GrampsXML.attribute(GrampsXML.child(lds, "status"), "val");
		if(status != null && !"None".equalsIgnoreCase(status))
			event.addChild(note(t("title.status"), status, holder));
		final Element sealed = target(GrampsXML.child(lds, "sealed_to"), GRAMPS_FAMILY);
		if(sealed != null)
			event.addChild(noteStructure(t("title.sealed_to"), "[" + id(sealed) + "](flef:" + id(sealed) + ")", MIME_MARKDOWN, isoOf(holder)));
		notes(event, lds);
		privacy(event, lds);
		event.addChild(audit(isoOf(holder)));
		model.addRecord(event);

		if(ofFamily){
			participation(event, EventParticipationReader.TAG_PARTICIPANT_GROUP, id(holder), ROLE_PRINCIPAL, null, holder);
			for(final String parentTag : List.of(GRAMPS_FATHER, GRAMPS_MOTHER)){
				final Element parent = target(GrampsXML.child(holder, parentTag), GRAMPS_PERSON);
				if(parent != null)
					participation(event, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, id(parent), ROLE_PRINCIPAL, null, holder);
			}
		}
		else
			participation(event, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, id(holder), ROLE_PRINCIPAL, null, holder);
	}

}
