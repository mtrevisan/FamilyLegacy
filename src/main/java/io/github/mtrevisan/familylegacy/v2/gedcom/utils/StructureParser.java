package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Parses common GEDCOM structures and converts them to FLEF records.
 * <p>
 * All FLEF field tags are correctly set according to the FLEF grammar.
 * Notes are handled as inline NoteStructure (not global NoteRecord references)
 * and always include a required audit.
 */
public class StructureParser{

	private final PlaceCache placeCache;

	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------

	public StructureParser(PlaceCache placeCache){
		this.placeCache = placeCache;
	}

	// ------------------------------------------------------------------------
	// Note parsing – inline NoteStructure with audit
	// ------------------------------------------------------------------------

	/**
	 * Creates an inline NoteStructure with the given text and an audit.
	 * This is the central method for creating notes in all converters.
	 *
	 * @param text       the note content
	 * @param sourNode the GEDCOM node from which the note originates (used for audit date)
	 * @return a FLEF record with the tag "NOTE" and a value + audit child, or {@code null} if the text is blank
	 */
	public FLEFRecord createNoteStruct(String text, GEDCOMNode sourNode){
		if(StringUtils.isBlank(text)){
			return null;
		}
		FLEFRecord note = FLEFRecord.createChildWithTag("note")
			.addChild(FLEFRecord.createChildWithTagAndValue("text", text.trim()))
			.addChild(AuditBuilder.build(sourNode));
		return note;
	}

	/**
	 * Parses a GEDCOM note node into an inline NoteStructure, handling CONC and CONT tags.
	 */
	public FLEFRecord parseNoteStruct(GEDCOMNode noteNode){
		if(noteNode == null){
			return null;
		}
		String text = GEDCOMHelper.extractFullText(noteNode);
		if(StringUtils.isBlank(text)){
			return null;
		}
		return createNoteStruct(text, noteNode);
	}

	// ------------------------------------------------------------------------
	// Date parsing
	// ------------------------------------------------------------------------

	public FLEFRecord parseDateStructure(GEDCOMNode dateNode){
		if(dateNode == null){
			return null;
		}
		String gedcomDate = dateNode.getValue();
		if(StringUtils.isBlank(gedcomDate)){
			return null;
		}
		if(gedcomDate.equalsIgnoreCase("N")){
			return null;
		}
		for(GEDCOMNode child : dateNode.getChildren()){
			if(child.getTag().equalsIgnoreCase("date")){
				gedcomDate = child.getValue();
				break;
			}
		}
//		if(gedcomDate.equalsIgnoreCase("Y") || gedcomDate.equalsIgnoreCase("N")){
//			return null;
//		}

		DateInfo info = GEDCOMDateParser.parse(gedcomDate);
		if(info == null){
			return null;
		}

		FLEFRecord dateStruct = FLEFRecord.createChildWithTag("date");
		FLEFRecord valueRec = FLEFRecord.createChildWithTag("value");

		switch(info.getType()){
			case POINT -> {
				FLEFRecord pointRec = FLEFRecord.createChildWithTag("point");
				FLEFRecord fullDateRec = FLEFRecord.createChildWithTag("full_date");
				fullDateRec.addChild(FLEFRecord.createChildWithTagAndValue("value", info.getValue()));
				fullDateRec.addChild(FLEFRecord.createChildWithTagAndValue("calendar", getCalendarForDate(info.getValue())));
				pointRec.addChild(fullDateRec);

				if(info.isApproximate()){
					FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
					String basis = switch(info.getQualifier()){
						case "ABT" -> "stated";
						case "CAL" -> "calculated";
						case "EST" -> "conventional";
						default -> "unspecified";
					};
					approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", basis));
					fullDateRec.addChild(approx);
				}
				valueRec.addChild(pointRec);
			}
			case BOUNDED -> {
				FLEFRecord boundedRec = FLEFRecord.createChildWithTag("bounded");
				if(info.getNotBefore() != null){
					FLEFRecord nb = buildQualifiedDate(info.getNotBefore());
					FLEFRecord nbNode = FLEFRecord.createChildWithTag("not_before");
					nbNode.addChild(nb);
					if(info.isApproximate()){
						FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
						approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", "stated"));
						nbNode.addChild(approx);
					}
					boundedRec.addChild(nbNode);
				}
				if(info.getNotAfter() != null){
					FLEFRecord na = buildQualifiedDate(info.getNotAfter());
					FLEFRecord naNode = FLEFRecord.createChildWithTag("not_after");
					naNode.addChild(na);
					if(info.isApproximate()){
						FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
						approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", "stated"));
						naNode.addChild(approx);
					}
					boundedRec.addChild(naNode);
				}
				valueRec.addChild(boundedRec);
			}
			case SPANNING -> {
				FLEFRecord spanningRec = FLEFRecord.createChildWithTag("spanning");
				if(info.getFrom() != null){
					FLEFRecord from = buildQualifiedDate(info.getFrom());
					FLEFRecord fromNode = FLEFRecord.createChildWithTag("from");
					fromNode.addChild(from);
					if(info.isApproximate()){
						FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
						approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", "stated"));
						fromNode.addChild(approx);
					}
					spanningRec.addChild(fromNode);
				}
				if(info.getTo() != null){
					FLEFRecord to = buildQualifiedDate(info.getTo());
					FLEFRecord toNode = FLEFRecord.createChildWithTag("to");
					toNode.addChild(to);
					if(info.isApproximate()){
						FLEFRecord approx = FLEFRecord.createChildWithTag("approximate");
						approx.addChild(FLEFRecord.createChildWithTagAndValue("basis", "stated"));
						toNode.addChild(approx);
					}
					spanningRec.addChild(toNode);
				}
				valueRec.addChild(spanningRec);
			}
		}

		dateStruct.addChild(valueRec);
		return dateStruct;
	}

