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
package io.github.mtrevisan.familylegacy.io;

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParseException;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarValidator;
import io.github.mtrevisan.familylegacy.io.grammar.contraints.Constraint;
import io.github.mtrevisan.familylegacy.io.grammar.contraints.EveryConstraint;
import io.github.mtrevisan.familylegacy.io.grammar.contraints.InConstraint;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
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


/**
 * The extracts are records of their own: {@code require every e in extract: e.source == source} in SourceCitation,
 * {@code require every media_part.media in source.media} in ExtractRecord, and the migration of the extracts written as structures.
 */
class ExtractRecordTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = ExtractRecordTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static List<String> constraintErrors(final String flef){
		final FLEFModel model = new FLEFParser().parse(flef);
		return new FLEFValidator(grammar).validateSchema(model).stream()
			.filter(error -> error.contains("Constraint violation"))
			.toList();
	}

	private static String sources(){
		return "source S1 {\n\ttitle.value One\n\tmedia M1\n" + AUDIT + "}\n"
			+ "source S2 {\n\ttitle.value Two\n" + AUDIT + "}\n"
			+ "media M1 {\n\turi file:/a.png\n\tkind image\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi file:/b.png\n\tkind image\n" + AUDIT + "}\n";
	}

	private static String individualCiting(final String sourceId, final String extractId){
		return "individual I1 {\n\tsource {\n\t\tsource " + sourceId + "\n\t\textract " + extractId + "\n\t\tstance supports\n\t}\n" + AUDIT
			+ "}\n";
	}


	@Test
	void theGrammarHasTheConstraintsOfTheExtracts(){
		final List<Constraint> citation = ((StructType)grammar.getType("SourceCitation")).getConstraints();
		Assertions.assertTrue(citation.stream().anyMatch(c -> c instanceof EveryConstraint), citation.toString());
		Assertions.assertEquals("require every e in extract: e.source == source", citation.stream()
			.filter(c -> c instanceof EveryConstraint)
			.findFirst()
			.orElseThrow()
			.toString());

		final List<Constraint> extract = ((StructType)grammar.getType("ExtractRecord")).getConstraints();
		Assertions.assertTrue(extract.stream().anyMatch(c -> c instanceof InConstraint
			&& "media_part.media".equals(((InConstraint)c).getField())), extract.toString());

		final List<String> errors = FLEFGrammarValidator.validate(grammar).errors();
		Assertions.assertTrue(errors.stream().noneMatch(error -> error.contains("every")), errors.toString());
	}

	@Test
	void anExtractOfTheSourceCitedIsAccepted(){
		final List<String> errors = constraintErrors(sources()
			+ "extract X1 {\n\tsource S1\n\ttext Born\n" + AUDIT + "}\n"
			+ individualCiting("S1", "X1"));

		Assertions.assertTrue(errors.isEmpty(), errors.toString());
	}

	@Test
	void anExtractOfAnotherSourceIsAnError(){
		final List<String> errors = constraintErrors(sources()
			+ "extract X1 {\n\tsource S1\n\ttext Born\n" + AUDIT + "}\n"
			+ individualCiting("S2", "X1"));

		Assertions.assertEquals(1, errors.size(), errors.toString());
		Assertions.assertTrue(errors.getFirst().contains("e.source == source"), errors.getFirst());
	}

	@Test
	void theMediaOfAnExtractMustBeMediaOfItsSource(){
		final String extracts = "extract X1 {\n\tsource S1\n\tmedia_part.media M1\n" + AUDIT + "}\n"
			+ "extract X2 {\n\tsource S1\n\tmedia_part.media M2\n" + AUDIT + "}\n";
		final List<String> errors = constraintErrors(sources() + extracts);

		Assertions.assertEquals(1, errors.size(), errors.toString());
		Assertions.assertTrue(errors.getFirst().contains("M2"), errors.getFirst());
	}

	@Test
	void requireEveryNeedsAComparisonAfterTheColon(){
		Assertions.assertThrows(FLEFGrammarParseException.class, () -> FLEFGrammarParser.parseStandalone("""
			file F {
			  Record+
			}
			Record = oneof {
			  header: Header
			}
			record Header {
			  version: string
			  item*: string
			  require every e in item: e.version
			}
			"""));
	}


	@Test
	void theExtractsWrittenInTheCitationsBecomeRecordsOfTheSourceCited(){
		final FLEFModel model = new FLEFParser().parse(sources()
			+ "individual I1 {\n\tsource {\n\t\tsource S1\n\t\textract {\n\t\t\ttext Born on the 3rd\n\t\t\ttype verbatim\n\t\t}\n"
			+ "\t\textract {\n\t\t\tmedia_part.media M1\n\t\t}\n\t\tstance supports\n\t}\n" + AUDIT + "}\n"
			+ "individual I2 {\n\tsource {\n\t\tsource S1\n\t\textract {\n\t\t\ttype verbatim\n\t\t\ttext Born on the 3rd\n\t\t}\n"
			+ "\t\tstance supports\n\t}\n" + AUDIT + "}\n");

		final List<FLEFRecord> extracts = model.getRecordsByType(ExtractReader.TYPE);
		//the same extract of the same source is one record
		Assertions.assertEquals(2, extracts.size(), extracts.toString());
		final FLEFRecord citation1 = FLEFRecordHelper.findChild(model.getRecordById("I1"), SourceCitationReader.TAG_SOURCE);
		final FLEFRecord citation2 = FLEFRecordHelper.findChild(model.getRecordById("I2"), SourceCitationReader.TAG_SOURCE);
		final List<FLEFRecord> extracts1 = ExtractReader.extractsOf(citation1, model);
		final List<FLEFRecord> extracts2 = ExtractReader.extractsOf(citation2, model);
		Assertions.assertEquals(2, extracts1.size());
		Assertions.assertEquals(List.of(extracts1.getFirst()), extracts2);

		final FLEFRecord text = extracts1.getFirst();
		Assertions.assertEquals("S1", ExtractReader.extractSource(text));
		Assertions.assertEquals("Born on the 3rd", ExtractReader.extractText(text));
		Assertions.assertEquals(ExtractReader.ENUM_TYPE_VERBATIM, ExtractReader.extractType(text));
		Assertions.assertEquals("2020-01-01T00:00:00Z", FLEFRecordHelper.getChildValue(text, "audit.creation.date"));
		Assertions.assertEquals(ExtractMigration.MIGRATION_COMMENT, FLEFRecordHelper.getChildValue(text, "audit.creation.comment"));
		Assertions.assertEquals("M1", FLEFRecordHelper.getChildValue(extracts1.get(1), ExtractReader.TAG_MEDIA_PART_MEDIA));

		//the citations reference the records
		Assertions.assertEquals(List.of(text.getId(), extracts1.get(1).getId()),
			FLEFRecordHelper.findChildren(citation1, SourceCitationReader.TAG_EXTRACT).stream()
				.map(FLEFRecord::getValue)
				.toList());

		//and the result is valid
		final List<String> errors = new FLEFValidator(grammar).validateSchema(model);
		Assertions.assertTrue(errors.stream().noneMatch(error -> error.contains("extract")), errors.toString());
	}

	@Test
	void migratingAMigratedModelDoesNothing(){
		final FLEFModel model = new FLEFParser().parse(sources()
			+ "extract X7 {\n\tsource S1\n\ttext Born\n" + AUDIT + "}\n"
			+ individualCiting("S1", "X7"));

		Assertions.assertEquals(0, ExtractMigration.migrate(model));
		Assertions.assertEquals(1, model.getRecordsByType(ExtractReader.TYPE).size());
	}

	@Test
	void theIdsOfTheMigratedExtractsFollowTheExistingOnes(){
		final FLEFModel model = new FLEFParser().parse(sources()
			+ "extract X7 {\n\tsource S1\n\ttext Born\n" + AUDIT + "}\n"
			+ "individual I1 {\n\tsource {\n\t\tsource S2\n\t\textract {\n\t\t\ttext Died\n\t\t}\n\t\tstance supports\n\t}\n" + AUDIT
			+ "}\n");

		Assertions.assertNotNull(model.getRecordById("X8"));
		Assertions.assertEquals("S2", ExtractReader.extractSource(model.getRecordById("X8")));
	}

}
