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
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


/** {@code require if type == sex: value in (male, female, unknown)}: the value of a sex attribute is an enumeration. */
class SexValueConstraintTest{

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = SexValueConstraintTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static List<String> errorsOf(final String type, final String value){
		final FLEFModel model = new FLEFParser().parse("individual I1 {\n\taudit.creation.date 2020-01-01T00:00:00Z\n}\n"
			+ "individual_attribute IA1 {\n\tindividual I1\n\ttype " + type + "\n\tvalue " + value
			+ "\n\taudit.creation.date 2020-01-01T00:00:00Z\n}\n");
		return new FLEFValidator(grammar).validateSchema(model).stream()
			.filter(error -> error.contains("must be one of"))
			.toList();
	}


	@Test
	void theValuesOfTheEnumerationAreAccepted(){
		for(final String value : List.of("male", "female", "unknown", "Male"))
			Assertions.assertTrue(errorsOf("sex", value).isEmpty(), value);
	}

	@Test
	void anotherValueOfASexIsAnError(){
		Assertions.assertEquals(1, errorsOf("sex", "intersex").size());
	}

	@Test
	void otherTypesAreFree(){
		Assertions.assertTrue(errorsOf("occupation", "smith").isEmpty());
	}

}
