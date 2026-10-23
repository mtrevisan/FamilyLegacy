package io.github.mtrevisan.calendars.sovieteternal;


/**
 * Data container for parsed Soviet Revolutionary (Eternal) calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Soviet/Gregorian calendar year
 * @param month The Soviet month (1 to 12 for standard months, negative integers for astronomical seasons)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record SovietEternalDateInput(int year, int month, int day){}
