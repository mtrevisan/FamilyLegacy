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
package io.github.mtrevisan.familylegacy.places;

import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.CalendarReforms;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
 * Builds the complete name of a place, from the place itself up to the outermost place that contains it
 * ("Borgo, Treviso, Veneto").
 * <p>
 * The relationships are indexed once, when the resolver is created, so it is meant to be created once and used for many places.
 * A place with several containers follows the first relationship found; a cycle in a malformed file is cut at the first
 * repeated place.
 */
public final class PlaceNameResolver{

	private static final String SEPARATOR = ", ";


	private final FLEFModel model;
	private final Map<String, String> parentByChild = new HashMap<>();
	private final Map<String, List<String>> parentsByChild = new HashMap<>();


	public PlaceNameResolver(final FLEFModel model){
		this.model = model;

		//"subject is part of object": the subject is the contained place (the child), the object is the containing one (the parent)
		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(PlaceRelationshipReader.TYPE))){
			final String childId = relationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, PlaceReader.TYPE);
			final String parentId = relationship.extractReferencedId(PlaceRelationshipReader.TAG_TARGET, PlaceReader.TYPE);
			if(childId != null && parentId != null){
				parentByChild.putIfAbsent(childId, parentId);
				final List<String> parents = parentsByChild.computeIfAbsent(childId, k -> new ArrayList<>());
				if(!parents.contains(parentId))
					parents.add(parentId);
			}
		}
	}


	/**
	 * All the places that contain a place, at any distance and through any relationship (a town can be part of a province for the
	 * administration and of a diocese for the church: both are containers), so that a place can be found by the name of any of
	 * them. A cycle in a malformed file is cut at the first repeated place.
	 *
	 * @param placeId	the id of the place
	 * @return	the names (all of them, with the alternative ones) of the containers, nearest first; empty if the place is not contained
	 * 	in any other
	 */
	public List<String> containerNames(final String placeId){
		final List<String> names = new ArrayList<>(0);
		final Set<String> visited = new HashSet<>();
		visited.add(placeId);
		final Deque<String> queue = new ArrayDeque<>(parentsByChild.getOrDefault(placeId, List.of()));
		while(!queue.isEmpty()){
			final String id = queue.poll();
			if(!visited.add(id))
				continue;

			final FLEFRecord place = model.getRecordById(id);
			if(place != null)
				names.addAll(PlaceReader.extractNames(place));
			queue.addAll(parentsByChild.getOrDefault(id, List.of()));
		}
		return names;
	}


	/**
	 * The names of a place and of its containers, one level at a time: the place itself first, then the places that contain it
	 * directly, then the ones that contain these, and so on (a cycle in a malformed file is cut at the first repeated place).
	 *
	 * @param placeId	the id of the place
	 * @return	for each level, all the names (primary and alternative) of the places of that level; empty if the place is unknown
	 */
	public List<List<String>> nameLevels(final String placeId){
		final List<List<String>> levels = new ArrayList<>(0);
		final Set<String> visited = new HashSet<>();
		List<String> current = (placeId != null? List.of(placeId): List.of());
		while(!current.isEmpty()){
			final List<String> names = new ArrayList<>(0);
			final List<String> next = new ArrayList<>(0);
			for(final String id : current)
				if(visited.add(id)){
					final FLEFRecord place = model.getRecordById(id);
					if(place != null)
						names.addAll(PlaceReader.extractNames(place));
					next.addAll(parentsByChild.getOrDefault(id, List.of()));
				}
			if(!names.isEmpty())
				levels.add(names);
			current = next;
		}
		return levels;
	}

	/**
	 * The Gregorian reform that applies to a place, from the historical jurisdiction the place belongs to: a town in Tyrol
	 * changed with Tyrol, not with Austria. Every place is a jurisdiction here, because a place that is not one is recognised
	 * through the places that contain it.
	 *
	 * @param placeId	the id of the place
	 * @return	the reform, empty if neither the place nor its containers are a known jurisdiction (for example a place whose
	 *		country had several reform dates and whose province is not recorded)
	 * @see CalendarReforms#resolveByNames(List)
	 */
	public Optional<CalendarReform> calendarReform(final String placeId){
		return CalendarReforms.resolveByNames(nameLevels(placeId));
	}


	/**
	 * @param placeId	the id of the place
	 * @return	the names of the place and of its containers, separated by commas; the id itself if the place is unknown
	 */
	public String fullName(final String placeId){
		final StringBuilder sb = new StringBuilder();
		final Set<String> visited = new HashSet<>();
		String id = placeId;
		while(id != null && visited.add(id)){
			final FLEFRecord place = model.getRecordById(id);
			final String name = (place != null? PlaceReader.displayName(place): id);
			if(!sb.isEmpty())
				sb.append(SEPARATOR);
			sb.append(name);

			id = parentByChild.get(id);
		}
		return sb.toString();
	}

}
