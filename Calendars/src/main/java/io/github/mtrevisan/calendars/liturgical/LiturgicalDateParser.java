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
package io.github.mtrevisan.calendars.liturgical;

import io.github.mtrevisan.calendars.text.Ordinals;
import io.github.mtrevisan.calendars.text.TextNormalizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Parser of the dates expressed through the liturgical year, as in the parish registers and the medieval documents, e.g.
 * "festa di San Martino 1612", "il giovedì dopo San Martino 1612", "terza domenica dopo Pentecoste 1700",
 * "Dominica in Albis 1650", "feria tertia post festum Sancti Michaelis 1420", "vigilia dell'Assunta 1530",
 * "in octava Paschae 1480", "the morrow of Michaelmas 1420", "Saturday before Palm Sunday 1599".
 * <p>
 * The expression is a feast, optionally preceded by relative positions, applied from the feast outwards:
 * <ul>
 *   <li>a weekday, optionally numbered, after ("dopo", "post", "after"), before ("prima di", "ante", "before"), or in
 *   ("di", "in", "of", or a Latin genitive) the feast;</li>
 *   <li>the vigil ("vigilia", "eve") and the morrow ("crastino", "morrow", "giorno dopo");</li>
 *   <li>the octave ("ottava", "octava", "octave"), seven days after the feast;</li>
 *   <li>a number of days after or before the feast ("tre giorni dopo", "3 days after").</li>
 * </ul>
 * The year is the one of the feast, counted from 1 January.
 */
final class LiturgicalDateParser{

	private static final Pattern YEAR_WORD = Pattern.compile("\\d{3,4}");
	/** A relative position, applied to the day of a feast. */
	record Shift(Kind kind, int weekday, int count, int days){

		enum Kind{
			/** A number of days later (or earlier, if negative). */
			DAYS,
			/** The n-th given weekday strictly after the day. */
			WEEKDAY_AFTER,
			/** The n-th given weekday strictly before the day. */
			WEEKDAY_BEFORE,
			/** The n-th given weekday on or after the day. */
			WEEKDAY_FROM
		}

		static Shift days(final int days){
			return new Shift(Kind.DAYS, 0, 0, days);
		}

		static Shift weekday(final Kind kind, final int weekday, final int count){
			return new Shift(kind, weekday, count, 0);
		}

	}

	/**
	 * A parsed liturgical date.
	 *
	 * @param year   the year of the feast
	 * @param feast  the feast
	 * @param shifts the relative positions, from the outermost to the innermost (the one nearest to the feast)
	 */
	record LiturgicalDate(int year, Feast feast, List<Shift> shifts){}


	/** Weekdays, as {@code 0} (Monday) to {@code 6} (Sunday). */
	private static final Map<String, Integer> WEEKDAYS = new HashMap<>();
	static{
		registerWeekday(0, "lunedi", "monday", "lundi", "montag", "lunes");
		registerWeekday(1, "martedi", "tuesday", "mardi", "dienstag", "martes");
		registerWeekday(2, "mercoledi", "wednesday", "mercredi", "mittwoch", "miercoles");
		registerWeekday(3, "giovedi", "thursday", "jeudi", "donnerstag", "jueves");
		registerWeekday(4, "venerdi", "friday", "vendredi", "freitag", "viernes");
		registerWeekday(5, "sabato", "sabbato", "sabbatum", "sabbati", "saturday", "samedi", "samstag", "sonnabend", "sabado");
		registerWeekday(6, "domenica", "dominica", "dominicae", "dominicam", "dominicis", "sunday", "dimanche", "sonntag",
			"domingo");
	}
	/** The Latin weekdays are numbered from Sunday: "feria secunda" is Monday, "feria sexta" is Friday. */
	private static final String FERIA = "feria";

	private static final Set<String> AFTER = Set.of("dopo", "post", "after", "nach", "apres", "despues", "depois",
		"seguente", "following");
	private static final Set<String> BEFORE = Set.of("prima", "ante", "before", "vor", "avant", "antes", "innanzi",
		"preceding", "precedente");
	private static final Set<String> WITHIN = Set.of("di", "del", "della", "dello", "delle", "dei", "degli", "dell", "of",
		"in", "de", "des", "du", "nella", "nel", "nell", "within", "im", "der");
	/** After the weekday, "prima" is "before" only when followed by one of these (otherwise it is "first"). */
	private static final Set<String> BEFORE_LINKS = Set.of("di", "del", "della", "dello", "delle", "dei", "degli", "dell",
		"de", "d", "l");

