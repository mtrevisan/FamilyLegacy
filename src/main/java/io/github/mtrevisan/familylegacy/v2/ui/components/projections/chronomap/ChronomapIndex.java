/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.DateNormalizer;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Index of geo-referenced temporal anchors, plus the global temporal
 * range of the whole FLEF file.
 * <p>
 * An anchor is a position valid over a time interval. Events are anchors
 * of zero duration; attributes with {@code valid_from} / {@code valid_to}
 * are anchors with a real duration. The chronomap overlay uses anchors
 * to interpolate the position of an individual or group at any point in
 * time.
 */
public final class ChronomapIndex{

	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_DATE = "date";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_GROUP = "group";

	/** Tags that may carry a DateStructure anywhere in a record. */
	private static final String[] DATE_TAGS = {
		"date", "valid_from", "valid_to", "closed_date", "due_date"
	};


	private final FLEFModel model;
	private final DateNormalizer dateNormalizer = new DateNormalizer();
	private final PlaceCoordinateResolver placeResolver;

	/** Not final: rebuilt after a background geocoding pass. */
	private Map<String, List<GeoAnchor>> anchorsByOwner;


	public ChronomapIndex(final FLEFModel model, final PlaceCoordinateResolver placeResolver){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.model = model;
		this.placeResolver = Objects.requireNonNull(placeResolver, "PlaceCoordinateResolver must not be null");
		this.anchorsByOwner = build();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public List<GeoAnchor> anchorsOf(final String ownerId){
		return anchorsByOwner.getOrDefault(ownerId, List.of());
	}

	public Set<String> owners(){
		return anchorsByOwner.keySet();
	}

	/**
	 * Returns the distinct event types carried by the anchors, sorted
	 * alphabetically. Used by the UI to populate the event type filter.
	 *
	 * @return the distinct event types; never {@code null}
	 */
	public java.util.Set<String> eventTypes(){
		final java.util.Set<String> types = new java.util.TreeSet<>();
		for(final List<GeoAnchor> list : anchorsByOwner.values())
			for(final GeoAnchor a : list)
				if(a.kind().startsWith("event:"))
					types.add(a.kind().substring("event:".length()));
		return types;
	}

	/**
	 * Rebuilds the geo-event index from the model. Call this after the
	 * place resolver has geocoded new places, so the events of those
	 * places appear in the index.
	 */
	public void rebuild(){
		anchorsByOwner = build();
	}

	/**
	 * Computes the global temporal range of the whole FLEF file, walking
	 * every record and every date-bearing child. Returns {@code null}
	 * when no date is found.
	 *
	 * @return {@code long[]{minJdn, maxJdn}}, or {@code null}
	 */
	public long[] computeGlobalDateRange(){
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;

		for(final FLEFRecord record : model.getRecords()){
			for(final String tag : DATE_TAGS){
				final FLEFRecord dateStruct = FLEFRecordHelper.findChild(record, tag);
				if(dateStruct == null)
					continue;

				final TemporalSpan span = dateNormalizer.normalize(dateStruct);
				if(span == null || span.start() == null)
					continue;

				final long jdn = span.start().jdn();
				if(jdn < min)
					min = jdn;
				if(jdn > max)
					max = jdn;
				if(span.end() != null){
					final long endJdn = span.end().jdn();
					if(endJdn > max)
						max = endJdn;
				}
			}
		}
		return (min == Long.MAX_VALUE? null: new long[]{min, max});
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	private Map<String, List<GeoAnchor>> build(){
		final Map<String, List<GeoAnchor>> result = new HashMap<>();

		// 1. Events: zero-duration anchors.
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String ownerId = extractParticipantId(participation);
			if(ownerId == null)
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(participation, TAG_EVENT);
			if(eventId == null)
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null)
				continue;

			final NormalizedDate date = extractDate(event, TAG_DATE);
			if(date == null)
				continue;

			final GeoCoordinate position = extractPosition(event);
			if(position == null)
				continue;

			final String placeName = extractPlaceName(event);
			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			final String kind = "event:" + (type != null? type: "event");

			result.computeIfAbsent(ownerId, k -> new ArrayList<>())
				.add(new GeoAnchor(ownerId, date.jdn(), date.jdn(), position,
					(placeName != null? placeName: StringUtils.EMPTY),
					kind, StringUtils.EMPTY));
		}

		// 2. Attributes: anchors with a real duration.
		addAttributeAnchors(result, IndividualAttributeHandler.TYPE, TAG_SUBJECT);
		addAttributeAnchors(result, GroupAttributeHandler.TYPE, TAG_GROUP);

		for(final List<GeoAnchor> list : result.values())
			list.sort(Comparator.comparingLong(GeoAnchor::startJdn));
		return result;
	}