	/**
	 * Builds a QualifiedDate structure from an ISO date string.
	 */
	private FLEFRecord buildQualifiedDate(String isoDate){
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
	private String getCalendarForDate(String isoDate){
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

	// ------------------------------------------------------------------------
	// PlaceCitation
	// ------------------------------------------------------------------------

	/**
	 * Parses a GEDCOM PLAC node and returns a PlaceCitation.
	 * It also creates/updates the underlying PlaceRecord with subfields.
	 */
	public FLEFRecord parsePlaceCitation(GEDCOMNode placNode){
		if(placNode == null){
			return null;
		}
		String placeName = placNode.getValue();
		if(StringUtils.isBlank(placeName)){
			return null;
		}

		FLEFRecord place = placeCache.getOrCreatePlace(placNode);
		FLEFRecord placeCitation = FLEFRecord.createChildWithTag("place");
		FLEFRecord placeRef = FLEFRecord.createChildWithTag("place");
		placeRef.setValue(place.getId());
		placeCitation.addChild(placeRef);

//		// FORM -> note
//		GEDCOMNode formNode = GEDCOMHelper.findFirstChild(placNode, "FORM");
//		if (formNode != null && formNode.getValue() != null) {
//			FLEFRecord note = createNoteStruct("Hierarchy: " + formNode.getValue(), formNode);
//			if (note != null) place.addChild(note);
//		}

		// ---- FONE (phonetic variation) ----
		for (GEDCOMNode foneNode : GEDCOMHelper.findChildren(placNode, "FONE")) {
			String phonetic = GEDCOMHelper.extractFullText(foneNode);
			if (StringUtils.isNotEmpty(phonetic)) {
				// Store as a variant note
				String system = "IPA"; // default
				GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(foneNode, "TYPE");
				if (typeNode != null && typeNode.getValue() != null) {
					system = typeNode.getValue();
				}
				String text = "Phonetic (" + system + "): " + phonetic;
				FLEFRecord note = createNoteStruct(text, foneNode);
				if (note != null) place.addChild(note);
			}
		}

		// ---- ROMN (romanized variation) ----
		for (GEDCOMNode romnNode : GEDCOMHelper.findChildren(placNode, "ROMN")) {
			String romanized = GEDCOMHelper.extractFullText(romnNode);
			if (StringUtils.isNotEmpty(romanized)) {
				String system = "scientific"; // default
				GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(romnNode, "TYPE");
				if (typeNode != null && typeNode.getValue() != null) {
					system = typeNode.getValue();
				}
				String text = "Romanized (" + system + "): " + romanized;
				FLEFRecord note = createNoteStruct(text, romnNode);
				if (note != null) place.addChild(note);
			}
		}

		// ---- MAP (coordinates) ----
		GEDCOMNode mapNode = GEDCOMHelper.findFirstChild(placNode, "MAP");
		if (mapNode != null) {
			GEDCOMNode latiNode = GEDCOMHelper.findFirstChild(mapNode, "LATI");
			GEDCOMNode longNode = GEDCOMHelper.findFirstChild(mapNode, "LONG");
			if (latiNode != null && longNode != null &&
				latiNode.getValue() != null && longNode.getValue() != null) {
				FLEFRecord mapRecord = FLEFRecord.createChildWithTag("map");
				mapRecord.addChild(FLEFRecord.createChildWithTagAndValue("coordinates",
					latiNode.getValue() + " " + longNode.getValue()));
				// Evidence can be added if present (optional)
				place.addChild(mapRecord);
			}
		}

		// ---- NOTE (notes under PLAC) ----
		for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(placNode, "NOTE")) {
			FLEFRecord noteStruct = parseNoteStruct(noteNode);
			if (noteStruct != null) place.addChild(noteStruct);
		}

		// original_text is omitted because the name itself is the same as the original
		return placeCitation;
	}

	// ------------------------------------------------------------------------
	// SourceCitation
	// ------------------------------------------------------------------------

	public FLEFRecord parseSourceCitation(GEDCOMNode sourNode, FLEFModel model, Map<String, GEDCOMNode> noteRawMap){
		return parseSourceCitation(sourNode, model, null, null, noteRawMap);
	}

	public FLEFRecord parseSourceCitation(GEDCOMNode sourNode, FLEFModel model, String currentXref, String currentTag, Map<String, GEDCOMNode> noteRawMap){
		if(sourNode == null){
			return null;
		}

		FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
		String rawSourceVal = sourNode.getValue();

		if(StringUtils.isNotEmpty(rawSourceVal)){
			if(rawSourceVal.startsWith("@") && rawSourceVal.endsWith("@")){
				// 1. Puntatore ad un record sorgente top-level (@S1@)
				String cleanId = GEDCOMHelper.cleanId(rawSourceVal);
				FLEFRecord sourceRef = FLEFRecord.createChildWithTag("source");
				sourceRef.setValue(cleanId);
				sourceCitation.addChild(sourceRef);
			}
			else{
				// 2. SOUR incorporato (free-form source description)
				String inlineDescription = GEDCOMHelper.extractFullText(sourNode);
				if(StringUtils.isNotEmpty(inlineDescription) && model != null){
					// Crea un nuovo SourceRecord dinamico da registrare nel modello
					String newSourceId = IDGenerator.nextId(SourceHandler.ID_PREFIX);
					FLEFRecord inlineSource = FLEFRecord.createMainRecord(newSourceId, SourceHandler.TYPE);

					// Title / Description
					FLEFRecord titleRec = FLEFRecord.createChildWithTag("title")
						.addChild(FLEFRecord.createChildWithTagAndValue("value", inlineDescription));
					inlineSource.addChild(titleRec);

					// Verbatim text sotto l'inline SOUR (1 SOUR / 2 TEXT)
					GEDCOMNode textNode = GEDCOMHelper.findFirstChild(sourNode, "TEXT");
					if(textNode != null){
						String verbatimText = GEDCOMHelper.extractFullText(textNode);
						if(StringUtils.isNotEmpty(verbatimText)){
							FLEFRecord note = createNoteStruct("Verbatim text: " + verbatimText, textNode);
							if(note != null){
								inlineSource.addChild(note);
							}
						}
					}

					// Note sotto l'inline SOUR
					for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(sourNode, "NOTE")){
						FLEFRecord noteStruct = parseNoteStruct(noteNode);
						if(noteStruct != null){
							inlineSource.addChild(noteStruct);
						}
					}

					// Audit per la sorgente generata
					inlineSource.addChild(createAudit(sourNode));

					// Aggiunta al modello e collegamento della citazione
					model.addRecord(inlineSource);

					FLEFRecord sourceRef = FLEFRecord.createChildWithTag("source");
					sourceRef.setValue(newSourceId);
					sourceCitation.addChild(sourceRef);
				}
			}
		}

		// PAGE -> location
		GEDCOMNode pageNode = GEDCOMHelper.findFirstChild(sourNode, "PAGE");
		if(pageNode != null && pageNode.getValue() != null){
			sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("location", pageNode.getValue()));
		}

