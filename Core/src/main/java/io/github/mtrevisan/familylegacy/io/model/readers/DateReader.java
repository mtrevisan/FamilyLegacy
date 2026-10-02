package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateService;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;


/**
 * Handler for EVENT records.
 * <p>
 * Structure:
 * <pre>
 * // Used for historical, documentary, and genealogical dates whose value may require interpretation, source evaluation, calendar conversion, or
 * // evidentiary analysis. Administrative dates such as creation dates, update dates, search dates, expiration dates, and other system metadata should
 * // use the simpler `Date` type instead.
 * struct DateStructure {
 *   value: DateValue       // contains the normalized date assertion according to the conventions of this protocol. The normalized value MAY be derived from a source expression, calendar conversion, date normalization, abbreviation expansion, calculation, or researcher interpretation.
 *   original_text?: Text   // preserves the date expression exactly as found in the source, when available
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 * }
 *
 * // Represents the nature of a date assertion. A date can be a single point, an uncertainty interval, or a duration spanning an actual period of time.
 * DateValue = oneof {
 *   point: SingleDate        // a single point in time. Unless marked as APPROXIMATE, the value is accepted as recorded, derived, or concluded from the available evidence.
 *   bounded: BoundedDate     // the exact date is unknown but is known to fall within the specified interval. The interval represents uncertainty about the date itself, not the duration of the fact.
 *   spanning: SpanningDate   // the fact itself extends across the specified interval. Unlike BoundedDate, the interval does not represent uncertainty about a date; it represents the actual duration of the event, status, relationship, or condition.
 * }
 *
 * struct BoundedDate {
 *   not_before?: SingleDate
 *   not_after?: SingleDate
 *
 *   require one_of(not_before, not_after)   // At least one of NOT_BEFORE or NOT_AFTER is required. An omitted bound represents an open-ended limit. When both bounds are present, NOT_BEFORE MUST be <= NOT_AFTER.
 * }
 *
 * struct SpanningDate {
 *   from?: SingleDate
 *   to?: SingleDate
 *
 *   require one_of(from, to)   // At least one of FROM or TO is required. An omitted bound represents an open-ended limit. When both bounds are present, FROM MUST be <= TO.
 * }
 *
 * // Indicates that the asserted date should be treated as approximate.
 * // APPROXIMATE qualifies the confidence of the date but does not change the underlying normalized value.
 * struct Approximate {
 *   basis?: enum {
 *     stated,         // the source itself says "about"/"around"
 *     calculated,     // arithmetically derived from another known date, e.g., age at death
 *     conventional,   // deduced by applying a cultural norm typical of the period/place
 *     unspecified
 *   }
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;   // mandatory in practice when basis = 'conventional': it is the record that justifies the estimate
 *   margin?: Duration                          // e.g., 'P2Y' = plus/minus 2 years, when the margin is quantifiable
 * }
 *
 * // A single point in time expressed at an explicitly declared level of precision. The granularity is determined by the selected representation
 * // and is not inferred from formatting conventions.
 * SingleDate = oneof {
 *   full_date: struct {
 *     value: HistoricalDate       // a historical date
 *     approximate?: Approximate   // Qualifies the precision of this date. The underlying date remains the asserted value; APPROXIMATE describes uncertainty about that value.
 *     calendar: CalendarType | Text
 *   }
 *   decade: struct {
 *     start_year: Int             // a decade identified by its starting year. For example, 1490 represents the decade commonly referred to as the '1490s'
 *     approximate?: Approximate   // Qualifies the precision of this date. The underlying date remains the asserted value; APPROXIMATE describes uncertainty about that value.
 *     calendar: CalendarType | Text
 *   }
 *   century: struct {
 *     ordinal: Int                // a century designation, such as 15 for the 15th century
 *     part?: CenturyPart          // an optional subdivision of the century
 *     approximate?: Approximate   // Qualifies the precision of this date. The underlying date remains the asserted value; APPROXIMATE describes uncertainty about that value.
 *     calendar: CalendarType | Text
 *   }
 * }
 *
 * enum CalendarType {
 *   gregorian, julian, islamic, hebrew, chinese, indian, buddhist, french_republican, coptic, soviet_eternal, ethiopian, mayan
 * }
 *
 * // A subdivision of the century, such as a quarter, half, or relative position.
 * enum CenturyPart {
 *   first_quarter, second_quarter, third_quarter, fourth_quarter,
 *   first_half, second_half, early, mid, late
 * }
 * </pre>
 */
public final class DateReader{

	public static final String TAG_VALUE = "value";
	public static final String TAG_ORIGINAL_TEXT = "original_text";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_EVIDENCE = "evidence";

