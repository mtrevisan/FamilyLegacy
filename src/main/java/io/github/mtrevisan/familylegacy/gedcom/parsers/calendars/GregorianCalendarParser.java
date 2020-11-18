package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;


class GregorianCalendarParser extends AbstractCalendarParser{

	public static final String DOUBLE_ENTRY_YEAR_SEPARATOR = "/";
	private static final Pattern PATTERN_DATE = RegexHelper.pattern("(?i)^((?<day>\\d{1,2}) )?((?<month>[A-Z]+) )?((?<year>\\d{1,4})(\\/(?<doubleEntryYear>\\d{2}))?)?( (?<era>[ABCE.]+))?$");

	private static final List<String> BEFORE_COMMON_ERA = Arrays.asList("BC", "B.C.", "BCE", "B.C.E.");

	public static enum Era{
		CE("CE", RegexHelper.pattern("(?i)A\\.?C\\.?|(^|[^B.])C\\.?E\\.?")),
		BCE("BCE", RegexHelper.pattern("(?i)B\\.?C\\.?(E\\.?)?"));


		private final String description;
		private final Pattern pattern;


		public static Era createFromIndex(int index){
			return values()[index];
		}

		public static Era createFromDate(String date){
			for(Era type : values())
				if(RegexHelper.find(date, type.pattern))
					return type;
			return null;
		}

		Era(final String description, final Pattern pattern){
			this.description = description;
			this.pattern = pattern;
		}

		public static String[] getDescriptions(){
			final List<String> list = new ArrayList<>();
			for(final Era era : values())
				list.add(era.description);
			return list.toArray(new String[0]);
		}

		public static String replaceAll(String era){
			era = RegexHelper.replaceAll(era, BCE.pattern, BCE.description);
			era = RegexHelper.replaceAll(era, CE.pattern, StringUtils.EMPTY);
			return era;
		}

		public static String restoreAll(String era){
			return era.replaceAll(CE.description, StringUtils.EMPTY);
		}
	};


	private static class SingletonHelper{
		private static final AbstractCalendarParser INSTANCE = new GregorianCalendarParser();
	}


	public static AbstractCalendarParser getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public CalendarType getCalendarType(){
		return CalendarType.GREGORIAN;
	}

	@Override
	protected DateData extractSingleDateComponents(String singleDate){
		singleDate = CalendarParserBuilder.removeCalendarType(singleDate);

		DateData.DateDataBuilder dateBuilder = DateData.builder();
		PATTERN_DATE.reset(singleDate);
		if(PATTERN_DATE.find()){
			String day = PATTERN_DATE.group("day");
			if(StringUtils.isNotBlank(day))
				dateBuilder.day(Integer.parseInt(day));
			String month = PATTERN_DATE.group("month");
			if(StringUtils.isNotBlank(month))
				dateBuilder.month(GregorianMonth.createFromAbbreviation(month).ordinal());
			String year = PATTERN_DATE.group("year");
			if(StringUtils.isNotBlank(year))
				dateBuilder.year(Integer.parseInt(year));
			String doubleEntryYear = PATTERN_DATE.group("doubleEntryYear");
			if(StringUtils.isNotBlank(doubleEntryYear))
				dateBuilder.doubleEntryYear(Integer.parseInt(doubleEntryYear));
			String era = PATTERN_DATE.group("era");
			if(StringUtils.isNotBlank(era))
				dateBuilder.era(Era.createFromDate(era));
		}
		return dateBuilder.build();
	}

	/**
	 * Parse a Gregorian/Julian/unknown date string
	 *
	 * @param date				the date string to parse
	 * @param preciseness	the preference for handling an imprecise date.
	 * @return	the date, if one can be derived from the string
	 */
	@Override
	public LocalDate parse(String date, DatePreciseness preciseness){
		date = CalendarParserBuilder.removeCalendarType(date);

		date = removeApproximations(date);
		date = removeOpenEndedRangesAndPeriods(date);

		return (isRange(date)? getDateFromRangeOrPeriod(date, preciseness): getDate(date, preciseness));
	}

	private LocalDate getDate(String date, DatePreciseness preciseness) throws IllegalArgumentException{
		LocalDate dt = null;
		PATTERN_DATE.reset(date);
		if(PATTERN_DATE.find()){
			String day = PATTERN_DATE.group("day");
			String month = PATTERN_DATE.group("month");
			String year = PATTERN_DATE.group("year");
			String doubleEntryYear = PATTERN_DATE.group("doubleEntryYear");
			String era = PATTERN_DATE.group("era");
			
			try{
				year = resolveEnglishCalendarSwitchYear(year, doubleEntryYear);
				
				int y = Integer.parseInt(year);
				if(BEFORE_COMMON_ERA.contains(era))
					y = 1 - y;
				int m = (month != null? GregorianMonth.createFromAbbreviation(month).ordinal() + 1: 1);
				int d = (day != null? Integer.parseInt(day): 1);
				
				try{
					dt = LocalDate.of(y, m, d);
				}
				catch(DateTimeException e){
					if(m == 2 && d == 29)
						dt = LocalDate.of(y, 3, 1);
				}
				
				dt = managePreciseness(day, month, dt, preciseness);
			}
			catch(NullPointerException ignored){}
		}
		return dt;
	}

	/**
	 * <p>
	 * Resolve a date in double-dated format, for the old/new dates preceding the English calendar switch of 1752.
	 * </p>
	 * <p>
	 * Because of the switch to the Gregorian Calendar in 1752 in England and its colonies, and the corresponding change of the
	 * first day of the year, it's not uncommon for dates in the range between 1582 and 1752 to be written using a double-dated
	 * format, showing the old and new dates simultaneously. For example, today we would render George Washington's birthday in
	 * GEDCOM format as <code>22 FEB 1732</code>. However, in 1760 or so, one might have written it as Feb 22 1731/32, thus be
	 * entered into a GEDCOM field as <code>22 FEB 1731/32</code>.
	 * </p>
	 *
	 * @param year					the year.
	 * @param doubleEntryYear	the double-entry for the year.
	 * @return	the year, resolved to a Gregorian year
	 */
	private String resolveEnglishCalendarSwitchYear(String year, String doubleEntryYear){
		int y = Integer.parseInt(year);
		if(1582 <= y && y <= 1752 && doubleEntryYear != null){
			int yy = Integer.parseInt(doubleEntryYear);
			//handle century boundary
			if(yy == 0 && y % 100 == 99)
				y ++;

			year = Integer.toString(y / 100) + doubleEntryYear;
		}
		return year;
	}

	private LocalDate managePreciseness(String day, String month, LocalDate d, DatePreciseness preciseness) throws IllegalArgumentException{
		if(preciseness == null)
			throw new IllegalArgumentException("Unknown value for date handling preference: " + preciseness);

		if(d != null && day == null)
			d = (month != null? preciseness.applyToMonth(d): preciseness.applyToYear(d));
		return d;
	}

	@Override
	public int parseMonth(String month){
		GregorianMonth m = GregorianMonth.createFromAbbreviation(month);
		return (m != null? m.ordinal(): -1);
	}

}
