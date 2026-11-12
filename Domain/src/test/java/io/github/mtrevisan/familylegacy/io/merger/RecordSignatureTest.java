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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


class RecordSignatureTest{

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = RecordSignatureTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	@Test
	void aStructureWithOnlyEvidenceStatesTheSameAsOneWithNothing(){
		final FLEFModel model = new FLEFParser().parse("""
			header {
				date 2026-01-01T00:00:00Z
			}
			individual I1 {
				audit.creation.date 2026-01-01T00:00:00Z
			}
			individual I2 {
				note {
					text Only a note.
					mime text/markdown
					audit.creation.date 2026-01-01T00:00:00Z
				}
				audit.creation.date 2026-01-01T00:00:00Z
			}
			""");
		final RecordSignature signature = new RecordSignature(new GrammarSupport(grammar));

		Assertions.assertEquals(signature.ofRecord(model.getRecordById("I1")), signature.ofRecord(model.getRecordById("I2")));
	}

	@Test
	void aStructureWithAnotherFieldStatesSomethingElse(){
		final FLEFModel model = new FLEFParser().parse("""
			header {
				date 2026-01-01T00:00:00Z
			}
			individual I1 {
				audit.creation.date 2026-01-01T00:00:00Z
			}
			individual I2 {
				name.part {
					type given
					value Mario
				}
				audit.creation.date 2026-01-01T00:00:00Z
			}
			""");
		final RecordSignature signature = new RecordSignature(new GrammarSupport(grammar));

		Assertions.assertNotEquals(signature.ofRecord(model.getRecordById("I1")), signature.ofRecord(model.getRecordById("I2")));
	}

}
