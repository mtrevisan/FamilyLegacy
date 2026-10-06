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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
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

	private final FLEFModel model;
	private final Map<String, List<AttributeDatum>> attributesByOwner = new HashMap<>();
	private final PlaceCoordinateResolver placeResolver;


	public TemporalAttributeIndex(final FLEFModel model, final PlaceCoordinateResolver placeResolver){
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
		final List<FLEFRecord> individualAttributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(final FLEFRecord individualAttribute : individualAttributes){
			final String ownerId = extractIndividualOwner(individualAttribute);
			if(ownerId == null)
				continue;

			final AttributeDatum datum = parse(individualAttribute, ownerId);
			if(datum != null)
				attributesByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(datum);
		}

		final List<FLEFRecord> groupAttributes = model.getRecordsByType(GroupAttributeHandler.TYPE);
		for(final FLEFRecord groupAttribute : groupAttributes){
			final String ownerId = extractGroupOwner(groupAttribute);
			if(ownerId == null)
				continue;

			final AttributeDatum datum = parse(groupAttribute, ownerId);
			if(datum != null)
				attributesByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(datum);
		}

		for(final List<AttributeDatum> list : attributesByOwner.values())
			list.sort(Comparator.comparingLong(AttributeDatum::fromJdn));
	}

	private AttributeDatum parse(final FLEFRecord attribute, final String ownerId){
		final boolean isIndividual = (FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL) != null);
		final String type = FLEFRecordHelper.getChildValue(attribute, (isIndividual? IndividualAttributeReader.TAG_TYPE: GroupAttributeReader.TAG_TYPE));
		final String value = FLEFRecordHelper.getChildValue(attribute, (isIndividual? IndividualAttributeReader.TAG_VALUE: GroupAttributeReader.TAG_VALUE));

		final Long from = extractDate(attribute, (isIndividual? IndividualAttributeReader.TAG_VALID_FROM: GroupAttributeReader.TAG_VALID_FROM));
		final Long to = extractDate(attribute, (isIndividual? IndividualAttributeReader.TAG_VALID_TO: GroupAttributeReader.TAG_VALID_TO));
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

		final TemporalSpan span = DateNormalizer.normalize(dateStruct);
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

	private static String extractIndividualOwner(final FLEFRecord individualAttribute){
		final FLEFRecord field = FLEFRecordHelper.findChild(individualAttribute, IndividualAttributeReader.TAG_INDIVIDUAL);
		if(field == null)
			return null;

		final FLEFRecord inner = field.getTheOnlyChild();
		if(inner != null && inner.getValue() != null)
			return inner.getValue();

		return null;
	}

	private static String extractGroupOwner(final FLEFRecord groupAttribute){
		final FLEFRecord field = FLEFRecordHelper.findChild(groupAttribute, GroupAttributeReader.TAG_GROUP);
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
