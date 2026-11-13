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
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.reports.renderers.ReportRenderers;
import io.github.mtrevisan.reports.ReportDocument;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;


/**
 * The Markdown of every report of the Reports menu, on the sample file: what each report says, and that no report leaves a gap (a
 * double space, a {@code null}) where a piece of data is missing.
 */
class ReportMarkdownTest{

	private static final Pattern NULL_WORD = Pattern.compile("\\bnull\\b");
	/** The lines that are lists, tables, quotes, images, anchors or code: their spacing is part of the format. */
	private static final String FORMATTED_LINE_START = "-|*•!<>` ";

	private static FLEFModel model;
	@TempDir
	static Path directory;


	@BeforeAll
	static void load() throws IOException{
		model = new FLEFParser().parse(Path.of("src", "main", "resources", "tests", "test.flef"));
	}


	/** The report of the type about the record {@code id}, as Markdown (with its default options, and the individuals that may be living shown). */
	private static String markdown(final ReportType type, final String id) throws IOException{
		return markdown(type, id, LivingPrivacy.SHOW);
	}

	/** The same, with the given treatment of the individuals that may be living. */
	private static String markdown(final ReportType type, final String id, final LivingPrivacy living) throws IOException{
		final ReportDocument document = ReportBuilder.build(type, model, (id != null? model.getRecordById(id): null),
			ReportConfig.defaults().withLivingPrivacy(living), ReportProgressListener.NOOP);
		final Path target = directory.resolve(type.name() + "-" + id + "-" + living + ".md");
		ReportRenderers.available().stream()
			.filter(renderer -> "markdown".equals(renderer.id()))
			.findFirst()
			.orElseThrow()
			.render(document, target);
		return Files.readString(target, StandardCharsets.UTF_8);
	}

	/** The lines that follow the heading, up to the next heading of the same or a higher level (the blank ones are left out). */
	private static List<String> sectionOf(final String markdown, final String heading){
		final List<String> lines = markdown.lines().toList();
		final int start = lines.indexOf(heading);
		Assertions.assertTrue(start >= 0, heading + " is missing");

		final int level = heading.indexOf(' ');
		int end = start + 1;
		while(end < lines.size() && !(lines.get(end).startsWith("#") && lines.get(end).indexOf(' ') <= level))
			end ++;
		return lines.subList(start + 1, end).stream()
			.filter(line -> !line.isBlank())
			.toList();
	}

	private static String firstLine(final String markdown){
		return markdown.lines().findFirst().orElseThrow();
	}

	private static void assertContains(final String markdown, final String... expected){
		for(final String text : expected)
			Assertions.assertTrue(markdown.contains(text), "missing: " + text + "\n" + markdown);
	}

	private static void assertDoesNotContain(final String markdown, final String... unexpected){
		for(final String text : unexpected)
			Assertions.assertFalse(markdown.contains(text), "unexpected: " + text);
	}

	/** The subject of the report of the type, {@code null} if it is about the whole file. */
	private static String rootOf(final ReportType type){
		return switch(type){
			case GROUP -> "G1";
			case PLACE -> "P1";
			case SOURCE -> "S1";
			case RESEARCH_QUESTION -> "RQ1";
			case FULL_BIBLIOGRAPHY, FAMILY_BOOK, TO_VERIFY, SURNAMES -> null;
			default -> "I1";
		};
	}


	/* ----- Every report ------------------------------------------------------------------------------------------------------------ */

	@ParameterizedTest
	@EnumSource(ReportType.class)
	void noReportHasAGapWhereADataIsMissing(final ReportType type) throws IOException{
		final String markdown = markdown(type, rootOf(type));

		Assertions.assertFalse(NULL_WORD.matcher(markdown).find(), type + " says null");
		for(final String line : markdown.lines().toList())
			if(!line.isEmpty() && FORMATTED_LINE_START.indexOf(line.charAt(0)) < 0)
				Assertions.assertFalse(line.contains("  "), type + ": a gap in: " + line);
	}

