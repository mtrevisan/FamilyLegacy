package io.github.mtrevisan.familylegacy.v2.gedcom;

import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.DateInfo;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMDateParser;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
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


	private GEDCOMHelper(){}


	@SuppressWarnings({"ResultOfMethodCallIgnored", "IOResourceOpenedButNotSafelyClosed"})
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
		if (xref != null) {
			String cleaned = IDNormalizer.clean(xref);
			if (isValidIdFormat(cleaned)) {
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
		if(id != null && id.startsWith("@") && id.endsWith("@")){
			return IDNormalizer.clean(id);
		}
		return null;
	}

	/**
	 * Checks if the ID matches the FLEF format: letters followed by digits.
	 */
	private static boolean isValidIdFormat(String id) {
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
		if(node == null){
			return Collections.emptyList();
		}
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.toList();
	}


	/**
	 * Reconstructs the full note text from a GEDCOM NOTE node.
	 * Handles CONC and CONT children to concatenate lines correctly.
	 *
	 * @param noteNode the GEDCOM NOTE node
	 * @return the full text, or null if no text found
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
					if("CONT".equals(tag) && !sb.isEmpty()){
						sb.append('\n');
					}
					sb.append(child.getValue());
				}
			}
		}
		return !sb.isEmpty() ? sb.toString(): null;
	}

	public static String getDateTime(GEDCOMNode dateNode){
		String dateTime = null;
		if(dateNode != null && StringUtils.isNotEmpty(dateNode.getValue())){
			String datePart = dateNode.getValue()
				.trim();
			GEDCOMNode timeNode = findFirstChild(dateNode, "TIME");
			if(timeNode != null && timeNode.getValue() != null){
				dateTime = datePart + (isIsoDate(datePart)? "T": " ") + timeNode.getValue().trim();
			}
			else{
				dateTime = datePart;
			}
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
		catch(Exception e){
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

		FLEFRecord note = FLEFRecord.createChildWithTag("note")
			.addChild(FLEFRecord.createChildWithTagAndValue("text", fullText.trim()))
			.addChild(AuditBuilder.build(node));
		parent.addChild(note);
	}


	/**
	 * Parses a PersonalNameStructure (for individuals) extracting all sub-tags
	 * (GIVN, SURN, NPFX, NSFX, SPFX, NICK), inline sources, and notes with CONC/CONT.
	 */
	public static void attachPersonalNameStructure(FLEFRecord parent,
			GEDCOMNode nameNode, FLEFModel model, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap, Map<String, GEDCOMNode> objeRawMap){
		if(nameNode == null){
			return;
		}

		FLEFRecord name = FLEFRecord.createChildWithTag("name");

		// Type (optional)
		GEDCOMNode typeNode = findFirstChild(nameNode, "TYPE");
		transferValue(name, "type", typeNode);

		// Parse inline value (e.g., "Joseph Tag /Torture/") into full text
		String given = "";
		String family = "";
		String raw = extractFullText(nameNode);
		if(StringUtils.isNotEmpty(raw)){
			int slash1 = raw.indexOf('/');
			int slash2 = raw.indexOf('/', slash1 + 1);
			if(slash1 >= 0 && slash2 > slash1){
				given = raw.substring(0, slash1).trim();
				family = raw.substring(slash1 + 1, slash2)
					.trim();
				String suffix = raw.substring(slash2 + 1)
					.trim();
				if(!suffix.isEmpty()){
					given = (given + " " + suffix).trim();
				}
			}
			else{
				given = raw.trim();
			}
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
//			String system = (romnType != null && romnType.getValue() != null) ? romnType.getValue() : "scientific";
//			transcription.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
//			transcription.addChild(FLEFRecord.createChildWithTagAndValue("value", romn.getValue()));
//			variant.addChild(transcription);
//			namePhoneticVariations.addChild(variant);
//		}

		for (GEDCOMNode sourNode : findChildren(nameNode, "SOUR")) {
			attachSource(parent, model,
				sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		for (GEDCOMNode noteNode : findChildren(nameNode, "NOTE")) {
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
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap, Map<String, GEDCOMNode> objeRawMap){
		if(node == null)
			return;

		String sourValue = extractFullText(node);
		if(cleanId(sourValue) != null){
			// TODO is there something to fetch from actual raw source?

			FLEFRecord event = null;
			GEDCOMNode pageNode = findFirstChild(node, "PAGE");
			GEDCOMNode evenNode = findFirstChild(node, "EVEN");
			String evenValue = (evenNode != null? evenNode.getValue(): null);
			if(StringUtils.isNotEmpty(evenValue)){
				GEDCOMNode evenRoleNode = findFirstChild(evenNode, "ROLE");
				String roleValue = (evenRoleNode != null? evenRoleNode.getValue(): null);

				event = FLEFRecord.createMainRecord(IDGenerator.nextId(EventHandler.ID_PREFIX), EventHandler.TYPE)
					.addChild(FLEFRecord.createChildWithTagAndValue("type", GEDCOMMapper.mapEvent(evenValue, evenValue)))
					.addChild(AuditBuilder.build(node));

				model.addRecord(event);

				FLEFRecord eventParticipant = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationHandler.ID_PREFIX), EventParticipationHandler.TYPE)
					.addChild(FLEFRecord.createChildWithTag("participant")
						.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
					)
					.addChild(FLEFRecord.createChildWithTagAndValue("event", event.getId()));
				if(StringUtils.isNotEmpty(roleValue))
					eventParticipant.addChild(FLEFRecord.createChildWithTagAndValue("role", GEDCOMMapper.mapRole(roleValue, roleValue)));
				eventParticipant.addChild(AuditBuilder.build(node));

				// check for duplicates before adding
				Deduplicator.getDeduplicatedRecordId(model, eventParticipant);
			}

			FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");

			GEDCOMNode dataNode = findFirstChild(node, "DATA");
			GEDCOMNode dataDateNode = findFirstChild(dataNode, "DATE");
			String dataTextNode = extractFullText(findFirstChild(dataNode, "TEXT"));
			for (GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE")) {
				attachMultimediaLink(parent, model,
					multimediaLinkNode, objeRawMap);
			}

			sourValue = cleanId(sourValue);
			FLEFRecord source = FLEFRecord.createMainRecord(sourValue, SourceHandler.TYPE);
			transferValue(source, "date", dataDateNode);
			for (GEDCOMNode noteNode : findChildren(node, "NOTE")) {
				attachNote(source, noteNode, noteRawMap);
			}
			source.addChild(AuditBuilder.build(node));
			// check for duplicates before adding
			Deduplicator.getDeduplicatedRecordId(model, source);

			sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("source", sourValue));
			transferValue(sourceCitation, "location", pageNode);
			if(StringUtils.isNotEmpty(dataTextNode)){
				sourceCitation.addChild(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", dataTextNode))
				);
			}
			attachQuay(source, node);

			if(event != null){
				event.addChild(sourceCitation);
			}

			parent.addChild(sourceCitation);
		}
		else{
			FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");

			String textNode = extractFullText(findFirstChild(node, "TEXT"));
			for (GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE")) {
				attachMultimediaLink(parent, model,
					multimediaLinkNode, objeRawMap);
			}

			FLEFRecord source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceHandler.ID_PREFIX), SourceHandler.TYPE)
				.addChild(FLEFRecord.createChildWithTag("title")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", "Source for " + parent.getId()))
				);
			for (GEDCOMNode noteNode : findChildren(node, "NOTE")) {
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


	public static void attachQuay(FLEFRecord parent, GEDCOMNode node){
		GEDCOMNode quayNode = findFirstChild(node, "QUAY");
		if(quayNode != null && quayNode.getValue() != null){
			FLEFRecord evidence = FLEFRecord.createChildWithTag("evidence");
			// 0 = unreliable/estimated data
			// 1 = Questionable reliability of evidence
			// 2 = Secondary evidence, data officially recorded sometime after event
			// 3 = Direct and primary evidence used, or by dominance of the evidence
			String informationType = switch(quayNode.getValue().trim()){
				case "3" -> "primary";
				case "2" -> "secondary";
				default -> "undetermined";
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
				return;
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

				// Crop from _CUTD
				FLEFRecord crop = null;
				GEDCOMNode cutdNode = GEDCOMHelper.findFirstChild(node, "_CUTD");
				if(cutdNode != null && cutdNode.getValue() != null){
					String[] parts = cutdNode.getValue().split(" ");
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
					String fileUri = FLEFRecordHelper.getChildValue(document, "uri");
					if(fileUri != null && !fileUri.isEmpty()){
						FLEFRecord prefImg = FLEFRecord.createChildWithTag("preferred_image");
						prefImg.addChild(FLEFRecord.createChildWithTagAndValue("uri", fileUri));
						prefImg.addChild(crop);

						parent.addChild(prefImg);
					}
				}

				// TODO _PUBL

				document.addChild(AuditBuilder.build(node));

				// check for duplicates before adding
				objeXrefId = Deduplicator.getDeduplicatedRecordId(model, document);

				FLEFRecord source = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceHandler.ID_PREFIX), SourceHandler.TYPE);
				FLEFRecord titleRec = FLEFRecord.createChildWithTag("title")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", "Document " + objeXrefId));
				source.addChild(titleRec);
				FLEFRecord docRef = FLEFRecord.createChildWithTagAndValue("document", objeXrefId);
				source.addChild(docRef);
				source.addChild(AuditBuilder.build(node));

				FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source")
					.addChild(FLEFRecord.createChildWithTagAndValue("source", source.getId()))
					.addChild(FLEFRecord.createChildWithTag("extract")
						.addChild(FLEFRecord.createChildWithTag("document_part")
							.addChild(FLEFRecord.createChildWithTagAndValue("document", objeXrefId))
							.addChild(crop)
						)
					);

				parent.addChild(sourceCitation);

				model.addRecord(source);
			}
		}
	}


	public static void attachIndividualEvent(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap, Map<String, GEDCOMNode> objeRawMap, List<GEDCOMNode> roots){
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

		attachEventOrFactDetail(parent, event, model, node, noteRawMap, sourRawMap, objeRawMap, roots);
	}


	public static void attachIndividualAttribute(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap, Map<String, GEDCOMNode> objeRawMap, List<GEDCOMNode> roots){
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

		attachEventOrFactDetail(parent, individualAttribute, model, node, noteRawMap, sourRawMap, objeRawMap, roots);
	}

	private static void attachEventOrFactDetail(FLEFRecord parent, FLEFRecord record, FLEFModel model, GEDCOMNode node,
			Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap, Map<String, GEDCOMNode> objeRawMap, List<GEDCOMNode> roots){
		// Place
		GEDCOMNode placNode = findFirstChild(node, "PLAC");
		GEDCOMNode addrNode = findFirstChild(node, "ADDR");
		attachPlaceCitation(record, model,
			placNode, addrNode, noteRawMap);

		// Agency (AGNC) -- ONLY FOR EVENTS!
		GEDCOMNode agncNode = findFirstChild(node, "AGNC");
		transferValue(record, "agency", agncNode);

		// Cause (CAUS) -- ONLY FOR EVENTS!
		GEDCOMNode causNode = findFirstChild(node, "CAUS");
		if(causNode != null && causNode.getValue() != null){
			FLEFRecord cause = FLEFRecord.createChildWithTag("cause");
			transferValue(cause, "reason", causNode);
			record.addChild(cause);
		}

		// ---- Sources (SOUR) ----
		for (GEDCOMNode sourNode : findChildren(node, "SOUR")) {
			attachSource(record, model,
				sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for (GEDCOMNode noteNode : findChildren(node, "NOTE")) {
			attachNote(record,
				noteNode, noteRawMap);
		}

		// Multimedia (OBJE)
		for (GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE")) {
			attachMultimediaLink(record, model,
				multimediaLinkNode, objeRawMap);
		}

		// Age at event → inline note (with audit)
		GEDCOMNode ageNode = findFirstChild(node, "AGE");
		if(ageNode != null && ageNode.getValue() != null){
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
			.addChild(AuditBuilder.build(node));
		// check for duplicates before adding
		Deduplicator.getDeduplicatedRecordId(model, eventParticipant);
	}


	public static void attachFamilyOfOrigin(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, List<GEDCOMNode> roots){
		for (GEDCOMNode famcNode : findChildren(node, "FAMC")) {
			if (famcNode.getValue() != null) {
				GEDCOMNode famcAdopNode = findFirstChild(famcNode, "ADOP");
				String adopParent = (famcAdopNode != null ? famcAdopNode.getValue() : null);

				StringBuilder sb = new StringBuilder();
				GEDCOMNode pediNode = findFirstChild(famcNode, "PEDI");
				if (pediNode != null && pediNode.getValue() != null) {
					sb.append(" (Pedigree: ").append(pediNode.getValue()).append(")");
				}
				GEDCOMNode statNode = findFirstChild(famcNode, "STAT");
				if (statNode != null && statNode.getValue() != null) {
					sb.append(" (Status: ").append(statNode.getValue()).append(")");
				}

				String groupId = cleanId(famcNode.getValue());
				if(adopParent == null){
					if(node.getTag().equalsIgnoreCase("ADOP"))
						attachRelationship(parent, model, node, noteRawMap, famcNode, "group", groupId, sb);
				}
				else if("HUSB".equals(adopParent) || "WIFE".equals(adopParent) || "BOTH".equals(adopParent)){
					// Build an index of all level-0 records by xref id
					Map<String, GEDCOMNode> recordsById = roots.stream()
						.filter(n -> n.getXrefId() != null)
						.collect(Collectors.toMap(GEDCOMNode::getXrefId, Function.identity()));
					GEDCOMNode familyNode = recordsById.get(groupId);
					GEDCOMNode husbandNode = familyNode.getChildren().stream()
						.filter(child -> "HUSB".equals(child.getTag()))
						.findFirst()
						.orElse(null);
					String husbandId = husbandNode != null ? cleanId(husbandNode.getValue()) : null;
					GEDCOMNode wifeNode = familyNode.getChildren().stream()
						.filter(child -> "WIFE".equals(child.getTag()))
						.findFirst()
						.orElse(null);
					String wifeId = wifeNode != null ? cleanId(wifeNode.getValue()) : null;

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

	private static void attachRelationship(FLEFRecord parent, FLEFModel model, GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, GEDCOMNode famcNode, String targetTag, String targetXrefId, StringBuilder sb){
		FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(RelationshipHandler.ID_PREFIX), RelationshipHandler.TYPE)
			// subject: child
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
			)
			// target
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue(targetTag, targetXrefId))
			)
			.addChild(FLEFRecord.createChildWithTagAndValue("type", (node.getTag().equalsIgnoreCase("ADOP")? "adoptive_child": "biological_child")));
		relationship.addChild(AuditBuilder.build(node));

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for (GEDCOMNode noteNode : findChildren(famcNode, "NOTE")) {
			attachNote(relationship,
				noteNode, noteRawMap);
		}

		FLEFRecord note1 = FLEFRecord.createChildWithTag("note")
			.addChild(FLEFRecord.createChildWithTagAndValue("text", sb.toString().trim()))
			.addChild(AuditBuilder.build(node));
		relationship.addChild(note1);

		FLEFRecord note2 = FLEFRecord.createChildWithTag("note")
			.addChild(FLEFRecord.createChildWithTagAndValue("text", "TO BE REVISED: the relationship should be with a biological father and a biological mother instead?"))
			.addChild(AuditBuilder.build(node));
		relationship.addChild(note2);

		// check for duplicates before adding
		Deduplicator.getDeduplicatedRecordId(model, relationship);
	}


	public static void attachSpouseToFamily(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap){
		for (GEDCOMNode famsNode : findChildren(node, "FAMS")) {
			if (famsNode.getValue() != null) {
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
				for (GEDCOMNode noteNode : findChildren(famsNode, "NOTE")) {
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
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap, Map<String, GEDCOMNode> objeRawMap, List<GEDCOMNode> roots, String groupId, String husbandId, String wifeId){
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

		attachEventOrFactDetail(parent, groupAttribute, model, node, noteRawMap, sourRawMap, objeRawMap, roots);

		// Link participants
		if(husbandId != null){
			createEventParticipation(groupId, husbandId, husbAge, "individual", "spouse", model, node);
		}
		if(wifeId != null){
			createEventParticipation(groupId, wifeId, wifeAge, "individual", "spouse", model, node);
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
			eventParticipation.addChild(FLEFRecord.createChildWithTagAndValue("role", (mappedRole != null? role: mappedRole)));
		}

		if(entityAge != null){
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
					FLEFRecord valuePoint = FLEFRecord.createChildWithTag("point");
					FLEFRecord valuePointFullDate = FLEFRecord.createChildWithTag("full_date");
					valuePointFullDate.addChild(FLEFRecord.createChildWithTagAndValue("value", dateInfo.getValue()));
					valuePointFullDate.addChild(FLEFRecord.createChildWithTagAndValue("calendar", getCalendarForDate(dateInfo.getValue())));
					valuePoint.addChild(valuePointFullDate);

					if(dateInfo.isApproximate()){
						FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
						String basis = switch(dateInfo.getQualifier()){
							case "ABT" -> "stated";
							case "CAL" -> "calculated";
							case "EST" -> "conventional";
							default -> "unspecified";
						};
						approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", basis));
						valuePointFullDate.addChild(approx);
					}
					dateValue.addChild(valuePoint);
				}
				case BOUNDED -> {
					FLEFRecord boundedRec = FLEFRecord.createChildWithTag("bounded");
					if(dateInfo.getNotBefore() != null){
						FLEFRecord nb = buildQualifiedDate(dateInfo.getNotBefore());
						FLEFRecord nbNode = FLEFRecord.createChildWithTag("not_before");
						nbNode.addChild(nb);
						if(dateInfo.isApproximate()){
							FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
							approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", "stated"));
							nbNode.addChild(approx);
						}
						boundedRec.addChild(nbNode);
					}
					if(dateInfo.getNotAfter() != null){
						FLEFRecord na = buildQualifiedDate(dateInfo.getNotAfter());
						FLEFRecord naNode = FLEFRecord.createChildWithTag("not_after");
						naNode.addChild(na);
						if(dateInfo.isApproximate()){
							FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
							approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", "stated"));
							naNode.addChild(approx);
						}
						boundedRec.addChild(naNode);
					}
					dateValue.addChild(boundedRec);
				}
				case SPANNING -> {
					FLEFRecord spanningRec = FLEFRecord.createChildWithTag("spanning");
					if(dateInfo.getFrom() != null){
						FLEFRecord from = buildQualifiedDate(dateInfo.getFrom());
						FLEFRecord fromNode = FLEFRecord.createChildWithTag("from");
						fromNode.addChild(from);
						if(dateInfo.isApproximate()){
							FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
							approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", "stated"));
							fromNode.addChild(approx);
						}
						spanningRec.addChild(fromNode);
					}
					if(dateInfo.getTo() != null){
						FLEFRecord to = buildQualifiedDate(dateInfo.getTo());
						FLEFRecord toNode = FLEFRecord.createChildWithTag("to");
						toNode.addChild(to);
						if(dateInfo.isApproximate()){
							FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
							approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", "stated"));
							toNode.addChild(approx);
						}
						spanningRec.addChild(toNode);
					}
					dateValue.addChild(spanningRec);
				}
			}
			parent.addChild(FLEFRecord.createChildWithTag(tag)
				.addChild(dateValue)
			);
		}
	}

	/**
	 * Builds a QualifiedDate structure from an ISO date string.
	 */
	private static FLEFRecord buildQualifiedDate(String isoDate){
		// Ensure we have a non‑empty date string; fallback to a default if missing
		if(StringUtils.isBlank(isoDate)){
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
		if(isoDate == null){
			return "gregorian";
		}
		int year;
		try{
			if(isoDate.contains("-")){
				year = Integer.parseInt(isoDate.split("-")[0]);
			}
			else{
				year = Integer.parseInt(isoDate);
			}
		}
		catch(NumberFormatException e){
			return "gregorian";
		}
		return year < 1582 ? "julian" : "gregorian";
	}


	/**
	 * Parses a GEDCOM PLAC node and returns a PlaceCitation.
	 * It also creates/updates the underlying PlaceRecord with subfields.
	 */
	public static void attachPlaceCitation(FLEFRecord parent, FLEFModel model,
			GEDCOMNode placNode, GEDCOMNode addrNode, Map<String, GEDCOMNode> noteRawMap){
		if(placNode == null){
			return;
		}

		String placeName = placNode.getValue();
		if(StringUtils.isBlank(placeName)){
			return;
		}

		// TODO
//		// ---- FONE (phonetic variation) ----
//		for (GEDCOMNode foneNode : findChildren(placNode, "FONE")) {
//			String phonetic = extractFullText(foneNode);
//			if (StringUtils.isNotEmpty(phonetic)) {
//				// Store as a variant note
//				String system = "IPA"; // default
//				GEDCOMNode typeNode = findFirstChild(foneNode, "TYPE");
//				if (typeNode != null && typeNode.getValue() != null) {
//					system = typeNode.getValue();
//				}
//				String text = "Phonetic (" + system + "): " + phonetic;
//				FLEFRecord note = createNoteStruct(text, foneNode);
//				if (note != null) place.addChild(note);
//			}
//		}

		// TODO
//		// ---- ROMN (romanized variation) ----
//		for (GEDCOMNode romnNode : findChildren(placNode, "ROMN")) {
//			String romanized = extractFullText(romnNode);
//			if (StringUtils.isNotEmpty(romanized)) {
//				String system = "scientific"; // default
//				GEDCOMNode typeNode = findFirstChild(romnNode, "TYPE");
//				if (typeNode != null && typeNode.getValue() != null) {
//					system = typeNode.getValue();
//				}
//				String text = "Romanized (" + system + "): " + romanized;
//				FLEFRecord note = createNoteStruct(text, romnNode);
//				if (note != null) place.addChild(note);
//			}
//		}


		final StringBuilder fullAddr = new StringBuilder(addrNode != null && addrNode.getValue() != null ? extractFullText(addrNode) : "");
		for(String subTag : List.of("ADR1", "ADR2", "ADR3", "CITY", "STAE", "POST", "CTRY")){
			GEDCOMNode sub = findFirstChild(addrNode, subTag);
			if(sub != null && sub.getValue() != null){
				if(!fullAddr.isEmpty()){
					fullAddr.append("\n");
				}
				fullAddr.append(sub.getValue());
			}
		}


		FLEFRecord place = FLEFRecord.createMainRecord(IDGenerator.nextId(PlaceHandler.ID_PREFIX), PlaceHandler.TYPE)
			.addChild(FLEFRecord.createChildWithTag("name")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", placeName))
			);
		if(!fullAddr.isEmpty()){
			place.addChild(FLEFRecord.createChildWithTag("name")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", fullAddr.toString()))
			);
		}
		place.addChild(AuditBuilder.build(placNode));

		// ---- MAP (coordinates) ----
		GEDCOMNode mapNode = findFirstChild(placNode, "MAP");
		if (mapNode != null) {
			GEDCOMNode latiNode = findFirstChild(mapNode, "LATI");
			GEDCOMNode longNode = findFirstChild(mapNode, "LONG");
			if (latiNode != null && longNode != null && latiNode.getValue() != null && longNode.getValue() != null) {
				FLEFRecord mapRecord = FLEFRecord.createChildWithTag("map")
					.addChild(FLEFRecord.createChildWithTagAndValue("coordinates", latiNode.getValue() + " " + longNode.getValue()));
				place.addChild(mapRecord);
			}
		}

		// ---- NOTE (notes under PLAC) ----
		for (GEDCOMNode noteNode : findChildren(placNode, "NOTE")) {
			attachNote(place,
				noteNode, noteRawMap);
		}


		// check for duplicates before adding
		String placeRecordId = Deduplicator.getDeduplicatedRecordId(model, place);


		FLEFRecord placeCitation = FLEFRecord.createChildWithTag("place");
		placeCitation.setValue(placeRecordId);
		parent.addChild(FLEFRecord.createChildWithTag("place")
			.addChild(placeCitation)
		);
	}


	public static void attachRestriction(FLEFRecord parent,
			GEDCOMNode node){
		GEDCOMNode resnNode = findFirstChild(node, "RESN");
		if (resnNode != null && resnNode.getValue() != null) {
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

		final StringBuilder fullAddr = new StringBuilder(addrNode.getValue() != null ? extractFullText(addrNode) : "");
		for(String subTag : List.of("ADR1", "ADR2", "ADR3", "CITY", "STAE", "POST", "CTRY")){
			GEDCOMNode sub = findFirstChild(addrNode, subTag);
			if(sub != null && sub.getValue() != null){
				if(!fullAddr.isEmpty()){
					fullAddr.append("\n");
				}
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

}
