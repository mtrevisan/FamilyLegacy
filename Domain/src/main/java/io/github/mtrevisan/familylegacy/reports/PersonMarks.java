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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * What a report adds to a person wherever the person is written: the number the person has in the numbering of the report (Ahnentafel,
 * d'Aboville, Henry), and whether the person is a direct ancestor of the subject (written in bold).
 */
final class PersonMarks{

	static final PersonMarks NONE = new PersonMarks(Map.of(), Set.of());

	private static final BigInteger TWO = BigInteger.TWO;


	private final Map<String, String> numbers;
	private final Set<String> directAncestors;


	private PersonMarks(final Map<String, String> numbers, final Set<String> directAncestors){
		this.numbers = numbers;
		this.directAncestors = directAncestors;
	}


	/**
	 * @param ctx	The context of the report, with the root the report is about.
	 * @param type	The type of the report ({@code null} if it is not chosen).
	 * @return	The marks the options of the report ask for.
	 */
	static PersonMarks of(final ReportContext ctx, final ReportType type){
		final ReportConfig config = ctx.config;
		if(ReportContext.rootKindOf(ctx.root) != ReportContext.RootKind.INDIVIDUAL)
			return NONE;

		final NumberingSystem system = config.numbering().resolve(type);
		final Map<String, String> numbers = switch(system){
			case AHNENTAFEL -> ahnentafel(ctx);
			case D_ABOVILLE, HENRY -> descendants(ctx, system);
			case AUTO, NONE -> Map.of();
		};

		final Set<String> direct = new HashSet<>();
		if(config.boldDirectAncestors())
			for(final FLEFRecord ancestor : ctx.index.ancestorsByGeneration(ctx.root))
				if(!ancestor.equals(ctx.root))
					direct.add(ancestor.getId());
		return (numbers.isEmpty() && direct.isEmpty()? NONE: new PersonMarks(numbers, direct));
	}


	/** @return	The number of the person in the numbering of the report, {@code null} if it has none. */
	String numberOf(final FLEFRecord person){
		return numbers.get(person.getId());
	}

	boolean isDirectAncestor(final FLEFRecord person){
		return directAncestors.contains(person.getId());
	}


	/* ======================================================================
	 *                          Ahnentafel
	 * ====================================================================== */

	/** The subject is 1, the father of n is 2n, the mother is 2n + 1; a person that is an ancestor twice has the lowest number. */
	private static Map<String, String> ahnentafel(final ReportContext ctx){
		final FLEFRecord root = ctx.root;
		final int generations = ctx.config.generations();
		final Map<String, String> out = new HashMap<>();
		final Deque<FLEFRecord> queue = new ArrayDeque<>();
		final Deque<BigInteger> numbers = new ArrayDeque<>();
		final Deque<Integer> depths = new ArrayDeque<>();

		out.put(root.getId(), "1");
		queue.add(root);
		numbers.add(BigInteger.ONE);
		depths.add(0);
		while(!queue.isEmpty()){
			final FLEFRecord current = queue.poll();
			final BigInteger number = numbers.poll();
			final int depth = depths.poll();
			if(generations > 0 && depth >= generations)
				continue;

			final FLEFRecord[] parents = fatherAndMother(ctx, current);
			for(int slot = 0; slot < parents.length; slot ++){
				final FLEFRecord parent = parents[slot];
				if(parent == null || out.containsKey(parent.getId()))
					continue;

				final BigInteger parentNumber = number.multiply(TWO).add(BigInteger.valueOf(slot));
				out.put(parent.getId(), parentNumber.toString());
				queue.add(parent);
				numbers.add(parentNumber);
				depths.add(depth + 1);
			}
		}
		return out;
	}

	/**
	 * @return	The father (the first male parent) and the mother (the first female one) of the person; a parent of unknown sex, or a second
	 * 	one of the same sex, takes the place that is left free (the father's first).
	 */
	private static FLEFRecord[] fatherAndMother(final ReportContext ctx, final FLEFRecord person){
		final List<FLEFRecord> males = new ArrayList<>(2);
		final List<FLEFRecord> females = new ArrayList<>(2);
		final List<FLEFRecord> others = new ArrayList<>(2);
		for(final FLEFRecord parent : ctx.index.parentsOf(person)){
			final SexType sex = PreferredSex.of(parent, ctx.model);
			(sex == SexType.MALE? males: (sex == SexType.FEMALE? females: others)).add(parent);
		}

		final FLEFRecord father = (!males.isEmpty()? males.removeFirst(): null);
		final FLEFRecord mother = (!females.isEmpty()? females.removeFirst(): null);
		final List<FLEFRecord> left = new ArrayList<>(males);
		left.addAll(females);
		left.addAll(others);
		return new FLEFRecord[]{
			(father != null? father: (!left.isEmpty()? left.removeFirst(): null)),
			(mother != null? mother: (!left.isEmpty()? left.removeFirst(): null))};
	}


	/* ======================================================================
	 *                          d'Aboville and Henry
	 * ====================================================================== */

	/**
	 * The subject is 1; its children, eldest first, are 1.1, 1.2... (d'Aboville) or 11, 12... (Henry); a person that descends in two ways
	 * has the number of the first one found.
	 */
	private static Map<String, String> descendants(final ReportContext ctx, final NumberingSystem system){
		final FLEFRecord root = ctx.root;
		final int generations = ctx.config.generations();
		final TreeText text = new TreeText(ctx);
		final Map<String, String> out = new HashMap<>();
		final Deque<FLEFRecord> queue = new ArrayDeque<>();
		final Deque<Integer> depths = new ArrayDeque<>();

		out.put(root.getId(), "1");
		queue.add(root);
		depths.add(0);
		while(!queue.isEmpty()){
			final FLEFRecord current = queue.poll();
			final int depth = depths.poll();
			if(generations > 0 && depth >= generations)
				continue;

			final String number = out.get(current.getId());
			int position = 0;
			for(final FLEFRecord child : text.childrenOf(current)){
				if(out.containsKey(child.getId()))
					continue;

				position ++;
				out.put(child.getId(), (system == NumberingSystem.HENRY
					? number + henryDigit(position): number + '.' + position));
				queue.add(child);
				depths.add(depth + 1);
			}
		}
		return out;
	}

	/** @return	The digit of the child in the Henry system: 1-9, then X for the tenth, A for the eleventh, B... */
	private static String henryDigit(final int position){
		if(position < 10)
			return String.valueOf(position);
		if(position == 10)
			return "X";
		return String.valueOf((char)('A' + position - 11));
	}

}
