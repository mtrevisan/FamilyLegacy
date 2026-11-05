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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
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
 *
 * <p>The computation walks the ancestor graph from both individuals, finds
 * every common ancestor, and derives:</p>
 * <ul>
 *   <li>the most recent common ancestor (MRCA), i.e. the one minimizing
 *       the total number of generations between the two individuals;</li>
 *   <li>the furthest common ancestor, i.e. the one maximizing that
 *       distance;</li>
 *   <li>the chain of ancestors from each individual up to the MRCA;</li>
 *   <li>a natural-language description of the relationship, following the
 *       standard genealogy vocabulary (father, uncle, first cousin once
 *       removed, and so on);</li>
 *   <li>the Wright's relationship coefficient, computed exactly by
 *       {@link ConsanguinityCalculator} (all the paths through the common
 *       ancestors, and the inbreeding of the ancestors themselves). The
 *       per-ancestor term {@code (1/2)^(dA + dB)} reported with each common
 *       ancestor is only an indication.</li>
 * </ul>
 *
 * <p>The calculator can be built either from a
 * custom {@link ParentProvider} (for instance one that honours the relationship filter configured for a tree), or from a plain {@link FLEFModel} through
 * the {@link #forModel(FLEFModel)} factory, which indexes the model's
 * relationship records once and honours the standard parent/child
 * directions.</p>
 *
 * <p>The ancestor-distance map of every queried individual is cached, so
 * repeated calls (e.g. when describing a whole branch of the tree) do not
 * re-walk the graph.</p>
 */
public final class KinshipCalculator{

	/**
	 * Supplies the parents of a given individual by its ID.
	 *
	 * <p>This functional interface decouples {@link KinshipCalculator} from
	 * any concrete service, so it can be used from other modules (e.g. the
	 * report generator) without pulling in the tree-view dependencies.</p>
	 */
	@FunctionalInterface
	public interface ParentProvider{
		/**
		 * @param id the ID of the individual whose parents are requested
		 * @return the parents of {@code id}; may be empty, never {@code null}
		 */
		List<FLEFRecord> getParents(String id);
	}


	private final FLEFModel model;
	private final ParentProvider parents;
	private final KinshipVocabulary vocabulary;

	/**
	 * Cache of the ancestor-distance maps, keyed by individual ID.
	 * Each value associates every reachable ancestor ID with its generation
	 * distance from the cached root.
	 */
	private final Map<String, Map<String, Integer>> ancestorCache = new HashMap<>();


	/**
	 * Constructor backed by a custom {@link ParentProvider}. Useful when the
	 * caller already has a pre-indexed parent lookup.
	 *
	 * @param model   the FLEF model (must not be {@code null})
	 * @param parents the parent provider (must not be {@code null})
	 */
	public KinshipCalculator(final FLEFModel model, final ParentProvider parents){
		this(model, parents, KinshipVocabulary.ui());
	}

	private KinshipCalculator(final FLEFModel model, final ParentProvider parents, final KinshipVocabulary vocabulary){
		if(parents == null)
			throw new IllegalArgumentException("Parent provider must not be null");

		this.model = model;
		this.parents = parents;
		this.vocabulary = vocabulary;
	}

	/**
	 * @param vocabulary	Where the words of the terms and of the descriptions come from (by default, the messages of the interface).
	 * @return	A calculator on the same model that says the terms in the language of the vocabulary; a report uses the one of its language.
	 */
	public KinshipCalculator withVocabulary(final KinshipVocabulary vocabulary){
		return new KinshipCalculator(model, parents, vocabulary);
	}


	/**
	 * Builds a calculator from a plain model by indexing its relationship
	 * records once. The indexing honours the "_child" / "_parent" suffixes
	 * used by the protocol and skips relationships whose endpoints do not
	 * reference an {@code individual}.
	 *
	 * @param model the model to index (must not be {@code null})
	 * @return a calculator ready to use
	 */
	public static KinshipCalculator forModel(final FLEFModel model){
		final Map<String, List<FLEFRecord>> index = buildParentIndex(model);
		return new KinshipCalculator(model, id -> index.getOrDefault(id, List.of()));
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Returns a short kinship term describing how {@code idA} is related
	 * to {@code idB}, e.g. {@code "father"}, {@code "uncle"},
	 * {@code "nephew"}, {@code "first cousin once removed"},
	 * {@code "grand-aunt"}.
	 *
	 * <p>The term is expressed from {@code idA}'s perspective: if A is B's
	 * uncle, the method returns {@code "uncle"}. Returns {@code "self"} when
	 * the IDs are equal and {@code "unrelated"} when no common ancestor
	 * exists.</p>
	 *
	 * @param idA the ID of the individual whose role is described; may be null
	 * @param idB the ID of the individual the role refers to; may be null
	 * @return the term, never {@code null}
	 */
	public String shortTerm(final String idA, final String idB){
		if(idA == null || idB == null)
			return "unrelated";
		if(idA.equals(idB))
			return "self";

		final Map<String, Integer> ancestorsA = collectAncestorDistances(idA);
		final Map<String, Integer> ancestorsB = collectAncestorDistances(idB);

		final Set<String> commonIds = new HashSet<>(ancestorsA.keySet());
		commonIds.retainAll(ancestorsB.keySet());
		if(commonIds.isEmpty())
			return "unrelated";

		// Find the MRCA (minimal total distance).
		int bestTotal = Integer.MAX_VALUE;
		int na = 0;
		int nb = 0;
		for(final String id : commonIds){
			final int dA = ancestorsA.get(id);
			final int dB = ancestorsB.get(id);
			if(dA + dB < bestTotal){
				bestTotal = dA + dB;
				na = dA;
				nb = dB;
			}
		}

		return termFor(na, nb, sexOf(idA));
	}


	/* ======================================================================
	 *                          Main calculation
	 * ====================================================================== */

	/**
	 * Computes the kinship between the two individuals. See the class-level
	 * documentation for what information is returned.
	 *
	 * @param idA the id of the first individual; may be {@code null}
	 * @param idB the id of the second individual; may be {@code null}
	 * @return the result, never {@code null}
	 */
	public KinshipResult calculate(final String idA, final String idB){
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

		// Build the common-ancestor list. The term (1/2)^(dA + dB) of each
		// one (dA and dB are its generation distances from A and B) is
		// only informative: the coefficient R is computed exactly below.
		final List<KinshipResult.CommonAncestorInfo> commons = new ArrayList<>(commonIds.size());
		for(final String ancestorId : commonIds){
			final int dA = ancestorsA.get(ancestorId);
			final int dB = ancestorsB.get(ancestorId);
			final double contribution = Math.pow(0.5, dA + dB);
			commons.add(new KinshipResult.CommonAncestorInfo(ancestorId, labelOf(ancestorId), dA, dB, contribution));
		}
		final double coefficient = relationshipCoefficient(idA, idB, ancestorsA.keySet(), ancestorsB.keySet());

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
		//   Direct line: max(dA, dB)
		//   Collateral:  dA + dB
		// ------------------------------------------------------------------
		final int civilDegree = (directLine? Math.max(dA, dB): dA + dB);

		// ------------------------------------------------------------------
		// Canonical degree: Catholic Church. The same numeric value is
		// also the Germanic "knee" number.
		//   Direct line: max(dA, dB)
		//   Collateral:  min(dA, dB)
		// ------------------------------------------------------------------
		final int canonicalDegree = (directLine? Math.max(dA, dB): Math.min(dA, dB));

		// ------------------------------------------------------------------
		// Chinese generation count (PRC Marriage Law). Self is counted as
		// the first generation.
		// ------------------------------------------------------------------
		final int chineseGeneration = 1 + Math.max(dA, dB);

		return new KinshipResult(idA, labelOf(idA), idB, labelOf(idB),
			sexA, sexB, chainA, chainB, mrca, furthest, commons, description, coefficient,
			directLine, civilDegree, canonicalDegree, chineseGeneration);
	}


	/**
	 * The exact coefficient of relationship of Wright, through {@link ConsanguinityCalculator} (every path through every common
	 * ancestor counts, and so does the inbreeding of the ancestors). The ancestry is the one of the parent provider, limited to the
	 * ancestors of the two individuals (all that matters), with at most two parents each.
	 */
	private double relationshipCoefficient(final String idA, final String idB, final Set<String> ancestorsOfA,
			final Set<String> ancestorsOfB){
		final Set<String> ids = new HashSet<>(ancestorsOfA);
		ids.addAll(ancestorsOfB);
		final Map<String, List<String>> parentsByChild = new HashMap<>(ids.size());
		for(final String id : ids){
			final List<String> parentIds = new ArrayList<>(2);
			for(final FLEFRecord parent : parents.getParents(id))
				if(parent.getId() != null)
					parentIds.add(parent.getId());
			if(!parentIds.isEmpty())
				parentsByChild.put(id, parentIds);
		}
		return new ConsanguinityCalculator(new AncestryGraph(parentsByChild))
			.relationship(idA, idB);
	}


	/* ======================================================================
	 *                          Ancestor walking
	 * ====================================================================== */

	/**
	 * Returns the cached ancestor-distance map of the given individual,
	 * computing it on first access.
	 */
	private Map<String, Integer> collectAncestorDistances(final String rootId){
		return ancestorCache.computeIfAbsent(rootId, this::computeAncestorDistances);
	}

	/**
	 * Breadth-first walk of the ancestor graph starting from the given
	 * individual. Returns a map from every reachable ancestor id to its
	 * generation distance from the starting individual. The starting
	 * individual itself is included with distance 0.
	 */
	private Map<String, Integer> computeAncestorDistances(final String rootId){
		final Map<String, Integer> distances = new HashMap<>();
		final Deque<String> queue = new ArrayDeque<>();
		distances.put(rootId, 0);
		queue.add(rootId);
		while(!queue.isEmpty()){
			final String id = queue.poll();
			final int distance = distances.get(id);

			for(final FLEFRecord parent : parents.getParents(id)){
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
			for(final FLEFRecord parent : parents.getParents(id)){
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

	private String describe(final KinshipResult.CommonAncestorInfo mrca,
		final List<KinshipResult.CommonAncestorInfo> commons, final String nameA, final String nameB,
		final String sexA, final String sexB){
		final int na = mrca.distanceFromA();
		final int nb = mrca.distanceFromB();

		if(na == 0)
			return vocabulary.text("kinship.sentence.is_the", nameA, ancestorTerm(nb, sexA), nameB);
		if(nb == 0)
			return vocabulary.text("kinship.sentence.is_the", nameB, ancestorTerm(na, sexB), nameA);

		if(na == 1 && nb == 1){
			final long sharedParents = commons.stream()
				.filter(c -> c.distanceFromA() == 1 && c.distanceFromB() == 1)
				.count();
			return vocabulary.text((sharedParents >= 2? "kinship.sentence.full_siblings": "kinship.sentence.half_siblings"), nameA, nameB,
				siblingTerm(sexA, sexB));
		}

		if(na == 1 && nb == 2)
			return vocabulary.text("kinship.sentence.is_the", nameA, uncleTerm(sexA, false), nameB);
		if(na == 2 && nb == 1)
			return vocabulary.text("kinship.sentence.is_the", nameB, uncleTerm(sexB, false), nameA);

		if(na == 1 && nb == 3)
			return vocabulary.text("kinship.sentence.is_the", nameA, uncleTerm(sexA, true), nameB);
		if(na == 3 && nb == 1)
			return vocabulary.text("kinship.sentence.is_the", nameB, uncleTerm(sexB, true), nameA);

		final int degree = Math.min(na, nb) - 1;
		final int removed = Math.abs(na - nb);
		return vocabulary.text("kinship.sentence.are", nameA, nameB, KinshipTerms.cousins(vocabulary, degree, removed));
	}


	/**
	 * Returns the short term describing how an individual at the given
	 * {@code na},{@code nb} distances from the MRCA is related to the other.
	 * {@code sexA} is the sex of the individual whose role is described.
	 */
	private String termFor(final int na, final int nb, final String sexA){
		if(na == 0)
			return ancestorTerm(nb, sexA);
		if(nb == 0)
			return descendantTerm(na, sexA);
		if(na == 1 && nb == 1)
			return siblingSingularTerm(sexA);
		if(na == 1 && nb == 2)
			return uncleTerm(sexA, false);
		if(na == 2 && nb == 1)
			return nephewTerm(sexA, false);
		if(na == 1 && nb == 3)
			return uncleTerm(sexA, true);
		if(na == 3 && nb == 1)
			return nephewTerm(sexA, true);

		return KinshipTerms.cousin(vocabulary, Math.min(na, nb) - 1, Math.abs(na - nb), IndividualReader.isSexFemale(sexA));
	}


	/** The word for the sex: the one of the male, the one of the female, or "unknown" when the sex is not known. */
	private String bySex(final String sex, final String maleKey, final String femaleKey, final Object... args){
		if(IndividualReader.isSexMale(sex))
			return vocabulary.text(maleKey, args);
		if(IndividualReader.isSexFemale(sex))
			return vocabulary.text(femaleKey, args);
		return vocabulary.text("kinship.term.unknown");
	}

	private String ancestorTerm(final int distance, final String sex){
		return bySex(sex, "kinship.term.ancestor.male", "kinship.term.ancestor.female", distance, distance - 2);
	}

	private String descendantTerm(final int distance, final String sex){
		return bySex(sex, "kinship.term.descendant.male", "kinship.term.descendant.female", distance, distance - 2);
	}

	private String uncleTerm(final String sex, final boolean grand){
		return (grand
			? bySex(sex, "kinship.term.great_uncle.male", "kinship.term.great_uncle.female")
			: bySex(sex, "kinship.term.uncle.male", "kinship.term.uncle.female"));
	}

	private String nephewTerm(final String sex, final boolean grand){
		return (grand
			? bySex(sex, "kinship.term.great_nephew.male", "kinship.term.great_nephew.female")
			: bySex(sex, "kinship.term.nephew.male", "kinship.term.nephew.female"));
	}

	private String siblingTerm(final String sexA, final String sexB){
		if(IndividualReader.isSexMale(sexA) && IndividualReader.isSexMale(sexB))
			return vocabulary.text("kinship.term.brothers");
		if(IndividualReader.isSexFemale(sexA) && IndividualReader.isSexFemale(sexB))
			return vocabulary.text("kinship.term.sisters");
		return vocabulary.text("kinship.term.siblings");
	}

	private String siblingSingularTerm(final String sex){
		return bySex(sex, "kinship.term.sibling.male", "kinship.term.sibling.female");
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
			final String text = IndividualReader.displayText(record);
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

		final String raw = IndividualReader.extractRawSex(record);
		return (raw != null? raw: StringUtils.EMPTY);
	}


	/* ======================================================================
	 *                          Model indexing
	 * ====================================================================== */

	/**
	 * Builds a {@code childId -> [parents]} index from the model's
	 * relationship records.
	 */
	private static Map<String, List<FLEFRecord>> buildParentIndex(final FLEFModel model){
		final Map<String, List<FLEFRecord>> index = new HashMap<>();
		List<FLEFRecord> relationships = PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE));
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null)
				continue;

			final String t = type.toLowerCase(Locale.ROOT);
			final String subject = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String object = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(subject == null || object == null)
				continue;

			final boolean childRel = t.equals(RelationshipReader.ENUM_TYPE_CHILD) || t.endsWith("_child");
			final boolean parentRel = t.endsWith("_parent");
			if(!childRel && !parentRel)
				continue;

			final String childId = (childRel? subject: object);
			final String parentId = (childRel? object: subject);

			final FLEFRecord parent = model.getRecordById(parentId);
			if(parent == null)
				continue;

			index.computeIfAbsent(childId, k -> new ArrayList<>())
				.add(parent);
		}
		return index;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

}
