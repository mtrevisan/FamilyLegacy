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

import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;


public class AgeData{

	private AgeType ageType;
	private String years;
	private String months;
	private String days;


	public AgeData withAgeType(final AgeType ageType){
		this.ageType = ageType;
		return this;
	}

	public AgeData withYears(final String years){
		this.years = years;
		return this;
	}

	public AgeData withMonths(final String months){
		this.months = months;
		return this;
	}

	public AgeData withDays(final String days){
		this.days = days;
		return this;
	}

	public String getAge(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(ageType.getDescription());
		if(ageType == AgeType.EXACT || ageType == AgeType.LESS_THAN || ageType == AgeType.MORE_THAN){
			if(StringUtils.isNotBlank(years))
				sj.add(years)
					.add("y");
			if(StringUtils.isNotBlank(months))
				sj.add(months)
					.add("m");
			if(StringUtils.isNotBlank(days))
				sj.add(days)
					.add("d");
		}
		return sj.toString();
	}

}
