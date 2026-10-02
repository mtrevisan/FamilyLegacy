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
package io.github.mtrevisan.familylegacy.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.names.NameAnatomyService;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Builds a {@link GroupDossier} from a FLEF model.
 * <p>
 * Reads every record type that references the group and produces a
 * sectioned, compact view, mirroring the structure of the individual
 * dossier service.
 */
public final class GroupDossierService{

	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_NOTE = "note";
	private static final String TAG_EVIDENCE = "evidence";


	private final FLEFModel model;

	private final GroupHandler groupHandler;
	private final IndividualHandler individualHandler;
	private final DossierFormatting formatting;

	private final Map<String, ProofStatus> conclusionIndex;


	public GroupDossierService(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		this.model = model;
		this.groupHandler = GroupHandler.getInstance();
		this.individualHandler = IndividualHandler.getInstance();
		this.formatting = new DossierFormatting(model);
		this.conclusionIndex = buildConclusionIndex();
	}


	/**
	 * Builds the dossier for the given group.
	 *
	 * @param groupId the group id; may be {@code null}
	 * @return the dossier, or {@link GroupDossier#empty()} when the id is
	 * {@code null} or does not resolve to a group record
	 */
	public GroupDossier build(final String groupId){
		if(StringUtils.isEmpty(groupId))
			return GroupDossier.empty();

		final FLEFRecord group = model.getRecordById(groupId);
		if(group == null || !GroupHandler.TYPE.equalsIgnoreCase(group.getTag()))
			return GroupDossier.empty();

		final String displayName = resolveGroupName(group);

		final Map<GroupDossierSectionType, List<DossierEntry>> sections =
			new EnumMap<>(GroupDossierSectionType.class);
		sections.put(GroupDossierSectionType.IDENTITY, buildIdentity(group, displayName));
		sections.put(GroupDossierSectionType.MEMBERS, buildMembers(groupId));
		sections.put(GroupDossierSectionType.SUBGROUPS, buildSubgroups(groupId));
		sections.put(GroupDossierSectionType.ATTRIBUTES, buildAttributes(groupId));
		sections.put(GroupDossierSectionType.EVENTS, buildEvents(groupId));
		sections.put(GroupDossierSectionType.SOURCES, buildSources(group, groupId));
		sections.put(GroupDossierSectionType.CONTEXT, buildContext(groupId));
		sections.put(GroupDossierSectionType.RESEARCH, buildResearch(groupId));
		sections.put(GroupDossierSectionType.NOTES, buildNotes(group, groupId));

		return new GroupDossier(group, displayName, sections);
	}


	/* ======================================================================
	 *                          Identity
	 * ====================================================================== */

	private List<DossierEntry> buildIdentity(final FLEFRecord group, final String displayName){
		final List<DossierEntry> entries = new ArrayList<>();

		// Names, using NameAnatomyService for consistency with the individual
		// dossier. Group names are generic (no parts), so each name produces
		// a header row plus a value row.
		final List<Name> names = NameAnatomyService.extractForGeneric(group);

		for(final Name name : names){
			entries.add(DossierEntry.nameHeader(name.displayType(), group));
			if(StringUtils.isNotEmpty(name.value()))
				entries.add(DossierEntry.namePart("value", name.value(), group));
		}

		if(names.isEmpty())
			entries.add(DossierEntry.of("Name", displayName, group));

		final String type = FLEFRecordHelper.getChildValue(group, GroupReader.TAG_TYPE);
		if(StringUtils.isNotEmpty(type))
			entries.add(DossierEntry.of("Type", type.replace('_', ' '), group));

		// Preferred image.
		final String uri = GroupReader.extractPreferredImageUri(group);
		if(StringUtils.isNotEmpty(uri))
			entries.add(DossierEntry.of("Photo", uri, group));

		final FLEFRecord privacy = FLEFRecordHelper.findChild(group, GroupReader.TAG_PRIVACY);
		if(privacy != null){
			final String level = FLEFRecordHelper.getChildValue(privacy, PrivacyReader.TAG_LEVEL);
			if(StringUtils.isNotEmpty(level))
				entries.add(DossierEntry.of("Privacy", level, group));
		}
		return entries;
	}

	private String resolveGroupName(final FLEFRecord group){
		try{
			final String text = groupHandler.getDisplayText(group, model);
			if(StringUtils.isNotEmpty(text))
				return text;
		}
		catch(final RuntimeException ignored){
			// Fall through to the id-based fallback.
		}
		return (group.getId() != null? group.getId(): "?");
	}


