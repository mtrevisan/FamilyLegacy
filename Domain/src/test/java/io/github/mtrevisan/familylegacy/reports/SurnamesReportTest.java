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
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;


/** The "Surnames by Place" report: how the surnames are counted by place and period, and how the report writes them. */
class SurnamesReportTest{

	/** The defaults, showing the individuals that may be living (the fixtures have no death and no birth long ago). */
	private static ReportConfig everything(){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW);
	}


	private FLEFModel model;
	private int sequence;


	/**
	 * Borgo (P1) is part of Treviso (P2); Padova is P3. Mario, Anna and Luca Masutti and Zeno masutti come from Borgo (Zeno with no
	 * date of birth), Carlo Mazutti from Padova, Piero de Marco from nowhere; Elena has
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
		event("I3", EventReader.ENUM_TYPE_BIRTH, "P1", "1870-05-01");
		event("I4", EventReader.ENUM_TYPE_BIRTH, "P3", "1900-01-01");
		event("I7", EventReader.ENUM_TYPE_BIRTH, "P1", null);
	}


	private static FLEFRecord place(final String id, final String name){
		return FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", name)));
	}

	private static FLEFRecord place(final String id, final String name, final String type){
		return place(id, name)
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_TYPE, type));
	}

	private static FLEFRecord partOf(final String id, final String child, final String parent){
		return FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, child)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, parent)));
	}

	private SurnameDistribution.Result distribution(final SurnameDistribution.Options options){
		return SurnameDistribution.of(model, RelationIndex.build(model, r -> true), record -> true, Locale.ENGLISH, options);
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
		//Zeno has the place of the birth and no year
		Assertions.assertEquals(1850, masutti.firstYear());
		Assertions.assertEquals(1870, masutti.lastYear());
		Assertions.assertEquals(1, masutti.undated());
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
		Assertions.assertEquals(Arrays.asList("P1", "P3", null), result.places().stream().map(SurnameDistribution.PlaceGroup::placeId).toList());
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
		final ReportDocument document = new SurnamesGenerator(model, everything()).generate();

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
				Assertions.assertEquals(List.of("Masutti", "4", "1850–1870", "1"), table.rows().getFirst());
				return;
			}
		}
		Assertions.fail("no table for Borgo");
	}

	@Test
	void anEmptyFileAndAFileWithoutSurnamesGiveAReport(){
		Assertions.assertFalse(new SurnamesGenerator(new FLEFModel(), everything()).generate().sections().isEmpty());

		final FLEFModel nobody = new FLEFModel();
		nobody.addRecord(person("I1", null, "Elena", null));
		final ReportDocument document = new SurnamesGenerator(nobody, everything()).generate();
		Assertions.assertTrue(document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Paragraph paragraph && paragraph.text().startsWith("No individual has a surname")),
			document.sections().toString());
	}

	@Test
	void thePlacesCanBeGroupedByAHigherLevelOfTheHierarchy(){
		//Quinto (hamlet) is in Cornuda (town), which is in Treviso (P2), which is in Veneto (region) with Padova
		model.addRecord(place("P4", "Veneto", PlaceReader.ENUM_TYPE_REGION));
		model.addRecord(place("P5", "Cornuda", PlaceReader.ENUM_TYPE_TOWN));
		model.addRecord(place("P6", "Quinto", PlaceReader.ENUM_TYPE_HAMLET));
		model.addRecord(partOf("PR2", "P2", "P4"));
		model.addRecord(partOf("PR3", "P3", "P4"));
		model.addRecord(partOf("PR4", "P5", "P2"));
		model.addRecord(partOf("PR5", "P6", "P5"));
		model.addRecord(person("I8", null, "Rita", "Masutti"));
		event("I8", EventReader.ENUM_TYPE_BIRTH, "P6", "1880-01-01");

		final SurnameDistribution.Result region = distribution(new SurnameDistribution.Options(SurnamePlaceLevel.REGION,
			SurnamePeriod.WHOLE, false));
		Assertions.assertEquals(Arrays.asList("P4", null), region.places().stream().map(SurnameDistribution.PlaceGroup::placeId).toList());
		Assertions.assertEquals("Veneto", group(region, "P4").placeName());
		Assertions.assertEquals(2, group(region, "P4").rows().size());
		Assertions.assertEquals(6, group(region, "P4").individuals());

		//the hamlet of Quinto is in the town of Cornuda; Borgo has no town: it stays as it is
		final SurnameDistribution.Result town = distribution(new SurnameDistribution.Options(SurnamePlaceLevel.MUNICIPALITY,
			SurnamePeriod.WHOLE, false));
		Assertions.assertEquals(1, group(town, "P5").individuals());
		Assertions.assertEquals(4, group(town, "P1").individuals());
	}

	@Test
	void thePeriodCanBeSplitByCentury(){
		model.addRecord(person("I8", null, "Rita", "Masutti"));
		event("I8", EventReader.ENUM_TYPE_BIRTH, "P1", "1901-01-01");

		final SurnameDistribution.PlaceGroup borgo = group(distribution(new SurnameDistribution.Options(SurnamePlaceLevel.PLACE,
			SurnamePeriod.CENTURY, false)), "P1");

		//1850, 1860, 1870 (19th), 1901 (20th), and Zeno with no date
		Assertions.assertEquals(3, borgo.rows().size());
		Assertions.assertEquals(19, borgo.rows().get(0).century());
		Assertions.assertEquals(3, borgo.rows().get(0).individuals());
		Assertions.assertEquals(20, borgo.rows().get(1).century());
		Assertions.assertNull(borgo.rows().get(2).century());
		Assertions.assertEquals(1, borgo.rows().get(2).undated());
		Assertions.assertEquals(5, borgo.individuals());
		Assertions.assertEquals(18, SurnameDistribution.centuryOf(1800));
		Assertions.assertEquals(19, SurnameDistribution.centuryOf(1801));
		Assertions.assertEquals("XIX", SurnamesGenerator.roman(19));
		Assertions.assertEquals("XLIV", SurnamesGenerator.roman(44));
	}

	@Test
	void theSurnamesOfAllTheNamesCountAndAPersonCountsOnceInAPlace(){
		//Anna is also known by her maiden name
		final FLEFRecord anna = model.getRecordById("I2");
		anna.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
			.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
				.addChild(FLEFRecord.createChildWithTagAndValue("value", "Zanatta"))));

		final SurnameDistribution.Result first = distribution(new SurnameDistribution.Options(SurnamePlaceLevel.PLACE,
			SurnamePeriod.WHOLE, false));
		final SurnameDistribution.Result all = distribution(new SurnameDistribution.Options(SurnamePlaceLevel.PLACE,
			SurnamePeriod.WHOLE, true));

		Assertions.assertEquals(3, first.surnames());
		Assertions.assertEquals(4, all.surnames());
		Assertions.assertEquals(6, all.individuals());
		//(four people in Borgo, one of them in two surnames)
		Assertions.assertEquals(4, group(all, "P1").individuals());
		Assertions.assertEquals(2, group(all, "P1").rows().size());
	}

	@Test
	void similarSpellingsArePutSideBySide(){
		final List<SurnameDistribution.SimilarGroup> groups = SurnameDistribution.similarSpellings(distribution(), Locale.ENGLISH);

		//Masutti (4, in Borgo) and Mazutti (1, in Padova)
		Assertions.assertEquals(1, groups.size());
		final SurnameDistribution.SimilarGroup group = groups.getFirst();
		Assertions.assertEquals(List.of("Masutti", "Mazutti"), group.spellings().stream().map(SurnameDistribution.Spelling::surname).toList());
		Assertions.assertEquals(5, group.individuals());
		Assertions.assertEquals(List.of("Borgo, Treviso", "Padova"), group.places());

		final ReportDocument document = new SurnamesGenerator(model, everything().with(ReportOption.SURNAME_SIMILAR, true)).generate();
		Assertions.assertTrue(document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Table table
				&& table.rows().contains(List.of("Masutti (4), Mazutti (1)", "5", "Borgo, Treviso; Padova"))), document.sections().toString());
	}

	@Test
	void theReportTellsTheLevelAndTheCenturies(){
		final ReportDocument document = new SurnamesGenerator(model, everything()
			.with(ReportOption.SURNAME_PLACE_LEVEL, SurnamePlaceLevel.PROVINCE)
			.with(ReportOption.SURNAME_PERIOD, SurnamePeriod.CENTURY)).generate();

		Assertions.assertTrue(document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Paragraph paragraph && paragraph.text().contains("by the province")),
			document.sections().toString());
		Assertions.assertTrue(document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Table table
				&& table.rows().stream().anyMatch(row -> row.contains("XIX century (1850–1870)"))), document.sections().toString());
	}

	@Test
	void theReportIsAboutTheWholeFileAndOnlyTheHeaderIsAnOption(){
		Assertions.assertTrue(ReportType.SURNAMES.isGlobal());
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.SURNAMES, null, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.SURNAMES, null, ReportOption.NOTES));
	}

}
