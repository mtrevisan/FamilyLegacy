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
package io.github.mtrevisan.familylegacy.temporal;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.places.PlaceHierarchyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Tells how far a place is from the places of a person (see {@link HistoricalScope}), walking the place hierarchy
 * ({@code place_relationship}: a place is part of another).
 * <p>
 * A place is <i>related</i> to the person if it is one of the places of the person, one that contains it (the province of the town), or one
 * inside it (a parish of the city). A place that is not related is {@link HistoricalScope#ELSEWHERE}. The level of a related place is the one
 * its type says (a village is local, a province regional, a country national, a continent world); a place with no type, or one that is
 * not a known type, is taken as local if it is inside a place of the person, as the person's place itself if it is one of them, and
 * regional, then national, as the places that contain it are further away.
 * <p>
 * The dates of the relationships are not considered: a place that was part of another only for some time still counts as related, as the
 * timelines list the historic events by the dates of the events themselves.
 */
public final class HistoricalPlaceScope{

	private static final Set<String> LOCAL_TYPES = Set.of(PlaceReader.ENUM_TYPE_ADDRESS, PlaceReader.ENUM_TYPE_BUILDING,
		PlaceReader.ENUM_TYPE_STREET, PlaceReader.ENUM_TYPE_HAMLET, PlaceReader.ENUM_TYPE_VILLAGE, PlaceReader.ENUM_TYPE_TOWN,
		PlaceReader.ENUM_TYPE_MUNICIPALITY, PlaceReader.ENUM_TYPE_CITY, PlaceReader.ENUM_TYPE_METROPOLITAN_AREA,
		PlaceReader.ENUM_TYPE_DISTRICT, PlaceReader.ENUM_TYPE_PARISH, PlaceReader.ENUM_TYPE_CEMETERY, PlaceReader.ENUM_TYPE_PARCEL,
		PlaceReader.ENUM_TYPE_ISLAND);
	private static final Set<String> REGIONAL_TYPES = Set.of(PlaceReader.ENUM_TYPE_COUNTY, PlaceReader.ENUM_TYPE_PROVINCE,
		PlaceReader.ENUM_TYPE_DEPARTMENT, PlaceReader.ENUM_TYPE_REGION, PlaceReader.ENUM_TYPE_MACRO_REGION,
		PlaceReader.ENUM_TYPE_DIOCESE, PlaceReader.ENUM_TYPE_ARCHDIOCESE, PlaceReader.ENUM_TYPE_ARCHIPELAGO);
	private static final Set<String> NATIONAL_TYPES = Set.of(PlaceReader.ENUM_TYPE_COUNTRY, PlaceReader.ENUM_TYPE_STATE,
		PlaceReader.ENUM_TYPE_EMPIRE);


	private final PlaceHierarchyService.Hierarchy hierarchy;
	private final Set<String> personPlaces;
	/** The places of the person and the ones that contain them, with the number of steps up from the nearest place of the person. */
	private final Map<String, Integer> depths;


	/**
	 * @param model	The file.
	 * @param personPlaceIds	The identifiers of the places of the person.
	 */
	public HistoricalPlaceScope(final FLEFModel model, final Set<String> personPlaceIds){
		this(PlaceHierarchyService.load(model), personPlaceIds);
	}

	/**
	 * @param hierarchy	The place hierarchy.
	 * @param personPlaceIds	The identifiers of the places of the person.
	 */
	public HistoricalPlaceScope(final PlaceHierarchyService.Hierarchy hierarchy, final Set<String> personPlaceIds){
		this.hierarchy = hierarchy;
		personPlaces = Set.copyOf(personPlaceIds);
		depths = ancestorsOf(personPlaces);
	}


	/**
	 * @param placeId	The identifier of a place ({@code null} or blank for a historic event that has no place).
	 * @return	The scope of the place.
	 */
	public HistoricalScope scopeOf(final String placeId){
		if(StringUtils.isBlank(placeId))
			return HistoricalScope.WORLD;

		final PlaceHierarchyService.PlaceReference place = hierarchy.places()
			.get(placeId);
		if(place == null)
			return HistoricalScope.ELSEWHERE;

		//a continent is the world, whoever lived in it
		if(HistoricalScope.WORLD == byType(place.type()))
			return HistoricalScope.WORLD;

		final Integer depth = depths.get(placeId);
		//the places inside a place of the person
		final boolean inside = (depth == null && ancestorsOf(Set.of(placeId)).keySet().stream()
			.anyMatch(personPlaces::contains));
		if(depth == null && !inside)
			return HistoricalScope.ELSEWHERE;

		final HistoricalScope byType = byType(place.type());
		if(byType != null)
			return byType;

		if(inside)
			return HistoricalScope.LOCAL;
		return (depth == 0? HistoricalScope.LOCAL: (depth == 1? HistoricalScope.REGIONAL: HistoricalScope.NATIONAL));
	}

	private static HistoricalScope byType(final String type){
		if(StringUtils.isBlank(type))
			return null;

		final String normalized = type.strip().toLowerCase(Locale.ROOT);
		if(LOCAL_TYPES.contains(normalized))
			return HistoricalScope.LOCAL;
		if(REGIONAL_TYPES.contains(normalized))
			return HistoricalScope.REGIONAL;
		if(NATIONAL_TYPES.contains(normalized))
			return HistoricalScope.NATIONAL;
		if(PlaceReader.ENUM_TYPE_CONTINENT.equals(normalized))
			return HistoricalScope.WORLD;
		return null;
	}

	/** The given places and the places that contain them, up to the roots (iterative, a cycle in the hierarchy does not loop). */
	private Map<String, Integer> ancestorsOf(final Set<String> places){
		final Map<String, Integer> depth = new HashMap<>();
		final Deque<String> queue = new ArrayDeque<>();
		for(final String place : places)
			if(depth.putIfAbsent(place, 0) == null)
				queue.add(place);

		final Set<String> visited = new HashSet<>(places);
		while(!queue.isEmpty()){
			final String id = queue.poll();
			final int next = depth.get(id) + 1;
			for(final PlaceHierarchyService.PlaceRelation relation : hierarchy.parentsOf(id))
				if(visited.add(relation.parentId())){
					depth.put(relation.parentId(), next);
					queue.add(relation.parentId());
				}
		}
		return depth;
	}

}
