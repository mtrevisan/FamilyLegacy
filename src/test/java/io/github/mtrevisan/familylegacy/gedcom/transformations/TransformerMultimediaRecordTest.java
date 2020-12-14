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


class TransformerMultimediaRecordTest{

	@Test
	void multimediaRecordTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode object = transformerTo.createWithID("OBJE", "@M1@")
			.addChild(transformerTo.create("FILE")
				.withValue("MULTIMEDIA_FILE_REFN")
				.addChild(transformerTo.create("FORM")
					.withValue("MULTIMEDIA_FORMAT")
					.addChildValue("TYPE", "SOURCE_MEDIA_TYPE")
				)
				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
			)
			.addChild(transformerTo.create("REFN")
				.withValue("USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChildReference("NOTE", "@N1@")
			.addChildReference("SOUR", "@S1@");

		final Gedcom origin = new Gedcom();
		origin.addObject(object);
		origin.addNote(transformerTo.createWithIDValue("NOTE", "@N1@", "SUBMITTER_TEXT"));
		origin.addNote(transformerTo.createWithID("SOUR", "@S1@"));
		final Flef destination = new Flef();
		transformerTo.multimediaRecordTo(object, destination);

		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: MEDIA_TYPE, value: SOURCE_MEDIA_TYPE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: DESCRIPTION, value: DESCRIPTIVE_TITLE}]}, {tag: NOTE, ref: @N1@}]", destination.getSources().get(0).toString());
	}

	@Test
	void multimediaRecordFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode source= transformerFrom.create("SOURCE")
			.addChildValue("EVENT", "EVENTS_RECORDED")
			.addChildValue("TITLE", "SOURCE_DESCRIPTIVE_TITLE")
			.addChildValue("AUTHOR", "SOURCE_ORIGINATOR")
			.addChildValue("PUBLICATION_FACTS", "SOURCE_PUBLICATION_FACTS")
			.addChildValue("DATE", "ENTRY_RECORDING_DATE")
			.addChildReference("REPOSITORY", "@R1@")
			.addChild(transformerFrom.create("FILE")
				.withValue("DOCUMENT_FILE_REFERENCE")
				.addChildValue("DESCRIPTION", "DOCUMENT_DESCRIPTION")
				.addChild(transformerFrom.create("EXTRACT")
					.withValue("TEXTED_TEXT_FROM_SOURCE")
					.addChildValue("TYPE", "EXTRACT_TYPE")
					.addChildValue("LOCALE", "EXTRACT_LOCALE_CODE")
				)
			)
			.addChildValue("MEDIA_TYPE", "SOURCE_MEDIA_TYPE")
			.addChildReference("NOTE", "@N1@");
		final GedcomNode note = transformerFrom.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("tag: SOURCE, children: [{tag: EVENT, value: EVENTS_RECORDED}, {tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: AUTHOR, value: SOURCE_ORIGINATOR}, {tag: PUBLICATION_FACTS, value: SOURCE_PUBLICATION_FACTS}, {tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: REPOSITORY, ref: @R1@}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE, children: [{tag: DESCRIPTION, value: DOCUMENT_DESCRIPTION}, {tag: EXTRACT, value: TEXTED_TEXT_FROM_SOURCE, children: [{tag: TYPE, value: EXTRACT_TYPE}, {tag: LOCALE, value: EXTRACT_LOCALE_CODE}]}]}, {tag: MEDIA_TYPE, value: SOURCE_MEDIA_TYPE}, {tag: NOTE, ref: @N1@}]", source.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final Gedcom destination = new Gedcom();
		transformerFrom.multimediaRecordFrom(source, destination);

		Assertions.assertEquals("id: O1, tag: OBJE, children: [{tag: FILE, value: DOCUMENT_FILE_REFERENCE, children: [{tag: TITL, value: DOCUMENT_DESCRIPTION}, {tag: FORM, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}]}, {tag: NOTE, ref: @N1@}]", destination.getObjects().get(0).toString());
	}

}