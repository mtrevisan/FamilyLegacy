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

import io.github.mtrevisan.calendars.buddhist.BuddhistCalendarParser;
import io.github.mtrevisan.calendars.buddhist.BuddhistDateInput;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.indian.IndianCalendarParser;
import io.github.mtrevisan.calendars.islamic.IslamicCalendarParser;
import io.github.mtrevisan.calendars.mayan.MayanCalendarParser;
import io.github.mtrevisan.calendars.mayan.MayanDateInput;
import io.github.mtrevisan.calendars.parsi.ParsiCalendarParser;
import io.github.mtrevisan.calendars.persian.PersianCalendarParser;
import io.github.mtrevisan.calendars.text.ScriptText;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;


/** The parsers of the non-Gregorian calendars: tables of names, scripts, and seasonal expressions. */
class NonGregorianParsersTest{

	private static void everyKeyMatchesItsOwnPattern(final String calendar, final Set<String> keys, final Consumer<String> parser){
		Assertions.assertFalse(keys.isEmpty());
		for(final String key : keys){
			try{
				parser.accept(key + " 1400");
				parser.accept("5 " + key + " 1400");
			}
			catch(final IllegalArgumentException iae){
				//a season cannot be given with a day: the message of the unknown names or invalid layouts is not this one
				if(!iae.getMessage().startsWith("A season cannot be given with a day"))
					Assertions.fail(calendar + ": the key '" + key + "' does not match its own pattern: " + iae.getMessage());
			}
		}
	}

	@Test
	void everyIslamicKeyMatchesItsOwnPattern(){
		everyKeyMatchesItsOwnPattern("Islamic", IslamicCalendarParser.knownNames(), IslamicCalendarParser::parse);
	}

	@Test
	void everyPersianKeyMatchesItsOwnPattern(){
		everyKeyMatchesItsOwnPattern("Persian", PersianCalendarParser.knownNames(), PersianCalendarParser::parse);
	}

	@Test
	void everyParsiKeyMatchesItsOwnPattern(){
		everyKeyMatchesItsOwnPattern("Parsi", ParsiCalendarParser.knownNames(), ParsiCalendarParser::parse);
	}

	@Test
	void everyIndianKeyMatchesItsOwnPattern(){
		everyKeyMatchesItsOwnPattern("Indian", IndianCalendarParser.knownNames(), IndianCalendarParser::parse);
	}

	@Test
	void everyBuddhistKeyMatchesItsOwnPattern(){
		everyKeyMatchesItsOwnPattern("Buddhist", BuddhistCalendarParser.knownNames(), BuddhistCalendarParser::parse);
	}

	@Test
	void everyMayanKeyMatchesItsOwnPattern(){
		for(final String key : MayanCalendarParser.knownNames())
			Assertions.assertDoesNotThrow(() -> MayanCalendarParser.parse(key + " 2026"), key);
	}

	@Test
	void keysAreWrittenInASingleScript(){
		final Set<String> keys = new HashSet<>();
		keys.addAll(IslamicCalendarParser.knownNames());
		keys.addAll(PersianCalendarParser.knownNames());
		keys.addAll(ParsiCalendarParser.knownNames());
		keys.addAll(IndianCalendarParser.knownNames());
		keys.addAll(BuddhistCalendarParser.knownNames());
		keys.addAll(MayanCalendarParser.knownNames());

		for(final String key : keys){
			final Set<Character.UnicodeScript> scripts = new HashSet<>();
			key.codePoints().forEach(cp -> {
				if(Character.isLetter(cp) || Character.getType(cp) == Character.NON_SPACING_MARK
						|| Character.getType(cp) == Character.COMBINING_SPACING_MARK)
					scripts.add(Character.UnicodeScript.of(cp));
			});
			//the Arabic keys of the Persian parser can be a mix of Arabic and Persian letters: both are the Arabic script
			scripts.remove(Character.UnicodeScript.INHERITED);
			Assertions.assertTrue(scripts.size() <= 1, "The key '" + key + "' mixes the scripts " + scripts);
		}
	}

	@Test
	void arabicScriptVariantsAreUnified(){
		//Persian yeh, Arabic yeh, alef maqsura; Persian and Arabic kaf; alef with madda, hamza and plain
		Assertions.assertEquals(ScriptText.normalize("فروردین"), ScriptText.normalize("فروردين"));
		Assertions.assertEquals(ScriptText.normalize("ربيع الآخر"), ScriptText.normalize("ربیع الاخر"));
		Assertions.assertEquals(ScriptText.normalize("جمادى الأولى"), ScriptText.normalize("جمادی الاولی"));

		Assertions.assertEquals(new YearMonthDay(1357, 11, 22), PersianCalendarParser.parse("۲۲ بهمن ۱۳۵۷"));
		Assertions.assertEquals(new YearMonthDay(1300, 1, 1), PersianCalendarParser.parse("1 فروردين 1300"));
		Assertions.assertEquals(new YearMonthDay(1300, 1, 1), PersianCalendarParser.parse("1 فروردین 1300"));
		Assertions.assertEquals(new YearMonthDay(1300, 3, 1), PersianCalendarParser.parse("1 Khordad 1300"));
		Assertions.assertEquals(new YearMonthDay(1300, 2, 1), PersianCalendarParser.parse("1 Ordibehešt 1300"));
		Assertions.assertEquals(new YearMonthDay(1300, 2, 1), PersianCalendarParser.parse("1 Ордибеҳишт 1300"));
		Assertions.assertEquals(new YearMonthDay(1300, 9, 1), PersianCalendarParser.parse("1 ОЗАР 1300"));
		Assertions.assertEquals(new YearMonthDay(1300, -4, 0), PersianCalendarParser.parse("Тобистон 1300"));
		Assertions.assertEquals(new YearMonthDay(1300, 12, 1), PersianCalendarParser.parse("1 ESFAND 1300"));
	}

