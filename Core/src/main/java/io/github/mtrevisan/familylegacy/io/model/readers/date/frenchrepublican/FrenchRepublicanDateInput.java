package io.github.mtrevisan.familylegacy.io.model.readers.date.frenchrepublican;


/**
 * Data container for parsed French Republican calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The French Republican year (Year 1 started in 1792 CE)
 * @param month The Revolutionary month (1 to 12 for standard months, 13 for Sansculottides, negative integers for seasons)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record FrenchRepublicanDateInput(int year, int month, int day){}
