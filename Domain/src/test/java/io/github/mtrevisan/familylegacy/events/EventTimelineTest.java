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
package io.github.mtrevisan.familylegacy.events;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AgentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;


class EventTimelineTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		EventSummary.invalidate();

		model = new FLEFModel();
		model.addRecord(event("E1", EventReader.ENUM_TYPE_BIRTH, point("2 MAR 1850")));
		model.addRecord(event("E2", "baptism", point("1850")));
		model.addRecord(event("E3", "death", null));
		model.addRecord(event("E4", "marriage", point("1 JAN 1800")));
		model.addRecord(event("E5", "residence", bounded("1 JAN 1900", "31 DEC 1910")));
		model.addRecord(participation("EP1", "E1", "I1"));
		model.addRecord(participation("EP2", "E4", "I2"));
	}


	private static FLEFRecord event(final String id, final String type, final FLEFRecord date){
		final FLEFRecord event = FLEFRecord.createMainRecord(id, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type));
		if(date != null)
			event.addChild(date);
		return event;
	}

	private static FLEFRecord participation(final String id, final String eventId, final String individualId){
		return FLEFRecord.createMainRecord(id, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(AgentReader.TAG_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId));
	}

	private static FLEFRecord point(final String fullDate){
		return FLEFRecord.createChildWithTag("date")
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("point")
					.addChild(FLEFRecord.createChildWithTag("value")
						.addChild(FLEFRecord.createChildWithTagAndValue("full_date", fullDate)))
					.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian"))));
	}

	private static FLEFRecord bounded(final String from, final String to){
		return FLEFRecord.createChildWithTag("date")
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("bounded")
					.addChild(FLEFRecord.createChildWithTag("not_before")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", from)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))
					.addChild(FLEFRecord.createChildWithTag("not_after")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", to)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))));
	}

	private static List<String> ids(final EventTimeline.Result result){
		return result.entries().stream()
			.map(entry -> entry.event().getId())
			.toList();
	}


	@Test
	void theEventsAreInChronologicalOrderAndThoseWithoutADateAreLast(){
		final EventTimeline.Result result = EventTimeline.build(model, null, null, null, null);

		//1800, the year 1850 (that can start on 1 January), 2 March 1850, 1900-1910, and the one without a date
		Assertions.assertEquals(List.of("E4", "E2", "E1", "E5", "E3"), ids(result));
		Assertions.assertEquals(0, result.undatedHidden());
		Assertions.assertNull(result.entries().getLast().range());
	}

	@Test
	void aPartialDateIsPlacedByTheDaysItCovers(){
		final EventTimeline.DayRange year = EventTimeline.dayRange(model.getRecordById("E2"));
		final EventTimeline.DayRange day = EventTimeline.dayRange(model.getRecordById("E1"));

		Assertions.assertTrue(year.last() - year.first() >= 364, year.toString());
		Assertions.assertEquals(day.first(), day.last());
		Assertions.assertTrue(year.first() <= day.first() && day.last() <= year.last());
		Assertions.assertNull(EventTimeline.dayRange(model.getRecordById("E3")));
	}

	@Test
	void theIntervalKeepsTheEventsThatHaveADayInIt(){
		final EventTimeline.Result march = EventTimeline.build(model, LocalDate.of(1850, 2, 1), LocalDate.of(1850, 12, 31), null, null);

		//the year 1850 covers the interval, the event with no date cannot be placed and is counted
		Assertions.assertEquals(List.of("E2", "E1"), ids(march));
		Assertions.assertEquals(1, march.undatedHidden());

		final EventTimeline.Result inside = EventTimeline.build(model, LocalDate.of(1905, 6, 1), null, null, null);
		Assertions.assertEquals(List.of("E5"), ids(inside));

		final EventTimeline.Result before = EventTimeline.build(model, null, LocalDate.of(1799, 12, 31), null, null);
		Assertions.assertTrue(before.entries().isEmpty());
	}

	@Test
	void theEventsAreFilteredByParticipantAndByType(){
		Assertions.assertEquals(List.of("E1"), ids(EventTimeline.build(model, null, null, "I1", null)));
		Assertions.assertEquals(List.of("E4"), ids(EventTimeline.build(model, null, null, "I2", null)));
		Assertions.assertTrue(EventTimeline.build(model, null, null, "I9", null).entries().isEmpty());

		Assertions.assertEquals(List.of("E3"), ids(EventTimeline.build(model, null, null, null, "DEATH")));
		Assertions.assertEquals(List.of("E1"), ids(EventTimeline.build(model, null, null, "I1", EventReader.ENUM_TYPE_BIRTH)));
	}

	@Test
	void theDayNumberOfADateIsTheJulianOne(){
		//the J2000 epoch
		Assertions.assertEquals(2_451_545L, EventTimeline.toJdn(LocalDate.of(2000, 1, 1)));
	}

}
