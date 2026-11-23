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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;


/** The "Number of Ancestors" report: the places of the tree by generation, and how an ancestor by more than one line is counted. */
class PedigreeCollapseReportTest{

	/** (A style of the given names other than the default makes the report write the names itself: the service that names a record is in Core.) */
	private static ReportConfig everything(){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW).with(ReportOption.GIVEN_NAMES, GivenNameStyle.FIRST_AND_INITIALS);
	}


	private FLEFModel model;
	private int sequence;


	/**
	 * Mario (I1) is the child of Anna (I2) and Luca (I3). Anna is the child of Carlo (I4) and Elena (I5), Luca is the child of Carlo and
	 * Gina (I6): Carlo is the grandfather of Mario by two lines.
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		sequence = 0;
		model.addRecord(person("I1", "Mario"));
		model.addRecord(person("I2", "Anna"));
		model.addRecord(person("I3", "Luca"));
		model.addRecord(person("I4", "Carlo"));
		model.addRecord(person("I5", "Elena"));
		model.addRecord(person("I6", "Gina"));
		child("I1", "I2");
		child("I1", "I3");
		child("I2", "I4");
		child("I2", "I5");
		child("I3", "I4");
		child("I3", "I6");
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

	private ReportDocument report(final String rootId, final int generations){
		return new PedigreeCollapseGenerator(model, model.getRecordById(rootId), everything().with(ReportOption.GENERATIONS, generations))
			.generate();
	}

	private PedigreeCollapseGenerator.Result compute(final String rootId, final int generations){
		return PedigreeCollapseGenerator.compute(RelationIndex.build(model), model.getRecordById(rootId), generations);
	}

	private static List<ReportSection.Table> tables(final ReportDocument document){
		return document.sections().stream()
			.filter(ReportSection.Table.class::isInstance)
			.map(ReportSection.Table.class::cast)
			.toList();
	}

	private static boolean hasParagraph(final ReportDocument document, final String start){
		return document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Paragraph paragraph && paragraph.text().startsWith(start));
	}


	@Test
	void anAncestorByTwoLinesFillsTwoPlacesButIsOneIndividual(){
		final PedigreeCollapseGenerator.Result result = compute("I1", 0);

		Assertions.assertEquals(List.of(new PedigreeCollapseGenerator.Level(1, 2, 2, 2), new PedigreeCollapseGenerator.Level(2, 4, 4, 3)),
			result.levels());
		Assertions.assertEquals(6, result.known());
		Assertions.assertEquals(6, result.possible());
		Assertions.assertEquals(5, result.different());
		Assertions.assertEquals(1, result.repeated().size());
		Assertions.assertEquals("I4", result.repeated().getFirst().person().getId());
		Assertions.assertEquals(2, result.repeated().getFirst().times());
		Assertions.assertEquals(List.of(2), result.repeated().getFirst().generations());
	}

	@Test
	void theGenerationsAskedLimitTheCount(){
		final PedigreeCollapseGenerator.Result result = compute("I1", 1);

		Assertions.assertEquals(1, result.levels().size());
		Assertions.assertTrue(result.repeated().isEmpty());
	}

	@Test
	void aSubjectWithNoParentsHasNoGenerations(){
		Assertions.assertTrue(compute("I4", 0).levels().isEmpty());
		final ReportDocument document = report("I4", 0);

		Assertions.assertTrue(hasParagraph(document, "No ancestor is known"), document.sections().toString());
		Assertions.assertTrue(tables(document).isEmpty());
	}

	@Test
	void theReportHasATableOfTheGenerationsAndTheListOfTheRepeatedAncestors(){
		final ReportDocument document = report("I1", 0);

		Assertions.assertEquals("Number of Ancestors", document.title());
		final List<ReportSection.Table> tables = tables(document);
		Assertions.assertEquals(2, tables.size());
		Assertions.assertEquals(List.of("Generation", "Possible", "Known", "Known (%)", "Different", "Collapse"), tables.get(0).headers());
		Assertions.assertEquals(List.of("1", "2", "2", "100.0%", "2", "0.0%"), tables.get(0).rows().get(0));
		Assertions.assertEquals(List.of("2", "4", "4", "100.0%", "3", "25.0%"), tables.get(0).rows().get(1));
		Assertions.assertEquals(List.of("Name", "Lines", "Generations"), tables.get(1).headers());
		Assertions.assertTrue(tables.get(1).rows().getFirst().get(0).contains("Carlo"), tables.get(1).rows().toString());
		Assertions.assertEquals("2", tables.get(1).rows().getFirst().get(1));
		Assertions.assertEquals("2", tables.get(1).rows().getFirst().get(2));
		Assertions.assertTrue(hasParagraph(document, "Known ancestors: 6 of 6 possible, in 2 generations. Different individuals: 5."
			+ " Pedigree collapse: 16.7%."), document.sections().toString());
	}

	@Test
	void aTreeWithNoRepetitionHasNoCollapse(){
		model = new FLEFModel();
		model.addRecord(person("I1", "Mario"));
		model.addRecord(person("I2", "Anna"));
		model.addRecord(person("I3", "Luca"));
		sequence = 0;
		child("I1", "I2");
		child("I1", "I3");
		final ReportDocument document = report("I1", 0);

		Assertions.assertEquals(1, tables(document).size());
		Assertions.assertTrue(hasParagraph(document, "No individual is an ancestor by more than one line"), document.sections().toString());
	}

	@Test
	void thePossiblePlacesDoubleAtEveryGeneration(){
		Assertions.assertEquals("50.0%", PedigreeCollapseGenerator.percent(Locale.ENGLISH, 2, 4));
		Assertions.assertEquals("—", PedigreeCollapseGenerator.percent(Locale.ENGLISH, 0, 0));
	}

	@Test
	void theReportIsAboutAPersonAndHasFewOptions(){
		Assertions.assertFalse(ReportType.PEDIGREE_COLLAPSE.isGlobal());
		final FLEFRecord root = model.getRecordById("I1");
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.PEDIGREE_COLLAPSE, root, ReportOption.GENERATIONS));
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.PEDIGREE_COLLAPSE, root, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.PEDIGREE_COLLAPSE, root, ReportOption.NOTES));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.PEDIGREE_COLLAPSE, root, ReportOption.COLLATERAL_SCOPE));
	}

}
