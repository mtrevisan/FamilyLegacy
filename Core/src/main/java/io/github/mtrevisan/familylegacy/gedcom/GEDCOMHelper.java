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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GEDCOMHelper{

	private static final Pattern PATTERN_SPACES = Pattern.compile("\\s+");
	/** A name followed by its phonemic transcription between slashes, e.g. {@code Dorato /do.ˈra.to/}. */
	private static final Pattern NAME_WITH_PHONETIC = Pattern.compile("^(?<name>[^/]+?)\\s*(?<phonetic>/[^/]+/)$");
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
						if(generatorName == null && "SOUR".equals(id))
							generatorName = split[2];
						else if("CHAR".equals(id)){
							//get encoding
							encoding = split[2].toUpperCase(Locale.ROOT);
							//look for version
							line = in.readLine();
							if(line != null){
								split = PATTERN_SPACES.split(line, 3);
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
		return value != null && value.matches("^[A-Z]+\\d+$");
	}

	/**
	 * Checks if the ID matches the FLEF format: letters followed by digits.
	 */
	private static boolean isValidIdFormat(String id){
		return id != null && id.matches("^[A-Z]+\\d+$");
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
	private static boolean isFlag(final String value){
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


	public static void transferValue(FLEFRecord parent, String tag, GEDCOMNode node){
		if(node != null && StringUtils.isNotEmpty(node.getValue()))
			parent.addChild(FLEFRecord.createChildWithTagAndValue(tag, node.getValue()));
	}


	/*
	NOTE_STRUCTURE :=
	[
	n NOTE @<XREF:NOTE>@    {1:1}
	|
	n NOTE [ <SUBMITTER_TEXT> | <NULL> ]    {1:1}
	  +1 [ CONC | CONT ] <SUBMITTER_TEXT>    {0:M}
	]
	*/
	public static void attachNote(FLEFRecord parent,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap){
		attachNote(parent, node, noteRawMap, null);
	}

	/**
	 * Attaches a NoteStructure built from a NOTE_STRUCTURE (or from any GEDCOM line whose text is a note).
	 *
	 * @param title	The title of the note, may be {@code null}.
	 */
	public static void attachNote(FLEFRecord parent,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, String title){
		if(node == null || StringUtils.isBlank(node.getValue()))
			return;

		final GEDCOMNode noteNode = resolveNote(node, noteRawMap);
		if(noteNode == null)
			return;

		final FLEFRecord note = createNoteStruct(title, extractFullText(noteNode), noteNode);
		if(note == null)
			return;

		attachNoteFormat(note, noteNode);
		parent.addChild(note);
	}

	/**
	 * Returns the NOTE record a NOTE structure points to, or the structure itself when it holds the text.
	 *
	 * @return	The node holding the text of the note, or {@code null} if the record does not exist.
	 */
	public static GEDCOMNode resolveNote(final GEDCOMNode node, final Map<String, GEDCOMNode> noteRawMap){
		final String noteId = (node != null? pointer(node.getValue()): null);
		if(noteId == null)
			return node;

		return (noteRawMap != null? noteRawMap.get(noteId): null);
	}

	/*
	(GEDCOM 7)
	n NOTE <Text>
	  +1 MIME <MediaType>    {0:1}
	  +1 LANG <Language>    {0:1}
	  +1 TRAN <Text>    {0:M}
	    +2 MIME <MediaType>    {0:1}
	    +2 LANG <Language>    {0:1}
	*/
	/** Attaches the media type, the language, and the translations of a note, before its audit. */
	private static void attachNoteFormat(final FLEFRecord note, final GEDCOMNode noteNode){
		final List<FLEFRecord> format = new ArrayList<>();
		final String mime = findFirstValue(noteNode, "MIME");
		if(mime != null)
			format.add(FLEFRecord.createChildWithTagAndValue("mime", mime));
		final String locale = findFirstValue(noteNode, "LANG");
		if(locale != null)
			format.add(FLEFRecord.createChildWithTagAndValue("locale", locale));
		for(final GEDCOMNode tranNode : findChildren(noteNode, "TRAN")){
			final String text = StringUtils.trimToNull(extractFullText(tranNode));
			if(text == null)
				continue;

			final FLEFRecord translation = FLEFRecord.createChildWithTag("translation")
				.addChild(FLEFRecord.createChildWithTagAndValue("text", text));
			final String translationLocale = findFirstValue(tranNode, "LANG");
			if(translationLocale != null)
				translation.addChild(FLEFRecord.createChildWithTagAndValue("locale", translationLocale));
			format.add(translation);
		}
		note.getChildren()
			.addAll(note.getChildren().size() - 1, format);
	}

	public static FLEFRecord createNoteStruct(String title, String text, GEDCOMNode node){
		if(StringUtils.isBlank(text))
			return null;

		final FLEFRecord note = FLEFRecord.createChildWithTag("note");
		if(StringUtils.isNotBlank(title))
			note.addChild(FLEFRecord.createChildWithTagAndValue("title", title));
		return note.addChild(FLEFRecord.createChildWithTagAndValue("text", text.trim()))
			.addChild(AuditBuilder.build(node));
	}


	/*
	PERSONAL_NAME_STRUCTURE :=
	n NAME <NAME_PERSONAL>    {1:1}
	  +1 TYPE <NAME_TYPE>    {0:1}
	  +1 <<PERSONAL_NAME_PIECES>>    {0:1}
	  +1 FONE <NAME_PHONETIC_VARIATION>    {0:M}
	    +2 TYPE <PHONETIC_TYPE>    {1:1}
	    +2 <<PERSONAL_NAME_PIECES>>    {0:1}
	  +1 ROMN <NAME_ROMANIZED_VARIATION>    {0:M}
	    +2 TYPE <ROMANIZED_TYPE>    {1:1}
	    +2 <<PERSONAL_NAME_PIECES>>    {0:1}

	PERSONAL_NAME_PIECES :=
	  n NPFX, n GIVN, n NICK, n SPFX, n SURN, n NSFX    {0:1}
	  n <<NOTE_STRUCTURE>>, n <<SOURCE_CITATION>>    {0:M}
	*/
	/**
	 * Parses a PersonalNameStructure (for individuals) extracting all sub-tags
	 * (NPFX, GIVN, NICK, SPFX, SURN, NSFX), phonetic and romanized variations, inline sources, and notes.
	 */
	public static void attachPersonalNameStructure(FLEFRecord parent,
			GEDCOMNode nameNode, FLEFModel model, Map<String, GEDCOMNode> noteRawMap){
		if(nameNode == null)
			return;

		final FLEFRecord name = createPersonalName(nameNode.getValue(), nameNode,
			GEDCOMMapper.mapNameType(findFirstValue(nameNode, "TYPE")));
		if(name == null)
			return;

		for(GEDCOMNode foneNode : findChildren(nameNode, "FONE"))
			attachNameVariant(name, foneNode, "phonetic", "Phonetic variation");
		for(GEDCOMNode romnNode : findChildren(nameNode, "ROMN"))
			attachNameVariant(name, romnNode, "transcription", "Romanized variation");
		// GEDCOM 7 replaces FONE and ROMN with translations, distinguished by their language
		for(GEDCOMNode tranNode : findChildren(nameNode, "TRAN"))
			attachNameVariant(name, tranNode, "transcription", "Translated name");

		for(GEDCOMNode sourNode : findChildren(nameNode, "SOUR"))
			attachSource(name, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : findChildren(nameNode, "NOTE"))
			attachNote(name, noteNode, noteRawMap);

		parent.addChild(name);
	}

	/**
	 * Builds a PersonalNameStructure.
	 *
	 * @param value	The name, in GEDCOM form (e.g. {@code John /Smith/ Jr.}).
	 * @param piecesNode	The node holding the PERSONAL_NAME_PIECES, may be {@code null}.
	 * @param type	The type of the name, may be {@code null}.
	 * @return	The structure, or {@code null} if the name has no parts.
	 */
	public static FLEFRecord createPersonalName(final String value, final GEDCOMNode piecesNode, final String type){
		final Map<String, List<String>> pieces = extractNamePieces(value, piecesNode);
		if(pieces.isEmpty())
			return null;

		final FLEFRecord name = FLEFRecord.createChildWithTag("name");
		if(type != null)
			name.addChild(FLEFRecord.createChildWithTagAndValue("type", type));
		for(final Map.Entry<String, List<String>> entry : pieces.entrySet())
			for(final String piece : entry.getValue())
				name.addChild(createNamePart(entry.getKey(), piece));
		return name;
	}

	/**
	 * Builds a NamePartStructure; a phonemic transcription following the value between slashes (e.g.
	 * {@code Milio /ˈmi.ljo/}) becomes its IPA variant.
	 */
	public static FLEFRecord createNamePart(final String type, final String value){
		String partValue = value;
		String phonetic = null;
		final Matcher matcher = NAME_WITH_PHONETIC.matcher(value);
		if(matcher.matches()){
			partValue = matcher.group("name");
			phonetic = matcher.group("phonetic");
		}

		final FLEFRecord part = FLEFRecord.createChildWithTag("part")
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type))
			.addChild(FLEFRecord.createChildWithTagAndValue("value", partValue));
		if(phonetic != null)
			part.addChild(FLEFRecord.createChildWithTag("variant")
				.addChild(FLEFRecord.createChildWithTag("phonetic")
					.addChild(FLEFRecord.createChildWithTagAndValue("system", "ipa"))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", phonetic))));
		return part;
	}

	/**
	 * Extracts the name parts, in display order, from a name in GEDCOM form and from its PERSONAL_NAME_PIECES (which take
	 * precedence).
	 *
	 * @return	The values of the parts, by FLEF part type, in display order.
	 */
	private static Map<String, List<String>> extractNamePieces(final String value, final GEDCOMNode piecesNode){
		String given = null;
		String surname = null;
		String suffix = null;
		if(StringUtils.isNotBlank(value)){
			final int slash1 = value.indexOf('/');
			final int slash2 = (slash1 >= 0? value.indexOf('/', slash1 + 1): -1);
			if(slash2 > slash1){
				given = value.substring(0, slash1);
				surname = value.substring(slash1 + 1, slash2);
				suffix = value.substring(slash2 + 1);
			}
			else
				given = value.replace("/", StringUtils.SPACE);
		}

		final Map<String, List<String>> pieces = new LinkedHashMap<>();
		putNamePieces(pieces, "prefix", findFirstValue(piecesNode, "NPFX"));
		putNamePieces(pieces, "given", ObjectUtils.firstNonNull(findFirstValue(piecesNode, "GIVN"),
			StringUtils.trimToNull(given)));
		putNamePieces(pieces, "nickname", findFirstValue(piecesNode, "NICK"));
		final String surnamePrefix = findFirstValue(piecesNode, "SPFX");
		final String surnames = ObjectUtils.firstNonNull(findFirstValue(piecesNode, "SURN"), StringUtils.trimToNull(surname));
		if(surnames != null){
			//the surname prefix (e.g. "de", "van der") is part of the family name
			final String[] families = StringUtils.split(surnames, ',');
			if(surnamePrefix != null && families.length > 0 && !Strings.CI.startsWith(families[0].trim(), surnamePrefix))
				families[0] = surnamePrefix + StringUtils.SPACE + families[0].trim();
			putNamePieces(pieces, "family", String.join(",", families));
		}
		putNamePieces(pieces, "suffix", ObjectUtils.firstNonNull(findFirstValue(piecesNode, "NSFX"),
			StringUtils.trimToNull(suffix)));
		return pieces;
	}

	/** Puts the comma-separated values of a name piece. */
	private static void putNamePieces(final Map<String, List<String>> pieces, final String type, final String values){
		if(values == null)
			return;

		final List<String> list = new ArrayList<>();
		for(final String piece : StringUtils.split(values, ','))
			if(StringUtils.isNotBlank(piece))
				list.add(StringUtils.normalizeSpace(piece));
		if(!list.isEmpty())
			pieces.put(type, list);
	}

	/**
	 * Attaches a phonetic, romanized, or translated (GEDCOM 7) variation of a name to the matching parts of the name.
	 * <p>
	 * A variation that cannot be split into parts is kept as a note of the name.
	 *
	 * @param kind	The kind of variant ({@code phonetic} or {@code transcription}).
	 */
	private static void attachNameVariant(final FLEFRecord name, final GEDCOMNode variantNode, final String kind,
			final String noteTitle){
		final String typeValue = ObjectUtils.firstNonNull(findFirstValue(variantNode, "TYPE"), findFirstValue(variantNode, "LANG"));
		final String system = ObjectUtils.firstNonNull(GEDCOMMapper.toTypeToken(findFirstValue(variantNode, "TYPE")),
			"unspecified");
		// the language of a translation tells how the name was adapted (e.g. anglicized, romanized)
		final String transcriptionType = ("transcription".equals(kind)
			? GEDCOMMapper.mapTranslationType(findFirstValue(variantNode, "LANG"))
			: null);

		final Map<String, List<String>> pieces = extractNamePieces(variantNode.getValue(), variantNode);
		final Map<String, Integer> occurrences = new HashMap<>();
		final List<FLEFRecord> parts = FLEFRecordHelper.findChildren(name, "part");
		//a variation without the surname delimiters cannot be matched to the parts of a name made of more parts
		final boolean splittable = (parts.size() == 1 || StringUtils.contains(variantNode.getValue(), '/')
			|| findFirstChild(variantNode, "GIVN") != null || findFirstChild(variantNode, "SURN") != null);
		boolean attached = false;
		if(splittable)
			for(final FLEFRecord part : parts){
				final String partType = FLEFRecordHelper.getChildValue(part, "type");
				final int index = occurrences.merge(partType, 1, Integer::sum) - 1;
				List<String> values = pieces.get(partType);
				if(values == null && parts.size() == 1)
					values = pieces.values().stream()
						.flatMap(List::stream)
						.toList();
				if(values != null && index < values.size()){
					final FLEFRecord variant = FLEFRecord.createChildWithTag(kind)
						.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
					if(transcriptionType != null)
						variant.addChild(FLEFRecord.createChildWithTagAndValue("type", transcriptionType));
					part.addChild(FLEFRecord.createChildWithTag("variant")
						.addChild(variant.addChild(FLEFRecord.createChildWithTagAndValue("value", values.get(index)))));
					attached = true;
				}
			}
		if(!attached)
			name.addChild(createNoteStruct(noteTitle + (typeValue != null? " (" + typeValue + ")": StringUtils.EMPTY),
				variantNode.getValue(), variantNode));
	}


	/*
	SOURCE_CITATION :=
	[
	n SOUR @<XREF:SOUR>@    {1:1}
	  +1 PAGE <WHERE_WITHIN_SOURCE>    {0:1}
	  +1 EVEN <EVENT_TYPE_CITED_FROM>    {0:1}
		 +2 ROLE <ROLE_IN_EVENT>    {0:1}
	  +1 DATA    {0:1}
		 +2 DATE <ENTRY_RECORDING_DATE>    {0:1}
		 +2 TEXT <TEXT_FROM_SOURCE>    {0:M}
			+3 [ CONC | CONT ] <TEXT_FROM_SOURCE>    {0:M}
	  +1 <<MULTIMEDIA_LINK>>    {0:M}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 QUAY <CERTAINTY_ASSESSMENT>    {0:1}
	|
	n SOUR <SOURCE_DESCRIPTION>    {1:1}
	  +1 [ CONC | CONT ] <SOURCE_DESCRIPTION>    {0:M}
	  +1 TEXT <TEXT_FROM_SOURCE>    {0:M}
		 +2 [ CONC | CONT ] <TEXT_FROM_SOURCE>    {0:M}
	  +1 <<MULTIMEDIA_LINK>>    {0:M}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 QUAY <CERTAINTY_ASSESSMENT>    {0:1}
	]
	 */
	/**
	 * Attaches a SourceCitation built from a SOURCE_CITATION.
	 * <p>
	 * The source records must be already in the model; a citation of an unrecorded source (a source description) creates its
	 * SourceRecord. Everything that GEDCOM records about the citation stays on the citation: the entry recording date and the
	 * cited event go into the locator, the text and the multimedia into the extracts, the notes into the remarks of the
	 * extract (or, when there is no extract, into the notes of the parent), the certainty assessment into the evidence
	 * qualifiers.
	 */
	public static void attachSource(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap){
		if(node == null)
			return;

		final List<String> locator = new ArrayList<>();
		final List<FLEFRecord> extracts = new ArrayList<>();
		FLEFRecord source;
		String sourceId = pointer(node.getValue());
		if(sourceId != null){
			source = model.getRecordById(sourceId);
			if(source == null){
				// dangling pointer: keep the citation with a placeholder source
				source = FLEFRecord.createMainRecord(sourceId, SourceReader.TYPE)
					.addChild(FLEFRecord.createChildWithTag("title")
						.addChild(FLEFRecord.createChildWithTagAndValue("value", "Source " + sourceId)))
					.addChild(AuditBuilder.build(node));
				model.addRecord(source);
			}
		}
		else{
			// a source description: the text is the title of an unrecorded source (GEDCOM 7 cites it as @VOID@, without text)
			final String description = ObjectUtils.firstNonNull(StringUtils.trimToNull(extractFullText(node)), "Unspecified source");
			source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceReader.ID_PREFIX), SourceReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag("title")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", description)))
				.addChild(AuditBuilder.build(node));
			sourceId = Deduplicator.getDeduplicatedRecordId(model, source);
			source = model.getRecordById(sourceId);
		}

		final String page = findFirstValue(node, "PAGE");
		if(page != null)
			locator.add(page);
		final GEDCOMNode evenNode = findFirstChild(node, "EVEN");
		if(evenNode != null && StringUtils.isNotBlank(evenNode.getValue())){
			final String role = findFirstValue(evenNode, "ROLE");
			locator.add("Event: " + evenNode.getValue().trim() + (role != null? " (role: " + role + ")": StringUtils.EMPTY));
		}
		final GEDCOMNode dataNode = findFirstChild(node, "DATA");
		final String entryDate = findFirstValue(dataNode, "DATE");
		if(entryDate != null)
			locator.add("Date: " + entryDate);

		// a citation of a recorded source has its texts in DATA, a source description directly
		final List<GEDCOMNode> textNodes = new ArrayList<>(findChildren(node, "TEXT"));
		textNodes.addAll(findChildren(dataNode, "TEXT"));
		for(final GEDCOMNode textNode : textNodes){
			final String text = extractFullText(textNode);
			if(StringUtils.isNotBlank(text)){
				final FLEFRecord extract = FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", text.trim()))
					.addChild(FLEFRecord.createChildWithTagAndValue("type", "verbatim"));
				// GEDCOM 7 states the language of the text
				final String locale = findFirstValue(textNode, "LANG");
				if(locale != null)
					extract.addChild(FLEFRecord.createChildWithTagAndValue("locale", locale));
				extracts.add(extract);
			}
		}
		for(final GEDCOMNode objeNode : findChildren(node, "OBJE")){
			final FLEFRecord crop = buildCrop(objeNode);
			for(final String mediaId : resolveMedia(model, objeNode, noteRawMap)){
				// a media of an extract must be a media of the source
				addMediaReference(source, mediaId);
				extracts.add(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTag("media_part")
						.addChild(FLEFRecord.createChildWithTagAndValue("media", mediaId))
						.addChild(crop)));
			}
		}

		final FLEFRecord citation = FLEFRecord.createChildWithTag("source")
			.addChild(FLEFRecord.createChildWithTagAndValue("source", sourceId));
		if(!locator.isEmpty())
			citation.addChild(FLEFRecord.createChildWithTagAndValue("locator", String.join(", ", locator)));
		for(final GEDCOMNode noteNode : findChildren(node, "NOTE")){
			if(extracts.isEmpty())
				attachNote(parent, noteNode, noteRawMap, "Citation of " + sourceId);
			else{
				final String noteId = pointer(noteNode.getValue());
				final String text = extractFullText(noteId != null && noteRawMap != null? noteRawMap.get(noteId): noteNode);
				if(StringUtils.isNotBlank(text))
					extracts.getFirst()
						.addChild(FLEFRecord.createChildWithTagAndValue("remark", text.trim()));
			}
		}
		for(final FLEFRecord extract : extracts)
			citation.addChild(extract);
		//GEDCOM has no way to cite a source against an assertion
		citation.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS));
		citation.addChild(buildEvidence(node));

		parent.addChild(citation);
	}

	/**
	 * Maps a QUAY (CERTAINTY_ASSESSMENT) to the evidence qualifiers.
	 * <ul>
	 *   <li>0 = unreliable evidence or estimated data;</li>
	 *   <li>1 = questionable reliability of evidence;</li>
	 *   <li>2 = secondary evidence, data officially recorded sometime after event;</li>
	 *   <li>3 = direct and primary evidence used, or by dominance of the evidence.</li>
	 * </ul>
	 */
	private static FLEFRecord buildEvidence(final GEDCOMNode citationNode){
		final String quay = findFirstValue(citationNode, "QUAY");
		if(quay == null)
			return null;

		final FLEFRecord evidence = FLEFRecord.createChildWithTag("evidence");
		switch(quay){
			case "3" -> evidence.addChild(FLEFRecord.createChildWithTagAndValue("information_type",
					EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY))
				.addChild(FLEFRecord.createChildWithTagAndValue("evidence_type", "direct"));
			case "2" -> evidence.addChild(FLEFRecord.createChildWithTagAndValue("information_type",
				EvidenceQualifiersReader.INFORMATION_TYPE_SECONDARY));
			default -> evidence.addChild(FLEFRecord.createChildWithTagAndValue("information_type",
				EvidenceQualifiersReader.INFORMATION_TYPE_UNDETERMINED));
		}
		return evidence;
	}


	/*
	MULTIMEDIA_LINK :=
	[
	n OBJE @<XREF:OBJE>@    {1:1}
	|
	n OBJE    {1:1}
	  +1 FILE <MULTIMEDIA_FILE_REFN>    {1:M}
		 +2 FORM <MULTIMEDIA_FORMAT>    {1:1}
			+3 MEDI <SOURCE_MEDIA_TYPE>    {0:1}
	  +1 TITL <DESCRIPTIVE_TITLE>    {0:1}
	]
	(GEDCOM 5.5 has FORM, TITL, FILE, NOTE as siblings)
	 */
	/**
	 * Attaches a MULTIMEDIA_LINK to a record.
	 * <ul>
	 *   <li>to a SourceRecord, as one of its media;</li>
	 *   <li>to any other record, as the evidence of a SourceRecord made of the media; the preferred image of an individual
	 *   or of a group (flagged by {@code _PRIMARY} or {@code _PREF}) also becomes its {@code preferred_image}.</li>
	 * </ul>
	 */
	public static void attachMultimediaLink(FLEFRecord parent, FLEFModel model, GEDCOMNode node,
			Map<String, GEDCOMNode> noteRawMap){
		final List<String> mediaIds = resolveMedia(model, node, noteRawMap);
		if(SourceReader.isOfType(parent)){
			for(final String mediaId : mediaIds)
				addMediaReference(parent, mediaId);
			return;
		}

		final boolean preferred = ("Y".equalsIgnoreCase(findFirstValue(node, "_PRIMARY"))
			|| "Y".equalsIgnoreCase(findFirstValue(node, "_PREF")));
		final boolean canHavePreferredImage = (IndividualReader.isOfType(parent) || GroupReader.isOfType(parent));
		for(final String mediaId : mediaIds){
			if(preferred && canHavePreferredImage && FLEFRecordHelper.findChild(parent, "preferred_image") == null)
				parent.addChild(FLEFRecord.createChildWithTag("preferred_image")
					.addChild(FLEFRecord.createChildWithTagAndValue("media", mediaId))
					.addChild(buildCrop(node)));

			final FLEFRecord media = model.getRecordById(mediaId);
			final String title = ObjectUtils.firstNonNull(FLEFRecordHelper.getChildValue(media, "description"),
				"Media " + mediaId);
			final FLEFRecord source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceReader.ID_PREFIX), SourceReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag("title")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", title)))
				.addChild(FLEFRecord.createChildWithTagAndValue("media", mediaId))
				.addChild(AuditBuilder.build(node));
			final String sourceId = Deduplicator.getDeduplicatedRecordId(model, source);

			parent.addChild(FLEFRecord.createChildWithTag("source")
				.addChild(FLEFRecord.createChildWithTagAndValue("source", sourceId))
				.addChild(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTag("media_part")
						.addChild(FLEFRecord.createChildWithTagAndValue("media", mediaId))
						.addChild(buildCrop(node))))
				.addChild(FLEFRecord.createChildWithTagAndValue(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCE_SUPPORTS)));
		}
	}

	/**
	 * Returns the ids of the MediaRecords a MULTIMEDIA_LINK refers to, creating those of an embedded link (one for each
	 * FILE).
	 * <p>
	 * The multimedia records must be already in the model.
	 */
	public static List<String> resolveMedia(final FLEFModel model, final GEDCOMNode node,
			final Map<String, GEDCOMNode> noteRawMap){
		final String objeId = pointer(node.getValue());
		if(objeId != null)
			return (model.hasRecord(objeId)? List.of(objeId): List.of());

		final List<String> mediaIds = new ArrayList<>();
		for(final GEDCOMNode fileNode : findChildren(node, "FILE")){
			final FLEFRecord media = FLEFRecord.createMainRecord(IDGenerator.nextId(MediaReader.ID_PREFIX),
				MediaReader.TYPE);
			fillMediaRecord(media, node, fileNode, noteRawMap);
			mediaIds.add(Deduplicator.getDeduplicatedRecordId(model, media));
		}
		return mediaIds;
	}

	/**
	 * Fills a MediaRecord from a multimedia record or link and one of its files.
	 * <p>
	 * The kind comes from the media type (FORM.TYPE, or FORM.MEDI), else from the format (FORM), else from the extension of the
	 * file. The format, the media type, and the custom date ({@code _DATE}) and publisher ({@code _PUBL}) have no place in a
	 * MediaRecord, and become notes.
	 *
	 * @param fileNode	The FILE, may be {@code null}.
	 */
	public static void fillMediaRecord(final FLEFRecord media, final GEDCOMNode objeNode, final GEDCOMNode fileNode,
			final Map<String, GEDCOMNode> noteRawMap){
		final String uri = (fileNode != null? StringUtils.trimToNull(fileNode.getValue()): null);
		// 5.5.1 has FORM under FILE, 5.5 under OBJE
		final GEDCOMNode formNode = ObjectUtils.firstNonNull(findFirstChild(fileNode, "FORM"), findFirstChild(objeNode, "FORM"));
		final String format = (formNode != null? StringUtils.trimToNull(formNode.getValue()): null);
		final String mediaType = ObjectUtils.firstNonNull(findFirstValue(formNode, "TYPE"), findFirstValue(formNode, "MEDI"));
		media.addChild(FLEFRecord.createChildWithTagAndValue("uri", ObjectUtils.firstNonNull(uri, "unknown")))
			.addChild(FLEFRecord.createChildWithTagAndValue("kind", GEDCOMMapper.mapMediaKind(mediaType, format, uri)));

		final String title = ObjectUtils.firstNonNull(findFirstValue(fileNode, "TITL"), findFirstValue(objeNode, "TITL"));
		if(title != null)
			media.addChild(FLEFRecord.createChildWithTagAndValue("description", title));

		media.addChild(createNoteStruct("Format", format, objeNode))
			.addChild(createNoteStruct("Media type", mediaType, objeNode))
			.addChild(createNoteStruct("Date", findFirstValue(objeNode, "_DATE"), objeNode));
		final String publisher = findFirstValue(objeNode, "_PUBL");
		if(publisher != null && !isFlag(publisher))
			media.addChild(createNoteStruct("Published by", publisher, objeNode));
		// GEDCOM 7 alternative versions of the file (e.g. a thumbnail, a transcription)
		for(final GEDCOMNode tranNode : findChildren(fileNode, "TRAN")){
			final String tranUri = StringUtils.trimToNull(tranNode.getValue());
			final String tranFormat = findFirstValue(tranNode, "FORM");
			if(tranUri != null)
				media.addChild(createNoteStruct("Alternative file",
					tranUri + (tranFormat != null? " (" + tranFormat + ")": StringUtils.EMPTY), objeNode));
		}
		for(final GEDCOMNode noteNode : findChildren(objeNode, "NOTE"))
			attachNote(media, noteNode, noteRawMap);

		attachRestriction(media, objeNode);
		media.addChild(AuditBuilder.build(objeNode));
	}

	/** Adds a reference to a media to a SourceRecord, if not already there. */
	private static void addMediaReference(final FLEFRecord source, final String mediaId){
		final boolean present = FLEFRecordHelper.findChildren(source, "media").stream()
			.anyMatch(reference -> mediaId.equals(reference.getValue()));
		if(!present)
			source.addChild(FLEFRecord.createChildWithTagAndValue("media", mediaId));
	}

	/*
	(GEDCOM 7)
	n OBJE @<XREF:OBJE>@
	  +1 CROP    {0:1}
	    +2 TOP <Integer>    {0:1}
	    +2 LEFT <Integer>    {0:1}
	    +2 HEIGHT <Integer>    {0:1}
	    +2 WIDTH <Integer>    {0:1}
	*/
	/**
	 * Builds a CropRect from the CROP of GEDCOM 7, or from the custom {@code _CUTD} ("x y width height"), or {@code null}.
	 * <p>
	 * A CROP without HEIGHT or WIDTH extends to the edge of the image, whose size is not known here: it is dropped.
	 */
	private static FLEFRecord buildCrop(final GEDCOMNode objeNode){
		final GEDCOMNode cropNode = findFirstChild(objeNode, "CROP");
		if(cropNode != null)
			return createCrop(ObjectUtils.firstNonNull(findFirstValue(cropNode, "LEFT"), "0"),
				ObjectUtils.firstNonNull(findFirstValue(cropNode, "TOP"), "0"),
				findFirstValue(cropNode, "WIDTH"), findFirstValue(cropNode, "HEIGHT"));

		final String cut = findFirstValue(objeNode, "_CUTD");
		if(cut == null)
			return null;

		final String[] parts = StringUtils.split(cut);
		return (parts.length == 4? createCrop(parts[0], parts[1], parts[2], parts[3]): null);
	}

	private static FLEFRecord createCrop(final String x, final String y, final String width, final String height){
		try{
			final int w = Integer.parseInt(width);
			final int h = Integer.parseInt(height);
			if(w <= 0 || h <= 0)
				return null;

			return FLEFRecord.createChildWithTag("crop")
				.addChild(FLEFRecord.createChildWithTagAndValue("x", String.valueOf(Integer.parseInt(x))))
				.addChild(FLEFRecord.createChildWithTagAndValue("y", String.valueOf(Integer.parseInt(y))))
				.addChild(FLEFRecord.createChildWithTagAndValue("width", String.valueOf(w)))
				.addChild(FLEFRecord.createChildWithTagAndValue("height", String.valueOf(h)));
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}


	/*
	EVENT_DETAIL :=
	  n TYPE <EVENT_OR_FACT_CLASSIFICATION>    {0:1}
	  n DATE <DATE_VALUE>    {0:1}
	  n <<PLACE_STRUCTURE>>    {0:1}
	  n <<ADDRESS_STRUCTURE>>    {0:1}
	  n AGNC <RESPONSIBLE_AGENCY>    {0:1}
	  n RELI <RELIGIOUS_AFFILIATION>    {0:1}
	  n CAUS <CAUSE_OF_EVENT>    {0:1}
	  n RESN <RESTRICTION_NOTICE>    {0:1}
	  n <<NOTE_STRUCTURE>>    {0:M}
	  n <<SOURCE_CITATION>>    {0:M}
	  n <<MULTIMEDIA_LINK>>    {0:M}
	*/
	/**
	 * Creates an EventRecord from an INDIVIDUAL_EVENT_STRUCTURE or a FAMILY_EVENT_STRUCTURE, and adds it to the model.
	 * <p>
	 * The participants (and their age, which GEDCOM records in the event) are linked by the caller.
	 *
	 * @return	The event.
	 */
	public static FLEFRecord createEvent(FLEFModel model, GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap,
			PlaceCache placeCache){
		final String tag = node.getTag();
		final String typeValue = findFirstValue(node, "TYPE");
		final String type = GEDCOMMapper.mapEventType(tag, typeValue);
		final FLEFRecord event = FLEFRecord.createMainRecord(IDGenerator.nextId(EventReader.ID_PREFIX), EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));

		// Description: the value of the event (e.g. of EVEN), or else a TYPE that does not give the type of the event
		final boolean typeConsumed = (GEDCOMMapper.TAG_GENERIC_EVENT.equals(tag) && GEDCOMMapper.toTypeToken(typeValue) != null);
		String description = (!isFlag(node.getValue())? StringUtils.trimToNull(extractFullText(node)): null);
		if(typeValue != null && !typeConsumed){
			if(description == null)
				description = typeValue;
			else
				event.addChild(createNoteStruct("Type", typeValue, node));
		}
		if(description != null)
			event.addChild(FLEFRecord.createChildWithTagAndValue("description", description));

		attachDate(event, "date", findFirstValue(node, "DATE"));
		attachPlaceCitation(event, model, findFirstChild(node, "PLAC"), findFirstChild(node, "ADDR"), noteRawMap, placeCache);

		final String cause = findFirstValue(node, "CAUS");
		if(cause != null)
			event.addChild(FLEFRecord.createChildWithTag("cause")
				.addChild(FLEFRecord.createChildWithTagAndValue("reason", cause)));

		attachEventDetail(event, model, node, noteRawMap);
		return event;
	}

	/**
	 * Creates an IndividualAttributeRecord from an INDIVIDUAL_ATTRIBUTE_STRUCTURE (or from an event that is an attribute in
	 * FLEF), and adds it to the model.
	 *
	 * @param type	The FLEF type of the attribute, or {@code null} to derive it from the GEDCOM tag.
	 */
	public static FLEFRecord createIndividualAttribute(FLEFModel model, String individualId, GEDCOMNode node, String type,
			Map<String, GEDCOMNode> noteRawMap, PlaceCache placeCache){
		final String tag = node.getTag();
		final String typeValue = findFirstValue(node, "TYPE");
		boolean typeConsumed = (type != null);
		if(type == null){
			type = GEDCOMMapper.mapAttribute(tag, tag);
			if(GEDCOMMapper.TAG_GENERIC_FACT.equals(tag) && GEDCOMMapper.toTypeToken(typeValue) != null){
				type = GEDCOMMapper.toTypeToken(typeValue);
				typeConsumed = true;
			}
		}

		final FLEFRecord attribute = FLEFRecord.createMainRecord(IDGenerator.nextId(IndividualAttributeReader.ID_PREFIX),
				IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", individualId))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));

		// Value: the value of the attribute, or else its TYPE (that some programs use to hold it)
		String value = (!isFlag(node.getValue())? StringUtils.trimToNull(extractFullText(node)): null);
		if(typeValue != null && !typeConsumed){
			if(value == null)
				value = typeValue;
			else
				attribute.addChild(createNoteStruct("Type", typeValue, node));
		}
		if(value != null)
			attribute.addChild(FLEFRecord.createChildWithTagAndValue("value", value));

		attachValidity(attribute, findFirstValue(node, "DATE"));
		attachPlaceCitation(attribute, model, findFirstChild(node, "PLAC"), findFirstChild(node, "ADDR"), noteRawMap,
			placeCache);

		// Cause and age have no place in an attribute
		attribute.addChild(createNoteStruct("Cause", findFirstValue(node, "CAUS"), node));
		attribute.addChild(createNoteStruct("Age", findFirstValue(node, "AGE"), node));

		attachEventDetail(attribute, model, node, noteRawMap);
		return attribute;
	}

	/** Attaches the details shared by events and attributes, the audit, and adds the record to the model. */
	private static void attachEventDetail(FLEFRecord record, FLEFModel model, GEDCOMNode node,
			Map<String, GEDCOMNode> noteRawMap){
		attachNote(record, findFirstChild(node, "AGNC"), noteRawMap, "Agency");
		attachNote(record, findFirstChild(node, "RELI"), noteRawMap, "Religion");
		// LDS ordinances
		attachNote(record, findFirstChild(node, "TEMP"), noteRawMap, "Temple");
		attachNote(record, findFirstChild(node, "STAT"), noteRawMap, "Status");

		for(GEDCOMNode sourNode : findChildren(node, "SOUR"))
			attachSource(record, model, sourNode, noteRawMap);
		for(GEDCOMNode noteNode : findChildren(node, "NOTE"))
			attachNote(record, noteNode, noteRawMap);
		for(GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE"))
			attachMultimediaLink(record, model, multimediaLinkNode, noteRawMap);

		attachRestriction(record, node);
		record.addChild(AuditBuilder.build(node));

		model.addRecord(record);
	}

	/**
	 * Attaches an EventParticipationRecord linking an individual or group to an EventRecord.
	 *
	 * @param age	The age of the participant at the event, may be {@code null}.
	 * @param node	The GEDCOM node used as the audit source, may be {@code null}.
	 */
	public static void attachEventParticipation(String eventId, String entityType, String entityId, String role, String age,
			GEDCOMNode node, FLEFModel model){
		if(StringUtils.isEmpty(eventId) || StringUtils.isEmpty(entityId))
			return;

		final FLEFRecord participation = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationReader.ID_PREFIX),
				EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("participant")
				.addChild(FLEFRecord.createChildWithTagAndValue(entityType, entityId)))
			.addChild(FLEFRecord.createChildWithTagAndValue("event", eventId));
		if(role != null)
			participation.addChild(FLEFRecord.createChildWithTagAndValue("role", role));
		participation.addChild(createNoteStruct("Age", age, node))
			.addChild(AuditBuilder.build(node));

		Deduplicator.getDeduplicatedRecordId(model, participation);
	}

	/*
	(GEDCOM 7, under an event)
	n ASSO @<XREF:INDI>@
	  +1 PHRASE <Text>    {0:1}
	  +1 ROLE <Enum>    {1:1}
	    +2 PHRASE <Text>    {0:1}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 <<SOURCE_CITATION>>    {0:M}
	*/
	/**
	 * Attaches an EventParticipationRecord for each individual associated with an event (GEDCOM 7), with its role.
	 * <p>
	 * A role that is not a function in the event (e.g. a kinship, as {@code FATH}) is kept as a note, as roles never express
	 * kinship.
	 *
	 * @param eventNode	The GEDCOM event.
	 * @param recordsById	The GEDCOM records, by id.
	 */
	public static void attachEventAssociates(String eventId, GEDCOMNode eventNode, FLEFModel model,
			Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> recordsById){
		for(GEDCOMNode assoNode : findChildren(eventNode, "ASSO")){
			final String associateId = pointer(assoNode.getValue());
			final GEDCOMNode associate = recordsById.get(associateId);
			if(associate == null || !"INDI".equals(associate.getTag()))
				continue;

			final String gedcomRole = findFirstValue(assoNode, "ROLE");
			final String role = GEDCOMMapper.mapEventRole(gedcomRole);
			final FLEFRecord participation = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationReader.ID_PREFIX),
					EventParticipationReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag("participant")
					.addChild(FLEFRecord.createChildWithTagAndValue("individual", associateId)))
				.addChild(FLEFRecord.createChildWithTagAndValue("event", eventId));
			if(role != null)
				participation.addChild(FLEFRecord.createChildWithTagAndValue("role", role));
			for(GEDCOMNode sourNode : findChildren(assoNode, "SOUR"))
				attachSource(participation, model, sourNode, noteRawMap);
			if(role == null)
				participation.addChild(createNoteStruct("Role", GEDCOMMapper.mapAssociationRoleLabel(gedcomRole), assoNode));
			for(GEDCOMNode noteNode : findChildren(assoNode, "NOTE"))
				attachNote(participation, noteNode, noteRawMap);
			participation.addChild(AuditBuilder.build(eventNode));

			Deduplicator.getDeduplicatedRecordId(model, participation);
		}
	}

	/*
	(GEDCOM 7)
	n NO <EVENT_TYPE>
	  +1 DATE <DatePeriod>    {0:1}
	    +2 PHRASE <Text>    {0:1}
	  +1 <<NOTE_STRUCTURE>>    {0:M}
	  +1 <<SOURCE_CITATION>>    {0:M}
	*/
	/**
	 * Attaches the note that an event did not happen (in the period given, if any), with the sources stating it.
	 */
	public static void attachNonEvent(FLEFRecord parent, GEDCOMNode node, FLEFModel model, Map<String, GEDCOMNode> noteRawMap){
		final String eventTag = StringUtils.trimToNull(node.getValue());
		if(eventTag == null)
			return;

		final StringBuilder text = new StringBuilder(GEDCOMMapper.mapEvent(eventTag, eventTag).replace('_', ' '));
		final String period = findFirstValue(node, "DATE");
		if(period != null)
			text.append(" (").append(period).append(')');
		for(GEDCOMNode noteNode : findChildren(node, "NOTE")){
			final String noteText = StringUtils.trimToNull(extractFullText(resolveNote(noteNode, noteRawMap)));
			if(noteText != null)
				text.append(StringUtils.LF).append(noteText);
		}

		final FLEFRecord note = createNoteStruct("Did not happen", text.toString(), node);
		// the sources go before the audit, the last field of a note
		final FLEFRecord audit = note.getChildren().removeLast();
		for(GEDCOMNode sourNode : findChildren(node, "SOUR"))
			attachSource(note, model, sourNode, noteRawMap);
		// a note cannot have notes: those of the citations (see attachSource) are appended to its text
		final List<FLEFRecord> citationNotes = FLEFRecordHelper.findChildren(note, "note");
		if(!citationNotes.isEmpty()){
			note.getChildren().removeAll(citationNotes);
			for(final FLEFRecord citationNote : citationNotes)
				text.append(StringUtils.LF).append(FLEFRecordHelper.getChildValue(citationNote, "text"));
			FLEFRecordHelper.findChild(note, "text").setValue(text.toString());
		}
		note.addChild(audit);
		parent.addChild(note);
	}


	/*
	n ADOP [Y|<NULL>]
	  +1 FAMC @<XREF:FAM>@    {0:1}
	    +2 ADOP <ADOPTED_BY_WHICH_PARENT>    {0:1}
	*/
	/**
	 * Creates the adoptive relationships stated by an adoption event (ADOP.FAMC, with ADOP telling which parents adopted).
	 * <p>
	 * A parent by birth of the individual is not an adoptive one (e.g. the mother whose new husband adopted the child).
	 *
	 * @param indiNode	The GEDCOM individual.
	 * @param recordsById	The GEDCOM records, by id.
	 */
	public static void attachAdoptiveParents(FLEFRecord individual, FLEFModel model, GEDCOMNode indiNode,
			GEDCOMNode adoptionNode, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> recordsById){
		final Set<String> biologicalParents = biologicalParents(indiNode, recordsById);
		for(GEDCOMNode famcNode : findChildren(adoptionNode, "FAMC")){
			final GEDCOMNode familyNode = recordsById.get(pointer(famcNode.getValue()));
			if(familyNode == null)
				continue;

			final String adoptedBy = ObjectUtils.firstNonNull(findFirstValue(famcNode, "ADOP"), "BOTH")
				.toUpperCase(Locale.ROOT);
			final String husbandId = pointer(findFirstValue(familyNode, "HUSB"));
			final String wifeId = pointer(findFirstValue(familyNode, "WIFE"));
			if(husbandId != null && !biologicalParents.contains(husbandId)
					&& ("HUSB".equals(adoptedBy) || "BOTH".equals(adoptedBy)))
				attachAdoptiveRelationship(individual, model, adoptionNode, famcNode, husbandId, noteRawMap);
			if(wifeId != null && !biologicalParents.contains(wifeId)
					&& ("WIFE".equals(adoptedBy) || "BOTH".equals(adoptedBy)))
				attachAdoptiveRelationship(individual, model, adoptionNode, famcNode, wifeId, noteRawMap);
		}
	}

	/**
	 * Returns the parents by birth of an individual: the spouses of the families it is a child of with a birth (or no)
	 * pedigree, except the families of an adoption event.
	 *
	 * @param indiNode	The GEDCOM individual, may be {@code null}.
	 * @param recordsById	The GEDCOM records, by id.
	 * @return	The ids of the parents by birth.
	 */
	public static Set<String> biologicalParents(GEDCOMNode indiNode, Map<String, GEDCOMNode> recordsById){
		final Set<String> adoptionFamilyIds = new HashSet<>();
		for(GEDCOMNode adoptionNode : findChildren(indiNode, "ADOP"))
			for(GEDCOMNode famcNode : findChildren(adoptionNode, "FAMC"))
				adoptionFamilyIds.add(pointer(famcNode.getValue()));

		final Set<String> parents = new HashSet<>();
		for(GEDCOMNode famcNode : findChildren(indiNode, "FAMC")){
			final String familyId = pointer(famcNode.getValue());
			final GEDCOMNode familyNode = recordsById.get(familyId);
			if(familyNode == null || adoptionFamilyIds.contains(familyId)
					|| !RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(GEDCOMMapper.mapPedigree(findFirstValue(famcNode, "PEDI"))))
				continue;

			for(String spouseTag : List.of("HUSB", "WIFE")){
				final String parentId = pointer(findFirstValue(familyNode, spouseTag));
				if(parentId != null)
					parents.add(parentId);
			}
		}
		return parents;
	}

	private static void attachAdoptiveRelationship(FLEFRecord individual, FLEFModel model, GEDCOMNode adoptionNode,
			GEDCOMNode famcNode, String parentId, Map<String, GEDCOMNode> noteRawMap){
		final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipReader.ID_PREFIX),
				RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", individual.getId())))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD));
		attachDate(relationship, "valid_from", findFirstValue(adoptionNode, "DATE"));
		for(GEDCOMNode noteNode : findChildren(famcNode, "NOTE"))
			attachNote(relationship, noteNode, noteRawMap);
		relationship.addChild(AuditBuilder.build(adoptionNode));

		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}


	/**
	 * Attaches a {@code DateStructure} built from a GEDCOM date; a date that cannot be normalized is kept as a note.
	 *
	 * @param date	The GEDCOM date, may be {@code null}.
	 */
	public static void attachDate(FLEFRecord parent, String tag, String date){
		if(StringUtils.isBlank(date))
			return;

		final FLEFRecord dateStructure = GEDCOMDateConverter.buildDateStructure(tag, date);
		if(dateStructure != null)
			parent.addChild(dateStructure);
		else
			parent.addChild(createNoteStruct("Date", date, null));
	}

	/**
	 * Attaches the {@code valid_from} and {@code valid_to} of a fact observed at a GEDCOM date; a date that cannot be
	 * normalized is kept as a note.
	 *
	 * @param date	The GEDCOM date, may be {@code null}.
	 */
	public static void attachValidity(FLEFRecord parent, String date){
		if(StringUtils.isBlank(date))
			return;

		final FLEFRecord[] validity = GEDCOMDateConverter.buildValidity(date);
		if(validity != null){
			parent.addChild(validity[0]);
			parent.addChild(validity[1]);
		}
		else
			parent.addChild(createNoteStruct("Date", date, null));
	}


	/**
	 * Parses a GEDCOM PLAC node and returns a PlaceCitation.
	 * It also creates/updates the underlying PlaceRecord with subfields.
	 */
	public static void attachPlaceCitation(FLEFRecord parent, FLEFModel model,
			GEDCOMNode placNode, GEDCOMNode addrNode, Map<String, GEDCOMNode> noteRawMap, PlaceCache placeCache){
		if(placNode == null && addrNode == null)
			return;

		final String placeName = (placNode != null? StringUtils.trimToNull(placNode.getValue()): null);

		// Extract address components if present
		final StringBuilder streetAddr = new StringBuilder();
		String addressPlaceName = null;
		if(addrNode != null){
			boolean hasSubTags = false;
			for(final String subTag : List.of("ADR1", "ADR2", "ADR3")){
				final GEDCOMNode sub = findFirstChild(addrNode, subTag);
				if(sub != null && sub.getValue() != null){
					hasSubTags = true;
					if(!streetAddr.isEmpty())
						streetAddr.append(StringUtils.LF);
					streetAddr.append(sub.getValue());
				}
			}
			// Fallback to main address value if no specific street sub-tags were found
			if(!hasSubTags && addrNode.getValue() != null){
				streetAddr.append(extractFullText(addrNode));
			}

			// The jurisdictions of the address, used when there is no place
			final List<String> jurisdictions = new ArrayList<>();
			for(final String subTag : List.of("CITY", "STAE", "CTRY")){
				final String jurisdiction = findFirstValue(addrNode, subTag);
				if(jurisdiction != null)
					jurisdictions.add(jurisdiction);
			}
			if(!jurisdictions.isEmpty())
				addressPlaceName = String.join(", ", jurisdictions);
		}
		final String basePlaceName = ObjectUtils.firstNonNull(placeName, addressPlaceName);

		final String finalPlaceName;
		final GEDCOMNode targetPlacNode;
		if(StringUtils.isEmpty(basePlaceName)){
			// Only a street address exists, use it as place name
			finalPlaceName = StringUtils.trimToNull(streetAddr.toString().replace(StringUtils.LF, ", "));
			targetPlacNode = new GEDCOMNode(1, "PLAC", finalPlaceName);
		}
		else if(streetAddr.isEmpty()){
			// Only PLAC exists
			finalPlaceName = basePlaceName;
			targetPlacNode = (placNode != null? placNode: new GEDCOMNode(1, "PLAC", finalPlaceName));
		}
		else{
			// Both exist: combine street address with place name, avoiding duplication
			final String streetStr = streetAddr.toString().replace(StringUtils.LF, ", ");
			if(!basePlaceName.toLowerCase(Locale.ROOT).contains(streetStr.toLowerCase(Locale.ROOT))){
				finalPlaceName = streetStr + ", " + basePlaceName;
			}
			else{
				finalPlaceName = streetStr;
			}
			targetPlacNode = createSyntheticPlacNode(placNode, finalPlaceName);
		}

		if(StringUtils.isEmpty(finalPlaceName))
			return;

		// Use PlaceCache to handle hierarchy, splitting, deduplication, and links (and FONE, ROMN, MAP)
		final FLEFRecord placeRecord = (placeCache != null? placeCache.getOrCreatePlace(targetPlacNode): null);
		if(placeRecord == null)
			return;

		// Attach NOTE (notes under PLAC) to the leaf place if needed
		for(final GEDCOMNode noteNode : findChildren(placNode, "NOTE")){
			attachNote(placeRecord, noteNode, noteRawMap);
		}

		parent.addChild(FLEFRecord.createChildWithTag("place")
			.addChild(FLEFRecord.createChildWithTagAndValue("place", placeRecord.getId()))
		);
	}

	private static GEDCOMNode createSyntheticPlacNode(final GEDCOMNode originalPlac, final String newAddress){
		final GEDCOMNode synthetic = new GEDCOMNode((originalPlac != null? originalPlac.getLevel(): 1), "PLAC", newAddress);
		if(originalPlac != null)
			for(final GEDCOMNode child : originalPlac.getChildren())
				synthetic.addChild(child);
		return synthetic;
	}


	public static void attachRestriction(FLEFRecord parent, GEDCOMNode node){
		final String level = GEDCOMMapper.mapPrivacyLevel(findFirstValue(node, "RESN"));
		if(level != null)
			parent.addChild(FLEFRecord.createChildWithTag("privacy")
				.addChild(FLEFRecord.createChildWithTagAndValue("level", level)));
	}


	/*
	ADDRESS_STRUCTURE :=
	  n ADDR <ADDRESS_LINE>    {1:1}
	    +1 CONT <ADDRESS_LINE>    {0:3}
	    +1 ADR1, ADR2, ADR3, CITY, STAE, POST, CTRY    {0:1}
	  n PHON <PHONE_NUMBER>    {0:3}
	  n EMAIL <ADDRESS_EMAIL>    {0:3}
	  n FAX <ADDRESS_FAX>    {0:3}
	  n WWW <ADDRESS_WEB_PAGE>    {0:3}
	*/
	/**
	 * Attaches a ContactStructure for the address, and one for each phone, email, fax, and web page of an ADDRESS_STRUCTURE.
	 * <p>
	 * PHON, EMAIL, FAX, and WWW are siblings of ADDR, but some programs write them under it.
	 *
	 * @param parentNode	The node holding the ADDRESS_STRUCTURE.
	 * @param contactName	The name of the person or organization the contacts belong to, may be {@code null}.
	 */
	public static void attachContacts(FLEFRecord parent, GEDCOMNode parentNode, String contactName){
		final GEDCOMNode addrNode = findFirstChild(parentNode, "ADDR");
		if(addrNode != null){
			final String formattedAddress = StringUtils.trimToNull(extractFullText(addrNode));
			final StringBuilder fullAddr = new StringBuilder(StringUtils.defaultString(formattedAddress));
			for(String subTag : List.of("ADR1", "ADR2", "ADR3", "CITY", "STAE", "POST", "CTRY")){
				final String sub = findFirstValue(addrNode, subTag);
				// the ADDR value already holds the formatted address, its parts only make it machine-readable
				if(sub != null && (formattedAddress == null || !Strings.CI.contains(formattedAddress, sub))){
					if(!fullAddr.isEmpty())
						fullAddr.append(StringUtils.LF);
					fullAddr.append(sub);
				}
			}
			addContact(parent, fullAddr.toString(), "postal", contactName, parentNode);
		}

		final List<GEDCOMNode> holders = new ArrayList<>(2);
		holders.add(parentNode);
		if(addrNode != null)
			holders.add(addrNode);
		for(final GEDCOMNode holder : holders)
			for(final GEDCOMNode child : holder.getChildren()){
				final String type = switch(child.getTag()){
					case "PHON" -> "phone";
					case "EMAIL", "_EMAIL" -> "email";
					case "FAX" -> "fax";
					case "WWW", "_WWW" -> "website";
					default -> null;
				};
				if(type != null)
					addContact(parent, child.getValue(), type, contactName, parentNode);
			}
	}

	private static void addContact(FLEFRecord parent, String value, String type, String contactName, GEDCOMNode auditNode){
		if(StringUtils.isBlank(value))
			return;

		final FLEFRecord contact = FLEFRecord.createChildWithTag("contact")
			.addChild(FLEFRecord.createChildWithTagAndValue("value", value.trim()))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));
		if(StringUtils.isNotBlank(contactName))
			contact.addChild(FLEFRecord.createChildWithTag("name")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", contactName.trim())));
		contact.addChild(AuditBuilder.build(auditNode));
		parent.addChild(contact);
	}


	/**
	 * Computes a content signature for a record, ignoring its ID and audit children.
	 * The signature is a string that represents the record's structure and values.
	 * It is deterministic regardless of child order.
	 *
	 * @param record the record
	 * @return a signature string
	 */
	public static String computeSignature(FLEFRecord record){
		StringBuilder sb = new StringBuilder();
		sb.append(record.getTag());
		sb.append('|');

		// Include the scalar value (if any)
		String value = record.getValue();
		if(value != null){
			sb.append(value);
		}
		sb.append('|');

		// Collect signatures of all children except those with tag "audit"
		List<String> childSignatures = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if("audit".equals(child.getTag())){
				continue;
			}
			childSignatures.add(computeSignature(child));
		}
		// Sort to make order-independent
		Collections.sort(childSignatures);
		sb.append(String.join(",", childSignatures));

		return sb.toString();
	}

}
