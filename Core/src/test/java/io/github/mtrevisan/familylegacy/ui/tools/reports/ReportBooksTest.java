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
import io.github.mtrevisan.familylegacy.reports.OptionState;
import io.github.mtrevisan.familylegacy.reports.PersonOrder;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/** The two reports about the whole file: the Family Book, with a page for everybody, and the Full Bibliography, with every source. */
class ReportBooksTest{

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

	/** Five persons: Anna, Bruno and Dario Rossi, Carla Bianchi and Elena Verdi, the last one without a birth. */
	private static String familyText(){
		return "header {\n\tPROTOCOL {\n\t\tNAME Family LEgacy Format\n\t\tVERSION 0.1.3\n\t}\n}\n\nrecords {\n"
			+ individual("I1", "Anna", "Rossi") + individual("I2", "Bruno", "Rossi") + individual("I3", "Carla", "Bianchi")
			+ individual("I4", "Dario", "Rossi") + individual("I5", "Elena", "Verdi")
			+ birth("1", "I1", "1990-01-01") + birth("2", "I2", "1960-01-01") + birth("3", "I3", "1962-01-01") + birth("4", "I4", "1930-01-01")
			+ "}\n";
	}

	private static FLEFModel family;
	private static FLEFModel sample;


	@BeforeAll
	static void load() throws IOException{
		family = new FLEFParser().parse(familyText());
		sample = new FLEFParser().parse(Path.of("src", "main", "resources", "tests", "test.flef"));
	}


	private static ReportDocument build(final ReportType type, final FLEFModel model, final ReportConfig config){
		return ReportBuilder.build(type, model, null, config, ReportProgressListener.NOOP);
	}

	/** @return	The identifiers of the persons of the Index of individuals, in the order of the book. */
	private static List<String> personsOf(final ReportDocument document){
		final List<String> ids = new ArrayList<>();
		boolean inIndex = false;
		for(final ReportSection section : document.sections()){
			if(section instanceof final ReportSection.Heading heading)
				inIndex = "Index of Individuals".equals(heading.text());
			else if(inIndex && section instanceof final ReportSection.Table table){
				for(final List<String> row : table.rows()){
					final String text = row.getFirst();
					final int start = text.indexOf("[I");
					ids.add(text.substring(start + 1, text.indexOf(']', start)).replace("\\", ""));
				}
				return ids;
			}
		}
		return ids;
	}

	private static List<ReportSection.Heading> headings(final ReportDocument document){
		return document.sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(ReportSection.Heading.class::cast)
			.toList();
	}


	@Test
	void theFamilyBookHasEverybodyInAlphabeticalOrderByFamilyName(){
		final ReportDocument book = build(ReportType.FAMILY_BOOK, family, ReportConfig.defaults());

		Assertions.assertEquals("Family Book", book.title());
		Assertions.assertEquals(List.of("I3", "I1", "I2", "I4", "I5"), personsOf(book));
	}

	@Test
	void theFamilyBookCanBeInOrderOfBirth(){
		final ReportConfig config = ReportConfig.defaults().with(ReportOption.PERSON_ORDER, PersonOrder.BIRTH_DATE);

		//whoever has no birth year comes last
		Assertions.assertEquals(List.of("I4", "I2", "I3", "I1", "I5"), personsOf(build(ReportType.FAMILY_BOOK, family, config)));
	}

	@Test
	void theFamilyBookHasAPageForEachPersonAndWhatTheyCiteTogether(){
		final ReportDocument book = build(ReportType.FAMILY_BOOK, sample, ReportConfig.defaults());
		final List<String> texts = headings(book).stream().map(ReportSection.Heading::text).toList();

		Assertions.assertEquals(3, headings(book).stream()
			.filter(heading -> heading.level() == 1 && heading.text().contains("[I") && !heading.text().startsWith("Life of"))
			.count(), texts.toString());
		Assertions.assertTrue(texts.contains("Sources"), texts.toString());
		Assertions.assertTrue(texts.contains("Places"), texts.toString());
		Assertions.assertTrue(texts.contains("Index of Places"), texts.toString());
		Assertions.assertTrue(texts.stream().anyMatch(text -> text.startsWith("Life of")), texts.toString());
	}

	@Test
	void theFamilyBookHasAPageForEachGroupAndTheSourcesTheyCite(){
		final List<String> with = headings(build(ReportType.FAMILY_BOOK, sample, ReportConfig.defaults())).stream()
			.map(ReportSection.Heading::text)
			.toList();
		final List<String> without = headings(build(ReportType.FAMILY_BOOK, sample, ReportConfig.defaults().with(ReportOption.GROUPS, false)))
			.stream()
			.map(ReportSection.Heading::text)
			.toList();

		Assertions.assertTrue(with.stream().anyMatch(text -> text.startsWith("History of") && text.contains("[G1]")), with.toString());
		Assertions.assertTrue(with.stream().anyMatch(text -> text.startsWith("History of") && text.contains("[G2]")), with.toString());
		//what the group G1 cites is in the sources of the book only when the groups are in it
		Assertions.assertTrue(with.contains("src title for group [S2]"), with.toString());
		Assertions.assertFalse(without.contains("src title for group [S2]"), without.toString());
		Assertions.assertTrue(without.stream().noneMatch(text -> text.startsWith("History of")), without.toString());
	}

