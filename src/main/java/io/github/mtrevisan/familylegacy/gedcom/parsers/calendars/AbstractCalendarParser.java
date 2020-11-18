package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


abstract class AbstractCalendarParser{

	private static final String FORMAT_RANGE_PREFIX = "(FROM|BEF(?:ORE|\\.)?|AFT(?:ER|\\.)?|BET(?:WEEN|\\.)?|BTW\\.?|TO) ";
	private static final String FORMAT_DAY_MONTH_YEAR = "(\\d{1,2} )?([A-Z]{3,4} )?\\d{1,4}(\\/\\d{2})? ?([AB]C|[AB]\\.C\\.|B?CE|(?:B\\.)?C\\.E\\.)?";

	private static final Pattern PATTERN_RANGE_INFIX = RegexHelper.pattern("(?i) (AND|TO) ");

	private static final Pattern PATTERN_PREFIXES_APPROXIMATIONS = RegexHelper.pattern("(?i)((?:(?:ABT|APP(?:RO)?X|CALC?|EST)\\.?|ABOUT)) ");
	private static final Pattern PATTERN_PREFIXES_RANGE = RegexHelper.pattern("(?i)((?:BET(?:WEEN|\\.)?|BTW\\.?|FROM)) ");
	private static final Pattern PATTERN_INTERPRETED = RegexHelper.pattern("(?i)^INT\\.?\\s+((\\d{1,2} )?([A-Z]{3,4} )?(\\d{1,4}(/\\d{2})?)?)\\s+\\([^)]+\\)$");
	private static final Pattern PATTERN_INTERPRETATION_TEXT = RegexHelper.pattern("\\(([^)]+)\\)$");

	public static final Pattern PATTERN_APPROX = RegexHelper.pattern("(?i)APP(RO)?X\\.?");
	public static final String DESCRIPTION_APPROX = "Approximated";
	public static final String TYPE_APPROX = "APPX";
	public static final Pattern PATTERN_AND = RegexHelper.pattern("(?i)AND");
	public static final String DESCRIPTION_AND = "And";
	public static final String TYPE_AND = "AND";

	/** The regex pattern that identifies two-date range or period. Works for Gregorian, Julian, and Hebrew years */
	private static final Pattern PATTERN_TWO_DATES = RegexHelper.pattern("(?i)^" + FORMAT_RANGE_PREFIX
		+ FORMAT_DAY_MONTH_YEAR + " (AND|TO) " + FORMAT_DAY_MONTH_YEAR + "$");

	private static final Pattern PATTERN_PREFIXES_FROM_TO = RegexHelper.pattern("(?i)((?:FROM|BEF(?:ORE|\\.)?|AFT(?:ER|\\.)?|TO)) ");



	public enum CalendarType{
		GREGORIAN("Gregorian", CalendarParserBuilder.CALENDAR_GREGORIAN),
		JULIAN("Julian", CalendarParserBuilder.CALENDAR_JULIAN),
		FRENCH_REPUBLICAN("French Republican", CalendarParserBuilder.CALENDAR_FRENCH_REPUBLICAN),
		HEBREW("Hebrew", CalendarParserBuilder.CALENDAR_HEBREW);


		private final String description;
		private final String type;


		public static CalendarType createFromIndex(int index){
			return values()[index];
		}

		CalendarType(final String description, final String type){
			this.description = description;
			this.type = type;
		}

		public static String[] getDescriptions(){
			final List<String> list = new ArrayList<>();
			for(final CalendarType calendarType : values())
				list.add(calendarType.description);
			return list.toArray(new String[0]);
		}

		public String getType(){
			return type;
		}

		public boolean isGregorianOrJulian(){
			return (this == GREGORIAN || this == JULIAN);
		}
	};

	public enum IntervalType{
		NO_TYPE(StringUtils.EMPTY, StringUtils.EMPTY, null),
		ABOUT("About", "ABT", RegexHelper.pattern("(?i)(?:ABT\\.?|ABOUT|APP(?:RO)?X\\.?)")),
		CALCULATED("Calculated", "CAL", RegexHelper.pattern("(?i)CALC?\\.?")),
		ESTIMATED("Estimated", "EST", RegexHelper.pattern("(?i)EST\\.?")),
		BETWEEN("Between", "BET", RegexHelper.pattern("(?i)(?:(?:BET|BTW)\\.?|BETWEEN)")),
		BEFORE("Before", "BEF", RegexHelper.pattern("(?i)BEF(?:ORE|\\.)?")),
		AFTER("After", "AFT", RegexHelper.pattern("(?i)AFT(?:ER|\\.)?")),
		FROM("From", "FROM", RegexHelper.pattern("(?i)FROM")),
		TO("To", "TO", RegexHelper.pattern("(?i)TO")),
		INTERPRETED("Interpreted", "INT", RegexHelper.pattern("(?i)INT\\.?"));


