package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;


class DateData{

	private final Integer day;
	private final Integer month;
	private final Integer year;
	private final Integer doubleEntryYear;
	private final GregorianCalendarParser.Era era;


	public boolean isEmpty(){
		return (day == null && month == null && year == null && doubleEntryYear == null && era == null);
	}

}
