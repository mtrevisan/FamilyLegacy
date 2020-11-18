package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;


class DateData{

	private Integer day;
	private Integer month;
	private Integer year;
	private Integer doubleEntryYear;
	private GregorianCalendarParser.Era era;


	public Integer getDay(){
		return day;
	}

	public DateData withDay(final Integer day){
		this.day = day;
		return this;
	}

	public Integer getMonth(){
		return month;
	}

	public DateData withMonth(final Integer month){
		this.month = month;
		return this;
	}

	public Integer getYear(){
		return year;
	}

	public DateData withYear(final Integer year){
		this.year = year;
		return this;
	}

	public Integer getDoubleEntryYear(){
		return doubleEntryYear;
	}

	public DateData withDoubleEntryYear(final Integer doubleEntryYear){
		this.doubleEntryYear = doubleEntryYear;
		return this;
	}

	public GregorianCalendarParser.Era getEra(){
		return era;
	}

	public DateData withEra(final GregorianCalendarParser.Era era){
		this.era = era;
		return this;
	}

	public boolean isEmpty(){
		return (day == null && month == null && year == null && doubleEntryYear == null && era == null);
	}

}
