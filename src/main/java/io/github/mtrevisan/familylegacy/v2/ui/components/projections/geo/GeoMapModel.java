/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import java.util.List;

/**
 * Immutable snapshot of the geographic projection.
 * <p>
 * Contains the places with coordinates, the events anchored to a place,
 * the migration routes and the overall bounds of the visible data.
 */
public record GeoMapModel(
	List<GeoPlaceRef> places,
	List<GeoEventRef> events,
	List<GeoMigrationRoute> routes,
	GeoBounds bounds,
	Integer snapshotYear
){

	public GeoMapModel{
		places = (places != null? List.copyOf(places): List.of());
		events = (events != null? List.copyOf(events): List.of());
		routes = (routes != null? List.copyOf(routes): List.of());
	}


	public static GeoMapModel empty(){
		return new GeoMapModel(List.of(), List.of(), List.of(), null, null);
	}

	public boolean isEmpty(){
		return places.isEmpty() && events.isEmpty() && routes.isEmpty();
	}

	public boolean hasBounds(){
		return (bounds != null);
	}

	@Override
	public String toString(){
		return "GeoMapModel[places=" + places.size()
			+ ", events=" + events.size()
			+ ", routes=" + routes.size()
			+ "]";
	}

}
