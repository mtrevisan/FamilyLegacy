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
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The type of a GEDCOM source (_TYPE) is its kind: known types are mapped, others are kept as a custom kind, descriptions stay notes. */
class GEDCOMSourceKindTest{

	private static FLEFModel convert(final String body) throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots, (percent, message) -> {}, new ImportReport());
	}


	@Test
	void aKnownTypeOrASynonymIsAKindOfTheVocabulary(){
		Assertions.assertEquals(SourceReader.ENUM_KIND_CENSUS, GEDCOMMapper.mapSourceKind("Census Record"));
		Assertions.assertEquals(SourceReader.ENUM_KIND_CHURCH_REGISTER, GEDCOMMapper.mapSourceKind("Parish register"));
		Assertions.assertEquals(SourceReader.ENUM_KIND_SCHOOL_RECORD, GEDCOMMapper.mapSourceKind("school_record"));
		Assertions.assertEquals(SourceReader.ENUM_KIND_NEWSPAPER, GEDCOMMapper.mapSourceKind("Obituary"));
	}

	@Test
	void anUnknownShortTypeIsKeptAsItWasWritten(){
		Assertions.assertEquals("land_survey", GEDCOMMapper.mapSourceKind("Land Survey"));
		Assertions.assertNull(GEDCOMMapper.mapSourceKind(null));
		//a description is not a type
		Assertions.assertNull(GEDCOMMapper.mapSourceKind("This is a long description of the source and not a type"));
	}

	@Test
	void theKnownKindsAreTheOnlyOnesMadeFromAMediumOrATag(){
		Assertions.assertEquals(SourceReader.ENUM_KIND_NEWSPAPER, GEDCOMMapper.mapKnownSourceKind("Newspaper"));
		Assertions.assertNull(GEDCOMMapper.mapKnownSourceKind("ToDo"));
	}

	@Test
	void theTypeOfASourceBecomesItsKind() throws IOException{
		final FLEFModel model = convert("""
			0 @S1@ SOUR
			1 TITL Household list 1881
			1 _TYPE Census Record
			0 @S2@ SOUR
			1 TITL Survey
			1 _TYPE Land Survey
			0 @S3@ SOUR
			1 TITL Annual
			1 _TYPE A long text that describes the source and is no type at all
			""");

		Assertions.assertEquals("census", SourceReader.extractKind(model.getRecordById("S1")));
		Assertions.assertEquals("land_survey", SourceReader.extractKind(model.getRecordById("S2")));
		//the description is not lost
		Assertions.assertNull(SourceReader.extractKind(model.getRecordById("S3")));
		Assertions.assertFalse(FLEFRecordHelper.findChildren(model.getRecordById("S3"), NoteReader.TAG_NOTE).isEmpty());
		//the type that became the kind is not kept again as a note
		Assertions.assertTrue(FLEFRecordHelper.findChildren(model.getRecordById("S1"), NoteReader.TAG_NOTE).isEmpty());
	}

	@Test
	void theCivilOrReligiousTypeOfAnEventIsTheKindOfItsSource() throws IOException{
		final FLEFModel model = convert("""
			0 @I1@ INDI
			1 NAME Mario /Rossi/
			1 BIRT
			2 TYPE religious
			2 DATE 1 JAN 1850
			2 SOUR @S1@
			1 DEAT
			2 TYPE civil
			2 DATE 1 JAN 1900
			2 SOUR @S2@
			1 BURI
			2 TYPE religious
			2 SOUR @S3@
			1 CHR
			2 TYPE religious
			0 @S1@ SOUR
			1 TITL Register A
			0 @S2@ SOUR
			1 TITL Register B
			0 @S3@ SOUR
			1 TITL Register C
			1 _TYPE Census Record
			""");

		Assertions.assertEquals(SourceReader.ENUM_KIND_CHURCH_REGISTER, SourceReader.extractKind(model.getRecordById("S1")));
		Assertions.assertEquals(SourceReader.ENUM_KIND_CIVIL_REGISTRATION, SourceReader.extractKind(model.getRecordById("S2")));
		//a source that already has another kind keeps it, and the type of the event is not lost
		Assertions.assertEquals(SourceReader.ENUM_KIND_CENSUS, SourceReader.extractKind(model.getRecordById("S3")));
		//with a cited source the type is not a description (the baptism cites none: its type stays as the description)
		final List<String> descriptions = model.getRecordsByType("event").stream()
			.map(event -> FLEFRecordHelper.getChildValue(event, "description"))
			.filter(java.util.Objects::nonNull)
			.toList();
		Assertions.assertEquals(List.of("religious"), descriptions);
	}

	@Test
	void theKindAndTheMediaTypeAreIndependent() throws IOException{
		final FLEFModel model = convert("""
			0 @R1@ REPO
			1 NAME Archive
			0 @S1@ SOUR
			1 TITL Register
			1 _TYPE Church Register
			1 REPO @R1@
			2 CALN 12
			3 MEDI Film
			""");

		final FLEFRecord source = model.getRecordById("S1");
		Assertions.assertEquals("church_register", SourceReader.extractKind(source));
		Assertions.assertEquals("film", SourceReader.extractMediaType(source));
	}

}
