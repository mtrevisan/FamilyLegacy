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

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


public class GEDCOMHelper{

	private static final Pattern UPPERCASE_LETTERS_THEN_DIGITS = Pattern.compile("^[A-Z]+\\d+$");
	private static final Pattern PATTERN_SPACES = Pattern.compile("\\s+");
	/** The version of the GEDCOM 7 header, {@code 1 GEDC} followed by {@code 2 VERS 7.x}. */
	private static final Pattern GEDCOM7_VERSION = Pattern.compile("^\\s*1\\s+GEDC\\s*$\\s*^\\s*2\\s+VERS\\s+7",
		Pattern.MULTILINE);
	private static final int HEADER_PEEK_SIZE = 8192;


	private GEDCOMHelper(){}


	@SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
	public static BufferedReader getBufferedReader(InputStream in) throws IOException{
		if(!in.markSupported())
			in = new BufferedInputStream(in);

		// 1. Primary check: GEDCOM 7 is always UTF-8, otherwise detect charset directly from byte sequence
		String charEncoding = (declaresGEDCOM7(in)
			? StandardCharsets.UTF_8.name()
			: GEDCOMCharsetDetector.detectCharsetFromBytes(in));

		// 2. Fallback check: Read '1 CHAR' header if byte detection is inconclusive
		if(charEncoding == null){
			in.mark(Integer.MAX_VALUE);
			charEncoding = readCorrectedCharsetName(in);
			in.reset();

			// Retry reading header using UTF-16 reader if initial read failed
			if(StringUtils.isEmpty(charEncoding)){
				in.mark(Integer.MAX_VALUE);
				try(BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_16))){
					charEncoding = readCorrectedCharsetName(br);
				}
				in.reset();
			}
		}

		// 3. Default fallback if no charset was detected
		if(StringUtils.isEmpty(charEncoding))
			charEncoding = AnselInputStreamReader.CHARACTER_ENCODING;

		// 4. Skip initial garbage/junk characters before '0' (0 HEAD)
		return buildBufferedReaderSkippingJunk(in, charEncoding);
	}

	/** Whether the header declares GEDCOM 7 ({@code 1 GEDC} followed by {@code 2 VERS 7.x}). */
	private static boolean declaresGEDCOM7(final InputStream in) throws IOException{
		in.mark(HEADER_PEEK_SIZE);
		try{
			// the header is ASCII, whatever the charset of the file (except UTF-16, that GEDCOM 7 forbids)
			final byte[] header = in.readNBytes(HEADER_PEEK_SIZE);
			return GEDCOM7_VERSION.matcher(new String(header, StandardCharsets.ISO_8859_1)).find();
		}
		finally{
			in.reset();
		}
	}

	private static BufferedReader buildBufferedReaderSkippingJunk(InputStream in, String charEncoding) throws IOException{
		final InputStreamReader reader = AnselInputStreamReader.CHARACTER_ENCODING.equalsIgnoreCase(charEncoding)
			? new AnselInputStreamReader(in)
			: new InputStreamReader(in, charEncoding);

		final BufferedReader bufferedReader = new BufferedReader(reader);

		// Skip junk until the first GEDCOM record marker '0'
		bufferedReader.mark(8192);
		int c;
		while((c = bufferedReader.read()) != -1){
			if(c == '0'){
				bufferedReader.reset();

				break;
			}
			bufferedReader.mark(8192);
		}

		return bufferedReader;
	}

	private static String readCorrectedCharsetName(final InputStream is) throws IOException{
		return readCorrectedCharsetName(new BufferedReader(new InputStreamReader(is)));
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
				String[] split = PATTERN_SPACES.split(line, 3);
				if(split.length == 3){
					final boolean level1 = "1".equals(split[0]);
					if(level1){
						final String id = split[1];
						if(generatorName == null && GEDCOMMapper.TAG_SOURCE.equals(id))
							generatorName = split[2];
						else if("CHAR".equals(id)){
							//get encoding
							encoding = split[2].toUpperCase(Locale.ROOT);
							//look for version
							line = in.readLine();
							if(line != null){
								split = PATTERN_SPACES.split(line, 3);
								if(split.length == 3 && "2".equals(split[0]) && GEDCOMMapper.TAG_VERSION.equals(split[1]))
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
			//ASCII followed by VERS macOS Roman is MACINTOSH
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
			//not found, use default character encoding
			encoding = "UTF-8";
		return encoding;
	}


	public static String extractId(GEDCOMNode node, String idPrefix){
		String id;
		String xref = node.getXrefId();
		if(xref != null){
			String cleaned = IDNormalizer.clean(xref);
			if(isValidIdFormat(cleaned)){
				id = cleaned;
			} else {
				id = IDGenerator.nextId(idPrefix);
			}
		} else {
			id = IDGenerator.nextId(idPrefix);
		}
		IDGenerator.registerExistingId(id);
		return id;
	}

	public static String cleanId(String id){
		if(id != null && id.startsWith("@") && id.endsWith("@"))
			return IDNormalizer.clean(id);
		return (isUppercaseLettersThenNumbers(id)? id: null);
	}

	/**
	 * Returns true if the string starts with one or more uppercase letters
	 * followed by one or more digits, and contains nothing else.
	 */
	public static boolean isUppercaseLettersThenNumbers(String value){
		return value != null && UPPERCASE_LETTERS_THEN_DIGITS.matcher(value).matches();
	}

	/**
	 * Checks if the ID matches the FLEF format: letters followed by digits.
	 */
	private static boolean isValidIdFormat(String id){
		return id != null && UPPERCASE_LETTERS_THEN_DIGITS.matcher(id).matches();
	}


	public static GEDCOMNode findFirstChild(GEDCOMNode node, String tag){
		if(node == null)
			return null;

		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.findFirst()
			.orElse(null);
	}

	public static List<GEDCOMNode> findChildren(GEDCOMNode node, String tag){
		if(node == null)
			return Collections.emptyList();
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.toList();
	}


	/**
	 * Returns the id of the record a GEDCOM pointer refers to.
	 *
	 * @param value	The value of a GEDCOM line (e.g. {@code @I12@}).
	 * @return	The id (e.g. {@code I12}), or {@code null} if the value is not a pointer.
	 */
	public static String pointer(final String value){
		if(value == null)
			return null;

		final String trimmed = value.trim();
		return (trimmed.length() > 2 && trimmed.charAt(0) == '@' && trimmed.charAt(trimmed.length() - 1) == '@'
			&& trimmed.charAt(1) != '#'
			? IDNormalizer.clean(trimmed)
			: null);
	}

	/** Returns the trimmed value of the first child with the given tag, or {@code null} if absent or blank. */
	public static String findFirstValue(final GEDCOMNode node, final String tag){
		final GEDCOMNode child = findFirstChild(node, tag);
		return (child != null? StringUtils.trimToNull(child.getValue()): null);
	}

	/** Whether the value of a GEDCOM line is the "Y" flag that only asserts that an event happened. */
	public static boolean isFlag(final String value){
		return ("Y".equalsIgnoreCase(value) || "N".equalsIgnoreCase(value));
	}


	/**
	 * Reconstructs the full note text from a GEDCOM NOTE node.
	 * Handles CONC and CONT children to concatenate lines correctly.
	 *
	 * @param noteNode the GEDCOM NOTE node
	 * @return the full text, or {@code null} if no text found
	 */
	public static String extractFullText(GEDCOMNode noteNode){
		if(noteNode == null)
			return null;

		StringBuilder sb = new StringBuilder();
		if(noteNode.getValue() != null && StringUtils.isNotEmpty(noteNode.getValue())){
			sb.append(noteNode.getValue());
		}
		final List<GEDCOMNode> children = noteNode.getChildren();
		for(GEDCOMNode child : children){
			String tag = child.getTag();
			if("CONC".equals(tag) || "CONT".equals(tag)){
				if(child.getValue() != null){
					if("CONT".equals(tag) && !sb.isEmpty())
						sb.append(StringUtils.LF);
					sb.append(child.getValue());
				}
			}
		}
		return (!sb.isEmpty()? sb.toString(): null);
	}

}
