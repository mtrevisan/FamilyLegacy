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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.UIManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Service for building and navigating genealogical ancestor trees.
 */
class TreeService{

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_SEX = "sex";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";

	private static final String ENUM_TYPE_FAMILY = "family";
	private static final String ENUM_TYPE_PARTNER = "partner";


	private final Predicate<String> relationshipTypeFilter;

	private final FLEFModel model;

	// Inverted indices for direct lookup
	private final Map<String, List<FLEFRecord>> individualToParentsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> parentToChildrenMap = new HashMap<>();
	//	private final Map<String, List<FLEFRecord>> individualToMarriageEventMap = new HashMap<>();
//	private final Map<String, FLEFRecord> individualToFamilyMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> individualToEventMap = new HashMap<>();
	private final Set<String> individualsWithDescendantsSet = new HashSet<>();


	public TreeService(final Predicate<String> relationshipTypeFilter, final FLEFModel model){
		this.relationshipTypeFilter = relationshipTypeFilter;

		this.model = model;
	}


	/**
	 * Builds an ancestor tree up to the specified maxGenerations using BFS traversal.
	 * <p>
	 * Pruning rule: does not expand branches for non-existent individuals.
	 *
	 * @param rootIndividualId the root individual record ID
	 * @param maxGenerations depth limit (0-based: 0 = target only, 1 = target + parents, etc.)
	 * @return the root {@link TreeNode} of the constructed tree, or {@code null} if root is {@code null}
	 */
	public TreeNode buildTree(final String rootIndividualId, final boolean showPartner, final int maxGenerations){
		if(StringUtils.isEmpty(rootIndividualId))
			return null;
		final FLEFRecord rootIndividual = model.getRecordById(rootIndividualId);
		if(rootIndividual == null || maxGenerations < 0)
			return null;

		// Pre-index relationships, groups, and events in single-pass lookup tables
		ensureIndices();

		final IndividualData rootData = IndividualData.create(rootIndividual, relationshipTypeFilter, individualToEventMap,
			model);
		final TreeNode rootNode = new TreeNode(rootIndividual, rootData, 0);
		if(showPartner){
			final Map<IndividualData, SiblingsData> partnerChildrenDataMap = buildChildrenData(rootIndividualId);
			if(!partnerChildrenDataMap.isEmpty()){
				// choose partner and children at random
				final Map.Entry<IndividualData, SiblingsData> partnerChildrenData = partnerChildrenDataMap.entrySet().stream()
					.findFirst()
					.get();
				final IndividualData partnerData = partnerChildrenData.getKey();
				final FLEFRecord partner = (partnerData != null? partnerData.getIndividual(): null);
				final SiblingsData childrenData = partnerChildrenData.getValue();
				rootNode.setPartnerAndBiologicalChildren(partner, partnerData, childrenData);
			}
		}

		final Queue<TreeNode> queue = new ArrayDeque<>();
		queue.add(rootNode);
		while(!queue.isEmpty()){
			final TreeNode currentNode = queue.poll();

			final int currentGeneration = currentNode.getGeneration();
			if(currentGeneration >= maxGenerations)
				continue;


			final String currentIndividualId = currentNode.getIndividualId();
			if(currentIndividualId == null)
				continue;
			final int nextGeneration = currentGeneration + 1;

			final List<FLEFRecord> parents = getParents(currentIndividualId);
			FLEFRecord father = extractParent(parents, SexType.MALE);
			FLEFRecord mother = extractParent(parents, SexType.FEMALE);
			// fallback to random if sex is unknown
			if(!parents.isEmpty() && father == null)
				father = parents.removeFirst();
			if(!parents.isEmpty() && mother == null)
				mother = parents.removeFirst();

			final IndividualData fatherData = IndividualData.create(father, relationshipTypeFilter, individualToEventMap,
				model);
			final IndividualData motherData = IndividualData.create(mother, relationshipTypeFilter, individualToEventMap,
				model);

			// PRUNING: Only instantiate father/mother nodes if the underlying record actually exists:
			// Process Father
			if(father != null){
				final TreeNode fatherNode = new TreeNode(father, fatherData, nextGeneration);
				setPartnerData(fatherNode, mother, motherData);
				currentNode.setFather(fatherNode);

				queue.add(fatherNode);
			}

			// Process Mother
			if(mother != null){
				final TreeNode motherNode = new TreeNode(mother, motherData, nextGeneration);
				setPartnerData(motherNode, father, fatherData);
				currentNode.setMother(motherNode);

				queue.add(motherNode);
			}
		}

		return rootNode;
	}

