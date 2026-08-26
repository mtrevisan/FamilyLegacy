package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parses GEDCOM date strings (e.g., "2 APR 1956", "AFT 3 MAY 1894", "BET 1900 AND 1910")
 * and produces a {@link DateInfo} object that describes the intended FLEF structure.
 */
public final class GEDCOMDateParser{

	private static final Pattern DATE_PATTERN = Pattern.compile(
		"^(?:(?<qualifier>ABT|CAL|EST|BEF|AFT|BET|FROM|TO)\\s+)?" +
			"(?<from>.+?)(?:\\s+AND\\s+(?<to>.+))?$",
		Pattern.CASE_INSENSITIVE
	);

	private static final Pattern RANGE_PATTERN = Pattern.compile(
		"^(FROM\\s+(?<from>.+?))?\\s*(?:TO\\s+(?<to>.+))?$",
		Pattern.CASE_INSENSITIVE
	);

	private GEDCOMDateParser(){
	}

	public static DateInfo parse(String dateStr){
		if(StringUtils.isBlank(dateStr)) return null;

		String trimmed = dateStr.trim();
		DateInfo.Builder builder = new DateInfo.Builder();

		// 1. Check for leading qualifier (ABT, BEF, AFT, etc.)
		Matcher m = DATE_PATTERN.matcher(trimmed);
		if(m.matches()){
			String qualifier = m.group("qualifier");
			String fromPart = m.group("from");
			String toPart = m.group("to");

			if(qualifier != null){
				qualifier = qualifier.toUpperCase();
				builder.qualifier(qualifier);
				switch(qualifier){
					case "ABT":
					case "CAL":
					case "EST":
						builder.approximate(true);
						// Treat as a point date with approximate qualifier
						builder.type(DateInfo.Type.POINT);
						builder.value(fromPart);
						break;
					case "BEF":
						builder.type(DateInfo.Type.BOUNDED);
						builder.notAfter(fromPart);
						break;
					case "AFT":
						builder.type(DateInfo.Type.BOUNDED);
						builder.notBefore(fromPart);
						break;
					case "BET":
						if(toPart != null){
							builder.type(DateInfo.Type.BOUNDED);
							builder.notBefore(fromPart);
							builder.notAfter(toPart);
						}
						else{
							// Fallback: treat as point
							builder.type(DateInfo.Type.POINT);
							builder.value(fromPart);
						}
						break;
					case "FROM":
					case "TO":
						// Handle FROM/TO as spanning (if both present) or bounded (if one)
						String fromDate = m.group("from");
						String toDate = m.group("to");
						if(qualifier.equals("FROM") && toDate != null){
							builder.type(DateInfo.Type.SPANNING);
							builder.from(fromDate);
							builder.to(toDate);
						}
						else if(qualifier.equals("TO") && fromDate != null){
							builder.type(DateInfo.Type.BOUNDED);
							builder.notAfter(fromDate);
						}
						else{
							// Fallback: point
							builder.type(DateInfo.Type.POINT);
							builder.value(fromPart);
						}
						break;
					default:
						builder.type(DateInfo.Type.POINT);
						builder.value(trimmed);
				}
			}
			else{
				// No qualifier: simple date
				builder.type(DateInfo.Type.POINT);
				builder.value(trimmed);
			}
		}
		else{
			// No qualifier match: treat as simple date
			builder.type(DateInfo.Type.POINT);
			builder.value(trimmed);
		}

		return builder.build();
	}

}
