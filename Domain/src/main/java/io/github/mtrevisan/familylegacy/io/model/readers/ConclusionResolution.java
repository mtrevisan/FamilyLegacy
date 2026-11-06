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
package io.github.mtrevisan.familylegacy.io.model.readers;

import java.util.Collection;
import java.util.Set;


/**
 * When a conflict between competing assertions is settled: the one definition shared by whoever asks (the merger, that warns about
 * the conflicts it leaves open, and the Conclusion Assistant, that reports them).
 * <p>
 * A conflict is settled when a conclusion <em>evaluates</em> it, that is it resolves at least two of the competing records, and it
 * takes a position: it prefers one of them, or its proof status says that none prevails ({@code conflicting_evidence}: the researcher
 * looked and the evidence does not decide; {@code disproven}: none of them holds). A conclusion that only lists the records, with no
 * preferred assertion and no status or a status that decides nothing (e.g. {@code supported} without a preferred assertion), is a draft, and does not settle anything.
 * </p>
 * The assertions are never changed by a conclusion: this only tells whether the researcher already judged them.
 */
public final class ConclusionResolution{

	/** The proof statuses with which a conclusion settles a conflict without preferring any of the assertions. */
	private static final Set<String> NO_PREVAILING_STATUSES = Set.of(ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE, ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN);


	/**
	 * What is needed of a conclusion to tell whether it settles a conflict.
	 *
	 * @param resolves    the identifiers of the assertions it evaluates
	 * @param preferred   the identifier of the assertion judged most likely to be correct, or {@code null}
	 * @param proofStatus the proof status, or {@code null}
	 */
	public record Evaluation(Set<String> resolves, String preferred, String proofStatus){}


	private ConclusionResolution(){}


	/**
	 * @param competing   the identifiers of the competing records (they may include the participations in an event, and the like:
	 *                    a conclusion can point to any of them)
	 * @param evaluations the conclusions of the research
	 * @return whether one of the conclusions settles the conflict
	 */
	public static boolean isResolved(final Set<String> competing, final Collection<Evaluation> evaluations){
		for(final Evaluation evaluation : evaluations)
			if(settles(competing, evaluation))
				return true;
		return false;
	}

	/** @return whether the conclusion evaluates at least two of the competing records and takes a position on them */
	public static boolean settles(final Set<String> competing, final Evaluation evaluation){
		final long evaluated = evaluation.resolves().stream()
			.filter(competing::contains)
			.count();
		return (evaluated >= 2 && (evaluation.preferred() != null && competing.contains(evaluation.preferred())
			|| evaluation.proofStatus() != null && NO_PREVAILING_STATUSES.contains(evaluation.proofStatus())));
	}

}
