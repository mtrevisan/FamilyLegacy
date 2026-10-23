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
package io.github.mtrevisan.calendars.regnal;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import io.github.mtrevisan.calendars.text.Ordinals;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dates counted in the years of a reign or of a pontificate, e.g. "5 March 3 George II",
 * "the third year of the reign of George II", "anno regni regis Georgii secundi tertio", "1 & 2 Philip and Mary",
 * "pontificatus domini Pii papae quinti anno tertio, die 5 martii", "anno III del pontificato di Pio V".
 * <p>
 * The expression contains the ruler (name and ordinal), the regnal year, and optionally the day and the month, and the
 * year of the Incarnation (used only to tell apart homonymous rulers, since it may follow a different year-start
 * style). Without the day, the date is the first day of the regnal year (or of the month).
 */
public class RegnalCalendarStrategy implements CalendarConverterPlugin{

	/** Words that introduce the regnal year. */
	private static final Set<String> YEAR_MARKERS = Set.of("anno", "anni", "annus", "annum", "year", "years", "yr", "jahr",
		"annee", "an", "regni", "regnal", "pontificatus", "pontificato", "pontificate", "reign");
	/** Words allowed between the name of the ruler and the ordinal (e.g. "Pii papae quinti", "Pii divina providentia papae V"). */
	private static final Set<String> TITLE_WORDS = Set.of("papae", "papa", "pape", "pope", "pp", "divina", "providentia",
		"rex", "regis", "regina", "reginae", "king", "queen", "the", "of", "il", "di", "del");
	private static final int MAX_TITLE_WORDS = 4;
	/** The largest day of a month; a larger number is the year of the Incarnation. */
	private static final int MAX_DAY = 31;
	private static final int MIN_INCARNATION_YEAR = 100;

	/**
	 * The regnal years of the kings and queens of England, Great Britain, and the United Kingdom.
	 * <p>
	 * After the reform of 1752 the regnal years of George II kept their length: the 26th year ran from 11 June 1752 to
	 * 21 June 1753, and the following ones began on 22 June (Cheney, Handbook of Dates, p. 42, note 82).
	 */
	public static final RegnalCalendarStrategy ENGLISH = new RegnalCalendarStrategy(Reigns.ENGLISH, CalendarReform.BRITAIN,
		true);
	/**
	 * The pontifical years of the popes (and antipopes), from Innocent III.
	 * <p>
	 * The anniversaries are kept on the same nominal day across the reform of 1582 (Gregory XIII).
	 */
	public static final RegnalCalendarStrategy PONTIFICAL = new RegnalCalendarStrategy(Reigns.POPES, CalendarReform.ROME,
		false);


	private final List<Reign> reigns;
	private final CalendarReform reform;
	/** Whether the anniversaries of a reign started before the reform stay Julian after it. */
	private final boolean julianAnniversary;


	RegnalCalendarStrategy(final List<Reign> reigns, final CalendarReform reform, final boolean julianAnniversary){
		this.reigns = reigns;
		this.reform = reform;
		this.julianAnniversary = julianAnniversary;
	}


	/**
	 * A parsed regnal date.
	 *
	 * @param candidates     the reigns matching the ruler (more than one for homonyms)
	 * @param regnalYears    the regnal years (one, or two for a double style)
	 * @param month          the month, or 0 if missing
	 * @param day            the day, or 0 if missing
	 * @param incarnationYear the year of the Incarnation, or 0 if missing
	 */
	private record RegnalDate(List<Reign> candidates, List<Integer> regnalYears, int month, int day, int incarnationYear){}


	@Override
	public long parseToJdn(final String rawDate){
		final RegnalDate date = parse(rawDate);

		final List<Long> results = new ArrayList<>();
		final List<String> errors = new ArrayList<>();
		final List<Reign> reigns = date.candidates();
		for(final Reign reign : reigns){
			try{
				final long jdn = toJdn(reign, date, rawDate);
				if(date.incarnationYear() == 0 || Math.abs(yearOf(jdn) - date.incarnationYear()) <= 1)
					results.add(jdn);
			}
			catch(final IllegalArgumentException iae){
				errors.add(iae.getMessage());
			}
		}
		if(results.size() == 1)
			return results.getFirst();
		if(results.size() > 1)
			throw new IllegalArgumentException("Ambiguous ruler, add the year of the Incarnation: " + rawDate);
		throw new IllegalArgumentException(errors.isEmpty()
			? "The date does not fall within the reign: " + rawDate
			: errors.getFirst());
	}

