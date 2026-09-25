package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


/**
 * Pre-computed index of the relationships, events, attributes, groups,
 * place relationships, event participations, context impacts, research
 * questions and source citations of a {@link FLEFModel}.
 *
 * <p>The index is built once by {@link #build(FLEFModel, Predicate)} and
 * queried read-only. Records rejected by the visibility predicate are
 * skipped entirely.</p>
 */
final class RelationIndex{

	/* ======================================================================
	 *                          Record type tags
	 * ====================================================================== */

	private static final String TYPE_EVENT = "event";
	private static final String TYPE_EVENT_PARTICIPATION = "event_participation";
	private static final String TYPE_INDIVIDUAL_ATTRIBUTE = "individual_attribute";
	private static final String TYPE_GROUP_ATTRIBUTE = "group_attribute";
	private static final String TYPE_PLACE_RELATIONSHIP = "place_relationship";
	private static final String TYPE_CONTEXT_IMPACT = "context_impact";
	private static final String TYPE_RESEARCH_QUESTION = "research_question";
	private static final String TYPE_SOURCE = "source";

	/* ======================================================================
	 *                          Field tags
	 * ====================================================================== */

	private static final String TAG_TYPE = "type";
	private static final String TAG_EVENT = "event";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_GROUP = "group";
	private static final String TAG_PLACE = "place";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_SOURCE = "source";
	private static final String TAG_DOCUMENT = "document";
	private static final String TAG_REPOSITORY = "repository";
	private static final String TAG_ROLE = "role";
	private static final String TAG_VOID = "void";

	private static final String PATH_SUBJECT_PLACE = "subject.place";
	private static final String PATH_TARGET_PLACE = "target.place";
	private static final String PATH_PLACE_PLACE = "place.place";

	private static final String REL_GROUP_MEMBER = "group_member";
	private static final String REL_PART_OF = "part_of";
	private static final String REL_ASSOCIATE = "associate";


	/* ======================================================================
	 *                          Nested types
	 * ====================================================================== */

	enum IndirectKind{
		SPOUSE,
		ASSOCIATE
	}

	record IndirectRelation(FLEFRecord individual, FLEFRecord via, IndirectKind kind){
	}

	record ParentEdge(FLEFRecord parent, String relationshipType){
	}

	record Ref(String tag, String id){
	}

	/**
	 * A participant of an event: the referenced record, its oneof branch
	 * kind, the role it plays (may be {@code null}), and the participation
	 * record itself (which carries its own sources, notes, evidence,
	 * privacy and audit).
	 */
	record Participant(FLEFRecord record, String kind, String role, FLEFRecord participation){
	}


	/* ======================================================================
	 *                          Indexed state
	 * ====================================================================== */

	private final FLEFModel model;

	// Family relationships.
	private final Map<String, List<String>> parentsByChild = new HashMap<>();
	private final Map<String, List<String>> childrenByParent = new HashMap<>();
	private final Map<String, List<String>> spousesByIndividual = new HashMap<>();
	private final Map<String, List<String>> associatesByIndividual = new HashMap<>();
	private final Map<String, List<ParentEdge>> parentEdgesByChild = new HashMap<>();
	private final Map<String, List<FLEFRecord>> relationshipsBySubject = new HashMap<>();

	// Events and attributes of individuals.
	private final Map<String, List<FLEFRecord>> eventsByIndividual = new HashMap<>();
	private final Map<String, List<FLEFRecord>> attributesByIndividual = new HashMap<>();

	// Groups.
	private final Map<String, List<FLEFRecord>> groupMembershipsByIndividual = new HashMap<>();
	private final Map<String, List<FLEFRecord>> groupMembershipsByGroup = new HashMap<>();
	private final Map<String, List<FLEFRecord>> parentGroupsByChildGroup = new HashMap<>();
	private final Map<String, List<FLEFRecord>> childGroupsByParentGroup = new HashMap<>();
	private final Map<String, List<FLEFRecord>> groupAttributesByGroup = new HashMap<>();
	private final Map<String, List<FLEFRecord>> eventsByGroup = new HashMap<>();

