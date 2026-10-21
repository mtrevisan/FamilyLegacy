package io.github.mtrevisan.familylegacy.io.model.readers.date.rumi;


/**
 * Data container for parsed Ottoman Rumi calendar components.
 *
 * @param year  The Rumi year (Maliye fiscal era)
 * @param month The Rumi month (1 = March before reform / January after reform)
 * @param day   The day of the month
 * @author Mauro Trevisan
 */
public record RumiDateInput(int year, int month, int day){}
