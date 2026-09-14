package io.github.mtrevisan.familylegacy.v2.ui.components.projections.places;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Pure service that reads the place hierarchy from a {@link FLEFModel}.
 * <p>
 * The hierarchy is not a tree: a place can be the subject of several
 * {@code place_relationship} records (for example, a town can be
 * administratively part of a province and ecclesiastically part of a
 * diocese), so the data is a directed acyclic graph, not a tree. The
 * service preserves the DAG and leaves to the panel the task of
 * presenting it as a tree, by duplicating a place under each of its
 * parents.
 * <p>
 * <b>Orphan places.</b> Not every place has a declared hierarchy: some
 * places are recorded with no {@code place_relationship} at all, and
 * some are recorded only as subjects of a relationship whose target is
 * missing from the model. Both cases are handled by treating a place as
 * a <b>root</b> whenever it never appears as a subject of a valid
 * relationship. This is a deliberate choice: an orphan place is still a
 * place, not an error, and it belongs at the top level of the
 * presentation, next to the places that are genuinely at the top of a
 * hierarchy.
 * <p>
 * <b>Cycles.</b> The FLEF specification recommends preventing cycles in
 * place relationships, but a malformed file could still contain one.
 * The service detects reachability without recursion and, when a cycle
 * exists, the panel breaks it at the first repeated node and marks the
 * node as a cycle so the renderer can flag it. The service never throws
 * on cycles.
 */
public final class PlaceHierarchyService{

	/** Tag of a {@code PlaceRecord}. */
	private static final String TYPE_PLACE = "place";
	/** Tag of a {@code PlaceRelationshipRecord}. */
	private static final String TYPE_PLACE_RELATIONSHIP = "place_relationship";

	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_PLACE = "place";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_TYPE = "type";
	private static final String TAG_MAP = "map";
	private static final String TAG_COORDINATES = "coordinates";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";

	/**
	 * The relation types declared by the protocol. Used by the panel to
	 * build the filter combo; custom types present in the data are added
	 * to the list dynamically.
	 */
	public static final List<String> DECLARED_RELATION_TYPES = List.of(
		"administrative_part_of",
		"geographic_part_of",
		"ecclesiastical_part_of",
		"judicial_part_of",
		"cadastral_part_of"
	);


	/** A minimal, immutable view of a {@code PlaceRecord}. */
	public record PlaceReference(String id, String name, String type, String coordinates){

		/** Returns {@code true} when the place has a usable display name. */
		public boolean hasName(){
			return name != null && !name.isBlank();
		}
	}

	/** A relation between two places, as declared by a {@code PlaceRelationshipRecord}. */
	public record PlaceRelation(
		String relationshipId,
		String parentId,
		String childId,
		String relationType,
		String validFrom,
		String validTo){}

	/**
	 * The complete hierarchy: places indexed by id, relations, and the
	 * indexes needed to walk the graph in either direction.
	 */
	public record Hierarchy(
		Map<String, PlaceReference> places,
		List<PlaceRelation> relations,
		Map<String, List<PlaceRelation>> outgoingByParent,
		Map<String, List<PlaceRelation>> incomingByChild,
		List<String> rootIds){

		/** Returns the outgoing relations of the given place, in stable order. */
		public List<PlaceRelation> childrenOf(final String parentId){
			return outgoingByParent.getOrDefault(parentId, List.of());
		}

		/** Returns the incoming relations of the given place, in stable order. */
		public List<PlaceRelation> parentsOf(final String childId){
			return incomingByChild.getOrDefault(childId, List.of());
		}

		/** Returns whether the given place is presented as a root. */
		public boolean isRoot(final String placeId){
			return rootIds.contains(placeId);
		}
	}


	private PlaceHierarchyService(){
	}


	/**
	 * Reads the place hierarchy from the given model.
	 *
	 * @param model the model; must not be {@code null}
	 * @return the hierarchy, never {@code null}
	 */
	public static Hierarchy load(final FLEFModel model){
		// --- 1. Index every place ---
		final Map<String, PlaceReference> places = new LinkedHashMap<>();
		for(final FLEFRecord record : model.getRecordsByType(PlaceHandler.TYPE)){
			final PlaceReference ref = toPlaceReference(record);
			if(ref != null)
				places.put(ref.id(), ref);
		}

		// --- 2. Index every relation that resolves to two known places ---
		final List<PlaceRelation> relations = new ArrayList<>();
		for(final FLEFRecord record : model.getRecordsByType(PlaceRelationshipHandler.TYPE)){
			final PlaceRelation rel = toPlaceRelation(record, places);
			if(rel != null)
				relations.add(rel);
		}

		// --- 3. Group relations by parent and by child ---
		final Map<String, List<PlaceRelation>> outgoing = new LinkedHashMap<>();
		final Map<String, List<PlaceRelation>> incoming = new LinkedHashMap<>();
		for(final PlaceRelation rel : relations){
			outgoing.computeIfAbsent(rel.parentId(), k -> new ArrayList<>()).add(rel);
			incoming.computeIfAbsent(rel.childId(), k -> new ArrayList<>()).add(rel);
		}

		// --- 4. Compute the roots ---
		// A place is a root when it never appears as a subject of a valid
		// relation. This includes:
		//   - places that are at the top of a hierarchy;
		//   - places with no relations at all (orphans);
		//   - places that are only children of a relation whose parent
		//     is missing from the model.
		final Set<String> subjects = new LinkedHashSet<>(incoming.keySet());
		final List<String> roots = new ArrayList<>();
		for(final String id : places.keySet())
			if(!subjects.contains(id))
				roots.add(id);

		// --- 5. Handle cycles and unreachable places ---
		// A place that only appears inside a cycle would never be reached
		// from a root. We compute the set of reachable places and promote
		// the unreachable ones to roots, so nothing is silently dropped.
		final Set<String> reachable = computeReachable(roots, outgoing);
		for(final String id : places.keySet())
			if(!reachable.contains(id) && !roots.contains(id))
				roots.add(id);

		// --- 6. Sort the roots by name for a stable, readable order ---
		roots.sort((a, b) -> compareByName(places, a, b));

		return new Hierarchy(places, relations, outgoing, incoming, roots);
	}


