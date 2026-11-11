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
package io.github.mtrevisan.calendars.assistant;

import io.github.mtrevisan.calendars.text.Ordinals;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Interprets a day written in the Roman way, by Kalends, Nones and Ides ("ante diem III Nonas Martias", "pridie Kalendas
 * Ianuarias", "Idibus Octobribus", "the Ides of March"), in a given year.
 * <p>
 * The reckoning is the one of the Julian calendar: the Kalends are the first day of the month, the Nones the 5th (the 7th in
 * March, May, July and October), the Ides the 8th day after the Nones (the 13th, or the 15th in those four months); the other
 * days are counted backwards, inclusively, from the next of these three days ("ante diem III Nonas" is the second day before the
 * Nones, "pridie" is the day before).
 * </p>
 */
public final class RomanDateTool{

	//the words are compared after TextNormalizer.normalize: lowercase, no accents, no punctuation
	/** The words of "the day before" in English ("the day before the Nones"): the day, then before. */
	private static final String BEFORE_WORD = "before";
	private static final String DAY_WORD = "day";
	/** The count (inclusive) of the sixth day before the Kalends of March, the one that was doubled in a leap year. */
	private static final int SIXTH_DAY = 6;
	private static final Set<String> KALENDS = Set.of("kal", "kalendas", "kalendis", "kalendae", "kalendarum", "kalendarium",
		"kalends", "cal", "calendas", "calendis", "calendae", "calends");
	private static final Set<String> NONES = Set.of("non", "nonas", "nonis", "nonae", "nonarum", "nones");
	private static final Set<String> IDES = Set.of("id", "idus", "idibus", "idum", "ides");
	/** The day before the reference ("pridie"), that is the second day counting inclusively. */
	private static final Set<String> DAY_BEFORE = Set.of("pridie", "prid");
	/** The doubled sixth day before the Kalends of March of a leap year ("ante diem bis sextum Kalendas Martias"). */
	private static final Set<String> DOUBLED = Set.of("bis", "bissextum", "bissexto", "bissextus");
	/** The words that carry no information ("ante diem", "a.d.", "on the", "of"). */
	private static final Set<String> NOISE = Set.of("a", "d", "ad", "ante", "diem", "die", "dies", "the", "of", "on", "in", DAY_WORD, BEFORE_WORD,
		"mensis", "mensem");
	/** The (lowercase, unaccented) beginnings of the Latin and English names of the months, from January to December. */
	//the months of the early Julian calendar are included: Quintilis (July) and Sextilis (August), renamed in honour of
	//Julius Caesar and of Augustus, but still used in the later sources
	private static final String[][] MONTH_STEMS = {
		{"ian", "jan"}, {"feb"}, {"mar"}, {"apr"}, {"mai", "may"}, {"iun", "jun"}, {"iul", "jul", "quint"}, {"aug", "sext"},
		{"sep"}, {"oct"}, {"nov"}, {"dec"}};

	private static final int MARCH = 3;
	private static final int JANUARY = 1;
	private static final int DECEMBER = 12;
	private static final int FIRST_REGULAR_LEAP_YEAR = 8;


	private RomanDateTool(){}


	/**
	 * Interprets a Roman expression in a year.
	 *
	 * @param expression the expression ("ante diem VI Kal. Mart.", "Idibus Martiis", "pridie Nonas Maias")
	 * @param year       the year of the day (the Julian one, as written in the source: for a day of the last days of December, written
	 *                   with the Kalends of January, it is the year of December)
	 * @return the candidates (the Julian and the Gregorian dates; in a leap year, for the days of February before the 24th
	 *    also the classical reckoning)
	 * @throws IllegalArgumentException if the expression is not understood, or names a day that does not exist
	 */
	public static List<DateSuggestion> interpret(final String expression, final int year){
		if(year < 1)
			throw new AssistantInputException("date_assistant.error.roman.year");
		if(StringUtils.isBlank(expression))
			throw new AssistantInputException("date_assistant.error.roman.empty");

		final String[] words = TextNormalizer.tokenize(expression);
		int reference = -1;
		for(int i = 0; i < words.length; i ++)
			if(isReference(words[i])){
				reference = i;
				break;
			}
		if(reference < 0)
			throw new AssistantInputException("date_assistant.error.roman.no_reference", expression);

		final Kind kind = kindOf(words[reference]);
		final int count = readDayNumber(words, reference, expression);
		final boolean doubled = hasWord(words, reference, DOUBLED);
		final int month = readMonth(words, reference, expression);
		final String written = expression.trim();

		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(Message.of("date_assistant.assumption.roman_reckoning"));
		if(year < FIRST_REGULAR_LEAP_YEAR)
			assumptions.add(Message.of("date_assistant.assumption.roman_early_leap"));
		final List<Message> steps = new ArrayList<>();
		final Message named = Message.of("date_assistant.roman.named", Message.of(kind.labelKey), java.time.Month.of(month));
		steps.add(Message.of("date_assistant.step.roman_read", written, describeCount(count), named));

		//the Kalends of January of a date of December are those of the following year
		final int referenceYear = (kind == Kind.KALENDS && month == JANUARY && count > 1? year + 1: year);
		if(referenceYear != year)
			assumptions.add(Message.of("date_assistant.assumption.roman_next_year_kalends", year, referenceYear));
		final int referenceDay = referenceDay(kind, month);
		final long referenceJdn = CivilDates.toJdn(referenceYear, month, referenceDay, CivilDates.JULIAN);
		steps.add(Message.of("date_assistant.step.roman_reference", named, referenceYear, referenceDay, referenceJdn));

		final long arithmetic = referenceJdn - (count - 1);
		validate(kind, month, referenceYear, arithmetic, count, expression);

		final boolean leapFebruary = (kind == Kind.KALENDS && month == MARCH && isLeap(year) && count >= SIXTH_DAY);
		if(doubled && !(kind == Kind.KALENDS && month == MARCH && count == SIXTH_DAY && isLeap(year)))
			throw new AssistantInputException("date_assistant.error.roman.bis");
		if(!leapFebruary){
			steps.add(Message.of("date_assistant.step.roman_subtracted", count - 1, arithmetic));
			return CalendarConversionTool.fromJdn(arithmetic, Message.of("date_assistant.meaning.roman", written, year), assumptions, steps);
		}

		//a leap year: the sixth day before the Kalends of March was doubled. The bissextile day was inserted after the 23rd,
		//so the first of the two sixth days (24 February) is the "bis sextum" (Digest 50.16.98, Celsus; Macrobius,
		//Saturnalia 1.14; Blackburn and Holford-Strevens, The Oxford Companion to the Year, "bissextile"), and the second
		//(25 February) the plain sixth day: the feast of Saint Matthias, "a.d. VI Kal. Mart.", falls on the 25th in the
		//leap years. The days before the doubled one (the 23rd and the earlier ones) are the same in the two reckonings
		//but for the shift of one day against the arithmetic one, that adds 29 February at the end of the month.
		final List<DateSuggestion> result = new ArrayList<>();
		final List<Message> classicalSteps = new ArrayList<>(steps);
		final long classical = arithmetic - 1;
		if(!doubled){
			final List<Message> arithmeticAssumptions = new ArrayList<>(assumptions);
			arithmeticAssumptions.add(Message.of(count == SIXTH_DAY
				? "date_assistant.assumption.roman_leap_arithmetic_matthias": "date_assistant.assumption.roman_leap_arithmetic"));
			final List<Message> arithmeticSteps = new ArrayList<>(steps);
			arithmeticSteps.add(Message.of("date_assistant.step.roman_subtracted_plain", count - 1, arithmetic));
			result.addAll(CalendarConversionTool.fromJdn(arithmetic, Message.of("date_assistant.meaning.roman_leap_end", written, year),
				arithmeticAssumptions, arithmeticSteps));
		}
		final List<Message> classicalAssumptions = new ArrayList<>(assumptions);
		classicalAssumptions.add(Message.of("date_assistant.assumption.roman_leap_classical"));
		classicalSteps.add(Message.of("date_assistant.step.roman_subtracted_doubled", count - 1, classical));
		result.addAll(CalendarConversionTool.fromJdn(classical, Message.of(doubled
			? "date_assistant.meaning.roman_bis": "date_assistant.meaning.roman_doubled", written, year), classicalAssumptions, classicalSteps));
		return result;
	}


	private static void validate(final Kind kind, final int month, final int referenceYear, final long jdn, final int count,
			final String expression){
		if(count < 1)
			throw new AssistantInputException("date_assistant.error.roman.bad_count", expression);

		//the day must be after the previous reference day
		final long previous;
		if(kind == Kind.KALENDS){
			final int previousMonth = (month == JANUARY? DECEMBER: month - 1);
			final int previousYear = (month == JANUARY? referenceYear - 1: referenceYear);
			previous = CivilDates.toJdn(previousYear, previousMonth, referenceDay(Kind.IDES, previousMonth), CivilDates.JULIAN);
		}
		else if(kind == Kind.IDES)
			previous = CivilDates.toJdn(referenceYear, month, referenceDay(Kind.NONES, month), CivilDates.JULIAN);
		else
			previous = CivilDates.toJdn(referenceYear, month, 1, CivilDates.JULIAN);
		if(count > 1 && jdn <= previous)
			throw new AssistantInputException("date_assistant.error.roman.counts_back", expression.trim());
	}

	private static int referenceDay(final Kind kind, final int month){
		if(kind == Kind.KALENDS)
			return 1;

		final int nones = (month == MARCH || month == 5 || month == 7 || month == 10? 7: 5);
		return (kind == Kind.NONES? nones: nones + 8);
	}

	private static boolean isLeap(final int year){
		return (year % 4 == 0);
	}

	private static boolean isReference(final String word){
		return (KALENDS.contains(word) || NONES.contains(word) || IDES.contains(word));
	}

	private static Kind kindOf(final String word){
		return (KALENDS.contains(word)? Kind.KALENDS: NONES.contains(word)? Kind.NONES: Kind.IDES);
	}

	private static boolean hasWord(final String[] words, final int before, final Set<String> candidates){
		for(int i = 0; i < before; i ++)
			if(candidates.contains(words[i]))
				return true;
		return false;
	}

	/** @return the count of the days, inclusive of the reference day: 1 if the day is the reference itself, 2 for "pridie" */
	private static int readDayNumber(final String[] words, final int reference, final String expression){
		int count = 1;
		boolean found = false;
		for(int i = 0; i < reference; i ++){
			final String word = words[i];
			if(DAY_BEFORE.contains(word)){
				count = 2;
				found = true;
				continue;
			}
			if(DOUBLED.contains(word))
				continue;
			if(BEFORE_WORD.equals(word) && i > 0 && DAY_WORD.equals(words[i - 1]) && !found){
				//"the day before the Nones"
				count = 2;
				found = true;
				continue;
			}
			if(NOISE.contains(word))
				continue;

			final int value = Ordinals.parse(word);
			if(value < 1 || found)
				throw new AssistantInputException("date_assistant.error.roman.not_understood", word, expression.trim());
			count = value;
			found = true;
		}
		return count;
	}

	private static int readMonth(final String[] words, final int reference, final String expression){
		int month = -1;
		for(int i = reference + 1; i < words.length; i ++){
			if(NOISE.contains(words[i]))
				continue;

			final int value = monthOf(words[i]);
			if(value < 1 || month > 0)
				throw new AssistantInputException("date_assistant.error.roman.not_understood", words[i], expression.trim());
			month = value;
		}
		if(month < 1)
			throw new AssistantInputException("date_assistant.error.roman.no_month", expression.trim());
		return month;
	}

	private static int monthOf(final String word){
		for(int i = 0; i < MONTH_STEMS.length; i ++)
			for(final String stem : MONTH_STEMS[i])
				if(word.length() >= 3 && word.startsWith(stem))
					return i + 1;
		return -1;
	}

	private static Message describeCount(final int count){
		return (count == 1
			? Message.of("date_assistant.roman.count_itself")
			: count == 2? Message.of("date_assistant.roman.count_before"): Message.of("date_assistant.roman.count_number", count));
	}


	private enum Kind{
		KALENDS("date_assistant.roman.kalends"), NONES("date_assistant.roman.nones"), IDES("date_assistant.roman.ides");

		private final String labelKey;

		Kind(final String labelKey){
			this.labelKey = labelKey;
		}
	}

}