	public static final String TAG_POINT = "point";
	public static final String TAG_BOUNDED = "bounded";
	public static final String TAG_SPANNING = "spanning";

	public static final String TAG_NOT_BEFORE = "not_before";
	public static final String TAG_NOT_AFTER = "not_after";

	public static final String TAG_FROM = "from";
	public static final String TAG_TO = "to";

	public static final String TAG_BASIS = "basis";
	public static final String TAG_CULTURAL_NORM = "cultural_norm";
	public static final String TAG_MARGIN = "margin";

	public static final String TAG_APPROXIMATE = "approximate";
	public static final String TAG_CALENDAR = "calendar";

	public static final String TAG_FULL_DATE = "full_date";
	public static final String TAG_FULL_DATE_VALUE = FLEFRecordHelper.composePath(TAG_FULL_DATE, TAG_VALUE);

	public static final String TAG_DECADE = "decade";
	public static final String TAG_START_YEAR = "start_year";
	public static final String TAG_DECADE_START_YEAR = FLEFRecordHelper.composePath(TAG_DECADE, TAG_START_YEAR);

	public static final String TAG_CENTURY = "century";
	public static final String TAG_ORDINAL = "ordinal";
	public static final String TAG_PART = "part";
	public static final String TAG_CENTURY_ORDINAL = FLEFRecordHelper.composePath(TAG_CENTURY, TAG_ORDINAL);
	public static final String TAG_CENTURY_PART = FLEFRecordHelper.composePath(TAG_CENTURY, TAG_PART);

	public static final String ENUM_CALENDAR_GREGORIAN = "gregorian";
	// TODO use CalendarType
	public static final String[] CALENDARS = {
		ENUM_CALENDAR_GREGORIAN, "julian", "reformed_julian", "islamic", "hebrew", "chinese", "indian", "buddhist",
		"french_republican", "coptic", "soviet_eternal", "ethiopian", "mayan", "persian", "parsi", "byzantine",
		"egyptian", "seleucid", "armenian", "rumi"
	};

	public static final String ENUM_PART_FIRST_QUARTER = "first_quarter";
	public static final String ENUM_PART_SECOND_QUARTER = "second_quarter";
	public static final String ENUM_PART_THIRD_QUARTER = "third_quarter";
	public static final String ENUM_PART_FOURTH_QUARTER = "fourth_quarter";
	public static final String ENUM_PART_FIRST_HALF = "first_half";
	public static final String ENUM_PART_SECOND_HALF = "second_half";
	public static final String ENUM_PART_EARLY = "early";
	public static final String ENUM_PART_MID = "mid";
	public static final String ENUM_PART_LATE = "late";
	public static final String[] CENTURY_PARTS = {
		ENUM_PART_FIRST_QUARTER, ENUM_PART_SECOND_QUARTER, ENUM_PART_THIRD_QUARTER, ENUM_PART_FOURTH_QUARTER,
		ENUM_PART_FIRST_HALF, ENUM_PART_SECOND_HALF,
		ENUM_PART_EARLY, ENUM_PART_MID, ENUM_PART_LATE
	};


	private DateReader(){}


	/**
	 * Returns the {@code original_text} of a date structure, when present.
	 *
	 * @param dateStructureRecord the date structure record
	 * @return the original source expression, or {@code null}
	 */
	public static String extractOriginalText(final FLEFRecord dateStructureRecord){
		return FLEFRecordHelper.getChildValue(dateStructureRecord, TAG_ORIGINAL_TEXT);
	}

	public static TemporalSpan extractTemporalSpan(final FLEFRecord dateStructureRecord){
		return DateNormalizer.normalize(dateStructureRecord);
	}

	public static String extractPrettyPrintDate(final FLEFRecord dateStructureRecord){
		final FLEFRecord value = FLEFRecordHelper.findChild(dateStructureRecord, TAG_VALUE);
		return DateService.getDateDisplayText(value);
	}

//	public static String extractPrettyPrintYear(final FLEFRecord dateStructureRecord){
//		final FLEFRecord value = FLEFRecordHelper.findChild(dateStructureRecord, TAG_VALUE);
//		return DateService.getYearDisplayText(value);
//	}

//	public static GenealogicalDate extractGenealogicalDate(final FLEFRecord dateStructureRecord){
//		final FLEFRecord value = FLEFRecordHelper.findChild(dateStructureRecord, TAG_VALUE);
//		return UniversalDateConverter.parse(calendarStr, dateStr);
//	}



	public static String extractBasis(final FLEFRecord approxRecord){
		return FLEFRecordHelper.getChildValue(approxRecord, TAG_BASIS);
	}

	public static String extractMargin(final FLEFRecord approxRecord){
		return FLEFRecordHelper.getChildValue(approxRecord, TAG_MARGIN);
	}

}
