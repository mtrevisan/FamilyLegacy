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
package io.github.mtrevisan.familylegacy.ui.components.projections.repository;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.ui.components.projections.siblings.SiblingsData;
import org.apache.commons.lang3.StringUtils;

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
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;


/**
 * Service for building and navigating genealogical ancestor trees,
 * leveraging the shared GenealogyRepository.
 */
public class TreeService{

	private final FLEFModel model;
	private final GenealogyRepository repository;

	// Local cache for children data per parent
	private final Map<String, Map<IndividualData, SiblingsData>> childrenDataCache = new HashMap<>();


	public TreeService(final GenealogyRepository repository, final FLEFModel model){
		this.model = model;
		this.repository = repository;
	}


	public GenealogyRepository getRepository(){
		return repository;
	}

	public TreeNode buildTree(final String individualId, final String partnerId, final boolean showPartner,
			final int maxAncestors){
		return buildTree(individualId, partnerId, null, null, showPartner, maxAncestors);
	}

	public TreeNode buildTree(final String individualId, final String partnerId, final String preferredFatherId,
			final String preferredMotherId, final boolean showPartner, final int maxAncestors){
		if(StringUtils.isEmpty(individualId) || maxAncestors < 0)
			return null;

		final FLEFRecord rootIndividual = model.getRecordById(individualId);
		if(rootIndividual == null)
			return null;

		repository.ensureIndices();

		final IndividualData rootData = repository.getIndividualData(rootIndividual);
		final TreeNode rootNode = new TreeNode(rootIndividual, rootData, 0);
		if(showPartner){
			final Map<IndividualData, SiblingsData> partnerChildrenDataMap = buildChildrenData(individualId);

			IndividualData partnerData = null;
			SiblingsData childrenData = null;

			// Preferred partner: match by id in the children map, otherwise
			// fetch the partner record directly (the couple may have no children).
			if(partnerId != null){
				for(final Map.Entry<IndividualData, SiblingsData> entry : partnerChildrenDataMap.entrySet()){
					final IndividualData candidate = entry.getKey();
					if(candidate != null && partnerId.equals(candidate.getId())){
						partnerData = candidate;
						childrenData = entry.getValue();

						break;
					}
				}
				if(partnerData == null){
					final FLEFRecord partnerRecord = model.getRecordById(partnerId);
					if(partnerRecord != null)
						partnerData = repository.getIndividualData(partnerRecord);
				}
			}

			// No preferred partner (or not resolvable): pick the first couple
			// that has recorded children, if any.
			if(partnerData == null && !partnerChildrenDataMap.isEmpty()){
				final Map.Entry<IndividualData, SiblingsData> first = partnerChildrenDataMap.entrySet().iterator().next();
				partnerData = first.getKey();
				childrenData = first.getValue();
			}

			if(partnerData != null){
				final FLEFRecord partner = partnerData.getIndividual();
				rootNode.setPartnerAndBiologicalChildren(partner, partnerData, childrenData);
			}
		}

		// Resolve the preferred couple once, before the BFS.
		final FLEFRecord preferredFatherRecord = (preferredFatherId != null
			? model.getRecordById(preferredFatherId): null);
		final FLEFRecord preferredMotherRecord = (preferredMotherId != null
			? model.getRecordById(preferredMotherId): null);
		final boolean hasPreferredCouple = (preferredFatherRecord != null && preferredMotherRecord != null);

		final Queue<TreeNode> queue = new ArrayDeque<>();
		queue.add(rootNode);
		while(!queue.isEmpty()){
			final TreeNode currentNode = queue.poll();

			final int currentGeneration = currentNode.getGeneration();
			if(currentGeneration >= maxAncestors)
				continue;

			// ---- Root generation: prefer the caller-provided couple ----
			// The check is by generation, not by reference, so it works no
			// matter how the root node was constructed by the caller.
			if(currentGeneration == 0 && hasPreferredCouple && maxAncestors > 0){
				final IndividualData fatherData = repository.getIndividualData(preferredFatherRecord);
				final TreeNode fatherNode = new TreeNode(preferredFatherRecord, fatherData, 1);
				final IndividualData motherData = repository.getIndividualData(preferredMotherRecord);
				final TreeNode motherNode = new TreeNode(preferredMotherRecord, motherData, 1);

				// Wire the couple itself for further expansion.
				setPartnerData(fatherNode, preferredMotherRecord, motherData);
				setPartnerData(motherNode, preferredFatherRecord, fatherData);

				currentNode.setFather(fatherNode);
				currentNode.setMother(motherNode);
				queue.add(fatherNode);
				queue.add(motherNode);
				continue;
			}

			// ---- Normal ancestor expansion ----
			final String currentIndividualId = currentNode.getIndividualId();
			if(currentIndividualId == null)
				continue;

			final List<FLEFRecord> parents = repository.getParents(currentIndividualId);
			if(parents.isEmpty())
				continue;

			final int nextGeneration = currentGeneration + 1;
			FLEFRecord father = extractParent(parents, SexType.MALE);
			FLEFRecord mother = extractParent(parents, SexType.FEMALE);
			if(!parents.isEmpty() && father == null)
				father = parents.removeFirst();
			if(!parents.isEmpty() && mother == null)
				mother = parents.removeFirst();

			if(father != null){
				final IndividualData fatherData = repository.getIndividualData(father);
				final TreeNode fatherNode = new TreeNode(father, fatherData, nextGeneration);
				setPartnerData(fatherNode, mother, (mother != null? repository.getIndividualData(mother): null));
				currentNode.setFather(fatherNode);
				queue.add(fatherNode);
			}

			if(mother != null){
				final IndividualData motherData = repository.getIndividualData(mother);
				final TreeNode motherNode = new TreeNode(mother, motherData, nextGeneration);
				setPartnerData(motherNode, father, (father != null? repository.getIndividualData(father): null));
				currentNode.setMother(motherNode);
				queue.add(motherNode);
			}
		}

		return rootNode;
	}

