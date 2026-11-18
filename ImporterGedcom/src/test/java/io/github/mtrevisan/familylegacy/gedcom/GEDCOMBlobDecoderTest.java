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
package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMBlobDecoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The decoding of the BLOB of GEDCOM 5.5 (appendix E): 6 bits per character, three bytes read right to left. */
class GEDCOMBlobDecoderTest{

	private static final String ALPHABET = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";


	/** The reference encoder: the bytes of each chunk are reversed and split into four 6-bit keys. */
	private static String encode(final byte[] data){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < data.length; i += 3){
			final int b1 = data[i] & 0xFF;
			final int b2 = (i + 1 < data.length? data[i + 1] & 0xFF: 0);
			final int b3 = (i + 2 < data.length? data[i + 2] & 0xFF: 0);
			final int group = (b3 << 16) | (b2 << 8) | b1;
			sb.append(ALPHABET.charAt((group >> 18) & 0x3F))
				.append(ALPHABET.charAt((group >> 12) & 0x3F))
				.append(ALPHABET.charAt((group >> 6) & 0x3F))
				.append(ALPHABET.charAt(group & 0x3F));
		}
		return sb.toString();
	}


	@Test
	void aKnownVector(){
		//0x01 0x02 0x03 -> 0x030201 -> keys 0, 48, 8, 1
		Assertions.assertEquals(".k6/", encode(new byte[]{1, 2, 3}));
		Assertions.assertArrayEquals(new byte[]{1, 2, 3}, GEDCOMBlobDecoder.decode(".k6/"));
		//the key of a dot is 0 and the one of a slash is 1
		Assertions.assertArrayEquals(new byte[]{0, 0, 0}, GEDCOMBlobDecoder.decode("...."));
		Assertions.assertArrayEquals(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF}, GEDCOMBlobDecoder.decode("zzzz"));
		Assertions.assertArrayEquals(new byte[]{0x40, 0x00, 0x00}, GEDCOMBlobDecoder.decode("../."));
	}

	@Test
	void everyByteValueSurvivesTheRoundTrip(){
		final byte[] data = new byte[768];
		for(int i = 0; i < data.length; i ++)
			data[i] = (byte)(i * 7 + i / 256);

		Assertions.assertArrayEquals(data, GEDCOMBlobDecoder.decode(encode(data)));
	}

	@Test
	void theContinuationLinesAreJoined(){
		final GEDCOMNode blob = new GEDCOMNode(1, "BLOB", "");
		final GEDCOMNode first = new GEDCOMNode(2, "CONT", ".k6/");
		final GEDCOMNode second = new GEDCOMNode(2, "CONT", "zzzz");
		blob.addChild(first);
		blob.addChild(second);

		Assertions.assertArrayEquals(new byte[]{1, 2, 3, (byte)0xFF, (byte)0xFF, (byte)0xFF}, GEDCOMBlobDecoder.decode(blob));
	}

	@Test
	void invalidDataGivesNull(){
		Assertions.assertNull(GEDCOMBlobDecoder.decode(""));
		Assertions.assertNull(GEDCOMBlobDecoder.decode("  "));
		//not a multiple of four characters
		Assertions.assertNull(GEDCOMBlobDecoder.decode(".HM......."));
		//a character out of the table
		Assertions.assertNull(GEDCOMBlobDecoder.decode(".k6!"));
		Assertions.assertNull(GEDCOMBlobDecoder.decode((GEDCOMNode)null));
	}

}
