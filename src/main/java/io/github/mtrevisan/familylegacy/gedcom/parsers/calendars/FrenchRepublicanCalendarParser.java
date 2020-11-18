package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.time.LocalDate;
import java.time.Month;
import java.util.regex.Matcher;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;


/**
 * Class for parsing French Republican Calendar dates. Calculations based on Romme method. Only supports dates on or after September
 * 22, 1792 (Gregorian).
 */
class FrenchRepublicanCalendarParser extends AbstractCalendarParser{

	/** Pattern for matching a single French Republican date in GEDCOM format */
	private static final Matcher MATCHER_DATE = RegexHelper.matcher("(?i)^((?<day>\\d{1,2}) )?((?<month>[A-Z]+) )?(?<year>\\d{1,4})?$");


	private static class SingletonHelper{
		private static final AbstractCalendarParser INSTANCE = new FrenchRepublicanCalendarParser();
	}


	public static AbstractCalendarParser getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public CalendarType getCalendarType(){
		return CalendarType.FRENCH_REPUBLICAN;
	}

	@Override
	protected DateData extractSingleDateComponents(String singleDate){
		singleDate = CalendarParserBuilder.removeCalendarType(singleDate);

		DateData.DateDataBuilder dateBuilder = DateData.builder();
		MATCHER_DATE.reset(singleDate);
		if(MATCHER_DATE.find()){
			String day = MATCHER_DATE.group("day");
			if(StringUtils.isNotBlank(day))
				dateBuilder.day(Integer.parseInt(day));
			String month = MATCHER_DATE.group("month");
			if(StringUtils.isNotBlank(month))
				dateBuilder.month(Integer.parseInt(month));
			String year = MATCHER_DATE.group("year");
			if(StringUtils.isNotBlank(year))
				dateBuilder.year(Integer.parseInt(year));
		}
		return dateBuilder.build();
	}

	/**
	 * Convert a French Republican date string (in proper GEDCOM format) to a (Gregorian) java.time.LocalDate.
	 *
	 * @param date				the French Republican date in GEDCOM spec format - see DATE_FREN and MONTH_FREN in the spec. Could be a single
	 *								date, an approximate date, a date range, or a date period.
	 * @param preciseness	preference on how to handle imprecise dates - return the earliest day of the month, the latest, or the midpoint
	 * @return	the Gregorian date that represents the French Republican date supplied
	 */
	@Override
	public LocalDate parse(String date, DatePreciseness preciseness){
		date = CalendarParserBuilder.removeCalendarType(date);
		date = removeApproximations(date);
		date = removeOpenEndedRangesAndPeriods(date);

		return (isRange(date)? getDateFromRangeOrPeriod(date, preciseness): getDate(date, preciseness));
	}

	private LocalDate getDate(String date, DatePreciseness preciseness){
		LocalDate dt = null;
		MATCHER_DATE.reset(date);
		if(MATCHER_DATE.find()){
			String day = MATCHER_DATE.group("day");
			String month = MATCHER_DATE.group("month");
			String year = MATCHER_DATE.group("year");

			try{
				int y = Integer.parseInt(year);
				FrenchRepublicanMonth m = (month != null? FrenchRepublicanMonth.createFromAbbreviation(month): null);
				Integer d = (day != null? Integer.parseInt(day): null);
				
				dt = managePreciseness(y, m, d, preciseness);
			}
			catch(NullPointerException ignored){}
		}
		return dt;
	}

	private LocalDate managePreciseness(int year, FrenchRepublicanMonth month, Integer day, DatePreciseness preciseness) throws IllegalArgumentException{
		if(preciseness == null)
			throw new IllegalArgumentException("Unknown value for date handling preference: " + preciseness);

		if(day == null){
			switch(preciseness){
				case FAVOR_LATEST:
					if(month == null){
						month = FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS;
						day = (isLeapYear(year)? 6: 5);
					}
					else if(month == FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS)
						day = (isLeapYear(year)? 6: 5);
					else
						day = 30;
					break;

				case FAVOR_MIDPOINT:
					if(month == null){
						month = FrenchRepublicanMonth.GERMINAL;
						day = 1;
					}
					else if(month == FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS)
						day = (isLeapYear(year)? 3: 2);
					else
						day = 15;
					break;

				case PRECISE:
				case FAVOR_EARLIEST:
					if(month == null)
						month = FrenchRepublicanMonth.VENDEMIAIRE;
					day = 1;
			}
		}
		return convertToGregorian(year, month, day);
	}

	/**
	 * This function converts a French Republican date into the Gregorian date.
	 *
	 * @param year		the French Republican year (french year 1 corresponds to Gregorian year 1792)
	 * @param month	the French Republican month abbreviation in GEDCOM format
	 * @param day		the day within the month
	 * @return the date in Gregorian form
	 */
	private LocalDate convertToGregorian(int year, FrenchRepublicanMonth month, int day){
		if(year < 1 || month == null || day < 1
				//there were never more than 6 days in Jour Complementairs, and that was only on leap years
				|| month == FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS && day > 6
				//all the other months had 30 days
				|| month != FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS && day > 30)
			return null;

		//start just before beginning of French Republican time - 21 SEP 1792
		LocalDate date = LocalDate.of(1792, Month.SEPTEMBER, 21);

		//add years already passed
		for(int i = 1; i <= year - 1; i ++){
			//365 days per year
			date = date.plusDays(365);

			if(isLeapYear(i))
				//add a leap day
				date = date.plusDays(1);
		}

		//add 30 days per month
		date = date.plusDays(30 * ((Enum)month).ordinal() + day);

		return date;
	}

	/**
	 * Is the French Republican year supplied a French Leap Year? Uses the Romme rule, which says:
	 * <ul>
	 * <li>Years III, VII, XI, XV, and XX are to be leap years</li>
	 * <li>After that, every four years; <strong>but</strong> if the French year is divisible by 100 it must also be divisible by
	 * 400 to be a leap year (much like Gregorian).
	 * </ul>
	 *
	 * @param year	the French Republican Year
	 * @return true if it's a French Leap Year.
	 */
	private boolean isLeapYear(int year){
		return (year == 3 || year == 7 || year == 11 || year == 15
			|| year >= 20 && year % 4 == 0 && (year % 100 != 0 || year % 400 == 0));
	}

	@Override
	public int parseMonth(String month){
		FrenchRepublicanMonth m = FrenchRepublicanMonth.createFromAbbreviation(month);
		return (m != null? m.ordinal(): -1);
	}

}
