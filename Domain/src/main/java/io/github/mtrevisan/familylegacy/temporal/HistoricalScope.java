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
package io.github.mtrevisan.familylegacy.temporal;


/**
 * How far from the places of a person a historic event (or a cultural norm) happened: the more local, the more it is likely to have
 * touched the person, so the timelines list the local ones first and let the others be hidden.
 * <p>
 * The order of the constants is the order of priority.
 */
public enum HistoricalScope{

	/** In a place of the person, or in a place inside one (a village, a parish, a city). */
	LOCAL("text.historical.scope.local"),
	/** In an area that contains a place of the person and is not a country (a province, a region, a diocese). */
	REGIONAL("text.historical.scope.regional"),
	/** In a country, a state or an empire that contains a place of the person. */
	NATIONAL("text.historical.scope.national"),
	/** With no place at all, or in a continent: a world event. */
	WORLD("text.historical.scope.world"),
	/** In a place that has no relation with the places of the person. */
	ELSEWHERE("text.historical.scope.elsewhere");


	private final String key;


	HistoricalScope(final String key){
		this.key = key;
	}


	/** @return	The key of the label of the scope. */
	public String key(){
		return key;
	}

}
