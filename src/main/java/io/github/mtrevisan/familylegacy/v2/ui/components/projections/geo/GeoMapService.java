package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.DateNormalizer;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the construction of a {@link GeoMapModel} from a FLEF
 * model.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>enumerate places with a coordinate;</li>
 *   <li>extract events anchored to a place and filter them;</li>
 *   <li>build migration routes for the focus entity (when specified);</li>
 *   <li>resolve territorial snapshots on demand;</li>
 *   <li>compute the overall bounds of the visible data.</li>
 * </ul>
 * The service caches the last built model; {@link #invalidate()} clears
 * the cache when the underlying model changes.
 */
public final class GeoMapService{

	private final FLEFModel model;
	private final DateNormalizer normalizer;
	private final GeoPlaceResolver placeResolver;
	private final GeoMigrationExtractor migrationExtractor;
	private final GeoTerritorialResolver territorialResolver;

	private GeoMapModel cachedModel;


	public GeoMapService(final FLEFModel model){
		this.model = model;
		this.normalizer = new DateNormalizer();
		this.placeResolver = new GeoPlaceResolver(model);
		this.migrationExtractor = new GeoMigrationExtractor(model, normalizer, placeResolver);
		this.territorialResolver = new GeoTerritorialResolver(model);
	}


	/* ======================================================================
	 *                          Main entry point
	 * ====================================================================== */

	/**
	 * Builds a map model with the given filters.
	 *
	 * @param filters the filters (must not be {@code null})
	 * @return the map model
	 */
	public GeoMapModel build(final GeoFilters filters){
		if(filters == null)
			throw new IllegalArgumentException("Filters must not be null");

		final List<GeoPlaceRef> places = collectPlaces();
		final List<GeoEventRef> events = collectEvents(filters);
		final List<GeoMigrationRoute> routes = collectRoutes(filters);

		final GeoBounds bounds = computeBounds(places, events, routes, filters);
		cachedModel = new GeoMapModel(places, events, routes, bounds, null);
		return cachedModel;
	}

	/**
	 * Invalidates the cached map model.
	 */
	public void invalidate(){
		cachedModel = null;
	}

	/**
	 * Returns the last built model, or {@code null}.
	 */
	public GeoMapModel getCachedModel(){
		return cachedModel;
	}

	/**
	 * Resolves a territorial snapshot for the given place and year.
	 */
	public GeoTerritorialSnapshot resolveTerritory(final GeoPlaceRef place, final int year){
		return territorialResolver.resolve(place, year);
	}


	/* ======================================================================
	 *                          Collection
	 * ====================================================================== */

	private List<GeoPlaceRef> collectPlaces(){
		final List<GeoPlaceRef> result = new ArrayList<>();
		for(final FLEFRecord record : model.getRecordsByType(PlaceHandler.TYPE)){
			final GeoPlaceRef ref = placeResolver.resolve(record, null);
			if(ref.hasCoordinate())
				result.add(ref);
		}
		return result;
	}

	private List<GeoEventRef> collectEvents(final GeoFilters filters){
		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		final List<GeoEventRef> result = new ArrayList<>();
		final java.util.Set<String> seenEvents = new java.util.HashSet<>();

		for(final FLEFRecord participation : participations){
			final String eventId = io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper
				.getChildValue(participation, "event");
			if(eventId == null || !seenEvents.add(eventId))
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null)
				continue;

			final GeoEventRef ref = buildEventRef(event);
			if(ref == null)
				continue;
			if(!filters.acceptsEvent(ref))
				continue;
			result.add(ref);
		}
		return result;
	}

	private GeoEventRef buildEventRef(final FLEFRecord event){
		final GeoPlaceRef place = placeResolver.resolveFromEvent(event);
		if(place == null || !place.hasCoordinate())
			return null;

		final FLEFRecord dateStruct = io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper
			.findChild(event, "date");
		final var span = normalizer.normalize(dateStruct);
		final var date = (span != null? span.start(): null);

		final String eventType = io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper
			.getChildValue(event, "type");
		final MigrationEventKind kind = MigrationEventKind.fromEventType(eventType);
		return new GeoEventRef(event, place, date, kind, List.of());
	}

	private List<GeoMigrationRoute> collectRoutes(final GeoFilters filters){
		if(!filters.hasFocus())
			return List.of();

		final FLEFRecord focusRecord = model.getRecordById(filters.focusEntityId());
		if(focusRecord == null)
			return List.of();

		final String tag = focusRecord.getTag();
		final TemporalEntityType type = ("group".equalsIgnoreCase(tag)
			? TemporalEntityType.GROUP
			: TemporalEntityType.INDIVIDUAL);
		final TemporalEntityRef subject = new TemporalEntityRef(type, focusRecord.getId(), focusRecord,
			focusRecord.getId() != null? focusRecord.getId(): "?");

		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		final GeoMigrationRoute route = migrationExtractor.extract(subject, participations);
		return (route.isEmpty()? List.of(): List.of(route));
	}

	private static GeoBounds computeBounds(final List<GeoPlaceRef> places, final List<GeoEventRef> events,
		final List<GeoMigrationRoute> routes, final GeoFilters filters){
		final List<GeoCoordinate> coords = new ArrayList<>();
		if(filters.hasFocus() && !routes.isEmpty()){
			for(final GeoMigrationRoute route : routes)
				for(final GeoEventRef stop : route.stops())
					if(stop.hasPlace())
						coords.add(stop.placeRef()
							.coordinate());
		}
		else{
			for(final GeoPlaceRef place : places)
				if(place.hasCoordinate())
					coords.add(place.coordinate());
			for(final GeoEventRef event : events)
				if(event.hasPlace())
					coords.add(event.placeRef()
						.coordinate());
		}
		return GeoBounds.enclosing(coords, 0.5);
	}

	// Reserved for future expansion.
	static IndividualHandler individualHandler(){
		return IndividualHandler.getInstance();
	}

}
