package io.github.mtrevisan.familylegacy.importer.gedcom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;


class AnselInputStreamReaderTest{

	private static AnselInputStreamReader reader(final int... bytes) throws IOException{
		final byte[] data = new byte[bytes.length];
		for(int i = 0; i < bytes.length; i ++)
			data[i] = (byte)bytes[i];
		return new AnselInputStreamReader(new ByteArrayInputStream(data));
	}

	private static String readAll(final AnselInputStreamReader reader) throws IOException{
		final StringBuilder sb = new StringBuilder();
		int c;
		while((c = reader.read()) >= 0)
			sb.append((char)c);
		return sb.toString();
	}

	@Test
	void precomposedPairIsMerged() throws IOException{
		//acute + e
		Assertions.assertEquals("aéb", readAll(reader('a', 0xE2, 'e', 'b')));
	}

	@Test
	void diacriticWithoutPrecomposedFormFollowsItsLetter() throws IOException{
		//acute + x has no precomposed form: in Unicode the mark comes after the letter
		Assertions.assertEquals("ax́b", readAll(reader('a', 0xE2, 'x', 'b')));
	}

	@Test
	void stackedDiacriticsAreReversed() throws IOException{
		//ANSEL: (outer) tilde, (inner) acute, x; Unicode: x, acute, tilde
		Assertions.assertEquals("x́̃", readAll(reader(0xE4, 0xE2, 'x')));
	}

	@Test
	void diacriticAtTheEndOfTheStreamIsKept() throws IOException{
		Assertions.assertEquals("á", readAll(reader('a', 0xE2)));
	}

	@Test
	void bufferIsFilledFromTheOffset() throws IOException{
		final AnselInputStreamReader reader = reader('a', 'b', 'c');
		final char[] buffer = new char[6];

		final int read = reader.read(buffer, 2, 3);

		Assertions.assertEquals(3, read);
		Assertions.assertEquals("abc", new String(buffer, 2, 3));
		Assertions.assertEquals(-1, reader.read(buffer, 0, 3));
	}

	@Test
	void shortStreamReturnsWhatItHas() throws IOException{
		final AnselInputStreamReader reader = reader('a', 'b');
		final char[] buffer = new char[6];

		final int read = reader.read(buffer, 1, 4);

		Assertions.assertEquals(2, read);
		Assertions.assertEquals("ab", new String(buffer, 1, 2));
	}

}
