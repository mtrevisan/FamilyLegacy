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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;


/** The kind of a note: what a note without one is, the ones that tell the narrative notes from the working ones, and that the protocol has it. */
class NoteKindTest{

	@Test
	void aNoteWithoutAKindIsGeneral(){
		final FLEFRecord note = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE);

		Assertions.assertEquals(NoteReader.ENUM_KIND_GENERAL, NoteReader.extractKind(note));
		Assertions.assertFalse(NoteReader.isStory(note));
		Assertions.assertFalse(NoteReader.isResearch(note));
	}

	@Test
	void theKindTellsAStoryFromAResearchNote(){
		final FLEFRecord story = withKind("story");
		final FLEFRecord research = withKind("research");

		Assertions.assertTrue(NoteReader.isStory(story));
		Assertions.assertFalse(NoteReader.isResearch(story));
		Assertions.assertTrue(NoteReader.isResearch(research));
		Assertions.assertFalse(NoteReader.isStory(research));
	}

	@Test
	void theKindIsReadWithoutRegardToCaseAndSpaces(){
		Assertions.assertTrue(NoteReader.isStory(withKind("  Story ")));
	}

	@Test
	void aCustomKindIsKeptAndIsNeitherStoryNorResearch(){
		final FLEFRecord note = withKind("oral_history");

		Assertions.assertEquals("oral_history", NoteReader.extractKind(note));
		Assertions.assertFalse(NoteReader.isStory(note));
		Assertions.assertFalse(NoteReader.isResearch(note));
	}

	@Test
	void theProtocolDeclaresTheKindOfANote() throws IOException{
		try(final InputStream stream = getClass().getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			final FLEFGrammar grammar = FLEFGrammarParser.parse(stream);

			Assertions.assertNotNull(grammar.getType("NoteStructure"));
		}
		try(final InputStream stream = getClass().getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			final String grammar = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

			Assertions.assertTrue(grammar.contains("kind?: enum {"), "the NoteStructure has no kind");
			for(final String kind : NoteReader.KINDS)
				Assertions.assertTrue(grammar.contains(kind), kind);
		}
	}


	private static FLEFRecord withKind(final String kind){
		return FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, kind));
	}

}
