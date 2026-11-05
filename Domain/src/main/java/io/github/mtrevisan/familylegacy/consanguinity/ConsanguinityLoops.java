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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The consanguinity loops of an individual: <em>why</em> it is inbred, not only how much. A loop is a common ancestor of the two
 * parents and the two lines of descent that lead from it to them, one to each parent, with no individual in common but the ancestor
 * (Wright's paths). A child of first cousins has two loops, one for each grandparent they share, and a child of siblings two as well.
 * <p>
 * The ancestors are the <em>closest</em> common ancestors of the two parents (the ones that are not ancestors of another common
 * ancestor), as {@link ConsanguinityCalculator#closestCommonAncestors(String, String)} gives them. Each loop contributes
 * {@code (1/2)^(n1 + n2 + 1) (1 + F(ancestor))} to the coefficient of inbreeding, where {@code n1} and {@code n2} are the generations
 * from the ancestor to each parent: it is shown to compare the loops among themselves. The sum of the listed loops is not the
 * coefficient: the coefficient counts the lines through the farther common ancestors too, and it is the calculator that gives it exactly
 * (the result carries it).
 * <p>
 * It only reads the graph. A pedigree with a lot of collapse has a lot of loops: no more than {@link #MAX_LOOPS} are listed (the
 * shortest first, those that weigh the most), and the result says if there were more.
 */
public final class ConsanguinityLoops{

	/** The most loops listed for an individual. */
	public static final int MAX_LOOPS = 100;

	/** The most generations followed from a parent up to a common ancestor: beyond it the contribution is under one in a million. */
	static final int MAX_GENERATIONS = 20;


	/**
	 * One loop.
	 *
	 * @param ancestorId	The common ancestor.
	 * @param firstLine	The line from the first parent up to the ancestor, both included ({@code [parent]} if the parent is the ancestor).
	 * @param secondLine	The line from the second parent up to the ancestor, both included.
	 * @param contribution	What the loop adds to the coefficient of inbreeding of the individual.
	 */
	public record Loop(String ancestorId, List<String> firstLine, List<String> secondLine, double contribution){

		/** @return	The generations from the ancestor to the first parent. */
		public int firstGenerations(){
			return firstLine.size() - 1;
		}

		/** @return	The generations from the ancestor to the second parent. */
		public int secondGenerations(){
			return secondLine.size() - 1;
		}
	}

	/**
	 * What was found for an individual.
	 *
	 * @param individualId	The individual.
	 * @param firstParentId	The first of the two parents ({@code null} if the individual does not have two known ones).
	 * @param secondParentId	The second parent.
	 * @param inbreeding	The exact coefficient of inbreeding, from the calculator.
	 * @param loops	The loops, the ones that weigh the most first.
	 * @param truncated	Whether there are loops that are not listed ({@link #MAX_LOOPS}).
	 */
	public record Result(String individualId, String firstParentId, String secondParentId, double inbreeding, List<Loop> loops,
			boolean truncated){

		/** @return	Whether the individual has two known parents, the only way to be inbred. */
		public boolean hasTwoParents(){
			return (firstParentId != null && secondParentId != null);
		}
	}


	private final AncestryGraph graph;
	private final ConsanguinityCalculator calculator;
	private final Map<String, Set<String>> ancestors = new HashMap<>();


	/**
	 * @param graph	The ancestry.
	 * @param calculator	The calculator <em>of the same graph</em> (it is asked for the closest common ancestors and the coefficients).
	 */
	public ConsanguinityLoops(final AncestryGraph graph, final ConsanguinityCalculator calculator){
		this.graph = graph;
		this.calculator = calculator;
	}


	/**
	 * @param id	The individual.
	 * @return	Its loops: none if the parents are not related, or if one of them is unknown.
	 */
	public Result of(final String id){
		final List<String> parents = graph.parentsOf(id);
		if(parents.size() < 2)
			return new Result(id, null, null, 0., List.of(), false);

		final String first = parents.get(0);
		final String second = parents.get(1);
		final double inbreeding = calculator.inbreeding(id);

		final List<Loop> loops = new ArrayList<>();
		for(final ConsanguinityCalculator.CommonAncestor ancestor : calculator.closestCommonAncestors(first, second)){
			final double weight = (1. + calculator.inbreeding(ancestor.id()));
			final List<List<String>> firstLines = linesTo(first, ancestor.id());
			final List<List<String>> secondLines = linesTo(second, ancestor.id());
			for(final List<String> firstLine : firstLines)
				for(final List<String> secondLine : secondLines)
					if(shareOnlyTheAncestor(firstLine, secondLine)){
						final int generations = (firstLine.size() - 1) + (secondLine.size() - 1);
						loops.add(new Loop(ancestor.id(), firstLine, secondLine, Math.pow(0.5, generations + 1) * weight));
					}
		}
		loops.sort(Comparator.comparingDouble(Loop::contribution).reversed()
			.thenComparing(Loop::ancestorId)
			.thenComparing(loop -> String.join(">", loop.firstLine()))
			.thenComparing(loop -> String.join(">", loop.secondLine())));

		final boolean truncated = (loops.size() > MAX_LOOPS);
		return new Result(id, first, second, inbreeding, List.copyOf(truncated? loops.subList(0, MAX_LOOPS): loops), truncated);
	}


	/** @return	Every line of descent from the ancestor to the individual, written from the individual up to the ancestor. */
	private List<List<String>> linesTo(final String from, final String ancestor){
		final List<List<String>> result = new ArrayList<>();
		final Deque<String> path = new ArrayDeque<>();
		walk(from, ancestor, path, result);
		return result;
	}

	private void walk(final String current, final String ancestor, final Deque<String> path, final List<List<String>> result){
		path.addLast(current);
		if(current.equals(ancestor)){
			result.add(List.copyOf(path));
			path.removeLast();

			return;
		}

		//a line longer than the limit, or one that cannot reach the ancestor, is not followed
		if(path.size() <= MAX_GENERATIONS && result.size() < MAX_LOOPS * 4)
			for(final String parent : graph.parentsOf(current))
				if(!path.contains(parent) && (parent.equals(ancestor) || ancestorsOf(parent).contains(ancestor)))
					walk(parent, ancestor, path, result);
		path.removeLast();
	}

	/** @return	The individual and all of its ancestors. */
	private Set<String> ancestorsOf(final String id){
		final Set<String> cached = ancestors.get(id);
		if(cached != null)
			return cached;

		final Set<String> result = new HashSet<>();
		final Deque<String> queue = new ArrayDeque<>();
		result.add(id);
		queue.add(id);
		while(!queue.isEmpty())
			for(final String parent : graph.parentsOf(queue.poll()))
				if(result.add(parent))
					queue.add(parent);
		final Set<String> unmodifiable = Collections.unmodifiableSet(result);
		ancestors.put(id, unmodifiable);
		return unmodifiable;
	}

	private static boolean shareOnlyTheAncestor(final List<String> firstLine, final List<String> secondLine){
		final String ancestor = firstLine.getLast();
		final Set<String> inFirst = new HashSet<>(firstLine);
		for(final String id : secondLine)
			if(!id.equals(ancestor) && inFirst.contains(id))
				return false;
		return true;
	}

}
