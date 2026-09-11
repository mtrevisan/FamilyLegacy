/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import java.util.List;

/**
 * The territorial hierarchy of a place at a specific year.
 * <p>
 * The hierarchy is a list of levels, from the place itself up to the
 * outermost administrative ancestor valid at the requested year. Each
 * level carries the FLEF {@code PlaceRelationshipRecord.type} that links
 * it to the level below.
 */
public record GeoTerritorialSnapshot(
	GeoPlaceRef place,
	int year,
	List<Level> hierarchy
){

	/**
	 * A single level of the hierarchy.
	 *
	 * @param relationshipType the FLEF {@code place_relationship.type} that
	 *                         links this level to the level below
	 * @param place            the place at this level
	 */
	public record Level(String relationshipType, GeoPlaceRef place){}


	public GeoTerritorialSnapshot{
		if(place == null)
			throw new IllegalArgumentException("Place must not be null");
		hierarchy = (hierarchy != null? List.copyOf(hierarchy): List.of());
	}


	public boolean isEmpty(){
		return hierarchy.isEmpty();
	}

	public int depth(){
		return hierarchy.size();
	}

	@Override
	public String toString(){
		return place.displayName() + " (" + year + ", " + hierarchy.size() + " levels)";
	}

}
