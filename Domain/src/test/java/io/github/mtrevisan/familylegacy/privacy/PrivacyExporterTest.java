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
package io.github.mtrevisan.familylegacy.privacy;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


class PrivacyExporterTest{

	private static final String AUDIT = "\taudit.creation.date 2026-01-01T00:00:00Z\n";
	private static final String PUBLIC_INDIVIDUAL = "\tprivacy.level public\n";


	@BeforeAll
	static void loadGrammar() throws IOException{
		// the grammar initializes the index of the Xref paths
		try(final InputStream is = PrivacyExporterTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static FLEFModel parse(final String header, final String records){
		return new FLEFParser().parse("header {\n"
			+ "\tprotocol {\n\t\tname Family LEgacy Format\n\t\tversion 0.2.0\n\t}\n"
			+ "\tdate 2026-01-01T00:00:00Z\n" + header + "}\n" + records);
	}

	private static FLEFModel parse(final String records){
		return parse("", records);
	}

	private static String record(final String type, final String id, final String body){
		return type + StringUtils.SPACE + id + " {\n" + body + AUDIT + "}\n";
	}


	@Test
	void hiddenRecordsAreOmittedAndTheModelIsUntouched(){
		final FLEFModel model = parse(record("individual", "I1", PUBLIC_INDIVIDUAL)
			+ record("individual", "I2", "\tprivacy.level confidential\n")
			+ record("relationship", "R1", "\tsubject.individual I1\n\ttarget.individual I2\n\ttype friend\n"));

		final PrivacyExporter.Result result = PrivacyExporter.filter(model, PrivacyPolicy.HIDE_CONFIDENTIAL);

		Assertions.assertNotNull(result.model().getRecordById("I1"));
		Assertions.assertNull(result.model().getRecordById("I2"));
		//the relationship inherits the level of the confidential individual
		Assertions.assertNull(result.model().getRecordById("R1"));
		Assertions.assertEquals(2, result.omittedRecords());
		//the model is unchanged
		Assertions.assertNotNull(model.getRecordById("I2"));
		Assertions.assertNotNull(model.getRecordById("R1"));
	}

	@Test
	void restrictedRecordsAreKeptUnderHideConfidential(){
		final FLEFModel model = parse(record("individual", "I1", "\tprivacy.level restricted\n"));

		Assertions.assertNotNull(PrivacyExporter.filter(model, PrivacyPolicy.HIDE_CONFIDENTIAL)
			.model().getRecordById("I1"));
		Assertions.assertNull(PrivacyExporter.filter(model, PrivacyPolicy.HIDE_RESTRICTED_AND_CONFIDENTIAL)
			.model().getRecordById("I1"));
	}

	@Test
	void showAllReturnsTheModelItself(){
		final FLEFModel model = parse(record("individual", "I1", "\tprivacy.level confidential\n"));

		final PrivacyExporter.Result result = PrivacyExporter.filter(model, PrivacyPolicy.SHOW_ALL);

		Assertions.assertSame(model, result.model());
		Assertions.assertEquals(0, result.omittedRecords());
	}

	@Test
	void referencesToOmittedRecordsAreRemovedFromTheKeptOnes(){
		final FLEFModel model = parse(record("place", "P1", "\tprivacy.level confidential\n")
			+ record("event", "E1", "\ttype birth\n\tplace P1\n\tdescription At home\n"));

		final PrivacyExporter.Result result = PrivacyExporter.filter(model, PrivacyPolicy.HIDE_CONFIDENTIAL);

		final FLEFRecord event = result.model().getRecordById("E1");
		Assertions.assertNotNull(event);
		Assertions.assertNull(FLEFRecordHelper.findChild(event, "place"));
		Assertions.assertEquals("At home", FLEFRecordHelper.getChildValue(event, "description"));
		Assertions.assertTrue(result.omittedNodes() >= 1);
		//the original keeps its reference
		Assertions.assertNotNull(FLEFRecordHelper.findChild(model.getRecordById("E1"), "place"));
	}

	@Test
	void nestedStructureWithHiddenPrivacyIsRemoved(){
		final FLEFModel model = parse(record("individual", "I1", PUBLIC_INDIVIDUAL
			+ "\tsource {\n\t\tlocator page 3\n\t\tprivacy.level restricted\n\t}\n"
			+ "\tsource {\n\t\tlocator page 4\n\t}\n"));

		final FLEFRecord individual = PrivacyExporter.filter(model, PrivacyPolicy.HIDE_RESTRICTED_AND_CONFIDENTIAL)
			.model().getRecordById("I1");

		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(individual, "source").size());
	}

	@Test
	void confidentialLinksAndLinksToOmittedRecordsAreRedacted(){
		final FLEFModel model = parse(record("individual", "I1", PUBLIC_INDIVIDUAL
				+ "\tnote.text He was [born at home](flef:confidential) near [Mario](flef:I2) and [Anna](flef:I3).\n")
			+ record("individual", "I2", "\tprivacy.level confidential\n")
			+ record("individual", "I3", PUBLIC_INDIVIDUAL));

		final FLEFRecord individual = PrivacyExporter.filter(model, PrivacyPolicy.HIDE_CONFIDENTIAL)
			.model().getRecordById("I1");

		Assertions.assertEquals("He was  near [omitted] and [Anna](flef:I3).",
			FLEFRecordHelper.getChildValue(individual, "note.text"));
	}

	@Test
	void submitterContactsAreOmittedUnlessPublic(){
		final FLEFModel model = parse("\tsubmitter {\n\t\tcontact {\n\t\t\tvalue a@example.org\n\t\t}\n"
			+ "\t\tcontact {\n\t\t\tvalue b@example.org\n\t\t\tprivacy.level public\n\t\t}\n\t}\n", "");

		final FLEFRecord submitter = FLEFRecordHelper.findChild(PrivacyExporter.filter(model, PrivacyPolicy.HIDE_CONFIDENTIAL)
			.model().getHeader(), "submitter");

		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(submitter, "contact").size());
		Assertions.assertEquals("b@example.org", FLEFRecordHelper.getChildValue(submitter, "contact.value"));
		Assertions.assertEquals(2, FLEFRecordHelper.findChildren(FLEFRecordHelper.findChild(model.getHeader(), "submitter"),
			"contact").size());
	}

}
