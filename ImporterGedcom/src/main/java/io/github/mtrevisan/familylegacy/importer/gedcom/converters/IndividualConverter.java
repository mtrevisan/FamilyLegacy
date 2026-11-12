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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMImportContext;
import io.github.mtrevisan.familylegacy.importer.gedcom.GedcomTexts;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Converts GEDCOM INDI records to FLEF IndividualRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>Names (NAME) → PersonalNameStructure</li>
 *   <li>Sex (SEX) → an IndividualAttributeRecord of type sex</li>
 *   <li>Events (BIRT, DEAT, etc.) → EventRecord + EventParticipationRecord (role principal, with the age at the event); the
 *   individuals associated with the event (ASSO with ROLE, GEDCOM 7) → EventParticipationRecord</li>
 *   <li>Non-events (NO, GEDCOM 7) → note</li>
 *   <li>Attributes (CAST, RESI, OCCU, etc.) → IndividualAttributeRecord</li>
 *   <li>EVEN and FACT whose TYPE is a name (e.g. a family nickname) → PersonalNameStructure, or an attribute (e.g. an
 *   address) → IndividualAttributeRecord</li>
 *   <li>ADOP.FAMC → adoptive_child relationships</li>
 *   <li>FAMC (PEDI, STAT, NOTE) → collected for the parent-child relationships built by {@link FamilyConverter}</li>
 *   <li>OBJE → MediaRecord + preferred_image (if _PRIMARY or _PREF Y) + SourceCitation</li>
 *   <li>Notes (NOTE) → inline note structs</li>
 *   <li>ALIA → IdentityHypothesisRecord (pointer) or alias name (text)</li>
 *   <li>ASSO → RelationshipRecord</li>
 *   <li>Sources (SOUR) → SourceCitation</li>
 *   <li>Privacy (RESN) → PrivacyStructure</li>
 *   <li>Audit (CHAN) → AuditStructure</li>
 * </ul>
 * FAMS is redundant with FAM; SUBM, ANCI, DESI, RFN, AFN, REFN, RIN, UID, EXID, and SDATE have no counterpart.
 */
public class IndividualConverter {

	/** The children that a fact whose value is the individual it is named after can have, without loss. */
	private static final Set<String> NAMED_AFTER_TAGS = Set.of(GEDCOMMapper.TAG_TYPE, GEDCOMMapper.TAG_CONCATENATION,
		GEDCOMMapper.TAG_CONTINUATION);
	/** The children that a membership can have, without loss. */
	private static final Set<String> MEMBERSHIP_TAGS = Set.of(GEDCOMMapper.TAG_TYPE, GEDCOMMapper.TAG_CONCATENATION,
		GEDCOMMapper.TAG_CONTINUATION, GEDCOMMapper.TAG_DATE, GEDCOMMapper.TAG_SOURCE, GEDCOMMapper.TAG_NOTE);
	/** The children that a twin or a midwife can have, without loss. */
	private static final Set<String> LINK_FACT_TAGS = Set.of(GEDCOMMapper.TAG_TYPE, GEDCOMMapper.TAG_CONCATENATION,
		GEDCOMMapper.TAG_CONTINUATION, GEDCOMMapper.TAG_SOURCE, GEDCOMMapper.TAG_NOTE);
	/** A word of a name. */
	private static final Pattern PATTERN_WORD = Pattern.compile("[\\p{L}\\p{N}]+");
	/** The shortest word of a name that tells it from another (the others are particles, as {@code to}). */
	private static final int MIN_WORD_LENGTH = 3;
	/** The number of the words of a full date (day, month, year). */
	private static final int FULL_DATE_WORDS = 3;

	private final FLEFModel model;
	private final Map<String, FLEFRecord> individualMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final Map<String, GEDCOMNode> recordsById;
	private final List<PendingEventAssociation> pendingEventAssociations = new ArrayList<>();
	private final List<PendingFact> pendingFacts = new ArrayList<>();
	/** The individuals made from the name of a midwife, by name. */
	private final Map<String, String> midwifeIds = new HashMap<>();
	private final Map<String, GEDCOMNode> childFamilyLinks;
	private final PlaceCache placeCache;
	private final AgeBirthConverter ageBirthConverter;


	/**
	 * Constructor.
	 *
	 * @param model          the FLEF model
	 * @param individualMap  map of individual IDs to FLEF records
	 * @param recordsById    the GEDCOM records, by id
	 * @param childFamilyLinks	filled with the FAMC of each individual, by {@code individualId|familyId}
	 * @param placeCache     cache for place records
	 */
	public IndividualConverter(FLEFModel model, Map<String, FLEFRecord> individualMap, Map<String, GEDCOMNode> noteRawMap,
			Map<String, GEDCOMNode> recordsById, Map<String, GEDCOMNode> childFamilyLinks, PlaceCache placeCache,
			AgeBirthConverter ageBirthConverter){
		this.model = model;
		this.individualMap = individualMap;
		this.noteRawMap = noteRawMap;
		this.recordsById = recordsById;
		this.childFamilyLinks = childFamilyLinks;
		this.placeCache = placeCache;
		this.ageBirthConverter = ageBirthConverter;
	}

