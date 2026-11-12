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
package io.github.mtrevisan.familylegacy.consanguinity;

import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;


/** The coefficients of Wright, on the pedigrees whose values are known. */
class ConsanguinityCalculatorTest{

	private static final double EPSILON = 1e-12;


	/** The parents by child: {@code graph("c", "f", "m", "d", "f", "m")} says that c and d are children of f and m. */
	private static AncestryGraph graph(final String... childAndParents){
		final Map<String, List<String>> parents = new LinkedHashMap<>();
		for(int i = 0; i < childAndParents.length; i += 3)
			parents.put(childAndParents[i], List.of(childAndParents[i + 1], childAndParents[i + 2]));
		return new AncestryGraph(parents);
	}

	/** X (of A and WA) and Y (of B and WB), where A and B are children of G1 and G2. */
	private static AncestryGraph firstCousins(){
		return graph("A", "G1", "G2", "B", "G1", "G2", "X", "A", "WA", "Y", "B", "WB");
	}


	@Test
	void siblingsHaveRelationshipOneHalf(){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("A", "F", "M", "B", "F", "M"));

		Assertions.assertEquals(0.25, calculator.kinship("A", "B"), EPSILON);
		Assertions.assertEquals(0.5, calculator.relationship("A", "B"), EPSILON);
	}

	@Test
	void aParentAndAChildHaveRelationshipOneHalf(){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("C", "F", "M"));

		Assertions.assertEquals(0.5, calculator.relationship("F", "C"), EPSILON);
		Assertions.assertEquals(0.5, calculator.relationship("C", "M"), EPSILON);
		Assertions.assertEquals(0., calculator.relationship("F", "M"), EPSILON);
	}

	@Test
	void anUncleAndANieceHaveRelationshipOneQuarter(){
		//N is the child of B, and B is the sibling of A
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("A", "G1", "G2", "B", "G1", "G2", "N", "B", "W"));

		Assertions.assertEquals(0.25, calculator.relationship("A", "N"), EPSILON);
	}

	@Test
	void firstCousinsHaveRelationshipOneEighth(){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(firstCousins());

		Assertions.assertEquals(0.125, calculator.relationship("X", "Y"), EPSILON);
		Assertions.assertEquals(0.0625, calculator.kinship("X", "Y"), EPSILON);
	}

	@Test
	void theChildOfFirstCousinsHasInbreedingOneSixteenth(){
		final AncestryGraph graph = graph("A", "G1", "G2", "B", "G1", "G2", "X", "A", "WA", "Y", "B", "WB", "Z", "X", "Y");
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph);

		Assertions.assertEquals(0.0625, calculator.inbreeding("Z"), EPSILON);
		Assertions.assertEquals(0., calculator.inbreeding("X"), EPSILON);
	}

	@Test
	void theChildOfSiblingsHasInbreedingOneQuarter(){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("A", "F", "M", "B", "F", "M", "C", "A", "B"));

		Assertions.assertEquals(0.25, calculator.inbreeding("C"), EPSILON);
	}

	@Test
	void theChildOfHalfSiblingsHasInbreedingOneEighth(){
		//A and B have the same father and different mothers
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("A", "F", "M1", "B", "F", "M2", "C", "A", "B"));

		Assertions.assertEquals(0.125, calculator.inbreeding("C"), EPSILON);
	}

	@Test
	void doubleFirstCousinsHaveKinshipOneEighth(){
		//two brothers married two sisters: their children are double first cousins
		final AncestryGraph graph = graph("B1", "G1", "G2", "B2", "G1", "G2", "S1", "H1", "H2", "S2", "H1", "H2",
			"X", "B1", "S1", "Y", "B2", "S2");
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph);

		Assertions.assertEquals(0.125, calculator.kinship("X", "Y"), EPSILON);
		Assertions.assertEquals(0.25, calculator.relationship("X", "Y"), EPSILON);
	}

	@Test
	void theInbreedingOfAnAncestorCountsInTheRelationship(){
		//C is the child of siblings: with its parent A the coefficient is 0.75 / sqrt(1.25), not 1/2
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("A", "F", "M", "B", "F", "M", "C", "A", "B"));

		Assertions.assertEquals(0.375, calculator.kinship("A", "C"), EPSILON);
		Assertions.assertEquals(0.75 / Math.sqrt(1.25), calculator.relationship("A", "C"), EPSILON);
	}

	@Test
	void everyPathCountsNotOnlyTheShortestOne(){
		//P and Q are half-siblings through G3 and, at the same time, cousins through G1 and G2: more than one path joins X and Y
		final AncestryGraph graph = graph("A", "G1", "G2", "B", "G1", "G2", "P", "A", "G3", "Q", "B", "G3",
			"X", "P", "WP", "Y", "Q", "WQ");
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph);

		//P and Q: phi = 3/16 (half-siblings 1/8, plus first cousins 1/16); X and Y are a quarter of that
		Assertions.assertEquals(3. / 64., calculator.kinship("X", "Y"), EPSILON);
	}

	@Test
	void unrelatedIndividualsHaveNoCoefficient(){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("A", "F1", "M1", "B", "F2", "M2"));

		Assertions.assertEquals(0., calculator.kinship("A", "B"), EPSILON);
		Assertions.assertEquals(0., calculator.relationship("A", "B"), EPSILON);
		Assertions.assertFalse(calculator.areRelated("A", "B"));
		Assertions.assertTrue(calculator.closestCommonAncestors("A", "B").isEmpty());
	}

	@Test
	void anUnknownParentMeansNoInbreeding(){
		final AncestryGraph graph = new AncestryGraph(Map.of("A", List.of("F"), "C", List.of("A")));
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph);

		Assertions.assertEquals(0., calculator.inbreeding("A"), EPSILON);
		Assertions.assertEquals(0., calculator.inbreeding("C"), EPSILON);
		Assertions.assertEquals(0., calculator.inbreeding("nobody"), EPSILON);
	}

	@Test
	void anIndividualWithItselfHasRelationshipOne(){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("C", "F", "M"));

		Assertions.assertEquals(1., calculator.relationship("C", "C"), EPSILON);
		Assertions.assertEquals(0.5, calculator.kinship("C", "C"), EPSILON);
		Assertions.assertEquals(0., calculator.relationship(null, "C"), EPSILON);
	}

	@Test
	void aPedigreeThatIsItsOwnAncestorDoesNotHang(){
		//an error of the data: A is the parent of B and B the parent of A
		final AncestryGraph graph = graph("A", "B", "X", "B", "A", "Y", "C", "A", "B");
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph);

		Assertions.assertDoesNotThrow(() -> {
			calculator.inbreeding("C");
			calculator.kinship("A", "B");
			calculator.relationship("A", "C");
		});
	}

	@Test
	void theClosestCommonAncestorsAreTheOnesThatAreNotAncestorsOfTheOthers(){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(firstCousins());

		final List<ConsanguinityCalculator.CommonAncestor> ancestors = calculator.closestCommonAncestors("X", "Y");
		Assertions.assertEquals(2, ancestors.size(), "the two grandparents");
		Assertions.assertEquals("G1", ancestors.get(0).id());
		Assertions.assertEquals(2, ancestors.get(0).stepsFromFirst());
		Assertions.assertEquals(2, ancestors.get(0).stepsFromSecond());
	}

	@Test
	void aParentIsTheClosestCommonAncestorOfParentAndChild(){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph("C", "F", "M"));

		final List<ConsanguinityCalculator.CommonAncestor> ancestors = calculator.closestCommonAncestors("F", "C");
		Assertions.assertEquals(1, ancestors.size());
		Assertions.assertEquals("F", ancestors.getFirst().id());
		Assertions.assertEquals(0, ancestors.getFirst().stepsFromFirst());
		Assertions.assertEquals(1, ancestors.getFirst().stepsFromSecond());
	}

	@Test
	void aThirdParentIsIgnored(){
		final AncestryGraph graph = new AncestryGraph(Map.of("C", List.of("F", "M", "X")));

		Assertions.assertEquals(List.of("F", "M"), graph.parentsOf("C"));
		Assertions.assertTrue(graph.childrenOf("X").isEmpty());
	}

	@Test
	void theKinshipHasAName(){
		Assertions.assertEquals("first cousins", KinshipTerms.cousins(1, 0));
		Assertions.assertEquals("second cousins once removed", KinshipTerms.cousins(2, 1));
		Assertions.assertEquals("third cousins 2 times removed", KinshipTerms.cousins(3, 2));
		Assertions.assertEquals("first cousin once removed", KinshipTerms.cousin(1, 1));
		Assertions.assertEquals("ninth", KinshipTerms.ordinal(9));
		Assertions.assertEquals("10th", KinshipTerms.ordinal(10));
		Assertions.assertEquals("21st", KinshipTerms.ordinal(21));
		Assertions.assertEquals("3-times-great-grandparent and descendant", KinshipTerms.describe(0, 5));
		Assertions.assertEquals("collateral relatives, 4 generations apart", KinshipTerms.describe(1, 5));
		Assertions.assertEquals("the same individual", KinshipTerms.describe(0, 0));
		//the languages without messages of their own say the terms in English, as the rest of the report does
		final KinshipVocabulary english = new ReportLabels(ReportLanguage.ENGLISH).kinship();
		for(final ReportLanguage language : new ReportLanguage[]{ReportLanguage.of(Locale.forLanguageTag("fr")), ReportLanguage.of(Locale.forLanguageTag("es")), ReportLanguage.of(Locale.forLanguageTag("it")),
				ReportLanguage.of(Locale.forLanguageTag("de"))}){
			final KinshipVocabulary vocabulary = new ReportLabels(language).kinship();
			Assertions.assertEquals(english.text("kinship.term.ancestor.male", 4, 2), vocabulary.text("kinship.term.ancestor.male", 4, 2));
			Assertions.assertEquals(english.text("kinship.term.ancestor.male", 6, 4), vocabulary.text("kinship.term.ancestor.male", 6, 4));
			Assertions.assertEquals(KinshipTerms.cousin(english, 2, 1, true), KinshipTerms.cousin(vocabulary, 2, 1, true));
			Assertions.assertEquals(KinshipTerms.cousin(english, 3, 0, false), KinshipTerms.cousin(vocabulary, 3, 0, false));
		}
		//the interface has its own vocabulary: the same terms, from its own messages
		Assertions.assertEquals("great-grandfather", KinshipVocabulary.ui().text("kinship.term.ancestor.male", 3, 1));
		Assertions.assertEquals("siblings", KinshipTerms.describe(1, 1));
		Assertions.assertEquals("parent and child", KinshipTerms.describe(0, 1));
		Assertions.assertEquals("parent and child", KinshipTerms.describe(1, 0));
		Assertions.assertEquals("uncle or aunt and niece or nephew", KinshipTerms.describe(1, 2));
		Assertions.assertEquals("first cousins", KinshipTerms.describe(2, 2));
		Assertions.assertEquals("first cousins once removed", KinshipTerms.describe(3, 2));
		Assertions.assertEquals("second cousins", KinshipTerms.describe(3, 3));
		Assertions.assertEquals("second cousins 2 times removed", KinshipTerms.describe(3, 5));
	}

}
