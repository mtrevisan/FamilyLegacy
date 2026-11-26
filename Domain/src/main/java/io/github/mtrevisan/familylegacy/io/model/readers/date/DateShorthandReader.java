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
package io.github.mtrevisan.familylegacy.io.model.readers.date;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Reads the short notation of a date, the one genealogists write in a hurry, and tells the shape of the {@code DateValue} it means.
 * <p>
 * Unlike GEDCOM the notation reaches everything a FLEF date can say: every date of the expression (the bounds too) can be approximate,
 * with its own basis and margin, and a date can be a decade or a century (with its part).
 * </p>
 * <pre>
 * 1850, 12 March 1850              a point
 * abt 1850, c. 1850, ~1850         an approximate point (basis <em>stated</em>)
 * calc 1850                        an approximate point (basis <em>calculated</em>)
 * 1850 ±2, abt 1850 +/-6m          an approximate point with a margin (years if there is no unit)
 * 1850s, 19th century, early XIX c.   a decade, a century (with its part)
 * bef 1850, &lt;1850                 not after 1850
 * aft 1850, &gt;1850                 not before 1850
 * bet 1850 and 1860                between the two (not before 1850, not after 1860)
 * from 1850 to 1860                a period
 * from 1850, until 1860, to 1860   a period open on one side
 * </pre>
 * Every date of the expression can have its own qualifier (e.g. {@code bet abt 1850 and 1860 ±1}). The words are the ones of English,
 * Italian, French, German and Spanish, written in any case, with or without the final dot. A {@code (language)} at the end of the
 * expression is kept at the end of the full dates.
 * <p>
 * The reader does not check the full dates: that is the job of their calendar.
 * </p>
 */
public final class DateShorthandReader{

	/** Why an expression cannot be read: the user interface has a message for each. */
	public enum Reason{

		/** There is nothing to read. */
		EMPTY,
		/** A qualifier or a keyword is not followed by a date. */
		MISSING_VALUE,
		/** {@code between} without the {@code and} that closes it. */
		MISSING_BETWEEN_AND,
		/** The margin is not a positive number with an optional unit, nor an ISO 8601 duration. */
		BAD_MARGIN,
		/** A decade whose year is not a multiple of ten. */
		BAD_DECADE,
		/** The words before the century are not a part of it. */
		BAD_CENTURY_PART

	}

	/** The expression cannot be read. */
	public static final class SyntaxException extends IllegalArgumentException{

		private final Reason reason;


		private SyntaxException(final Reason reason){
			super(reason.name());

			this.reason = reason;
		}

		public Reason getReason(){
			return reason;
		}

	}


	/** The symbol that starts the margin. */
	public static final String MARGIN_SYMBOL = "±";
	private static final String MARGIN_SYMBOL_SLASH = "+/-";
	private static final String MARGIN_SYMBOL_DASH = "+-";
	private static final String MARGIN_BASE = "P";
	private static final String UNIT_YEAR = "Y";
	private static final String UNIT_MONTH = "M";
	private static final String UNIT_WEEK = "W";
	private static final String UNIT_DAY = "D";

	private static final String SYMBOL_ABOUT = "~";
	private static final String SYMBOL_BEFORE = "<";
	private static final String SYMBOL_AFTER = ">";

