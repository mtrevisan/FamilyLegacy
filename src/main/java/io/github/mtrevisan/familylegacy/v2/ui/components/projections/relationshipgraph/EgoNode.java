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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Represents a central individual or group (Ego) and their surrounding
 * network of relationships (parents, partners, children, groups, and
 * associates) regardless of whether they are biological or non-biological.
 * <p>
 * The node owns mutable collections during construction (through the
 * {@code add*} methods) but exposes them through unmodifiable views, so
 * that external code cannot accidentally break the internal consistency
 * of the node.
 * <p>
 * Equality and hash code are based on the ego id only, and are safe when
 * the id is {@code null}: two nodes whose id is {@code null} are
 * considered distinct, to avoid accidental collisions on partially built
 * nodes.
 */
final class EgoNode{

	public record RelationInfo(String type, String role, boolean isInverse){}


	/**
	 * Defines the category of relationship relative to the Ego node.
	 */
	public enum RelationshipCategory{
		PARENT,
		PARTNER,
		CHILD,
		GROUP,
		ASSOCIATE
	}


	private final FLEFRecord egoRecord;
	private final IndividualData egoData;

	private final List<RelationInfo> relationsWithEgo = new ArrayList<>();

	private final Map<RelationshipCategory, Set<EgoNode>> relatedNodesMap = new EnumMap<>(RelationshipCategory.class);
	private final Set<FLEFRecord> groupRecords = new LinkedHashSet<>();
	private final Map<FLEFRecord, List<RelationInfo>> groupRelationsMap = new HashMap<>();


	public EgoNode(final FLEFRecord egoRecord, final IndividualData egoData){
		this.egoRecord = egoRecord;
		this.egoData = egoData;

		for(final RelationshipCategory category : RelationshipCategory.values())
			relatedNodesMap.put(category, new LinkedHashSet<>());
	}


	public FLEFRecord getEgoRecord(){
		return egoRecord;
	}

	public IndividualData getEgoData(){
		return egoData;
	}

	public String getEgoId(){
		if(egoRecord != null)
			return egoRecord.getId();

		return (egoData != null? egoData.getId(): null);
	}

	public void addRelationInfo(final String type, final String role, final boolean isInverse){
		relationsWithEgo.add(new RelationInfo(type, role, isInverse));
	}

	/**
	 * Returns the relation metadata collected for this node.
	 *
	 * @return the unmodifiable list, never {@code null}
	 */
	public List<RelationInfo> getRelationsWithEgo(){
		return relationsWithEgo;
	}

	public void addRelatedNode(final RelationshipCategory category, final EgoNode node){
		if(node != null && category != null)
			relatedNodesMap.get(category)
				.add(node);
	}

	/**
	 * Returns the nodes related through the given category.
	 *
	 * @param category the relationship category
	 * @return the unmodifiable set, never {@code null}
	 */
	public Set<EgoNode> getRelatedNodes(final RelationshipCategory category){
		return relatedNodesMap.get(category);
	}


	public void addGroupRecord(final FLEFRecord groupRecord, final String type, final String role,
			final boolean isInverse){
		if(groupRecord != null){
			groupRecords.add(groupRecord);
			groupRelationsMap.computeIfAbsent(groupRecord, k -> new ArrayList<>())
				.add(new RelationInfo(type, role, isInverse));
		}
	}

	/**
	 * Returns the group records directly related to this node.
	 *
	 * @return the unmodifiable set, never {@code null}
	 */
	public Set<FLEFRecord> getGroupRecords(){
		return groupRecords;
	}

	/**
	 * Returns the relation metadata associated with the given group record. Returns an empty list if the group is not
	 * directly related to this node.
	 *
	 * @param groupRecord the group record
	 * @return the unmodifiable list, never {@code null}
	 */
	public List<RelationInfo> getGroupRelationInfo(final FLEFRecord groupRecord){
		return groupRelationsMap.getOrDefault(groupRecord, Collections.emptyList());
	}


	@Override
	public boolean equals(final Object other){
		if(this == other)
			return true;
		if(other == null || getClass() != other.getClass())
			return false;

		final EgoNode egoNode = (EgoNode)other;
		final String thisId = getEgoId();
		final String otherId = egoNode.getEgoId();
		// Nodes with a null id are considered distinct, to avoid accidental
		// collisions during construction.
		return (thisId != null && thisId.equals(otherId));
	}

	@Override
	public int hashCode(){
		final String id = getEgoId();
		return (id != null? id.hashCode(): System.identityHashCode(this));
	}

}
