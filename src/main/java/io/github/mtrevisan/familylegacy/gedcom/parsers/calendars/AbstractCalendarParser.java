package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


abstract class AbstractCalendarParser{

	private static final String FORMAT_RANGE_PREFIX = "(FROM|BEF(?:ORE|\\.)?|AFT(?:ER|\\.)?|BET(?:WEEN|\\.)?|BTW\\.?|TO) ";
	private static final String FORMAT_DAY_MONTH_YEAR = "(\\d{1,2} )?([A-Z]{3,4} )?\\d{1,4}(\\/\\d{2})? ?([AB]C|[AB]\\.C\\.|B?CE|(?:B\\.)?C\\.E\\.)?";

	private static final Pattern PATTERN_RANGE_INFIX = RegexHelper.pattern("(?i) (AND|TO) ");

	private static final Matcher MATCHER_PREFIXES_APPROXIMATIONS = RegexHelper.matcher("(?i)((?:(?:ABT|APP(?:RO)?X|CALC?|EST)\\.?|ABOUT)) ");
	private static final Matcher MATCHER_PREFIXES_RANGE = RegexHelper.matcher("(?i)((?:BET(?:WEEN|\\.)?|BTW\\.?|FROM)) ");
	private static final Matcher MATCHER_INTERPRETED = RegexHelper.matcher("(?i)^INT\\.?\\s+((\\d{1,2} )?([A-Z]{3,4} )?(\\d{1,4}(/\\d{2})?)?)\\s+\\([^)]+\\)$");
	private static final Matcher MATCHER_INTERPRETATION_TEXT = RegexHelper.matcher("\\(([^)]+)\\)$");

	public static final Matcher MATCHER_APPROX = RegexHelper.matcher("(?i)APP(RO)?X\\.?");
	public static final String DESCRIPTION_APPROX = "Approximated";
	public static final String TYPE_APPROX = "APPX";
	public static final Matcher MATCHER_AND = RegexHelper.matcher("(?i)AND");
	public static final String DESCRIPTION_AND = "And";
	public static final String TYPE_AND = "AND";

	/** The regex pattern that identifies two-date range or period. Works for Gregorian, Julian, and Hebrew years */
	private static final Matcher MATCHER_TWO_DATES = RegexHelper.matcher("(?i)^" + FORMAT_RANGE_PREFIX
		+ FORMAT_DAY_MONTH_YEAR + " (AND|TO) " + FORMAT_DAY_MONTH_YEAR + "$");

	private static final Matcher MATCHER_PREFIXES_FROM_TO = RegexHelper.matcher("(?i)((?:FROM|BEF(?:ORE|\\.)?|AFT(?:ER|\\.)?|TO)) ");



	public static enum CalendarType{
		GREGORIAN("Gregorian", CalendarParserBuilder.GREGORIAN_CALENDAR),
		JULIAN("Julian", CalendarParserBuilder.JULIAN_CALENDAR),
		FRENCH_REPUBLICAN("French Republican", CalendarParserBuilder.FRENCH_REPUBLICAN_CALENDAR),
		HEBREW("Hebrew", CalendarParserBuilder.HEBREW_CALENDAR);


		private final String description;
		private final String type;


		public static CalendarType createFromIndex(int index){
			return values()[index];
		}

		public static String[] getDescriptions(){
			return Arrays.stream(values())
				.map(CalendarType::getDescription)
				.toArray(String[]::new);
		}

		public boolean isGregorianOrJulian(){
			return (this == CalendarType.GREGORIAN || this == CalendarType.JULIAN);
		}
	};

	public static enum IntervalType{
		NO_TYPE(StringUtils.EMPTY, StringUtils.EMPTY, null),
		ABOUT("About", "ABT", RegexHelper.matcher("(?i)(?:ABT\\.?|ABOUT|APP(?:RO)?X\\.?)")),
		CALCULATED("Calculated", "CAL", RegexHelper.matcher("(?i)CALC?\\.?")),
		ESTIMATED("Estimated", "EST", RegexHelper.matcher("(?i)EST\\.?")),
		BETWEEN("Between", "BET", RegexHelper.matcher("(?i)(?:(?:BET|BTW)\\.?|BETWEEN)")),
		BEFORE("Before", "BEF", RegexHelper.matcher("(?i)BEF(?:ORE|\\.)?")),
		AFTER("After", "AFT", RegexHelper.matcher("(?i)AFT(?:ER|\\.)?")),
		FROM("From", "FROM", RegexHelper.matcher("(?i)FROM")),
		TO("To", "TO", RegexHelper.matcher("(?i)TO")),
		INTERPRETED("Interpreted", "INT", RegexHelper.matcher("(?i)INT\\.?"));


		private static final Set<IntervalType> QUALIFICATION_NOT_PRESENT_IF = new HashSet<>();
		static{
			QUALIFICATION_NOT_PRESENT_IF.add(IntervalType.ABOUT);
			QUALIFICATION_NOT_PRESENT_IF.add(IntervalType.CALCULATED);
			QUALIFICATION_NOT_PRESENT_IF.add(IntervalType.ESTIMATED);
			QUALIFICATION_NOT_PRESENT_IF.add(IntervalType.INTERPRETED);
		}