	// Place relationships.
	private final Map<String, List<FLEFRecord>> placeRelationshipsBySubject = new HashMap<>();
	private final Map<String, List<FLEFRecord>> placeRelationshipsByTarget = new HashMap<>();

	// Events and attributes touching a place.
	private final Map<String, List<FLEFRecord>> eventsAtPlace = new HashMap<>();
	private final Map<String, List<FLEFRecord>> eventsByPlace = new HashMap<>();
	private final Map<String, List<FLEFRecord>> attributesAtPlace = new HashMap<>();

	// Event participations.
	private final Map<String, List<FLEFRecord>> participationsByParticipant = new HashMap<>();
	private final Map<String, List<FLEFRecord>> participationsByEvent = new HashMap<>();

	// Contextual references.
	private final Map<String, List<FLEFRecord>> contextImpactsByTarget = new HashMap<>();
	private final Map<String, List<FLEFRecord>> researchQuestionsByTarget = new HashMap<>();

	// Source indexing.
	private final Map<String, List<FLEFRecord>> citationsBySource = new HashMap<>();
	private final Map<String, List<FLEFRecord>> documentsBySource = new HashMap<>();
	private final Map<String, List<FLEFRecord>> repositoriesBySource = new HashMap<>();
	private final Map<String, List<FLEFRecord>> sourcesByRepository = new HashMap<>();
	private final Map<String, List<FLEFRecord>> repositoryCitationsByRepository = new HashMap<>();

	// ----- document-centric queries -----------------------------------------
	private final Map<String, List<FLEFRecord>> sourcesByDocument = new HashMap<>();
	private final Map<String, List<FLEFRecord>> citationsByDocument = new HashMap<>();

	// ----- research-question-centric queries --------------------------------
	private final Map<String, List<FLEFRecord>> activitiesByQuestion = new HashMap<>();
	private final Map<String, List<FLEFRecord>> tasksByQuestion = new HashMap<>();
	private final Map<String, List<FLEFRecord>> conclusionsByQuestion = new HashMap<>();

	private final Map<String, List<FLEFRecord>> associatesByParticipant = new HashMap<>();

	private final Map<String, List<FLEFRecord>> tasksByActivity = new HashMap<>();


	private RelationIndex(final FLEFModel model){
		this.model = model;
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	static RelationIndex build(final FLEFModel model){
		return build(model, r -> true);
	}

	static RelationIndex build(final FLEFModel model, final Predicate<FLEFRecord> visible){
		final RelationIndex idx = new RelationIndex(model);

		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			if(!visible.test(rel)) continue;
			idx.indexRelationship(rel, visible);
		}

		for(final FLEFRecord ep : model.getRecordsByType(TYPE_EVENT_PARTICIPATION)){
			if(!visible.test(ep)) continue;
			idx.indexEventParticipation(ep, visible);
		}

		for(final FLEFRecord attr : model.getRecordsByType(TYPE_INDIVIDUAL_ATTRIBUTE)){
			if(!visible.test(attr)) continue;
			final String indId = attr.extractReferencedId(TAG_INDIVIDUAL, IndividualHandler.TYPE);
			if(indId != null)
				idx.attributesByIndividual.computeIfAbsent(indId, k -> new ArrayList<>()).add(attr);
			idx.indexPlaceOf(attr, idx.attributesAtPlace, visible);
		}

		for(final FLEFRecord ga : model.getRecordsByType(TYPE_GROUP_ATTRIBUTE)){
			if(!visible.test(ga)) continue;
			final String groupId = ga.extractReferencedId(TAG_GROUP, TAG_GROUP);
			if(groupId != null)
				idx.groupAttributesByGroup.computeIfAbsent(groupId, k -> new ArrayList<>()).add(ga);
			idx.indexPlaceOf(ga, idx.attributesAtPlace, visible);
		}

		for(final FLEFRecord evt : model.getRecordsByType(TYPE_EVENT)){
			if(!visible.test(evt)) continue;
			idx.indexPlaceOf(evt, idx.eventsAtPlace, visible);
		}

		for(final FLEFRecord pr : model.getRecordsByType(TYPE_PLACE_RELATIONSHIP)){
			if(!visible.test(pr)) continue;
			final String subj = FLEFRecordHelper.getChildValue(pr, PATH_SUBJECT_PLACE);
			final String targ = FLEFRecordHelper.getChildValue(pr, PATH_TARGET_PLACE);
			if(subj == null || targ == null) continue;
			if(!visible.test(model.getRecordById(subj)) || !visible.test(model.getRecordById(targ)))
				continue;
			idx.placeRelationshipsBySubject.computeIfAbsent(subj, k -> new ArrayList<>()).add(pr);
			idx.placeRelationshipsByTarget.computeIfAbsent(targ, k -> new ArrayList<>()).add(pr);
		}

		for(final FLEFRecord ci : model.getRecordsByType(TYPE_CONTEXT_IMPACT)){
			if(!visible.test(ci)) continue;
			idx.indexContextImpact(ci);
		}

		for(final FLEFRecord q : model.getRecordsByType(TYPE_RESEARCH_QUESTION)){
			if(!visible.test(q)) continue;
			idx.indexResearchQuestion(q);
		}

		idx.indexSources(visible);

		idx.indexDocuments();
		idx.indexQuestions();

		return idx;
	}

