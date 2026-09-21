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
package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Shared extraction utilities and precomputed indexes for
 * {@code GroupRecord} and for the relationships that define group
 * membership ({@code group_member}) and group nesting ({@code part_of}).
 * <p>
 * A group is defined by two kinds of relationships, both expressed as
 * {@code RelationshipRecord}:
 * <ul>
 *   <li>{@code group_member} — a {@code subject} individual belongs to
 *       a {@code target} group. The optional {@code role} field carries
 *       the function of the member (member, president, elder, …);</li>
 *   <li>{@code part_of} — a {@code subject} group is contained in a
 *       {@code target} group. This is how a family can be part of a
 *       household, a household part of a neighborhood, and so on.</li>
 * </ul>
 * The indexes built by this class are the single point where those
 * relationships are turned into per-group member and sub-group lists.
 * Every tool in the {@code groups} package goes through this class.
 */
public final class GroupHelper{

	public static final String TYPE_GROUP = "group";
	public static final String TYPE_INDIVIDUAL = "individual";
	public static final String TYPE_RELATIONSHIP = "relationship";

	public static final String TAG_NAME = "name";
	public static final String TAG_VALUE = "value";
	public static final String TAG_TYPE = "type";
	public static final String TAG_SUBJECT = "subject";
	public static final String TAG_TARGET = "target";
	public static final String TAG_ROLE = "role";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";

	public static final String REL_GROUP_MEMBER = "group_member";
	public static final String REL_PART_OF = "part_of";

	/** Group types declared by the protocol, in a stable order. */
	public static final List<String> DECLARED_GROUP_TYPES = List.of(
		"family", "household", "neighborhood", "fraternity", "club",
		"literary_society", "association", "organization", "tribe"
	);


	private GroupHelper(){
	}


	public static List<FLEFRecord> listAllGroups(final FLEFModel model){
		return model.getRecordsByType(GroupHandler.TYPE);
	}

	public static Map<String, FLEFRecord> indexGroupsById(final FLEFModel model){
		final Map<String, FLEFRecord> result = new LinkedHashMap<>();
		for(final FLEFRecord group : listAllGroups(model))
			if(group.getId() != null)
				result.put(group.getId(), group);
		return result;
	}

	/**
	 * Returns the primary display name of a group. {@code GroupRecord.name}
	 * is a list of {@code NameStructure}; the first one with a non-blank
	 * {@code value} is used. When no name is available, the group id is
	 * returned so the display stays readable.
	 */
	public static String displayName(final FLEFRecord group){
		if(group == null)
			return "";
		for(final FLEFRecord child : group.getChildren()){
			if(!TAG_NAME.equalsIgnoreCase(child.getTag()))
				continue;
			final String value = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
			if(value != null && !value.isBlank())
				return value;
			final FLEFRecord onlyChild = child.getTheOnlyChild();
			if(onlyChild != null && onlyChild.getValue() != null && !onlyChild.getValue().isBlank())
				return onlyChild.getValue();
		}
		return group.getId() != null? group.getId(): "";
	}

	/** Returns the type of a group, or {@code null} when missing. */
	public static String groupType(final FLEFRecord group){
		return (group != null? FLEFRecordHelper.getChildValue(group, TAG_TYPE): null);
	}


	/* ======================================================================
	 *                          Precomputed indexes
	 * ====================================================================== */

	/** The cached view of one group, with all its relationships resolved. */
	public record GroupProfile(
		String id,
		String name,
		String type,
		Set<String> memberIds,
		Set<String> parentGroupIds,
		Set<String> childGroupIds,
		Set<String> sourceIds){}

	/**
	 * Builds the profile of every group in a single pass over the
	 * relationship list. The result is keyed by group id.
	 */
	public static Map<String, GroupProfile> buildProfiles(final FLEFModel model){
		final List<FLEFRecord> groups = listAllGroups(model);

		// Members, sub-groups, and parent groups, keyed by group id.
		final Map<String, Set<String>> members = new LinkedHashMap<>();
		final Map<String, Set<String>> children = new LinkedHashMap<>();
		final Map<String, Set<String>> parents = new LinkedHashMap<>();

		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			if(type == null)
				continue;
			final String t = type.toLowerCase(java.util.Locale.ROOT);

			final String subject = rel.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			final String subjectGroup = rel.extractReferencedId(TAG_SUBJECT, TYPE_GROUP);
			final String target = rel.extractReferencedId(TAG_TARGET, TYPE_INDIVIDUAL);
			final String targetGroup = rel.extractReferencedId(TAG_TARGET, TYPE_GROUP);

			if(REL_GROUP_MEMBER.equals(t)){
				// subject = individual, target = group.
				if(subject != null && targetGroup != null)
					members.computeIfAbsent(targetGroup, k -> new LinkedHashSet<>()).add(subject);
			}
			else if(REL_PART_OF.equals(t)){
				// subject = child group, target = parent group.
				if(subjectGroup != null && targetGroup != null){
					children.computeIfAbsent(targetGroup, k -> new LinkedHashSet<>()).add(subjectGroup);
					parents.computeIfAbsent(subjectGroup, k -> new LinkedHashSet<>()).add(targetGroup);
				}
			}
		}

