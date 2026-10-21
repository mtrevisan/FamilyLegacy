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

import io.github.mtrevisan.familylegacy.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.gedcom.utils.DateInfo;
import io.github.mtrevisan.familylegacy.gedcom.utils.GEDCOMDateParser;
import io.github.mtrevisan.familylegacy.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.gedcom.utils.PlaceCache;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class GEDCOMHelper{

	private static final Pattern PATTERN_SPACES = Pattern.compile("\\s+");

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
		.ofPattern("uuuu-MM-dd")
		.withResolverStyle(ResolverStyle.STRICT);

	// Formato data GEDCOM: [GG ]MMM[ AAAA] oppure AAAA
	private static final String GEDCOM_MONTHS = "JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC";
	private static final Pattern FORMAL_GEDCOM_DATE_PATTERN = Pattern.compile(
		"^(?:(?:\\d{1,2}\\s+)?(?:" + GEDCOM_MONTHS + ")(?:\\s+\\d{1,4})?|\\d{1,4})$",
		Pattern.CASE_INSENSITIVE
	);


	private GEDCOMHelper(){}


	@SuppressWarnings({"ResultOfMethodCallIgnored", "IOResourceOpenedButNotSafelyClosed"})
	public static BufferedReader getBufferedReader(InputStream in) throws IOException{
		if(!in.markSupported())
			in = new BufferedInputStream(in);

		// 1. Primary check: Detect charset directly from byte sequence
		String charEncoding = GEDCOMCharsetDetector.detectCharsetFromBytes(in);

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
		for(GEDCOMNode child : noteNode.getChildren()){
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

	public static String getDateTime(GEDCOMNode dateNode){
		String dateTime = null;
		if(dateNode != null && StringUtils.isNotEmpty(dateNode.getValue())){
			String datePart = dateNode.getValue()
				.trim();
			GEDCOMNode timeNode = findFirstChild(dateNode, "TIME");
			if(timeNode != null && timeNode.getValue() != null)
				dateTime = datePart + (isIsoDate(datePart)? "T": StringUtils.SPACE) + timeNode.getValue().trim();
			else
				dateTime = datePart;
		}
		return dateTime;
	}

	public static boolean isIsoDate(String value){
		if(value == null)
			return false;

		try{
			LocalDate.parse(value, DATE_TIME_FORMATTER);

			return true;
		}
		catch(final Exception e){
			return false;
		}
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
		if(node == null || StringUtils.isEmpty(node.getValue()))
			return;

		String fullText = extractFullText(node);
		if(StringUtils.isEmpty(fullText))
			return;

		String noteXrefId = cleanId(node.getValue());
		if(noteXrefId != null){
			GEDCOMNode rawNote = noteRawMap.get(noteXrefId);
			fullText = extractFullText(rawNote);
		}

		if(!fullText.trim().isEmpty()){
			FLEFRecord note = FLEFRecord.createChildWithTag("note")
				.addChild(FLEFRecord.createChildWithTagAndValue("text", fullText.trim()))
				.addChild(AuditBuilder.build(node));
			parent.addChild(note);
		}
	}


	/**
	 * Parses a PersonalNameStructure (for individuals) extracting all sub-tags
	 * (GIVN, SURN, NPFX, NSFX, SPFX, NICK), inline sources, and notes with CONC/CONT.
	 */
	public static void attachPersonalNameStructure(FLEFRecord parent,
			GEDCOMNode nameNode, FLEFModel model, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> objeRawMap){
		if(nameNode == null){
			return;
		}

		FLEFRecord name = FLEFRecord.createChildWithTag("name");

		// Type (optional)
		GEDCOMNode typeNode = findFirstChild(nameNode, "TYPE");
		transferValue(name, "type", typeNode);

		// Parse inline value (e.g., "Joseph Tag /Torture/") into full text
		String given = StringUtils.EMPTY;
		String family = StringUtils.EMPTY;
		final String raw = extractFullText(nameNode);
		if(StringUtils.isNotEmpty(raw)){
			final int slash1 = raw.indexOf('/');
			final int slash2 = raw.indexOf('/', slash1 + 1);
			if(slash1 >= 0 && slash2 > slash1){
				given = raw.substring(0, slash1).trim();
				family = raw.substring(slash1 + 1, slash2)
					.trim();
				final String suffix = raw.substring(slash2 + 1)
					.trim();
				if(!suffix.isEmpty())
					given = (given + StringUtils.SPACE + suffix).trim();
			}
			else
				given = raw.trim();
		}

		// Explicit parts mapping: priority to sub-tags, fallback to inline parsing
		personalNamePieces(name, nameNode, given, family);

		// TODO
		// Phonetic variations (FONE)
//		FLEFRecord namePhoneticVariations = FLEFRecord.createChildWithTag("name");
//		for(GEDCOMNode nameFoneNode : findChildren(nameNode, "FONE")){
//			GEDCOMNode nameFoneType = findFirstChild(nameFoneNode, "TYPE");
//			personalNamePieces(namePhoneticVariations, nameFoneNode, null, null);
//
//			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
//			FLEFRecord phonetic = FLEFRecord.createChildWithTag("phonetic");
//			transferValue(phonetic, "system", nameFoneType);
//			transferValue(phonetic, "value", nameFoneNode);
//			if(!phonetic.isEmpty())
//				variant.addChild(phonetic);
//			if(!variant.isEmpty())
//				namePhoneticVariations.addChild(variant);
//		}

		// TODO
//		// Romanized variations (ROMN)
//		for(GEDCOMNode romn : findChildren(nameNode, "ROMN")){
//			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
//			FLEFRecord transcription = FLEFRecord.createChildWithTag("transcription");
//			GEDCOMNode romnType = findFirstChild(romn, "TYPE");
//			String system = (romnType != null && romnType.getValue() != null? romnType.getValue(): "scientific");
//			transcription.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
//			transcription.addChild(FLEFRecord.createChildWithTagAndValue("value", romn.getValue()));
//			variant.addChild(transcription);
//			namePhoneticVariations.addChild(variant);
//		}

		for(GEDCOMNode sourNode : findChildren(nameNode, "SOUR")){
			attachSource(parent, model,
				sourNode, noteRawMap, objeRawMap);
		}

		for(GEDCOMNode noteNode : findChildren(nameNode, "NOTE")){
			attachNote(parent,
				noteNode, noteRawMap);
		}

		if(name.hasData())
			parent.addChild(name);
	}

	private static void personalNamePieces(FLEFRecord name, GEDCOMNode nameNode, String defaultGiven, String defaultFamily){
		addNamePart(name, nameNode, "GIVN", "given", defaultGiven);
		addNamePart(name, nameNode, "SURN", "family", defaultFamily);
		addNamePart(name, nameNode, "NPFX", "prefix", null);
		addNamePart(name, nameNode, "NSFX", "suffix", null);
//		addNamePart(name, nameNode, "SPFX", "surname_prefix", null);
		addNamePart(name, nameNode, "NICK", "nickname", null);
	}

	/**
	 * Helper method to add a name part record.
	 */
	private static void addNamePart(FLEFRecord parent, GEDCOMNode nameNode, String gedcomTag, String type, String fallbackValue){
		GEDCOMNode childNode = findFirstChild(nameNode, gedcomTag);
		String value = (childNode != null && StringUtils.isNotEmpty(childNode.getValue()))
			? childNode.getValue().trim()
			: fallbackValue;

		if(StringUtils.isNotEmpty(value)){
			FLEFRecord part = FLEFRecord.createChildWithTag("part")
				.addChild(FLEFRecord.createChildWithTagAndValue("type", type))
				.addChild(FLEFRecord.createChildWithTagAndValue("value", value));
			parent.addChild(part);
		}
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
	public static void attachSource(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> objeRawMap){
		if(node == null)
			return;

		String sourValue = extractFullText(node);
		if(cleanId(sourValue) != null){
			// TODO is there something to fetch from actual raw source?

//			FLEFRecord event = null;
			GEDCOMNode pageNode = findFirstChild(node, "PAGE");
//			GEDCOMNode evenNode = findFirstChild(node, "EVEN");
//			String evenValue = (evenNode != null? evenNode.getValue(): null);
//			if(StringUtils.isNotEmpty(evenValue)){
//				GEDCOMNode evenRoleNode = findFirstChild(evenNode, "ROLE");
//				String roleValue = (evenRoleNode != null? evenRoleNode.getValue(): null);
//
//				String role = GEDCOMMapper.mapRole(roleValue, roleValue);
//				String roleType = GEDCOMMapper.mapEvent(evenValue, evenValue);
//				StringJoiner sj = new StringJoiner(StringUtils.LF);
//				if(StringUtils.isNotEmpty(role))
//					sj.add("Role: " + role);
//				if(StringUtils.isNotEmpty(roleType))
//					sj.add("Role type: " + roleType);
//				parent.addChild(createNoteStruct(sj.toString(), null));
//				event = FLEFRecord.createMainRecord(IDGenerator.nextId(EventHandler.ID_PREFIX), EventHandler.TYPE)
//					.addChild(FLEFRecord.createChildWithTagAndValue("type", GEDCOMMapper.mapEvent(evenValue, evenValue)))
//					.addChild(AuditBuilder.build(node));
//
//				model.addRecord(event);
//
//				FLEFRecord eventParticipant = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationHandler.ID_PREFIX), EventParticipationHandler.TYPE)
//					.addChild(FLEFRecord.createChildWithTag("participant")
//						.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
//					)
//					.addChild(FLEFRecord.createChildWithTagAndValue("event", event.getId()));
//				if(StringUtils.isNotEmpty(roleValue))
//					eventParticipant.addChild(FLEFRecord.createChildWithTagAndValue("role", GEDCOMMapper.mapRole(roleValue, roleValue)));
//				eventParticipant.addChild(AuditBuilder.build(node));
//
//				// check for duplicates before adding
//				Deduplicator.getDeduplicatedRecordId(model, eventParticipant);
//			}

			FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");

			GEDCOMNode dataNode = findFirstChild(node, "DATA");
			GEDCOMNode dataDateNode = findFirstChild(dataNode, "DATE");
			String dataTextNode = extractFullText(findFirstChild(dataNode, "TEXT"));
			for(GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE")){
				attachMultimediaLink(parent, model,
					multimediaLinkNode, objeRawMap);
			}

			sourValue = cleanId(sourValue);
			FLEFRecord source = FLEFRecord.createMainRecord(sourValue, SourceHandler.TYPE);
			attachDate(source, "date", getDateTime(dataDateNode));
			for(GEDCOMNode noteNode : findChildren(node, "NOTE")){
				attachNote(source, noteNode, noteRawMap);
			}
			source.addChild(AuditBuilder.build(node));
			// check for duplicates before adding
			Deduplicator.getDeduplicatedRecordId(model, source);

			sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("source", sourValue));
			transferValue(sourceCitation, "locator", pageNode);
			if(StringUtils.isNotEmpty(dataTextNode)){
				sourceCitation.addChild(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", dataTextNode))
				);
			}
			attachQuay(source, node);

			parent.addChild(sourceCitation);
		}
		else{
			FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");

			String textNode = extractFullText(findFirstChild(node, "TEXT"));
			for(GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE")){
				attachMultimediaLink(parent, model,
					multimediaLinkNode, objeRawMap);
			}

			FLEFRecord source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceHandler.ID_PREFIX), SourceHandler.TYPE)
				.addChild(FLEFRecord.createChildWithTag("title")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", "Source for " + parent.getId()))
				);
			for(GEDCOMNode noteNode : findChildren(node, "NOTE")){
				attachNote(source, noteNode, noteRawMap);
			}
			source.addChild(AuditBuilder.build(node));

			// check for duplicates before adding
			Deduplicator.getDeduplicatedRecordId(model, source);

			sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("source", source.getId()));
			if(StringUtils.isNotEmpty(textNode)){
				sourceCitation.addChild(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", textNode))
				);
			}
			attachQuay(source, node);

			parent.addChild(sourceCitation);
		}
	}

	public static FLEFRecord createNoteStruct(String text, GEDCOMNode sourNode){
		if(StringUtils.isEmpty(text)){
			return null;
		}
		FLEFRecord note = FLEFRecord.createChildWithTag("note")
			.addChild(FLEFRecord.createChildWithTagAndValue("text", text.trim()))
			.addChild(AuditBuilder.build(sourNode));
		return note;
	}


	public static void attachQuay(FLEFRecord parent, GEDCOMNode node){
		GEDCOMNode quayNode = findFirstChild(node, "QUAY");
		if(quayNode != null && quayNode.getValue() != null){
			FLEFRecord evidence = FLEFRecord.createChildWithTag("evidence");
			// 0 = unreliable/estimated data
			// 1 = Questionable reliability of evidence
			// 2 = Secondary evidence, data officially recorded sometime after event
			// 3 = Direct and primary evidence used, or by dominance of the evidence
			String informationType = switch(quayNode.getValue().trim()){
				case "3" -> EvidenceQualifiersReader.INFORMATION_TYPE_PRIMARY;
				case "2" -> EvidenceQualifiersReader.INFORMATION_TYPE_SECONDARY;
				default -> EvidenceQualifiersReader.INFORMATION_TYPE_UNDETERMINED;
			};
			evidence.addChild(FLEFRecord.createChildWithTagAndValue("information_type", informationType));
			parent.addChild(evidence);
		}
	}


	/*
	MULTIMEDIA_LINK :=
	[
	n OBJE @<XREF:OBJE>@    {1:1}
	|
	n OBJE    {1:1}
	  +1 FILE <MULTIMEDIA_FILE_REFN>    {1:M}
		 +2 FORM <MULTIMEDIA_FORMAT>  FAMILY_EVENT_STRUCTURE  {1:1}
			+3 MEDI <SOURCE_MEDIA_TYPE>    {0:1}
	  +1 TITL <DESCRIPTIVE_TITLE>    {0:1}
	]
	 */
	public static void attachMultimediaLink(FLEFRecord parent, FLEFModel model, GEDCOMNode node, Map<String, GEDCOMNode> objeRawMap){
		// Extract Xref – either from getXrefId() or from the value if it's a reference
		String objeXrefId = node.getXrefId();
		if(objeXrefId == null && StringUtils.isNotEmpty(node.getValue())){
			objeXrefId = node.getValue().trim();
		}
		objeXrefId = cleanId(objeXrefId);

		if(objeXrefId != null){
			GEDCOMNode rawObjeNode = objeRawMap.get(objeXrefId);
			if(rawObjeNode != null){
				// Recursively parse the raw OBJE record to process its FILE/TITL/etc. components
				attachMultimediaLink(parent, model, rawObjeNode, objeRawMap);
			}
		}
		else{
			List<GEDCOMNode> fileNodes = findChildren(node, "FILE");
			for(GEDCOMNode fileNode : fileNodes){
				GEDCOMNode fileFormNode = findFirstChild(fileNode, "FORM");
				String fileFormNodeValue = (fileFormNode != null? fileFormNode.getValue(): null);
				GEDCOMNode fileFormMediNode = findFirstChild(fileFormNode, "MEDI");
				String fileFormMediNodeValue = (fileFormMediNode != null? fileFormMediNode.getValue(): null);
				GEDCOMNode titlNode = findFirstChild(node, "TITL");

				FLEFRecord document = FLEFRecord.createMainRecord(IDGenerator.nextId(DocumentHandler.ID_PREFIX), DocumentHandler.TYPE);
				transferValue(document, "uri", fileNode);
				transferValue(document, "description", titlNode);
				if(StringUtils.isNotEmpty(fileFormNodeValue)){
					FLEFRecord note = FLEFRecord.createChildWithTag("note")
						.addChild(FLEFRecord.createChildWithTagAndValue("text", "Format: " + fileFormNodeValue))
						.addChild(AuditBuilder.build(node));
					document.addChild(note);
				}
				if(StringUtils.isNotEmpty(fileFormMediNodeValue)){
					FLEFRecord note = FLEFRecord.createChildWithTag("note")
						.addChild(FLEFRecord.createChildWithTagAndValue("text", "Media type: " + fileFormMediNodeValue))
						.addChild(AuditBuilder.build(node));
					document.addChild(note);
				}

				GEDCOMNode dateNode = findFirstChild(node, "_DATE");
				if(dateNode != null && StringUtils.isNotEmpty(dateNode.getValue())){
					FLEFRecord note = FLEFRecord.createChildWithTag("note")
						.addChild(FLEFRecord.createChildWithTagAndValue("text", "Date: " + dateNode.getValue()))
						.addChild(AuditBuilder.build(node));
					document.addChild(note);
				}

				document.addChild(AuditBuilder.build(node));

				// Crop from _CUTD
				FLEFRecord crop = null;
				GEDCOMNode cutdNode = GEDCOMHelper.findFirstChild(node, "_CUTD");
				if(cutdNode != null && cutdNode.getValue() != null){
					String[] parts = cutdNode.getValue().split(StringUtils.SPACE);
					if(parts.length == 4){
						try{
							int x = Integer.parseInt(parts[0]);
							int y = Integer.parseInt(parts[1]);
							int w = Integer.parseInt(parts[2]);
							int h = Integer.parseInt(parts[3]);
							crop = FLEFRecord.createChildWithTag("crop");
							crop.addChild(FLEFRecord.createChildWithTagAndValue("x", String.valueOf(x)));
							crop.addChild(FLEFRecord.createChildWithTagAndValue("y", String.valueOf(y)));
							crop.addChild(FLEFRecord.createChildWithTagAndValue("width", String.valueOf(w)));
							crop.addChild(FLEFRecord.createChildWithTagAndValue("height", String.valueOf(h)));
						}
						catch(NumberFormatException ignored){
						}
					}
				}

				// TODO _PUBL

				// check for duplicates before adding
				objeXrefId = Deduplicator.getDeduplicatedRecordId(model, document);

				// Retrieve the actual document record from the model after deduplication
				FLEFRecord actualDocument = model.getRecordById(objeXrefId);
				if(actualDocument == null)
					actualDocument = document; // Fallback

				// Find the primary OBJE (with _PRIMARY Y)
				GEDCOMNode preferredObj = null;
				GEDCOMNode primaryNode = GEDCOMHelper.findFirstChild(node, "_PRIMARY");
				if(primaryNode != null && "Y".equalsIgnoreCase(primaryNode.getValue())){
					preferredObj = node;
				}
				if(preferredObj == null){
					primaryNode = GEDCOMHelper.findFirstChild(node, "_PREF");
					if(primaryNode != null && "Y".equalsIgnoreCase(primaryNode.getValue())){
						preferredObj = node;
					}
				}

				if(preferredObj != null){
					String fileUri = FLEFRecordHelper.getChildValue(actualDocument, "uri");
					if(fileUri != null && !fileUri.isEmpty()){
						FLEFRecord prefImg = FLEFRecord.createChildWithTag("preferred_image");
						// Referencing the document by ID instead of URI
						prefImg.addChild(FLEFRecord.createChildWithTagAndValue("document", actualDocument.getId()));

						if(crop != null)
							prefImg.addChild(crop);

						parent.addChild(prefImg);
					}
				}

				FLEFRecord source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceHandler.ID_PREFIX), SourceHandler.TYPE)
					.addChild(FLEFRecord.createChildWithTag("title")
						.addChild(FLEFRecord.createChildWithTagAndValue("value", "Document " + actualDocument.getId()))
					)
					.addChild(FLEFRecord.createChildWithTagAndValue("document", actualDocument.getId()))
					.addChild(AuditBuilder.build(node));

				FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source")
					.addChild(FLEFRecord.createChildWithTagAndValue("source", source.getId()))
					.addChild(FLEFRecord.createChildWithTag("extract")
						.addChild(FLEFRecord.createChildWithTag("document_part")
							.addChild(FLEFRecord.createChildWithTagAndValue("document", actualDocument.getId()))
							.addChild(crop)
						)
					);

				parent.addChild(sourceCitation);

				model.addRecord(source);
			}
		}
	}


	public static void attachIndividualEvent(FLEFRecord parent, FLEFModel model,
		GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap,
		Map<String, GEDCOMNode> objeRawMap, PlaceCache placeCache, List<GEDCOMNode> roots){
		// Determine the FLEF event type
		String customType = node.getTag();
		String type = GEDCOMMapper.mapEvent(customType, (customType != null? customType: "other"));

		FLEFRecord event = FLEFRecord.createMainRecord(IDGenerator.nextId(EventHandler.ID_PREFIX), EventHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));

		// Date
		GEDCOMNode dateNode = findFirstChild(node, "DATE");
		attachDate(event, "date", getDateTime(dateNode));

		// Description (for generic EVEN) – ignore "Y" or "N"
		String eventValue = node.getValue();
		if(StringUtils.isNotEmpty(eventValue) && !eventValue.equalsIgnoreCase("Y")
			&& !eventValue.equalsIgnoreCase("N")){
			event.addChild(FLEFRecord.createChildWithTagAndValue("description", eventValue));
		}

		attachEventOrFactDetail(parent, event, model, node, noteRawMap, objeRawMap, placeCache, roots);
	}


	public static void attachIndividualAttribute(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap,
			Map<String, GEDCOMNode> objeRawMap, PlaceCache placeCache, List<GEDCOMNode> roots){
		// Determine the FLEF attribute type
		GEDCOMNode typeNode = findFirstChild(node, "TYPE");
		String customType = (typeNode != null? typeNode.getValue().trim(): null);
		if(customType == null)
			customType = node.getTag();
		String type = GEDCOMMapper.mapAttribute(customType, (customType != null? customType: "other"));

		FLEFRecord individualAttribute = FLEFRecord.createMainRecord(IDGenerator.nextId(IndividualAttributeHandler.ID_PREFIX), IndividualAttributeHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));

		// Value
		String eventValue = node.getValue();
		if(StringUtils.isNotEmpty(eventValue)){
			individualAttribute.addChild(FLEFRecord.createChildWithTagAndValue("value", eventValue));
		}

		// Date
		GEDCOMNode dateNode = findFirstChild(node, "DATE");
		attachDate(individualAttribute, "valid_from", getDateTime(dateNode));
		attachDate(individualAttribute, "valid_to", getDateTime(dateNode));

		attachEventOrFactDetail(parent, individualAttribute, model, node, noteRawMap, objeRawMap, placeCache, roots);
	}

	private static void attachEventOrFactDetail(FLEFRecord parent, FLEFRecord record, FLEFModel model, GEDCOMNode node,
			Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> objeRawMap, PlaceCache placeCache, List<GEDCOMNode> roots){
		// Place
		GEDCOMNode placNode = findFirstChild(node, "PLAC");
		GEDCOMNode addrNode = findFirstChild(node, "ADDR");
		attachPlaceCitation(record, model,
			placNode, addrNode, noteRawMap, placeCache);

		// Agency (AGNC) -- ONLY FOR EVENTS!
		GEDCOMNode agncNode = findFirstChild(node, "AGNC");
		GEDCOMHelper.attachNote(record, agncNode, noteRawMap);

		// Cause (CAUS) -- ONLY FOR EVENTS!
		GEDCOMNode causNode = findFirstChild(node, "CAUS");
		if(causNode != null && causNode.getValue() != null){
			FLEFRecord cause = FLEFRecord.createChildWithTag("cause");
			transferValue(cause, "reason", causNode);
			record.addChild(cause);
		}

		// ---- Sources (SOUR) ----
		for(GEDCOMNode sourNode : findChildren(node, "SOUR")){
			attachSource(record, model,
				sourNode, noteRawMap, objeRawMap);
		}

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for(GEDCOMNode noteNode : findChildren(node, "NOTE")){
			attachNote(record,
				noteNode, noteRawMap);
		}

		// Multimedia (OBJE)
		for(GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE")){
			attachMultimediaLink(record, model,
				multimediaLinkNode, objeRawMap);
		}

		// Age at event → inline note (with audit)
		GEDCOMNode ageNode = findFirstChild(node, "AGE");
		if(ageNode != null && StringUtils.isNotEmpty(ageNode.getValue())){
			FLEFRecord note = FLEFRecord.createChildWithTag("note")
				.addChild(FLEFRecord.createChildWithTagAndValue("text", "Age at event: " + ageNode.getValue()))
				.addChild(AuditBuilder.build(node));
			record.addChild(note);
		}

		// ---- FAMC (family of origin) ----
		attachFamilyOfOrigin(parent, model,
			node, noteRawMap, roots);

		attachRestriction(record, node);


		record.addChild(AuditBuilder.build(node));

		model.addRecord(record);


		FLEFRecord eventParticipant = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationHandler.ID_PREFIX), EventParticipationHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag("participant")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
			)
			.addChild(FLEFRecord.createChildWithTagAndValue("event", record.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue("role", "principal"))
			.addChild(AuditBuilder.build(node));
		// check for duplicates before adding
		Deduplicator.getDeduplicatedRecordId(model, eventParticipant);
	}


	public static void attachFamilyOfOrigin(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, List<GEDCOMNode> roots){
		for(GEDCOMNode famcNode : findChildren(node, "FAMC")){
			if(famcNode.getValue() != null){
				GEDCOMNode famcAdopNode = findFirstChild(famcNode, "ADOP");
				String adopParent = (famcAdopNode != null? famcAdopNode.getValue(): null);

				StringBuilder sb = new StringBuilder();
				GEDCOMNode pediNode = findFirstChild(famcNode, "PEDI");
				if(pediNode != null && pediNode.getValue() != null){
					sb.append(" (Pedigree: ")
						.append(pediNode.getValue())
						.append(')');
				}
				GEDCOMNode statNode = findFirstChild(famcNode, "STAT");
				if(statNode != null && statNode.getValue() != null){
					sb.append(" (Status: ")
						.append(statNode.getValue())
						.append(')');
				}

				String groupId = cleanId(famcNode.getValue());
				/*if(adopParent == null)
					attachRelationship(parent, model, node, noteRawMap, famcNode, "group", groupId, sb);
				else*/ if("HUSB".equals(adopParent) || "WIFE".equals(adopParent) || "BOTH".equals(adopParent)){
					// Build an index of all level-0 records by xref id
					Map<String, GEDCOMNode> recordsById = roots.stream()
						.filter(n -> n.getXrefId() != null)
						.collect(Collectors.toMap(GEDCOMNode::getXrefId, Function.identity()));
					GEDCOMNode familyNode = recordsById.get(groupId);
					GEDCOMNode husbandNode = familyNode.getChildren().stream()
						.filter(child -> "HUSB".equals(child.getTag()))
						.findFirst()
						.orElse(null);
					String husbandId = (husbandNode != null? cleanId(husbandNode.getValue()): null);
					GEDCOMNode wifeNode = familyNode.getChildren().stream()
						.filter(child -> "WIFE".equals(child.getTag()))
						.findFirst()
						.orElse(null);
					String wifeId = (wifeNode != null? cleanId(wifeNode.getValue()): null);

					if(husbandId != null && ("HUSB".equals(adopParent) || "BOTH".equals(adopParent))){
						attachRelationship(parent, model, node, noteRawMap, famcNode, "individual", husbandId, sb);
					}
					if(wifeId != null && ("WIFE".equals(adopParent) || "BOTH".equals(adopParent))){
						attachRelationship(parent, model, node, noteRawMap, famcNode, "individual", wifeId, sb);
					}
				}
			}
		}
	}

	private static void attachRelationship(FLEFRecord parent, FLEFModel model, GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, GEDCOMNode famcNode, String objectTag, String objectXrefId, StringBuilder sb){
		FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipHandler.ID_PREFIX), RelationshipHandler.TYPE)
			// subject: child
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
			)
			// target
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue(objectTag, objectXrefId))
			)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", (node.getTag().equalsIgnoreCase("ADOP")? RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD: RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)))
			.addChild(AuditBuilder.build(node));

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for(GEDCOMNode noteNode : findChildren(famcNode, "NOTE")){
			attachNote(relationship,
				noteNode, noteRawMap);
		}

		if(!sb.toString().trim().isEmpty()){
			FLEFRecord note1 = FLEFRecord.createChildWithTag("note")
				.addChild(FLEFRecord.createChildWithTagAndValue("text", sb.toString().trim()))
				.addChild(AuditBuilder.build(node));
			relationship.addChild(note1);
		}

		FLEFRecord note2 = FLEFRecord.createChildWithTag("note")
			.addChild(FLEFRecord.createChildWithTagAndValue("text", "TO BE REVISED: the relationship should be with a biological father and a biological mother instead?"))
			.addChild(AuditBuilder.build(node));
		relationship.addChild(note2);

		// check for duplicates before adding
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}


	public static void attachSpouseToFamily(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap){
		for(GEDCOMNode famsNode : findChildren(node, "FAMS")){
			if(famsNode.getValue() != null){
				FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipHandler.ID_PREFIX), RelationshipHandler.TYPE)
					// subject: child
					.addChild(FLEFRecord.createChildWithTag("subject")
						.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
					)
					// target: group
					.addChild(FLEFRecord.createChildWithTag("target")
						.addChild(FLEFRecord.createChildWithTagAndValue("group", cleanId(famsNode.getValue())))
					)
					.addChild(FLEFRecord.createChildWithTagAndValue("type", "spouse"));
				relationship.addChild(AuditBuilder.build(node));

				// ---- Notes (GEDCOM NOTE) – inline structs ----
				for(GEDCOMNode noteNode : findChildren(famsNode, "NOTE")){
					attachNote(relationship,
						noteNode, noteRawMap);
				}

				FLEFRecord note2 = FLEFRecord.createChildWithTag("note")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", "TO BE REVISED: is it civil or religious or else?"))
					.addChild(AuditBuilder.build(node));
				relationship.addChild(note2);

				// check for duplicates before adding
				Deduplicator.getDeduplicatedRecordId(model, relationship);
			}
		}
	}


	public static void attachGroupAttribute(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap,
			Map<String, GEDCOMNode> objeRawMap, PlaceCache placeCache, List<GEDCOMNode> roots, String groupId,
			String husbandId, String wifeId){
		GEDCOMNode husbNode = findFirstChild(node, "HUSB");
		GEDCOMNode husbAgeNode = findFirstChild(husbNode, "AGE");
		String husbAge = (husbAgeNode != null? husbAgeNode.getValue(): null);
		GEDCOMNode wifeNode = findFirstChild(node, "WIFE");
		GEDCOMNode wifeAgeNode = findFirstChild(wifeNode, "AGE");
		String wifeAge = (wifeAgeNode != null? wifeAgeNode.getValue(): null);

		// Determine the FLEF attribute type
		GEDCOMNode typeNode = findFirstChild(node, "TYPE");
		String customType = (typeNode != null? typeNode.getValue().trim(): null);
		if(customType == null)
			customType = node.getTag();
		String type = GEDCOMMapper.mapAttribute(customType, (customType != null? customType: "other"));

		FLEFRecord groupAttribute = FLEFRecord.createMainRecord(IDGenerator.nextId(GroupAttributeHandler.ID_PREFIX), GroupAttributeHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue("group", parent.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));

		// Value
		String eventValue = node.getValue();
		if(StringUtils.isNotEmpty(eventValue)){
			groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("value", eventValue));
		}

		// Date
		GEDCOMNode dateNode = findFirstChild(node, "DATE");
		attachDate(groupAttribute, "valid_from", getDateTime(dateNode));
		attachDate(groupAttribute, "valid_to", getDateTime(dateNode));

		attachEventOrFactDetail(parent, groupAttribute, model, node, noteRawMap, objeRawMap, placeCache, roots);

		// Link participants
		if(husbandId != null){
			createEventParticipation(groupId, husbandId, husbAge, "individual", "principal", model, node);
		}
		if(wifeId != null){
			createEventParticipation(groupId, wifeId, wifeAge, "individual", "principal", model, node);
		}
	}

	private static void createEventParticipation(String eventId, String entityId, String entityAge, String entityType, String role, FLEFModel model, GEDCOMNode node){
		FLEFRecord eventParticipation = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationHandler.ID_PREFIX), EventParticipationHandler.TYPE)
			// participant
			.addChild(FLEFRecord.createChildWithTag("participant")
				.addChild(FLEFRecord.createChildWithTagAndValue(entityType, entityId))
			)
			// event
			.addChild(FLEFRecord.createChildWithTagAndValue("event", eventId));
		// role (mappato)
		if(role != null){
			String mappedRole = GEDCOMMapper.mapRole(role, role);
			eventParticipation.addChild(FLEFRecord.createChildWithTagAndValue("role", (mappedRole != null? role: null)));
		}

		if(StringUtils.isNotEmpty(entityAge)){
			FLEFRecord note = FLEFRecord.createChildWithTag("note")
				.addChild(FLEFRecord.createChildWithTagAndValue("text", "Age: " + entityAge))
				.addChild(AuditBuilder.build(node));
			eventParticipation.addChild(note);
		}

		eventParticipation.addChild(AuditBuilder.build(node));

		// check for duplicates before adding
		Deduplicator.getDeduplicatedRecordId(model, eventParticipation);
	}


	public static void attachDate(FLEFRecord parent, String tag, String date){
		DateInfo dateInfo = GEDCOMDateParser.parse(date);
		if(dateInfo != null){
			FLEFRecord dateValue = FLEFRecord.createChildWithTag("value");
			switch(dateInfo.getType()){
				case POINT -> {
					if(dateInfo.getValue() != null && !FORMAL_GEDCOM_DATE_PATTERN.matcher(dateInfo.getValue()).matches()){
						// Fallback: If parsing fails, save the raw date text as a note
						FLEFRecord note = createNoteStruct(date, null);
						if(note != null){
							parent.addChild(note);
						}
						return;
					}

					FLEFRecord valuePoint = FLEFRecord.createChildWithTag("point");
					FLEFRecord valuePointFullDate = FLEFRecord.createChildWithTag("full_date");
					valuePointFullDate.setValue(dateInfo.getValue());

					FLEFRecord valueContainer = FLEFRecord.createChildWithTag("value");
					valueContainer.addChild(valuePointFullDate);
					valuePoint.addChild(valueContainer);

					valuePoint.addChild(FLEFRecord.createChildWithTagAndValue("calendar", getCalendarForDate(dateInfo.getValue())));

					if(dateInfo.isApproximate()){
						FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
						String basis = switch(dateInfo.getQualifier()){
							case "ABT" -> DateReader.ENUM_BASIS_STATED;
							case "CAL" -> DateReader.ENUM_BASIS_CALCULATED;
							case "EST" -> DateReader.ENUM_BASIS_CONVENTIONAL;
							default -> DateReader.ENUM_BASIS_UNSPECIFIED;
						};
						approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", basis));
						valuePoint.addChild(approx);
					}
					dateValue.addChild(valuePoint);
				}
				case BOUNDED -> {
					if(dateInfo.getNotBefore() != null && !FORMAL_GEDCOM_DATE_PATTERN.matcher(dateInfo.getNotBefore()).matches()
							|| dateInfo.getNotAfter() != null && !FORMAL_GEDCOM_DATE_PATTERN.matcher(dateInfo.getNotAfter()).matches()){
						// Fallback: If parsing fails, save the raw date text as a note
						FLEFRecord note = createNoteStruct(date, null);
						if(note != null){
							parent.addChild(note);
						}
						return;
					}

					FLEFRecord boundedRec = FLEFRecord.createChildWithTag("bounded");
					if(dateInfo.getNotBefore() != null){
						FLEFRecord nbNode = buildSingleDateForHelper(dateInfo.getNotBefore(), dateInfo.isApproximate());
						boundedRec.addChild(nbNode.setTag("not_before"));
					}
					if(dateInfo.getNotAfter() != null){
						FLEFRecord naNode = buildSingleDateForHelper(dateInfo.getNotAfter(), dateInfo.isApproximate());
						boundedRec.addChild(naNode.setTag("not_after"));
					}
					dateValue.addChild(boundedRec);
				}
				case SPANNING -> {
					if(dateInfo.getFrom() != null && !FORMAL_GEDCOM_DATE_PATTERN.matcher(dateInfo.getFrom()).matches()
							|| dateInfo.getTo() != null && !FORMAL_GEDCOM_DATE_PATTERN.matcher(dateInfo.getTo()).matches()){
						// Fallback: If parsing fails, save the raw date text as a note
						FLEFRecord note = createNoteStruct(date, null);
						if(note != null){
							parent.addChild(note);
						}
						return;
					}

					FLEFRecord spanningRec = FLEFRecord.createChildWithTag("spanning");
					if(dateInfo.getFrom() != null){
						FLEFRecord fromNode = buildSingleDateForHelper(dateInfo.getFrom(), dateInfo.isApproximate());
						spanningRec.addChild(fromNode.setTag("from"));
					}
					if(dateInfo.getTo() != null){
						FLEFRecord toNode = buildSingleDateForHelper(dateInfo.getTo(), dateInfo.isApproximate());
						spanningRec.addChild(toNode.setTag("to"));
					}
					dateValue.addChild(spanningRec);
				}
			}
			parent.addChild(FLEFRecord.createChildWithTag(tag)
				.addChild(dateValue)
			);
		}
	}

	private static FLEFRecord buildSingleDateForHelper(String isoDate, boolean approximate){
		if(StringUtils.isEmpty(isoDate)){
			isoDate = null;
		}
		FLEFRecord singleDate = FLEFRecord.createEmpty();
		FLEFRecord fullDate = FLEFRecord.createChildWithTag("full_date");
		if(isoDate != null)
			fullDate.setValue(isoDate);

		FLEFRecord valueContainer = FLEFRecord.createChildWithTag("value");
		valueContainer.addChild(fullDate);
		singleDate.addChild(valueContainer);

		if(isoDate != null)
			singleDate.addChild(FLEFRecord.createChildWithTagAndValue("calendar", getCalendarForDate(isoDate)));

		if(approximate){
			FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
			approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", DateReader.ENUM_BASIS_STATED));
			singleDate.addChild(approx);
		}
		return singleDate;
	}

	/**
	 * Builds a QualifiedDate structure from an ISO date string.
	 */
	private static FLEFRecord buildQualifiedDate(String isoDate){
		// Ensure we have a non‑empty date string; fallback to a default if missing
		if(StringUtils.isEmpty(isoDate)){
			isoDate = "1900-01-01";
		}
		FLEFRecord fullDate = FLEFRecord.createChildWithTag("full_date");
		fullDate.addChild(FLEFRecord.createChildWithTagAndValue("value", isoDate));
		fullDate.addChild(FLEFRecord.createChildWithTagAndValue("calendar", getCalendarForDate(isoDate)));
		return fullDate;
	}

	/**
	 * Determines the appropriate calendar for a date.
	 * If the year is before 1582, returns "julian", otherwise "gregorian".
	 */
	private static String getCalendarForDate(String isoDate){
		if(isoDate == null)
			return "gregorian";
		int year;
		try{
			if(isoDate.contains("-")){
				year = Integer.parseInt(StringUtils.split(isoDate, '-')[0]);
			}
			else{
				year = Integer.parseInt(isoDate);
			}
		}
		catch(NumberFormatException e){
			return "gregorian";
		}
		return (year < 1582? "julian": "gregorian");
	}


	/**
	 * Parses a GEDCOM PLAC node and returns a PlaceCitation.
	 * It also creates/updates the underlying PlaceRecord with subfields.
	 */
	public static void attachPlaceCitation(FLEFRecord parent, FLEFModel model,
			GEDCOMNode placNode, GEDCOMNode addrNode, Map<String, GEDCOMNode> noteRawMap, PlaceCache placeCache){
		if(placNode == null && addrNode == null)
			return;

		final String placeName = (placNode != null? placNode.getValue(): null);

		// Extract address components if present
		final StringBuilder streetAddr = new StringBuilder();
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
		}

		final String finalPlaceName;
		final GEDCOMNode targetPlacNode;
		if(StringUtils.isEmpty(placeName)){
			// Only ADDR exists, use address as place name
			finalPlaceName = streetAddr.toString().replace(StringUtils.LF, ", ");
			targetPlacNode = (addrNode != null? addrNode: new GEDCOMNode(1, "PLAC", finalPlaceName));
		}
		else if(streetAddr.isEmpty()){
			// Only PLAC exists
			finalPlaceName = placeName;
			targetPlacNode = placNode;
		}
		else{
			// Both exist: combine street address with place name, avoiding duplication
			final String streetStr = streetAddr.toString().replace(StringUtils.LF, ", ");
			if(!placeName.toLowerCase(Locale.ROOT).contains(streetStr.toLowerCase(Locale.ROOT))){
				finalPlaceName = streetStr + ", " + placeName;
			}
			else{
				finalPlaceName = streetStr;
			}
			targetPlacNode = createSyntheticPlacNode(placNode, finalPlaceName);
		}

		if(StringUtils.isEmpty(finalPlaceName))
			return;

		// TODO