	/**
	 * Indexes a record's {@code place.place} reference into the given map.
	 * Shared by events and attributes at a place.
	 */
	private void indexPlaceOf(final FLEFRecord rec, final Map<String, List<FLEFRecord>> target,
		final Predicate<FLEFRecord> visible){
		final String pid = FLEFRecordHelper.getChildValue(rec, PATH_PLACE_PLACE);
		if(pid == null)
			return;
		if(!visible.test(model.getRecordById(pid)))
			return;
		target.computeIfAbsent(pid, k -> new ArrayList<>()).add(rec);
	}


	/* ======================================================================
	 *                          Indexing helpers
	 * ====================================================================== */

	private void indexRelationship(final FLEFRecord rel, final Predicate<FLEFRecord> visible){
		final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
		if(type == null) return;
		final String t = type.toLowerCase(Locale.ROOT);

		if(REL_GROUP_MEMBER.equals(t)){
			indexGroupMembership(rel, visible);
			return;
		}
		if(REL_PART_OF.equals(t)){
			indexPartOf(rel, visible);
			return;
		}

		final String subj = rel.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
		final String targ = rel.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
		if(subj == null || targ == null) return;

		relationshipsBySubject.computeIfAbsent(subj, k -> new ArrayList<>()).add(rel);

		final boolean childRel = t.endsWith("_child");
		final boolean parentRel = t.endsWith("_parent");
		if(childRel || parentRel){
			final String childId = (childRel? subj: targ);
			final String parentId = (childRel? targ: subj);
			parentsByChild.computeIfAbsent(childId, k -> new ArrayList<>()).add(parentId);
			childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
			final FLEFRecord parent = model.getRecordById(parentId);
			if(parent != null && visible.test(parent))
				parentEdgesByChild.computeIfAbsent(childId, k -> new ArrayList<>())
					.add(new ParentEdge(parent, t));
			return;
		}

		final boolean spouseRel = t.endsWith("_spouse") || t.endsWith("_partner");
		if(spouseRel){
			spousesByIndividual.computeIfAbsent(subj, k -> new ArrayList<>()).add(targ);
			spousesByIndividual.computeIfAbsent(targ, k -> new ArrayList<>()).add(subj);
			return;
		}

		if(REL_ASSOCIATE.equals(t)){
			// Subject may be individual or group; target may be individual or group.
			final Ref subjRef = extractOneOf(rel, TAG_SUBJECT);
			final Ref targRef = extractOneOf(rel, TAG_TARGET);
			if(subjRef != null && targRef != null){
				associatesByParticipant.computeIfAbsent(subjRef.id(), k -> new ArrayList<>()).add(rel);
				associatesByParticipant.computeIfAbsent(targRef.id(), k -> new ArrayList<>()).add(rel);
			}
		}
	}

