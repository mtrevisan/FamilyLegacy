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
package io.github.mtrevisan.familylegacy.search;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * The names of the members of the groups, so that a group is found by the people that belong to it ("Mario Rossi Anna Bianchi"
 * finds the family of the two spouses).
 * <p>
 * Every name of every member is included (a person can have many), not only the one shown. The members hidden by the privacy policy
 * are not included, so that they cannot be found through their group.
 * <p>
 * A group of type family or household is also found by the names of the parents and of the children of its members, through the
 * "child of" relationships (of any kind: biological, adoptive, ...), because those relationships link two individuals and never a
 * group: "Mario Rossi Anna Bianchi" finds the family whose members are their children.
 * <p>
 * The relationships are indexed once, when the object is created, so it is meant to be created once and used for many groups.
 */
public final class GroupMemberNames{

	private final FLEFModel model;
	private final Map<String, Set<String>> membersByGroup = new HashMap<>();
	//the role of a member in a group, by group id and member id
	private final Map<String, Map<String, String>> rolesByGroup = new HashMap<>();
	private final Map<String, Set<String>> parentsByChild = new HashMap<>();
	private final Map<String, Set<String>> childrenByParent = new HashMap<>();


	public GroupMemberNames(final FLEFModel model){
		this.model = model;

		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE))){
			final String type = RelationshipReader.extractType(relationship);
			if(type != null && RelationshipReader.CHILD_TYPES.contains(type.toLowerCase(Locale.ROOT))){
				//"subject is the child of target"
				final String childId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
				final String parentId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
				if(childId != null && parentId != null){
					parentsByChild.computeIfAbsent(childId, k -> new LinkedHashSet<>())
						.add(parentId);
					childrenByParent.computeIfAbsent(parentId, k -> new LinkedHashSet<>())
						.add(childId);
				}
				continue;
			}
			if(!RelationshipReader.ENUM_TYPE_GROUP_MEMBER.equalsIgnoreCase(type))
				continue;

			final String memberId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String groupId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupReader.TYPE);
			if(memberId != null && groupId != null){
				membersByGroup.computeIfAbsent(groupId, k -> new LinkedHashSet<>())
					.add(memberId);
				final String role = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE);
				if(StringUtils.isNotBlank(role))
					rolesByGroup.computeIfAbsent(groupId, k -> new HashMap<>())
						.putIfAbsent(memberId, role);
			}
		}
	}


	/** A member of a group: the name to show and the role, which is {@code null} if none is recorded. */
	public record Member(String name, String role){}

	/**
	 * @param group	The group.
	 * @return	The visible individuals that are members of the group (not their relatives), with their role; empty if there are none.
	 */
	public List<Member> members(final FLEFRecord group){
		if(group == null)
			return List.of();

		final Set<String> memberIds = membersByGroup.get(group.getId());
		if(memberIds == null)
			return List.of();

		final Map<String, String> roles = rolesByGroup.getOrDefault(group.getId(), Map.of());
		final List<Member> members = new ArrayList<>(memberIds.size());
		for(final String memberId : memberIds){
			final FLEFRecord individual = model.getRecordById(memberId);
			if(individual == null || !PrivacyService.isVisible(model, individual))
				continue;

			final String name = IndividualReader.extractPrimaryFullname(individual);
			members.add(new Member(StringUtils.defaultIfBlank(name, memberId), roles.get(memberId)));
		}
		return members;
	}

	/**
	 * @param group	The group.
	 * @return	All the names of all the visible individuals that are members of the group and, for a family or a household, of the
	 * 	parents and children of its members; empty if there are none.
	 */
	public List<String> names(final FLEFRecord group){
		if(group == null)
			return List.of();

		final Set<String> memberIds = membersByGroup.get(group.getId());
		if(memberIds == null)
			return List.of();

		final Set<String> ids = new LinkedHashSet<>(memberIds);
		final String type = GroupReader.extractType(group);
		if(GroupReader.ENUM_TYPE_FAMILY.equalsIgnoreCase(type) || GroupReader.ENUM_TYPE_HOUSEHOLD.equalsIgnoreCase(type))
			for(final String memberId : memberIds){
				ids.addAll(parentsByChild.getOrDefault(memberId, Set.of()));
				ids.addAll(childrenByParent.getOrDefault(memberId, Set.of()));
			}

		final List<String> names = new ArrayList<>(ids.size());
		for(final String id : ids){
			final FLEFRecord individual = model.getRecordById(id);
			if(individual != null && PrivacyService.isVisible(model, individual))
				names.addAll(IndividualReader.extractFullNames(individual));
		}
		return names;
	}

}