	/** Words that start an approximate date, basis <em>stated</em>. */
	private static final Set<String> ABOUT_WORDS = words(
		SYMBOL_ABOUT,
		//en
		"about", "abt", "approx", "approximately", "around", "circa", "ca", "c",
		//it
		"intorno", "verso",
		//fr
		"vers",
		//de
		"um",
		//es
		"hacia");
	/** Words that start an approximate date, basis <em>calculated</em>. */
	private static final Set<String> CALCULATED_WORDS = words(
		//en
		"calc", "cal", "calculated",
		//it
		"calcolato",
		//fr
		"calculé", "calcule",
		//de
		"berechnet",
		//es
		"calculado");
	/** Words that start a date that is not after the given one. */
	private static final Set<String> BEFORE_WORDS = words(
		SYMBOL_BEFORE,
		//en
		"before", "bef",
		//it
		"prima",
		//fr
		"avant",
		//de
		"vor",
		//es
		"antes");
	/** Words that start a date that is not before the given one. */
	private static final Set<String> AFTER_WORDS = words(
		SYMBOL_AFTER,
		//en
		"after", "aft",
		//it
		"dopo",
		//fr
		"après", "apres",
		//de
		"nach",
		//es
		"después", "despues");
	/** Words that start an interval. */
	private static final Set<String> BETWEEN_WORDS = words(
		//en
		"between", "bet",
		//it
		"tra", "fra",
		//fr, es
		"entre",
		//de
		"zwischen");
	/** Words that join the two bounds of an interval. */
	private static final Set<String> BETWEEN_AND_WORDS = words(
		//en
		"and", "&",
		//it
		"e",
		//fr
		"et",
		//de
		"und",
		//es
		"y");
	/** Words that start a period. */
	private static final Set<String> FROM_WORDS = words(
		//en
		"from",
		//it
		"da", "dal", "dall", "dalla", "dallo",
		//fr, es
		"de",
		//de
		"von");
	/** Words that start a period with no beginning. */
	private static final Set<String> UNTIL_WORDS = words(
		//en
		"until", "till", "to",
		//it
		"fino", "sino",
		//de
		"bis",
		//es
		"hasta");
	/** Words that join the beginning and the end of a period. */
	private static final Set<String> TO_WORDS = words(
		//en
		"to", "until", "till",
		//it, es
		"a", "al", "all", "alla", "allo",
		//fr
		"à",
		//de
		"bis");
	/** Words that can follow a keyword and are not part of the date (e.g. <em>prima del</em> 1850). */
	private static final Set<String> FILLER_WORDS = words(
		//it
		"di", "del", "dello", "della", "dell", "dei", "il", "lo", "la", "l", "a", "al", "all", "alla", "allo",
		//fr
		"de", "d", "le", "en",
		//es
		"el");