		private static final Set<IntervalType> QUALIFICATION_NOT_PRESENT_IF = new HashSet<>();
		static{
			QUALIFICATION_NOT_PRESENT_IF.add(ABOUT);
			QUALIFICATION_NOT_PRESENT_IF.add(CALCULATED);
			QUALIFICATION_NOT_PRESENT_IF.add(ESTIMATED);
			QUALIFICATION_NOT_PRESENT_IF.add(INTERPRETED);
		}


		private final String description;
		private final String type;
		private final Pattern pattern;


		public static IntervalType createFromIndex(int index){
			return values()[index];
		}

		public static IntervalType createFromDate(String date){
			for(IntervalType type : values())
				if(type.pattern != null && RegexHelper.find(date, type.pattern))
					return type;
			return NO_TYPE;
		}

		IntervalType(final String description, final String type, final Pattern pattern){
			this.description = description;
			this.type = type;
			this.pattern = pattern;
		}

		public static String[] getDescriptions(){
			final List<String> list = new ArrayList<>();
			for(final IntervalType intervalType : values())
				list.add(intervalType.description);
			return list.toArray(new String[0]);
		}

		public String getType(){
			return type;
		}

		public boolean canQualificationBePresent(){
			return !QUALIFICATION_NOT_PRESENT_IF.contains(this);
		}

		public boolean isRangePresent(){
			return (this == BETWEEN || this == FROM);
		}

		public static String replaceAll(String date){
			for(IntervalType value : values())
				if(value != NO_TYPE)
					date = RegexHelper.replaceAll(date, value.pattern, value.description);
			return date;
		}

		public static String restoreAll(String date){
			for(IntervalType value : values())
				if(value != NO_TYPE)
					date = StringUtils.replaceEachRepeatedly(date, new String[]{value.description}, new String[]{value.type});
			return date;
		}
	};

	public enum Qualification{
		NO_QUALIFICATION(StringUtils.EMPTY, null),
		ABOUT("About", RegexHelper.pattern("(?i)ABT\\.?|ABOUT|APP(?:RO)?X\\.?")),
		CALCULATED("Calculated", RegexHelper.pattern("(?i)CALC?\\.?")),
		ESTIMATED("Estimated", RegexHelper.pattern("(?i)EST\\.?"));


		private final String description;
		private final Pattern pattern;


		public static Qualification createFromIndex(int index){
			return values()[index];
		}

		public static Qualification createFromDate(String date){
			for(Qualification type : values())
				if(type.pattern != null && RegexHelper.find(date, type.pattern))
					return type;
			return null;
		}

		Qualification(final String description, final Pattern pattern){
			this.description = description;
			this.pattern = pattern;
		}

		public static String[] getDescriptions(){
			final List<String> list = new ArrayList<>();
			for(final Qualification qualification : values())
				list.add(qualification.description);
			return list.toArray(new String[0]);
		}
	};


	public abstract CalendarType getCalendarType();

