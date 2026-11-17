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

package io.github.mtrevisan.familylegacy.externalsearch;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;


/**
 * Where and when a site has records: the countries (ISO 3166-1 alpha-2 codes, none for the whole world) and the years (either bound
 * may be open).
 *
 * @param countries	The countries, upper case; empty for the whole world.
 * @param fromYear	The first year, {@code null} if open.
 * @param toYear	The last year, {@code null} if open.
 */
public record SiteCoverage(Set<String> countries, Integer fromYear, Integer toYear){

	/** Every country, every year. */
	public static final SiteCoverage WORLDWIDE = new SiteCoverage(Set.of(), null, null);

	private static final char SEPARATOR = ',';


	public SiteCoverage{
		final Set<String> codes = new TreeSet<>();
		if(countries != null)
			for(final String country : countries){
				final String code = StringUtils.strip(country);
				if(StringUtils.isEmpty(code))
					continue;
				if(!isCountryCode(code))
					throw new IllegalArgumentException("Not a country code: " + code);

				codes.add(code.toUpperCase(Locale.ROOT));
			}
		countries = Collections.unmodifiableSet(codes);
		if(fromYear != null && toYear != null && fromYear > toYear)
			throw new IllegalArgumentException("The first year (" + fromYear + ") is after the last one (" + toYear + ")");
	}

	/**
	 * @param countries	The codes, separated by commas or spaces (e.g. {@code IT, SM, VA}); blank for the whole world.
	 * @param fromYear	The first year, blank if open.
	 * @param toYear	The last year, blank if open.
	 * @return	The coverage.
	 * @throws IllegalArgumentException	If a code is not of two letters, a year is not a number, or the years are reversed.
	 */
	public static SiteCoverage parse(final String countries, final String fromYear, final String toYear){
		final Set<String> codes = new TreeSet<>();
		if(countries != null)
			for(final String code : StringUtils.split(StringUtils.replaceChars(countries, SEPARATOR, ' ')))
				codes.add(code);
		return new SiteCoverage(codes, parseYear(fromYear), parseYear(toYear));
	}

	private static Integer parseYear(final String year){
		if(StringUtils.isBlank(year))
			return null;

		try{
			return Integer.valueOf(year.strip());
		}
		catch(final NumberFormatException e){
			throw new IllegalArgumentException("Not a year: " + year, e);
		}
	}

	/**
	 * @param code	A text.
	 * @return	Whether it is an ISO 3166-1 alpha-2 code in use (any case).
	 */
	public static boolean isCountryCode(final String code){
		return (code != null && code.length() == 2
			&& Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2).contains(code.toUpperCase(Locale.ROOT)));
	}


	/** @return	Whether it covers every country. */
	public boolean isWorldwide(){
		return countries.isEmpty();
	}

	/** @return	The countries, separated by a comma and a space; empty for the whole world. */
	public String countriesText(){
		return String.join(SEPARATOR + " ", countries);
	}

	/** @return	The years ({@code 1500-1950}, {@code 1500-}, {@code -1950}); empty if both are open. */
	public String yearsText(){
		if(fromYear == null && toYear == null)
			return StringUtils.EMPTY;
		return (fromYear != null? fromYear.toString(): StringUtils.EMPTY) + "-" + (toYear != null? toYear.toString(): StringUtils.EMPTY);
	}

	/**
	 * @param personCountries	The countries the person lived in (the codes; empty if not known).
	 * @param personFrom	The first year the person may have been alive, {@code null} if not known.
	 * @param personTo	The last year the person may have been alive, {@code null} if not known.
	 * @return	How likely the site is to have the person.
	 */
	public Relevance relevanceFor(final Set<String> personCountries, final Integer personFrom, final Integer personTo){
		boolean countryMatch = false;
		if(!countries.isEmpty() && personCountries != null && !personCountries.isEmpty()){
			for(final String country : personCountries)
				if(country != null && countries.contains(country.toUpperCase(Locale.ROOT)))
					countryMatch = true;
			if(!countryMatch)
				return Relevance.UNLIKELY;
		}

		//the life is outside the years of the site
		if(toYear != null && personFrom != null && personFrom > toYear)
			return Relevance.UNLIKELY;
		if(fromYear != null && personTo != null && personTo < fromYear)
			return Relevance.UNLIKELY;

		return (countryMatch? Relevance.LIKELY: Relevance.POSSIBLE);
	}

}
