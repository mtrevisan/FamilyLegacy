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
package io.github.mtrevisan.familylegacy.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.merger.RecordAbsorber;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


class RecordMergerTest{

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = RecordMergerTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	@Test
	void twoIndividualsCanBeRecordedAsDifferent(){
		final FLEFModel model = new FLEFParser().parse("""
			header {
				date 2026-01-01T00:00:00Z
			}
			individual I1 {
				name.part {
					type given
					value Giovanni
				}
				audit.creation.date 2026-01-01T00:00:00Z
			}
			individual I2 {
				name.part {
					type given
					value Giovanni
				}
				audit.creation.date 2026-01-01T00:00:00Z
			}
			event E1 {
				type birth
				audit.creation.date 2026-01-01T00:00:00Z
			}
			event E2 {
				type birth
				audit.creation.date 2026-01-01T00:00:00Z
			}
			""");
		final RecordMerger merger = new RecordMerger(model, grammar);

		Assertions.assertTrue(merger.canReject(model.getRecordById("I1"), model.getRecordById("I2")));
		// events are assertions, not entities: they are merged or kept, never judged the same entity
		Assertions.assertFalse(merger.canReject(model.getRecordById("E1"), model.getRecordById("E2")));
		Assertions.assertFalse(merger.canReject(model.getRecordById("I1"), model.getRecordById("I1")));

		final RecordAbsorber.Rejection rejection = merger.reject("I2", "I1", "born in different parishes");

		Assertions.assertEquals(4 + 2, model.getRecordCount());
		Assertions.assertEquals("disproven", FLEFRecordHelper.getChildValue(model.getRecordById(rejection.conclusionId()),
			"proof_status"));
		Assertions.assertEquals("born in different parishes",
			FLEFRecordHelper.getChildValue(model.getRecordById(rejection.conclusionId()), "narrative"));
		Assertions.assertNotNull(model.getRecordById("I1"));
		Assertions.assertNotNull(model.getRecordById("I2"));
	}

}