	/** Words that name the century, after (or before) its ordinal. */
	private static final Set<String> CENTURY_WORDS = words(
		//en
		"c", "cent", "century",
		//it
		"secolo", "sec",
		//fr
		"siècle", "siecle",
		//de
		"jahrhundert", "jh",
		//es
		"siglo", "s");
	/** Words that name the century when they are written next to the ordinal (e.g. {@code 19c}; {@code 19s} would be a decade). */
	private static final Set<String> ATTACHED_CENTURY_WORDS = words("c", "cent", "century", "sec", "secolo", "siecle", "siglo");
	/** Suffixes of an ordinal (e.g. 19<em>th</em>). */
	private static final Set<String> ORDINAL_SUFFIXES = words(
		"st", "nd", "rd", "th", "°", "º", "ª", "e", "er", "ème", "eme", "o", "a");
	/** Words between the part and the century that are not part of the notation (e.g. <em>the</em> in <em>first half of the century</em>). */
	private static final Set<String> PART_FILLER_WORDS = words("of", "the", "di", "del", "dello", "della", "dell", "de", "du", "des", "der",
		"la", "le", "el", "il", "lo", "l");
	private static final String[][] PART_PHRASES = {
		//en
		{"early", DateReader.ENUM_PART_EARLY}, {"mid", DateReader.ENUM_PART_MID}, {"middle", DateReader.ENUM_PART_MID},
		{"late", DateReader.ENUM_PART_LATE},
		{"first half", DateReader.ENUM_PART_FIRST_HALF}, {"1st half", DateReader.ENUM_PART_FIRST_HALF},
		{"second half", DateReader.ENUM_PART_SECOND_HALF}, {"2nd half", DateReader.ENUM_PART_SECOND_HALF},
		{"first quarter", DateReader.ENUM_PART_FIRST_QUARTER}, {"1st quarter", DateReader.ENUM_PART_FIRST_QUARTER},
		{"second quarter", DateReader.ENUM_PART_SECOND_QUARTER}, {"2nd quarter", DateReader.ENUM_PART_SECOND_QUARTER},
		{"third quarter", DateReader.ENUM_PART_THIRD_QUARTER}, {"3rd quarter", DateReader.ENUM_PART_THIRD_QUARTER},
		{"fourth quarter", DateReader.ENUM_PART_FOURTH_QUARTER}, {"4th quarter", DateReader.ENUM_PART_FOURTH_QUARTER},
		{"last quarter", DateReader.ENUM_PART_FOURTH_QUARTER},
		//it
		{"inizio", DateReader.ENUM_PART_EARLY}, {"inizi", DateReader.ENUM_PART_EARLY},
		{"metà", DateReader.ENUM_PART_MID}, {"meta", DateReader.ENUM_PART_MID},
		{"fine", DateReader.ENUM_PART_LATE},
		{"prima metà", DateReader.ENUM_PART_FIRST_HALF}, {"prima meta", DateReader.ENUM_PART_FIRST_HALF},
		{"seconda metà", DateReader.ENUM_PART_SECOND_HALF}, {"seconda meta", DateReader.ENUM_PART_SECOND_HALF},
		{"primo quarto", DateReader.ENUM_PART_FIRST_QUARTER}, {"secondo quarto", DateReader.ENUM_PART_SECOND_QUARTER},
		{"terzo quarto", DateReader.ENUM_PART_THIRD_QUARTER}, {"quarto quarto", DateReader.ENUM_PART_FOURTH_QUARTER},
		{"ultimo quarto", DateReader.ENUM_PART_FOURTH_QUARTER},
		//fr
		{"début", DateReader.ENUM_PART_EARLY}, {"debut", DateReader.ENUM_PART_EARLY},
		{"milieu", DateReader.ENUM_PART_MID}, {"fin", DateReader.ENUM_PART_LATE},
		{"première moitié", DateReader.ENUM_PART_FIRST_HALF}, {"premiere moitie", DateReader.ENUM_PART_FIRST_HALF},
		{"seconde moitié", DateReader.ENUM_PART_SECOND_HALF}, {"seconde moitie", DateReader.ENUM_PART_SECOND_HALF},
		{"deuxième moitié", DateReader.ENUM_PART_SECOND_HALF}, {"deuxieme moitie", DateReader.ENUM_PART_SECOND_HALF},
		//de
		{"anfang", DateReader.ENUM_PART_EARLY}, {"mitte", DateReader.ENUM_PART_MID}, {"ende", DateReader.ENUM_PART_LATE},
		{"erste hälfte", DateReader.ENUM_PART_FIRST_HALF}, {"erste halfte", DateReader.ENUM_PART_FIRST_HALF},
		{"zweite hälfte", DateReader.ENUM_PART_SECOND_HALF}, {"zweite halfte", DateReader.ENUM_PART_SECOND_HALF},
		//es
		{"principios", DateReader.ENUM_PART_EARLY}, {"comienzos", DateReader.ENUM_PART_EARLY},
		{"mediados", DateReader.ENUM_PART_MID}, {"finales", DateReader.ENUM_PART_LATE},
		{"primera mitad", DateReader.ENUM_PART_FIRST_HALF}, {"segunda mitad", DateReader.ENUM_PART_SECOND_HALF}
	};
	private static final Map<String, String> PARTS = new HashMap<>(PART_PHRASES.length);
	static{
		for(final String[] phrase : PART_PHRASES)
			PARTS.put(phrase[0], phrase[1]);
	}

	/** Units of the margin. */
	private static final Set<String> YEAR_UNITS = words("y", "yr", "yrs", "year", "years", "a", "an", "ans", "anno", "anni", "jahr", "jahre",
		"j", "año", "años", "ano", "anos");
	private static final Set<String> MONTH_UNITS = words("m", "mo", "month", "months", "mese", "mesi", "mois", "monat", "monate", "mes",
		"meses");
	private static final Set<String> WEEK_UNITS = words("w", "wk", "week", "weeks", "settimana", "settimane", "semaine", "semaines", "woche",
		"wochen", "semana", "semanas");
	private static final Set<String> DAY_UNITS = words("d", "day", "days", "g", "giorno", "giorni", "jour", "jours", "tag", "tage",
		"día", "días", "dia", "dias");

	private static final Pattern ISO_DURATION = Pattern.compile("^P(?=\\d)(?:\\d+Y)?(?:\\d+M)?(?:\\d+W)?(?:\\d+D)?$");

	private static final int MAX_MARGIN_DIGITS = 4;
	private static final int MIN_DECADE_DIGITS = 3;
	private static final int MAX_DECADE_DIGITS = 4;
	private static final int MAX_CENTURY = 40;
	private static final char APOSTROPHE = '\'';
	private static final char APOSTROPHE_TYPOGRAPHIC = '’';
	private static final char DOT = '.';


	private DateShorthandReader(){}


