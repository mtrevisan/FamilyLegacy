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
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.renderers.MarkdownReportRenderer;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/** The stories in a book: each report of the book is generated with its own options, and its stories are in the chapter, one level down. */
class StoryBookTest{

	private static final String TITLE = "Life in Treviso";
	private static final String FIRST = "He was born in a house by the river.";
	private static final String SECOND = "Then the family moved to the city.";


	private static FLEFRecord story(final String title, final String text){
		return FLEFRecord.createChildWithTag(NoteReader.TAG_NOTE)
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TITLE, title))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_KIND, NoteReader.ENUM_KIND_STORY))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_TEXT, text))
			.addChild(FLEFRecord.createChildWithTagAndValue(NoteReader.TAG_MIME, "text/markdown"));
	}

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(story(TITLE, FIRST + "\n\n" + SECOND)));
		model.addRecord(FLEFRecord.createMainRecord("G1", GroupReader.TYPE)
			.addChild(story("The family tradition", "Every year they met at the farm.")));
		return model;
	}

	private static ReportConfig config(final StoryPlacement placement){
		return ReportConfig.defaults()
			.withLivingPrivacy(LivingPrivacy.SHOW)
			.with(ReportOption.STORIES, placement);
	}

	private static BookEntry entry(final ReportType type, final String subject, final StoryPlacement placement){
		return new BookEntry(type, subject, config(placement));
	}

	private static ReportDocument build(final FLEFModel model, final BookEntry... entries){
		return BookBuilder.build(model, new BookDefinition("My Book", "A subtitle", "Prepared for the family", ReportLanguage.ENGLISH, false,
			false, "", "", Set.of(), List.of(entries)), ReportProgressListener.NOOP);
	}

	/** @return	The headings ({@code #level text}) and the paragraphs of the book, in order. */
	private static List<String> lines(final ReportDocument document){
		final List<String> lines = new ArrayList<>();
		for(final ReportSection section : document.sections())
			if(section instanceof final ReportSection.Heading heading)
				lines.add("#" + heading.level() + " " + heading.text());
			else if(section instanceof final ReportSection.Paragraph paragraph)
				lines.add(paragraph.text());
		return lines;
	}


	@Test
	void theStoriesOfAPersonAreInItsChapterOneLevelDown(){
		final List<String> lines = lines(build(model(), entry(ReportType.INDIVIDUAL, "I1", StoryPlacement.SECTION)));

		//(in a report the section of the stories is of the second level and the story of the second too: in the book, the third)
		final int section = lines.indexOf("#3 Stories");
		Assertions.assertTrue(section >= 0, lines.toString());
		Assertions.assertEquals("#3 " + TITLE, lines.get(section + 1), lines.toString());
		Assertions.assertEquals(FIRST, lines.get(section + 2), lines.toString());
		Assertions.assertEquals(SECOND, lines.get(section + 3), lines.toString());
	}

	@Test
	void everyReportOfTheBookHasItsOwnOptionsForTheStories(){
		final List<String> lines = lines(build(model(),
			entry(ReportType.INDIVIDUAL, "I1", StoryPlacement.EXCLUDE),
			entry(ReportType.INDIVIDUAL, "I1", StoryPlacement.INLINE)));

		//the first chapter leaves the story out, the second tells it right after the narrative (so, once in the whole book)
		Assertions.assertEquals(1, lines.stream().filter(line -> line.equals(FIRST)).count(), lines.toString());
		Assertions.assertFalse(lines.contains("#3 Stories"), lines.toString());
		Assertions.assertTrue(lines.contains("#3 " + TITLE), lines.toString());
	}

	@Test
	void theFamilyBookAndTheGroupHaveTheirStoriesToo(){
		final List<String> lines = lines(build(model(),
			entry(ReportType.FAMILY_BOOK, "I1", StoryPlacement.SECTION),
			entry(ReportType.GROUP, "G1", StoryPlacement.SECTION)));

		Assertions.assertTrue(lines.contains(FIRST), lines.toString());
		Assertions.assertTrue(lines.contains("Every year they met at the farm."), lines.toString());
		Assertions.assertTrue(lines.contains("#3 The family tradition") || lines.contains("#4 The family tradition"), lines.toString());
	}

	@Test
	void aBookWithStoriesIsWrittenByTheRenderers(@TempDir final Path directory) throws IOException{
		final ReportDocument book = build(model(), entry(ReportType.INDIVIDUAL, "I1", StoryPlacement.SECTION));
		final Path target = directory.resolve("book.md");

		new MarkdownReportRenderer().render(book, target);

		final String text = Files.readString(target, StandardCharsets.UTF_8);
		Assertions.assertTrue(text.contains(TITLE) && text.contains(FIRST) && text.contains(SECOND), text);
	}

}
