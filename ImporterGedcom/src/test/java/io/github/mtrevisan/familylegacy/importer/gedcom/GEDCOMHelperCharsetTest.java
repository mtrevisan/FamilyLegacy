package io.github.mtrevisan.familylegacy.importer.gedcom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


class GEDCOMHelperCharsetTest{

	private static String read(final byte[] data) throws IOException{
		try(final BufferedReader reader = GEDCOMHelper.getBufferedReader(new ByteArrayInputStream(data))){
			final StringBuilder sb = new StringBuilder();
			int c;
			while((c = reader.read()) >= 0)
				sb.append((char)c);
			return sb.toString();
		}
	}

	private static byte[] bytes(final String text, final Charset charset){
		return text.getBytes(charset);
	}

	@Test
	void declaredAnselIsReadAsAnselEvenIfTheBytesLookLikeLatin1() throws IOException{
		//ANSEL: "Ren" + acute + "e", then a few lines of Latin-looking text (acute 0xE2 + vowels)
		final StringBuilder body = new StringBuilder("0 HEAD\n1 CHAR ANSEL\n");
		for(int i = 0; i < 200; i ++)
			body.append("0 @I").append(i).append("@ INDI\n1 NAME Renâe /Dupont/\n");
		final byte[] data = body.toString().getBytes(StandardCharsets.ISO_8859_1);

		final String text = read(data);

		Assertions.assertTrue(text.contains("René /Dupont/") || text.contains("René/"), text.substring(0, 80));
		Assertions.assertFalse(text.contains("â"));
	}

	@Test
	void asciiDeclaredButUtf8IsReadAsUtf8() throws IOException{
		final String text = "0 HEAD\n1 CHAR ASCII\n0 @I1@ INDI\n1 NAME José /García/\n";

		Assertions.assertEquals(text, read(bytes(text, StandardCharsets.UTF_8)));
	}

	@Test
	void declaredWindows1252IsRespected() throws IOException{
		final String text = "0 HEAD\n1 CHAR ANSI\n0 @I1@ INDI\n1 NAME José /García/\n";

		Assertions.assertEquals(text, read(bytes(text, Charset.forName("windows-1252"))));
	}

	@Test
	void unknownCharsetNameFallsBack() throws IOException{
		final String text = "0 HEAD\n1 CHAR NOT-A-CHARSET\n0 @I1@ INDI\n1 NAME John /Doe/\n";

		Assertions.assertEquals(text, read(bytes(text, StandardCharsets.UTF_8)));
	}

	@Test
	void missingCharDefaultsToUtf8() throws IOException{
		final String text = "0 HEAD\n0 @I1@ INDI\n1 NAME José /García/\n";

		Assertions.assertEquals(text, read(bytes(text, StandardCharsets.UTF_8)));
	}

	@Test
	void bomWins() throws IOException{
		final String text = "0 HEAD\n1 CHAR ANSI\n0 @I1@ INDI\n1 NAME José /García/\n";
		final byte[] utf8 = bytes(text, StandardCharsets.UTF_8);
		final byte[] data = new byte[utf8.length + 3];
		data[0] = (byte)0xEF;
		data[1] = (byte)0xBB;
		data[2] = (byte)0xBF;
		System.arraycopy(utf8, 0, data, 3, utf8.length);

		Assertions.assertEquals(text, read(data));
	}

	@Test
	void utf16WithBomIsRead() throws IOException{
		final String text = "0 HEAD\n1 CHAR UNICODE\n0 @I1@ INDI\n1 NAME José /García/\n";
		final byte[] body = bytes(text, StandardCharsets.UTF_16LE);
		final byte[] data = new byte[body.length + 2];
		data[0] = (byte)0xFF;
		data[1] = (byte)0xFE;
		System.arraycopy(body, 0, data, 2, body.length);

		Assertions.assertEquals(text, read(data));
	}

	@Test
	void multibyteCharacterAtTheEndOfThePeekWindowDoesNotConfuseDetection() throws IOException{
		//a long UTF-8 file with accents everywhere, so that the 8 KB window cuts a character
		final StringBuilder sb = new StringBuilder("0 HEAD\n");
		for(int i = 0; i < 2000; i ++)
			sb.append("1 NOTE àèìòù\n");
		final String text = sb.toString();

		Assertions.assertEquals(text, read(bytes(text, StandardCharsets.UTF_8)));
	}

	@Test
	void trimmedLengthCutsAnIncompleteUtf8Sequence(){
		//e-grave is C3 A8: the window ends after C3
		final byte[] cut = {'a', (byte)0xC3};
		Assertions.assertEquals(1, GEDCOMCharsetDetector.trimmedLength(cut, 2, true));
		Assertions.assertEquals(2, GEDCOMCharsetDetector.trimmedLength(cut, 2, false));
		//three-byte sequence cut after two bytes
		final byte[] cut3 = {'a', (byte)0xE2, (byte)0x82};
		Assertions.assertEquals(1, GEDCOMCharsetDetector.trimmedLength(cut3, 3, true));
		//complete sequences are kept
		final byte[] whole = {'a', (byte)0xC3, (byte)0xA8};
		Assertions.assertEquals(3, GEDCOMCharsetDetector.trimmedLength(whole, 3, true));
		Assertions.assertEquals(1, GEDCOMCharsetDetector.trimmedLength(new byte[]{'a'}, 1, true));
	}

}
