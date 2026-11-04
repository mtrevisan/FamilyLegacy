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
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The coefficient of inbreeding {@code F} of Wright and the coefficient of relationship {@code R}, computed on the recorded ancestry.
 * <p>
 * Both come from the <em>kinship coefficient</em> {@code phi(x, y)}: the probability that an allele taken at random from {@code x} and
 * one taken from {@code y} at the same locus are identical by descent. It is computed exactly, by the classical recursion over the
 * pedigree (the ancestors before the descendants, with no truncation of the distant ancestors):
 * </p>
 * <ul>
 *   <li>{@code phi(x, x) = (1 + F(x)) / 2};</li>
 *   <li>{@code phi(x, y) = (phi(x, father(y)) + phi(x, mother(y))) / 2}, where {@code y} is not an ancestor of {@code x} (it is
 *   the one with the longer line of ancestors); an unknown parent counts as unrelated to everyone;</li>
 *   <li>{@code F(x) = phi(father(x), mother(x))}, and 0 if one of the parents is unknown;</li>
 *   <li>{@code R(x, y) = 2 phi(x, y) / sqrt((1 + F(x)) (1 + F(y)))}.</li>
 * </ul>
 * Every path that joins two individuals through a common ancestor counts, and so does the inbreeding of the common ancestors: what
 * the sum of {@code (1/2)^n} over the shortest paths does not do.
 * <p>
 * The values are the ones a pedigree allows: an unknown ancestor is a founder, unrelated to the others, so a coefficient is a lower
 * bound of the real one. They measure what is recorded, not what happened: a wrong or missing link changes them.
 * </p>
 * The results are cached: build one calculator for a graph and ask it as many times as needed. It is not thread safe.
 */
public final class ConsanguinityCalculator{

	/**
	 * A common ancestor of two individuals that is not an ancestor of another common ancestor: the closest the two lines get.
	 *
	 * @param id                the identifier of the ancestor
	 * @param stepsFromFirst    the generations between the first individual and the ancestor (0 if it is the individual itself)
	 * @param stepsFromSecond   the generations between the second individual and the ancestor
	 */
	public record CommonAncestor(String id, int stepsFromFirst, int stepsFromSecond){}

	private record Pair(String first, String second){

		static Pair of(final String a, final String b){
			return (a.compareTo(b) <= 0? new Pair(a, b): new Pair(b, a));
		}

	}


	private final AncestryGraph graph;
	private final Map<String, Set<String>> ancestors = new HashMap<>();
	private final Map<String, Integer> depths = new HashMap<>();
	private final Set<String> depthInProgress = new HashSet<>();
	private final Map<Pair, Double> kinships = new HashMap<>();
	private final Set<Pair> kinshipInProgress = new HashSet<>();
	private final Map<String, Double> inbreedings = new HashMap<>();
	private final Set<String> inbreedingInProgress = new HashSet<>();


	public ConsanguinityCalculator(final AncestryGraph graph){
		this.graph = graph;
	}


	/**
	 * The coefficient of inbreeding of Wright: the probability that the two alleles of an individual at a locus are identical by
	 * descent, that is the kinship of the two parents.
	 *
	 * @param id	The individual.
	 * @return	A value between 0 (the parents are not related, or one of them is unknown) and 1.
	 */
	public double inbreeding(final String id){
		final Double cached = inbreedings.get(id);
		if(cached != null)
			return cached;

		final List<String> parents = graph.parentsOf(id);
		if(parents.size() < 2)
			return 0.;
		//a pedigree that is its own ancestor is an error of the data: it must not hang the computation
		if(!inbreedingInProgress.add(id))
			return 0.;

		final double result = kinship(parents.get(0), parents.get(1));
		inbreedingInProgress.remove(id);
		inbreedings.put(id, result);
		return result;
	}

	/**
	 * The kinship coefficient: the probability that an allele of the first individual and one of the second are identical by
	 * descent. It is also the coefficient of inbreeding that a child of the two would have.
	 *
	 * @return	A value between 0 (unrelated) and 1 (an individual with itself, if fully inbred).
	 */
	public double kinship(final String first, final String second){
		if(first == null || second == null)
			return 0.;
		if(first.equals(second))
			return (1. + inbreeding(first)) / 2.;

		final Pair key = Pair.of(first, second);
		final Double cached = kinships.get(key);
		if(cached != null)
			return cached;

		//no common ancestor, no kinship: most of the pairs end here, and it keeps the recursion small
		if(disjoint(ancestorsOf(first), ancestorsOf(second))){
			kinships.put(key, 0.);

			return 0.;
		}
		if(!kinshipInProgress.add(key))
			return 0.;

		//the one with the longer line of ancestors cannot be an ancestor of the other: it is the one that is replaced by its parents
		final boolean firstIsYounger = (depth(first) >= depth(second));
		final String younger = (firstIsYounger? first: second);
		final String other = (firstIsYounger? second: first);
		double sum = 0.;
		for(final String parent : graph.parentsOf(younger))
			sum += kinship(other, parent);
		final double result = sum / 2.;

		kinshipInProgress.remove(key);
		kinships.put(key, result);
		return result;
	}

