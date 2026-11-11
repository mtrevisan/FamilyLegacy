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
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
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
import java.util.Set;


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

	private final FLEFModel model;
	private final Map<String, FLEFRecord> individualMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final Map<String, GEDCOMNode> recordsById;
	private final List<PendingEventAssociation> pendingEventAssociations = new ArrayList<>();
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

			final boolean isEvent = (GEDCOMMapper.INDIVIDUAL_EVENT_TAGS.contains(tag) || GEDCOMMapper.isCustomEvent(child));
			if(!isEvent && !GEDCOMMapper.ATTRIBUTE_MAP.containsKey(tag)){
				unmapped.add(child);
				continue;
			}

			final String typeToken = (GEDCOMMapper.TAG_GENERIC_EVENT.equals(tag) || GEDCOMMapper.TAG_GENERIC_FACT.equals(tag)
				? GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(child, GEDCOMMapper.TAG_TYPE))
				: null);
			final String age = GEDCOMHelper.findFirstValue(child, GEDCOMMapper.TAG_AGE);
			if(typeToken != null && GEDCOMMapper.NAME_PART_FACT_TYPES.containsKey(typeToken))
				attachNameFact(individual, child, GEDCOMMapper.NAME_PART_FACT_TYPES.get(typeToken));
			else if(typeToken != null && GEDCOMMapper.ATTRIBUTE_FACT_TYPES.containsKey(typeToken)){
				final FLEFRecord attribute = GEDCOMRecordBuilder.createIndividualAttribute(model, id, child,
					GEDCOMMapper.ATTRIBUTE_FACT_TYPES.get(typeToken), noteRawMap, placeCache);
				hasResidence |= isResidence(attribute);
				ageBirthConverter.track(id, attribute, null, age, child);
			}
			else if(isEvent){
				final FLEFRecord event = GEDCOMRecordBuilder.createEvent(model, child, noteRawMap, placeCache);
				//a citation may state that the individual had a role in the event, other than the main one; but in the birth, the baptism,
				//the death or the burial of the individual the individual is the principal, whatever the citation says: a role that does not
				//agree with it is an error of the file, reported and not kept
				final String citedRole = GEDCOMRecordBuilder.citedEventRole(child);
				final boolean ownEvent = GEDCOMMapper.PRINCIPAL_ONLY_EVENT_TAGS.contains(tag);
				final String participationId = GEDCOMRecordBuilder.attachEventParticipation(event.getId(), AgentReader.TAG_INDIVIDUAL, id,
					(citedRole != null && !ownEvent? citedRole: EventParticipationReader.ENUM_ROLE_PRINCIPAL), age, child, model);
				if(citedRole != null && ownEvent)
					GEDCOMRecordBuilder.reportInconsistentCitedRole(child, citedRole);
				ageBirthConverter.track(id, event, participationId, age, child);
				if(GEDCOMMapper.TAG_CENSUS.equals(tag) && (citedRole == null || EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(citedRole)))
					censuses.add(new CensusEvent(event, child));
				GEDCOMRecordBuilder.attachEventAssociates(event.getId(), child, model, noteRawMap, recordsById);
				if(GEDCOMMapper.TAG_ADOPTION.equals(tag))
					GEDCOMRecordBuilder.attachAdoptiveParents(individual, model, indiNode, child, noteRawMap, recordsById);
			}
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
			if(StringUtils.isBlank(date) || place == null || !done.add(date + "|" + place.getValue()))
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
			residence.addChild(GEDCOMRecordBuilder.createLinkNote("Census", "Derived from the census [" + census.event().getId() + "]("
				+ GEDCOMMapper.FLEF_LINK_PREFIX + census.event().getId() + "): the individual was listed there", census.node()));
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
					.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, "From GEDCOM ALIA"))
					.addChild(AuditBuilder.build(indiNode))));
			return;
		}
		if(aliasId.equals(individual.getId()) || !recordsById.containsKey(aliasId))
			return;

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
		if(associate == null)
			return;

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

		//the events in which each individual is the principal, by type
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
			participation.addChild(GEDCOMRecordBuilder.createNoteStruct("Association", pending.relation(), pending.assoNode()));
			for(final GEDCOMNode noteNode : GEDCOMHelper.findChildren(pending.assoNode(), GEDCOMMapper.TAG_NOTE))
				GEDCOMRecordBuilder.attachNote(participation, model, noteNode, noteRawMap);
			participation.addChild(AuditBuilder.build(pending.indiNode()));

			Deduplicator.getDeduplicatedRecordId(model, participation);
		}
		pendingEventAssociations.clear();
	}

	/** An association whose RELA names an event of the associate, waiting for the events to be there. */
	private record PendingEventAssociation(String individualId, String associateId, String eventType, String relation,
			GEDCOMNode indiNode, GEDCOMNode assoNode){}

}
