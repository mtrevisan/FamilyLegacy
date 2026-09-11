/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

/**
 * Kind of marker drawn on the map. Drives the icon shape and color used by
 * the marker renderer.
 */
public enum GeoMarkerType{

	/** A place from the FLEF model (PlaceRecord). */
	PLACE,

	/** A generic event anchored to a place. */
	EVENT,

	/** A residence attribute anchored to a place. */
	RESIDENCE,

	/** A stop along a migration route. */
	MIGRATION_STOP,

	/** A historic event anchored to a place. */
	HISTORIC_EVENT;

	/**
	 * Returns whether this marker is part of a migration route.
	 *
	 * @return {@code true} for {@link #MIGRATION_STOP}
	 */
	public boolean isMigrationRelated(){
		return (this == MIGRATION_STOP);
	}

}
