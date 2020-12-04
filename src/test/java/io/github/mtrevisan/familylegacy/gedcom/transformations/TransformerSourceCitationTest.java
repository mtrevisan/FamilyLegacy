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


class TransformerSourceCitationTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void sourceCitationToXRefMultimediaXRef(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithReference("SOUR", "@S1@")
				.addChildValue("PAGE", "WHERE_WITHIN_SOURCE")
				.addChild(transformerTo.createWithValue("EVEN", "EVENT_TYPE_CITED_FROM")
					.addChildValue("ROLE", "ROLE_IN_EVENT")
				)
				.addChild(transformerTo.create("DATA")
					.addChildValue("DATE", "ENTRY_RECORDING_DATE")
					.addChildValue("TEXT", "TEXT_FROM_SOURCE")
				)
				.addChild(transformerTo.createWithReference("OBJE", "@M1@"))
				.addChildReference("NOTE", "N1")
				.addChildReference("QUAY", "CERTAINTY_ASSESSMENT")
			);
		final GedcomNode source = transformerTo.createWithID("SOUR", "@S1@");
		final GedcomNode multimedia = transformerTo.createWithID("OBJE", "@M1@")
			.addChildValue("TITLE", "DOCUMENT_TITLE")
			.addChildValue("FILE", "DOCUMENT_FILE_REFERENCE")
			.addChildValue("MEDIA_TYPE", "SOURCE_MEDIA_TYPE");

		Assertions.assertEquals("children: [{tag: SOUR, ref: @S1@, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}]}, {tag: OBJE, ref: @M1@}, {tag: NOTE, ref: N1}, {tag: QUAY, ref: CERTAINTY_ASSESSMENT}]}]", parent.toString());
		Assertions.assertEquals("id: @S1@, tag: SOUR", source.toString());
		Assertions.assertEquals("id: @M1@, tag: OBJE, children: [{tag: TITLE, value: DOCUMENT_TITLE}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE}, {tag: MEDIA_TYPE, value: SOURCE_MEDIA_TYPE}]", multimedia.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		origin.addSource(source);
		origin.addObject(multimedia);
		final Flef destination = new Flef();
		transformerTo.sourceCitationTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: SOURCE, ref: @S1@}]", destinationNode.toString());
		Assertions.assertEquals("id: @S1@, tag: SOURCE, children: [{tag: LOCATION, value: WHERE_WITHIN_SOURCE}, {tag: MEDIA_TYPE, value: DOCUMENT_FILE_REFERENCE}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE, children: [{tag: EXTRACT, value: TEXT_FROM_SOURCE}]}, {tag: NOTE, ref: N1}]", destination.getSources().get(0).toString());
	}

	@Test
	void sourceCitationToXRefMultimediaNoXRef(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithID("SOUR", "@S1@")
				.addChildValue("PAGE", "WHERE_WITHIN_SOURCE")
				.addChild(transformerTo.createWithValue("EVEN", "EVENT_TYPE_CITED_FROM")
					.addChildValue("ROLE", "ROLE_IN_EVENT")
				)
				.addChild(transformerTo.create("DATA")
					.addChildValue("DATE", "ENTRY_RECORDING_DATE")
					.addChildValue("TEXT", "TEXT_FROM_SOURCE")
				)
				.addChild(transformerTo.create("OBJE")
					.addChildValue("TITL", "DESCRIPTIVE_TITLE")
					.addChild(transformerTo.createWithValue("FORM", "MULTIMEDIA_FORMAT")
						.addChild(transformerTo.createWithValue("MEDI", "SOURCE_MEDIA_TYPE"))
					)
					.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
					.addChildValue("_CUTD", "CUT_COORDINATES")
					.addChildValue("_PREF", "PREFERRED_MEDIA")
				)
				.addChildReference("NOTE", "N1")
				.addChildReference("QUAY", "CERTAINTY_ASSESSMENT")
			);
		final GedcomNode source = transformerTo.createWithID("SOUR", "@S1@");

		Assertions.assertEquals("children: [{id: @S1@, tag: SOUR, children: [{tag: PAGE, value: WHERE_WITHIN_SOURCE}, {tag: EVEN, value: EVENT_TYPE_CITED_FROM, children: [{tag: ROLE, value: ROLE_IN_EVENT}]}, {tag: DATA, children: [{tag: DATE, value: ENTRY_RECORDING_DATE}, {tag: TEXT, value: TEXT_FROM_SOURCE}]}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: _CUTD, value: CUT_COORDINATES}, {tag: _PREF, value: PREFERRED_MEDIA}]}, {tag: NOTE, ref: N1}, {tag: QUAY, ref: CERTAINTY_ASSESSMENT}]}]", parent.toString());
		Assertions.assertEquals("id: @S1@, tag: SOUR", source.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		origin.addSource(source);
		final Flef destination = new Flef();
		destination.addSource(source);
		transformerTo.sourceCitationTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: SOURCE, ref: S2, children: [{tag: CUTOUT, value: CUT_COORDINATES}]}]", destinationNode.toString());
		Assertions.assertEquals("id: @S1@, tag: SOUR", destination.getSources().get(0).toString());
		Assertions.assertEquals("id: S2, tag: SOURCE, children: [{tag: MEDIA_TYPE, value: MULTIMEDIA_FILE_REFN}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: DESCRIPTION, value: DESCRIPTIVE_TITLE}]}, {tag: NOTE, ref: N1}]", destination.getSources().get(1).toString());
	}

	@Test
	void sourceCitationToNoXRefMultimediaXRef(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithValue("SOUR", "SOURCE_DESCRIPTION")
				.addChildValue("TEXT", "TEXT_FROM_SOURCE")
				.addChild(transformerTo.createWithReference("OBJE", "@M1@"))
				.addChildReference("NOTE", "N1")
				.addChildReference("QUAY", "CERTAINTY_ASSESSMENT")
			);
		final GedcomNode source = transformerTo.createWithID("SOUR", "@S1@");
		final GedcomNode multimedia = transformerTo.createWithID("OBJE", "@M1@")
			.addChildValue("TITLE", "DOCUMENT_TITLE")
			.addChildValue("FILE", "DOCUMENT_FILE_REFERENCE")
			.addChildValue("MEDIA_TYPE", "SOURCE_MEDIA_TYPE");

		Assertions.assertEquals("children: [{tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: TEXT, value: TEXT_FROM_SOURCE}, {tag: OBJE, ref: @M1@}, {tag: NOTE, ref: N1}, {tag: QUAY, ref: CERTAINTY_ASSESSMENT}]}]", parent.toString());
		Assertions.assertEquals("id: @S1@, tag: SOUR", source.toString());
		Assertions.assertEquals("id: @M1@, tag: OBJE, children: [{tag: TITLE, value: DOCUMENT_TITLE}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE}, {tag: MEDIA_TYPE, value: SOURCE_MEDIA_TYPE}]", multimedia.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		origin.addSource(source);
		origin.addObject(multimedia);
		final Flef destination = new Flef();
		transformerTo.sourceCitationTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: SOURCE, ref: S1}]", destinationNode.toString());
		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: SOURCE_DESCRIPTION}, {tag: MEDIA_TYPE, value: DOCUMENT_FILE_REFERENCE}, {tag: FILE, value: DOCUMENT_FILE_REFERENCE, children: [{tag: EXTRACT, value: TEXT_FROM_SOURCE}]}, {tag: NOTE, ref: N1}]", destination.getSources().get(0).toString());
	}

	@Test
	void sourceCitationToNoXRefMultimediaNoXRef(){
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithValue("SOUR", "SOURCE_DESCRIPTION")
				.addChildValue("TEXT", "TEXT_FROM_SOURCE")
				.addChild(transformerTo.create("OBJE")
					.addChildValue("TITL", "DESCRIPTIVE_TITLE")
					.addChild(transformerTo.createWithValue("FORM", "MULTIMEDIA_FORMAT")
						.addChild(transformerTo.createWithValue("MEDI", "SOURCE_MEDIA_TYPE"))
					)
					.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
					.addChildValue("_CUTD", "CUT_COORDINATES")
					.addChildValue("_PREF", "PREFERRED_MEDIA")
				)
				.addChildReference("NOTE", "N1")
				.addChildReference("QUAY", "CERTAINTY_ASSESSMENT")
			);
		final GedcomNode source = transformerTo.createWithID("SOUR", "@S1@");

		Assertions.assertEquals("children: [{tag: SOUR, value: SOURCE_DESCRIPTION, children: [{tag: TEXT, value: TEXT_FROM_SOURCE}, {tag: OBJE, children: [{tag: TITL, value: DESCRIPTIVE_TITLE}, {tag: FORM, value: MULTIMEDIA_FORMAT, children: [{tag: MEDI, value: SOURCE_MEDIA_TYPE}]}, {tag: FILE, value: MULTIMEDIA_FILE_REFN}, {tag: _CUTD, value: CUT_COORDINATES}, {tag: _PREF, value: PREFERRED_MEDIA}]}, {tag: NOTE, ref: N1}, {tag: QUAY, ref: CERTAINTY_ASSESSMENT}]}]", parent.toString());
		Assertions.assertEquals("id: @S1@, tag: SOUR", source.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Gedcom origin = new Gedcom();
		origin.addSource(source);
		final Flef destination = new Flef();
		transformerTo.sourceCitationTo(parent, destinationNode, origin, destination);

		Assertions.assertEquals("children: [{tag: SOURCE, ref: S1, children: [{tag: CUTOUT, value: CUT_COORDINATES}]}]", destinationNode.toString());
		Assertions.assertEquals("id: S1, tag: SOURCE, children: [{tag: TITLE, value: SOURCE_DESCRIPTION}, {tag: MEDIA_TYPE, value: MULTIMEDIA_FILE_REFN}, {tag: FILE, value: MULTIMEDIA_FILE_REFN, children: [{tag: DESCRIPTION, value: DESCRIPTIVE_TITLE}, {tag: EXTRACT, value: TEXT_FROM_SOURCE}]}, {tag: NOTE, ref: N1}]", destination.getSources().get(0).toString());
	}


	@Test
	void sourceCitationFrom(){
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithReference("REPOSITORY", "@R1@")
				.addChildValue("LOCATION", "REPOSITORY_LOCATION_TEXT")
				.addChildReference("NOTE", "@N1@")
			);

		Assertions.assertEquals("children: [{tag: REPOSITORY, ref: @R1@, children: [{tag: LOCATION, value: REPOSITORY_LOCATION_TEXT}, {tag: NOTE, ref: @N1@}]}]", parent.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		transformerFrom.sourceCitationFrom(parent, destinationNode);

		Assertions.assertEquals("children: [{tag: REPO, ref: @R1@, children: [{tag: CALN, value: REPOSITORY_LOCATION_TEXT}, {tag: NOTE, ref: @N1@}]}]", destinationNode.toString());
	}

}