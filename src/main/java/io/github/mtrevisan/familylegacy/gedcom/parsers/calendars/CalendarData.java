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

import io.github.mtrevisan.familylegacy.services.JavaHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;


public class CalendarData{

	private CalendarType calendarType;
	private boolean afterSunset;
	private IntervalType intervalType;
	private DateData fromDate;
	private Qualification fromQualification;
	private DateData toDate;
	private Qualification toQualification;
	private String interpretedFrom;


	public CalendarData withCalendarType(final CalendarType calendarType){
		this.calendarType = calendarType;
		return this;
	}

	public CalendarData withAfterSunset(final boolean afterSunset){
		this.afterSunset = afterSunset;
		return this;
	}

	public CalendarData withIntervalType(final IntervalType intervalType){
		this.intervalType = intervalType;
		return this;
	}

	public CalendarData withFromDate(final DateData fromDate){
		this.fromDate = fromDate;
		return this;
	}

	public CalendarData withFromQualification(final Qualification fromQualification){
		this.fromQualification = fromQualification;
		return this;
	}

	public CalendarData withToDate(final DateData toDate){
		this.toDate = toDate;
		return this;
	}

	public CalendarData withToQualification(final Qualification toQualification){
		this.toQualification = toQualification;
		return this;
	}

	public CalendarData withInterpretedFrom(final String interpretedFrom){
		this.interpretedFrom = interpretedFrom;
		return this;
	}

	public String getDate(){
		final String fromMonth = (fromDate.getMonth() != null? GregorianMonth.values()[fromDate.getMonth()].toString(): null);
		final Era fromEra = fromDate.getEra();
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		if(calendarType != null)
			sj.add(calendarType.getType());
		if(intervalType != null)
			sj.add(intervalType.getType());
		if(fromDate != null)
			sj.add(Integer.toString(fromDate.getDay()));
		JavaHelper.addValueIfNotNull(sj, fromMonth);
		if(fromDate != null)
			sj.add(Integer.toString(fromDate.getYear()));
		if(fromEra == Era.BCE)
			sj.add(Era.BCE.toString());
		return sj.toString();
	}

}
