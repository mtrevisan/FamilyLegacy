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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ParsiCalendarConverterTest{

	@Test
	void epoch(){
		// Yazdegerdi era epoch, uninterrupted reckoning (16 Jun 632 Julian)
		final long jdn = CalendarType.PARSI.parseToJdn("1 Fravardin 1 Kadmi");

		Assertions.assertEquals(1952063L, jdn);
	}

	@Test
	void kadmiNavroz1394(){
		// Kadmi Navroz, one month before the Shahenshahi one (16 Jul 2024 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("1 Fravardin 1394 Kadmi");

		Assertions.assertEquals(2460508L, jdn);
	}

	@Test
	void shahenshahiNavroz1393(){
		// Shahenshahi Navroz, the default reckoning (16 Aug 2023 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("1 Fravardin 1393");

		Assertions.assertEquals(2460173L, jdn);
	}

	@Test
	void shahenshahiNavroz1394(){
		// Shahenshahi Navroz (15 Aug 2024 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("1 Fravardin 1394 Shahenshahi");

		Assertions.assertEquals(2460538L, jdn);
	}

	@Test
	void shahenshahiNavroz1394Gujarati(){
		// Shahenshahi Navroz, Gujarati script and digits (15 Aug 2024 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("૧ ફરવરદીન ૧૩૯૪");

		Assertions.assertEquals(2460538L, jdn);
	}

	@Test
	void shahenshahiNavroz1390(){
		// Shahenshahi Navroz (16 Aug 2020 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("1 Fravardin 1390");

		Assertions.assertEquals(2459078L, jdn);
	}

	@Test
	void shahenshahiNavroz1392(){
		// Shahenshahi Navroz (16 Aug 2022 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("1 Fravardin 1392");

		Assertions.assertEquals(2459808L, jdn);
	}

	@Test
	void shahenshahiNavroz1395(){
		// Shahenshahi Navroz (15 Aug 2025 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("1 Fravardin 1395");

		Assertions.assertEquals(2460903L, jdn);
	}

	@Test
	void khordadSal1394(){
		// Khordad Sal, birth of Zarathustra on roj Khordad (6th day) of mah Fravardin (20 Aug 2024 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("6 Fravardin 1394");

		Assertions.assertEquals(2460543L, jdn);
	}

	@Test
	void pateti1393(){
		// Pateti, the last Gatha day of the year, eve of the Shahenshahi Navroz (14 Aug 2024 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("5 Gatha 1393");

		Assertions.assertEquals(2460537L, jdn);
	}

	@Test
	void shahenshahiAndKadmiDifferByOneMonth(){
		final long shahenshahi = CalendarType.PARSI.parseToJdn("15 Meher 1394 Shahenshahi");
		final long kadmi = CalendarType.PARSI.parseToJdn("15 Meher 1394 Kadmi");

		Assertions.assertEquals(30L, shahenshahi - kadmi);
	}

	@Test
	void persianScriptAndDigits(){
		// 22 Fravardin 1390 Shahenshahi, Persian script and digits (6 Sep 2020 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("۲۲ فروردین ۱۳۹۰");

		Assertions.assertEquals(2459099L, jdn);
	}

	@Test
	void persianScriptWithZeroWidthNonJoiner(){
		// 1 Ardibehest 1394 Shahenshahi, written with the zero-width non-joiner (14 Sep 2024 Gregorian)
		final long jdn = CalendarType.PARSI.parseToJdn("۱ اردی‌بهشت ۱۳۹۴");

		Assertions.assertEquals(2460568L, jdn);
	}

	@Test
	void summerSolsticeInYear1394(){
		// Hamin: summer solstice within the Shahenshahi year 1394 (21 Jun 2025 02:42 UT)
		final long jdn = CalendarType.PARSI.parseToJdn("Hamin 1394");

		Assertions.assertEquals(2460848L, jdn);
	}

	@Test
	void completeDateMetadata(){
		final CalendarType.CalendarMetadata metadata = CalendarType.PARSI.getMetadata("1 Fravardin 1394");

		Assertions.assertTrue(metadata.isComplete());
	}

	@Test
	void dayOutOfRange(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.PARSI.parseToJdn("31 Fravardin 1394"));
	}

	@Test
	void gathaDayOutOfRange(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.PARSI.parseToJdn("6 Gatha 1394"));
	}

	@Test
	void unknownMonth(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.PARSI.parseToJdn("1 Foo 1394"));
	}

	@Test
	void unknownReckoning(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.PARSI.parseToJdn("1 Fravardin 1394 Fasli"));
	}

}
