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
	/** The complementary days at the end of each year. */
	JOUR_COMPLEMENTAIRS("Jour complementairs", "COMP");


	private final String description;
	private final String abbreviation;


	/**
	 * Get the enumerated constant value with the supplied abbreviation.
	 *
	 * @param abbreviation	The gedcom-spec abbreviation for the month.
	 * @return	The enumerated constant value with the supplied abbreviation, or null if no match is found.
	 */
	public static FrenchRepublicanMonth fromAbbreviation(final String abbreviation){
		for(final FrenchRepublicanMonth month : values())
			if(month.abbreviation.equalsIgnoreCase(abbreviation))
				return month;
		return null;
	}

	FrenchRepublicanMonth(final String description, final String abbreviation){
		this.description = description;
		this.abbreviation = abbreviation;
	}

	public String getDescription(){
		return description;
	}

	public static String[] getDescriptionsWithEmptyValueFirst(){
		final FrenchRepublicanMonth[] values = values();
		final String[] descriptions = new String[values.length];
		for(int i = 0; i < values.length; i ++)
			descriptions[i] = values[i].description;
		return descriptions;
	}

	public static String replaceAll(final String month){
		return StringUtils.replaceEachRepeatedly(month, getAbbreviations(), getDescriptions());
	}

	public static String restoreAll(final String month){
		return StringUtils.replaceEachRepeatedly(month, getDescriptions(), getAbbreviations());
	}

	private static String[] getAbbreviations(){
		final FrenchRepublicanMonth[] values = values();
		final List<String> list = new ArrayList<>(values.length);
		for(final FrenchRepublicanMonth frenchRepublicanMonth : values)
			list.add(frenchRepublicanMonth.abbreviation);
		return list.toArray(new String[0]);
	}

	private static String[] getDescriptions(){
		return Arrays.stream(values())
			.map(FrenchRepublicanMonth::getDescription)
			.toArray(String[]::new);
	}

}
