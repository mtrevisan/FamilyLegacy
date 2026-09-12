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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.kinship;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeService;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Computes the kinship between two individuals of a FLEF model.
 * <p>
 * The computation walks the ancestor graph from both individuals, finds
 * every common ancestor, and derives:
 * <ul>
 *   <li>the most recent common ancestor (MRCA), i.e. the one minimizing
 *       the total number of generations between the two individuals;</li>
 *   <li>the furthest common ancestor, i.e. the one maximizing that
 *       distance;</li>
 *   <li>the chain of ancestors from each individual up to the MRCA;</li>
 *   <li>a natural-language description of the relationship, following the
 *       standard genealogy vocabulary (father, uncle, first cousin once
 *       removed, and so on);</li>
 *   <li>the Wright's relationship coefficient, computed as
 *       {@code R = Σ (1/2)^(dA + dB)} over all common ancestors, with
 *       {@code dA} and {@code dB} the generation distances from A and B
 *       to that ancestor. This ignores the inbreeding coefficient of the
 *       ancestors themselves, which is the standard approximation used by
 *       non-specialist tools.</li>
 * </ul>
 * The calculator uses the parent index provided by {@link TreeService}, so
 * it honors the same relationship type filter (biological, adoptive, etc.)
 * that was configured for the tree.
 */
final class KinshipCalculator{

	private static final String TAG_SEX = "sex";

	private static final String ENUM_SEX_MALE = "male";


	private final FLEFModel model;
	private final TreeService treeService;
	private final IndividualHandler individualHandler;


	/**
	 * Constructor.
	 *
	 * @param model       the FLEF model (must not be {@code null})
	 * @param treeService the tree service providing the parent index (must not be {@code null})
	 */
	KinshipCalculator(final FLEFModel model, final TreeService treeService){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		if(treeService == null)
			throw new IllegalArgumentException("Tree service must not be null");

		this.model = model;
		this.treeService = treeService;
		this.individualHandler = IndividualHandler.getInstance();
	}


