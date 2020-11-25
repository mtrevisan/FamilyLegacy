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


class SourceTransformationTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void to(){
		final GedcomNode source = transformerTo.create("SOUR")
			.withID("S1")
			.addChild(transformerTo.create("DATA")
				.addChild(transformerTo.create("EVEN")
					.withValue("EVENTS_RECORDED1")
					.addChildValue("DATE", "DATE_PERIOD")
					.addChildValue("PLAC", "SOURCE_JURISDICTION_PLACE")
				)
				.addChildValue("EVEN", "EVENTS_RECORDED2")
			)
			.addChildValue("AUTH", "SOURCE_ORIGINATOR")
			.addChildValue("TITL", "SOURCE_DESCRIPTIVE_TITLE")
			.addChildValue("ABBR", "SOURCE_FILED_BY_ENTRY")
			.addChildValue("PUBL", "SOURCE_PUBLICATION_FACTS")
			.addChildValue("TEXT", "TEXT_FROM_SOURCE")
			.addChildReference("REPO", "R1")
			.addChild(transformerTo.create("REPO")
				.addChildReference("NOTE", "N2")
				.addChildValue("CALN", "SOURCE_CALL_NUMBER")
			)
			.addChild(transformerTo.create("REFN")
				.withValue("USER_REFERENCE_NUMBER")
				.addChildValue("TYPE", "USER_REFERENCE_TYPE")
			)
			.addChildValue("RIN", "AUTOMATED_RECORD_ID")
			.addChild(transformerTo.create("CHAN")
				.addChildValue("DATE", "CHANGE_DATE"))
			.addChildReference("NOTE", "N1")
			.addChildValue("NOTE", "SUBMITTER_TEXT")
			.addChildReference("OBJE", "D1")
			.addChild(transformerTo.create("OBJE")
				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
				.addChild(transformerTo.create("FORM")
					.withValue("MULTIMEDIA_FORMAT")
					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
				)
				.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
				.addChildValue("_CUTD", "CUT_COORDINATES")
				.addChildValue("_PREF", "Y")
			);
		final Gedcom origin = new Gedcom();
		origin.addSource(source);
		origin.addNote(transformerTo.createWithID("NOTE", "N1", "SUBMITTER_TEXT"));
		final Flef destination = new Flef();