		private final String description;
		private final String type;
		private final Matcher matcher;


		public static IntervalType createFromIndex(int index){
			return values()[index];
		}

		public static IntervalType createFromDate(String date){
			for(IntervalType type : values())
				if(type.matcher != null && RegexHelper.find(date, type.matcher))
					return type;
			return NO_TYPE;
		}

		public static String[] getDescriptions(){
			return Arrays.stream(values())
				.map(IntervalType::getDescription)
				.toArray(String[]::new);
		}

		public boolean canQualificationBePresent(){
			return !QUALIFICATION_NOT_PRESENT_IF.contains(this);
		}

		public boolean isRangePresent(){
			return (this == IntervalType.BETWEEN || this == IntervalType.FROM);
		}

		public static String replaceAll(String date){
			for(IntervalType value : values())
				if(value != NO_TYPE)
					date = RegexHelper.replaceAll(date, value.getMatcher(), value.getDescription());
			return date;
		}

		public static String restoreAll(String date){
			for(IntervalType value : values())
				if(value != NO_TYPE)
					date = StringUtils.replaceEachRepeatedly(date, new String[]{value.getDescription()}, new String[]{value.getType()});
			return date;
		}
	};

	public static enum Qualification{
		NO_QUALIFICATION(StringUtils.EMPTY, null),
		ABOUT("About", RegexHelper.matcher("(?i)ABT\\.?|ABOUT|APP(?:RO)?X\\.?")),
		CALCULATED("Calculated", RegexHelper.matcher("(?i)CALC?\\.?")),
		ESTIMATED("Estimated", RegexHelper.matcher("(?i)EST\\.?"));


		private final String description;
		private final Matcher matcher;


		public static Qualification createFromIndex(int index){
			return values()[index];
		}

		public static Qualification createFromDate(String date){
			for(Qualification type : values())
				if(type.matcher != null && RegexHelper.find(date, type.matcher))
					return type;
			return null;
		}

		public static String[] getDescriptions(){
			return Arrays.stream(values())
				.map(Qualification::getDescription)
				.toArray(String[]::new);
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

		CalendarData.CalendarDataBuilder responseBuilder = CalendarData.builder()
			.calendarType(getCalendarType())
			.intervalType(intervalType)
			.interpretedFrom(interpretedFrom);
		if(isRange(date)){
			Pair<String, String> range = getDatesFromRangeOrPeriod(date);
			if(range == null)
				return null;

			DateData fromDate = extractSingleDateComponents(range.getLeft());
			responseBuilder.fromDate(fromDate)
				.fromQualification(fromQualification);
			DateData toDate = extractSingleDateComponents(range.getRight());
			Qualification toQualification = null;
			for(int i = 2; i < rawComponents.length; i ++){
				toQualification = Qualification.createFromDate(rawComponents[i]);
				if(toQualification != null)
					break;
				}
			responseBuilder.toDate(toDate)
				.toQualification(toQualification);
		}
		else{
			DateData singleDate = extractSingleDateComponents(date);
			responseBuilder.fromDate(singleDate)
				.fromQualification(fromQualification);
		}
		return responseBuilder.build();
	}

	protected abstract DateData extractSingleDateComponents(String singleDate);

	public abstract LocalDate parse(String date, DatePreciseness preciseness);

	public abstract int parseMonth(String month);

	public boolean isRange(String date){
		return RegexHelper.matches(date, MATCHER_TWO_DATES);
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
		date = RegexHelper.clear(date, MATCHER_PREFIXES_RANGE);
		return RegexHelper.split(date, PATTERN_RANGE_INFIX);
	}

	/**
	 * Return a version of the string with approximation prefixes removed, including handling for interpreted dates
	 *
	 * @param date	the date string
	 * @return a version of the string with approximation prefixes removed
	 */
	public static boolean isApproximation(String date){
		return RegexHelper.find(date, MATCHER_PREFIXES_APPROXIMATIONS);
	}

	/**
	 * Return a version of the string with approximation prefixes removed, including handling for interpreted dates
	 *
	 * @param date	the date string
	 * @return a version of the string with approximation prefixes removed
	 */
	protected String removeApproximations(String date){
		date = RegexHelper.clear(date, MATCHER_PREFIXES_APPROXIMATIONS);
		date = RegexHelper.replaceFirst(date, MATCHER_INTERPRETED, "$1");
		return date;
	}

	protected String extractInterpretedFrom(String date){
		MATCHER_INTERPRETATION_TEXT.reset(date);
		return (MATCHER_INTERPRETATION_TEXT.find()? MATCHER_INTERPRETATION_TEXT.group(1): null);
	}

	/**
	 * Remove the prefixes for open ended date ranges with only one date (e.g., "BEF 1900", "FROM 1756", "AFT 2000")
	 *
	 * @param date	the date string
	 * @return	the same date string with range/period prefixes removed, but only if it's an open-ended period or range
	 */
	protected String removeOpenEndedRangesAndPeriods(String date){
		return (!RegexHelper.matches(date, MATCHER_TWO_DATES)? RegexHelper.clear(date, MATCHER_PREFIXES_FROM_TO): date);
	}

}
