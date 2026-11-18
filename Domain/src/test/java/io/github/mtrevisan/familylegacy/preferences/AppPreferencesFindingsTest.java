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
package io.github.mtrevisan.familylegacy.preferences;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


class AppPreferencesFindingsTest{

	@Test
	void idsWithCommasAndBackslashesRoundTrip(){
		final Set<String> findings = Set.of("plain", "a,b", "c\\d", "e\\,f", ",", "tail\\");

		final String encoded = AppPreferences.encodeFindings(findings);

		Assertions.assertEquals(findings, Set.copyOf(AppPreferences.decodeFindings(encoded)));
	}

	@Test
	void oldCommaSeparatedFormatIsStillRead(){
		Assertions.assertEquals(List.of("abc123", "def456", "0a1b"), AppPreferences.decodeFindings("abc123,def456,0a1b"));
	}

	@Test
	void emptySetIsEncodedAsEmptyText(){
		Assertions.assertEquals("", AppPreferences.encodeFindings(Set.of()));
		Assertions.assertTrue(AppPreferences.decodeFindings("").isEmpty());
	}

}
