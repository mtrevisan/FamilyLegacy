package io.github.mtrevisan.familylegacy.io.model.readers.date.coptic;


/**
 * Data container for parsed Coptic calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Coptic year (Era of Martyrs / Anno Martyrum)
 * @param month The Coptic month (1 to 12 for standard months, 13 for Pi Kogi Enavot, negative integers for seasons)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record CopticDateInput(int year, int month, int day){}
