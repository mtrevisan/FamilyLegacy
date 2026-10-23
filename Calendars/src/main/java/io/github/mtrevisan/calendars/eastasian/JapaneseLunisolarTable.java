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

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


/**
 * The Japanese lunisolar calendar from 1685 (the Jōkyō calendar) to its end on 31 December 1872 (明治5年12月2日), as
 * published by the National Astronomical Observatory of Japan (日本の暦日データベース, after 内田正男『日本暦日原典』).
 * <p>
 * The calendars of the Edo period (Jōkyō, Hōreki, Kansei, Tenpō) were computed for the meridian of Kyoto with methods
 * and ephemerides of their time, so their months cannot be reproduced exactly by modern astronomy: the published
 * month starts are used instead.
 */
public final class JapaneseLunisolarTable{

	private static final String RESOURCE = "japanese-lunisolar.csv";
	private static final String COMMENT = "#";
	private static final String LEAP_PREFIX = "L";

	/**
	 * The months of a lunar year.
	 *
	 * @param newYearJdn the Julian Day Number of the first day of the year
	 * @param numbers    the number of each month, in order
	 * @param leap       whether each month is a leap month
	 * @param lengths    the number of days of each month
	 */
	record LunarYear(long newYearJdn, int[] numbers, boolean[] leap, int[] lengths){

		/**
		 * @return the position of the month in the year, or {@code -1} if the month does not exist
		 */
		int indexOf(final int month, final boolean leapMonth){
			for(int i = 0; i < numbers.length; i ++)
				if(numbers[i] == month && leap[i] == leapMonth)
					return i;
			return -1;
		}

		long monthStartJdn(final int index){
			long jdn = newYearJdn;
			for(int i = 0; i < index; i ++)
				jdn += lengths[i];
			return jdn;
		}

		long endJdn(){
			return monthStartJdn(numbers.length);
		}

	}

	private static final TreeMap<Integer, LunarYear> TABLE = load();
	private static final Map<Integer, LunarYear> YEARS = Collections.unmodifiableMap(TABLE);


	private JapaneseLunisolarTable(){}


	/**
	 * @return the first lunar year of the table
	 */
	public static int firstYear(){
		return TABLE.firstKey();
	}

	/**
	 * @return the last lunar year of the table
	 */
	public static int lastYear(){
		return TABLE.lastKey();
	}

	/**
	 * Converts a Japanese lunisolar date into its Julian Day Number.
	 *
	 * @param lunarYear the lunar year (numbered as the Gregorian year in which it starts)
	 * @param month     the month (1 to 12)
	 * @param leapMonth whether the month is the leap month (閏月) following the month with the same number
	 * @param day       the day of the month, or {@link EastAsianDateInput#LAST_DAY} for the last day (晦日)
	 * @return the Julian Day Number
	 * @throws IllegalArgumentException if the date is outside the table or does not exist
	 */
	public static long toJdn(final int lunarYear, final int month, final boolean leapMonth, final int day){
		final LunarYear year = year(lunarYear);
		final int index = year.indexOf(month, leapMonth);
		if(index < 0)
			throw new IllegalArgumentException("The " + (leapMonth? "leap ": "") + "month " + month
				+ " does not exist in the Japanese lunisolar year " + lunarYear);

		final int length = year.lengths()[index];
		final int dayOfMonth = (day == EastAsianDateInput.LAST_DAY? length: day);
		if(dayOfMonth < 1 || dayOfMonth > length)
			throw new IllegalArgumentException("The " + (leapMonth? "leap ": "") + "month " + month + " of the Japanese year "
				+ lunarYear + " has " + length + " days");

		return year.monthStartJdn(index) + dayOfMonth - 1;
	}

	/**
	 * @param lunarYear the lunar year
	 * @return the Julian Day Number of the first day of the year
	 */
	public static long newYearJdn(final int lunarYear){
		return year(lunarYear).newYearJdn();
	}

	/**
	 * @param lunarYear the lunar year
	 * @return the number of days of the year
	 */
	public static int yearLength(final int lunarYear){
		final LunarYear year = year(lunarYear);
		return (int)(year.endJdn() - year.newYearJdn());
	}

	private static LunarYear year(final int lunarYear){
		final LunarYear year = YEARS.get(lunarYear);
		if(year == null)
			throw new IllegalArgumentException("The Japanese lunisolar year " + lunarYear + " is outside the supported range "
				+ firstYear() + "-" + lastYear());
		return year;
	}


	private static TreeMap<Integer, LunarYear> load(){
		final TreeMap<Integer, LunarYear> years = new TreeMap<>();
		try(final InputStream is = JapaneseLunisolarTable.class.getResourceAsStream(RESOURCE)){
			if(is == null)
				throw new IllegalStateException("Missing resource " + RESOURCE);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			while((line = reader.readLine()) != null){
				if(line.isBlank() || line.startsWith(COMMENT))
					continue;

				final String[] fields = line.split(";");
				final int lunarYear = Integer.parseInt(fields[0]);
				final LocalDate newYear = LocalDate.parse(fields[1]);
				final String[] months = fields[2].split(" ");
				final int[] numbers = new int[months.length];
				final boolean[] leap = new boolean[months.length];
				final int[] lengths = new int[months.length];
				for(int i = 0; i < months.length; i ++){
					String month = months[i];
					leap[i] = month.startsWith(LEAP_PREFIX);
					if(leap[i])
						month = month.substring(LEAP_PREFIX.length());
					final int separator = month.indexOf(':');
					numbers[i] = Integer.parseInt(month.substring(0, separator));
					lengths[i] = Integer.parseInt(month.substring(separator + 1));
				}
				final long newYearJdn = GregorianCalendarStrategy.toJdn(newYear.getYear(), newYear.getMonthValue(),
					newYear.getDayOfMonth());
				years.put(lunarYear, new LunarYear(newYearJdn, numbers, leap, lengths));
			}
		}
		catch(final IOException ioe){
			throw new UncheckedIOException(ioe);
		}
		return years;
	}

}
