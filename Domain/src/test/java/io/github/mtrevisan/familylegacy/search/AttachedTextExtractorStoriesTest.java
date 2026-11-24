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
package io.github.mtrevisan.familylegacy.search;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The text of the stories is kept apart from the text of the other notes, wherever the note is written. */
class AttachedTextExtractorStoriesTest{

	private static FLEFRecord note(final String text, final String kind){
		final FLEFRecord note = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text));
		if(kind != null)
			note.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, kind));
		return note;
	}

	private static FLEFRecord individual(){
		return FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(note("plain remark", null))
			.addChild(note("research reasoning", NoteReader.ENUM_KIND_RESEARCH))
			.addChild(note("a life story", NoteReader.ENUM_KIND_STORY))
			//(a note written deeper in the record)
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(note("a story of a place", NoteReader.ENUM_KIND_STORY))
				.addChild(note("a remark of a place", null)));
	}


	@Test
	void theNotesDoNotHaveTheStories(){
		final String notes = AttachedTextExtractor.extract(individual(), new FLEFModel());

		Assertions.assertTrue(notes.contains("plain remark"), notes);
		Assertions.assertTrue(notes.contains("research reasoning"), notes);
		Assertions.assertTrue(notes.contains("a remark of a place"), notes);
		Assertions.assertFalse(notes.contains("story"), notes);
	}

	@Test
	void theStoriesDoNotHaveTheNotes(){
		final String stories = AttachedTextExtractor.extractStories(individual());

		Assertions.assertEquals("a life story\na story of a place", stories);
	}

	@Test
	void aRecordThatIsItselfAStoryHasItsTextAmongTheStories(){
		final FLEFRecord story = FLEFRecord.createMainRecord("N1", NoteReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, "narrative"))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, NoteReader.ENUM_KIND_STORY));

		Assertions.assertEquals("narrative", AttachedTextExtractor.extractStories(story));
		Assertions.assertEquals("", AttachedTextExtractor.extract(story, new FLEFModel()));
		Assertions.assertEquals("", AttachedTextExtractor.extractStories(null));
	}

}