	/**
	 * Computes the kinship between the two individuals.
	 * <p>
	 * The computation walks the ancestor graph from both individuals, finds
	 * every common ancestor, and derives:
	 * <ul>
	 *   <li>the most recent common ancestor (MRCA), i.e. the one minimizing
	 *       the total number of generations between the two individuals;</li>
	 *   <li>the furthest common ancestor, i.e. the one maximizing that
	 *       distance;</li>
	 *   <li>the chain of ancestors from each individual up to the MRCA;</li>
	 *   <li>a natural-language description of the relationship, following
	 *       the standard genealogy vocabulary;</li>
	 *   <li>the Wright's relationship coefficient, computed as
	 *       {@code R = sum (1/2)^(dA + dB)} over all common ancestors;</li>
	 *   <li>the civil (Roman) degree, the canonical degree, and the Chinese
	 *       generation count. The Korean chon and the Germanic knee are also
	 *       exposed by {@link KinshipResult} with the same numeric value as
	 *       the civil and canonical degrees respectively.</li>
	 * </ul>
	 * The calculator uses the parent index provided by {@link TreeService}, so
	 * it honors the same relationship type filter (biological, adoptive, etc.)
	 * that was configured for the tree.
	 *
	 * @param idA the id of the first individual; may be {@code null}
	 * @param idB the id of the second individual; may be {@code null}
	 * @return the result, never {@code null}
	 */
	KinshipResult calculate(final String idA, final String idB){
		if(idA == null || idB == null)
			return KinshipResult.notRelated(idA, labelOf(idA), idB, labelOf(idB));
		if(idA.equals(idB))
			return KinshipResult.sameIndividual(idA, labelOf(idA));

		final String sexA = sexOf(idA);
		final String sexB = sexOf(idB);

		// Walk the ancestor graph from both individuals. Each map associates
		// every reachable ancestor id to its generation distance from the
		// corresponding individual (0 = the individual itself).
		final Map<String, Integer> ancestorsA = collectAncestorDistances(idA);
		final Map<String, Integer> ancestorsB = collectAncestorDistances(idB);

		// Intersection of the two ancestor sets: the ids shared by both.
		final Set<String> commonIds = new HashSet<>(ancestorsA.keySet());
		commonIds.retainAll(ancestorsB.keySet());

		if(commonIds.isEmpty())
			return KinshipResult.notRelated(idA, labelOf(idA), idB, labelOf(idB));

		// Build the common-ancestor list and accumulate the Wright
		// coefficient. Each common ancestor contributes a term
		// (1/2)^(dA + dB), where dA and dB are its generation distances
		// from A and B respectively.
		final List<KinshipResult.CommonAncestorInfo> commons = new ArrayList<>(commonIds.size());
		double coefficient = 0.;
		for(final String ancestorId : commonIds){
			final int dA = ancestorsA.get(ancestorId);
			final int dB = ancestorsB.get(ancestorId);
			final double contribution = Math.pow(0.5, dA + dB);
			commons.add(new KinshipResult.CommonAncestorInfo(ancestorId, labelOf(ancestorId), dA, dB, contribution));
			coefficient += contribution;
		}

		// Sort by total distance ascending, then by distance from A. The
		// first element is the MRCA, the last is the furthest one.
		commons.sort(Comparator
			.comparingInt((KinshipResult.CommonAncestorInfo c) -> c.distanceFromA() + c.distanceFromB())
			.thenComparingInt(KinshipResult.CommonAncestorInfo::distanceFromA));

		final KinshipResult.CommonAncestorInfo mrca = commons.getFirst();
		final KinshipResult.CommonAncestorInfo furthest = commons.getLast();

		// Build the two chains up to the MRCA. Each chain is ordered from
		// the individual (step 0) up to the ancestor (last step).
		final List<KinshipResult.ChainEntry> chainA = buildChain(idA, mrca.id());
		final List<KinshipResult.ChainEntry> chainB = buildChain(idB, mrca.id());

		// Natural-language description of the relationship.
		final String description = describe(mrca, commons, labelOf(idA), labelOf(idB), sexA, sexB);

		final int dA = mrca.distanceFromA();
		final int dB = mrca.distanceFromB();
		final boolean directLine = (dA == 0 || dB == 0);

		// ------------------------------------------------------------------
		// Civil (Roman) degree: Italy, Germany, Japan, historically the
		// whole Roman-law tradition. The same numeric value is also the
		// Korean chon.
		//   Direct line: max(dA, dB)   (the number of generations between
		//                               the two, excluding the ancestor as
		//                               a transit point)
		//   Collateral:  dA + dB       (the sum of the ascension from A and
		//                               the descension to B, again
		//                               excluding the ancestor)
		// ------------------------------------------------------------------
		final int civilDegree = (directLine? Math.max(dA, dB): dA + dB);

		// ------------------------------------------------------------------
		// Canonical degree: Catholic Church. The same numeric value is
		// also the Germanic "knee" number.
		//   Direct line: max(dA, dB)   (same as the civil computation)
		//   Collateral:  min(dA, dB)   (only the shorter side is counted,
		//                               reflecting the Germanic
		//                               "computation by steps" system)
		// ------------------------------------------------------------------
		final int canonicalDegree = (directLine? Math.max(dA, dB): Math.min(dA, dB));

		// ------------------------------------------------------------------
		// Chinese generation count (PRC Marriage Law). Self is counted as
		// the first generation, so the common ancestor is at generation
		// 1 + max(dA, dB) from the further of the two individuals.
		//   Parent-child:          2 generations
		//   Siblings:              2 generations
		//   Grandparent-grandchild: 3 generations
		//   Uncle-nephew:          3 generations
		//   First cousins:         3 generations
		//   First cousins once removed: 4 generations
		// ------------------------------------------------------------------
		final int chineseGeneration = 1 + Math.max(dA, dB);

		return new KinshipResult(idA, labelOf(idA), idB, labelOf(idB),
			sexA, sexB, chainA, chainB, mrca, furthest, commons, description, coefficient,
			directLine, civilDegree, canonicalDegree, chineseGeneration);
	}


	/* ======================================================================
	 *                          Ancestor walking
	 * ====================================================================== */

	/**
	 * Breadth-first walk of the ancestor graph starting from the given
	 * individual. Returns a map from every reachable ancestor id to its
	 * generation distance from the starting individual. The starting
	 * individual itself is included with distance 0.
	 */
	private Map<String, Integer> collectAncestorDistances(final String rootId){
		final Map<String, Integer> distances = new HashMap<>();
		final Deque<String> queue = new ArrayDeque<>();
		distances.put(rootId, 0);
		queue.add(rootId);
		while(!queue.isEmpty()){
			final String id = queue.poll();
			final int distance = distances.get(id);

			for(final FLEFRecord parent : treeService.getParents(id)){
				final String parentId = parent.getId();
				if(parentId == null || distances.containsKey(parentId))
					continue;

				distances.put(parentId, distance + 1);

				queue.add(parentId);
			}
		}
		return distances;
	}

