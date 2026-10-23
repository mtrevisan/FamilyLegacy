package io.github.mtrevisan.familylegacy.io.model.readers.date.gregorian;


/**
 * Data container for parsed Gregorian calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Gregorian year
 * @param month The Gregorian month (1 to 12 for standard months, negative integers for astronomical seasons)
 * @param day   The day of the month, or 0 if missing
 */
public record GregorianDateInput(int year, int month, int day){

	/**
	 * @return the day of the month, or the first day of the month if the day is missing
	 */
	public int dayOrFirst(){
		return (day > 0? day: 1);
	}

	/**
	 * Verifies that the 29th of February falls in a leap year.
	 *
	 * @param leapYear whether the year is a leap year in the target calendar
	 * @throws IllegalArgumentException if the date is the 29th of February of a common year
	 */
	public void validateLeapDay(final boolean leapYear){
		if(month == 2 && day == 29 && !leapYear)
			throw new IllegalArgumentException("29 February does not exist in the common year " + year);
	}

}
