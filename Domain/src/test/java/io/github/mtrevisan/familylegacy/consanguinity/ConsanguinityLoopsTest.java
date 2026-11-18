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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/** The loops of consanguinity, on the pedigrees whose coefficient is known: the loops explain it. */
class ConsanguinityLoopsTest{

	private static final double EPSILON = 1e-12;


	/** The parents by child: {@code graph("c", "f", "m", "d", "f", "m")} says that c and d are children of f and m. */
	private static AncestryGraph graph(final String... childAndParents){
		final Map<String, List<String>> parents = new LinkedHashMap<>();
		for(int i = 0; i < childAndParents.length; i += 3)
			parents.put(childAndParents[i], List.of(childAndParents[i + 1], childAndParents[i + 2]));
		return new AncestryGraph(parents);
	}

	private static ConsanguinityLoops.Result loopsOf(final AncestryGraph graph, final String id){
		return new ConsanguinityLoops(graph, new ConsanguinityCalculator(graph))
			.of(id);
	}

	private static double sum(final ConsanguinityLoops.Result result){
		return result.loops().stream()
			.mapToDouble(ConsanguinityLoops.Loop::contribution)
			.sum();
	}


	@Test
	void theChildOfFirstCousinsHasOneLoopForEachSharedGrandparent(){
		final AncestryGraph graph = graph("A", "G1", "G2", "B", "G1", "G2", "X", "A", "WA", "Y", "B", "WB", "Z", "X", "Y");

		final ConsanguinityLoops.Result result = loopsOf(graph, "Z");

		Assertions.assertTrue(result.hasTwoParents());
		Assertions.assertEquals(2, result.loops().size());
		Assertions.assertEquals(List.of("G1", "G2"), result.loops().stream().map(ConsanguinityLoops.Loop::ancestorId).toList());
		final ConsanguinityLoops.Loop loop = result.loops().getFirst();
		Assertions.assertEquals(List.of("X", "A", "G1"), loop.firstLine());
		Assertions.assertEquals(List.of("Y", "B", "G1"), loop.secondLine());
		Assertions.assertEquals(2, loop.firstGenerations());
		Assertions.assertEquals(2, loop.secondGenerations());
		Assertions.assertEquals(1. / 32., loop.contribution(), EPSILON);
		Assertions.assertEquals(1. / 16., result.inbreeding(), EPSILON);
		Assertions.assertEquals(result.inbreeding(), sum(result), EPSILON);
		Assertions.assertFalse(result.truncated());
	}

	@Test
	void theChildOfSiblingsHasTwoLoopsOfOneGenerationEach(){
		final ConsanguinityLoops.Result result = loopsOf(graph("A", "F", "M", "B", "F", "M", "C", "A", "B"), "C");

		Assertions.assertEquals(2, result.loops().size());
		Assertions.assertEquals(List.of("A", "F"), result.loops().getFirst().firstLine());
		Assertions.assertEquals(List.of("B", "F"), result.loops().getFirst().secondLine());
		Assertions.assertEquals(1. / 8., result.loops().getFirst().contribution(), EPSILON);
		Assertions.assertEquals(0.25, sum(result), EPSILON);
	}

	@Test
	void theChildOfAnUncleAndHisNieceHasLoopsOfDifferentLength(){
		//A and B are siblings, N is a child of B, and Z is a child of A and N
		final AncestryGraph graph = graph("A", "F", "M", "B", "F", "M", "N", "B", "W", "Z", "A", "N");

		final ConsanguinityLoops.Result result = loopsOf(graph, "Z");

		Assertions.assertEquals(2, result.loops().size());
		final ConsanguinityLoops.Loop loop = result.loops().getFirst();
		Assertions.assertEquals(List.of("A", "F"), loop.firstLine());
		Assertions.assertEquals(List.of("N", "B", "F"), loop.secondLine());
		Assertions.assertEquals(1. / 16., loop.contribution(), EPSILON);
		Assertions.assertEquals(0.125, sum(result), EPSILON);
		Assertions.assertEquals(0.125, result.inbreeding(), EPSILON);
	}

	@Test
	void theChildOfAParentAndItsChildHasAZeroLengthLine(){
		//C is a child of F and of D, who is a child of F
		final ConsanguinityLoops.Result result = loopsOf(graph("D", "F", "M", "C", "F", "D"), "C");

		Assertions.assertEquals(1, result.loops().size());
		final ConsanguinityLoops.Loop loop = result.loops().getFirst();
		Assertions.assertEquals("F", loop.ancestorId());
		Assertions.assertEquals(List.of("F"), loop.firstLine());
		Assertions.assertEquals(0, loop.firstGenerations());
		Assertions.assertEquals(List.of("D", "F"), loop.secondLine());
		Assertions.assertEquals(0.25, loop.contribution(), EPSILON);
		Assertions.assertEquals(0.25, result.inbreeding(), EPSILON);
	}

	@Test
	void unrelatedParentsHaveNoLoops(){
		final ConsanguinityLoops.Result result = loopsOf(graph("C", "F", "M"), "C");

		Assertions.assertTrue(result.hasTwoParents());
		Assertions.assertTrue(result.loops().isEmpty());
		Assertions.assertEquals(0., result.inbreeding(), EPSILON);
	}

	@Test
	void anIndividualWithoutTwoKnownParentsHasNoLoops(){
		final AncestryGraph graph = new AncestryGraph(Map.of("C", List.of("F")));

		final ConsanguinityLoops.Result result = loopsOf(graph, "C");

		Assertions.assertFalse(result.hasTwoParents());
		Assertions.assertTrue(result.loops().isEmpty());
		Assertions.assertFalse(loopsOf(graph, "F").hasTwoParents());
	}

	@Test
	void aLoopThroughAnInbredAncestorWeighsMore(){
		//F is the child of siblings (inbreeding 1/4), M is unrelated to them; A and B are children of F and M; C is a child of A and B
		final AncestryGraph graph = graph("P1", "G1", "G2", "P2", "G1", "G2", "F", "P1", "P2", "A", "F", "M", "B", "F", "M", "C", "A", "B");
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph);

		final ConsanguinityLoops.Result result = new ConsanguinityLoops(graph, calculator).of("C");

		Assertions.assertEquals(2, result.loops().size());
		Assertions.assertEquals("F", result.loops().getFirst().ancestorId());
		Assertions.assertEquals(1. / 8. * 1.25, result.loops().getFirst().contribution(), EPSILON);
		Assertions.assertEquals("M", result.loops().getLast().ancestorId());
		Assertions.assertEquals(1. / 8., result.loops().getLast().contribution(), EPSILON);
		Assertions.assertEquals(calculator.inbreeding("C"), sum(result), EPSILON);
	}

}
