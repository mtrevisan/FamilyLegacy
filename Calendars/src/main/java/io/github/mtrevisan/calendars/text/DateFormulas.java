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
package io.github.mtrevisan.calendars.text;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * The words and markers of the date formulas of the historical documents that carry no date information, defined once
 * (as tokens) and composed into the patterns used by the date parsers, so that the parsers share the same lists.
 */
public final class DateFormulas{

	/** Words of the date formulas (e.g. "die 5 mensis martii anno Domini 1450", "adì 5 marzo 1500", "5 de marzo de 1950"). */
	public static final List<String> FORMULA_WORDS = List.of("die", "mensis", "anno", "domini", "nativitatis", "incarnationis",
		"adi", "adì", "addi", "addì", "a dì", "the", "of", "de", "del", "di", "du", "des");

	/** Markers of the Anno Domini era. */
	public static final List<String> ANNO_DOMINI_MARKERS = List.of("a.d.", "a. d.", "a.d", "ad");

	/** Suffixes of the ordinal numbers written with digits (e.g. "1st", "2nd", "1°", "1º"). */
	public static final List<String> DAY_ORDINAL_SUFFIXES = List.of("st", "nd", "rd", "th", "°", "º");

	/** Markers of the era before Christ, in the Latin-script languages (lowercase, the dots are optional in the pattern). */
	public static final List<String> BEFORE_CHRIST_MARKERS = List.of("bce", "bc", "ac");

	/** The Unicode spaces that {@link Character#isWhitespace(char)} does not recognize, turned into plain spaces. */
	private static final Pattern EXOTIC_SPACES = Pattern.compile("[\\u00A0\\u2007\\u202F]");

	private static final Pattern FORMULA_PATTERN = Pattern.compile(
		"(?iu)(?<![\\p{L}])(?:" + alternation(FORMULA_WORDS) + ")(?![\\p{L}])");
	private static final Pattern ANNO_DOMINI = Pattern.compile("(?iu)(?:(?<![\\p{L}])(?:" + alternation(ANNO_DOMINI_MARKERS)
		+ ")(?=\\s*\\d)|(?<=\\d)\\s*(?:" + alternation(ANNO_DOMINI_MARKERS) + ")$)");
	private static final Pattern DAY_ORDINAL_SUFFIX = Pattern.compile(
		"(?iu)(?<![\\p{L}\\d])(\\d{1,2})(?:" + alternation(DAY_ORDINAL_SUFFIXES) + ")(?![\\p{L}])");
	/**
	 * Trailing era marker for the years before Christ, as "BC", "BCE", "B.C.", "B.C.E.", "a.C.", "a. C.", "a. de C." (case
	 * insensitive, the letters can be separated by dots and spaces, also non-breaking ones).
	 */
	private static final Pattern BEFORE_CHRIST = Pattern.compile("(?iu)(?<=\\d)\\s*(?:" + eraAlternation(BEFORE_CHRIST_MARKERS)
		+ ")\\s*$");


	/**
	 * The text without the era before Christ.
	 *
	 * @param text         the text without the era marker, with the plain spaces
	 * @param beforeChrist whether the text had the era before Christ
	 */
	public record Era(String text, boolean beforeChrist){}


	private DateFormulas(){}


	/**
	 * Builds the alternation of tokens of a regular expression, the longest first, with the spaces matching any run of
	 * spaces.
	 *
	 * @param tokens the plain tokens
	 * @return the alternation (without the enclosing group)
	 */
	public static String alternation(final List<String> tokens){
		return tokens.stream()
			.sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
			.map(token -> Pattern.quote(token).replace(StringUtils.SPACE, "\\E\\s+\\Q"))
			.collect(Collectors.joining("|"));
	}

	/** The era markers: the letters are separated by optional dots and spaces ("a c", "b c e", "a de c"). */
	private static String eraAlternation(final List<String> tokens){
		return tokens.stream()
			.sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
			.map(token -> {
				final StringBuilder sb = new StringBuilder();
				for(final String part : StringUtils.split(token)){
					if(!sb.isEmpty())
						sb.append("\\s?");
					for(int i = 0; i < part.length(); i ++){
						if(i > 0)
							sb.append("\\.?\\s?");
						sb.append(Pattern.quote(String.valueOf(part.charAt(i))));
					}
					sb.append("\\.?");
				}
				return sb.toString();
			})
			.collect(Collectors.joining("|"));
	}

