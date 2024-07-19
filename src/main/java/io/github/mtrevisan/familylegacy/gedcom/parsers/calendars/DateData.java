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


class DateData{

	private Integer day;
	private Integer month;
	private Integer year;
	private Integer doubleEntryYear;
	private Era era;


	public Integer getDay(){
		return day;
	}

	public DateData withDay(final Integer day){
		this.day = day;
		return this;
	}

	public Integer getMonth(){
		return month;
	}

	public DateData withMonth(final Integer month){
		this.month = month;
		return this;
	}

	public Integer getYear(){
		return year;
	}

	public DateData withYear(final Integer year){
		this.year = year;
		return this;
	}

	public Integer getDoubleEntryYear(){
		return doubleEntryYear;
	}

	public DateData withDoubleEntryYear(final Integer doubleEntryYear){
		this.doubleEntryYear = doubleEntryYear;
		return this;
	}

	public Era getEra(){
		return era;
	}

	public DateData withEra(final Era era){
		this.era = era;
		return this;
	}

	public boolean isEmpty(){
		return (day == null && month == null && year == null && doubleEntryYear == null && era == null);
	}

}
