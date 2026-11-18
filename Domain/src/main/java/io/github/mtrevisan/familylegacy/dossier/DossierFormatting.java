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
package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.JulianDay;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpanKind;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;


/**
 * Shared formatting helpers for the individual and group dossier
 * services. Encapsulates the {@link DateNormalizer} and the place
 * resolution logic, so the two services do not duplicate them.
 */
public final class DossierFormatting{

	private static final String TAG_DATE = EventReader.TAG_DATE;


	private final FLEFModel model;


	public DossierFormatting(final FLEFModel model){
		this.model = model;
	}


	/* ==================================================================
	 *                          Dates
	 * ================================================================== */

	public String formatDate(final FLEFRecord parent){
		return formatDate(parent, TAG_DATE);
	}

	public String formatDate(final FLEFRecord parent, final String tag){
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(parent, tag);
		if(dateStruct == null)
			return StringUtils.EMPTY;

		final TemporalSpan span = DateNormalizer.normalize(dateStruct);
		if(span == null || (span.start() == null && span.end() == null))
			return StringUtils.EMPTY;

		return formatSpan(span);
	}

	/**
	 * Tells an exact date from an approximate one and from a range: "about", "calculated", "before", "after", "between ... and ...",
	 * "from ... to ...".
	 */
	static String formatSpan(final TemporalSpan span){
		final NormalizedDate start = span.start();
		final NormalizedDate end = span.end();
		if(span.isPoint())
			return formatBound(start);

		final boolean spanning = (span.kind() == TemporalSpanKind.SPANNING);
		if(start != null && end != null)
			return I18N.tf(spanning? "text.dossier.date.from_to": "text.dossier.date.between", formatBound(start), formatBound(end));
		if(start != null)
			return I18N.tf(spanning? "text.dossier.date.from": "text.dossier.date.after", formatBound(start));
		return I18N.tf(spanning? "text.dossier.date.to": "text.dossier.date.before", formatBound(end));
	}

	private static String formatBound(final NormalizedDate date){
		final String text = formatNormalizedDate(date);
		if(date.isAbout())
			return I18N.tf("text.dossier.date.about", text);
		if(date.isCalculated())
			return I18N.tf("text.dossier.date.calculated", text);
		return text;
	}

	public String formatValidity(final FLEFRecord parent, final String tagValidFrom, final String tagValidTo){
		final String from = formatDate(parent, tagValidFrom);
		final String to = formatDate(parent, tagValidTo);
		if(from.isEmpty() && to.isEmpty())
			return StringUtils.EMPTY;

		if(from.isEmpty())
			return I18N.tf("text.dossier.validity.until", to);

		if(to.isEmpty())
			return I18N.tf("text.dossier.validity.from", from);

		return I18N.tf("text.dossier.validity.range", from, to);
	}


	/* ==================================================================
	 *                          Places
	 * ================================================================== */

	public String resolvePlaceName(final FLEFRecord parent){
		final FLEFRecord citation = FLEFRecordHelper.findChild(parent, PlaceReader.TYPE);
		if(citation == null)
			return StringUtils.EMPTY;

		final FLEFRecord placeRef = FLEFRecordHelper.findChild(citation, PlaceReader.TYPE);
		final String placeId = (placeRef != null? placeRef.getValue(): null);
		if(placeId == null)
			return StringUtils.EMPTY;

		final FLEFRecord place = model.getRecordById(placeId);
		if(place == null)
			return placeId;

		// a place may have more names (e.g. translated ones): the first is the primary one
		final String v = PlaceReader.extractPrimaryName(place);
		if(StringUtils.isNotEmpty(v))
			return v;
		return placeId;
	}


	/* ==================================================================
	 *                          Internals
	 * ================================================================== */

	private static String monthName(final int month){
		return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault());
	}

	private static String formatNormalizedDate(final NormalizedDate date){
		if(date == null)
			return StringUtils.EMPTY;

		final int[] ymd = JulianDay.toGregorian(date.jdn());
		return switch(date.precision()){
			case DAY -> ymd[2] + StringUtils.SPACE + monthName(ymd[1]) + StringUtils.SPACE + ymd[0];
			case MONTH -> monthName(ymd[1]) + StringUtils.SPACE + ymd[0];
			case YEAR -> Integer.toString(ymd[0]);
			case DECADE -> I18N.tf("text.dossier.date.decade", ymd[0] / 10 * 10);
			case CENTURY -> I18N.tf("text.dossier.date.century", ymd[0] / 100 + 1);
		};
	}

}
