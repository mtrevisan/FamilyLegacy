package io.github.mtrevisan.familylegacy.v2.io.model.readers.date;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;


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
	private static String getPointDisplayText(final FLEFRecord record){
		final StringBuilder dateStr = new StringBuilder(getSingleDateDisplayText(record));
		if(dateStr.isEmpty())
			return "--";

		// Check for APPROXIMATE (direct child of the node)
		final FLEFRecord approx = FLEFRecordHelper.findChild(record, DateReader.TAG_APPROXIMATE);
		if(approx != null){
			final String basis = FLEFRecordHelper.getChildValue(approx, DateReader.TAG_BASIS);
			final String margin = FLEFRecordHelper.getChildValue(approx, DateReader.TAG_MARGIN);
			if(basis != null || margin != null){
				dateStr.append(" (")
					.append(I18N.t("dialog.date.description.approximate"));
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
				dateStr.append(')');
			}
			else
				dateStr.append(" (")
					.append(I18N.t("dialog.date.description.approximate"))
					.append(')');
		}
		return dateStr.toString();
	}

	/**
	 * Extracts the actual date value from FULL_DATE, DECADE, or CENTURY (including CALENDAR).
	 */
	private static String getSingleDateDisplayText(final FLEFRecord singleDateRecord){
		if(singleDateRecord == null)
			return StringUtils.EMPTY;

		final FLEFRecord fullDate = singleDateRecord.getTheOnlyChild(DateReader.TAG_FULL_DATE);
		if(fullDate != null){
			final String value = FLEFRecordHelper.getChildValue(fullDate, DateReader.TAG_VALUE);
			final String calendar = FLEFRecordHelper.getChildValue(fullDate, DateReader.TAG_CALENDAR);
			return value + (calendar != null? " (" + calendar + ")": StringUtils.EMPTY);
		}

		final FLEFRecord decade = singleDateRecord.getTheOnlyChild(DateReader.TAG_DECADE);
		if(decade != null){
			final String startYear = FLEFRecordHelper.getChildValue(decade, DateReader.TAG_START_YEAR);
			final String calendar = FLEFRecordHelper.getChildValue(decade, DateReader.TAG_CALENDAR);
			return I18N.tf("dialog.date.description.decade", startYear)
				+ (calendar != null? " (" + calendar + ")": StringUtils.EMPTY);
		}

		final FLEFRecord century = singleDateRecord.getTheOnlyChild(DateReader.TAG_CENTURY);
		if(century != null){
			final String ordinal = FLEFRecordHelper.getChildValue(century, DateReader.TAG_ORDINAL);
			final String part = FLEFRecordHelper.getChildValue(century, DateReader.TAG_PART);
			final String calendar = FLEFRecordHelper.getChildValue(century, DateReader.TAG_CALENDAR);
			String centuryStr = I18N.tf("dialog.date.description.century", ordinal);
			if(part != null)
				centuryStr += " (" + part + ")";
			if(calendar != null)
				centuryStr += " (" + calendar + ")";
			return centuryStr;
		}

		return StringUtils.EMPTY;
	}

	private static String getBoundedDisplayText(final FLEFRecord boundedRecord){
		final FLEFRecord notBeforeRecord = boundedRecord.getTheOnlyChild(DateReader.TAG_NOT_BEFORE);
		final String notBefore = getSingleDateDisplayText(notBeforeRecord);
		final FLEFRecord notAfterRecord = boundedRecord.getTheOnlyChild(DateReader.TAG_NOT_AFTER);
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
		final FLEFRecord fromRecord = spanningRecord.getTheOnlyChild(DateReader.TAG_FROM);
		final String from = getSingleDateDisplayText(fromRecord);
		final FLEFRecord toRecord = spanningRecord.getTheOnlyChild(DateReader.TAG_TO);
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

		final FLEFRecord fullDate = singleDateRecord.getTheOnlyChild(DateReader.TAG_FULL_DATE);
		if(fullDate != null){
			final String calendar = FLEFRecordHelper.getChildValue(fullDate, DateReader.TAG_CALENDAR);
			final String date = FLEFRecordHelper.getChildValue(fullDate, DateReader.TAG_VALUE);
			final GenealogicalDate genealogicalDate = UniversalDateConverter.parse(calendar, date);
			final int year = genealogicalDate.isoDate().getYear();
			return applyApproximatePrefix(singleDateRecord, String.valueOf(year));
		}

		final FLEFRecord decade = singleDateRecord.getTheOnlyChild(DateReader.TAG_DECADE);
		if(decade != null){
			final String year = FLEFRecordHelper.getChildValue(decade, DateReader.TAG_START_YEAR);
			final String value = I18N.tf("dialog.date.description.decade.abbreviation", year);
			return applyApproximatePrefix(singleDateRecord, value);
		}

		final FLEFRecord century = singleDateRecord.getTheOnlyChild(DateReader.TAG_CENTURY);
		if(century != null){
			final String ordinal = FLEFRecordHelper.getChildValue(century, DateReader.TAG_ORDINAL);
			final String part = FLEFRecordHelper.getChildValue(century, DateReader.TAG_PART);

			String value;
			if(part != null)
				value = I18N.tf("dialog.date.description.century.abbreviation.with.part", part, ordinal);
			else
				value = I18N.tf("dialog.date.description.century.abbreviation", ordinal);

			return applyApproximatePrefix(singleDateRecord, value);
		}

		return StringUtils.EMPTY;
	}

	private static String getBoundedYearDisplayText(final FLEFRecord boundedRecord){
		final FLEFRecord notBeforeRecord = boundedRecord.getTheOnlyChild(DateReader.TAG_NOT_BEFORE);
		final FLEFRecord notAfterRecord = boundedRecord.getTheOnlyChild(DateReader.TAG_NOT_AFTER);
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
		final FLEFRecord fromRecord = spanningRecord.getTheOnlyChild(DateReader.TAG_FROM);
		final FLEFRecord toRecord = spanningRecord.getTheOnlyChild(DateReader.TAG_TO);
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

		final FLEFRecord approximate = singleDateRecord.getTheOnlyChild(DateReader.TAG_APPROXIMATE);

		return (approximate != null
			? "~" + value
			: value);
	}

}
