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
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import org.apache.commons.lang3.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;


/**
 * Gregorian months and days, with the year counted from a fixed epoch.
 */
public class EraOffsetCalendarStrategy implements CalendarConverterPlugin{

	private static final String ERA_MINGUO = "minguo";
	private static final String ERA_BEFORE_MINGUO = "before_minguo";
	private static final String ERA_DANGI = "dangi";
	private static final String ERA_JUCHE = "juche";

	/**
	 * Minguo calendar of the Republic of China (Taiwan): the year 1 is 1912, the foundation of the Republic
	 * (e.g. "民國50年3月1日"); the years before are counted backward ("民前1年" is 1911).
	 */
	public static final EraOffsetCalendarStrategy MINGUO = new EraOffsetCalendarStrategy(
		Map.of("中華民國", ERA_MINGUO, "中华民国", ERA_MINGUO, "民國", ERA_MINGUO, "民国", ERA_MINGUO, "minguo", ERA_MINGUO,
			"roc", ERA_MINGUO, "民前", ERA_BEFORE_MINGUO),
		ERA_MINGUO,
		Map.of(ERA_MINGUO, year -> year + 1911, ERA_BEFORE_MINGUO, year -> 1912 - year));

	/**
	 * Korean Dangi calendar, counted from the legendary foundation of Gojoseon in 2333 BC (e.g. "단기 4290년 3월 1일"),
	 * official in South Korea from 1948 to 1961.
	 */
	public static final EraOffsetCalendarStrategy DANGI = new EraOffsetCalendarStrategy(
		Map.of("단기", ERA_DANGI, "檀紀", ERA_DANGI, "檀纪", ERA_DANGI, "dangi", ERA_DANGI),
		ERA_DANGI,
		Map.of(ERA_DANGI, year -> year - 2333));


	/**
	 * Juche calendar of North Korea, counted from 1912 (the birth of Kim Il-sung) as the year 1 (e.g. "주체112(2023)년"),
	 * in official use from 1997.
	 */
	public static final EraOffsetCalendarStrategy JUCHE = new EraOffsetCalendarStrategy(
		Map.of("주체", ERA_JUCHE, "主體", ERA_JUCHE, "主体", ERA_JUCHE, "juche", ERA_JUCHE),
		ERA_JUCHE,
		Map.of(ERA_JUCHE, year -> year + 1911));

	/** A parenthesized text, e.g. the Gregorian year written next to the era year. */
	private static final Pattern PARENTHESIZED = Pattern.compile("[(（][^)）]*[)）]");


	private final Map<String, String> eraAliases;
	private final String defaultEra;
	private final Map<String, IntUnaryOperator> toGregorianYear;


	private EraOffsetCalendarStrategy(final Map<String, String> eraAliases, final String defaultEra,
			final Map<String, IntUnaryOperator> toGregorianYear){
		this.eraAliases = eraAliases;
		this.defaultEra = defaultEra;
		this.toGregorianYear = toGregorianYear;
	}


	@Override
	public long parseToJdn(final String rawDate){
		final EastAsianDateInput input = parse(rawDate);
		if(input.leapMonth())
			throw new IllegalArgumentException("There are no leap months in the Gregorian calendar: " + rawDate);

		final int year = toGregorianYear.get(input.era())
			.applyAsInt(input.year());
		final int month = input.monthOrFirst();
		final LocalDate date;
		try{
			// validate the day of the month
			date = (input.day() == EastAsianDateInput.LAST_DAY
				? LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
				: LocalDate.of(year, month, input.dayOrFirst()));
		}
		catch(final DateTimeException dte){
			throw new IllegalArgumentException("Invalid date: " + rawDate, dte);
		}
		return GregorianCalendarStrategy.toJdn(year, month, date.getDayOfMonth());
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		return parse(rawDate).toMetadata();
	}

	private EastAsianDateInput parse(final String rawDate){
		// the Gregorian year written next to the era year, e.g. "주체112(2023)년"
		final String withoutGregorianYear = (rawDate != null? PARENTHESIZED.matcher(rawDate).replaceAll(StringUtils.SPACE): null);
		return EastAsianEraDateParser.parse(withoutGregorianYear, eraAliases, defaultEra);
	}

}
