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
package io.github.mtrevisan.calendars.yearstart;

import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import io.github.mtrevisan.calendars.gregorian.GregorianDateInput;
import io.github.mtrevisan.calendars.gregorian.Indiction;
import io.github.mtrevisan.calendars.liturgical.Computus;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.DateInterpretation;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Style of Easter (<i>mos gallicanus</i>), used in France until the Edict of Roussillon (1564, applied from 1567) and in
 * parts of the Low Countries: the year N starts on Easter Sunday of the year N and ends on the eve of the following
 * Easter.
 * <p>
 * Since Easter moves, the style year has from 330 to 400 days: some days of spring occur twice in the same year (once
 * after its Easter and once before the next one), and some do not occur at all. The documents then add "avant Pâques"
 * or "après Pâques"; the same qualifier ("avant/après Pâques", "ante/post Pascha", "before/after Easter",
 * "prima/dopo Pasqua") picks the occurrence here, and an expression that is still ambiguous is rejected.
 * <p>
 * Days and months follow the Julian calendar, and the Gregorian one from 20 December 1582 (as in France).
 */
public class EasterStyleCalendarStrategy implements CalendarConverterPlugin{

	private static final CalendarReform REFORM = CalendarReform.FRANCE;

	private static final Pattern BEFORE_EASTER = Pattern.compile(
		"(?iu)(?<![\\p{L}])(?:avant|ante|before|prima di|prima della|prima|vor)\\s+(?:p[aâ]ques|pascha[em]?|easter|pasqua|ostern)(?![\\p{L}])");
	private static final Pattern AFTER_EASTER = Pattern.compile(
		"(?iu)(?<![\\p{L}])(?:apr[eè]s|post|after|dopo|nach)\\s+(?:p[aâ]ques|pascha[em]?|easter|pasqua|ostern)(?![\\p{L}])");

	private enum Qualifier{NONE, BEFORE_EASTER, AFTER_EASTER}


	@Override
	public long parseToJdn(final String rawDate){
		Qualifier qualifier = Qualifier.NONE;
		String text = rawDate;
		final Matcher before = BEFORE_EASTER.matcher(text);
		if(before.find()){
			qualifier = Qualifier.BEFORE_EASTER;
			text = before.replaceAll(StringUtils.SPACE);
		}
		final Matcher after = AFTER_EASTER.matcher(text);
		if(after.find()){
			if(qualifier != Qualifier.NONE)
				throw new IllegalArgumentException("Contradictory Easter qualifiers: " + rawDate);
			qualifier = Qualifier.AFTER_EASTER;
			text = after.replaceAll(StringUtils.SPACE);
		}

		final GregorianDateInput input = GregorianCalendarParser.parse(StringUtils.normalizeSpace(text));
		final int year = input.year();
		if(input.month() <= 0){
			// a year alone starts on Easter Sunday
			Indiction.validate(input, year);
			return Computus.easterJdn(year, REFORM);
		}

		final int month = input.month();
		final int day = input.dayOrFirst();
		// the day after the Easter of its year, or before the Easter of the next year
		final Long afterEaster = occurrence(year, month, day, true);
		final Long beforeEaster = occurrence(year + 1, month, day, false);

		final long jdn;
		final int commonYear;
		if(afterEaster != null && beforeEaster != null){
			if(qualifier == Qualifier.NONE)
				throw new IllegalArgumentException("The day occurs twice in the Easter-style year " + year
					+ ": add \"avant Pâques\" or \"après Pâques\": " + rawDate);
			jdn = (qualifier == Qualifier.AFTER_EASTER? afterEaster: beforeEaster);
			commonYear = (qualifier == Qualifier.AFTER_EASTER? year: year + 1);
		}
		else if(afterEaster != null && qualifier != Qualifier.BEFORE_EASTER){
			jdn = afterEaster;
			commonYear = year;
		}
		else if(beforeEaster != null && qualifier != Qualifier.AFTER_EASTER){
			jdn = beforeEaster;
			commonYear = year + 1;
		}
		else
			throw new IllegalArgumentException("The day does not exist in the Easter-style year " + year + ": " + rawDate);

		Indiction.validate(input, commonYear);
		return jdn;
	}

	/**
	 * @param commonYear  the year counted from 1 January
	 * @param month       the month
	 * @param day         the day
	 * @param afterEaster whether the day must be on or after the Easter of the year (otherwise strictly before)
	 * @return the Julian Day Number of the day, or {@code null} if it is on the wrong side of Easter, or does not exist
	 */
	private static Long occurrence(final int commonYear, final int month, final int day, final boolean afterEaster){
		if(day > REFORM.monthLength(commonYear, month))
			return null;

		final long jdn;
		try{
			jdn = REFORM.toJdn(commonYear, month, day);
		}
		catch(final IllegalArgumentException iae){
			return null;
		}
		final long easter = Computus.easterJdn(commonYear, REFORM);
		return ((jdn >= easter) == afterEaster? jdn: null);
	}

	@Override
	public List<DateInterpretation> getInterpretations(final String rawDate, final Locale locale){
		return GregorianCalendarParser.interpretations(rawDate, removeQualifiers(rawDate), locale);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(removeQualifiers(rawDate));
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

	private static String removeQualifiers(final String rawDate){
		final String text = AFTER_EASTER.matcher(BEFORE_EASTER.matcher(rawDate).replaceAll(StringUtils.SPACE))
			.replaceAll(StringUtils.SPACE);
		return StringUtils.normalizeSpace(text);
	}

}
