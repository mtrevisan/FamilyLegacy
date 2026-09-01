package io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;


/**
 * Extracts display information for a biological parents group (family) from a FLEFModel.
 */
public final class BiologicalParentsData{

	private static final String DOT = ".";

	private static final String TAG_NAME = "name";
	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
	private static final String TAG_VALUE = "value";
	private static final String TAG_POINT = "point";
	private static final String TAG_FULL_DATE = "full_date";
	private static final String TAG_DATE_VALUE_POINT_FULL_DATE = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_POINT + DOT + TAG_FULL_DATE;
	private static final String TAG_NAME_VALUE = TAG_NAME + DOT + TAG_VALUE;
	private static final String TAG_PLACE_PLACE = TAG_PLACE + DOT + TAG_PLACE;

	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BR = "<br>";

	private static final String NO_DATA = "?";


	private final String marriageTooltip;


	public static BiologicalParentsData create(final FLEFRecord marriageEvent, final FLEFModel model){
		return new BiologicalParentsData(marriageEvent, model);
	}


	private BiologicalParentsData(final FLEFRecord marriageEvent, final FLEFModel model){
		// Extract marriage date and place from marriageEventRecord
		if(marriageEvent != null){
			final String dateStr = extractFullDate(marriageEvent);
			final String place = extractPlace(marriageEvent, model);

			final StringJoiner toolTipSJ = new StringJoiner(StringUtils.EMPTY);
			if(place != null){
				toolTipSJ.add(TAG_HTML_OPEN);
				toolTipSJ.add(dateStr != null? dateStr: NO_DATA);
				toolTipSJ.add(TAG_BR);
				toolTipSJ.add(place);
				toolTipSJ.add(TAG_HTML_CLOSE);
			}
			else
				toolTipSJ.add(dateStr != null? dateStr: NO_DATA);
			marriageTooltip = toolTipSJ.toString();
		}
		else
			marriageTooltip = NO_DATA;
	}

	public String getMarriageTooltip(){
		return marriageTooltip;
	}

	private String extractFullDate(final FLEFRecord event){
		final FLEFRecord fullDate = FLEFRecordHelper.findChild(event, TAG_DATE_VALUE_POINT_FULL_DATE);
		if(fullDate == null)
			return null;

		return FLEFRecordHelper.getChildValue(fullDate, TAG_VALUE);
	}

	private String extractPlace(final FLEFRecord event, final FLEFModel model){
		final String placeId = FLEFRecordHelper.getChildValue(event, TAG_PLACE_PLACE);
		if(placeId == null)
			return null;

		// Try to get the place record via xref
		final FLEFRecord place = model.getRecordById(placeId);
		// get first name value
		for(final FLEFRecord name : FLEFRecordHelper.findChildren(place, TAG_NAME_VALUE))
			if(name != null)
				return name.getValue();
		return null;
	}

}
