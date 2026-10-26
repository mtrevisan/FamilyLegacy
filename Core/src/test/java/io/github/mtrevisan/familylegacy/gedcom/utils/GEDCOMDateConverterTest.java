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
package io.github.mtrevisan.familylegacy.gedcom.utils;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class GEDCOMDateConverterTest{

	@Test
	void exactDate(){
		final FLEFRecord date = GEDCOMDateConverter.buildDateStructure("date", "2 APR 1956");

		Assertions.assertEquals("2 APR 1956", FLEFRecordHelper.getChildValue(date, "value.point.value.full_date"));
		Assertions.assertEquals("gregorian", FLEFRecordHelper.getChildValue(date, "value.point.calendar"));
	}

	@Test
	void dateBefore1582IsJulian(){
		final FLEFRecord date = GEDCOMDateConverter.buildDateStructure("date", "12 MAR 1500");

		Assertions.assertEquals("julian", FLEFRecordHelper.getChildValue(date, "value.point.calendar"));
	}

	@Test
	void calendarEscapes(){
		final FLEFRecord julian = GEDCOMDateConverter.buildDateStructure("date", "@#DJULIAN@ 11 FEB 1731/32");
		Assertions.assertEquals("11 FEB 1731/32", FLEFRecordHelper.getChildValue(julian, "value.point.value.full_date"));
		Assertions.assertEquals("julian", FLEFRecordHelper.getChildValue(julian, "value.point.calendar"));

		final FLEFRecord hebrew = GEDCOMDateConverter.buildDateStructure("date", "@#DHEBREW@ 2 TVT 5758");
		Assertions.assertEquals("2 tevet 5758", FLEFRecordHelper.getChildValue(hebrew, "value.point.value.full_date"));
		Assertions.assertEquals("hebrew", FLEFRecordHelper.getChildValue(hebrew, "value.point.calendar"));

		final FLEFRecord french = GEDCOMDateConverter.buildDateStructure("date", "@#DFRENCH R@ 5 VEND 10");
		Assertions.assertEquals("5 vendemiaire 10", FLEFRecordHelper.getChildValue(french, "value.point.value.full_date"));
		Assertions.assertEquals("french_republican", FLEFRecordHelper.getChildValue(french, "value.point.calendar"));

		Assertions.assertNull(GEDCOMDateConverter.buildDateStructure("date", "@#DROMAN@ 5 AUC"));
	}

	@Test
	void calendarFromMonthWithoutEscape(){
		final FLEFRecord hebrew = GEDCOMDateConverter.buildDateStructure("date", "25 SVN 5757");

		Assertions.assertEquals("25 sivan 5757", FLEFRecordHelper.getChildValue(hebrew, "value.point.value.full_date"));
		Assertions.assertEquals("hebrew", FLEFRecordHelper.getChildValue(hebrew, "value.point.calendar"));
	}

	@Test
	void era(){
		for(final String gedcomDate : new String[]{"5 AUG 1100 B.C.", "5 AUG 1100 BC", "5 AUG 1100 BCE", "5 AUG 1100 B.C.E."}){
			final FLEFRecord date = GEDCOMDateConverter.buildDateStructure("date", gedcomDate);
			Assertions.assertEquals("5 AUG 1100 BCE", FLEFRecordHelper.getChildValue(date, "value.point.value.full_date"),
				gedcomDate);
			Assertions.assertEquals("julian", FLEFRecordHelper.getChildValue(date, "value.point.calendar"), gedcomDate);
		}
		for(final String gedcomDate : new String[]{"5 AUG 1900 CE", "5 AUG 1900 C.E.", "5 AUG 1900 AD", "5 AUG 1900 A.D."}){
			final FLEFRecord date = GEDCOMDateConverter.buildDateStructure("date", gedcomDate);
			Assertions.assertEquals("5 AUG 1900", FLEFRecordHelper.getChildValue(date, "value.point.value.full_date"),
				gedcomDate);
		}
	}

	@Test
	void approximateDates(){
		Assertions.assertEquals("stated", FLEFRecordHelper.getChildValue(
			GEDCOMDateConverter.buildDateStructure("date", "ABT 1930"), "value.point.approximate.basis"));
		Assertions.assertEquals("calculated", FLEFRecordHelper.getChildValue(
			GEDCOMDateConverter.buildDateStructure("date", "CAL 1930"), "value.point.approximate.basis"));
		//conventional would require a cultural norm
		Assertions.assertEquals("unspecified", FLEFRecordHelper.getChildValue(
			GEDCOMDateConverter.buildDateStructure("date", "EST 1930"), "value.point.approximate.basis"));
	}

	@Test
	void rangesAndPeriods(){
		final FLEFRecord between = GEDCOMDateConverter.buildDateStructure("date", "BET MAY 1979 AND AUG 1979");
		Assertions.assertEquals("MAY 1979", FLEFRecordHelper.getChildValue(between, "value.bounded.not_before.value.full_date"));
		Assertions.assertEquals("AUG 1979", FLEFRecordHelper.getChildValue(between, "value.bounded.not_after.value.full_date"));

		final FLEFRecord to = GEDCOMDateConverter.buildDateStructure("date", "TO 31 DEC 1997");
		Assertions.assertEquals("31 DEC 1997", FLEFRecordHelper.getChildValue(to, "value.spanning.to.value.full_date"));
		Assertions.assertNull(FLEFRecordHelper.findChild(to, "value.spanning.from"));

		final FLEFRecord from = GEDCOMDateConverter.buildDateStructure("date", "FROM 31 DEC 1997");
		Assertions.assertEquals("31 DEC 1997", FLEFRecordHelper.getChildValue(from, "value.spanning.from.value.full_date"));
	}

	@Test
	void interpretedAndPhrase(){
		final FLEFRecord interpreted = GEDCOMDateConverter.buildDateStructure("date", "INT 1995 (from estimated age)");
		Assertions.assertEquals("1995", FLEFRecordHelper.getChildValue(interpreted, "value.point.value.full_date"));
		Assertions.assertEquals("from estimated age", FLEFRecordHelper.getChildValue(interpreted, "original_text"));

		Assertions.assertNull(GEDCOMDateConverter.buildDateStructure("date", "(No idea of the date)"));
	}

	@Test
	void validityOfAnObservedFact(){
		final FLEFRecord[] validity = GEDCOMDateConverter.buildValidity("14 FEB 1953");

		Assertions.assertEquals("14 FEB 1953", FLEFRecordHelper.getChildValue(validity[0],
			"value.bounded.not_after.value.full_date"));
		Assertions.assertEquals("14 FEB 1953", FLEFRecordHelper.getChildValue(validity[1],
			"value.bounded.not_before.value.full_date"));
	}

	@Test
	void validityOfAPeriod(){
		final FLEFRecord[] validity = GEDCOMDateConverter.buildValidity("FROM 1900 TO 1910");

		Assertions.assertEquals("1900", FLEFRecordHelper.getChildValue(validity[0], "value.point.value.full_date"));
		Assertions.assertEquals("1910", FLEFRecordHelper.getChildValue(validity[1], "value.point.value.full_date"));
	}

	@Test
	void dateTime(){
		final GEDCOMNode date = new GEDCOMNode(2, "DATE", "14 OCT 2026");
		date.addChild(new GEDCOMNode(3, "TIME", "20:51:22"));

		Assertions.assertEquals("2026-10-14T20:51:22Z", GEDCOMDateConverter.toDateTime(date));
		Assertions.assertEquals("2026-10-14T00:00:00Z", GEDCOMDateConverter.toDateTime(new GEDCOMNode(2, "DATE", "14 OCT 2026")));
		Assertions.assertNull(GEDCOMDateConverter.toDateTime(new GEDCOMNode(2, "DATE", "OCT 2026")));
	}

}
