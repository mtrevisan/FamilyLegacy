/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

/**
 * Cartographic projection used to map (latitude, longitude) onto screen
 * pixel coordinates.
 * <p>
 * All projections are deterministic and reversible within their valid
 * domain, so that clicking on the map can be translated back into
 * geographic coordinates without ambiguity.
 */
public enum GeoProjectionType{

	/**
	 * Equirectangular (plate carrée) projection. Longitude maps linearly to
	 * X and latitude maps linearly to Y. Simple and fast, but distorts
	 * areas increasingly toward the poles. Suitable for a global overview
	 * and for regions near the equator.
	 */
	EQUIRECTANGULAR("Equirectangular"),

	/**
	 * Mercator projection. Longitude maps linearly to X, latitude maps
	 * through a logarithmic function to Y. Preserves local shapes but
	 * heavily distorts areas at high latitudes. Suitable for interactive
	 * maps of Europe and the Mediterranean.
	 */
	MERCATOR("Mercator"),

	/**
	 * Web Mercator (EPSG:3857), the de-facto standard for interactive web
	 * maps. Differs from the classical Mercator by assuming a spherical
	 * Earth, which makes it compatible with most online tile services if
	 * they are ever integrated.
	 */
	MERCATOR_WEB("Web Mercator");


	private final String displayLabel;


	GeoProjectionType(final String displayLabel){
		this.displayLabel = displayLabel;
	}


	public String getDisplayLabel(){
		return displayLabel;
	}

	/**
	 * Returns whether this projection requires a latitude clamp to avoid
	 * divergence at the poles.
	 *
	 * @return {@code true} for the Mercator variants
	 */
	public boolean requiresLatitudeClamp(){
		return (this != EQUIRECTANGULAR);
	}

	@Override
	public String toString(){
		return displayLabel;
	}

}
