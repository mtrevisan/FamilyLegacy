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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Properties;


/*
 * Reads an input stream of bytes representing ANSEL-encoded characters, and delivers a stream of UNICODE characters.
 * Conversion tables based upon <a href="http://www.heiner-eichmann.de/gedcom/oldansset.htm">ANSEL to Unicode Conversion Table</a> and
 * <a href="http://lcweb2.loc.gov/diglib/codetables/45.html">Code Table Extended Latin (ANSEL)</a>.
 */
class AnselInputStreamReader extends InputStreamReader{

	private static final Logger LOGGER = LoggerFactory.getLogger(AnselInputStreamReader.class);


	static final String CHARACTER_ENCODING = "ANSEL";
	static final String ANSEL_FORMATTER = "0x%04X";

	static final Properties ANSEL = getProperties("/ansel.properties");
	static final int UNICODE_REPLACEMENT_CHARACTER = 0xFFFD;


	private final InputStream input;
	private int pending;
	/** The characters already decoded, waiting to be returned (the base letter comes before its marks in Unicode). */
	private final Deque<Integer> decoded = new ArrayDeque<>();


	AnselInputStreamReader(final InputStream in) throws IOException{
		super(in);

		input = in;
		//read one character ahead to cope with non-spacing diacriticals
		pending = input.read();
	}

	public int read() throws IOException{
		if(!decoded.isEmpty())
			return decoded.poll();

		final int c = decode();
		if(c < 0 || !isCombining(c))
			return c;

		//ANSEL puts the non-spacing marks before the letter they belong to, Unicode after it (and the mark closest to the letter
		//comes first): the letter is returned first, then the marks, reversed
		final List<Integer> marks = new ArrayList<>();
		marks.add(c);
		int next;
		while((next = decode()) >= 0 && isCombining(next))
			marks.add(next);
		if(next < 0){
			//no letter: the marks are returned as they are
			decoded.addAll(marks.subList(1, marks.size()));
			return c;
		}

		if(next < ' '){
			//a line break (or any control character) ends what the marks belong to: they stay where they are
			decoded.addAll(marks.subList(1, marks.size()));
			decoded.add(next);
			return c;
		}

		for(int i = marks.size() - 1; i >= 0; i --)
			decoded.add(marks.get(i));
		return next;
	}

	@Override
	public boolean ready(){
		return (!decoded.isEmpty() || pending >= 0);
	}

	/** Whether the character is a non-spacing mark (a combining diacritic, or a half of a double one). */
	private static boolean isCombining(final int c){
		return ((c >= 0x0300 && c <= 0x036F) || (c >= 0xFE20 && c <= 0xFE2F));
	}

	/** Decodes the next character (or the next pair, if the table has a precomposed one), marks included. */
	private int decode() throws IOException{
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
			final String chr = ANSEL.getProperty(String.format(ANSEL_FORMATTER, b * 256 + pending));
			final int u = (chr != null? Integer.decode(chr): -1);
			if(u > 0){
				pending = input.read();
				return u;
			}
		}

		//else match one char
		final String chr = ANSEL.getProperty(String.format(ANSEL_FORMATTER, b));
		return (chr != null? Integer.decode(chr): UNICODE_REPLACEMENT_CHARACTER);
	}

	/**
	 * Fill a supplied buffer with UNICODE characters.
	 */
	public int read(final char[] buffer, final int offset, final int length) throws IOException{
		if(length == 0)
			return 0;
		if(pending < 0 && decoded.isEmpty())
			//have already hit EOF
			return -1;

		for(int i = 0; i < length; i ++){
			final int c = read();
			if(c < 0)
				return i;

			buffer[offset + i] = (char)c;
		}
		return length;
	}

	public String getEncoding(){
		return CHARACTER_ENCODING;
	}


	public static Properties getProperties(final String filename){
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
