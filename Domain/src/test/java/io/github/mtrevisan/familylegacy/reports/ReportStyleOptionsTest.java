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
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * The options of the narrative style (the numbering, the bold of the direct ancestors, the cross references, the names) and of the placement
 * of notes and sources.
 * <p>
 * Maria Rossi (I1) is the child of Giovanni Battista Rossi (I2) and Anna Bianchi (I3); Giovanni is the child of Pietro Rossi (I4) and Lucia Verdi
 * (I5). Maria has two children, Luca (I6, born 1900) and Sara (I7, born 1890); Sara has a child, Ugo (I8).
 * </p>
 */
class ReportStyleOptionsTest{

	private static ReportConfig everything(){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW);
	}


	private FLEFModel model;
	private int sequence;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		sequence = 0;
		model.addRecord(person("I1", "Maria", "Rossi", "female"));
		model.addRecord(person("I2", "Giovanni Battista", "Rossi", "male"));
		model.addRecord(person("I3", "Anna", "Bianchi", "female"));
		model.addRecord(person("I4", "Pietro", "Rossi", "male"));
		model.addRecord(person("I5", "Lucia", "Verdi", "female"));
		model.addRecord(person("I6", "Luca", "Neri", "male"));
		model.addRecord(person("I7", "Sara", "Neri", "female"));
		model.addRecord(person("I8", "Ugo", "Neri", "male"));
		model.addRecord(FLEFRecord.createMainRecord("I9", IndividualReader.TYPE));
		sex("I1", "female");
		sex("I2", "male");
		sex("I3", "female");
		sex("I4", "male");
		sex("I5", "female");
		sex("I6", "male");
		sex("I7", "female");
		sex("I8", "male");

		child("I1", "I2");
		child("I1", "I3");
		child("I2", "I4");
		child("I2", "I5");
		child("I6", "I1");
		child("I7", "I1");
		child("I8", "I7");
		event("I6", EventReader.ENUM_TYPE_BIRTH, "1900-01-01");
		event("I7", EventReader.ENUM_TYPE_BIRTH, "1890-01-01");
	}


	private static FLEFRecord person(final String id, final String given, final String family, final String ignoredSex){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", given)))
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", family))));
	}

	private void sex(final String individual, final String value){
		sequence ++;
		model.addRecord(FLEFRecord.createMainRecord("IA" + sequence, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individual))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value)));
	}

	/** The child is the subject of a relationship whose target is the parent. */
	private void child(final String child, final String parent){
		sequence ++;
		model.addRecord(FLEFRecord.createMainRecord("R" + sequence, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, child)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, parent))));
	}

	/** An event of the person; {@code children} are added to it (a note, a citation). */
	private FLEFRecord event(final String personId, final String type, final String date, final FLEFRecord... children){
		sequence ++;
		final FLEFRecord event = FLEFRecord.createMainRecord("E" + sequence, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type));
		if(date != null)
			event.addChild(FLEFRecord.createChildWithTag("date")
				.addChild(FLEFRecord.createChildWithTag("value")
					.addChild(FLEFRecord.createChildWithTag("point")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", date)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))));
		for(final FLEFRecord child : children)
			event.addChild(child);
		model.addRecord(event);
		model.addRecord(FLEFRecord.createMainRecord("EP" + sequence, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));
		return event;
	}

	private static FLEFRecord note(final String text){
		return FLEFRecord.createChildWithTagAndValue("note", text);
	}

	private void source(final String id, final String title){
		model.addRecord(FLEFRecord.createMainRecord(id, SourceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(SourceReader.TAG_TITLE)
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_TITLE_VALUE, title))));
	}

	/** A citation of a source, with a locator, an extract text, and (if {@code strong}) the evidence of an original source with direct evidence. */
	private FLEFRecord citation(final String sourceId, final String locator, final String extractText, final boolean strong){
		final FLEFRecord citation = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId));
		if(locator != null)
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_LOCATOR, locator));
		if(extractText != null){
			sequence ++;
			final String extractId = "X" + sequence;
			model.addRecord(FLEFRecord.createMainRecord(extractId, ExtractReader.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_SOURCE, sourceId))
				.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_TEXT, extractText)));
			citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_EXTRACT, extractId));
		}
		if(strong)
			citation.addChild(FLEFRecord.createChildWithTag(EvidenceQualifiersReader.TAG_EVIDENCE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_SOURCE_TYPE, EvidenceQualifiersReader.ENUM_SOURCE_TYPE_ORIGINAL))
				.addChild(FLEFRecord.createChildWithTagAndValue(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT)));
		return citation;
	}


	private static List<String> texts(final ReportDocument document){
		final List<String> out = new ArrayList<>();
		for(final ReportSection section : document.sections())
			switch(section){
				case ReportSection.Heading heading -> out.add(heading.text());
				case ReportSection.Paragraph paragraph -> out.add(paragraph.text());
				case ReportSection.BulletList list -> out.addAll(list.items());
				case ReportSection.Table table -> {
					out.add(String.join(" | ", table.headers()));
					for(final List<String> row : table.rows())
						out.add(String.join(" | ", row));
				}
				default -> { }
			}
		return out;
	}

	private static String all(final ReportDocument document){
		return String.join("\n", texts(document));
	}


	/* ----- Numbering ------------------------------------------------------- */

	@Test
	void anAhnentafelNumberGivesTheFatherTwiceAndTheMotherTwicePlusOne(){
		final ReportContext ctx = ReportContext.build(model, model.getRecordById("I1"), everything());

		final PersonMarks marks = PersonMarks.of(ctx, ReportType.ANCESTOR);

		Assertions.assertEquals("1", marks.numberOf(model.getRecordById("I1")));
		Assertions.assertEquals("2", marks.numberOf(model.getRecordById("I2")));
		Assertions.assertEquals("3", marks.numberOf(model.getRecordById("I3")));
		Assertions.assertEquals("4", marks.numberOf(model.getRecordById("I4")));
		Assertions.assertEquals("5", marks.numberOf(model.getRecordById("I5")));
		Assertions.assertNull(marks.numberOf(model.getRecordById("I6")));
	}

	@Test
	void theGenerationsLimitTheNumberedAncestors(){
		final ReportContext ctx = ReportContext.build(model, model.getRecordById("I1"), everything().with(ReportOption.GENERATIONS, 1));

		final PersonMarks marks = PersonMarks.of(ctx, ReportType.ANCESTOR);

		Assertions.assertEquals("3", marks.numberOf(model.getRecordById("I3")));
		Assertions.assertNull(marks.numberOf(model.getRecordById("I4")));
	}

	@Test
	void dAbovilleNumbersTheChildrenFromTheEldest(){
		final ReportContext ctx = ReportContext.build(model, model.getRecordById("I1"), everything());

		final PersonMarks marks = PersonMarks.of(ctx, ReportType.DESCENDANT);

		Assertions.assertEquals("1", marks.numberOf(model.getRecordById("I1")));
		//Sara (1890) is older than Luca (1900)
		Assertions.assertEquals("1.1", marks.numberOf(model.getRecordById("I7")));
		Assertions.assertEquals("1.2", marks.numberOf(model.getRecordById("I6")));
		Assertions.assertEquals("1.1.1", marks.numberOf(model.getRecordById("I8")));
	}

	@Test
	void henryWritesTheNumberOfTheChildAfterTheOneOfTheParent(){
		final ReportContext ctx = ReportContext.build(model, model.getRecordById("I1"), everything().with(ReportOption.NUMBERING, NumberingSystem.HENRY));

		final PersonMarks marks = PersonMarks.of(ctx, ReportType.DESCENDANT);

		Assertions.assertEquals("11", marks.numberOf(model.getRecordById("I7")));
		Assertions.assertEquals("12", marks.numberOf(model.getRecordById("I6")));
		Assertions.assertEquals("111", marks.numberOf(model.getRecordById("I8")));
	}

	@Test
	void aSystemOfTheOtherDirectionIsReplacedByTheOneOfTheReport(){
		Assertions.assertEquals(NumberingSystem.AHNENTAFEL, NumberingSystem.AUTO.resolve(ReportType.ANCESTOR));
		Assertions.assertEquals(NumberingSystem.D_ABOVILLE, NumberingSystem.AUTO.resolve(ReportType.DESCENDANT));
		Assertions.assertEquals(NumberingSystem.AHNENTAFEL, NumberingSystem.HENRY.resolve(ReportType.ANCESTOR));
		Assertions.assertEquals(NumberingSystem.D_ABOVILLE, NumberingSystem.AHNENTAFEL.resolve(ReportType.DESCENDANT));
		Assertions.assertEquals(NumberingSystem.HENRY, NumberingSystem.HENRY.resolve(ReportType.DESCENDANT));
		Assertions.assertEquals(NumberingSystem.NONE, NumberingSystem.NONE.resolve(ReportType.ANCESTOR));
		//the other reports are not numbered
		Assertions.assertEquals(NumberingSystem.NONE, NumberingSystem.AUTO.resolve(ReportType.INDIVIDUAL));
	}

	@Test
	void theAncestorReportNumbersTheChaptersTheRosterAndTheIndex(){
		final ReportDocument document = new AncestorReportGenerator(model, model.getRecordById("I1"), everything()).generate();
		final List<String> texts = texts(document);

		//(without the user interface the persons are told by their identifier)
		Assertions.assertTrue(texts.contains("1. I1"), texts.toString());
		Assertions.assertTrue(texts.contains("2. I2"), texts.toString());
		Assertions.assertTrue(texts.contains("5. I5"), texts.toString());
		Assertions.assertTrue(texts.stream().anyMatch(text -> text.startsWith("No. | Individual | Years")), texts.toString());
		Assertions.assertTrue(texts.stream().anyMatch(text -> text.startsWith("2 | [I2](@I2@)")), texts.toString());
	}

	@Test
	void theDescendantReportNumbersTheChaptersWithTheNumbersOfTheSystem(){
		final ReportDocument document = new DescendantReportGenerator(model, model.getRecordById("I1"), everything()).generate();
		final List<String> texts = texts(document);

		Assertions.assertTrue(texts.contains("1.1. I7"), texts.toString());
		Assertions.assertTrue(texts.contains("1.1.1. I8"), texts.toString());
	}

	@Test
	void withoutNumberingThereAreNoNumbersNorColumn(){
		final ReportDocument document = new AncestorReportGenerator(model, model.getRecordById("I1"),
			everything().with(ReportOption.NUMBERING, NumberingSystem.NONE)).generate();
		final List<String> texts = texts(document);

		Assertions.assertTrue(texts.contains("I2"), texts.toString());
		Assertions.assertFalse(texts.stream().anyMatch(text -> text.startsWith("No. |")), texts.toString());
	}

	@Test
	void theYearsOfTheIndexCanBeLeftOut(){
		final ReportDocument with = new AncestorReportGenerator(model, model.getRecordById("I1"), everything()).generate();
		final ReportDocument without = new AncestorReportGenerator(model, model.getRecordById("I1"),
			everything().with(ReportOption.INDEX_YEARS, false)).generate();

		Assertions.assertTrue(all(with).contains("| Years"));
		Assertions.assertFalse(all(without).contains("Years"));
	}


	/* ----- Bold and cross references --------------------------------------- */

	@Test
	void aMentionOfADirectAncestorIsInBoldAndHasTheNumberInBrackets(){
		final ReportConfig config = everything().with(ReportOption.BOLD_DIRECT_ANCESTORS, true).with(ReportOption.CROSS_REFERENCES, true);
		final ReportContext ctx = ReportContext.build(model, model.getRecordById("I1"), config);
		ctx.useMarks(PersonMarks.of(ctx, ReportType.ANCESTOR));

		//(Giovanni is the number 2, an ancestor of Maria; Sara is a descendant: not in bold, no number in an ancestor report)
		Assertions.assertEquals("**Giovanni Battista Rossi** [2]", ctx.mention(model.getRecordById("I2"), false));
		Assertions.assertEquals("Sara Neri", ctx.mention(model.getRecordById("I7"), false));
		//(the person the report is about is not a reference to the person)
		Assertions.assertEquals("Maria Rossi", ctx.mention(model.getRecordById("I1"), false));
	}

	@Test
	void withoutTheOptionsAMentionIsTheName(){
		final ReportContext ctx = ReportContext.build(model, model.getRecordById("I1"), everything());
		ctx.useMarks(PersonMarks.of(ctx, ReportType.ANCESTOR));

		Assertions.assertEquals("Giovanni Battista Rossi", ctx.mention(model.getRecordById("I2"), false));
	}

	@Test
	void theAncestorsAreInBoldInTheRosterOfTheReport(){
		final ReportDocument document = new AncestorReportGenerator(model, model.getRecordById("I1"),
			everything().with(ReportOption.BOLD_DIRECT_ANCESTORS, true)).generate();

		Assertions.assertTrue(texts(document).stream().anyMatch(text -> text.contains("**[I2](@I2@)**")), texts(document).toString());
	}


	/* ----- Names ----------------------------------------------------------- */

	@Test
	void theGivenNamesAreAllInFullOrTheFirstAndTheInitials(){
		final FLEFRecord giovanni = model.getRecordById("I2");

		Assertions.assertEquals("Giovanni Battista Rossi", PersonNames.plain(giovanni, GivenNameStyle.ALL, ""));
		Assertions.assertEquals("Giovanni B. Rossi", PersonNames.plain(giovanni, GivenNameStyle.FIRST_AND_INITIALS, ""));
		Assertions.assertEquals("Giovanni B. Rossi [I2]", PersonNames.display(giovanni, GivenNameStyle.FIRST_AND_INITIALS, ""));
		//(one given name is left alone)
		Assertions.assertEquals("Maria Rossi", PersonNames.plain(model.getRecordById("I1"), GivenNameStyle.FIRST_AND_INITIALS, ""));
	}

	@Test
	void aCompoundGivenNameHasTheInitialOfEachPart(){
		model.addRecord(person("I10", "Anna Jean-Paul Maria", "Rossi", "female"));

		Assertions.assertEquals("Anna J.-P. M. Rossi", PersonNames.plain(model.getRecordById("I10"), GivenNameStyle.FIRST_AND_INITIALS, ""));
	}

	@Test
	void aPersonWithNoNameIsTheUnknownNameOrTheIdentifier(){
		final FLEFRecord nobody = model.getRecordById("I9");

		Assertions.assertEquals("I9", PersonNames.plain(nobody, GivenNameStyle.ALL, ""));
		Assertions.assertEquals("?", PersonNames.plain(nobody, GivenNameStyle.ALL, "?"));
		Assertions.assertEquals("(unknown) [I9]", PersonNames.display(nobody, GivenNameStyle.ALL, "(unknown)"));
		Assertions.assertEquals("[I9]", PersonNames.display(nobody, GivenNameStyle.ALL, ""));
	}

	@Test
	void theReportsWriteTheNamesAsTheOptionsSay(){
		final ReportConfig config = everything().with(ReportOption.GIVEN_NAMES, GivenNameStyle.FIRST_AND_INITIALS);
		final ReportContext ctx = ReportContext.build(model, model.getRecordById("I1"), config);

		Assertions.assertEquals("Giovanni B. Rossi [I2]", ctx.displayText(model.getRecordById("I2")));
		Assertions.assertEquals("Giovanni B. Rossi", ctx.narrativeName(model.getRecordById("I2")));
	}


	/* ----- Placement of the notes ------------------------------------------ */

	private static final String LONG_NOTE = "First line of the note.\nSecond line.\nThird line.\nFourth line.";

	private ReportDocument lifeOf(final String personId, final ReportConfig config){
		return new ReportGenerator(model, model.getRecordById(personId), config, ReportType.INDIVIDUAL).generate();
	}

	@Test
	void theNotesAreInlineByDefault(){
		event("I1", EventReader.ENUM_TYPE_BIRTH, "1860-01-01", note("A short note."));

		final String text = all(lifeOf("I1", everything()));

		Assertions.assertTrue(text.contains("**Note:** A short note."), text);
		Assertions.assertFalse(text.contains("[Note 1]"), text);
	}

	@Test
	void aNoteInTheSectionLeavesItsNumberWhereItBelongs(){
		event("I1", EventReader.ENUM_TYPE_BIRTH, "1860-01-01", note("A short note."));

		final List<String> texts = texts(lifeOf("I1", everything().with(ReportOption.NOTES_PLACEMENT, TextPlacement.SECTION)));

		Assertions.assertTrue(texts.contains("*[Note 1]*"), texts.toString());
		Assertions.assertTrue(texts.contains("**Note 1:** A short note."), texts.toString());
		Assertions.assertFalse(texts.contains("**Note:** A short note."), texts.toString());
		//the notes of the section are after the section heading
		Assertions.assertTrue(texts.indexOf("Notes") < texts.indexOf("**Note 1:** A short note."), texts.toString());
	}

	@Test
	void onlyTheLongNotesGoToTheSectionWhenTheyAreAskedTo(){
		event("I1", EventReader.ENUM_TYPE_BIRTH, "1860-01-01", note("A short note."), note(LONG_NOTE));
		final ReportConfig config = everything().with(ReportOption.NOTES_PLACEMENT, TextPlacement.LONG_IN_SECTION)
			.with(ReportOption.LONG_TEXT_LINES, 2);

		final List<String> texts = texts(lifeOf("I1", config));

		Assertions.assertTrue(texts.contains("**Note:** A short note."), texts.toString());
		Assertions.assertTrue(texts.contains("*[Note 1]*"), texts.toString());
		Assertions.assertTrue(texts.stream().anyMatch(text -> text.startsWith("**Note 1:** First line")), texts.toString());
		Assertions.assertFalse(texts.stream().anyMatch(text -> text.startsWith("**Note:** First line")), texts.toString());
	}

	@Test
	void aTextIsLongByTheLinesItTakes(){
		Assertions.assertEquals(0, CitationRenderer.lines(""));
		Assertions.assertEquals(1, CitationRenderer.lines("one line"));
		Assertions.assertEquals(3, CitationRenderer.lines("a\nb\nc"));
		//(a line of 200 characters takes three lines of the page)
		Assertions.assertEquals(3, CitationRenderer.lines("x".repeat(200)));
		Assertions.assertFalse(TextPlacement.LONG_IN_SECTION.inSection(3, 3));
		Assertions.assertTrue(TextPlacement.LONG_IN_SECTION.inSection(4, 3));
		Assertions.assertFalse(TextPlacement.INLINE.inSection(100, 3));
		Assertions.assertTrue(TextPlacement.SECTION.inSection(1, 3));
	}

	@Test
	void theNotesOfAGroupOrAnotherReportStayWhereTheyAre(){
		//(only the chapter of a person has a section for the notes: a report that is not about a person never moves them)
		event("I1", EventReader.ENUM_TYPE_BIRTH, "1860-01-01", note("A short note."));

		final String text = all(new ReportGenerator(model, model.getRecordById("I1"), everything()
			.with(ReportOption.NOTES_PLACEMENT, TextPlacement.SECTION), ReportType.RELATIONSHIP).generate());

		Assertions.assertFalse(text.contains("[Note 1]"), text);
	}


	/* ----- Placement of the sources ---------------------------------------- */

	private void twoSources(){
		source("S1", "Parish register");
		source("S2", "Census");
		event("I1", EventReader.ENUM_TYPE_BIRTH, "1860-01-01",
			citation("S1", "folio 12", "Baptised today.\nGodfather: Pietro.\nSecond line.", true),
			citation("S2", "page 7", null, false));
	}

	@Test
	void theCitationsHaveTheLocatorAndTheTextByDefault(){
		twoSources();

		final String text = all(lifeOf("I1", everything()));

		Assertions.assertTrue(text.contains("Parish register [S1] — *Locator:* folio 12"), text);
		Assertions.assertTrue(text.contains("Baptised today."), text);
		Assertions.assertTrue(text.contains("Census [S2]"), text);
	}

	@Test
	void theLocatorCanBeHidden(){
		twoSources();

		final String text = all(lifeOf("I1", everything().with(ReportOption.SOURCE_LOCATOR, false)));

		Assertions.assertFalse(text.contains("folio 12"), text);
		Assertions.assertFalse(text.contains("page 7"), text);
		Assertions.assertTrue(text.contains("Parish register [S1]"), text);
	}

	@Test
	void thePrincipalSourcesAreTheBestOnesOfWhatTheyCite(){
		twoSources();

		final String text = all(lifeOf("I1", everything().with(ReportOption.PRINCIPAL_SOURCES_ONLY, true)));

		//(the register is an original source with direct evidence; the census says nothing of it, and is not cited by the event any more: its
		//title is left only in the list of the sources of the person)
		Assertions.assertTrue(text.contains("Parish register [S1] — "), text);
		Assertions.assertFalse(text.contains("Census [S2] —"), text);
	}

	@Test
	void theTextOfASourceCanGoToTheSectionOfTheCitations(){
		twoSources();

		final List<String> texts = texts(lifeOf("I1", everything().with(ReportOption.SOURCES_PLACEMENT, TextPlacement.SECTION)));

		Assertions.assertTrue(texts.stream().anyMatch(text -> text.contains("Parish register [S1]") && text.contains("*(Extract 1)*")),
			texts.toString());
		Assertions.assertTrue(texts.stream().anyMatch(text -> text.startsWith("**Extract 1:** Parish register [S1]") && text.contains("Baptised today.")),
			texts.toString());
		//(the citation of the census has no text: it stays as it is)
		Assertions.assertTrue(texts.stream().anyMatch(text -> text.contains("Census [S2]") && !text.contains("(Extract")), texts.toString());
	}

	@Test
	void aShortTextOfASourceStaysInlineWhenOnlyTheLongOnesAreMoved(){
		source("S1", "Parish register");
		event("I1", EventReader.ENUM_TYPE_BIRTH, "1860-01-01", citation("S1", null, "Baptised.", false));
		final ReportConfig config = everything().with(ReportOption.SOURCES_PLACEMENT, TextPlacement.LONG_IN_SECTION)
			.with(ReportOption.LONG_TEXT_LINES, 2);

		final String text = all(lifeOf("I1", config));

		Assertions.assertTrue(text.contains("*Extract:* \"Baptised.\""), text);
		Assertions.assertFalse(text.contains("(Extract 1)"), text);
	}

	@Test
	void theWeightOfACitationCountsTheOriginalPrimaryAndDirectEvidence(){
		final FLEFRecord plain = citation("S1", null, null, false);
		final FLEFRecord strong = citation("S1", null, null, true);

		Assertions.assertEquals(1, SourceCitationReader.citationWeight(plain));
		Assertions.assertEquals(3, SourceCitationReader.citationWeight(strong));
	}

}
