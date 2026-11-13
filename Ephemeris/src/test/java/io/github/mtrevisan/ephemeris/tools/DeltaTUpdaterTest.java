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
package io.github.mtrevisan.ephemeris.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


class DeltaTUpdaterTest{

	private static String table(final int months){
		final StringBuilder sb = new StringBuilder("# comment\r\n");
		for(int i = 0; i < months; i ++)
			sb.append(String.format(Locale.US, " %d %2d  1  %.4f\r\n", 1973 + i / 12, i % 12 + 1, 43. + i * 0.01));
		return sb.toString();
	}

	@Test
	void parsesAShippedStyleTable(){
		final List<String> rows = DeltaTUpdater.parse(table(600));

		Assertions.assertEquals(600, rows.size());
		Assertions.assertEquals(" 1973  1  1  43.0000", rows.getFirst());
	}

	@Test
	void theShippedFileIsValid() throws Exception{
		final String content = Files.readString(Path.of("src/main/resources/deltat.data"), StandardCharsets.UTF_8);

		Assertions.assertTrue(DeltaTUpdater.parse(content).size() >= 500);
	}

	@Test
	void rejectsTooShortMalformedAndUnorderedTables(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> DeltaTUpdater.parse(table(10)));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> DeltaTUpdater.parse(table(600) + "<html>not a table</html>\n"));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> DeltaTUpdater.parse(table(600) + " 1973  1  1  43.0\n"));
	}

}
