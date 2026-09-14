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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.DateNormalizer;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;


/**
 * Shared formatting helpers for the individual and group dossier
 * services. Encapsulates the {@link DateNormalizer} and the place
 * resolution logic, so the two services do not duplicate them.
 */
final class DossierFormatting{

	private static final String TAG_DATE = "date";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";

	private static final String[] MONTH_NAMES = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};


	private final FLEFModel model;
	private final DateNormalizer dateNormalizer = new DateNormalizer();


	DossierFormatting(final FLEFModel model){
		this.model = model;
	}


	/* ==================================================================
	 *                          Dates
	 * ================================================================== */

	String formatDate(final FLEFRecord parent){
		return formatDate(parent, TAG_DATE);
	}

	String formatDate(final FLEFRecord parent, final String tag){
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(parent, tag);
		if(dateStruct == null)
			return StringUtils.EMPTY;

		final TemporalSpan span = dateNormalizer.normalize(dateStruct);
		if(span == null || span.start() == null)
			return StringUtils.EMPTY;

		return formatNormalizedDate(span.start());
	}

	String formatValidity(final FLEFRecord parent){
		final String from = formatDate(parent, TAG_VALID_FROM);
		final String to = formatDate(parent, TAG_VALID_TO);
		if(from.isEmpty() && to.isEmpty())
			return StringUtils.EMPTY;

		if(from.isEmpty())
			return "until " + to;

		if(to.isEmpty())
			return "from " + from;

		return from + " – " + to;
	}


	/* ==================================================================
	 *                          Places
	 * ================================================================== */

	String resolvePlaceName(final FLEFRecord parent){
		final FLEFRecord citation = FLEFRecordHelper.findChild(parent, PlaceHandler.TYPE);
		if(citation == null)
			return StringUtils.EMPTY;

		final FLEFRecord placeRef = FLEFRecordHelper.findChild(citation, PlaceHandler.TYPE);
		final String placeId = (placeRef != null? placeRef.getValue(): null);
		if(placeId == null)
			return StringUtils.EMPTY;

		final FLEFRecord place = model.getRecordById(placeId);
		if(place == null)
			return placeId;

		final FLEFRecord nameStruct = FLEFRecordHelper.findChild(place, TAG_NAME);
		if(nameStruct != null){
			final String v = FLEFRecordHelper.getChildValue(nameStruct, TAG_VALUE);
			if(StringUtils.isNotEmpty(v))
				return v;
		}
		return placeId;
	}


	/* ==================================================================
	 *                          Internals
	 * ================================================================== */

	private static String formatNormalizedDate(final NormalizedDate date){
		if(date == null)
			return StringUtils.EMPTY;

		final int[] ymd = jdnToGregorian(date.jdn());
		return switch(date.precision()){
			case DAY -> ymd[2] + StringUtils.SPACE + MONTH_NAMES[ymd[1] - 1] + StringUtils.SPACE + ymd[0];
			case MONTH -> MONTH_NAMES[ymd[1] - 1] + StringUtils.SPACE + ymd[0];
			case YEAR -> Integer.toString(ymd[0]);
			case DECADE -> (ymd[0] / 10 * 10) + "s";
			case CENTURY -> (ymd[0] / 100 + 1) + "th c.";
		};
	}

	private static int[] jdnToGregorian(final long jdn){
		final long a = jdn + 32044L;
		final long b = (4L * a + 3L) / 146097L;
		final long c = a - (146097L * b) / 4L;
		final long d = (4L * c + 3L) / 1461L;
		final long e = c - (1461L * d) / 4L;
		final long m = (5L * e + 2L) / 153L;
		final int day = (int)(e - (153L * m + 2L) / 5L + 1L);
		final int month = (int)(m + 3L - 12L * (m / 10L));
		final int year = (int)(100L * b + d - 4800L + m / 10L);
		return new int[]{year, month, day};
	}

}
