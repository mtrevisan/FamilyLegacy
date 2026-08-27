package io.github.mtrevisan.familylegacy.v2.gedcom.converters;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;


public class GEDCOMHelper{

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
		.ofPattern("uuuu-MM-dd")
		.withResolverStyle(ResolverStyle.STRICT);


	private GEDCOMHelper(){}


	public static GEDCOMNode findFirstChild(GEDCOMNode node, String tag){
		return node.getChildren().stream()
			.filter(c -> c.getTag().equals(tag))
			.findFirst()
			.orElse(null);
	}


	/**
	 * Reconstructs the full note text from a GEDCOM NOTE node.
	 * Handles CONC and CONT children to concatenate lines correctly.
	 *
	 * @param noteNode the GEDCOM NOTE node
	 * @return the full text, or null if no text found
	 */
	public static String getFullNoteText(GEDCOMNode noteNode){
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

}
