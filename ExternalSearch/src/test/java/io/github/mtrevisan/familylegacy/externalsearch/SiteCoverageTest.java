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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;


class SiteCoverageTest{

	@Test
	void theCountriesAndTheYearsAreReadFromText(){
		final SiteCoverage coverage = SiteCoverage.parse("it, sm VA", "1800", " ");

		Assertions.assertEquals(Set.of("IT", "SM", "VA"), coverage.countries());
		Assertions.assertEquals(1800, coverage.fromYear());
		Assertions.assertNull(coverage.toYear());
		Assertions.assertEquals("IT, SM, VA", coverage.countriesText());
		Assertions.assertEquals("1800-", coverage.yearsText());
	}

	@Test
	void whatIsNotACoverageIsRefused(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> SiteCoverage.parse("Italy", null, null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> SiteCoverage.parse("XX", null, null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> SiteCoverage.parse(null, "18th", null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> SiteCoverage.parse(null, "1900", "1800"));
	}

	@Test
	void aSiteOfTheCountryAndTheYearsOfThePersonIsLikely(){
		final SiteCoverage italy = SiteCoverage.parse("IT", "1800", "1945");

		Assertions.assertEquals(Relevance.LIKELY, italy.relevanceFor(Set.of("IT"), 1805, 1870));
		//one of the countries of the person is enough (an emigrant)
		Assertions.assertEquals(Relevance.LIKELY, italy.relevanceFor(Set.of("US", "IT"), 1805, 1870));
	}

	@Test
	void aSiteOfAnotherCountryOrOtherYearsIsUnlikely(){
		final SiteCoverage italy = SiteCoverage.parse("IT", "1800", "1945");

		Assertions.assertEquals(Relevance.UNLIKELY, italy.relevanceFor(Set.of("FR"), 1805, 1870));
		Assertions.assertEquals(Relevance.UNLIKELY, italy.relevanceFor(Set.of("IT"), 1650, 1720));
		Assertions.assertEquals(Relevance.UNLIKELY, italy.relevanceFor(Set.of(), 1950, 2010));
	}

	@Test
	void whenNothingTellsTheSiteIsPossible(){
		Assertions.assertEquals(Relevance.POSSIBLE, SiteCoverage.WORLDWIDE.relevanceFor(Set.of("IT"), 1805, 1870));
		Assertions.assertEquals(Relevance.POSSIBLE, SiteCoverage.parse("IT", null, null).relevanceFor(Set.of(), null, null));
		//a life that overlaps the years of the site only in part may be there
		Assertions.assertEquals(Relevance.POSSIBLE, SiteCoverage.parse(null, "1800", "1945").relevanceFor(Set.of(), 1780, 1850));
	}

}
