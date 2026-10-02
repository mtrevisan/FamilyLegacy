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
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.names.NameAnatomyService;
import io.github.mtrevisan.familylegacy.io.model.readers.names.NamePart;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.github.mtrevisan.familylegacy.ui.tools.sources.SourceHelper.TAG_DESCRIPTION;


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

	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_NOTE = "note";
	private static final String TAG_EVIDENCE = "evidence";


	private final FLEFModel model;
	private final IndividualHandler individualHandler;
	private final Map<String, List<FLEFRecord>> eventMap;

	private final DossierFormatting formatting;

	/** Reverse index: assertion record id -> proof status. */
	private final Map<String, ProofStatus> conclusionIndex;


	public IndividualDossierService(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		this.model = model;
		this.individualHandler = IndividualHandler.getInstance();
		this.eventMap = buildEventMap();

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
		final List<Name> names = NameAnatomyService.extractForIndividual(individual);
		for(final Name name : names){
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
			for(final FLEFRecord nameStruct : FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_NAME)){
				final String value = extractNameValue(nameStruct);
				if(StringUtils.isNotEmpty(value)){
					final String type = FLEFRecordHelper.getChildValue(nameStruct, NameReader.TAG_TYPE);
					final String label = (StringUtils.isNotEmpty(type)
						? type.replace('_', ' ')
						: "Name");
					entries.add(DossierEntry.of(label, value, individual));
				}
			}
		}

		// Preferred image.
		final String uri = IndividualReader.extractPreferredImageUri(individual);
		if(StringUtils.isNotEmpty(uri))
			entries.add(DossierEntry.of("Photo", uri, individual));

		// Sex.
		final String sex = IndividualReader.extractRawSex(individual);
		if(StringUtils.isNotEmpty(sex))
			entries.add(DossierEntry.of("Sex", sex, individual));

		// Vital dates.
		if(data != null && !data.isEmpty()){
			final String info = data.getInfoText();
			if(StringUtils.isNotEmpty(info))
				entries.add(DossierEntry.of("Vital", info, individual));
		}

		// Privacy.
		final FLEFRecord privacy = FLEFRecordHelper.findChild(individual, IndividualReader.TAG_PRIVACY);
		if(privacy != null){
			final String level = FLEFRecordHelper.getChildValue(privacy, PrivacyReader.TAG_LEVEL);
			if(StringUtils.isNotEmpty(level))
				entries.add(DossierEntry.of("Privacy", level, individual));
		}

		return entries;
	}

	private static String extractNameValue(final FLEFRecord nameStruct){
		final String direct = FLEFRecordHelper.getChildValue(nameStruct, NameReader.TAG_VALUE);
		if(StringUtils.isNotEmpty(direct))
			return direct;

		final StringBuilder sb = new StringBuilder();
		for(final FLEFRecord part : FLEFRecordHelper.findChildren(nameStruct, "part")){
			final String v = FLEFRecordHelper.getChildValue(part, NameReader.TAG_PART_VALUE);
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
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final String participantId = extractParticipantId(eventParticipation);
			if(!individualId.equals(participantId))
				continue;

			final DossierEntry entry = buildEventEntry(eventParticipation);
			if(entry != null)
				entries.add(entry);
		}
		return entries;
	}

	private DossierEntry buildEventEntry(final FLEFRecord eventParticipation){
		final String eventId = FLEFRecordHelper.getChildValue(eventParticipation, EventParticipationReader.TAG_EVENT);
		if(eventId == null)
			return null;

		final FLEFRecord event = model.getRecordById(eventId);
		if(event == null)
			return null;

		final String eventType = FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE);
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
		final String role = FLEFRecordHelper.getChildValue(eventParticipation, EventParticipationReader.TAG_ROLE);
		if(StringUtils.isNotEmpty(role))
			subtitle.append("role: ").append(role);
		final String agency = FLEFRecordHelper.getChildValue(event, EventReader.TAG_AGENCY);
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
		final List<FLEFRecord> individualAttributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(final FLEFRecord individualAttribute : individualAttributes){
			final String ownerId = FLEFRecordHelper.getChildValue(individualAttribute, IndividualHandler.TYPE);
			if(!individualId.equals(ownerId))
				continue;

			entries.add(buildAttributeEntry(individualAttribute));
		}
		return entries;
	}

	private DossierEntry buildAttributeEntry(final FLEFRecord individualAttribute){
		final String type = FLEFRecordHelper.getChildValue(individualAttribute, IndividualAttributeReader.TAG_TYPE);
		final String label = (StringUtils.isNotEmpty(type)
			? type.replace('_', ' ')
			: "Attribute");
		final String value = FLEFRecordHelper.getChildValue(individualAttribute, IndividualAttributeReader.TAG_VALUE);

		final String validity = formatting.formatValidity(individualAttribute);
		final String place = formatting.resolvePlaceName(individualAttribute);
		final StringBuilder subtitle = new StringBuilder();
		if(StringUtils.isNotEmpty(validity))
			subtitle.append(validity);
		if(StringUtils.isNotEmpty(place)){
			if(!subtitle.isEmpty())
				subtitle.append(" · ");
			subtitle.append(place);
		}

		return new DossierEntry(label, (value != null? value: StringUtils.EMPTY), subtitle.toString(),
			buildEvidenceBadge(individualAttribute), individualAttribute, DossierEntry.Kind.NORMAL,
			proofStatusFor(individualAttribute));
	}


	/* ======================================================================
	 *                          Relationships
	 * ====================================================================== */

	private List<DossierEntry> buildRelationships(final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = extractParticipantRef(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractParticipantRef(relationship, RelationshipReader.TAG_OBJECT);
			final boolean isSubject = individualId.equals(subjectId);
			final boolean isTarget = individualId.equals(objectId);
			if(!isSubject && !isTarget)
				continue;

			entries.add(buildRelationshipEntry(relationship, isSubject));
		}
		return entries;
	}

	private DossierEntry buildRelationshipEntry(final FLEFRecord relationship, final boolean isSubject){
		final String type = RelationshipReader.extractType(relationship);
		final String typeLabel = (StringUtils.isNotEmpty(type)
			? type.replace('_', ' ')
			: "Relationship");

		final String otherId = (isSubject
			? extractParticipantRef(relationship, RelationshipReader.TAG_OBJECT)
			: extractParticipantRef(relationship, RelationshipReader.TAG_SUBJECT));
		final String otherName = resolveParticipantName(otherId);

		final String direction = (isSubject? "→ ": "← ");
		final String label = direction + typeLabel;
		final String value = (otherName != null? otherName: otherId != null? otherId: "?");

		final StringBuilder subtitle = new StringBuilder();
		final String role = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE);
		if(StringUtils.isNotEmpty(role))
			subtitle.append("role: ").append(role);
		final String status = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_STATUS);
		if(StringUtils.isNotEmpty(status)){
			if(!subtitle.isEmpty())
				subtitle.append(" · ");
			subtitle.append(status);
		}
		final String validity = formatting.formatValidity(relationship);
		if(StringUtils.isNotEmpty(validity)){
			if(!subtitle.isEmpty())
				subtitle.append(" · ");
			subtitle.append(validity);
		}

		return new DossierEntry(label, value, subtitle.toString(),
			buildEvidenceBadge(relationship), relationship, DossierEntry.Kind.NORMAL,
			proofStatusFor(relationship));
	}


	/* ======================================================================
	 *                          Sources
	 * ====================================================================== */

	private List<DossierEntry> buildSources(final FLEFRecord individual, final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		final Set<String> seenSourceIds = new HashSet<>();

		collectSources(individual, IndividualHandler.TYPE, entries, seenSourceIds);

		for(final FLEFRecord event : eventMap.getOrDefault(individualId, List.of()))
			collectSources(event, "Event", entries, seenSourceIds);

		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(final FLEFRecord attribute : attributes){
			final String ownerId = FLEFRecordHelper.getChildValue(attribute, IndividualHandler.TYPE);
			if(individualId.equals(ownerId))
				collectSources(attribute, "Attribute", entries, seenSourceIds);
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = extractParticipantRef(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractParticipantRef(relationship, RelationshipReader.TAG_OBJECT);
			if(individualId.equals(subjectId) || individualId.equals(objectId))
				collectSources(relationship, "Relationship", entries, seenSourceIds);
		}

		final List<FLEFRecord> impacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord impact : impacts){
			final String targetId = extractRef(impact, ContextImpactReader.TAG_TARGET);
			if(individualId.equals(targetId))
				collectSources(impact, "Context", entries, seenSourceIds);
		}

		return entries;
	}

	private void collectSources(final FLEFRecord parent, final String origin,
		final List<DossierEntry> entries, final Set<String> seenSourceIds){
		final List<FLEFRecord> citations = FLEFRecordHelper.findChildren(parent, TAG_SOURCE);
		for(final FLEFRecord citation : citations){
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
				: StringUtils.EMPTY);

			entries.add(new DossierEntry(label, title, subtitle,
				buildEvidenceBadge(citation), source, DossierEntry.Kind.NORMAL,
				proofStatusFor(citation)));
		}
	}

	private static String resolveSourceTitle(final FLEFRecord source){
		final FLEFRecord titleStruct = FLEFRecordHelper.findChild(source, SourceReader.TAG_TITLE);
		if(titleStruct != null){
			final String value = FLEFRecordHelper.getChildValue(titleStruct, NameReader.TAG_VALUE);
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
		final List<FLEFRecord> impacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord impact : impacts){
			final String targetId = extractRef(impact, ContextImpactReader.TAG_TARGET);
			if(!individualId.equals(targetId))
				continue;

			final String contextId = extractRef(impact, ContextImpactReader.TAG_CONTEXT);
			if(contextId == null)
				continue;

			final FLEFRecord context = model.getRecordById(contextId);
			if(context == null)
				continue;

			final String impactType = FLEFRecordHelper.getChildValue(impact, ContextImpactReader.TAG_IMPACT_TYPE);
			final String label = (StringUtils.isNotEmpty(impactType)? impactType: "context");
			final String contextTitle = resolveContextTitle(context);
			final String rationale = FLEFRecordHelper.getChildValue(impact, ContextImpactReader.TAG_RATIONALE);
			final String subtitle = (StringUtils.isNotEmpty(rationale)
				? rationale
				: context.getTag() != null? context.getTag().replace('_', ' '): StringUtils.EMPTY);

			entries.add(new DossierEntry(label, contextTitle, subtitle,
				buildEvidenceBadge(impact), context, DossierEntry.Kind.NORMAL,
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
	 *                          Identity hypotheses
	 * ====================================================================== */

	private List<DossierEntry> buildIdentityHypotheses(final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();
		final List<FLEFRecord> hypotheses = model.getRecordsByType(IdentityHypothesisHandler.TYPE);
		for(final FLEFRecord hypothesis : hypotheses){
			final List<String> candidateIds = new ArrayList<>();
			for(final FLEFRecord candidate : FLEFRecordHelper.findChildren(hypothesis, IdentityHypothesisReader.TAG_IDENTITY)){
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
			final String comment = FLEFRecordHelper.getChildValue(hypothesis, IdentityHypothesisReader.TAG_COMMENT);

			entries.add(new DossierEntry("Possible duplicate",
				(otherName != null? otherName: otherId),
				(comment != null? comment: StringUtils.EMPTY),
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

		final List<FLEFRecord> questions = model.getRecordsByType(ResearchQuestionHandler.TYPE);
		for(final FLEFRecord question : questions){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET))
				if(individualId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets)
				continue;

			final String title = FLEFRecordHelper.getChildValue(question, ResearchQuestionReader.TAG_TITLE);
			final String questionText = FLEFRecordHelper.getChildValue(question, ResearchQuestionReader.TAG_QUESTION);
			final String status = FLEFRecordHelper.getChildValue(question, ResearchQuestionReader.TAG_STATUS);

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
				subtitle.toString(), StringUtils.EMPTY, question, DossierEntry.Kind.NORMAL,
				proofStatusFor(question)));
		}

		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES))
				if(individualId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets && individualId.equals(extractRef(conclusion, ConclusionReader.TAG_PREFERRED)))
				targets = true;
			if(!targets)
				continue;

			final String issue = FLEFRecordHelper.getChildValue(conclusion, "issue");
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

	private List<DossierEntry> buildNotes(final FLEFRecord individual, final String individualId){
		final List<DossierEntry> entries = new ArrayList<>();

		collectNotes(individual, IndividualHandler.TYPE, entries);

		for(final FLEFRecord event : eventMap.getOrDefault(individualId, List.of()))
			collectNotes(event, "Event", entries);

		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(final FLEFRecord attribute : attributes){
			final String ownerId = FLEFRecordHelper.getChildValue(attribute, IndividualHandler.TYPE);
			if(individualId.equals(ownerId))
				collectNotes(attribute, "Attribute", entries);
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = extractParticipantRef(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractParticipantRef(relationship, RelationshipReader.TAG_OBJECT);
			if(individualId.equals(subjectId) || individualId.equals(objectId))
				collectNotes(relationship, "Relationship", entries);
		}

		final List<FLEFRecord> impacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord impact : impacts){
			final String targetId = extractRef(impact, ContextImpactReader.TAG_TARGET);
			if(individualId.equals(targetId))
				collectNotes(impact, "Context", entries);
		}

		final List<FLEFRecord> hypotheses = model.getRecordsByType(IdentityHypothesisHandler.TYPE);
		for(final FLEFRecord hypothesis : hypotheses){
			boolean targets = false;
			for(final FLEFRecord candidate : FLEFRecordHelper.findChildren(hypothesis, IdentityHypothesisReader.TAG_IDENTITY))
				if(individualId.equals(extractRef(candidate, null))){
					targets = true;
					break;
				}
			if(targets)
				collectNotes(hypothesis, "Identity hypothesis", entries);
		}

		final List<FLEFRecord> questions = model.getRecordsByType(ResearchQuestionHandler.TYPE);
		for(final FLEFRecord question : questions){
			boolean targets = false;
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET))
				if(individualId.equals(extractRef(target, null))){
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
				if(individualId.equals(extractRef(target, null))){
					targets = true;
					break;
				}
			if(!targets && individualId.equals(extractRef(conclusion, ConclusionReader.TAG_PREFERRED)))
				targets = true;
			if(targets)
				collectNotes(conclusion, "Conclusion", entries);
		}

		return entries;
	}

	private void collectNotes(final FLEFRecord parent, final String origin, final List<DossierEntry> entries){
		for(final FLEFRecord note : FLEFRecordHelper.findChildren(parent, TAG_NOTE)){
			final String title = FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TITLE);
			final String text = NoteReader.extractText(note);
			final String label = origin + " note";
			final String value = (StringUtils.isNotEmpty(title)? title: text);
			final String subtitle = (StringUtils.isNotEmpty(title) && StringUtils.isNotEmpty(text)
				? text
				: StringUtils.EMPTY);

			entries.add(new DossierEntry(label, value, subtitle, StringUtils.EMPTY, parent,
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
	 *                          Event index
	 * ====================================================================== */

	private Map<String, List<FLEFRecord>> buildEventMap(){
		final Map<String, List<FLEFRecord>> result = new HashMap<>();
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final String eventParticipantId = extractParticipantId(eventParticipation);
			if(eventParticipantId == null)
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(eventParticipation, EventParticipationReader.TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event != null)
				result.computeIfAbsent(eventParticipantId, k -> new ArrayList<>()).add(event);
		}
		return result;
	}

	private static String extractParticipantId(final FLEFRecord eventParticipation){
		final FLEFRecord eventParticipantField = FLEFRecordHelper.findChild(eventParticipation, EventParticipationReader.TAG_PARTICIPANT);
		if(eventParticipantField == null)
			return null;

		final FLEFRecord ref = eventParticipantField.getTheOnlyChild();
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
			if(GroupHandler.TYPE.equalsIgnoreCase(tag))
				return GroupHandler.getInstance()
					.getDisplayText(record, model);
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
			return StringUtils.EMPTY;

		final String sourceType = EvidenceQualifiersReader.extractSourceType(evidence);
		final String infoType = EvidenceQualifiersReader.extractInformationType(evidence);
		final String evidenceType = EvidenceQualifiersReader.extractEvidenceType(evidence);

		final StringBuilder sb = new StringBuilder();
		if(StringUtils.isNotEmpty(sourceType))
			sb.append(I18N.t("enum.evidence.source.type.abbreviation." + sourceType));
		if(StringUtils.isNotEmpty(infoType)){
			if(!sb.isEmpty())
				sb.append('/');
			sb.append(I18N.t("enum.evidence.information.type.abbreviation." + infoType));
		}
		if(StringUtils.isNotEmpty(evidenceType)){
			if(!sb.isEmpty())
				sb.append('/');
			sb.append(I18N.t("enum.evidence.evidence.type.abbreviation." + evidenceType));
		}
		return sb.toString();
	}

}
