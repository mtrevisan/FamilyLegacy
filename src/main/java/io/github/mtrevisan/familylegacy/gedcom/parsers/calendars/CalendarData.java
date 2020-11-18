package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;


class CalendarData{

	private AbstractCalendarParser.CalendarType calendarType;
	private boolean afterSunset;
	private AbstractCalendarParser.IntervalType intervalType;
	private DateData fromDate;
	private AbstractCalendarParser.Qualification fromQualification;
	private DateData toDate;
	private AbstractCalendarParser.Qualification toQualification;
	private String interpretedFrom;


	public CalendarData withCalendarType(final AbstractCalendarParser.CalendarType calendarType){
		this.calendarType = calendarType;
		return this;
	}

	public CalendarData withAfterSunset(final boolean afterSunset){
		this.afterSunset = afterSunset;
		return this;
	}

	public CalendarData withIntervalType(final AbstractCalendarParser.IntervalType intervalType){
		this.intervalType = intervalType;
		return this;
	}

	public CalendarData withFromDate(final DateData fromDate){
		this.fromDate = fromDate;
		return this;
	}

	public CalendarData withFromQualification(final AbstractCalendarParser.Qualification fromQualification){
		this.fromQualification = fromQualification;
		return this;
	}

	public CalendarData withToDate(final DateData toDate){
		this.toDate = toDate;
		return this;
	}

	public CalendarData withToQualification(final AbstractCalendarParser.Qualification toQualification){
		this.toQualification = toQualification;
		return this;
	}

	public CalendarData withInterpretedFrom(final String interpretedFrom){
		this.interpretedFrom = interpretedFrom;
		return this;
	}

	public String getDate(){
		//TODO
		String fromMonth = (fromDate.getMonth() != null? GregorianMonth.values()[fromDate.getMonth()].toString(): null);
		GregorianCalendarParser.Era fromEra = fromDate.getEra();
		return calendarType.getType() + intervalType.getType()
			+ fromDate.getDay() + fromMonth + fromDate.getYear()
			+ (fromEra!= null && fromEra == GregorianCalendarParser.Era.BCE? fromEra.getDescription(): StringUtils.EMPTY);
	}

}
