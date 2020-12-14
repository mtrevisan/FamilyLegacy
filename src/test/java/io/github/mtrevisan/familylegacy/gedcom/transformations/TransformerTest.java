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

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class TransformerTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void valueNoContinuationFlef(){
		final GedcomNode node = transformerTo.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertFalse(node.hasChildren());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT", node.getRawValue());
	}

	@Test
	void valueContinuationFlef(){
		final GedcomNode node = transformerTo.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY\nVERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertEquals(1, node.getChildren().size());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY", node.getRawValue());
		Assertions.assertEquals("CONTINUATION", node.getChildren().get(0).getTag());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT", node.getChildren().get(0).getRawValue());
	}

	@Test
	void value(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("SHORT TEXT");

		Assertions.assertFalse(node.hasChildren());
		Assertions.assertEquals("SHORT TEXT", node.getRawValue());
	}

	@Test
	void valueContinuation(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("SHORT\nTEXT");

		Assertions.assertEquals(1, node.getChildren().size());
		Assertions.assertEquals("SHORT", node.getRawValue());
		Assertions.assertEquals("CONT", node.getChildren().get(0).getTag());
		Assertions.assertEquals("TEXT", node.getChildren().get(0).getRawValue());
	}

	@Test
	void valueConcatenationWithoutSpace(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertEquals(1, node.getChildren().size());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
		Assertions.assertEquals("RY LONG TEXT", node.getChildren().get(0).getRawValue());
	}

	@Test
	void valueConcatenationWithSpace(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE Y LONG TEXT");

		Assertions.assertEquals(1, node.getChildren().size());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
	}