	private void setPartnerData(final TreeNode node, final FLEFRecord partner, final IndividualData partnerData){
		final Map<IndividualData, SiblingsData> childrenDataMap = buildChildrenData(node.getIndividualId());
		final String partnerId = (partner != null? partner.getId(): null);
		SiblingsData childrenData = null;
		for(final Map.Entry<IndividualData, SiblingsData> entry : childrenDataMap.entrySet()){
			final IndividualData currentPartner = entry.getKey();

			// Match by id when the partner is known, by null key otherwise:
			// the "unknown other parent" entry is the only legitimate match
			// for a null partner.
			final boolean matches;
			if(partnerId != null)
				matches = (currentPartner != null && partnerId.equals(currentPartner.getId()));
			else
				matches = (currentPartner == null);

			if(matches){
				childrenData = entry.getValue();

				break;
			}
		}
		node.setPartnerAndBiologicalChildren(partner, partnerData, childrenData);
	}

	public Map<IndividualData, SiblingsData> buildChildrenData(final String parentId){
		if(parentId == null)
			return Collections.emptyMap();

		repository.ensureIndices();
		return childrenDataCache.computeIfAbsent(parentId, this::computeChildrenData);
	}

	private Map<IndividualData, SiblingsData> computeChildrenData(final String parentId){
		final List<FLEFRecord> children = repository.getChildren(parentId);
		if(children == null || children.isEmpty())
			return Collections.emptyMap();

		// Candidate partners: registered partners of parentId + every other
		// parent of every child of parentId. The union covers both formal
		// unions (M,S) and biological couplings that were never registered
		// as partnerships (M,F).
		final Set<String> candidatePartnerIds = new LinkedHashSet<>(repository.getPartnerIds(parentId));
		for(int i = 0, size = children.size(); i < size; i ++){
			final FLEFRecord child = children.get(i);
			final List<GenealogyRepository.ParentLink> links = repository.getParentLinks(child.getId());
			for(int j = 0, linksSize = links.size(); j < linksSize; j ++){
				final GenealogyRepository.ParentLink link = links.get(j);
				if(!parentId.equals(link.parent().getId()))
					candidatePartnerIds.add(link.parent().getId());
			}
		}

		// For each candidate partner, find the children shared with parentId
		// and compute the couple relationship type for each shared child.
		final Map<FLEFRecord, Set<FLEFRecord>> childrenByCouple = new LinkedHashMap<>();
		final Map<FLEFRecord, Map<String, String>> typesByCouple = new LinkedHashMap<>();
		final Set<String> groupedChildIds = new HashSet<>();

		for(final String partnerId : candidatePartnerIds){
			final List<FLEFRecord> partnerChildren = repository.getChildren(partnerId);
			final Set<String> partnerChildIds = new HashSet<>(partnerChildren.size());
			for(int i = 0, size = partnerChildren.size(); i < size; i ++)
				partnerChildIds.add(partnerChildren.get(i).getId());

			final FLEFRecord partnerRecord = model.getRecordById(partnerId);
			if(partnerRecord == null)
				continue;

			for(int i = 0, size = children.size(); i < size; i ++){
				final FLEFRecord child = children.get(i);
				if(!partnerChildIds.contains(child.getId()))
					continue;

				final String typeToParent = repository.getParentLinkType(child.getId(), parentId);
				final String typeToPartner = repository.getParentLinkType(child.getId(), partnerId);
				final String coupleType = pickCoupleType(typeToParent, typeToPartner);

				childrenByCouple.computeIfAbsent(partnerRecord, k -> new LinkedHashSet<>())
					.add(child);
				typesByCouple.computeIfAbsent(partnerRecord, k -> new HashMap<>())
					.put(child.getId(), coupleType);
				groupedChildIds.add(child.getId());
			}
		}

		// Children with no partner at all: group under the null key. This is
		// the "single parent" case and must remain explicit so setPartnerData
		// can match it.
		for(int i = 0, size = children.size(); i < size; i ++){
			final FLEFRecord child = children.get(i);
			if(groupedChildIds.contains(child.getId()))
				continue;

			childrenByCouple.computeIfAbsent(null, k -> new LinkedHashSet<>())
				.add(child);
			typesByCouple.computeIfAbsent(null, k -> new HashMap<>())
				.put(child.getId(), repository.getParentLinkType(child.getId(), parentId));
		}

		// Build one SiblingsData per couple
		final Map<IndividualData, SiblingsData> resultMap = new LinkedHashMap<>(childrenByCouple.size());
		for(final Map.Entry<FLEFRecord, Set<FLEFRecord>> entry : childrenByCouple.entrySet()){
			final FLEFRecord otherParent = entry.getKey();
			final Set<FLEFRecord> sharedChildren = entry.getValue();

			final IndividualData otherParentData = (otherParent != null
				? repository.getIndividualData(otherParent)
				: null);
			final Map<String, String> types = typesByCouple.get(otherParent);
			final List<IndividualData> childrenDataList = new ArrayList<>(sharedChildren.size());
			final Set<String> childrenWithDescendants = new HashSet<>();

			for(final FLEFRecord child : sharedChildren){
				final IndividualData data = repository.getIndividualData(child);
				childrenDataList.add(data);
				if(repository.hasDescendants(data.getId()))
					childrenWithDescendants.add(data.getId());
			}

			resultMap.put(otherParentData,
				SiblingsData.create(childrenDataList, types, childrenWithDescendants));
		}

		return resultMap;
	}

