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

import io.github.mtrevisan.calendars.gregorian.Indiction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class IndictionTest{

	@Test
	void indictionOfYear(){
		// the first indiction cycle started on 1 September 312, so 313 (from 1 January) is the first year of a cycle
		Assertions.assertEquals(1, Indiction.of(313));
		Assertions.assertEquals(15, Indiction.of(312));
		Assertions.assertEquals(4, Indiction.of(1));
		Assertions.assertEquals(13, Indiction.of(1450));
		Assertions.assertEquals(10, Indiction.of(1582));
	}

	@Test
	void extractLatinOrdinal(){
		final Indiction.Extraction extraction = Indiction.extract("5 martii 1450, indictione tertia decima");

		Assertions.assertEquals(13, extraction.indiction());
		Assertions.assertEquals("5 martii 1450", extraction.text());
	}

	@Test
	void extractRomanNumeralInTheMiddle(){
		final Indiction.Extraction extraction = Indiction.extract("anno 1450 ind. XIII die 5 martii");

		Assertions.assertEquals(13, extraction.indiction());
		Assertions.assertEquals("anno 1450 die 5 martii", extraction.text());
	}

	@Test
	void extractItalian(){
		Assertions.assertEquals(3, Indiction.extract("12 aprile 1515 indizione terza").indiction());
		Assertions.assertEquals(3, Indiction.extract("12 aprile 1515 indizione 3").indiction());
	}

	@Test
	void consistentIndictionIsAccepted(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("5 March 1450"),
			CalendarType.JULIAN.parseToJdn("die 5 mensis martii anno Domini 1450, indictione tertia decima"));
	}

	@Test
	void inconsistentIndictionIsRejected(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.JULIAN.parseToJdn("5 martii 1450 indictione quarta decima"));
	}

	@Test
	void greekIndictionFromSeptember(){
		// from 1 September the Greek (and Bedan, and Christmas) styles already count the next indiction
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("5 October 1450"),
			CalendarType.JULIAN.parseToJdn("5 octobris 1450 indictione XIV"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("5 October 1450"),
			CalendarType.JULIAN.parseToJdn("5 octobris 1450 indictione XIII"));
	}

	@Test
	void indictionOfTheModernYearInMoreVeneto(){
		// February 1499 more veneto is February 1500, indiction 3
		Assertions.assertEquals(CalendarType.VENETAN.parseToJdn("15 febraro 1499"),
			CalendarType.VENETAN.parseToJdn("adì 15 febraro 1499 indictione tertia"));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.VENETAN.parseToJdn("15 febraro 1499 indictione secunda"));
	}

	@Test
	void indictionOfTheModernYearInFlorentineStyle(){
		// January 1499 Florentine is January 1500, indiction 3
		Assertions.assertEquals(CalendarType.FLORENTINE.parseToJdn("10 gennaio 1499"),
			CalendarType.FLORENTINE.parseToJdn("10 gennaio 1499 indizione terza"));
	}

	@Test
	void invalidIndiction(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JULIAN.parseToJdn("5 martii 1450 ind. 16"));
	}

}