//	@Test
//	void sourceCitationToXRefMultimediaLinkXRefNoteXRef(){
//		final GedcomNode parent = transformerFrom.createEmpty()
//			.addChild(transformerFrom.createWithReference("SOUR", "@S1@")
//				.addChildValue("PAGE", "WHERE_WITHIN_SOURCE")
//				.addChild(transformerFrom.create("EVEN")
//					.withValue("EVENT_TYPE_CITED_FROM")
//					.addChildValue("ROLE", "ROLE_IN_EVENT")
//				)
//				.addChild(transformerFrom.create("DATA")
//					.addChildValue("DATE", "ENTRY_RECORDING_DATE")
//					.addChildValue("TEXT", "TEXT_FROM_SOURCE")
//				)
//				.addChildReference("OBJE", "@O1@")
//				.addChildReference("NOTE", "@N1@")
//				.addChildValue("QUAY", "CERTAINTY_ASSESSMENT")
//			);
//		final GedcomNode source = transformerFrom.createWithID("SOUR", "@S1@");
//		final GedcomNode note = transformerFrom.createWithID("NOTE", "@N1@");
//
//		final Transformer t = new Transformer(Protocol.FLEF);
//		final GedcomNode destinationNode = transformerTo.createEmpty();
//		final Flef destination = new Flef();
//		destination.addSource(source);
//		destination.addNote(note);
//		t.sourceCitationTo(parent, destinationNode, destination);
//
//		Assertions.assertEquals("children: [{tag: SOURCE, ref: @S1@, children: [{tag: LOCATION, value: WHERE_WITHIN_SOURCE}, {tag: ROLE, value: ROLE_IN_EVENT}, {tag: CREDIBILITY, value: CERTAINTY_ASSESSMENT}]}]", destinationNode.toString());
//	}
//
//	@Test
//	void sourceCitationToXRefMultimediaLinkXRefNoteNoXRef(){
//		final GedcomNode node = transformerFrom.createWithReference("SOUR", "@S1@")
//			.addChildValue("PAGE", "WHERE_WITHIN_SOURCE")
//			.addChild(transformerFrom.create("EVEN")
//				.withValue("EVENT_TYPE_CITED_FROM")
//				.addChildValue("ROLE", "ROLE_IN_EVENT")
//			)
//			.addChild(transformerFrom.create("DATA")
//				.addChildValue("DATE", "ENTRY_RECORDING_DATE")
//				.addChildValue("TEXT", "TEXT_FROM_SOURCE")
//			)
//			.addChildReference("OBJE", "@O1@")
//			.addChildValue("NOTE", "SUBMITTER_TEXT")
//			.addChildValue("QUAY", "CERTAINTY_ASSESSMENT");
//
//		Assertions.assertEquals(1, node.getChildren().size());
//		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
//		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
//		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
//	}
//
//	@Test
//	void sourceCitationToXRefMultimediaLinkNoXRefNoteXRef(){
//		final GedcomNode node = transformerFrom.createWithReference("SOUR", "@S1@")
//			.addChildValue("PAGE", "WHERE_WITHIN_SOURCE")
//			.addChild(transformerFrom.create("EVEN")
//				.withValue("EVENT_TYPE_CITED_FROM")
//				.addChildValue("ROLE", "ROLE_IN_EVENT")
//			)
//			.addChild(transformerFrom.create("DATA")
//				.addChildValue("DATE", "ENTRY_RECORDING_DATE")
//				.addChildValue("TEXT", "TEXT_FROM_SOURCE")
//			)
//			.addChild(transformerFrom.create("OBJE")
//				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
//				.addChild(transformerFrom.create("FORM")
//					.withValue("MULTIMEDIA_FORMAT")
//					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
//				)
//				.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
//				.addChildValue("_CUTD", "CUT_COORDINATES")
//				.addChildValue("_PREF", "PREFERRED_MEDIA")
//			)
//			.addChildReference("NOTE", "@N1@")
//			.addChildValue("QUAY", "CERTAINTY_ASSESSMENT");
//
//		Assertions.assertEquals(1, node.getChildren().size());
//		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
//		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
//		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
//	}
//
//	@Test
//	void sourceCitationToXRefMultimediaLinkNoXRefNoteNpXRef(){
//		final GedcomNode node = transformerFrom.createWithReference("SOUR", "@S1@")
//			.addChildValue("PAGE", "WHERE_WITHIN_SOURCE")
//			.addChild(transformerFrom.create("EVEN")
//				.withValue("EVENT_TYPE_CITED_FROM")
//				.addChildValue("ROLE", "ROLE_IN_EVENT")
//			)
//			.addChild(transformerFrom.create("DATA")
//				.addChildValue("DATE", "ENTRY_RECORDING_DATE")
//				.addChildValue("TEXT", "TEXT_FROM_SOURCE")
//			)
//			.addChild(transformerFrom.create("OBJE")
//				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
//				.addChild(transformerFrom.create("FORM")
//					.withValue("MULTIMEDIA_FORMAT")
//					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
//				)
//				.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
//				.addChildValue("_CUTD", "CUT_COORDINATES")
//				.addChildValue("_PREF", "PREFERRED_MEDIA")
//			)
//			.addChildValue("NOTE", "SUBMITTER_TEXT")
//			.addChildValue("QUAY", "CERTAINTY_ASSESSMENT");
//
//		Assertions.assertEquals(1, node.getChildren().size());
//		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
//		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
//		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
//	}
//
//	@Test
//	void sourceCitationToNoXRefMultimediaLinkXRefNoteXRef(){
//		final GedcomNode node = transformerFrom.create("SOUR")
//			.withValue("SOURCE_DESCRIPTION")
//			.addChildValue("TEXT", "TEXT_FROM_SOURCE1")
//			.addChildReference("OBJE", "@O1@")
//			.addChildReference("NOTE", "@N1@")
//			.addChildValue("QUAY", "CERTAINTY_ASSESSMENT");
//
//		Assertions.assertEquals(1, node.getChildren().size());
//		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
//		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
//		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
//	}
//
//	@Test
//	void sourceCitationToNoXRefMultimediaLinkXRefNoteNoXRef(){
//		final GedcomNode node = transformerFrom.create("SOUR")
//			.withValue("SOURCE_DESCRIPTION")
//			.addChildValue("TEXT", "TEXT_FROM_SOURCE1")
//			.addChildReference("OBJE", "@O1@")
//			.addChildValue("NOTE", "SUBMITTER_TEXT")
//			.addChildValue("QUAY", "CERTAINTY_ASSESSMENT");
//
//		Assertions.assertEquals(1, node.getChildren().size());
//		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
//		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
//		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
//	}
//
//	@Test
//	void sourceCitationToNoXRefMultimediaLinkNoXRefNoteXRef(){
//		final GedcomNode node = transformerFrom.create("SOUR")
//			.withValue("SOURCE_DESCRIPTION")
//			.addChildValue("TEXT", "TEXT_FROM_SOURCE1")
//			.addChild(transformerFrom.create("OBJE")
//				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
//				.addChild(transformerFrom.create("FORM")
//					.withValue("MULTIMEDIA_FORMAT")
//					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
//				)
//				.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
//				.addChildValue("_CUTD", "CUT_COORDINATES")
//				.addChildValue("_PREF", "PREFERRED_MEDIA")
//			)
//			.addChildReference("NOTE", "@N1@")
//			.addChildValue("QUAY", "CERTAINTY_ASSESSMENT");
//
//		Assertions.assertEquals(1, node.getChildren().size());
//		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
//		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
//		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
//	}
//
//	@Test
//	void sourceCitationToNoXRefMultimediaLinkNoXRefNoteNpXRef(){
//		final GedcomNode node = transformerFrom.create("SOUR")
//			.withValue("SOURCE_DESCRIPTION")
//			.addChildValue("TEXT", "TEXT_FROM_SOURCE1")
//			.addChild(transformerFrom.create("OBJE")
//				.addChildValue("TITL", "DESCRIPTIVE_TITLE")
//				.addChild(transformerFrom.create("FORM")
//					.withValue("MULTIMEDIA_FORMAT")
//					.addChildValue("MEDI", "SOURCE_MEDIA_TYPE")
//				)
//				.addChildValue("FILE", "MULTIMEDIA_FILE_REFN")
//				.addChildValue("_CUTD", "CUT_COORDINATES")
//				.addChildValue("_PREF", "PREFERRED_MEDIA")
//			)
//			.addChildValue("NOTE", "SUBMITTER_TEXT")
//			.addChildValue("QUAY", "CERTAINTY_ASSESSMENT");
//
//		Assertions.assertEquals(1, node.getChildren().size());
//		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
//		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
//		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
//	}

}