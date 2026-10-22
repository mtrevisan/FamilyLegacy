package io.github.mtrevisan.familylegacy.io.model.readers.date.buddhist;


/**
 * Data container for parsed Buddhist calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Buddhist calendar year (Gregorian year + 543)
 * @param month The Buddhist month (1 to 12 for standard months, negative integers for astronomical seasons)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record BuddhistDateInput(int year, int month, int day){}
