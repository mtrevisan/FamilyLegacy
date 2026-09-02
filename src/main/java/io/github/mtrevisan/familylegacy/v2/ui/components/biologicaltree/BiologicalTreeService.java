package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

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
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Service for building and navigating genealogical ancestor trees.
 */
public class BiologicalTreeService{

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_SEX = "sex";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";

	private static final String ENUM_TYPE_ENDS_WITH_CHILD = "child";
	private static final String ENUM_TYPE_FAMILY = "family";
	private static final String ENUM_SEX_MALE = "male";
	private static final String ENUM_SEX_FEMALE = "female";


	private final FLEFModel model;

	// Inverted indices for direct lookup
	private Map<String, List<FLEFRecord>> individualToParentsMap;
	private Map<String, List<FLEFRecord>> parentToChildrenMap;
	//	private Map<String, List<FLEFRecord>> individualToMarriageEventMap;
//	private Map<String, FLEFRecord> individualToFamilyMap;
	private Map<String, List<FLEFRecord>> individualToEventMap;
	private Set<String> individualsWithDescendantsSet;


	public BiologicalTreeService(final FLEFModel model){
		this.model = model;
	}


	/**
	 * Builds an ancestor tree up to the specified maxGenerations using BFS traversal.
	 *
	 * @param rootIndividualId the root individual record ID
	 * @param maxGenerations depth limit (0-based: 0 = target only, 1 = target + parents, etc.)
	 * @return the root {@link AncestorNode} of the constructed tree, or {@code null} if root is {@code null}
	 */
	public AncestorNode buildAncestorTree(final String rootIndividualId, final int maxGenerations){
		final FLEFRecord rootIndividual = model.getRecordById(rootIndividualId);
		if(rootIndividual == null || maxGenerations < 0)
			return null;

		// Pre-index relationships, groups, and events in single-pass lookup tables
		ensureIndices();

		final IndividualData rootData = IndividualData.create(rootIndividual, individualToEventMap, model);
		final AncestorNode rootNode = new AncestorNode(rootIndividual, rootData, 0);
//		rootNode.setBiologicalChildrenData(buildSiblingsData(rootIndividualId));
		final Map<IndividualData, SiblingsData> partnerChildrenDataMap = buildChildrenData(rootIndividualId);
		//TODO choose partner and children
		if(!partnerChildrenDataMap.isEmpty()){
			final Map.Entry<IndividualData, SiblingsData> partnerChildrenData = partnerChildrenDataMap.entrySet().stream()
				.findFirst()
				.orElse(null);
			final IndividualData partnerData = partnerChildrenData.getKey();
			final FLEFRecord partner = model.getRecordById(partnerData.getIndividualId());
			final SiblingsData childrenData = partnerChildrenData.getValue();
			rootNode.setMotherBiologicalChildrenData(partner, partnerData, childrenData);
		}

		final Queue<AncestorNode> queue = new ArrayDeque<>();
		queue.add(rootNode);
		while(!queue.isEmpty()){
			final AncestorNode currentNode = queue.poll();

			final int currentGeneration = currentNode.getGeneration();
			if(currentGeneration >= maxGenerations)
				continue;


			final FLEFRecord currentIndividual = currentNode.getIndividual();
			final String currentIndividualId = currentIndividual.getId();
			final int nextGeneration = currentGeneration + 1;

			final List<FLEFRecord> parents = getParents(currentIndividualId);
			FLEFRecord father = extractParent(parents, ENUM_SEX_MALE);
			FLEFRecord mother = extractParent(parents, ENUM_SEX_FEMALE);
			// fallback to random if sex is unknown
			if(!parents.isEmpty() && father == null)
				father = parents.removeFirst();
			if(!parents.isEmpty() && mother == null)
				mother = parents.removeFirst();

			final IndividualData fatherData = (father != null
				? IndividualData.create(father, individualToEventMap, model)
				: null);
			final IndividualData motherData = (mother != null
				? IndividualData.create(mother, individualToEventMap, model)
				: null);

			// Process Father
//			List<FLEFRecord> fatherEvents = Collections.emptyList();
			if(father != null){
//				fatherEvents = individualToEventMap.get(father.getId());

				final AncestorNode fatherNode = new AncestorNode(father, fatherData, nextGeneration);
//				fatherNode.setBiologicalChildrenData(buildSiblingsData(father.getId()));
				if(mother != null){
					final Map<IndividualData, SiblingsData> motherChildrenDataMap = buildChildrenData(father.getId());
					final String motherId = mother.getId();
					final SiblingsData childrenData = motherChildrenDataMap.entrySet().stream()
						.filter(entry -> entry.getKey().getIndividualId().equals(motherId))
						.map(Map.Entry::getValue)
						.findFirst()
						.orElse(null);
					fatherNode.setMotherBiologicalChildrenData(mother, motherData, childrenData);
				}
				currentNode.setFather(fatherNode);

				queue.add(fatherNode);
			}
			// Process Mother
//			List<FLEFRecord> motherEvents = Collections.emptyList();
			if(mother != null){
//				motherEvents = individualToEventMap.get(mother.getId());

				final AncestorNode motherNode = new AncestorNode(mother, motherData, nextGeneration);
//				motherNode.setBiologicalChildrenData(buildSiblingsData(mother.getId()));
				if(father != null){
					final Map<IndividualData, SiblingsData> fatherChildrenDataMap = buildChildrenData(mother.getId());
					final String fatherId = father.getId();
					final SiblingsData childrenData = fatherChildrenDataMap.entrySet().stream()
						.filter(entry -> entry.getKey().getIndividualId().equals(fatherId))
						.map(Map.Entry::getValue)
						.findFirst()
						.orElse(null);
					motherNode.setMotherBiologicalChildrenData(mother, motherData, childrenData);
				}
				currentNode.setMother(motherNode);

				queue.add(motherNode);
			}

			// Process Biological Parents
//			final BiologicalParentsData biologicalParentsData = BiologicalParentsData.create(
//				father, fatherEvents,
//				mother, motherEvents,
//				parentsBiologicalChildrenData, model);
//			currentNode.setBiologicalParentsData(biologicalParentsData);
		}

		return rootNode;
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

			childrenByOtherParentMap
				.computeIfAbsent(otherParent, k -> new ArrayList<>())
				.add(child);
		}

