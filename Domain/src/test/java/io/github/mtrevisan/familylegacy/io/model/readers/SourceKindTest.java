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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** The kind of a source (what it is, as opposed to its media type): the reader constants agree with the grammar, and the field is valid and survives the round trip. */
class SourceKindTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";

	private static String schema;
	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = SourceKindTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			Assertions.assertNotNull(is, "schema not found");
			schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		grammar = FLEFGrammarParser.parse(schema);
	}


	private static String enumBody(){
		final Matcher matcher = Pattern.compile("record SourceRecord \\{.*?\\n  kind\\?: enum \\{(.*?)\\n  } \\| Text", Pattern.DOTALL)
			.matcher(schema);
		Assertions.assertTrue(matcher.find(), "kind of SourceRecord not found in schema");
		return matcher.group(1);
	}

	private static List<String> validate(final String kind){
		final FLEFModel model = new FLEFParser().parse("source S1 {\n\ttitle.value Register\n\tkind " + kind + "\n" + AUDIT + "}\n");
		//the file header is not the concern of this test
		return new FLEFValidator(grammar).validateSchema(model).stream()
			.filter(error -> !error.contains("'header'"))
			.toList();
	}


	@Test
	void theKindsOfTheReaderAreTheEnumOfTheGrammar(){
		final Pattern comment = Pattern.compile("//[^\r\n]*");
		final List<String> schemaKinds = Arrays.stream(StringUtils.split(comment.matcher(enumBody()).replaceAll(""), ','))
			.map(String::trim)
			.filter(token -> !token.isEmpty())
			.toList();

		Assertions.assertEquals(schemaKinds, List.of(SourceReader.KINDS));
	}

	@Test
	void everyKindOfTheGrammarHasAComment(){
		final List<String> uncommented = Arrays.stream(StringUtils.split(enumBody(), "\r\n"))
			.map(String::trim)
			.filter(line -> !line.isEmpty() && !line.contains("//"))
			.toList();

		Assertions.assertEquals(List.of(), uncommented);
	}

	@Test
	void theSchoolRecordIsAKindAndTheResearchRecordsAreNot(){
		Assertions.assertTrue(List.of(SourceReader.KINDS).contains("school_record"));
		for(final String notAGenre : List.of("theory", "guess", "hunch", "hypothesis", "researcher"))
			Assertions.assertFalse(List.of(SourceReader.KINDS).contains(notAGenre), notAGenre);
	}

	@Test
	void everyKindHasALabelAndAnExplanation() throws IOException{
		final Properties messages = new Properties();
		try(final InputStream is = SourceKindTest.class.getResourceAsStream("/i18n/ui/messages.properties")){
			Assertions.assertNotNull(is, "messages not found");
			messages.load(new InputStreamReader(is, StandardCharsets.UTF_8));
		}

		Assertions.assertAll(Arrays.stream(SourceReader.KINDS)
			.flatMap(kind -> java.util.stream.Stream.of("enum.source.kind." + kind, "enum.source.kind." + kind + ".tooltip"))
			.map(key -> () -> Assertions.assertTrue(messages.containsKey(key), key)));
	}

	@Test
	void theKindIsReadFromTheRecord(){
		final FLEFRecord source = FLEFRecord.createMainRecord("S1", SourceReader.TYPE);
		Assertions.assertNull(SourceReader.extractKind(source));

		source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_KIND, SourceReader.ENUM_KIND_CENSUS));
		Assertions.assertEquals("census", SourceReader.extractKind(source));
	}

	@Test
	void everyKindOfTheVocabularyAndACustomTextAreValid(){
		for(final String kind : SourceReader.KINDS)
			Assertions.assertEquals(List.of(), validate(kind), kind);
		Assertions.assertEquals(List.of(), validate("land_survey"));
	}

	@Test
	void theKindAndTheMediaTypeCoexistAndSurviveTheRoundTrip(){
		final FLEFModel model = new FLEFParser().parse("source S1 {\n\ttitle.value Register\n\tkind church_register\n\tmedia_type film\n"
			+ AUDIT + "}\n");

		for(final FLEFWriter writer : List.of(FLEFWriter.create(), FLEFWriter.createCompact())){
			final FLEFRecord reread = new FLEFParser().parse(writer.writeToString(model)).getRecordById("S1");

			Assertions.assertEquals("church_register", SourceReader.extractKind(reread));
			Assertions.assertEquals("film", SourceReader.extractMediaType(reread));
		}
	}

}
