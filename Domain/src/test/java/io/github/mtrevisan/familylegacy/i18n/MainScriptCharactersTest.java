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
package io.github.mtrevisan.familylegacy.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class MainScriptCharactersTest{

	@Test
	void keepsMainScriptAndNeutralCharactersOnly(){
		final String result = I18N.mainScriptCharacters(List.of((int)'a', (int)'ō', (int)'1', (int)'(', 0x2011, 0x0301,
				0x30ED, 0x62FC, 0x0643, 0xCD0C),
			Map.of(Character.UnicodeScript.LATIN, 500, Character.UnicodeScript.HAN, 3, Character.UnicodeScript.ARABIC, 2));

		assertTrue(result.contains("a") && result.contains("ō") && result.contains("1") && result.contains("("));
		assertTrue(result.indexOf(0x2011) >= 0, "script-neutral punctuation is still required");
		assertTrue(result.indexOf(0x0301) >= 0, "combining marks are script-neutral");
		assertFalse(result.indexOf(0x30ED) >= 0 || result.indexOf(0x62FC) >= 0 || result.indexOf(0x0643) >= 0
			|| result.indexOf(0xCD0C) >= 0, "letters of other scripts are not required");
	}

	@Test
	void mainScriptFollowsTheMostFrequentLetters(){
		final String result = I18N.mainScriptCharacters(List.of((int)'a', 0x0643),
			Map.of(Character.UnicodeScript.LATIN, 1, Character.UnicodeScript.ARABIC, 9));

		assertEquals(String.valueOf((char)0x0643), result);
	}

	@Test
	void bundleCharactersExcludeNativeScriptLabels(){
		final String characters = I18N.allCharacters();

		assertFalse(characters.isEmpty());
		assertTrue(characters.codePoints().noneMatch(cp -> {
			final Character.UnicodeScript script = Character.UnicodeScript.of(cp);
			return script != Character.UnicodeScript.LATIN && script != Character.UnicodeScript.COMMON
				&& script != Character.UnicodeScript.INHERITED;
		}));
	}
}
