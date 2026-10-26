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
package io.github.mtrevisan.calendars.nepali;

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * Bikram Sambat, the official solar calendar of Nepal (e.g. "1 Baisakh 2080", "२०८० बैशाख १", "2080/01/01").
 * <p>
 * The months are not computed by a public rule, but published every year by the Nepal Panchanga Nirnayak Samiti: the
 * conversion uses the table of the month lengths of 1970-2100 BS (see the resource for its sources and its uncertain
 * years; the years from 2084 are projections).
 */
public class NepaliCalendarStrategy implements CalendarConverterPlugin{

	private static final String RESOURCE = "bikram-sambat.csv";
	/** 1 Baisakh of the first year of the table (1970 BS) is 13 April 1913. */
	private static final long FIRST_NEW_YEAR_JDN = GregorianCalendarStrategy.toJdn(1913, 4, 13);
	private static final int MIN_YEAR = 1000;

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		registerMonth(1, "Baisakh", "Baishakh", "Vaisakh", "Vaishakh", "Besakh", "बैशाख", "वैशाख", "बैसाख");
		registerMonth(2, "Jestha", "Jeth", "Jeshtha", "Jyeshtha", "जेठ", "जेष्ठ", "ज्येष्ठ");
		registerMonth(3, "Asar", "Asadh", "Ashadh", "Ashar", "असार", "आषाढ", "आषाढ़");
		registerMonth(4, "Shrawan", "Shravan", "Saun", "Sawan", "साउन", "श्रावण", "सावन");
		registerMonth(5, "Bhadra", "Bhadau", "Bhadon", "भदौ", "भाद्र");
		registerMonth(6, "Asoj", "Ashwin", "Aswin", "Ashoj", "असोज", "आश्विन", "आसोज");
		registerMonth(7, "Kartik", "Kattik", "कार्तिक", "कात्तिक");
		registerMonth(8, "Mangsir", "Marga", "Mangshir", "Margashirsha", "मंसिर", "मङ्सिर", "मार्ग");
		registerMonth(9, "Poush", "Push", "Paush", "Pus", "पुस", "पौष", "पुष");
		registerMonth(10, "Magh", "Mah", "माघ");
		registerMonth(11, "Falgun", "Phagun", "Fagun", "Phalgun", "फागुन", "फाल्गुन");
		registerMonth(12, "Chaitra", "Chait", "Chaitr", "चैत", "चैत्र");
	}
	/** Era markers ("BS", "B.S.", "Vikram Samvat", "वि.सं."). */
	private static final Set<String> ERA_WORDS = Set.of("bs", "b", "s", "vs", "v", "sambat", "samvat", "bikram", "vikram",
		"vikrami", "nepal", "sal", TextNormalizer.normalize("वि"), TextNormalizer.normalize("सं"),
		TextNormalizer.normalize("साल"), TextNormalizer.normalize("विक्रम"), TextNormalizer.normalize("संवत्"));

	/** The month lengths of each year, and the Julian Day Number of its first day. */
	private static final TreeMap<Integer, int[]> MONTH_LENGTHS = new TreeMap<>();
	private static final Map<Integer, Long> NEW_YEARS = new HashMap<>();
	static{
		load();
	}

	private static void registerMonth(final int month, final String... names){
		for(final String name : names)
			MONTHS.put(TextNormalizer.normalize(name), month);
	}


	@Override
	public long parseToJdn(final String rawDate){
		final int[] date = parse(rawDate);
		final int year = date[0];
		final int month = (date[1] != 0? date[1]: 1);
		final int day = (date[2] != 0? date[2]: 1);

		final int[] lengths = MONTH_LENGTHS.get(year);
		if(lengths == null)
			throw new IllegalArgumentException("The Bikram Sambat year " + year + " is outside the supported range "
				+ MONTH_LENGTHS.firstKey() + "-" + MONTH_LENGTHS.lastKey() + ": " + rawDate);
		if(day < 1 || day > lengths[month - 1])
			throw new IllegalArgumentException("The month " + month + " of " + year + " BS has " + lengths[month - 1] + " days: "
				+ rawDate);

		long jdn = NEW_YEARS.get(year);
		for(int m = 1; m < month; m ++)
			jdn += lengths[m - 1];
		return jdn + day - 1;
	}

	/**
	 * @return the year, the month (0 if missing), and the day (0 if missing)
	 */
	private static int[] parse(final String rawDate){
		final List<String> words = new ArrayList<>();
		final String[] wordList = TextNormalizer.tokenize(rawDate);
		for(final String word : wordList)
			if(!ERA_WORDS.contains(word))
				words.add(word);
		if(words.isEmpty() || words.size() > 3)
			throw new IllegalArgumentException("Invalid Bikram Sambat date: " + rawDate);

		int year = 0;
		int month = 0;
		int day = 0;
		final List<Integer> numbers = new ArrayList<>();
		for(final String word : words){
			final Integer named = MONTHS.get(word);
			if(named != null)
				month = named;
			else if(StringUtils.isNumeric(word))
				numbers.add(Integer.parseInt(word));
			else
				throw new IllegalArgumentException("Unknown Bikram Sambat month '" + word + "': " + rawDate);
		}
		for(final int number : numbers)
			if(number >= MIN_YEAR && year == 0)
				year = number;
		if(year == 0)
			throw new IllegalArgumentException("Missing year in the Bikram Sambat date: " + rawDate);
		numbers.remove(Integer.valueOf(year));

		if(month == 0 && !numbers.isEmpty()){
			// numeric layout: "2080/01/15" (year, month, day) or "15/01/2080"
			final boolean yearFirst = (StringUtils.isNumeric(words.getFirst()) && Integer.parseInt(words.getFirst()) == year);
			month = (yearFirst? numbers.getFirst(): numbers.getLast());
			numbers.remove(yearFirst? 0: numbers.size() - 1);
		}
		if(!numbers.isEmpty())
			day = numbers.getFirst();
		if(month < 0 || month > 12)
			throw new IllegalArgumentException("Month must be between 1 and 12: " + rawDate);
		return new int[]{year, month, day};
	}

	private static void load(){
		try(final InputStream is = NepaliCalendarStrategy.class.getResourceAsStream(RESOURCE)){
			if(is == null)
				throw new IllegalStateException("Missing resource " + RESOURCE);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			while((line = reader.readLine()) != null){
				if(line.isBlank() || line.startsWith("#") || line.startsWith("year"))
					continue;

				final String[] fields = line.split(";");
				final int[] lengths = new int[12];
				for(int i = 0; i < 12; i ++)
					lengths[i] = Integer.parseInt(fields[i + 1].trim());
				MONTH_LENGTHS.put(Integer.parseInt(fields[0].trim()), lengths);
			}
		}
		catch(final IOException ioe){
			throw new UncheckedIOException(ioe);
		}

		long newYear = FIRST_NEW_YEAR_JDN;
		final Set<Map.Entry<Integer, int[]>> entries = MONTH_LENGTHS.entrySet();
		for(final Map.Entry<Integer, int[]> entry : entries){
			NEW_YEARS.put(entry.getKey(), newYear);
			final int[] lengths = entry.getValue();
			for(final int length : lengths)
				newYear += length;
		}
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final int[] input = parse(rawDate);
		return CalendarMetadata.createFromMonthDay(input[1], input[2]);
	}

}
