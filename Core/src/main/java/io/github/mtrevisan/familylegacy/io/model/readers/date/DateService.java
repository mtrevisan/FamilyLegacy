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
package io.github.mtrevisan.familylegacy.io.model.readers.date;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.Locale;


public class DateService{

	private DateService(){}


	/**
	 * Extracts a human-readable summary from a DATE node.
	 * The DATE node can contain:
	 * - VALUE (with ISO/CENTURY/DECADE + optional APPROXIMATE)
	 * - BOUNDED (with NOT_BEFORE/NOT_AFTER, each containing a date)
	 * - SPANNING (with FROM/TO, each containing a date)
	 */
	public static String getDateDisplayText(final FLEFRecord valueRecord){
		if(valueRecord == null)
			return StringUtils.EMPTY;

		// Check for POINT
		final FLEFRecord point = FLEFRecordHelper.findChild(valueRecord, DateReader.TAG_POINT);
		if(point != null)
			return getPointDisplayText(point);

		// Check for BOUNDED
		final FLEFRecord bounded = FLEFRecordHelper.findChild(valueRecord, DateReader.TAG_BOUNDED);
		if(bounded != null)
			return getBoundedDisplayText(bounded);

		// Check for SPANNING
		final FLEFRecord spanning = FLEFRecordHelper.findChild(valueRecord, DateReader.TAG_SPANNING);
		if(spanning != null)
			return getSpanningDisplayText(spanning);

		return null;
	}

	/**
	 * Extracts a single date string from a node that may contain FULL_DATE, DECADE, or CENTURY
	 * and optional APPROXIMATE.
	 */
	private static String getPointDisplayText(final FLEFRecord singleDateRecord){
		final StringBuilder dateStr = new StringBuilder(getSingleDateDisplayText(singleDateRecord));
		if(dateStr.isEmpty())
			return "--";

		// Check for APPROXIMATE (direct child of SingleDate struct)
		final FLEFRecord approx = FLEFRecordHelper.findChild(singleDateRecord, DateReader.TAG_APPROXIMATE);
		if(approx != null){
			final String basis = FLEFRecordHelper.getChildValue(approx, DateReader.TAG_BASIS);
			final String margin = FLEFRecordHelper.getChildValue(approx, DateReader.TAG_MARGIN);
			if(basis != null || margin != null){
				dateStr.append(StringUtils.SPACE)
					.append('[')
					.append(I18N.t("dialog.date.description.approximate.abbreviation"));
				if(basis != null)
					dateStr.append(StringUtils.SPACE)
						.append(I18N.t("dialog.date.description.basis"))
						.append(':')
						.append(StringUtils.SPACE)
						.append(basis);
				if(margin != null)
					dateStr.append(StringUtils.SPACE)
						.append(I18N.t("dialog.date.description.margin"))
						.append(':')
						.append(StringUtils.SPACE)
						.append(margin);
				dateStr.append(']');
			}
			else
				dateStr.append(StringUtils.SPACE)
					.append('[')
					.append(I18N.t("dialog.date.description.approximate.abbreviation"))
					.append(']');
		}
		return dateStr.toString();
	}

	/**
	 * Extracts the actual date value from FULL_DATE, DECADE, or CENTURY (including CALENDAR).
	 */
	private static String getSingleDateDisplayText(final FLEFRecord singleDateRecord){
		if(singleDateRecord == null)
			return StringUtils.EMPTY;

		final String calendar = FLEFRecordHelper.getChildValue(singleDateRecord, DateReader.TAG_CALENDAR);
		final FLEFRecord valueContainer = FLEFRecordHelper.findChild(singleDateRecord, DateReader.TAG_VALUE);
		if(valueContainer == null)
			return StringUtils.EMPTY;

		final FLEFRecord fullDate = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_FULL_DATE);
		if(fullDate != null){
			final String value = fullDate.getValue();
			return value + (calendar != null? StringUtils.SPACE + "(" + calendar + ")": StringUtils.EMPTY);
		}

