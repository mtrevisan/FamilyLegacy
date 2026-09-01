package io.github.mtrevisan.familylegacy.v2.ui.components.genealogicaltree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents.BiologicalParentsData;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;


/**
 * Service for building and navigating genealogical ancestor trees.
 */
public class GenealogicalTreeService{

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

	private static final String EVENT_TYPE_MARRIAGE = "marriage";


	private final FLEFModel model;

	// Inverted indices for O(1) direct lookup
	private Map<String, List<FLEFRecord>> individualToParentsMap;
	private Map<String, List<FLEFRecord>> individualToMarriageEventMap;
	private Map<String, FLEFRecord> individualToFamilyMap;
	private Map<String, List<FLEFRecord>> individualToEventMap;


	public GenealogicalTreeService(final FLEFModel model){
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

		// Pre-index relationships, groups, and marriage events in single-pass O(N) lookup tables
		ensureIndices();

		final IndividualData rootData = IndividualData.create(rootIndividual, individualToEventMap, model);
		final AncestorNode rootNode = new AncestorNode(rootIndividual, rootData, 0);

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

			// Process Father
			final FLEFRecord father = findParent(currentIndividualId, ENUM_SEX_MALE);
			if(father != null){
				final IndividualData fatherData = IndividualData.create(father, individualToEventMap, model);
				final AncestorNode fatherNode = new AncestorNode(father, fatherData, nextGeneration);
				currentNode.setFather(fatherNode);
				queue.add(fatherNode);
			}

			// Process Mother
			final FLEFRecord mother = findParent(currentIndividualId, ENUM_SEX_FEMALE);
			if(mother != null){
				final IndividualData motherData = IndividualData.create(mother, individualToEventMap, model);
				final AncestorNode motherNode = new AncestorNode(mother, motherData, nextGeneration);
				currentNode.setMother(motherNode);
				queue.add(motherNode);
			}

			// Process Biological Parents
			final List<FLEFRecord> fatherMarriageEvents = (father != null
				? individualToMarriageEventMap.get(father.getId())
				: Collections.emptyList());
			final List<FLEFRecord> motherMarriageEvents = (mother != null
				? individualToMarriageEventMap.get(mother.getId())
				: Collections.emptyList());
			final FLEFRecord parentsFamily = individualToFamilyMap.get(currentIndividualId);
			if(parentsFamily != null){
				final BiologicalParentsData biologicalParentsData = BiologicalParentsData.create(parentsFamily,
					father, fatherMarriageEvents,
					mother, motherMarriageEvents,
					model);
				currentNode.setBiologicalParentsData(biologicalParentsData);
			}
		}

		return rootNode;
	}

	/**
	 * Finds a biological parent of the specified sex for a given child ID.
	 *
	 * @param childId the ID of the child
	 * @param sex     desired sex ("male" or "female")
	 * @return the matching parent record, or {@code null} if not found
	 */
	private FLEFRecord findParent(final String childId, final String sex){
		if(childId == null || individualToParentsMap == null)
			return null;

		final List<FLEFRecord> parents = individualToParentsMap.get(childId);
		if(parents == null || parents.isEmpty())
			return null;

		FLEFRecord fallbackParent = null;
		for(final FLEFRecord parent : parents){
			final String parentSex = FLEFRecordHelper.getChildValue(parent, TAG_SEX);

			if(sex.equals(parentSex))
				return parent;

			// If sex is missing or unspecified, keep as fallback
			if(parentSex == null && fallbackParent == null)
				fallbackParent = parent;
		}

		return fallbackParent;
	}

	/**
	 * Pre-indexes all relationships, group memberships, and marriage events in single passes.
	 */
	private void ensureIndices(){
		if(individualToParentsMap != null)
			return;

		individualToParentsMap = new HashMap<>();
		individualToMarriageEventMap = new HashMap<>();
		individualToFamilyMap = new HashMap<>();
		individualToEventMap = new HashMap<>();

		// 1. Index parents and child-group relationships
		final Map<String, Set<String>> childToGroupIdsMap = new HashMap<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null)
				continue;

			final String subjectId = extractReferencedId(relationship, TAG_SUBJECT);
			final String targetId = extractReferencedId(relationship, TAG_TARGET);
			if(subjectId == null || targetId == null)
				continue;

			if(type.endsWith(ENUM_TYPE_ENDS_WITH_CHILD)){
				final FLEFRecord parent = model.getRecordById(targetId);
				if(parent != null)
					individualToParentsMap.computeIfAbsent(subjectId, k -> new ArrayList<>())
						.add(parent);

				// Collect group candidate references for child
				childToGroupIdsMap.computeIfAbsent(subjectId, k -> new HashSet<>())
					.add(targetId);
			}
		}

		// 2. Index family group records O(G)
		final List<FLEFRecord> groups = model.getRecordsByType(GroupHandler.TYPE);
		for(final FLEFRecord group : groups){
			final String type = FLEFRecordHelper.getChildValue(group, TAG_TYPE);
			if(!ENUM_TYPE_FAMILY.equals(type))
				continue;

			final String groupId = group.getId();
			for(final Map.Entry<String, Set<String>> entry : childToGroupIdsMap.entrySet())
				if(entry.getValue().contains(groupId))
					individualToFamilyMap.putIfAbsent(entry.getKey(), group);
		}

		// 3. Index marriage events O(E)
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final FLEFRecord participant = FLEFRecordHelper.findChild(eventParticipation, TAG_PARTICIPANT);
			if(participant == null)
				continue;
			final FLEFRecord individualRef = participant.getTheOnlyChild();
			if(individualRef == null)
				continue;

			final String childId = individualRef.getValue();
			if(childId == null)
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(eventParticipation, TAG_EVENT);
			if(eventId == null)
				continue;
			final FLEFRecord event = model.getRecordById(eventId);
			if(event != null && EventHandler.TYPE.equalsIgnoreCase(event.getTag())){
				final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
				if(type != null && type.endsWith(EVENT_TYPE_MARRIAGE))
					individualToMarriageEventMap.computeIfAbsent(childId, k -> new ArrayList<>())
						.add(event);

				individualToEventMap.computeIfAbsent(individualRef.getValue(), k -> new ArrayList<>())
					.add(event);
			}
		}
	}

	/**
	 * Helper function to extract a referenced ID from a structure's xref tag.
	 *
	 * @param record   the parent record
	 * @param fieldTag the tag to search for
	 * @return referenced ID or {@code null}
	 */
	private String extractReferencedId(final FLEFRecord record, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(record, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord ref = field.getTheOnlyChild();
		if(ref == null)
			return null;

		return ref.getValue();
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		String modelUri = "/tests/TGMZ.flef";
		String recordId = "I1";
		int generations = 2;

		final String content;
		try(final InputStream is = GenealogicalTreeService.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);


		final GenealogicalTreeService service = new GenealogicalTreeService(model);
		final AncestorNode root = service.buildAncestorTree(recordId, generations);
		System.out.println(root);
	}

}