	/** The key of a child-to-family link. */
	static String childFamilyKey(final String individualId, final String familyId){
		return individualId + "|" + familyId;
	}

	/**
	 * Converts a GEDCOM INDI node into an FLEF IndividualRecord.
	 *
	 * @param indiNode the GEDCOM node with tag "INDI"
	 */
	public void convert(GEDCOMNode indiNode){
		String id = GEDCOMHelper.extractId(indiNode, IndividualReader.ID_PREFIX);
		FLEFRecord individual = FLEFRecord.createMainRecord(id, IndividualReader.TYPE);
		individualMap.put(id, individual);
		GEDCOMImportContext.setRecord(id);
		final List<GEDCOMNode> unmapped = new ArrayList<>();

		// ---- Names ----
		for(GEDCOMNode nameNode : GEDCOMHelper.findChildren(indiNode, GEDCOMMapper.TAG_NAME)){
			GEDCOMRecordBuilder.attachPersonalNameStructure(individual, nameNode, model, noteRawMap);
		}

		// ---- Sex ----
		GEDCOMNode sexNode = GEDCOMHelper.findFirstChild(indiNode, GEDCOMMapper.TAG_SEX);
		//the sex X (neither male nor female) is merged into unknown, and kept as a note
		boolean sexX = false;
		if(sexNode != null && sexNode.getValue() != null){
			sexX = "X".equalsIgnoreCase(sexNode.getValue().trim());
			//the sex is an assertion of the file (with its own citations and notes): an attribute of the individual
			final FLEFRecord sex = GEDCOMRecordBuilder.createIndividualAttribute(model, id, sexNode, IndividualAttributeReader.ENUM_TYPE_SEX,
				noteRawMap, placeCache);
			FLEFRecordHelper.updateChildValue(sex, IndividualAttributeReader.TAG_VALUE, GEDCOMMapper.mapSex(sexNode.getValue()));
		}

		// ---- Events and attributes, in the order of the file ----
		boolean hasResidence = false;
		final List<CensusEvent> censuses = new ArrayList<>();
		final List<FLEFRecord> factNotes = new ArrayList<>();
		for(GEDCOMNode child : indiNode.getChildren()){
			final String tag = child.getTag();
			if("NO".equals(tag)){
				GEDCOMRecordBuilder.createNonEventActivity(model, AgentReader.TAG_INDIVIDUAL, id, child, noteRawMap);
				continue;
			}
			if(GEDCOMMapper.TAG_MARRIED_NAME.equalsIgnoreCase(tag)){
				attachCustomName(individual, child, NameReader.ENUM_PERSONAL_TYPE_MARRIED);
				continue;
			}
			if(GEDCOMMapper.TAG_AKA.equalsIgnoreCase(tag)){
				attachCustomName(individual, child, NameReader.ENUM_PERSONAL_TYPE_ALIAS);
				continue;
			}
			if(GEDCOMMapper.TAG_TODO.equalsIgnoreCase(tag)){
				GEDCOMRecordBuilder.createResearchTask(model, child, noteRawMap, AgentReader.TAG_INDIVIDUAL, id);
				continue;
			}

			final boolean isEvent = (GEDCOMMapper.INDIVIDUAL_EVENT_TAGS.contains(tag) || GEDCOMHelper.isCustomEvent(child));
			if(!isEvent && !GEDCOMMapper.ATTRIBUTE_MAP.containsKey(tag)){
				unmapped.add(child);
				continue;
			}

			final String typeToken = (GEDCOMMapper.TAG_GENERIC_EVENT.equals(tag) || GEDCOMMapper.TAG_GENERIC_FACT.equals(tag)
				? GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(child, GEDCOMMapper.TAG_TYPE))
				: null);
			final String age = GEDCOMHelper.findFirstValue(child, GEDCOMMapper.TAG_AGE);
			if(isEvent && typeToken != null && attachCustomFact(individual, indiNode, child, typeToken, age, factNotes))
				continue;

			if(typeToken != null && GEDCOMMapper.NAME_PART_FACT_TYPES.containsKey(typeToken))
				attachNameFact(individual, child, GEDCOMMapper.NAME_PART_FACT_TYPES.get(typeToken));
			else if(typeToken != null && GEDCOMMapper.ATTRIBUTE_FACT_TYPES.containsKey(typeToken)){
				final FLEFRecord attribute = GEDCOMRecordBuilder.createIndividualAttribute(model, id, child,
					GEDCOMMapper.ATTRIBUTE_FACT_TYPES.get(typeToken), noteRawMap, placeCache);
				//the file said more than the FLEF type (a retirement home is a residence, but not any residence): what it said is kept
				if(GEDCOMMapper.LABELLED_ATTRIBUTE_FACT_TYPES.contains(typeToken))
					addBeforeAudit(attribute, NoteBuilder.createNoteStruct(GedcomTexts.t("title.type"),
						GEDCOMHelper.findFirstValue(child, GEDCOMMapper.TAG_TYPE), child));
				hasResidence |= isResidence(attribute);
				ageBirthConverter.track(id, attribute, null, age, child);
			}
			else if(isEvent)
				attachEvent(individual, indiNode, child, age, censuses);
			else{
				final FLEFRecord attribute = GEDCOMRecordBuilder.createIndividualAttribute(model, id, child, null, noteRawMap, placeCache);
				hasResidence |= isResidence(attribute);
				ageBirthConverter.track(id, attribute, null, age, child);
			}
		}
		if(!hasResidence)
			deriveResidences(id, censuses);

