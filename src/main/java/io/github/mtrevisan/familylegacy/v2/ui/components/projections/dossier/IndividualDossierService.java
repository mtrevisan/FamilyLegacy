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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.names.NameAnatomy;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.names.NameAnatomyService;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.names.NamePart;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
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
 * Builds an {@link IndividualDossier} from a FLEF model.
 * <p>
 * The service reads every record type that carries information about an
 * individual and produces a compact, sectioned view. Record types are
 * identified by their FLEF protocol tag, so the service works with any
 * model produced by a compliant parser.
 * <p>
 * The service is stateless apart from the model reference, the
 * pre-computed event index, the name-anatomy service, the shared
 * formatting helpers, and the conclusion index. Not thread-safe; use
 * it from the Swing Event Dispatch Thread.
 */
public final class IndividualDossierService{

	// Record type tags, as defined by the FLEF protocol.
	private static final String TYPE_INDIVIDUAL = "individual";
	private static final String TYPE_GROUP = "group";
	private static final String TYPE_EVENT_PARTICIPATION = "event_participation";
	private static final String TYPE_INDIVIDUAL_ATTRIBUTE = "individual_attribute";
	private static final String TYPE_RELATIONSHIP = "relationship";
	private static final String TYPE_CONTEXT_IMPACT = "context_impact";
	private static final String TYPE_IDENTITY_HYPOTHESIS = "identity_hypothesis";
	private static final String TYPE_RESEARCH_QUESTION = "research_question";
	private static final String TYPE_CONCLUSION = "conclusion";

	// Child tags inside FLEF records.
	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_ROLE = "role";
	private static final String TAG_VALUE = "value";
	private static final String TAG_SOURCE = "source";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_EVIDENCE = "evidence";
	private static final String TAG_SOURCE_TYPE = "source_type";
	private static final String TAG_INFORMATION_TYPE = "information_type";
	private static final String TAG_EVIDENCE_TYPE = "evidence_type";
	private static final String TAG_NAME = "name";
	private static final String TAG_SEX = "sex";
	private static final String TAG_STATUS = "status";
	private static final String TAG_CONTEXT = "context";
	private static final String TAG_IMPACT_TYPE = "impact_type";
	private static final String TAG_RATIONALE = "rationale";
	private static final String TAG_IDENTITY = "identity";
	private static final String TAG_COMMENT = "comment";
	private static final String TAG_TITLE = "title";
	private static final String TAG_QUESTION = "question";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_AGENCY = "agency";
	private static final String TAG_PREFERRED = "preferred";
	private static final String TAG_RESOLVES = "resolves";
	private static final String TAG_PROOF_STATUS = "proof_status";
	private static final String TAG_NOTE = "note";
	private static final String TAG_TEXT = "text";
	private static final String TAG_PREFERRED_IMAGE = "preferred_image";
	private static final String TAG_URI = "uri";
	private static final String TAG_PRIVACY = "privacy";
	private static final String TAG_LEVEL = "level";


	private final FLEFModel model;
	private final IndividualHandler individualHandler;
	private final Map<String, List<FLEFRecord>> eventMap;

	private final NameAnatomyService nameAnatomyService;
	private final DossierFormatting formatting;

	/** Reverse index: assertion record id -> proof status. */
	private final Map<String, ProofStatus> conclusionIndex;


