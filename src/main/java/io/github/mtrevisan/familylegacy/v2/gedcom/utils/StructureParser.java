package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
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
		if(evtNode == null) return null;

		// Determine the FLEF event type
		String customType = getChildValue(evtNode, "TYPE");
		String flefType = GEDCOMMapper.mapEvent(gedcomTag, customType);

		FLEFRecord eventRec = FLEFRecord.createChildWithTag("event");
		eventRec.addChild(FLEFRecord.createChildWithTagAndValue("type", flefType));

		// Date
		GEDCOMNode dateNode = findFirstChild(evtNode, "DATE");
		if(dateNode != null){
			FLEFRecord dateStruct = parseDateStructure(dateNode);
			if(dateStruct != null) eventRec.addChild(dateStruct);
		}

		// Place
		GEDCOMNode placNode = findFirstChild(evtNode, "PLAC");
		if(placNode != null){
			FLEFRecord placeCitation = parsePlaceCitation(placNode);
			if(placeCitation != null) eventRec.addChild(placeCitation);
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
			if(note != null) eventRec.addChild(note);
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
		if(attrNode == null) return null;

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
			if(dateStruct != null) attrRec.addChild(dateStruct);
		}

		// Place
		GEDCOMNode placNode = findFirstChild(attrNode, "PLAC");
		if(placNode != null){
			FLEFRecord placeCitation = parsePlaceCitation(placNode);
			if(placeCitation != null) attrRec.addChild(placeCitation);
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
		if(node == null || node.getValue() == null) return null;
		FLEFRecord nameRec = FLEFRecord.createChildWithTag(fieldTag);
		nameRec.addChild(FLEFRecord.createChildWithTagAndValue("value", node.getValue()));
		return nameRec;
	}

	/**
	 * Parses a PersonalNameStructure (for individuals).
	 * The field tag is "name".
	 */
	public FLEFRecord parsePersonalNameStructure(GEDCOMNode nameNode){
		if(nameNode == null) return null;
		FLEFRecord nameRec = FLEFRecord.createChildWithTag("name");

		// Type (optional)
		GEDCOMNode typeNode = findFirstChild(nameNode, "TYPE");
		if(typeNode != null && typeNode.getValue() != null){
			nameRec.addChild(FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue()));
		}

		// Parse inline value: "given /surname/"
		String raw = nameNode.getValue();
		if(raw != null){
			String given = "", surname = "";
			int slash1 = raw.indexOf('/');
			int slash2 = raw.indexOf('/', slash1 + 1);
			if(slash1 >= 0 && slash2 > slash1){
				given = raw.substring(0, slash1).trim();
				surname = raw.substring(slash1 + 1, slash2).trim();
				String suffix = raw.substring(slash2 + 1).trim();
				if(!suffix.isEmpty()){
					given = given + " " + suffix;
				}
			}
			else{
				given = raw.trim();
			}
			if(!given.isEmpty()){
				FLEFRecord part = FLEFRecord.createChildWithTag("part");
				part.addChild(FLEFRecord.createChildWithTagAndValue("type", "given"));
				part.addChild(FLEFRecord.createChildWithTagAndValue("value", given));
				nameRec.addChild(part);
			}
			if(!surname.isEmpty()){
				FLEFRecord part = FLEFRecord.createChildWithTag("part");
				part.addChild(FLEFRecord.createChildWithTagAndValue("type", "family"));
				part.addChild(FLEFRecord.createChildWithTagAndValue("value", surname));
				nameRec.addChild(part);
			}
		}

		// Phonetic variations (FONE) -> variant > phonetic
		for(GEDCOMNode fone : findChildren(nameNode, "FONE")){
			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
			FLEFRecord phonetic = FLEFRecord.createChildWithTag("phonetic");
			GEDCOMNode foneType = findFirstChild(fone, "TYPE");
			String system = (foneType != null && foneType.getValue() != null)? foneType.getValue(): "IPA";
			phonetic.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
			phonetic.addChild(FLEFRecord.createChildWithTagAndValue("value", fone.getValue()));
			variant.addChild(phonetic);
			nameRec.addChild(variant);
		}

		// Romanized variations (ROMN) -> variant > transcription
		for(GEDCOMNode romn : findChildren(nameNode, "ROMN")){
			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
			FLEFRecord transcription = FLEFRecord.createChildWithTag("transcription");
			GEDCOMNode romnType = findFirstChild(romn, "TYPE");
			String system = (romnType != null && romnType.getValue() != null)? romnType.getValue(): "scientific";
			transcription.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
			transcription.addChild(FLEFRecord.createChildWithTagAndValue("value", romn.getValue()));
			variant.addChild(transcription);
			nameRec.addChild(variant);
		}

		return nameRec.getChildren().isEmpty()? null: nameRec;
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
	 * @return a FLEF record with tag "note" and a value + audit child, or null if text is blank
	 */
	public FLEFRecord createNoteStruct(String text, GEDCOMNode sourceNode){
		if(StringUtils.isBlank(text)) return null;
		FLEFRecord note = FLEFRecord.createChildWithTag("note");
		note.addChild(FLEFRecord.createChildWithTagAndValue("value", text.trim()));
		// Add required audit
		note.addChild(auditBuilder.build(sourceNode));
		return note;
	}

	/**
	 * Parses a GEDCOM note node into an inline NoteStructure.
	 * Delegates to createNoteStruct after extracting the text.
	 *
	 * @param noteNode the GEDCOM node (tag "NOTE")
	 * @return a FLEF record with tag "note" and value+audit, or null if empty
	 */
	public FLEFRecord parseNoteStruct(GEDCOMNode noteNode){
		if(noteNode == null) return null;
		String text = noteNode.getValue();
		if(StringUtils.isBlank(text)) return null;
		return createNoteStruct(text, noteNode);
	}

	// ------------------------------------------------------------------------
	// Date parsing
	// ------------------------------------------------------------------------

	public FLEFRecord parseDateStructure(GEDCOMNode dateNode){
		if(dateNode == null) return null;
		String gedcomDate = dateNode.getValue();
		if(StringUtils.isBlank(gedcomDate)) return null;
		if(gedcomDate.equalsIgnoreCase("Y") || gedcomDate.equalsIgnoreCase("N")){
			return null;
		}

		DateInfo info = GEDCOMDateParser.parse(gedcomDate);
		if(info == null) return null;

		FLEFRecord dateStruct = FLEFRecord.createChildWithTag("date");
		FLEFRecord valueRec = FLEFRecord.createChildWithTag("value");

		switch(info.getType()){
			case POINT -> {
				FLEFRecord pointRec = FLEFRecord.createChildWithTag("point");
				FLEFRecord singleDateRec = FLEFRecord.createChildWithTag("single_date");
				FLEFRecord fullDateRec = FLEFRecord.createChildWithTag("full_date");
				fullDateRec.addChild(FLEFRecord.createChildWithTagAndValue("value", info.getValue()));
				fullDateRec.addChild(FLEFRecord.createChildWithTagAndValue("calendar", getCalendarForDate(info.getValue())));
				singleDateRec.addChild(fullDateRec);
				pointRec.addChild(singleDateRec);

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
	 * Always adds a 'single_date' child (required by the grammar).
	 */
	private FLEFRecord buildQualifiedDate(String isoDate){
		// Ensure we have a non‑empty date string; fallback to a default if missing
		if(StringUtils.isBlank(isoDate)){
			isoDate = "1900-01-01";
		}
		FLEFRecord singleDate = FLEFRecord.createChildWithTag("single_date");
		FLEFRecord fullDate = FLEFRecord.createChildWithTag("full_date");
		fullDate.addChild(FLEFRecord.createChildWithTagAndValue("value", isoDate));
		fullDate.addChild(FLEFRecord.createChildWithTagAndValue("calendar", getCalendarForDate(isoDate)));
		singleDate.addChild(fullDate);
		return singleDate;
	}

	/**
	 * Determines the appropriate calendar for a date.
	 * If the year is before 1582, returns "julian", otherwise "gregorian".
	 */
	private String getCalendarForDate(String isoDate){
		if(isoDate == null) return "gregorian";
		int year = 0;
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
		return year < 1582? "julian": "gregorian";
	}

	// ------------------------------------------------------------------------
	// PlaceCitation
	// ------------------------------------------------------------------------

	/**
	 * Parses a GEDCOM PLAC node and returns a PlaceCitation.
	 * It also creates/updates the underlying PlaceRecord with subfields.
	 */
	public FLEFRecord parsePlaceCitation(GEDCOMNode placNode){
		if(placNode == null) return null;
		String placeName = placNode.getValue();
		if(StringUtils.isBlank(placeName)) return null;

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
		if(repoNode == null) return null;
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
		if(sourNode == null) return null;
		FLEFRecord sourceCitation = FLEFRecord.createChildWithTag("source_citation");
		String sourceXref = sourNode.getValue();
		if(sourceXref != null){
			String cleanId = IDNormalizer.clean(sourceXref);
			FLEFRecord sourceRef = FLEFRecord.createChildWithTag("source");
			sourceRef.setValue(cleanId);
			sourceCitation.addChild(sourceRef);
		}
		// PAGE -> location
		GEDCOMNode pageNode = findFirstChild(sourNode, "PAGE");
		if(pageNode != null && pageNode.getValue() != null){
			sourceCitation.addChild(FLEFRecord.createChildWithTagAndValue("location", pageNode.getValue()));
		}

		// QUAY -> EvidenceQualifiers
		GEDCOMNode quayNode = findFirstChild(sourNode, "QUAY");
		if(quayNode != null && quayNode.getValue() != null){
			FLEFRecord evidence = FLEFRecord.createChildWithTag("evidence");
			String quayVal = quayNode.getValue();
			String evidenceType;
			try{
				int q = Integer.parseInt(quayVal);
				if(q >= 3) evidenceType = "direct";
				else if(q == 2) evidenceType = "indirect";
				else evidenceType = "negative";
			}
			catch(NumberFormatException e){
				evidenceType = "undetermined";
			}
			evidence.addChild(FLEFRecord.createChildWithTagAndValue("evidence_type", evidenceType));
			sourceCitation.addChild(evidence);
		}
		return sourceCitation;
	}

	// ------------------------------------------------------------------------
	// MultimediaLink (Xref<DocumentRecord>)
	// ------------------------------------------------------------------------

	public FLEFRecord parseMultimediaLink(GEDCOMNode objNode, FLEFModel model){
		if(objNode == null) return null;
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
		if(addrNode == null) return null;

		String fullAddr = addrNode.getValue() != null? addrNode.getValue(): "";
		for(String subTag : List.of("ADR1", "ADR2", "ADR3", "CITY", "STAE", "POST", "CTRY")){
			GEDCOMNode sub = findFirstChild(addrNode, subTag);
			if(sub != null && sub.getValue() != null){
				if(!fullAddr.isEmpty()) fullAddr += "\n";
				fullAddr += sub.getValue();
			}
		}
		if(fullAddr.isEmpty()) return null;

		FLEFRecord contact = FLEFRecord.createChildWithTag("contact");
		contact.addChild(FLEFRecord.createChildWithTagAndValue("address", fullAddr));

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
				if(note != null) contact.addChild(note);
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
			if(sourCitation != null) target.addChild(sourCitation);
		}

		// Notes – now using inline NoteStructure with audit
		for(GEDCOMNode noteNode : findChildren(node, "NOTE")){
			FLEFRecord noteStruct = parseNoteStruct(noteNode);
			if(noteStruct != null) target.addChild(noteStruct);
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
		if(node == null) return null;
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.findFirst()
			.orElse(null);
	}

	public List<GEDCOMNode> findChildren(GEDCOMNode node, String tag){
		if(node == null) return Collections.emptyList();
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.toList();
	}

	private String getChildValue(GEDCOMNode node, String tag){
		GEDCOMNode child = findFirstChild(node, tag);
		return child != null? child.getValue(): null;
	}

	// ------------------------------------------------------------------------
	// Audit builder (wrapper)
	// ------------------------------------------------------------------------

	public FLEFRecord createAudit(GEDCOMNode node){
		return auditBuilder.build(node);
	}

}
