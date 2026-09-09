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
package io.github.mtrevisan.familylegacy.v2.ui.components.partners;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;


/**
 * Extracts display information for a biological parents group (family) from a FLEFModel.
 */
public final class PartnersData{

	private static final String DOT = ".";

	private static final String TAG_DATE = "date";
	private static final String TAG_VALUE = "value";
	private static final String TAG_POINT = "point";
	private static final String TAG_FULL_DATE = "full_date";
	private static final String TAG_DATE_VALUE_POINT_FULL_DATE = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_POINT + DOT + TAG_FULL_DATE;

	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BR = "<br>";

	private static final String NO_DATA = "?";


	private final String marriageTooltip;


	public static PartnersData create(
			final FLEFRecord father, final List<FLEFRecord> fatherEvents,
			final FLEFRecord mother, final List<FLEFRecord> motherEvents,
			final FLEFModel model){
		return new PartnersData(
			father, fatherEvents,
			mother, motherEvents,
			model);
	}


	private PartnersData(
			final FLEFRecord father, List<FLEFRecord> fatherEvents,
			final FLEFRecord mother, List<FLEFRecord> motherEvents,
			final FLEFModel model){
		// extract common events
		final Set<String> motherEventIds = motherEvents.stream()
			.map(FLEFRecord::getId)
			.collect(Collectors.toSet());
		final List<FLEFRecord> parentsEvents = fatherEvents.stream()
			.filter(event -> motherEventIds.contains(event.getId()))
			.toList();
		// retain only parent-specific events
		final Set<String> commonEventIds = parentsEvents.stream()
			.map(FLEFRecord::getId)
			.collect(Collectors.toSet());
		fatherEvents = fatherEvents.stream()
			.filter(event -> !commonEventIds.contains(event.getId()))
			.toList();
		motherEvents = motherEvents.stream()
			.filter(event -> !commonEventIds.contains(event.getId()))
			.toList();

//---
		final Set<FLEFRecord> marriageEvents = new HashSet<>(fatherEvents);
		marriageEvents.retainAll(motherEvents);
		FLEFRecord marriageEvent = (!marriageEvents.isEmpty()? marriageEvents.iterator().next(): null);
		for(final FLEFRecord fatherMarriageEvent : fatherEvents)
			if("civil_marriage".equals(FLEFRecordHelper.getChildValue(fatherMarriageEvent, "type"))){
				marriageEvent = fatherMarriageEvent;

				break;
			}

		// Extract marriage date and place from marriageEventRecord
		if(marriageEvent != null){
			final String dateStr = extractFullDate(marriageEvent);
			final String place = IndividualData.extractPlace(marriageEvent, model);

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

}