	@Test
	void theFamilyBookHasAnIndexOfTheGroupsWhenItHasGroupsAndTheIndexIsOn(){
		final ReportConfig defaults = ReportConfig.defaults();
		final ReportDocument book = build(ReportType.FAMILY_BOOK, sample, defaults);

		Assertions.assertEquals(List.of("Index of Individuals", "Index of Groups"), headings(book).stream()
			.map(ReportSection.Heading::text)
			.filter(text -> text.startsWith("Index of") && !text.equals("Index of Places"))
			.toList());
		final ReportSection.Table groups = book.sections().stream()
			.filter(ReportSection.Table.class::isInstance)
			.map(ReportSection.Table.class::cast)
			.filter(table -> table.headers().equals(List.of("Group")))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals(2, groups.rows().size());

		for(final ReportConfig config : List.of(defaults.with(ReportOption.INDEX_INDIVIDUALS, false), defaults.with(ReportOption.GROUPS, false)))
			Assertions.assertTrue(headings(build(ReportType.FAMILY_BOOK, sample, config)).stream()
				.noneMatch(heading -> "Index of Groups".equals(heading.text())));
		//a book of a file without groups has none
		Assertions.assertTrue(headings(build(ReportType.FAMILY_BOOK, family, defaults)).stream()
			.noneMatch(heading -> "Index of Groups".equals(heading.text())));
	}

	@Test
	void aFileWithOnlyGroupsStillGivesABook(){
		final FLEFModel groups = new FLEFModel();
		groups.addRecord(io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord.createMainRecord("G1",
			io.github.mtrevisan.familylegacy.io.model.readers.GroupReader.TYPE));

		Assertions.assertDoesNotThrow(() -> build(ReportType.FAMILY_BOOK, groups, ReportConfig.defaults()));
	}

	@Test
	void anEmptyFileGivesAnEmptyBookAndAnEmptyBibliography(){
		final FLEFModel empty = new FLEFModel();

		Assertions.assertFalse(build(ReportType.FAMILY_BOOK, empty, ReportConfig.defaults()).sections().isEmpty());
		Assertions.assertFalse(build(ReportType.FULL_BIBLIOGRAPHY, empty, ReportConfig.defaults()).sections().isEmpty());
	}

	@Test
	void theFullBibliographyListsEverySourceOnce(){
		final ReportDocument bibliography = build(ReportType.FULL_BIBLIOGRAPHY, sample, ReportConfig.defaults());

		Assertions.assertEquals("Bibliography", bibliography.title());
		final ReportSection.Table table = bibliography.sections().stream()
			.filter(ReportSection.Table.class::isInstance)
			.map(ReportSection.Table.class::cast)
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals(2, table.rows().size());
		Assertions.assertEquals(4, table.headers().size());
		//in alphabetical order
		Assertions.assertTrue(table.rows().get(0).getFirst().startsWith("src title [S1]"), table.rows().toString());
		Assertions.assertTrue(table.rows().get(1).getFirst().startsWith("src title for group"), table.rows().toString());
	}

	@Test
	void eachSourceOfTheFullBibliographyIsOneLevelDown(){
		final List<ReportSection.Heading> headings = headings(build(ReportType.FULL_BIBLIOGRAPHY, sample, ReportConfig.defaults()));

		final ReportSection.Heading first = headings.stream()
			.filter(heading -> heading.text().startsWith("Source: src title [S1]"))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals(2, first.level());
		Assertions.assertTrue(headings.stream().anyMatch(heading -> heading.level() == 3 && "Repository".equals(heading.text())),
			headings.toString());
		Assertions.assertTrue(headings.stream().noneMatch(heading -> heading.level() == 1 && heading.text().startsWith("Source:")),
			headings.toString());
	}

	@Test
	void theBooksNeedNoSubjectAndTheirOptionsAreTheirOwn(){
		Assertions.assertTrue(ReportType.FAMILY_BOOK.isGlobal());
		Assertions.assertTrue(ReportType.FULL_BIBLIOGRAPHY.isGlobal());
		Assertions.assertFalse(ReportType.BIBLIOGRAPHY.isGlobal());

		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.FAMILY_BOOK, null, ReportOption.PERSON_ORDER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.FAMILY_BOOK, null, ReportOption.COLLATERAL_SCOPE));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.FAMILY_BOOK, null, ReportOption.GENERATIONS));
		//the groups have a page: their options apply (and not to the ancestor report)
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.FAMILY_BOOK, null, ReportOption.GROUPS));
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.FAMILY_BOOK, null, ReportOption.GROUP_MEMBERS));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.ANCESTOR, null, ReportOption.GROUP_MEMBERS));
		Assertions.assertEquals(OptionState.ALWAYS_ON, ReportBuilder.optionState(ReportType.FAMILY_BOOK, null, ReportOption.INTRODUCTION));
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.FAMILY_BOOK, null, ReportOption.INDEX_INDIVIDUALS));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.ANCESTOR, null, ReportOption.PERSON_ORDER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.INDIVIDUAL, null, ReportOption.PERSON_ORDER));

		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.FULL_BIBLIOGRAPHY, null, ReportOption.NOTES));
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.FULL_BIBLIOGRAPHY, null, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.FULL_BIBLIOGRAPHY, null, ReportOption.TIMELINE));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.FULL_BIBLIOGRAPHY, null, ReportOption.INTRODUCTION));
	}

}
