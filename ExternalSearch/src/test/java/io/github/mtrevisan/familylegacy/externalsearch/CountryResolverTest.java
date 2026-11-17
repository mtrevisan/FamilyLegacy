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

import java.util.List;
import java.util.Optional;


class CountryResolverTest{

	@Test
	void aCountryIsKnownByItsNameInSeveralLanguages(){
		Assertions.assertEquals(Optional.of("IT"), CountryResolver.resolve("Italy"));
		Assertions.assertEquals(Optional.of("IT"), CountryResolver.resolve("  ITALIA "));
		Assertions.assertEquals(Optional.of("DE"), CountryResolver.resolve("Deutschland"));
		Assertions.assertEquals(Optional.of("FR"), CountryResolver.resolve("France"));
		Assertions.assertEquals(Optional.of("AT"), CountryResolver.resolve("Österreich"));
		Assertions.assertEquals(Optional.of("AT"), CountryResolver.resolve("Osterreich"));
	}

	@Test
	void historicalAndCommonNamesAreKnown(){
		Assertions.assertEquals(Optional.of("GB"), CountryResolver.resolve("England"));
		Assertions.assertEquals(Optional.of("US"), CountryResolver.resolve("U.S.A."));
		Assertions.assertEquals(Optional.of("DE"), CountryResolver.resolve("Preussen"));
		Assertions.assertEquals(Optional.of("DE"), CountryResolver.resolve("Preußen"));
		Assertions.assertEquals(Optional.of("CZ"), CountryResolver.resolve("Bohemia"));
		Assertions.assertEquals(Optional.of("IT"), CountryResolver.resolve("Regno d'Italia"));
	}

	@Test
	void whatIsNotACountryTellsNothing(){
		Assertions.assertEquals(Optional.empty(), CountryResolver.resolve("Treviso"));
		Assertions.assertEquals(Optional.empty(), CountryResolver.resolve(""));
		Assertions.assertEquals(Optional.empty(), CountryResolver.resolve(null));
		//the code alone is not a place
		Assertions.assertEquals(Optional.empty(), CountryResolver.resolve("IT"));
	}

	@Test
	void theOutermostLevelThatNamesACountryIsTaken(){
		final List<List<String>> levels = List.of(List.of("Borgo"), List.of("Treviso"), List.of("Veneto"), List.of("Regno d'Italia", "Italia"));

		Assertions.assertEquals(Optional.of("IT"), CountryResolver.resolveLevels(levels));
		Assertions.assertEquals(Optional.empty(), CountryResolver.resolveLevels(List.of(List.of("Borgo"), List.of("Treviso"))));
	}

}
