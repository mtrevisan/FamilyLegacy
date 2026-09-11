/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.DateNormalizer;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a {@link GeoMigrationRoute} for a single subject by collecting
 * all geographic events in which the subject participated, ordering them
 * chronologically, and computing the total distance and duration.
 */
public final class GeoMigrationExtractor{

	private static final String TAG_TYPE        = "type";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT       = "event";
	private static final String TAG_DATE        = "date";
	private static final String TAG_PLACE       = "place";


	private final FLEFModel model;
	private final DateNormalizer normalizer;
	private final GeoPlaceResolver placeResolver;


	public GeoMigrationExtractor(final FLEFModel model, final DateNormalizer normalizer,
		final GeoPlaceResolver placeResolver){
		this.model = model;
		this.normalizer = normalizer;
		this.placeResolver = placeResolver;
	}


	/**
	 * Extracts the migration route of the given subject.
	 *
	 * @param subject the subject entity (must not be {@code null})
	 * @param allParticipations all {@code EventParticipationRecord}
	 *        instances in the model; passed by the caller to avoid
	 *        repeated scans
	 * @return the route, never {@code null}
	 */
	public GeoMigrationRoute extract(final TemporalEntityRef subject, final List<FLEFRecord> allParticipations){
		final List<GeoEventRef> stops = new ArrayList<>();

		for(final FLEFRecord participation : allParticipations){
			if(!isParticipantOf(participation, subject))
				continue;

			final GeoEventRef stop = buildStop(participation);
			if(stop != null)
				stops.add(stop);
		}

		// Sort chronologically; undated stops come last, in insertion order.
		stops.sort(Comparator
			.comparing((GeoEventRef e) -> (e.hasDate()? 0: 1))
			.thenComparing(e -> (e.hasDate()? e.date(): null),
				Comparator.nullsLast(Comparator.naturalOrder())));

		final double totalKm = GeoDistanceCalculator.totalDistanceKm(
			stops.stream()
				.map(s -> (s.hasPlace()? s.placeRef()
					.coordinate(): null))
				.toList());

		final long totalDays = computeTotalDurationDays(stops);

		return new GeoMigrationRoute(subject, stops, totalKm, totalDays);
	}


	private GeoEventRef buildStop(final FLEFRecord participation){
		final String eventId = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
		if(eventId == null)
			return null;

		final FLEFRecord event = model.getRecordById(eventId);
		if(event == null)
			return null;

		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(event, TAG_DATE);
		final TemporalSpan span = normalizer.normalize(dateStruct);
		final NormalizedDate date = (span != null? span.start(): null);

		final GeoPlaceRef place = placeResolver.resolveFromEvent(event);
		if(place == null)
			return null;

		final String eventType = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
		final MigrationEventKind kind = MigrationEventKind.fromEventType(eventType);
		return new GeoEventRef(event, place, date, kind, List.of());
	}

	private static boolean isParticipantOf(final FLEFRecord participation, final TemporalEntityRef subject){
		final FLEFRecord participantField = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
		if(participantField == null)
			return false;
		final FLEFRecord ref = participantField.getTheOnlyChild();
		if(ref == null || ref.getValue() == null)
			return false;
		return subject.id()
			.equals(ref.getValue());
	}

	private static long computeTotalDurationDays(final List<GeoEventRef> stops){
		NormalizedDate first = null;
		NormalizedDate last = null;
		for(final GeoEventRef stop : stops){
			if(!stop.hasDate())
				continue;
			if(first == null)
				first = stop.date();
			last = stop.date();
		}
		if(first == null || last == null)
			return 0;
		return Math.max(0, last.jdn() - first.jdn());
	}

}