	private void addAttributeAnchors(final Map<String, List<GeoAnchor>> result,
		final String recordType, final String ownerTag){
		for(final FLEFRecord attribute : model.getRecordsByType(recordType)){
			final String ownerId = extractOwner(attribute, ownerTag);
			if(ownerId == null)
				continue;

			final NormalizedDate from = extractDate(attribute, TAG_VALID_FROM);
			final NormalizedDate to = extractDate(attribute, TAG_VALID_TO);
			if(from == null && to == null)
				continue;

			final GeoCoordinate position = extractPosition(attribute);
			if(position == null)
				continue;

			final long startJdn = (from != null? from.jdn(): Long.MIN_VALUE);
			final long endJdn = (to != null? to.jdn(): Long.MAX_VALUE);
			final String type = FLEFRecordHelper.getChildValue(attribute, TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(attribute, TAG_VALUE);
			final String placeName = extractPlaceName(attribute);
			final String kind = "attribute:" + (type != null? type: "attribute");

			result.computeIfAbsent(ownerId, k -> new ArrayList<>())
				.add(new GeoAnchor(ownerId, startJdn, endJdn, position,
					(placeName != null? placeName: StringUtils.EMPTY),
					kind, (value != null? value: StringUtils.EMPTY)));
		}
	}

	private NormalizedDate extractDate(final FLEFRecord parent, final String tag){
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(parent, tag);
		if(dateStruct == null)
			return null;

		final TemporalSpan span = dateNormalizer.normalize(dateStruct);
		return (span != null? span.start(): null);
	}

	private GeoCoordinate extractPosition(final FLEFRecord record){
		final String placeId = extractPlaceId(record);
		if(placeId == null)
			return null;

		final PlaceCoordinateResolver.Resolved resolved = placeResolver.resolve(placeId);
		return (resolved != null? resolved.coordinate(): null);
	}

	private String extractPlaceName(final FLEFRecord record){
		final String placeId = extractPlaceId(record);
		if(placeId == null)
			return null;

		final FLEFRecord placeCitation = FLEFRecordHelper.findChild(record, PlaceHandler.TYPE);
		if(placeCitation != null){
			final String original = FLEFRecordHelper.getChildValue(placeCitation, "original_text");
			if(StringUtils.isNotEmpty(original))
				return original;
		}

		final FLEFRecord place = model.getRecordById(placeId);
		if(place == null)
			return null;

		final FLEFRecord nameStruct = FLEFRecordHelper.findChild(place, "name");
		if(nameStruct == null)
			return null;

		return FLEFRecordHelper.getChildValue(nameStruct, "value");
	}

	private static String extractPlaceId(final FLEFRecord record){
		final FLEFRecord placeCitation = FLEFRecordHelper.findChild(record, PlaceHandler.TYPE);
		if(placeCitation == null)
			return null;

		final FLEFRecord placeRef = FLEFRecordHelper.findChild(placeCitation, PlaceHandler.TYPE);
		return (placeRef != null? placeRef.getValue(): null);
	}

	private static String extractParticipantId(final FLEFRecord participation){
		final FLEFRecord field = FLEFRecordHelper.findChild(participation, TAG_PARTICIPANT);
		if(field == null)
			return null;

		final FLEFRecord ref = field.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}

	private static String extractOwner(final FLEFRecord record, final String ownerTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(record, ownerTag);
		if(field == null)
			return null;

		final FLEFRecord inner = field.getTheOnlyChild();
		return (inner != null? inner.getValue(): null);
	}


	/* ======================================================================
	 *                          Records
	 * ====================================================================== */

	/**
	 * A position valid over a time interval.
	 *
	 * @param ownerId   the individual or group id
	 * @param startJdn  the start of the validity interval, or {@link Long#MIN_VALUE}
	 * @param endJdn    the end of the validity interval, or {@link Long#MAX_VALUE}
	 * @param position  the geographic position
	 * @param placeName the display name of the place
	 * @param kind      {@code "event:birth"}, {@code "attribute:residence"}, …
	 * @param value     the attribute value, or empty for events
	 */
	public record GeoAnchor(String ownerId, long startJdn, long endJdn,
									GeoCoordinate position, String placeName, String kind, String value){
	}


	public record GeoCoordinate(double latitude, double longitude){

		/**
		 * Matches a coordinate pair in any of these forms:
		 * <ul>
		 *   <li>ISO 6709 signed decimal: {@code +45.650556+12.213333/}</li>
		 *   <li>decimal with hemisphere letters: {@code N45.650556 E12.213333},
		 *       {@code 45.650556N 12.213333E}, {@code N45.650556E12.213333}</li>
		 *   <li>decimal with hemisphere after both: {@code 45.650556 N 12.213333 E}</li>
		 *   <li>plain signed/unsigned decimal: {@code 45.650556, 12.213333},
		 *       {@code -33.86, 151.20}</li>
		 *   <li>any of the above with an optional degree symbol after the numbers</li>
		 * </ul>
		 * Group layout:
		 * <pre>
		 *   1: N/S before latitude (optional)
		 *   2: latitude number
		 *   3: N/S after latitude  (optional)
		 *   4: E/W before longitude (optional)
		 *   5: longitude number
		 *   6: E/W after longitude  (optional)
		 * </pre>
		 */
		private static final Pattern DECIMAL_COORDINATES = Pattern.compile(
			"\\s*"
				+ "([NS])?\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*°?\\s*([NS])?"
				+ "[,\\s]*"
				+ "([EW])?\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*°?\\s*([EW])?"
				+ "\\s*/?\\s*",
			Pattern.CASE_INSENSITIVE
		);


		public static GeoCoordinate parse(final String raw){
			if(StringUtils.isEmpty(raw))
				return null;

			final Matcher m = DECIMAL_COORDINATES.matcher(raw.trim());
			if(!m.matches())
				return null;

			final String latHemi = firstNonEmpty(m.group(1), m.group(3));
			final String lonHemi = firstNonEmpty(m.group(4), m.group(6));

			try{
				double lat = Double.parseDouble(m.group(2));
				double lon = Double.parseDouble(m.group(5));

				// When a hemisphere letter is present, it overrides the
				// sign of the number: the value is taken as an absolute
				// magnitude and the letter determines the direction.
				if(latHemi != null){
					lat = Math.abs(lat);
					if("S".equalsIgnoreCase(latHemi))
						lat = -lat;
				}
				if(lonHemi != null){
					lon = Math.abs(lon);
					if("W".equalsIgnoreCase(lonHemi))
						lon = -lon;
				}

				if(lat < -90. || lat > 90. || lon < -180. || lon > 180.)
					return null;
				return new GeoCoordinate(lat, lon);
			}
			catch(final NumberFormatException ignored){
				return null;
			}
		}

		private static String firstNonEmpty(final String a, final String b){
			return (a != null && !a.isEmpty()? a: b);
		}
	}

}
