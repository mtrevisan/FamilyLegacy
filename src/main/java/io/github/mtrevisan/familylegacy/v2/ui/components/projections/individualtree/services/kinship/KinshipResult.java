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

import java.util.List;


/**
 * Immutable outcome of a kinship computation between two individuals.
 * <p>
 * The result is produced by {@link KinshipCalculator} and consumed by
 * {@link KinshipDialog}. It contains the two chains from each individual
 * up to the most recent common ancestor (MRCA), the MRCA itself, the
 * furthest common ancestor found, the complete list of common ancestors
 * with their contributions, a natural-language description of the
 * relationship, and the kinship degrees computed according to the legal
 * and religious systems described below.
 * <p>
 * <b>Kinship systems computed.</b> The distances {@code dA} and {@code dB}
 * from each individual to the MRCA are the primitive values from which
 * every degree is derived:
 * <ul>
 *   <li><b>Civil (Roman) degree</b> — direct line: {@code max(dA, dB)};
 *       collateral line: {@code dA + dB}. Used by Italy, Germany, Japan,
 *       and historically by the whole Roman-law tradition.</li>
 *   <li><b>Canonical degree</b> — direct line: {@code max(dA, dB)};
 *       collateral line: {@code min(dA, dB)}. Used by the Catholic Church
 *       and derived from the Germanic legal tradition.</li>
 *   <li><b>Germanic knee number</b> — same numerical value as the
 *       canonical degree; the two systems share the same computation but
 *       belong to historically distinct traditions.</li>
 *   <li><b>Korean chon</b> — same numerical value as the civil degree.
 *       The Korean system counts the same way as the Roman one, with a
 *       different name.</li>
 *   <li><b>Chinese generation</b> — {@code 1 + max(dA, dB)}. In the PRC
 *       system the self is counted as the first generation, so parent and
 *       child are at the second generation. Used to decide whether a
 *       marriage falls within the "three generations" prohibition.</li>
 * </ul>
 * <p>
 * <b>Marriage prohibitions.</b> Whether a marriage between the two
 * individuals would be prohibited or would require a dispensation depends
 * on the system and, in some cases, on the sex of the two individuals.
 * The record exposes this through the {@code isMarriageProhibited*}
 * methods and through {@link #requiresCanonicalDispensation()}, which
 * returns {@code true} only when the two individuals are of opposite sex,
 * because the Catholic Church does not recognize same-sex marriage.
 *
 * @param idA                     the id of A
 * @param displayA                the display name of A
 * @param idB                     the id of B
 * @param displayB                the display name of B
 * @param sexA                    the sex of A ({@code male}, {@code female}, or empty)
 * @param sexB                    the sex of B
 * @param chainA                  the chain from A up to the MRCA
 * @param chainB                  the chain from B up to the MRCA
 * @param mrca                    the most recent common ancestor, or {@code null}
 * @param furthestCommonAncestor  the furthest common ancestor, or {@code null}
 * @param allCommonAncestors      the full list of common ancestors
 * @param relationshipDescription the natural-language description
 * @param relationshipCoefficient the Wright's relationship coefficient
 * @param directLine              whether one of the two is an ancestor of the other
 * @param civilDegree             the civil (Roman) degree
 * @param canonicalDegree         the canonical degree
 * @param chineseGeneration       the Chinese generation count
 */
