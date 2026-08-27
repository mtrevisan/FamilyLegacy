package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.AuditBuilder;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.DateInfo;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMDateParser;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.GEDCOMMapper;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDGenerator;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.IDNormalizer;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class GEDCOMHelper{

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
		.ofPattern("uuuu-MM-dd")
		.withResolverStyle(ResolverStyle.STRICT);


	private GEDCOMHelper(){}


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
			.addChild(FLEFRecord.createChildWithTagAndValue("value", fullText.trim()))
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
		GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(nameNode, "TYPE");
		GEDCOMHelper.transferValue(name, "type", typeNode);

		// Parse inline value (e.g., "Joseph Tag /Torture/") into full text
		String given = "";
		String family = "";
		String raw = GEDCOMHelper.extractFullText(nameNode);
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
//		for(GEDCOMNode nameFoneNode : GEDCOMHelper.findChildren(nameNode, "FONE")){
//			GEDCOMNode nameFoneType = GEDCOMHelper.findFirstChild(nameFoneNode, "TYPE");
//			personalNamePieces(namePhoneticVariations, nameFoneNode, null, null);
//
//			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
//			FLEFRecord phonetic = FLEFRecord.createChildWithTag("phonetic");
//			GEDCOMHelper.transferValue(phonetic, "system", nameFoneType);
//			GEDCOMHelper.transferValue(phonetic, "value", nameFoneNode);
//			if(!phonetic.isEmpty())
//				variant.addChild(phonetic);
//			if(!variant.isEmpty())
//				namePhoneticVariations.addChild(variant);
//		}

		// TODO
//		// Romanized variations (ROMN)
//		for(GEDCOMNode romn : GEDCOMHelper.findChildren(nameNode, "ROMN")){
//			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
//			FLEFRecord transcription = FLEFRecord.createChildWithTag("transcription");
//			GEDCOMNode romnType = GEDCOMHelper.findFirstChild(romn, "TYPE");
//			String system = (romnType != null && romnType.getValue() != null) ? romnType.getValue() : "scientific";
//			transcription.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
//			transcription.addChild(FLEFRecord.createChildWithTagAndValue("value", romn.getValue()));
//			variant.addChild(transcription);
//			namePhoneticVariations.addChild(variant);
//		}

		for (GEDCOMNode sourNode : GEDCOMHelper.findChildren(nameNode, "SOUR")) {
			GEDCOMHelper.attachSource(parent, model,
				sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(nameNode, "NOTE")) {
			GEDCOMHelper.attachNote(parent,
				noteNode, noteRawMap);
		}

		if(!name.isEmpty())
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
		GEDCOMNode childNode = GEDCOMHelper.findFirstChild(nameNode, gedcomTag);
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
				String roleValue = evenRoleNode.getValue();

				event = FLEFRecord.createMainRecord(IDGenerator.nextId(EventHandler.ID_PREFIX), "event")
					.addChild(FLEFRecord.createChildWithTagAndValue("type", GEDCOMMapper.mapEvent(evenValue, evenValue)))
					.addChild(AuditBuilder.build(node));
				model.addRecord(event);
				FLEFRecord eventParticipant = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationHandler.ID_PREFIX), "event_participation")
					.addChild(FLEFRecord.createChildWithTag("participant")
						.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
					)
					.addChild(FLEFRecord.createChildWithTagAndValue("event", event.getId()));
				if(StringUtils.isNotEmpty(roleValue))
					eventParticipant.addChild(FLEFRecord.createChildWithTagAndValue("role", GEDCOMMapper.mapRole(roleValue, roleValue)));
				eventParticipant.addChild(AuditBuilder.build(node));
				model.addRecord(eventParticipant);
			}
			GEDCOMNode dataNode = findFirstChild(node, "DATA");
			GEDCOMNode dataDateNode = findFirstChild(dataNode, "DATE");
			String dataTextNode = extractFullText(findFirstChild(dataNode, "TEXT"));
			for (GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE")) {
				attachMultimediaLink(parent, model,
					multimediaLinkNode, objeRawMap);
			}

			sourValue = cleanId(sourValue);
			FLEFRecord sourceParent = FLEFRecord.createMainRecord(sourValue, "source");
			GEDCOMHelper.transferValue(sourceParent, "date", dataDateNode);
			for (GEDCOMNode noteNode : findChildren(node, "NOTE")) {
				attachNote(sourceParent, noteNode, noteRawMap);
			}
			sourceParent.addChild(AuditBuilder.build(node));
			model.addRecord(sourceParent);

			FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
			sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("source", sourValue));
			GEDCOMHelper.transferValue(sourceCitation, "location", pageNode);
			if(StringUtils.isNotEmpty(dataTextNode)){
				sourceCitation.addChild(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", dataTextNode))
				);
			}
			attachQuay(sourceParent, node);

			if(event != null){
				event.addChild(sourceCitation);
			}

			parent.addChild(sourceCitation);
		}
		else{
			String textNode = extractFullText(findFirstChild(node, "TEXT"));
			for (GEDCOMNode multimediaLinkNode : findChildren(node, "OBJE")) {
				attachMultimediaLink(parent, model,
					multimediaLinkNode, objeRawMap);
			}

			FLEFRecord sourceParent = FLEFRecord.createMainRecord(IDGenerator.nextId(SourceHandler.ID_PREFIX), "source");
			for (GEDCOMNode noteNode : findChildren(node, "NOTE")) {
				attachNote(sourceParent, noteNode, noteRawMap);
			}
			sourceParent.addChild(AuditBuilder.build(node));
			model.addRecord(sourceParent);

			FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
			sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("source", sourceParent.getId()));
			if(StringUtils.isNotEmpty(textNode)){
				sourceCitation.addChild(FLEFRecord.createChildWithTag("extract")
					.addChild(FLEFRecord.createChildWithTagAndValue("text", textNode))
				);
			}
			attachQuay(sourceParent, node);

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
	public static void attachMultimediaLink(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> objeRawMap){
		// Extract Xref – either from getXrefId() or from the value if it's a reference
		String objeXrefId = node.getXrefId();
		if(objeXrefId == null && StringUtils.isNotEmpty(node.getValue())){
			objeXrefId = node.getValue().trim();
		}
		objeXrefId = cleanId(objeXrefId);

		if(objeXrefId != null){
			// TODO is there something to fetch from actual raw obje?
		}
		else{
			List<GEDCOMNode> fileNodes = GEDCOMHelper.findChildren(node, "FILE");
			for(GEDCOMNode fileNode : fileNodes){
				GEDCOMNode fileFormNode = GEDCOMHelper.findFirstChild(fileNode, "FORM");
				String fileFormNodeValue = (fileFormNode != null? fileFormNode.getValue(): null);
				GEDCOMNode fileFormMediNode = GEDCOMHelper.findFirstChild(fileFormNode, "MEDI");
				String fileFormMediNodeValue = (fileFormMediNode != null? fileFormMediNode.getValue(): null);
				GEDCOMNode titlNode = GEDCOMHelper.findFirstChild(node, "TITL");

				FLEFRecord document = FLEFRecord.createMainRecord(IDGenerator.nextId(DocumentHandler.ID_PREFIX), "document");
				transferValue(document, "file", fileNode);
				transferValue(document, "description", titlNode);
				if(StringUtils.isNotEmpty(fileFormNodeValue)){
					FLEFRecord note = FLEFRecord.createChildWithTag("note")
						.addChild(FLEFRecord.createChildWithTagAndValue("value", "Format: " + fileFormNodeValue));
					document.addChild(note);
				}
				if(StringUtils.isNotEmpty(fileFormMediNodeValue)){
					FLEFRecord note = FLEFRecord.createChildWithTag("note")
						.addChild(FLEFRecord.createChildWithTagAndValue("value", "Media type: " + fileFormMediNodeValue));
					document.addChild(note);
				}
				document.addChild(AuditBuilder.build(node));
				model.addRecord(document);

				objeXrefId = document.getId();
			}

			parent.addChild(FLEFRecord.createChildWithTagAndValue("document", objeXrefId));
		}
	}


	public static void attachIndividualEvent(FLEFRecord parent, FLEFModel model,
			GEDCOMNode node, Map<String, GEDCOMNode> noteRawMap, Map<String, GEDCOMNode> sourRawMap, Map<String, GEDCOMNode> objeRawMap){
		//TODO INDIVIDUAL_EVENT_DETAIL
		//TODO FAMC
		//TODO FAMC.ADOP


		// Determine the FLEF event type
		GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(node, "TYPE");
		String customType = (typeNode != null? typeNode.getValue().trim(): null);
		if(customType == null)
			customType = node.getTag();
		String type = GEDCOMMapper.mapEvent(customType, (customType != null? customType: "other"));

		// Date
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(node, "DATE");


		FLEFRecord event = FLEFRecord.createMainRecord(IDGenerator.nextId(EventHandler.ID_PREFIX), "event")
			.addChild(FLEFRecord.createChildWithTagAndValue("type", type));

		attachDate(event, getDateTime(dateNode));

		// Description (for generic EVEN) – ignore "Y" or "N"
		String eventValue = node.getValue();
		if(StringUtils.isNotEmpty(eventValue) && !eventValue.equalsIgnoreCase("Y")
				&& !eventValue.equalsIgnoreCase("N")){
			event.addChild(FLEFRecord.createChildWithTagAndValue("description", eventValue));
		}

		// Place
		GEDCOMNode placNode = GEDCOMHelper.findFirstChild(node, "PLAC");
		GEDCOMNode addrNode = GEDCOMHelper.findFirstChild(node, "ADDR");
		attachPlaceCitation(event, model,
			placNode, addrNode, noteRawMap);

		// Agency (AGNC)
		GEDCOMNode agncNode = GEDCOMHelper.findFirstChild(node, "AGNC");
		transferValue(event, "agency", agncNode);

		// Cause (CAUS)
		GEDCOMNode causNode = GEDCOMHelper.findFirstChild(node, "CAUS");
		if(causNode != null && causNode.getValue() != null){
			FLEFRecord cause = FLEFRecord.createChildWithTag("cause");
			transferValue(cause, "reason", causNode);
			event.addChild(cause);
		}

		// ---- Sources (SOUR) ----
		for (GEDCOMNode sourNode : GEDCOMHelper.findChildren(node, "SOUR")) {
			GEDCOMHelper.attachSource(event, model,
				sourNode, noteRawMap, sourRawMap, objeRawMap);
		}

		// ---- Notes (GEDCOM NOTE) – inline structs ----
		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(node, "NOTE")) {
			GEDCOMHelper.attachNote(event,
				noteNode, noteRawMap);
		}

		// Multimedia (OBJE)
		for (GEDCOMNode multimediaLinkNode : GEDCOMHelper.findChildren(node, "OBJE")) {
			GEDCOMHelper.attachMultimediaLink(event, model,
				multimediaLinkNode, objeRawMap);
		}

		// Age at event → inline note (with audit)
		GEDCOMNode ageNode = GEDCOMHelper.findFirstChild(node, "AGE");
		if(ageNode != null && ageNode.getValue() != null){
			FLEFRecord note = FLEFRecord.createChildWithTag("note")
				.addChild(FLEFRecord.createChildWithTagAndValue("value", "Age at event: " + ageNode.getValue()));
			event.addChild(note);
		}

		//TODO here
