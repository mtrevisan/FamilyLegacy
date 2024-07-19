/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;


class Age{

	private static final Pattern AGE_MATCHER = RegexHelper.pattern("[^\\d]+");


	public enum ApproximationType{
		PREFIX("~"),
		SUFFIX(" ca.");

		private final String approx;

		ApproximationType(final String approx){
			this.approx = approx;
		}

	}


	private final int value;
	private final boolean approximated;
	private final ApproximationType approximationType;


	Age(final int value, final boolean approximated, final ApproximationType approximationType){
		this.value = value;
		this.approximated = approximated;
		this.approximationType = approximationType;
	}

	Age(String value, final ApproximationType approximationType){
		value = StringUtils.strip(value);

		final String age = RegexHelper.replaceAll(value, AGE_MATCHER, StringUtils.EMPTY);
		this.value = Integer.parseInt(age);
		approximated = (value.length() != age.length());
		this.approximationType = approximationType;
	}

	public static Age createFromExactAge(final int value){
		return new Age(value, false, null);
	}

	public static Age createFromApproximatedAge(final int value, final ApproximationType approximationType){
		return new Age(value, true, approximationType);
	}

	public static Age createFromDates(final String dateFrom, final String dateTo, final ApproximationType approximationType){
		Age age = null;
		if(dateFrom != null && dateTo != null){
			final LocalDate from = DateParser.parse(dateFrom);
			final LocalDate to = DateParser.parse(dateTo);
			if(from != null && to != null){
				final boolean approximatedAge = (AbstractCalendarParser.isApproximation(dateFrom) || AbstractCalendarParser.isApproximation(dateTo));
				age = new Age(Period.between(from, to).getYears(), approximatedAge, approximationType);
			}
		}
		return age;
	}

	public static Age createFromDates(final LocalDate from, final LocalDate to){
		return createFromPeriod(Period.between(from, to));
	}

	public static Age createFromPeriod(final Period period){
		return createFromExactAge(period.getYears());
	}

	@Override
	public String toString(){
		final String text = Integer.toString(value);
		if(!approximated)
			return text;

		return (approximationType == ApproximationType.PREFIX? approximationType.approx + text: text + approximationType.approx);
	}

}