		Assertions.assertEquals("id: S1, tag: SOUR, children: [{tag: DATA, children: [{tag: EVEN, value: EVENTS_RECORDED1, children: [{tag: DATE, value: DATE_PERIOD}, {tag: PLAC, value: SOURCE_JURISDICTION_PLACE}]}, {tag: EVEN, value: EVENTS_RECORDED2}]}, {tag: AUTH, value: SOURCE_ORIGINATOR}, {tag: TITL, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: ABBR, value: SOURCE_FILED_BY_ENTRY}, {tag: PUBL, value: SOURCE_PUBLICATION_FACTS}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {tag: REPO, ref: R1}, {tag: REPO, children: [{tag: NOTE, ref: N2}, {tag: CALN, value: SOURCE_CALL_NUMBER}]}, {tag: REFN, value: USER_REFERENCE_NUMBER, children: [{tag: TYPE, value: USER_REFERENCE_TYPE}]}, {tag: RIN, value: AUTOMATED_RECORD_ID}, {tag: CHAN, children: [{tag: DATE, value: CHANGE_DATE}]}, {tag: NOTE, ref: N1}, {tag: NOTE, value: SUBMITTER_TEXT}, {tag: OBJE, ref: D1}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: _CUTD, value: CUT_COORDINATES}, {tag: _PREF, value: Y}]}]", origin.getSources().get(0).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", origin.getNotes().get(0).toString());

		final Transformation<Gedcom, Flef> t = new SourceTransformation();
		t.to(origin, destination);

		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: DESCRIPTIVE_TITLE}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: FORMAT, value: MULTIMEDIA_FORMAT}, {tag: MEDIA, value: SOURCE_MEDIA_TYPE}, {tag: CUT, value: CUT_COORDINATES}, {tag: PREFERRED, value: Y}]}]", destination.getSources().get(0).toString());
		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: EVENT, value: EVENTS_RECORDED1}, {tag: EVENT, value: EVENTS_RECORDED2}, {tag: DATE, value: DATE_PERIOD}, {tag: EXTRACT, value: TEXT_FROM_SOURCE}, {tag: NOTE, ref: N1}, {tag: SOURCE, ref: D1}, {tag: NOTE, ref: N1}, {tag: NOTE, ref: N2}, {id: R1, tag: REPOSITORY, children: [{tag: NOTE, ref: N2}]}]", destination.getSources().get(1).toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SOURCE_ORIGINATOR, SOURCE_PUBLICATION_FACTS", destination.getNotes().get(0).toString());
		Assertions.assertEquals("id: N2, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(1).toString());
		Assertions.assertEquals("id: R1, tag: REPOSITORY, children: [{tag: NOTE, ref: N2}]", destination.getRepositories().get(0).toString());
	}

	@Test
	void from(){
		final GedcomNode source = transformerFrom.create("SOURCE")
			.withID("S1")
			.addChildValue("TYPE", "DIGITAL_ARCHIVE")
			.addChildValue("TITLE", "SOURCE_DESCRIPTIVE_TITLE")
			.addChildValue("EVENT", "EVENTS_RECORDED1")
			.addChildValue("EVENT", "EVENTS_RECORDED2")
			.addChild(transformerFrom.create("DATE")
				.withValue("ENTRY_RECORDING_DATE")
				.addChildValue("CALENDAR", "CALENDAR_TYPE")
			)
			.addChildValue("LOCALE", "en-US")
			.addChildValue("EXTRACT", "TEXT_FROM_SOURCE")
			.addChildReference("NOTE", "N1")
			.addChild(transformerFrom.create("REPOSITORY")
				.withXRef("R1")
				.addChildValue("REPOSITORY_LOCATION", "REPOSITORY_LOCATION_TEXT")
				.addChildReference("NOTE", "N2")
			)
			.addChild(transformerFrom.create("FILE")
				.withValue("DOCUMENT_FILE_REFERENCE")
				.addChildValue("FORMAT", "DOCUMENT_FORMAT")
				.addChildValue("MEDIA", "SOURCE_MEDIA_TYPE")
				.addChildValue("CUT", "CUT_COORDINATES")
				.addChildValue("PREFERRED", "Y")
				.addChild(transformerFrom.create("ASSOCIATION")
					.withValue("DOCUMENT_FILE_REFERENCE")
					.addChildValue("RELATIONSHIP", "RELATION_IS_DESCRIPTOR")
				)
			)
			.addChildValue("URL", "ADDRESS_WEB_PAGE")
			.addChildValue("RESTRICTION", "RESTRICTION_NOTICE");
		final Flef origin = new Flef();
		origin.addSource(source);
		final Gedcom destination = new Gedcom();

		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TYPE, value: DIGITAL_ARCHIVE}, {tag: TITLE, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: EVENT, value: EVENTS_RECORDED1}, {tag: EVENT, value: EVENTS_RECORDED2}, {tag: DATE, value: ENTRY_RECORDING_DATE, children: [{tag: CALENDAR, value: CALENDAR_TYPE}]}, {tag: LOCALE, value: en-US}, {tag: EXTRACT, value: TEXT_FROM_SOURCE}, {tag: NOTE, ref: N1}, {tag: REPOSITORY, ref: R1, children: [{tag: REPOSITORY_LOCATION, value: REPOSITORY_LOCATION_TEXT}, {tag: NOTE, ref: N2}]}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE, children: [{tag: FORMAT, value: DOCUMENT_FORMAT}, {tag: MEDIA, value: SOURCE_MEDIA_TYPE}, {tag: CUT, value: CUT_COORDINATES}, {tag: PREFERRED, value: Y}, {tag: ASSOCIATION, value: DOCUMENT_FILE_REFERENCE, children: [{tag: RELATIONSHIP, value: RELATION_IS_DESCRIPTOR}]}]}, {tag: URL, value: ADDRESS_WEB_PAGE}, {tag: RESTRICTION, value: RESTRICTION_NOTICE}]", origin.getSources().get(0).toString());

		final Transformation<Gedcom, Flef> t = new SourceTransformation();
		t.from(origin, destination);

		Assertions.assertEquals("id: S1, tag: SOUR, children: [{tag: DATA, children: [{tag: EVEN, value: EVENTS_RECORDED1, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}]}, {tag: EVEN, value: EVENTS_RECORDED2, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}]}]}, {tag: TITL, value: SOURCE_DESCRIPTIVE_TITLE}, {tag: TEXT, value: TEXT_FROM_SOURCE}, {tag: REPO, ref: R1}, {tag: NOTE, ref: N1}, {tag: OBJE, children: [{tag: FORM, value: DOCUMENT_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE}, {tag: CUT, value: Y}, {tag: _CUTD, value: CUT_COORDINATES}, {tag: _PREF, value: Y}]}]", destination.getSources().get(0).toString());
	}

}