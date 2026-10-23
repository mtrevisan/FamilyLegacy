/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.calendars.eastasian;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.util.Map;


/**
 * Japanese dates counted in the years of an era (元号), as written in the family registers (koseki), from Tenshō
 * (1573) onward, e.g. "明治45年7月29日", "天保十四年閏九月十日", "Shōwa 20 8 15", "H1.1.8".
 * <ul>
 *   <li>From 1 January 1873 (明治6年) the months and days are Gregorian.</li>
 *   <li>From 1685 (the Jōkyō calendar) to 31 December 1872 (明治5年12月2日) they are lunisolar, converted with the
 *   month starts published by the National Astronomical Observatory of Japan (see {@link JapaneseLunisolarTable}),
 *   including the leap months (閏) and the last day of the month (晦日).</li>
 *   <li>The earlier dates belong to the Senmyō calendar, whose days cannot be reconstructed reliably, and are rejected.</li>
 * </ul>
 */
public class JapaneseCalendarStrategy implements CalendarConverterPlugin{

	private static final Map<String, String> ERA_ALIASES = JapaneseEra.aliases();

	/** Japan adopted the Gregorian calendar on 1 January 1873 (Meiji 6). */
	private static final int GREGORIAN_ADOPTION_YEAR = 1873;
	private static final LocalDate GREGORIAN_ADOPTION = LocalDate.of(GREGORIAN_ADOPTION_YEAR, 1, 1);


	@Override
	public long parseToJdn(final String rawDate){
		final EastAsianDateInput input = EastAsianEraDateParser.parse(rawDate, ERA_ALIASES, null);
		final JapaneseEra era = JapaneseEra.valueOf(input.era());
		final int year = era.toYear(input.year());
		if(year > era.getLastYear())
			throw new IllegalArgumentException("The era " + era.getKanji() + " lasted until the year "
				+ (era.getLastYear() - era.getFirstYear() + 1) + ": " + rawDate);

		return (year >= GREGORIAN_ADOPTION_YEAR
			? gregorianToJdn(era, input, year, rawDate)
			: lunisolarToJdn(input, year, rawDate));
	}

	private static long lunisolarToJdn(final EastAsianDateInput input, final int lunarYear, final String rawDate){
		if(lunarYear < JapaneseLunisolarTable.firstYear())
			throw new IllegalArgumentException("Japanese dates before 1685 belong to the Senmyō calendar, whose days cannot be"
				+ " reconstructed reliably: " + rawDate);

		return JapaneseLunisolarTable.toJdn(lunarYear, input.monthOrFirst(), input.leapMonth(), input.dayOrFirst());
	}

	private static long gregorianToJdn(final JapaneseEra era, final EastAsianDateInput input, final int year,
			final String rawDate){
		if(input.leapMonth())
			throw new IllegalArgumentException("There are no leap months in the Gregorian calendar: " + rawDate);

		final LocalDate eraStart = eraStart(era);
		final LocalDate date;
		try{
			final int month = input.monthOrFirst();
			final int day = (input.day() == EastAsianDateInput.LAST_DAY
				? LocalDate.of(year, month, 1).lengthOfMonth()
				: input.dayOrFirst());
			LocalDate candidate = LocalDate.of(year, month, day);
			// a year or a month alone of the first year of an era starts at the accession
			if(input.day() == 0 && candidate.isBefore(eraStart)
					&& (input.month() == 0 || candidate.getMonthValue() == eraStart.getMonthValue()))
				candidate = eraStart;
			date = candidate;
		}
		catch(final DateTimeException dte){
			throw new IllegalArgumentException("Invalid Japanese date: " + rawDate, dte);
		}

		if(date.isBefore(eraStart) || !date.isBefore(GREGORIAN_ADOPTION) && JapaneseDate.from(date).getEra() != toChronologyEra(era))
			throw new IllegalArgumentException("The date does not belong to the era " + era.getKanji() + ": " + rawDate);

		return GregorianCalendarStrategy.toJdn(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
	}

	/**
	 * Accession day of a modern era (Meiji is counted from 1 January 1873 in the Gregorian calendar).
	 */
	private static LocalDate eraStart(final JapaneseEra era){
		if(era == JapaneseEra.MEIJI)
			return GREGORIAN_ADOPTION;

		return LocalDate.from(JapaneseChronology.INSTANCE.dateYearDay(toChronologyEra(era), 1, 1));
	}

	private static java.time.chrono.JapaneseEra toChronologyEra(final JapaneseEra era){
		return switch(era){
			case MEIJI -> java.time.chrono.JapaneseEra.MEIJI;
			case TAISHO -> java.time.chrono.JapaneseEra.TAISHO;
			case SHOWA -> java.time.chrono.JapaneseEra.SHOWA;
			case HEISEI -> java.time.chrono.JapaneseEra.HEISEI;
			case REIWA -> java.time.chrono.JapaneseEra.REIWA;
			default -> throw new IllegalArgumentException("The era " + era.getKanji() + " predates the Gregorian calendar");
		};
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		return EastAsianEraDateParser.parse(rawDate, ERA_ALIASES, null)
			.toMetadata();
	}

}