	private long toJdn(final Reign reign, final RegnalDate date, final String rawDate){
		final int regnalYear = (reign.rule() == Reign.YearRule.DOUBLE_ANNIVERSARY && date.regnalYears().size() > 1
			? date.regnalYears().get(1)
			: date.regnalYears().getFirst());
		if(regnalYear < 1)
			throw new IllegalArgumentException("The regnal year starts from 1: " + rawDate);

		final long yearStart = reign.yearStartJdn(regnalYear, reform, julianAnniversary);
		final long nextYearStart = reign.yearStartJdn(regnalYear + 1, reform, julianAnniversary);
		final long jdn;
		if(date.month() == 0)
			// a regnal year alone starts with the reign itself, if later (e.g. the readeption of Henry VI, in his 49th year)
			jdn = Math.max(yearStart, reign.validFrom().toJdn(reform));
		else{
			// the day falls in one of the (at most two) calendar years overlapping the regnal year
			final int firstYear = yearOf(yearStart);
			final List<Long> matches = new ArrayList<>();
			for(int year = firstYear; year <= firstYear + 1; year ++){
				final int day = (date.day() != 0? date.day(): 1);
				if(day > reform.monthLength(year, date.month()))
					continue;

				try{
					final long candidate = reform.toJdn(year, date.month(), day);
					if(candidate >= yearStart && candidate < nextYearStart)
						matches.add(candidate);
				}
				catch(final IllegalArgumentException ignored){}
			}
			if(matches.isEmpty())
				throw new IllegalArgumentException("The day does not exist in the regnal year " + regnalYear + ": " + rawDate);
			// a day occurring twice (a regnal year longer than a year, as the 26th of George II) needs the year
			if(matches.size() > 1 && date.incarnationYear() != 0)
				matches.removeIf(candidate -> yearOf(candidate) != date.incarnationYear());
			if(matches.size() > 1)
				throw new IllegalArgumentException("The day occurs twice in the regnal year " + regnalYear
					+ ", add the year of the Incarnation: " + rawDate);
			jdn = matches.getFirst();
		}

		if(!reign.contains(jdn, reform))
			throw new IllegalArgumentException("The date does not fall within the reign: " + rawDate);
		return jdn;
	}

	/**
	 * @return the civil year of a Julian Day Number, in the calendar of the reform
	 */
	private int yearOf(final long jdn){
		// the Julian and Gregorian years differ only around 1 January
		final long gregorianReformJdn = reform.toJdn(reform.firstGregorianYear(), reform.firstGregorianMonth(),
			reform.firstGregorianDay());
		final long z = jdn;
		if(jdn >= gregorianReformJdn){
			// Gregorian (Richards' algorithm)
			final long f = z + 1401 + (((4 * z + 274277) / 146097) * 3) / 4 - 38;
			final long e = 4 * f + 3;
			final long h = 5 * ((e % 1461) / 4) + 2;
			final long month = ((h / 153 + 2) % 12) + 1;
			return (int)(e / 1461 - 4716 + (12 + 2 - month) / 12);
		}
		final long f = z + 1401;
		final long e = 4 * f + 3;
		final long h = 5 * ((e % 1461) / 4) + 2;
		final long month = ((h / 153 + 2) % 12) + 1;
		return (int)(e / 1461 - 4716 + (12 + 2 - month) / 12);
	}

