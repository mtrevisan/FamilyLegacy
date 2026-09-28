package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * High-level facade orchestrating specialized indices for events, attributes,
 * relationships, lineage, place reverse-lookups, groups, documents, sources, and research questions.
 */
public final class RelationIndex {

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

	public static RelationIndex build(final FLEFModel model, final Predicate<FLEFRecord> isVisibleFilter){
		return new RelationIndex(model, isVisibleFilter != null ? isVisibleFilter : r -> true);
	}


	// --- Individual & Kinship ---
	public List<FLEFRecord> eventsOf(final FLEFRecord person){ return eventIndex.eventsOf(person); }
	public List<FLEFRecord> attributesOf(final FLEFRecord person){ return attributeIndex.attributesOf(person); }
	public List<FLEFRecord> relationshipsOfSubject(final FLEFRecord person){ return relationshipIndex.relationshipsOf(person); }
	public List<FLEFRecord> relationshipsOf(final FLEFRecord person){ return relationshipIndex.relationshipsOf(person); }
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

	public List<FLEFRecord> participationsOfParticipant(final FLEFRecord p){ return eventIndex.participationsOfParticipant(p); }
	public List<FLEFRecord> groupMembershipsOf(final FLEFRecord person){ return relationshipIndex.groupMembershipsOf(person); }
	public List<FLEFRecord> childrenOf(final FLEFRecord parent){ return pedigreeIndex.childrenOf(parent); }

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
	public List<FLEFRecord> attributesOfGroup(final FLEFRecord g){ return groupIndex.attributesOfGroup(g); }
	public List<FLEFRecord> eventsOfGroup(final FLEFRecord g){ return eventIndex.eventsOf(g); }
	public List<FLEFRecord> membersOf(final FLEFRecord g){ return groupIndex.membersOf(g); }
	public List<FLEFRecord> parentGroupsOf(final FLEFRecord g){ return groupIndex.parentGroupsOf(g); }
	public List<FLEFRecord> childGroupsOf(final FLEFRecord g){ return groupIndex.childGroupsOf(g); }


	// --- Place ---
	public List<FLEFRecord> eventsAtPlace(final FLEFRecord place){ return placeIndex.eventsAtPlace(place != null ? place.getId() : null); }
	public List<FLEFRecord> eventsByPlace(final FLEFRecord place){ return placeIndex.eventsAtPlace(place != null ? place.getId() : null); }
	public List<FLEFRecord> attributesAtPlace(final FLEFRecord place){ return placeIndex.attributesAtPlace(place != null ? place.getId() : null); }
	public List<FLEFRecord> placeRelationshipsAsSubject(final FLEFRecord place){ return placeIndex.placeRelationshipsAsSubject(place); }
	public List<FLEFRecord> placeRelationshipsAsTarget(final FLEFRecord place){ return placeIndex.placeRelationshipsAsTarget(place); }


	// --- Source / Repository / Document ---
	public List<FLEFRecord> citationsOfSource(final FLEFRecord s){ return sourceAndDocumentIndex.citationsOfSource(s); }
	public List<FLEFRecord> documentsOfSource(final FLEFRecord s){ return sourceAndDocumentIndex.documentsOfSource(s); }
	public List<FLEFRecord> repositoriesOfSource(final FLEFRecord s){ return sourceAndDocumentIndex.repositoriesOfSource(s); }
	public List<FLEFRecord> sourcesOfRepository(final FLEFRecord r){ return sourceAndDocumentIndex.sourcesOfRepository(r); }
	public List<FLEFRecord> repositoryCitationsOf(final FLEFRecord r){ return sourceAndDocumentIndex.repositoryCitationsOf(r); }
	public List<FLEFRecord> sourcesOfDocument(final FLEFRecord d){ return sourceAndDocumentIndex.sourcesOfDocument(d); }
	public List<FLEFRecord> citationsOfDocument(final FLEFRecord d){ return sourceAndDocumentIndex.citationsOfDocument(d); }


	// --- Research ---
	public List<FLEFRecord> activitiesForQuestion(final FLEFRecord q){ return researchIndex.activitiesForQuestion(q); }
	public List<FLEFRecord> tasksForQuestion(final FLEFRecord q){ return researchIndex.tasksForQuestion(q); }
	public List<FLEFRecord> conclusionsForQuestion(final FLEFRecord q){ return researchIndex.conclusionsForQuestion(q); }

	// --- Direct Group & Ancestry ---
	public List<FLEFRecord> parentsOf(final FLEFRecord ind){ return pedigreeIndex.parentsOf(ind); }
	public List<FLEFRecord> spousesOf(final FLEFRecord ind){ return relationshipIndex.spousesOf(ind); }
	public List<FLEFRecord> associatesOf(final FLEFRecord ind){ return relationshipIndex.associatesOf(ind); }
	public List<FLEFRecord> paternalAncestors(final FLEFRecord root){ return kinshipResolver.paternalAncestors(root); }
	public List<FLEFRecord> maternalAncestors(final FLEFRecord root){ return kinshipResolver.maternalAncestors(root); }
	public List<FLEFRecord> reachable(final FLEFRecord start){ return kinshipResolver.reachable(start); }
	public List<FLEFRecord> indirectRelations(final FLEFRecord root){
		return kinshipResolver.indirectRelationsDetailed(root).stream().map(KinshipResolver.IndirectRelation::individual).toList();
	}

	// --- Research Tasks & Context ---
	public List<FLEFRecord> tasksForActivity(final FLEFRecord activity){ return researchIndex.tasksForActivity(activity); }
	public List<FLEFRecord> contextImpactsFor(final Set<String> targetIds){ return researchIndex.contextImpactsFor(targetIds); }
	public List<FLEFRecord> researchQuestionsFor(final Set<String> targetIds){ return researchIndex.researchQuestionsFor(targetIds); }

}
