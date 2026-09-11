/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * A geographic event, i.e. a FLEF {@code EventRecord} with a resolved
 * place reference.
 * <p>
 * The record is immutable. The {@code kind} classifies the event into a
 * {@link MigrationEventKind} for route ordering and rendering.
 */
public record GeoEventRef(
	FLEFRecord eventRecord,
	GeoPlaceRef placeRef,
	NormalizedDate date,
	MigrationEventKind kind,
	List<TemporalEntityRef> participants
){

	public GeoEventRef{
		if(eventRecord == null)
			throw new IllegalArgumentException("Event record must not be null");
		if(placeRef == null)
			throw new IllegalArgumentException("Place reference must not be null");
		if(kind == null)
			kind = MigrationEventKind.OTHER;
		participants = (participants != null? List.copyOf(participants): List.of());
	}


	public boolean hasDate(){
		return (date != null);
	}

	public boolean hasPlace(){
		return placeRef.hasCoordinate();
	}

	public String eventId(){
		return eventRecord.getId();
	}

	@Override
	public String toString(){
		return kind + " @ " + placeRef.displayName()
			+ (hasDate()? " (" + date + ")": StringUtils.EMPTY);
	}

}
