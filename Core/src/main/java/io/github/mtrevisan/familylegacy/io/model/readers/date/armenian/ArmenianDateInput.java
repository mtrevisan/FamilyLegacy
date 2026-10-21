package io.github.mtrevisan.familylegacy.io.model.readers.date.armenian;


/**
 * Data container for parsed traditional Armenian calendar components.
 *
 * @param year  The Armenian year (Great Armenian Era / Thvakan)
 * @param month The Armenian month (1 to 12 for standard months, 13 for Avelyats days)
 * @param day   The day of the month (1 to 30 for months 1-12, 1 to 5 for month 13)
 */
public record ArmenianDateInput(int year, int month, int day){}