	/* ======================================================================
	 *                          Extraction helpers
	 * ====================================================================== */

	private static PlaceReference toPlaceReference(final FLEFRecord record){
		final String id = record.getId();
		if(id == null)
			return null;
		final String name = extractName(record);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String coordinates = extractCoordinates(record);
		return new PlaceReference(id, name, type, coordinates);
	}

	/**
	 * Extracts the primary name of a place. {@code PlaceRecord.name} is
	 * a list of {@code NameStructure}, each with its own {@code value}
	 * child; the first one with a non-blank value is used as the display
	 * name. When no name is available, the place id is returned so the
	 * tree stays readable.
	 */
	private static String extractName(final FLEFRecord place){
		for(final FLEFRecord child : place.getChildren()){
			if(!TAG_NAME.equalsIgnoreCase(child.getTag()))
				continue;
			final String value = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
			if(value != null && !value.isBlank())
				return value;
			final FLEFRecord onlyChild = child.getTheOnlyChild();
			if(onlyChild != null && onlyChild.getValue() != null && !onlyChild.getValue().isBlank())
				return onlyChild.getValue();
		}
		return place.getId();
	}

	/**
	 * Extracts the coordinates of a place from the nested
	 * {@code map.coordinates} structure, or {@code null} when missing.
	 */
	private static String extractCoordinates(final FLEFRecord place){
		final FLEFRecord map = FLEFRecordHelper.findChild(place, TAG_MAP);
		if(map == null)
			return null;
		return FLEFRecordHelper.getChildValue(map, TAG_COORDINATES);
	}

	private static PlaceRelation toPlaceRelation(final FLEFRecord record,
		final Map<String, PlaceReference> places){
		final String parentId = record.extractReferencedId(TAG_SUBJECT, TAG_PLACE);
		final String childId = record.extractReferencedId(TAG_TARGET, TAG_PLACE);
		if(parentId == null || childId == null)
			return null;
		// Both endpoints must resolve to a place that exists in the model.
		// A relation that points to a missing place is ignored: it would
		// produce a dangling edge in the tree.
		if(!places.containsKey(parentId) || !places.containsKey(childId))
			return null;

		final String relationType = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String validFrom = extractDateValue(record, TAG_VALID_FROM);
		final String validTo = extractDateValue(record, TAG_VALID_TO);
		return new PlaceRelation(record.getId(), parentId, childId,
			relationType, validFrom, validTo);
	}

	/**
	 * Extracts the {@code value} of a nested {@code DateStructure}, or
	 * {@code null} when the field is absent. The value is used as-is:
	 * the panel shows it as a plain string, without parsing.
	 */
	private static String extractDateValue(final FLEFRecord record, final String tag){
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(record, tag);
		if(dateStruct == null)
			return null;
		final String value = FLEFRecordHelper.getChildValue(dateStruct, TAG_VALUE);
		if(value != null)
			return value;
		final FLEFRecord onlyChild = dateStruct.getTheOnlyChild();
		return (onlyChild != null? onlyChild.getValue(): null);
	}

	/**
	 * Computes the set of places reachable from the given roots, walking
	 * the outgoing relations. Uses an iterative stack rather than
	 * recursion, so a deep or malformed hierarchy cannot overflow the
	 * call stack.
	 */
	private static Set<String> computeReachable(final List<String> roots,
		final Map<String, List<PlaceRelation>> outgoing){
		final Set<String> reachable = new HashSet<>();
		final Deque<String> stack = new ArrayDeque<>(roots);
		while(!stack.isEmpty()){
			final String id = stack.pop();
			if(!reachable.add(id))
				continue;
			for(final PlaceRelation rel : outgoing.getOrDefault(id, List.of()))
				stack.push(rel.childId());
		}
		return reachable;
	}

	private static int compareByName(final Map<String, PlaceReference> places,
		final String a, final String b){
		final PlaceReference ra = places.get(a);
		final PlaceReference rb = places.get(b);
		final String na = (ra != null? ra.name(): a);
		final String nb = (rb != null? rb.name(): b);
		return String.CASE_INSENSITIVE_ORDER.compare(
			na != null? na: "", nb != null? nb: "");
	}

}