record KinshipResult(
	String idA, String displayA,
	String idB, String displayB,
	String sexA, String sexB,
	List<ChainEntry> chainA, List<ChainEntry> chainB,
	CommonAncestorInfo mrca,
	CommonAncestorInfo furthestCommonAncestor,
	List<CommonAncestorInfo> allCommonAncestors,
	String relationshipDescription,
	double relationshipCoefficient,
	boolean directLine,
	int civilDegree,
	int canonicalDegree,
	int chineseGeneration
){

	private static final String ENUM_SEX_MALE = "male";
	private static final String ENUM_SEX_FEMALE = "female";


	/**
	 * A single step in the chain from one individual up to the MRCA.
	 *
	 * @param id      the individual id
	 * @param display the display name
	 * @param step    the step number (0 = the individual itself)
	 */
	record ChainEntry(String id, String display, int step){}

	/**
	 * One common ancestor shared by the two individuals.
	 *
	 * @param id            the ancestor id
	 * @param display       the display name
	 * @param distanceFromA number of generations from A up to this ancestor
	 * @param distanceFromB number of generations from B up to this ancestor
	 * @param contribution  the term {@code (1/2)^(distanceFromA + distanceFromB)}
	 */
	record CommonAncestorInfo(String id, String display, int distanceFromA, int distanceFromB, double contribution){}


	/* ======================================================================
	 *                          Factories
	 * ====================================================================== */

	/**
	 * Creates a result representing two unrelated individuals.
	 *
	 * @param idA      the id of A
	 * @param displayA the display name of A
	 * @param idB      the id of B
	 * @param displayB the display name of B
	 * @return the result
	 */
	static KinshipResult notRelated(final String idA, final String displayA, final String idB, final String displayB){
		return new KinshipResult(idA, displayA, idB, displayB, "", "",
			List.of(), List.of(), null, null, List.of(),
			"No common ancestor found in the current data.", 0.,
			false, 0, 0, 0);
	}

	/**
	 * Creates a result representing the same individual.
	 *
	 * @param id      the id
	 * @param display the display name
	 * @return the result
	 */
	static KinshipResult sameIndividual(final String id, final String display){
		final ChainEntry self = new ChainEntry(id, display, 0);
		return new KinshipResult(id, display, id, display, "", "",
			List.of(self), List.of(self), null, null, List.of(),
			"The two individuals are the same person.", 1.,
			false, 0, 0, 0);
	}


	/* ======================================================================
	 *                          Basic accessors
	 * ====================================================================== */

	/**
	 * Returns whether the two individuals are the same person.
	 */
	boolean isSameIndividual(){
		return (idA != null && idA.equals(idB));
	}

	/**
	 * Returns whether the two individuals share at least one ancestor.
	 */
	boolean isRelated(){
		return (mrca != null);
	}

	/**
	 * Returns the total number of steps between the two individuals,
	 * going up from A to the MRCA and down from the MRCA to B, or -1 if
	 * they are not related.
	 */
	int totalSteps(){
		return (isRelated()? mrca.distanceFromA() + mrca.distanceFromB(): -1);
	}

	/**
	 * Returns whether the two individuals are of opposite sex.
	 * Used to decide whether systems that only concern man-woman marriage
	 * apply to this pair.
	 */
	boolean areOppositeSex(){
		return (ENUM_SEX_MALE.equals(sexA) && ENUM_SEX_FEMALE.equals(sexB)
			|| ENUM_SEX_FEMALE.equals(sexA) && ENUM_SEX_MALE.equals(sexB));
	}


	/* ======================================================================
	 *                          Derived degrees
	 * ====================================================================== */

	/**
	 * Returns the Korean {@code chon} value, which uses the same numeric
	 * computation as the civil (Roman) degree.
	 */
	int koreanChon(){
		return civilDegree;
	}

	/**
	 * Returns the Germanic "knee" number, which uses the same numeric
	 * computation as the canonical degree.
	 */
	int germanicKnee(){
		return canonicalDegree;
	}


	/* ======================================================================
	 *                          Relevance ranges
	 * ====================================================================== */

	/**
	 * Returns whether the civil degree falls within the range recognized
	 * by Italian law (up to the 6th degree).
	 */
	boolean isCivillyRelevant(){
		return (civilDegree >= 1 && civilDegree <= 6);
	}

	/**
	 * Returns whether the canonical degree falls within the range
	 * recognized by the Catholic Church (up to the 4th degree).
	 */
	boolean isCanonicallyRelevant(){
		return (canonicalDegree >= 1 && canonicalDegree <= 4);
	}

	/**
	 * Returns whether the Chinese generation falls within the "three
	 * generations" range used by the PRC Marriage Law.
	 */
	boolean isChineseRelevant(){
		return (chineseGeneration >= 1 && chineseGeneration <= 3);
	}


	/* ======================================================================
	 *                          Marriage prohibitions
	 * ====================================================================== */

	/**
	 * Returns whether a canonical dispensation would be required for a
	 * marriage between the two individuals.
	 * <p>
	 * The dispensation is required only when the two individuals are of
	 * opposite sex, because the Catholic Church does not recognize
	 * same-sex marriage. Within the 4th canonical degree the
	 * dispensation is mandatory; beyond it, no dispensation is required.
	 */
	boolean requiresCanonicalDispensation(){
		return (areOppositeSex() && isCanonicallyRelevant());
	}

	/**
	 * Returns whether the marriage is prohibited under Italian civil law
	 * (article 87 of the civil code): always prohibited in the direct
	 * line, prohibited up to the 4th civil degree in the collateral line.
	 * The prohibition is independent of sex.
	 */
	boolean isMarriageProhibitedCivilly(){
		if(civilDegree <= 0)
			return false;
		if(directLine)
			return true;
		return civilDegree <= 4;
	}

	/**
	 * Returns whether the marriage is prohibited under Catholic canon
	 * law: always prohibited in the direct line, prohibited up to the
	 * 4th canonical degree in the collateral line. The prohibition only
	 * has legal meaning for opposite-sex couples.
	 */
	boolean isMarriageProhibitedCanonically(){
		if(canonicalDegree <= 0)
			return false;
		if(directLine)
			return true;
		return canonicalDegree <= 4;
	}

	/**
	 * Returns whether the marriage is prohibited under the PRC Marriage
	 * Law: always prohibited in the direct line, prohibited for
	 * collateral relatives within three generations.
	 */
	boolean isMarriageProhibitedChinese(){
		if(chineseGeneration <= 0)
			return false;
		if(directLine)
			return true;
		return chineseGeneration <= 3;
	}

}