	public IndividualDossierService(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		this.model = model;
		this.individualHandler = IndividualHandler.getInstance();
		this.eventMap = buildEventMap();

		this.nameAnatomyService = new NameAnatomyService(model);
		this.formatting = new DossierFormatting(model);
		this.conclusionIndex = buildConclusionIndex();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Builds the dossier for the given individual.
	 *
	 * @param individualId the individual id; may be {@code null}
	 * @return the dossier, or {@link IndividualDossier#empty()} when the
	 * id is {@code null} or does not resolve to a record
	 */
	public IndividualDossier build(final String individualId){
		if(StringUtils.isEmpty(individualId))
			return IndividualDossier.empty();

		final FLEFRecord individual = model.getRecordById(individualId);
		if(individual == null)
			return IndividualDossier.empty();

		final IndividualData data = IndividualData.create(individual, eventMap, model);
		final String displayName = (!data.isEmpty()
			? data.getNameText()
			: individualId);

		final Map<DossierSectionType, List<DossierEntry>> sections =
			new EnumMap<>(DossierSectionType.class);
		sections.put(DossierSectionType.IDENTITY, buildIdentity(individual, data));
		sections.put(DossierSectionType.EVENTS, buildEvents(individualId));
		sections.put(DossierSectionType.ATTRIBUTES, buildAttributes(individualId));
		sections.put(DossierSectionType.RELATIONSHIPS, buildRelationships(individualId));
		sections.put(DossierSectionType.SOURCES, buildSources(individual, individualId));
		sections.put(DossierSectionType.CONTEXT, buildContext(individualId));
		sections.put(DossierSectionType.IDENTITY_HYPOTHESES, buildIdentityHypotheses(individualId));
		sections.put(DossierSectionType.RESEARCH, buildResearch(individualId));
		sections.put(DossierSectionType.NOTES, buildNotes(individual, individualId));

		return new IndividualDossier(individual, data, displayName, sections);
	}


	/* ======================================================================
	 *                          Identity
	 * ====================================================================== */

	private List<DossierEntry> buildIdentity(final FLEFRecord individual, final IndividualData data){
		final List<DossierEntry> entries = new ArrayList<>();

		// Names: use the NameAnatomyService so the parsing logic is shared
		// with NameAnatomyPanel. Each name produces a header row and one
		// row per part.
		final String individualId = individual.getId();
		final List<NameAnatomy> names = (individualId != null
			? nameAnatomyService.extractForIndividual(individualId)
			: List.of());

		for(final NameAnatomy name : names){
			entries.add(DossierEntry.nameHeader(name.displayType(), individual));

			if(name.hasParts()){
				for(final NamePart part : name.parts())
					if(StringUtils.isNotEmpty(part.value()))
						entries.add(DossierEntry.namePart(part.displayType(), part.value(), individual));
			}
			else if(StringUtils.isNotEmpty(name.value())){
				entries.add(DossierEntry.namePart("value", name.value(), individual));
			}
		}

		// Fallback: keep the old joined-string behavior if the anatomy
		// service returned nothing (e.g. malformed record).
		if(names.isEmpty()){
			for(final FLEFRecord nameStruct : FLEFRecordHelper.findChildren(individual, TAG_NAME)){
				final String value = extractNameValue(nameStruct);
				if(StringUtils.isNotEmpty(value)){
					final String type = FLEFRecordHelper.getChildValue(nameStruct, TAG_TYPE);
					final String label = (StringUtils.isNotEmpty(type)
						? type.replace('_', ' ')
						: "Name");
					entries.add(DossierEntry.of(label, value, individual));
				}
			}
		}

		// Preferred image.
		final FLEFRecord imageStruct = FLEFRecordHelper.findChild(individual, TAG_PREFERRED_IMAGE);
		if(imageStruct != null){
			final String uri = FLEFRecordHelper.getChildValue(imageStruct, TAG_URI);
			if(StringUtils.isNotEmpty(uri))
				entries.add(DossierEntry.of("Photo", uri, individual));
		}

		// Sex.
		final String sex = FLEFRecordHelper.getChildValue(individual, TAG_SEX);
		if(StringUtils.isNotEmpty(sex))
			entries.add(DossierEntry.of("Sex", sex, individual));

		// Vital dates.
		if(data != null && !data.isEmpty()){
			final String info = data.getInfoText();
			if(StringUtils.isNotEmpty(info))
				entries.add(DossierEntry.of("Vital", info, individual));
		}

		// Privacy.
		final FLEFRecord privacy = FLEFRecordHelper.findChild(individual, TAG_PRIVACY);
		if(privacy != null){
			final String level = FLEFRecordHelper.getChildValue(privacy, TAG_LEVEL);
			if(StringUtils.isNotEmpty(level))
				entries.add(DossierEntry.of("Privacy", level, individual));
		}

		return entries;
	}

	private static String extractNameValue(final FLEFRecord nameStruct){
		final String direct = FLEFRecordHelper.getChildValue(nameStruct, TAG_VALUE);
		if(StringUtils.isNotEmpty(direct))
			return direct;

		final StringBuilder sb = new StringBuilder();
		for(final FLEFRecord part : FLEFRecordHelper.findChildren(nameStruct, "part")){
			final String v = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
			if(StringUtils.isNotEmpty(v)){
				if(!sb.isEmpty())
					sb.append(' ');
				sb.append(v);
			}
		}
		return sb.toString();
	}


	/* ======================================================================
	 *                          Events
	 * ====================================================================== */

	private List<DossierEntry> buildEvents(final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String participantId = extractParticipantId(participation);
			if(!individualId.equals(participantId))
				continue;

			final DossierEntry entry = buildEventEntry(participation);
			if(entry != null)
				entries.add(entry);
		}
		return entries;
	}

	private DossierEntry buildEventEntry(final FLEFRecord participation){
		final String eventId = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
		if(eventId == null)
			return null;

		final FLEFRecord event = model.getRecordById(eventId);
		if(event == null)
			return null;

		final String eventType = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
		final String label = (StringUtils.isNotEmpty(eventType)
			? eventType.replace('_', ' ')
			: "Event");

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
		final String role = FLEFRecordHelper.getChildValue(participation, TAG_ROLE);
		if(StringUtils.isNotEmpty(role))
			subtitle.append("role: ").append(role);
		final String agency = FLEFRecordHelper.getChildValue(event, TAG_AGENCY);
		if(StringUtils.isNotEmpty(agency)){
			if(!subtitle.isEmpty())
				subtitle.append(" · ");
			subtitle.append(agency);
		}
		final String description = FLEFRecordHelper.getChildValue(event, TAG_DESCRIPTION);
		if(StringUtils.isNotEmpty(description)){
			if(!subtitle.isEmpty())
				subtitle.append(" · ");
			subtitle.append(description);
		}

		return new DossierEntry(label, value.toString(), subtitle.toString(),
			buildEvidenceBadge(event), event, DossierEntry.Kind.NORMAL,
			proofStatusFor(event));
	}


	/* ======================================================================
	 *                          Attributes
	 * ====================================================================== */

	private List<DossierEntry> buildAttributes(final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			final String ownerId = attribute.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			final String resolvedOwner = (ownerId != null? ownerId: extractRef(attribute, "individual"));
			if(!individualId.equals(resolvedOwner))
				continue;

			entries.add(buildAttributeEntry(attribute));
		}
		return entries;
	}

