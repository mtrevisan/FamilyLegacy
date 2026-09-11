/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable filter configuration for the map view.
 */
public record GeoFilters(
	Set<GeoMarkerType> markerTypes,
	NormalizedDate minDate,
	NormalizedDate maxDate,
	String focusEntityId
){

	public GeoFilters{
		markerTypes = (markerTypes != null && !markerTypes.isEmpty()
			? Collections.unmodifiableSet(EnumSet.copyOf(markerTypes))
			: Collections.emptySet());
		if(minDate != null && maxDate != null && minDate.compareTo(maxDate) > 0)
			throw new IllegalArgumentException("minDate must not be greater than maxDate");
		if(focusEntityId != null && focusEntityId.isBlank())
			focusEntityId = null;
	}


	public static GeoFilters all(){
		return new GeoFilters(null, null, null, null);
	}

	public boolean acceptsMarkerType(final GeoMarkerType type){
		return (markerTypes.isEmpty() || markerTypes.contains(type));
	}

	public boolean acceptsDate(final NormalizedDate date){
		if(date == null)
			return true;
		if(minDate != null && date.jdn() < minDate.jdn())
			return false;
		if(maxDate != null && date.jdn() > maxDate.jdn())
			return false;
		return true;
	}

	public boolean acceptsEvent(final GeoEventRef event){
		if(event == null)
			return false;
		if(!acceptsMarkerType(mapKindToMarker(event.kind())))
			return false;
		return acceptsDate(event.date());
	}

	public boolean hasTemporalWindow(){
		return (minDate != null || maxDate != null);
	}

	public boolean hasFocus(){
		return (focusEntityId != null);
	}

	private static GeoMarkerType mapKindToMarker(final MigrationEventKind kind){
		return switch(kind){
			case BIRTH, MARRIAGE, DEATH, BURIAL -> GeoMarkerType.EVENT;
			case RESIDENCE -> GeoMarkerType.RESIDENCE;
			case IMMIGRATION, EMIGRATION -> GeoMarkerType.MIGRATION_STOP;
			default -> GeoMarkerType.EVENT;
		};
	}

	public GeoFilters withMarkerTypes(final Set<GeoMarkerType> types){
		return new GeoFilters(types, minDate, maxDate, focusEntityId);
	}

	public GeoFilters withTemporalWindow(final NormalizedDate min, final NormalizedDate max){
		return new GeoFilters(markerTypes, min, max, focusEntityId);
	}

	public GeoFilters withFocus(final String focus){
		return new GeoFilters(markerTypes, minDate, maxDate, focus);
	}

}
