package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;


class CalendarData{

	private final AbstractCalendarParser.CalendarType calendarType;
	private final boolean afterSunset;
	private final AbstractCalendarParser.IntervalType intervalType;
	private final DateData fromDate;
	private final AbstractCalendarParser.Qualification fromQualification;
	private final DateData toDate;
	private final AbstractCalendarParser.Qualification toQualification;
	private final String interpretedFrom;


	public String getDate(){
		//TODO
		String fromMonth = (fromDate.getMonth() != null? GregorianMonth.values()[fromDate.getMonth()].toString(): null);
		GregorianCalendarParser.Era fromEra = fromDate.getEra();
		return calendarType.getType() + intervalType.getType()
			+ fromDate.getDay() + fromMonth + fromDate.getYear()
			+ (fromEra!= null && fromEra == GregorianCalendarParser.Era.BCE? fromEra.getDescription(): StringUtils.EMPTY);
	}

}