	/**
	 * Reads an expression.
	 *
	 * @param text	The expression.
	 * @return	The date it means.
	 * @throws SyntaxException	If the expression cannot be read.
	 */
	public static DateShorthand parse(final String text){
		String body = StringUtils.strip(text);
		if(StringUtils.isEmpty(body))
			throw new SyntaxException(Reason.EMPTY);

		//the language of the date stays at the end of the full dates
		String languageSuffix = null;
		final int languageStart = body.lastIndexOf('(');
		if(body.endsWith(")") && languageStart >= 0){
			languageSuffix = body.substring(languageStart);
			body = StringUtils.strip(body.substring(0, languageStart));
		}

		body = StringUtils.replace(body, MARGIN_SYMBOL_SLASH, MARGIN_SYMBOL);
		body = StringUtils.replace(body, MARGIN_SYMBOL_DASH, MARGIN_SYMBOL);
		final List<String> tokens = tokenize(body);
		if(tokens.isEmpty())
			throw new SyntaxException(Reason.EMPTY);

		final String keyword = key(tokens.getFirst());
		final List<String> rest = afterKeyword(tokens);
		if(BEFORE_WORDS.contains(keyword))
			return new DateShorthand(DateShorthand.Kind.BOUNDED, null, single(rest, languageSuffix));
		if(AFTER_WORDS.contains(keyword))
			return new DateShorthand(DateShorthand.Kind.BOUNDED, single(rest, languageSuffix), null);
		if(BETWEEN_WORDS.contains(keyword)){
			final int and = indexOf(rest, BETWEEN_AND_WORDS);
			if(and < 0)
				throw new SyntaxException(Reason.MISSING_BETWEEN_AND);

			return new DateShorthand(DateShorthand.Kind.BOUNDED, single(rest.subList(0, and), languageSuffix),
				single(rest.subList(and + 1, rest.size()), languageSuffix));
		}
		if(FROM_WORDS.contains(keyword)){
			final int to = indexOf(rest, TO_WORDS);
			if(to < 0)
				return new DateShorthand(DateShorthand.Kind.SPANNING, single(rest, languageSuffix), null);

			return new DateShorthand(DateShorthand.Kind.SPANNING, single(rest.subList(0, to), languageSuffix),
				single(rest.subList(to + 1, rest.size()), languageSuffix));
		}
		if(UNTIL_WORDS.contains(keyword))
			return new DateShorthand(DateShorthand.Kind.SPANNING, null, single(rest, languageSuffix));

		return new DateShorthand(DateShorthand.Kind.POINT, single(tokens, languageSuffix), null);
	}

	/** Gives the tokens that follow the first one, without the words that only decorate the keyword. */
	private static List<String> afterKeyword(final List<String> tokens){
		final List<String> rest = new ArrayList<>(tokens.subList(1, tokens.size()));
		skipFillers(rest);
		return rest;
	}

	private static void skipFillers(final List<String> tokens){
		while(!tokens.isEmpty() && FILLER_WORDS.contains(key(tokens.getFirst())))
			tokens.removeFirst();
	}

	private static int indexOf(final List<String> tokens, final Set<String> words){
		for(int i = 0; i < tokens.size(); i ++)
			if(words.contains(key(tokens.get(i))))
				return i;
		return -1;
	}

	/** Reads a single date: [qualifier] value [± margin]. */
	private static DateShorthand.Single single(final List<String> tokens, final String languageSuffix){
		final List<String> date = new ArrayList<>(tokens);
		skipFillers(date);

		String basis = null;
		if(!date.isEmpty()){
			final String keyword = key(date.getFirst());
			if(ABOUT_WORDS.contains(keyword))
				basis = DateReader.ENUM_BASIS_STATED;
			else if(CALCULATED_WORDS.contains(keyword))
				basis = DateReader.ENUM_BASIS_CALCULATED;
			if(basis != null){
				date.removeFirst();
				skipFillers(date);
			}
		}

		String margin = null;
		final int marginStart = date.indexOf(MARGIN_SYMBOL);
		if(marginStart >= 0){
			margin = parseMargin(date.subList(marginStart + 1, date.size()));
			//a margin is an approximation
			if(basis == null)
				basis = DateReader.ENUM_BASIS_STATED;
			date.subList(marginStart, date.size())
				.clear();
		}

		if(date.isEmpty())
			throw new SyntaxException(Reason.MISSING_VALUE);

		final Integer decade = parseDecade(date);
		if(decade != null)
			return new DateShorthand.Single(null, decade, null, null, basis, margin);

		final int[] century = parseCentury(date);
		if(century != null){
			final String part = parseCenturyPart(date.subList(0, century[1]));
			return new DateShorthand.Single(null, null, century[0], part, basis, margin);
		}

		final String fullDate = String.join(StringUtils.SPACE, date) + (languageSuffix != null? StringUtils.SPACE + languageSuffix: StringUtils.EMPTY);
		return new DateShorthand.Single(fullDate, null, null, null, basis, margin);
	}

