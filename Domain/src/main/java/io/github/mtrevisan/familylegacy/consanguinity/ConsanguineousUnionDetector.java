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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


/**
 * Finds the couples whose partners are related, and the individuals whose parents are related.
 * <p>
 * A couple is a pair of individuals linked by a union (a marriage of any kind, or a cohabitation: not an engagement) or that have a
 * biological child together: the child counts even if the union is not recorded. The partners are related if they have a common
 * ancestor in the recorded ancestry (see {@link ConsanguinityCalculator}), and the couple is reported if their kinship coefficient
 * reaches the minimum (by default 1/256, that of third cousins). The values are the ones of the recorded ancestry, and a wrong link
 * changes them: it is a list of possibilities to look into, never a judgement.
 * </p>
 */
public final class ConsanguineousUnionDetector{

	/** The kinship coefficient from which a couple is reported: third cousins (1/256) and closer. */
	public static final double DEFAULT_MINIMUM_KINSHIP = 1. / 256.;
	/** The coefficient of inbreeding from which an individual is reported: the child of second cousins (1/64) and closer. */
	public static final double DEFAULT_MINIMUM_INBREEDING = 1. / 64.;

	private static final String TYPE_ENGAGED_PARTNER = "engaged_partner";


	/**
	 * Two individuals that form a couple.
	 *
	 * @param firstId          a partner
	 * @param secondId         the other partner
	 * @param relationshipIds  the relationships that unite them
	 * @param childIds         their biological children
	 */
	public record Couple(String firstId, String secondId, Set<String> relationshipIds, List<String> childIds){

		public Couple{
			relationshipIds = Set.copyOf(relationshipIds);
			childIds = List.copyOf(childIds);
		}

	}


	private ConsanguineousUnionDetector(){}


	/** @return	The report of the model, with the default minimums. */
	public static ConsanguinityReport detect(final FLEFModel model){
		final AncestryGraph graph = AncestryGraph.of(model);
		return detect(graph, couples(model, graph), id -> nameOf(model, id), DEFAULT_MINIMUM_KINSHIP, DEFAULT_MINIMUM_INBREEDING);
	}

	/**
	 * @param graph            the ancestry
	 * @param couples          the couples to look at
	 * @param names            the name to show of an individual
	 * @param minimumKinship   the kinship coefficient from which a couple is reported
	 * @param minimumInbreeding the coefficient of inbreeding from which an individual is reported
	 * @return	The report.
	 */
	public static ConsanguinityReport detect(final AncestryGraph graph, final Collection<Couple> couples,
			final Function<String, String> names, final double minimumKinship, final double minimumInbreeding){
		final ConsanguinityCalculator calculator = new ConsanguinityCalculator(graph);

		final List<ConsanguinityReport.ConsanguineousUnion> unions = unionsOf(calculator, couples, names, minimumKinship);

		final List<ConsanguinityReport.InbredIndividual> inbred = new ArrayList<>();
		for(final String id : graph.childrenWithTwoParents()){
			final double inbreeding = calculator.inbreeding(id);
			if(inbreeding > 0. && inbreeding >= minimumInbreeding){
				final List<String> parents = graph.parentsOf(id);
				inbred.add(new ConsanguinityReport.InbredIndividual(id, names.apply(id), inbreeding, parents,
					calculator.closestCommonAncestors(parents.get(0), parents.get(1))));
			}
		}
		inbred.sort(Comparator.comparingDouble(ConsanguinityReport.InbredIndividual::inbreeding)
			.reversed()
			.thenComparing(ConsanguinityReport.InbredIndividual::name));

		return new ConsanguinityReport(unions, inbred, minimumKinship, minimumInbreeding);
	}

