package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import io.github.mtrevisan.familylegacy.services.RegexHelper;

import java.util.regex.Pattern;


class CalendarParserBuilder{

	private static final Pattern PATTERN_CALENDAR_TYPE_PREFIX = RegexHelper.pattern("@#D[^@]+@ ");

	public static final String CALENDAR_HEBREW = "@#DHEBREW@";
	public static final String CALENDAR_FRENCH_REPUBLICAN = "@#DFRENCH R@";
	public static final String CALENDAR_GREGORIAN = "@#DGREGORIAN@";
	public static final String CALENDAR_JULIAN = "@#DJULIAN@";


	public static AbstractCalendarParser getParser(final String date){
		AbstractCalendarParser parser;
		if(date.startsWith(CALENDAR_GREGORIAN))
			parser = GregorianCalendarParser.getInstance();
		else if(date.startsWith(CALENDAR_JULIAN))
			parser = JulianCalendarParser.getInstance();
		else if(date.startsWith(CALENDAR_FRENCH_REPUBLICAN))
			parser = FrenchRepublicanCalendarParser.getInstance();
		else if(date.startsWith(CALENDAR_HEBREW))
			parser = HebrewCalendarParser.getInstance();
		else
			parser = GregorianCalendarParser.getInstance();
		return parser;
	}

	public static String removeCalendarType(final String date){
		return RegexHelper.clear(date, PATTERN_CALENDAR_TYPE_PREFIX);
	}

}
