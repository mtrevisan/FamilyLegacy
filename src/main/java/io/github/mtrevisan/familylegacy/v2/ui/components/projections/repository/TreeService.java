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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
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

	private static final String TAG_SEX = "sex";


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

	public TreeNode buildTree(final String rootIndividualId, final boolean showPartner, final int maxAncestors){
		if(StringUtils.isEmpty(rootIndividualId) || maxAncestors < 0)
			return null;

		final FLEFRecord rootIndividual = model.getRecordById(rootIndividualId);
		if(rootIndividual == null)
			return null;

		repository.ensureIndices();

		final IndividualData rootData = repository.getIndividualData(rootIndividual);
		final TreeNode rootNode = new TreeNode(rootIndividual, rootData, 0);
		if(showPartner){
			final Map<IndividualData, SiblingsData> partnerChildrenDataMap = buildChildrenData(rootIndividualId);
			if(!partnerChildrenDataMap.isEmpty()){
				final Map.Entry<IndividualData, SiblingsData> partnerChildrenData = partnerChildrenDataMap.entrySet().iterator().next();
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
			if(currentGeneration >= maxAncestors)
				continue;

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
				setPartnerData(fatherNode, mother, (mother != null ? repository.getIndividualData(mother) : null));
				currentNode.setFather(fatherNode);
				queue.add(fatherNode);
			}

			if(mother != null){
				final IndividualData motherData = repository.getIndividualData(mother);
				final TreeNode motherNode = new TreeNode(mother, motherData, nextGeneration);
				setPartnerData(motherNode, father, (father != null ? repository.getIndividualData(father) : null));
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
			if(partnerId == null || (currentPartner != null && partnerId.equals(currentPartner.getId()))){
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

		final Map<FLEFRecord, List<FLEFRecord>> childrenByOtherParentMap = new LinkedHashMap<>();
		for(int i = 0, childrenSize = children.size(); i < childrenSize; i ++){
			final FLEFRecord child = children.get(i);
			final List<FLEFRecord> parents = repository.getParents(child.getId());

			FLEFRecord otherParent = null;
			for(int j = 0, parentSize = (parents != null? parents.size(): 0); j < parentSize; j ++){
				final FLEFRecord parent = parents.get(j);
				if(!parent.getId().equals(parentId)){
					otherParent = parent;

					break;
				}
			}
			childrenByOtherParentMap.computeIfAbsent(otherParent, k -> new ArrayList<>())
				.add(child);
		}

		final Map<IndividualData, SiblingsData> resultMap = new LinkedHashMap<>(childrenByOtherParentMap.size());
		for(final Map.Entry<FLEFRecord, List<FLEFRecord>> entry : childrenByOtherParentMap.entrySet()){
			final FLEFRecord otherParentRecord = entry.getKey();
			final List<FLEFRecord> sharedChildren = entry.getValue();

			final IndividualData otherParentData = (otherParentRecord != null
				? repository.getIndividualData(otherParentRecord)
				: null);
			final List<IndividualData> childrenDataList = new ArrayList<>(sharedChildren.size());
			final Set<String> childrenIdsWithDescendants = new HashSet<>();

			for(int i = 0, size = sharedChildren.size(); i < size; i ++){
				final FLEFRecord childRecord = sharedChildren.get(i);

				final IndividualData data = repository.getIndividualData(childRecord);
				childrenDataList.add(data);
				if(repository.hasDescendants(data.getId()))
					childrenIdsWithDescendants.add(data.getId());
			}
			resultMap.put(otherParentData, SiblingsData.create(childrenDataList, childrenIdsWithDescendants));
		}

		return resultMap;
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
			final SexType parentSex = IndividualData.extractSex(parent);
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
