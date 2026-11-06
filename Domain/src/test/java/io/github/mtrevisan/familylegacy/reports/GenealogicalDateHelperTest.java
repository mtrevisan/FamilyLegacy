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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;


/** The dates of the events are read in the structure the files have: the year (to sort and to give the lifespan) and the text. */
class GenealogicalDateHelperTest{

	private static FLEFModel model;
	private static ReportLabels labels;


	private static String event(final int number, final String date){
		return "\tEVENT E" + number + " {\n\t\tTYPE birth\n" + date + "\t}\n";
	}

	@BeforeAll
	static void load(){
		final String text = "header {\n\tPROTOCOL {\n\t\tNAME Family LEgacy Format\n\t\tVERSION 0.1.3\n\t}\n}\n\nrecords {\n"
			+ event(1, "\t\tDATE.VALUE.POINT {\n\t\t\tVALUE.FULL_DATE 1990-03-04\n\t\t\tCALENDAR gregorian\n\t\t}\n")
			+ event(2, "\t\tDATE.VALUE.POINT {\n\t\t\tVALUE.DECADE 1930\n\t\t\tCALENDAR gregorian\n\t\t}\n")
			+ event(3, "\t\tDATE.VALUE.POINT {\n\t\t\tVALUE.CENTURY.ORDINAL 18\n\t\t\tCALENDAR gregorian\n\t\t}\n")
			+ event(4, "\t\tDATE.VALUE.BOUNDED {\n\t\t\tNOT_BEFORE {\n\t\t\t\tVALUE.FULL_DATE 1850-01-01\n\t\t\t\tCALENDAR gregorian\n\t\t\t}\n"
				+ "\t\t\tNOT_AFTER {\n\t\t\t\tVALUE.FULL_DATE 1852-12-31\n\t\t\t\tCALENDAR gregorian\n\t\t\t}\n\t\t}\n")
			+ event(5, "\t\tDATE.VALUE.SPANNING {\n\t\t\tFROM {\n\t\t\t\tVALUE.FULL_DATE 1700-05-06\n\t\t\t\tCALENDAR gregorian\n\t\t\t}\n"
				+ "\t\t\tTO {\n\t\t\t\tVALUE.FULL_DATE 1710-01-01\n\t\t\t\tCALENDAR gregorian\n\t\t\t}\n\t\t}\n")
			+ event(6, "\t\tDATE {\n\t\t\tVALUE.POINT {\n\t\t\t\tVALUE.FULL_DATE 1800-01-01\n\t\t\t\tCALENDAR gregorian\n\t\t\t}\n"
				+ "\t\t\tORIGINAL_TEXT in the year of the plague\n\t\t}\n")
			+ event(7, "")
			+ "}\n";
		model = new FLEFParser().parse(text);
		labels = new ReportLabels(ReportLanguage.ENGLISH);
	}


	private static FLEFRecord event(final int number){
		return model.getRecordById("E" + number);
	}


	@Test
	void theYearOfAFullDateIsRead(){
		Assertions.assertEquals(1990, GenealogicalDateHelper.yearOrNull(event(1)));
		Assertions.assertEquals(LocalDate.of(1990, 3, 4), GenealogicalDateHelper.exactDateOrNull(event(1)));
	}

	@Test
	void theYearOfADecadeIsItsFirst(){
		Assertions.assertEquals(1930, GenealogicalDateHelper.yearOrNull(event(2)));
		Assertions.assertNull(GenealogicalDateHelper.exactDateOrNull(event(2)));
	}

	@Test
	void theYearOfACenturyIsRead(){
		final Integer year = GenealogicalDateHelper.yearOrNull(event(3));

		Assertions.assertNotNull(year);
		Assertions.assertTrue(year >= 1700 && year <= 1800, "the 18th century: " + year);
	}

	@Test
	void theYearOfABoundedOrSpanningDateIsTheFirstEndRead(){
		Assertions.assertEquals(1850, GenealogicalDateHelper.yearOrNull(event(4)));
		Assertions.assertEquals(1700, GenealogicalDateHelper.yearOrNull(event(5)));
	}

	@Test
	void anEventWithoutDateHasNoYearAndSortsLast(){
		Assertions.assertNull(GenealogicalDateHelper.yearOrNull(event(7)));
		Assertions.assertEquals(Integer.MAX_VALUE, GenealogicalDateHelper.yearOrMax(event(7)));
	}

	@Test
	void theTextIsTheOneTheFileHas(){
		Assertions.assertEquals("1990-03-04", GenealogicalDateHelper.formatEventDate(event(1), labels));
		//what the user wrote wins
		Assertions.assertEquals("in the year of the plague", GenealogicalDateHelper.formatEventDate(event(6), labels));
		Assertions.assertNull(GenealogicalDateHelper.formatEventDate(event(7), labels));
	}

	@Test
	void theTextOfABoundedAndASpanningDateHasBothEnds(){
		final String bounded = GenealogicalDateHelper.formatEventDate(event(4), labels);
		final String spanning = GenealogicalDateHelper.formatEventDate(event(5), labels);

		Assertions.assertTrue(bounded.contains("1850-01-01") && bounded.contains("1852-12-31"), bounded);
		Assertions.assertTrue(spanning.contains("1700-05-06") && spanning.contains("1710-01-01"), spanning);
	}

	@Test
	void theTextOfADecadeAndACenturyIsLocalized(){
		Assertions.assertTrue(GenealogicalDateHelper.formatEventDate(event(2), labels).contains("1930"));
		Assertions.assertTrue(GenealogicalDateHelper.formatEventDate(event(3), labels).contains("18"));
	}

}