	private void setPartnerData(final TreeNode node, final FLEFRecord partner, final IndividualData partnerData){
		final Map<IndividualData, SiblingsData> childrenDataMap = buildChildrenData(node.getIndividualId());
		final String partnerId = (partner != null? partner.getId(): null);
		final SiblingsData childrenData = childrenDataMap.entrySet().stream()
			.filter(entry -> (partnerId == null || partnerId.equals(entry.getKey().getId())))
			.map(Map.Entry::getValue)
			.findFirst()
			.orElse(null);

		node.setPartnerAndBiologicalChildren(partner, partnerData, childrenData);
	}

	private boolean hasPartnerRelationships(final String partnerId){
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final List<FLEFRecord> partnerRelationships = new ArrayList<>();
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null || !type.endsWith(ENUM_TYPE_PARTNER))
				continue;

			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String targetRefId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(partnerId.equals(subjectId) || partnerId.equals(targetRefId))
				partnerRelationships.add(relationship);
		}
		return !partnerRelationships.isEmpty();
	}

	/**
	 * Extracts direct biological children for a given parent ID, grouped by the other parent.
	 *
	 * @param parentId target parent individual ID
	 * @return map where the key is the IndividualData of the other parent (or {@code null} if unknown),
	 *         and the value is the SiblingsData containing the children shared with that parent
	 */
	public Map<IndividualData, SiblingsData> buildChildrenData(final String parentId){
		final List<FLEFRecord> children = parentToChildrenMap.get(parentId);
		if(children == null || children.isEmpty())
			return Collections.emptyMap();

		// Group children by the second parent (otherParent)
		final Map<FLEFRecord, List<FLEFRecord>> childrenByOtherParentMap = new LinkedHashMap<>();
		for(final FLEFRecord child : children){
			final List<FLEFRecord> parents = individualToParentsMap.get(child.getId());

			FLEFRecord otherParent = null;
			if(parents != null)
				for(final FLEFRecord parent : parents)
					if(!parent.getId().equals(parentId)){
						otherParent = parent;

						break;
					}

			// Group child under their respective second parent (or null if single parent/unknown)
			childrenByOtherParentMap
				.computeIfAbsent(otherParent, k -> new ArrayList<>())
				.add(child);
		}

		// Build the resulting Map<IndividualData, SiblingsData>
		final Map<IndividualData, SiblingsData> resultMap = new LinkedHashMap<>();
		for(final Map.Entry<FLEFRecord, List<FLEFRecord>> entry : childrenByOtherParentMap.entrySet()){
			final FLEFRecord otherParentRecord = entry.getKey();
			final List<FLEFRecord> sharedChildren = entry.getValue();

			// Create IndividualData for the second parent (will be null if otherParentRecord == null)
			final IndividualData otherParentData = (otherParentRecord != null
				? IndividualData.create(otherParentRecord, relationshipTypeFilter, individualToEventMap, model)
				: null);

			final List<IndividualData> childrenDataList = new ArrayList<>();
			for(final FLEFRecord childRecord : sharedChildren)
				childrenDataList.add(IndividualData.create(childRecord, relationshipTypeFilter, individualToEventMap,
					model));

			final Set<String> childrenIdsWithDescendants = childrenDataList.stream()
				.map(IndividualData::getId)
				.filter(individualsWithDescendantsSet::contains)
				.collect(Collectors.toSet());

			final SiblingsData siblingsData = SiblingsData.create(childrenDataList, childrenIdsWithDescendants);
			resultMap.put(otherParentData, siblingsData);
		}

		return resultMap;
	}

	/**
	 * Extracts and builds SiblingsData for an individual by finding all children sharing their parents.
	 *
	 * @param individualId target individual ID
	 * @return SiblingsData containing sibling IndividualData instances
	 */
	public SiblingsData buildSiblingsData(final String individualId){
		final List<FLEFRecord> parents = individualToParentsMap.get(individualId);
		if(parents == null || parents.isEmpty())
			return SiblingsData.create(null, null);

		// Find all siblings sharing at least one parent
		final Set<FLEFRecord> siblingRecords = new LinkedHashSet<>();
		for(final FLEFRecord parent : parents){
			final List<FLEFRecord> children = parentToChildrenMap.get(parent.getId());
			if(children != null)
				siblingRecords.addAll(children);
		}

		final List<IndividualData> siblingDataList = new ArrayList<>();
		for(final FLEFRecord siblingRecord : siblingRecords)
			siblingDataList.add(IndividualData.create(siblingRecord, relationshipTypeFilter, individualToEventMap, model));

		final Set<String> siblingIdsWithDescendants = siblingDataList.stream()
			.map(IndividualData::getId)
			.filter(individualsWithDescendantsSet::contains)
			.collect(Collectors.toSet());

		return SiblingsData.create(siblingDataList, siblingIdsWithDescendants);
	}

	List<FLEFRecord> getParents(final String currentIndividualId){
		List<FLEFRecord> parents = individualToParentsMap.get(currentIndividualId);
		if(parents != null && !parents.isEmpty())
			parents = new ArrayList<>(parents);
		else
			parents = new ArrayList<>();

		assert parents.size() <= 2 : "There must be at most two parents";

		return parents;
	}

	FLEFRecord extractParent(final List<FLEFRecord> parents, final SexType sex){
		FLEFRecord father = null;
		final Iterator<FLEFRecord> itr = parents.iterator();
		while(itr.hasNext()){
			final FLEFRecord parent = itr.next();
			final String rawSex = FLEFRecordHelper.getChildValue(parent, TAG_SEX);
			final SexType parentSex = (rawSex != null? Enum.valueOf(SexType.class, rawSex.toUpperCase(Locale.ROOT)): null);

			if(sex == parentSex){
				itr.remove();
				father = parent;

				break;
			}
		}
		return father;
	}

	/**
	 * Pre-indexes all relationships, group memberships, and marriage events in single passes.
	 */
	private void ensureIndices(){
		if(!individualToParentsMap.isEmpty())
			return;

		// Index parents and parent-child relationships
		final Map<String, Set<String>> childToGroupIdsMap = new HashMap<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null)
				continue;

			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(subjectId == null || targetId == null)
				continue;

			if(relationshipTypeFilter.test(type)){
				final FLEFRecord child = model.getRecordById(subjectId);
				final FLEFRecord parent = model.getRecordById(targetId);
				if(parent != null)
					individualToParentsMap.computeIfAbsent(subjectId, k -> new ArrayList<>())
						.add(parent);
				if(child != null){
					parentToChildrenMap.computeIfAbsent(targetId, k -> new ArrayList<>())
						.add(child);

					// Mark parent as having descendants
					individualsWithDescendantsSet.add(targetId);
				}

				// Collect group candidate references for child
				childToGroupIdsMap.computeIfAbsent(subjectId, k -> new HashSet<>())
					.add(targetId);
			}
		}

