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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** A class for parsing ages from strings. */
public final class AgeParser{

	private static final String PARAM_RELATION = "relation";
	private static final String PARAM_YEARS = "years";
	private static final String PARAM_MONTHS = "months";
	private static final String PARAM_DAYS = "days";
	private static final String PARAM_INSTANT = "instant";
	private static final String PATTERN_AGE_RELATION = "(?:(?<" + PARAM_RELATION + "><|>) )?";
	private static final String PATTERN_AGE_YEARS = "(?:(?<" + PARAM_YEARS + ">\\d{1,2})y)? ?";
	private static final String PATTERN_AGE_MONTHS = "(?:(?<" + PARAM_MONTHS + ">\\d{1,2})m)? ?";
	private static final String PATTERN_AGE_DAYS = "(?:(?<" + PARAM_DAYS + ">\\d{1,3})d)?";
	private static final String PATTERN_AGE_INSTANT = "(?<" + PARAM_INSTANT + ">CHILD|INFANT|STILLBORN)";
	private static final Pattern PATTERN_AGE = RegexHelper.pattern("(?i)^(?:" + PATTERN_AGE_RELATION + PATTERN_AGE_YEARS
		+ PATTERN_AGE_MONTHS + PATTERN_AGE_DAYS + "|" + PATTERN_AGE_INSTANT + ")$");

	private AgeParser(){}


	/**
	 * Parse the string as age.
	 *
	 * @param age	The age string.
	 * @return	The age, if it can be derived from the string.
	 */
	public static AgeData parse(final CharSequence age){
		final AgeData ageData = new AgeData();
		final Matcher matcher = RegexHelper.matcher(age, PATTERN_AGE);
		if(matcher.find()){
			final String instant = matcher.group(PARAM_INSTANT);
			if(StringUtils.isNotEmpty(instant))
				ageData.withAgeType(AgeType.createFromText(instant));
			else{
				final String relation = matcher.group(PARAM_RELATION);
				final String years = matcher.group(PARAM_YEARS);
				final String months = matcher.group(PARAM_MONTHS);
				final String days = matcher.group(PARAM_DAYS);

				ageData.withAgeType(AgeType.createFromText(relation))
					.withYears(years)
					.withMonths(months)
					.withDays(days);
			}
		}
		return ageData;
	}

	public static String formatAge(final String age){
		String formattedAge = null;
		if(StringUtils.isNotBlank(age)){
			RegexHelper.replaceAll(age, AgeType.CHILD.getPattern(), AgeType.CHILD.getDescription());
			RegexHelper.replaceAll(age, AgeType.INFANT.getPattern(), AgeType.INFANT.getDescription());
			RegexHelper.replaceAll(age, AgeType.STILLBORN.getPattern(), AgeType.STILLBORN.getDescription());
			formattedAge = age;
		}
		return formattedAge;
	}

}
