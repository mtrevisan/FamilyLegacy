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
package io.github.mtrevisan.calendars.islamic;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;


/**
 * The official Umm al-Qura month starts for 1300-1600 AH, loaded from the resource {@code umm-al-qura.csv}.
 * <p>
 * The table is the one of the King Abdulaziz City for Science and Technology (KACST), as published by umalqura.org.sa and
 * distributed in the JDK (resource {@code hijrah-config-Hijrah-umalqura.properties} of {@code java.time.chrono.HijrahChronology});
 * the runtime does not depend on that class, the data is embedded. Inside this range the table is authoritative, outside it
 * the calendar falls back to the astronomical criterion of {@link IslamicCalendarAstronomicalEngine}.
 * </p>
 */
final class UmmAlQuraTable{

	static final int FIRST_YEAR = 1300;
	static final int LAST_YEAR = 1600;

	private static final String RESOURCE = "umm-al-qura.csv";
	private static final int MONTHS_PER_YEAR = 12;
	private static final int YEARS = LAST_YEAR - FIRST_YEAR + 1;
	private static final int MIN_YEAR_DAYS = 354;
	private static final int MAX_YEAR_DAYS = 355;

	/** The first day of each month, plus the first day after the last month. */
	private static final long[] MONTH_STARTS = load();


	private UmmAlQuraTable(){}


	/**
	 * @param year the Islamic year
	 * @return whether the official table covers the year
	 */
	static boolean covers(final int year){
		return (year >= FIRST_YEAR && year <= LAST_YEAR);
	}

	/**
	 * @param year  the Islamic year, within {@link #FIRST_YEAR} and {@link #LAST_YEAR}
	 * @param month the Islamic month (1 to 12)
	 * @return the Julian Day Number of the first day of the month
	 */
	static long monthStartJdn(final int year, final int month){
		return MONTH_STARTS[(year - FIRST_YEAR) * MONTHS_PER_YEAR + month - 1];
	}

	/**
	 * @param year  the Islamic year, within {@link #FIRST_YEAR} and {@link #LAST_YEAR}
	 * @param month the Islamic month (1 to 12)
	 * @return the number of days of the month (29 or 30)
	 */
	static int monthLength(final int year, final int month){
		final int index = (year - FIRST_YEAR) * MONTHS_PER_YEAR + month - 1;
		return (int)(MONTH_STARTS[index + 1] - MONTH_STARTS[index]);
	}

	private static long[] load(){
		final long[] starts = new long[YEARS * MONTHS_PER_YEAR + 1];
		try(final InputStream is = UmmAlQuraTable.class.getResourceAsStream(RESOURCE)){
			if(is == null)
				throw new IllegalStateException("Missing resource " + RESOURCE);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			int expectedYear = FIRST_YEAR;
			long nextYearStart = -1;
			int lineNumber = 0;
			String line;
			while((line = reader.readLine()) != null){
				lineNumber ++;
				if(line.isBlank() || line.startsWith("#"))
					continue;

				if(expectedYear > LAST_YEAR)
					throw error(lineNumber, "more years than the last one, " + LAST_YEAR);
				final String[] fields = StringUtils.splitPreserveAllTokens(line, ',');
				if(fields.length != 3 || fields[2].length() != MONTHS_PER_YEAR)
					throw error(lineNumber, "expected year,startJdn,12 month lengths");

				final int year;
				final long start;
				try{
					year = Integer.parseInt(fields[0].trim());
					start = Long.parseLong(fields[1].trim());
				}
				catch(final NumberFormatException nfe){
					throw error(lineNumber, "not a number: " + nfe.getMessage());
				}
				if(year != expectedYear)
					throw error(lineNumber, "year " + year + " instead of " + expectedYear);
				if(nextYearStart >= 0 && start != nextYearStart)
					throw error(lineNumber, "year " + year + " starts at " + start + " instead of " + nextYearStart
						+ " (the day after the previous year)");

				long monthStart = start;
				final int base = (year - FIRST_YEAR) * MONTHS_PER_YEAR;
				for(int month = 0; month < MONTHS_PER_YEAR; month ++){
					starts[base + month] = monthStart;
					final char length = fields[2].charAt(month);
					if(length != '0' && length != '1')
						throw error(lineNumber, "month length must be 0 (29 days) or 1 (30 days): " + length);
					monthStart += 29 + (length - '0');
				}
				final long days = monthStart - start;
				if(days < MIN_YEAR_DAYS || days > MAX_YEAR_DAYS)
					throw error(lineNumber, "year " + year + " has " + days + " days");

				nextYearStart = monthStart;
				expectedYear ++;
			}
			if(expectedYear != LAST_YEAR + 1)
				throw new IllegalStateException("The resource " + RESOURCE + " ends at year " + (expectedYear - 1) + " instead of "
					+ LAST_YEAR);
			starts[starts.length - 1] = nextYearStart;
		}
		catch(final IOException ioe){
			throw new UncheckedIOException(ioe);
		}
		return starts;
	}

	private static IllegalStateException error(final int lineNumber, final String message){
		return new IllegalStateException("Invalid " + RESOURCE + " at line " + lineNumber + ": " + message);
	}

}