	@ParameterizedTest
	@EnumSource(ReportType.class)
	void everyReportHasATitleAndTheDocumentInformation(final ReportType type) throws IOException{
		final List<String> lines = markdown(type, rootOf(type)).lines().toList();

		Assertions.assertTrue(lines.getFirst().startsWith("# "), lines.getFirst());
		Assertions.assertTrue(lines.contains("# Document information"));
		Assertions.assertTrue(lines.contains("- **Protocol:** Family LEgacy Format"));
	}


	/* ----- One test for each report ------------------------------------------------------------------------------------------------ */

	@Test
	void ancestorReport() throws IOException{
		final String markdown = markdown(ReportType.ANCESTOR, "I1");

		Assertions.assertEquals("# Ancestors of first name [I1]", firstLine(markdown));
		assertContains(markdown, "This report covers one individual.", "# Ancestor tree", "# Index of Individuals",
			"Little is known about the birth of first name.");
		Assertions.assertEquals(List.of("| Individual | Years |", "| --- | --- |", "| [first name [I1]](#I1) |  |"), sectionOf(markdown, "# Index of Individuals").stream()
			.filter(line -> line.startsWith("|"))
			.toList());
		assertDoesNotContain(markdown, "# Descendant tree");
	}

	@Test
	void aReportWithSeveralChaptersHasTheirContentsWithTheAnchorsOfTheViewers() throws IOException{
		final String markdown = markdown(ReportType.ANCESTOR, "I1");

		assertContains(markdown, "**Contents**", "- [Document information](#document-information)", "- [Ancestor tree](#ancestor-tree)",
			"- [Index of Individuals](#index-of-individuals)");
		//the title of the report is written once: the chapter after the front matter is the overview
		assertContains(markdown, "- [Overview](#overview)");
		Assertions.assertEquals(1, markdown.lines().filter(line -> line.equals("# Ancestors of first name [I1]")).count());
	}

	@Test
	void theSectionsOfAPersonAreSectionsOfThePersonNotChaptersOfTheReport() throws IOException{
		final String markdown = markdown(ReportType.ANCESTOR, "I1");

		final List<String> lines = markdown.lines().toList();
		for(final String heading : List.of("# first name [I1]", "## Introduction", "## Life of first name [I1]", "### Personal data"))
			Assertions.assertTrue(lines.contains(heading), heading + " is missing\n" + markdown);
		for(final String heading : List.of("# Introduction", "# Life of first name [I1]"))
			Assertions.assertFalse(lines.contains(heading), heading + " is a chapter");
	}

	@Test
	void descendantReport() throws IOException{
		final String markdown = markdown(ReportType.DESCENDANT, "I1");

		Assertions.assertEquals("# Descendants of first name [I1]", firstLine(markdown));
		assertContains(markdown, "This report covers one individual.", "# Descendant tree", "# Index of Individuals");
		assertDoesNotContain(markdown, "# Ancestor tree");
	}

	@Test
	void individualReport() throws IOException{
		final String markdown = markdown(ReportType.INDIVIDUAL, "I1");

		Assertions.assertEquals("# first name [I1]", firstLine(markdown));
		assertContains(markdown, "# Introduction", "# Life of first name [I1]", "## Personal data", "- **ID:** I1", "- **Sex:** female",
			"## Life events", "### epidemic (spouse) — late 19th century", "- **Place:** a certain place", "- **Description:** cholera",
			"## Attributes", "### residence: attr", "# Paternal Ancestry", "# Maternal Ancestry", "# Descendants",
			"# Sources", "## src title [S1]", "# Places", "# Index of Individuals", "# Index of Places");
		//the narrative tells what is known
		assertContains(markdown, "Little is known about the birth of first name.", "first name had epidemic: cholera");
	}

	@Test
	void theProvenanceOfANameSaysWhichNameItIs() throws IOException{
		final String markdown = markdown(ReportType.INDIVIDUAL, "I1");

		assertContains(markdown, "### Sources of the name (maiden): nòme");
		assertDoesNotContain(markdown, "### Sources of the name (maiden)\n");
	}

	@Test
	void groupReport() throws IOException{
		final String markdown = markdown(ReportType.GROUP, "G1");

		Assertions.assertEquals("# household group [G1]", firstLine(markdown));
		assertContains(markdown, "# History of household group [G1]", "## Group data", "## Members",
			"| Person | Years | Role | From | To |", "| first name [I1] |  | president | original text | original text |",
			"## Group attributes", "| residence | group attr value | original text | original text | a certain place |",
			"## Events involving the group", "### deed");
	}

