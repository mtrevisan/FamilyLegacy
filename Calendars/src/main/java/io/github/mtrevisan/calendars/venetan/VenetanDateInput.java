package io.github.mtrevisan.calendars.venetan;


/**
 * Data container for parsed historical Venetian (more Veneto) calendar components.
 *
 * @param year  The Venetian year (starts on March 1st)
 * @param month The Venetian month (1 = March, 12 = February)
 * @param day   The day of the month
 * @param indiction The indiction written next to the year (1 to 15), or 0 if missing
 */
public record VenetanDateInput(int year, int month, int day, int indiction){}
