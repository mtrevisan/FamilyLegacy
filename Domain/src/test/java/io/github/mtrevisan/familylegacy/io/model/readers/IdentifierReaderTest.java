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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The identifiers of a source and of a repository citation: the schemes, the checks, the readings of the labels. */
class IdentifierReaderTest{

	@Test
	void anIsbn10IsCheckedByItsCheckDigit(){
		Assertions.assertTrue(IdentifierReader.isIsbn("0-306-40615-2"));
		Assertions.assertTrue(IdentifierReader.isIsbn("0306406152"));
		//the check digit can be X
		Assertions.assertTrue(IdentifierReader.isIsbn("0-8044-2957-X"));
		Assertions.assertTrue(IdentifierReader.isIsbn("080442957x"));
		Assertions.assertFalse(IdentifierReader.isIsbn("0-306-40615-3"));
	}

	@Test
	void anIsbn13IsCheckedByItsCheckDigit(){
		Assertions.assertTrue(IdentifierReader.isIsbn("978-0-306-40615-7"));
		Assertions.assertTrue(IdentifierReader.isIsbn("9780199535569"));
		Assertions.assertTrue(IdentifierReader.isIsbn("ISBN 978 0 19 953556 9"));
		Assertions.assertTrue(IdentifierReader.isIsbn("ISBN: 9780199535569"));
		Assertions.assertFalse(IdentifierReader.isIsbn("9780199535560"));
	}

	@Test
	void whatIsNotTheLengthOfAnIsbnOrHasOtherCharactersIsNotAnIsbn(){
		Assertions.assertFalse(IdentifierReader.isIsbn("123456789"));
		Assertions.assertFalse(IdentifierReader.isIsbn("97801995355691"));
		Assertions.assertFalse(IdentifierReader.isIsbn("978019953556A"));
		Assertions.assertFalse(IdentifierReader.isIsbn("X306406152"));
		Assertions.assertFalse(IdentifierReader.isIsbn(""));
		Assertions.assertFalse(IdentifierReader.isIsbn(null));
	}

	@Test
	void anIssnIsCheckedByItsCheckDigit(){
		Assertions.assertTrue(IdentifierReader.isIssn("0378-5955"));
		Assertions.assertTrue(IdentifierReader.isIssn("03785955"));
		//the check digit can be X
		Assertions.assertTrue(IdentifierReader.isIssn("2434-561X"));
		Assertions.assertTrue(IdentifierReader.isIssn("2434-561x"));
		Assertions.assertFalse(IdentifierReader.isIssn("0378-5954"));
		Assertions.assertFalse(IdentifierReader.isIssn("0378-59"));
		Assertions.assertFalse(IdentifierReader.isIssn("ABCD-EFGH"));
	}

	@Test
	void aUrlIsAnAbsoluteUriWithNoSpace(){
		Assertions.assertTrue(IdentifierReader.isUrl("https://www.familysearch.org/search/catalog/123"));
		Assertions.assertTrue(IdentifierReader.isUrl("http://example.org/a%20b?x=1#frag"));
		Assertions.assertTrue(IdentifierReader.isUrl("urn:isbn:0451450523"));
		Assertions.assertFalse(IdentifierReader.isUrl("www.example.org"));
		Assertions.assertFalse(IdentifierReader.isUrl("https://example.org/a b"));
		Assertions.assertFalse(IdentifierReader.isUrl("https://exa mple.org"));
		Assertions.assertFalse(IdentifierReader.isUrl("https://"));
		Assertions.assertFalse(IdentifierReader.isUrl(""));
	}

	@Test
	void aDoiIsRecognizedWithOrWithoutItsPrefix(){
		Assertions.assertTrue(IdentifierReader.isDoi("10.1000/182"));
		Assertions.assertTrue(IdentifierReader.isDoi("doi:10.1000/182"));
		Assertions.assertTrue(IdentifierReader.isDoi("https://doi.org/10.1000/182"));
		Assertions.assertFalse(IdentifierReader.isDoi("10.1/182"));
		Assertions.assertFalse(IdentifierReader.isDoi("1000/182"));
	}