	private static final Set<String> VIGIL = Set.of("vigilia", "vigiliam", "vigiliae", "vigil", "eve", "pervigilio",
		"veille", "vorabend");
	private static final Set<String> MORROW = Set.of("crastino", "crastinum", "morrow", "lendemain");
	private static final Set<String> DAY_WORDS = Set.of("giorno", "giorni", "day", "days", "dies", "diebus", "die", "jour",
		"jours", "tag", "tage", "tagen", "dia", "dias");
	private static final Set<String> OCTAVE = Set.of("ottava", "octava", "octavam", "octavas", "octavis", "octave",
		"octaves", "oktav", "oktave");
	/** Articles and prepositions that can precede a relative position (e.g. "il giovedì", "in vigilia", "the morrow"). */
	private static final Set<String> LEADING_ARTICLES = Set.of("il", "lo", "la", "l", "the", "on", "in", "le", "am", "au",
		"el", "nel", "nella", "nell", "a", "al", "alla", "all", "die", "der", "den");
	private static final Set<String> YEAR_WORDS = Set.of("anno", "domini", "ad", "year", "an", "jahr");


	private LiturgicalDateParser(){}


	private static void registerWeekday(final int weekday, final String... names){
		for(final String name : names)
			WEEKDAYS.put(name, weekday);
	}


	/**
	 * Parses a liturgical date expression.
	 *
	 * @param rawDate the expression
	 * @return the parsed date
	 * @throws IllegalArgumentException if the expression cannot be parsed
	 */
	static LiturgicalDate parse(final String rawDate){
		final List<String> words = new ArrayList<>(Arrays.asList(TextNormalizer.tokenize(rawDate)));
		final int year = extractYear(words, rawDate);

		final List<Shift> shifts = new ArrayList<>();
		final Feast feast = parseExpression(words, 0, shifts, rawDate);
		return new LiturgicalDate(year, feast, shifts);
	}

	/**
	 * Removes the year (and the "anno Domini" before it) from the words.
	 */
	private static int extractYear(final List<String> words, final String rawDate){
		int yearIndex = -1;
		for(int i = 0; i < words.size(); i ++)
			if(YEAR_WORD.matcher(words.get(i)).matches()){
				if(yearIndex >= 0)
					throw new IllegalArgumentException("More than one year in the liturgical date: " + rawDate);
				yearIndex = i;
			}
		if(yearIndex < 0)
			throw new IllegalArgumentException("Missing year in the liturgical date: " + rawDate);

		final int year = Integer.parseInt(words.remove(yearIndex));
		// the formula before the year ("anno Domini", "anno", "A.D."), and "A.D." after it ("Domini" alone belongs to
		// feasts like "Corpus Domini")
		if(endsWith(words, yearIndex, "anno", "domini") || endsWith(words, yearIndex, "a", "d"))
			yearIndex = removeBefore(words, yearIndex, 2);
		else if(yearIndex > 0 && YEAR_WORDS.contains(words.get(yearIndex - 1)) && !words.get(yearIndex - 1).equals("domini"))
			yearIndex = removeBefore(words, yearIndex, 1);
		if(yearIndex + 1 < words.size() && words.get(yearIndex).equals("a") && words.get(yearIndex + 1).equals("d")){
			words.remove(yearIndex);
			words.remove(yearIndex);
		}
		else if(yearIndex < words.size() && words.get(yearIndex).equals("ad"))
			words.remove(yearIndex);
		return year;
	}

	private static boolean endsWith(final List<String> words, final int end, final String first, final String second){
		return (end >= 2 && words.get(end - 2).equals(first) && words.get(end - 1).equals(second));
	}

	private static int removeBefore(final List<String> words, final int end, final int count){
		for(int i = 0; i < count; i ++)
			words.remove(end - count);
		return end - count;
	}

