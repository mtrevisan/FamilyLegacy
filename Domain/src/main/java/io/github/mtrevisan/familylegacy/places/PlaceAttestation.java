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
package io.github.mtrevisan.familylegacy.places;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


/**
 * The interval in which a place surely existed, derived from the sources (nothing of it is stored in the file, so it never goes stale).
 * <p>
 * A source that has a place and a date proves that the place existed at that date; so does a source with a date that cites a record at
 * the place (an event, an attribute, a repository, ...), or the place itself, or a relationship of the place. For each place the earliest
 * and the latest dates of its sources are kept. The same dates hold for the places that contain the place (the town is in the province,
 * the province is in the country, ...), as the place could not exist without them.
 * <p>
 * Sources that are not visible (privacy) are not counted.
 * <p>
 * The dates are given as {@code date} structures with a {@code point} value, to be formatted as any other date.
 */
public final class PlaceAttestation{

	/** The place of a record (a PlaceCitation named {@code place}), and the sources that cite it. */
	private static final String PATH_PLACE = FLEFRecordHelper.composePath("place", PlaceCitationReader.TAG_PLACE);
	private static final String PATH_PLACE_SOURCE = FLEFRecordHelper.composePath("place", PlaceCitationReader.TAG_SOURCE);


	/**
	 * The dates between which a place is known to have existed.
	 *
	 * @param holder	A record with the {@code valid_from} and the {@code valid_to} structures (the same one if the date is only one).
	 * @param sameDate	Whether the earliest and the latest dates are the same.
	 */
	public record Interval(FLEFRecord holder, boolean sameDate){

		public static final String TAG_FROM = DateReader.TAG_VALID_FROM;
		public static final String TAG_TO = DateReader.TAG_VALID_TO;

	}


	/** A date on which a place is known to have existed. */
	private record Attestation(FLEFRecord singleDate, long jdn){}


	private static final class Range{

		private Attestation first;
		private Attestation last;


		private void add(final Attestation earliest, final Attestation latest){
			if(earliest != null && (first == null || earliest.jdn() < first.jdn()))
				first = earliest;
			if(latest != null && (last == null || latest.jdn() > last.jdn()))
				last = latest;
		}
	}


	private final Map<String, Interval> intervals = new HashMap<>();


	public PlaceAttestation(final FLEFModel model, final Predicate<FLEFRecord> visible){
		final Map<String, List<String>> containers = containers(model);
		final Map<String, Range> ranges = new HashMap<>();
		//every record that is at a place and is cited by a source with a date (events, attributes, repositories, cultural norms, historic
		//events, places, place relationships), and every source that has a place and a date of its own
		for(final FLEFRecord record : model.getRecords()){
			if(!visible.test(record))
				continue;

			final List<String> placeIds = placesOf(record);
			if(placeIds.isEmpty())
				continue;

			final List<FLEFRecord> sources = new ArrayList<>();
			if(SourceReader.isOfType(record))
				sources.add(record);
			final List<FLEFRecord> citations = new ArrayList<>(FLEFRecordHelper.findChildren(record, PlaceReader.TAG_SOURCE));
			citations.addAll(FLEFRecordHelper.findChildren(record, PATH_PLACE_SOURCE));
			for(final FLEFRecord citation : citations){
				final FLEFRecord source = model.getRecordById(FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_SOURCE));
				if(source != null)
					sources.add(source);
			}
			for(final String placeId : placeIds)
				for(final FLEFRecord source : sources)
					attest(ranges, containers, placeId, source, visible);
		}

