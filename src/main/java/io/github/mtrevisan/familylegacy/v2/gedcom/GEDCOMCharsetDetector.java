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
package io.github.mtrevisan.familylegacy.v2.gedcom;

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


	/**
	 * Inspects the byte stream to determine the character encoding.
	 * Checks for BOM first, then falls back to heuristic byte analysis via Apache Tika.
	 *
	 * @param in InputStream with markSupported() == true
	 * @return Standardized Charset name (e.g. "UTF-8", "UTF-16LE", "WINDOWS-1252"), or null if uncertain.
	 * @throws IOException if reading the stream fails.
	 */
	public static String detectCharsetFromBytes(final InputStream in) throws IOException{
		if(!in.markSupported())
			throw new IllegalArgumentException("InputStream must support mark/reset operations.");

		// 1. Direct BOM Detection using Apache Commons IO
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

			if(bomIn.hasBOM())
				return bomIn.getBOM().getCharsetName();
		}
		finally{
			in.reset();
		}

		// 2. Heuristic Byte Detection using Apache Tika (CharsetDetector)
		in.mark(BUFFER_SIZE);
		final byte[] buffer = new byte[BUFFER_SIZE];
		final int bytesRead = IOUtils.read(in, buffer, 0, BUFFER_SIZE);
		in.reset();

		if(bytesRead <= 0)
			return null;

		final CharsetDetector detector = new CharsetDetector();
		detector.setText(buffer);
		final CharsetMatch match = detector.detect();

		if(match != null && match.getConfidence() >= 50){
			final String detectedName = match.getName();
			try{
				return Charset.forName(detectedName).name();
			}
			catch(final Exception e){
				return detectedName;
			}
		}

		return null;
	}

}
