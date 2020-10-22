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
package io.github.mtrevisan.familylegacy.gedcom;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Properties;


/*
 * Produces an output stream of bytes representing ANSEL-encoded characters, from UNICODE characters supplied as
 * input.
 * Conversion tables based upon <a href="http://www.heiner-eichmann.de/gedcom/oldansset.htm">ANSEL to Unicode Conversion Table</a> and
 * <a href="http://lcweb2.loc.gov/diglib/codetables/45.html">Code Table Extended Latin (ANSEL)</a>.
 */
class AnselOutputStreamWriter extends OutputStreamWriter{

	private static final Properties ANSEL_REVERSED = new Properties();
	static{
		for(final Map.Entry<Object, Object> entry : AnselInputStreamReader.ANSEL.entrySet())
			ANSEL_REVERSED.put(entry.getValue(), entry.getKey());
	}


	private final OutputStream output;


	AnselOutputStreamWriter(final OutputStream out){
		super(out);

		output = out;
	}

	/*
	 * Write one UNICODE character.
	 */
	public void write(final int chr) throws IOException{
		if(chr < 128)
			output.write(chr);
		else{
			final int ansel = convert(chr);
			if(ansel < 256)
				output.write(ansel);
			else{
				output.write(ansel / 256);
				output.write(ansel % 256);
			}
		}
	}

	/*
	 * Write part of an array of UNICODE characters.
	 */
	public void write(final char[] buffer, final int offset, final int length) throws IOException{
		for(int i = offset; i < offset + length; i ++)
			write(buffer[i]);
	}

	/*
	 * Write a string of UNICODE characters.
	 */
	public void write(final String s) throws IOException{
		for(int i = 0; i < s.length(); i ++)
			write(s.charAt(i));
	}

	/*
	 * Determine the character code in use.
	 */
	public String getEncoding(){
		return AnselInputStreamReader.CHARACTER_ENCODING;
	}

	/*
	 * Conversion table for UNICODE to Ansel.
	 */
	private int convert(final int unicode){
		final String u = String.format(AnselInputStreamReader.ANSEL_FORMATTER, unicode);
		final String chr = (String)ANSEL_REVERSED.get(u);
		return (chr != null? Integer.parseInt(chr, 16): AnselInputStreamReader.UNICODE_REPLACEMENT_CHARACTER);
	}

}
