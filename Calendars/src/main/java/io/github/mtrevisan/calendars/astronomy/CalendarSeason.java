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
package io.github.mtrevisan.calendars.astronomy;


public class CalendarSeason{

	public static final int SEASON_AUTUMN = -1;
	public static final int SEASON_WINTER = -2;
	public static final int SEASON_SPRING = -3;
	public static final int SEASON_SUMMER = -4;


	private CalendarSeason(){}


	public static int gregorianSeasonMonth(final int season){
		return switch(season){
			case CalendarSeason.SEASON_SPRING -> 3;	// March
			case CalendarSeason.SEASON_SUMMER -> 6;	// June
			case CalendarSeason.SEASON_AUTUMN -> 9;	// September
			case CalendarSeason.SEASON_WINTER -> 12;	// December
			default -> 6;
		};
	}

	public static double getGeneralTargetLongitude(final int month){
		return switch(month){
			case SEASON_AUTUMN -> Math.PI;
			case SEASON_WINTER -> 1.5 * Math.PI;
			case SEASON_SPRING -> 0.;
			case SEASON_SUMMER -> 0.5 * Math.PI;
			default -> 0.;
		};
	}

}
