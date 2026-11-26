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


class DateShorthandReaderTest{

	private static final DateShorthand.Kind POINT = DateShorthand.Kind.POINT;
	private static final DateShorthand.Kind BOUNDED = DateShorthand.Kind.BOUNDED;
	private static final DateShorthand.Kind SPANNING = DateShorthand.Kind.SPANNING;


	@Test
	void plainDateIsAPoint(){
		final DateShorthand date = DateShorthandReader.parse("12 March 1850");

		Assertions.assertEquals(POINT, date.kind());
		Assertions.assertEquals(single("12 March 1850", null, null), date.first());
		Assertions.assertNull(date.second());
		Assertions.assertFalse(date.first().approximate());
	}

	@Test
	void aboutIsApproximateStated(){
		for(final String text : new String[]{"abt 1850", "ABT 1850", "about 1850", "c. 1850", "c.1850", "ca 1850", "ca. 1850", "circa 1850",
				"~1850", "~ 1850", "circa il 1850", "intorno al 1850", "verso il 1850", "vers 1850", "um 1850", "hacia 1850"}){
			final DateShorthand date = DateShorthandReader.parse(text);

			Assertions.assertEquals(POINT, date.kind(), text);
			Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, null), date.first(), text);
		}
	}

	@Test
	void calculatedIsApproximateCalculated(){
		for(final String text : new String[]{"calc 1850", "CAL 1850", "calculated 1850", "calcolato 1850"}){
			final DateShorthand date = DateShorthandReader.parse(text);

			Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_CALCULATED, null), date.first(), text);
		}
	}

	@Test
	void marginMakesTheDateApproximate(){
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P2Y"), DateShorthandReader.parse("1850 ±2").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P2Y"), DateShorthandReader.parse("1850±2").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P2Y"), DateShorthandReader.parse("1850 +/- 2 years").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P2Y"), DateShorthandReader.parse("1850 +-2y").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P2Y"), DateShorthandReader.parse("1850 ± 2 anni").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_CALCULATED, "P6M"), DateShorthandReader.parse("calc 1850 ±6m").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P3W"), DateShorthandReader.parse("abt 1850 ± 3 weeks").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P10D"), DateShorthandReader.parse("1850 ±10d").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P1Y6M"), DateShorthandReader.parse("1850 ±P1Y6M").first());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P2Y"), DateShorthandReader.parse("1850 ±p2y").first());
	}

	@Test
	void beforeAndAfterAreOpenBounds(){
		for(final String text : new String[]{"bef 1850", "before 1850", "BEF. 1850", "<1850", "< 1850", "prima del 1850", "prima di 1850",
				"avant 1850", "vor 1850", "antes de 1850"}){
			final DateShorthand date = DateShorthandReader.parse(text);

			Assertions.assertEquals(BOUNDED, date.kind(), text);
			Assertions.assertNull(date.first(), text);
			Assertions.assertEquals(single("1850", null, null), date.second(), text);
		}
		for(final String text : new String[]{"aft 1850", "after 1850", ">1850", "dopo il 1850", "après 1850", "nach 1850",
				"después de 1850"}){
			final DateShorthand date = DateShorthandReader.parse(text);

			Assertions.assertEquals(BOUNDED, date.kind(), text);
			Assertions.assertEquals(single("1850", null, null), date.first(), text);
			Assertions.assertNull(date.second(), text);
		}
	}

	@Test
	void between(){
		for(final String text : new String[]{"bet 1850 and 1860", "BETWEEN 1850 AND 1860", "tra il 1850 e il 1860", "entre 1850 et 1860",
				"zwischen 1850 und 1860", "entre 1850 y 1860", "bet 1850 & 1860"}){
			final DateShorthand date = DateShorthandReader.parse(text);

			Assertions.assertEquals(BOUNDED, date.kind(), text);
			Assertions.assertEquals(single("1850", null, null), date.first(), text);
			Assertions.assertEquals(single("1860", null, null), date.second(), text);
		}
	}

	@Test
	void everyBoundHasItsOwnQualifier(){
		final DateShorthand date = DateShorthandReader.parse("bet abt 1850 ±2 and 1860");

		Assertions.assertEquals(BOUNDED, date.kind());
		Assertions.assertEquals(single("1850", DateReader.ENUM_BASIS_STATED, "P2Y"), date.first());
		Assertions.assertEquals(single("1860", null, null), date.second());

		final DateShorthand open = DateShorthandReader.parse("bef calc 12 March 1850");
		Assertions.assertEquals(single("12 March 1850", DateReader.ENUM_BASIS_CALCULATED, null), open.second());
	}

	@Test
	void spanning(){
		for(final String text : new String[]{"from 1850 to 1860", "FROM 1850 TO 1860", "dal 1850 al 1860", "da 1850 a 1860", "de 1850 à 1860",
				"von 1850 bis 1860", "de 1850 a 1860"}){
			final DateShorthand date = DateShorthandReader.parse(text);

			Assertions.assertEquals(SPANNING, date.kind(), text);
			Assertions.assertEquals(single("1850", null, null), date.first(), text);
			Assertions.assertEquals(single("1860", null, null), date.second(), text);
		}
	}

	@Test
	void spanningOpenOnOneSide(){
		final DateShorthand from = DateShorthandReader.parse("from 1850");
		Assertions.assertEquals(SPANNING, from.kind());
		Assertions.assertEquals(single("1850", null, null), from.first());
		Assertions.assertNull(from.second());

		for(final String text : new String[]{"until 1860", "to 1860", "till 1860", "fino al 1860", "bis 1860", "hasta 1860"}){
			final DateShorthand to = DateShorthandReader.parse(text);

			Assertions.assertEquals(SPANNING, to.kind(), text);
			Assertions.assertNull(to.first(), text);
			Assertions.assertEquals(single("1860", null, null), to.second(), text);
		}
	}

	@Test
	void decade(){
		final DateShorthand date = DateShorthandReader.parse("1850s");

		Assertions.assertEquals(POINT, date.kind());
		Assertions.assertEquals(new DateShorthand.Single(null, 1850, null, null, null, null), date.first());
		Assertions.assertEquals(1850, DateShorthandReader.parse("1850's").first().decade());
		Assertions.assertEquals(new DateShorthand.Single(null, 1850, null, null, DateReader.ENUM_BASIS_STATED, null),
			DateShorthandReader.parse("abt 1850s").first());
	}

	@Test
	void century(){
		for(final String text : new String[]{"19th century", "19th c.", "19c", "19 century", "XIX secolo", "sec. XIX", "xix c."}){
			final DateShorthand date = DateShorthandReader.parse(text);

			Assertions.assertEquals(new DateShorthand.Single(null, null, 19, null, null, null), date.first(), text);
		}
		Assertions.assertEquals(1, DateShorthandReader.parse("1st century").first().century());
		Assertions.assertEquals(21, DateShorthandReader.parse("21st c.").first().century());
	}

	@Test
	void centuryPart(){
		Assertions.assertEquals(DateReader.ENUM_PART_EARLY, DateShorthandReader.parse("early 19th century").first().centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_MID, DateShorthandReader.parse("mid XIX c.").first().centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_LATE, DateShorthandReader.parse("late 19th c.").first().centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_FIRST_HALF, DateShorthandReader.parse("first half of the 19th century").first()
			.centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_SECOND_HALF, DateShorthandReader.parse("2nd half 19th c.").first().centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_THIRD_QUARTER, DateShorthandReader.parse("third quarter 19th c.").first().centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_EARLY, DateShorthandReader.parse("inizio XIX secolo").first().centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_MID, DateShorthandReader.parse("metà del XIX secolo").first().centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_SECOND_HALF, DateShorthandReader.parse("seconda metà del XIX secolo").first()
			.centuryPart());
		Assertions.assertEquals(DateReader.ENUM_PART_LATE, DateShorthandReader.parse("fine sec. XIX").first().centuryPart());
		Assertions.assertEquals(19, DateShorthandReader.parse("late 19th c. ±20").first().century());
	}

	@Test
	void decadeAndCenturyInBounds(){
		final DateShorthand date = DateShorthandReader.parse("bet 1850s and early 20th century");

		Assertions.assertEquals(BOUNDED, date.kind());
		Assertions.assertEquals(1850, date.first().decade());
		Assertions.assertEquals(20, date.second().century());
		Assertions.assertEquals(DateReader.ENUM_PART_EARLY, date.second().centuryPart());
	}

	@Test
	void languageSuffixStaysWithTheFullDates(){
		final DateShorthand date = DateShorthandReader.parse("bet 12 marzo 1850 and 1860 (it)");

		Assertions.assertEquals("12 marzo 1850 (it)", date.first().fullDate());
		Assertions.assertEquals("1860 (it)", date.second().fullDate());

		final DateShorthand approximate = DateShorthandReader.parse("abt 1850 ±2 (it)");
		Assertions.assertEquals("1850 (it)", approximate.first().fullDate());
		Assertions.assertEquals("P2Y", approximate.first().margin());
	}

	@Test
	void datesWithPointsAreLeftAlone(){
		Assertions.assertEquals("17.03.1850", DateShorthandReader.parse("17.03.1850").first().fullDate());
		Assertions.assertEquals("1850-03-17", DateShorthandReader.parse("abt 1850-03-17").first().fullDate());
		Assertions.assertEquals("12 Mar. 1850", DateShorthandReader.parse("12 Mar. 1850").first().fullDate());
	}


	@Test
	void emptyExpression(){
		assertFails(DateShorthandReader.Reason.EMPTY, "");
		assertFails(DateShorthandReader.Reason.EMPTY, "   ");
		assertFails(DateShorthandReader.Reason.EMPTY, null);
		assertFails(DateShorthandReader.Reason.EMPTY, "(it)");
	}

	@Test
	void keywordWithoutDate(){
		assertFails(DateShorthandReader.Reason.MISSING_VALUE, "abt");
		assertFails(DateShorthandReader.Reason.MISSING_VALUE, "bef");
		assertFails(DateShorthandReader.Reason.MISSING_VALUE, "from");
		assertFails(DateShorthandReader.Reason.MISSING_VALUE, "from 1850 to");
		assertFails(DateShorthandReader.Reason.MISSING_VALUE, "bet 1850 and");
		assertFails(DateShorthandReader.Reason.MISSING_VALUE, "abt ±2");
	}

	@Test
	void betweenWithoutAnd(){
		assertFails(DateShorthandReader.Reason.MISSING_BETWEEN_AND, "bet 1850");
	}

	@Test
	void badMargin(){
		assertFails(DateShorthandReader.Reason.BAD_MARGIN, "1850 ±");
		assertFails(DateShorthandReader.Reason.BAD_MARGIN, "1850 ±x");
		assertFails(DateShorthandReader.Reason.BAD_MARGIN, "1850 ±0");
		assertFails(DateShorthandReader.Reason.BAD_MARGIN, "1850 ±2 parsecs");
		assertFails(DateShorthandReader.Reason.BAD_MARGIN, "1850 ±P");
		assertFails(DateShorthandReader.Reason.BAD_MARGIN, "1850 ±P2X");
	}

	@Test
	void badDecade(){
		assertFails(DateShorthandReader.Reason.BAD_DECADE, "1855s");
	}

	@Test
	void badCenturyPart(){
		assertFails(DateShorthandReader.Reason.BAD_CENTURY_PART, "nearly 19th century");
	}


	private static DateShorthand.Single single(final String fullDate, final String basis, final String margin){
		return new DateShorthand.Single(fullDate, null, null, null, basis, margin);
	}

	private static void assertFails(final DateShorthandReader.Reason reason, final String text){
		final DateShorthandReader.SyntaxException exception = Assertions.assertThrows(DateShorthandReader.SyntaxException.class,
			() -> DateShorthandReader.parse(text), text);

		Assertions.assertEquals(reason, exception.getReason(), text);
	}

}
