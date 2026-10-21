package io.github.mtrevisan.familylegacy.io.model.readers.date.seleucid;


/**
 * Data container for parsed Seleucid (Syro-Macedonian) calendar components.
 *
 * @param year  The Seleucid year (Anno Graecorum)
 * @param month The Seleucid month (1 = Hyperberetaios/October, 12 = Gorpiose/September)
 * @param day   The day of the month
 */
public record SeleucidDateInput(int year, int month, int day){}
