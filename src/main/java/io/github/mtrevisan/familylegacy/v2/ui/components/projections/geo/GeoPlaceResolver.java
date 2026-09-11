/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;


/**
 * Resolves a {@link GeoPlaceRef} from a FLEF record that carries a place
 * citation.
 * <p>
 * Extracts the coordinate (if any) from the referenced {@code PlaceRecord},
 * reads the display name via {@link PlaceHandler}, and preserves the
 * {@code original_text} field as the historical name.
 */
public final class GeoPlaceResolver{

	private static final String TAG_PLACE = "place";
	private static final String TAG_ORIGINAL_TEXT = "original_text";
	private static final String TAG_MAP = "map";
	private static final String TAG_COORDINATES = "coordinates";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";


	private final FLEFModel model;


	public GeoPlaceResolver(final FLEFModel model){
		this.model = model;
	}


	/**
	 * Resolves a place reference from a record carrying a
	 * {@code place: PlaceCitation} field.
	 *
	 * @param record the record (must not be {@code null})
	 * @return the resolved reference, or {@code null} if the record has no
	 *         place citation
	 */
	public GeoPlaceRef resolveFromEvent(final FLEFRecord record){
		final FLEFRecord citation = FLEFRecordHelper.findChild(record, TAG_PLACE);
		if(citation == null)
			return null;
		final FLEFRecord ref = FLEFRecordHelper.findChild(citation, TAG_PLACE);
		final String placeId = (ref != null? ref.getValue(): null);
		if(placeId == null)
			return null;

		final FLEFRecord placeRecord = model.getRecordById(placeId);
		if(placeRecord == null)
			return null;

		final String historical = FLEFRecordHelper.getChildValue(citation, TAG_ORIGINAL_TEXT);
		return resolve(placeRecord, historical);
	}

	/**
	 * Resolves a place reference from a {@code PlaceRecord}.
	 *
	 * @param placeRecord the place record
	 * @param historicalName the source text, or {@code null}
	 * @return the reference, never {@code null}
	 */
	public GeoPlaceRef resolve(final FLEFRecord placeRecord, final String historicalName){
		final TemporalEntityRef entity = new TemporalEntityRef(
			TemporalEntityType.PLACE, placeRecord.getId(), placeRecord,
			computeLabel(placeRecord));
		final GeoCoordinate coordinate = extractCoordinate(placeRecord);
		return new GeoPlaceRef(entity, coordinate, computeLabel(placeRecord),
			historicalName != null? historicalName: StringUtils.EMPTY, null);
	}


	private GeoCoordinate extractCoordinate(final FLEFRecord placeRecord){
		final FLEFRecord map = FLEFRecordHelper.findChild(placeRecord, TAG_MAP);
		if(map == null)
			return null;
		final String raw = FLEFRecordHelper.getChildValue(map, TAG_COORDINATES);
		return Iso6709Parser.parse(raw);
	}

	private static String computeLabel(final FLEFRecord placeRecord){
		final FLEFRecord nameStruct = FLEFRecordHelper.findChild(placeRecord, TAG_NAME);
		if(nameStruct != null){
			final String value = FLEFRecordHelper.getChildValue(nameStruct, TAG_VALUE);
			if(value != null && !value.isBlank())
				return value;
		}
		return placeRecord.getId() != null? placeRecord.getId(): "?";
	}

	// Reserved for future use.
	static PlaceHandler handler(){
		return PlaceHandler.getInstance();
	}

}
