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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The stories that link to a record are found from the record, and shown in its dossier, with the editor of the record they are in. */
class StoryMentionsTest{

	private static FLEFRecord note(final String title, final String text, final String kind){
		final FLEFRecord note = FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE);
		if(title != null)
			note.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TITLE, title));
		if(kind != null)
			note.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, kind));
		return note.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_MIME, "text/markdown"));
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I2", IndividualReader.TYPE)
			.addChild(note("The emigration", "He left with his brother [Anna](flef:I1) and the family [Rossi](flef:G1).", NoteReader.ENUM_KIND_STORY))
			//(an ordinary note that links is not a story)
			.addChild(note(null, "See [Anna](flef:I1).", null)));
		model.addRecord(FLEFRecord.createMainRecord("I3", IndividualReader.TYPE)
			//(a story of the record itself, a confidential mark and a link written twice)
			.addChild(note("My life", "I am [me](flef:I3), [hidden](flef:confidential), [Anna](flef:I1) and [Anna again](flef:I1).", NoteReader.ENUM_KIND_STORY)));
		model.addRecord(FLEFRecord.createMainRecord("G1", GroupReader.TYPE));
		return model;
	}

	private static List<String> ids(final List<StoryMentions.Mention> mentions){
		return mentions.stream()
			.map(mention -> mention.owner().getId())
			.toList();
	}


	@Test
	void theStoriesThatLinkToARecordAreFoundFromIt(){
		final FLEFModel model = model();

		Assertions.assertEquals(List.of("I2", "I3"), ids(StoryMentions.of(model, "I1")), "once for each story, whatever the links are");
		Assertions.assertEquals(List.of("I2"), ids(StoryMentions.of(model, "G1")));
		Assertions.assertEquals(List.of(), ids(StoryMentions.of(model, "I2")));
	}

	@Test
	void aStoryOfTheRecordItselfAndTheConfidentialMarkAreNotMentions(){
		final FLEFModel model = model();

		Assertions.assertEquals(List.of(), ids(StoryMentions.of(model, "I3")), "the story of I3 that links to I3");
		Assertions.assertEquals(List.of(), ids(StoryMentions.of(model, "confidential")));
	}

	@Test
	void theIndexFollowsTheChangesOfTheModel(){
		final FLEFModel model = model();
		Assertions.assertEquals(List.of(), ids(StoryMentions.of(model, "I9")));

		model.addRecord(FLEFRecord.createMainRecord("I9", IndividualReader.TYPE));
		model.addRecord(FLEFRecord.createMainRecord("I8", IndividualReader.TYPE)
			.addChild(note("Late", "About [him](flef:I9).", NoteReader.ENUM_KIND_STORY)));

		Assertions.assertEquals(List.of("I8"), ids(StoryMentions.of(model, "I9")));
	}

	@Test
	void theDossierOfAnIndividualTellsTheStoriesThatNameIt(){
		final IndividualDossier dossier = new IndividualDossierService(model()).build("I1");

		final List<DossierEntry> mentions = dossier.entriesOf(DossierSectionType.MENTIONS);
		Assertions.assertEquals(2, mentions.size());
		final DossierEntry first = mentions.getFirst();
		Assertions.assertEquals("The emigration", first.value());
		Assertions.assertTrue(first.label().startsWith("Story of "), first.label());
		Assertions.assertTrue(first.label().contains("I2"), first.label());
		Assertions.assertTrue(first.subtitle().contains("He left with his brother"), first.subtitle());
		//(the row opens the editor of the record the story is in, on its stories, with the story selected)
		Assertions.assertEquals("I2", first.editRecord().getId());
		Assertions.assertEquals(EditorTab.STORIES, first.tab());
		Assertions.assertEquals("The emigration", first.focus() == null? null: io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper
			.getChildValue(first.focus(), NoteReader.TAG_TITLE));
		Assertions.assertTrue(DossierSectionType.STORIES.ordinal() < DossierSectionType.MENTIONS.ordinal()
			&& DossierSectionType.MENTIONS.ordinal() < DossierSectionType.NOTES.ordinal());
	}

	@Test
	void theDossierOfAGroupTellsThemToo(){
		final GroupDossier dossier = new GroupDossierService(model()).build("G1");

		final List<DossierEntry> mentions = dossier.entriesOf(GroupDossierSectionType.MENTIONS);
		Assertions.assertEquals(1, mentions.size());
		Assertions.assertEquals("I2", mentions.getFirst().editRecord().getId());
	}

	@Test
	void aStoryOfAnEventOpensTheStoriesOfTheEventAndOneOfAnAttributeItsNotes(){
		final FLEFModel model = model();
		model.addRecord(FLEFRecord.createMainRecord("E1", io.github.mtrevisan.familylegacy.io.model.readers.EventReader.TYPE)
			.addChild(note("The wedding day", "It was [Anna](flef:I1)'s wedding.", NoteReader.ENUM_KIND_STORY)));

		final List<DossierEntry> mentions = new IndividualDossierService(model).build("I1").entriesOf(DossierSectionType.MENTIONS);

		final DossierEntry wedding = mentions.stream()
			.filter(entry -> "The wedding day".equals(entry.value()))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("E1", wedding.editRecord().getId());
		Assertions.assertEquals(EditorTab.STORIES, wedding.tab());
	}

}
