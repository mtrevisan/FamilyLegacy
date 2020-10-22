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

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;


final class GedcomHelper{

	private static final Pattern PATTERN_SPACES = Pattern.compile("\\s+");


	private GedcomHelper(){}

	@SuppressWarnings({"InjectedReferences", "ResultOfMethodCallIgnored"})
	static BufferedReader getBufferedReader(InputStream in) throws IOException{
		if(!in.markSupported())
			in = new BufferedInputStream(in);
		in.mark(Integer.MAX_VALUE);

		String charEncoding = readCorrectedCharsetName(in);
		in.reset();

		if(charEncoding.isEmpty()){
			//let's try again with a UTF-16 reader
			final BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_16));
			charEncoding = readCorrectedCharsetName(br);
			in.reset();

			if("UTF-16".equals(charEncoding)){
				//skip over junk at the beginning of the file
				InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_16);
				int cnt = 0;
				int c;
				while((c = reader.read()) != '0' && c != -1)
					cnt ++;

				in.reset();
				reader = new InputStreamReader(in, StandardCharsets.UTF_16);
				for(int i = 0; i < cnt; i ++)
					reader.read();
				return new BufferedReader(reader);
			}
		}

		if(charEncoding.isEmpty())
			//default
			charEncoding = AnselInputStreamReader.CHARACTER_ENCODING;

		//skip over junk at the beginning of the file
		in.reset();
		int cnt = 0;
		int c;
		while((c = in.read()) != '0' && c != -1)
			cnt ++;

		in.reset();
		for(int i = 0; i < cnt; i ++)
			in.read();

		final InputStreamReader reader = (AnselInputStreamReader.CHARACTER_ENCODING.equals(charEncoding)?
			new AnselInputStreamReader(in): new InputStreamReader(in, charEncoding));

		return new BufferedReader(reader);
	}

	private static String readCorrectedCharsetName(final InputStream is) throws IOException{
		final BufferedReader r = new BufferedReader(new InputStreamReader(is));
		return readCorrectedCharsetName(r);
	}

	private static String readCorrectedCharsetName(final BufferedReader in) throws IOException{
		//try to read only the first 100 lines of the file attempting to get the char encoding.
		String line;
		String generatorName = null;
		String encoding = null;
		String version = null;
		for(int i = 0; i < 100; i ++){
			line = in.readLine();
			if(line != null){
				String[] split = PATTERN_SPACES.split(line.trim(), 3);
				if(split.length == 3){
					final boolean level1 = "1".equals(split[0]);
					if(level1){
						final String id = split[1];
						if(generatorName == null && "SOUR".equals(id))
							generatorName = split[2];
						else if("CHAR".equals(id) || "CHARACTER".equals(id)){
							//get encoding
							encoding = split[2].toUpperCase();
							//look for version
							line = in.readLine();
							if(line != null){
								split = PATTERN_SPACES.split(line.trim(), 3);
								if(split.length == 3 && "2".equals(split[0]) && "VERS".equals(split[1]))
									version = split[2];
							}
						}
					}
				}
			}
			if(generatorName != null && encoding != null)
				break;
		}

		return getCorrectedCharsetName(generatorName, encoding, version);
	}

	static String getCorrectedCharsetName(final String generatorName, String encoding, final String version){
		//correct incorrectly-assigned encoding values
		if("GeneWeb".equals(generatorName) && "ASCII".equals(encoding))
			//GeneWeb ASCII -> Cp1252 (ANSI)
			encoding = "Cp1252";
		else if("Geni.com".equals(generatorName) && "UNICODE".equals(encoding))
			//Geni.com UNICODE -> UTF-8
			encoding = "UTF-8";
		else if("Geni.com".equals(generatorName) && AnselInputStreamReader.CHARACTER_ENCODING.equals(encoding))
			//Geni.com ANSEL -> UTF-8
			encoding = "UTF-8";
		else if("GENJ".equals(generatorName) && "UNICODE".equals(encoding))
			//GENJ UNICODE -> UTF-8
			encoding = "UTF-8";
		//make encoding value java-friendly
		else if("ASCII".equals(encoding)){
			//ASCII followed by VERS MacOS Roman is MACINTOSH
			if("MacOS Roman".equals(version))
				encoding = "x-MacRoman";
		}
		else if("ATARIST_ASCII".equals(encoding))
			encoding = "ASCII";
		else if("MACROMAN".equals(encoding) || "MACINTOSH".equals(encoding))
			encoding = "x-MacRoman";
		else if("ANSI".equals(encoding) || "IBM WINDOWS".equals(encoding))
			encoding = "Cp1252";
		else if("WINDOWS-874".equals(encoding))
			encoding = "Cp874";
		else if("WINDOWS-1251".equals(encoding))
			encoding = "Cp1251";
		else if("WINDOWS-1254".equals(encoding))
			encoding = "Cp1254";
		else if("IBMPC".equals(encoding) || "IBM DOS".equals(encoding))
			encoding = "Cp850";
		else if("UNICODE".equals(encoding))
			encoding = "UTF-16";
		else if("UTF-16BE".equals(encoding))
			encoding = "UnicodeBigUnmarked";
		else if(encoding == null)
			//not found
			encoding = StringUtils.EMPTY;
		return encoding;
	}

}