		final Map<IndividualData, SiblingsData> resultMap = new LinkedHashMap<>();
		for(final Map.Entry<FLEFRecord, List<FLEFRecord>> entry : childrenByOtherParentMap.entrySet()){
			final FLEFRecord otherParentRecord = entry.getKey();
			final List<FLEFRecord> sharedChildren = entry.getValue();

			final IndividualData otherParentData = (otherParentRecord != null
				? IndividualData.create(otherParentRecord, individualToEventMap, model)
				: null);

			final List<IndividualData> childrenDataList = new ArrayList<>();
			for(final FLEFRecord childRecord : sharedChildren)
				childrenDataList.add(IndividualData.create(childRecord, individualToEventMap, model));

			final Set<String> childrenIdsWithDescendants = childrenDataList.stream()
				.map(IndividualData::getIndividualId)
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
			siblingDataList.add(IndividualData.create(siblingRecord, individualToEventMap, model));

		final Set<String> siblingIdsWithDescendants = siblingDataList.stream()
			.map(IndividualData::getIndividualId)
			.filter(individualsWithDescendantsSet::contains)
			.collect(Collectors.toSet());

		return SiblingsData.create(siblingDataList, siblingIdsWithDescendants);
	}

	private List<FLEFRecord> getParents(final String currentIndividualId){
		List<FLEFRecord> parents = individualToParentsMap.get(currentIndividualId);
		if(parents != null && !parents.isEmpty())
			parents = new ArrayList<>(parents);
		else
			parents = new ArrayList<>();

		assert parents.size() <= 2 : "There must be at most two parents";

		return parents;
	}

	private static FLEFRecord extractParent(final List<FLEFRecord> parents, final String sex){
		FLEFRecord father = null;
		final Iterator<FLEFRecord> itr = parents.iterator();
		while(itr.hasNext()){
			final FLEFRecord parent = itr.next();
			final String parentSex = FLEFRecordHelper.getChildValue(parent, TAG_SEX);

			if(sex.equals(parentSex)){
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
		if(individualToParentsMap != null)
			return;

		individualToParentsMap = new HashMap<>();
		parentToChildrenMap = new HashMap<>();
//		individualToMarriageEventMap = new HashMap<>();
//		individualToFamilyMap = new HashMap<>();
		individualToEventMap = new HashMap<>();
		individualsWithDescendantsSet = new HashSet<>();

		// 1. Index parents and parent-child relationships
		final Map<String, Set<String>> childToGroupIdsMap = new HashMap<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null)
				continue;

			final String subjectId = extractReferencedId(relationship, TAG_SUBJECT, IndividualHandler.TYPE);
			final String targetId = extractReferencedId(relationship, TAG_TARGET, IndividualHandler.TYPE);
			if(subjectId == null || targetId == null)
				continue;

			if(type.endsWith(ENUM_TYPE_ENDS_WITH_CHILD)){
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

//		// 2. Index family group records O(G)
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

		// 3. Index events O(E)
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

	/**
	 * Helper function to extract a referenced ID from a structure's xref tag.
	 *
	 * @param record   the parent record
	 * @param fieldTag the tag to search for
	 * @param referencedType expected record type
	 * @return referenced ID or {@code null}
	 */
	private String extractReferencedId(final FLEFRecord record, final String fieldTag, final String referencedType){
		final FLEFRecord field = FLEFRecordHelper.findChild(record, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord ref = field.getTheOnlyChild();
		if(ref == null || !referencedType.equalsIgnoreCase(ref.getTag()))
			return null;

		return ref.getValue();
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
		try(final InputStream is = BiologicalTreeService.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);


		final BiologicalTreeService service = new BiologicalTreeService(model);
		final AncestorNode root = service.buildAncestorTree(recordId, generations);
		System.out.println(root);
	}

}
