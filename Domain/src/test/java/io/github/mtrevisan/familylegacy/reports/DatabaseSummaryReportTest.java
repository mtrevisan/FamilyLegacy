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
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The "Database Summary" report: what is counted, and how the report writes it. */
class DatabaseSummaryReportTest{

	private static ReportConfig everything(){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW);
	}


	private FLEFModel model;
	private int sequence;


	/**
	 * Mario (I1) is the child of Anna (I2); both have a surname, Mario was born in 1850 and Anna's birth has no date, Anna died in 1900. An
	 * event of 1920 has no one in it.
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		sequence = 0;
		model.addRecord(person("I1", "Mario", "Masutti"));
		model.addRecord(person("I2", "Anna", "Masutti"));
		model.addRecord(FLEFRecord.createMainRecord("R1", RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, "I1")))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, "I2")))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)));
		event("I1", EventReader.ENUM_TYPE_BIRTH, "1850-03-01");
		event("I2", EventReader.ENUM_TYPE_BIRTH, null);
		event("I2", EventReader.ENUM_TYPE_DEATH, "1900-01-01");
		event(null, "census", "1920-01-01");
	}


	private static FLEFRecord person(final String id, final String given, final String family){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", given)))
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", family))));
	}

	/** An event, with a date (it can be {@code null}), of a person (it can be {@code null}: no one is in it). */
	private void event(final String personId, final String type, final String date){
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
		model.addRecord(event);
		if(personId != null)
			model.addRecord(FLEFRecord.createMainRecord("EP" + sequence, EventParticipationReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
					.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event.getId()))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));
	}

	private ReportDocument report(){
		return new DatabaseSummaryGenerator(model, everything()).generate();
	}

	private static ReportSection.Table table(final ReportDocument document, final String firstHeader, final String secondHeader){
		return document.sections().stream()
			.filter(ReportSection.Table.class::isInstance)
			.map(ReportSection.Table.class::cast)
			.filter(table -> table.headers().get(0).equals(firstHeader) && table.headers().get(1).equals(secondHeader))
			.findFirst()
			.orElseThrow();
	}

	private static String value(final ReportSection.Table table, final String row, final int column){
		return table.rows().stream()
			.filter(r -> r.get(0).equals(row))
			.findFirst()
			.orElseThrow()
			.get(column);
	}


	@Test
	void theRecordsAreCountedByKind(){
		final ReportSection.Table kinds = table(report(), "Kind", "Records");

		Assertions.assertEquals("2", value(kinds, "Individual", 1));
		Assertions.assertEquals("1", value(kinds, "Relationship", 1));
		Assertions.assertEquals("4", value(kinds, "Event", 1));
		//(the most numerous first)
		Assertions.assertEquals("Event", kinds.rows().getFirst().get(0));
	}

	@Test
	void theCompletenessOfTheIndividualsIsCountedAndSharedOut(){
		final ReportSection.Table completeness = table(report(), "Measure", "Individuals");

		Assertions.assertEquals(List.of("With a surname", "2", "100.0%"), completeness.rows().get(0));
		Assertions.assertEquals(List.of("With a birth year", "1", "50.0%"), List.of(completeness.rows().get(2).get(0),
			completeness.rows().get(2).get(1), completeness.rows().get(2).get(2)));
		Assertions.assertEquals("1", value(completeness, "With a death year", 1));
		Assertions.assertEquals("1", value(completeness, "With a known parent", 1));
		Assertions.assertEquals("0", value(completeness, "With a birth place", 1));
		Assertions.assertEquals("0", value(completeness, "With a source citation", 1));
	}

	@Test
	void thePeriodIsTheOneOfTheDatedEvents(){
		final ReportSection.Table period = table(report(), "Measure", "Value");

		Assertions.assertEquals("3 / 4 (75.0%)", value(period, "Events with a year", 1));
		Assertions.assertEquals("1850", value(period, "First year of an event", 1));
		Assertions.assertEquals("1920", value(period, "Last year of an event", 1));
		Assertions.assertEquals("1850", value(period, "First year of a birth", 1));
		Assertions.assertEquals("1850", value(period, "Last year of a birth", 1));
	}

	@Test
	void anEmptyFileAndAFileWithNoIndividualsGiveAReport(){
		final ReportDocument empty = new DatabaseSummaryGenerator(new FLEFModel(), everything()).generate();
		Assertions.assertEquals("Database Summary", empty.title());
		Assertions.assertFalse(empty.sections().isEmpty());

		final FLEFModel events = new FLEFModel();
		events.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, "census")));
		final ReportDocument document = new DatabaseSummaryGenerator(events, everything()).generate();
		Assertions.assertEquals("—", value(table(document, "Measure", "Value"), "First year of an event", 1));
		Assertions.assertTrue(document.sections().stream()
			.filter(ReportSection.Table.class::isInstance)
			.map(ReportSection.Table.class::cast)
			.noneMatch(table -> table.headers().get(1).equals("Individuals")));
	}

	@Test
	void theReportIsAboutTheWholeFileAndHasOnlyTheHeaderOption(){
		Assertions.assertTrue(ReportType.DATABASE_SUMMARY.isGlobal());
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.DATABASE_SUMMARY, null, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.DATABASE_SUMMARY, null, ReportOption.NOTES));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.DATABASE_SUMMARY, null, ReportOption.GENERATIONS));
	}

}
