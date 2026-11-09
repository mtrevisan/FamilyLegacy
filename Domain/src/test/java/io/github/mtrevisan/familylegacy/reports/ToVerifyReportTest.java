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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/** The "To Verify" report: what the finder collects and how the report writes it. */
class ToVerifyReportTest{

	private FLEFModel model;


	/**
	 * Anna (I1): a dated birth E1 with no place and no source. Bruno (I2): no birth, and a citation of S1 that contradicts him. Carla (I3):
	 * a birth E2 with no date, with a place and a source. S1 is cited, S2 is not; the task T1 is open, T2 is done.
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		model.addRecord(individual("I1", "Anna"));
		model.addRecord(individual("I2", "Bruno")
			.addChild(citation("S1", SourceCitationReader.STANCE_CONTRADICTS)));
		model.addRecord(individual("I3", "Carla"));
		model.addRecord(named("P1", PlaceReader.TYPE, IndividualReader.TAG_NAME, "Treviso"));
		model.addRecord(named("S1", SourceReader.TYPE, SourceReader.TAG_TITLE, "Register of baptisms"));
		model.addRecord(named("S2", SourceReader.TYPE, SourceReader.TAG_TITLE, "A book nobody cites"));

		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(point("date", "1990-01-01")));
		model.addRecord(participation("EP1", "E1", "I1"));
		model.addRecord(FLEFRecord.createMainRecord("E2", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, "P1")))
			.addChild(citation("S1", SourceCitationReader.STANCE_SUPPORTS)));
		model.addRecord(participation("EP2", "E2", "I3"));

		model.addRecord(FLEFRecord.createMainRecord("T1", ResearchTaskReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, "Look in the parish of San Nicolò"))
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, ResearchTaskReader.ENUM_STATUS_OPEN)));
		model.addRecord(FLEFRecord.createMainRecord("T2", ResearchTaskReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, ResearchTaskReader.ENUM_STATUS_COMPLETED)));
	}


	private static FLEFRecord individual(final String id, final String name){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", name))));
	}

	private static FLEFRecord named(final String id, final String type, final String tag, final String name){
		return FLEFRecord.createMainRecord(id, type)
			.addChild(FLEFRecord.createChildWithTag(tag)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", name)));
	}

	private static FLEFRecord citation(final String sourceId, final String stance){
		return FLEFRecord.createChildWithTag(SourceCitationReader.TAG_SOURCE)
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_SOURCE, sourceId))
			.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, stance));
	}

	private static FLEFRecord point(final String tag, final String fullDate){
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("point")
					.addChild(FLEFRecord.createChildWithTag("value")
						.addChild(FLEFRecord.createChildWithTagAndValue("full_date", fullDate)))
					.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian"))));
	}

	private static FLEFRecord participation(final String id, final String eventId, final String individualId){
		return FLEFRecord.createMainRecord(id, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, individualId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL));
	}

	private Set<String> ids(final ToVerifyFinder.Kind kind){
		return ToVerifyFinder.find(model, record -> true, null).stream()
			.filter(finding -> finding.kind() == kind)
			.map(ToVerifyFinder.Finding::recordId)
			.collect(Collectors.toSet());
	}


	@Test
	void whatIsMissingFromTheLife(){
		Assertions.assertEquals(Set.of("I2", "I3"), ids(ToVerifyFinder.Kind.NO_BIRTH_DATE));
		Assertions.assertEquals(Set.of("E2"), ids(ToVerifyFinder.Kind.EVENT_NO_DATE));
		Assertions.assertEquals(Set.of("E1"), ids(ToVerifyFinder.Kind.EVENT_NO_PLACE));
		Assertions.assertEquals(Set.of("E1"), ids(ToVerifyFinder.Kind.EVENT_NO_SOURCE));
	}

	@Test
	void aContradictingCitationIsNotASupport(){
		Assertions.assertEquals(Set.of("I2"), ids(ToVerifyFinder.Kind.CONTRADICTING_CITATION));

		//E2 is backed by a supporting citation, a contradicting one would not be a backing
		final FLEFRecord event = model.getRecordById("E2");
		Assertions.assertTrue(SourceCitationReader.hasSupportingCitation(event));
		Assertions.assertFalse(ids(ToVerifyFinder.Kind.EVENT_NO_SOURCE).contains("E2"));
	}

	@Test
	void sourcesNobodyCitesAndTasksNotDone(){
		Assertions.assertEquals(Set.of("S2"), ids(ToVerifyFinder.Kind.SOURCE_NOT_CITED));
		Assertions.assertEquals(Set.of("T1"), ids(ToVerifyFinder.Kind.OPEN_TASK));
	}

	@Test
	void aHiddenRecordIsNotTold(){
		final List<ToVerifyFinder.Finding> findings = ToVerifyFinder.find(model, record -> !"I3".equals(record.getId())
			&& !"E1".equals(record.getId()), null);

		Assertions.assertTrue(findings.stream().noneMatch(finding -> "I3".equals(finding.recordId()) || "E1".equals(finding.recordId())));
		Assertions.assertTrue(findings.stream().anyMatch(finding -> "I2".equals(finding.recordId())));
	}

	@Test
	void theFindingsComeInTheOrderOfTheKinds(){
		final List<ToVerifyFinder.Kind> kinds = ToVerifyFinder.find(model, record -> true, null).stream()
			.map(ToVerifyFinder.Finding::kind)
			.toList();

		for(int i = 1; i < kinds.size(); i ++)
			Assertions.assertTrue(kinds.get(i - 1).compareTo(kinds.get(i)) <= 0, kinds.toString());
	}


	@Test
	void theReportHasAGroupForEachKindOfFinding(){
		final ReportDocument document = new ToVerifyGenerator(model, ReportConfig.defaults(), null).generate();

		Assertions.assertEquals("To Verify", document.title());
		final List<String> headings = document.sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(section -> ((ReportSection.Heading)section).text())
			.toList();
		Assertions.assertTrue(headings.contains("Individuals with no birth date (2)"), headings.toString());
		Assertions.assertTrue(headings.contains("Vital events with no date (1)"), headings.toString());
		Assertions.assertTrue(headings.contains("Vital events with no place (1)"), headings.toString());
		Assertions.assertTrue(headings.contains("Vital events with no source (1)"), headings.toString());
		Assertions.assertTrue(headings.contains("Sources that no record cites (1)"), headings.toString());
		Assertions.assertTrue(headings.contains("Citations that contradict the record (1)"), headings.toString());
		Assertions.assertTrue(headings.contains("Research tasks to do (1)"), headings.toString());
	}

	@Test
	void theTableOfAGroupListsItsRecordsAndTheDetail(){
		final ReportDocument document = new ToVerifyGenerator(model, ReportConfig.defaults(), null).generate();

		final List<List<String>> tasks = tableAfter(document, "Research tasks to do (1)");
		Assertions.assertEquals(1, tasks.size());
		Assertions.assertTrue(tasks.getFirst().get(0).contains("T1"), tasks.toString());
		Assertions.assertTrue(tasks.getFirst().get(1).contains("San Nicol"), tasks.toString());

		Assertions.assertEquals(2, tableAfter(document, "Individuals with no birth date (2)").size());
		//the contradicting citation tells which source contradicts
		Assertions.assertTrue(tableAfter(document, "Citations that contradict the record (1)").getFirst().get(1).contains("Register of baptisms"));
	}

	@Test
	void theReportAnswersWhenThereIsNothingToVerify(){
		final FLEFModel clean = new FLEFModel();
		clean.addRecord(individual("I1", "Anna"));
		clean.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(point("date", "1990-01-01"))
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, "P1")))
			.addChild(citation("S1", SourceCitationReader.STANCE_SUPPORTS)));
		clean.addRecord(participation("EP1", "E1", "I1"));
		clean.addRecord(named("P1", PlaceReader.TYPE, IndividualReader.TAG_NAME, "Treviso"));
		clean.addRecord(named("S1", SourceReader.TYPE, SourceReader.TAG_TITLE, "Register"));

		final ReportDocument document = new ToVerifyGenerator(clean, ReportConfig.defaults(), null).generate();

		Assertions.assertTrue(document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Paragraph paragraph && paragraph.text().startsWith("Nothing to verify")),
			document.sections().toString());
	}

	@Test
	void anEmptyFileGivesAnEmptyReport(){
		Assertions.assertFalse(new ToVerifyGenerator(new FLEFModel(), ReportConfig.defaults(), null).generate().sections().isEmpty());
	}

	@Test
	void theReportIsAboutTheWholeFileAndOnlyTheHeaderIsAnOption(){
		Assertions.assertTrue(ReportType.TO_VERIFY.isGlobal());
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.TO_VERIFY, null, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.TO_VERIFY, null, ReportOption.NOTES));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.TO_VERIFY, null, ReportOption.TIMELINE));
	}


	/** @return	The rows of the table that follows the heading with the text. */
	private static List<List<String>> tableAfter(final ReportDocument document, final String heading){
		boolean found = false;
		for(final ReportSection section : document.sections()){
			if(section instanceof final ReportSection.Heading h)
				found = heading.equals(h.text());
			else if(found && section instanceof final ReportSection.Table table)
				return table.rows();
		}
		throw new AssertionError("no table after " + heading);
	}

}
