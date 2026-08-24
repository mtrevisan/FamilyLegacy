package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.time.LocalDate;


public class AuditBuilder{

	public AuditBuilder(){
	}

	public FLEFRecord build(GEDCOMNode node){
		FLEFRecord audit = FLEFRecord.createChildWithTag("audit");
		GEDCOMNode chanNode = (node != null)? findFirstChild(node, "CHAN"): null;

		String isoDateTime = null;
		if(chanNode != null){
			GEDCOMNode dateNode = findFirstChild(chanNode, "DATE");
			GEDCOMNode timeNode = findFirstChild(dateNode, "TIME");
			if(dateNode != null && dateNode.getValue() != null){
				String datePart = DateNormalizer.normalize(dateNode.getValue());
				if(datePart != null){
					if(timeNode != null && timeNode.getValue() != null){
						// Combine date and time: "YYYY-MM-DDTHH:MM:SS"
						isoDateTime = datePart + "T" + timeNode.getValue().trim();
					}
					else{
						isoDateTime = datePart;
					}
				}
			}
		}

		// Fallback to current date if no valid datetime
		if(isoDateTime == null){
			isoDateTime = LocalDate.now().toString(); // e.g., "2026-08-25"
		}

		FLEFRecord creation = FLEFRecord.createChildWithTag("creation");
		creation.addChild(FLEFRecord.createChildWithTagAndValue("date", isoDateTime));
		audit.addChild(creation);

		return audit;
	}

	private GEDCOMNode findFirstChild(GEDCOMNode node, String tag){
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.findFirst()
			.orElse(null);
	}

}
