package io.github.mtrevisan.familylegacy.io.model.readers.date.indian;


/**
 * Data container for parsed Indian National (Saka Samvat) calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Saka year (Gregorian year - 78)
 * @param month The Saka month (1 to 12 for standard months, negative integers for traditional Ritus)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record IndianDateInput(int year, int month, int day){}