	private void indexGroupMembership(final FLEFRecord rel, final Predicate<FLEFRecord> visible){
		final Ref subj = extractOneOf(rel, TAG_SUBJECT);
		final Ref targ = extractOneOf(rel, TAG_TARGET);
		if(subj == null || targ == null) return;
		if(!TAG_INDIVIDUAL.equals(subj.tag()) || !TAG_GROUP.equals(targ.tag())) return;
		final FLEFRecord group = model.getRecordById(targ.id());
		if(group == null || !visible.test(group)) return;
		groupMembershipsByIndividual.computeIfAbsent(subj.id(), k -> new ArrayList<>()).add(rel);
		groupMembershipsByGroup.computeIfAbsent(targ.id(), k -> new ArrayList<>()).add(rel);
	}

	private void indexPartOf(final FLEFRecord rel, final Predicate<FLEFRecord> visible){
		final Ref subj = extractOneOf(rel, TAG_SUBJECT);
		final Ref targ = extractOneOf(rel, TAG_TARGET);
		if(subj == null || targ == null) return;
		if(!TAG_GROUP.equals(subj.tag()) || !TAG_GROUP.equals(targ.tag())) return;
		final FLEFRecord parent = model.getRecordById(targ.id());
		final FLEFRecord child = model.getRecordById(subj.id());
		if(parent == null || !visible.test(parent)) return;
		if(child == null || !visible.test(child)) return;
		parentGroupsByChildGroup.computeIfAbsent(subj.id(), k -> new ArrayList<>()).add(rel);
		childGroupsByParentGroup.computeIfAbsent(targ.id(), k -> new ArrayList<>()).add(rel);
	}

	private void indexEventParticipation(final FLEFRecord ep, final Predicate<FLEFRecord> visible){
		final Ref participant = extractOneOf(ep, TAG_PARTICIPANT);
		final String evtId = FLEFRecordHelper.getChildValue(ep, TAG_EVENT);
		if(participant == null || evtId == null) return;

		final FLEFRecord evt = model.getRecordById(evtId);
		if(evt == null || !visible.test(evt)) return;

		final FLEFRecord participantRec = model.getRecordById(participant.id());
		if(participantRec == null || !visible.test(participantRec)) return;

		participationsByParticipant.computeIfAbsent(participant.id(), k -> new ArrayList<>()).add(ep);
		participationsByEvent.computeIfAbsent(evtId, k -> new ArrayList<>()).add(ep);

		if(TAG_INDIVIDUAL.equals(participant.tag()))
			eventsByIndividual.computeIfAbsent(participant.id(), k -> new ArrayList<>()).add(evt);
		else if(TAG_GROUP.equals(participant.tag()))
			eventsByGroup.computeIfAbsent(participant.id(), k -> new ArrayList<>()).add(evt);
		else if(TAG_PLACE.equals(participant.tag()))
			eventsByPlace.computeIfAbsent(participant.id(), k -> new ArrayList<>()).add(evt);
	}

	private void indexContextImpact(final FLEFRecord ci){
		for(final FLEFRecord t : FLEFRecordHelper.findChildren(ci, TAG_TARGET)){
			final FLEFRecord ref = t.getTheOnlyChild();
			if(ref == null || TAG_VOID.equalsIgnoreCase(ref.getTag())) continue;
			final String id = ref.getValue();
			if(id != null && !id.isBlank())
				contextImpactsByTarget.computeIfAbsent(id, k -> new ArrayList<>()).add(ci);
		}
	}

	private void indexResearchQuestion(final FLEFRecord q){
		for(final FLEFRecord t : FLEFRecordHelper.findChildren(q, TAG_TARGET)){
			final FLEFRecord ref = t.getTheOnlyChild();
			if(ref == null || TAG_VOID.equalsIgnoreCase(ref.getTag())) continue;
			final String id = ref.getValue();
			if(id != null && !id.isBlank())
				researchQuestionsByTarget.computeIfAbsent(id, k -> new ArrayList<>()).add(q);
		}
	}