	@Test
	void onlyTheSchemesThatHaveARuleAreChecked(){
		Assertions.assertEquals(IdentifierReader.Validity.VALID, IdentifierReader.validity("isbn", "9780199535569"));
		Assertions.assertEquals(IdentifierReader.Validity.INVALID, IdentifierReader.validity("isbn", "9780199535560"));
		Assertions.assertEquals(IdentifierReader.Validity.INVALID, IdentifierReader.validity("ISSN", "0378-5954"));
		Assertions.assertEquals(IdentifierReader.Validity.VALID, IdentifierReader.validity("url", "https://example.org"));
		Assertions.assertEquals(IdentifierReader.Validity.INVALID, IdentifierReader.validity("url", "not a url"));
		for(final String scheme : List.of("call_number", "dewey", "lcc", "udc", "colon", "garside", "fhl_film", "fhl_digital_folder",
				"fhl_ancestral_file", "index_card", "doi", "a custom scheme", ""))
			Assertions.assertEquals(IdentifierReader.Validity.UNCHECKED, IdentifierReader.validity(scheme, "MS 45/12"), scheme);
		Assertions.assertEquals(IdentifierReader.Validity.UNCHECKED, IdentifierReader.validity(null, "MS 45/12"));
		Assertions.assertEquals(IdentifierReader.Validity.UNCHECKED, IdentifierReader.validity("isbn", " "));
	}

	@Test
	void aValueTellsItsSchemeOnlyWhenItIsSure(){
		Assertions.assertEquals("isbn", IdentifierReader.inferScheme("978-0-306-40615-7"));
		Assertions.assertEquals("issn", IdentifierReader.inferScheme("0378-5955"));
		Assertions.assertEquals("doi", IdentifierReader.inferScheme("10.1000/182"));
		Assertions.assertEquals("doi", IdentifierReader.inferScheme("https://doi.org/10.1000/182"));
		Assertions.assertEquals("url", IdentifierReader.inferScheme("https://example.org/book"));
		//a number with a wrong check digit, or a shelf mark, could be anything
		Assertions.assertNull(IdentifierReader.inferScheme("9780199535560"));
		Assertions.assertNull(IdentifierReader.inferScheme("MS 45/12"));
		Assertions.assertNull(IdentifierReader.inferScheme("1234567"));
		Assertions.assertNull(IdentifierReader.inferScheme(" "));
	}

	@Test
	void theNamesThatOtherProgramsGiveToASchemeAreRead(){
		Assertions.assertEquals("isbn", IdentifierReader.schemeOfLabel("ISBN"));
		Assertions.assertEquals("isbn", IdentifierReader.schemeOfLabel("ISBN-13"));
		Assertions.assertEquals("call_number", IdentifierReader.schemeOfLabel("Call number"));
		Assertions.assertEquals("call_number", IdentifierReader.schemeOfLabel("Shelf mark"));
		Assertions.assertEquals("call_number", IdentifierReader.schemeOfLabel("CALN"));
		Assertions.assertEquals("dewey", IdentifierReader.schemeOfLabel("Dewey Decimal"));
		Assertions.assertEquals("lcc", IdentifierReader.schemeOfLabel("Library of Congress Classification"));
		Assertions.assertEquals("fhl_film", IdentifierReader.schemeOfLabel("FHL film number"));
		//a film may be any microfilm, not necessarily one of FamilySearch
		Assertions.assertNull(IdentifierReader.schemeOfLabel("Film"));
		Assertions.assertEquals("fhl_digital_folder", IdentifierReader.schemeOfLabel("digital folder number"));
		//not safely mapped: the caller keeps the name as a custom scheme
		Assertions.assertNull(IdentifierReader.schemeOfLabel("REFN"));
		Assertions.assertNull(IdentifierReader.schemeOfLabel("Barcode"));
		Assertions.assertNull(IdentifierReader.schemeOfLabel(null));
		Assertions.assertNull(IdentifierReader.schemeOfLabel("  "));
	}

	@Test
	void everySchemeOfTheGrammarIsInTheVocabulary(){
		Assertions.assertEquals(14, IdentifierReader.SCHEMES.length);
		Assertions.assertEquals(14, List.of(IdentifierReader.SCHEMES).stream().distinct().count());
		for(final String scheme : IdentifierReader.SOURCE_SCHEMES)
			Assertions.assertTrue(List.of(IdentifierReader.SCHEMES).contains(scheme), scheme);
		for(final String scheme : IdentifierReader.REPOSITORY_SCHEMES)
			Assertions.assertTrue(List.of(IdentifierReader.SCHEMES).contains(scheme), scheme);
	}