		for(final Map.Entry<String, Range> entry : ranges.entrySet()){
			final Range range = entry.getValue();
			final Attestation first = (range.first != null? range.first: range.last);
			final Attestation last = (range.last != null? range.last: range.first);
			if(first == null)
				continue;

			final FLEFRecord holder = FLEFRecord.createChildWithTag("attestation")
				.addChild(dateStructure(DateReader.TAG_VALID_FROM, first))
				.addChild(dateStructure(DateReader.TAG_VALID_TO, last));
			intervals.put(entry.getKey(), new Interval(holder, first.jdn() == last.jdn()));
		}
	}


	/** The places a record is about: the place itself, the two places of a relationship, or the place that the record is at. */
	private static List<String> placesOf(final FLEFRecord record){
		if(PlaceReader.isOfType(record))
			return List.of(record.getId());
		if(PlaceRelationshipReader.isOfType(record)){
			final List<String> ids = new ArrayList<>(2);
			for(final String path : new String[]{PlaceRelationshipReader.TAG_SUBJECT_PLACE, PlaceRelationshipReader.TAG_TARGET_PLACE}){
				final String id = FLEFRecordHelper.getChildValue(record, path);
				if(StringUtils.isNotBlank(id))
					ids.add(id);
			}
			return ids;
		}

		final String id = FLEFRecordHelper.getChildValue(record, PATH_PLACE);
		return (StringUtils.isNotBlank(id)? List.of(id): List.of());
	}

	/** Counts the date of a source as a date on which the place, and the places that contain it, existed. */
	private static void attest(final Map<String, Range> ranges, final Map<String, List<String>> containers, final String placeId,
			final FLEFRecord source, final Predicate<FLEFRecord> visible){
		if(StringUtils.isBlank(placeId) || !visible.test(source))
			return;

		final FLEFRecord date = FLEFRecordHelper.findChild(source, SourceReader.TAG_DATE);
		final Attestation[] attestations = (date != null? attestations(date): null);
		if(attestations == null)
			return;

		final Set<String> visited = new HashSet<>();
		final Deque<String> queue = new ArrayDeque<>();
		queue.add(placeId);
		while(!queue.isEmpty()){
			final String id = queue.poll();
			if(!visited.add(id))
				continue;

			ranges.computeIfAbsent(id, k -> new Range())
				.add(attestations[0], attestations[1]);
			queue.addAll(containers.getOrDefault(id, List.of()));
		}
	}

	/** @return	The interval in which the place surely existed, or {@code null} if no source with a date talks about it or its contents. */
	public Interval of(final String placeId){
		return (placeId != null? intervals.get(placeId): null);
	}


	/** The places that contain each place (by place id). */
	private static Map<String, List<String>> containers(final FLEFModel model){
		final Map<String, List<String>> containers = new HashMap<>();
		for(final FLEFRecord relationship : model.getRecordsByType(PlaceRelationshipReader.TYPE)){
			final String type = PlaceRelationshipReader.extractType(relationship);
			if(type == null || !type.endsWith("_part_of"))
				continue;

			final String subject = FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_SUBJECT_PLACE);
			final String target = FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_TARGET_PLACE);
			if(StringUtils.isNotBlank(subject) && StringUtils.isNotBlank(target))
				containers.computeIfAbsent(subject, k -> new ArrayList<>())
					.add(target);
		}
		return containers;
	}

	/**
	 * The dates by which the place surely existed (the earliest one) and on which it still existed (the latest one), given by the date
	 * of a source.
	 * <p>
	 * A point is both; a period gives its bounds; for an uncertain date ({@code BET x AND y}) the place existed by the end of the
	 * interval and still at its start; an open bound says nothing.
	 *
	 * @return	The two attestations (either may be {@code null}), or {@code null} if the date is not understood.
	 */
	private static Attestation[] attestations(final FLEFRecord date){
		final FLEFRecord value = FLEFRecordHelper.findChild(date, DateReader.TAG_VALUE);
		if(value == null)
			return null;

		final FLEFRecord point = FLEFRecordHelper.findChild(value, DateReader.TAG_POINT);
		if(point != null){
			final Attestation attestation = attestation(point);
			return (attestation != null? new Attestation[]{attestation, attestation}: null);
		}

		final FLEFRecord bounded = FLEFRecordHelper.findChild(value, DateReader.TAG_BOUNDED);
		if(bounded != null){
			final Attestation by = attestation(FLEFRecordHelper.findChild(bounded, DateReader.TAG_NOT_AFTER));
			final Attestation at = attestation(FLEFRecordHelper.findChild(bounded, DateReader.TAG_NOT_BEFORE));
			return (by != null || at != null? new Attestation[]{by, at}: null);
		}

		final FLEFRecord spanning = FLEFRecordHelper.findChild(value, DateReader.TAG_SPANNING);
		if(spanning != null){
			final Attestation from = attestation(FLEFRecordHelper.findChild(spanning, DateReader.TAG_FROM));
			final Attestation to = attestation(FLEFRecordHelper.findChild(spanning, DateReader.TAG_TO));
			final Attestation by = (from != null? from: to);
			final Attestation at = (to != null? to: from);
			return (by != null? new Attestation[]{by, at}: null);
		}
		return null;
	}

	private static Attestation attestation(final FLEFRecord singleDate){
		if(singleDate == null)
			return null;

		final TemporalSpan span = DateReader.extractTemporalSpan(dateStructure(DateReader.TAG_DATE, new Attestation(singleDate, 0)));
		return (span != null? new Attestation(singleDate, span.start().jdn()): null);
	}

	/** The structure {@code tag: value: point: <date>}. */
	private static FLEFRecord dateStructure(final String tag, final Attestation attestation){
		final FLEFRecord point = FLEFRecord.createChildWithTag(DateReader.TAG_POINT);
		for(final FLEFRecord child : attestation.singleDate().getChildren()){
			final FLEFRecord copy = FLEFRecord.createEmpty();
			child.deepCopyTo(copy);
			point.addChild(copy);
		}
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
				.addChild(point));
	}

}
