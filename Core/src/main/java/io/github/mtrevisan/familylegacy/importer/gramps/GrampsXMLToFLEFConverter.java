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

import io.github.mtrevisan.familylegacy.importer.gedcom.Deduplicator;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.importer.gedcom.PlaceCache;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * Gramps has no counterpart in FLEF for the bookmarks and the formats of the names; the regions of the images, and the
 * dates in a calendar that FLEF cannot write here (Islamic, Persian, Swedish), are kept as notes.
 */
public final class GrampsXMLToFLEFConverter{

	private static final String PROTOCOL_NAME = "Family LEgacy Format";
	private static final String PROTOCOL_VERSION = "0.2.0";

	private static final String ROLE_PRINCIPAL = "principal";
	private static final String PRIVACY_RESTRICTED = "restricted";
	private static final String TYPE_UNKNOWN = "unknown";

	/** The identifiers that can be kept: they must begin with a letter, and have only letters and digits. */
	private static final Pattern ID_FORMAT = Pattern.compile("[A-Za-z][A-Za-z0-9]*");
	private static final Pattern FAMILY_ID = Pattern.compile("F(\\d+)");

	/** The GEDCOM tag of the types of event of Gramps (lowercase), whose FLEF type is given by the mapper of GEDCOM. */
	private static final Map<String, String> EVENT_TAGS = Map.ofEntries(
		Map.entry("birth", "BIRT"), Map.entry("death", "DEAT"), Map.entry("baptism", "BAPM"), Map.entry("burial", "BURI"),
		Map.entry("christening", "CHR"), Map.entry("cremation", "CREM"), Map.entry("adopted", "ADOP"),
		Map.entry("bar mitzvah", "BARM"), Map.entry("bas mitzvah", "BASM"), Map.entry("blessing", "BLES"),
		Map.entry("adult christening", "CHRA"), Map.entry("confirmation", "CONF"), Map.entry("first communion", "FCOM"),
		Map.entry("ordination", "ORDN"), Map.entry("naturalization", "NATU"), Map.entry("emigration", "EMIG"),
		Map.entry("immigration", "IMMI"), Map.entry("census", "CENS"), Map.entry("probate", "PROB"), Map.entry("will", "WILL"),
		Map.entry("graduation", "GRAD"), Map.entry("retirement", "RETI"),
		Map.entry("occupation", "OCCU"), Map.entry("residence", "RESI"), Map.entry("education", "EDUC"),
		Map.entry("religion", "RELI"), Map.entry("nationality", "NATI"), Map.entry("property", "PROP"),
		Map.entry("caste", "CAST"), Map.entry("physical description", "DSCR"), Map.entry("number of children", "NCHI"),
		Map.entry("number of marriages", "NMR"), Map.entry("social security number", "SSN"),
		Map.entry("nobility title", "TITL"), Map.entry("identification number", "IDNO"),
		Map.entry("marriage", "MARR"), Map.entry("divorce", "DIV"), Map.entry("annulment", "ANUL"),
		Map.entry("engagement", "ENGA"), Map.entry("marriage banns", "MARB"), Map.entry("marriage contract", "MARC"),
		Map.entry("marriage license", "MARL"), Map.entry("marriage settlement", "MARS"), Map.entry("divorce filing", "DIVF"));
	/** The tags that are attributes of an individual (they are not events). */
	private static final Set<String> ATTRIBUTE_TAGS = Set.of("OCCU", "RESI", "EDUC", "RELI", "NATI", "PROP", "CAST", "DSCR",
		"NCHI", "NMR", "SSN", "TITL", "IDNO");
	/** The GEDCOM tag of the attributes of Gramps (lowercase). */
	private static final Map<String, String> ATTRIBUTE_TAG_BY_TYPE = Map.of(
		"caste", "CAST", "description", "DSCR", "identification number", "IDNO", "national origin", "NATI",
		"number of children", "NCHI", "social security number", "SSN");
	private static final Map<String, String> LDS_TAGS = Map.of(
		"baptism", "BAPL", "confirmation", "CONL", "endowment", "ENDL", "initiatory", "INIL", "sealed_to_parents", "SLGC",
		"sealed_to_spouse", "SLGS");
	private static final Map<String, String> NAME_TYPES = Map.of(
		"birth name", "birth", "also known as", "alias", "married name", "married");
	/** The type of name part that a derivation of the surname says. */
	private static final Map<String, String> SURNAME_DERIVATIONS = Map.of(
		"patronymic", "patronymic", "matronymic", "matronymic", "location", "toponymic", "occupation", "occupational",
		"feudal", "lineage");
	private static final Map<String, String> EVENT_ROLES = Map.ofEntries(
		Map.entry("primary", ROLE_PRINCIPAL), Map.entry("family", ROLE_PRINCIPAL), Map.entry("bride", ROLE_PRINCIPAL),
		Map.entry("groom", ROLE_PRINCIPAL), Map.entry("clergy", "officiant"), Map.entry("celebrant", "officiant"),
		Map.entry("witness", "witness"), Map.entry("informant", "informant"), Map.entry("godparent", "sponsor"));
	private static final Map<String, String> CHILD_RELATIONSHIPS = Map.of(
		"birth", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "adopted", "adoptive_child", "stepchild", "step_child",
		"foster", "foster_child");

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
	private String mediaPath;
	private final String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS));


	private GrampsXMLToFLEFConverter(final Document document){
		root = document.getDocumentElement();
	}


	/**
	 * @param xml	The Gramps XML document.
	 * @param listener	Told of the progress of the conversion.
	 * @return	The model.
	 * @throws IOException	If the document is not a valid XML, or is not a Gramps one.
	 */
	public static FLEFModel convert(final byte[] xml, final GEDCOMToFLEFConverter.ProgressListener listener) throws IOException{
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
		return new GrampsXMLToFLEFConverter(document).build(listener);
	}


	private FLEFModel build(final GEDCOMToFLEFConverter.ProgressListener listener){
		listener.onProgress(0, "Indexing…");
		index();
		allocateIds();
		collectOwners();

		listener.onProgress(10, "Header…");
		header();
		listener.onProgress(20, "Places…");
		for(final Element place : items("places", "placeobj"))
			place(place);
		for(final Element place : items("places", "placeobj"))
			placeRelationships(place);
		listener.onProgress(30, "Repositories and media…");
		for(final Element repository : items("repositories", "repository"))
			repository(repository);
		for(final Element object : items("objects", "object"))
			media(object);
		listener.onProgress(40, "Individuals…");
		for(final Element person : items("people", "person"))
			person(person);
		listener.onProgress(60, "Families…");
		for(final Element family : items("families", "family"))
			family(family);
		listener.onProgress(75, "Events…");
		for(final Element event : items("events", "event"))
			event(event);
		listener.onProgress(90, "Sources…");
		for(final Element source : items("sources", "source"))
			source(source);
		listener.onProgress(100, "Done");
		return model;
	}


	/* ======================================================================
	 *                          Preparation
	 * ====================================================================== */

	private List<Element> items(final String collection, final String item){
		return GrampsXML.children(GrampsXML.child(root, collection), item);
	}

	private void index(){
		for(final Element collection : GrampsXML.children(root))
			for(final Element item : GrampsXML.children(collection)){
				final String handle = GrampsXML.attribute(item, "handle");
				if(handle != null)
					handles.put(handle, item);
			}
		mediaPath = GrampsXML.childText(GrampsXML.child(root, "header"), "mediapath");
		for(final Element tag : items("tags", "tag"))
			tagNames.put(GrampsXML.attribute(tag, "handle"), GrampsXML.attribute(tag, "name"));
	}

	/** The element a reference points to, if it is of the kind. */
	private Element target(final Element reference, final String kind){
		final String handle = (reference != null? GrampsXML.attribute(reference, "hlink"): null);
		final Element element = (handle != null? handles.get(handle): null);
		return (element != null && kind.equals(GrampsXML.name(element))? element: null);
	}

	private String id(final Element element){
		return ids.get(GrampsXML.attribute(element, "handle"));
	}

	/** Identifiers of the records: the ones of Gramps if they can be kept, otherwise new ones. */
	private void allocateIds(){
		final Map<Element, String> prefixes = new LinkedHashMap<>();
		for(final Element person : items("people", "person"))
			prefixes.put(person, IndividualReader.ID_PREFIX);
		for(final Element family : items("families", "family"))
			prefixes.put(family, GroupReader.ID_PREFIX);
		for(final Element event : items("events", "event"))
			prefixes.put(event, EventReader.ID_PREFIX);
		for(final Element place : items("places", "placeobj"))
			prefixes.put(place, PlaceReader.ID_PREFIX);
		for(final Element source : items("sources", "source"))
			prefixes.put(source, SourceReader.ID_PREFIX);
		for(final Element repository : items("repositories", "repository"))
			prefixes.put(repository, RepositoryReader.ID_PREFIX);
		for(final Element object : items("objects", "object"))
			prefixes.put(object, MediaReader.ID_PREFIX);

		for(final Map.Entry<Element, String> entry : prefixes.entrySet()){
			String id = GrampsXML.attribute(entry.getKey(), "id");
			//the groups are the records that begin with G (the families of Gramps begin with F)
			if(id != null && entry.getValue().equals(GroupReader.ID_PREFIX) && FAMILY_ID.matcher(id).matches())
				id = GroupReader.ID_PREFIX + id.substring(1);
			if(id != null && ID_FORMAT.matcher(id).matches() && usedIds.add(id.toLowerCase(Locale.ROOT))){
				ids.put(GrampsXML.attribute(entry.getKey(), "handle"), id);
				IDGenerator.registerExistingId(id);
			}
		}
		for(final Map.Entry<Element, String> entry : prefixes.entrySet())
			if(!ids.containsKey(GrampsXML.attribute(entry.getKey(), "handle")))
				ids.put(GrampsXML.attribute(entry.getKey(), "handle"), nextId(entry.getValue()));
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
		for(final Element person : items("people", "person"))
			for(final Element reference : GrampsXML.children(person, "eventref")){
				final String event = GrampsXML.attribute(reference, "hlink");
				if(event != null)
					owners.computeIfAbsent(event, k -> new ArrayList<>())
						.add(new Owner(false, GrampsXML.attribute(person, "handle"), reference));
			}
		for(final Element family : items("families", "family"))
			for(final Element reference : GrampsXML.children(family, "eventref")){
				final String event = GrampsXML.attribute(reference, "hlink");
				if(event != null){
					owners.computeIfAbsent(event, k -> new ArrayList<>())
						.add(new Owner(true, GrampsXML.attribute(family, "handle"), reference));
					familyEvents.add(event);
				}
			}
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

	private static FLEFRecord audit(final String iso){
		return FLEFRecord.createChildWithTag("audit")
			.addChild(FLEFRecord.createChildWithTag("creation")
				.addChild(child("date", iso)));
	}

	private void privacy(final FLEFRecord parent, final Element element){
		if("1".equals(GrampsXML.attribute(element, "priv")))
			parent.addChild(FLEFRecord.createChildWithTag("privacy")
				.addChild(child("level", PRIVACY_RESTRICTED)));
	}

	private static FLEFRecord noteStructure(final String title, final String text, final String mime, final String iso){
		if(StringUtils.isBlank(text))
			return null;

		return FLEFRecord.createChildWithTag("note")
			.addChild(StringUtils.isNotBlank(title)? child("title", title): null)
			.addChild(child("text", text.trim()))
			.addChild(StringUtils.isNotBlank(mime)? child("mime", mime): null)
			.addChild(audit(iso));
	}

	/** A note about something that FLEF has no field for. */
	private FLEFRecord note(final String title, final String text, final Element about){
		return noteStructure(title, text, null, isoOf(about));
	}

	/** The notes that a record refers to, and its tags. */
	private void notes(final FLEFRecord parent, final Element holder){
		for(final Element reference : GrampsXML.children(holder, "noteref")){
			final Element note = target(reference, "note");
			if(note != null)
				parent.addChild(noteOf(note));
		}
		final List<String> tags = new ArrayList<>();
		for(final Element reference : GrampsXML.children(holder, "tagref")){
			final String name = tagNames.get(GrampsXML.attribute(reference, "hlink"));
			if(name != null)
				tags.add(name);
		}
		if(!tags.isEmpty())
			parent.addChild(note("Tags", String.join(", ", tags), holder));
	}

	/** A note of Gramps; its formatting (bold, italic, links) is kept as Markdown. */
	private FLEFRecord noteOf(final Element note){
		final String text = GrampsXML.childText(note, "text");
		final String markdown = ("1".equals(GrampsXML.attribute(note, "format"))? null: markdown(text, note));
		final String type = GrampsXML.attribute(note, "type");
		//the types that only say what the note is about are of no use
		final String title = (type != null && !type.endsWith(" Note") && !"General".equalsIgnoreCase(type)
			&& !"Unknown".equalsIgnoreCase(type)? type: null);
		return (markdown != null
			? noteStructure(title, markdown, "text/markdown", isoOf(note))
			: noteStructure(title, text, null, isoOf(note)));
	}

	/** @return	The text with the styles of the note written as Markdown, or {@code null} if it has none. */
	private static String markdown(final String text, final Element note){
		if(text == null)
			return null;

		final List<int[]> insertions = new ArrayList<>();
		final List<String> marks = new ArrayList<>();
		for(final Element style : GrampsXML.children(note, "style")){
			final String name = GrampsXML.attribute(style, "name");
			final String value = GrampsXML.attribute(style, "value");
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
				close = "](" + value + ")";
			}
			else
				continue;

			for(final Element range : GrampsXML.children(style, "range"))
				try{
					final int start = Integer.parseInt(GrampsXML.attribute(range, "start"));
					final int end = Integer.parseInt(GrampsXML.attribute(range, "end"));
					if(start >= 0 && end <= text.length() && start < end){
						marks.add(open);
						insertions.add(new int[]{start, 1, marks.size() - 1});
						marks.add(close);
						insertions.add(new int[]{end, 0, marks.size() - 1});
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

		final FLEFRecord structure = GrampsDates.structure(tag, date);
		if(structure != null)
			parent.addChild(structure);
		else
			parent.addChild(note("Date", GrampsDates.text(date), about));
	}

	/** Writes the validity of a fact observed at a date. */
	private void validity(final FLEFRecord parent, final Element date, final Element about){
		if(date == null)
			return;

		final FLEFRecord[] validity = GrampsDates.validity(date);
		if(validity != null){
			parent.addChild(validity[0]);
			parent.addChild(validity[1]);
		}
		else
			parent.addChild(note("Date", GrampsDates.text(date), about));
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
		final Element place = target(reference, "placeobj");
		return (place != null
			? FLEFRecord.createChildWithTag("place").addChild(child("place", id(place)))
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
		for(final Element reference : GrampsXML.children(holder, "citationref")){
			final Element citation = target(reference, "citation");
			final Element source = (citation != null? target(GrampsXML.child(citation, "sourceref"), "source"): null);
			if(source == null)
				continue;

			final String sourceId = id(source);
			final List<String> locator = new ArrayList<>();
			final String page = GrampsXML.childText(citation, "page");
			if(page != null)
				locator.add(page);
			final String date = GrampsDates.text(dateOf(citation));
			if(date != null)
				locator.add("Date: " + date);

			final List<FLEFRecord> extracts = new ArrayList<>();
			for(final Element objectReference : GrampsXML.children(citation, "objref")){
				final Element object = target(objectReference, "object");
				if(object == null)
					continue;

				sourceMedia.computeIfAbsent(GrampsXML.attribute(source, "handle"), k -> new LinkedHashSet<>())
					.add(id(object));
				extracts.add(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTag("media_part")
						.addChild(child("media", id(object)))));
			}

			final List<String> remarks = new ArrayList<>();
			for(final Element noteReference : GrampsXML.children(citation, "noteref")){
				final Element note = target(noteReference, "note");
				final String text = (note != null? GrampsXML.childText(note, "text"): null);
				if(text != null)
					remarks.add(text);
			}
			for(final Element attribute : GrampsXML.children(citation, "srcattribute"))
				remarks.add(GrampsXML.attribute(attribute, "type") + ": " + GrampsXML.attribute(attribute, "value"));
			if(!remarks.isEmpty())
				if(extracts.isEmpty())
					parent.addChild(note("Citation of " + sourceId, String.join(StringUtils.LF, remarks), citation));
				else
					extracts.getFirst()
						.addChild(child("remark", String.join(StringUtils.LF, remarks)));

			final FLEFRecord structure = FLEFRecord.createChildWithTag("source")
				.addChild(child("source", sourceId));
			if(!locator.isEmpty())
				structure.addChild(child("locator", String.join(", ", locator)));
			for(final FLEFRecord extract : extracts)
				structure.addChild(extract);
			structure.addChild(child("stance", "supports"))
				.addChild(evidence(GrampsXML.childText(citation, "confidence")));
			parent.addChild(structure);
		}
	}

	/** Gramps has five levels of confidence: from very low (0) to very high (4). */
	private static FLEFRecord evidence(final String confidence){
		if(confidence == null)
			return null;

		final FLEFRecord evidence = FLEFRecord.createChildWithTag("evidence");
		switch(confidence){
			case "3", "4" -> evidence.addChild(child("information_type", "primary"))
				.addChild(child("evidence_type", "direct"));
			case "2" -> evidence.addChild(child("information_type", "secondary"));
			default -> evidence.addChild(child("information_type", "undetermined"));
		}
		return evidence;
	}

	/**
	 * Writes the media of a record: the first one is its preferred image (for an individual and a family), and each one is cited
	 * as a source made of the media.
	 */
	private void media(final FLEFRecord parent, final Element holder){
		boolean first = true;
		for(final Element reference : GrampsXML.children(holder, "objref")){
			final Element object = target(reference, "object");
			if(object == null)
				continue;

			final String mediaId = id(object);
			if(first && (IndividualReader.isOfType(parent) || GroupReader.isOfType(parent)))
				parent.addChild(FLEFRecord.createChildWithTag("preferred_image")
					.addChild(child("media", mediaId)));
			first = false;

			final FLEFRecord source = mediaSources.computeIfAbsent(mediaId, k -> {
				final String description = GrampsXML.attribute(GrampsXML.child(object, "file"), "description");
				final FLEFRecord record = FLEFRecord.createMainRecord(nextId(SourceReader.ID_PREFIX), SourceReader.TYPE)
					.addChild(FLEFRecord.createChildWithTag("title")
						.addChild(child("value", description != null? description: "Media " + mediaId)))
					.addChild(child("media", mediaId))
					.addChild(audit(isoOf(object)));
				model.addRecord(record);
				return record;
			});
			final FLEFRecord structure = FLEFRecord.createChildWithTag("source")
				.addChild(child("source", source.getId()))
				.addChild(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTag("media_part")
						.addChild(child("media", mediaId))))
				.addChild(child("stance", "supports"));
			parent.addChild(structure);

			final String region = regionOf(reference);
			if(region != null)
				parent.addChild(note("Region of " + mediaId, region, holder));
		}
	}

	/** The region of an image that a reference selects, in percent (FLEF wants pixels, whose size Gramps does not keep). */
	private static String regionOf(final Element reference){
		final Element region = GrampsXML.child(reference, "region");
		if(region == null)
			return null;

		return "from (" + GrampsXML.attribute(region, "corner1_x") + "%, " + GrampsXML.attribute(region, "corner1_y") + "%) to ("
			+ GrampsXML.attribute(region, "corner2_x") + "%, " + GrampsXML.attribute(region, "corner2_y") + "%)";
	}


	/* ======================================================================
	 *                          Header
	 * ====================================================================== */

	private void header(){
		final Element headerElement = GrampsXML.child(root, "header");
		final Element created = GrampsXML.child(headerElement, "created");

		final FLEFRecord header = FLEFRecord.createChildWithTag("header")
			.addChild(FLEFRecord.createChildWithTag("protocol")
				.addChild(child("name", PROTOCOL_NAME))
				.addChild(child("version", PROTOCOL_VERSION)))
			.addChild(FLEFRecord.createChildWithTag("source")
				.addChild(child("name", "Gramps"))
				.addChild(child("version", GrampsXML.attribute(created, "version"))));

		final String date = GrampsXML.attribute(created, "date");
		header.addChild(child("date", (date != null && date.length() == 10? date + "T00:00:00Z": now)));

		final Element researcher = GrampsXML.child(headerElement, "researcher");
		if(researcher != null){
			final FLEFRecord submitter = FLEFRecord.createChildWithTag("submitter");
			final String name = GrampsXML.childText(researcher, "resname");
			final String address = addressText(GrampsXML.childText(researcher, "resaddr"),
				GrampsXML.childText(researcher, "reslocality"), GrampsXML.childText(researcher, "rescity"), null,
				GrampsXML.childText(researcher, "resstate"), GrampsXML.childText(researcher, "rescountry"),
				GrampsXML.childText(researcher, "respostal"));
			submitter.addChild(contact(address, "postal", name, null))
				.addChild(contact(GrampsXML.childText(researcher, "resphone"), "phone", name, null))
				.addChild(contact(GrampsXML.childText(researcher, "resemail"), "email", name, null));
			if(name != null)
				submitter.addChild(child("remark", StringUtils.strip(name, ", ")));
			header.addChild(submitter);
		}
		model.setHeader(header);
	}

	private FLEFRecord contact(final String value, final String type, final String name, final String remark){
		if(value == null)
			return null;

		return FLEFRecord.createChildWithTag("contact")
			.addChild(child("value", value))
			.addChild(child("type", type))
			.addChild(name != null? FLEFRecord.createChildWithTag("name").addChild(child("value", StringUtils.strip(name, ", "))): null)
			.addChild(remark != null? child("remark", remark): null)
			.addChild(audit(now));
	}

	/** @return	The lines of an address, one over the other ({@code null} if it has none). */
	private static String addressText(final String street, final String locality, final String city, final String county,
			final String state, final String country, final String postal){
		final List<String> lines = new ArrayList<>();
		for(final String part : new String[]{street, locality, city, county})
			if(part != null)
				lines.add(part);
		final String region = StringUtils.trimToNull(StringUtils.defaultString(state) + " " + StringUtils.defaultString(postal));
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
			final String value = GrampsXML.attribute(name, "value");
			if(value == null)
				continue;

			final FLEFRecord structure = FLEFRecord.createChildWithTag("name")
				.addChild(child("value", value))
				.addChild(child("locale", GrampsXML.attribute(name, "lang")));
			validity(structure, dateOf(name), element);
			place.addChild(structure);
		}
		if(FLEFRecordHelper.findChildren(place, "name").isEmpty()){
			final String title = GrampsXML.childText(element, "ptitle");
			place.addChild(FLEFRecord.createChildWithTag("name")
				.addChild(child("value", title != null? title: "Place " + id(element))));
		}

		final String type = GrampsXML.attribute(element, "type");
		if(type != null && !TYPE_UNKNOWN.equalsIgnoreCase(type))
			place.addChild(child("type", GEDCOMMapper.toTypeToken(type)));

		final Element coordinates = GrampsXML.child(element, "coord");
		final String iso = PlaceCache.toISO6709(GrampsXML.attribute(coordinates, "lat"), GrampsXML.attribute(coordinates, "long"));
		if(iso != null)
			place.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_LOCATION)
				.addChild(child(PlaceReader.TAG_COORDINATES, iso)));

		citations(place, element);
		final String code = GrampsXML.childText(element, "code");
		if(code != null)
			place.addChild(note("Code", code, element));
		for(final Element location : GrampsXML.children(element, "location"))
			place.addChild(note("Location", locationText(location), element));
		for(final Element url : GrampsXML.children(element, "url"))
			place.addChild(note("URL", urlText(url), element));
		notes(place, element);
		media(place, element);
		privacy(place, element);
		place.addChild(audit(isoOf(element)));
		model.addRecord(place);
	}

	private static String locationText(final Element location){
		final List<String> parts = new ArrayList<>();
		for(final String name : List.of("street", "locality", "city", "parish", "county", "state", "country", "postal", "phone")){
			final String value = GrampsXML.attribute(location, name);
			if(value != null)
				parts.add(name + ": " + value);
		}
		return String.join(", ", parts);
	}

	private static String urlText(final Element url){
		final String type = GrampsXML.attribute(url, "type");
		final String description = GrampsXML.attribute(url, "description");
		return GrampsXML.attribute(url, "href") + (description != null? " (" + description + ")": StringUtils.EMPTY)
			+ (type != null? " [" + type + "]": StringUtils.EMPTY);
	}

	/** A place is part of the places that contain it, for the time that Gramps says. */
	private void placeRelationships(final Element element){
		for(final Element reference : GrampsXML.children(element, "placeref")){
			final Element container = target(reference, "placeobj");
			if(container == null)
				continue;

			final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(PlaceRelationshipReader.ID_PREFIX),
					PlaceRelationshipReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag("subject")
					.addChild(child("place", id(element))))
				.addChild(FLEFRecord.createChildWithTag("target")
					.addChild(child("place", id(container))))
				.addChild(child("type", PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF));
			validity(relationship, dateOf(reference), element);
			relationship.addChild(audit(isoOf(element)));
			model.addRecord(relationship);
		}
	}


	/* ======================================================================
	 *                          Repositories, media, sources
	 * ====================================================================== */

	private void repository(final Element element){
		final FLEFRecord repository = record(element, RepositoryReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("name")
				.addChild(child("value", StringUtils.defaultString(GrampsXML.childText(element, "rname"),
					"Repository " + id(element)))));

		for(final Element address : GrampsXML.children(element, "address")){
			repository.addChild(contact(addressText(GrampsXML.childText(address, "street"), GrampsXML.childText(address, "locality"),
				GrampsXML.childText(address, "city"), GrampsXML.childText(address, "county"), GrampsXML.childText(address, "state"),
				GrampsXML.childText(address, "country"), GrampsXML.childText(address, "postal")), "postal", null, null));
			repository.addChild(contact(GrampsXML.childText(address, "phone"), "phone", null, null));
		}
		for(final Element url : GrampsXML.children(element, "url"))
			repository.addChild(contact(GrampsXML.attribute(url, "href"), "website", null,
				StringUtils.defaultString(GrampsXML.attribute(url, "description"), GrampsXML.attribute(url, "type"))));

		final String type = GrampsXML.childText(element, "type");
		if(type != null)
			repository.addChild(note("Type", type, element));
		notes(repository, element);
		privacy(repository, element);
		repository.addChild(audit(isoOf(element)));
		model.addRecord(repository);
	}

	private void media(final Element element){
		final Element file = GrampsXML.child(element, "file");
		final String source = GrampsXML.attribute(file, "src");
		final FLEFRecord media = record(element, MediaReader.TYPE)
			.addChild(child("uri", (source != null? location(source): "unknown")))
			.addChild(child("kind", GEDCOMMapper.mapMediaKind(null, GrampsXML.attribute(file, "mime"), source)))
			.addChild(child("description", GrampsXML.attribute(file, "description")));

		date(media, "date", dateOf(element), element);
		final String checksum = GrampsXML.attribute(file, "checksum");
		if(checksum != null)
			media.addChild(note("Checksum", checksum, element));
		for(final Element attribute : GrampsXML.children(element, "attribute"))
			media.addChild(note(GrampsXML.attribute(attribute, "type"), GrampsXML.attribute(attribute, "value"), element));
		notes(media, element);
		privacy(media, element);
		media.addChild(audit(isoOf(element)));
		model.addRecord(media);
	}

	/** The location of a media file: relative to the media path of the file, unless it is one that Gramps resolves by itself. */
	private String location(final String source){
		final boolean relative = !source.startsWith("/") && !source.contains(":") && !source.startsWith("\\");
		if(!relative || mediaPath == null || mediaPath.contains("{"))
			return source;

		return StringUtils.removeEnd(mediaPath, "/") + "/" + source;
	}

	private void source(final Element element){
		final FLEFRecord source = record(element, SourceReader.TYPE);

		final String title = GrampsXML.childText(element, "stitle");
		source.addChild(FLEFRecord.createChildWithTag("title")
			.addChild(child("value", title != null? title: "Source " + id(element))));
		final String abbreviation = GrampsXML.childText(element, "sabbrev");
		if(abbreviation != null)
			source.addChild(FLEFRecord.createChildWithTag("title")
				.addChild(child("type", "abbreviated"))
				.addChild(child("value", abbreviation)));
		final String author = GrampsXML.childText(element, "sauthor");
		if(author != null)
			source.addChild(FLEFRecord.createChildWithTag("author")
				.addChild(child("text", author)));
		source.addChild(child("publisher", GrampsXML.childText(element, "spubinfo")));

		String mediaType = null;
		final List<FLEFRecord> repositories = new ArrayList<>();
		for(final Element reference : GrampsXML.children(element, "reporef")){
			final Element repository = target(reference, "repository");
			if(repository == null)
				continue;

			final FLEFRecord citation = FLEFRecord.createChildWithTag("repository")
				.addChild(child("repository", id(repository)))
				.addChild(child("locator", GrampsXML.attribute(reference, "callno")));
			for(final Element noteReference : GrampsXML.children(reference, "noteref")){
				final Element note = target(noteReference, "note");
				final String text = (note != null? GrampsXML.childText(note, "text"): null);
				if(text != null)
					citation.addChild(child("remark", text));
			}
			repositories.add(citation);

			final String medium = GEDCOMMapper.toTypeToken(GrampsXML.attribute(reference, "medium"));
			if(mediaType == null && medium != null && !TYPE_UNKNOWN.equals(medium))
				mediaType = medium;
		}
		source.addChild(child("media_type", mediaType));
		for(final FLEFRecord repository : repositories)
			source.addChild(repository);

		final Set<String> media = new LinkedHashSet<>(sourceMedia.getOrDefault(GrampsXML.attribute(element, "handle"), Set.of()));
		for(final Element reference : GrampsXML.children(element, "objref")){
			final Element object = target(reference, "object");
			if(object != null)
				media.add(id(object));
		}
		for(final String mediaId : media)
			source.addChild(child("media", mediaId));

		for(final Element attribute : GrampsXML.children(element, "srcattribute"))
			source.addChild(note(GrampsXML.attribute(attribute, "type"), GrampsXML.attribute(attribute, "value"), element));
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
		individuals.put(GrampsXML.attribute(element, "handle"), individual);

		final List<Element> names = GrampsXML.children(element, "name");
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

		final String gender = StringUtils.defaultString(GrampsXML.childText(element, "gender")).toUpperCase(Locale.ROOT);
		switch(gender){
			case "M" -> individual.addChild(child("sex", "male"));
			case "F" -> individual.addChild(child("sex", "female"));
			case "U" -> individual.addChild(child("sex", TYPE_UNKNOWN));
			default -> {}
		}

		citations(individual, element);
		notes(individual, element);
		for(final Element url : GrampsXML.children(element, "url"))
			individual.addChild(note("URL", urlText(url), element));
		media(individual, element);
		privacy(individual, element);
		individual.addChild(audit(isoOf(element)));
		model.addRecord(individual);

		for(final Element attribute : GrampsXML.children(element, "attribute"))
			personAttribute(element, attribute);
		for(final Element address : GrampsXML.children(element, "address"))
			residence(element, address);
		for(final Element association : GrampsXML.children(element, "personref"))
			association(element, association);
		for(final Element lds : GrampsXML.children(element, "lds_ord"))
			lds(lds, false, element);
	}

	private FLEFRecord name(final Element name){
		final FLEFRecord structure = FLEFRecord.createChildWithTag("name");
		final String type = GrampsXML.attribute(name, "type");
		if(type != null && !TYPE_UNKNOWN.equalsIgnoreCase(type) && !"Other Name".equalsIgnoreCase(type))
			structure.addChild(child("type", NAME_TYPES.getOrDefault(type.toLowerCase(Locale.ROOT), GEDCOMMapper.toTypeToken(type))));

		part(structure, "title", GrampsXML.childText(name, "title"));
		part(structure, "given", GrampsXML.childText(name, "first"));
		part(structure, "nickname", GrampsXML.childText(name, "nick"));
		for(final Element surname : GrampsXML.children(name, "surname")){
			final String prefix = GrampsXML.attribute(surname, "prefix");
			final String text = GrampsXML.text(surname);
			final String value = StringUtils.trimToNull(StringUtils.defaultString(prefix) + " " + StringUtils.defaultString(text));
			final String derivation = StringUtils.defaultString(GrampsXML.attribute(surname, "derivation")).toLowerCase(Locale.ROOT);
			part(structure, SURNAME_DERIVATIONS.getOrDefault(derivation, "family"), value);
		}
		part(structure, "family_nickname", GrampsXML.childText(name, "familynick"));
		part(structure, "suffix", GrampsXML.childText(name, "suffix"));
		if(FLEFRecordHelper.findChildren(structure, "part").isEmpty())
			return null;

		validity(structure, dateOf(name), name);
		citations(structure, name);
		notes(structure, name);
		final String call = GrampsXML.childText(name, "call");
		if(call != null)
			structure.addChild(note("Call name", call, name));
		final String group = GrampsXML.childText(name, "group");
		if(group != null)
			structure.addChild(note("Grouped as", group, name));
		return structure;
	}

	private static void part(final FLEFRecord name, final String type, final String value){
		if(value != null)
			name.addChild(FLEFRecord.createChildWithTag("part")
				.addChild(child("type", type))
				.addChild(child("value", value)));
	}

	private void personAttribute(final Element person, final Element attribute){
		final String type = GrampsXML.attribute(attribute, "type");
		final String value = GrampsXML.attribute(attribute, "value");
		if(type == null || value == null)
			return;

		final String tag = ATTRIBUTE_TAG_BY_TYPE.get(type.toLowerCase(Locale.ROOT));
		final FLEFRecord record = FLEFRecord.createMainRecord(nextId(IndividualAttributeReader.ID_PREFIX),
				IndividualAttributeReader.TYPE)
			.addChild(child("individual", id(person)))
			.addChild(child("type", (tag != null? GEDCOMMapper.mapAttribute(tag, tag): GEDCOMMapper.toTypeToken(type))))
			.addChild(child("value", value));
		citations(record, attribute);
		notes(record, attribute);
		privacy(record, attribute);
		record.addChild(audit(isoOf(person)));
		model.addRecord(record);
	}

	/** An address of an individual is a residence. */
	private void residence(final Element person, final Element address){
		final String value = addressText(GrampsXML.childText(address, "street"), GrampsXML.childText(address, "locality"),
			GrampsXML.childText(address, "city"), GrampsXML.childText(address, "county"), GrampsXML.childText(address, "state"),
			GrampsXML.childText(address, "country"), GrampsXML.childText(address, "postal"));
		final FLEFRecord record = FLEFRecord.createMainRecord(nextId(IndividualAttributeReader.ID_PREFIX),
				IndividualAttributeReader.TYPE)
			.addChild(child("individual", id(person)))
			.addChild(child("type", "residence"))
			.addChild(child("value", (value != null? StringUtils.replace(value, StringUtils.LF, ", "): null)));
		validity(record, dateOf(address), person);
		citations(record, address);
		final String phone = GrampsXML.childText(address, "phone");
		if(phone != null)
			record.addChild(note("Phone", phone, person));
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
		final Element associate = target(reference, "person");
		if(associate == null)
			return;

		final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(child("individual", id(associate))))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(child("individual", id(person))))
			.addChild(child("type", "associate"))
			.addChild(child("role", GrampsXML.attribute(reference, "rel")));
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
		final Element father = target(GrampsXML.child(element, "father"), "person");
		final Element mother = target(GrampsXML.child(element, "mother"), "person");

		final String fatherName = familyName(father);
		final String motherName = familyName(mother);
		final String name = (father == null && mother == null
			? "Family " + id(element)
			: (fatherName != null? fatherName: "?") + " and " + (motherName != null? motherName: "?") + " family");
		group.addChild(FLEFRecord.createChildWithTag("name")
			.addChild(child("value", name)));
		group.addChild(child("type", "family"));
		citations(group, element);
		notes(group, element);
		media(group, element);
		privacy(group, element);
		group.addChild(audit(isoOf(element)));
		model.addRecord(group);

		if(father != null)
			membership(father, group, "father", element);
		if(mother != null)
			membership(mother, group, "mother", element);
		for(final Element reference : GrampsXML.children(element, "childref")){
			final Element offspring = target(reference, "person");
			if(offspring == null)
				continue;

			membership(offspring, group, "child", element);
			if(father != null)
				parentChild(offspring, father, GrampsXML.attribute(reference, "frel"), reference, element);
			if(mother != null)
				parentChild(offspring, mother, GrampsXML.attribute(reference, "mrel"), reference, element);
		}
		if(father != null && mother != null)
			spouses(element, father, mother);

		for(final Element attribute : GrampsXML.children(element, "attribute"))
			groupAttribute(element, group, GEDCOMMapper.toTypeToken(GrampsXML.attribute(attribute, "type")),
				GrampsXML.attribute(attribute, "value"), attribute);
		for(final Element lds : GrampsXML.children(element, "lds_ord"))
			lds(lds, true, element);
	}

	/** @return	The family name of the individual (the first one it has), {@code null} if there is none. */
	private String familyName(final Element person){
		final FLEFRecord individual = (person != null? individuals.get(GrampsXML.attribute(person, "handle")): null);
		if(individual == null)
			return null;

		for(final FLEFRecord name : FLEFRecordHelper.findChildren(individual, "name"))
			for(final FLEFRecord part : FLEFRecordHelper.findChildren(name, "part"))
				if("family".equals(FLEFRecordHelper.getChildValue(part, "type")))
					return FLEFRecordHelper.getChildValue(part, "value");
		return null;
	}

	private void membership(final Element person, final FLEFRecord group, final String role, final Element family){
		final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(child("individual", id(person))))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(child("group", group.getId())))
			.addChild(child("type", RelationshipReader.ENUM_TYPE_GROUP_MEMBER))
			.addChild(child("role", role))
			.addChild(audit(isoOf(family)));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/**
	 * The relationship of a child with one of the parents: what Gramps says of that parent (the relationship with the mother can be
	 * different from the one with the father).
	 */
	private void parentChild(final Element child, final Element parent, final String kind, final Element reference,
			final Element family){
		final String key = StringUtils.defaultString(kind, "Birth").toLowerCase(Locale.ROOT);
		final String type = CHILD_RELATIONSHIPS.getOrDefault(key, "child");
		final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(child("individual", id(child))))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(child("individual", id(parent))))
			.addChild(child("type", type));
		if("child".equals(type) && !CHILD_RELATIONSHIPS.containsKey(key))
			relationship.addChild(note("Relationship", kind, family));
		citations(relationship, reference);
		notes(relationship, reference);
		privacy(relationship, reference);
		relationship.addChild(audit(isoOf(family)));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/** The relationship between the spouses: it lasts from the marriage to the divorce or the annulment. */
	private void spouses(final Element family, final Element father, final Element mother){
		final String kind = StringUtils.defaultString(GrampsXML.attribute(GrampsXML.child(family, "rel"), "type"), TYPE_UNKNOWN);
		final String type = switch(kind.toLowerCase(Locale.ROOT)){
			case "civil union" -> "civil_spouse";
			case "unmarried" -> "cohabiting_partner";
			default -> "spouse";
		};

		final FLEFRecord relationship = FLEFRecord.createMainRecord(nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(child("individual", id(father))))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(child("individual", id(mother))))
			.addChild(child("type", type));

		for(final Element reference : GrampsXML.children(family, "eventref")){
			final Element event = target(reference, "event");
			final String eventType = (event != null? StringUtils.defaultString(GrampsXML.childText(event, "type")).toLowerCase(Locale.ROOT): "");
			if("marriage".equals(eventType) && FLEFRecordHelper.findChild(relationship, "valid_from") == null)
				date(relationship, "valid_from", dateOf(event), event);
			else if(("divorce".equals(eventType) || "annulment".equals(eventType)) && FLEFRecordHelper.findChild(relationship, "valid_to") == null)
				date(relationship, "valid_to", dateOf(event), event);
		}
		if("spouse".equals(type) && !List.of("married", TYPE_UNKNOWN).contains(kind.toLowerCase(Locale.ROOT)))
			relationship.addChild(note("Relationship type", kind, family));
		relationship.addChild(audit(isoOf(family)));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	private void groupAttribute(final Element family, final FLEFRecord group, final String type, final String value,
			final Element holder){
		if(type == null)
			return;

		final FLEFRecord record = FLEFRecord.createMainRecord(nextId(GroupAttributeReader.ID_PREFIX), GroupAttributeReader.TYPE)
			.addChild(child("group", group.getId()))
			.addChild(child("type", type))
			.addChild(child("value", value));
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
		final List<Owner> eventOwners = owners.getOrDefault(GrampsXML.attribute(element, "handle"), List.of());
		final boolean ofFamily = familyEvents.contains(GrampsXML.attribute(element, "handle"));
		final EventKind kind = kindOf(GrampsXML.childText(element, "type"), ofFamily, GrampsXML.childText(element, "description"));

		final List<Owner> principals = new ArrayList<>();
		for(final Owner owner : eventOwners)
			if(!owner.isFamily() && ROLE_PRINCIPAL.equals(roleOf(GrampsXML.attribute(owner.reference(), "role"))))
				principals.add(owner);
		//an attribute of the individuals that have it
		if(kind.attributeType() != null && !principals.isEmpty()){
			for(final Owner owner : principals)
				individualAttribute(element, kind, owner);
			return;
		}

		final FLEFRecord event = record(element, EventReader.TYPE)
			.addChild(child("type", kind.type()))
			.addChild(child("description", kind.description()));
		date(event, "date", dateOf(element), element);
		event.addChild(placeCitation(GrampsXML.child(element, "place")));
		final List<String> notes = new ArrayList<>();
		final String cause = GrampsXML.childText(element, "cause");
		FLEFRecord causeStructure = (cause != null? FLEFRecord.createChildWithTag("cause").addChild(child("reason", cause)): null);
		for(final Element attribute : GrampsXML.children(element, "attribute")){
			final String type = GrampsXML.attribute(attribute, "type");
			final String value = GrampsXML.attribute(attribute, "value");
			if(type == null || value == null)
				continue;

			if("cause".equalsIgnoreCase(type) && causeStructure == null)
				causeStructure = FLEFRecord.createChildWithTag("cause").addChild(child("reason", value));
			else
				notes.add(type + ": " + value);
		}
		event.addChild(causeStructure);
		citations(event, element);
		media(event, element);
		if(kind.typeNote() != null)
			event.addChild(note("Type", kind.typeNote(), element));
		if(!notes.isEmpty())
			event.addChild(note("Attributes", String.join(StringUtils.LF, notes), element));
		notes(event, element);
		privacy(event, element);
		event.addChild(audit(isoOf(element)));
		model.addRecord(event);

		participations(event, eventOwners, element);
	}

	private void individualAttribute(final Element eventElement, final EventKind kind, final Owner owner){
		final FLEFRecord record = FLEFRecord.createMainRecord(nextId(IndividualAttributeReader.ID_PREFIX),
				IndividualAttributeReader.TYPE)
			.addChild(child("individual", id(handles.get(owner.handle()))))
			.addChild(child("type", kind.attributeType()))
			.addChild(child("value", kind.description()));
		validity(record, dateOf(eventElement), eventElement);
		record.addChild(placeCitation(GrampsXML.child(eventElement, "place")));
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
			final String participantTag = (owner.isFamily()? "group": "individual");
			final String role = (owner.isFamily()? ROLE_PRINCIPAL: roleOf(GrampsXML.attribute(owner.reference(), "role")));

			participation(event, participantTag, id(holder), role, owner.reference(), element);
			if(owner.isFamily())
				for(final String parentTag : List.of("father", "mother")){
					final Element parent = target(GrampsXML.child(holder, parentTag), "person");
					if(parent != null && principals.add(id(parent)))
						participation(event, "individual", id(parent), ROLE_PRINCIPAL, null, element);
				}
			else if(ROLE_PRINCIPAL.equals(role))
				principals.add(id(holder));
		}
	}

	private void participation(final FLEFRecord event, final String participantTag, final String participantId, final String role,
			final Element reference, final Element about){
		final FLEFRecord participation = FLEFRecord.createMainRecord(nextId(EventParticipationReader.ID_PREFIX),
				EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("participant")
				.addChild(child(participantTag, participantId)))
			.addChild(child("event", event.getId()))
			.addChild(child("role", role));
		if(reference != null){
			citations(participation, reference);
			for(final Element attribute : GrampsXML.children(reference, "attribute")){
				final String type = GrampsXML.attribute(attribute, "type");
				final String value = GrampsXML.attribute(attribute, "value");
				if(type != null && value != null)
					participation.addChild(note(("age".equalsIgnoreCase(type)? "Age": type), value, about));
			}
			notes(participation, reference);
			privacy(participation, reference);
		}
		participation.addChild(audit(isoOf(about)));
		Deduplicator.getDeduplicatedRecordId(model, participation);
	}

	/** An ordinance of the Church of Jesus Christ of Latter-day Saints is an event of the individual (or of the spouses). */
	private void lds(final Element lds, final boolean ofFamily, final Element holder){
		final String tag = LDS_TAGS.get(StringUtils.defaultString(GrampsXML.attribute(lds, "type")).toLowerCase(Locale.ROOT));
		//the sealing of spouses is told by the family, the other ordinances by the individual
		if(tag == null || ofFamily != "SLGS".equals(tag))
			return;

		final FLEFRecord event = FLEFRecord.createMainRecord(nextId(EventReader.ID_PREFIX), EventReader.TYPE)
			.addChild(child("type", GEDCOMMapper.mapEvent(tag, tag)));
		date(event, "date", dateOf(lds), holder);
		event.addChild(placeCitation(GrampsXML.child(lds, "place")));
		citations(event, lds);
		final String temple = GrampsXML.attribute(GrampsXML.child(lds, "temple"), "val");
		if(temple != null)
			event.addChild(note("Temple", temple, holder));
		final String status = GrampsXML.attribute(GrampsXML.child(lds, "status"), "val");
		if(status != null && !"None".equalsIgnoreCase(status))
			event.addChild(note("Status", status, holder));
		final Element sealed = target(GrampsXML.child(lds, "sealed_to"), "family");
		if(sealed != null)
			event.addChild(note("Sealed to", id(sealed), holder));
		notes(event, lds);
		privacy(event, lds);
		event.addChild(audit(isoOf(holder)));
		model.addRecord(event);

		if(ofFamily){
			participation(event, "group", id(holder), ROLE_PRINCIPAL, null, holder);
			for(final String parentTag : List.of("father", "mother")){
				final Element parent = target(GrampsXML.child(holder, parentTag), "person");
				if(parent != null)
					participation(event, "individual", id(parent), ROLE_PRINCIPAL, null, holder);
			}
		}
		else
			participation(event, "individual", id(holder), ROLE_PRINCIPAL, null, holder);
	}

}
