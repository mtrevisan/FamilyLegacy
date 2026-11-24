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
package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The dossier of an individual has the stories (the notes of kind story) in a section of their own, apart from the notes. */
class DossierStoriesTest{

	private static FLEFRecord note(final String title, final String kind){
		final FLEFRecord note = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TITLE, title))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, "Some text."));
		if(kind != null)
			note.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, kind));
		return note;
	}

	private static IndividualDossier dossier(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(note("A remark", null))
			.addChild(note("Life in Treviso", NoteReader.ENUM_KIND_STORY))
			.addChild(note("To check", NoteReader.ENUM_KIND_RESEARCH)));
		model.addRecord(FLEFRecord.createMainRecord("IA1", IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, "I1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, "occupation"))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, "baker"))
			.addChild(note("The bakery", NoteReader.ENUM_KIND_STORY)));
		return new IndividualDossierService(model).build("I1");
	}


	@Test
	void theStoriesAreApartFromTheNotes(){
		final IndividualDossier dossier = dossier();

		final List<String> stories = dossier.entriesOf(DossierSectionType.STORIES).stream()
			.map(DossierEntry::value)
			.toList();
		final List<String> notes = dossier.entriesOf(DossierSectionType.NOTES).stream()
			.map(DossierEntry::value)
			.toList();

		Assertions.assertEquals(List.of("Life in Treviso", "The bakery"), stories);
		Assertions.assertEquals(List.of("A remark", "To check"), notes);
	}

	@Test
	void aStoryOfTheIndividualOpensTheTabOfTheStories(){
		final DossierEntry story = dossier().entriesOf(DossierSectionType.STORIES).getFirst();

		Assertions.assertEquals(EditorTab.STORIES, story.tab());
		Assertions.assertTrue(story.label().endsWith("story"), story.label());
	}

	@Test
	void aStoryOfSomethingElseOpensTheNotesTabOfThatRecord(){
		final DossierEntry story = dossier().entriesOf(DossierSectionType.STORIES).get(1);

		Assertions.assertEquals(EditorTab.NOTES, story.tab());
		Assertions.assertTrue(story.subtitle().contains("occupation: baker"), story.subtitle());
	}

	@Test
	void theStoriesComeBeforeTheNotes(){
		Assertions.assertTrue(DossierSectionType.STORIES.ordinal() < DossierSectionType.NOTES.ordinal());
		Assertions.assertTrue(GroupDossierSectionType.STORIES.ordinal() < GroupDossierSectionType.NOTES.ordinal());
	}

	@Test
	void theDossierOfAGroupHasTheStoriesApartToo(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("G1", GroupReader.TYPE)
			.addChild(note("A remark", null))
			.addChild(note("The family farm", NoteReader.ENUM_KIND_STORY)));

		final GroupDossier dossier = new GroupDossierService(model).build("G1");

		final List<DossierEntry> stories = dossier.entriesOf(GroupDossierSectionType.STORIES);
		Assertions.assertEquals(List.of("The family farm"), stories.stream().map(DossierEntry::value).toList());
		Assertions.assertEquals(EditorTab.STORIES, stories.getFirst().tab());
		Assertions.assertEquals(List.of("A remark"), dossier.entriesOf(GroupDossierSectionType.NOTES).stream().map(DossierEntry::value).toList());
	}

}
