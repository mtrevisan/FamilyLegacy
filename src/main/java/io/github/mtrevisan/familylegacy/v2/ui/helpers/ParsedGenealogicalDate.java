package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import java.time.LocalDate;


public record ParsedGenealogicalDate(
	LocalDate isoDate,
	DatePrecision precision,
	String rawInput,
	CalendarType calendarType
)implements Comparable<ParsedGenealogicalDate>{
	@Override
	public int compareTo(final ParsedGenealogicalDate other){
		if(other == null)
			return 1;
		if(this.isoDate == null && other.isoDate == null)
			return 0;
		if(this.isoDate == null)
			return -1;
		if(other.isoDate == null)
			return 1;
		return this.isoDate.compareTo(other.isoDate);
	}

	public enum DatePrecision{
		EXACT,
		YEAR_MONTH,
		MONTH_DAY,
		YEAR_ONLY
	}
}
