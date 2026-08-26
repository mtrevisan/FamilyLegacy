package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
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
	private final AuditBuilder auditBuilder;

	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------

	public StructureParser(PlaceCache placeCache){
		this.placeCache = placeCache;
		this.auditBuilder = new AuditBuilder();
	}

	// ------------------------------------------------------------------------
	// Event parsing
	// ------------------------------------------------------------------------

	/**
	 * Parses a GEDCOM event node into an FLEF EventRecord.
	 * Adds the required audit structure.
	 */
	public FLEFRecord parseEvent(GEDCOMNode evtNode, String gedcomTag){
		if(evtNode == null){
			return null;
		}

		// Determine the FLEF event type
		String customType = getChildValue(evtNode, "TYPE");
		String flefType = GEDCOMMapper.mapEvent(gedcomTag, customType);

		FLEFRecord eventRec = FLEFRecord.createChildWithTag("event");
		eventRec.addChild(FLEFRecord.createChildWithTagAndValue("type", flefType));

		// Date
		GEDCOMNode dateNode = findFirstChild(evtNode, "DATE");
		if(dateNode != null){
			FLEFRecord dateStruct = parseDateStructure(dateNode);
			if(dateStruct != null){
				eventRec.addChild(dateStruct);
			}
		}

		// Place
		GEDCOMNode placNode = findFirstChild(evtNode, "PLAC");
		if(placNode != null){
			FLEFRecord placeCitation = parsePlaceCitation(placNode);
			if(placeCitation != null){
				eventRec.addChild(placeCitation);
			}
		}

		// Agency (AGNC)
		GEDCOMNode agncNode = findFirstChild(evtNode, "AGNC");
		if(agncNode != null && agncNode.getValue() != null){
			eventRec.addChild(FLEFRecord.createChildWithTagAndValue("agency", agncNode.getValue()));
		}

		// Cause (CAUS)
		GEDCOMNode causNode = findFirstChild(evtNode, "CAUS");
		if(causNode != null && causNode.getValue() != null){
			FLEFRecord cause = FLEFRecord.createChildWithTag("cause");
			cause.addChild(FLEFRecord.createChildWithTagAndValue("value", causNode.getValue()));
			eventRec.addChild(cause);
		}

		// Description (for generic EVEN) – ignore "Y" or "N"
		String eventValue = evtNode.getValue();
		if(StringUtils.isNotEmpty(eventValue)
			&& !eventValue.equalsIgnoreCase("Y")
			&& !eventValue.equalsIgnoreCase("N")){
			eventRec.addChild(FLEFRecord.createChildWithTagAndValue("description", eventValue));
		}

		// Sources, notes, multimedia
		addCommonSubstructures(evtNode, eventRec);

		// Age at event → inline note (with audit)
		GEDCOMNode ageNode = findFirstChild(evtNode, "AGE");
		if(ageNode != null && ageNode.getValue() != null){
			FLEFRecord note = createNoteStruct("Age at event: " + ageNode.getValue(), evtNode);
			if(note != null){
				eventRec.addChild(note);
			}
		}

		// Audit (required)
		eventRec.addChild(auditBuilder.build(evtNode));
		return eventRec;
	}

	// ------------------------------------------------------------------------
	// Attribute parsing (IndividualAttributeRecord)
	// ------------------------------------------------------------------------

	/**
	 * Parses a GEDCOM attribute node into an FLEF IndividualAttributeRecord.
	 * Adds the required audit structure.
	 */
	public FLEFRecord parseAttribute(GEDCOMNode attrNode, String gedcomTag){
		if(attrNode == null){
			return null;
		}

		String customType = getChildValue(attrNode, "TYPE");
		String flefType = GEDCOMMapper.mapAttribute(gedcomTag, customType);

		FLEFRecord attrRec = FLEFRecord.createChildWithTag("individual_attribute");
		attrRec.addChild(FLEFRecord.createChildWithTagAndValue("type", flefType));

		// Value – ignore "Y" or "N"
		String attrValue = attrNode.getValue();
		if(StringUtils.isNotEmpty(attrValue)
			&& !attrValue.equalsIgnoreCase("Y")
			&& !attrValue.equalsIgnoreCase("N")){
			attrRec.addChild(FLEFRecord.createChildWithTagAndValue("value", attrValue));
		}

		// Date (valid_from?)
		GEDCOMNode dateNode = findFirstChild(attrNode, "DATE");
		if(dateNode != null){
			FLEFRecord dateStruct = parseDateStructure(dateNode);
			if(dateStruct != null){
				attrRec.addChild(dateStruct);
			}
		}

		// Place
		GEDCOMNode placNode = findFirstChild(attrNode, "PLAC");
		if(placNode != null){
			FLEFRecord placeCitation = parsePlaceCitation(placNode);
			if(placeCitation != null){
				attrRec.addChild(placeCitation);
			}
		}

		// Sources, notes, multimedia
		addCommonSubstructures(attrNode, attrRec);

		// Audit (required)
		attrRec.addChild(auditBuilder.build(attrNode));
		return attrRec;
	}

	// ------------------------------------------------------------------------
	// Name parsing
	// ------------------------------------------------------------------------

	/**
	 * Parses a generic NameStructure (used in titles, repository names, etc.)
	 * The fieldTag is the FLEF tag to use (e.g., "title", "text").
	 */
	public FLEFRecord parseNameStructure(GEDCOMNode node, String fieldTag){
		if(node == null || node.getValue() == null){
			return null;
		}
		FLEFRecord nameRec = FLEFRecord.createChildWithTag(fieldTag);
		nameRec.addChild(FLEFRecord.createChildWithTagAndValue("value", node.getValue()));
		return nameRec;
	}

	/**
	 * Parses a PersonalNameStructure (for individuals) extracting all sub-tags
	 * (GIVN, SURN, NPFX, NSFX, SPFX, NICK), inline sources, and notes with CONC/CONT.
	 */
	public FLEFRecord parsePersonalNameStructure(GEDCOMNode nameNode, FLEFModel model, String currentXref, String currentTag){
		if(nameNode == null){
			return null;
		}
		FLEFRecord nameRec = FLEFRecord.createChildWithTag("name");

		// Type (optional)
		GEDCOMNode typeNode = findFirstChild(nameNode, "TYPE");
		if(typeNode != null && typeNode.getValue() != null){
			nameRec.addChild(FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue()));
		}

		// Parse inline value (e.g., "Joseph Tag /Torture/") into full text
		String raw = nameNode.getValue();
		String inlineGiven = "";
		String inlineSurname = "";

		if(StringUtils.isNotEmpty(raw)){
			FLEFRecord textRec = FLEFRecord.createChildWithTag("text");
			textRec.addChild(FLEFRecord.createChildWithTagAndValue("value", raw.replaceAll("/", "").trim()));
			nameRec.addChild(textRec);

			int slash1 = raw.indexOf('/');
			int slash2 = raw.indexOf('/', slash1 + 1);
			if(slash1 >= 0 && slash2 > slash1){
				inlineGiven = raw.substring(0, slash1).trim();
				inlineSurname = raw.substring(slash1 + 1, slash2).trim();
				String suffix = raw.substring(slash2 + 1).trim();
				if(!suffix.isEmpty()){
					inlineGiven = (inlineGiven + " " + suffix).trim();
				}
			}
			else{
				inlineGiven = raw.trim();
			}
		}

		// Explicit parts mapping: priority to sub-tags, fallback to inline parsing
		addNamePart(nameRec, nameNode, "GIVN", "given", inlineGiven);
		addNamePart(nameRec, nameNode, "SURN", "surname", inlineSurname);
		addNamePart(nameRec, nameNode, "NPFX", "prefix", null);
		addNamePart(nameRec, nameNode, "NSFX", "suffix", null);
		addNamePart(nameRec, nameNode, "SPFX", "surname_prefix", null);
		addNamePart(nameRec, nameNode, "NICK", "nickname", null);

		// Phonetic variations (FONE)
		for(GEDCOMNode fone : findChildren(nameNode, "FONE")){
			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
			FLEFRecord phonetic = FLEFRecord.createChildWithTag("phonetic");
			GEDCOMNode foneType = findFirstChild(fone, "TYPE");
			String system = (foneType != null && foneType.getValue() != null) ? foneType.getValue() : "IPA";
			phonetic.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
			phonetic.addChild(FLEFRecord.createChildWithTagAndValue("value", fone.getValue()));
			variant.addChild(phonetic);
			nameRec.addChild(variant);
		}

		// Romanized variations (ROMN)
		for(GEDCOMNode romn : findChildren(nameNode, "ROMN")){
			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
			FLEFRecord transcription = FLEFRecord.createChildWithTag("transcription");
			GEDCOMNode romnType = findFirstChild(romn, "TYPE");
			String system = (romnType != null && romnType.getValue() != null) ? romnType.getValue() : "scientific";
			transcription.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
			transcription.addChild(FLEFRecord.createChildWithTagAndValue("value", romn.getValue()));
			variant.addChild(transcription);
			nameRec.addChild(variant);
		}

		// Sources attached directly to NAME (2 SOUR)
		for(GEDCOMNode sourNode : findChildren(nameNode, "SOUR")){
			FLEFRecord sourCitation = parseSourceCitation(sourNode, model, currentXref, currentTag);
			if(sourCitation != null){
				nameRec.addChild(sourCitation);
			}
		}

		// Notes attached directly to NAME (2 NOTE + 3 CONC/CONT)
		for(GEDCOMNode noteNode : findChildren(nameNode, "NOTE")){
			FLEFRecord noteStruct = parseNoteStruct(noteNode);
			if(noteStruct != null){
				nameRec.addChild(noteStruct);
			}
		}

		return nameRec.getChildren().isEmpty() ? null : nameRec;
	}

	/**
	 * Helper method to add a name part record.
	 */
	private void addNamePart(FLEFRecord parent, GEDCOMNode nameNode, String gedcomTag, String flefType, String fallbackValue){
		GEDCOMNode childNode = findFirstChild(nameNode, gedcomTag);
		String value = (childNode != null && StringUtils.isNotEmpty(childNode.getValue()))
			? childNode.getValue().trim()
			: fallbackValue;

		if(StringUtils.isNotEmpty(value)){
			FLEFRecord part = FLEFRecord.createChildWithTag("part");
			part.addChild(FLEFRecord.createChildWithTagAndValue("type", flefType));
			part.addChild(FLEFRecord.createChildWithTagAndValue("value", value));
			parent.addChild(part);
		}
	}

	// ------------------------------------------------------------------------
	// Note parsing – inline NoteStructure with audit
	// ------------------------------------------------------------------------

	/**
	 * Creates an inline NoteStructure with the given text and an audit.
	 * This is the central method for creating notes in all converters.
	 *
	 * @param text       the note content
	 * @param sourceNode the GEDCOM node from which the note originates (used for audit date)
	 * @return a FLEF record with the tag "NOTE" and a value + audit child, or {@code null} if the text is blank
	 */
	public FLEFRecord createNoteStruct(String text, GEDCOMNode sourceNode){
		if(StringUtils.isBlank(text)){
			return null;
		}
		FLEFRecord note = FLEFRecord.createChildWithTag("note");
		note.addChild(FLEFRecord.createChildWithTagAndValue("value", text.trim()));
		// Add required audit
		note.addChild(auditBuilder.build(sourceNode));
		return note;
	}

	/**
	 * Parses a GEDCOM note node into an inline NoteStructure, handling CONC and CONT tags.
	 */
	public FLEFRecord parseNoteStruct(GEDCOMNode noteNode){
		if(noteNode == null){
			return null;
		}
		String text = extractContinuationData(noteNode);
		if(StringUtils.isBlank(text)){
			return null;
		}
		return createNoteStruct(text, noteNode);
	}

	/**
	 * Concatenates GEDCOM multi-line note content (CONC / CONT).
	 */
	public String extractContinuationData(GEDCOMNode node){
		StringBuilder sb = new StringBuilder(node.getValue() != null ? node.getValue() : "");
		for(GEDCOMNode child : node.getChildren()){
			String tag = child.getTag();
			if("CONT".equals(tag)){
				sb.append("\n").append(child.getValue() != null ? (sb.isEmpty() ? "" : " ") + child.getValue() : "");
			}
			else if("CONC".equals(tag)){
				sb.append(child.getValue() != null ? child.getValue() : "");
			}
		}
		return sb.toString();
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
		if(gedcomDate.equalsIgnoreCase("Y") || gedcomDate.equalsIgnoreCase("N")){
			return null;
		}

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
					pointRec.addChild(approx);
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

		// original_text is omitted because the name itself is the same as the original
		return placeCitation;
	}

	// ------------------------------------------------------------------------
	// RepositoryCitation
	// ------------------------------------------------------------------------

	/**
	 * Parses a GEDCOM REPO node into a RepositoryCitation.
	 * Returns null if the repository reference is missing or invalid.
	 */
	public FLEFRecord parseRepositoryCitation(GEDCOMNode repoNode, Map<String, FLEFRecord> repositoryMap){
		if(repoNode == null){
			return null;
		}
		String repoXref = IDNormalizer.clean(repoNode.getValue());
		if(repoXref == null || !repositoryMap.containsKey(repoXref)){
			return null;
		}
		FLEFRecord repoCitation = FLEFRecord.createChildWithTag("repository");
		FLEFRecord repoRef = FLEFRecord.createChildWithTag("repository");
		repoRef.setValue(repoXref);
		repoCitation.addChild(repoRef);

		// CALN -> location
		GEDCOMNode calnNode = findFirstChild(repoNode, "CALN");
		if(calnNode != null && calnNode.getValue() != null){
			repoCitation.addChild(FLEFRecord.createChildWithTagAndValue("location", calnNode.getValue()));
		}
		// Note (optional – plain text)
		GEDCOMNode noteNode = findFirstChild(repoNode, "NOTE");
		if(noteNode != null && noteNode.getValue() != null){
			repoCitation.addChild(FLEFRecord.createChildWithTagAndValue("note", noteNode.getValue()));
		}
		return repoCitation;
	}

	// ------------------------------------------------------------------------
	// SourceCitation
	// ------------------------------------------------------------------------

	public FLEFRecord parseSourceCitation(GEDCOMNode sourNode, FLEFModel model){
		return parseSourceCitation(sourNode, model, null, null);
	}

	public FLEFRecord parseSourceCitation(GEDCOMNode sourNode, FLEFModel model, String currentXref, String currentTag){
		if(sourNode == null){
			return null;
		}

		FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source");
		String rawSourceVal = sourNode.getValue();

		if(StringUtils.isNotEmpty(rawSourceVal)){
			if(rawSourceVal.startsWith("@") && rawSourceVal.endsWith("@")){
				// 1. Puntatore ad un record sorgente top-level (@S1@)
				String cleanId = IDNormalizer.clean(rawSourceVal);
				FLEFRecord sourceRef = FLEFRecord.createChildWithTag("source");
				sourceRef.setValue(cleanId);
				sourceCitation.addChild(sourceRef);
			}
			else{
				// 2. SOUR incorporato (free-form source description)
				String inlineDescription = extractContinuationData(sourNode);
				if(StringUtils.isNotEmpty(inlineDescription) && model != null){
					// Crea un nuovo SourceRecord dinamico da registrare nel modello
					FLEFRecord inlineSource = FLEFRecord.createChildWithTag("source");
					String newSourceId = IDGenerator.nextId("S");
					inlineSource.setId(newSourceId);

					// Title / Description
					FLEFRecord titleRec = FLEFRecord.createChildWithTag("title");
					titleRec.addChild(FLEFRecord.createChildWithTagAndValue("value", inlineDescription));
					inlineSource.addChild(titleRec);

					// Verbatim text sotto l'inline SOUR (1 SOUR / 2 TEXT)
					GEDCOMNode textNode = findFirstChild(sourNode, "TEXT");
					if(textNode != null){
						String verbatimText = extractContinuationData(textNode);
						if(StringUtils.isNotEmpty(verbatimText)){
							FLEFRecord note = createNoteStruct("Verbatim text: " + verbatimText, textNode);
							if(note != null){
								inlineSource.addChild(note);
							}
						}
					}

					// Note sotto l'inline SOUR
					for(GEDCOMNode noteNode : findChildren(sourNode, "NOTE")){
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
		GEDCOMNode pageNode = findFirstChild(sourNode, "PAGE");
		if(pageNode != null && pageNode.getValue() != null){
			sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("location", pageNode.getValue()));
		}

		// EVEN (+ ROLE) -> EventRecord + EventParticipationRecord
		GEDCOMNode evenNode = findFirstChild(sourNode, "EVEN");
		if(evenNode != null && model != null){
			String eventType = evenNode.getValue();
			if(StringUtils.isNotEmpty(eventType)){
				FLEFRecord eventRecord = FLEFRecord.createChildWithTag("event");
				String eventId = IDGenerator.nextId("E");
				eventRecord.setId(eventId);

				// 1. Il tipo per eventi custom/descrittivi da SOUR è "other"
				eventRecord.addChild(FLEFRecord.createChildWithTagAndValue("type", "other"));

				// 2. Preserva il testo originale ("Event type cited in source") nella description
				eventRecord.addChild(FLEFRecord.createChildWithTagAndValue("description", eventType.trim()));

				eventRecord.addChild(createAudit(evenNode));
				model.addRecord(eventRecord);

				GEDCOMNode roleNode = findFirstChild(evenNode, "ROLE");
				String roleValue = (roleNode != null) ? roleNode.getValue() : null;

				if(StringUtils.isNotEmpty(roleValue) && StringUtils.isNotEmpty(currentXref) && StringUtils.isNotEmpty(currentTag)){
					FLEFRecord participationRecord = FLEFRecord.createChildWithTag("event_participation");
					participationRecord.setId(IDGenerator.nextId("EP"));

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
		GEDCOMNode dataNode = findFirstChild(sourNode, "DATA");
		if(dataNode != null){
			GEDCOMNode entryDateNode = findFirstChild(dataNode, "DATE");
			if(entryDateNode != null){
				FLEFRecord dateStruct = parseDateStructure(entryDateNode);
				if(dateStruct != null){
					sourceCitation.addChild(dateStruct);
				}
			}
			for(GEDCOMNode textNode : findChildren(dataNode, "TEXT")){
				String verbatimText = extractContinuationData(textNode);
				if(StringUtils.isNotEmpty(verbatimText)){
					FLEFRecord note = createNoteStruct("Verbatim text: " + verbatimText, textNode);
					if(note != null){
						sourceCitation.addChild(note);
					}
				}
			}
		}

		// QUAY -> EvidenceQualifiers
		GEDCOMNode quayNode = findFirstChild(sourNode, "QUAY");
		if(quayNode != null && quayNode.getValue() != null){
			FLEFRecord evidence = FLEFRecord.createChildWithTag("evidence");
			String evidenceType = switch(quayNode.getValue().trim()){
				case "3" -> "direct";
				case "2" -> "indirect";
				case "0", "1" -> "negative";
				default -> "undetermined";
			};
			evidence.addChild(FLEFRecord.createChildWithTagAndValue("evidence_type", evidenceType));
			sourceCitation.addChild(evidence);
		}

		// Sub-notes e Multimedia (NOTE / OBJE) collegati alla citazione
		for(GEDCOMNode noteNode : findChildren(sourNode, "NOTE")){
			FLEFRecord noteStruct = parseNoteStruct(noteNode);
			if(noteStruct != null){
				sourceCitation.addChild(noteStruct);
			}
		}

		for(GEDCOMNode objNode : findChildren(sourNode, "OBJE")){
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
		String objXref = IDNormalizer.clean(objNode.getValue());
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

		final StringBuilder fullAddr = new StringBuilder(addrNode.getValue() != null ? addrNode.getValue() : "");
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
			return null;
		}

		FLEFRecord contact = FLEFRecord.createChildWithTag("contact");
		contact.addChild(FLEFRecord.createChildWithTagAndValue("address", fullAddr.toString()));

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
			? auditBuilder.build(parentNode)
			: auditBuilder.build(null);
		contact.addChild(audit);
		return contact;
	}

	// ------------------------------------------------------------------------
	// Common substructures (sources, notes, multimedia)
	// ------------------------------------------------------------------------

	private void addCommonSubstructures(GEDCOMNode node, FLEFRecord target){
		// Sources
		for(GEDCOMNode sourNode : findChildren(node, "SOUR")){
			FLEFRecord sourCitation = parseSourceCitation(sourNode, null);
			if(sourCitation != null){
				target.addChild(sourCitation);
			}
		}

		// Notes – now using inline NoteStructure with audit
		for(GEDCOMNode noteNode : findChildren(node, "NOTE")){
			FLEFRecord noteStruct = parseNoteStruct(noteNode);
			if(noteStruct != null){
				target.addChild(noteStruct);
			}
		}

		// Multimedia
		for(GEDCOMNode objNode : findChildren(node, "OBJE")){
			FLEFRecord multimediaLink = parseMultimediaLink(objNode, null);
			if(multimediaLink != null && !multimediaLink.getChildren().isEmpty()){
				target.addChild(multimediaLink);
			}
		}
	}

	// ------------------------------------------------------------------------
	// Utility methods
	// ------------------------------------------------------------------------

	public GEDCOMNode findFirstChild(GEDCOMNode node, String tag){
		if(node == null){
			return null;
		}
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.findFirst()
			.orElse(null);
	}

	public List<GEDCOMNode> findChildren(GEDCOMNode node, String tag){
		if(node == null){
			return Collections.emptyList();
		}
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.toList();
	}

	private String getChildValue(GEDCOMNode node, String tag){
		GEDCOMNode child = findFirstChild(node, tag);
		return child != null ? child.getValue() : null;
	}

	// ------------------------------------------------------------------------
	// Audit builder (wrapper)
	// ------------------------------------------------------------------------

	public FLEFRecord createAudit(GEDCOMNode node){
		return auditBuilder.build(node);
	}

}
