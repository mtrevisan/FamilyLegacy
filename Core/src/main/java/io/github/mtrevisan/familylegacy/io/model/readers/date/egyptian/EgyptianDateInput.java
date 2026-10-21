package io.github.mtrevisan.familylegacy.io.model.readers.date.egyptian;


/**
 * Data container for parsed ancient Egyptian civil calendar components.
 *
 * @param year  The Egyptian year (Era of Nabonassar)
 * @param month The Egyptian month (1 to 12 for standard months, 13 for Epagomenal days)
 * @param day   The day of the month (1 to 30 for months 1-12, 1 to 5 for month 13)
 */
public record EgyptianDateInput(int year, int month, int day){}