	/**
	 * Indexes every source, its citations, its documents and its repository
	 * references. Also builds the reverse index from repository to the
	 * sources stored there.
	 */
	private void indexSources(final Predicate<FLEFRecord> visible){
		// Citations anywhere in the tree.
		for(final FLEFRecord rec : model.getRecords()){
			for(final FLEFRecord cit : collectDescendantsWithTag(rec, TAG_SOURCE)){
				final String sid = ReportFormatters.extractSourceId(cit);
				if(sid == null) continue;
				if(!visible.test(model.getRecordById(sid))) continue;
				citationsBySource.computeIfAbsent(sid, k -> new ArrayList<>()).add(cit);
			}
		}

		// Documents, repositories and reverse-index.
		for(final FLEFRecord src : model.getRecordsByType(TYPE_SOURCE)){
			if(!visible.test(src)) continue;
			for(final FLEFRecord d : FLEFRecordHelper.findChildren(src, TAG_DOCUMENT))
				if(d.getValue() != null)
					documentsBySource.computeIfAbsent(src.getId(), k -> new ArrayList<>()).add(d);
			for(final FLEFRecord r : FLEFRecordHelper.findChildren(src, TAG_REPOSITORY)){
				repositoriesBySource.computeIfAbsent(src.getId(), k -> new ArrayList<>()).add(r);
				final String rid = r.extractReferencedId(TAG_REPOSITORY, TAG_REPOSITORY);
				if(rid != null && visible.test(model.getRecordById(rid))){
					sourcesByRepository.computeIfAbsent(rid, k -> new ArrayList<>()).add(src);
					repositoryCitationsByRepository.computeIfAbsent(rid, k -> new ArrayList<>()).add(r);
				}
			}
		}
	}

	private void indexDocuments(){
		for(final FLEFRecord src : model.getRecordsByType("source")){
			for(final FLEFRecord docRef : FLEFRecordHelper.findChildren(src, "document")){
				final String did = docRef.getValue();
				if(did != null)
					sourcesByDocument.computeIfAbsent(did, k -> new ArrayList<>()).add(src);
			}
		}
		for(final FLEFRecord rec : model.getRecords()){
			for(final FLEFRecord cit : collectDescendantsWithTag(rec, "source")){
				for(final FLEFRecord dp : FLEFRecordHelper.findChildren(cit,
					"extract.document_part.document")){
					final String did = dp.getValue();
					if(did != null)
						citationsByDocument.computeIfAbsent(did, k -> new ArrayList<>()).add(cit);
				}
			}
		}
	}

	private void indexQuestions(){
		for(final FLEFRecord a : model.getRecordsByType("research_activity")){
			for(final FLEFRecord q : FLEFRecordHelper.findChildren(a, "question"))
				if(q.getValue() != null)
					activitiesByQuestion.computeIfAbsent(q.getValue(), k -> new ArrayList<>()).add(a);
		}
		for(final FLEFRecord t : model.getRecordsByType("research_task")){
			for(final FLEFRecord q : FLEFRecordHelper.findChildren(t, "question"))
				if(q.getValue() != null)
					tasksByQuestion.computeIfAbsent(q.getValue(), k -> new ArrayList<>()).add(t);
		}
		for(final FLEFRecord c : model.getRecordsByType("conclusion")){
			for(final FLEFRecord q : FLEFRecordHelper.findChildren(c, "research"))
				if(q.getValue() != null)
					conclusionsByQuestion.computeIfAbsent(q.getValue(), k -> new ArrayList<>()).add(c);
		}
		for(final FLEFRecord t : model.getRecordsByType("research_task")){
			final String cid = FLEFRecordHelper.getChildValue(t, "created_by");
			if(cid != null)
				tasksByActivity.computeIfAbsent(cid, k -> new ArrayList<>()).add(t);
		}
	}

	private static List<FLEFRecord> collectDescendantsWithTag(final FLEFRecord rec, final String tag){
		final List<FLEFRecord> out = new ArrayList<>();
		collectDescendantsWithTag(rec, tag, out);
		return out;
	}

	private static void collectDescendantsWithTag(final FLEFRecord rec, final String tag,
		final List<FLEFRecord> out){
		for(final FLEFRecord child : rec.getChildren()){
			if(tag.equalsIgnoreCase(child.getTag()))
				out.add(child);
			collectDescendantsWithTag(child, tag, out);
		}
	}


