/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

/**
 * Precision of a geographic coordinate, mirroring the three forms allowed
 * by ISO 6709. Determines how many decimal places are shown in tooltips and
 * how large the marker is drawn.
 */
public enum GeoCoordinatePrecision{

	/** Decimal degrees (e.g. {@code +40.6892-074.0445/}). */
	DEGREES(4),

	/** Degrees and minutes (e.g. {@code +4041.352-07402.670/}). */
	MINUTES(2),

	/** Degrees, minutes and seconds (e.g. {@code +404121.1-0740240.2/}). */
	SECONDS(0);


	private final int displayDecimals;


	GeoCoordinatePrecision(final int displayDecimals){
		this.displayDecimals = displayDecimals;
	}


	/**
	 * Returns the number of decimal places to show when formatting a
	 * coordinate of this precision.
	 *
	 * @return the decimal count
	 */
	public int getDisplayDecimals(){
		return displayDecimals;
	}

}
