/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;

import java.util.List;

/**
 * A sequence of geographic events that describes the movements of a single
 * subject (an individual or a group).
 * <p>
 * The stops are ordered chronologically. The record also carries the total
 * distance covered (in kilometres) and the total duration in days, both
 * computed at construction time.
 */
public record GeoMigrationRoute(
	TemporalEntityRef subject,
	List<GeoEventRef> stops,
	double totalDistanceKm,
	long totalDurationDays
){

	public GeoMigrationRoute{
		if(subject == null)
			throw new IllegalArgumentException("Subject must not be null");
		stops = (stops != null? List.copyOf(stops): List.of());
		if(totalDistanceKm < 0)
			totalDistanceKm = 0;
		if(totalDurationDays < 0)
			totalDurationDays = 0;
	}


	public boolean isEmpty(){
		return stops.isEmpty();
	}

	public int stopCount(){
		return stops.size();
	}

	public GeoEventRef firstStop(){
		return (stops.isEmpty()? null: stops.getFirst());
	}

	public GeoEventRef lastStop(){
		return (stops.isEmpty()? null: stops.getLast());
	}

	@Override
	public String toString(){
		return "Route[" + subject.displayLabel() + ", " + stops.size()
			+ " stops, " + String.format(java.util.Locale.ROOT, "%.1f", totalDistanceKm) + " km]";
	}

}
