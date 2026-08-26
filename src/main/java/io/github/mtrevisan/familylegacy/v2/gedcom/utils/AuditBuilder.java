package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class AuditBuilder{

	public AuditBuilder(){
	}

	public FLEFRecord build(GEDCOMNode node){
		FLEFRecord audit = FLEFRecord.createChildWithTag("audit");
		GEDCOMNode chanNode = (node != null) ? findFirstChild(node, "CHAN") : null;

		String isoDateTime = null;
		String chanNoteText = null;

		if(chanNode != null){
			// Extraction of CHAN -> DATE and TIME
			GEDCOMNode dateNode = findFirstChild(chanNode, "DATE");
			if(dateNode != null && dateNode.getValue() != null){
				String datePart = dateNode.getValue().trim();
				GEDCOMNode timeNode = findFirstChild(dateNode, "TIME");
				if(timeNode != null && timeNode.getValue() != null){
					isoDateTime = datePart + "T" + timeNode.getValue().trim();
				}
				else{
					isoDateTime = datePart;
				}
			}

			// Extraction of CHAN -> NOTE
			GEDCOMNode noteNode = findFirstChild(chanNode, "NOTE");
			if(noteNode != null){
				chanNoteText = extractFullNoteText(noteNode);
			}
		}

		// Fallback to current date if no valid datetime
		String auditComment = chanNoteText;
		if(isoDateTime == null){
			isoDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.DAYS));
			if(auditComment == null || auditComment.isBlank()){
				auditComment = "From GEDCOM conversion";
			}
			else{
				auditComment = "From GEDCOM conversion. " + auditComment;
			}
		}

		FLEFRecord creation = FLEFRecord.createChildWithTag("creation");
		creation.addChild(FLEFRecord.createChildWithTagAndValue("date", isoDateTime));

		if(auditComment != null && !auditComment.isBlank()){
			creation.addChild(FLEFRecord.createChildWithTagAndValue("comment", auditComment));
		}

		audit.addChild(creation);

		return audit;
	}

	/**
	 * Extracts full text from a NOTE node, appending CONT/CONC lines if present.
	 */
	private String extractFullNoteText(GEDCOMNode noteNode){
		StringBuilder builder = new StringBuilder();
		if(noteNode.getValue() != null){
			builder.append(noteNode.getValue().trim());
		}

		for(GEDCOMNode child : noteNode.getChildren()){
			String tag = child.getTag();
			if("CONT".equals(tag) && child.getValue() != null){
				if(!builder.isEmpty()){
					builder.append("\n");
				}
				if(!builder.isEmpty())
					builder.append(' ');
				builder.append(child.getValue().trim());
			}
			else if("CONC".equals(tag) && child.getValue() != null){
				builder.append(" ").append(child.getValue().trim());
			}
		}

		return builder.toString();
	}

	private GEDCOMNode findFirstChild(GEDCOMNode node, String tag){
		if(node == null){
			return null;
		}
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.findFirst()
			.orElse(null);
	}

}