//		// ---- FAMC (family of origin) ----
//		for (GEDCOMNode famcNode : GEDCOMHelper.findChildren(node, "FAMC")) {
//			if (famcNode.getValue() != null) {
//				String text = "Family of origin: " + famcNode.getValue();
//				FLEFRecord note = createNoteStruct(text, famcNode);
//				if (note != null) eventRec.addChild(note);
//			}
//		}

		GEDCOMHelper.attachRestriction(event, node);

//		// ---- ADOP sub‑field (which parent adopted) ----
//		if ("ADOP".equals(gedcomTag)) {
//			GEDCOMNode adopNode = GEDCOMHelper.findFirstChild(node, "ADOP");
//			if (adopNode != null && adopNode.getValue() != null) {
//				String text = "Adopted by: " + adopNode.getValue();
//				FLEFRecord note = createNoteStruct(text, adopNode);
//				if (note != null) eventRec.addChild(note);
//			}
//		}



		event.addChild(AuditBuilder.build(node));
		model.addRecord(event);


		FLEFRecord eventParticipant = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationHandler.ID_PREFIX), "event_participation")
			.addChild(FLEFRecord.createChildWithTag("participant")
				.addChild(FLEFRecord.createChildWithTagAndValue("individual", parent.getId()))
			)
			.addChild(FLEFRecord.createChildWithTagAndValue("event", event.getId()))
			.addChild(AuditBuilder.build(node));
		model.addRecord(eventParticipant);
	}

	public static void attachDate(FLEFRecord parent, String date){
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
			parent.addChild(FLEFRecord.createChildWithTag("date")
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
//		for (GEDCOMNode foneNode : GEDCOMHelper.findChildren(placNode, "FONE")) {
//			String phonetic = GEDCOMHelper.extractFullText(foneNode);
//			if (StringUtils.isNotEmpty(phonetic)) {
//				// Store as a variant note
//				String system = "IPA"; // default
//				GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(foneNode, "TYPE");
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
//		for (GEDCOMNode romnNode : GEDCOMHelper.findChildren(placNode, "ROMN")) {
//			String romanized = GEDCOMHelper.extractFullText(romnNode);
//			if (StringUtils.isNotEmpty(romanized)) {
//				String system = "scientific"; // default
//				GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(romnNode, "TYPE");
//				if (typeNode != null && typeNode.getValue() != null) {
//					system = typeNode.getValue();
//				}
//				String text = "Romanized (" + system + "): " + romanized;
//				FLEFRecord note = createNoteStruct(text, romnNode);
//				if (note != null) place.addChild(note);
//			}
//		}


		final StringBuilder fullAddr = new StringBuilder(addrNode != null && addrNode.getValue() != null ? GEDCOMHelper.extractFullText(addrNode) : "");
		for(String subTag : List.of("ADR1", "ADR2", "ADR3", "CITY", "STAE", "POST", "CTRY")){
			GEDCOMNode sub = GEDCOMHelper.findFirstChild(addrNode, subTag);
			if(sub != null && sub.getValue() != null){
				if(!fullAddr.isEmpty()){
					fullAddr.append("\n");
				}
				fullAddr.append(sub.getValue());
			}
		}


		FLEFRecord placeRecord = FLEFRecord.createMainRecord(IDGenerator.nextId(PlaceHandler.ID_PREFIX), "place")
			.addChild(FLEFRecord.createChildWithTag("name")
				.addChild(FLEFRecord.createChildWithTag("text")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", placeName))
				)
			);
		if(!fullAddr.isEmpty()){
			placeRecord.addChild(FLEFRecord.createChildWithTag("name")
				.addChild(FLEFRecord.createChildWithTag("text")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", fullAddr.toString()))
				)
			);
		}
		placeRecord.addChild(AuditBuilder.build(placNode));

		FLEFRecord placeCitation = FLEFRecord.createChildWithTag("place");
		placeCitation.setValue(placeRecord.getId());

		// ---- MAP (coordinates) ----
		GEDCOMNode mapNode = GEDCOMHelper.findFirstChild(placNode, "MAP");
		if (mapNode != null) {
			GEDCOMNode latiNode = GEDCOMHelper.findFirstChild(mapNode, "LATI");
			GEDCOMNode longNode = GEDCOMHelper.findFirstChild(mapNode, "LONG");
			if (latiNode != null && longNode != null && latiNode.getValue() != null && longNode.getValue() != null) {
				FLEFRecord mapRecord = FLEFRecord.createChildWithTag("map")
					.addChild(FLEFRecord.createChildWithTagAndValue("coordinates", latiNode.getValue() + " " + longNode.getValue()));
				placeRecord.addChild(mapRecord);
			}
		}

		// ---- NOTE (notes under PLAC) ----
		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(placNode, "NOTE")) {
			GEDCOMHelper.attachNote(placeRecord,
				noteNode, noteRawMap);
		}

		model.addRecord(placeRecord);

		parent.addChild(FLEFRecord.createChildWithTag("place")
			.addChild(placeCitation)
		);
	}

	public static void attachRestriction(FLEFRecord parent,
			GEDCOMNode node){
		GEDCOMNode resnNode = GEDCOMHelper.findFirstChild(node, "RESN");
		if (resnNode != null && resnNode.getValue() != null) {
			String level = GEDCOMMapper.mapPrivacyLevel(resnNode.getValue());
			FLEFRecord privacy = FLEFRecord.createChildWithTag("privacy")
				.addChild(FLEFRecord.createChildWithTagAndValue("level", level));
			parent.addChild(privacy);
		}
	}

}