	/**
	 * Replaces the non-breaking and other exotic Unicode spaces with plain spaces.
	 *
	 * @param text the text
	 * @return the text with plain spaces
	 */
	public static String plainSpaces(final String text){
		return EXOTIC_SPACES.matcher(text)
			.replaceAll(StringUtils.SPACE);
	}

	/**
	 * Removes the formulas that carry no date information: the Latin "die ... mensis ...", "anno (Domini)", the Venetan
	 * "adì", the Romance "de"/"del"/"di" between the components, "A.D.", and the ordinal suffixes of the day ("1st", "1°").
	 *
	 * @param text the date text
	 * @return the text without the formulas, with the spaces normalized
	 */
	public static String removeFormulas(final String text){
		return removeFormulas(text, word -> false);
	}

	/**
	 * Removes the formulas that carry no date information (see {@link #removeFormulas(String)}), except the words that can be
	 * the name (or the abbreviation) of a month or of a season, because a formula word can also be one of them (e.g. "des"
	 * is the Venetan abbreviation of December, and a formula in French).
	 * <p>
	 * A formula word that is a name is kept only if the date has no other name once the formulas are removed: in
	 * "5 de marzo de 1950" the "de" is a formula (even if it is the abbreviation of a month in some language, as it is in
	 * the polyglot parsers), in "10 des 1500" it is the month.
	 * </p>
	 *
	 * @param text the date text
	 * @param isName tells whether a word (in lowercase) can be the name of a month or of a season
	 * @return the text without the formulas, with the spaces normalized
	 */
	public static String removeFormulas(final String text, final Predicate<String> isName){
		final String plain = plainSpaces(text);
		final String result = removeFormulasKeeping(plain, word -> false);
		if(hasName(result, isName))
			return result;

		return removeFormulasKeeping(plain, isName);
	}

	private static boolean hasName(final String text, final Predicate<String> isName){
		for(final String word : StringUtils.split(text))
			if(isName.test(word.toLowerCase(Locale.ROOT)))
				return true;
		return false;
	}

	private static String removeFormulasKeeping(final String text, final Predicate<String> isName){
		String result = replaceUnlessName(FORMULA_PATTERN, text, isName);
		result = replaceUnlessName(ANNO_DOMINI, result, isName);
		result = DAY_ORDINAL_SUFFIX.matcher(result)
			.replaceAll("$1");
		return StringUtils.normalizeSpace(result);
	}

	private static String replaceUnlessName(final Pattern pattern, final String text, final Predicate<String> isName){
		return pattern.matcher(text)
			.replaceAll(match -> (isName.test(match.group().toLowerCase(Locale.ROOT).strip())
				? Matcher.quoteReplacement(match.group())
				: StringUtils.SPACE));
	}

	/**
	 * Removes the era before Christ from the end of a text.
	 *
	 * @param text the text (the formulas, like the Spanish "de" of "a. de C.", must be already removed)
	 * @return the text without the era, and whether it had the era before Christ
	 */
	public static Era extractEra(final String text){
		final String plain = plainSpaces(text);
		final Matcher matcher = BEFORE_CHRIST.matcher(plain);
		return (matcher.find()
			? new Era(plain.substring(0, matcher.start()), true)
			: new Era(plain, false));
	}

	/**
	 * Converts a historical year into the astronomical one.
	 *
	 * @param historicalYear the year (there is no year 0)
	 * @param beforeChrist   whether the year is before Christ
	 * @return the astronomical year (1 BC is the year 0)
	 * @throws IllegalArgumentException if the year is 0 before Christ
	 */
	public static int toAstronomicalYear(final int historicalYear, final boolean beforeChrist){
		if(beforeChrist && historicalYear < 1)
			throw new IllegalArgumentException("There is no year " + historicalYear + " before Christ");

		return (beforeChrist? 1 - historicalYear: historicalYear);
	}

}