	/** Reads a margin: an ISO 8601 duration, or a positive number with an optional unit (years if missing). */
	private static String parseMargin(final List<String> tokens){
		final String margin = String.join(StringUtils.EMPTY, tokens);
		if(margin.isEmpty())
			throw new SyntaxException(Reason.BAD_MARGIN);

		if(margin.charAt(0) == 'P' || margin.charAt(0) == 'p'){
			final String iso = margin.toUpperCase(Locale.ROOT);
			if(!ISO_DURATION.matcher(iso).matches())
				throw new SyntaxException(Reason.BAD_MARGIN);

			return iso;
		}

		final int digits = leadingDigits(margin);
		if(digits == 0 || digits > MAX_MARGIN_DIGITS)
			throw new SyntaxException(Reason.BAD_MARGIN);

		final int amount = Integer.parseInt(margin.substring(0, digits));
		final String unit = key(margin.substring(digits));
		final String code;
		if(unit.isEmpty() || YEAR_UNITS.contains(unit))
			code = UNIT_YEAR;
		else if(MONTH_UNITS.contains(unit))
			code = UNIT_MONTH;
		else if(WEEK_UNITS.contains(unit))
			code = UNIT_WEEK;
		else if(DAY_UNITS.contains(unit))
			code = UNIT_DAY;
		else
			throw new SyntaxException(Reason.BAD_MARGIN);
		if(amount == 0)
			throw new SyntaxException(Reason.BAD_MARGIN);

		return MARGIN_BASE + amount + code;
	}

	/** @return	The first year of the decade if the tokens are one (e.g. {@code 1850s}), {@code null} otherwise. */
	private static Integer parseDecade(final List<String> tokens){
		if(tokens.size() != 1)
			return null;

		String decade = tokens.getFirst()
			.toLowerCase(Locale.ROOT);
		if(!decade.endsWith("s"))
			return null;

		decade = StringUtils.removeEnd(StringUtils.removeEnd(decade, "s"), String.valueOf(APOSTROPHE));
		final int digits = leadingDigits(decade);
		if(digits != decade.length() || digits < MIN_DECADE_DIGITS || digits > MAX_DECADE_DIGITS)
			return null;

		if(decade.charAt(digits - 1) != '0')
			throw new SyntaxException(Reason.BAD_DECADE);

		return Integer.valueOf(decade);
	}

	/**
	 * @return	The ordinal of the century and the number of tokens that precede the century (they are its part), {@code null} if the tokens
	 * 	are not a century.
	 */
	private static int[] parseCentury(final List<String> tokens){
		for(int i = 0; i < tokens.size(); i ++){
			final String token = key(tokens.get(i));

			//the ordinal and the word are one token (e.g. 19c)
			final int digits = leadingDigits(token);
			if(digits > 0 && digits < token.length() && ATTACHED_CENTURY_WORDS.contains(token.substring(digits))){
				final int ordinal = Integer.parseInt(token.substring(0, digits));
				return (ordinal > 0 && ordinal <= MAX_CENTURY && i == tokens.size() - 1? new int[]{ordinal, i}: null);
			}

			if(CENTURY_WORDS.contains(token)){
				//19th century
				final int before = (i > 0? parseOrdinal(tokens.get(i - 1)): 0);
				if(before > 0)
					return (i == tokens.size() - 1? new int[]{before, i - 1}: null);

				//sec. XIX
				final int after = (i + 1 < tokens.size()? parseOrdinal(tokens.get(i + 1)): 0);
				if(after > 0)
					return (i + 1 == tokens.size() - 1? new int[]{after, i}: null);
			}
		}
		return null;
	}

