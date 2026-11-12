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