	/* ======================================================================
	 *                          Document queries
	 * ====================================================================== */

	List<FLEFRecord> sourcesOfDocument(final FLEFRecord document){
		return sourcesByDocument.getOrDefault(document.getId(), List.of());
	}

	List<FLEFRecord> citationsOfDocument(final FLEFRecord document){
		return citationsByDocument.getOrDefault(document.getId(), List.of());
	}


	/* ======================================================================
	 *                          Research question queries
	 * ====================================================================== */

	List<FLEFRecord> activitiesForQuestion(final FLEFRecord question){
		return activitiesByQuestion.getOrDefault(question.getId(), List.of());
	}

	List<FLEFRecord> tasksForQuestion(final FLEFRecord question){
		return tasksByQuestion.getOrDefault(question.getId(), List.of());
	}

	List<FLEFRecord> conclusionsForQuestion(final FLEFRecord question){
		return conclusionsByQuestion.getOrDefault(question.getId(), List.of());
	}

	/** Every {@code associate} relationship in which the record is subject or target. */
	List<FLEFRecord> associatesOfAny(final FLEFRecord record){
		return associatesByParticipant.getOrDefault(record.getId(), List.of());
	}

	/** Tasks generated by the given activity. */
	List<FLEFRecord> tasksForActivity(final FLEFRecord activity){
		return tasksByActivity.getOrDefault(activity.getId(), List.of());
	}


	/* ======================================================================
	 *                          Family relationships
	 * ====================================================================== */

	List<FLEFRecord> parentsOf(final FLEFRecord ind){
		return records(parentsByChild.getOrDefault(ind.getId(), List.of()));
	}

	List<FLEFRecord> childrenOf(final FLEFRecord ind){
		return records(childrenByParent.getOrDefault(ind.getId(), List.of()));
	}

	List<FLEFRecord> spousesOf(final FLEFRecord ind){
		return records(spousesByIndividual.getOrDefault(ind.getId(), List.of()));
	}

	List<FLEFRecord> associatesOf(final FLEFRecord ind){
		return records(associatesByIndividual.getOrDefault(ind.getId(), List.of()));
	}

	List<ParentEdge> parentEdgesOf(final FLEFRecord child){
		return parentEdgesByChild.getOrDefault(child.getId(), List.of());
	}

	List<FLEFRecord> relationshipsOfSubject(final FLEFRecord subject){
		return relationshipsBySubject.getOrDefault(subject.getId(), List.of());
	}


	/* ======================================================================
	 *                          Events and attributes
	 * ====================================================================== */

	List<FLEFRecord> eventsOf(final FLEFRecord ind){
		final List<FLEFRecord> evts = new ArrayList<>(
			eventsByIndividual.getOrDefault(ind.getId(), List.of()));
		evts.sort(Comparator.comparingInt(GenealogicalDateHelper::yearOrMax));
		return evts;
	}

	List<FLEFRecord> attributesOf(final FLEFRecord ind){
		return attributesByIndividual.getOrDefault(ind.getId(), List.of());
	}


	/* ======================================================================
	 *                          Event participations
	 * ====================================================================== */

	List<FLEFRecord> participationsOfParticipant(final FLEFRecord participant){
		return participationsByParticipant.getOrDefault(participant.getId(), List.of());
	}

	List<FLEFRecord> participationsOfEvent(final FLEFRecord event){
		return participationsByEvent.getOrDefault(event.getId(), List.of());
	}

	/**
	 * Resolves every participant of the given event into a
	 * {@link Participant}, carrying the participation record alongside.
	 */
	List<Participant> participantsOf(final FLEFRecord event){
		final List<Participant> out = new ArrayList<>();
		for(final FLEFRecord ep : participationsOfEvent(event)){
			final Ref ref = extractOneOf(ep, TAG_PARTICIPANT);
			if(ref == null) continue;
			final FLEFRecord rec = model.getRecordById(ref.id());
			if(rec == null) continue;
			final String role = FLEFRecordHelper.getChildValue(ep, TAG_ROLE);
			out.add(new Participant(rec, ref.tag(), role, ep));
		}
		return out;
	}


