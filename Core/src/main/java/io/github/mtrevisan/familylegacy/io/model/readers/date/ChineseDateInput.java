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
package io.github.mtrevisan.familylegacy.io.model.readers.date;


/**
 * A lightweight data record holding raw integers parsed from traditional Chinese calendar strings.
 */
public final class ChineseDateInput{

	private final int year;
	private final int month;
	private final boolean leapMonth;
	private final int day;


	public ChineseDateInput(final int year, final int month, final boolean leapMonth, final int day){
		this.year = year;
		this.month = month;
		this.leapMonth = leapMonth;
		this.day = day;
	}


	public int getYear(){
		return year;
	}

	public int getMonth(){
		return month;
	}

	public boolean isLeapMonth(){
		return leapMonth;
	}

	public int getDay(){
		return day;
	}


	@Override
	public String toString(){
		return "Year: " + year + ", Month: " + month + " (Leap: " + leapMonth + "), Day: " + day;
	}

}

