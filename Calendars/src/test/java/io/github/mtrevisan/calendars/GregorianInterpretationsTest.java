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
package io.github.mtrevisan.calendars;

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import io.github.mtrevisan.calendars.spi.DateInterpretation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;


/**
 * Readings of the dates whose month word has more than one meaning, offered to the user to choose from.
 */
class GregorianInterpretationsTest{

	@Test
	void seasonAndMonth(){
		// "est" is August in Cornish and summer in Italian: the date is kept as written, followed by its language; the
		// reading chosen by the parser (the month) comes first
		final List<DateInterpretation> readings = CalendarType.GREGORIAN.getInterpretations("est 1850", Locale.ENGLISH);
		Assertions.assertEquals(List.of("est 1850 (Cornish)", "est 1850 (Italian)"), expressions(readings));
		Assertions.assertEquals(List.of("August", "summer"), readings.stream().map(DateInterpretation::meaning).toList());
		// "pri" is spring in Italian and April in Albanian: the reading chosen by the parser (the season) comes first, and
		// the language of the user is preferred
		Assertions.assertEquals(List.of("pri 2024 (italiano)", "pri 2024 (albanese)"), expressions("pri 2024", Locale.ITALIAN));
	}

	@Test
	void seasonOfDifferentLanguages(){
		// "yaz" is spring in Azerbaijani and summer in Turkish: the parser rejects it, the user can choose
		Assertions.assertEquals(Set.of("yaz 2024 (azerbaigiano)", "yaz 2024 (turco)"),
			Set.copyOf(expressions("yaz 2024", Locale.ITALIAN)));
	}

	@Test
	void monthOfDifferentLanguages(){
		// "lis" is November in Czech and Polish, and October in Croatian
		assertReadings("lis 1850", Set.of(10, 11));
		// "pay" is November in Asturian, July in Nuer (and autumn in Azerbaijani and Kurdish, without the day)
		Assertions.assertEquals(List.of("1 pay 2006 (Asturian)", "1 pay 2006 (Nuer)"),
			expressions("1 pay 2006", Locale.ENGLISH));
	}

	@Test
	void languageOfTheDate(){
		// the name of the language in any language, or its code
		Assertions.assertEquals(11, GregorianCalendarParser.parse("1 pay 2006 (asturiano)").month());
		Assertions.assertEquals(11, GregorianCalendarParser.parse("1 Pay 2006 (Asturian)").month());
		Assertions.assertEquals(11, GregorianCalendarParser.parse("1 pay 2006 (ast)").month());
		Assertions.assertEquals(7, GregorianCalendarParser.parse("pay 2006 (nuer)").month());
		Assertions.assertEquals(3, GregorianCalendarParser.parse("15 03 1850 (italiano)").month());
		// the word is not a month or season in the language
		Assertions.assertThrows(IllegalArgumentException.class, () -> GregorianCalendarParser.parse("1 pay 2006 (italiano)"));
		// a date with its language has a single reading
		assertUnambiguous("pay 2006 (asturiano)");
	}

	@Test
	void languageThatDoesNotResolveTheWord(){
		// "hydref" is both October and autumn in Welsh, whose other names are ambiguous too: the word is replaced
		Assertions.assertEquals(Set.of("October 1850", "autumn 1850"), Set.copyOf(expressions("hydref 1850", Locale.ENGLISH)));
		Assertions.assertEquals(Set.of("October 1850", "autumn 1850"),
			Set.copyOf(expressions("hydref 1850 (Welsh)", Locale.ENGLISH)));
	}

	@Test
	void westernMonthHidesTheOtherLanguages(){
		// "may" is also November in Basaa and October in Duala, "mar" is also May in Teso, "jan" is also June in Sotho
		assertUnambiguous("May 1850");
		assertUnambiguous("mar 1850");
		assertUnambiguous("jan 1850");
	}

	@Test
	void dayExcludesSeasonsAndShorterMonths(){
		// a season has no days
		assertUnambiguous("15 pri 2024");
		// November has 30 days
		assertUnambiguous("31 lis 1850");
		assertReadings("15 lis 1850", Set.of(10, 11));
	}