	/**
	 * Picks the relationship type to display on the connection line between
	 * a child and a couple.
	 * <p>
	 * A couple is "biological" only when <b>both</b> parents are biological
	 * parents of the child. If any parent is adoptive, foster, guarded, or
	 * step, the whole couple is treated as non-biological and the connection
	 * is drawn with the dashed stroke.
	 *
	 * @param type1 relationship type of the child to the first parent
	 * @param type2 relationship type of the child to the second parent
	 * @return the type to display, or {@code null} if both inputs are null
	 */
	private static String pickCoupleType(final String type1, final String type2){
		if(type1 == null)
			return type2;
		if(type2 == null)
			return type1;

		// Prefer the non-biological type: any non-biological link makes the
		// couple connection dashed.
		if(!RelationshipReader.isTypeBiologicalChild(type1))
			return type1;
		if(!RelationshipReader.isTypeBiologicalChild(type2))
			return type2;

		return type1;
	}

	public SiblingsData buildSiblingsData(final String individualId){
		repository.ensureIndices();

		final List<FLEFRecord> parents = repository.getParents(individualId);
		if(parents == null || parents.isEmpty())
			return SiblingsData.create(null, null);

		final Set<FLEFRecord> siblingRecords = new LinkedHashSet<>();
		for(int i = 0, size = parents.size(); i < size; i ++){
			final FLEFRecord parent = parents.get(i);

			final List<FLEFRecord> children = repository.getChildren(parent.getId());
			if(children != null)
				siblingRecords.addAll(children);
		}

		final List<IndividualData> siblingDataList = new ArrayList<>(siblingRecords.size());
		final Set<String> siblingIdsWithDescendants = new HashSet<>();
		for(final FLEFRecord siblingRecord : siblingRecords){
			final IndividualData data = repository.getIndividualData(siblingRecord);
			siblingDataList.add(data);
			if(repository.hasDescendants(data.getId()))
				siblingIdsWithDescendants.add(data.getId());
		}

		return SiblingsData.create(siblingDataList, siblingIdsWithDescendants);
	}

	public List<String> getRelationshipIdsForIndividual(final String individualId){
		return repository.getRelationshipIdsForIndividual(individualId);
	}

	public List<FLEFRecord> getParents(final String currentIndividualId){
		return repository.getParents(currentIndividualId);
	}

	FLEFRecord extractParent(final List<FLEFRecord> parents, final SexType sex){
		final Iterator<FLEFRecord> itr = parents.iterator();
		while(itr.hasNext()){
			final FLEFRecord parent = itr.next();
			final SexType parentSex = IndividualReader.extractSex(parent);
			if(parentSex != SexType.UNKNOWN && sex == parentSex){
				itr.remove();

				return parent;
			}
		}
		return null;
	}

	public void invalidateIndices(){
		repository.invalidateIndices();
		childrenDataCache.clear();
	}

	public Predicate<String> getRelationshipTypeFilter(){
		return repository.getRelationshipTypeFilter();
	}

}