	/** @return	The ordinal (e.g. {@code 19}, {@code 19th}, {@code XIX}), {@code 0} if the token is not one. */
	private static int parseOrdinal(final String token){
		final String ordinal = key(token);
		if(ordinal.isEmpty())
			return 0;

		final int digits = leadingDigits(ordinal);
		if(digits > 0){
			final String suffix = ordinal.substring(digits);
			if(digits > 2 || !suffix.isEmpty() && !ORDINAL_SUFFIXES.contains(suffix))
				return 0;

			final int value = Integer.parseInt(ordinal.substring(0, digits));
			return (value <= MAX_CENTURY? value: 0);
		}
		return parseRoman(ordinal);
	}

	private static int parseRoman(final String roman){
		int total = 0;
		int previous = 0;
		for(int i = roman.length() - 1; i >= 0; i --){
			final int value = switch(roman.charAt(i)){
				case 'i' -> 1;
				case 'v' -> 5;
				case 'x' -> 10;
				case 'l' -> 50;
				default -> 0;
			};
			if(value == 0)
				return 0;

			total += (value < previous? -value: value);
			previous = Math.max(previous, value);
		}
		return (total <= MAX_CENTURY? total: 0);
	}

	private static String parseCenturyPart(final List<String> tokens){
		final List<String> words = new ArrayList<>(tokens.size());
		for(final String token : tokens){
			final String word = key(token);
			if(!PART_FILLER_WORDS.contains(word))
				words.add(word);
		}
		if(words.isEmpty())
			return null;

		final String part = PARTS.get(String.join(StringUtils.SPACE, words));
		if(part == null)
			throw new SyntaxException(Reason.BAD_CENTURY_PART);

		return part;
	}


	/** Splits the expression on blanks and around the symbols and the dots and apostrophes that stick a keyword to its date. */
	private static List<String> tokenize(final String body){
		final List<String> tokens = new ArrayList<>();
		for(final String word : StringUtils.split(body))
			splitToken(word, tokens);
		return tokens;
	}

	private static void splitToken(String word, final List<String> tokens){
		//symbols stick to the date (e.g. ~1850, <1850)
		while(!word.isEmpty() && isSymbol(word.charAt(0))){
			tokens.add(word.substring(0, 1));
			word = word.substring(1);
		}
		if(word.isEmpty())
			return;

		final int margin = word.indexOf(MARGIN_SYMBOL);
		if(margin > 0){
			splitToken(word.substring(0, margin), tokens);
			tokens.add(MARGIN_SYMBOL);
			splitToken(word.substring(margin + 1), tokens);
			return;
		}

		//a keyword stuck to the date by a dot or an apostrophe (c.1850, dell'1850)
		for(int i = 1; i < word.length() - 1; i ++){
			final char c = word.charAt(i);
			if((c == DOT || c == APOSTROPHE || c == APOSTROPHE_TYPOGRAPHIC) && isLexiconWord(word.substring(0, i).toLowerCase(Locale.ROOT))){
				tokens.add(word.substring(0, i));
				splitToken(word.substring(i + 1), tokens);
				return;
			}
		}
		tokens.add(word);
	}

	private static boolean isSymbol(final char c){
		return (c == SYMBOL_ABOUT.charAt(0) || c == SYMBOL_BEFORE.charAt(0) || c == SYMBOL_AFTER.charAt(0) || c == MARGIN_SYMBOL.charAt(0));
	}

	private static boolean isLexiconWord(final String word){
		return (ABOUT_WORDS.contains(word) || CALCULATED_WORDS.contains(word) || BEFORE_WORDS.contains(word) || AFTER_WORDS.contains(word)
			|| BETWEEN_WORDS.contains(word) || FROM_WORDS.contains(word) || UNTIL_WORDS.contains(word) || FILLER_WORDS.contains(word));
	}

	/** @return	The token as it is compared with the words: lower case, without the final dot. */
	private static String key(final String token){
		return StringUtils.stripEnd(token.toLowerCase(Locale.ROOT), String.valueOf(DOT));
	}

	private static int leadingDigits(final String text){
		int digits = 0;
		while(digits < text.length() && text.charAt(digits) >= '0' && text.charAt(digits) <= '9')
			digits ++;
		return digits;
	}

	private static Set<String> words(final String... words){
		return Set.copyOf(Arrays.asList(words));
	}

}
