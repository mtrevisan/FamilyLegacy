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
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * The events in chronological order, for the Timeline View.
 * <p>
 * The date of an event is not always a day: it can be a month, a year, a decade, a century, an interval, or open at one end. Each
 * event is placed by the first day its date can be (an open start is placed by the last day), then by the last one, so that an
 * event of "1850" comes before one of "1850-03-02" only if it can start earlier, and none is moved to the end for being imprecise.
 * The events with no usable date come last, in the order of the file.
 */
public final class EventTimeline{

	/** The Julian Day Number of 1 January 1970, at noon: the day number of a {@link LocalDate} is its epoch day plus this offset. */
	private static final long EPOCH_DAY_TO_JDN = 2_440_588L;


	/**
	 * The days a date can be.
	 *
	 * @param first	The first day (JDN), {@link Long#MIN_VALUE} if the date has no start.
	 * @param last	The last day (JDN), {@link Long#MAX_VALUE} if the date has no end.
	 */
	public record DayRange(long first, long last){

		/** @return	The day the event is placed by: the first one, or the last if the date has no start. */
		public long sortKey(){
			return (first != Long.MIN_VALUE? first: last);
		}

		/** @return	Whether some day of the date is in the interval. */
		public boolean overlaps(final long from, final long to){
			return (first <= to && last >= from);
		}

	}

	/**
	 * @param event	The event.
	 * @param range	The days of its date, {@code null} if it has no usable date.
	 */
	public record Entry(FLEFRecord event, DayRange range){}

	/**
	 * @param entries	The events, in chronological order.
	 * @param undatedHidden	The events with no usable date left out because a date interval was asked for.
	 */
	public record Result(List<Entry> entries, int undatedHidden){}


	private EventTimeline(){}


	/** @return	The Julian Day Number of the day (proleptic Gregorian calendar). */
	public static long toJdn(final LocalDate date){
		return date.toEpochDay() + EPOCH_DAY_TO_JDN;
	}

	/**
	 * @param event	The event.
	 * @return	The days its date can be, or {@code null} if the event has no date, or its date cannot be read, or it is open at both ends.
	 */
	public static DayRange dayRange(final FLEFRecord event){
		final FLEFRecord dateStructure = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
		if(dateStructure == null)
			return null;

		final TemporalSpan span;
		try{
			span = DateReader.extractTemporalSpan(dateStructure);
		}
		catch(final RuntimeException ignored){
			return null;
		}
		if(span == null)
			return null;

		final long first = span.minJdn();
		long last = span.maxJdn();
		if(first == Long.MIN_VALUE && last == Long.MAX_VALUE)
			return null;

		//a day, a month, a year, ...: the days it covers
		if(span.isPoint() && first != Long.MIN_VALUE)
			last = first + span.start().precisionSpanInDays() - 1;
		return new DayRange(first, last);
	}

	/**
	 * @param model	The model.
	 * @param from	The first day of the interval asked for ({@code null} for no limit).
	 * @param to	The last day of the interval asked for ({@code null} for no limit).
	 * @param participantId	The id of a participant that has to take part in the event ({@code null} for any).
	 * @param type	The type the event has to be of, without regard to case ({@code null} for any).
	 * @return	The events that have a day in the interval, take part the participant and are of the type, in chronological order.
	 * 	When an interval is asked for, the events with no usable date cannot be placed: they are not in the list, and are counted.
	 */
	public static Result build(final FLEFModel model, final LocalDate from, final LocalDate to, final String participantId,
			final String type){
		final long fromJdn = (from != null? toJdn(from): Long.MIN_VALUE);
		final long toJdn = (to != null? toJdn(to): Long.MAX_VALUE);
		final boolean intervalAsked = (from != null || to != null);
		final Map<String, List<EventHelper.Participation>> participations = EventHelper.participationsByEvent(model);

		final List<Entry> dated = new ArrayList<>();
		final List<Entry> undated = new ArrayList<>();
		int undatedHidden = 0;
		for(final FLEFRecord event : EventHelper.listAllEvents(model)){
			if(StringUtils.isNotEmpty(type) && !type.equalsIgnoreCase(EventHelper.eventType(event)))
				continue;
			if(participantId != null && !participatesIn(participations.getOrDefault(event.getId(), List.of()), participantId))
				continue;

			final DayRange range = dayRange(event);
			if(range == null){
				if(intervalAsked)
					undatedHidden ++;
				else
					undated.add(new Entry(event, null));
			}
			else if(range.overlaps(fromJdn, toJdn))
				dated.add(new Entry(event, range));
		}

		dated.sort(Comparator.comparingLong((Entry entry) -> entry.range().sortKey())
			.thenComparingLong(entry -> entry.range().last()));

		final List<Entry> entries = new ArrayList<>(dated);
		entries.addAll(undated);
		return new Result(entries, undatedHidden);
	}

	private static boolean participatesIn(final List<EventHelper.Participation> participations, final String participantId){
		for(final EventHelper.Participation participation : participations)
			if(participantId.equals(participation.participantId()))
				return true;
		return false;
	}

}