	@Test
	void dateIsKeptAsWritten(){
		assertReadings("die 3 mensis lis anno 1850", Set.of(10, 11));
		assertReadings("3-lis-1850", Set.of(10, 11));
		Assertions.assertEquals(List.of("Est 1850 (Cornish)", "Est 1850 (Italian)"), expressions("Est 1850", Locale.ENGLISH));
		final List<String> easterReadings = expressions(CalendarType.EASTER_STYLE.getInterpretations("15 lis 1400 avant Pâques",
			Locale.ENGLISH));
		final Set<Integer> easterMonths = new HashSet<>();
		for(final String reading : easterReadings){
			// the Easter qualifier is kept, and the language follows it
			Assertions.assertTrue(reading.startsWith("15 lis 1400 avant Pâques ("), reading);
			easterMonths.add(GregorianCalendarParser.parse(reading.replace(" avant Pâques", "")).month());
		}
		Assertions.assertEquals(Set.of(10, 11), easterMonths);
		Assertions.assertEquals(List.of("est 1850 (Cornish)", "est 1850 (Italian)"),
			expressions(CalendarType.JULIAN.getInterpretations("est 1850", Locale.ENGLISH)));
	}

	@Test
	void unambiguousDates(){
		assertUnambiguous("estate 2024");
		assertUnambiguous("15 agosto 1850");
		assertUnambiguous("1850");
		assertUnambiguous("15 03 1850");
		assertUnambiguous("not a date");
		// the calendars that do not offer readings return no readings
		Assertions.assertEquals(List.of(), CalendarType.HEBREW.getInterpretations("est 1850", Locale.ITALIAN));
	}

	@Test
	void everyReadingHasASingleMeaning() throws IOException{
		final Set<String> words = new TreeSet<>(readSeasonWords());
		for(final Locale locale : Locale.getAvailableLocales())
			for(final Month month : Month.values())
				for(final TextStyle style : new TextStyle[]{TextStyle.FULL, TextStyle.SHORT})
					words.add(month.getDisplayName(style, locale).toLowerCase(Locale.ROOT));

		final List<String> errors = new ArrayList<>();
		int ambiguousWords = 0;
		for(final String word : words)
			for(final Locale locale : new Locale[]{Locale.ITALIAN, Locale.ENGLISH}){
				final List<String> readings = expressions(GregorianCalendarParser.interpretations(word + " 1850", locale));
				if(readings.isEmpty())
					continue;

				ambiguousWords ++;
				final Set<Long> jdns = new HashSet<>();
				for(final String reading : readings){
					final List<String> readingsOfReading = expressions(GregorianCalendarParser.interpretations(reading, locale));
					if(!readingsOfReading.isEmpty())
						errors.add(word + ": the reading " + reading + " is ambiguous " + readingsOfReading);
					else
						jdns.add(CalendarType.GREGORIAN.parseToJdn(reading));
				}
				if(jdns.size() != readings.size())
					errors.add(word + ": the readings " + readings + " are not different dates");
			}

		Assertions.assertEquals(List.of(), errors);
		Assertions.assertTrue(ambiguousWords > 0);
	}


	private static void assertUnambiguous(final String date){
		Assertions.assertEquals(List.of(), CalendarType.GREGORIAN.getInterpretations(date, Locale.ENGLISH));
	}

	/** Each reading is the date as written followed by a language, and the readings are the given months. */
	private static void assertReadings(final String date, final Set<Integer> months){
		final List<String> readings = expressions(date, Locale.ENGLISH);
		final Set<Integer> readMonths = new HashSet<>();
		for(final String reading : readings){
			Assertions.assertTrue(reading.startsWith(date + " ("), reading);
			readMonths.add(GregorianCalendarParser.parse(reading).month());
		}
		Assertions.assertEquals(months.size(), readings.size());
		Assertions.assertEquals(months, readMonths);
	}

	private static List<String> expressions(final String date, final Locale locale){
		return expressions(CalendarType.GREGORIAN.getInterpretations(date, locale));
	}

	private static List<String> expressions(final List<DateInterpretation> readings){
		return readings.stream()
			.map(DateInterpretation::expression)
			.toList();
	}

	private static Set<String> readSeasonWords() throws IOException{
		final Set<String> words = new HashSet<>();
		try(final InputStream is = GregorianCalendarParser.class.getResourceAsStream("seasons.csv");
				final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
			reader.lines()
				.filter(line -> !line.isBlank() && !line.startsWith("#"))
				.forEach(line -> {
					final String[] fields = line.split(";", -1);
					for(int season = 1; season < fields.length; season ++)
						words.addAll(List.of(fields[season].split(",")));
				});
		}
		return words;
	}

}