//		// ---- FONE (phonetic variation) ----
//		for(GEDCOMNode foneNode : findChildren(placNode, "FONE")){
//			String phonetic = extractFullText(foneNode);
//			if(StringUtils.isNotEmpty(phonetic)){
//				// Store as a variant note
//				String system = "IPA"; // default
//				GEDCOMNode typeNode = findFirstChild(foneNode, "TYPE");
//				if(typeNode != null && typeNode.getValue() != null){
//					system = typeNode.getValue();
//				}
//				String text = "Phonetic (" + system + "): " + phonetic;
//				FLEFRecord note = createNoteStruct(text, foneNode);
//				if(note != null) place.addChild(note);
//			}
//		}

		// TODO
//		// ---- ROMN (romanized variation) ----
//		for(GEDCOMNode romnNode : findChildren(placNode, "ROMN")){
//			String romanized = extractFullText(romnNode);
//			if(StringUtils.isNotEmpty(romanized)){
//				String system = "scientific"; // default
//				GEDCOMNode typeNode = findFirstChild(romnNode, "TYPE");
//				if(typeNode != null && typeNode.getValue() != null){
//					system = typeNode.getValue();
//				}
//				String text = "Romanized (" + system + "): " + romanized;
//				FLEFRecord note = createNoteStruct(text, romnNode);
//				if(note != null) place.addChild(note);
//			}
//		}


		// Use PlaceCache to handle hierarchy, splitting, deduplication, and links
		final FLEFRecord placeRecord = (placeCache != null? placeCache.getOrCreatePlace(targetPlacNode): null);
		if(placeRecord == null)
			return;

		// Attach NOTE (notes under PLAC) to the leaf place if needed
		for(final GEDCOMNode noteNode : findChildren(placNode, "NOTE")){
			attachNote(placeRecord, noteNode, noteRawMap);
		}

		final FLEFRecord placeCitation = FLEFRecord.createChildWithTag("place");
		placeCitation.setValue(placeRecord.getId());
		parent.addChild(FLEFRecord.createChildWithTag("place")
			.addChild(placeCitation)
		);
	}

	private static GEDCOMNode createSyntheticPlacNode(final GEDCOMNode originalPlac, final String newAddress){
		final GEDCOMNode synthetic = new GEDCOMNode(originalPlac.getLevel(), "PLAC", newAddress);
		for(final GEDCOMNode child : originalPlac.getChildren())
			synthetic.addChild(child);
		return synthetic;
	}


	public static void attachRestriction(FLEFRecord parent, GEDCOMNode node){
		GEDCOMNode resnNode = findFirstChild(node, "RESN");
		if(resnNode != null && resnNode.getValue() != null){
			String level = GEDCOMMapper.mapPrivacyLevel(resnNode.getValue());
			FLEFRecord privacy = FLEFRecord.createChildWithTag("privacy")
				.addChild(FLEFRecord.createChildWithTagAndValue("level", level));
			parent.addChild(privacy);
		}
	}



	public static void attachAddressToContact(FLEFRecord parent, GEDCOMNode addrNode, GEDCOMNode parentNode){
		if(addrNode == null){
			return;
		}

		final StringBuilder fullAddr = new StringBuilder(addrNode.getValue() != null? extractFullText(addrNode): StringUtils.EMPTY);
		for(String subTag : List.of("ADR1", "ADR2", "ADR3", "CITY", "STAE", "POST", "CTRY")){
			GEDCOMNode sub = findFirstChild(addrNode, subTag);
			if(sub != null && sub.getValue() != null){
				if(!fullAddr.isEmpty())
					fullAddr.append(StringUtils.LF);
				fullAddr.append(sub.getValue());
			}
		}
		if(fullAddr.isEmpty()){
			return;
		}

		FLEFRecord contact = FLEFRecord.createChildWithTag("contact")
			.addChild(FLEFRecord.createChildWithTagAndValue("value", fullAddr.toString()));

		// Phone, email, etc. as inline notes (with audit)
		for(GEDCOMNode child : addrNode.getChildren()){
			String tag = child.getTag();
			if(tag.equals("PHON") || tag.equals("EMAIL") || tag.equals("FAX") || tag.equals("WWW")){
				String type = switch(tag){
					case "PHON" -> "phone";
					case "EMAIL" -> "email";
					case "FAX" -> "fax";
					default -> "website";
				};
				String text = type + ": " + child.getValue();

				FLEFRecord note = FLEFRecord.createChildWithTag("note")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", text.trim()))
					.addChild(AuditBuilder.build(parentNode));
				contact.addChild(note);
			}
		}

		// Audit for the contact (required)
		contact.addChild(AuditBuilder.build(parentNode));

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

	/**
	 * Creates an EventRecord from a GEDCOM event node, adds it to the roots list,
	 * and returns the generated FLEF event ID.
	 */
	public static String createAndAddEventRecord(
		GEDCOMNode eventNode,
		Map<String, GEDCOMNode> noteRawMap,
		Map<String, GEDCOMNode> objeRawMap, PlaceCache placeCache, FLEFModel model){

		if(eventNode == null)
			return null;

		String eventFlefId = IDGenerator.nextId(EventHandler.ID_PREFIX);
		FLEFRecord eventRecord = FLEFRecord.createMainRecord(eventFlefId, EventHandler.TYPE);

		// Type
		if(eventNode.getTag() != null){
			eventRecord.addChild(FLEFRecord.createChildWithTagAndValue("type", eventNode.getTag().toLowerCase()));
		}

		// Date
		GEDCOMNode dateNode = findFirstChild(eventNode, "DATE");
		if(dateNode != null && dateNode.getValue() != null){
			attachDate(eventRecord, "date", getDateTime(dateNode));
		}

		// Place
		GEDCOMNode placNode = findFirstChild(eventNode, "PLAC");
		GEDCOMNode addrNode = findFirstChild(eventNode, "ADDR");
		if(placNode != null && placNode.getValue() != null){
			attachPlaceCitation(eventRecord, model,
				placNode, addrNode, noteRawMap, placeCache);
		}

		// Inline Notes
		for(GEDCOMNode noteNode : findChildren(eventNode, "NOTE")){
			attachNote(eventRecord, noteNode, noteRawMap);
		}

		// Inline Sources
		for(GEDCOMNode sourNode : findChildren(eventNode, "SOUR")){
			attachSource(eventRecord, model, sourNode, noteRawMap, objeRawMap);
		}

		// Se roots gestisce gli FLEFRecord principali
		// (Adatta il cast/aggiunta in base alla struttura delle tue liste di output)
		// roots.add(eventRecord);
		eventRecord.addChild(AuditBuilder.build(eventNode));

		model.addRecord(eventRecord);

		return eventFlefId;
	}

	/**
	 * Attaches an EventParticipationRecord linking an individual or group to an EventRecord.
	 */
	public static void attachEventParticipation(
		String eventId,
		String entityType,
		String entityId,
		String role, FLEFModel model){

		if(StringUtils.isEmpty(eventId)){
			return;
		}

		FLEFRecord participation = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationHandler.ID_PREFIX), EventParticipationHandler.TYPE);

		participation.addChild(FLEFRecord.createChildWithTagAndValue("event", eventId));

		FLEFRecord participant = FLEFRecord.createChildWithTag("participant")
			.addChild(FLEFRecord.createChildWithTagAndValue(entityType, entityId));
		participation.addChild(participant);

		if(role != null){
			participation.addChild(FLEFRecord.createChildWithTagAndValue("role", role));
		}
		participation.addChild(AuditBuilder.build());

		Deduplicator.getDeduplicatedRecordId(model, participation);
	}

}
