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
import io.github.mtrevisan.familylegacy.quality.DuplicateFinderService;
import io.github.mtrevisan.familylegacy.quality.NameSimilarity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;


/** The similarity of personal names by the letters, with no list of names: spelling variants, initials, the consonant skeleton. */
class NameSimilarityTest{

	private static List<DuplicateFinderService.Candidate> find(final String... individuals) throws IOException{
		final StringBuilder gedcom = new StringBuilder("0 HEAD\n1 CHAR UTF-8\n");
		for(int i = 0; i < individuals.length; i ++)
			gedcom.append("0 @I").append(i + 1).append("@ INDI\n1 NAME ").append(individuals[i])
				.append("\n1 SEX ").append(individuals[i].contains("Cat") || individuals[i].contains("Kat")? "F": "M")
				.append("\n1 BIRT\n2 DATE 1 JAN 1850\n");
		final FLEFModel model = new GEDCOMToFLEFConverter().convert(new GEDCOMParser().parse(new StringReader(gedcom.toString())));
		//the names alone and the birth are under the default threshold of the search: it is lowered to look at the name
		final DuplicateFinderService.Options options = new DuplicateFinderService.Options();
		options.threshold = 30;
		return DuplicateFinderService.find(model, options);
	}

	private static DuplicateFinderService.FeatureMatch name(final DuplicateFinderService.Candidate candidate){
		return candidate.matches().stream()
			.filter(match -> "Name".equals(match.feature()))
			.findFirst()
			.orElse(null);
	}


	@Test
	void theLevelOfTwoNames(){
		Assertions.assertEquals(NameSimilarity.Level.EXACT, NameSimilarity.level("giovanni", "giovanni"));
		Assertions.assertEquals(NameSimilarity.Level.SIMILAR, NameSimilarity.level("giovanni", "giovani"));
		Assertions.assertEquals(NameSimilarity.Level.SIMILAR, NameSimilarity.level("stephan", "stefan"));
		//the consonants are the same although the first letter is not
		Assertions.assertEquals(NameSimilarity.Level.SIMILAR, NameSimilarity.level("catarina", "katarina"));
		Assertions.assertEquals(NameSimilarity.Level.NONE, NameSimilarity.level("marco", "mario"));
		Assertions.assertEquals(NameSimilarity.Level.NONE, NameSimilarity.level("giovanni", "pietro"));
		//too short to tell a typing error from another name
		Assertions.assertEquals(NameSimilarity.Level.NONE, NameSimilarity.level("ugo", "ugu"));
		//an initial agrees with the names that begin with it, weakly
		Assertions.assertEquals(NameSimilarity.Level.INITIAL, NameSimilarity.level("g", "giovanni"));
		Assertions.assertEquals(NameSimilarity.Level.NONE, NameSimilarity.level("g", "pietro"));
		Assertions.assertEquals(NameSimilarity.Level.NONE, NameSimilarity.level("", "pietro"));
	}

	@Test
	void theBestMatchOfTwoSetsPrefersTheSameName(){
		final NameSimilarity.Match match = NameSimilarity.best(Set.of("giovani", "pietro"), Set.of("giovanni", "pietro"));

		Assertions.assertEquals(NameSimilarity.Level.EXACT, match.level());
		Assertions.assertEquals("pietro", match.first());
		Assertions.assertFalse(NameSimilarity.best(Set.of("marco"), Set.of("paolo")).agrees());
		Assertions.assertFalse(NameSimilarity.best(Set.of(), Set.of("paolo")).agrees());
	}

	@Test
	void theSkeletonKeepsTheConsonantsAndTheLettersThatSoundAlike(){
		Assertions.assertEquals("ktrn", NameSimilarity.skeleton("catarina"));
		Assertions.assertEquals(NameSimilarity.skeleton("catarina"), NameSimilarity.skeleton("katarina"));
		Assertions.assertEquals("gvn", NameSimilarity.skeleton("giovanni"));
		Assertions.assertEquals(NameSimilarity.skeleton("philip"), NameSimilarity.skeleton("filip"));
	}

	@Test
	void aSpellingVariantOfTheGivenNameIsAPartialMatch() throws IOException{
		final List<DuplicateFinderService.Candidate> candidates = find("Giovanni /Rossi/", "Giovani /Rossi/");

		Assertions.assertEquals(1, candidates.size());
		final DuplicateFinderService.FeatureMatch name = name(candidates.getFirst());
		Assertions.assertEquals(20, name.score());
		Assertions.assertTrue(name.detail().contains("giovanni / giovani"), name.detail());
	}

	@Test
	void anotherNameIsNotAVariant() throws IOException{
		//only the family name is the same: 10 points and the birth 15, under the threshold of 30 used here, and no similarity is claimed
		Assertions.assertTrue(find("Marco /Rossi/", "Mario /Rossi/").isEmpty());
	}

	@Test
	void anInitialIsAWeakPartialMatch() throws IOException{
		final List<DuplicateFinderService.Candidate> candidates = find("G. /Rossi/", "Giovanni /Rossi/");

		Assertions.assertEquals(15, name(candidates.getFirst()).score());
	}

	@Test
	void theSpellingsThatDifferInTheFirstLetterAreComparedThroughTheConsonants() throws IOException{
		//no two names begin with the same two letters: only the consonants put the two records in the same block
		final List<DuplicateFinderService.Candidate> candidates = find("Catarina /Cattaneo/", "Katarina /Kattaneo/");

		Assertions.assertEquals(1, candidates.size());
		Assertions.assertEquals(20, name(candidates.getFirst()).score());
	}

	@Test
	void theSameNameStillScoresTheMost() throws IOException{
		final List<DuplicateFinderService.Candidate> candidates = find("Giovanni /Rossi/", "Giovanni /Rossi/");

		Assertions.assertEquals(40, name(candidates.getFirst()).score());
	}

}
