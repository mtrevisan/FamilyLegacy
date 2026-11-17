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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


class UrlTemplateTest{

	private static SearchSubject subject(){
		final Map<Placeholder, String> values = new EnumMap<>(Placeholder.class);
		values.put(Placeholder.NAME_GIVEN, "Giovanni Battista");
		values.put(Placeholder.SURNAME, "D'Àlvise");
		values.put(Placeholder.BIRTH_YEAR, "1805");
		values.put(Placeholder.BIRTH_PLACE, "Treviso & Co");
		return SearchSubject.of(values);
	}


	@Test
	void theValuesAreEncodedAndAMissingOneIsEmpty() throws UrlTemplate.TemplateException{
		final UrlTemplate template = UrlTemplate.parse("https://example.org/s?first={name_given}&last={surname}&by={birth_year}"
			+ "&bp={birth_place}&dy={death_year}");

		Assertions.assertEquals("https://example.org/s?first=Giovanni%20Battista&last=D%27%C3%80lvise&by=1805&bp=Treviso%20%26%20Co&dy=",
			template.expand(subject(), name -> null));
		Assertions.assertEquals(Set.of(Placeholder.NAME_GIVEN, Placeholder.SURNAME, Placeholder.BIRTH_YEAR, Placeholder.BIRTH_PLACE,
			Placeholder.DEATH_YEAR), template.placeholders());
	}

	@Test
	void theNamesOfThePlaceholdersIgnoreCaseAndSpaces() throws UrlTemplate.TemplateException{
		final UrlTemplate template = UrlTemplate.parse("https://example.org/{ SURNAME }");

		Assertions.assertEquals("https://example.org/D%27%C3%80lvise", template.expand(subject(), name -> null));
	}

	@Test
	void aBraceOfTheAddressIsWrittenTwice() throws UrlTemplate.TemplateException{
		final UrlTemplate template = UrlTemplate.parse("https://example.org/q?json={{%22n%22:%22{surname}%22}}");

		Assertions.assertEquals("https://example.org/q?json=%7B%22n%22:%22D%27%C3%80lvise%22%7D", template.expand(subject(), name -> null));
	}

	@Test
	void aCredentialIsFilledInTheAddressAndHiddenInTheOneShown() throws UrlTemplate.TemplateException{
		final UrlTemplate template = UrlTemplate.parse("https://api.example.org/search?q={surname}&key={credential:api_key}");

		Assertions.assertEquals(Set.of("api_key"), template.credentials());
		Assertions.assertEquals("https://api.example.org/search?q=D%27%C3%80lvise&key=s%2Fcr%20t",
			template.expand(subject(), name -> "s/cr t"));
		Assertions.assertEquals("https://api.example.org/search?q=D%27%C3%80lvise&key=" + UrlTemplate.HIDDEN_CREDENTIAL,
			template.expandForDisplay(subject()));
	}

	@Test
	void anInvalidTemplateSaysWhy(){
		final Map<String, UrlTemplate.Problem> cases = Map.of(
			"  ", UrlTemplate.Problem.EMPTY,
			"ftp://example.org/{surname}", UrlTemplate.Problem.NOT_WEB,
			"javascript:alert({surname})", UrlTemplate.Problem.NOT_WEB,
			"https://example.org/{surname", UrlTemplate.Problem.UNCLOSED_BRACE,
			"https://example.org/{sur{name}}", UrlTemplate.Problem.UNCLOSED_BRACE,
			"https://example.org/{nickname}", UrlTemplate.Problem.UNKNOWN_PLACEHOLDER,
			"https://example.org/{credential:}", UrlTemplate.Problem.BAD_CREDENTIAL_NAME,
			"https://example.org/a b/{surname}", UrlTemplate.Problem.INVALID_ADDRESS,
			"https:///{surname}", UrlTemplate.Problem.INVALID_ADDRESS);
		for(final Map.Entry<String, UrlTemplate.Problem> entry : cases.entrySet()){
			final UrlTemplate.TemplateException e = Assertions.assertThrows(UrlTemplate.TemplateException.class,
				() -> UrlTemplate.parse(entry.getKey()), entry.getKey());
			Assertions.assertEquals(entry.getValue(), e.problem(), entry.getKey());
			Assertions.assertFalse(e.describe().isBlank());
		}
	}

	@Test
	void everyPlaceholderCanBeWritten() throws UrlTemplate.TemplateException{
		final StringBuilder text = new StringBuilder("https://example.org/?");
		for(final Placeholder placeholder : Placeholder.values())
			text.append(placeholder.token()).append('=').append(placeholder.written()).append('&');

		Assertions.assertEquals(Set.of(Placeholder.values()), UrlTemplate.parse(text.toString()).placeholders());
		Assertions.assertEquals(List.of(Placeholder.values()).size(), Placeholder.values().length);
	}

}
