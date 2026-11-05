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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.quality.DuplicateFinderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;


/**
 * The duplicate finder compares by name what has no record in common: a tree that is in the file twice (an import of two copies) shares
 * no parent, spouse or place record, only ones with the same name.
 */
class IdentitySecondPassTest{

	/** The same family twice, with different identifiers: Mario and his parents, his wife, his occupation. */
	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1850
		1 OCCU Farmer
		1 FAMC @F1@
		1 FAMS @F2@
		0 @I2@ INDI
		1 NAME Giovanni /Rossi/
		1 SEX M
		1 FAMS @F1@
		0 @I3@ INDI
		1 NAME Anna /Bianchi/
		1 SEX F
		1 FAMS @F1@
		0 @I4@ INDI
		1 NAME Maria /Verdi/
		1 SEX F
		1 FAMS @F2@
		0 @F1@ FAM
		1 HUSB @I2@
		1 WIFE @I3@
		1 CHIL @I1@
		0 @F2@ FAM
		1 HUSB @I1@
		1 WIFE @I4@
		0 @I5@ INDI
		1 NAME Mario /Rossi/
		1 SEX M
		1 BIRT
		2 DATE 1 JAN 1850
		1 OCCU farmer
		1 FAMC @F3@
		1 FAMS @F4@
		0 @I6@ INDI
		1 NAME Giovanni /Rossi/
		1 SEX M
		1 FAMS @F3@
		0 @I7@ INDI
		1 NAME Anna /Bianchi/
		1 SEX F
		1 FAMS @F3@
		0 @I8@ INDI
		1 NAME Maria /Verdi/
		1 SEX F
		1 FAMS @F4@
		0 @F3@ FAM
		1 HUSB @I6@
		1 WIFE @I7@
		1 CHIL @I5@
		0 @F4@ FAM
		1 HUSB @I5@
		1 WIFE @I8@
		""";


	private static FLEFModel convert() throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(GEDCOM));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	/** @return	The pair of the two Marios, whatever the identifiers that the conversion gave them. */
	private static DuplicateFinderService.Candidate marios(final FLEFModel model){
		return DuplicateFinderService.find(model, new DuplicateFinderService.Options()).stream()
			.filter(candidate -> isMario(model, candidate.left().id()) && isMario(model, candidate.right().id()))
			.findFirst()
			.orElseThrow();
	}

	private static boolean isMario(final FLEFModel model, final String id){
		return IndividualReader.extractPrimaryFullname(model.getRecordById(id)).contains("Mario");
	}

	private static boolean has(final DuplicateFinderService.Candidate candidate, final String feature){
		return candidate.matches().stream()
			.anyMatch(match -> feature.equals(match.feature()));
	}


	@Test
	void theSameFamilyTwiceIsFoundThroughTheNamesOfParentsSpousesAndOccupation() throws IOException{
		final DuplicateFinderService.Candidate mario = marios(convert());

		Assertions.assertTrue(has(mario, "Parents (by name)"), mario.matches().toString());
		Assertions.assertTrue(has(mario, "Spouses (by name)"), mario.matches().toString());
		Assertions.assertTrue(has(mario, "Occupations"), mario.matches().toString());
		//no record is in common, so the first pass found none of them
		Assertions.assertFalse(has(mario, "Parents"), mario.matches().toString());
		//the name (40), the birth (15), the parents (12), the spouses (8) and the occupation (4): above the 65 of the name and the dates
		Assertions.assertTrue(mario.score() >= 79, String.valueOf(mario.score()));
	}

	@Test
	void parentsWithDifferentNamesAreNotTakenForTheSame() throws IOException{
		final DuplicateFinderService.Candidate mario = marios(convert());

		Assertions.assertFalse(mario.left().parentNames().isEmpty());
		//Giovanni Rossi and Anna Bianchi against another couple: no agreement by name
		Assertions.assertNull(DuplicateFinderService.agreeingNames(mario.left().parentNames(), Set.of("pietro ferrari", "lucia neri"),
			DuplicateFinderService.NameKind.PERSON));
	}

	@Test
	void theRulesOfAgreementDependOnWhatIsCompared(){
		//a single given name is not a full name: Giovanni is not Giovanni Rossi
		Assertions.assertNull(DuplicateFinderService.agreeingNames(Set.of("giovanni"), Set.of("giovanni rossi"),
			DuplicateFinderService.NameKind.PERSON));
		Assertions.assertNotNull(DuplicateFinderService.agreeingNames(Set.of("giovanni rossi"), Set.of("giovanni battista rossi"),
			DuplicateFinderService.NameKind.PERSON));
		//a spelling variant
		Assertions.assertNotNull(DuplicateFinderService.agreeingNames(Set.of("giovanni rossi"), Set.of("giovani rossi"),
			DuplicateFinderService.NameKind.PERSON));
		//a place with more words is another place
		Assertions.assertNull(DuplicateFinderService.agreeingNames(Set.of("san martino"), Set.of("san martino di lupari"),
			DuplicateFinderService.NameKind.PLACE));
		Assertions.assertNotNull(DuplicateFinderService.agreeingNames(Set.of("treviso"), Set.of("treviso"),
			DuplicateFinderService.NameKind.PLACE));
		//an occupation contained in another agrees
		Assertions.assertNotNull(DuplicateFinderService.agreeingNames(Set.of("farmer"), Set.of("farmer day laborer"),
			DuplicateFinderService.NameKind.TEXT));
		Assertions.assertNull(DuplicateFinderService.agreeingNames(Set.of(), Set.of("farmer"), DuplicateFinderService.NameKind.TEXT));
	}

}
