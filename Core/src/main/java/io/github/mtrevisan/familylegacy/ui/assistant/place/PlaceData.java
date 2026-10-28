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

import java.util.List;


/**
 * What the Place Assistant knows about the places of the model: the places and how they are related.
 *
 * @param places    the places
 * @param relations the relationships between places
 */
record PlaceData(List<Place> places, List<Relation> relations){

	/**
	 * A place.
	 *
	 * @param id    the identifier
	 * @param names the names, the first one is the primary name (a name written with several parts is joined with commas)
	 * @param type  the type of the place, or {@code null} if it has none
	 * @param lat   the latitude in degrees, or {@code null} if the place has no coordinates
	 * @param lon   the longitude in degrees, or {@code null} if the place has no coordinates
	 * @param usage how many events happened in the place
	 */
	record Place(String id, List<String> names, String type, Double lat, Double lon, int usage){

		Place{
			names = List.copyOf(names);
		}


		String label(){
			return (names.isEmpty()? id: names.getFirst());
		}

		boolean hasCoordinates(){
			return (lat != null && lon != null);
		}

	}


	/**
	 * A relationship between two places.
	 *
	 * @param id        the identifier of the relationship
	 * @param subjectId the place that is part of, or that succeeds, the target
	 * @param targetId  the other place
	 * @param type      the type of the relationship
	 */
	record Relation(String id, String subjectId, String targetId, String type){}

}