	@Test
	void anIndividualThatMayBeLivingHasOnlyTheNameAndTheRoleWhenTheReportSaysSo() throws IOException{
		final String markdown = markdown(ReportType.INDIVIDUAL, "I1", LivingPrivacy.NAME_AND_ROLE);

		//the name and the role in the family
		assertContains(markdown, "# first name [I1]", "# Life of first name [I1]", "## Personal data", "- **ID:** I1",
			"### Group member: household group [G1] (president)");
		//nothing else: no events, attributes, narrative, sex, sources, places, notes
		assertDoesNotContain(markdown, "## Life events", "epidemic", "cholera", "## Attributes", "- **Sex:**", "# Sources",
			"had epidemic");
	}

	@Test
	void anIndividualThatMayBeLivingIsNotInTheReportWhenTheReportHidesThem() throws IOException{
		final String markdown = markdown(ReportType.INDIVIDUAL, "I1", LivingPrivacy.HIDE);

		assertContains(markdown, "No individual is available for this report");
		assertDoesNotContain(markdown, "first name", "Life of", "epidemic");
	}

	@Test
	void everyReportAboutTheWholeFileKeepsTheLivingHiddenToo() throws IOException{
		for(final ReportType type : new ReportType[]{ReportType.SURNAMES, ReportType.FAMILY_BOOK, ReportType.TO_VERIFY}){
			final String markdown = markdown(type, rootOf(type), LivingPrivacy.HIDE);

			assertDoesNotContain(markdown, "epidemic", "cholera");
		}
	}

	@Test
	void theIndexOfIndividualsOfAGroupListsItsMembers() throws IOException{
		final String markdown = markdown(ReportType.GROUP, "G1");

		Assertions.assertTrue(sectionOf(markdown, "# Index of Individuals").contains("| first name [I1] |  |"), markdown);
	}

	@Test
	void relationshipReport() throws IOException{
		final String markdown = markdown(ReportType.RELATIONSHIP, "I1");

		assertContains(markdown, "# Direct Relations", "## Subject", "| first name [I1] |", "# Indirect Relations",
			"(no indirect relations reported)", "# Index of Individuals");
		//the relations only: not the life of the person, nor what the life cites
		assertDoesNotContain(markdown, "# Paternal Ancestry", "# Descendants", "Life of", "# Sources", "# Places", "# Index of Places");
	}

	@Test
	void placeReport() throws IOException{
		final String markdown = markdown(ReportType.PLACE, "P1");

		assertContains(markdown, "# Place: a certain place", "## Historical jurisdictions", "## Contained places", "## Events at this place",
			"### epidemic — late 19th century", "#### Participants", "| spouse | first name [I1] | individual |",
			"## Attributes recorded at this place");
		assertDoesNotContain(markdown, "# Introduction");
	}

	@Test
	void sourceReport() throws IOException{
		final String markdown = markdown(ReportType.SOURCE, "S1");

		assertContains(markdown, "# Source: src title [S1]", "- **Publisher:** pub", "- **Record date:** 2001-01-01",
			"- **Place:** a certain place", "- **Media type:** book", "## Repository", "## Documents", "## Cited by");
	}

	@Test
	void theIndexOfIndividualsOfASourceDoesNotListTheSource() throws IOException{
		final String markdown = markdown(ReportType.SOURCE, "S1");

		Assertions.assertEquals(List.of("(empty)"), sectionOf(markdown, "# Index of Individuals"));
	}

	@Test
	void everyCitationOfASourceSaysWhoCitesIt() throws IOException{
		final List<String> citedBy = sectionOf(markdown(ReportType.SOURCE, "S1"), "## Cited by");

		Assertions.assertFalse(citedBy.isEmpty());
		for(final String line : citedBy){
			Assertions.assertNotEquals("- ?", line);
			Assertions.assertFalse(line.startsWith("- ? "), line);
		}
	}

	@Test
	void theRepositoryOfASourceIsToldByItsNameAndItsContacts() throws IOException{
		final List<String> repository = sectionOf(markdown(ReportType.SOURCE, "S1"), "## Repository");

		Assertions.assertEquals(List.of("- name — *shelf 1*", "  **email:** addr — contact name"), repository);
	}

