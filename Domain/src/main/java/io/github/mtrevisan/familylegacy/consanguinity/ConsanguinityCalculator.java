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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


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


	private final Function<String, List<String>> parentsOf;
	/** The individuals met so far, numbered in the order they were met: everything below works on these numbers. */
	private final Map<String, Integer> numbers = new HashMap<>();
	private final List<String> ids = new ArrayList<>();
	/** The parents (numbers, at most two) of each individual, {@code null} until asked for. */
	private int[][] parentNumbers = new int[64][];
	/** The individual and all of its ancestors, {@code null} until asked for. */
	private BitSet[] ancestorSets = new BitSet[64];
	/** The longest line of ancestors, -1 until asked for. */
	private int[] depths = newIntArray(64, -1);
	private final BitSet depthInProgress = new BitSet();
	private double[] inbreedings = newDoubleArray(64);
	private final BitSet inbreedingInProgress = new BitSet();
	private final PairValues kinships = new PairValues();
	private final Set<Long> kinshipInProgress = new HashSet<>();


	public ConsanguinityCalculator(final AncestryGraph graph){
		this(graph::parentsOf);
	}

	/**
	 * @param parentsOf	The identifiers of the parents of an individual (the first two, without repetitions, are the ones that count; an empty
	 * 	list if they are unknown). It is asked once for each individual, and its answer is kept: it must not change while the
	 * 	calculator is in use.
	 */
	public ConsanguinityCalculator(final Function<String, List<String>> parentsOf){
		this.parentsOf = parentsOf;
	}


	/**
	 * The coefficient of inbreeding of Wright: the probability that the two alleles of an individual at a locus are identical by
	 * descent, that is the kinship of the two parents.
	 *
	 * @param id	The individual.
	 * @return	A value between 0 (the parents are not related, or one of them is unknown) and 1.
	 */
	public double inbreeding(final String id){
		return inbreeding(numberOf(id));
	}

	private double inbreeding(final int individual){
		final double cached = inbreedings[individual];
		if(!Double.isNaN(cached))
			return cached;

		final int[] parents = parentsOf(individual);
		if(parents.length < 2)
			return 0.;
		//a pedigree that is its own ancestor is an error of the data: it must not hang the computation
		if(inbreedingInProgress.get(individual))
			return 0.;

		inbreedingInProgress.set(individual);
		final double result = kinship(parents[0], parents[1]);
		inbreedingInProgress.clear(individual);
		inbreedings[individual] = result;
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

		return kinship(numberOf(first), numberOf(second));
	}

	private double kinship(final int first, final int second){
		if(first == second)
			return (1. + inbreeding(first)) / 2.;

		final long key = (first < second? ((long)first << 32) | second: ((long)second << 32) | first);
		final double cached = kinships.get(key);
		if(!Double.isNaN(cached))
			return cached;

		//no common ancestor, no kinship: most of the pairs end here, and it keeps the recursion small
		if(!ancestorsOf(first).intersects(ancestorsOf(second))){
			kinships.put(key, 0.);

			return 0.;
		}
		if(!kinshipInProgress.add(key))
			return 0.;

		//the one with the longer line of ancestors cannot be an ancestor of the other: it is the one that is replaced by its parents
		final boolean firstIsYounger = (depth(first) >= depth(second));
		final int younger = (firstIsYounger? first: second);
		final int other = (firstIsYounger? second: first);
		double sum = 0.;
		for(final int parent : parentsOf(younger))
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

		final int a = numberOf(first);
		final int b = numberOf(second);
		return 2. * kinship(a, b) / Math.sqrt((1. + inbreeding(a)) * (1. + inbreeding(b)));
	}

	/**
	 * @return	Whether the two individuals have a common ancestor (or one is an ancestor of the other) in the recorded ancestry. A
	 * pair that has none is unrelated as far as the data says.
	 */
	public boolean areRelated(final String first, final String second){
		return (first != null && second != null && ancestorsOf(numberOf(first)).intersects(ancestorsOf(numberOf(second))));
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

		final BitSet common = (BitSet)ancestorsOf(numberOf(first)).clone();
		common.and(ancestorsOf(numberOf(second)));
		if(common.isEmpty())
			return List.of();

		final Map<String, Integer> distancesFromFirst = distances(first);
		final Map<String, Integer> distancesFromSecond = distances(second);
		//a common ancestor is not the closest if another common ancestor descends from it: it is among the (strict) ancestors of that one.
		//All of them at once, with one pass on the common ancestors (looking at each pair of them is quadratic, and in a closed
		//community the common ancestors are thousands)
		final BitSet dominated = new BitSet();
		for(int member = common.nextSetBit(0); member >= 0; member = common.nextSetBit(member + 1)){
			final BitSet strict = (BitSet)ancestorsOf(member).clone();
			strict.clear(member);
			dominated.or(strict);
		}
		common.andNot(dominated);
		final List<CommonAncestor> result = new ArrayList<>();
		for(int candidate = common.nextSetBit(0); candidate >= 0; candidate = common.nextSetBit(candidate + 1)){
			final String id = ids.get(candidate);
			result.add(new CommonAncestor(id, distancesFromFirst.get(id), distancesFromSecond.get(id)));
		}
		result.sort(Comparator.comparingInt((CommonAncestor a) -> a.stepsFromFirst() + a.stepsFromSecond())
			.thenComparing(CommonAncestor::id));
		return result;
	}


	/** @return	The number of the individual, given the first time it is met. */
	private int numberOf(final String id){
		final Integer known = numbers.get(id);
		if(known != null)
			return known;

		final int number = ids.size();
		ids.add(id);
		numbers.put(id, number);
		if(number == parentNumbers.length){
			final int capacity = number << 1;
			parentNumbers = Arrays.copyOf(parentNumbers, capacity);
			ancestorSets = Arrays.copyOf(ancestorSets, capacity);
			final int oldLength = depths.length;
			depths = Arrays.copyOf(depths, capacity);
			Arrays.fill(depths, oldLength, capacity, -1);
			inbreedings = Arrays.copyOf(inbreedings, capacity);
			Arrays.fill(inbreedings, oldLength, capacity, Double.NaN);
		}
		return number;
	}

	/** @return	The numbers of the parents of the individual: at most two, without repetitions. */
	private int[] parentsOf(final int individual){
		int[] parents = parentNumbers[individual];
		if(parents == null){
			final List<String> known = parentsOf.apply(ids.get(individual));
			final int[] found = new int[Math.min(known.size(), 2)];
			int count = 0;
			for(int i = 0, size = known.size(); i < size && count < 2; i ++){
				final int parent = numberOf(known.get(i));
				if(count == 0 || found[0] != parent)
					found[count ++] = parent;
			}
			parents = (count == found.length? found: Arrays.copyOf(found, count));
			parentNumbers[individual] = parents;
		}
		return parents;
	}

	/** @return	The individual and all of its ancestors. */
	private BitSet ancestorsOf(final int individual){
		BitSet result = ancestorSets[individual];
		if(result == null){
			result = new BitSet();
			final Deque<Integer> queue = new ArrayDeque<>();
			result.set(individual);
			queue.add(individual);
			while(!queue.isEmpty())
				for(final int parent : parentsOf(queue.poll()))
					if(!result.get(parent)){
						result.set(parent);
						queue.add(parent);
					}
			ancestorSets[individual] = result;
		}
		return result;
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
			for(final int parent : parentsOf(numberOf(current))){
				final String parentId = ids.get(parent);
				if(result.putIfAbsent(parentId, steps + 1) == null)
					queue.add(parentId);
			}
		}
		return result;
	}

	/** @return	The generations of ancestors above the individual: the longest line, 0 for an individual without known parents. */
	private int depth(final int individual){
		final int cached = depths[individual];
		if(cached >= 0)
			return cached;
		if(depthInProgress.get(individual))
			return 0;

		depthInProgress.set(individual);
		int result = 0;
		for(final int parent : parentsOf(individual))
			result = Math.max(result, 1 + depth(parent));
		depthInProgress.clear(individual);
		depths[individual] = result;
		return result;
	}

	private static int[] newIntArray(final int size, final int value){
		final int[] array = new int[size];
		Arrays.fill(array, value);
		return array;
	}

	private static double[] newDoubleArray(final int size){
		final double[] array = new double[size];
		Arrays.fill(array, Double.NaN);
		return array;
	}


	/** The values of the pairs of individuals (the key is the two numbers in a {@code long}), with no object for each of them. */
	private static final class PairValues{

		private static final long EMPTY = -1L;

		private long[] keys = new long[1024];
		private double[] values = new double[1024];
		private int size;


		private PairValues(){
			Arrays.fill(keys, EMPTY);
		}


		/** @return	The value, NaN if there is none. */
		private double get(final long key){
			final int mask = keys.length - 1;
			for(int slot = slotOf(key, mask); ; slot = (slot + 1) & mask){
				final long found = keys[slot];
				if(found == key)
					return values[slot];
				if(found == EMPTY)
					return Double.NaN;
			}
		}

		private void put(final long key, final double value){
			if((size + 1) << 1 > keys.length)
				grow();

			final int mask = keys.length - 1;
			for(int slot = slotOf(key, mask); ; slot = (slot + 1) & mask){
				final long found = keys[slot];
				if(found == key){
					values[slot] = value;

					return;
				}
				if(found == EMPTY){
					keys[slot] = key;
					values[slot] = value;
					size ++;

					return;
				}
			}
		}

		private void grow(){
			final long[] oldKeys = keys;
			final double[] oldValues = values;
			keys = new long[oldKeys.length << 1];
			values = new double[keys.length];
			Arrays.fill(keys, EMPTY);
			size = 0;
			for(int i = 0; i < oldKeys.length; i ++)
				if(oldKeys[i] != EMPTY)
					put(oldKeys[i], oldValues[i]);
		}

		private static int slotOf(final long key, final int mask){
			final long mixed = key * 0x9E3779B97F4A7C15L;
			return (int)(mixed >>> 32) & mask;
		}

	}

}