		// ---- REPO (source repository citation) ----
		for (GEDCOMNode repoNode : GEDCOMHelper.findChildren(sourNode, "REPO")) {
			if (repoNode.getValue() != null) {
				FLEFRecord repoCitation = FLEFRecord.createChildWithTag("repository");
				FLEFRecord repoRef = FLEFRecord.createChildWithTag("repository");
				repoRef.setValue(GEDCOMHelper.cleanId(repoNode.getValue()));
				repoCitation.addChild(repoRef);
				// CALN -> location
				GEDCOMNode calnNode = GEDCOMHelper.findFirstChild(repoNode, "CALN");
				if (calnNode != null && calnNode.getValue() != null) {
					repoCitation.addChild(FLEFRecord.createChildWithTagAndValue("location", calnNode.getValue()));
				}
				// NOTE
				for (GEDCOMNode noteNode : GEDCOMHelper.findChildren(repoNode, "NOTE")) {
					FLEFRecord noteStruct = parseNoteStruct(noteNode);
					if (noteStruct != null) repoCitation.addChild(noteStruct);
				}
				sourceCitation.addChild(repoCitation);
			}
		}

		// EVEN (+ ROLE) -> EventRecord + EventParticipationRecord
		GEDCOMNode evenNode = GEDCOMHelper.findFirstChild(sourNode, "EVEN");
		if(evenNode != null && model != null){
			String eventType = evenNode.getValue();
			if(StringUtils.isNotEmpty(eventType)){
				FLEFRecord eventRecord = FLEFRecord.createChildWithTag("event");
				String eventId = IDGenerator.nextId(EventHandler.ID_PREFIX);
				eventRecord.setId(eventId);

				// 1. Il tipo per eventi custom/descrittivi da SOUR è "other"
				eventRecord.addChild(FLEFRecord.createChildWithTagAndValue("type", "other"));

				// 2. Preserva il testo originale ("Event type cited in source") nella description
				eventRecord.addChild(FLEFRecord.createChildWithTagAndValue("description", eventType.trim()));

				eventRecord.addChild(createAudit(evenNode));
				model.addRecord(eventRecord);

				GEDCOMNode roleNode = GEDCOMHelper.findFirstChild(evenNode, "ROLE");
				String roleValue = (roleNode != null) ? roleNode.getValue() : null;

				if(StringUtils.isNotEmpty(roleValue) && StringUtils.isNotEmpty(currentXref) && StringUtils.isNotEmpty(currentTag)){
					FLEFRecord participationRecord = FLEFRecord.createMainRecord(IDGenerator.nextId(EventParticipationHandler.ID_PREFIX), EventParticipationHandler.TYPE);

					FLEFRecord participant = FLEFRecord.createChildWithTag("participant");
					FLEFRecord indRef = FLEFRecord.createChildWithTag(currentTag);
					indRef.setValue(currentXref);
					participant.addChild(indRef);
					participationRecord.addChild(participant);

					FLEFRecord evtRef = FLEFRecord.createChildWithTag("event");
					evtRef.setValue(eventId);
					participationRecord.addChild(evtRef);

					// Preserva il ruolo mantenendo la stringa pulita
					participationRecord.addChild(FLEFRecord.createChildWithTagAndValue("role", roleValue.trim().toLowerCase(Locale.ROOT)));
					participationRecord.addChild(createAudit(evenNode));
					model.addRecord(participationRecord);
				}
			}
		}

