/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.CalendarConverter;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalEntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the historical territorial hierarchy of a place.
 * <p>
 * Walks the {@code PlaceRelationshipRecord} graph backwards from the given
 * place, including only the relationships whose validity interval covers
 * the requested year. Handles cycles by tracking visited ids.
 */
public final class GeoTerritorialResolver{

	private static final String TAG_SUBJECT     = "subject";
	private static final String TAG_TARGET      = "target";
	private static final String TAG_PLACE       = "place";
	private static final String TAG_TYPE        = "type";
	private static final String TAG_VALID_FROM  = "valid_from";
	private static final String TAG_VALID_TO    = "valid_to";
	private static final String TYPE_PLACE_REL  = "place_relationship";


	private final FLEFModel model;


	public GeoTerritorialResolver(final FLEFModel model){
		this.model = model;
	}


	/**
	 * Resolves the territorial hierarchy of the given place at the given
	 * year.
	 *
	 * @param placeRef the starting place (must not be {@code null})
	 * @param year     the year of interest
	 * @return the snapshot, never {@code null}
	 */
	public GeoTerritorialSnapshot resolve(final GeoPlaceRef placeRef, final int year){
		final List<GeoTerritorialSnapshot.Level> levels = new ArrayList<>();
		final Set<String> visited = new HashSet<>();

		GeoPlaceRef current = placeRef;
		while(current != null && visited.add(current.entity()
			.id())){
			final FLEFRecord rel = findActiveParentRelationship(current.entity()
				.id(), year);
			if(rel == null)
				break;

			final String relType = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
			final String parentId = extractPlaceId(rel, TAG_TARGET);
			if(parentId == null)
				break;

			final FLEFRecord parentRecord = model.getRecordById(parentId);
			if(parentRecord == null)
				break;

			final GeoPlaceRef parent = GeoPlaceRef.of(toEntityRef(parentRecord));
			levels.add(new GeoTerritorialSnapshot.Level(relType, parent));
			current = parent;
		}

		return new GeoTerritorialSnapshot(placeRef, year, levels);
	}


	private FLEFRecord findActiveParentRelationship(final String childPlaceId, final int year){
		for(final FLEFRecord rel : model.getRecordsByType(TYPE_PLACE_REL)){
			final String subjectId = extractPlaceId(rel, TAG_SUBJECT);
			if(!childPlaceId.equals(subjectId))
				continue;
			if(!isActiveInYear(rel, year))
				continue;
			return rel;
		}
		return null;
	}

	private boolean isActiveInYear(final FLEFRecord relationship, final int year){
		final String fromRaw = extractYear(relationship, TAG_VALID_FROM);
		final String toRaw = extractYear(relationship, TAG_VALID_TO);
		final Integer from = (fromRaw != null? tryParse(fromRaw): null);
		final Integer to = (toRaw != null? tryParse(toRaw): null);
		if(from != null && year < from)
			return false;
		if(to != null && year > to)
			return false;
		return true;
	}

	private String extractYear(final FLEFRecord record, final String tag){
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(record, tag);
		if(dateStruct == null)
			return null;
		final FLEFRecord valueChild = FLEFRecordHelper.findChild(dateStruct, "value");
		if(valueChild == null)
			return null;
		final FLEFRecord fullDate = FLEFRecordHelper.findChild(valueChild, "full_date");
		if(fullDate == null){
			// Try point.bounded.spanning nesting.
			for(final String kind : new String[]{"point", "bounded", "spanning"}){
				final FLEFRecord kindChild = FLEFRecordHelper.findChild(valueChild, kind);
				if(kindChild != null){
					final FLEFRecord fd = FLEFRecordHelper.findChild(kindChild, "full_date");
					if(fd != null){
						final String raw = FLEFRecordHelper.getChildValue(fd, "value");
						return extractYearFromIso(raw);
					}
				}
			}
			return null;
		}
		final String raw = FLEFRecordHelper.getChildValue(fullDate, "value");
		return extractYearFromIso(raw);
	}

	private static String extractYearFromIso(final String iso){
		if(iso == null || iso.isBlank())
			return null;
		final String trimmed = iso.trim();
		final int dash = trimmed.indexOf('-');
		return (dash > 0? trimmed.substring(0, dash): trimmed);
	}

	private static Integer tryParse(final String s){
		try{
			return Integer.valueOf(s);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	private static String extractPlaceId(final FLEFRecord record, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(record, fieldTag);
		if(field == null)
			return null;
		final FLEFRecord placeChild = FLEFRecordHelper.findChild(field, TAG_PLACE);
		final FLEFRecord ref = (placeChild != null? placeChild: field.getTheOnlyChild());
		return (ref != null? ref.getValue(): null);
	}

	private static TemporalEntityRef toEntityRef(final FLEFRecord record){
		return new TemporalEntityRef(TemporalEntityType.PLACE, record.getId(), record,
			record.getId() != null? record.getId(): "?");
	}

	// Kept for future use: year to JDN conversion is provided by
	// CalendarConverter and is currently unused because the resolver
	// compares year integers directly.
	static long yearToJdn(final int year){
		return CalendarConverter.gregorianToJdn(year, 1, 1);
	}

}
