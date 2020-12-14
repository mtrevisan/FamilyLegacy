/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class TransformerSourceRecordTest{

	@Test
	void sourceRecordTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithID("SOUR", "@S1@")
				.addChild(transformerTo.create("DATA")
					.addChild(transformerTo.create("EVEN")
						.withValue("EVENTS_RECORDED")
						.addChildValue("DATE", "DATE_PERIOD")
						.addChildValue("PLAC", "SOURCE_JURISDICTION_PLACE")
					)
					.addChildValue("AGNC", "RESPONSIBLE_AGENCY")
					.addChildReference("NOTE", "@N1@")
				)
				.addChildValue("AUTH", "SOURCE_ORIGINATOR")
				.addChildValue("TITL", "SOURCE_DESCRIPTIVE_TITLE")
				.addChildValue("ABBR", "SOURCE_FILED_BY_ENTRY")
				.addChildValue("PUBL", "SOURCE_PUBLICATION_FACTS")
				.addChildValue("TEXT", "TEXT_FROM_SOURCE")
				.addChildValue("REPO", "@R1@")
				.addChild(transformerTo.createWithReference("OBJE", "@O1@"))
				.addChild(transformerTo.create("OBJE")
					.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
					.addChild(transformerTo.create("FORM")
						.withValue("MULTIMEDIA_FORMAT")
						.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
					)
					.addChildValue("TITL", "DESCRIPTIVE_TITLE")
				)
				.addChild(transformerTo.create("REFN")
					.withValue("USER_REFERENCE_NUMBER")
					.addChildValue("TYPE", "AUTOMATED_RECORD_ID")
				)
				.addChildReference("NOTE", "@N2@")
				.addChild(transformerTo.create("OBJE")
					.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
				)
			);
		final GedcomNode repository = transformerTo.createWithID("REPO", "@R1@");
		final GedcomNode note1 = transformerTo.createWithID("NOTE", "@N1@");
		final GedcomNode note2 = transformerTo.createWithID("NOTE", "@N2@");
		final GedcomNode object = transformerTo.createWithID("OBJE", "@O1@")
			.addChildValue("FILE", "MULTIMEDIA_FILE_REFN");

		Assertions.assertEquals("children: [{id: @S1@, tag: SOUR, children: [{tag: DATA, children: [{tag: EVEN, value: EVENTS_RECORDED, children: [{tag: DATE, value: DATE_PERIOD}, {tag: PLAC, value: SOURCE_JURISDICTION_PLACE}]}, {tag: AGNC, value: RESPONSIBLE_AGENCY}, {tag: NOTE, ref: @N1@}]}, {tag: AUTH, value: SOURCE_ORIGINATOR}, {tag: TITL, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: ABBR, value: SOURCE_FILED_BY_ENTRY}, {tag: PUBL, value: SOURCE_PUBLICATION_FACTS}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {tag: REPO, value: @R1@}, {tag: OBJE, ref: @O1@}, {tag: OBJE, children: [{tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: TITL, value: DESCRIPTIVE_TITLE}]}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: AUTOMATED_RECORD_ID}]}, {tag: NOTE, ref: @N2@}, {tag: OBJE, children: [{tag: FILE, value: MULTIMEDIA_FILE_REFN}]}]}]", parent.toString());
		Assertions.assertEquals("id: @R1@, tag: REPO", repository.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note1.toString());
		Assertions.assertEquals("id: @N2@, tag: NOTE", note2.toString());

		final Gedcom origin = new Gedcom();
		origin.addRepository(repository);
		origin.addNote(note1);
		origin.addNote(note2);
		origin.addObject(object);
		final Flef destination = new Flef();
		transformerTo.sourceRecordTo(parent, origin, destination);

		Assertions.assertEquals("id: @S1@, tag: SOURCE, children: [{tag: EVENT, value: EVENTS_RECORDED}, {tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: DATE, value: DATE_PERIOD, children: [{tag: CALENDAR, value: gregorian}]}, {tag: AUTHOR, value: SOURCE_ORIGINATOR}, {tag: PUBLICATION_FACTS, value: SOURCE_PUBLICATION_FACTS}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: EXTRACT, value: TEXT_FROM_SOURCE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: DESCRIPTION, value: DESCRIPTIVE_TITLE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: NOTE, ref: @N2@}]", destination.getSources().get(0).toString());
	}

	@Test
	void sourceRecordFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode source = transformerFrom.createWithID("SOURCE", "@S1@")
			.addChildValue("EVENT", "EVENTS_RECORDED")
			.addChildValue("TITLE", "SOURCE_DESCRIPTIVE_TITLE")
			.addChildValue("AUTHOR", "SOURCE_ORIGINATOR")
			.addChildValue("PUBLICATION_FACTS", "SOURCE_PUBLICATION_FACTS")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE")
			.addChildValue("REPOSITORY", "@R1@")
			.addChildValue("FILE", "DOCUMENT_FILE_REFERENCE")
			.addChildValue("MEDIA_TYPE", "SOURCE_MEDIA_TYPE")
			.addChildReference("NOTE", "@N1@");

		Assertions.assertEquals("id: @S1@, tag: SOURCE, children: [{tag: EVENT, value: EVENTS_RECORDED}, {tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: AUTHOR, value: SOURCE_ORIGINATOR}, {tag: PUBLICATION_FACTS, value: SOURCE_PUBLICATION_FACTS}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: REPOSITORY, value: @R1@}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE}, {tag: MEDIA_TYPE, value: SOURCE_MEDIA_TYPE}, {tag: NOTE, ref: @N1@}]", source.toString());

		final Gedcom destination = new Gedcom();
		transformerFrom.sourceRecordFrom(source, destination);

		Assertions.assertEquals("id: @S1@, tag: SOUR, children: [{tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}]}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: NOTE, ref: @N1@}]", destination.getSources().get(0).toString());
	}

}