	/* ======================================================================
	 *                          Context impacts / research questions
	 * ====================================================================== */

	List<FLEFRecord> contextImpactsFor(final Set<String> targetIds){
		final Set<String> seen = new HashSet<>();
		final List<FLEFRecord> out = new ArrayList<>();
		for(final String id : targetIds)
			for(final FLEFRecord ci : contextImpactsByTarget.getOrDefault(id, List.of()))
				if(seen.add(ci.getId()))
					out.add(ci);
		return out;
	}

	List<FLEFRecord> researchQuestionsFor(final Set<String> targetIds){
		final Set<String> seen = new HashSet<>();
		final List<FLEFRecord> out = new ArrayList<>();
		for(final String id : targetIds)
			for(final FLEFRecord q : researchQuestionsByTarget.getOrDefault(id, List.of()))
				if(seen.add(q.getId()))
					out.add(q);
		return out;
	}


	/* ======================================================================
	 *                          Groups
	 * ====================================================================== */

	List<FLEFRecord> groupMembershipsOf(final FLEFRecord individual){
		return groupMembershipsByIndividual.getOrDefault(individual.getId(), List.of());
	}

	List<FLEFRecord> membersOf(final FLEFRecord group){
		return groupMembershipsByGroup.getOrDefault(group.getId(), List.of());
	}

	List<FLEFRecord> parentGroupsOf(final FLEFRecord group){
		return parentGroupsByChildGroup.getOrDefault(group.getId(), List.of());
	}

	List<FLEFRecord> childGroupsOf(final FLEFRecord group){
		return childGroupsByParentGroup.getOrDefault(group.getId(), List.of());
	}

	List<FLEFRecord> attributesOfGroup(final FLEFRecord group){
		return groupAttributesByGroup.getOrDefault(group.getId(), List.of());
	}

	List<FLEFRecord> eventsOfGroup(final FLEFRecord group){
		final List<FLEFRecord> evts = new ArrayList<>(
			eventsByGroup.getOrDefault(group.getId(), List.of()));
		evts.sort(Comparator.comparingInt(GenealogicalDateHelper::yearOrMax));
		return evts;
	}


	/* ======================================================================
	 *                          Place-centric queries
	 * ====================================================================== */

	List<FLEFRecord> placeRelationshipsAsSubject(final FLEFRecord place){
		return placeRelationshipsBySubject.getOrDefault(place.getId(), List.of());
	}

	List<FLEFRecord> placeRelationshipsAsTarget(final FLEFRecord place){
		return placeRelationshipsByTarget.getOrDefault(place.getId(), List.of());
	}

	/** Events that happened at the given place (via {@code place.place}). */
	List<FLEFRecord> eventsAtPlace(final FLEFRecord place){
		final List<FLEFRecord> evts = new ArrayList<>(
			eventsAtPlace.getOrDefault(place.getId(), List.of()));
		evts.sort(Comparator.comparingInt(GenealogicalDateHelper::yearOrMax));
		return evts;
	}

	/** Events in which the place participates (via {@code event_participation}). */
	List<FLEFRecord> eventsByPlace(final FLEFRecord place){
		final List<FLEFRecord> evts = new ArrayList<>(
			eventsByPlace.getOrDefault(place.getId(), List.of()));
		evts.sort(Comparator.comparingInt(GenealogicalDateHelper::yearOrMax));
		return evts;
	}

	/** Attributes recorded at the given place (via {@code place.place}). */
	List<FLEFRecord> attributesAtPlace(final FLEFRecord place){
		return attributesAtPlace.getOrDefault(place.getId(), List.of());
	}


	/* ======================================================================
	 *                          Source / repository queries
	 * ====================================================================== */

	List<FLEFRecord> citationsOfSource(final FLEFRecord source){
		return citationsBySource.getOrDefault(source.getId(), List.of());
	}

	List<FLEFRecord> documentsOfSource(final FLEFRecord source){
		return documentsBySource.getOrDefault(source.getId(), List.of());
	}

