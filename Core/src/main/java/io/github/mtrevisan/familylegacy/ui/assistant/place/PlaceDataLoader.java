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
package io.github.mtrevisan.familylegacy.ui.assistant.place;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** Reads the places, their relationships and how much they are used from the model. */
final class PlaceDataLoader{

	/** The place of an event is the place of the place citation of the event. */
	private static final String EVENT_PLACE = FLEFRecordHelper.composePath(PlaceHandler.TYPE, PlaceHandler.TYPE);


	private PlaceDataLoader(){}


	static PlaceData load(final FLEFModel model){
		final Map<String, Integer> usage = new HashMap<>();
		final List<FLEFRecord> events = model.getRecordsByType(EventHandler.TYPE);
		for(final FLEFRecord event : events){
			final String placeId = FLEFRecordHelper.getChildValue(event, EVENT_PLACE);
			if(placeId != null)
				usage.merge(placeId, 1, Integer::sum);
		}

		final List<PlaceData.Place> filteredPlaces = new ArrayList<>();
		final List<FLEFRecord> places = model.getRecordsByType(PlaceHandler.TYPE);
		for(final FLEFRecord place : places){
			final double[] coordinates = PlaceChecker.parseCoordinates(PlaceReader.extractCoordinates(place));
			filteredPlaces.add(new PlaceData.Place(place.getId(), PlaceReader.extractNames(place), PlaceReader.extractType(place),
				(coordinates != null? coordinates[0]: null), (coordinates != null? coordinates[1]: null),
				usage.getOrDefault(place.getId(), 0)));
		}

		final List<PlaceData.Relation> relations = new ArrayList<>();
		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
		for(final FLEFRecord placeRelationship : placeRelationships){
			final String subject = FLEFRecordHelper.getChildValue(placeRelationship,
				FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_SUBJECT, PlaceHandler.TYPE));
			final String target = FLEFRecordHelper.getChildValue(placeRelationship,
				FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_TARGET, PlaceHandler.TYPE));
			if(subject != null && target != null)
				relations.add(new PlaceData.Relation(placeRelationship.getId(), subject, target, PlaceRelationshipReader.extractType(placeRelationship)));
		}
		return new PlaceData(filteredPlaces, relations);
	}

}
