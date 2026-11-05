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
package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.quality.DuplicateFinderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;


/** The rest of the second pass: the children by name, the attributes (other than the occupations) by text, the sources by title. */
class IdentitySecondPassMoreTest{

	/** Mario twice: the same child, the same residence, a source with the same title but another record. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @S1@ SOUR
		1 TITL Parish register of Treviso
		0 @S2@ SOUR
		1 TITL Parish register of Treviso
		0 @I1@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1850
		1 RESI Treviso
		1 SOUR @S1@
		1 FAMS @F1@
		0 @I2@ INDI
		1 NAME Pietro /Rossi/
		1 SEX M
		1 FAMC @F1@
		0 @F1@ FAM
		1 HUSB @I1@
		1 CHIL @I2@
		0 @I3@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1850
		1 RESI treviso
		1 SOUR @S2@
		1 FAMS @F2@
		0 @I4@ INDI
		1 NAME Pietro /Rossi/
		1 SEX M
		1 FAMC @F2@
		0 @F2@ FAM
		1 HUSB @I3@
		1 CHIL @I4@
		""";


	private static DuplicateFinderService.Candidate marios() throws IOException{
		final FLEFModel model = new GEDCOMToFLEFConverter().convert(new GEDCOMParser().parse(new StringReader(GEDCOM)));
		return DuplicateFinderService.find(model, new DuplicateFinderService.Options()).stream()
			.filter(candidate -> IndividualReader.extractPrimaryFullname(model.getRecordById(candidate.left().id())).contains("Mario")
				&& IndividualReader.extractPrimaryFullname(model.getRecordById(candidate.right().id())).contains("Mario"))
			.findFirst()
			.orElseThrow();
	}

	private static boolean has(final DuplicateFinderService.Candidate candidate, final String feature){
		return candidate.matches().stream()
			.anyMatch(match -> feature.equals(match.feature()));
	}


	@Test
	void theChildrenTheResidenceAndTheSourceTitleAreComparedByName() throws IOException{
		final DuplicateFinderService.Candidate mario = marios();

		Assertions.assertTrue(has(mario, "Children (by name)"), mario.matches().toString());
		Assertions.assertTrue(has(mario, "Attributes (by text)"), mario.matches().toString());
		Assertions.assertTrue(has(mario, "Source titles"), mario.matches().toString());
		//no record is in common, so the first pass found none of them
		Assertions.assertFalse(has(mario, "Children"), mario.matches().toString());
		Assertions.assertFalse(has(mario, "Sources"), mario.matches().toString());
		//the name (40), the birth (15), the children (8), the residence (3) and the source title (3)
		Assertions.assertTrue(mario.score() >= 69, String.valueOf(mario.score()));
	}

	@Test
	void anAttributeAgreesOnlyWithTheSameTypeOfAttribute(){
		Assertions.assertNull(DuplicateFinderService.agreeingAttributeTexts(Map.of("residence", Set.of("treviso")),
			Map.of("religion", Set.of("treviso"))));
		Assertions.assertArrayEquals(new String[]{"residence", "treviso", "treviso"},
			DuplicateFinderService.agreeingAttributeTexts(Map.of("residence", Set.of("treviso")), Map.of("residence", Set.of("treviso"))));
		Assertions.assertNull(DuplicateFinderService.agreeingAttributeTexts(Map.of(), Map.of("residence", Set.of("treviso"))));
	}

	@Test
	void aTitleDoesNotAgreeBecauseItIsPartOfAnother(){
		//the registers of the baptisms and the registers of the baptisms of Treviso are different works
		Assertions.assertNull(DuplicateFinderService.agreeingNames(Set.of("register of baptisms"),
			Set.of("register of baptisms of treviso"), DuplicateFinderService.NameKind.TITLE));
		Assertions.assertNotNull(DuplicateFinderService.agreeingNames(Set.of("register of baptisms"), Set.of("register of baptisms"),
			DuplicateFinderService.NameKind.TITLE));
		//a text, instead, agrees with the longer one
		Assertions.assertNotNull(DuplicateFinderService.agreeingNames(Set.of("register of baptisms"),
			Set.of("register of baptisms of treviso"), DuplicateFinderService.NameKind.TEXT));
	}

}