		final FLEFRecord decade = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_DECADE);
		if(decade != null){
			final String startYear = decade.getValue();
			return I18N.tf("dialog.date.description.decade", startYear)
				+ (calendar != null? StringUtils.SPACE + "(" + calendar + ")": StringUtils.EMPTY);
		}

		final FLEFRecord century = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_CENTURY);
		if(century != null){
			final String ordinal = FLEFRecordHelper.getChildValue(century, DateReader.TAG_ORDINAL);
			final String part = FLEFRecordHelper.getChildValue(century, DateReader.TAG_PART);
			String centuryStr = I18N.tf("dialog.date.description.century", ordinal);
			if(part != null)
				centuryStr += " (" + part + ")";
			if(calendar != null)
				centuryStr += StringUtils.SPACE + "(" + calendar + ")";
			return centuryStr;
		}

		return StringUtils.EMPTY;
	}

	private static String getBoundedDisplayText(final FLEFRecord boundedRecord){
		final FLEFRecord notBeforeRecord = FLEFRecordHelper.findChild(boundedRecord, DateReader.TAG_NOT_BEFORE);
		final String notBefore = getSingleDateDisplayText(notBeforeRecord);
		final FLEFRecord notAfterRecord = FLEFRecordHelper.findChild(boundedRecord, DateReader.TAG_NOT_AFTER);
		final String notAfter = getSingleDateDisplayText(notAfterRecord);
		if(!notBefore.isEmpty() && !notAfter.isEmpty())
			return I18N.tf("dialog.date.description.between.and", notBefore, notAfter);
		if(!notBefore.isEmpty())
			return I18N.tf("dialog.date.description.after", notBefore);
		if(!notAfter.isEmpty())
			return I18N.tf("dialog.date.description.before", notAfter);
		return null;
	}

	private static String getSpanningDisplayText(final FLEFRecord spanningRecord){
		final FLEFRecord fromRecord = FLEFRecordHelper.findChild(spanningRecord, DateReader.TAG_FROM);
		final String from = getSingleDateDisplayText(fromRecord);
		final FLEFRecord toRecord = FLEFRecordHelper.findChild(spanningRecord, DateReader.TAG_TO);
		final String to = getSingleDateDisplayText(toRecord);
		if(!from.isEmpty() && !to.isEmpty())
			return I18N.tf("dialog.date.description.from.to", from, to);
		if(!from.isEmpty())
			return I18N.tf("dialog.date.description.from", from);
		if(!to.isEmpty())
			return I18N.tf("dialog.date.description.until", to);
		return null;
	}


	public static String getYearDisplayText(final FLEFRecord valueRecord){
		if(valueRecord == null)
			return StringUtils.EMPTY;

		final FLEFRecord point = FLEFRecordHelper.findChild(valueRecord, DateReader.TAG_POINT);
		if(point != null)
			return getSingleDateYearDisplayText(point);

		final FLEFRecord bounded = FLEFRecordHelper.findChild(valueRecord, DateReader.TAG_BOUNDED);
		if(bounded != null)
			return getBoundedYearDisplayText(bounded);

		final FLEFRecord spanning = FLEFRecordHelper.findChild(valueRecord, DateReader.TAG_SPANNING);
		if(spanning != null)
			return getSpanningYearDisplayText(spanning);

		return StringUtils.EMPTY;
	}

	private static String getSingleDateYearDisplayText(final FLEFRecord singleDateRecord){
		if(singleDateRecord == null)
			return StringUtils.EMPTY;

		final FLEFRecord valueContainer = FLEFRecordHelper.findChild(singleDateRecord, DateReader.TAG_VALUE);
		if(valueContainer == null)
			return StringUtils.EMPTY;

		final FLEFRecord fullDate = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_FULL_DATE);
		if(fullDate != null){
			final String calendar = FLEFRecordHelper.getChildValue(singleDateRecord, DateReader.TAG_CALENDAR);
			final String date = fullDate.getValue();
			final CalendarType calendarType = (calendar != null? CalendarType.valueOf(calendar.toUpperCase(Locale.ROOT)): CalendarType.GREGORIAN);
			final long dateJdn = calendarType.parseToJdn(date);
			final LocalDate prolepticDate = CalendarType.jdnToProlepticGregorianLocalDate(dateJdn);
			return applyApproximatePrefix(singleDateRecord, String.valueOf(prolepticDate.getYear()));
		}

		final FLEFRecord decade = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_DECADE);
		if(decade != null){
			final String year = decade.getValue();
			final String value = I18N.tf("dialog.date.description.decade.abbreviation", year);
			return applyApproximatePrefix(singleDateRecord, value);
		}

		final FLEFRecord century = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_CENTURY);
		if(century != null){
			final String ordinal = FLEFRecordHelper.getChildValue(century, DateReader.TAG_ORDINAL);
			final String part = FLEFRecordHelper.getChildValue(century, DateReader.TAG_PART);

			String value;
			if(part != null)
				value = I18N.tf("dialog.date.description.century.abbreviation_with_part", part, ordinal);
			else
				value = I18N.tf("dialog.date.description.century.abbreviation", ordinal);

			return applyApproximatePrefix(singleDateRecord, value);
		}

		return StringUtils.EMPTY;
	}

	private static String getBoundedYearDisplayText(final FLEFRecord boundedRecord){
		final FLEFRecord notBeforeRecord = FLEFRecordHelper.findChild(boundedRecord, DateReader.TAG_NOT_BEFORE);
		final FLEFRecord notAfterRecord = FLEFRecordHelper.findChild(boundedRecord, DateReader.TAG_NOT_AFTER);
		final String notBefore = getSingleDateYearDisplayText(notBeforeRecord);
		final String notAfter = getSingleDateYearDisplayText(notAfterRecord);

		if(!notBefore.isEmpty() && !notAfter.isEmpty())
			return notBefore + "-" + notAfter;

		if(!notBefore.isEmpty())
			return ">" + notBefore;

		if(!notAfter.isEmpty())
			return "<" + notAfter;

		return StringUtils.EMPTY;
	}

	private static String getSpanningYearDisplayText(final FLEFRecord spanningRecord){
		final FLEFRecord fromRecord = FLEFRecordHelper.findChild(spanningRecord, DateReader.TAG_FROM);
		final FLEFRecord toRecord = FLEFRecordHelper.findChild(spanningRecord, DateReader.TAG_TO);
		final String from = getSingleDateYearDisplayText(fromRecord);
		final String to = getSingleDateYearDisplayText(toRecord);

		if(!from.isEmpty() && !to.isEmpty())
			return from + "-" + to;

		if(!from.isEmpty())
			return from + "-";

		if(!to.isEmpty())
			return "-" + to;

		return StringUtils.EMPTY;
	}

	private static String applyApproximatePrefix(final FLEFRecord singleDateRecord, final String value){
		if(singleDateRecord == null || value.isEmpty())
			return value;

		final FLEFRecord approximate = FLEFRecordHelper.findChild(singleDateRecord, DateReader.TAG_APPROXIMATE);

		return (approximate != null
			? "~" + value
			: value);
	}

}
