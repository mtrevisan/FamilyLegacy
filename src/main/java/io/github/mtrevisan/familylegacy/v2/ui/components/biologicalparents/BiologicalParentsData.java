package io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_TYPE = "type";
	private static final String TAG_DATE_VALUE_POINT_FULL_DATE = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_POINT + DOT + TAG_FULL_DATE;
	private static final String TAG_NAME_VALUE = TAG_NAME + DOT + TAG_VALUE;
	private static final String TAG_PLACE_PLACE = TAG_PLACE + DOT + TAG_PLACE;

	private static final String EVENT_TYPE_MARRIAGE = "marriage";

	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BR = "<br>";

	private static final String NO_DATA = "?";


	private final String marriageTooltip;


	public static BiologicalParentsData create(final FLEFRecord parentsFamily,
			final FLEFRecord father, final List<FLEFRecord> fatherMarriageEvents,
			final FLEFRecord mother, final List<FLEFRecord> motherMarriageEvents,
			final FLEFModel model){
		return new BiologicalParentsData(parentsFamily,
			father, fatherMarriageEvents,
			mother, motherMarriageEvents,
			model);
	}


	private BiologicalParentsData(final FLEFRecord parentsFamily,
			final FLEFRecord father, final List<FLEFRecord> fatherMarriageEvents,
			final FLEFRecord mother, final List<FLEFRecord> motherMarriageEvents,
			final FLEFModel model){
		final Set<FLEFRecord> marriageEvents = new HashSet<>();
		if(fatherMarriageEvents != null)
			marriageEvents.addAll(fatherMarriageEvents);
		if(motherMarriageEvents != null){
			marriageEvents.addAll(motherMarriageEvents);
			marriageEvents.retainAll(motherMarriageEvents);
		}
		FLEFRecord marriageEvent = (!marriageEvents.isEmpty()? marriageEvents.iterator().next(): null);
		for(final FLEFRecord fatherMarriageEvent : fatherMarriageEvents)
			if("civil_marriage".equals(FLEFRecordHelper.getChildValue(fatherMarriageEvent, "type"))){
				marriageEvent = fatherMarriageEvent;
				break;
			}

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


//		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
//		for(final FLEFRecord eventParticipation : eventParticipations){
//			final FLEFRecord participant = FLEFRecordHelper.findChild(eventParticipation, TAG_PARTICIPANT);
//			if(participant == null)
//				continue;
//			final FLEFRecord individualRef = participant.getTheOnlyChild();
//			if(individualRef == null)
//				continue;
//
//			final String childId = individualRef.getValue();
//			if(childId == null || !childId.equals())
//				continue;
//
//			final String eventId = FLEFRecordHelper.getChildValue(eventParticipation, TAG_EVENT);
//			if(eventId == null)
//				continue;
//			final FLEFRecord event = model.getRecordById(eventId);
//			if(event != null && EventHandler.TYPE.equals(event.getTag())){
//				final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
//				if(EVENT_TYPE_MARRIAGE.equals(type))
//					childToMarriageEventMap.put(childId, event);
//
//				individualToEventMap.computeIfAbsent(individualRef.getValue(), k -> new ArrayList<>()).add(event);
//			}
//		}
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
