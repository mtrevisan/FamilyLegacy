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
package io.github.mtrevisan.familylegacy.v2.ui.components.grouptree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * Service for building and navigating non-biological group and entity trees.
 */
public class GroupTreeService{

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";

	private final FLEFModel model;

	// Lookup maps
	private final Map<String, List<FLEFRecord>> entityToParentsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> entityToMembersMap = new HashMap<>();

	public GroupTreeService(final FLEFModel model){
		this.model = model;
	}

	/**
	 * Builds a group tree hierarchy up to maxGenerations using BFS traversal.
	 *
	 * @param rootEntityId   the root entity or group record ID
	 * @param maxGenerations depth limit
	 * @return the root {@link GroupTreeNode} of the constructed tree, or {@code null} if not found
	 */
	public GroupTreeNode buildTree(final String rootEntityId, final int maxGenerations){
		if(rootEntityId == null || rootEntityId.isEmpty() || maxGenerations < 0){
			return null;
		}

		final FLEFRecord rootRecord = model.getRecordById(rootEntityId);
		if(rootRecord == null){
			return null;
		}

		ensureIndices();

		final GroupTreeNode rootNode = new GroupTreeNode(rootRecord, 0);
		final Queue<GroupTreeNode> queue = new ArrayDeque<>();
		queue.add(rootNode);

		while(!queue.isEmpty()){
			final GroupTreeNode currentNode = queue.poll();
			final int currentGen = currentNode.getGeneration();

			if(currentGen >= maxGenerations){
				continue;
			}

			final String currentId = currentNode.getRecordId();
			if(currentId == null){
				continue;
			}

			final int nextGen = currentGen + 1;

			// Add parent/super-group nodes
			final List<FLEFRecord> parents = entityToParentsMap.getOrDefault(currentId, List.of());
			for(final FLEFRecord parentRecord : parents){
				final GroupTreeNode parentNode = new GroupTreeNode(parentRecord, nextGen);
				currentNode.addParent(parentNode);
				queue.add(parentNode);
			}

			// Add member/sub-group nodes
			final List<FLEFRecord> members = entityToMembersMap.getOrDefault(currentId, List.of());
			for(final FLEFRecord memberRecord : members){
				final GroupTreeNode memberNode = new GroupTreeNode(memberRecord, nextGen);
				currentNode.addMember(memberNode);
				queue.add(memberNode);
			}
		}

		return rootNode;
	}

	private void ensureIndices(){
		if(!entityToParentsMap.isEmpty() || !entityToMembersMap.isEmpty()){
			return;
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord rel : relationships){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			if(type == null){
				continue;
			}

			String subjectId = rel.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			if(subjectId == null){
				subjectId = rel.extractReferencedId(TAG_SUBJECT, GroupHandler.TYPE);
			}

			String targetId = rel.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(targetId == null){
				targetId = rel.extractReferencedId(TAG_TARGET, GroupHandler.TYPE);
			}

			if(subjectId != null && targetId != null){
				final FLEFRecord subjectRecord = model.getRecordById(subjectId);
				final FLEFRecord targetRecord = model.getRecordById(targetId);

				if(subjectRecord != null && targetRecord != null){
					entityToParentsMap.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(targetRecord);
					entityToMembersMap.computeIfAbsent(targetId, k -> new ArrayList<>()).add(subjectRecord);
				}
			}
		}
	}

	public void invalidateIndices(){
		entityToParentsMap.clear();
		entityToMembersMap.clear();
	}

}
