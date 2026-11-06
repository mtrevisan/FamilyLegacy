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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;


/**
 * Decodes the BLOB of a GEDCOM 5.5 multimedia record (an embedded binary file).
 * <p>
 * Each character stands for 6 bits, through the table {@code . / 0-9 A-Z a-z} (0 to 63). Four characters make a 24-bit group (the
 * first character gives the highest bits), whose three bytes are read right to left (GEDCOM 5.5, appendix E). The encoder pads the last
 * group with zero bytes, and the padding cannot be told from the data: a group of four characters is always decoded to three bytes
 * (the zero bytes of the padding stay).
 */
public final class GEDCOMBlobDecoder{

	private static final String ALPHABET = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int BITS_PER_CHARACTER = 6;
	private static final int CHARACTERS_PER_GROUP = 4;
	private static final int BYTES_PER_GROUP = 3;
	private static final int BYTE_MASK = 0xFF;


	private GEDCOMBlobDecoder(){}


	/**
	 * Decodes the BLOB line: its value and its continuation lines (CONT, CONC), concatenated, are the encoded text.
	 *
	 * @return	The bytes, or {@code null} if there is no data or it is not valid.
	 */
	public static byte[] decode(final GEDCOMNode blobNode){
		if(blobNode == null)
			return null;

		final StringBuilder text = new StringBuilder(StringUtils.defaultString(blobNode.getValue()));
		for(final GEDCOMNode child : blobNode.getChildren())
			if(GEDCOMMapper.TAG_CONTINUATION.equals(child.getTag()) || GEDCOMMapper.TAG_CONCATENATION.equals(child.getTag()))
				text.append(StringUtils.defaultString(child.getValue()));
		return decode(text.toString());
	}

	/**
	 * Decodes an encoded text.
	 *
	 * @return	The bytes, or {@code null} if the text is blank, has a character out of the table (spaces are skipped) or its length is
	 * 	not a multiple of four.
	 */
	public static byte[] decode(final String encoded){
		final String text = StringUtils.deleteWhitespace(encoded);
		if(text.isEmpty() || text.length() % CHARACTERS_PER_GROUP != 0)
			return null;

		final ByteArrayOutputStream out = new ByteArrayOutputStream(text.length() / CHARACTERS_PER_GROUP * BYTES_PER_GROUP);
		for(int i = 0; i < text.length(); i += CHARACTERS_PER_GROUP){
			int group = 0;
			for(int j = 0; j < CHARACTERS_PER_GROUP; j ++){
				final int key = ALPHABET.indexOf(text.charAt(i + j));
				if(key < 0)
					return null;

				group = (group << BITS_PER_CHARACTER) | key;
			}
			//the bytes are read right to left
			for(int j = 0; j < BYTES_PER_GROUP; j ++)
				out.write((group >> (Byte.SIZE * j)) & BYTE_MASK);
		}
		return out.toByteArray();
	}

}
