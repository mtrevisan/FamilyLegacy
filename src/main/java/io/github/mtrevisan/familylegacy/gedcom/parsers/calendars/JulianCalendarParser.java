package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.regex.Pattern;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;


class JulianCalendarParser extends GregorianCalendarParser{

	private static final Pattern PATTERN_DATE = RegexHelper.pattern("(?i)^((?<day>\\d{1,2}) )?((?<month>[A-Z]+) )?(?<year>\\d{1,4})?( (?<era>[ABCE.]+))?$");


	private static class SingletonHelper{
		private static final AbstractCalendarParser INSTANCE = new JulianCalendarParser();
	}


	public static AbstractCalendarParser getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public CalendarType getCalendarType(){
		return CalendarType.JULIAN;
	}

	@Override
	protected DateData extractSingleDateComponents(String singleDate){
		singleDate = CalendarParserBuilder.removeCalendarType(singleDate);

		DateData date = new DateData();
		PATTERN_DATE.reset(singleDate);
		if(PATTERN_DATE.find()){
			String day = PATTERN_DATE.group("day");
			if(StringUtils.isNotBlank(day))
				date.withDay(Integer.parseInt(day));
			String month = PATTERN_DATE.group("month");
			if(StringUtils.isNotBlank(month))
				date.withMonth(GregorianMonth.createFromAbbreviation(month).ordinal());
			String year = PATTERN_DATE.group("year");
			if(StringUtils.isNotBlank(year))
				date.withYear(Integer.parseInt(year));
			String era = PATTERN_DATE.group("era");
			if(StringUtils.isNotBlank(era))
				date.withEra(Era.createFromDate(era));
		}
		return date;
	}

}
