package io.github.mtrevisan.calendars.ethiopian;


/**
 * Data container for parsed Ethiopian calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Ethiopian year (Era of Incarnation / Amete Mihret)
 * @param month The Ethiopian month (1 to 12 for standard months, 13 for Pagumēn, negative integers for seasons)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record EthiopianDateInput(int year, int month, int day){}
