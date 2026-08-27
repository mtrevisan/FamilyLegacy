package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class HeaderConverter{

	private final FLEFModel model;


	public HeaderConverter(FLEFModel model){
		this.model = model;
	}

	public void convert(GEDCOMNode headNode){
		FLEFRecord header = FLEFRecord.createChildWithTag("header");

		// ---- Protocol (hardcoded) ----
		FLEFRecord protocol = FLEFRecord.createChildWithTag("protocol")
			.addChild(FLEFRecord.createChildWithTagAndValue("name", "Family LEgacy Format"))
			.addChild(FLEFRecord.createChildWithTagAndValue("version", "0.1.2"));
		header.addChild(protocol);

		// ---- Source (from GEDCOM SOUR) ----
		GEDCOMNode sourNode = GEDCOMHelper.findFirstChild(headNode, "SOUR");
		if(sourNode != null){
			GEDCOMNode nameNode = GEDCOMHelper.findFirstChild(sourNode, "NAME");
			GEDCOMNode versNode = GEDCOMHelper.findFirstChild(sourNode, "VERS");
			GEDCOMNode corpNode = GEDCOMHelper.findFirstChild(sourNode, "CORP");

			FLEFRecord source = FLEFRecord.createChildWithTag("source");
			GEDCOMHelper.transferValue(source, "system_id", sourNode);
			GEDCOMHelper.transferValue(source, "name", nameNode);
			GEDCOMHelper.transferValue(source, "version", versNode);
			GEDCOMHelper.transferValue(source, "organization", corpNode);
			header.addChild(source);
		}

		// ---- Date (transmission date) ----
		String iso = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS));
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(headNode, "DATE");
		if(dateNode != null && StringUtils.isNotEmpty(dateNode.getValue()))
			iso = GEDCOMHelper.getDateTime(dateNode);
		header.addChild(FLEFRecord.createChildWithTagAndValue("date", iso));

		// ---- Copyright ----
		GEDCOMNode coprNode = GEDCOMHelper.findFirstChild(headNode, "COPR");
		GEDCOMHelper.transferValue(header, "copyright", coprNode);

		// ---- Scope (from NOTE) ----
		GEDCOMNode noteNode = GEDCOMHelper.findFirstChild(headNode, "NOTE");
		if(noteNode != null && noteNode.getValue() != null){
			header.addChild(FLEFRecord.createChildWithTagAndValue("scope", GEDCOMHelper.extractFullText(noteNode)));
		}

		// ---- Submitter (will be added later) ----
		model.setHeader(header);
	}

}
