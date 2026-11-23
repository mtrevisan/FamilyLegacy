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
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The "Kinship" report: the blood relatives of a person, by civil degree, each one once. */
class KinshipReportTest{

	/** (A style of the given names other than the default makes the report write the names itself: the service that names a record is in Core.) */
	private static ReportConfig everything(){
		return ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW).with(ReportOption.GIVEN_NAMES, GivenNameStyle.FIRST_AND_INITIALS);
	}


	private FLEFModel model;
	private int sequence;


	/**
	 * Mario (I1) is the child of Anna (I2) and Luca (I3), and has a son, Teo (I8). Anna has another child, Paolo (I5), and is the child of
	 * Carlo (I4), who has another child, Rita (I6); Rita has a daughter, Gina (I7), a first cousin of Mario. Dino (I9) is adopted by Mario.
	 */
	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		sequence = 0;
		for(final String[] person : new String[][]{{"I1", "Mario"}, {"I2", "Anna"}, {"I3", "Luca"}, {"I4", "Carlo"}, {"I5", "Paolo"},
				{"I6", "Rita"}, {"I7", "Gina"}, {"I8", "Teo"}, {"I9", "Dino"}})
			model.addRecord(person(person[0], person[1]));
		child("I1", "I2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		child("I1", "I3", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		child("I5", "I2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		child("I2", "I4", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		child("I6", "I4", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		child("I7", "I6", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		child("I8", "I1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		child("I9", "I1", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD);
	}


	private static FLEFRecord person(final String id, final String given){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", given))));
	}

	private void child(final String childId, final String parentId, final String type){
		sequence ++;
		model.addRecord(FLEFRecord.createMainRecord("R" + sequence, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type)));
	}

	private ReportDocument report(final String rootId, final int generations){
		return new KinshipReportGenerator(model, model.getRecordById(rootId), everything().with(ReportOption.GENERATIONS, generations)).generate();
	}

	private static List<ReportSection.Table> tables(final ReportDocument document){
		return document.sections().stream()
			.filter(ReportSection.Table.class::isInstance)
			.map(ReportSection.Table.class::cast)
			.toList();
	}

	/** @return	The names of the rows of a table. */
	private static List<String> names(final ReportSection.Table table){
		return table.rows().stream()
			.map(row -> row.get(1))
			.toList();
	}

	private static boolean hasParagraph(final ReportDocument document, final String start){
		return document.sections().stream()
			.anyMatch(section -> section instanceof final ReportSection.Paragraph paragraph && paragraph.text().startsWith(start));
	}


	@Test
	void theRelativesAreInTablesByCivilDegree(){
		final ReportDocument document = report("I1", 0);

		Assertions.assertEquals("Kinship", document.title());
		final List<ReportSection.Table> tables = tables(document);
		//1: Anna, Luca, Teo; 2: Carlo, Paolo; 3: Rita; 4: Gina
		Assertions.assertEquals(4, tables.size(), document.sections().toString());
		Assertions.assertEquals(List.of("Kinship", "Name", "Born", "Died"), tables.getFirst().headers());
		Assertions.assertEquals(3, tables.get(0).rows().size(), tables.get(0).rows().toString());
		Assertions.assertTrue(names(tables.get(0)).stream().anyMatch(name -> name.contains("Anna")), tables.get(0).rows().toString());
		Assertions.assertTrue(names(tables.get(0)).stream().anyMatch(name -> name.contains("Luca")), tables.get(0).rows().toString());
		Assertions.assertTrue(names(tables.get(0)).stream().anyMatch(name -> name.contains("Teo")), tables.get(0).rows().toString());
		Assertions.assertEquals(2, tables.get(1).rows().size(), tables.get(1).rows().toString());
		Assertions.assertTrue(names(tables.get(1)).stream().anyMatch(name -> name.contains("Carlo")), tables.get(1).rows().toString());
		Assertions.assertTrue(names(tables.get(1)).stream().anyMatch(name -> name.contains("Paolo")), tables.get(1).rows().toString());
		Assertions.assertTrue(names(tables.get(2)).getFirst().contains("Rita"), tables.get(2).rows().toString());
		Assertions.assertTrue(names(tables.get(3)).getFirst().contains("Gina"), tables.get(3).rows().toString());
		Assertions.assertTrue(tables.get(3).rows().getFirst().getFirst().contains("cousin"), tables.get(3).rows().toString());
		Assertions.assertTrue(hasParagraph(document, "Blood relatives: 7, in 4 degrees"), document.sections().toString());
	}

	@Test
	void anAdoptedChildIsNotABloodRelative(){
		Assertions.assertTrue(tables(report("I1", 0)).stream()
			.flatMap(table -> names(table).stream())
			.noneMatch(name -> name.contains("Dino")));
	}

	@Test
	void theGenerationsLimitHowFarTheCommonAncestorsAre(){
		//one generation: the parents, the siblings and the children (Carlo is a generation farther)
		final List<ReportSection.Table> tables = tables(report("I1", 1));

		final List<String> names = tables.stream()
			.flatMap(table -> names(table).stream())
			.toList();
		Assertions.assertEquals(4, names.size(), names.toString());
		Assertions.assertTrue(names.stream().noneMatch(name -> name.contains("Carlo") || name.contains("Rita") || name.contains("Gina")),
			names.toString());
	}

	@Test
	void aRelativeWithTwoCommonAncestorsIsListedOnce(){
		//Paolo is also a child of Luca: a full brother of Mario, reached through two ancestors
		child("I5", "I3", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);

		final long times = tables(report("I1", 0)).stream()
			.flatMap(table -> names(table).stream())
			.filter(name -> name.contains("Paolo"))
			.count();

		Assertions.assertEquals(1, times);
	}

	@Test
	void aSubjectWithNoRelativesSaysSo(){
		model.addRecord(person("I10", "Solo"));
		final ReportDocument document = report("I10", 0);

		Assertions.assertTrue(hasParagraph(document, "No blood relative is known"), document.sections().toString());
		Assertions.assertTrue(tables(document).isEmpty());
	}

	@Test
	void theReportIsAboutAPersonAndHasFewOptions(){
		Assertions.assertFalse(ReportType.KINSHIP.isGlobal());
		final FLEFRecord root = model.getRecordById("I1");
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.KINSHIP, root, ReportOption.GENERATIONS));
		Assertions.assertEquals(OptionState.EDITABLE, ReportBuilder.optionState(ReportType.KINSHIP, root, ReportOption.HEADER));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.KINSHIP, root, ReportOption.NOTES));
		Assertions.assertEquals(OptionState.HIDDEN, ReportBuilder.optionState(ReportType.KINSHIP, root, ReportOption.COLLATERAL_SCOPE));
	}

}
