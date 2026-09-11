package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

/**
 * Arithmetic converter between calendar-specific dates and the Julian Day
 * Number (JDN) used as the common axis of the General Temporal Projection.
 * <p>
 * The JDN is an integer day count; dates from different calendars are
 * mapped onto the same axis and can therefore be compared and laid out
 * directly.
 * <p>
 * Supported calendars (exact, arithmetic):
 * <ul>
 *   <li>{@code gregorian} — proleptic Gregorian, Fliegel–Van Flandern</li>
 *   <li>{@code julian} — proleptic Julian</li>
 *   <li>{@code islamic} — tabular Islamic (civil epoch, 16 July 622 Julian)</li>
 *   <li>{@code coptic} — Coptic (epoch 29 August 284 Julian)</li>
 *   <li>{@code ethiopian} — Ethiopian (epoch 29 August 8 Julian)</li>
 * </ul>
 * The remaining calendars declared by the FLEF protocol
 * ({@code hebrew}, {@code chinese}, {@code indian}, {@code buddhist},
 * {@code french_republican}, {@code soviet_eternal}, {@code mayan}) require
 * astronomical calculations, non-standard correlation constants, or have
 * ambiguous definitions; they raise {@link UnsupportedOperationException}
 * so that the extension point is explicit.
 */
public final class CalendarConverter{

	private CalendarConverter(){
	}


	/**
	 * Converts a calendar-specific date to its Julian Day Number.
	 *
	 * @param calendar the calendar name (see class Javadoc); a blank value
	 *                 is treated as Gregorian
	 * @param year     the year in the given calendar; may be negative
	 * @param month    the month (1‑based)
	 * @param day      the day of month (1‑based)
	 * @return the Julian Day Number
	 * @throws UnsupportedOperationException if the calendar is not arithmetic
	 */
	public static long toJdn(final String calendar, final int year, final int month, final int day){
		if(calendar == null || calendar.isBlank())
			return gregorianToJdn(year, month, day);
		return switch(calendar){
			case NormalizedDate.CALENDAR_GREGORIAN -> gregorianToJdn(year, month, day);
			case NormalizedDate.CALENDAR_JULIAN -> julianToJdn(year, month, day);
			case NormalizedDate.CALENDAR_ISLAMIC -> islamicToJdn(year, month, day);
			case NormalizedDate.CALENDAR_COPTIC -> copticToJdn(year, month, day);
			case NormalizedDate.CALENDAR_ETHIOPIAN -> ethiopianToJdn(year, month, day);
			default -> throw new UnsupportedOperationException(
				"Calendar not supported by the arithmetic converter: " + calendar
					+ ". Extend CalendarConverter to add it.");
		};
	}

	/**
	 * Returns the JDN of the first day of the given year in the given
	 * calendar. Used for reduced‑precision dates (decade, century) where
	 * only the year is known.
	 *
	 * @param calendar the calendar name
	 * @param year     the year
	 * @return the Julian Day Number of 1 January of that year
	 */
	public static long toJdnStartOfYear(final String calendar, final int year){
		return toJdn(calendar, year, 1, 1);
	}


	/* ======================================================================
	 *                          Gregorian
	 * ====================================================================== */

	/**
	 * Proleptic Gregorian calendar to JDN (Fliegel–Van Flandern).
	 * Works for negative years as well.
	 */
	public static long gregorianToJdn(final int year, final int month, final int day){
		final long a = (14 - month) / 12;
		final long y = year + 4800L - a;
		final long m = month + 12L * a - 3;
		return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
	}


	/* ======================================================================
	 *                          Julian
	 * ====================================================================== */

	/**
	 * Proleptic Julian calendar to JDN.
	 */
	public static long julianToJdn(final int year, final int month, final int day){
		final long a = (14 - month) / 12;
		final long y = year + 4800L - a;
		final long m = month + 12L * a - 3;
		return day + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
	}


	/* ======================================================================
	 *                          Islamic (tabular, civil epoch)
	 * ====================================================================== */

	/**
	 * Tabular Islamic (civil) calendar to JDN.
	 * <p>
	 * Month structure: 30, 29, 30, 29, …, 30, 29/30.
	 * Leap years are those whose {@code year mod 30} is in
	 * {@code {2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29}}.
	 */
	public static long islamicToJdn(final int year, final int month, final int day){
		return day
			+ (long)Math.ceil(29.5 * (month - 1))
			+ (year - 1L) * 354L
			+ (long)Math.floor((3 + 11. * year) / 30.)
			+ 1948440L - 1L;
	}


	/* ======================================================================
	 *                          Coptic
	 * ====================================================================== */

	/**
	 * Coptic calendar to JDN.
	 * <p>
	 * The Coptic year has 12 months of 30 days plus 5 epagomenal days,
	 * extended to 6 in leap years ({@code year mod 4 == 3}).
	 * The Coptic epoch is 29 August 284 Julian, i.e. JDN 1825030.
	 */
	public static long copticToJdn(final int year, final int month, final int day){
		return 1825030L
			+ 365L * (year - 1L)
			+ (year / 4L)
			+ 30L * (month - 1L)
			+ (day - 1L);
	}


	/* ======================================================================
	 *                          Ethiopian
	 * ====================================================================== */

	/**
	 * Ethiopian calendar to JDN.
	 * <p>
	 * The Ethiopian calendar shares month structure and leap year rule with
	 * the Coptic calendar, but its epoch is 29 August 8 Julian, i.e.
	 * JDN 1724221.
	 */
	public static long ethiopianToJdn(final int year, final int month, final int day){
		return 1724221L
			+ 365L * (year - 1L)
			+ (year / 4L)
			+ 30L * (month - 1L)
			+ (day - 1L);
	}

}
