package io.github.mtrevisan.familylegacy.io.model.readers.date.hebrew;


/**
 * Data container for parsed Hebrew calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Hebrew year (Anno Mundi)
 * @param month The Hebrew month (1 to 13, negative integers for astronomical Tekufot seasons)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record HebrewDateInput(int year, int month, int day){}