	private RegnalDate parse(final String rawDate){
		final List<String> words = new ArrayList<>(Arrays.asList(TextNormalizer.tokenize(rawDate)));
		if(words.isEmpty())
			throw new IllegalArgumentException("Date string context cannot be empty.");

		// 1. the ruler: name (one or more words), optional titles, and ordinal
		final List<Reign> withOrdinal = new ArrayList<>();
		final List<Reign> withoutOrdinal = new ArrayList<>();
		int matchStart = -1;
		int matchEnd = -1;
		int matchEndWithoutOrdinal = -1;
		for(int i = 0; i < words.size() && withOrdinal.isEmpty() && withoutOrdinal.isEmpty(); i ++)
			for(final Reign reign : reigns){
				final List<String> names = reign.names();
				for(final String name : names){
					final String[] nameWords = StringUtils.split(name);
					if(!matchesAt(words, i, nameWords))
						continue;

					int next = i + nameWords.length;
					int skipped = 0;
					while(next < words.size() && skipped < MAX_TITLE_WORDS && TITLE_WORDS.contains(words.get(next))){
						next ++;
						skipped ++;
					}
					final int[] ordinal = (next < words.size()? Ordinals.parse(words.toArray(String[]::new), next): null);
					if(ordinal != null && ordinal[0] == reign.ordinal()){
						// e.g. "Pii papae quinti", "George II"
						if(!withOrdinal.contains(reign))
							withOrdinal.add(reign);
						matchStart = i;
						matchEnd = Math.max(matchEnd, next + ordinal[1]);
					}
					else if(reign.ordinal() == 0 || reign.ordinal() == 1){
						// a ruler without ordinal (e.g. "John", "Victoria"), or the first of the name written without it
						if(!withoutOrdinal.contains(reign))
							withoutOrdinal.add(reign);
						matchStart = i;
						matchEndWithoutOrdinal = Math.max(matchEndWithoutOrdinal, i + nameWords.length);
					}
				}
			}
		// an explicit ordinal excludes the rulers matched without it
		final List<Reign> rulers = (!withOrdinal.isEmpty()? withOrdinal: withoutOrdinal);
		if(rulers.isEmpty())
			throw new IllegalArgumentException("Unknown ruler: " + rawDate);
		words.subList(matchStart, (!withOrdinal.isEmpty()? matchEnd: matchEndWithoutOrdinal)).clear();

		// 2. the month and the day next to it
		int month = 0;
		int day = 0;
		for(int i = 0; i < words.size() && month == 0; i ++){
			final int candidate = GregorianCalendarParser.parseWesternMonthName(words.get(i));
			if(candidate <= 0)
				continue;

			month = candidate;
			words.remove(i);
			final int dayIndex = findDay(words, i);
			if(dayIndex >= 0)
				day = Ordinals.parse(words.remove(dayIndex));
		}

		// 3. the year of the Incarnation
		int incarnationYear = 0;
		for(int i = 0; i < words.size(); i ++)
			if(StringUtils.isNumeric(words.get(i)) && Integer.parseInt(words.get(i)) >= MIN_INCARNATION_YEAR){
				incarnationYear = Integer.parseInt(words.remove(i));
				break;
			}

		// 4. the regnal year(s): preferably next to a marker ("anno tertio", "third year", "3 George II")
		final List<Integer> regnalYears = new ArrayList<>();
		final String[] remaining = words.toArray(String[]::new);
		for(int i = 0; i < remaining.length; i ++){
			if(YEAR_MARKERS.contains(remaining[i]) || TITLE_WORDS.contains(remaining[i]))
				continue;

			final int[] number = Ordinals.parse(remaining, i);
			if(number != null){
				regnalYears.add(number[0]);
				i += number[1] - 1;
			}
		}
		if(regnalYears.isEmpty())
			throw new IllegalArgumentException("Missing regnal year: " + rawDate);
		if(regnalYears.size() > 2)
			throw new IllegalArgumentException("Ambiguous regnal year: " + rawDate);

		return new RegnalDate(rulers, regnalYears, month, day, incarnationYear);
	}

	private static boolean matchesAt(final List<String> words, final int index, final String[] nameWords){
		if(index + nameWords.length > words.size())
			return false;

		for(int j = 0; j < nameWords.length; j ++)
			if(!words.get(index + j).equals(nameWords[j]))
				return false;
		return true;
	}

	/**
	 * @return the position of the day next to the month (before it, or after it), or {@code -1}
	 */
	private static int findDay(final List<String> words, final int monthIndex){
		final int[] indices = {monthIndex - 1, monthIndex};
		for(final int index : indices){
			if(index < 0 || index >= words.size())
				continue;

			final String word = words.get(index);
			final int value = Ordinals.parse(word);
			if(value >= 1 && value <= MAX_DAY && (Character.isDigit(word.charAt(0))))
				return index;
		}
		return -1;
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final RegnalDate date = parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", true);
		components.put("month", (date.month() != 0));
		components.put("day", (date.day() != 0));
		return new CalendarType.CalendarMetadata(components, (date.month() != 0 && date.day() != 0));
	}

}
