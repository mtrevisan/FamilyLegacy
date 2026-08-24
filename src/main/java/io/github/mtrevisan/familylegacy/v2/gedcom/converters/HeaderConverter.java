package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.gedcom.utils.DateNormalizer;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.Map;


public class HeaderConverter{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> submitterMap;

	public HeaderConverter(FLEFModel model, Map<String, FLEFRecord> submitterMap){
		this.model = model;
		this.submitterMap = submitterMap;
	}

	public void convert(GEDCOMNode headNode){
		FLEFRecord header = FLEFRecord.createChildWithTag("header");

		// ---- Protocol (hardcoded) ----
		FLEFRecord protocol = FLEFRecord.createChildWithTag("protocol");
		protocol.addChild(FLEFRecord.createChildWithTagAndValue("name", "Family LEgacy Format"));
		protocol.addChild(FLEFRecord.createChildWithTagAndValue("version", "0.1.2"));
		header.addChild(protocol);

		// ---- Source (from GEDCOM SOUR) ----
		GEDCOMNode sourNode = findFirstChild(headNode, "SOUR");
		if(sourNode != null){
			FLEFRecord source = FLEFRecord.createChildWithTag("source");
			source.addChild(FLEFRecord.createChildWithTagAndValue("system_id", sourNode.getValue()));
			GEDCOMNode vers = findFirstChild(sourNode, "VERS");
			if(vers != null) source.addChild(FLEFRecord.createChildWithTagAndValue("version", vers.getValue()));
			GEDCOMNode name = findFirstChild(sourNode, "NAME");
			if(name != null) source.addChild(FLEFRecord.createChildWithTagAndValue("name", name.getValue()));
			GEDCOMNode corp = findFirstChild(sourNode, "CORP");
			if(corp != null) source.addChild(FLEFRecord.createChildWithTagAndValue("corporate", corp.getValue()));
			header.addChild(source);
		}

		// ---- Date (transmission date) ----
		GEDCOMNode dateNode = findFirstChild(headNode, "DATE");
		if(dateNode != null){
			String iso = new DateNormalizer().normalize(dateNode.getValue());
			if(iso != null) header.addChild(FLEFRecord.createChildWithTagAndValue("date", iso));
		}

		// ---- Copyright ----
		GEDCOMNode coprNode = findFirstChild(headNode, "COPR");
		if(coprNode != null){
			header.addChild(FLEFRecord.createChildWithTagAndValue("copyright", coprNode.getValue()));
		}

		// ---- Scope (from NOTE) ----
		GEDCOMNode noteNode = findFirstChild(headNode, "NOTE");
		if(noteNode != null && noteNode.getValue() != null){
			header.addChild(FLEFRecord.createChildWithTagAndValue("scope", noteNode.getValue()));
		}

		// ---- DEST (receiving system) ----
		GEDCOMNode destNode = findFirstChild(headNode, "DEST");
		if(destNode != null && destNode.getValue() != null){
			// No direct mapping; store as note
			FLEFRecord note = FLEFRecord.createChildWithTag("note");
			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Destination: " + destNode.getValue()));
			header.addChild(note);
		}

		// ---- FILE (GEDCOM file name) ----
		GEDCOMNode fileNode = findFirstChild(headNode, "FILE");
		if(fileNode != null && fileNode.getValue() != null){
			// Store as note
			FLEFRecord note = FLEFRecord.createChildWithTag("note");
			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "File: " + fileNode.getValue()));
			header.addChild(note);
		}

		// ---- GEDC (GEDCOM version and form) ----
		GEDCOMNode gedcNode = findFirstChild(headNode, "GEDC");
		if(gedcNode != null){
			GEDCOMNode vers = findFirstChild(gedcNode, "VERS");
			GEDCOMNode form = findFirstChild(gedcNode, "FORM");
			if(vers != null && vers.getValue() != null){
				FLEFRecord note = FLEFRecord.createChildWithTag("note");
				note.addChild(FLEFRecord.createChildWithTagAndValue("value", "GEDCOM version: " + vers.getValue()));
				header.addChild(note);
			}
			if(form != null && form.getValue() != null){
				FLEFRecord note = FLEFRecord.createChildWithTag("note");
				note.addChild(FLEFRecord.createChildWithTagAndValue("value", "GEDCOM form: " + form.getValue()));
				header.addChild(note);
			}
		}

		// ---- CHAR (character set) ----
		GEDCOMNode charNode = findFirstChild(headNode, "CHAR");
		if(charNode != null && charNode.getValue() != null){
			FLEFRecord note = FLEFRecord.createChildWithTag("note");
			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Character set: " + charNode.getValue()));
			header.addChild(note);
		}

		// ---- LANG (language) ----
		GEDCOMNode langNode = findFirstChild(headNode, "LANG");
		if(langNode != null && langNode.getValue() != null){
			FLEFRecord note = FLEFRecord.createChildWithTag("note");
			note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Language: " + langNode.getValue()));
			header.addChild(note);
		}

		// ---- PLAC (place hierarchy format) ----
		GEDCOMNode placNode = findFirstChild(headNode, "PLAC");
		if(placNode != null){
			GEDCOMNode formNode = findFirstChild(placNode, "FORM");
			if(formNode != null && formNode.getValue() != null){
				FLEFRecord note = FLEFRecord.createChildWithTag("note");
				note.addChild(FLEFRecord.createChildWithTagAndValue("value", "Place hierarchy: " + formNode.getValue()));
				header.addChild(note);
			}
		}

		// ---- Submitter (will be added later) ----
		model.setHeader(header);
	}

	public void ensureHeader(){ /* same as before, but also merge with any existing */ }

	private GEDCOMNode findFirstChild(GEDCOMNode node, String tag){
		return node.getChildren().stream().filter(c -> c.getTag().equals(tag)).findFirst().orElse(null);
	}

}