	/**
	 * Reconstructs the chain of ancestors from {@code fromId} up to
	 * {@code toAncestorId}, inclusive. Uses a BFS with parent pointers so
	 * that the shortest chain is returned. Returns an empty list if no
	 * chain exists.
	 */
	private List<KinshipResult.ChainEntry> buildChain(final String fromId, final String toAncestorId){
		if(fromId.equals(toAncestorId))
			return List.of(new KinshipResult.ChainEntry(fromId, labelOf(fromId), 0));

		final Map<String, String> parentPointer = new HashMap<>();
		final Set<String> visited = new HashSet<>();
		final Deque<String> queue = new ArrayDeque<>();
		visited.add(fromId);
		queue.add(fromId);
		boolean found = false;
		while(!queue.isEmpty() && !found){
			final String id = queue.poll();
			for(final FLEFRecord parent : treeService.getParents(id)){
				final String parentId = parent.getId();
				if(parentId == null || !visited.add(parentId))
					continue;

				parentPointer.put(parentId, id);
				if(parentId.equals(toAncestorId)){
					found = true;

					break;
				}

				queue.add(parentId);
			}
		}

		if(!found)
			return List.of(new KinshipResult.ChainEntry(fromId, labelOf(fromId), 0));

		final List<String> ids = new ArrayList<>();
		String current = toAncestorId;
		while(current != null && !current.equals(fromId)){
			ids.add(current);

			current = parentPointer.get(current);
		}
		ids.add(fromId);
		Collections.reverse(ids);

		final List<KinshipResult.ChainEntry> chain = new ArrayList<>(ids.size());
		for(int i = 0; i < ids.size(); i ++){
			final String id = ids.get(i);
			chain.add(new KinshipResult.ChainEntry(id, labelOf(id), i));
		}
		return chain;
	}


	/* ======================================================================
	 *                          Description
	 * ====================================================================== */

	private static String describe(final KinshipResult.CommonAncestorInfo mrca,
			final List<KinshipResult.CommonAncestorInfo> commons, final String nameA, final String nameB,
			final String sexA, final String sexB){
		final int na = mrca.distanceFromA();
		final int nb = mrca.distanceFromB();

		if(na == 0)
			return nameA + " is the " + ancestorTerm(nb, sexA) + " of " + nameB;
		if(nb == 0)
			return nameB + " is the " + ancestorTerm(na, sexB) + " of " + nameA;

		if(na == 1 && nb == 1){
			final long sharedParents = commons.stream()
				.filter(c -> c.distanceFromA() == 1 && c.distanceFromB() == 1)
				.count();
			final String siblingTerm = siblingTerm(sexA, sexB);
			return (sharedParents >= 2
				? nameA + " and " + nameB + " are full " + siblingTerm
				: nameA + " and " + nameB + " are half-" + siblingTerm);
		}

		if(na == 1 && nb == 2)
			return nameA + " is the " + uncleTerm(sexA, false) + " of " + nameB;
		if(na == 2 && nb == 1)
			return nameB + " is the " + uncleTerm(sexB, false) + " of " + nameA;

		if(na == 1 && nb == 3)
			return nameA + " is the grand-" + uncleTerm(sexA, false) + " of " + nameB;
		if(na == 3 && nb == 1)
			return nameB + " is the grand-" + uncleTerm(sexB, false) + " of " + nameA;

		final int degree = Math.min(na, nb) - 1;
		final int removed = Math.abs(na - nb);
		return nameA + " and " + nameB + " are " + cousinTerm(degree, removed);
	}

	private static String ancestorTerm(final int distance, final String sex){
		final String base = (ENUM_SEX_MALE.equals(sex)? "father": "mother");
		if(distance == 1)
			return base;
		if(distance == 2)
			return (ENUM_SEX_MALE.equals(sex)? "grandfather": "grandmother");

		final String prefix = (distance == 3? "great-grand": (distance - 2) + "x great-grand");
		return prefix + base;
	}

	private static String uncleTerm(final String sex, final boolean grand){
		final String base = (ENUM_SEX_MALE.equals(sex)? "uncle": "aunt");
		return (grand? "grand-" + base: base);
	}

	private static String siblingTerm(final String sexA, final String sexB){
		final boolean maleA = ENUM_SEX_MALE.equals(sexA);
		final boolean maleB = ENUM_SEX_MALE.equals(sexB);
		if(maleA && maleB)
			return "brothers";
		if(!maleA && !maleB)
			return "sisters";
		return "siblings";
	}

	private static String cousinTerm(final int degree, final int removed){
		final String degreeLabel = switch(degree){
			case 1 -> "first";
			case 2 -> "second";
			case 3 -> "third";
			case 4 -> "fourth";
			case 5 -> "fifth";
			default -> degree + "th";
		};
		final String base = degreeLabel + " cousin";
		if(removed == 0)
			return base + "s";
		if(removed == 1)
			return base + "s once removed";
		return base + "s " + removed + " times removed";
	}


	/* ======================================================================
	 *                          Label and sex lookup
	 * ====================================================================== */

	private String labelOf(final String id){
		if(id == null)
			return StringUtils.EMPTY;

		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return id;

		try{
			final String text = individualHandler.getDisplayText(record, model);
			return (text != null && !text.isBlank()? text: id);
		}
		catch(final RuntimeException ignored){
			return id;
		}
	}

	private String sexOf(final String id){
		if(id == null)
			return StringUtils.EMPTY;

		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return StringUtils.EMPTY;

		final String raw = FLEFRecordHelper.getChildValue(record, TAG_SEX);
		return (raw != null? raw.trim()
			.toLowerCase(Locale.ROOT): StringUtils.EMPTY);
	}

}