		final Map<String, GroupProfile> profiles = new LinkedHashMap<>();
		for(final FLEFRecord group : groups){
			final String id = group.getId();
			if(id == null)
				continue;
			final Set<String> sources = new LinkedHashSet<>();
			collectSources(group, sources);
			profiles.put(id, new GroupProfile(
				id,
				displayName(group),
				groupType(group),
				members.getOrDefault(id, Set.of()),
				parents.getOrDefault(id, Set.of()),
				children.getOrDefault(id, Set.of()),
				sources
			));
		}
		return profiles;
	}

	private static void collectSources(final FLEFRecord record, final Set<String> out){
		for(final FLEFRecord child : record.getChildren()){
			if(!TAG_SOURCE.equalsIgnoreCase(child.getTag()))
				continue;
			final String sourceId = FLEFRecordHelper.getChildValue(child, TAG_SOURCE);
			if(sourceId != null)
				out.add(sourceId);
		}
	}


	/* ======================================================================
	 *                          Row models
	 * ====================================================================== */

	/** Row for the group management table. */
	public record GroupRow(String id, String name, String type,
								  int memberCount, int childCount, int parentCount, int sourceCount){}

	public static GroupRow toRow(final GroupProfile profile){
		return new GroupRow(
			profile.id(),
			profile.name(),
			profile.type(),
			profile.memberIds().size(),
			profile.childGroupIds().size(),
			profile.parentGroupIds().size(),
			profile.sourceIds().size()
		);
	}


	/* ======================================================================
	 *                          Record creation
	 * ====================================================================== */

	/**
	 * Creates a new {@code GroupRecord} with the given name and type.
	 * The record is added to the model and returned so the caller can
	 * further customize it.
	 */
	public static FLEFRecord createGroup(final FLEFModel model, final String name,
		final String type, final String idPrefix){
		final FLEFRecord group = FLEFRecord.createMainRecord(TYPE_GROUP, idPrefix, model)
			.addChild(FLEFRecord.createChildWithTag(TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(TAG_VALUE, name))
			)
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, type))
			.addChild(AuditBuilder.build());
		model.addRecord(group);
		return group;
	}

	/**
	 * Creates a {@code group_member} relationship linking the given
	 * individual to the given group, with an optional role.
	 */
	public static FLEFRecord createMembership(final FLEFModel model, final String individualId,
		final String groupId, final String role, final String idPrefix){
		final FLEFRecord rel = FLEFRecord.createMainRecord(TYPE_RELATIONSHIP, idPrefix, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_TYPE, REL_GROUP_MEMBER))
			.addChild(FLEFRecord.createChildWithTag(TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(TYPE_INDIVIDUAL, individualId))
			)
			.addChild(FLEFRecord.createChildWithTag(TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(TYPE_GROUP, groupId))
			);
		if(role != null && !role.isBlank())
			rel.addChild(FLEFRecord.createChildWithTagAndValue(TAG_ROLE, role));
		rel.addChild(AuditBuilder.build());
		model.addRecord(rel);
		return rel;
	}


	/* ======================================================================
	 *                          Bulk operations
	 * ====================================================================== */

	/**
	 * Returns the ids of every relationship record that makes an
	 * individual a member of a group.
	 */
	public static List<String> membershipRelationshipIds(final FLEFModel model,
		final String groupId){
		final List<String> result = new ArrayList<>();
		if(groupId == null)
			return result;
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			if(type == null || !REL_GROUP_MEMBER.equalsIgnoreCase(type))
				continue;
			final String target = rel.extractReferencedId(TAG_TARGET, TYPE_GROUP);
			if(groupId.equals(target))
				result.add(rel.getId());
		}
		return result;
	}

	/**
	 * Returns the ids of every relationship record that involves the
	 * given group as either subject or target, regardless of type. Used
	 * before deleting a group, to clean up the relationships that would
	 * otherwise dangle.
	 */
	public static List<String> allRelationshipIdsForGroup(final FLEFModel model,
		final String groupId){
		final List<String> result = new ArrayList<>();
		if(groupId == null)
			return result;
		for(final FLEFRecord rel : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String subject = rel.extractReferencedId(TAG_SUBJECT, TYPE_GROUP);
			final String target = rel.extractReferencedId(TAG_TARGET, TYPE_GROUP);
			if(groupId.equals(subject) || groupId.equals(target))
				result.add(rel.getId());
		}
		return result;
	}

}
