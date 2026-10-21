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
package io.github.mtrevisan.familylegacy.ui.tools.places;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;


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

	private PlaceHelper(){}


	/**
	 * Returns the primary display name of a place. {@code PlaceRecord.name}
	 * is a list of {@code NameStructure}; the first one with a non-blank
	 * {@code value} is used. When no name is available, the place id is
	 * returned so the display stays readable.
	 */
	public static String displayName(final FLEFRecord place){
		if(place == null)
			return StringUtils.EMPTY;

		for(final FLEFRecord child : place.getChildren()){
			if(!PlaceReader.TAG_NAME.equalsIgnoreCase(child.getTag()))
				continue;

			final String value = PlaceReader.extractPrimaryName(child);
			if(value != null && !value.isBlank())
				return value;

			final FLEFRecord onlyChild = child.getTheOnlyChild();
			if(onlyChild != null && onlyChild.getValue() != null && !onlyChild.getValue().isBlank())
				return onlyChild.getValue();
		}
		return place.getId() != null? place.getId(): StringUtils.EMPTY;
	}

	/** Returns the coordinates of a place, or {@code null} when missing. */
	public static String coordinates(final FLEFRecord place){
		if(place == null)
			return null;

		final FLEFRecord location = FLEFRecordHelper.findChild(place, PlaceReader.TAG_LOCATION);
		if(location == null)
			return null;

		return FLEFRecordHelper.getChildValue(location, PlaceReader.TAG_COORDINATES);
	}

	/**
	 * Deletes the given place and all the relationships that
	 * involve it, in a single operation. The relationships are removed
	 * first, so the model is never left with a dangling reference.
	 *
	 * @param placeId the place to delete
	 * @param model        the model
	 */
	public static void deletePlace(final String placeId, final FLEFModel model){
		if(placeId == null)
			return;

		final Map<Class<? extends RecordTypeHandler<?>>, Set<FLEFRecord>> relationshipIds
			= model.getReferencingRecords(placeId);
		for(final Map.Entry<Class<? extends RecordTypeHandler<?>>, Set<FLEFRecord>> entry : relationshipIds.entrySet())
			for(final FLEFRecord record : entry.getValue())
				model.removeRecord(record.getId());

		model.removeRecord(placeId);
	}

}
