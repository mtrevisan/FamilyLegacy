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

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentifierReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
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
 * {@code struct IdentifierStructure}: what the grammar says of the identifiers of a source and of a repository citation, and what the
 * writer and the parser do with them.
 */
class IdentifierStructureTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = IdentifierStructureTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static FLEFModel model(final String sourceBody){
		return new FLEFParser().parse("repository R1 {\n\tname.value Archive\n" + AUDIT + "}\n"
			+ "source S1 {\n\ttitle.value Rossi Family History\n" + sourceBody + AUDIT + "}\n");
	}

	/** The problems of the schema that are about an identifier. */
	private static List<String> identifierErrors(final FLEFModel model){
		return new FLEFValidator(grammar).validateSchema(model).stream()
			.filter(error -> error.contains("identifier"))
			.toList();
	}


	@Test
	void theGrammarDeclaresTheStructAndTheTwoFieldsThatUseIt(){
		final StructType identifier = (StructType)grammar.getType("IdentifierStructure");
		Assertions.assertNotNull(identifier);
		Assertions.assertNotNull(identifier.getField("scheme"));
		Assertions.assertNotNull(identifier.getField("value"));
		Assertions.assertFalse(identifier.getConstraints().isEmpty(), "require not_blank(value)");
		Assertions.assertNotNull(((StructType)grammar.getType("RepositoryCitation")).getField("identifier"));
		Assertions.assertNotNull(((StructType)grammar.getType("RepositoryCitation")).getField("locator"), "the locator stays");
		Assertions.assertNotNull(((StructType)grammar.getType("SourceRecord")).getField("identifier"));
	}

	@Test
	void aSourceMayHaveSeveralIdentifiers(){
		final FLEFModel model = model("""
			\tidentifier {
			\t\tscheme isbn
			\t\tvalue 9780199535569
			\t}
			\tidentifier {
			\t\tscheme doi
			\t\tvalue 10.1000/182
			\t}
			\tidentifier {
			\t\tscheme url
			\t\tvalue https://example.org/rossi
			\t}
			""");

		Assertions.assertEquals(List.of(), identifierErrors(model));
		Assertions.assertEquals(3, SourceReader.extractIdentifiers(model.getRecordById("S1")).size());
	}

	@Test
	void aRepositoryCitationHasIdentifiersAndALocator(){
		final FLEFModel model = model("""
			\trepository {
			\t\trepository R1
			\t\tidentifier {
			\t\t\tscheme call_number
			\t\t\tvalue MS 45/12
			\t\t}
			\t\tidentifier {
			\t\t\tscheme dewey
			\t\t\tvalue 929.2 ROS
			\t\t}
			\t\tlocator Page 27
			\t}
			""");

		Assertions.assertEquals(List.of(), identifierErrors(model));
		final FLEFRecord citation = FLEFRecordHelper.findChild(model.getRecordById("S1"), SourceReader.TAG_REPOSITORY);
		Assertions.assertEquals(2, RepositoryCitationReader.extractIdentifiers(citation).size());
		Assertions.assertEquals("Page 27", FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_LOCATOR));
	}

	@Test
	void everySchemeOfTheVocabularyAndAFreeTextOneAreAccepted(){
		final StringBuilder body = new StringBuilder();
		for(final String scheme : IdentifierReader.SCHEMES)
			body.append("\tidentifier {\n\t\tscheme ").append(scheme).append("\n\t\tvalue X1\n\t}\n");
		body.append("\tidentifier {\n\t\tscheme Barcode\n\t\tvalue 0042\n\t}\n");

		Assertions.assertEquals(List.of(), identifierErrors(model(body.toString())));
	}

	@Test
	void aBlankValueIsAnError(){
		final FLEFModel model = model("\tidentifier {\n\t\tscheme isbn\n\t\tvalue   \n\t}\n");

		final List<String> errors = new FLEFValidator(grammar).validateSchema(model).stream()
			.filter(error -> error.contains("must not be blank"))
			.toList();
		Assertions.assertEquals(1, errors.size(), errors.toString());
	}

	@Test
	void aMissingValueOrSchemeIsReported(){
		Assertions.assertFalse(identifierErrors(model("\tidentifier {\n\t\tscheme isbn\n\t}\n")).isEmpty());
		Assertions.assertFalse(identifierErrors(model("\tidentifier {\n\t\tvalue 9780199535569\n\t}\n")).isEmpty());
	}

	@Test
	void aWrongCheckDigitIsNotAnErrorOfTheGrammar(){
		//a failure of the check is a warning of the validation of the file, never an error of the schema
		final FLEFModel model = model("\tidentifier {\n\t\tscheme isbn\n\t\tvalue 9780199535560\n\t}\n"
			+ "\tidentifier {\n\t\tscheme url\n\t\tvalue not a url\n\t}\n");

		Assertions.assertEquals(List.of(), identifierErrors(model));
	}

	@Test
	void theIdentifiersSurviveTheRoundTripInTheOrderTheyWereWritten(){
		final FLEFModel model = model("""
			\tidentifier {
			\t\tscheme isbn
			\t\tvalue 9780199535569
			\t}
			\trepository {
			\t\trepository R1
			\t\tidentifier {
			\t\t\tscheme call_number
			\t\t\tvalue MS 45/12
			\t\t}
			\t\tlocator Folio 8r
			\t}
			""");

		for(final FLEFWriter writer : List.of(FLEFWriter.create(), FLEFWriter.createCompact())){
			final String text = writer.writeToString(model);
			final FLEFModel reread = new FLEFParser().parse(text);
			final FLEFRecord source = reread.getRecordById("S1");

			Assertions.assertEquals(List.of("9780199535569"), IdentifierReader.extractValues(source, "isbn"), text);
			final FLEFRecord citation = FLEFRecordHelper.findChild(source, SourceReader.TAG_REPOSITORY);
			Assertions.assertEquals(List.of("MS 45/12"), IdentifierReader.extractValues(citation, "call_number"), text);
			Assertions.assertEquals("Folio 8r", FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_LOCATOR), text);
			Assertions.assertEquals(text, writer.writeToString(reread), "writing it again changes nothing");
		}
	}

}
