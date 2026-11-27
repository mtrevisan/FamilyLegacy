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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateShorthandReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DateStructureBuilderTest{

	private static String value(final FLEFRecord date, final String... path){
		return FLEFRecordHelper.getChildValue(date, FLEFRecordHelper.composePath(path));
	}


	@Test
	void aPlainDateIsAPointInTheGregorianCalendarWithoutOriginalText(){
		final FLEFRecord date = DateStructureBuilder.fromShorthand("12 March 1850", null);

		Assertions.assertEquals("date", date.getTag());
		Assertions.assertEquals("12 March 1850", value(date, DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_VALUE,
			DateReader.TAG_FULL_DATE));
		Assertions.assertEquals(DateReader.ENUM_CALENDAR_GREGORIAN, value(date, DateReader.TAG_VALUE, DateReader.TAG_POINT,
			DateReader.TAG_CALENDAR));
		Assertions.assertNull(value(date, DateReader.TAG_ASSISTANT_FINDING_IDAL_TEXT));
		Assertions.assertNull(value(date, DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_APPROXIMATE, DateReader.TAG_BASIS));
	}

	@Test
	void theCalendarIsTheOneGiven(){
		final FLEFRecord date = DateStructureBuilder.fromShorthand("12 March 1850", DateReader.ENUM_CALENDAR_JULIAN);

		Assertions.assertEquals(DateReader.ENUM_CALENDAR_JULIAN, value(date, DateReader.TAG_VALUE, DateReader.TAG_POINT,
			DateReader.TAG_CALENDAR));
	}

	@Test
	void anApproximateDateKeepsItsBasisItsMarginAndTheTextTyped(){
		final FLEFRecord date = DateStructureBuilder.fromShorthand("abt 1850 ±2", null);

		Assertions.assertEquals("1850", value(date, DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_VALUE, DateReader.TAG_FULL_DATE));
		Assertions.assertEquals(DateReader.ENUM_BASIS_STATED, value(date, DateReader.TAG_VALUE, DateReader.TAG_POINT,
			DateReader.TAG_APPROXIMATE, DateReader.TAG_BASIS));
		Assertions.assertEquals("P2Y", value(date, DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_APPROXIMATE,
			DateReader.TAG_MARGIN));
		Assertions.assertEquals("abt 1850 ±2", value(date, DateReader.TAG_ASSISTANT_FINDING_IDAL_TEXT));
	}

	@Test
	void aBoundedDateHasItsTwoEnds(){
		final FLEFRecord date = DateStructureBuilder.fromShorthand("bet 1850 and 1855", null);

		Assertions.assertEquals("1850", value(date, DateReader.TAG_VALUE, DateReader.TAG_BOUNDED, DateReader.TAG_NOT_BEFORE, DateReader.TAG_VALUE,
			DateReader.TAG_FULL_DATE));
		Assertions.assertEquals("1855", value(date, DateReader.TAG_VALUE, DateReader.TAG_BOUNDED, DateReader.TAG_NOT_AFTER, DateReader.TAG_VALUE,
			DateReader.TAG_FULL_DATE));
		Assertions.assertEquals("bet 1850 and 1855", value(date, DateReader.TAG_ASSISTANT_FINDING_IDAL_TEXT));
	}

	@Test
	void anOpenEndIsNotWritten(){
		final FLEFRecord date = DateStructureBuilder.fromShorthand("from 1850", null);

		Assertions.assertEquals("1850", value(date, DateReader.TAG_VALUE, DateReader.TAG_SPANNING, DateReader.TAG_FROM, DateReader.TAG_VALUE,
			DateReader.TAG_FULL_DATE));
		Assertions.assertNull(FLEFRecordHelper.findChild(date, FLEFRecordHelper.composePath(DateReader.TAG_VALUE, DateReader.TAG_SPANNING,
			DateReader.TAG_TO)));
	}

	@Test
	void aDecadeAndACenturyAreNotFullDates(){
		final FLEFRecord decade = DateStructureBuilder.fromShorthand("1850s", null);
		Assertions.assertEquals("1850", value(decade, DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_VALUE, DateReader.TAG_DECADE));
		Assertions.assertNull(value(decade, DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_VALUE, DateReader.TAG_FULL_DATE));

		final FLEFRecord century = DateStructureBuilder.fromShorthand("early 19th century", null);
		Assertions.assertEquals("19", value(century, DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_VALUE, DateReader.TAG_CENTURY,
			DateReader.TAG_ORDINAL));
		Assertions.assertEquals(DateReader.ENUM_PART_EARLY, value(century, DateReader.TAG_VALUE, DateReader.TAG_POINT, DateReader.TAG_VALUE,
			DateReader.TAG_CENTURY, DateReader.TAG_PART));
	}

	@Test
	void blankIsNoDateAndGibberishIsRefused(){
		Assertions.assertNull(DateStructureBuilder.fromShorthand(null, null));
		Assertions.assertNull(DateStructureBuilder.fromShorthand("  ", null));
		Assertions.assertThrows(DateShorthandReader.SyntaxException.class, () -> DateStructureBuilder.fromShorthand("bet 1850", null));
	}

}
