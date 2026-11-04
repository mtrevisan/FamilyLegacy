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

import io.github.mtrevisan.familylegacy.importer.gedcom.AuditBuilder;
import io.github.mtrevisan.familylegacy.importer.gedcom.Deduplicator;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.IDGenerator;
import io.github.mtrevisan.familylegacy.importer.gedcom.PlaceCache;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;


/**
 * Converts GEDCOM INDI records to FLEF IndividualRecord.
 * <p>
 * Handles:
 * <ul>
 *   <li>Names (NAME) → PersonalNameStructure</li>
 *   <li>Sex (SEX) → sex</li>
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
	private final Map<String, GEDCOMNode> childFamilyLinks;
	private final PlaceCache placeCache;


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
			Map<String, GEDCOMNode> recordsById, Map<String, GEDCOMNode> childFamilyLinks, PlaceCache placeCache){
		this.model = model;
		this.individualMap = individualMap;
		this.noteRawMap = noteRawMap;
		this.recordsById = recordsById;
		this.childFamilyLinks = childFamilyLinks;
		this.placeCache = placeCache;
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

		// ---- Names ----
		for(GEDCOMNode nameNode : GEDCOMHelper.findChildren(indiNode, "NAME")){
			GEDCOMHelper.attachPersonalNameStructure(individual, nameNode, model, noteRawMap);
		}

		// ---- Sex ----
		GEDCOMNode sexNode = GEDCOMHelper.findFirstChild(indiNode, "SEX");
		if(sexNode != null && sexNode.getValue() != null){
			individual.addChild(FLEFRecord.createChildWithTagAndValue("sex", GEDCOMMapper.mapSex(sexNode.getValue())));
		}

		// ---- Events and attributes, in the order of the file ----
		for(GEDCOMNode child : indiNode.getChildren()){
			final String tag = child.getTag();
			if("NO".equals(tag)){
				GEDCOMHelper.attachNonEvent(individual, child, model, noteRawMap);
				continue;
			}

			final boolean isEvent = GEDCOMMapper.INDIVIDUAL_EVENT_TAGS.contains(tag);
			if(!isEvent && !GEDCOMMapper.ATTRIBUTE_MAP.containsKey(tag))
				continue;

			final String typeToken = (GEDCOMMapper.TAG_GENERIC_EVENT.equals(tag) || GEDCOMMapper.TAG_GENERIC_FACT.equals(tag)
				? GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(child, "TYPE"))
				: null);
			if(typeToken != null && GEDCOMMapper.NAME_PART_FACT_TYPES.containsKey(typeToken))
				attachNameFact(individual, child, GEDCOMMapper.NAME_PART_FACT_TYPES.get(typeToken));
			else if(typeToken != null && GEDCOMMapper.ATTRIBUTE_FACT_TYPES.containsKey(typeToken))
				GEDCOMHelper.createIndividualAttribute(model, id, child, GEDCOMMapper.ATTRIBUTE_FACT_TYPES.get(typeToken), noteRawMap,
					placeCache);
			else if(isEvent){
				final FLEFRecord event = GEDCOMHelper.createEvent(model, child, noteRawMap, placeCache);
				GEDCOMHelper.attachEventParticipation(event.getId(), "individual", id, "principal",
					GEDCOMHelper.findFirstValue(child, "AGE"), child, model);
				GEDCOMHelper.attachEventAssociates(event.getId(), child, model, noteRawMap, recordsById);
				if("ADOP".equals(tag))
					GEDCOMHelper.attachAdoptiveParents(individual, model, indiNode, child, noteRawMap, recordsById);
			}
			else
				GEDCOMHelper.createIndividualAttribute(model, id, child, null, noteRawMap, placeCache);
		}

		// ---- Sources (SOUR) ----
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(indiNode, "SOUR")){
			GEDCOMHelper.attachSource(individual, model, sourNode, noteRawMap);
		}

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(indiNode, "NOTE")){
			GEDCOMHelper.attachNote(individual, noteNode, noteRawMap);
		}

		// Multimedia (OBJE)
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(indiNode, "OBJE")){
			GEDCOMHelper.attachMultimediaLink(individual, model, multimediaLinkNode, noteRawMap);
		}

		// ---- FAMC (child to family link): the relationships are created with the family ----
		for(GEDCOMNode famcNode : GEDCOMHelper.findChildren(indiNode, "FAMC")){
			final String familyId = GEDCOMHelper.pointer(famcNode.getValue());
			if(familyId != null)
				childFamilyLinks.putIfAbsent(childFamilyKey(id, familyId), famcNode);
		}

		// ---- Aliases and associations ----
		for(GEDCOMNode aliaNode : GEDCOMHelper.findChildren(indiNode, "ALIA"))
			attachAlias(individual, indiNode, aliaNode);
		for(GEDCOMNode assoNode : GEDCOMHelper.findChildren(indiNode, "ASSO"))
			attachAssociation(id, indiNode, assoNode);

		// ---- Privacy (RESN) ----
		GEDCOMHelper.attachRestriction(individual, indiNode);

		// ---- Audit ----
		individual.addChild(AuditBuilder.build(indiNode));

		model.addRecord(individual);
	}

	/**
	 * Converts an EVEN or FACT that holds a name (e.g. {@code 1 EVEN Dorato /do.ˈra.to/ 2 TYPE Family nickname}) into a
	 * PersonalNameStructure made of one part; a transcription between slashes becomes its phonetic variant.
	 */
	private void attachNameFact(final FLEFRecord individual, final GEDCOMNode node, final String partType){
		final String value = StringUtils.trimToNull(node.getValue());
		if(value == null)
			return;

		final FLEFRecord name = FLEFRecord.createChildWithTag("name")
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "nickname"))
			.addChild(GEDCOMHelper.createNamePart(partType, value));
		GEDCOMHelper.attachValidity(name, GEDCOMHelper.findFirstValue(node, "DATE"));
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(node, "SOUR"))
			GEDCOMHelper.attachSource(name, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(node, "NOTE"))
			GEDCOMHelper.attachNote(name, noteNode, noteRawMap);
		individual.addChild(name);
	}

	/**
	 * Converts an ALIA: a pointer to another individual becomes an identity hypothesis between the two, a text becomes a name of
	 * type {@code alias}.
	 */
	private void attachAlias(final FLEFRecord individual, final GEDCOMNode indiNode, final GEDCOMNode aliaNode){
		final String aliasId = GEDCOMHelper.pointer(aliaNode.getValue());
		if(aliasId == null){
			final FLEFRecord name = GEDCOMHelper.createPersonalName(aliaNode.getValue(), null, "alias");
			if(name != null)
				individual.addChild(name.addChild(FLEFRecord.createChildWithTag("note")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", "From GEDCOM ALIA"))
					.addChild(AuditBuilder.build(indiNode))));
			return;
		}
		if(aliasId.equals(individual.getId()) || !recordsById.containsKey(aliasId))
			return;

		final FLEFRecord identityHypothesis = FLEFRecord.createMainRecord(IDGenerator.nextId(IdentityHypothesisReader.ID_PREFIX),
				IdentityHypothesisReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("identity")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", individual.getId())))
			.addChild(FLEFRecord.createChildWithTag("identity")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", aliasId)))
			.addChild(FLEFRecord.createChildWithTagAndValue("rationale", "Linked as alias (ALIA) in the GEDCOM file"))
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
			GEDCOMMapper.mapAssociationRoleLabel(GEDCOMHelper.findFirstValue(assoNode, "ROLE")));
		final String relationToken = GEDCOMMapper.toTypeToken(relation);
		final String type = (relationToken != null && ArrayUtils.contains(RelationshipReader.TYPES, relationToken)
			? relationToken
			: "associate");
		//RELA states the relation of the associate to the individual ("1 ASSO @I2@ 2 RELA Godfather": I2 is the godfather),
		//and the subject of a relationship is the one whose role is described; a group can only be the target
		final boolean associateIsGroup = "FAM".equals(associate.getTag());
		final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX),
				RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", (associateIsGroup? individualId: associateId))))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(associateIsGroup
					? FLEFRecord.createChildWithTagAndValue("group", associateId)
					: FLEFRecord.createChildWithTagAndValue("individual", individualId)))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));
		if(relation != null && !type.equals(relationToken))
			relationship.addChild(FLEFRecord.createChildWithTagAndValue("role", relation));

		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(assoNode, "SOUR"))
			GEDCOMHelper.attachSource(relationship, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(assoNode, "NOTE"))
			GEDCOMHelper.attachNote(relationship, noteNode, noteRawMap);
		relationship.addChild(AuditBuilder.build(indiNode));

		// check for duplicates before adding
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

}
