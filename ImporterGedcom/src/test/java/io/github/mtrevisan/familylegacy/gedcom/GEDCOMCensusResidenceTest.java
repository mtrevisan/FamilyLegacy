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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The residences derived from the censuses of an individual that has no residence. */
class GEDCOMCensusResidenceTest{

	private static List<FLEFRecord> residences(final String individual) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n" + individual + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		final FLEFModel model = new GEDCOMToFLEFConverter().convert(roots);
		return model.getRecordsByType(IndividualAttributeReader.TYPE).stream()
			.filter(a -> IndividualAttributeReader.ENUM_TYPE_RESIDENCE.equals(FLEFRecordHelper.getChildValue(a,
				IndividualAttributeReader.TAG_TYPE)))
			.toList();
	}

	@Test
	void aCensusWithDateAndPlaceGivesAResidence() throws IOException{
		final List<FLEFRecord> residences = residences("""
			0 @I1@ INDI
			1 NAME Mario /Rossi/
			1 CENS Y
			2 DATE 4 APR 1940
			2 PLAC Passaic, New Jersey, United States of America
			""");

		Assertions.assertEquals(1, residences.size());
		final FLEFRecord residence = residences.get(0);
		Assertions.assertNotNull(FLEFRecordHelper.findChild(residence, IndividualAttributeReader.TAG_PLACE));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(residence, IndividualAttributeReader.TAG_VALID_FROM));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(residence, IndividualAttributeReader.TAG_NOTE));
	}

	@Test
	void twoCensusesAtDifferentDatesGiveTwoResidences() throws IOException{
		Assertions.assertEquals(2, residences("""
			0 @I1@ INDI
			1 NAME Mario /Rossi/
			1 CENS Y
			2 DATE 1930
			2 PLAC Clifton, New Jersey, United States of America
			1 CENS Y
			2 DATE 1940
			2 PLAC Passaic, New Jersey, United States of America
			""").size());
	}

	@Test
	void aCensusWithoutPlaceGivesNothing() throws IOException{
		Assertions.assertTrue(residences("""
			0 @I1@ INDI
			1 NAME Mario /Rossi/
			1 CENS Y
			2 DATE 1940
			""").isEmpty());
	}

	@Test
	void aCensusWithoutDateGivesNothing() throws IOException{
		Assertions.assertTrue(residences("""
			0 @I1@ INDI
			1 NAME Mario /Rossi/
			1 CENS Y
			2 PLAC Passaic, New Jersey, United States of America
			""").isEmpty());
	}

	@Test
	void anIndividualWithAResidenceGetsNoOtherOne() throws IOException{
		Assertions.assertEquals(1, residences("""
			0 @I1@ INDI
			1 NAME Mario /Rossi/
			1 RESI
			2 PLAC Treviso, Italia
			1 CENS Y
			2 DATE 1940
			2 PLAC Passaic, New Jersey, United States of America
			""").size());
	}
}
