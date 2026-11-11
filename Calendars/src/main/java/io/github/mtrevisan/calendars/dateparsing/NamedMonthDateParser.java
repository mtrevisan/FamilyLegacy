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
package io.github.mtrevisan.calendars.dateparsing;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Parser of the dates written with a named month, in the layout {@code [day] month year}: "15 Nisan 5784", "Tekufat Nisan
 * 5784", "5784" (a year alone), shared by the calendars whose months have names.
 * <p>
 * The text is read case-insensitively (the names are lowercase), with the typographic apostrophes taken for the
 * straight one. The month may be written with more words. The parser:
 * </p>
 * <ul>
 *   <li>accepts a year alone, that stands for the first day of the year (month and day are {@code 0});</li>
 *   <li>maps a season (an equinox or a solstice) to a negative {@code CalendarSeason} flag, and rejects a day written
 *   with it ("5 winter 1500"): a season is an instant, not a month;</li>
 *   <li>maps a named day (Lenin Day, the Sansculottides) to the month and the day it stands for;</li>
 *   <li>validates the year (from 1) and the day against the real length of the month of the year;</li>
 *   <li>rejects an empty text.</li>
 * </ul>
 */
public final class NamedMonthDateParser{

	/** The smallest year accepted. */
	public static final int MIN_YEAR = 1;
	/** The largest year accepted. */
	public static final int MAX_YEAR = 999_999;

	private static final int MAX_DIGITS = 9;
	/** The typographic apostrophes and the accent used as an apostrophe, replaced by the straight one. */
	private static final String APOSTROPHES = "’ʼʹ‘`´";
	private static final char APOSTROPHE = '\'';


	/** A day with its own name (e.g. Lenin Day), or a group of them (e.g. the two Labor Days). */
	public record NamedDay(int month, int firstDay, int days){

		public NamedDay{
			if(month < 1 || firstDay < 1 || days < 1)
				throw new IllegalArgumentException("Not a valid named day");
		}

	}

	/** Resolves the month of the name, when it depends on the year (e.g. the bare "Adar" of the Hebrew calendar). */
	@FunctionalInterface
	public interface MonthResolver{

		/**
		 * @param name  the (normalized) name of the month as written
		 * @param month the month of the name
		 * @param year  the year
		 * @return the month
		 */
		int resolve(String name, int month, int year);

	}


	/** The builder of a {@link NamedMonthDateParser}. */
	public static final class Builder{

		private final String calendar;
		private final Map<String, Integer> months = new HashMap<>();
		private final Map<String, NamedDay> namedDays = new HashMap<>();
		private final Map<String, String> rejected = new HashMap<>();
		private MonthLengths lengths;
		private MonthResolver resolver;


		private Builder(final String calendar){
			this.calendar = calendar;
		}


		/**
		 * @param names the months (and seasons) by name, in any case
		 * @return this builder
		 * @throws IllegalStateException if a name is already defined for another month
		 */
		public Builder months(final Map<String, Integer> names){
			for(final Map.Entry<String, Integer> name : names.entrySet()){
				final String key = normalize(name.getKey());
				final Integer previous = months.put(key, name.getValue());
				if(previous != null && !previous.equals(name.getValue()))
					throw new IllegalStateException("The " + calendar + " month name '" + key + "' is defined twice");
			}
			return this;
		}

		/**
		 * @param names the days that have a name, by name, in any case
		 * @return this builder
		 */
		public Builder namedDays(final Map<String, NamedDay> names){
			for(final Map.Entry<String, NamedDay> name : names.entrySet())
				namedDays.put(normalize(name.getKey()), name.getValue());
			return this;
		}

		/**
		 * @param names the names that are known but cannot be turned into a date, with the reason, by name, in any case
		 * @return this builder
		 */
		public Builder rejected(final Map<String, String> names){
			for(final Map.Entry<String, String> name : names.entrySet())
				rejected.put(normalize(name.getKey()), name.getValue());
			return this;
		}

		/**
		 * @param monthLengths the length of the months, to validate the days
		 * @return this builder
		 */
		public Builder monthLengths(final MonthLengths monthLengths){
			this.lengths = monthLengths;
			return this;
		}

		/**
		 * @param monthResolver the resolver of the months that depend on the year
		 * @return this builder
		 */
		public Builder monthResolver(final MonthResolver monthResolver){
			this.resolver = monthResolver;
			return this;
		}

		public NamedMonthDateParser build(){
			if(lengths == null)
				throw new IllegalStateException("The month lengths are missing");

			return new NamedMonthDateParser(this);
		}

	}


	private final String calendar;
	private final Map<String, Integer> months;
	private final Map<String, NamedDay> namedDays;
	private final Map<String, String> rejected;
	private final MonthLengths lengths;
	private final MonthResolver resolver;


	/**
	 * @param calendar the name of the calendar, for the error messages (e.g. "Coptic")
	 * @return a builder
	 */
	public static Builder builder(final String calendar){
		return new Builder(calendar);
	}

