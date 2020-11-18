package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;


/**
 * A class for parsing dates from strings.
 */
class DateParser{

	private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern("d MMM u", Locale.US);


	/**
	 * Parse the string as date, with the default imprecise date handling preference of {@link DatePreciseness#PRECISE}.
	 *
	 * @param date	the date string
	 * @return	the date, if it can be derived from the string
	 */
	public static LocalDate parse(final String date){
		return parse(date, DatePreciseness.PRECISE);
	}

	/**
	 * Parse the string as date.
	 *
	 * @param date				the date string
	 * @param preciseness	the preference for handling an imprecise date.
	 * @return	the date, if one can be derived from the string
	 */
	public static LocalDate parse(final String date, final DatePreciseness preciseness){
		LocalDate dt = null;
		if(StringUtils.isNotBlank(date)){
			final AbstractCalendarParser calendar = CalendarParserBuilder.getParser(date);
			dt = calendar.parse(date, preciseness);
		}
		return dt;
	}

	public static CalendarData toCalendarData(final String date){
		CalendarData data = null;
		if(StringUtils.isNotBlank(date)){
			final AbstractCalendarParser calendarParser = CalendarParserBuilder.getParser(date);
			data = calendarParser.extractComponents(date);
		}
		return data;
	}

	public static String formatDate(String date){
		String formattedDate = null;
		if(StringUtils.isNotBlank(date)){
			date = AbstractCalendarParser.IntervalType.replaceAll(date);
			date = RegexHelper.replaceAll(date, AbstractCalendarParser.PATTERN_APPROX, AbstractCalendarParser.DESCRIPTION_APPROX);
			date = RegexHelper.replaceAll(date, AbstractCalendarParser.PATTERN_AND, AbstractCalendarParser.DESCRIPTION_AND);
			date = RegexHelper.replaceAll(date, GregorianCalendarParser.Era.BCE.getPattern(), GregorianCalendarParser.Era.BCE.getDescription());
			date = GregorianCalendarParser.Era.replaceAll(date);
			date = GregorianMonth.replaceAll(date);
			date = FrenchRepublicanMonth.replaceAll(date);
			date = HebrewMonth.replaceAll(date);
			formattedDate = date;
		}
		return formattedDate;
	}

	public static String unformatDate(String date){
		String formattedDate = null;
		if(StringUtils.isNotBlank(date)){
			date = AbstractCalendarParser.IntervalType.restoreAll(date);
			date = date.replaceAll(AbstractCalendarParser.DESCRIPTION_APPROX, AbstractCalendarParser.TYPE_APPROX);
			date = date.replaceAll(AbstractCalendarParser.DESCRIPTION_AND, AbstractCalendarParser.TYPE_AND);
			date = GregorianCalendarParser.Era.restoreAll(date);
			date = GregorianMonth.restoreAll(date);
			date = FrenchRepublicanMonth.restoreAll(date);
			date = HebrewMonth.restoreAll(date);
			formattedDate = date;
		}
		return formattedDate;
	}

	public static String formatYear(final String date){
		String formattedDate = null;
		LocalDate dt = parse(date);
		if(dt != null)
			formattedDate = (AbstractCalendarParser.isApproximation(date)? "~": StringUtils.EMPTY) + dt.getYear();
		return formattedDate;
	}

}
