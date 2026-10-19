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
package io.github.mtrevisan.familylegacy.ui.tools.groups;

import io.github.mtrevisan.familylegacy.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

	private GroupHelper(){}


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

	/** Returns the type of a group, or {@code null} when missing. */
	public static String groupType(final FLEFRecord group){
		return (group != null? GroupReader.extractType(group): null);
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

		List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null)
				continue;
			final String t = type.toLowerCase(Locale.ROOT);

			final String subject = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String subjectGroup = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, GroupHandler.TYPE);
			final String objectGroup = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupHandler.TYPE);

			if(RelationshipReader.isTypeGroupMember(t)){
				// subject = individual, target = group.
				if(subject != null && objectGroup != null)
					members.computeIfAbsent(objectGroup, k -> new LinkedHashSet<>())
						.add(subject);
			}
			else if(RelationshipReader.isTypePartOf(t)){
				// subject = child group, target = parent group.
				if(subjectGroup != null && objectGroup != null){
					children.computeIfAbsent(objectGroup, k -> new LinkedHashSet<>())
						.add(subjectGroup);
					parents.computeIfAbsent(subjectGroup, k -> new LinkedHashSet<>())
						.add(objectGroup);
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
			final String displayName = GroupReader.extractPrimaryName(group);
			profiles.put(id, new GroupProfile(
				id,
				displayName,
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
			if(!GroupReader.TAG_SOURCE.equalsIgnoreCase(child.getTag()))
				continue;

			final String sourceId = GroupReader.extractSource(child);
			if(sourceId != null)
				out.add(sourceId);
		}
	}


	/* ======================================================================
	 *                          Row models
	 * ====================================================================== */

	/** Row for the group management table. */
	public record GroupRow(String id, String name, String type, int memberCount, int parentCount, int childCount,
		int sourceCount){}


	public static GroupRow toRow(final GroupProfile profile){
		return new GroupRow(
			profile.id(),
			profile.name(),
			profile.type(),
			profile.memberIds().size(),
			profile.parentGroupIds().size(),
			profile.childGroupIds().size(),
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
		final FLEFRecord group = FLEFRecord.createMainRecord(GroupHandler.TYPE, idPrefix, model)
			.addChild(FLEFRecord.createChildWithTag(GroupReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, name))
			)
			.addChild(FLEFRecord.createChildWithTagAndValue(GroupReader.TAG_TYPE, type))
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
		final FLEFRecord rel = FLEFRecord.createMainRecord(RelationshipHandler.TYPE, idPrefix, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_GROUP_MEMBER))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualHandler.TYPE, individualId))
			)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(GroupHandler.TYPE, groupId))
			);
		if(role != null && !role.isBlank())
			rel.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_ROLE, role));
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
	public static List<String> membershipRelationshipIds(final String groupId, final FLEFModel model){
		final List<String> result = new ArrayList<>();
		if(groupId == null)
			return result;

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(!RelationshipReader.isTypeGroupMember(type))
				continue;

			final String object = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupHandler.TYPE);
			if(groupId.equals(object))
				result.add(relationship.getId());
		}
		return result;
	}

	/**
	 * Returns the ids of every relationship record that involves the
	 * given group as either subject or target, regardless of type. Used
	 * before deleting a group, to clean up the relationships that would
	 * otherwise dangle.
	 */
	public static Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> relationshipIds(final String groupId,
			final FLEFModel model){
		final Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> result = new HashMap<>();
		if(groupId == null)
			return result;

		final List<FLEFRecord> groupAttributes = model.getRecordsByType(GroupAttributeHandler.TYPE);
		for(final FLEFRecord groupAttribute : groupAttributes){
			final String groupRecordId = groupAttribute.extractReferencedId(GroupAttributeReader.TAG_GROUP, GroupHandler.TYPE);
			if(groupId.equals(groupRecordId))
				result.computeIfAbsent(GroupAttributeHandler.class, k -> new ArrayList<>())
					.add(groupAttribute);
		}

		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final String participant = eventParticipation.extractReferencedId(EventParticipationReader.TAG_PARTICIPANT, GroupHandler.TYPE);
			if(groupId.equals(participant))
				result.computeIfAbsent(EventParticipationHandler.class, k -> new ArrayList<>())
					.add(eventParticipation);
		}

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, GroupHandler.TYPE);
			final String objectId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupHandler.TYPE);
			if(groupId.equals(subjectId) || groupId.equals(objectId))
				result.computeIfAbsent(RelationshipHandler.class, k -> new ArrayList<>())
					.add(relationship);
		}

		final List<FLEFRecord> contextImpacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord contextImpact : contextImpacts){
			final String target = contextImpact.extractReferencedId(ContextImpactReader.TAG_TARGET, GroupHandler.TYPE);
			if(groupId.equals(target))
				result.computeIfAbsent(ContextImpactHandler.class, k -> new ArrayList<>())
					.add(contextImpact);
		}

		final List<FLEFRecord> researchQuestions = model.getRecordsByType(ResearchQuestionHandler.TYPE);
		for(final FLEFRecord researchQuestion : researchQuestions){
			final List<FLEFRecord> targets = FLEFRecordHelper.findChildren(researchQuestion, ResearchQuestionReader.TAG_TARGET, GroupHandler.TYPE);
			for(final FLEFRecord target : targets)
				if(groupId.equals(target.getId())){
					result.computeIfAbsent(ResearchQuestionHandler.class, k -> new ArrayList<>())
						.add(researchQuestion);

					break;
				}
		}

		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES, GroupHandler.TYPE);
			for(final FLEFRecord resolve : resolves)
				if(groupId.equals(resolve.getId())){
					result.computeIfAbsent(ConclusionHandler.class, k -> new ArrayList<>())
						.add(conclusion);

					break;
				}
		}

		final List<FLEFRecord> identityHypotheses = model.getRecordsByType(IdentityHypothesisHandler.TYPE);
		for(final FLEFRecord identityHypothesis : identityHypotheses){
			final List<FLEFRecord> identities = FLEFRecordHelper.findChildren(identityHypothesis, IdentityHypothesisReader.TAG_IDENTITY, GroupHandler.TYPE);
			for(final FLEFRecord identity : identities)
				if(groupId.equals(identity.getId())){
					result.computeIfAbsent(IdentityHypothesisHandler.class, k -> new ArrayList<>())
						.add(identityHypothesis);

					break;
				}
		}

		return result;
	}

	/**
	 * Deletes the given group and all the relationships that
	 * involve it, in a single operation. The relationships are removed
	 * first, so the model is never left with a dangling reference.
	 *
	 * @param groupId the group to delete
	 * @param model        the model
	 */
	public static void deleteGroup(final String groupId, final FLEFModel model){
		if(groupId == null)
			return;

		final Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> relationshipIds = relationshipIds(groupId, model);
		for(final Map.Entry<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> entry : relationshipIds.entrySet())
			for(final FLEFRecord record : entry.getValue())
				model.removeRecord(record.getId());

		model.removeRecord(groupId);
	}

}