	private NamedMonthDateParser(final Builder builder){
		calendar = builder.calendar;
		months = Map.copyOf(builder.months);
		namedDays = Map.copyOf(builder.namedDays);
		rejected = Map.copyOf(builder.rejected);
		lengths = builder.lengths;
		resolver = builder.resolver;
	}


	/**
	 * Parses a date.
	 *
	 * @param rawDate the date expression (e.g. "15 Nisan 5784", "Aviv 5784", "5784")
	 * @return the components
	 * @throws IllegalArgumentException if the text is empty, cannot be parsed, or names a day that does not exist
	 */
	public YearMonthDay parse(final String rawDate){
		if(StringUtils.isBlank(rawDate))
			throw new IllegalArgumentException("The " + calendar + " date is empty.");

		final String normalized = normalize(rawDate);

		// a year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(isNumber(normalized))
			return new YearMonthDay(year(normalized, rawDate), 0, 0);

		final String[] tokens = StringUtils.split(normalized, ' ');
		final int last = tokens.length - 1;
		if(tokens.length < 2 || !isNumber(tokens[last]))
			throw new IllegalArgumentException("Invalid " + calendar + " date format: " + rawDate);

		final int year = year(tokens[last], rawDate);
		final boolean hasDay = (tokens.length >= 3 && isNumber(tokens[0]));
		if(hasDay && tokens[0].length() > MAX_DIGITS)
			throw new IllegalArgumentException("Invalid day in the " + calendar + " date: " + rawDate);
		final int dayNumber = (hasDay? Integer.parseInt(tokens[0]): 0);
		if(hasDay && dayNumber < 1)
			throw new IllegalArgumentException("The day must be 1 or later: " + rawDate);
		final String name = String.join(StringUtils.SPACE, Arrays.copyOfRange(tokens, (hasDay? 1: 0), last));

		final String reason = rejected.get(name);
		if(reason != null)
			throw new IllegalArgumentException(reason + ": " + rawDate);

		final NamedDay namedDay = namedDays.get(name);
		if(namedDay != null)
			return namedDay(namedDay, hasDay, dayNumber, year, rawDate);

		final Integer found = months.get(name);
		if(found == null)
			throw new IllegalArgumentException("Unknown " + calendar + " month or seasonal descriptor: " + name);

		int month = found;
		if(month < 0){
			if(hasDay)
				throw new IllegalArgumentException("A day cannot be given with a season, that is an instant and not a month: "
					+ rawDate);
			return new YearMonthDay(year, month, 0);
		}

		if(resolver != null)
			month = resolver.resolve(name, month, year);
		return validated(year, month, (hasDay? dayNumber: 0), rawDate);
	}

	private YearMonthDay namedDay(final NamedDay namedDay, final boolean hasDay, final int dayNumber, final int year,
			final String rawDate){
		// "2 labor days" is the second of the days of the group
		final int index = (hasDay? dayNumber: 1);
		if(index < 1 || index > namedDay.days())
			throw new IllegalArgumentException("The day " + index + " does not exist in the named days (" + namedDay.days()
				+ " day" + (namedDay.days() == 1? StringUtils.EMPTY: "s") + "): " + rawDate);

		return validated(year, namedDay.month(), namedDay.firstDay() + index - 1, rawDate);
	}

	private YearMonthDay validated(final int year, final int month, final int day, final String rawDate){
		final int length = lengths.length(year, month);
		if(length <= 0)
			throw new IllegalArgumentException("The month " + month + " does not exist in the " + calendar + " year " + year
				+ ": " + rawDate);
		if(day != 0 && (day < 1 || day > length))
			throw new IllegalArgumentException("The day " + day + " is not valid: the month " + month + " of the " + calendar
				+ " year " + year + " has " + length + " days: " + rawDate);

		return new YearMonthDay(year, month, day);
	}

	private int year(final String token, final String rawDate){
		if(token.length() > MAX_DIGITS)
			throw new IllegalArgumentException("Invalid year in the " + calendar + " date: " + rawDate);

		final int year = Integer.parseInt(token);
		if(year < MIN_YEAR || year > MAX_YEAR)
			throw new IllegalArgumentException("The year of the " + calendar + " date must be between " + MIN_YEAR + " and "
				+ MAX_YEAR + ": " + rawDate);
		return year;
	}

	/** @return whether the text is made of digits only */
	private static boolean isNumber(final String text){
		if(StringUtils.isEmpty(text))
			return false;

		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);
			if(c < '0' || c > '9')
				return false;
		}
		return true;
	}

	/** Lowercases the text, takes the typographic apostrophes for the straight one, and collapses the spaces. */
	private static String normalize(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);
			sb.append(APOSTROPHES.indexOf(c) >= 0? APOSTROPHE: c);
		}
		return StringUtils.normalizeSpace(sb.toString())
			.toLowerCase(Locale.ROOT);
	}

}
