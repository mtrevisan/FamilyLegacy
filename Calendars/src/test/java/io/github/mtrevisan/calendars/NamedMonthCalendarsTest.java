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
package io.github.mtrevisan.calendars;

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * The calendars with named months (Armenian, Byzantine, Coptic, Egyptian, Ethiopian, French Republican, Rumi, Seleucid,
 * Soviet Eternal): the real length of the months, the named days, the seasons, and the rejected expressions.
 */
class NamedMonthCalendarsTest{

	private static long greg(final int year, final int month, final int day){
		return GregorianCalendarStrategy.toJdn(year, month, day);
	}

	private static void rejected(final CalendarType type, final String text){
		Assertions.assertThrows(IllegalArgumentException.class, () -> type.parseToJdn(text), type + ": " + text);
	}


	@Test
	void everyParserRejectsNullBlankAndYearZero(){
		for(final CalendarType type : new CalendarType[]{CalendarType.ARMENIAN, CalendarType.BYZANTINE, CalendarType.COPTIC,
				CalendarType.EGYPTIAN, CalendarType.ETHIOPIAN, CalendarType.FRENCH_REPUBLICAN, CalendarType.RUMI,
				CalendarType.SELEUCID, CalendarType.SOVIET_ETERNAL, CalendarType.HEBREW, CalendarType.QUAKER,
				CalendarType.LITURGICAL, CalendarType.ENGLISH_REGNAL, CalendarType.VIETNAMESE, CalendarType.JAPANESE}){
			rejected(type, null);
			rejected(type, "");
			rejected(type, "   ");
		}
		rejected(CalendarType.COPTIC, "1 Thout 0");
		rejected(CalendarType.FRENCH_REPUBLICAN, "1 Vendemiaire 0");
		rejected(CalendarType.SOVIET_ETERNAL, "1 январь 0");
		rejected(CalendarType.COPTIC, "0");
	}

	@Test
	void aDayWithASeasonIsAnError(){
		rejected(CalendarType.ARMENIAN, "5 ashun 1473");
		rejected(CalendarType.BYZANTINE, "5 theros 7532");
		rejected(CalendarType.COPTIC, "5 akhet 1740");
		rejected(CalendarType.EGYPTIAN, "5 peret 2772");
		rejected(CalendarType.ETHIOPIAN, "5 keremt 2016");
		rejected(CalendarType.FRENCH_REPUBLICAN, "5 hiver 232");
		rejected(CalendarType.RUMI, "5 yaz 1439");
		rejected(CalendarType.SELEUCID, "5 tishrin-season 2335");
		rejected(CalendarType.SOVIET_ETERNAL, "5 зима 1930");
		rejected(CalendarType.HEBREW, "5 Tekufat Nisan 5784");
	}

	@Test
	void copticAndEthiopianDaySixOfTheLastMonthOnlyInLeapYears(){
		//1739 AM is the year before a Julian leap year (it has six epagomenal days): 1739 % 4 == 3
		Assertions.assertEquals(CalendarType.COPTIC.parseToJdn("1 Thout 1740") - 1, CalendarType.COPTIC.parseToJdn("6 Nasiee 1739"));
		rejected(CalendarType.COPTIC, "6 Nasiee 1740");
		Assertions.assertEquals(CalendarType.COPTIC.parseToJdn("1 Thout 1741") - 1, CalendarType.COPTIC.parseToJdn("5 Nasiee 1740"));
		rejected(CalendarType.COPTIC, "7 Nasiee 1739");
		rejected(CalendarType.COPTIC, "31 Thout 1740");

		Assertions.assertEquals(CalendarType.ETHIOPIAN.parseToJdn("1 Meskerem 2016") - 1,
			CalendarType.ETHIOPIAN.parseToJdn("6 Pagumen 2015"));
		rejected(CalendarType.ETHIOPIAN, "6 Pagumen 2016");
	}

