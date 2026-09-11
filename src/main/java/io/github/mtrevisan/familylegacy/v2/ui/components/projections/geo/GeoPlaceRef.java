/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * A reference to a FLEF place together with its optional geographic
 * coordinate and its display names.
 * <p>
 * The {@code parentRef} field points to the enclosing administrative place
 * (if any), as resolved by the {@code GeoTerritorialResolver}. It is used
 * to inherit coordinates when a place has no explicit coordinate.
 * <p>
 * Equality is based on the underlying entity only.
 */
public record GeoPlaceRef(
	TemporalEntityRef entity,
	GeoCoordinate coordinate,
	String displayName,
	String historicalName,
	GeoPlaceRef parentRef
){

	public GeoPlaceRef{
		if(entity == null)
			throw new IllegalArgumentException("Entity must not be null");
		if(displayName == null)
			displayName = entity.displayLabel();
		if(historicalName == null)
			historicalName = StringUtils.EMPTY;
	}


	/**
	 * Creates a reference with no coordinate and no parent.
	 */
	public static GeoPlaceRef of(final TemporalEntityRef entity){
		return new GeoPlaceRef(entity, null, entity.displayLabel(), StringUtils.EMPTY, null);
	}

	public boolean hasCoordinate(){
		return (coordinate != null);
	}

	public boolean hasParent(){
		return (parentRef != null);
	}

	public boolean hasHistoricalName(){
		return !historicalName.isEmpty();
	}


	@Override
	public boolean equals(final Object other){
		if(this == other)
			return true;
		if(!(other instanceof GeoPlaceRef ref))
			return false;
		return entity.equals(ref.entity);
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(entity);
	}

	@Override
	public String toString(){
		return displayName + (hasCoordinate()? StringUtils.SPACE + coordinate.format(): StringUtils.EMPTY);
	}

}