	@Test
	void aRelationshipOfAnIndividualWithAGroupSaysWhichGroup() throws IOException{
		final String markdown = markdown(ReportType.INDIVIDUAL, "I1");

		assertContains(markdown, "### Group member: household group [G1] (president)");
		Assertions.assertTrue(markdown.lines().noneMatch(line -> line.startsWith("#") && line.endsWith(": ")), "a title ends with a colon");
	}

	@Test
	void researchQuestionReport() throws IOException{
		final String markdown = markdown(ReportType.RESEARCH_QUESTION, "RQ1");

		Assertions.assertEquals("# is? [open]", firstLine(markdown));
		assertContains(markdown, "# Research question: rq title", "- **Question:** is?", "- **Status:** open",
			"- **Conclusion:** it must be true", "- **Confidence:** medium", "## Targets", "- first name [I1]", "## Activities",
			"### review — do things", "- **Result:** positive", "## Tasks", "### correct individual 1", "- **Priority:** low",
			"## Conclusions", "### Issue: death cause", "**Proof status:** proven");
	}

	@Test
	void bibliographyReport() throws IOException{
		final String markdown = markdown(ReportType.BIBLIOGRAPHY, "I1");

		assertContains(markdown, "# Sources", "## src title [S1]", "- **Publisher:** pub", "### Documents", "# Index of Places");
		assertDoesNotContain(markdown, "# Introduction", "# Paternal Ancestry");
	}

	@Test
	void researchProgressReport() throws IOException{
		final String markdown = markdown(ReportType.RESEARCH_PROGRESS, "I1");

		assertContains(markdown, "# Historical Context", "# Research", "## rq title", "### Activities", "### Tasks", "# Conclusions",
			"## death cause", "# Identity Hypotheses", "# Coverage report", "| Record | Fields | Source | Evidence |",
			"| first name [I1] [individual] | 2/2 | — | — |");
		assertDoesNotContain(markdown, "# Paternal Ancestry", "# Direct Relations");
	}

	@Test
	void fullBibliographyReport() throws IOException{
		final String markdown = markdown(ReportType.FULL_BIBLIOGRAPHY, null);

		Assertions.assertEquals("# Bibliography", firstLine(markdown));
		assertContains(markdown, "Sources cited: 2", "| Title | Author | Record date | Citations |", "## Source: src title [S1]",
			"## Source: src title for group [S2]", "### Cited by");
		Assertions.assertTrue(markdown.lines().anyMatch(line -> line.startsWith("| src title [S1] |  | 2001-01-01 | ")), markdown);
		Assertions.assertTrue(markdown.lines().anyMatch(line -> line.startsWith("| src title for group [S2] |  |  | ")), markdown);
	}

	@Test
	void surnamesReport() throws IOException{
		final String markdown = markdown(ReportType.SURNAMES, null);

		Assertions.assertEquals("# Surnames by Place", firstLine(markdown));
		//the individuals of the sample file have a given name only
		assertContains(markdown, "No individual has a surname.");
	}

	@Test
	void toVerifyReport() throws IOException{
		final String markdown = markdown(ReportType.TO_VERIFY, null);

		Assertions.assertEquals("# To Verify", firstLine(markdown));
		assertContains(markdown, "Things to verify:", "| What | Number |", "| Record | Detail |", "research assistant also looks");
	}

	@Test
	void familyBookReport() throws IOException{
		final String markdown = markdown(ReportType.FAMILY_BOOK, null);

		Assertions.assertEquals("# Family Book", firstLine(markdown));
		assertContains(markdown, "This report covers 3 individuals.", "# Index of Individuals", "| first name [I1] |", "# Index of Groups",
			"| household group [G1] |", "# first name [I1]", "# household group [G1]", "# Sources", "# Places");
		//the persons come one after the other, each with its own life (a section of the chapter of the person)
		Assertions.assertEquals(3, markdown.lines().filter(line -> line.startsWith("## Life of ")).count());
		Assertions.assertEquals(0, markdown.lines().filter(line -> line.startsWith("# Life of ")).count());
		Assertions.assertTrue(StringUtils.countMatches(markdown, "## Personal data") >= 3);
	}

}
