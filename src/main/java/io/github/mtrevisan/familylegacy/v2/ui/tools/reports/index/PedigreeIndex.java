package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


public final class PedigreeIndex{

	private static final String DOT = ".";


	public record ParentEdge(FLEFRecord parent, String relationshipType){}

	private final Map<String, List<FLEFRecord>> parentToChildrenMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> childToParentsMap = new HashMap<>();
	private final Map<String, List<ParentEdge>> childToParentEdgesMap = new HashMap<>();

	public PedigreeIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(int i = 0, size = relationships.size(); i < size; i++){
			final FLEFRecord relationship = relationships.get(i);
			if(!filter.test(relationship))
				continue;

			final String type = RelationshipReader.extractType(relationship);
			final String subjectId = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_SUBJECT + DOT + IndividualHandler.TYPE);
			final String objectId = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_OBJECT + DOT + IndividualHandler.TYPE);

			if(subjectId == null || objectId == null || type == null)
				continue;

			final boolean childRel = type.endsWith("_child");
			final boolean parentRel = type.endsWith("_parent");

			if(childRel || parentRel){
				final String childId = (childRel? subjectId: objectId);
				final String parentId = (childRel? objectId: subjectId);

				final FLEFRecord child = model.getRecordById(childId);
				final FLEFRecord parent = model.getRecordById(parentId);

				if(child != null && parent != null && filter.test(child) && filter.test(parent)){
					parentToChildrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(child);
					childToParentsMap.computeIfAbsent(childId, k -> new ArrayList<>()).add(parent);
					childToParentEdgesMap.computeIfAbsent(childId, k -> new ArrayList<>()).add(new ParentEdge(parent, type));
				}
			}
		}
	}

	public List<FLEFRecord> parentsOf(final FLEFRecord child){
		if(child == null || child.getId() == null)
			return Collections.emptyList();
		return childToParentsMap.getOrDefault(child.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> childrenOf(final FLEFRecord parent){
		if(parent == null || parent.getId() == null)
			return Collections.emptyList();
		return parentToChildrenMap.getOrDefault(parent.getId(), Collections.emptyList());
	}

	public List<ParentEdge> parentEdgesOf(final FLEFRecord child){
		if(child == null || child.getId() == null)
			return Collections.emptyList();
		return childToParentEdgesMap.getOrDefault(child.getId(), Collections.emptyList());
	}

}