	List<FLEFRecord> repositoriesOfSource(final FLEFRecord source){
		return repositoriesBySource.getOrDefault(source.getId(), List.of());
	}

	/** Sources stored in the given repository. */
	List<FLEFRecord> sourcesOfRepository(final FLEFRecord repository){
		return sourcesByRepository.getOrDefault(repository.getId(), List.of());
	}

	/** Repository-citation nodes that reference the given repository. */
	List<FLEFRecord> repositoryCitationsOf(final FLEFRecord repository){
		return repositoryCitationsByRepository.getOrDefault(repository.getId(), List.of());
	}


	/* ======================================================================
	 *                          Ancestry / reachability
	 * ====================================================================== */

	List<FLEFRecord> paternalAncestors(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		final List<FLEFRecord> parents = parentsOf(root);
		if(!parents.isEmpty())
			collectAncestors(parents.get(0), out, new HashSet<>());
		return out;
	}

	List<FLEFRecord> maternalAncestors(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		final List<FLEFRecord> parents = parentsOf(root);
		if(parents.size() > 1)
			collectAncestors(parents.get(1), out, new HashSet<>());
		return out;
	}

	private void collectAncestors(final FLEFRecord start, final List<FLEFRecord> out,
		final Set<String> seen){
		if(!seen.add(start.getId())) return;
		out.add(start);
		for(final FLEFRecord p : parentsOf(start))
			collectAncestors(p, out, seen);
	}

	List<FLEFRecord> reachable(final FLEFRecord start){
		final List<FLEFRecord> out = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		final Deque<FLEFRecord> stack = new ArrayDeque<>();
		stack.push(start);
		while(!stack.isEmpty()){
			final FLEFRecord cur = stack.pop();
			if(!seen.add(cur.getId())) continue;
			out.add(cur);
			for(final FLEFRecord c : childrenOf(cur)) stack.push(c);
		}
		return out;
	}

	List<FLEFRecord> indirectRelations(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		for(final IndirectRelation ir : indirectRelationsDetailed(root)) out.add(ir.individual());
		return out;
	}

	List<IndirectRelation> indirectRelationsDetailed(final FLEFRecord root){
		final List<FLEFRecord> direct = reachable(root);
		final Set<String> seen = new HashSet<>();
		for(final FLEFRecord r : direct) seen.add(r.getId());
		final List<IndirectRelation> out = new ArrayList<>();
		for(final FLEFRecord r : direct){
			for(final FLEFRecord s : spousesOf(r))
				if(seen.add(s.getId())) out.add(new IndirectRelation(s, r, IndirectKind.SPOUSE));
			for(final FLEFRecord a : associatesOf(r))
				if(seen.add(a.getId())) out.add(new IndirectRelation(a, r, IndirectKind.ASSOCIATE));
		}
		return out;
	}


	/* ======================================================================
	 *                          Whole-model collections
	 * ====================================================================== */

	Collection<FLEFRecord> allIndividuals(){
		return model.getRecordsByType(IndividualHandler.TYPE);
	}


	/* ======================================================================
	 *                          Internals
	 * ====================================================================== */

	private static String extractOneOfXref(final FLEFRecord rec, final String fieldTag,
		final String expectedType){
		final Ref ref = extractOneOf(rec, fieldTag);
		return (ref != null && expectedType.equalsIgnoreCase(ref.tag())? ref.id(): null);
	}

	private static Ref extractOneOf(final FLEFRecord rec, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(rec, fieldTag);
		if(field == null) return null;
		final FLEFRecord ref = field.getTheOnlyChild();
		if(ref == null) return null;
		final String tag = ref.getTag();
		final String id = ref.getValue();
		if(tag == null || id == null || id.isBlank()) return null;
		return new Ref(tag.toLowerCase(Locale.ROOT), id);
	}

	private List<FLEFRecord> records(final List<String> ids){
		final List<FLEFRecord> out = new ArrayList<>(ids.size());
		for(final String id : ids){
			final FLEFRecord r = model.getRecordById(id);
			if(r != null) out.add(r);
		}
		return out;
	}

}
