package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.CollateralScope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


/**
 * High-level facade orchestrating specialized indices for events, attributes,
 * relationships, lineage, place reverse-lookups, groups, documents, sources, and research questions.
 */
public final class RelationIndex{

	public record ParentEdge(FLEFRecord parent, String relationshipType){}

	private final EventIndex eventIndex;
	private final AttributeIndex attributeIndex;
	private final RelationshipIndex relationshipIndex;
	private final PedigreeIndex pedigreeIndex;
	private final PlaceIndex placeIndex;
	private final GroupIndex groupIndex;
	private final SourceAndDocumentIndex sourceAndDocumentIndex;
	private final ResearchIndex researchIndex;
	private final KinshipResolver kinshipResolver;


	private RelationIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		this.eventIndex = new EventIndex(model, filter);
		this.attributeIndex = new AttributeIndex(model, filter);
		this.relationshipIndex = new RelationshipIndex(model, filter);
		this.pedigreeIndex = new PedigreeIndex(model, filter);
		this.placeIndex = new PlaceIndex(model, filter);
		this.groupIndex = new GroupIndex(model, filter);
		this.sourceAndDocumentIndex = new SourceAndDocumentIndex(model, filter);
		this.researchIndex = new ResearchIndex(model, filter);
		this.kinshipResolver = new KinshipResolver(this.pedigreeIndex, this.relationshipIndex);
	}


	public static RelationIndex build(final FLEFModel model){
		return build(model, r -> true);
	}

	public static RelationIndex build(final FLEFModel model, final Predicate<FLEFRecord> isVisibleFilter){
		return new RelationIndex(model, isVisibleFilter != null? isVisibleFilter: r -> true);
	}

	/**
	 * Returns the root followed by every ancestor, grouped by generation:
	 * root, parents, grandparents, great-grandparents, and so on. Within a
	 * generation, order follows the model. Each individual appears at most
	 * once.
	 */
	public List<FLEFRecord> ancestorsByGeneration(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		final Deque<FLEFRecord> queue = new ArrayDeque<>();

		out.add(root);
		seen.add(root.getId());
		queue.add(root);

		while(!queue.isEmpty()){
			final FLEFRecord current = queue.poll();
			for(final FLEFRecord parent : parentsOf(current))
				if(seen.add(parent.getId())){
					out.add(parent);
					queue.add(parent);
				}
		}
		return out;
	}

	public List<FLEFRecord> ancestorsWithCollaterals(final FLEFRecord root, final CollateralScope scope){
		final List<FLEFRecord> direct = ancestorsByGeneration(root);
		if(scope == CollateralScope.NONE)
			return direct;

		final List<FLEFRecord> out = new ArrayList<>(direct);
		final Set<String> seen = new HashSet<>();
		for(final FLEFRecord r : direct)
			seen.add(r.getId());

		// Layer 1: siblings of each direct ancestor.
		if(scope.ordinal() >= CollateralScope.SIBLINGS.ordinal()){
			for(final FLEFRecord ancestor : direct){
				final FLEFRecord parent = firstParentOf(ancestor);
				if(parent == null)
					continue;
				for(final FLEFRecord sibling : childrenOf(parent))
					if(seen.add(sibling.getId()))
						out.add(sibling);
			}
		}

		// Layer 2: descendants of those siblings.
		if(scope.ordinal() >= CollateralScope.COUSINS.ordinal()){
			final int snap = out.size();
			for(int i = 0; i < snap; i++){
				final FLEFRecord r = out.get(i);
				if(direct.contains(r))
					continue;
				for(final FLEFRecord d : descendantsByGeneration(r))
					if(seen.add(d.getId()))
						out.add(d);
			}
		}

		// Layer 3 (EXTENDED): associates of everyone already collected.
		if(scope == CollateralScope.EXTENDED){
			final int snap = out.size();
			for(int i = 0; i < snap; i++)
				for(final FLEFRecord a : associatesOf(out.get(i)))
					if(seen.add(a.getId()))
						out.add(a);
		}

		return out;
	}

	/**
	 * Returns the root followed by every descendant, grouped by generation:
	 * root, children, grandchildren, and so on.
	 */
	public List<FLEFRecord> descendantsByGeneration(final FLEFRecord root){
		final List<FLEFRecord> out = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		final Deque<FLEFRecord> queue = new ArrayDeque<>();

		out.add(root);
		seen.add(root.getId());
		queue.add(root);
		while(!queue.isEmpty()){
			final FLEFRecord current = queue.poll();
			for(final FLEFRecord child : childrenOf(current))
				if(seen.add(child.getId())){
					out.add(child);
					queue.add(child);
				}
		}
		return out;
	}

	public List<FLEFRecord> descendantsWithCollaterals(final FLEFRecord root, final CollateralScope scope){
		final List<FLEFRecord> direct = descendantsByGeneration(root);
		if(scope == CollateralScope.NONE)
			return direct;

		final List<FLEFRecord> out = new ArrayList<>(direct);
		final Set<String> seen = new HashSet<>();
		for(final FLEFRecord r : direct)
			seen.add(r.getId());

		// Layer 1: spouses of each descendant.
		if(scope.ordinal() >= CollateralScope.SPOUSES.ordinal()){
			for(final FLEFRecord desc : direct)
				for(final FLEFRecord spouse : spousesOf(desc))
					if(seen.add(spouse.getId()))
						out.add(spouse);
		}

		// Layer 2: step-children (children of a spouse, from a different
		// union, that are not descendants of the root).
		if(scope.ordinal() >= CollateralScope.SIBLINGS.ordinal()){
			for(final FLEFRecord desc : direct){
				for(final FLEFRecord spouse : spousesOf(desc))
					for(final FLEFRecord child : childrenOf(spouse))
						if(!isDescendantOf(child, root) && seen.add(child.getId()))
							out.add(child);
			}
		}

		// Layer 3: descendants of step-children.
		if(scope.ordinal() >= CollateralScope.COUSINS.ordinal()){
			final int snap = out.size();
			for(int i = 0; i < snap; i++){
				final FLEFRecord r = out.get(i);
				if(direct.contains(r))
					continue;
				for(final FLEFRecord d : descendantsByGeneration(r))
					if(seen.add(d.getId()))
						out.add(d);
			}
		}

		// Layer 4 (EXTENDED): associates.
		if(scope == CollateralScope.EXTENDED){
			final int snap = out.size();
			for(int i = 0; i < snap; i++)
				for(final FLEFRecord a : associatesOf(out.get(i)))
					if(seen.add(a.getId()))
						out.add(a);
		}

		return out;
	}

	/* ======================================================================
	 *                          Internal helpers
	 * ====================================================================== */

	/**
	 * Returns the first parent of the given person, or {@code null} when the
	 * person has no parents documented. The choice is deterministic (the
	 * first in model order) but arbitrary: it is used only to climb one step
	 * up the pedigree when generating collateral lines, so which parent is
	 * chosen does not matter as long as the sibling set is explored.
	 */
	private FLEFRecord firstParentOf(final FLEFRecord person){
		final List<FLEFRecord> parents = parentsOf(person);
		return (parents.isEmpty()? null: parents.getFirst());
	}

	/**
	 * Whether {@code person} is a descendant of {@code ancestor}, following
	 * the {@code childrenOf} relation transitively. Returns {@code false}
	 * when either endpoint is {@code null} or when {@code person} equals
	 * {@code ancestor} (a person is not their own descendant).
	 *
	 * <p>The walk is upward from {@code person} and stops as soon as
	 * {@code ancestor} is reached, so the typical cost is proportional to
	 * the depth of the tree rather than to the number of descendants.</p>
	 */
	private boolean isDescendantOf(final FLEFRecord person, final FLEFRecord ancestor){
		if(person == null || ancestor == null)
			return false;
		final String personId = person.getId();
		final String ancestorId = ancestor.getId();
		if(personId == null || ancestorId == null || personId.equals(ancestorId))
			return false;

		final Set<String> seen = new HashSet<>();
		final Deque<FLEFRecord> queue = new ArrayDeque<>();
		queue.add(person);

		while(!queue.isEmpty()){
			final FLEFRecord current = queue.poll();
			if(!seen.add(current.getId()))
				continue;
			for(final FLEFRecord parent : parentsOf(current)){
				if(ancestorId.equals(parent.getId()))
					return true;
				queue.add(parent);
			}
		}
		return false;
	}


	// --- Individual & Kinship ---
	public List<FLEFRecord> eventsOf(final FLEFRecord person){
		return eventIndex.eventsOf(person);
	}

	public List<FLEFRecord> attributesOf(final FLEFRecord person){
		return attributeIndex.attributesOf(person);
	}

	public List<FLEFRecord> relationshipsOfSubject(final FLEFRecord person){
		return relationshipIndex.relationshipsOf(person);
	}

	public List<FLEFRecord> relationshipsOf(final FLEFRecord person){
		return relationshipIndex.relationshipsOf(person);
	}

	public List<KinshipResolver.IndirectRelation> indirectRelationsDetailed(final FLEFRecord root){
		return kinshipResolver.indirectRelationsDetailed(root);
	}

	// --- Events & Participations ---
	public List<FLEFRecord> participationsOfEvent(final FLEFRecord e){
		return eventIndex.participationsOfEvent(e);
	}

	public List<EventIndex.Participant> participantsOf(final FLEFRecord event){
		final List<EventIndex.Participant> list = eventIndex.participantsOf(event);
		if(list.isEmpty())
			return List.of();

		final List<EventIndex.Participant> out = new java.util.ArrayList<>(list.size());
		for(int i = 0, size = list.size(); i < size; i++){
			final EventIndex.Participant p = list.get(i);
			out.add(new EventIndex.Participant(p.record(), p.kind(), p.role(), p.participation()));
		}
		return out;
	}

	public List<FLEFRecord> participationsOfParticipant(final FLEFRecord p){
		return eventIndex.participationsOfParticipant(p);
	}

	public List<FLEFRecord> groupMembershipsOf(final FLEFRecord person){
		return relationshipIndex.groupMembershipsOf(person);
	}

	public List<FLEFRecord> childrenOf(final FLEFRecord parent){
		return pedigreeIndex.childrenOf(parent);
	}

	public List<ParentEdge> parentEdgesOf(final FLEFRecord child){
		final List<PedigreeIndex.ParentEdge> edges = pedigreeIndex.parentEdgesOf(child);
		if(edges.isEmpty())
			return List.of();

		final ParentEdge[] result = new ParentEdge[edges.size()];
		for(int i = 0; i < edges.size(); i++){
			final PedigreeIndex.ParentEdge e = edges.get(i);
			result[i] = new ParentEdge(e.parent(), e.relationshipType());
		}
		return List.of(result);
	}

	public String relationshipTypeFrom(final FLEFRecord parent, final FLEFRecord child){
		return kinshipResolver.relationshipTypeFrom(parent, child);
	}

	public FLEFRecord otherParentOf(final FLEFRecord child, final FLEFRecord person){
		return kinshipResolver.otherParentOf(child, person);
	}


	// --- Group ---
	public List<FLEFRecord> attributesOfGroup(final FLEFRecord g){
		return groupIndex.attributesOfGroup(g);
	}

	public List<FLEFRecord> eventsOfGroup(final FLEFRecord g){
		return eventIndex.eventsOf(g);
	}

	public List<FLEFRecord> membersOf(final FLEFRecord g){
		return groupIndex.membersOf(g);
	}

	public List<FLEFRecord> parentGroupsOf(final FLEFRecord g){
		return groupIndex.parentGroupsOf(g);
	}

	public List<FLEFRecord> childGroupsOf(final FLEFRecord g){
		return groupIndex.childGroupsOf(g);
	}


	// --- Place ---
	public List<FLEFRecord> eventsAtPlace(final FLEFRecord place){
		return placeIndex.eventsAtPlace(place != null? place.getId(): null);
	}

	public List<FLEFRecord> eventsByPlace(final FLEFRecord place){
		return placeIndex.eventsByPlace(place != null? place.getId(): null);
	}

	public List<FLEFRecord> attributesAtPlace(final FLEFRecord place){
		return placeIndex.attributesAtPlace(place != null? place.getId(): null);
	}

	public List<FLEFRecord> placeRelationshipsAsSubject(final FLEFRecord place){
		return placeIndex.placeRelationshipsAsSubject(place);
	}

	public List<FLEFRecord> placeRelationshipsAsTarget(final FLEFRecord place){
		return placeIndex.placeRelationshipsAsTarget(place);
	}


	// --- Source / Repository / Document ---
	public List<FLEFRecord> citationsOfSource(final FLEFRecord s){
		return sourceAndDocumentIndex.citationsOfSource(s);
	}

	public List<FLEFRecord> documentsOfSource(final FLEFRecord s){
		return sourceAndDocumentIndex.documentsOfSource(s);
	}

	public List<FLEFRecord> repositoriesOfSource(final FLEFRecord s){
		return sourceAndDocumentIndex.repositoriesOfSource(s);
	}

	public List<FLEFRecord> sourcesOfRepository(final FLEFRecord r){
		return sourceAndDocumentIndex.sourcesOfRepository(r);
	}

	public List<FLEFRecord> repositoryCitationsOf(final FLEFRecord r){
		return sourceAndDocumentIndex.repositoryCitationsOf(r);
	}

	public List<FLEFRecord> sourcesOfDocument(final FLEFRecord d){
		return sourceAndDocumentIndex.sourcesOfDocument(d);
	}

	public List<FLEFRecord> citationsOfDocument(final FLEFRecord d){
		return sourceAndDocumentIndex.citationsOfDocument(d);
	}


	// --- Research ---
	public List<FLEFRecord> activitiesForQuestion(final FLEFRecord q){
		return researchIndex.activitiesForQuestion(q);
	}

	public List<FLEFRecord> tasksForQuestion(final FLEFRecord q){
		return researchIndex.tasksForQuestion(q);
	}

	public List<FLEFRecord> conclusionsForQuestion(final FLEFRecord q){
		return researchIndex.conclusionsForQuestion(q);
	}

	// --- Direct Group & Ancestry ---
	public List<FLEFRecord> parentsOf(final FLEFRecord ind){
		return pedigreeIndex.parentsOf(ind);
	}

	public List<FLEFRecord> spousesOf(final FLEFRecord ind){
		return relationshipIndex.spousesOf(ind);
	}

	public List<FLEFRecord> associatesOf(final FLEFRecord ind){
		return relationshipIndex.associatesOf(ind);
	}

	public List<FLEFRecord> paternalAncestors(final FLEFRecord root){
		return kinshipResolver.paternalAncestors(root);
	}

	public List<FLEFRecord> maternalAncestors(final FLEFRecord root){
		return kinshipResolver.maternalAncestors(root);
	}

	public List<FLEFRecord> reachable(final FLEFRecord start){
		return kinshipResolver.reachable(start);
	}

	public List<FLEFRecord> indirectRelations(final FLEFRecord root){
		return kinshipResolver.indirectRelationsDetailed(root).stream().map(KinshipResolver.IndirectRelation::individual).toList();
	}

	// --- Research Tasks & Context ---
	public List<FLEFRecord> tasksForActivity(final FLEFRecord activity){
		return researchIndex.tasksForActivity(activity);
	}

	public List<FLEFRecord> contextImpactsFor(final Set<String> targetIds){
		return researchIndex.contextImpactsFor(targetIds);
	}

	public List<FLEFRecord> researchQuestionsFor(final Set<String> targetIds){
		return researchIndex.researchQuestionsFor(targetIds);
	}

}