	@Test
	void twoWritingsOfTheSameIdentifierHaveTheSameKey(){
		Assertions.assertEquals(IdentifierReader.normalizedKey("isbn", "0-306-40615-2"), IdentifierReader.normalizedKey("ISBN", "ISBN 0306406152"));
		Assertions.assertEquals(IdentifierReader.normalizedKey("doi", "doi:10.1000/ABC"), IdentifierReader.normalizedKey("doi", "https://doi.org/10.1000/abc"));
		Assertions.assertEquals(IdentifierReader.normalizedKey("url", "https://Example.org/a/"), IdentifierReader.normalizedKey("url", "https://example.org/a"));
		Assertions.assertEquals(IdentifierReader.normalizedKey("call_number", "MS  45/12"), IdentifierReader.normalizedKey("call_number", "ms 45/12"));
		//an ISBN-10 is the ISBN-13 that begins with 978: the same book, written in the old way
		Assertions.assertEquals(IdentifierReader.normalizedKey("isbn", "978-0-306-40615-7"), IdentifierReader.normalizedKey("isbn", "0-306-40615-2"));
		Assertions.assertNotEquals(IdentifierReader.normalizedKey("call_number", "MS 45/12"), IdentifierReader.normalizedKey("dewey", "MS 45/12"));
		Assertions.assertEquals("", IdentifierReader.normalizedKey("isbn", " "));
		//a garbled ISBN keeps its own text, so that two different garbled ones do not collide
		Assertions.assertNotEquals(IdentifierReader.normalizedKey("isbn", "abc"), IdentifierReader.normalizedKey("isbn", "abd"));
	}

	@Test
	void theIdentifiersOfARecordAreReadInOrderAndByScheme(){
		final FLEFModel model = new FLEFParser().parse("""
			source S1 {
				title.value Rossi Family History
				identifier {
					scheme isbn
					value 9780199535569
				}
				identifier {
					scheme doi
					value 10.1000/182
				}
				identifier {
					scheme ISBN
					value 0-306-40615-2
				}
				audit.creation.date 2020-01-01T00:00:00Z
			}
			""");
		final FLEFRecord source = model.getRecordById("S1");

		final List<FLEFRecord> identifiers = SourceReader.extractIdentifiers(source);
		Assertions.assertEquals(3, identifiers.size());
		Assertions.assertEquals("isbn", IdentifierReader.extractScheme(identifiers.getFirst()));
		Assertions.assertEquals("10.1000/182", IdentifierReader.extractValue(identifiers.get(1)));
		Assertions.assertEquals(List.of("9780199535569", "0-306-40615-2"), IdentifierReader.extractValues(source, "isbn"));
		Assertions.assertEquals("isbn: 9780199535569", IdentifierReader.displayText(identifiers.getFirst()));
	}

	@Test
	void aRepositoryCitationHasItsIdentifiersApartFromItsLocator(){
		final FLEFModel model = new FLEFParser().parse("""
			source S1 {
				title.value Register
				repository {
					repository R1
					identifier {
						scheme call_number
						value MS 45/12
					}
					identifier {
						scheme dewey
						value 929.2 ROS
					}
					locator Page 27
				}
				audit.creation.date 2020-01-01T00:00:00Z
			}
			""");
		final FLEFRecord citation = FLEFRecordHelper.findChild(model.getRecordById("S1"), SourceReader.TAG_REPOSITORY);

		final List<FLEFRecord> identifiers = RepositoryCitationReader.extractIdentifiers(citation);
		Assertions.assertEquals(List.of("MS 45/12"), IdentifierReader.extractValues(citation, IdentifierReader.ENUM_SCHEME_CALL_NUMBER));
		Assertions.assertEquals(2, identifiers.size());
		Assertions.assertEquals("Page 27", FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_LOCATOR));
		//the source itself has none of them
		Assertions.assertTrue(SourceReader.extractIdentifiers(model.getRecordById("S1")).isEmpty());
	}

	@Test
	void aCreatedIdentifierWritesTheSchemeBeforeTheValue(){
		final FLEFRecord identifier = IdentifierReader.create("fhl_film", "1234567");

		Assertions.assertEquals("identifier", identifier.getTag());
		Assertions.assertEquals(IdentifierReader.TAG_SCHEME, identifier.getChildren().get(0).getTag());
		Assertions.assertEquals(IdentifierReader.TAG_VALUE, identifier.getChildren().get(1).getTag());
	}

}