	/**
	 * The couples among the given ones whose partners are related, the closest relatives first.
	 * <p>
	 * It is the part of {@link #detect(AncestryGraph, Collection, Function, double, double)} that is about the couples, apart so that
	 * the couples of a single individual can be looked at without the whole file: the cost is the one of those couples, not of the
	 * file. The calculator keeps what it has computed, so the more it is reused the cheaper the next call is (it is not thread safe).
	 * </p>
	 *
	 * @param calculator      the calculator of the ancestry
	 * @param couples         the couples to look at
	 * @param names           the name to show of an individual
	 * @param minimumKinship  the kinship coefficient from which a couple is reported
	 * @return	The unions that reach the minimum.
	 */
	public static List<ConsanguinityReport.ConsanguineousUnion> unionsOf(final ConsanguinityCalculator calculator,
			final Collection<Couple> couples, final Function<String, String> names, final double minimumKinship){
		final List<ConsanguinityReport.ConsanguineousUnion> unions = new ArrayList<>();
		for(final Couple couple : couples){
			if(couple.firstId().equals(couple.secondId()))
				continue;

			final double kinship = calculator.kinship(couple.firstId(), couple.secondId());
			if(kinship > 0. && kinship >= minimumKinship)
				unions.add(new ConsanguinityReport.ConsanguineousUnion(couple.firstId(), names.apply(couple.firstId()),
					couple.secondId(), names.apply(couple.secondId()), new ArrayList<>(couple.relationshipIds()), couple.childIds(),
					kinship, calculator.relationship(couple.firstId(), couple.secondId()),
					calculator.closestCommonAncestors(couple.firstId(), couple.secondId())));
		}
		unions.sort(Comparator.comparingDouble(ConsanguinityReport.ConsanguineousUnion::kinship)
			.reversed()
			.thenComparing(ConsanguinityReport.ConsanguineousUnion::firstName)
			.thenComparing(ConsanguinityReport.ConsanguineousUnion::secondName));
		return unions;
	}

	/** @return	The couples each individual is a partner of (the two partners of a couple both have it). */
	public static Map<String, List<Couple>> couplesByIndividual(final Collection<Couple> couples){
		final Map<String, List<Couple>> result = new LinkedHashMap<>();
		for(final Couple couple : couples){
			result.computeIfAbsent(couple.firstId(), k -> new ArrayList<>())
				.add(couple);
			if(!couple.secondId().equals(couple.firstId()))
				result.computeIfAbsent(couple.secondId(), k -> new ArrayList<>())
					.add(couple);
		}
		return result;
	}

	/**
	 * @param model the model
	 * @param graph the ancestry of the model
	 * @return	The couples of the model: the ones united by a relationship, and the parents of the same child.
	 */
	public static List<Couple> couples(final FLEFModel model, final AncestryGraph graph){
		final Set<String> visible = AncestryGraph.visibleIndividuals(model);

		final Map<String, Set<String>> relationshipsByPair = new LinkedHashMap<>();
		final Map<String, List<String>> childrenByPair = new LinkedHashMap<>();
		final Map<String, String[]> pairs = new LinkedHashMap<>();

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipReader.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null || !RelationshipReader.PARTNER_TYPES.contains(type) || TYPE_ENGAGED_PARTNER.equals(type))
				continue;

			final String first = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String second = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(first == null || second == null || first.equals(second) || !visible.contains(first) || !visible.contains(second))
				continue;

			final String key = pairKey(first, second, pairs);
			relationshipsByPair.computeIfAbsent(key, k -> new LinkedHashSet<>())
				.add(relationship.getId());
		}

		for(final String child : graph.childrenWithTwoParents()){
			final List<String> parents = graph.parentsOf(child);
			if(parents.get(0).equals(parents.get(1)))
				continue;

			final String key = pairKey(parents.get(0), parents.get(1), pairs);
			childrenByPair.computeIfAbsent(key, k -> new ArrayList<>())
				.add(child);
		}

		final List<Couple> couples = new ArrayList<>();
		for(final Map.Entry<String, String[]> entry : pairs.entrySet())
			couples.add(new Couple(entry.getValue()[0], entry.getValue()[1],
				relationshipsByPair.getOrDefault(entry.getKey(), Set.of()), childrenByPair.getOrDefault(entry.getKey(), List.of())));
		return couples;
	}


	private static String pairKey(final String a, final String b, final Map<String, String[]> pairs){
		final String first = (a.compareTo(b) <= 0? a: b);
		final String second = (first == a? b: a);
		final String key = first + '\u0000' + second;
		pairs.putIfAbsent(key, new String[]{first, second});
		return key;
	}

	/** @return	The name to show of an individual: the primary full name, or the identifier if it has none. */
	public static String nameOf(final FLEFModel model, final String id){
		final FLEFRecord record = model.getRecordById(id);
		final String name = (record != null? IndividualReader.extractPrimaryFullname(record): null);
		return (name != null && !name.isBlank()? name: id);
	}

}
