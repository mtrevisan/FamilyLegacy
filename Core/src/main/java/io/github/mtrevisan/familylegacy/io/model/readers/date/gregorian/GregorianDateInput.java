package io.github.mtrevisan.familylegacy.io.model.readers.date.gregorian;


/**
 * Data container for parsed Gregorian calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Gregorian year
 * @param month The Gregorian month (1 to 12 for standard months, negative integers for astronomical seasons)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record GregorianDateInput(int year, int month, int day){}