		// DATA -> DATE / TEXT
		GEDCOMNode dataNode = GEDCOMHelper.findFirstChild(sourNode, "DATA");
		if(dataNode != null){
			GEDCOMNode entryDateNode = GEDCOMHelper.findFirstChild(dataNode, "DATE");
			if(entryDateNode != null){
				FLEFRecord dateStruct = parseDateStructure(entryDateNode);
				if(dateStruct != null){
					sourceCitation.addChild(dateStruct);
				}
			}
			for(GEDCOMNode textNode : GEDCOMHelper.findChildren(dataNode, "TEXT")){
				String verbatimText = GEDCOMHelper.extractFullText(textNode);
				if(StringUtils.isNotEmpty(verbatimText)){
					FLEFRecord note = createNoteStruct("Verbatim text: " + verbatimText, textNode);
					if(note != null){
						sourceCitation.addChild(note);
					}
				}
			}
		}

		// QUAY -> EvidenceQualifiers
		GEDCOMNode quayNode = GEDCOMHelper.findFirstChild(sourNode, "QUAY");
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
			sourceCitation.addChild(evidence);
		}

		// Sub-notes (NOTE) collegati alla citazione
		for(GEDCOMNode noteNode : GEDCOMHelper.findChildren(sourNode, "NOTE")){
			GEDCOMHelper.attachNote(sourceCitation,
				noteNode, noteRawMap);
		}

		// Multimedia (OBJE) collegati alla citazione
		for(GEDCOMNode objNode : GEDCOMHelper.findChildren(sourNode, "OBJE")){
			FLEFRecord multimediaLink = parseMultimediaLink(objNode, model);
			if(multimediaLink != null && !multimediaLink.getChildren().isEmpty()){
				sourceCitation.addChild(multimediaLink);
			}
		}

		return sourceCitation;
	}

	// ------------------------------------------------------------------------
	// MultimediaLink (Xref<DocumentRecord>)
	// ------------------------------------------------------------------------

	public FLEFRecord parseMultimediaLink(GEDCOMNode objNode, FLEFModel model){
		if(objNode == null){
			return null;
		}
		FLEFRecord multimediaLink = FLEFRecord.createChildWithTag("multimedia_link");
		String objXref = GEDCOMHelper.cleanId(objNode.getValue());
		if(objXref != null){
			multimediaLink.addChild(FLEFRecord.createChildWithTagAndValue("document", objXref));
		}
		return multimediaLink;
	}

	// ------------------------------------------------------------------------
	// Address -> ContactStructure
	// ------------------------------------------------------------------------

	public FLEFRecord parseAddressToContact(GEDCOMNode addrNode, GEDCOMNode parentNode){
		if(addrNode == null){
			return null;
		}

		final StringBuilder fullAddr = new StringBuilder(addrNode.getValue() != null ? GEDCOMHelper.extractFullText(addrNode) : "");
		for(String subTag : List.of("ADR1", "ADR2", "ADR3", "CITY", "STAE", "POST", "CTRY")){
			GEDCOMNode sub = GEDCOMHelper.findFirstChild(addrNode, subTag);
			if(sub != null && sub.getValue() != null){
				if(!fullAddr.isEmpty()){
					fullAddr.append("\n");
				}
				fullAddr.append(sub.getValue());
			}
		}
		if(fullAddr.isEmpty()){
			return null;
		}

		FLEFRecord contact = FLEFRecord.createChildWithTag("contact");
		contact.addChild(FLEFRecord.createChildWithTagAndValue("value", fullAddr.toString()));

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
				FLEFRecord note = createNoteStruct(text, child);
				if(note != null){
					contact.addChild(note);
				}
			}
		}

		// Audit for the contact (required)
		FLEFRecord audit = (parentNode != null)
			? AuditBuilder.build(parentNode)
			: AuditBuilder.build(null);
		contact.addChild(audit);
		return contact;
	}

	// ------------------------------------------------------------------------
	// Audit builder (wrapper)
	// ------------------------------------------------------------------------

	public FLEFRecord createAudit(GEDCOMNode node){
		return AuditBuilder.build(node);
	}

}
