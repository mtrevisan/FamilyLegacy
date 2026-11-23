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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.BookBuilder;
import io.github.mtrevisan.familylegacy.reports.BookDefinition;
import io.github.mtrevisan.familylegacy.reports.BookEntry;
import io.github.mtrevisan.familylegacy.reports.BookIndex;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.renderers.ReportRenderers;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportRenderer;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/** The Book Builder: reports composed in a single document, with the pages before them and the indexes of the whole book after them. */
class BookBuilderTest{

	private static String individual(final String id, final String given, final String family){
		return "\tINDIVIDUAL " + id + " {\n\t\tNAME {\n\t\t\tPART {\n\t\t\t\tTYPE given\n\t\t\t\tVALUE " + given + "\n\t\t\t}\n"
			+ "\t\t\tPART {\n\t\t\t\tTYPE family\n\t\t\t\tVALUE " + family + "\n\t\t\t}\n\t\t}\n\t}\n";
	}

	private static String birth(final String number, final String person, final String date){
		return "\tEVENT E" + number + " {\n\t\tTYPE birth\n\t\tDATE.VALUE.POINT {\n\t\t\tVALUE.FULL_DATE " + date
			+ "\n\t\t\tCALENDAR gregorian\n\t\t}\n\t}\n"
			+ "\tEVENT_PARTICIPATION EP" + number + " {\n\t\tPARTICIPANT.INDIVIDUAL " + person + "\n\t\tEVENT E" + number
			+ "\n\t\tROLE principal\n\t}\n";
	}

	/** Three persons: Anna Rossi, Bruno Rossi and Carla Bianchi. */
	private static String familyText(){
		return "header {\n\tPROTOCOL {\n\t\tNAME Family LEgacy Format\n\t\tVERSION 0.1.3\n\t}\n}\n\nrecords {\n"
			+ individual("I1", "Anna", "Rossi") + individual("I2", "Bruno", "Rossi") + individual("I3", "Carla", "Bianchi")
			+ birth("1", "I1", "1990-01-01") + birth("2", "I2", "1960-01-01") + birth("3", "I3", "1962-01-01")
			+ "}\n";
	}

	private static FLEFModel family;
	private static FLEFModel sample;


	@BeforeAll
	static void load() throws IOException{
		family = new FLEFParser().parse(familyText());
		sample = new FLEFParser().parse(Path.of("src", "main", "resources", "tests", "test.flef"));
	}


