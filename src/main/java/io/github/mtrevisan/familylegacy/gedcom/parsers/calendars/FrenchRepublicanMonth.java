package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.Arrays;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;


enum FrenchRepublicanMonth{

	VENDEMIAIRE("Vendemiaire", "VEND"),
	BRUMAIRE("Brumaire", "BRUM"),
	FRIMAIRE("Frimaire", "FRIM"),
	NIVOSE("Nivose", "NIVO"),
	PLUVIOSE("Pluviose", "PLUV"),
	VENTOSE("Ventose", "VENT"),
	GERMINAL("Germinal", "GERM"),
	FLOREAL("Floreal", "FLOR"),
	PRAIRIAL("Prairial", "PRAI"),
	MESSIDOR("Messidor", "MESS"),
	THERMIDOR("Thermidor", "THER"),
	FRUCTIDOR("Fructidor", "FRUC"),
	/** The complementary days at the end of each year */
	JOUR_COMPLEMENTAIRS("Jour complementairs", "COMP");


	private final String description;
	private final String abbreviation;


	/**
	 * Get the enumerated constant value with the supplied abbreviation
	 *
	 * @param abbreviation	the gedcom-spec abbreviation for the month
	 * @return	the enumerated constant value with the supplied abbreviation, or null if no match is found
	 */
	public static FrenchRepublicanMonth createFromAbbreviation(String abbreviation){
		FrenchRepublicanMonth result = null;
		for(FrenchRepublicanMonth month : values())
			if(month.abbreviation.equalsIgnoreCase(abbreviation)){
				result = month;
				break;
			}
		return result;
	}

	public static String[] getDescriptionsWithEmptyValueFirst(){
		return Stream.concat(Stream.of(StringUtils.EMPTY), Arrays.stream(values()).map(FrenchRepublicanMonth::getDescription))
			.toArray(String[]::new);
	}

	public static String replaceAll(String month){
		return StringUtils.replaceEachRepeatedly(month, getAbbreviations(), getDescriptions());
	}

	public static String restoreAll(String month){
		return StringUtils.replaceEachRepeatedly(month, getDescriptions(), getAbbreviations());
	}

	private static String[] getAbbreviations(){
		return Arrays.stream(values())
			.map(FrenchRepublicanMonth::getAbbreviation)
			.toArray(String[]::new);
	}

	private static String[] getDescriptions(){
		return Arrays.stream(values())
			.map(FrenchRepublicanMonth::getDescription)
			.toArray(String[]::new);
	}

}
