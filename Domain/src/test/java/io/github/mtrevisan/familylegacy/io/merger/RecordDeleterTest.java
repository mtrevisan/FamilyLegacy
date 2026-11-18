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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/** Deleting a SourceRecord deletes its ExtractRecords, and the references to them in SourceCitation.extract are removed. */
class RecordDeleterTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = RecordDeleterTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static FLEFModel model(){
		return new FLEFParser().parse("source S1 {\n\ttitle.value One\n" + AUDIT + "}\n"
			+ "source S2 {\n\ttitle.value Two\n\tderived_from {\n\t\tsource S1\n\t\textract X1\n\t\tstance supports\n\t}\n" + AUDIT + "}\n"
			+ "extract X1 {\n\tsource S1\n\ttext Born\n" + AUDIT + "}\n"
			+ "extract X2 {\n\tsource S1\n\ttext Died\n" + AUDIT + "}\n"
			+ "extract X3 {\n\tsource S2\n\ttext Married\n" + AUDIT + "}\n"
			+ "individual I1 {\n"
			+ "\tsource {\n\t\tsource S1\n\t\textract X1\n\t\textract X2\n\t\tstance supports\n\t}\n"
			+ "\tsource {\n\t\tsource S2\n\t\textract X3\n\t\tstance supports\n\t}\n"
			+ AUDIT + "}\n");
	}

	private static List<String> citedSources(final FLEFRecord record, final String tag){
		return FLEFRecordHelper.findChildren(record, tag).stream()
			.map(SourceCitationReader::extractSource)
			.toList();
	}


	@Test
	void deletingASourceDeletesItsExtractsAndItsCitations(){
		final FLEFModel model = model();

		final List<String> deleted = new RecordDeleter(grammar).delete(model, "S1");

		Assertions.assertEquals("S1", deleted.getFirst());
		Assertions.assertTrue(deleted.containsAll(List.of("X1", "X2")), deleted.toString());
		Assertions.assertNull(model.getRecordById("S1"));
		Assertions.assertNull(model.getRecordById("X1"));
		Assertions.assertNull(model.getRecordById("X2"));
		//the extract of the other source stays, and so does its citation
		Assertions.assertNotNull(model.getRecordById("X3"));
		final FLEFRecord individual = model.getRecordById("I1");
		Assertions.assertEquals(List.of("S2"), citedSources(individual, SourceCitationReader.TAG_SOURCE));
		Assertions.assertEquals(List.of("X3"), FLEFRecordHelper.findChildren(individual, "source.extract").stream()
			.map(FLEFRecord::getValue)
			.toList());
		//a source derived from the deleted one loses that derivation, not itself
		Assertions.assertNotNull(model.getRecordById("S2"));
		Assertions.assertTrue(citedSources(model.getRecordById("S2"), "derived_from").isEmpty());
	}

	@Test
	void deletingAnExtractRemovesItsReferencesFromTheCitations(){
		final FLEFModel model = model();

		Assertions.assertEquals(List.of("X1"), new RecordDeleter(grammar).delete(model, "X1"));

		final FLEFRecord individual = model.getRecordById("I1");
		Assertions.assertEquals(List.of("S1", "S2"), citedSources(individual, SourceCitationReader.TAG_SOURCE));
		Assertions.assertEquals(List.of("X2", "X3"), FLEFRecordHelper.findChildren(individual, "source.extract").stream()
			.map(FLEFRecord::getValue)
			.toList());
		Assertions.assertTrue(FLEFRecordHelper.findChildren(model.getRecordById("S2"), "derived_from.extract").isEmpty());
	}

	@Test
	void deletingARecordThatIsNotThereDeletesNothing(){
		Assertions.assertTrue(new RecordDeleter(grammar).delete(model(), "S9").isEmpty());
	}

}