	/**
	 * The coefficient of relationship of Wright: the proportion of the genes that two individuals share by descent from common
	 * ancestors.
	 *
	 * @return	1 for an individual with itself, 1/2 for parent and child or siblings, 1/4 for uncle and niece, 1/8 for first cousins
	 * (for individuals that are not inbred).
	 */
	public double relationship(final String first, final String second){
		if(first == null || second == null)
			return 0.;
		if(first.equals(second))
			return 1.;

		return 2. * kinship(first, second) / Math.sqrt((1. + inbreeding(first)) * (1. + inbreeding(second)));
	}

	/**
	 * @return	Whether the two individuals have a common ancestor (or one is an ancestor of the other) in the recorded ancestry. A
	 * pair that has none is unrelated as far as the data says.
	 */
	public boolean areRelated(final String first, final String second){
		return (first != null && second != null && !disjoint(ancestorsOf(first), ancestorsOf(second)));
	}

	/**
	 * The common ancestors of two individuals that are the closest to them: the ones that are not ancestors of another common ancestor.
	 * An individual counts as an ancestor of itself (with 0 steps), so that for a parent and a child the parent is the answer.
	 *
	 * @return	The ancestors, the closest first (by the generations in all, then by the identifier); empty if the two are unrelated.
	 */
	public List<CommonAncestor> closestCommonAncestors(final String first, final String second){
		if(first == null || second == null)
			return List.of();

		final Set<String> common = new HashSet<>(ancestorsOf(first));
		common.retainAll(ancestorsOf(second));
		if(common.isEmpty())
			return List.of();

		final Map<String, Integer> distancesFromFirst = distances(first);
		final Map<String, Integer> distancesFromSecond = distances(second);
		final List<CommonAncestor> result = new ArrayList<>();
		for(final String candidate : common){
			boolean closest = true;
			for(final String descendant : common)
				if(!descendant.equals(candidate) && ancestorsOf(descendant).contains(candidate)){
					closest = false;

					break;
				}
			if(closest)
				result.add(new CommonAncestor(candidate, distancesFromFirst.get(candidate), distancesFromSecond.get(candidate)));
		}
		result.sort(Comparator.comparingInt((CommonAncestor a) -> a.stepsFromFirst() + a.stepsFromSecond())
			.thenComparing(CommonAncestor::id));
		return result;
	}


	/** @return	The individual and all of its ancestors. */
	private Set<String> ancestorsOf(final String id){
		return ancestors.computeIfAbsent(id, k -> distances(id).keySet());
	}

	/** @return	The generations that separate the individual from each of its ancestors (itself included, at 0), the shortest way. */
	private Map<String, Integer> distances(final String id){
		final Map<String, Integer> result = new HashMap<>();
		final Deque<String> queue = new ArrayDeque<>();
		result.put(id, 0);
		queue.add(id);
		while(!queue.isEmpty()){
			final String current = queue.poll();
			final int steps = result.get(current);
			for(final String parent : graph.parentsOf(current))
				if(result.putIfAbsent(parent, steps + 1) == null)
					queue.add(parent);
		}
		return result;
	}

	/** @return	The generations of ancestors above the individual: the longest line, 0 for an individual without known parents. */
	private int depth(final String id){
		final Integer cached = depths.get(id);
		if(cached != null)
			return cached;
		if(!depthInProgress.add(id))
			return 0;

		int result = 0;
		for(final String parent : graph.parentsOf(id))
			result = Math.max(result, 1 + depth(parent));
		depthInProgress.remove(id);
		depths.put(id, result);
		return result;
	}

	private static boolean disjoint(final Set<String> a, final Set<String> b){
		final Set<String> smaller = (a.size() <= b.size()? a: b);
		final Set<String> larger = (smaller == a? b: a);
		for(final String id : smaller)
			if(larger.contains(id))
				return false;
		return true;
	}

}
