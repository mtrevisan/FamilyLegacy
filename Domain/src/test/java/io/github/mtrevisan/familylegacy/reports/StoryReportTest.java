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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/** The stories (the notes of kind story) in a report: chosen apart from the notes, written as a text, placed as the option says. */
class StoryReportTest{

	private static final String STORY_TITLE = "Life in Treviso";
	private static final String STORY_FIRST = "He was born in a house by the river.";
	private static final String STORY_SECOND = "Then the family moved to the city.";
	private static final String NOTE_TEXT = "A remark about the birth register.";


	@Test
	void theStoriesAreInASectionOfTheirOwnByDefault(){
		final List<String> lines = lines(report(model(), config(StoryPlacement.SECTION)));

		final int section = lines.indexOf("#2 Stories");
		Assertions.assertTrue(section >= 0, lines.toString());
		Assertions.assertEquals("#2 " + STORY_TITLE, lines.get(section + 1), lines.toString());
		Assertions.assertEquals(STORY_FIRST, lines.get(section + 2), lines.toString());
		Assertions.assertEquals(STORY_SECOND, lines.get(section + 3), lines.toString());
		//the notes have their own section, before, and the story is not in it
		final int notes = lines.indexOf("#2 Notes");
		Assertions.assertTrue(notes >= 0 && notes < section, lines.toString());
		Assertions.assertTrue(lines.subList(notes, section).stream().noneMatch(line -> line.contains(STORY_FIRST)), lines.toString());
		Assertions.assertTrue(lines.subList(notes, section).stream().anyMatch(line -> line.contains(NOTE_TEXT)), lines.toString());
	}

	@Test
	void aStoryIsNotWrittenAsANote(){
		final List<String> lines = lines(report(model(), config(StoryPlacement.SECTION)));

		//(no label, no media type: the text alone, under the title)
		Assertions.assertTrue(lines.contains(STORY_FIRST), lines.toString());
		Assertions.assertTrue(lines.contains(STORY_SECOND), lines.toString());
		Assertions.assertTrue(lines.stream().noneMatch(line -> line.contains(STORY_TITLE) && !line.equals("#2 " + STORY_TITLE)), lines.toString());
		Assertions.assertTrue(lines.stream().noneMatch(line -> line.contains("text/markdown")), lines.toString());
	}

	@Test
	void inlineTheStoriesFollowTheNarrativeAndThereIsNoSection(){
		final List<String> lines = lines(report(model(), config(StoryPlacement.INLINE)));

		Assertions.assertFalse(lines.contains("#2 Stories"), lines.toString());
		final int story = lines.indexOf("#2 " + STORY_TITLE);
		final int notes = lines.indexOf("#2 Notes");
		Assertions.assertTrue(story >= 0, lines.toString());
		Assertions.assertTrue(notes < 0 || story < notes, lines.toString());
	}

	@Test
	void theStoriesCanBeLeftOutWhileTheNotesStay(){
		final String text = String.join("\n", lines(report(model(), config(StoryPlacement.EXCLUDE))));

		Assertions.assertFalse(text.contains(STORY_TITLE), text);
		Assertions.assertFalse(text.contains(STORY_FIRST), text);
		Assertions.assertTrue(text.contains(NOTE_TEXT), text);
	}

	@Test
	void theStoriesDoNotDependOnTheNotes(){
		final String text = String.join("\n", lines(report(model(), config(StoryPlacement.SECTION).with(ReportOption.NOTES, false))));

		Assertions.assertTrue(text.contains(STORY_FIRST), text);
		Assertions.assertFalse(text.contains(NOTE_TEXT), text);
	}

	@Test
	void aStoryOfAnEventIsKeptForTheSectionOfTheStories(){
		final FLEFModel model = model();
		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(story("The night of the flood", "The river rose that night.")));
		model.addRecord(participation("EP1", "E1", "I1"));

		final List<String> lines = lines(report(model, config(StoryPlacement.SECTION)));

