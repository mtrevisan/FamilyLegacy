package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import io.github.mtrevisan.familylegacy.services.RegexHelper;

import java.util.regex.Matcher;


class CalendarParserBuilder{

	private static final Matcher MATCHER_CALENDAR_TYPE_PREFIX = RegexHelper.matcher("@#[^@]+?@ ");

	public static final String HEBREW_CALENDAR = "@#DHEBREW@";
	public static final String FRENCH_REPUBLICAN_CALENDAR = "@#DFRENCH R@";
	public static final String GREGORIAN_CALENDAR = "@#DGREGORIAN@";
	public static final String JULIAN_CALENDAR = "@#DJULIAN@";


	public static AbstractCalendarParser getParser(String date){
		AbstractCalendarParser parser;
		if(date.startsWith(GREGORIAN_CALENDAR))
			parser = GregorianCalendarParser.getInstance();
		else if(date.startsWith(JULIAN_CALENDAR))
			parser = JulianCalendarParser.getInstance();
		else if(date.startsWith(FRENCH_REPUBLICAN_CALENDAR))
			parser = FrenchRepublicanCalendarParser.getInstance();
		else if(date.startsWith(HEBREW_CALENDAR))
			parser = HebrewCalendarParser.getInstance();
		else
			parser = GregorianCalendarParser.getInstance();
		return parser;
	}

	public static String removeCalendarType(String date){
		return RegexHelper.clear(date, MATCHER_CALENDAR_TYPE_PREFIX);
	}

}