	public CalendarData extractComponents(String date){
		date = CalendarParserBuilder.removeCalendarType(date);

		String[] rawComponents = StringUtils.splitByWholeSeparator(date, StringUtils.SPACE);
		IntervalType intervalType = (rawComponents.length > 0? IntervalType.createFromDate(rawComponents[0]): null);
		Qualification fromQualification = (rawComponents.length > 1? Qualification.createFromDate(rawComponents[1]): null);
		String interpretedFrom = extractInterpretedFrom(date);

		date = removeApproximations(date);
		date = removeOpenEndedRangesAndPeriods(date);

		CalendarData responseBuilder = new CalendarData()
			.withCalendarType(getCalendarType())
			.withIntervalType(intervalType)
			.withInterpretedFrom(interpretedFrom);
		if(isRange(date)){
			Pair<String, String> range = getDatesFromRangeOrPeriod(date);
			if(range == null)
				return null;

			DateData fromDate = extractSingleDateComponents(range.getLeft());
			responseBuilder.withFromDate(fromDate)
				.withFromQualification(fromQualification);
			DateData toDate = extractSingleDateComponents(range.getRight());
			Qualification toQualification = null;
			for(int i = 2; i < rawComponents.length; i ++){
				toQualification = Qualification.createFromDate(rawComponents[i]);
				if(toQualification != null)
					break;
				}
			responseBuilder.withToDate(toDate)
				.withToQualification(toQualification);
		}
		else{
			DateData singleDate = extractSingleDateComponents(date);
			responseBuilder.withFromDate(singleDate)
				.withFromQualification(fromQualification);
		}
		return responseBuilder;
	}

	protected abstract DateData extractSingleDateComponents(String singleDate);

	public abstract LocalDate parse(String date, DatePreciseness preciseness);

	public abstract int parseMonth(String month);

	public boolean isRange(String date){
		return RegexHelper.matches(date, PATTERN_TWO_DATES);
	}

	/**
	 * Get the preferred date from a range or period, for Gregorian/Julian dates
	 *
	 * @param dateRange		the date range string
	 * @param preciseness	the preferred method of handling the range
	 * @return	the date, or null if no date could be parsed from the data
	 */
	protected final LocalDate getDateFromRangeOrPeriod(String dateRange, DatePreciseness preciseness){
		if(preciseness == null)
			throw new IllegalArgumentException("Unexpected value for imprecise date preference: " + preciseness);

		Pair<String, String> range = getDatesFromRangeOrPeriod(dateRange);
		if(range == null)
			return null;

		//calculate the dates from the two strings, based on what's preferred
		return preciseness.applyToRange(range, this::parse);
	}

	/**
	 * Get the preferred date from a range or period, for Gregorian/Julian dates
	 *
	 * @param dateRange	the date range string
	 * @return	the date, or null if no date could be parsed from the data
	 */
	protected Pair<String, String> getDatesFromRangeOrPeriod(String dateRange){
		//split the string into two dates
		String[] dates = splitDates(dateRange);
		if(dates.length != 2)
			return null;

		return Pair.of(dates[0], dates[1]);
	}

	/**
	 * Split a two-date string, removing prefixes, and return an array of two date strings
	 *
	 * @param date	the date string containing two dates
	 * @return	an array of two strings, or an empty array if the supplied <code>dateString</code> value does not contain the
	 *				<code>splitOn</code> delimiter value. Never returns null.
	 */
	private String[] splitDates(String date){
		date = RegexHelper.clear(date, PATTERN_PREFIXES_RANGE);
		return RegexHelper.split(date, PATTERN_RANGE_INFIX);
	}

	/**
	 * Return a version of the string with approximation prefixes removed, including handling for interpreted dates
	 *
	 * @param date	the date string
	 * @return a version of the string with approximation prefixes removed
	 */
	public static boolean isApproximation(String date){
		return RegexHelper.find(date, PATTERN_PREFIXES_APPROXIMATIONS);
	}

	/**
	 * Return a version of the string with approximation prefixes removed, including handling for interpreted dates
	 *
	 * @param date	the date string
	 * @return a version of the string with approximation prefixes removed
	 */
	protected String removeApproximations(String date){
		date = RegexHelper.clear(date, PATTERN_PREFIXES_APPROXIMATIONS);
		date = RegexHelper.replaceFirst(date, PATTERN_INTERPRETED, "$1");
		return date;
	}

	protected String extractInterpretedFrom(String date){
		return (RegexHelper.find(date, PATTERN_INTERPRETATION_TEXT)? PATTERN_INTERPRETATION_TEXT.group(1): null);
	}

	/**
	 * Remove the prefixes for open ended date ranges with only one date (e.g., "BEF 1900", "FROM 1756", "AFT 2000")
	 *
	 * @param date	the date string
	 * @return	the same date string with range/period prefixes removed, but only if it's an open-ended period or range
	 */
	protected String removeOpenEndedRangesAndPeriods(String date){
		return (!RegexHelper.matches(date, PATTERN_TWO_DATES)? RegexHelper.clear(date, PATTERN_PREFIXES_FROM_TO): date);
	}

}