	/**
	 * Parses the expression starting at a position, collecting the relative positions, and returns the feast.
	 */
	private static Feast parseExpression(final List<String> words, int index, final List<Shift> shifts,
			final String rawDate){
		// a whole feast name first (it can contain weekdays, e.g. "Domenica delle Palme", "Giovedì Santo")
		final Feast whole = LiturgicalFeasts.find(words.subList(index, words.size()));
		if(whole != null)
			return whole;

		// skip the articles and prepositions before a relative position (e.g. "il giovedì", "in vigilia", "the morrow")
		while(index < words.size() && LEADING_ARTICLES.contains(words.get(index)) && !isShiftStart(words, index))
			index ++;
		if(index >= words.size())
			throw new IllegalArgumentException("Missing feast in the liturgical date: " + rawDate);

		// [ordinal] weekday [ordinal] [after|before|in] <expression>
		final int[] weekdayMatch = matchWeekday(words, index);
		if(weekdayMatch != null){
			final int weekday = weekdayMatch[0];
			int count = weekdayMatch[1];
			int next = weekdayMatch[2];

			Shift.Kind kind = Shift.Kind.WEEKDAY_FROM;
			if(next < words.size()){
				final String word = words.get(next);
				final boolean beforeLink = (next + 1 >= words.size() || BEFORE_LINKS.contains(words.get(next + 1)));
				if(AFTER.contains(word)){
					kind = Shift.Kind.WEEKDAY_AFTER;
					next ++;
				}
				else if(BEFORE.contains(word) && (!word.equals("prima") || beforeLink)){
					kind = Shift.Kind.WEEKDAY_BEFORE;
					next ++;
				}
				else if(count == 0){
					// a Latin ordinal after the weekday (e.g. "dominica tertia post Pentecosten")
					final int[] ordinal = Ordinals.parse(words.toArray(String[]::new), next);
					if(ordinal != null && !WEEKDAYS.containsKey(words.get(next))){
						count = ordinal[0];
						next += ordinal[1];
						if(next < words.size() && AFTER.contains(words.get(next))){
							kind = Shift.Kind.WEEKDAY_AFTER;
							next ++;
						}
						else if(next < words.size() && BEFORE.contains(words.get(next))){
							kind = Shift.Kind.WEEKDAY_BEFORE;
							next ++;
						}
					}
				}
				if(kind == Shift.Kind.WEEKDAY_FROM && next < words.size() && WITHIN.contains(words.get(next)))
					next ++;
			}
			shifts.add(Shift.weekday(kind, weekday, Math.max(count, 1)));
			return parseExpression(words, next, shifts, rawDate);
		}

		final String word = words.get(index);
		if(VIGIL.contains(word)){
			shifts.add(Shift.days(-1));
			return parseExpression(words, index + 1, shifts, rawDate);
		}
		if(MORROW.contains(word)){
			shifts.add(Shift.days(1));
			return parseExpression(words, index + 1, shifts, rawDate);
		}
		if(OCTAVE.contains(word)){
			shifts.add(Shift.days(7));
			return parseExpression(words, index + 1, shifts, rawDate);
		}
		// "giorno dopo", "day after", "dies post": the morrow
		if(DAY_WORDS.contains(word) && index + 1 < words.size() && AFTER.contains(words.get(index + 1))){
			shifts.add(Shift.days(1));
			return parseExpression(words, index + 2, shifts, rawDate);
		}
		// "<n> giorni dopo", "<n> days before"
		final int[] number = Ordinals.parse(words.toArray(String[]::new), index);
		if(number != null && index + number[1] + 1 < words.size() && DAY_WORDS.contains(words.get(index + number[1]))){
			final String direction = words.get(index + number[1] + 1);
			if(AFTER.contains(direction) || BEFORE.contains(direction)){
				shifts.add(Shift.days(AFTER.contains(direction)? number[0]: -number[0]));
				return parseExpression(words, index + number[1] + 2, shifts, rawDate);
			}
		}

		throw new IllegalArgumentException("Unknown feast in the liturgical date: " + rawDate);
	}

	private static boolean isShiftStart(final List<String> words, final int index){
		if(index >= words.size())
			return false;

		final String word = words.get(index);
		return (matchWeekday(words, index) != null || VIGIL.contains(word) || MORROW.contains(word) || OCTAVE.contains(word)
			|| DAY_WORDS.contains(word) || Ordinals.parse(word) > 0);
	}

	/**
	 * Matches an optionally numbered weekday at a position.
	 *
	 * @return the weekday ({@code 0} = Monday), the number ({@code 0} if missing), and the position after the match, or
	 * 	{@code null}
	 */
	private static int[] matchWeekday(final List<String> words, final int index){
		final String[] array = words.toArray(String[]::new);
		int position = index;
		int count = 0;
		// an ordinal before the weekday (e.g. "terza domenica", "second Sunday")
		final int[] ordinal = Ordinals.parse(array, position);
		if(ordinal != null && position + ordinal[1] < array.length && isWeekdayAt(array, position + ordinal[1])
				&& !WEEKDAYS.containsKey(array[position])){
			count = ordinal[0];
			position += ordinal[1];
		}
		if(position >= array.length)
			return null;

		final Integer weekday = WEEKDAYS.get(array[position]);
		if(weekday != null)
			return new int[]{weekday, count, position + 1};

		// "feria secunda" (Monday) ... "feria sexta" (Friday), "feria septima" (Saturday)
		if(array[position].equals(FERIA)){
			final int[] feria = Ordinals.parse(array, position + 1);
			if(feria != null && feria[0] >= 2 && feria[0] <= 7)
				return new int[]{feria[0] - 2, count, position + 1 + feria[1]};
		}
		return null;
	}

	private static boolean isWeekdayAt(final String[] words, final int index){
		return (WEEKDAYS.containsKey(words[index]) || words[index].equals(FERIA));
	}

}
