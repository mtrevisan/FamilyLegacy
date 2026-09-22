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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Index of individual and group attributes that carry a validity
 * interval. Used by the temporal views (lifespan strip, chronomap) to
 * render attributes such as residence, occupation, title, religion, etc.
 * as time-bounded bands or as temporal anchors.
 */
public final class TemporalAttributeIndex{

	private static final String TYPE_INDIVIDUAL = "individual";
	private static final String TYPE_GROUP = "group";
	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_GROUP = "group";
	private static final String TAG_NAME = "name";


	private final FLEFModel model;
	private final DateNormalizer dateNormalizer = new DateNormalizer();
	private final Map<String, List<AttributeDatum>> attributesByOwner = new HashMap<>();
	private final PlaceCoordinateResolver placeResolver;


	public TemporalAttributeIndex(final FLEFModel model, final PlaceCoordinateResolver placeResolver){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.model = model;
		this.placeResolver = placeResolver;

		build();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public List<AttributeDatum> attributesOf(final String ownerId){
		return attributesByOwner.getOrDefault(ownerId, List.of());
	}

	public void rebuild(){
		attributesByOwner.clear();
		build();
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	private void build(){
		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			final String ownerId = extractIndividualOwner(attribute);
			if(ownerId == null)
				continue;

			final AttributeDatum datum = parse(attribute, ownerId);
			if(datum != null)
				attributesByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(datum);
		}

		for(final FLEFRecord attribute : model.getRecordsByType(GroupAttributeHandler.TYPE)){
			final String ownerId = extractGroupOwner(attribute);
			if(ownerId == null)
				continue;

			final AttributeDatum datum = parse(attribute, ownerId);
			if(datum != null)
				attributesByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(datum);
		}

		for(final List<AttributeDatum> list : attributesByOwner.values())
			list.sort(java.util.Comparator.comparingLong(AttributeDatum::fromJdn));
	}

	private AttributeDatum parse(final FLEFRecord attribute, final String ownerId){
		final String type = FLEFRecordHelper.getChildValue(attribute, TAG_TYPE);
		final String value = FLEFRecordHelper.getChildValue(attribute, TAG_VALUE);

		final Long from = extractDate(attribute, TAG_VALID_FROM);
		final Long to = extractDate(attribute, TAG_VALID_TO);
		if(from == null && to == null)
			return null;

		final long fromJdn = (from != null? from: Long.MIN_VALUE);
		final long toJdn = (to != null? to: Long.MAX_VALUE);

		final ChronomapIndex.GeoCoordinate position = resolvePlace(attribute);

		final String id = attribute.getId();
		return new AttributeDatum(
			(id != null? id: StringUtils.EMPTY),
			ownerId,
			(type != null? type.replace('_', ' '): "attribute"),
			(value != null? value: StringUtils.EMPTY),
			fromJdn, toJdn,
			(from != null), (to != null),
			position);
	}

	private Long extractDate(final FLEFRecord parent, final String tag){
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(parent, tag);
		if(dateStruct == null)
			return null;

		final TemporalSpan span = dateNormalizer.normalize(dateStruct);
		if(span == null || span.start() == null)
			return null;

		return span.start().jdn();
	}

	private ChronomapIndex.GeoCoordinate resolvePlace(final FLEFRecord attribute){
		if(placeResolver == null)
			return null;
		final FLEFRecord placeCitation = FLEFRecordHelper.findChild(attribute, PlaceHandler.TYPE);
		if(placeCitation == null)
			return null;

		final FLEFRecord placeRef = FLEFRecordHelper.findChild(placeCitation, PlaceHandler.TYPE);
		final String placeId = (placeRef != null? placeRef.getValue(): null);
		if(placeId == null)
			return null;

		final PlaceCoordinateResolver.Resolved r = placeResolver.resolve(placeId);
		return (r != null? r.coordinate(): null);
	}

	private static String extractIndividualOwner(final FLEFRecord attribute){
		final FLEFRecord field = FLEFRecordHelper.findChild(attribute, TAG_SUBJECT);
		if(field == null)
			return null;

		final FLEFRecord inner = field.getTheOnlyChild();
		if(inner != null && inner.getValue() != null)
			return inner.getValue();

		return null;
	}

	private static String extractGroupOwner(final FLEFRecord attribute){
		final FLEFRecord field = FLEFRecordHelper.findChild(attribute, TAG_GROUP);
		if(field == null)
			return null;

		final FLEFRecord inner = field.getTheOnlyChild();
		if(inner != null && inner.getValue() != null)
			return inner.getValue();

		return null;
	}


	/* ======================================================================
	 *                          Records
	 * ====================================================================== */

	/**
	 * A single attribute with a validity interval.
	 *
	 * @param id       the attribute record id (may be empty)
	 * @param ownerId  the owner (individual or group) id
	 * @param type     the attribute type, with underscores replaced by spaces
	 * @param value    the attribute value
	 * @param fromJdn  the start of the validity interval, or {@link Long#MIN_VALUE}
	 * @param toJdn    the end of the validity interval, or {@link Long#MAX_VALUE}
	 * @param hasFrom  {@code true} when {@code valid_from} was present
	 * @param hasTo    {@code true} when {@code valid_to} was present
	 * @param position the resolved position, or {@code null}
	 */
	public record AttributeDatum(String id, String ownerId, String type, String value, long fromJdn, long toJdn,
		boolean hasFrom, boolean hasTo, ChronomapIndex.GeoCoordinate position){}

}
