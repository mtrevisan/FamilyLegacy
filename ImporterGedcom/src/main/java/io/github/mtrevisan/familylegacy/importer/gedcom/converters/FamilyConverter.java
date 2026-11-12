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
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

	private final FLEFModel model;
	private final Map<String, FLEFRecord> familyMap;
	private final Map<String, FLEFRecord> individualMap;
	private final Map<String, GEDCOMNode> noteRawMap;
	private final Map<String, GEDCOMNode> recordsById;
	private final Map<String, GEDCOMNode> childFamilyLinks;
	private final PlaceCache placeCache;
	private final AgeBirthConverter ageBirthConverter;

	private final List<FamilyLink> familyLinks = new ArrayList<>();
	private static final Pattern YEAR_PATTERN = Pattern.compile("\\d{4}");

	private static class FamilyLink{
		String familyId;
		String husbandId;
		String wifeId;
		final List<String> childrenIds = new ArrayList<>();
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
		PlaceCache placeCache,
		AgeBirthConverter ageBirthConverter){
		this.model = model;
		this.familyMap = familyMap;
		this.individualMap = individualMap;
		this.noteRawMap = noteRawMap;
		this.recordsById = recordsById;
		this.childFamilyLinks = childFamilyLinks;
		this.placeCache = placeCache;
		this.ageBirthConverter = ageBirthConverter;
	}

	/**
	 * First pass: collect family data, create the GroupRecord.
	 */
	public void collect(GEDCOMNode famNode){
		String famXref = famNode.getXrefId();
		if(famXref == null){
			GEDCOMImportContext.discardUnidentified(famNode);
			return;
		}

		IDGenerator.registerExistingId(famXref);
		GEDCOMImportContext.setRecord(famXref);

		// Create GroupRecord
		FLEFRecord group = FLEFRecord.createMainRecord(famXref, GroupReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(GroupReader.TAG_TYPE, GroupReader.ENUM_TYPE_FAMILY));
		familyMap.put(famXref, group);

		FamilyLink link = new FamilyLink();
		link.familyId = famXref;
		link.famNode = famNode;

		// Husband and wife
		link.husbandId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(famNode, GEDCOMMapper.TAG_HUSBAND));
		link.wifeId = GEDCOMHelper.pointer(GEDCOMHelper.findFirstValue(famNode, GEDCOMMapper.TAG_WIFE));

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
						.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_GROUP, group.getId()))
						.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_TYPE, GroupAttributeReader.ENUM_TYPE_CHILDREN_COUNT))
						.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_VALUE, String.valueOf(reportedCount)))
						.addChild(AuditBuilder.build(famNode));
					Deduplicator.getDeduplicatedRecordId(model, groupAttribute);
				}
			}
		}

		// Sources (SOUR) – attach directly to group
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(famNode, GEDCOMMapper.TAG_SOURCE)){
			GEDCOMRecordBuilder.attachSource(group, model, sourNode, noteRawMap);
		}

		// Notes – inline structs
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(famNode, GEDCOMMapper.TAG_NOTE)){
			GEDCOMRecordBuilder.attachNote(group, model, noteNode, noteRawMap);
		}
		// Non-events (NO, GEDCOM 7) – negative research activities
		for(GEDCOMNode noNode : GEDCOMHelper.findChildren(famNode, "NO")){
			GEDCOMRecordBuilder.createNonEventActivity(model, AgentReader.TAG_GROUP, group.getId(), noNode, noteRawMap);
		}

		// Associates of the family (ASSO, GEDCOM 7)
		for(GEDCOMNode assoNode : GEDCOMHelper.findChildren(famNode, GEDCOMMapper.TAG_ASSOCIATION)){
			attachAssociation(group, famNode, assoNode);
		}

		// Multimedia (OBJE) – attach directly to group
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(famNode, GEDCOMMapper.TAG_MULTIMEDIA)){
			GEDCOMRecordBuilder.attachMultimediaLink(group, model, multimediaLinkNode, noteRawMap);
		}

		// What has no field in FLEF is kept as notes
		KeptNotes.keepIdentifiers(group, famNode, StringUtils.EMPTY, famNode);

		// Restriction notice -> privacy
		GEDCOMRecordBuilder.attachRestriction(group, famNode);

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
			GEDCOMImportContext.setRecord(link.familyId);

			// ---- Set group name ----
			String husbandName = getDisplayName(link.husbandId);
			String wifeName = getDisplayName(link.wifeId);
			String groupName = (husbandName.isEmpty()? "?": husbandName) + " and " + (wifeName.isEmpty()? "?": wifeName);
			FLEFRecord nameRec = FLEFRecord.createChildWithTag(GroupReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, groupName + " family"));
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
					createGroupMembership(childId, group.getId(), RelationshipReader.ENUM_TYPE_CHILD, link.famNode);

			// ---- Spouse relationship (Inter-individual) ----
			if(individualMap.containsKey(link.husbandId) && individualMap.containsKey(link.wifeId))
				createSpouseRelationship(link);

			// ---- Parent-child relationships ----
			for(String childId : link.childrenIds){
				if(individualMap.containsKey(childId)){
					final GEDCOMNode famcNode = childFamilyLinks.get(IndividualConverter.childFamilyKey(childId, link.familyId));
					final Set<String> biologicalParents = GEDCOMRecordBuilder.biologicalParents(recordsById.get(childId), recordsById);
					if(individualMap.containsKey(link.husbandId))
						createParentChild(childId, link.husbandId, famcNode, link.famNode, biologicalParents);
					if(individualMap.containsKey(link.wifeId))
						createParentChild(childId, link.wifeId, famcNode, link.famNode, biologicalParents);
				}
			}

			// ---- Process family events (MARR, DIV, ENGA, etc.) and attributes (RESI), in the order of the file ----
			for(GEDCOMNode child : link.famNode.getChildren()){
				final String typeToken = (GEDCOMMapper.TAG_GENERIC_EVENT.equals(child.getTag())
					? GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(child, GEDCOMMapper.TAG_TYPE))
					: null);
				if(typeToken != null && GEDCOMMapper.GROUP_ATTRIBUTE_FACT_TYPES.containsKey(typeToken))
					createGroupAttribute(child, group, GEDCOMMapper.GROUP_ATTRIBUTE_FACT_TYPES.get(typeToken));
				else if(GEDCOMMapper.TAG_TODO.equalsIgnoreCase(child.getTag()))
					GEDCOMRecordBuilder.createResearchTask(model, child, noteRawMap, AgentReader.TAG_GROUP, group.getId());
				else if(GEDCOMMapper.FAMILY_EVENT_TAGS.contains(child.getTag()) || GEDCOMHelper.isCustomEvent(child)){
					final FLEFRecord event = GEDCOMRecordBuilder.createEvent(model, child, noteRawMap, placeCache);
					//the person the role of a citation refers to is not known
					GEDCOMRecordBuilder.reportCitedRoles(child);
					// The event is about the family and about each spouse
					GEDCOMRecordBuilder.attachEventParticipation(event.getId(), AgentReader.TAG_GROUP, group.getId(), EventParticipationReader.ENUM_ROLE_PRINCIPAL, null, child,
						model);
					if(individualMap.containsKey(link.husbandId)){
						final String age = GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(child, GEDCOMMapper.TAG_HUSBAND), GEDCOMMapper.TAG_AGE);
						ageBirthConverter.track(link.husbandId, event, GEDCOMRecordBuilder.attachEventParticipation(event.getId(),
							AgentReader.TAG_INDIVIDUAL, link.husbandId, EventParticipationReader.ENUM_ROLE_PRINCIPAL, age, child, model), age, child);
					}
					if(individualMap.containsKey(link.wifeId)){
						final String age = GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(child, GEDCOMMapper.TAG_WIFE), GEDCOMMapper.TAG_AGE);
						ageBirthConverter.track(link.wifeId, event, GEDCOMRecordBuilder.attachEventParticipation(event.getId(),
							AgentReader.TAG_INDIVIDUAL, link.wifeId, EventParticipationReader.ENUM_ROLE_PRINCIPAL, age, child, model), age, child);
					}
					GEDCOMRecordBuilder.attachEventAssociates(event.getId(), child, model, noteRawMap, recordsById);
				}
				else if("RESI".equals(child.getTag()))
					createGroupAttribute(child, group, GEDCOMMapper.mapAttribute(child.getTag(), child.getTag()));
				// a family fact (GEDCOM 7), whose type is given by its TYPE
				else if(GEDCOMMapper.TAG_GENERIC_FACT.equals(child.getTag()))
					createGroupAttribute(child, group, ObjectUtils.firstNonNull(
						GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(child, GEDCOMMapper.TAG_TYPE)), "fact"));
				else
					KeptNotes.keepUnmappedChild(group, child, GEDCOMMapper.FAMILY_CONSUMED_TAGS, StringUtils.EMPTY, link.famNode);
			}
		}
	}


	/**
	 * Derives the step relationships: a child of a spouse, from another family, is a step child of the other spouse (the file says
	 * who the parents are, and who married whom; it does not say the rest). A child born after the marriage is left out, as the
	 * spouse may be a parent of it as well: only what the file proves (the marriage, and the child of the other family) is used.
	 * <p>
	 * The relationships are marked as derived. To be called after {@link #resolveLinks()}.
	 */
	public void deriveStepRelationships(){
		//the families of each spouse, so as not to look for them among all the families for each marriage
		final Map<String, List<FamilyLink>> familiesOf = new HashMap<>();
		for(final FamilyLink link : familyLinks){
			if(link.husbandId != null)
				familiesOf.computeIfAbsent(link.husbandId, k -> new ArrayList<>())
					.add(link);
			if(link.wifeId != null && !link.wifeId.equals(link.husbandId))
				familiesOf.computeIfAbsent(link.wifeId, k -> new ArrayList<>())
					.add(link);
		}

		for(final FamilyLink marriage : familyLinks){
			if(!individualMap.containsKey(marriage.husbandId) || !individualMap.containsKey(marriage.wifeId))
				continue;

			final String marriageYear = yearOf(GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(marriage.famNode, "MARR"),
				GEDCOMMapper.TAG_DATE));
			for(final String[] spouses : new String[][]{{marriage.husbandId, marriage.wifeId}, {marriage.wifeId, marriage.husbandId}}){
				final String parentId = spouses[0];
				final String stepParentId = spouses[1];
				for(final FamilyLink other : familiesOf.getOrDefault(parentId, List.of())){
					if(other == marriage || stepParentId.equals(other.husbandId) || stepParentId.equals(other.wifeId))
						continue;

					for(final String childId : other.childrenIds){
						if(!individualMap.containsKey(childId) || marriage.childrenIds.contains(childId) || childId.equals(stepParentId))
							continue;

						final String birthYear = yearOf(GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(recordsById.get(childId),
							"BIRT"), GEDCOMMapper.TAG_DATE));
						if(marriageYear != null && birthYear != null && birthYear.compareTo(marriageYear) > 0)
							continue;

						createStepChild(childId, stepParentId, marriage.famNode);
					}
				}
			}
		}
	}

	private void createStepChild(final String childId, final String stepParentId, final GEDCOMNode marriageNode){
		GEDCOMImportContext.setRecord(marriageNode.getXrefId());
		final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX),
				RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, stepParentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_STEP_CHILD))
			.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.derived"), GedcomTexts.tf("text.step_child_derived", stepParentId, childId), marriageNode))
			.addChild(AuditBuilder.build(marriageNode));
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/** @return the year of a GEDCOM date (the first four digits), {@code null} if it has none */
	private static String yearOf(final String date){
		if(date == null)
			return null;

		final Matcher matcher = YEAR_PATTERN.matcher(date);
		return (matcher.find()? matcher.group(): null);
	}
	/**
	 * Reports the FAMC of an individual that points to a family not listing the individual as a child (FAM.CHIL): it creates no
	 * relationship.
	 */
	public void reportChildLinksWithoutChild(){
		final Map<String, FamilyLink> linksById = new HashMap<>();
		for(final FamilyLink link : familyLinks)
			linksById.put(link.familyId, link);
		for(final Map.Entry<String, GEDCOMNode> entry : childFamilyLinks.entrySet()){
			final String key = entry.getKey();
			final int separator = key.indexOf('|');
			final String individualId = key.substring(0, separator);
			final FamilyLink link = linksById.get(key.substring(separator + 1));
			if(link != null && individualMap.containsKey(individualId) && !link.childrenIds.contains(individualId)){
				GEDCOMImportContext.setRecord(individualId);
				GEDCOMImportContext.discard(ImportReasons.OTHER, entry.getValue(), StringUtils.EMPTY);
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
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_GROUP, groupId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE,
				RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		if(StringUtils.isNotEmpty(role))
			relationship.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, role));
		relationship.addChild(AuditBuilder.build(famNode));

		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}

	/**
	 * Creates the relationship between the spouses: its type comes from MARR.TYPE, it starts with the marriage and ends with
	 * the divorce or the annulment.
	 */
	private void createSpouseRelationship(final FamilyLink link){
		final GEDCOMNode marriageNode = GEDCOMHelper.findFirstChild(link.famNode, "MARR");
		final String marriageType = GEDCOMHelper.findFirstValue(marriageNode, GEDCOMMapper.TAG_TYPE);
		//without a MARR.TYPE, what the file says explicitly (a note, the title of the source the spouses both cite) tells the kind
		final boolean typed = ("civil".equalsIgnoreCase(marriageType) || "religious".equalsIgnoreCase(marriageType));
		final String kind = (typed? marriageType.toLowerCase(Locale.ROOT)
			: MarriageTypeInference.infer(link.famNode, recordsById.get(link.husbandId), recordsById.get(link.wifeId), recordsById));
		GEDCOMNode endNode = GEDCOMHelper.findFirstChild(link.famNode, "DIV");
		if(endNode == null)
			endNode = GEDCOMHelper.findFirstChild(link.famNode, "ANUL");

		FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, link.husbandId))
			)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, link.wifeId))
			);
		FLEFRecord note = null;
		if(kind != null)
			relationship.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, kind + "_spouse"));
		else{
			//the kind is not known: the generic type says so (the Research Assistant asks which it is), and the type the file gave to the
			//marriage, if any, is kept as a note, not lost
			relationship.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_SPOUSE));
			if(StringUtils.isNotEmpty(marriageType))
				note = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
					.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, GedcomTexts.tf("text.marriage_type", marriageType)))
					.addChild(AuditBuilder.build(null));
		}

		// The relationship lasts from the marriage to the divorce (or annulment)
		if(marriageNode != null)
			GEDCOMRecordBuilder.attachDate(relationship, RelationshipReader.TAG_VALID_FROM, GEDCOMHelper.findFirstValue(marriageNode, GEDCOMMapper.TAG_DATE));
		if(endNode != null)
			GEDCOMRecordBuilder.attachDate(relationship, RelationshipReader.TAG_VALID_TO, GEDCOMHelper.findFirstValue(endNode, GEDCOMMapper.TAG_DATE));

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
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type));
		final String status = GEDCOMHelper.findFirstValue(famcNode, "STAT");
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(famcNode, GEDCOMMapper.TAG_NOTE))
			GEDCOMRecordBuilder.attachNote(relationship, model, noteNode, noteRawMap);
		if(status != null)
			relationship.addChild(NoteBuilder.createNoteStruct(GedcomTexts.t("title.linkage_status"), status, famcNode));
		final String confidence = GEDCOMMapper.mapChildLinkageStatus(status);
		if(confidence != null)
			relationship.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_CONFIDENCE, confidence));
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
		if(associate == null || !GEDCOMMapper.TAG_INDIVIDUAL.equals(associate.getTag())){
			GEDCOMImportContext.discard(ImportReasons.OTHER, assoNode, StringUtils.EMPTY);
			return;
		}

		final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX),
				RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, associateId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_GROUP, group.getId())))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_ASSOCIATE));
		final String role = GEDCOMMapper.mapAssociationRoleLabel(GEDCOMHelper.findFirstValue(assoNode, GEDCOMMapper.TAG_ROLE));
		if(role != null)
			relationship.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, role));

		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(assoNode, GEDCOMMapper.TAG_SOURCE))
			GEDCOMRecordBuilder.attachSource(relationship, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(assoNode, GEDCOMMapper.TAG_NOTE))
			GEDCOMRecordBuilder.attachNote(relationship, model, noteNode, noteRawMap);
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
			.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_GROUP, group.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_TYPE, type));

		// Value (if present)
		String value = attrNode.getValue();
		if(StringUtils.isNotBlank(value) && !"Y".equalsIgnoreCase(value.trim()))
			groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue(GroupAttributeReader.TAG_VALUE, value.trim()));

		// Date (valid_from / valid_to)
		GEDCOMRecordBuilder.attachValidity(groupAttribute, GEDCOMHelper.findFirstValue(attrNode, GEDCOMMapper.TAG_DATE));

		// Place
		final FLEFRecord place = GEDCOMRecordBuilder.attachPlaceCitation(groupAttribute, model, GEDCOMHelper.findFirstChild(attrNode,
			GEDCOMMapper.TAG_PLACE), GEDCOMHelper.findFirstChild(attrNode, GEDCOMMapper.TAG_ADDRESS), noteRawMap, placeCache);

		// Sources
		for(GEDCOMNode sourNode : GEDCOMHelper.findChildren(attrNode, GEDCOMMapper.TAG_SOURCE)){
			GEDCOMRecordBuilder.attachSource(groupAttribute, model, sourNode, noteRawMap);
		}

		// Notes
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(attrNode, GEDCOMMapper.TAG_NOTE)){
			GEDCOMRecordBuilder.attachNote(groupAttribute, model, noteNode, noteRawMap);
		}

		// Multimedia
		for(GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(attrNode, GEDCOMMapper.TAG_MULTIMEDIA)){
			GEDCOMRecordBuilder.attachMultimediaLink(groupAttribute, model, multimediaLinkNode, noteRawMap);
		}

		// What has no field in FLEF is kept as notes
		GEDCOMRecordBuilder.keepEventLosses(groupAttribute, place, attrNode);

		// Restriction
		GEDCOMRecordBuilder.attachRestriction(groupAttribute, attrNode);

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
			if(GroupReader.TAG_NAME.equals(name.getTag())){
				for(FLEFRecord part : name.getChildren()){
					if(NameReader.TAG_PART.equals(part.getTag())){
						String type = FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_TYPE);
						if(NameReader.ENUM_PART_TYPE_FAMILY.equals(type)){
							String val = FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_VALUE);
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