	@Test
	void frenchSansculottides(){
		//the complementary day of a name is a fixed day of the thirteenth month
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN.parseToJdn("1 sansculottides 2"),
			CalendarType.FRENCH_REPUBLICAN.parseToJdn("jour de la vertu 2"));
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN.parseToJdn("2 sansculottides 2"),
			CalendarType.FRENCH_REPUBLICAN.parseToJdn("Jour du génie 2"));
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN.parseToJdn("3 sansculottides 2"),
			CalendarType.FRENCH_REPUBLICAN.parseToJdn("Jour du travail 2"));
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN.parseToJdn("4 sansculottides 2"),
			CalendarType.FRENCH_REPUBLICAN.parseToJdn("Jour de l'opinion 2"));
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN.parseToJdn("4 sansculottides 2"),
			CalendarType.FRENCH_REPUBLICAN.parseToJdn("jour de l’opinion 2"));
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN.parseToJdn("5 sansculottides 2"),
			CalendarType.FRENCH_REPUBLICAN.parseToJdn("Jour des récompenses 2"));
		//the sixth day exists in the sextile years III, VII, XI only
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN.parseToJdn("1 Vendemiaire 4") - 1,
			CalendarType.FRENCH_REPUBLICAN.parseToJdn("Jour de la révolution 3"));
		rejected(CalendarType.FRENCH_REPUBLICAN, "Jour de la révolution 2");
		rejected(CalendarType.FRENCH_REPUBLICAN, "6 sansculottides 4");
		rejected(CalendarType.FRENCH_REPUBLICAN, "3 jour de la vertu 2");
		rejected(CalendarType.FRENCH_REPUBLICAN, "31 Vendemiaire 2");
		rejected(CalendarType.FRENCH_REPUBLICAN, "franciade 3");
		//the abbreviations with a dot are read
		Assertions.assertEquals(CalendarType.FRENCH_REPUBLICAN.parseToJdn("9 Thermidor 2"),
			CalendarType.FRENCH_REPUBLICAN.parseToJdn("9 therm. 2"));
	}

	@Test
	void sovietNamedDaysAreFixedDays(){
		Assertions.assertEquals(greg(1930, 1, 22), CalendarType.SOVIET_ETERNAL.parseToJdn("день ленина 1930"));
		Assertions.assertEquals(greg(1930, 1, 22), CalendarType.SOVIET_ETERNAL.parseToJdn("Lenin Day 1930"));
		Assertions.assertEquals(greg(1930, 5, 1), CalendarType.SOVIET_ETERNAL.parseToJdn("labor days 1930"));
		Assertions.assertEquals(greg(1930, 5, 1), CalendarType.SOVIET_ETERNAL.parseToJdn("1 labor days 1930"));
		Assertions.assertEquals(greg(1930, 5, 2), CalendarType.SOVIET_ETERNAL.parseToJdn("2 labor days 1930"));
		Assertions.assertEquals(greg(1930, 11, 7), CalendarType.SOVIET_ETERNAL.parseToJdn("industry days 1930"));
		Assertions.assertEquals(greg(1930, 11, 8), CalendarType.SOVIET_ETERNAL.parseToJdn("2 industry days 1930"));
		Assertions.assertEquals(greg(1932, 2, 29), CalendarType.SOVIET_ETERNAL.parseToJdn("високосный день 1932"));
		rejected(CalendarType.SOVIET_ETERNAL, "високосный день 1931");
		rejected(CalendarType.SOVIET_ETERNAL, "3 labor days 1930");
		rejected(CalendarType.SOVIET_ETERNAL, "2 lenin day 1930");
		rejected(CalendarType.SOVIET_ETERNAL, "holiday-days 1930");
		rejected(CalendarType.SOVIET_ETERNAL, "yellow 1930");
		//they are never the January of the following year
		Assertions.assertNotEquals(greg(1931, 1, 1), CalendarType.SOVIET_ETERNAL.parseToJdn("industry days 1930"));
	}

	@Test
	void julianAndGregorianMonthLengths(){
		//Byzantine: February 6961 AM is the Julian year 1453 (not leap), February 7532 AM is 2024 (leap), 7533 AM is 2025
		rejected(CalendarType.BYZANTINE, "29 February 6961");
		Assertions.assertDoesNotThrow(() -> CalendarType.BYZANTINE.parseToJdn("29 February 7532"));
		rejected(CalendarType.BYZANTINE, "29 February 7533");
		rejected(CalendarType.BYZANTINE, "31 September 6961");
		rejected(CalendarType.BYZANTINE, "31 April 6961");
		Assertions.assertDoesNotThrow(() -> CalendarType.BYZANTINE.parseToJdn("31 August 6961"));
		//Seleucid: October 1 is the first day of the year
		rejected(CalendarType.SELEUCID, "31 Nisan 315");
		//Soviet: the Gregorian rule
		rejected(CalendarType.SOVIET_ETERNAL, "29 февраль 1931");
		Assertions.assertDoesNotThrow(() -> CalendarType.SOVIET_ETERNAL.parseToJdn("29 февраль 1932"));
		rejected(CalendarType.SOVIET_ETERNAL, "29 февраль 1900");
		rejected(CalendarType.SOVIET_ETERNAL, "31 апрель 1930");
		//Rumi: Mart is the first month; Nisan of the Rumi calendar is not a month (the test uses the Turkish names)
		rejected(CalendarType.RUMI, "32 Mart 1330");
	}

	@Test
	void armenianAndEgyptianHaveFiveEpagomenalDays(){
		Assertions.assertEquals(CalendarType.ARMENIAN.parseToJdn("1 Navasard 1401") - 1,
			CalendarType.ARMENIAN.parseToJdn("5 Avelyats 1400"));
		rejected(CalendarType.ARMENIAN, "6 Avelyats 1400");
		rejected(CalendarType.EGYPTIAN, "6 Epagomenal 1400");
		rejected(CalendarType.EGYPTIAN, "31 Thoth 1400");
	}

	@Test
	void seasonsOfTheCopticYear1740(){
		//autumn and winter in the Julian year 2023 (the year starts in August), spring and summer in the next one
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.COPTIC.parseToJdn("Akhet 1740"));
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.COPTIC.parseToJdn("Peret 1740"));
		Assertions.assertEquals(greg(2024, 3, 20), CalendarType.COPTIC.parseToJdn("الربيع 1740"));
		Assertions.assertEquals(greg(2024, 6, 20), CalendarType.COPTIC.parseToJdn("Shom 1740"));
	}

	@Test
	void seasonsOfTheEthiopianYear2016(){
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.ETHIOPIAN.parseToJdn("Tseday 2016"));
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.ETHIOPIAN.parseToJdn("Bega 2016"));
		Assertions.assertEquals(greg(2024, 3, 20), CalendarType.ETHIOPIAN.parseToJdn("Belg 2016"));
		Assertions.assertEquals(greg(2024, 6, 20), CalendarType.ETHIOPIAN.parseToJdn("Keremt 2016"));
	}

	@Test
	void seasonsOfTheFrenchYear232(){
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.FRENCH_REPUBLICAN.parseToJdn("automne 232"));
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.FRENCH_REPUBLICAN.parseToJdn("hiver 232"));
		Assertions.assertEquals(greg(2024, 3, 20), CalendarType.FRENCH_REPUBLICAN.parseToJdn("printemps 232"));
		Assertions.assertEquals(greg(2024, 6, 20), CalendarType.FRENCH_REPUBLICAN.parseToJdn("ete 232"));
	}

	@Test
	void seasonsOfTheByzantineYear7532(){
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.BYZANTINE.parseToJdn("phthinoporo 7532"), "autumn");
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.BYZANTINE.parseToJdn("cheimonas 7532"), "winter");
		Assertions.assertEquals(greg(2024, 3, 20), CalendarType.BYZANTINE.parseToJdn("anoixi 7532"), "spring");
		Assertions.assertEquals(greg(2024, 6, 20), CalendarType.BYZANTINE.parseToJdn("theros 7532"), "summer");
	}

	@Test
	void seasonsOfTheSeleucidYear2335(){
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.SELEUCID.parseToJdn("tishrin-season 2335"));
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.SELEUCID.parseToJdn("kanun-season 2335"));
		Assertions.assertEquals(greg(2024, 3, 20), CalendarType.SELEUCID.parseToJdn("nisan-season 2335"));
		Assertions.assertEquals(greg(2024, 6, 20), CalendarType.SELEUCID.parseToJdn("tammuz-season 2335"));
	}

	@Test
	void seasonsOfTheRumiAndSovietYears(){
		//the year of the Gregorian calendar: all the seasons are in it
		Assertions.assertEquals(greg(2023, 3, 20), CalendarType.RUMI.parseToJdn("bahar 1439"));
		Assertions.assertEquals(greg(2023, 6, 21), CalendarType.RUMI.parseToJdn("yaz 1439"));
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.RUMI.parseToJdn("guz 1439"));
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.RUMI.parseToJdn("kis 1439"));

		Assertions.assertEquals(greg(2023, 3, 20), CalendarType.SOVIET_ETERNAL.parseToJdn("vesna 2023"));
		Assertions.assertEquals(greg(2023, 6, 21), CalendarType.SOVIET_ETERNAL.parseToJdn("leto 2023"));
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.SOVIET_ETERNAL.parseToJdn("osen 2023"));
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.SOVIET_ETERNAL.parseToJdn("zima 2023"));
	}

	@Test
	void seasonsOfTheWanderingYearsFallInsideTheYear(){
		//Armenian 1473 starts on 22 July 2023, Egyptian 2772 on 18 April 2023: the first event on or after the start
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.ARMENIAN.parseToJdn("ashun 1473"));
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.ARMENIAN.parseToJdn("dzmer 1473"));
		Assertions.assertEquals(greg(2024, 3, 20), CalendarType.ARMENIAN.parseToJdn("garun 1473"));
		Assertions.assertEquals(greg(2024, 6, 20), CalendarType.ARMENIAN.parseToJdn("amar 1473"));

		Assertions.assertEquals(greg(2023, 6, 21), CalendarType.EGYPTIAN.parseToJdn("shemu 2772"));
		Assertions.assertEquals(greg(2023, 9, 23), CalendarType.EGYPTIAN.parseToJdn("akhet 2772"));
		Assertions.assertEquals(greg(2023, 12, 22), CalendarType.EGYPTIAN.parseToJdn("peret 2772"));
		Assertions.assertEquals(greg(2024, 3, 20), CalendarType.EGYPTIAN.parseToJdn("egyptian-spring 2772"));
	}

}
