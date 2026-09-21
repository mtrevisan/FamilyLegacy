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
package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Shared extraction utilities for {@code PlaceRecord} and
 * {@code PlaceRelationshipRecord}.
 * <p>
 * The methods here are the single source of truth for how a place field
 * is read from the model. Every tool in the {@code places} package goes
 * through this class, so a change to the FLEF structure only needs to be
 * reflected here.
 */
public final class PlaceHelper{

	public static final String TAG_TYPE = "type";
	public static final String TAG_NAME = "name";
	public static final String TAG_VALUE = "value";
	public static final String TAG_SUBJECT = "subject";
	public static final String TAG_TARGET = "target";
	public static final String TAG_PLACE = "place";
	public static final String TAG_MAP = "map";
	public static final String TAG_COORDINATES = "coordinates";
	public static final String TAG_VALID_FROM = "valid_from";
	public static final String TAG_VALID_TO = "valid_to";


	private PlaceHelper(){
	}


	/** Returns every {@code PlaceRecord} in the model, in insertion order. */
	public static List<FLEFRecord> listAllPlaces(final FLEFModel model){
		return model.getRecordsByType(PlaceHandler.TYPE);
	}

	/** Returns every {@code PlaceRelationshipRecord} in the model. */
	public static List<FLEFRecord> listAllRelationships(final FLEFModel model){
		return model.getRecordsByType(PlaceRelationshipHandler.TYPE);
	}

	/**
	 * Returns the primary display name of a place. {@code PlaceRecord.name}
	 * is a list of {@code NameStructure}; the first one with a non-blank
	 * {@code value} is used. When no name is available, the place id is
	 * returned so the display stays readable.
	 */
	public static String displayName(final FLEFRecord place){
		if(place == null)
			return "";
		for(final FLEFRecord child : place.getChildren()){
			if(!TAG_NAME.equalsIgnoreCase(child.getTag()))
				continue;
			final String value = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
			if(value != null && !value.isBlank())
				return value;
			final FLEFRecord onlyChild = child.getTheOnlyChild();
			if(onlyChild != null && onlyChild.getValue() != null && !onlyChild.getValue().isBlank())
				return onlyChild.getValue();
		}
		return place.getId() != null? place.getId(): "";
	}

	/** Returns the type of a place, or {@code null} when missing. */
	public static String placeType(final FLEFRecord place){
		return (place != null? FLEFRecordHelper.getChildValue(place, TAG_TYPE): null);
	}

	/** Returns the coordinates of a place, or {@code null} when missing. */
	public static String coordinates(final FLEFRecord place){
		if(place == null)
			return null;
		final FLEFRecord map = FLEFRecordHelper.findChild(place, TAG_MAP);
		if(map == null)
			return null;
		return FLEFRecordHelper.getChildValue(map, TAG_COORDINATES);
	}

	/**
	 * Returns the value of a nested {@code DateStructure} field, or
	 * {@code null} when the field is absent. The value is used as-is,
	 * without parsing.
	 */
	public static String dateValue(final FLEFRecord record, final String tag){
		if(record == null)
			return null;
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(record, tag);
		if(dateStruct == null)
			return null;
		final String value = FLEFRecordHelper.getChildValue(dateStruct, TAG_VALUE);
		if(value != null)
			return value;
		final FLEFRecord onlyChild = dateStruct.getTheOnlyChild();
		return (onlyChild != null? onlyChild.getValue(): null);
	}

	/** Returns the referenced place id of a relationship endpoint. */
	public static String endpointPlaceId(final FLEFRecord relationship, final String tag){
		if(relationship == null)
			return null;
		return relationship.extractReferencedId(tag, TAG_PLACE);
	}


	/** A row of the place management table. */
	public record PlaceRow(String id, String name, String type,
								  String parents, String children, String coordinates){}

	/**
	 * Builds a compact row for the management table. Parents and children
	 * are rendered as a comma-separated list of display names, truncated
	 * to keep the row readable.
	 */
	public static PlaceRow toRow(final FLEFRecord place, final FLEFModel model,
		final Map<String, FLEFRecord> placesById){
		final String id = place.getId();
		final String name = displayName(place);
		final String type = placeType(place);
		final String coords = coordinates(place);

		final List<String> parentNames = new ArrayList<>();
		final List<String> childNames = new ArrayList<>();
		for(final FLEFRecord rel : listAllRelationships(model)){
			final String subjectId = endpointPlaceId(rel, TAG_SUBJECT);
			final String targetId = endpointPlaceId(rel, TAG_TARGET);
			if(id.equals(subjectId) && targetId != null){
				final FLEFRecord child = placesById.get(targetId);
				if(child != null)
					childNames.add(displayName(child));
			}
			if(id.equals(targetId) && subjectId != null){
				final FLEFRecord parent = placesById.get(subjectId);
				if(parent != null)
					parentNames.add(displayName(parent));
			}
		}

		return new PlaceRow(id, name, type,
			join(parentNames), join(childNames), coords);
	}

	private static String join(final List<String> values){
		if(values.isEmpty())
			return "";
		if(values.size() <= 3)
			return String.join(", ", values);
		return values.get(0) + ", " + values.get(1) + ", " + values.get(2)
			+ " (+" + (values.size() - 3) + ")";
	}

}
