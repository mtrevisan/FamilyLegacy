package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;


/**
 * Parses GEDCOM personal name structures (NAME) and converts them to FLEF PersonalNameStructure.
 * Handles:
 * <ul>
 *   <li>Inline name format: "given /surname/"</li>
 *   <li>Sub-structures: TYPE, FONE (phonetic), ROMN (romanized)</li>
 *   <li>Name pieces: NPFX, GIVN, NICK, SPFX, SURN, NSFX</li>
 * </ul>
 */
public class NameParser {

	/**
	 * Parses a GEDCOM NAME node into a FLEF "name" structure.
	 * @param nameNode the GEDCOM node with tag "NAME"
	 * @return a FLEF record with tag "name", or null if no data
	 */
	public FLEFRecord parse(GEDCOMNode nameNode) {
		if (nameNode == null) return null;

		FLEFRecord nameRec = FLEFRecord.createChildWithTag("name");

		// 1. Process the inline value (if present) to extract given and surname
//		String raw = nameNode.getValue();
//		if (raw != null) {
//			parseInlineName(raw, nameRec);
//		}

		// 2. Process sub-structures for TYPE, FONE, ROMN
		GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(nameNode, "TYPE");
		if (typeNode != null && typeNode.getValue() != null) {
			nameRec.addChild(FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue()));
		}

		// Phonetic variants (FONE) -> variant > phonetic
		for (GEDCOMNode fone : GEDCOMHelper.findChildren(nameNode, "FONE")) {
			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
			FLEFRecord phonetic = FLEFRecord.createChildWithTag("phonetic");
			// Get system from TYPE sub-tag
			GEDCOMNode foneType = GEDCOMHelper.findFirstChild(fone, "TYPE");
			String system = (foneType != null && foneType.getValue() != null) ? foneType.getValue() : "IPA";
			phonetic.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
			phonetic.addChild(FLEFRecord.createChildWithTagAndValue("value", fone.getValue()));
			variant.addChild(phonetic);
			nameRec.addChild(variant);
		}

		// Romanized variants (ROMN) -> variant > transcription
		for (GEDCOMNode romn : GEDCOMHelper.findChildren(nameNode, "ROMN")) {
			FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
			FLEFRecord transcription = FLEFRecord.createChildWithTag("transcription");
			GEDCOMNode romnType = GEDCOMHelper.findFirstChild(romn, "TYPE");
			String system = (romnType != null && romnType.getValue() != null) ? romnType.getValue() : "scientific";
			transcription.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
			// Type (romanized, latinized, etc.) can be added if available, but not in FLEF yet.
			transcription.addChild(FLEFRecord.createChildWithTagAndValue("value", romn.getValue()));
			variant.addChild(transcription);
			nameRec.addChild(variant);
		}

		// 3. Process name pieces (NPFX, GIVN, NICK, SPFX, SURN, NSFX) if present.
		// These are separate children that may define the name components more explicitly.
		// They are typically used when the inline value is not sufficient.
		// We will merge them into "part" elements.
		boolean hasPieces = false;
		for (String pieceTag : List.of("NPFX", "GIVN", "NICK", "SPFX", "SURN", "NSFX")) {
			GEDCOMNode piece = GEDCOMHelper.findFirstChild(nameNode, pieceTag);
			if (piece != null && piece.getValue() != null) {
				hasPieces = true;
				FLEFRecord part = FLEFRecord.createChildWithTag("part");
				String type = mapPieceTag(pieceTag);
				part.addChild(FLEFRecord.createChildWithTagAndValue("type", type));
				part.addChild(FLEFRecord.createChildWithTagAndValue("value", piece.getValue()));
				nameRec.addChild(part);
			}
		}

		// If we already have parts from inline parsing but also have pieces, we might have duplicates.
		// To avoid duplication, we could clear the existing parts if pieces are present, but we'll just add both.
		// In practice, GEDCOM files usually use either inline or pieces, not both.

		return nameRec.getChildren().isEmpty() ? null : nameRec;
	}

	/**
	 * Parses the inline name format "given /surname/" and adds "part" elements.
	 */
	private void parseInlineName(String raw, FLEFRecord nameRec) {
		String given;
		String surname = "";
		int slash1 = raw.indexOf('/');
		int slash2 = raw.indexOf('/', slash1 + 1);
		if (slash1 >= 0 && slash2 > slash1) {
			given = raw.substring(0, slash1).trim();
			surname = raw.substring(slash1 + 1, slash2).trim();
			String suffix = raw.substring(slash2 + 1).trim();
			if (!suffix.isEmpty()) {
				given = given + " " + suffix; // may need to handle better
			}
		} else {
			given = raw.trim();
		}

		if (!given.isEmpty()) {
			FLEFRecord part = FLEFRecord.createChildWithTag("part");
			part.addChild(FLEFRecord.createChildWithTagAndValue("type", "given"));
			part.addChild(FLEFRecord.createChildWithTagAndValue("value", given));
			nameRec.addChild(part);
		}
		if (!surname.isEmpty()) {
			FLEFRecord part = FLEFRecord.createChildWithTag("part");
			part.addChild(FLEFRecord.createChildWithTagAndValue("type", "family"));
			part.addChild(FLEFRecord.createChildWithTagAndValue("value", surname));
			nameRec.addChild(part);
		}
	}

	/**
	 * Maps GEDCOM name piece tags to FLEF part types.
	 */
	private String mapPieceTag(String gedcomTag) {
		return switch (gedcomTag) {
			case "NPFX" -> "prefix";
			case "GIVN" -> "given";
			case "NICK" -> "nickname";
			case "SPFX" -> "surname_prefix";
			case "SURN" -> "family";
			case "NSFX" -> "suffix";
			default -> gedcomTag.toLowerCase();
		};
	}

	/**
	 * Parses a generic NameStructure (used in Source titles, etc.) – just value + locale.
	 */
	public FLEFRecord parseNameStructure(GEDCOMNode node) {
		if (node == null || node.getValue() == null) return null;
		FLEFRecord nameRec = FLEFRecord.createChildWithTag("name");
		FLEFRecord textRec = FLEFRecord.createChildWithTag("text"); // Actually it should be direct "value" field in NameStructure.
		// In FLEF, NameStructure has fields: value, locale, variant*, source*, note*.
		// We'll just use the value directly.
		nameRec.addChild(FLEFRecord.createChildWithTagAndValue("value", node.getValue()));
		// Locale not available.
		return nameRec;
	}

}
