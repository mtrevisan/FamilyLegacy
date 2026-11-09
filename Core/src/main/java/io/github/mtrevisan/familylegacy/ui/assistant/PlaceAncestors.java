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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/** The containment of the places, shared by the assistants that need to tell a place that contains another from an unrelated one. */
public final class PlaceAncestors{

	private static final Set<String> TERRITORIAL = Set.of(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF,
		PlaceRelationshipReader.ENUM_TYPE_GEOGRAPHIC_PART_OF);


	private PlaceAncestors(){}


	/** The places that contain each place, through the relationships of territory, at any depth. */
	public static Map<String, Set<String>> of(final FLEFModel model){
		final Map<String, Set<String>> parents = new HashMap<>();
		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
		for(final FLEFRecord placeRelationship : placeRelationships){
			if(!TERRITORIAL.contains(PlaceRelationshipReader.extractType(placeRelationship)))
				continue;

			final String subject = FLEFRecordHelper.getChildValue(placeRelationship,
				FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_SUBJECT, PlaceHandler.TYPE));
			final String target = FLEFRecordHelper.getChildValue(placeRelationship,
				FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_TARGET, PlaceHandler.TYPE));
			if(subject != null && target != null)
				parents.computeIfAbsent(subject, k -> new HashSet<>()).add(target);
		}

		final Map<String, Set<String>> ancestors = new HashMap<>();
		for(final String place : parents.keySet()){
			final Set<String> seen = new HashSet<>();
			final List<String> queue = new ArrayList<>(parents.get(place));
			while(!queue.isEmpty()){
				final String next = queue.remove(queue.size() - 1);
				if(!next.equals(place) && seen.add(next))
					queue.addAll(parents.getOrDefault(next, Set.of()));
			}
			ancestors.put(place, seen);
		}
		return ancestors;
	}

}
