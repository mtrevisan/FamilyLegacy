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
package io.github.mtrevisan.familylegacy.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


public final class GroupIndex{

	private static final String DOT = ".";


	private final Map<String, List<FLEFRecord>> groupToAttrsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> groupToMembersMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> groupToParentGroupsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> groupToChildGroupsMap = new HashMap<>();

	public GroupIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		final List<FLEFRecord> groupAttributes = model.getRecordsByType(GroupAttributeHandler.TYPE);
		for(final FLEFRecord groupAttribute : groupAttributes){
			if(!filter.test(groupAttribute))
				continue;
			final String groupId = FLEFRecordHelper.getChildValue(groupAttribute, "group");
			if(groupId != null){
				groupToAttrsMap.computeIfAbsent(groupId, k -> new ArrayList<>()).add(groupAttribute);
			}
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			if(!filter.test(relationship))
				continue;

			final String type = RelationshipReader.extractType(relationship);
			final String subjectId = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_SUBJECT + DOT + IndividualHandler.TYPE);
			final String objectGroup = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_OBJECT + DOT + GroupHandler.TYPE);
			final String subjectGroup = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_SUBJECT + DOT + GroupHandler.TYPE);

			if("group_member".equalsIgnoreCase(type) && objectGroup != null){
				groupToMembersMap.computeIfAbsent(objectGroup, k -> new ArrayList<>()).add(relationship);
			}
			else if("part_of".equalsIgnoreCase(type) && subjectGroup != null && objectGroup != null){
				groupToParentGroupsMap.computeIfAbsent(subjectGroup, k -> new ArrayList<>()).add(relationship);
				groupToChildGroupsMap.computeIfAbsent(objectGroup, k -> new ArrayList<>()).add(relationship);
			}
		}
	}

	public List<FLEFRecord> attributesOfGroup(final FLEFRecord group){
		return (group != null && group.getId() != null)? groupToAttrsMap.getOrDefault(group.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> membersOf(final FLEFRecord group){
		return (group != null && group.getId() != null)? groupToMembersMap.getOrDefault(group.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> parentGroupsOf(final FLEFRecord group){
		return (group != null && group.getId() != null)? groupToParentGroupsMap.getOrDefault(group.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> childGroupsOf(final FLEFRecord group){
		return (group != null && group.getId() != null)? groupToChildGroupsMap.getOrDefault(group.getId(), Collections.emptyList()): Collections.emptyList();
	}

}