		final int section = lines.indexOf("#2 Stories");
		Assertions.assertTrue(lines.indexOf("#3 The night of the flood") > section, lines.toString());
		Assertions.assertTrue(lines.contains("The river rose that night."), lines.toString());
	}

	@Test
	void aStoryThePolicyHidesIsNotTold(){
		final FLEFModel model = model();
		model.getRecordById("I1")
			.addChild(story("A secret", "Nobody should read this.")
				.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PRIVACY)
					.addChild(FLEFRecord.createChildWithTagAndValue(PrivacyReader.TAG_LEVEL, PrivacyReader.ENUM_LEVEL_CONFIDENTIAL))));

		final ReportConfig hiding = config(StoryPlacement.SECTION).withPrivacyPolicy(PrivacyPolicy.HIDE_CONFIDENTIAL);
		final String hidden = String.join("\n", lines(report(model, hiding)));

		Assertions.assertFalse(hidden.contains("Nobody should read this."), hidden);
		Assertions.assertFalse(hidden.contains("A secret"), hidden);
		Assertions.assertTrue(hidden.contains(STORY_FIRST), hidden);
	}


	@Test
	void theStoriesAreInTheOrderTheyHaveInTheRecord(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(story("Zebra years", "Told first."))
			.addChild(story("Apple years", "Told second.")));

		final List<String> lines = lines(report(model, config(StoryPlacement.SECTION)));

		//(not by title, not by date: as the user arranged them)
		Assertions.assertTrue(lines.indexOf("#2 Zebra years") < lines.indexOf("#2 Apple years"), lines.toString());
		Assertions.assertTrue(lines.indexOf("#2 Zebra years") >= 0, lines.toString());
	}

	@Test
	void aStoryInMarkdownHasItsHeadingsAndItsLists(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(story("The emigration", "He left in spring.\n\n## The crossing\n\nThe ship took:\n\n- three weeks\n- one storm\n\n---\n\nThen America.")));

		final List<String> lines = lines(report(model, config(StoryPlacement.SECTION)));

		final int title = lines.indexOf("#2 The emigration");
		Assertions.assertTrue(title >= 0, lines.toString());
		//(a heading of the text is under the title of the story)
		Assertions.assertEquals("#4 The crossing", lines.get(title + 2), lines.toString());
		Assertions.assertEquals("The ship took:", lines.get(title + 3), lines.toString());
		Assertions.assertEquals("[three weeks, one storm]", lines.get(title + 4), lines.toString());
		Assertions.assertEquals("---", lines.get(title + 5), lines.toString());
		Assertions.assertEquals("Then America.", lines.get(title + 6), lines.toString());
	}

	@Test
	void everyKindOfReportTellsTheStoriesOfItsSubject(){
		final FLEFModel model = model();
		model.addRecord(FLEFRecord.createMainRecord("G1", io.github.mtrevisan.familylegacy.io.model.readers.GroupReader.TYPE)
			.addChild(story("The family tradition", "Every year they met at the farm.")));
		model.addRecord(FLEFRecord.createMainRecord("P1", io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader.TYPE)
			.addChild(story("The village", "A village by the river.")));
		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(story("The birth", "It was a cold night.")));
		model.addRecord(FLEFRecord.createMainRecord("S1", io.github.mtrevisan.familylegacy.io.model.readers.SourceReader.TYPE)
			.addChild(story("The register", "A register of the parish.")));

		final List<String> missing = new ArrayList<>();
		for(final StoryPlacement placement : List.of(StoryPlacement.SECTION, StoryPlacement.INLINE))
			for(final Object[] each : new Object[][]{
				{ReportType.INDIVIDUAL, "I1", STORY_FIRST},
				{ReportType.ANCESTOR, "I1", STORY_FIRST},
				{ReportType.DESCENDANT, "I1", STORY_FIRST},
				{ReportType.FAMILY_BOOK, "I1", STORY_FIRST},
				{ReportType.GROUP, "G1", "Every year they met at the farm."},
				{ReportType.PLACE, "P1", "A village by the river."},
				{ReportType.INDIVIDUAL, "E1", "It was a cold night."},
				{ReportType.SOURCE, "S1", "A register of the parish."}}){
				final ReportType type = (ReportType)each[0];
				final String text = String.join("\n", lines(ReportBuilder.build(type, model, model.getRecordById((String)each[1]),
					config(placement), ReportProgressListener.NOOP)));
				if(!text.contains((String)each[2]))
					missing.add(placement + " " + type + " of " + each[1]);
			}

		Assertions.assertEquals(List.of(), missing);
	}

	@Test
	void theStoriesOfAPersonInABookAreUnderItsChapter(){
		final List<String> lines = lines(ReportBuilder.build(ReportType.FAMILY_BOOK, model(), model().getRecordById("I1"),
			config(StoryPlacement.SECTION), ReportProgressListener.NOOP));

		final int stories = lines.indexOf("#3 Stories");
		Assertions.assertTrue(stories >= 0, lines.toString());
		Assertions.assertEquals("#3 " + STORY_TITLE, lines.get(stories + 1), lines.toString());
	}


	private static ReportConfig config(final StoryPlacement placement){
		return ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.SHOW_ALL)
			.withLivingPrivacy(LivingPrivacy.SHOW)
			.with(ReportOption.NOTES, true)
			.with(ReportOption.STORIES, placement);
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NOTE)
				.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, NOTE_TEXT)))
			.addChild(story(STORY_TITLE, STORY_FIRST + "\n\n" + STORY_SECOND)));
		return model;
	}

	private static FLEFRecord story(final String title, final String text){
		return FLEFRecord.createChildWithTag(IndividualReader.TAG_NOTE)
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TITLE, title))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, NoteReader.ENUM_KIND_STORY))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_MIME, "text/markdown"));
	}

	private static FLEFRecord participation(final String id, final String eventId, final String individualId){
		return FLEFRecord.createMainRecord(id, io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(
					io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader.TAG_EVENT,
				eventId))
			.addChild(FLEFRecord.createChildWithTagAndValue(io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader.TAG_ROLE,
				io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader.ENUM_ROLE_PRINCIPAL));
	}

	private static io.github.mtrevisan.reports.ReportDocument report(final FLEFModel model, final ReportConfig config){
		return ReportBuilder.build(ReportType.INDIVIDUAL, model, model.getRecordById("I1"), config, ReportProgressListener.NOOP);
	}

	/** @return	The headings ({@code #level text}) and the paragraphs of the report, in order. */
	private static List<String> lines(final io.github.mtrevisan.reports.ReportDocument document){
		final List<String> lines = new ArrayList<>();
		for(final ReportSection section : document.sections())
			if(section instanceof final ReportSection.Heading heading)
				lines.add("#" + heading.level() + " " + heading.text());
			else if(section instanceof final ReportSection.Paragraph paragraph)
				lines.add(paragraph.text());
			else if(section instanceof final ReportSection.BulletList list)
				lines.add(list.items().toString());
			else if(section instanceof ReportSection.HorizontalRule)
				lines.add("---");
		return lines;
	}

}
