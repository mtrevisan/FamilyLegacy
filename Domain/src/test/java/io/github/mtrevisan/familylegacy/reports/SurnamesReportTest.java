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
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;


/** The "Surnames by Place" report: how the surnames are counted by place and period, and how the report writes them. */
class SurnamesReportTest{

	private FLEFModel model;
	private int sequence;


	/**
	 * Borgo (P1) is part of Treviso (P2); Padova is P3. Mario, Anna and Luca Masutti and Zeno masutti come from Borgo (Luca by the baptism,
	 * Zeno by the birth for the place and by the baptism for the year), Carlo Mazutti from Padova, Piero de Marco from nowhere; Elena has
	 * no surname.
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		sequence = 0;
		model.addRecord(place("P1", "Borgo"));
		model.addRecord(place("P2", "Treviso"));
		model.addRecord(place("P3", "Padova"));
		model.addRecord(FLEFRecord.createMainRecord("PR1", PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, "P1")))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, "P2"))));

		model.addRecord(person("I1", null, "Mario", "Masutti"));
		model.addRecord(person("I2", null, "Anna", "Masutti"));
		model.addRecord(person("I3", null, "Luca", "Masutti"));
		model.addRecord(person("I4", null, "Carlo", "Mazutti"));
		model.addRecord(person("I5", "de", "Piero", "Marco"));
		model.addRecord(person("I6", null, "Elena", null));
		model.addRecord(person("I7", null, "Zeno", "masutti"));

		event("I1", EventReader.ENUM_TYPE_BIRTH, "P1", "1850-03-01");
		event("I2", EventReader.ENUM_TYPE_BIRTH, "P1", "1860-03-01");
		event("I3", EventReader.CUSTOM_TYPE_BAPTISM, "P1", "1870-05-01");
		event("I4", EventReader.ENUM_TYPE_BIRTH, "P3", "1900-01-01");
		event("I7", EventReader.ENUM_TYPE_BIRTH, "P1", null);
		event("I7", EventReader.CUSTOM_TYPE_CHRISTENING, "P3", "1880-01-01");
	}


	private static FLEFRecord place(final String id, final String name){
		return FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", name)));
	}

	private static FLEFRecord person(final String id, final String prefix, final String given, final String family){
		final FLEFRecord name = FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
			.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
				.addChild(FLEFRecord.createChildWithTagAndValue("value", given)));
		if(prefix != null)
			name.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY_PREFIX))
				.addChild(FLEFRecord.createChildWithTagAndValue("value", prefix)));
		if(family != null)
			name.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
				.addChild(FLEFRecord.createChildWithTagAndValue("value", family)));
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(name);
	}

	/** An event of the person, with a place and a date (any of the two can be {@code null}). */
	private void event(final String personId, final String type, final String placeId, final String date){
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
		if(placeId != null)
			event.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, placeId)));
		model.addRecord(event);
		model.addRecord(FLEFRecord.createMainRecord("EP" + sequence, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));
	}

	private SurnameDistribution.Result distribution(){
		return SurnameDistribution.of(model, RelationIndex.build(model, r -> true), record -> true, Locale.ENGLISH);
	}

	private SurnameDistribution.PlaceGroup group(final SurnameDistribution.Result result, final String placeId){
		return result.places().stream()
			.filter(group -> placeId == null? group.placeId() == null: placeId.equals(group.placeId()))
			.findFirst()
			.orElseThrow();
	}


	@Test
	void theSurnamesOfAPlaceAreCountedWithTheirPeriod(){
		final SurnameDistribution.Result result = distribution();

		//Elena has no surname; Masutti and masutti are the same, de Marco has its prefix
		Assertions.assertEquals(6, result.individuals());
		Assertions.assertEquals(3, result.surnames());

		final SurnameDistribution.PlaceGroup borgo = group(result, "P1");
		Assertions.assertEquals("Borgo, Treviso", borgo.placeName());
		Assertions.assertEquals(4, borgo.individuals());
		Assertions.assertEquals(1, borgo.rows().size());
		final SurnameDistribution.Row masutti = borgo.rows().getFirst();
		Assertions.assertEquals("Masutti", masutti.surname());
		Assertions.assertEquals(4, masutti.individuals());
		//Zeno takes the place of the birth, and the year of the christening
		Assertions.assertEquals(1850, masutti.firstYear());
		Assertions.assertEquals(1880, masutti.lastYear());
		Assertions.assertEquals(0, masutti.undated());
	}

	@Test
	void whoHasNoPlaceIsInTheLastGroupAndKeepsThePrefix(){
		final SurnameDistribution.Result result = distribution();

		final SurnameDistribution.PlaceGroup nowhere = result.places().getLast();
		Assertions.assertNull(nowhere.placeId());
		Assertions.assertEquals("de Marco", nowhere.rows().getFirst().surname());
		Assertions.assertNull(nowhere.rows().getFirst().firstYear());
		Assertions.assertEquals(1, nowhere.rows().getFirst().undated());
		//the places in alphabetical order: Borgo, Treviso; Padova
		Assertions.assertEquals(java.util.Arrays.asList("P1", "P3", null), result.places().stream().map(SurnameDistribution.PlaceGroup::placeId).toList());
	}

	@Test
	void aHiddenIndividualIsNotCounted(){
		final SurnameDistribution.Result result = SurnameDistribution.of(model, RelationIndex.build(model, r -> true),
			record -> !"I1".equals(record.getId()), Locale.ENGLISH);

		Assertions.assertEquals(5, result.individuals());
		Assertions.assertEquals(1850 + 10, group(result, "P1").rows().getFirst().firstYear());
	}

	@Test
	void theReportHasAGroupForEachPlaceAndATableOfSurnames(){
		final ReportDocument document = new SurnamesGenerator(model, ReportConfig.defaults()).generate();

		Assertions.assertEquals("Surnames by Place", document.title());
		final List<String> headings = document.sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(section -> ((ReportSection.Heading)section).text())
			.toList();
		Assertions.assertTrue(headings.contains("Borgo, Treviso (4)"), headings.toString());
		Assertions.assertTrue(headings.contains("Padova (1)"), headings.toString());
		Assertions.assertTrue(headings.contains("Place unknown (1)"), headings.toString());

		boolean found = false;
		for(final ReportSection section : document.sections()){
			if(section instanceof final ReportSection.Heading heading)
				found = "Borgo, Treviso (4)".equals(heading.text());
			else if(found && section instanceof final ReportSection.Table table){
				Assertions.assertEquals(List.of("Surname", "Individuals", "Period", "Undated"), table.headers());
				Assertions.assertEquals(List.of("Masutti", "4", "1850–1880", "—"), table.rows().getFirst());
				return;
			}
		}
		Assertions.fail("no table for Borgo");
	}

	@Test
	void anEmptyFileAndAFileWithoutSurnamesGiveAReport(){
		Assertions.assertFalse(new SurnamesGenerator(new FLEFModel(), ReportConfig.defaults()).generate().sections().isEmpty());

		final FLEFModel nobody = new FLEFModel();
		nobody.addRecord(person("I1", null, "Elena", null));
		final ReportDocument document = new SurnamesGenerator(nobody, ReportConfig.defaults()).generate();
		Assertions.assertTrue(document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Paragraph paragraph && paragraph.text().startsWith("No individual has a surname")),
			document.sections().toString());
	}

	@Test
	void theReportIsAboutTheWholeFileAndOnlyTheHeaderIsAnOption(){
		Assertions.assertTrue(ReportType.SURNAMES.isGlobal());
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.SURNAMES, null, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.SURNAMES, null, ReportOption.NOTES));
	}

}
