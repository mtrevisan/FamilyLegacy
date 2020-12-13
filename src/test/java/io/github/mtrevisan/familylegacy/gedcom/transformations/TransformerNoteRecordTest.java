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


class TransformerNoteRecordTest{

	@Test
	void noteRecordTo(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode note = transformerTo.createWithIDValue("NOTE", "@N1@", "SUBMITTER_TEXT");

		Assertions.assertEquals("id: @N1@, tag: NOTE, value: SUBMITTER_TEXT", note.toString());

		final Flef destination = new Flef();
		destination.addNote(note);
		transformerTo.noteRecordTo(note, destination);

		Assertions.assertEquals("id: @N1@, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

	@Test
	void noteCitationToXRef(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithReference("NOTE", "@N1@"));
		final GedcomNode note = transformerTo.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("children: [{tag: NOTE, ref: @N1@}]", parent.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Flef destination = new Flef();
		destination.addNote(note);
		transformerTo.noteCitationTo(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{tag: NOTE, ref: @N1@}]", destinationNode.toString());
	}

	@Test
	void noteCitationToNoXRef(){
		final Transformer transformerTo = new Transformer(Protocol.FLEF);
		final GedcomNode parent = transformerTo.createEmpty()
			.addChild(transformerTo.createWithValue("NOTE", "SUBMITTER_TEXT"));

		Assertions.assertEquals("children: [{tag: NOTE, value: SUBMITTER_TEXT}]", parent.toString());

		final GedcomNode destinationNode = transformerTo.createEmpty();
		final Flef destination = new Flef();
		transformerTo.noteCitationTo(parent, destinationNode, destination);

		Assertions.assertEquals("children: [{tag: NOTE, ref: N1}]", destinationNode.toString());
		Assertions.assertEquals("id: N1, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

	@Test
	void noteRecordFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode note = transformerFrom.createWithIDValue("NOTE", "@N1@", "SUBMITTER_TEXT");

		Assertions.assertEquals("id: @N1@, tag: NOTE, value: SUBMITTER_TEXT", note.toString());

		final Gedcom destination = new Gedcom();
		transformerFrom.noteRecordFrom(note, destination);

		Assertions.assertEquals("id: @N1@, tag: NOTE, value: SUBMITTER_TEXT", destination.getNotes().get(0).toString());
	}

	@Test
	void noteCitationFrom(){
		final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);
		final GedcomNode parent = transformerFrom.createEmpty()
			.addChild(transformerFrom.createWithReference("NOTE", "@N1@"));
		final GedcomNode note = transformerFrom.createWithID("NOTE", "@N1@");

		Assertions.assertEquals("children: [{tag: NOTE, ref: @N1@}]", parent.toString());
		Assertions.assertEquals("id: @N1@, tag: NOTE", note.toString());

		final GedcomNode destinationNode = transformerFrom.createEmpty();
		final Gedcom destination = new Gedcom();
		destination.addNote(note);
		transformerFrom.noteCitationFrom(parent, destinationNode);

		Assertions.assertEquals("children: [{tag: NOTE, ref: @N1@}]", destinationNode.toString());
	}

}