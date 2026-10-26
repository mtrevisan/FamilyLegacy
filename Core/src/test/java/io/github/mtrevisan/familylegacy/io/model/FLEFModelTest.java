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
package io.github.mtrevisan.familylegacy.io.model;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


class FLEFModelTest{

	@BeforeAll
	static void loadGrammar() throws IOException{
		// the grammar initializes the index of the Xref paths
		try(final InputStream is = FLEFModelTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	@Test
	void replacingAnIdRedirectsReferencesAndLinksAndRemovesTheOldRecord(){
		final FLEFModel model = new FLEFParser().parse("""
			header {
				protocol {
					name Family LEgacy Format
					version 0.2.0
				}
				date 2026-01-01T00:00:00Z
			}
			place P1 {
				name.value Treviso
				audit.creation.date 2026-01-01T00:00:00Z
			}
			place P2 {
				name.value Treviso
				audit.creation.date 2026-01-01T00:00:00Z
			}
			place P12 {
				name.value Venezia
				audit.creation.date 2026-01-01T00:00:00Z
			}
			event E1 {
				type birth
				place.place P1
				note {
					text Born in [Treviso](flef:P1), not in [Venezia](flef:P12).
					translation {
						text Nato a [Treviso](flef:P1).
						locale it
					}
					audit.creation.date 2026-01-01T00:00:00Z
				}
				audit.creation.date 2026-01-01T00:00:00Z
			}
			""");

		model.replaceRecordId("P1", "P2");

		Assertions.assertNull(model.getRecordById("P1"));
		Assertions.assertFalse(model.getRecords().stream().anyMatch(record -> "P1".equals(record.getId())));
		Assertions.assertEquals(2, model.getRecordsByType("place").size());
		Assertions.assertFalse(model.getPlacesById().containsKey("P1"));

		final FLEFRecord event = model.getRecordById("E1");
		Assertions.assertEquals("P2", FLEFRecordHelper.getChildValue(event, "place.place"));
		Assertions.assertEquals("Born in [Treviso](flef:P2), not in [Venezia](flef:P12).",
			FLEFRecordHelper.getChildValue(event, "note.text"));
		Assertions.assertEquals("Nato a [Treviso](flef:P2).", FLEFRecordHelper.getChildValue(event, "note.translation.text"));
	}

	@Test
	void replacingWithAMissingRecordIsRefused(){
		final FLEFModel model = new FLEFParser().parse("""
			header {
				date 2026-01-01T00:00:00Z
			}
			place P1 {
				name.value Treviso
				audit.creation.date 2026-01-01T00:00:00Z
			}
			""");

		Assertions.assertThrows(IllegalArgumentException.class, () -> model.replaceRecordId("P1", "P9"));
		Assertions.assertNotNull(model.getRecordById("P1"));
	}

}