	private DossierEntry buildAttributeEntry(final FLEFRecord attribute){
		final String type = FLEFRecordHelper.getChildValue(attribute, TAG_TYPE);
		final String label = (StringUtils.isNotEmpty(type)
			? type.replace('_', ' ')
			: "Attribute");
		final String value = FLEFRecordHelper.getChildValue(attribute, TAG_VALUE);

		final String validity = formatting.formatValidity(attribute);
		final String place = formatting.resolvePlaceName(attribute);
		final StringBuilder subtitle = new StringBuilder();
		if(StringUtils.isNotEmpty(validity))
			subtitle.append(validity);
		if(StringUtils.isNotEmpty(place)){
			if(!subtitle.isEmpty())
				subtitle.append(" · ");
			subtitle.append(place);
		}

		return new DossierEntry(label, (value != null? value: ""), subtitle.toString(),
			buildEvidenceBadge(attribute), attribute, DossierEntry.Kind.NORMAL,
			proofStatusFor(attribute));
	}


	/* ======================================================================
	 *                          Relationships
	 * ====================================================================== */

	private List<DossierEntry> buildRelationships(final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String subjectId = extractParticipantRef(rel, TAG_SUBJECT);
			final String targetId = extractParticipantRef(rel, TAG_TARGET);
			final boolean isSubject = individualId.equals(subjectId);
			final boolean isTarget = individualId.equals(targetId);
			if(!isSubject && !isTarget)
				continue;

			entries.add(buildRelationshipEntry(rel, isSubject));
		}
		return entries;
	}

	private DossierEntry buildRelationshipEntry(final FLEFRecord rel, final boolean isSubject){
		final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
		final String typeLabel = (StringUtils.isNotEmpty(type)
			? type.replace('_', ' ')
			: "Relationship");

		final String otherId = (isSubject
			? extractParticipantRef(rel, TAG_TARGET)
			: extractParticipantRef(rel, TAG_SUBJECT));
		final String otherName = resolveParticipantName(otherId);

		final String direction = (isSubject? "→ ": "← ");
		final String label = direction + typeLabel;
		final String value = (otherName != null? otherName: otherId != null? otherId: "?");

		final StringBuilder subtitle = new StringBuilder();
		final String role = FLEFRecordHelper.getChildValue(rel, TAG_ROLE);
		if(StringUtils.isNotEmpty(role))
			subtitle.append("role: ").append(role);
		final String status = FLEFRecordHelper.getChildValue(rel, TAG_STATUS);
		if(StringUtils.isNotEmpty(status)){
			if(!subtitle.isEmpty())
				subtitle.append(" · ");
			subtitle.append(status);
		}
		final String validity = formatting.formatValidity(rel);
		if(StringUtils.isNotEmpty(validity)){
			if(!subtitle.isEmpty())
				subtitle.append(" · ");
			subtitle.append(validity);
		}

		return new DossierEntry(label, value, subtitle.toString(),
			buildEvidenceBadge(rel), rel, DossierEntry.Kind.NORMAL,
			proofStatusFor(rel));
	}


	/* ======================================================================
	 *                          Sources
	 * ====================================================================== */

	private List<DossierEntry> buildSources(final FLEFRecord individual, final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		final Set<String> seenSourceIds = new HashSet<>();

		collectSources(individual, "Individual", entries, seenSourceIds);

		for(final FLEFRecord event : eventMap.getOrDefault(individualId, List.of()))
			collectSources(event, "Event", entries, seenSourceIds);

		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			final String ownerId = attribute.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			final String resolvedOwner = (ownerId != null? ownerId: extractRef(attribute, "individual"));
			if(individualId.equals(resolvedOwner))
				collectSources(attribute, "Attribute", entries, seenSourceIds);
		}

		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String subjectId = extractParticipantRef(rel, TAG_SUBJECT);
			final String targetId = extractParticipantRef(rel, TAG_TARGET);
			if(individualId.equals(subjectId) || individualId.equals(targetId))
				collectSources(rel, "Relationship", entries, seenSourceIds);
		}

		for(final FLEFRecord impact : model.getRecordsByType(ContextImpactHandler.TYPE)){
			final String targetId = extractRef(impact, TAG_TARGET);
			if(individualId.equals(targetId))
				collectSources(impact, "Context", entries, seenSourceIds);
		}

		return entries;
	}

	private void collectSources(final FLEFRecord parent, final String origin,
		final List<DossierEntry> entries, final Set<String> seenSourceIds){
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(parent, TAG_SOURCE)){
			final String sourceId = extractRef(citation, TAG_SOURCE);
			if(sourceId == null || !seenSourceIds.add(sourceId))
				continue;

			final FLEFRecord source = model.getRecordById(sourceId);
			if(source == null)
				continue;

			final String title = resolveSourceTitle(source);
			final String locator = FLEFRecordHelper.getChildValue(citation, TAG_LOCATOR);
			final String label = origin + " source";
			final String subtitle = (StringUtils.isNotEmpty(locator)
				? "locator: " + locator
				: "");

			entries.add(new DossierEntry(label, title, subtitle,
				buildEvidenceBadge(citation), source, DossierEntry.Kind.NORMAL,
				proofStatusFor(citation)));
		}
	}

	private static String resolveSourceTitle(final FLEFRecord source){
		final FLEFRecord titleStruct = FLEFRecordHelper.findChild(source, TAG_TITLE);
		if(titleStruct != null){
			final String value = FLEFRecordHelper.getChildValue(titleStruct, TAG_VALUE);
			if(StringUtils.isNotEmpty(value))
				return value;
			final String direct = titleStruct.getValue();
			if(StringUtils.isNotEmpty(direct))
				return direct;
		}
		final String id = source.getId();
		return (id != null? id: "?");
	}


	/* ======================================================================
	 *                          Context
	 * ====================================================================== */

	private List<DossierEntry> buildContext(final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		for(final FLEFRecord impact : model.getRecordsByType(ContextImpactHandler.TYPE)){
			final String targetId = extractRef(impact, TAG_TARGET);
			if(!individualId.equals(targetId))
				continue;

			final String contextId = extractRef(impact, TAG_CONTEXT);
			if(contextId == null)
				continue;

			final FLEFRecord context = model.getRecordById(contextId);
			if(context == null)
				continue;

			final String impactType = FLEFRecordHelper.getChildValue(impact, TAG_IMPACT_TYPE);
			final String label = (StringUtils.isNotEmpty(impactType)? impactType: "context");
			final String contextTitle = resolveContextTitle(context);
			final String rationale = FLEFRecordHelper.getChildValue(impact, TAG_RATIONALE);
			final String subtitle = (StringUtils.isNotEmpty(rationale)
				? rationale
				: context.getTag() != null? context.getTag().replace('_', ' '): "");

			entries.add(new DossierEntry(label, contextTitle, subtitle,
				buildEvidenceBadge(impact), context, DossierEntry.Kind.NORMAL,
				proofStatusFor(impact)));
		}
		return entries;
	}

	private static String resolveContextTitle(final FLEFRecord context){
		final String title = FLEFRecordHelper.getChildValue(context, TAG_TITLE);
		if(StringUtils.isNotEmpty(title))
			return title;

		final String id = context.getId();
		return (id != null? id: "?");
	}


	/* ======================================================================
	 *                          Identity hypotheses
	 * ====================================================================== */

	private List<DossierEntry> buildIdentityHypotheses(final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		for(final FLEFRecord hypothesis : model.getRecordsByType(IdentityHypothesisHandler.TYPE)){
			final List<String> candidateIds = new ArrayList<>();
			for(final FLEFRecord candidate : FLEFRecordHelper.findChildren(hypothesis, TAG_IDENTITY)){
				final String id = extractRef(candidate, null);
				if(id != null)
					candidateIds.add(id);
			}
			if(!candidateIds.contains(individualId))
				continue;

			String otherId = null;
			for(final String id : candidateIds)
				if(!individualId.equals(id)){
					otherId = id;
					break;
				}

			final String otherName = (otherId != null? resolveParticipantName(otherId): "?");
			final String comment = FLEFRecordHelper.getChildValue(hypothesis, TAG_COMMENT);

			entries.add(new DossierEntry("Possible duplicate",
				(otherName != null? otherName: otherId),
				(comment != null? comment: ""),
				buildEvidenceBadge(hypothesis), hypothesis, DossierEntry.Kind.NORMAL,
				proofStatusFor(hypothesis)));
		}
		return entries;
	}


	/* ======================================================================
	 *                          Research
	 * ====================================================================== */

	private List<DossierEntry> buildResearch(final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();

		for(final FLEFRecord question : model.getRecordsByType(ResearchQuestionHandler.TYPE)){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, TAG_TARGET))
				if(individualId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets)
				continue;

			final String title = FLEFRecordHelper.getChildValue(question, TAG_TITLE);
			final String questionText = FLEFRecordHelper.getChildValue(question, TAG_QUESTION);
			final String status = FLEFRecordHelper.getChildValue(question, TAG_STATUS);

			final StringBuilder subtitle = new StringBuilder();
			if(StringUtils.isNotEmpty(status))
				subtitle.append(status);
			if(StringUtils.isNotEmpty(questionText)){
				if(!subtitle.isEmpty())
					subtitle.append(" · ");
				subtitle.append(questionText);
			}

			entries.add(new DossierEntry("Research question",
				(StringUtils.isNotEmpty(title)? title: questionText),
				subtitle.toString(), "", question, DossierEntry.Kind.NORMAL,
				proofStatusFor(question)));
		}

		for(final FLEFRecord conclusion : model.getRecordsByType(ConclusionHandler.TYPE)){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, TAG_RESOLVES))
				if(individualId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets && individualId.equals(extractRef(conclusion, TAG_PREFERRED)))
				targets = true;
			if(!targets)
				continue;

			final String issue = FLEFRecordHelper.getChildValue(conclusion, "issue");
			final String proof = FLEFRecordHelper.getChildValue(conclusion, TAG_PROOF_STATUS);
			entries.add(new DossierEntry("Conclusion",
				(StringUtils.isNotEmpty(issue)? issue: "?"),
				(proof != null? proof: ""), "", conclusion, DossierEntry.Kind.NORMAL,
				ProofStatus.fromString(proof)));
		}

		return entries;
	}


	/* ======================================================================
	 *                          Notes
	 * ====================================================================== */

	private List<DossierEntry> buildNotes(final FLEFRecord individual, final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();

		collectNotes(individual, "Individual", entries);

		for(final FLEFRecord event : eventMap.getOrDefault(individualId, List.of()))
			collectNotes(event, "Event", entries);

		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			final String ownerId = attribute.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			final String resolvedOwner = (ownerId != null? ownerId: extractRef(attribute, "individual"));
			if(individualId.equals(resolvedOwner))
				collectNotes(attribute, "Attribute", entries);
		}

		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String subjectId = extractParticipantRef(rel, TAG_SUBJECT);
			final String targetId = extractParticipantRef(rel, TAG_TARGET);
			if(individualId.equals(subjectId) || individualId.equals(targetId))
				collectNotes(rel, "Relationship", entries);
		}

		for(final FLEFRecord impact : model.getRecordsByType(ContextImpactHandler.TYPE)){
			final String targetId = extractRef(impact, TAG_TARGET);
			if(individualId.equals(targetId))
				collectNotes(impact, "Context", entries);
		}

		for(final FLEFRecord hypothesis : model.getRecordsByType(IdentityHypothesisHandler.TYPE)){
			boolean targets = false;
			for(final FLEFRecord candidate : FLEFRecordHelper.findChildren(hypothesis, TAG_IDENTITY))
				if(individualId.equals(extractRef(candidate, null))){
					targets = true;
					break;
				}
			if(targets)
				collectNotes(hypothesis, "Identity hypothesis", entries);
		}

		for(final FLEFRecord question : model.getRecordsByType(ResearchQuestionHandler.TYPE)){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, TAG_TARGET))
				if(individualId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(targets)
				collectNotes(question, "Research question", entries);
		}

		for(final FLEFRecord conclusion : model.getRecordsByType(ConclusionHandler.TYPE)){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, TAG_RESOLVES))
				if(individualId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets && individualId.equals(extractRef(conclusion, TAG_PREFERRED)))
				targets = true;
			if(targets)
				collectNotes(conclusion, "Conclusion", entries);
		}

		return entries;
	}

	private void collectNotes(final FLEFRecord parent, final String origin,
		final List<DossierEntry> entries){
		for(final FLEFRecord note : FLEFRecordHelper.findChildren(parent, TAG_NOTE)){
			final String title = FLEFRecordHelper.getChildValue(note, TAG_TITLE);
			final String text = FLEFRecordHelper.getChildValue(note, TAG_TEXT);
			final String label = origin + " note";
			final String value = (StringUtils.isNotEmpty(title)? title: text);
			final String subtitle = (StringUtils.isNotEmpty(title) && StringUtils.isNotEmpty(text)
				? text
				: "");

			entries.add(new DossierEntry(label, value, subtitle, "", parent,
				DossierEntry.Kind.NORMAL, proofStatusFor(parent)));
		}
	}


	/* ======================================================================
	 *                          Conclusion index
	 * ====================================================================== */

	/**
	 * Builds the reverse index that maps each assertion record id to the
	 * proof status of the conclusion that resolves it.
	 * <p>
	 * A conclusion may resolve one or more targets via {@code resolves},
	 * and may name a {@code preferred} target. Every resolved target and
	 * the preferred target are indexed under the same status.
	 */
	private Map<String, ProofStatus> buildConclusionIndex(){
		final Map<String, ProofStatus> index = new HashMap<>();
		for(final FLEFRecord conclusion : model.getRecordsByType(ConclusionHandler.TYPE)){
			final ProofStatus status = ProofStatus.fromString(
				FLEFRecordHelper.getChildValue(conclusion, TAG_PROOF_STATUS));
			if(status == null)
				continue;

			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, TAG_RESOLVES)){
				final String targetId = extractRef(target, null);
				if(targetId != null)
					index.put(targetId, status);
			}

			final String preferredId = extractRef(conclusion, TAG_PREFERRED);
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
	 *                          Event index
	 * ====================================================================== */

	private Map<String, List<FLEFRecord>> buildEventMap(){
		final Map<String, List<FLEFRecord>> result = new HashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String participantId = extractParticipantId(participation);
			if(participantId == null)
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event != null)
				result.computeIfAbsent(participantId, k -> new ArrayList<>()).add(event);
		}
		return result;
	}

	private static String extractParticipantId(final FLEFRecord participation){
		final FLEFRecord participantField = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
		if(participantField == null)
			return null;

		final FLEFRecord ref = participantField.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}

	private static String extractParticipantRef(final FLEFRecord rel, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(rel, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord child = field.getTheOnlyChild();
		return (child != null? child.getValue(): null);
	}

	private static String extractRef(final FLEFRecord parent, final String tag){
		final FLEFRecord field = (tag != null? FLEFRecordHelper.findChild(parent, tag): parent);
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


	/* ======================================================================
	 *                          Participant name
	 * ====================================================================== */

	private String resolveParticipantName(final String id){
		if(id == null)
			return null;

		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return id;

		try{
			final String tag = record.getTag();
			if(TYPE_GROUP.equalsIgnoreCase(tag))
				return GroupHandler.getInstance().getDisplayText(record, model);
			return individualHandler.getDisplayText(record, model);
		}
		catch(final RuntimeException ignored){
			return id;
		}
	}


	/* ======================================================================
	 *                          Evidence badge
	 * ====================================================================== */

	/**
	 * Builds a compact evidence badge (e.g. {@code "orig/prim/dir"}) from
	 * the {@code evidence} child of the given record. Returns an empty
	 * string when no evidence qualifiers are present.
	 */
	private static String buildEvidenceBadge(final FLEFRecord parent){
		final FLEFRecord evidence = FLEFRecordHelper.findChild(parent, TAG_EVIDENCE);
		if(evidence == null)
			return "";

		final String sourceType = FLEFRecordHelper.getChildValue(evidence, TAG_SOURCE_TYPE);
		final String infoType = FLEFRecordHelper.getChildValue(evidence, TAG_INFORMATION_TYPE);
		final String evidenceType = FLEFRecordHelper.getChildValue(evidence, TAG_EVIDENCE_TYPE);

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(sourceType))
			sb.append(abbreviate(sourceType));
		if(StringUtils.isNotEmpty(infoType)){
			if(!sb.isEmpty())
				sb.append('/');
			sb.append(abbreviate(infoType));
		}
		if(StringUtils.isNotEmpty(evidenceType)){
			if(!sb.isEmpty())
				sb.append('/');
			sb.append(abbreviate(evidenceType));
		}
		return sb.toString();
	}

	private static String abbreviate(final String value){
		return switch(value.toLowerCase(Locale.ROOT)){
			case "original" -> "orig";
			case "derived" -> "der";
			case "primary" -> "prim";
			case "secondary" -> "sec";
			case "undetermined" -> "und";
			case "direct" -> "dir";
			case "indirect" -> "ind";
			case "negative" -> "neg";
			default -> value;
		};
	}

}
