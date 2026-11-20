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


/** The "End of Line" report: which ancestors have no known parents, and how far the generations asked reach. */
class EndOfLineReportTest{

	/** (A style of the given names other than the default makes the report write the names itself: the service that names a record is in Core.) */
	private static ReportConfig everything(){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW).with(ReportOption.GIVEN_NAMES, GivenNameStyle.FIRST_AND_INITIALS);
	}


	private FLEFModel model;
	private int sequence;


	/**
	 * Mario (I1) is the child of Anna (I2) and Luca (I3); Anna is the child of Carlo (I4). Luca and Carlo have no parents: Luca is at the
	 * end of the line in the first generation, Carlo in the second one.
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		sequence = 0;
		model.addRecord(person("I1", "Mario"));
		model.addRecord(person("I2", "Anna"));
		model.addRecord(person("I3", "Luca"));
		model.addRecord(person("I4", "Carlo"));
		child("I1", "I2");
		child("I1", "I3");
		child("I2", "I4");
		birth("I3", "1850-03-01");
		birth("I4", "1820-01-01");
	}


	private static FLEFRecord person(final String id, final String given){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", given))));
	}

	private void child(final String childId, final String parentId){
		sequence ++;
		model.addRecord(FLEFRecord.createMainRecord("R" + sequence, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)));
	}

	private void birth(final String personId, final String date){
		sequence ++;
		model.addRecord(FLEFRecord.createMainRecord("E" + sequence, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(FLEFRecord.createChildWithTag("date")
				.addChild(FLEFRecord.createChildWithTag("value")
					.addChild(FLEFRecord.createChildWithTag("point")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", date)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian"))))));
		model.addRecord(FLEFRecord.createMainRecord("EP" + sequence, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, "E" + sequence))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));
	}

	private ReportDocument report(final String rootId, final int generations){
		return new EndOfLineGenerator(model, model.getRecordById(rootId), everything().with(ReportOption.GENERATIONS, generations)).generate();
	}

	private static ReportSection.Table table(final ReportDocument document){
		return document.sections().stream()
			.filter(ReportSection.Table.class::isInstance)
			.map(ReportSection.Table.class::cast)
			.findFirst()
			.orElseThrow();
	}

	private static boolean hasParagraph(final ReportDocument document, final String start){
		return document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Paragraph paragraph && paragraph.text().startsWith(start));
	}


	@Test
	void theAncestorsWithNoKnownParentsAreListedByGeneration(){
		final ReportDocument document = report("I1", 0);

		Assertions.assertEquals("End of Line", document.title());
		final ReportSection.Table table = table(document);
		Assertions.assertEquals(List.of("Generation", "Name", "Born", "Place of birth", "Died", "Place of death"), table.headers());
		Assertions.assertEquals(2, table.rows().size());
		Assertions.assertEquals("1", table.rows().get(0).get(0));
		Assertions.assertTrue(table.rows().get(0).get(1).contains("Luca"), table.rows().toString());
		Assertions.assertTrue(table.rows().get(0).get(2).contains("1850"), table.rows().toString());
		Assertions.assertEquals("2", table.rows().get(1).get(0));
		Assertions.assertTrue(table.rows().get(1).get(1).contains("Carlo"), table.rows().toString());
		Assertions.assertTrue(hasParagraph(document, "Ancestors with no known parents: 2 (of 3 ancestors, in 2 generations)"),
			document.sections().toString());
	}

	@Test
	void anAncestorOfTheLastGenerationWithParentsIsNotAnEndOfTheLine(){
		//one generation: Anna is the last one covered, but her father is known
		final ReportSection.Table table = table(report("I1", 1));

		Assertions.assertEquals(1, table.rows().size());
		Assertions.assertTrue(table.rows().getFirst().get(1).contains("Luca"), table.rows().toString());
	}

	@Test
	void aSubjectWithNoParentsHasNoAncestors(){
		final ReportDocument document = report("I4", 0);

		Assertions.assertTrue(hasParagraph(document, "No ancestor is known"), document.sections().toString());
		Assertions.assertTrue(document.sections().stream().noneMatch(ReportSection.Table.class::isInstance));
	}

	@Test
	void aLineWhoseAncestorsAllHaveParentsSaysSo(){
		child("I3", "I4");
		final ReportDocument document = report("I1", 1);

		Assertions.assertTrue(hasParagraph(document, "Every one of the 2 ancestors"), document.sections().toString());
	}

	@Test
	void theReportIsAboutAPersonAndHasFewOptions(){
		Assertions.assertFalse(ReportType.END_OF_LINE.isGlobal());
		final FLEFRecord root = model.getRecordById("I1");
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.END_OF_LINE, root, ReportOption.GENERATIONS));
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.END_OF_LINE, root, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.END_OF_LINE, root, ReportOption.NOTES));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.END_OF_LINE, root, ReportOption.COLLATERAL_SCOPE));
	}

}
