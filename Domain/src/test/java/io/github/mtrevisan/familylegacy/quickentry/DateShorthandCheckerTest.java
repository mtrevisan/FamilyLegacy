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

import java.util.List;
import java.util.Locale;


/** A date typed in the shorthand is checked by its calendar: the reader of the shorthand only splits it. */
class DateShorthandCheckerTest{

	private static DateShorthandChecker.Problem check(final String fullDate){
		return DateShorthandChecker.check(fullDate, DateReader.ENUM_CALENDAR_GREGORIAN, Locale.ENGLISH);
	}


	@Test
	void theFullDatesAreTheOnesThatAreNotDecadesOrCenturies(){
		Assertions.assertEquals(List.of("12 March 1850"), DateShorthandChecker.fullDatesOf(DateShorthandReader.parse("12 March 1850")));
		Assertions.assertEquals(List.of("1850", "1860"), DateShorthandChecker.fullDatesOf(DateShorthandReader.parse("bet 1850 and 1860")));
		Assertions.assertEquals(List.of("1850"), DateShorthandChecker.fullDatesOf(DateShorthandReader.parse("from 1850")));
		Assertions.assertTrue(DateShorthandChecker.fullDatesOf(DateShorthandReader.parse("1850s")).isEmpty());
		Assertions.assertTrue(DateShorthandChecker.fullDatesOf(DateShorthandReader.parse("19th century")).isEmpty());
	}

	@Test
	void aDateThatTheCalendarKnowsHasNoProblem(){
		Assertions.assertNull(check("12 March 1850"));
		Assertions.assertNull(check("1850"));
		Assertions.assertNull(check("March 1850"));
		Assertions.assertNull(check("1850-03-12"));
		Assertions.assertNull(check("15 oct 1582"));
	}

	@Test
	void aDateThatIsNotADateIsNotRecognized(){
		for(final String text : new String[]{"31 February 1850", "33 Foo 1850", "ABC"}){
			final DateShorthandChecker.Problem problem = check(text);

			Assertions.assertNotNull(problem, text);
			Assertions.assertEquals(DateShorthandChecker.Kind.UNRECOGNIZED, problem.kind(), text);
			Assertions.assertEquals(text, problem.date());
			Assertions.assertTrue(problem.readings().isEmpty());
		}
	}

	@Test
	void aWordThatCanBeReadInMoreWaysIsAmbiguous(){
		final DateShorthandChecker.Problem problem = check("est 1850");

		Assertions.assertNotNull(problem);
		Assertions.assertEquals(DateShorthandChecker.Kind.AMBIGUOUS, problem.kind());
		Assertions.assertTrue(problem.readings().size() >= 2);
	}

	@Test
	void aCalendarThatIsNotKnownCannotBeChecked(){
		Assertions.assertNull(DateShorthandChecker.check("ABC", "no_such_calendar", Locale.ENGLISH));
	}

	@Test
	void theReadingChosenIsWritten(){
		final FLEFRecord date = DateStructureBuilder.fromShorthand("date", "est 1850", null, fullDate -> "est 1850 (italiano)");

		Assertions.assertEquals("est 1850 (italiano)", FLEFRecordHelper.getChildValue(date, FLEFRecordHelper.composePath(DateReader.TAG_VALUE,
			DateReader.TAG_POINT, DateReader.TAG_VALUE, DateReader.TAG_FULL_DATE)));
	}

	@Test
	void theReadingIsChosenForEachEndOfABoundedDate(){
		final FLEFRecord date = DateStructureBuilder.fromShorthand("date", "bet 1850 and 1860", null, fullDate -> fullDate + "!");

		Assertions.assertEquals("1850!", FLEFRecordHelper.getChildValue(date, FLEFRecordHelper.composePath(DateReader.TAG_VALUE,
			DateReader.TAG_BOUNDED, DateReader.TAG_NOT_BEFORE, DateReader.TAG_VALUE, DateReader.TAG_FULL_DATE)));
		Assertions.assertEquals("1860!", FLEFRecordHelper.getChildValue(date, FLEFRecordHelper.composePath(DateReader.TAG_VALUE,
			DateReader.TAG_BOUNDED, DateReader.TAG_NOT_AFTER, DateReader.TAG_VALUE, DateReader.TAG_FULL_DATE)));
	}

}
