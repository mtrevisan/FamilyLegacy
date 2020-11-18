package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;


enum GregorianMonth{

	JANUARY("January", "JAN"),
	FEBRUARY("February", "FEB"),
	MARCH("March", "MAR"),
	APRIL("April", "APR"),
	MAY("May", "MAY"),
	JUNE("June", "JUN"),
	JULY("July", "JUL"),
	AUGUST("August", "AUG"),
	SEPTEMBER("September", "SEP"),
	OCTOBER("October", "OCT"),
	NOVEMBER("November", "NOV"),
	DECEMBER("December", "DEC");


	private final String description;
	private final String abbreviation;


	/**
	 * Get an enum value from the gedcom abbreviation
	 *
	 * @param abbreviation	The GEDCOM spec abbreviation for this month
	 * @return	the enum constant that matches the abbreviation
	 */
	public static GregorianMonth createFromAbbreviation(String abbreviation){
		GregorianMonth result = null;
		for(GregorianMonth month : values())
			if(month.abbreviation.equalsIgnoreCase(abbreviation)){
				result = month;
				break;
			}
		return result;
	}

	GregorianMonth(final String description, final String abbreviation){
		this.description = description;
		this.abbreviation = abbreviation;
	}

	public String getDescription(){
		return description;
	}

	public static String[] getDescriptionsWithEmptyValueFirst(){
		return Stream.concat(Stream.of(StringUtils.EMPTY), Arrays.stream(values())
			.map(GregorianMonth::getDescription))
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
		for(final GregorianMonth gregorianMonth : values())
			list.add(gregorianMonth.abbreviation);
		return list.toArray(new String[0]);
	}

	private static String[] getDescriptions(){
		final List<String> list = new ArrayList<>();
		for(final GregorianMonth gregorianMonth : values())
			list.add(gregorianMonth.description);
		return list.toArray(new String[0]);
	}

}
