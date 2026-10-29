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
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Converts GEDCOM FAM (family) records into FLEF GroupRecord,
 * RelationshipRecord (spouse, parent-child, and group membership),
 * EventRecord, EventParticipationRecord, and GroupAttributeRecord (for
 * RESI, NCHI).
 * <p>
 * A family is modelled in FLEF as a {@link GroupReader#TYPE} record
 * whose members are linked through {@code group_member} relationships.
 * That is why the converter emits three families of relationships for
 * every family:
 * <ul>
 *   <li>{@code group_member} — links each spouse and each child to the
 *       family group. This is what makes the family a group rather than
 *       a pure set of pairwise links;</li>
 *   <li>{@code civil_spouse} (or {@code religious_spouse}, depending on
 *       the MARR.TYPE, else {@code spouse}) — links the two spouses to each
 *       other, from the marriage to the divorce or annulment;</li>
 *   <li>{@code biological_child} (or the type given by the FAMC.PEDI of the
 *       child) — links each child to each known parent.</li>
 * </ul>
 * <p>
 * Handles:
 * <ul>
 *   <li>Group name → name</li>
 *   <li>Family events (MARR, DIV, ENGA, CENS, EVEN, etc.) → EventRecord + EventParticipationRecord (the family, and the
 *   spouses with their age)</li>
 *   <li>Family attributes (RESI, and FACT of GEDCOM 7) → GroupAttributeRecord</li>
 *   <li>Individuals associated with a family event (ASSO with ROLE, GEDCOM 7) → EventParticipationRecord</li>
 *   <li>Associates of the family (ASSO, GEDCOM 7) → RelationshipRecord ({@code associate})</li>
 *   <li>Non-events (NO, GEDCOM 7) → note</li>
 *   <li>NCHI → GroupAttributeRecord (children_count), when different from the children listed</li>
 *   <li>OBJE → MediaRecord + preferred_image (if _PRIMARY or _PREF Y) + SourceCitation</li>
 *   <li>Notes (NOTE) → inline note structs</li>
 *   <li>Sources → SourceCitation</li>
 *   <li>Privacy (RESN) → PrivacyStructure</li>
 *   <li>Audit → AuditStructure</li>
 * </ul>
 * SUBM, REFN, and RIN have no counterpart.
 */
public class FamilyConverter{

	private static final String ROLE_PRINCIPAL = "principal";


	private final FLEFModel model;
	private final Map<String, FLEFRecord> familyMap;
	private final Map<String, FLEFRecord> individualMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final Map<String, GEDCOMNode> recordsById;
	private final Map<String, GEDCOMNode> childFamilyLinks;
	private final PlaceCache placeCache;

	private final List<FamilyLink> familyLinks = new ArrayList<>();

	private static class FamilyLink{
		String familyId;
		String husbandId;
		String wifeId;
		List<String> childrenIds = new ArrayList<>();
		GEDCOMNode famNode;
	}

	/**
	 * @param recordsById	The GEDCOM records, by id.
	 * @param childFamilyLinks	The FAMC of each individual, by {@code individualId|familyId}.
	 */
	public FamilyConverter(FLEFModel model,
		Map<String, FLEFRecord> familyMap,
		Map<String, FLEFRecord> individualMap,
		Map<String, GEDCOMNode> noteRawMap,
		Map<String, GEDCOMNode> recordsById,
		Map<String, GEDCOMNode> childFamilyLinks,
		PlaceCache placeCache){
		this.model = model;
		this.familyMap = familyMap;
		this.individualMap = individualMap;
		this.noteRawMap = noteRawMap;
		this.recordsById = recordsById;
		this.childFamilyLinks = childFamilyLinks;
		this.placeCache = placeCache;
	}

	/**
	 * First pass: collect family data, create the GroupRecord.
	 */
	public void collect(GEDCOMNode famNode){
		String famXref = famNode.getXrefId();
		if(famXref == null)
			return;

		IDGenerator.registerExistingId(famXref);

		// Create GroupRecord
		FLEFRecord group = FLEFRecord.createMainRecord(famXref, GroupReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "family"));
		familyMap.put(famXref, group);

		FamilyLink link = new FamilyLink();
		link.familyId = famXref;
		link.famNode = famNode;

		// Husband and wife
		link.husbandId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(famNode, "HUSB"));
		link.wifeId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(famNode, "WIFE"));

		// Children
		for(GEDCOMNode chilNode : GEDCOMHelper.findChildren(famNode, "CHIL")){
			final String childId = GEDCOMHelper.pointer(chilNode.getValue());
			if(childId != null)
				link.childrenIds.add(childId);
		}

		// NCHI (number of children) – store as GroupAttributeRecord if it differs from the children listed
		String nchi = GEDCOMHelper.findFirstValue(famNode, "NCHI");
		if(nchi != null){
			int reportedCount;
			try{
				reportedCount = Integer.parseInt(nchi);
			}
			catch(NumberFormatException e){
				reportedCount = -1;
			}
			if(reportedCount >= 0){
				int actualCount = link.childrenIds.size();
				if(reportedCount != actualCount){
					FLEFRecord groupAttribute = FLEFRecord.createMainRecord(
							IDGenerator.nextId(GroupAttributeReader.ID_PREFIX), GroupAttributeReader.TYPE)
						.addChild(FLEFRecord.createChildWithTagAndValue("group", group.getId()))
						.addChild(FLEFRecord.createChildWithTagAndValue("type", "children_count"))
						.addChild(FLEFRecord.createChildWithTagAndValue("value", String.valueOf(reportedCount)))
						.addChild(AuditBuilder.build(famNode));
					Deduplicator.getDeduplicatedRecordId(model, groupAttribute);
				}
			}
		}

		// Sources (SOUR) – attach directly to group
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(famNode, "SOUR")){
			GEDCOMHelper.attachSource(group, model, sourNode, noteRawMap);
		}

		// Notes – inline structs
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(famNode, "NOTE")){
			GEDCOMHelper.attachNote(group, noteNode, noteRawMap);
		}
		// Non-events (NO, GEDCOM 7) – notes
		for(GEDCOMNode noNode : GEDCOMHelper.findChildren(famNode, "NO")){
			GEDCOMHelper.attachNonEvent(group, noNode, model, noteRawMap);
		}

		// Associates of the family (ASSO, GEDCOM 7)
		for(GEDCOMNode assoNode : GEDCOMHelper.findChildren(famNode, "ASSO")){
			attachAssociation(group, famNode, assoNode);
		}

		// Multimedia (OBJE) – attach directly to group
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(famNode, "OBJE")){
			GEDCOMHelper.attachMultimediaLink(group, model, multimediaLinkNode, noteRawMap);
		}

		// Restriction notice -> privacy
		GEDCOMHelper.attachRestriction(group, famNode);

		// Audit
		group.addChild(AuditBuilder.build(famNode));

		// Store the link for the second pass
		familyLinks.add(link);
	}

	/**
	 * Second pass: resolve all links, create relationships, events,
	 * and attributes.
	 * <p>
	 * Emits three families of relationships for every family:
	 * <ul>
	 *   <li>{@code group_member} — one per spouse and one per child,
	 *       linking the individual to the family group;</li>
	 *   <li>{@code civil_spouse}, {@code religious_spouse}, or {@code spouse} — between
	 *       the two spouses, when both are known;</li>
	 *   <li>a child relationship — between each child and each
	 *       known parent.</li>
	 * </ul>
	 */
	public void resolveLinks(){
		for(FamilyLink link : familyLinks){
			FLEFRecord group = familyMap.get(link.familyId);
			if(group == null)
				continue;

			// ---- Set group name ----
			String husbandName = getDisplayName(link.husbandId);
			String wifeName = getDisplayName(link.wifeId);
			String groupName = (husbandName.isEmpty()? "?": husbandName) + " and " + (wifeName.isEmpty()? "?": wifeName);
			FLEFRecord nameRec = FLEFRecord.createChildWithTag("name")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", groupName + " family"));
			group.getChildren()
				.addFirst(nameRec);

			// ---- Group memberships: every family member belongs to the family group ----
			// A family is a group in the FLEF model, and the group only
			// has members through explicit group_member relationships.
			// Without these, the group exists but has an empty member
			// list, and any view that iterates group_member to build the
			// member list would show nothing.
			if(individualMap.containsKey(link.husbandId))
				createGroupMembership(link.husbandId, group.getId(), "husband", link.famNode);
			if(individualMap.containsKey(link.wifeId))
				createGroupMembership(link.wifeId, group.getId(), "wife", link.famNode);
			for(String childId : link.childrenIds)
				if(individualMap.containsKey(childId))
					createGroupMembership(childId, group.getId(), "child", link.famNode);

			// ---- Spouse relationship (Inter-individual) ----
			if(individualMap.containsKey(link.husbandId) && individualMap.containsKey(link.wifeId))
				createSpouseRelationship(link);

			// ---- Parent-child relationships ----
			for(String childId : link.childrenIds){
				if(individualMap.containsKey(childId)){
					final GEDCOMNode famcNode = childFamilyLinks.get(IndividualConverter.childFamilyKey(childId, link.familyId));
					final Set<String> biologicalParents = GEDCOMHelper.biologicalParents(recordsById.get(childId), recordsById);
					if(individualMap.containsKey(link.husbandId))
						createParentChild(childId, link.husbandId, famcNode, link.famNode, biologicalParents);
					if(individualMap.containsKey(link.wifeId))
						createParentChild(childId, link.wifeId, famcNode, link.famNode, biologicalParents);
				}
			}

			// ---- Process family events (MARR, DIV, ENGA, etc.) and attributes (RESI), in the order of the file ----
			for(GEDCOMNode child : link.famNode.getChildren()){
				final String typeToken = (GEDCOMMapper.TAG_GENERIC_EVENT.equals(child.getTag())
					? GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(child, "TYPE"))
					: null);
				if(typeToken != null && GEDCOMMapper.GROUP_ATTRIBUTE_FACT_TYPES.containsKey(typeToken))
					createGroupAttribute(child, group, GEDCOMMapper.GROUP_ATTRIBUTE_FACT_TYPES.get(typeToken));
				else if(GEDCOMMapper.FAMILY_EVENT_TAGS.contains(child.getTag())){
					final FLEFRecord event = GEDCOMHelper.createEvent(model, child, noteRawMap, placeCache);
					// The event is about the family and about each spouse
					GEDCOMHelper.attachEventParticipation(event.getId(), "group", group.getId(), ROLE_PRINCIPAL, null, child,
						model);
					if(individualMap.containsKey(link.husbandId))
						GEDCOMHelper.attachEventParticipation(event.getId(), "individual", link.husbandId, ROLE_PRINCIPAL,
							GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(child, "HUSB"), "AGE"), child, model);
					if(individualMap.containsKey(link.wifeId))
						GEDCOMHelper.attachEventParticipation(event.getId(), "individual", link.wifeId, ROLE_PRINCIPAL,
							GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(child, "WIFE"), "AGE"), child, model);
					GEDCOMHelper.attachEventAssociates(event.getId(), child, model, noteRawMap, recordsById);
				}
				else if("RESI".equals(child.getTag()))
					createGroupAttribute(child, group, GEDCOMMapper.mapAttribute(child.getTag(), child.getTag()));
				// a family fact (GEDCOM 7), whose type is given by its TYPE
				else if(GEDCOMMapper.TAG_GENERIC_FACT.equals(child.getTag()))
					createGroupAttribute(child, group, ObjectUtils.firstNonNull(
						GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(child, "TYPE")), "fact"));
			}
		}
	}

	// ------------------------------------------------------------------------
	// Helper methods for creating records
	// ------------------------------------------------------------------------

	/**
	 * Creates a {@code group_member} relationship linking an individual
	 * to a family group.
	 * <p>
	 * A family is a group in the FLEF model, and every person that
	 * belongs to it (spouses and children) is a member of that group.
	 * The {@code group_member} relationship is what makes the family a
	 * group rather than a pure set of pairwise links: without it, the
	 * group exists but has no members, and any view that iterates
	 * {@code group_member} to populate the member list would show
	 * nothing.
	 *
	 * @param individualId the individual who belongs to the group
	 * @param groupId      the family group
	 * @param role         the position of the individual in the family (husband, wife, child)
	 * @param famNode      the GEDCOM family node, used as the audit
	 *                     source
	 */
	private void createGroupMembership(final String individualId, final String groupId,
		final String role, final GEDCOMNode famNode){
		final FLEFRecord relationship = FLEFRecord.createMainRecord(
				IDGenerator.nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", individualId)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("group", groupId)))
			.addChild(FLEFRecord.createChildWithTagAndValue("type",
				RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		if(StringUtils.isNotEmpty(role))
			relationship.addChild(FLEFRecord.createChildWithTagAndValue("role", role));
		relationship.addChild(AuditBuilder.build(famNode));

		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/**
	 * Creates the relationship between the spouses: its type comes from MARR.TYPE, it starts with the marriage and ends with
	 * the divorce or the annulment.
	 */
	private void createSpouseRelationship(final FamilyLink link){
		final GEDCOMNode marriageNode = GEDCOMHelper.findFirstChild(link.famNode, "MARR");
		final String marriageType = GEDCOMHelper.findFirstValue(marriageNode, "TYPE");
		GEDCOMNode endNode = GEDCOMHelper.findFirstChild(link.famNode, "DIV");
		if(endNode == null)
			endNode = GEDCOMHelper.findFirstChild(link.famNode, "ANUL");

		FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", link.husbandId))
			)
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", link.wifeId))
			);
		FLEFRecord note = null;
		if(("civil".equalsIgnoreCase(marriageType) || "religious".equalsIgnoreCase(marriageType)))
			relationship.addChild(FLEFRecord.createChildWithTagAndValue("type", marriageType.toLowerCase(Locale.ROOT) + "_spouse"));
		else{
			relationship.addChild(FLEFRecord.createChildWithTagAndValue("type", "spouse"));
			note = FLEFRecord.createChildWithTag("note")
				.addChild(FLEFRecord.createChildWithTagAndValue("text",
					(StringUtils.isNotEmpty(marriageType)? marriageType + " -- ": StringUtils.EMPTY)
						+ "TO BE REVISED: is it civil or religious or else?"))
				.addChild(AuditBuilder.build(null));
		}

		// The relationship lasts from the marriage to the divorce (or annulment)
		if(marriageNode != null)
			GEDCOMHelper.attachDate(relationship, "valid_from", GEDCOMHelper.findFirstValue(marriageNode, "DATE"));
		if(endNode != null)
			GEDCOMHelper.attachDate(relationship, "valid_to", GEDCOMHelper.findFirstValue(endNode, "DATE"));

		relationship.addChild(note);
		relationship.addChild(AuditBuilder.build(link.famNode));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/**
	 * Creates the relationship between a child and a parent.
	 * <p>
	 * Its type comes from the pedigree (FAMC.PEDI) of the child, its confidence from the linkage status (FAMC.STAT).
	 * <p>
	 * A parent by birth stays so in the other families of the child (e.g. the mother whose new husband adopted the
	 * child): no other relationship is created with them.
	 *
	 * @param famcNode	The FAMC of the child to this family, may be {@code null}.
	 * @param biologicalParents	The parents by birth of the child.
	 */
	private void createParentChild(String childId, String parentId, GEDCOMNode famcNode, GEDCOMNode famNode,
			Set<String> biologicalParents){
		final String type = GEDCOMMapper.mapPedigree(GEDCOMHelper.findFirstValue(famcNode, "PEDI"));
		if(!RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(type) && biologicalParents.contains(parentId))
			return;

		FLEFRecord relationship = FLEFRecord.createMainRecord(
				IDGenerator.nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", childId)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));
		final String status = GEDCOMHelper.findFirstValue(famcNode, "STAT");
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(famcNode, "NOTE"))
			GEDCOMHelper.attachNote(relationship, noteNode, noteRawMap);
		if(status != null)
			relationship.addChild(GEDCOMHelper.createNoteStruct("Linkage status", status, famcNode));
		final String confidence = GEDCOMMapper.mapChildLinkageStatus(status);
		if(confidence != null)
			relationship.addChild(FLEFRecord.createChildWithTagAndValue("confidence", confidence));
		relationship.addChild(AuditBuilder.build(famNode));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/*
	(GEDCOM 7)
	n ASSO @<XREF:INDI>@
	  +1 ROLE <Enum>    {1:1}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 <<SOURCE_CITATION>>    {0:M}
	*/
	/** Creates the relationship of an individual associated with the family (e.g. a friend of the family). */
	private void attachAssociation(final FLEFRecord group, final GEDCOMNode famNode, final GEDCOMNode assoNode){
		final String associateId = GEDCOMHelper.pointer(assoNode.getValue());
		final GEDCOMNode associate = recordsById.get(associateId);
		if(associate == null || !"INDI".equals(associate.getTag()))
			return;

		final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX),
				RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", associateId)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("group", group.getId())))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "associate"));
		final String role = GEDCOMMapper.mapAssociationRoleLabel(GEDCOMHelper.findFirstValue(assoNode, "ROLE"));
		if(role != null)
			relationship.addChild(FLEFRecord.createChildWithTagAndValue("role", role));

		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(assoNode, "SOUR"))
			GEDCOMHelper.attachSource(relationship, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(assoNode, "NOTE"))
			GEDCOMHelper.attachNote(relationship, noteNode, noteRawMap);
		relationship.addChild(AuditBuilder.build(famNode));

		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/**
	 * Creates a GroupAttributeRecord for a family residence (RESI) or fact (FACT), or for a family event that is an attribute in
	 * FLEF.
	 */
	private void createGroupAttribute(GEDCOMNode attrNode, FLEFRecord group, String type){
		FLEFRecord groupAttribute = FLEFRecord.createMainRecord(
				IDGenerator.nextId(GroupAttributeReader.ID_PREFIX), GroupAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("group", group.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));

		// Value (if present)
		String value = attrNode.getValue();
		if(StringUtils.isNotBlank(value) && !"Y".equalsIgnoreCase(value.trim()))
			groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("value", value.trim()));

		// Date (valid_from / valid_to)
		GEDCOMHelper.attachValidity(groupAttribute, GEDCOMHelper.findFirstValue(attrNode, "DATE"));

		// Place
		GEDCOMHelper.attachPlaceCitation(groupAttribute, model, GEDCOMHelper.findFirstChild(attrNode, "PLAC"),
			GEDCOMHelper.findFirstChild(attrNode, "ADDR"), noteRawMap, placeCache);

		// Sources
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(attrNode, "SOUR")){
			GEDCOMHelper.attachSource(groupAttribute, model, sourNode, noteRawMap);
		}

		// Notes
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(attrNode, "NOTE")){
			GEDCOMHelper.attachNote(groupAttribute, noteNode, noteRawMap);
		}

		// Multimedia
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(attrNode, "OBJE")){
			GEDCOMHelper.attachMultimediaLink(groupAttribute, model, multimediaLinkNode, noteRawMap);
		}

		// Restriction
		GEDCOMHelper.attachRestriction(groupAttribute, attrNode);

		// Audit
		groupAttribute.addChild(AuditBuilder.build(attrNode));

		// Deduplicate and add
		Deduplicator.getDeduplicatedRecordId(model, groupAttribute);
	}

	// ------------------------------------------------------------------------
	// Utility methods
	// ------------------------------------------------------------------------

	private String getDisplayName(String indiId){
		if(indiId == null)
			return StringUtils.EMPTY;

		FLEFRecord indi = individualMap.get(indiId);
		if(indi == null)
			return StringUtils.EMPTY;

		for(FLEFRecord name : indi.getChildren()){
			if("name".equals(name.getTag())){
				for(FLEFRecord part : name.getChildren()){
					if("part".equals(part.getTag())){
						String type = FLEFRecordHelper.getChildValue(part, "type");
						if("family".equals(type)){
							String val = FLEFRecordHelper.getChildValue(part, "value");
							if(val != null)
								return val;
						}
					}
				}
			}
		}
		return "Unknown";
	}

}
