package io.github.mtrevisan.familylegacy.io.model.readers.date.byzantine;


/**
 * Data container for parsed Byzantine calendar components.
 *
 * @param year  The Byzantine year (Anno Mundi)
 * @param month The Byzantine month (1 = September, 12 = August)
 * @param day   The day of the month
 */
public record ByzantineDateInput(int year, int month, int day){}
