package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;


/** Defects found by an independent review of the identifiers, the charset detection and the ANSEL reader. */
class IndependentReviewFixesTest{

	private static String read(final byte[] data) throws IOException{
		try(final BufferedReader reader = GEDCOMHelper.getBufferedReader(new ByteArrayInputStream(data))){
			final StringBuilder sb = new StringBuilder();
			int c;
			while((c = reader.read()) >= 0)
				sb.append((char)c);
			return sb.toString();
		}
	}

	private static String readAnsel(final int... bytes) throws IOException{
		final byte[] data = new byte[bytes.length];
		for(int i = 0; i < bytes.length; i ++)
			data[i] = (byte)bytes[i];
		final AnselInputStreamReader reader = new AnselInputStreamReader(new ByteArrayInputStream(data));
		final StringBuilder sb = new StringBuilder();
		int c;
		while((c = reader.read()) >= 0)
			sb.append((char)c);
		return sb.toString();
	}

	private static FLEFModel convert(final String body) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader("0 HEAD\n1 CHAR UTF-8\n" + body + "0 TRLR\n"));
		return new GEDCOMToFLEFConverter().convert(roots);
	}


	// ---- charset ----

	@Test
	void unicodeDeclaredOnAUtf8FileIsReadAsUtf8() throws IOException{
		final String text = "0 HEAD\n1 CHAR UNICODE\n0 @I1@ INDI\n1 NAME Jörg /Müller/\n";

		Assertions.assertEquals(text, read(text.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void utf16beDeclaredOnAUtf8FileIsReadAsUtf8() throws IOException{
		final String text = "0 HEAD\n1 CHAR UTF-16BE\n0 @I1@ INDI\n1 NAME Jörg /Müller/\n";

		Assertions.assertEquals(text, read(text.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void anselDeclaredOnAUtf8FileIsReadAsUtf8() throws IOException{
		final String text = "0 HEAD\n1 CHAR ANSEL\n0 @I1@ INDI\n1 NAME Jörg /Müller/\n";

		Assertions.assertEquals(text, read(text.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void trailingSpacesAfterTheCharsetNameAreIgnored() throws IOException{
		final String text = "0 HEAD\n1 CHAR ANSI \n0 @I1@ INDI\n1 NAME Jörg /Müller/\n";

		Assertions.assertEquals(text, read(text.getBytes(Charset.forName("windows-1252"))));
	}


	// ---- ANSEL ----

	@Test
	void markBeforeALineBreakStaysOnItsLine() throws IOException{
		Assertions.assertEquals("Á\r\n1 SEX M", readAnsel('A', 0xE2, '\r', '\n', '1', ' ', 'S', 'E', 'X', ' ', 'M'));
		Assertions.assertEquals("Á\n1", readAnsel('A', 0xE2, '\n', '1'));
	}

	@Test
	void diaeresisHasASingleByteForm() throws IOException{
		Assertions.assertEquals("b̈", readAnsel(0xE8, 'b'));
		Assertions.assertEquals("ú̈", readAnsel(0xE8, 0xE2, 'u'));
	}

	@Test
	void readerIsReadyWhileCharactersRemain() throws IOException{
		final AnselInputStreamReader reader = new AnselInputStreamReader(new ByteArrayInputStream(new byte[]{'a', 'b'}));

		Assertions.assertTrue(reader.ready());
		reader.read();
		Assertions.assertTrue(reader.ready());
		reader.read();
		Assertions.assertFalse(reader.ready());
	}


	// ---- identifiers ----

	private static List<String> ids(final FLEFModel model, final String type){
		return model.getRecordsByType(type).stream()
			.map(FLEFRecord::getId)
			.toList();
	}

	@Test
	void identifiersOfAnotherKindAreReplaced() throws IOException{
		final FLEFModel model = convert("""
			0 @P12@ INDI
			1 NAME John /Doe/
			1 FAMS @FAM1@
			0 @E5@ SOUR
			1 TITL Book
			0 @FAM1@ FAM
			1 HUSB @P12@
			0 @G7@ INDI
			1 NAME Jane /Roe/
			""");

		for(final String id : ids(model, "INDIVIDUAL"))
			Assertions.assertTrue(id.matches("I\\d+"), id);
		for(final String id : ids(model, "SOURCE"))
			Assertions.assertTrue(id.matches("S\\d+"), id);
		for(final String id : ids(model, "GROUP"))
			Assertions.assertTrue(id.matches("G\\d+"), id);
	}

	@Test
	void generatedIdentifierDoesNotCaptureADanglingPointer() throws IOException{
		final FLEFModel model = convert("""
			0 @x@ INDI
			1 NAME John /Doe/
			1 FAMC @I1@
			""");

		Assertions.assertFalse(model.hasRecord("I1"), ids(model, "INDIVIDUAL").toString());
	}

}
