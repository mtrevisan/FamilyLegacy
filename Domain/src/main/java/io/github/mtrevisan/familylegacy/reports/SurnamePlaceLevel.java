/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;

import java.util.Set;


/** The level of the place hierarchy the Surnames by Place report groups the surnames by. */
public enum SurnamePlaceLevel{

	/** The place of the event, whatever it is (a hamlet, a street). */
	PLACE(Set.of()),
	/** The town, village, city or municipality that contains the place. */
	MUNICIPALITY(Set.of(PlaceReader.ENUM_TYPE_MUNICIPALITY, PlaceReader.ENUM_TYPE_TOWN, PlaceReader.ENUM_TYPE_CITY, PlaceReader.ENUM_TYPE_VILLAGE)),
	/** The province, county or department that contains the place. */
	PROVINCE(Set.of(PlaceReader.ENUM_TYPE_PROVINCE, PlaceReader.ENUM_TYPE_COUNTY, PlaceReader.ENUM_TYPE_DEPARTMENT)),
	/** The region or state that contains the place. */
	REGION(Set.of(PlaceReader.ENUM_TYPE_REGION, PlaceReader.ENUM_TYPE_STATE, PlaceReader.ENUM_TYPE_MACRO_REGION)),
	/** The country or empire that contains the place. */
	COUNTRY(Set.of(PlaceReader.ENUM_TYPE_COUNTRY, PlaceReader.ENUM_TYPE_EMPIRE));


	private final Set<String> types;


	SurnamePlaceLevel(final Set<String> types){
		this.types = types;
	}


	/** @return	The types of place that are of this level (empty for the place itself). */
	public Set<String> types(){
		return types;
	}

}
