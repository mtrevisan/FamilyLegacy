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
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The "Tagged Records" report: the records that carry each free tag. */
class TaggedRecordsReportTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		model.addRecord(individual("I1", "Anna"));
		model.addRecord(individual("I2", "Bruno"));
		model.addRecord(FLEFRecord.createMainRecord("P1", PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", "Treviso"))));
		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE));
	}

	private static FLEFRecord individual(final String id, final String name){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", name))));
	}

	private ReportDocument generate(){
		return new TaggedRecordsGenerator(model, ReportConfig.defaults().withLivingPrivacy(LivingPrivacy.SHOW)).generate();
	}

	private static List<String> headings(final ReportDocument document){
		return document.sections().stream()
			.filter(ReportSection.Heading.class::isInstance)
			.map(section -> ((ReportSection.Heading)section).text())
			.toList();
	}


	@Test
	void theRecordsAreListedUnderEachTagWithTheNumberOfThem(){
		model.tags().set("I1", List.of("Brick Wall", "To Verify"));
		model.tags().set("P1", List.of("brick wall"));
		model.tags().set("I2", List.of("To Verify"));

		final ReportDocument document = generate();

		Assertions.assertEquals("Tagged Records", document.title());
		final List<String> headings = headings(document);
		Assertions.assertTrue(headings.contains("Brick Wall (2)"), headings.toString());
		Assertions.assertTrue(headings.contains("To Verify (2)"), headings.toString());
		Assertions.assertTrue(document.sections().toString().contains("[Place, P1]"));
		Assertions.assertTrue(document.sections().toString().contains("[Individual, I1]"));
	}

	@Test
	void aRecordThatCannotBeTaggedOrIsNotThereIsNotListed(){
		model.tags().set("E1", List.of("Brick Wall"));
		model.tags().set("I99", List.of("Brick Wall"));

		final ReportDocument document = generate();

		Assertions.assertFalse(headings(document).stream().anyMatch(heading -> heading.startsWith("Brick Wall")), headings(document).toString());
	}

	@Test
	void withNoTagTheReportSaysSo(){
		final ReportDocument document = generate();

		Assertions.assertTrue(document.sections().toString().contains("No record has a tag"));
	}

}
