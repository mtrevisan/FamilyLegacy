/**
 * Copyright (c) 2020 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


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
	public static HebrewMonth fromAbbreviation(final String abbreviation){
		HebrewMonth result = null;
		for(final HebrewMonth month : values())
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

	public static String replaceAll(final String month){
		return StringUtils.replaceEachRepeatedly(month, getAbbreviations(), getDescriptions());
	}

	public static String restoreAll(final String month){
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
