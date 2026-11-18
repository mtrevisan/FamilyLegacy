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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;


public class GEDCOMCharsetDetector{

	private static final int BUFFER_SIZE = 8192;
	private static final int MIN_CONFIDENCE = 50;


	private GEDCOMCharsetDetector(){}


	/**
	 * Inspects the byte stream to determine the character encoding.
	 * Checks for BOM first, then falls back to heuristic byte analysis via ICU4J.
	 *
	 * @param in InputStream with markSupported() == true
	 * @return Standardized Charset name (e.g. "UTF-8", "UTF-16LE", "WINDOWS-1252"), or null if uncertain.
	 * @throws IOException if reading the stream fails.
	 */
	public static String detectCharsetFromBytes(final InputStream in) throws IOException{
		final String bom = detectBom(in);
		if(bom != null)
			return bom;

		return detectHeuristically(in);
	}

	/**
	 * Looks for a byte order mark at the start of the stream (that is left where it was).
	 *
	 * @param in InputStream with markSupported() == true
	 * @return	The name of the charset the mark stands for, or {@code null} if there is no mark.
	 * @throws IOException if reading the stream fails.
	 */
	public static String detectBom(final InputStream in) throws IOException{
		if(!in.markSupported())
			throw new IllegalArgumentException("InputStream must support mark/reset operations.");

		in.mark(BUFFER_SIZE);
		try{
			// Wrap with CloseShieldInputStream to prevent closing the underlying stream 'in'
			final BOMInputStream bomIn = BOMInputStream.builder()
				.setInputStream(CloseShieldInputStream.wrap(in))
				.setByteOrderMarks(
					ByteOrderMark.UTF_8,
					ByteOrderMark.UTF_16LE,
					ByteOrderMark.UTF_16BE,
					ByteOrderMark.UTF_32LE,
					ByteOrderMark.UTF_32BE
				)
				.get();

			return (bomIn.hasBOM()
				? bomIn.getBOM()
					.getCharsetName()
				: null);
		}
		finally{
			in.reset();
		}
	}

	/**
	 * Guesses the charset from the first bytes of the stream (that is left where it was).
	 *
	 * @return	The name of the charset, or {@code null} if the guess is not confident enough.
	 */
	public static String detectHeuristically(final InputStream in) throws IOException{
		if(!in.markSupported())
			throw new IllegalArgumentException("InputStream must support mark/reset operations.");

		in.mark(BUFFER_SIZE);
		final byte[] buffer = new byte[BUFFER_SIZE];
		final int bytesRead;
		try{
			bytesRead = IOUtils.read(in, buffer, 0, BUFFER_SIZE);
		}
		finally{
			in.reset();
		}

		if(bytesRead <= 0)
			return null;

		//the window can end in the middle of a character, that would lower the confidence of the guess
		final byte[] sample = new byte[trimmedLength(buffer, bytesRead, bytesRead == BUFFER_SIZE)];
		System.arraycopy(buffer, 0, sample, 0, sample.length);

		final CharsetDetector detector = new CharsetDetector();
		detector.setText(sample);
		final CharsetMatch match = detector.detect();

		if(match != null && match.getConfidence() >= MIN_CONFIDENCE){
			final String detectedName = match.getName();
			try{
				return Charset.forName(detectedName)
					.name();
			}
			catch(final Exception e){
				return detectedName;
			}
		}

		return null;
	}

	/** @return	The length of the bytes without a UTF-8 sequence cut at their end (if the bytes are the beginning of a longer text). */
	static int trimmedLength(final byte[] bytes, final int length, final boolean truncated){
		if(!truncated)
			return length;

		//a sequence is at most 4 bytes: look for the lead byte among the last 3
		for(int back = 1; back <= 3 && back <= length; back ++){
			final int b = bytes[length - back] & 0xFF;
			if((b & 0xC0) == 0x80)
				//a continuation byte: keep looking for its lead byte
				continue;

			final int expected = ((b & 0xE0) == 0xC0? 2: (b & 0xF0) == 0xE0? 3: (b & 0xF8) == 0xF0? 4: 1);
			return (expected > back? length - back: length);
		}
		return length;
	}

}
