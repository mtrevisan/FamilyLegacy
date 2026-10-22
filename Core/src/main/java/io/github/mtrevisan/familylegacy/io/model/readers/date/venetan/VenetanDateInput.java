package io.github.mtrevisan.familylegacy.io.model.readers.date.venetan;


/**
 * Data container for parsed historical Venetian (more Veneto) calendar components.
 *
 * @param year  The Venetian year (starts on March 1st)
 * @param month The Venetian month (1 = March, 12 = February)
 * @param day   The day of the month
 */
public record VenetanDateInput(int year, int month, int day){}
