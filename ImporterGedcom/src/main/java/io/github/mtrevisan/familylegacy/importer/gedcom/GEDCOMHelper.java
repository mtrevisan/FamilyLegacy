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
import org.apache.commons.lang3.Strings;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;


public class GEDCOMHelper{

	private static final Pattern UPPERCASE_LETTERS_THEN_DIGITS = Pattern.compile("^[A-Z]+\\d+$");
	private static final Pattern PATTERN_SPACES = Pattern.compile("\\s+");
	/** The version of the GEDCOM 7 header, {@code 1 GEDC} followed by {@code 2 VERS 7.x}. */
	private static final Pattern GEDCOM7_VERSION = Pattern.compile("^\\s*1\\s+GEDC\\s*$\\s*^\\s*2\\s+VERS\\s+7",
		Pattern.MULTILINE);
	private static final int HEADER_PEEK_SIZE = 8192;
	private static final int HEADER_MARK_LIMIT = 1 << 20;
	/** The charsets that files in a Unicode encoding often (wrongly) declare. */
	private static final Set<String> AMBIGUOUS_CHARSETS = Set.of("ASCII", "US-ASCII", "CP1252", "WINDOWS-1252", "ISO-8859-1",
		"ANSEL", "UTF-16", "UNICODEBIGUNMARKED");


	private GEDCOMHelper(){}


	@SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
	public static BufferedReader getBufferedReader(InputStream in) throws IOException{
		if(!in.markSupported())
			in = new BufferedInputStream(in);

		// GEDCOM 7 is always UTF-8, otherwise the charset comes from the byte order mark, the header and the bytes themselves
		final String charEncoding = (declaresGEDCOM7(in)
			? StandardCharsets.UTF_8.name()
			: chooseCharset(in));

		// Skip initial garbage/junk characters before '0' (0 HEAD)
		return buildBufferedReaderSkippingJunk(in, charEncoding);
	}

	/**
	 * Chooses the charset of a file that is not GEDCOM 7.
	 * <ol>
	 *   <li>a byte order mark is conclusive;</li>
	 *   <li>so is the charset declared in the header ({@code 1 CHAR}), which is more reliable than a guess made on the first bytes (an
	 *   ANSEL file can look like Latin-1), but for the ones that are commonly wrong: a file that declares ASCII, ANSI, ... and is in
	 *   fact in a Unicode encoding is read as such;</li>
	 *   <li>lacking a (known) declaration, the guess made on the bytes, or else UTF-8.</li>
	 * </ol>
	 */
	private static String chooseCharset(final InputStream in) throws IOException{
		final String bom = GEDCOMCharsetDetector.detectBom(in);
		if(bom != null)
			return bom;

		String declared = readCorrectedCharsetName(in);
		if(declared != null && !isSupported(declared))
			declared = null;
		String detected = GEDCOMCharsetDetector.detectHeuristically(in);
		//the name of a charset that the guess found may not be known to this runtime
		if(detected != null && !isSupported(detected))
			detected = null;

		if(declared == null)
			return (detected != null? detected: StandardCharsets.UTF_8.name());
		if(AMBIGUOUS_CHARSETS.contains(declared.toUpperCase(Locale.ROOT)) && detected != null
				&& detected.toUpperCase(Locale.ROOT).startsWith("UTF"))
			return detected;
		return declared;
	}

	private static boolean isSupported(final String charsetName){
		if(AnselInputStreamReader.CHARACTER_ENCODING.equalsIgnoreCase(charsetName))
			return true;

		try{
			return Charset.isSupported(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			return false;
		}
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

	/**
	 * Reads the first lines of the header, looking for the declared charset (the stream is left where it was).
	 *
	 * @return	The name of the charset, corrected for the generators that are known to write it wrong, {@code null} if the header does
	 * 	not declare it (or it cannot be read: e.g. a UTF-16 file without a byte order mark).
	 */
	private static String readCorrectedCharsetName(final InputStream in) throws IOException{
		in.mark(HEADER_MARK_LIMIT);
		try{
			//the header is ASCII, whatever the charset of the file; the reader is not closed, as that would close the stream
			final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
			return readCorrectedCharsetName(reader);
		}
		finally{
			in.reset();
		}
	}

	private static String readCorrectedCharsetName(final BufferedReader in) throws IOException{
		//try to read only the first 100 lines of the file attempting to get the char encoding.
		String line;
		String generatorName = null;
		String encoding = null;
		String version = null;
		String levelOneTag = null;
		for(int i = 0; i < 100 && (line = in.readLine()) != null; i ++){
			final String[] split = PATTERN_SPACES.split(StringUtils.stripStart(line, null), 3);
			if(split.length < 2)
				continue;

			if("0".equals(split[0])){
				//the header ends where the next record starts
				if(i > 0 && !GEDCOMMapper.TAG_HEADER.equals(split[1]))
					break;
				continue;
			}
			if("1".equals(split[0])){
				levelOneTag = split[1];
				if(split.length < 3)
					continue;

				if(generatorName == null && GEDCOMMapper.TAG_SOURCE.equals(levelOneTag))
					generatorName = split[2].trim();
				else if("CHAR".equals(levelOneTag))
					//get encoding
					encoding = split[2].trim().toUpperCase(Locale.ROOT);
			}
			else if("2".equals(split[0]) && split.length == 3 && "CHAR".equals(levelOneTag) && GEDCOMMapper.TAG_VERSION.equals(split[1]))
				//look for version
				version = split[2].trim();
		}

		return (encoding != null? getCorrectedCharsetName(generatorName, encoding, version): null);
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
			if(isUppercaseLettersThenNumbers(cleaned)){
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

	/**
	 * Whether a custom tag (underscore tag) of an individual or of a family is an event: it holds a date or a place.
	 * <p>
	 * The custom tags without data (flags, identifiers, creation and update stamps) are not.
	 */
	public static boolean isCustomEvent(final GEDCOMNode node){
		final String tag = node.getTag();
		if(!Strings.CS.startsWith(tag, "_") || GEDCOMMapper.CUSTOM_NON_EVENT_TAGS.contains(tag.toUpperCase(Locale.ROOT))
				|| GEDCOMMapper.TAG_TODO.equalsIgnoreCase(tag) || GEDCOMMapper.TAG_MARRIED_NAME.equalsIgnoreCase(tag) || GEDCOMMapper.TAG_AKA.equalsIgnoreCase(tag))
			return false;

		return (findFirstChild(node, GEDCOMMapper.TAG_DATE) != null || findFirstChild(node, GEDCOMMapper.TAG_PLACE) != null);
	}

}