	/* ======================================================================
	 *                          Members
	 * ====================================================================== */

	private List<DossierEntry> buildMembers(final String groupId){
		final List<DossierEntry> entries = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_TYPE);
			if(!RelationshipReader.isTypeGroupMember(type))
				continue;

			final String memberId = extractRef(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractRef(relationship, RelationshipReader.TAG_OBJECT);
			if(!groupId.equals(objectId) || memberId == null)
				continue;

			final String name = resolveIndividualName(memberId);
			final String role = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE);
			final String status = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_STATUS);
			final String validity = formatting.formatValidity(relationship, GroupAttributeReader.TAG_VALID_FROM,
				GroupAttributeReader.TAG_VALID_TO);

			final StringBuilder subtitle = new StringBuilder();
			if(StringUtils.isNotEmpty(role))
				subtitle.append("role: ").append(role);
			if(StringUtils.isNotEmpty(status)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append(status);
			}
			if(StringUtils.isNotEmpty(validity)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append(validity);
			}

			entries.add(new DossierEntry("Member", name, subtitle.toString(),
				evidenceBadge(relationship), model.getRecordById(memberId), DossierEntry.Kind.NORMAL,
				proofStatusFor(relationship)));
		}
		return entries;
	}


	/* ======================================================================
	 *                          Subgroups
	 * ====================================================================== */

	private List<DossierEntry> buildSubgroups(final String groupId){
		final List<DossierEntry> entries = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_TYPE);
			if(!RelationshipReader.isTypePartOf(type))
				continue;

			final String subjectId = extractRef(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractRef(relationship, RelationshipReader.TAG_OBJECT);
			final boolean isParent = groupId.equals(objectId);
			final boolean isChild = groupId.equals(subjectId);
			if(!isParent && !isChild)
				continue;

			final String otherId = (isParent? subjectId: objectId);
			final String otherName = resolveGroupNameById(otherId);
			final String label = (isParent? "Subgroup": "Supergroup");
			final String status = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_STATUS);
			final String validity = formatting.formatValidity(relationship, RelationshipReader.TAG_VALID_FROM,
				RelationshipReader.TAG_VALID_TO);

			final StringBuilder subtitle = new StringBuilder();
			if(StringUtils.isNotEmpty(status))
				subtitle.append(status);
			if(StringUtils.isNotEmpty(validity)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append(validity);
			}

			entries.add(new DossierEntry(label, otherName, subtitle.toString(),
				evidenceBadge(relationship), model.getRecordById(otherId), DossierEntry.Kind.NORMAL,
				proofStatusFor(relationship)));
		}
		return entries;
	}


	/* ======================================================================
	 *                          Attributes
	 * ====================================================================== */

	private List<DossierEntry> buildAttributes(final String groupId){
		final List<DossierEntry> entries = new ArrayList<>();
		final List<FLEFRecord> attributes = model.getRecordsByType(GroupAttributeHandler.TYPE);
		for(final FLEFRecord attribute : attributes){
			final String ownerId = extractRef(attribute, GroupHandler.TYPE);
			if(!groupId.equals(ownerId))
				continue;

			final String type = FLEFRecordHelper.getChildValue(attribute, GroupAttributeReader.TAG_TYPE);
			final String label = (StringUtils.isNotEmpty(type)
				? type.replace('_', ' '): "Attribute");
			final String value = FLEFRecordHelper.getChildValue(attribute, GroupAttributeReader.TAG_VALUE);

			final String validity = formatting.formatValidity(attribute, GroupAttributeReader.TAG_VALID_FROM,
				GroupAttributeReader.TAG_VALID_TO);
			final String place = formatting.resolvePlaceName(attribute);

			final StringBuilder subtitle = new StringBuilder();
			if(StringUtils.isNotEmpty(validity))
				subtitle.append(validity);
			if(StringUtils.isNotEmpty(place)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append(place);
			}

			entries.add(new DossierEntry(label, (value != null? value: StringUtils.EMPTY),
				subtitle.toString(), evidenceBadge(attribute), attribute,
				DossierEntry.Kind.NORMAL, proofStatusFor(attribute)));
		}
		return entries;
	}


	/* ======================================================================
	 *                          Events
	 * ====================================================================== */

	private List<DossierEntry> buildEvents(final String groupId){
		final List<DossierEntry> entries = new ArrayList<>();
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final String participantId = extractParticipantId(participation);
			if(!groupId.equals(participantId))
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null)
				continue;

			final String eventType = FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE);
			final String label = (StringUtils.isNotEmpty(eventType)
				? eventType.replace('_', ' '): "Event");

			final String date = formatting.formatDate(event);
			final String place = formatting.resolvePlaceName(event);
			final StringBuilder value = new StringBuilder();
			if(StringUtils.isNotEmpty(date))
				value.append(date);
			if(StringUtils.isNotEmpty(place)){
				if(!value.isEmpty())
					value.append(" — ");
				value.append(place);
			}

			final StringBuilder subtitle = new StringBuilder();
			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			if(StringUtils.isNotEmpty(role))
				subtitle.append("role: ").append(role);
			final String agency = FLEFRecordHelper.getChildValue(event, EventReader.TAG_AGENCY);
			if(StringUtils.isNotEmpty(agency)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append(agency);
			}
			final String description = FLEFRecordHelper.getChildValue(event, EventReader.TAG_DESCRIPTION);
			if(StringUtils.isNotEmpty(description)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append(description);
			}

			entries.add(new DossierEntry(label, value.toString(), subtitle.toString(),
				evidenceBadge(event), event, DossierEntry.Kind.NORMAL,
				proofStatusFor(event)));
		}
		return entries;
	}


	/* ======================================================================
	 *                          Sources
	 * ====================================================================== */

	private List<DossierEntry> buildSources(final FLEFRecord group, final String groupId){
		final List<DossierEntry> entries = new ArrayList<>();
		final Set<String> seen = new HashSet<>();

		collectSources(group, "Group", entries, seen);

		final List<FLEFRecord> attributes = model.getRecordsByType(GroupAttributeHandler.TYPE);
		for(final FLEFRecord attribute : attributes){
			final String ownerId = extractRef(attribute, GroupHandler.TYPE);
			if(groupId.equals(ownerId))
				collectSources(attribute, "Attribute", entries, seen);
		}

		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final String participantId = extractParticipantId(participation);
			if(groupId.equals(participantId))
				collectSources(participation, "Event participation", entries, seen);
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = extractRef(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractRef(relationship, RelationshipReader.TAG_OBJECT);
			if(groupId.equals(subjectId) || groupId.equals(objectId))
				collectSources(relationship, "Relationship", entries, seen);
		}

		final List<FLEFRecord> impacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord impact : impacts){
			final String targetId = extractRef(impact, ContextImpactReader.TAG_TARGET);
			if(groupId.equals(targetId))
				collectSources(impact, "Context", entries, seen);
		}

		return entries;
	}

	private void collectSources(final FLEFRecord parent, final String origin,
			final List<DossierEntry> entries, final Set<String> seen){
		final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(parent, TAG_SOURCE);
		for(final FLEFRecord citation : citations){
			final String sourceId = extractRef(citation, TAG_SOURCE);
			if(sourceId == null || !seen.add(sourceId))
				continue;

			final FLEFRecord source = model.getRecordById(sourceId);
			if(source == null)
				continue;

			final String title = resolveSourceTitle(source);
			final String locator = FLEFRecordHelper.getChildValue(citation, TAG_LOCATOR);
			final String label = origin + " source";
			final String subtitle = (StringUtils.isNotEmpty(locator)
				? "locator: " + locator: StringUtils.EMPTY);

			entries.add(new DossierEntry(label, title, subtitle,
				evidenceBadge(citation), source, DossierEntry.Kind.NORMAL,
				proofStatusFor(citation)));
		}
	}

	private static String resolveSourceTitle(final FLEFRecord source){
		final FLEFRecord titleStruct = FLEFRecordHelper.findChild(source, SourceReader.TAG_TITLE);
		if(titleStruct != null){
			final String v = FLEFRecordHelper.getChildValue(titleStruct, NameReader.TAG_VALUE);
			if(StringUtils.isNotEmpty(v))
				return v;
			if(StringUtils.isNotEmpty(titleStruct.getValue()))
				return titleStruct.getValue();
		}
		final String id = source.getId();
		return (id != null? id: "?");
	}


	/* ======================================================================
	 *                          Context
	 * ====================================================================== */

	private List<DossierEntry> buildContext(final String groupId){
		final List<DossierEntry> entries = new ArrayList<>();
		final List<FLEFRecord> impacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord impact : impacts){
			final String targetId = extractRef(impact, ContextImpactReader.TAG_TARGET);
			if(!groupId.equals(targetId))
				continue;

			final String contextId = extractRef(impact, ContextImpactReader.TAG_CONTEXT);
			if(contextId == null)
				continue;

			final FLEFRecord context = model.getRecordById(contextId);
			if(context == null)
				continue;

			final String impactType = FLEFRecordHelper.getChildValue(impact, ContextImpactReader.TAG_IMPACT_TYPE);
			final String rationale = FLEFRecordHelper.getChildValue(impact, ContextImpactReader.TAG_RATIONALE);
			final String contextTitle = resolveContextTitle(context);
			final String subtitle = (StringUtils.isNotEmpty(rationale)? rationale: StringUtils.EMPTY);

			entries.add(new DossierEntry(
				(impactType != null? impactType: "context"),
				contextTitle, subtitle,
				evidenceBadge(impact), context, DossierEntry.Kind.NORMAL,
				proofStatusFor(impact)));
		}
		return entries;
	}

	private static String resolveContextTitle(final FLEFRecord context){
		String title = null;
		final String tag = context.getTag();
		if(CulturalNormHandler.TYPE.equals(tag.toLowerCase(Locale.ROOT)))
			title = CulturalNormReader.extractTitle(context);
		else if(HistoricEventHandler.TYPE.equals(tag.toLowerCase(Locale.ROOT)))
			title = HistoricEventReader.extractTitle(context);
		if(StringUtils.isNotEmpty(title))
			return title;

		final String id = context.getId();
		return (id != null? id: "?");
	}


	/* ======================================================================
	 *                          Research
	 * ====================================================================== */

	private List<DossierEntry> buildResearch(final String groupId){
		final List<DossierEntry> entries = new ArrayList<>();

		final List<FLEFRecord> questions = model.getRecordsByType(ResearchQuestionHandler.TYPE);
		for(final FLEFRecord question : questions){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET))
				if(groupId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets)
				continue;

			final String title = FLEFRecordHelper.getChildValue(question, ResearchQuestionReader.TAG_TITLE);
			final String q = FLEFRecordHelper.getChildValue(question, ResearchQuestionReader.TAG_QUESTION);
			final String status = FLEFRecordHelper.getChildValue(question, ResearchQuestionReader.TAG_STATUS);
			final String confidence = FLEFRecordHelper.getChildValue(question, ResearchQuestionReader.TAG_CONCLUSION_CONFIDENCE);
			final String rationale = FLEFRecordHelper.getChildValue(question, ResearchQuestionReader.TAG_RATIONALE);

			final StringBuilder subtitle = new StringBuilder();
			if(StringUtils.isNotEmpty(status))
				subtitle.append(status);
			if(StringUtils.isNotEmpty(confidence)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append("confidence: ").append(confidence);
			}
			if(StringUtils.isNotEmpty(rationale)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append(rationale);
			}

			entries.add(new DossierEntry("Research question",
				(StringUtils.isNotEmpty(title)? title: q),
				subtitle.toString(), StringUtils.EMPTY, question, DossierEntry.Kind.NORMAL,
				proofStatusFor(question)));
		}

		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES))
				if(groupId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets && groupId.equals(extractRef(conclusion, ConclusionReader.TAG_PREFERRED)))
				targets = true;
			if(!targets)
				continue;

			final String issue = FLEFRecordHelper.getChildValue(conclusion, ConclusionReader.TAG_ISSUE);
			final String proof = FLEFRecordHelper.getChildValue(conclusion, ConclusionReader.TAG_PROOF_STATUS);
			entries.add(new DossierEntry("Conclusion",
				(StringUtils.isNotEmpty(issue)? issue: "?"),
				(proof != null? proof: StringUtils.EMPTY), StringUtils.EMPTY, conclusion, DossierEntry.Kind.NORMAL,
				ProofStatus.fromString(proof)));
		}
		return entries;
	}


	/* ======================================================================
	 *                          Notes
	 * ====================================================================== */

	private List<DossierEntry> buildNotes(final FLEFRecord group, final String groupId){
		final List<DossierEntry> entries = new ArrayList<>();

		collectNotes(group, "Group", entries);

		final List<FLEFRecord> attributes = model.getRecordsByType(GroupAttributeHandler.TYPE);
		for(final FLEFRecord attribute : attributes){
			final String ownerId = extractRef(attribute, GroupHandler.TYPE);
			if(groupId.equals(ownerId))
				collectNotes(attribute, "Attribute", entries);
		}

		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : participations){
			final String participantId = extractParticipantId(participation);
			if(groupId.equals(participantId))
				collectNotes(participation, "Event participation", entries);
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = extractRef(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractRef(relationship, RelationshipReader.TAG_OBJECT);
			if(groupId.equals(subjectId) || groupId.equals(objectId))
				collectNotes(relationship, "Relationship", entries);
		}

		final List<FLEFRecord> impacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord impact : impacts){
			final String targetId = extractRef(impact, ContextImpactReader.TAG_TARGET);
			if(groupId.equals(targetId))
				collectNotes(impact, "Context", entries);
		}

		final List<FLEFRecord> questions = model.getRecordsByType(ResearchQuestionHandler.TYPE);
		for(final FLEFRecord question : questions){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET))
				if(groupId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(targets)
				collectNotes(question, "Research question", entries);
		}

		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES))
				if(groupId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets && groupId.equals(extractRef(conclusion, ConclusionReader.TAG_PREFERRED)))
				targets = true;
			if(targets)
				collectNotes(conclusion, "Conclusion", entries);
		}

		return entries;
	}

	private void collectNotes(final FLEFRecord parent, final String origin,
		final List<DossierEntry> entries){
		for(final FLEFRecord note : FLEFRecordHelper.findChildren(parent, TAG_NOTE)){
			final String title = FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TITLE);
			final String text = FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TEXT);
			final String label = origin + " note";
			final String value = (StringUtils.isNotEmpty(title)? title: text);
			final String subtitle = (StringUtils.isNotEmpty(title) && StringUtils.isNotEmpty(text)
				? text: StringUtils.EMPTY);

			entries.add(new DossierEntry(label, value, subtitle, StringUtils.EMPTY, parent,
				DossierEntry.Kind.NORMAL, proofStatusFor(parent)));
		}
	}


	/* ======================================================================
	 *                          Conclusion index
	 * ====================================================================== */

	private Map<String, ProofStatus> buildConclusionIndex(){
		final Map<String, ProofStatus> index = new HashMap<>();
		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			final ProofStatus status = ProofStatus.fromString(
				FLEFRecordHelper.getChildValue(conclusion, ConclusionReader.TAG_PROOF_STATUS));
			if(status == null)
				continue;

			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES)){
				final String targetId = extractRef(target, null);
				if(targetId != null)
					index.put(targetId, status);
			}

			final String preferredId = extractRef(conclusion, ConclusionReader.TAG_PREFERRED);
			if(preferredId != null)
				index.put(preferredId, status);
		}
		return index;
	}

	private ProofStatus proofStatusFor(final FLEFRecord record){
		if(record == null)
			return null;

		final String id = record.getId();
		return (id != null? conclusionIndex.get(id): null);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private String resolveIndividualName(final String id){
		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return id;

		try{
			return individualHandler.getDisplayText(record, model);
		}
		catch(final RuntimeException ignored){
			return id;
		}
	}

	private String resolveGroupNameById(final String id){
		if(id == null)
			return "?";

		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return id;

		try{
			return groupHandler.getDisplayText(record, model);
		}
		catch(final RuntimeException ignored){
			return id;
		}
	}

	private static String extractParticipantId(final FLEFRecord participation){
		final FLEFRecord field = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT);
		if(field == null)
			return null;

		final FLEFRecord ref = field.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}

	private static String extractRef(final FLEFRecord record, final String fieldTag){
		final FLEFRecord field = (fieldTag != null? FLEFRecordHelper.findChild(record, fieldTag): record);
		if(field == null)
			return null;

		final FLEFRecord child = field.getTheOnlyChild();
		if(child != null && child.getValue() != null)
			return child.getValue();
		if(field.getValue() != null)
			return field.getValue();

		for(final FLEFRecord candidate : field.getChildren()){
			final FLEFRecord inner = candidate.getTheOnlyChild();
			if(inner != null && inner.getValue() != null)
				return inner.getValue();
		}
		return null;
	}

	private static String evidenceBadge(final FLEFRecord parent){
		final FLEFRecord evidence = FLEFRecordHelper.findChild(parent, TAG_EVIDENCE);
		if(evidence == null)
			return StringUtils.EMPTY;

		final String st = EvidenceQualifiersReader.extractSourceType(evidence);
		final String it = EvidenceQualifiersReader.extractInformationType(evidence);
		final String et = EvidenceQualifiersReader.extractEvidenceType(evidence);

		final StringBuilder sb = new StringBuilder();
		if(st != null)
			sb.append(st.charAt(0));
		if(it != null){
			if(!sb.isEmpty())
				sb.append('/');
			sb.append(it.charAt(0));
		}
		if(et != null){
			if(!sb.isEmpty())
				sb.append('/');
			sb.append(et.charAt(0));
		}
		return sb.toString();
	}

}