//		// Index family group records O(G)
//		final List<FLEFRecord> groups = model.getRecordsByType(GroupHandler.TYPE);
//		for(final FLEFRecord group : groups){
//			final String type = FLEFRecordHelper.getChildValue(group, TAG_TYPE);
//			if(!ENUM_TYPE_FAMILY.equals(type))
//				continue;
//
//			for(final Map.Entry<String, Set<String>> entry : childToGroupIdsMap.entrySet())
//				if(entry.getValue().contains(group.getId()))
//					individualToFamilyMap.putIfAbsent(entry.getKey(), group);
//		}

		// Index events O(E)
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final FLEFRecord participant = FLEFRecordHelper.findChild(eventParticipation, TAG_PARTICIPANT);
			if(participant == null)
				continue;
			final FLEFRecord individualRef = participant.getTheOnlyChild();
			if(individualRef == null)
				continue;

			final String individualId = individualRef.getValue();
			if(individualId == null)
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(eventParticipation, TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event != null && EventHandler.TYPE.equalsIgnoreCase(event.getTag()))
				individualToEventMap.computeIfAbsent(individualId, k -> new ArrayList<>())
					.add(event);
		}
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";
		final String recordId = "I1";
		final int generations = 3;

		final String content;
		try(final InputStream is = TreeService.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);


		final Predicate<String> relationshipTypeFilter = type -> type.equalsIgnoreCase("biological_child");
		final TreeService service = new TreeService(relationshipTypeFilter, model);
		service.buildTree(recordId, true, generations);
	}


	/**
	 * Invalidates all internal lookup indices and caches.
	 * <p>
	 * Forces a recalculation of relationships and events on the next tree build.
	 */
	public void invalidateIndices(){
		individualToParentsMap.clear();
		parentToChildrenMap.clear();
		individualToEventMap.clear();
		individualsWithDescendantsSet.clear();
	}

}
