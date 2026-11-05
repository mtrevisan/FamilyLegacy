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
package io.github.mtrevisan.familylegacy.ui.tools.dates;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.StringJoiner;


/**
 * The calculations of the Date Calculator, on the dates of the (proleptic) Gregorian calendar written as {@code YYYY-MM-DD}.
 */
final class DateCalculations{

	private DateCalculations(){}


	/**
	 * @param text	The date, as {@code YYYY-MM-DD}.
	 * @return	The date, or {@code null} if the text is blank.
	 * @throws IllegalArgumentException	If the text is not a date.
	 */
	static LocalDate parseDate(final String text){
		if(StringUtils.isBlank(text))
			return null;

		try{
			return LocalDate.parse(text.trim());
		}
		catch(final DateTimeParseException ignored){
			throw new IllegalArgumentException(I18N.tf("text.date_calculator.invalid_date", text.trim()));
		}
	}

	/**
	 * @param birth	The date of birth.
	 * @param reference	The date the age is wanted at.
	 * @return	The time elapsed from the birth to the reference date.
	 * @throws IllegalArgumentException	If the reference date is before the birth.
	 */
	static Period age(final LocalDate birth, final LocalDate reference){
		if(reference.isBefore(birth))
			throw new IllegalArgumentException(I18N.t("text.age_before_birth"));

		return Period.between(birth, reference);
	}

	/**
	 * @param from	The first date.
	 * @param to	The second date.
	 * @return	The text of the time between the two dates, in whichever order they are given: with a minus sign if the second date is
	 * 	before the first one.
	 */
	static String interval(final LocalDate from, final LocalDate to){
		if(to.isBefore(from))
			return "−" + StringUtils.SPACE + formatPeriod(Period.between(to, from));
		return formatPeriod(Period.between(from, to));
	}

	/**
	 * @param reference	The date the age is known at.
	 * @param years	The completed years of the age.
	 * @param months	The months, after the years.
	 * @param days	The days, after the months.
	 * @return	The estimated date of birth.
	 */
	static LocalDate estimatedBirth(final LocalDate reference, final int years, final int months, final int days){
		return reference.minusYears(years)
			.minusMonths(months)
			.minusDays(days);
	}

	/** @return	The period as "39 years, 11 months, 30 days", in the language of the user: the zero parts are left out. */
	static String formatPeriod(final Period period){
		final StringJoiner text = new StringJoiner(", ");
		if(period.getYears() != 0)
			text.add(I18N.tf("text.period.years", period.getYears()));
		if(period.getMonths() != 0)
			text.add(I18N.tf("text.period.months", period.getMonths()));
		if(period.getDays() != 0 || text.length() == 0)
			text.add(I18N.tf("text.period.days", period.getDays()));
		return text.toString();
	}

}
