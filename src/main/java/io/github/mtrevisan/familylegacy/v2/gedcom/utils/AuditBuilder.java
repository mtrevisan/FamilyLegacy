package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class AuditBuilder{

	private AuditBuilder(){}

	public static FLEFRecord build(GEDCOMNode node){
		FLEFRecord audit = FLEFRecord.createChildWithTag("audit");
		GEDCOMNode chanNode = (node != null) ? GEDCOMHelper.findFirstChild(node, "CHAN") : null;

		// Extraction of CHAN -> DATE and TIME
		String isoDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS));
		GEDCOMNode dateNode = GEDCOMHelper.findFirstChild(chanNode, "DATE");
		if(dateNode != null && StringUtils.isNotEmpty(dateNode.getValue()))
			isoDateTime = GEDCOMHelper.getDateTime(dateNode);

		// Extraction of CHAN -> NOTE
		GEDCOMNode noteNode = GEDCOMHelper.findFirstChild(chanNode, "NOTE");
		String chanNoteText = GEDCOMHelper.extractFullText(noteNode);
//		if(chanNoteText == null){
//			chanNoteText = "From GEDCOM conversion";
//		}

		FLEFRecord creation = FLEFRecord.createChildWithTag("creation");
		creation.addChild(FLEFRecord.createChildWithTagAndValue("date", isoDateTime));
		if(StringUtils.isNotEmpty(chanNoteText)){
			creation.addChild(FLEFRecord.createChildWithTagAndValue("comment", chanNoteText));
		}

		audit.addChild(creation);

		return audit;
	}

}
