package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public final class RelationshipIndex {

	private final Map<String, List<FLEFRecord>> personToRelationshipsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> personToGroupMembershipsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> personToSpousesMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> personToAssociatesMap = new HashMap<>();

	public RelationshipIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(int i = 0, size = relationships.size(); i < size; i++){
			final FLEFRecord relationship = relationships.get(i);
			if(!filter.test(relationship))
				continue;

			final String type = FLEFRecordHelper.getChildValue(relationship, "type");
			if(type == null)
				continue;
			final String t = type.toLowerCase(Locale.ROOT);

			final String subjectId = FLEFRecordHelper.getChildValue(relationship, "subject.individual");
			final String objectId = FLEFRecordHelper.getChildValue(relationship, "object.individual");
			final String objectGroup = FLEFRecordHelper.getChildValue(relationship, "object.group");

			if(subjectId != null)
				personToRelationshipsMap.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(relationship);
			if(objectId != null && !objectId.equals(subjectId))
				personToRelationshipsMap.computeIfAbsent(objectId, k -> new ArrayList<>()).add(relationship);

			if("group_member".equals(t) && subjectId != null && objectGroup != null){
				personToGroupMembershipsMap.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(relationship);
			}

			if(subjectId != null && objectId != null){
				final FLEFRecord subjRec = model.getRecordById(subjectId);
				final FLEFRecord objeRec = model.getRecordById(objectId);

				if(subjRec != null && objeRec != null && filter.test(subjRec) && filter.test(objeRec)){
					if(t.endsWith("_spouse") || t.endsWith("_partner")){
						personToSpousesMap.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(objeRec);
						personToSpousesMap.computeIfAbsent(objectId, k -> new ArrayList<>()).add(subjRec);
					}
					else if("associate".equals(t)){
						personToAssociatesMap.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(objeRec);
						personToAssociatesMap.computeIfAbsent(objectId, k -> new ArrayList<>()).add(subjRec);
					}
				}
			}
		}
	}

	public List<FLEFRecord> relationshipsOf(final FLEFRecord person){
		if(person == null || person.getId() == null)
			return Collections.emptyList();
		return personToRelationshipsMap.getOrDefault(person.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> groupMembershipsOf(final FLEFRecord person){
		if(person == null || person.getId() == null)
			return Collections.emptyList();
		return personToGroupMembershipsMap.getOrDefault(person.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> spousesOf(final FLEFRecord ind){
		if(ind == null || ind.getId() == null)
			return Collections.emptyList();
		return personToSpousesMap.getOrDefault(ind.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> associatesOf(final FLEFRecord ind){
		if(ind == null || ind.getId() == null)
			return Collections.emptyList();
		return personToAssociatesMap.getOrDefault(ind.getId(), Collections.emptyList());
	}

}
