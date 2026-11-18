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
package io.github.mtrevisan.familylegacy.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


class TextSearchHelperTest{

	@Test
	void aMissingTextNeverMatches(){
		for(final Set<SearchMode> modes : List.of(Set.<SearchMode>of(), Set.of(SearchMode.WHOLE_WORD), Set.of(SearchMode.FUZZY),
				Set.of(SearchMode.WHOLE_WORD, SearchMode.FUZZY)))
			Assertions.assertFalse(TextSearchHelper.matchesText(null, "fire", modes, 0.5), modes.toString());
	}

	@Test
	void aWholeWordWithPunctuationIsFound(){
		Assertions.assertTrue(TextSearchHelper.matchesText("Conor O'Brien", "o'brien", Set.of(SearchMode.WHOLE_WORD), 0.5));
		Assertions.assertFalse(TextSearchHelper.matchesText("Conor Brienne", "o'brien", Set.of(SearchMode.WHOLE_WORD), 0.5));
	}

	@Test
	void aSubstringIsFoundWithoutWholeWord(){
		Assertions.assertTrue(TextSearchHelper.matchesText("Niccolò Machiavelli", "NICCOLO", Set.of(), 0.5));
	}

}
