package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.regex.Matcher;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;


class JulianCalendarParser extends GregorianCalendarParser{

	private static final Matcher MATCHER_DATE = RegexHelper.matcher("(?i)^((?<day>\\d{1,2}) )?((?<month>[A-Z]+) )?(?<year>\\d{1,4})?( (?<era>[ABCE.]+))?$");


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

		DateData.DateDataBuilder dateBuilder = DateData.builder();
		MATCHER_DATE.reset(singleDate);
		if(MATCHER_DATE.find()){
			String day = MATCHER_DATE.group("day");
			if(StringUtils.isNotBlank(day))
				dateBuilder.day(Integer.parseInt(day));
			String month = MATCHER_DATE.group("month");
			if(StringUtils.isNotBlank(month))
				dateBuilder.month(GregorianMonth.createFromAbbreviation(month).ordinal());
			String year = MATCHER_DATE.group("year");
			if(StringUtils.isNotBlank(year))
				dateBuilder.year(Integer.parseInt(year));
			String era = MATCHER_DATE.group("era");
			if(StringUtils.isNotBlank(era))
				dateBuilder.era(Era.createFromDate(era));
		}
		return dateBuilder.build();
	}

}
