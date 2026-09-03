package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import java.time.LocalDate;


/**
 * Represents a parsed genealogical date with precision and approximation metadata.
 *
 * @param isoDate      the normalized ISO date
 * @param precision    the temporal precision (e.g., EXACT, YEAR_ONLY)
 * @param approximate  whether the date is marked as approximate/uncertain
 * @param rawInput     the original raw string input
 * @param calendarType the calendar system used
 */
public record ParsedGenealogicalDate(
	LocalDate isoDate,
	DatePrecision precision,
	boolean approximate,
	String rawInput,
	CalendarType calendarType
)implements Comparable<ParsedGenealogicalDate>{

	/** Regular expression matching common genealogical approximation indicators. */
	public static final String APPROXIMATION_REGEX = "(?i)\\b(circa|ca\\.?|c\\.?|abt\\.?|about|approx\\.?)\\b|[?~]";


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
