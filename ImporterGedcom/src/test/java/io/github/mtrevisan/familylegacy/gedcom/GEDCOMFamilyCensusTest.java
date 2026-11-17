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
package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMDateConverter;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/** A census of a family is about the family and the spouses alive at its date: a spouse who died before it is not in it. */
class GEDCOMFamilyCensusTest{

	private static FLEFModel convert(final String body) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots, (percent, message) -> {}, new ImportReport());
	}

	private static String family(final String husbandDeath, final String censusDate){
		return "0 @I1@ INDI\n1 NAME Mario /Rossi/\n1 SEX M\n"
			+ (husbandDeath != null? "1 DEAT\n2 DATE " + husbandDeath + "\n": "")
			+ "0 @I2@ INDI\n1 NAME Anna /Bianchi/\n1 SEX F\n"
			+ "0 @F1@ FAM\n1 HUSB @I1@\n1 WIFE @I2@\n1 CENS\n2 DATE " + censusDate + "\n";
	}

	/** The participants of the census: the ids of the individuals and of the groups. */
	private static List<String> censusParticipants(final FLEFModel model){
		String censusId = null;
		for(final FLEFRecord event : model.getRecordsByType(EventReader.TYPE))
			if(EventReader.ENUM_TYPE_CENSUS.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
				censusId = event.getId();
		Assertions.assertNotNull(censusId, "no census");

		final List<String> participants = new ArrayList<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE))
			if(censusId.equals(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT))){
				final FLEFRecord participant = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT);
				participants.add(participant.getChildren().getFirst().getValue());
			}
		return participants;
	}


	@Test
	void aSpouseWhoDiedBeforeTheCensusIsNotInIt() throws IOException{
		final List<String> participants = censusParticipants(convert(family("12 MAR 1875", "3 APR 1881")));

		Assertions.assertEquals(List.of("G1", "I2"), participants);
	}

	@Test
	void aSpouseWhoDiedAfterTheCensusIsInIt() throws IOException{
		final List<String> participants = censusParticipants(convert(family("12 MAR 1885", "3 APR 1881")));

		Assertions.assertEquals(List.of("G1", "I1", "I2"), participants);
	}

	@Test
	void aSpouseWhoMayHaveDiedAfterTheCensusIsInIt() throws IOException{
		//the same year, an approximate death, a death known only to be after a date
		for(final String death : List.of("1881", "ABT 1880", "AFT 1870"))
			Assertions.assertEquals(List.of("G1", "I1", "I2"), censusParticipants(convert(family(death, "3 APR 1881"))), death);
	}

	@Test
	void aBurialBeforeTheCensusIsADeathBeforeIt() throws IOException{
		final List<String> participants = censusParticipants(convert("0 @I1@ INDI\n1 NAME Mario /Rossi/\n1 SEX M\n1 BURI\n2 DATE 1870\n"
			+ "0 @I2@ INDI\n1 NAME Anna /Bianchi/\n1 SEX F\n"
			+ "0 @F1@ FAM\n1 HUSB @I1@\n1 WIFE @I2@\n1 CENS\n2 DATE 1881\n"));

		Assertions.assertEquals(List.of("G1", "I2"), participants);
	}

	@Test
	void aDeathWrittenInAnotherLanguageIsADeathBeforeTheCensus() throws IOException{
		final List<String> participants = censusParticipants(convert(family("12 marzo 1875", "3 APR 1881")));

		Assertions.assertEquals(List.of("G1", "I2"), participants);
	}

	@Test
	void theOtherFamilyEventsKeepTheSpouses() throws IOException{
		final FLEFModel model = convert("0 @I1@ INDI\n1 NAME Mario /Rossi/\n1 SEX M\n1 DEAT\n2 DATE 1870\n"
			+ "0 @I2@ INDI\n1 NAME Anna /Bianchi/\n1 SEX F\n"
			+ "0 @F1@ FAM\n1 HUSB @I1@\n1 WIFE @I2@\n1 MARR\n2 DATE 1880\n");

		Assertions.assertEquals(3, model.getRecordsByType(EventParticipationReader.TYPE).stream()
			.filter(participation -> !FLEFRecordHelper.findChildren(participation, EventParticipationReader.TAG_EVENT).isEmpty())
			.filter(participation -> {
				final FLEFRecord event = model.getRecordById(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT));
				return EventReader.ENUM_TYPE_MARRIAGE.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE));
			})
			.count());
	}


	@Test
	void theDaysOfADate(){
		//1 Jan 1881 is the Julian Day 2408082 (1 Jan 2000 is 2451545)
		Assertions.assertArrayEquals(new long[]{2_408_082L, 2_408_082L + 364}, GEDCOMDateConverter.dayRange("1881"));
		Assertions.assertArrayEquals(new long[]{2_408_082L + 31 + 28, 2_408_082L + 31 + 28 + 30}, GEDCOMDateConverter.dayRange("MAR 1881"));
		Assertions.assertArrayEquals(new long[]{2_408_082L + 31 + 28 + 31 + 2, 2_408_082L + 31 + 28 + 31 + 2},
			GEDCOMDateConverter.dayRange("3 APR 1881"));
		final long[] approximate = GEDCOMDateConverter.dayRange("ABT 1881");
		Assertions.assertEquals(2_408_082L - 2 * 366, approximate[0]);
		Assertions.assertEquals(Long.MAX_VALUE, GEDCOMDateConverter.dayRange("AFT 1881")[1]);
		Assertions.assertEquals(Long.MIN_VALUE, GEDCOMDateConverter.dayRange("BEF 1881")[0]);
		//in 1650 the julian calendar is ten days behind the gregorian one
		Assertions.assertEquals(GEDCOMDateConverter.dayRange("11 JAN 1650")[0], GEDCOMDateConverter.dayRange("@#DJULIAN@ 1 JAN 1650")[0]);
		Assertions.assertNull(GEDCOMDateConverter.dayRange("(sometime)"));
		//a date not written as GEDCOM wants is read by the parser of its calendar
		Assertions.assertArrayEquals(GEDCOMDateConverter.dayRange("3 APR 1881"), GEDCOMDateConverter.dayRange("3 aprile 1881"));
		Assertions.assertArrayEquals(GEDCOMDateConverter.dayRange("3 APR 1881"), GEDCOMDateConverter.dayRange("April 3, 1881"));
		Assertions.assertArrayEquals(GEDCOMDateConverter.dayRange("MAR 1881"), GEDCOMDateConverter.dayRange("marzo 1881"));
		//a date of another calendar is read by its parser: a year lasts at most the longest year of any calendar
		final long[] hebrew = GEDCOMDateConverter.dayRange("@#DHEBREW@ 5600");
		Assertions.assertNotNull(hebrew);
		Assertions.assertTrue(hebrew[1] - hebrew[0] >= 383, hebrew[0] + " " + hebrew[1]);
	}

}
