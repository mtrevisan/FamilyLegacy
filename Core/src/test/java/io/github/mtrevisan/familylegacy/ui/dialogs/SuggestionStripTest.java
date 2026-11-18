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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.features.Suggestion;
import io.github.mtrevisan.familylegacy.ui.features.SuggestionKind;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class SuggestionStripTest{

	/** The width of a narrow editor: the longest wording of the strip must fit it, not be cut. */
	private static final int NARROW_EDITOR = 428;

	@Test
	void everyWordingFitsANarrowEditor(){
		for(final SuggestionKind kind : SuggestionKind.values())
			for(final UxLevel level : UxLevel.values()){
				final SuggestionStrip strip = new SuggestionStrip();
				strip.show(List.of(Suggestion.forLevel(kind, level, "explanation", () -> {})));

				Assertions.assertTrue(strip.getPreferredSize().width <= NARROW_EDITOR,
					kind + StringUtils.SPACE + level + ": " + strip.getPreferredSize().width);
				Assertions.assertTrue(strip.isVisible());
			}
	}

	@Test
	void withoutSuggestionsTheStripIsHidden(){
		final SuggestionStrip strip = new SuggestionStrip();
		strip.show(List.of());
		Assertions.assertFalse(strip.isVisible());
	}

}