	@Test
	void islamicMonthsWithApostrophesAndAlefMadda(){
		Assertions.assertEquals(new YearMonthDay(1445, 8, 1), IslamicCalendarParser.parse("1 Sha'ban 1445"));
		Assertions.assertEquals(new YearMonthDay(1445, 8, 1), IslamicCalendarParser.parse("1 Sha’ban 1445"));
		Assertions.assertEquals(new YearMonthDay(1445, 11, 5), IslamicCalendarParser.parse("5 Dhu'l-Qa'dah 1445"));
		Assertions.assertEquals(new YearMonthDay(1445, 4, 1), IslamicCalendarParser.parse("١ ربيع الآخر ١٤٤٥"));
		Assertions.assertEquals(new YearMonthDay(1445, 4, 1), IslamicCalendarParser.parse("1 ربيع الاخر 1445"));
		Assertions.assertEquals(new YearMonthDay(1445, 12, 10), IslamicCalendarParser.parse("10 ذو الحجة 1445"));
	}

	@Test
	void thaiDottedAbbreviations(){
		Assertions.assertEquals(new BuddhistDateInput(2567, 1, 1, false), BuddhistCalendarParser.parse("1 ม.ค. 2567"));
		Assertions.assertEquals(new BuddhistDateInput(2567, 12, 25, false), BuddhistCalendarParser.parse("๒๕ ธ.ค. ๒๕๖๗"));
		Assertions.assertEquals(new BuddhistDateInput(2567, 3, 31, false), BuddhistCalendarParser.parse("31 มี.ค. 2567"));
	}

	@Test
	void indianDevanagariDigitsAndApostrophe(){
		Assertions.assertEquals(3, IndianCalendarParser.parse("1 jyaist'ha 1946").month());
		Assertions.assertEquals(6, IndianCalendarParser.parse("१ भाद्रपद १९४६").month());
		Assertions.assertEquals(1946, IndianCalendarParser.parse("१ चैत्र १९४६").year());
		//the Tamil and Bengali months have their own calendars: they start at the sidereal sankranti, not at the equinox
		Assertions.assertThrows(IllegalArgumentException.class, () -> IndianCalendarParser.parse("1 Chithirai 1946"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> IndianCalendarParser.parse("1 Vaigasi 1946"));
	}

	@Test
	void aSeasonWithADayIsAnError(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> IslamicCalendarParser.parse("5 al-shita 1420"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> PersianCalendarParser.parse("5 bahar 1357"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ParsiCalendarParser.parse("5 padiz 1390"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> IndianCalendarParser.parse("5 vasanta 1920"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> BuddhistCalendarParser.parse("5 hemanta 2500"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> BuddhistCalendarParser.parse("5 ฤดูหนาว 2500"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> MayanCalendarParser.parse("5 yax 2026"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.CHINESE.parseToJdn("春 2024年 正月 初一"));

		//the season alone is fine
		Assertions.assertEquals(-3, PersianCalendarParser.parse("Bahar 1357").month());
		Assertions.assertEquals(-2, IslamicCalendarParser.parse("al-shita 1420").month());
	}

	@Test
	void parsiZeroWidthNonJoiner(){
		Assertions.assertEquals(2, ParsiCalendarParser.parse("1 اردی‌بهشت 1390").month());
		Assertions.assertEquals(2, ParsiCalendarParser.parse("1 اردیبهشت 1390").month());
		Assertions.assertEquals(2, ParsiCalendarParser.parse("1 અર્દિબહેશ્ત 1390").month());
		Assertions.assertEquals(5, ParsiCalendarParser.parse("1 અમરદાદ 1390").month());
	}

	@Test
	void mayanLongCountComponents(){
		Assertions.assertEquals(new MayanDateInput(13, 0, 0, 0, 0), MayanCalendarParser.parse("13.0.0.0.0"));
		Assertions.assertEquals(new MayanDateInput(9, 19, 19, 17, 19), MayanCalendarParser.parse("9.19.19.17.19"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> MayanCalendarParser.parse("13.0.0.0"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> MayanCalendarParser.parse("13.0.0.0.0.0"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> MayanCalendarParser.parse("13.0.0.18.0"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> MayanCalendarParser.parse("13.20.0.0.0"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> MayanCalendarParser.parse("13.0.20.0.0"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> MayanCalendarParser.parse("13.0.0.0.20"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> MayanCalendarParser.parse("13..0.0.0"));
	}

}
