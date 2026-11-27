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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quality.DateRules;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** An attribute dated after the death of the individual (a statement of the record of a relative) is dated before the death. */
class PosthumousAttributeDatesTest{

	private static FLEFModel convert(final String body) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader("0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n"));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	private static FLEFRecord occupation(final FLEFModel model){
		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeReader.TYPE).stream()
			.filter(attribute -> IndividualAttributeReader.ENUM_TYPE_OCCUPATION.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE)))
			.toList();
		Assertions.assertEquals(1, attributes.size());
		return attributes.getFirst();
	}

	private static DateRange range(final FLEFRecord attribute, final String tag){
		final FLEFRecord date = FLEFRecordHelper.findChild(attribute, tag);
		return (date != null? DateRange.of(DateNormalizer.normalize(date)): null);
	}


	@Test
	void anOccupationDatedAfterTheDeathIsDatedBeforeTheDeath() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Giovanni /Rossi/
			1 DEAT
			2 DATE 3 MAR 1850
			1 OCCU contadino
			2 DATE 10 JUN 1860
			""");

		final FLEFRecord occupation = occupation(model);
		final DateRange from = range(occupation, IndividualAttributeReader.TAG_VALID_FROM);
		final DateRange death = DateRange.of(DateNormalizer.normalize(
			FLEFRecordHelper.findChild(model.getRecordsByType(EventReader.TYPE).getFirst(), "date")));

		//open on the left, closed on the death: not after the death, and the end is not stated
		Assertions.assertEquals(Long.MIN_VALUE, from.min());
		Assertions.assertEquals(death.max(), from.max());
		Assertions.assertNull(FLEFRecordHelper.findChild(occupation, IndividualAttributeReader.TAG_VALID_TO));
		Assertions.assertTrue(DateRules.period(from, null, null, death).isEmpty());
		//the date of the file is not lost
		final String note = FLEFRecordHelper.getChildValue(occupation, FLEFRecordHelper.composePath(NoteReader.TAG_NOTE, NoteReader.TAG_TEXT));
		Assertions.assertNotNull(note);
		Assertions.assertTrue(note.contains("1860"));
	}

	@Test
	void anOccupationDatedAfterADeathBeforeADateIsDatedBeforeThatDate() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Anastasia /Bonamin/
			1 DEAT
			2 DATE BEF 18 JAN 1807
			1 OCCU contadino
			2 DATE 30 JUL 1814
			""");

		final FLEFRecord occupation = occupation(model);
		final DateRange from = range(occupation, IndividualAttributeReader.TAG_VALID_FROM);
		final DateRange death = DateRange.of(DateNormalizer.normalize(
			FLEFRecordHelper.findChild(model.getRecordsByType(EventReader.TYPE).getFirst(), "date")));

		Assertions.assertEquals(Long.MIN_VALUE, from.min());
		Assertions.assertEquals(death.max(), from.max());
		Assertions.assertTrue(DateRules.period(from, null, null, death).isEmpty());
	}

	@Test
	void anOccupationDatedInTheLifeIsLeftAlone() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Giovanni /Rossi/
			1 DEAT
			2 DATE 3 MAR 1850
			1 OCCU contadino
			2 DATE 10 JUN 1840
			""");

		final FLEFRecord occupation = occupation(model);

		Assertions.assertNotEquals(Long.MIN_VALUE, range(occupation, IndividualAttributeReader.TAG_VALID_FROM).min());
		Assertions.assertNotNull(FLEFRecordHelper.findChild(occupation, IndividualAttributeReader.TAG_VALID_TO));
		Assertions.assertNull(FLEFRecordHelper.findChild(occupation, NoteReader.TAG_NOTE));
	}

	@Test
	void anOccupationOfAnIndividualWithoutADeathIsLeftAlone() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Giovanni /Rossi/
			1 OCCU contadino
			2 DATE 10 JUN 1860
			""");

		Assertions.assertNotEquals(Long.MIN_VALUE, range(occupation(model), IndividualAttributeReader.TAG_VALID_FROM).min());
	}

}