	private static ReportConfig config(){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW);
	}

	private static BookDefinition book(final boolean cover, final boolean titlePage, final String preface, final Set<BookIndex> indexes,
			final BookEntry... entries){
		return new BookDefinition("My Book", "A subtitle", "Prepared for the family", ReportLanguage.ENGLISH, cover, titlePage, "",
			preface, indexes, List.of(entries));
	}

	private static BookEntry entry(final ReportType type, final String subject){
		return new BookEntry(type, subject, config());
	}

	private static ReportDocument build(final FLEFModel model, final BookDefinition book){
		return BookBuilder.build(model, book, ReportProgressListener.NOOP);
	}

	private static List<ReportSection.Heading> headings(final ReportDocument document){
		return document.sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(ReportSection.Heading.class::cast)
			.toList();
	}

	private static List<String> texts(final ReportDocument document, final int level){
		return headings(document).stream()
			.filter(heading -> heading.level() == level)
			.map(ReportSection.Heading::text)
			.toList();
	}

	/** @return	The table that follows the heading of the first level with the text. */
	private static ReportSection.Table tableAfter(final ReportDocument document, final String heading){
		return tableAfter(document, 1, heading);
	}

	/** @return	The table that follows the heading with the level and the text. */
	private static ReportSection.Table tableAfter(final ReportDocument document, final int level, final String heading){
		boolean found = false;
		for(final ReportSection section : document.sections()){
			if(section instanceof ReportSection.Heading h)
				found = (h.level() == level && h.text().equals(heading));
			else if(found && section instanceof ReportSection.Table table)
				return table;
		}
		return null;
	}


	@Test
	void theReportsAreChaptersInTheOrderOfTheBook(){
		final BookDefinition ancestors = book(false, false, "", Set.of(), entry(ReportType.INDIVIDUAL, "I1"), entry(ReportType.INDIVIDUAL, "I3"));

		final ReportDocument document = build(family, ancestors);

		Assertions.assertEquals("My Book", document.title());
		Assertions.assertEquals("A subtitle", document.subtitle());
		final List<String> chapters = texts(document, 1);
		Assertions.assertEquals(2, chapters.size(), chapters.toString());
		Assertions.assertTrue(chapters.getFirst().contains("Anna"), chapters.toString());
		Assertions.assertTrue(chapters.getLast().contains("Carla"), chapters.toString());

		//what a report tells is a section of its chapter, not a chapter of the book
		Assertions.assertFalse(texts(document, 2).isEmpty());

		//put in the other order
		final ReportDocument reversed = build(family, book(false, false, "", Set.of(), entry(ReportType.INDIVIDUAL, "I3"),
			entry(ReportType.INDIVIDUAL, "I1")));
		Assertions.assertTrue(texts(reversed, 1).getFirst().contains("Carla"), texts(reversed, 1).toString());
	}

	@Test
	void everyChapterStartsOnANewPage(){
		final ReportDocument document = build(family, book(false, false, "", Set.of(), entry(ReportType.INDIVIDUAL, "I1"),
			entry(ReportType.INDIVIDUAL, "I2")));

		final List<ReportSection> sections = document.sections();
		//(the first one is at the start of the book: a page break there would be the cover)
		Assertions.assertInstanceOf(ReportSection.Heading.class, sections.getFirst());
		for(int i = 1; i < sections.size(); i ++)
			if(sections.get(i) instanceof ReportSection.Heading heading && heading.level() == 1)
				Assertions.assertInstanceOf(ReportSection.PageBreak.class, sections.get(i - 1), heading.text());
	}

	@Test
	void theCoverPageTheTitlePageAndThePrefaceComeFirst(){
		final ReportDocument document = build(family, book(true, true, "First paragraph\nstill the first.\n\nSecond paragraph.", Set.of(),
			entry(ReportType.INDIVIDUAL, "I1")));
		final List<ReportSection> sections = document.sections();

		//the page break that ends the cover, then the title page
		Assertions.assertInstanceOf(ReportSection.PageBreak.class, sections.getFirst());
		Assertions.assertEquals(new ReportSection.Paragraph("**My Book**"), sections.get(1));
		Assertions.assertEquals(new ReportSection.Paragraph("*A subtitle*"), sections.get(2));
		Assertions.assertEquals(new ReportSection.Paragraph("Prepared for the family"), sections.get(3));
		Assertions.assertInstanceOf(ReportSection.PageBreak.class, sections.get(4));

		//the preface, a paragraph for each block of the text (the lines of a block are joined: a paragraph with a new line is preformatted)
		Assertions.assertEquals(new ReportSection.Heading(1, "Preface"), sections.get(5));
		Assertions.assertEquals(new ReportSection.Paragraph("First paragraph still the first."), sections.get(6));
		Assertions.assertEquals(new ReportSection.Paragraph("Second paragraph."), sections.get(7));
		Assertions.assertEquals("Preface", texts(document, 1).getFirst());
	}

	@Test
	void withoutCoverTitlePageAndPrefaceTheBookStartsWithTheFirstReport(){
		final ReportDocument document = build(family, book(false, false, "  ", Set.of(), entry(ReportType.INDIVIDUAL, "I1")));

		Assertions.assertInstanceOf(ReportSection.Heading.class, document.sections().getFirst());
		Assertions.assertEquals(1, texts(document, 1).size());
	}

	@Test
	void theIndexOfTheIndividualsIsOneForTheWholeBook(){
		final BookDefinition definition = book(false, false, "", EnumSet.of(BookIndex.INDIVIDUALS, BookIndex.PLACES),
			entry(ReportType.FAMILY_BOOK, null), entry(ReportType.INDIVIDUAL, "I1"));

		final ReportDocument document = build(family, definition);

		//no index of the individuals inside the reports, just the one at the end, with everybody once, by family name
		final long indexes = texts(document, 1).stream().filter("Index of Individuals"::equals).count();
		Assertions.assertEquals(1, indexes, texts(document, 1).toString());
		Assertions.assertEquals("Index of Individuals", texts(document, 1).stream().filter(text -> text.startsWith("Index")).findFirst().orElseThrow());

		final ReportSection.Table table = tableAfter(document, "Index of Individuals");
		Assertions.assertNotNull(table);
		Assertions.assertEquals(3, table.rows().size(), table.rows().toString());
		final List<String> order = new ArrayList<>();
		for(final List<String> row : table.rows()){
			final String text = row.getFirst();
			final int start = text.indexOf("[I");
			order.add(text.substring(start + 1, text.indexOf(']', start)).replace("\\", ""));
		}
		Assertions.assertEquals(List.of("I3", "I1", "I2"), order);
	}

	@Test
	void theIndexesOfTheReportsStayWhenTheBookHasNone(){
		final ReportDocument document = build(family, book(false, false, "", Set.of(), entry(ReportType.FAMILY_BOOK, null)));

		//(a section of the chapter of the report, as everything it tells)
		Assertions.assertNotNull(tableAfter(document, 2, "Index of Individuals"), texts(document, 2).toString());
	}

	@Test
	void theIndexesOfTheSourcesAndOfTheRepositoriesListWhatTheReportsHave(){
		final BookDefinition definition = book(false, false, "", EnumSet.allOf(BookIndex.class), entry(ReportType.FAMILY_BOOK, null));

		final ReportDocument document = build(sample, definition);

		final ReportSection.Table sources = tableAfter(document, "Index of Sources");
		Assertions.assertNotNull(sources, texts(document, 1).toString());
		Assertions.assertFalse(sources.rows().isEmpty());
		//every source of the index is a heading of the Sources section of the chapter
		final List<String> headingTexts = headings(document).stream().map(ReportSection.Heading::text).toList();
		for(final List<String> row : sources.rows())
			Assertions.assertTrue(headingTexts.contains(row.getFirst()), row.getFirst());
		Assertions.assertNotNull(tableAfter(document, "Index of Places"));

		//the indexes are the last chapters of the book, in this order
		final List<String> chapters = texts(document, 1);
		Assertions.assertTrue(chapters.indexOf("Index of Individuals") < chapters.indexOf("Index of Places"), chapters.toString());
		Assertions.assertTrue(chapters.indexOf("Index of Places") < chapters.indexOf("Index of Sources"), chapters.toString());
	}

	@Test
	void aBookNeedsAReport(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> build(family, book(false, false, "", Set.of())));
	}

	@Test
	void aReportAboutARecordThatIsNotThereIsAnError(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> build(family, book(false, false, "", Set.of(), entry(ReportType.INDIVIDUAL, "I99"))));
	}

	@Test
	void theLanguageOfTheBookIsTheOneOfEveryReport(){
		final BookDefinition italian = new BookDefinition("", "", "", ReportLanguage.of(Locale.ITALIAN), false, false, "", "Prefazione",
			Set.of(), List.of(entry(ReportType.INDIVIDUAL, "I1")));

		final ReportDocument document = build(family, italian);

		Assertions.assertEquals("it", document.language());
		//(the title of the language when the book has none)
		Assertions.assertFalse(document.title().isBlank());
	}

	@Test
	void theProgressGoesFromTheStartToTheEnd(){
		final List<Integer> percents = new ArrayList<>();

		BookBuilder.build(family, book(false, false, "", Set.of(), entry(ReportType.INDIVIDUAL, "I1"), entry(ReportType.INDIVIDUAL, "I2")),
			(percent, message) -> percents.add(percent));

		Assertions.assertEquals(100, percents.getLast());
		for(int i = 1; i < percents.size(); i ++)
			Assertions.assertTrue(percents.get(i) >= percents.get(i - 1), percents.toString());
	}

	@Test
	void theBookIsWrittenInEveryFormat(@TempDir final Path directory) throws IOException{
		final ReportDocument document = build(sample, book(true, true, "Hello.", EnumSet.allOf(BookIndex.class), entry(ReportType.FAMILY_BOOK, null),
			entry(ReportType.DATABASE_SUMMARY, null)));

		for(final ReportRenderer renderer : ReportRenderers.available()){
			final Path file = directory.resolve("book" + renderer.extension());

			renderer.render(document, file);

			Assertions.assertTrue(Files.exists(file), renderer.id());
			if(!renderer.isDirectory())
				Assertions.assertTrue(Files.size(file) > 0, renderer.id());
		}
		final String markdown = Files.readString(directory.resolve("book.md"));
		Assertions.assertTrue(markdown.contains("My Book"));
		Assertions.assertTrue(markdown.contains("Preface"));
		Assertions.assertTrue(markdown.contains("Index of Individuals"));
	}

}
