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
package io.github.mtrevisan.calendars;

import io.github.mtrevisan.calendars.text.Ordinals;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class OrdinalsTest{

	@Test
	void arabicWithSuffix(){
		Assertions.assertEquals(3, Ordinals.parse("3"));
		Assertions.assertEquals(3, Ordinals.parse("3rd"));
		Assertions.assertEquals(1, Ordinals.parse("1st"));
		Assertions.assertEquals(2, Ordinals.parse("2a"));
	}

	@Test
	void romanNumerals(){
		Assertions.assertEquals(14, Ordinals.parse("xiv"));
		Assertions.assertEquals(49, Ordinals.parse("xlix"));
		Assertions.assertEquals(63, Ordinals.parse("lxiii"));
		// common words are not numbers
		Assertions.assertEquals(-1, Ordinals.parse("di"));
		Assertions.assertEquals(-1, Ordinals.parse("l"));
	}

	@Test
	void words(){
		Assertions.assertEquals(3, Ordinals.parse("terza"));
		Assertions.assertEquals(12, Ordinals.parse("dodicesimo"));
		Assertions.assertEquals(3, Ordinals.parse("tertio"));
		Assertions.assertEquals(2, Ordinals.parse("secundae"));
		Assertions.assertEquals(20, Ordinals.parse("vicesimo"));
		Assertions.assertEquals(8, Ordinals.parse("eighth"));
	}

	@Test
	void compoundWords(){
		Assertions.assertArrayEquals(new int[]{13, 2}, Ordinals.parse(new String[]{"tertia", "decima"}, 0));
		Assertions.assertArrayEquals(new int[]{14, 2}, Ordinals.parse(new String[]{"decimo", "quarto"}, 0));
		Assertions.assertArrayEquals(new int[]{21, 2}, Ordinals.parse(new String[]{"vicesimo", "primo"}, 0));
		Assertions.assertArrayEquals(new int[]{21, 2}, TwentyFirst.parse());
		Assertions.assertArrayEquals(new int[]{3, 1}, Ordinals.parse(new String[]{"tertia", "martii"}, 0));
	}

	@Test
	void normalization(){
		Assertions.assertEquals("l ottava di pasqua", TextNormalizer.normalize("L'Ottava di Pasqua"));
		Assertions.assertEquals("festum sancti martini", TextNormalizer.normalize("Festum Sâncti  Martini."));
		Assertions.assertEquals("caeli", TextNormalizer.normalize("cæli"));
	}


	private static final class TwentyFirst{
		private static int[] parse(){
			return Ordinals.parse(TextNormalizer.tokenize("twenty-first"), 0);
		}
	}

}
