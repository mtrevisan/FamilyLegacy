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

import java.util.List;


/**
 * What the consanguinity of a file looks like: the couples whose partners are related and the individuals that are inbred.
 * <p>
 * It is a report on the recorded ancestry (see {@link ConsanguinityCalculator}): a possibility to look into, not a finding about
 * anybody. Nothing is changed by it.
 * </p>
 *
 * @param unions             the couples whose partners are related, the closest relatives first
 * @param inbredIndividuals  the individuals whose parents are related, the most inbred first
 * @param minimumKinship     the kinship coefficient from which a couple is reported
 * @param minimumInbreeding  the coefficient of inbreeding from which an individual is reported
 */
public record ConsanguinityReport(List<ConsanguineousUnion> unions, List<InbredIndividual> inbredIndividuals, double minimumKinship,
		double minimumInbreeding){

	public ConsanguinityReport{
		unions = List.copyOf(unions);
		inbredIndividuals = List.copyOf(inbredIndividuals);
	}


	/** @return	Whether there is nothing to report. */
	public boolean isEmpty(){
		return (unions.isEmpty() && inbredIndividuals.isEmpty());
	}


	/**
	 * A couple whose partners are related. It is a couple if they are linked by a union (marriage, cohabitation...) or if they have
	 * a biological child together.
	 *
	 * @param firstId          the identifier of a partner
	 * @param firstName        the name of the partner
	 * @param secondId         the identifier of the other partner
	 * @param secondName       the name of the other partner
	 * @param relationshipIds  the identifiers of the relationships that unite them (empty if they are known only as parents)
	 * @param childIds         the identifiers of their biological children
	 * @param kinship          the kinship coefficient of the partners: the coefficient of inbreeding that a child of theirs has
	 *                         (from this ancestry)
	 * @param relationship     the coefficient of relationship {@code R} of the partners
	 * @param commonAncestors  the closest common ancestors of the partners, the nearest first
	 */
	public record ConsanguineousUnion(String firstId, String firstName, String secondId, String secondName,
			List<String> relationshipIds, List<String> childIds, double kinship, double relationship,
			List<ConsanguinityCalculator.CommonAncestor> commonAncestors){

		public ConsanguineousUnion{
			relationshipIds = List.copyOf(relationshipIds);
			childIds = List.copyOf(childIds);
			commonAncestors = List.copyOf(commonAncestors);
		}


		/** @return	The kinship of the partners, by name (from their closest common ancestor); empty if there is none. */
		public String term(){
			return (commonAncestors.isEmpty()
				? ""
				: KinshipTerms.describe(commonAncestors.getFirst().stepsFromFirst(), commonAncestors.getFirst().stepsFromSecond()));
		}

	}


	/**
	 * An individual whose parents are related.
	 *
	 * @param id               the identifier of the individual
	 * @param name             the name of the individual
	 * @param inbreeding       the coefficient of inbreeding {@code F}
	 * @param parentIds        the identifiers of the parents
	 * @param commonAncestors  the closest common ancestors of the parents, the nearest first
	 */
	public record InbredIndividual(String id, String name, double inbreeding, List<String> parentIds,
			List<ConsanguinityCalculator.CommonAncestor> commonAncestors){

		public InbredIndividual{
			parentIds = List.copyOf(parentIds);
			commonAncestors = List.copyOf(commonAncestors);
		}


		/** @return	The kinship of the parents, by name (from their closest common ancestor); empty if there is none. */
		public String term(){
			return (commonAncestors.isEmpty()
				? ""
				: KinshipTerms.describe(commonAncestors.getFirst().stepsFromFirst(), commonAncestors.getFirst().stepsFromSecond()));
		}

	}

}
