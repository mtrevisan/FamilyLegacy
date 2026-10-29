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
		for(final FLEFRecord event : model.getRecordsByType(EventHandler.TYPE)){
			final String placeId = FLEFRecordHelper.getChildValue(event, EVENT_PLACE);
			if(placeId != null)
				usage.merge(placeId, 1, Integer::sum);
		}

		final List<PlaceData.Place> places = new ArrayList<>();
		for(final FLEFRecord record : model.getRecordsByType(PlaceHandler.TYPE)){
			final double[] coordinates = PlaceChecker.parseCoordinates(PlaceReader.extractCoordinates(record));
			places.add(new PlaceData.Place(record.getId(), PlaceReader.extractNames(record), PlaceReader.extractType(record),
				(coordinates != null? coordinates[0]: null), (coordinates != null? coordinates[1]: null),
				usage.getOrDefault(record.getId(), 0)));
		}

		final List<PlaceData.Relation> relations = new ArrayList<>();
		for(final FLEFRecord record : model.getRecordsByType(PlaceRelationshipHandler.TYPE)){
			final String subject = FLEFRecordHelper.getChildValue(record,
				FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_SUBJECT, PlaceHandler.TYPE));
			final String target = FLEFRecordHelper.getChildValue(record,
				FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_TARGET, PlaceHandler.TYPE));
			if(subject != null && target != null)
				relations.add(new PlaceData.Relation(record.getId(), subject, target, PlaceRelationshipReader.extractType(record)));
		}
		return new PlaceData(places, relations);
	}

}
