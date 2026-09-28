package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class PlaceIndex {

	private final Map<String, List<FLEFRecord>> placeToEventsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> placeToAttributesMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> placeToSubjectRelationshipsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> placeToTargetRelationshipsMap = new HashMap<>();

	public PlaceIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		for(final FLEFRecord event : model.getRecordsByType(EventHandler.TYPE)){
			if(!filter.test(event))
				continue;
			final String placeId = FLEFRecordHelper.getChildValue(event, "place.place");
			if(placeId != null)
				placeToEventsMap.computeIfAbsent(placeId, k -> new ArrayList<>()).add(event);
		}

		for(final FLEFRecord attr : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			if(!filter.test(attr))
				continue;
			final String placeId = FLEFRecordHelper.getChildValue(attr, "place.place");
			if(placeId != null)
				placeToAttributesMap.computeIfAbsent(placeId, k -> new ArrayList<>()).add(attr);
		}

		for(final FLEFRecord pr : model.getRecordsByType(PlaceRelationshipHandler.TYPE)){
			if(!filter.test(pr))
				continue;
			final String subjectPlace = FLEFRecordHelper.getChildValue(pr, "subject.place");
			final String targetPlace = FLEFRecordHelper.getChildValue(pr, "target.place");

			if(subjectPlace != null)
				placeToSubjectRelationshipsMap.computeIfAbsent(subjectPlace, k -> new ArrayList<>()).add(pr);
			if(targetPlace != null)
				placeToTargetRelationshipsMap.computeIfAbsent(targetPlace, k -> new ArrayList<>()).add(pr);
		}
	}

	public List<FLEFRecord> eventsAtPlace(final String placeId){
		if(placeId == null)
			return Collections.emptyList();
		return placeToEventsMap.getOrDefault(placeId, Collections.emptyList());
	}

	public List<FLEFRecord> attributesAtPlace(final String placeId){
		if(placeId == null)
			return Collections.emptyList();
		return placeToAttributesMap.getOrDefault(placeId, Collections.emptyList());
	}

	public List<FLEFRecord> placeRelationshipsAsSubject(final FLEFRecord place){
		if(place == null || place.getId() == null)
			return Collections.emptyList();
		return placeToSubjectRelationshipsMap.getOrDefault(place.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> placeRelationshipsAsTarget(final FLEFRecord place){
		if(place == null || place.getId() == null)
			return Collections.emptyList();
		return placeToTargetRelationshipsMap.getOrDefault(place.getId(), Collections.emptyList());
	}

}
