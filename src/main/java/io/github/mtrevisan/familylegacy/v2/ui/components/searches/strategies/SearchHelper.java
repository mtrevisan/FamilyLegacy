package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


public class SearchHelper{

	private static final String DOT = ".";

	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
	private static final String TAG_VALUE = "value";
	private static final String TAG_POINT = "point";
	private static final String TAG_BOUNDED = "bounded";
	private static final String TAG_NOT_BEFORE = "not_before";
	private static final String TAG_NOT_AFTER = "not_after";
	private static final String TAG_SPANNING = "spanning";
	private static final String TAG_FROM = "from";
	private static final String TAG_TO = "to";
	private static final String TAG_FULL_DATE = "full_date";
	private static final String TAG_DECADE = "decade";
	private static final String TAG_START_YEAR = "start_year";
	private static final String TAG_CENTURY = "century";
	private static final String TAG_ORDINAL = "ordinal";
	private static final String TAG_PART = "part";
	private static final String TAG_NAME = "name";
	private static final String TAG_ORIGINAL_TEXT = "original_text";
	private static final String TAG_APPROXIMATE = "approximate";
	private static final String TAG_BASIS = "basis";
	private static final String TAG_CENTURY_APPROXIMATE_BASIS = TAG_CENTURY + DOT + TAG_APPROXIMATE + DOT + TAG_BASIS;
	private static final String TAG_DECADE_APPROXIMATE_BASIS = TAG_DECADE + DOT + TAG_APPROXIMATE + DOT + TAG_BASIS;
	private static final String TAG_FULL_DATE_APPROXIMATE_BASIS = TAG_FULL_DATE + DOT + TAG_APPROXIMATE + DOT + TAG_BASIS;
	private static final String TAG_PLACE_PLACE = TAG_PLACE + DOT + TAG_PLACE;
	private static final String TAG_DATE_VALUE_BOUNDED_NOT_BEFORE = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_BOUNDED + DOT + TAG_NOT_BEFORE;
	private static final String TAG_DATE_VALUE_BOUNDED_NOT_AFTER = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_BOUNDED + DOT + TAG_NOT_AFTER;
	private static final String TAG_DATE_VALUE_SPANNING_FROM = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_SPANNING + DOT + TAG_FROM;
	private static final String TAG_DATE_VALUE_SPANNING_TO = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_SPANNING + DOT + TAG_TO;
	private static final String TAG_FULL_DATE_VALUE = TAG_FULL_DATE + DOT + TAG_VALUE;
	private static final String TAG_DECADE_START_YEAR = TAG_DECADE + DOT + TAG_START_YEAR;
	private static final String TAG_CENTURY_ORDINAL = TAG_CENTURY + DOT + TAG_ORDINAL;
	private static final String TAG_NAME0_VALUE = TAG_NAME + "[0]" + DOT + TAG_VALUE;
	private static final String TAG_CENTURY_PART = TAG_CENTURY + DOT + TAG_PART;
	private static final String TAG_PLACE_ORIGINAL_TEXT = TAG_PLACE + DOT + TAG_ORIGINAL_TEXT;
	private static final String TAG_DATE_ORIGINAL_TEXT = TAG_DATE + DOT + TAG_ORIGINAL_TEXT;


	private SearchHelper(){}


	public static String extractDate(final FLEFRecord event){
		final String originalText = FLEFRecordHelper.getChildValue(event, TAG_DATE_ORIGINAL_TEXT);
		if(originalText != null && !originalText.isBlank())
			return originalText;

		// Point Date
		final String point = formatSingleDate(event, TAG_DATE + DOT + TAG_VALUE + DOT + TAG_POINT);
		if(point != null)
			return point;

		// Bounded Date (not_before / not_after)
		final String notBefore = formatSingleDate(event, TAG_DATE_VALUE_BOUNDED_NOT_BEFORE);
		final String notAfter = formatSingleDate(event, TAG_DATE_VALUE_BOUNDED_NOT_AFTER);
		if(notBefore != null && notAfter != null)
			return "between " + notBefore + " and " + notAfter;
		if(notBefore != null)
			return "after " + notBefore;
		if(notAfter != null)
			return "before " + notAfter;

		// Spanning Date (from / to)
		final String from = formatSingleDate(event, TAG_DATE_VALUE_SPANNING_FROM);
		final String to = formatSingleDate(event, TAG_DATE_VALUE_SPANNING_TO);
		if(from != null && to != null)
			return "from " + from + " to " + to;
		if(from != null)
			return "from " + from;
		if(to != null)
			return "to " + to;

		return null;
	}

	private static String formatSingleDate(final FLEFRecord record, final String basePath){
		String dateStr = null;

		// full_date
		final String fullDate = FLEFRecordHelper.getChildValue(record, basePath + DOT + TAG_FULL_DATE_VALUE);
		if(fullDate != null && !fullDate.isBlank())
			dateStr = fullDate;
		else{
			// decade
			final String decade = FLEFRecordHelper.getChildValue(record, basePath + DOT + TAG_DECADE_START_YEAR);
			if(decade != null && !decade.isBlank())
				dateStr = decade + "s";
			else{
				// century
				final String centuryOrdinal = FLEFRecordHelper.getChildValue(record, basePath + DOT + TAG_CENTURY_ORDINAL);
				if(centuryOrdinal != null && !centuryOrdinal.isBlank()){
					final String part = FLEFRecordHelper.getChildValue(record, basePath + DOT + TAG_CENTURY_PART);
					dateStr = (part != null ? part.replace('_', ' ') + " " : "") + centuryOrdinal + "th century";
				}
			}
		}

		if(dateStr == null)
			return null;

		// Check for approximate qualifier
		final String approxBasis = FLEFRecordHelper.getChildValue(record, basePath + DOT + TAG_FULL_DATE_APPROXIMATE_BASIS);
		final String decadeApprox = FLEFRecordHelper.getChildValue(record, basePath + DOT + TAG_DECADE_APPROXIMATE_BASIS);
		final String centuryApprox = FLEFRecordHelper.getChildValue(record, basePath + DOT + TAG_CENTURY_APPROXIMATE_BASIS);

		if(approxBasis != null || decadeApprox != null || centuryApprox != null)
			dateStr = "abt. " + dateStr;

		return dateStr;
	}

	static String extractPlace(final FLEFRecord event, final FLEFModel model){
		final String originalText = FLEFRecordHelper.getChildValue(event, TAG_PLACE_ORIGINAL_TEXT);
		if(originalText != null && !originalText.isBlank())
			return originalText;

		final String placeRef = FLEFRecordHelper.getChildValue(event, TAG_PLACE_PLACE);
		if(placeRef != null){
			final FLEFRecord placeRecord = model.getRecordById(placeRef);
			if(placeRecord != null){
				final String placeName = FLEFRecordHelper.getChildValue(placeRecord, TAG_NAME0_VALUE);
				if(placeName != null && !placeName.isBlank())
					return placeName;
			}
		}
		return null;
	}

	static String joinNonNull(final String delimiter, final String first, final String second){
		if(first != null && second != null)
			return first + delimiter + second;
		return (first != null? first: second);
	}

}