		// ---- Sources (SOUR) ----
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(indiNode, GEDCOMMapper.TAG_SOURCE)){
			GEDCOMRecordBuilder.attachSource(individual, model, sourNode, noteRawMap);
		}

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(indiNode, GEDCOMMapper.TAG_NOTE)){
			GEDCOMRecordBuilder.attachNote(individual, model, noteNode, noteRawMap);
		}
		for(final FLEFRecord factNote : factNotes)
			individual.addChild(factNote);
		// what has no field in FLEF is kept as notes
		KeptNotes.keepIdentifiers(individual, indiNode, StringUtils.EMPTY, indiNode);
		if(sexX)
			KeptNotes.add(individual, KeptNotes.TITLE_SEX, KeptNotes.SEX_X_TEXT, indiNode);
		for(final GEDCOMNode child : unmapped)
			KeptNotes.keepUnmappedChild(individual, child, GEDCOMMapper.INDIVIDUAL_CONSUMED_TAGS, StringUtils.EMPTY, indiNode);

		// Multimedia (OBJE)
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(indiNode, GEDCOMMapper.TAG_MULTIMEDIA)){
			GEDCOMRecordBuilder.attachMultimediaLink(individual, model, multimediaLinkNode, noteRawMap);
		}

		// ---- FAMC (child to family link): the relationships are created with the family ----
		for(GEDCOMNode famcNode : GEDCOMHelper.findChildren(indiNode, GEDCOMMapper.TAG_FAMILY_CHILD)){
			final String familyId = GEDCOMHelper.pointer(famcNode.getValue());
			if(familyId != null)
				childFamilyLinks.putIfAbsent(childFamilyKey(id, familyId), famcNode);
		}

		// ---- Aliases and associations ----
		for(GEDCOMNode aliaNode : GEDCOMHelper.findChildren(indiNode, "ALIA"))
			attachAlias(individual, indiNode, aliaNode);
		for(GEDCOMNode assoNode : GEDCOMHelper.findChildren(indiNode, GEDCOMMapper.TAG_ASSOCIATION))
			attachAssociation(id, indiNode, assoNode);

		// ---- Privacy (RESN) ----
		GEDCOMRecordBuilder.attachRestriction(individual, indiNode);

		// ---- Audit ----
		individual.addChild(AuditBuilder.build(indiNode));

		model.addRecord(individual);
	}

	/** A census of the individual, with the GEDCOM node it comes from. */
	private record CensusEvent(FLEFRecord event, GEDCOMNode node){}

	/** A fact of the individual that links it to another individual, waiting for all the individuals to be converted. */
	private record PendingFact(FLEFRecord individual, GEDCOMNode indiNode, GEDCOMNode node, String type, String value, String age){}

	/** Converts an event of the individual: the event, the participation of the individual in it, and what comes with it. */
	private void attachEvent(final FLEFRecord individual, final GEDCOMNode indiNode, final GEDCOMNode node, final String age,
			final List<CensusEvent> censuses){
		final String id = individual.getId();
		final String tag = node.getTag();
		final FLEFRecord event = GEDCOMRecordBuilder.createEvent(model, node, noteRawMap, placeCache);
		//a citation may state that the individual had a role in the event, other than the main one; but in the birth, the baptism,
		//the death or the burial of the individual the individual is the principal, whatever the citation says: a role that does not
		//agree with it is an error of the file, reported and not kept
		final String citedRole = GEDCOMRecordBuilder.citedEventRole(node);
		final boolean ownEvent = GEDCOMMapper.PRINCIPAL_ONLY_EVENT_TAGS.contains(tag);
		final String participationId = GEDCOMRecordBuilder.attachEventParticipation(event.getId(), AgentReader.TAG_INDIVIDUAL, id,
			(citedRole != null && !ownEvent? citedRole: EventParticipationReader.ENUM_ROLE_PRINCIPAL), age, node, model);
		if(citedRole != null && ownEvent)
			GEDCOMRecordBuilder.reportInconsistentCitedRole(node, citedRole);
		ageBirthConverter.track(id, event, participationId, age, node);
		if(GEDCOMMapper.TAG_CENSUS.equals(tag) && (citedRole == null || EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(citedRole)))
			censuses.add(new CensusEvent(event, node));
		GEDCOMRecordBuilder.attachEventAssociates(event.getId(), node, model, noteRawMap, recordsById);
		if(GEDCOMMapper.TAG_ADOPTION.equals(tag))
			GEDCOMRecordBuilder.attachAdoptiveParents(individual, model, indiNode, node, noteRawMap, recordsById);
	}

	/** Puts a child of a record before its audit, the last field of a record; a {@code null} child is ignored. */
	private static void addBeforeAudit(final FLEFRecord record, final FLEFRecord child){
		if(child == null)
			return;

		final List<FLEFRecord> children = record.getChildren();
		children.add(Math.max(children.size() - 1, 0), child);
	}

	/** @return whether all the children of the node have one of the given tags */
	private static boolean hasOnlyChildren(final GEDCOMNode node, final Set<String> tags){
		for(final GEDCOMNode child : node.getChildren())
			if(!tags.contains(child.getTag()))
				return false;
		return true;
	}

	/**
	 * Converts an EVEN or FACT whose TYPE says that the value is about another individual, a group or the family of the individual, to
	 * what FLEF has for it, if the fact has nothing that the conversion would lose (a date, a place, ...) and a value that tells what it
	 * is about; otherwise it is left to be an event.
	 *
	 * @return	Whether the fact is converted (or will be, once all the individuals are there).
	 */
	private boolean attachCustomFact(final FLEFRecord individual, final GEDCOMNode indiNode, final GEDCOMNode node, final String type,
			final String age, final List<FLEFRecord> factNotes){
		final String value = (!GEDCOMHelper.isFlag(node.getValue())? StringUtils.trimToNull(GEDCOMHelper.extractFullText(node)): null);
		if(value == null)
			return false;

		switch(type){
			case GEDCOMMapper.FACT_TYPE_NAMED_AFTER -> {
				if(!hasOnlyChildren(node, NAMED_AFTER_TAGS))
					return false;

				factNotes.add(NoteBuilder.createNoteStruct(GedcomTexts.t("title.named_after"), value, node));
				return true;
			}
			case GEDCOMMapper.FACT_TYPE_MEMBERSHIP -> {
				if(!hasOnlyChildren(node, MEMBERSHIP_TAGS))
					return false;

				attachMembership(individual.getId(), indiNode, node, value);
				return true;
			}
			case GEDCOMMapper.FACT_TYPE_TWIN, GEDCOMMapper.FACT_TYPE_MIDWIFE -> {
				if(!hasOnlyChildren(node, LINK_FACT_TAGS))
					return false;

				pendingFacts.add(new PendingFact(individual, indiNode, node, type, value, age));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/** Converts a membership of the individual: the group of the given name (the same for the same name), and the individual in it. */
	private void attachMembership(final String individualId, final GEDCOMNode indiNode, final GEDCOMNode node, final String groupName){
		final FLEFRecord group = FLEFRecord.createMainRecord(IDGenerator.nextId(GroupReader.ID_PREFIX), GroupReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(GroupReader.TAG_TYPE, GroupReader.ENUM_TYPE_ASSOCIATION))
			.addChild(FLEFRecord.createChildWithTag(GroupReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, groupName)))
			.addChild(AuditBuilder.build(node));
		final String groupId = Deduplicator.getDeduplicatedRecordId(model, group);

		final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX),
				RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_GROUP, groupId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		GEDCOMRecordBuilder.attachValidity(relationship, GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_DATE));
		for(final GEDCOMNode sourNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_SOURCE))
			GEDCOMRecordBuilder.attachSource(relationship, model, sourNode, noteRawMap);
		for(final GEDCOMNode noteNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_NOTE))
			GEDCOMRecordBuilder.attachNote(relationship, model, noteNode, noteRawMap);
		relationship.addChild(AuditBuilder.build(indiNode));

		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	private static boolean isResidence(final FLEFRecord attribute){
		return IndividualAttributeReader.ENUM_TYPE_RESIDENCE.equals(FLEFRecordHelper.getChildValue(attribute,
			IndividualAttributeReader.TAG_TYPE));
	}

	/**
	 * Makes a residence of each census of an individual that has no residence at all: a census lists the people where they lived,
	 * so the place of the census, at the date of the census, is a residence of the individual.
	 * <p>
	 * It is done only for a census of the individual itself (not a witness), with both a place and a date; the residence has the
	 * same date, place and sources of the census, and a note that links to it. Two censuses with the same date and place give one
	 * residence. Nothing else is inferred (no continuity between two censuses).
	 */
	private void deriveResidences(final String individualId, final List<CensusEvent> censuses){
		final Set<String> done = new HashSet<>();
		for(final CensusEvent census : censuses){
			final String date = GEDCOMHelper.findFirstValue(census.node(), GEDCOMMapper.TAG_DATE);
			final FLEFRecord place = FLEFRecordHelper.findChild(census.event(), EventReader.TAG_PLACE);
			if(StringUtils.isBlank(date) || place == null
					|| !done.add(date + "|" + FLEFRecordHelper.getChildValue(place, PlaceCitationReader.TAG_PLACE)))
				continue;

			final FLEFRecord residence = FLEFRecord.createMainRecord(IDGenerator.nextId(IndividualAttributeReader.ID_PREFIX),
					IndividualAttributeReader.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individualId))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE,
					IndividualAttributeReader.ENUM_TYPE_RESIDENCE));
			GEDCOMRecordBuilder.attachValidity(residence, date);
			final FLEFRecord placeCopy = FLEFRecord.createEmpty();
			place.deepCopyTo(placeCopy);
			residence.addChild(placeCopy);
			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(census.event(), IndividualAttributeReader.TAG_SOURCE)){
				final FLEFRecord copy = FLEFRecord.createEmpty();
				citation.deepCopyTo(copy);
				residence.addChild(copy);
			}
			residence.addChild(NoteBuilder.createLinkNote(GedcomTexts.t("title.census"), GedcomTexts.tf("text.census_derived", census.event().getId(), GEDCOMMapper.FLEF_LINK_PREFIX), census.node()));
			residence.addChild(AuditBuilder.build(census.node()));
			model.addRecord(residence);
		}
	}

	/**
	 * Converts an EVEN or FACT that holds a name (e.g. {@code 1 EVEN Dorato /do.ˈra.to/ 2 TYPE Family nickname}) into a
	 * PersonalNameStructure made of one part; a transcription between slashes becomes its phonetic variant.
	 */
	private void attachNameFact(final FLEFRecord individual, final GEDCOMNode node, final String partType){
		final String value = StringUtils.trimToNull(node.getValue());
		if(value == null)
			return;

		final FLEFRecord name = FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PERSONAL_TYPE_NICKNAME))
			.addChild(GEDCOMRecordBuilder.createNamePart(partType, value));
		GEDCOMRecordBuilder.attachValidity(name, GEDCOMHelper.findFirstValue(node, GEDCOMMapper.TAG_DATE));
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_SOURCE))
			GEDCOMRecordBuilder.attachSource(name, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_NOTE))
			GEDCOMRecordBuilder.attachNote(name, model, noteNode, noteRawMap);
		individual.addChild(name);
	}

	/**
	 * Converts a custom name tag ({@code _MARNM}, {@code _AKA}) with a name value into a PersonalNameStructure of the given type,
	 * split into parts as a NAME is.
	 */
	private void attachCustomName(final FLEFRecord individual, final GEDCOMNode node, final String type){
		final FLEFRecord name = GEDCOMRecordBuilder.createPersonalName(node.getValue(), node, type);
		if(name == null)
			return;

		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_SOURCE))
			GEDCOMRecordBuilder.attachSource(name, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(node, GEDCOMMapper.TAG_NOTE))
			GEDCOMRecordBuilder.attachNote(name, model, noteNode, noteRawMap);
		individual.addChild(name);
	}

	/**
	 * Converts an ALIA: a pointer to another individual becomes an identity hypothesis between the two, a text becomes a name of
	 * type {@code alias}.
	 */
	private void attachAlias(final FLEFRecord individual, final GEDCOMNode indiNode, final GEDCOMNode aliaNode){
		final String aliasId = GEDCOMHelper.pointer(aliaNode.getValue());
		if(aliasId == null){
			final FLEFRecord name = GEDCOMRecordBuilder.createPersonalName(aliaNode.getValue(), null, NameReader.ENUM_PERSONAL_TYPE_ALIAS);
			if(name != null)
				individual.addChild(name.addChild(FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
					.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, GedcomTexts.t("text.from_alia")))
					.addChild(AuditBuilder.build(indiNode))));
			return;
		}
		if(aliasId.equals(individual.getId()))
			return;
		if(!recordsById.containsKey(aliasId)){
			GEDCOMImportContext.discard(ImportReasons.OTHER, aliaNode, StringUtils.EMPTY);
			return;
		}

		final FLEFRecord identityHypothesis = FLEFRecord.createMainRecord(IDGenerator.nextId(IdentityHypothesisReader.ID_PREFIX),
				IdentityHypothesisReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IdentityHypothesisReader.TAG_IDENTITY)
				.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TAG_IDENTITY_INDIVIDUAL, individual.getId())))
			.addChild(FLEFRecord.createChildWithTag(IdentityHypothesisReader.TAG_IDENTITY)
				.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TAG_IDENTITY_INDIVIDUAL, aliasId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(IdentityHypothesisReader.TAG_RATIONALE, "Linked as alias (ALIA) in the GEDCOM file"))
			.addChild(AuditBuilder.build(indiNode));

		// check for duplicates before adding (A ALIA B and B ALIA A give the same hypothesis)
		Deduplicator.getDeduplicatedRecordId(model, identityHypothesis);
	}

	/*
	n ASSO @<XREF:INDI>@
	  +1 TYPE <RECORD_TYPE>    (GEDCOM 5.5)
	  +1 RELA <RELATION_IS_DESCRIPTOR>
	  +1 ROLE <Enum>    (GEDCOM 7, instead of RELA)
	  +1 <<SOURCE_CITATION>>
	  +1 <<NOTE_STRUCTURE>>
	*/
	private void attachAssociation(final String individualId, final GEDCOMNode indiNode, final GEDCOMNode assoNode){
		final String associateId = GEDCOMHelper.pointer(assoNode.getValue());
		final GEDCOMNode associate = recordsById.get(associateId);
		if(associate == null){
			GEDCOMImportContext.discard(ImportReasons.OTHER, assoNode, StringUtils.EMPTY);
			return;
		}

		final String relation = ObjectUtils.firstNonNull(GEDCOMHelper.findFirstValue(assoNode, "RELA"),
			GEDCOMMapper.mapAssociationRoleLabel(GEDCOMHelper.findFirstValue(assoNode, GEDCOMMapper.TAG_ROLE)));
		final String relationToken = GEDCOMMapper.toTypeToken(relation);
		final String type = (relationToken != null && ArrayUtils.contains(RelationshipReader.TYPES, relationToken)
			? relationToken
			: RelationshipReader.ENUM_TYPE_ASSOCIATE);
		//a family relationship ("1 ASSO @I2@ 2 RELA child") that the other side contradicts with another kind of link ("1 ASSO @I1@
		//2 RELA midwife": I2 is the baby that I1 delivered, not a child of I1) is an error of the file: the other side is kept
		if(RelationshipReader.isTypeChild(type) && !"FAM".equals(associate.getTag())
				&& saysAnotherKindOfLink(associate, individualId)){
			GEDCOMImportContext.discard(ImportReasons.INCONSISTENT_ASSOCIATION, assoNode, StringUtils.EMPTY);
			return;
		}
		//a RELA that is the name of an event ("1 ASSO @I2@ 2 RELA birth") says that the individual took part in that event of the
		//associate, as a witness: it is settled once all the individuals are converted, as the event may not be there yet
		if(relationToken != null && ArrayUtils.contains(EventReader.TYPES, relationToken) && !"FAM".equals(associate.getTag())){
			pendingEventAssociations.add(new PendingEventAssociation(individualId, associateId, relationToken, relation, indiNode, assoNode));
			return;
		}
		createAssociation(individualId, indiNode, assoNode, associateId, associate, relation, relationToken, type);
	}

	/** @return whether the associate has an association to the individual whose RELA is not a family relationship (a child, a spouse) */
	private static boolean saysAnotherKindOfLink(final GEDCOMNode associate, final String individualId){
		for(GEDCOMNode reverse : GEDCOMHelper.findChildren(associate, GEDCOMMapper.TAG_ASSOCIATION)){
			if(!individualId.equals(GEDCOMHelper.pointer(reverse.getValue())))
				continue;

			final String token = GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(reverse, "RELA"));
			if(token != null && !RelationshipReader.isTypeChild(token) && !RelationshipReader.isTypePartner(token))
				return true;
		}
		return false;
	}

	/** Makes a plain association (a RelationshipRecord) between two individuals, or an individual and a group. */
	private void createAssociation(final String individualId, final GEDCOMNode indiNode, final GEDCOMNode assoNode,
			final String associateId, final GEDCOMNode associate, final String relation, final String relationToken, final String type){
		//RELA states the relation of the associate to the individual ("1 ASSO @I2@ 2 RELA Godfather": I2 is the godfather),
		//and the subject of a relationship is the one whose role is described; a group can only be the target
		final boolean associateIsGroup = "FAM".equals(associate.getTag());
		final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX),
				RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, (associateIsGroup? individualId: associateId))))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(associateIsGroup
					? FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_GROUP, associateId)
					: FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type));
		if(relation != null && !type.equals(relationToken))
			relationship.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, relation));

		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(assoNode, GEDCOMMapper.TAG_SOURCE))
			GEDCOMRecordBuilder.attachSource(relationship, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(assoNode, GEDCOMMapper.TAG_NOTE))
			GEDCOMRecordBuilder.attachNote(relationship, model, noteNode, noteRawMap);
		relationship.addChild(AuditBuilder.build(indiNode));

		// check for duplicates before adding
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/**
	 * Settles the associations whose RELA names an event of the associate ("1 ASSO @I2@ 2 RELA birth"): the individual is a witness
	 * of that event of the associate. It is to be called once all the individuals (and the families) are converted. When the associate
	 * has no such event, the association stays an association.
	 */
	public void resolveEventAssociations(){
		if(pendingEventAssociations.isEmpty())
			return;

		final Map<String, String> principalEvents = principalEvents();
		for(final PendingEventAssociation pending : pendingEventAssociations){
			GEDCOMImportContext.setRecord(pending.individualId());
			final String eventId = principalEvents.get(pending.associateId() + "|" + pending.eventType());
			if(eventId == null){
				final GEDCOMNode associate = recordsById.get(pending.associateId());
				createAssociation(pending.individualId(), pending.indiNode(), pending.assoNode(), pending.associateId(), associate,
					pending.relation(), pending.eventType(), RelationshipReader.ENUM_TYPE_ASSOCIATE);
				continue;
			}

			final FLEFRecord participation = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationReader.ID_PREFIX),
					EventParticipationReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
					.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, pending.individualId())))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE,
					EventParticipationReader.ENUM_ROLE_WITNESS));
			for(final GEDCOMNode sourNode : GEDCOMHelper.findChildren(pending.assoNode(), GEDCOMMapper.TAG_SOURCE))
				GEDCOMRecordBuilder.attachSource(participation, model, sourNode, noteRawMap);
			//what the file wrote is kept: the witness is an interpretation of it
			participation.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.association"), pending.relation(), pending.assoNode()));
			for(final GEDCOMNode noteNode : GEDCOMHelper.findChildren(pending.assoNode(), GEDCOMMapper.TAG_NOTE))
				GEDCOMRecordBuilder.attachNote(participation, model, noteNode, noteRawMap);
			participation.addChild(AuditBuilder.build(pending.indiNode()));

			Deduplicator.getDeduplicatedRecordId(model, participation);
		}
		pendingEventAssociations.clear();
	}

	/** @return the events in which each individual is the principal, by {@code individualId|type} */
	private Map<String, String> principalEvents(){
		final Map<String, String> principalEvents = new HashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			final String individualId = FLEFRecordHelper.getChildValue(participation, FLEFRecordHelper.composePath(
				EventParticipationReader.TAG_PARTICIPANT, AgentReader.TAG_INDIVIDUAL));
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(individualId == null || eventId == null || (role != null && !EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(role)))
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			final String type = (event != null? FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE): null);
			if(type != null)
				principalEvents.putIfAbsent(individualId + "|" + type, eventId);
		}
		return principalEvents;
	}

	/**
	 * Settles the facts that link the individual to another individual (a twin, a midwife), whose value is the name of the other one:
	 * it is to be called once all the individuals (and the events) are converted. A fact is converted only if the other individual
	 * is certain; otherwise the fact stays an event, with what the file says.
	 * <p>
	 * A twin is the one individual who is a child of the same family and was born the same day, and whose name shares a word with the
	 * value. A midwife is a participation in the birth of the individual, with the role of midwife; as the midwife is hardly in the
	 * file, she is made an individual with the name written (the same one for the same name).
	 */
	public void resolveFacts(){
		if(pendingFacts.isEmpty())
			return;

		final Map<String, String> principalEvents = principalEvents();
		for(final PendingFact fact : pendingFacts){
			GEDCOMImportContext.setRecord(fact.individual().getId());
			final boolean done = (GEDCOMMapper.FACT_TYPE_TWIN.equals(fact.type())
				? attachTwin(fact)
				: attachMidwife(fact, principalEvents));
			if(!done)
				attachEvent(fact.individual(), fact.indiNode(), fact.node(), fact.age(), new ArrayList<>());
		}
		pendingFacts.clear();
	}

	/** @return whether the twin was found, and the association made */
	private boolean attachTwin(final PendingFact fact){
		final String individualId = fact.individual().getId();
		final GEDCOMNode self = fact.indiNode();
		final String birth = fullBirthDate(self);
		final Set<String> words = nameWords(fact.value());
		if(birth == null || words.isEmpty())
			return false;

		//the siblings: the children of the families the individual is a child of
		final Set<String> siblingIds = new HashSet<>();
		for(final GEDCOMNode famc : GEDCOMHelper.findChildren(self, GEDCOMMapper.TAG_FAMILY_CHILD)){
			final GEDCOMNode family = recordsById.get(GEDCOMHelper.pointer(famc.getValue()));
			if(family == null)
				continue;

			for(final GEDCOMNode chil : GEDCOMHelper.findChildren(family, GEDCOMMapper.TAG_CHILD)){
				final String siblingId = GEDCOMHelper.pointer(chil.getValue());
				if(siblingId != null && !siblingId.equals(individualId))
					siblingIds.add(siblingId);
			}
		}

		String twinId = null;
		for(final String siblingId : siblingIds){
			final GEDCOMNode sibling = recordsById.get(siblingId);
			if(sibling == null || !birth.equals(fullBirthDate(sibling)) || !sharesWord(sibling, words))
				continue;

			//two candidates are not a certain one
			if(twinId != null)
				return false;
			twinId = siblingId;
		}
		if(twinId == null)
			return false;

		createAssociation(individualId, fact.indiNode(), fact.node(), twinId, recordsById.get(twinId), GEDCOMMapper.FACT_TYPE_TWIN,
			GEDCOMMapper.FACT_TYPE_TWIN, RelationshipReader.ENUM_TYPE_ASSOCIATE);
		return true;
	}

	/** @return whether the midwife was added to the birth of the individual */
	private boolean attachMidwife(final PendingFact fact, final Map<String, String> principalEvents){
		final String birthEventId = principalEvents.get(fact.individual().getId() + "|" + EventReader.ENUM_TYPE_BIRTH);
		if(birthEventId == null)
			return false;

		//a trailing detail between parentheses (as the birth of the midwife) is not part of her name, but it is kept
		final String name = StringUtils.defaultIfBlank(StringUtils.trim(StringUtils.substringBefore(fact.value(), "(")), fact.value());
		final String midwifeId = midwifeIds.computeIfAbsent(StringUtils.lowerCase(StringUtils.normalizeSpace(name), Locale.ROOT), key -> {
			final FLEFRecord nameStructure = GEDCOMRecordBuilder.createPersonalName(name, null, null);
			if(nameStructure == null)
				return null;

			final FLEFRecord midwife = FLEFRecord.createMainRecord(IDGenerator.nextId(IndividualReader.ID_PREFIX), IndividualReader.TYPE)
				.addChild(nameStructure)
				.addChild(AuditBuilder.build(fact.node()));
			model.addRecord(midwife);
			return midwife.getId();
		});
		if(midwifeId == null)
			return false;

		final FLEFRecord participation = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationReader.ID_PREFIX),
				EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, midwifeId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, birthEventId))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, GEDCOMMapper.FACT_TYPE_MIDWIFE));
		for(final GEDCOMNode sourNode : GEDCOMHelper.findChildren(fact.node(), GEDCOMMapper.TAG_SOURCE))
			GEDCOMRecordBuilder.attachSource(participation, model, sourNode, noteRawMap);
		if(!name.equals(fact.value()))
			participation.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.midwife"), fact.value(), fact.node()));
		for(final GEDCOMNode noteNode : GEDCOMHelper.findChildren(fact.node(), GEDCOMMapper.TAG_NOTE))
			GEDCOMRecordBuilder.attachNote(participation, model, noteNode, noteRawMap);
		participation.addChild(AuditBuilder.build(fact.indiNode()));

		Deduplicator.getDeduplicatedRecordId(model, participation);
		return true;
	}

	/** @return the date of the birth of the individual, if it is a full date (day, month and year), as a comparable text; else {@code null} */
	private static String fullBirthDate(final GEDCOMNode individual){
		final String date = StringUtils.upperCase(StringUtils.normalizeSpace(GEDCOMHelper.findFirstValue(
			GEDCOMHelper.findFirstChild(individual, GEDCOMMapper.TAG_BIRTH), GEDCOMMapper.TAG_DATE)), Locale.ROOT);
		if(date == null)
			return null;

		final String[] words = StringUtils.split(date);
		return (words.length == FULL_DATE_WORDS && StringUtils.isNumeric(words[0])? date: null);
	}

	/** @return whether a name of the individual has one of the words */
	private static boolean sharesWord(final GEDCOMNode individual, final Set<String> words){
		for(final GEDCOMNode name : GEDCOMHelper.findChildren(individual, GEDCOMMapper.TAG_NAME))
			for(final String word : nameWords(name.getValue()))
				if(words.contains(word))
					return true;
		return false;
	}

	/** @return the words of a name that tell it from another one, in lower case and without accents */
	private static Set<String> nameWords(final String name){
		final Set<String> words = new HashSet<>();
		if(name == null)
			return words;

		final Matcher matcher = PATTERN_WORD.matcher(StringUtils.stripAccents(name));
		while(matcher.find())
			if(matcher.group().length() >= MIN_WORD_LENGTH)
				words.add(matcher.group().toLowerCase(Locale.ROOT));
		return words;
	}

	/** An association whose RELA names an event of the associate, waiting for the events to be there. */
	private record PendingEventAssociation(String individualId, String associateId, String eventType, String relation,
			GEDCOMNode indiNode, GEDCOMNode assoNode){}

}
