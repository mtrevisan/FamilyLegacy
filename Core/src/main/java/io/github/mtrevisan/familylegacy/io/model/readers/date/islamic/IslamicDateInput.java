package io.github.mtrevisan.familylegacy.io.model.readers.date.islamic;


/**
 * Data container for parsed Islamic (Umm al-Qura) calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Hijri year
 * @param month The Islamic month (1 to 12 for standard months, negative integers for astronomical seasons)
 * @param day   The day of the month (or standard operational placeholder)
 */
public record IslamicDateInput(int year, int month, int day){}
