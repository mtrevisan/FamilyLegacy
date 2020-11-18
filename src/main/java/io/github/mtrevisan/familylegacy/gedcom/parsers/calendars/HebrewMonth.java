package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;


enum HebrewMonth{

	TISHREI("Tishrei", "TSH"),
	CHESHVAN("Cheshvan", "CSH"),
	KISLEV("Kislev", "KSL"),
	TEVET("Tevet", "TVT"),
	SHEVAT("Shevai", "SHV"),
	/** (only valid on leap years) */
	ADAR_A("Adar A", "ADR"),
	/** (called Adar B for leap years) */
	ADAR("Adar B", "ADS"),
	NISAN("Nisan", "NSN"),
	IYAR("Iyar", "IYR"),
	SIVAN("Sivan", "SVN"),
	TAMMUZ("Tammuz", "TMZ"),
	AV("Av", "AAV"),
	ELUL("Elul", "ELL");


	private final String description;
	private final String abbreviation;


	/**
	 * Get an enum value from the gedcom abbreviation
	 *
	 * @param abbreviation	The GEDCOM spec abbreviation for this month
	 * @return	the enum constant that matches the abbreviation
	 */
	public static HebrewMonth createFromAbbreviation(String abbreviation){
		HebrewMonth result = null;
		for(HebrewMonth month : values())
			if(month.abbreviation.equalsIgnoreCase(abbreviation)){
				result = month;
				break;
			}
		return result;
	}

	HebrewMonth(final String description, final String abbreviation){
		this.description = description;
		this.abbreviation = abbreviation;
	}

	public String getDescription(){
		return description;
	}

	public static String[] getDescriptionsWithEmptyValueFirst(){
		return Stream.concat(Stream.of(StringUtils.EMPTY), Arrays.stream(values())
			.map(HebrewMonth::getDescription))
			.toArray(String[]::new);
	}

	public static String replaceAll(String month){
		return StringUtils.replaceEachRepeatedly(month, getAbbreviations(), getDescriptions());
	}

	public static String restoreAll(String month){
		return StringUtils.replaceEachRepeatedly(month, getDescriptions(), getAbbreviations());
	}

	private static String[] getAbbreviations(){
		final List<String> list = new ArrayList<>();
		for(final HebrewMonth hebrewMonth : values())
			list.add(hebrewMonth.abbreviation);
		return list.toArray(new String[0]);
	}

	private static String[] getDescriptions(){
		final List<String> list = new ArrayList<>();
		for(final HebrewMonth hebrewMonth : values())
			list.add(hebrewMonth.description);
		return list.toArray(new String[0]);
	}

}
