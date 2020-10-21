/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom_old;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


/*
 * This class reads an input stream of bytes representing ANSEL-encoded characters, and delivers a stream of UNICODE characters.
 * Conversion tables based upon <a href="http://www.heiner-eichmann.de/gedcom/oldansset.htm">ANSEL to Unicode Convertion Table</a> and
 * <a href="http://lcweb2.loc.gov/diglib/codetables/45.html">Code Table Extended Latin (ANSEL)</a>.
 */
class AnselInputStreamReader extends InputStreamReader{

	private static final Logger LOGGER = LoggerFactory.getLogger(AnselInputStreamReader.class);

	private static final String CHARACTER_ENCODING = "ANSEL";
	private static final String FORMATTER_2BYTE = "0x%02X";
	private static final String FORMATTER_4BYTE = "0x%04X";

	private static final Properties ANSEL_1BYTE = getProperties("/ansel1byte.properties");
	private static final Properties ANSEL_2BYTE = getProperties("/ansel2byte.properties");


	private final InputStream input;
	private int pending;


	AnselInputStreamReader(final InputStream in) throws IOException{
		super(in);

		input = in;
		//read one character ahead to cope with non-spacing diacriticals
		pending = input.read();
	}

	public int read() throws IOException{
		final int b = pending;
		if(b < 0)
			//return EOF unchanged
			return b;

		pending = input.read();
		if(b < 128)
			//return ASCII characters unchanged
			return b;

		//try to match two ansel chars if it is possible
		if(pending > 0 && ((b >= 0xE0 && b <= 0xFF) || (b >= 0xD7 && b <= 0xD9))){
			final String chr = ANSEL_2BYTE.getProperty(String.format(FORMATTER_4BYTE, b * 256 + pending));
			final int u = (chr != null? Integer.parseInt(chr): -1);
			if(u > 0){
				pending = input.read();
				return u;
			}
		}

		//else match one char
		final String chr = ANSEL_1BYTE.getProperty(String.format(FORMATTER_2BYTE, b));
		//if no match, use Unicode REPLACEMENT CHARACTER
		return (chr != null? Integer.parseInt(chr): 0xFFFD);
	}

	/**
	 * Fill a supplied buffer with UNICODE characters.
	 */
	public int read(final char[] buffer, final int offset, final int length) throws IOException{
		if(pending < 0)
			//have already hit EOF
			return -1;

		for(int i = offset; i < offset + length; i ++){
			final int c = read();
			if(c < 0)
				return i - offset;

			buffer[offset + i] = (char)c;
		}
		return length;
	}

	public String getEncoding(){
		return CHARACTER_ENCODING;
	}

	private static Properties getProperties(final String filename){
		final Properties rulesProperties = new Properties();
		final InputStream is = AnselInputStreamReader.class.getResourceAsStream(filename);
		if(is != null){
			try(final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)){
				rulesProperties.load(isr);
			}
			catch(final IOException e){
				LOGGER.error("Cannot load ANSEL table", e);
			}
		}
		return rulesProperties;
	}

}
