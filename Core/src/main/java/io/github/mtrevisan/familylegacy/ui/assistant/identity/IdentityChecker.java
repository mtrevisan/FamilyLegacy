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
package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.tools.DuplicateFinderService;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Turns the pairs of individuals that the duplicate finder finds into findings of the Research Assistant: what makes them look like
 * the same person, what makes them differ, how sure the similarity is.
 * <p>
 * The comparison is the one of the duplicate finder (names, parents, children, spouses, siblings, dates, events, attributes, sources
 * and places, with the sex as a hard filter), not a second one. The assistant only suggests: a pair is a possibility, not a
 * probability, and nothing is merged. The researcher can record the possibility as an identity hypothesis (a draft to confirm),
 * compare the two records side by side, or ignore the pair; to say that they are different people for good, a conclusion that
 * disproves the hypothesis is the way of the protocol.
 * </p>
 */
final class IdentityChecker{

	/** The similarity score from which a pair is a strong candidate (a warning), and the confidence proposed for its hypothesis is high. */
	static final int HIGH_SCORE = 90;
	/** The similarity score from which the confidence proposed for the hypothesis is medium; below it, low. */
	static final int MEDIUM_SCORE = 70;

	private static final String CONFIDENCE_HIGH = "high";
	private static final String CONFIDENCE_MEDIUM = "medium";
	private static final String CONFIDENCE_LOW = "low";


	private IdentityChecker(){}


	/** @return the findings, without the drafts of the hypotheses */
	static List<Diagnostic> check(final List<DuplicateFinderService.Candidate> candidates){
		return findings(candidates).stream()
			.map(IdentityFinding::diagnostic)
			.toList();
	}

	/**
	 * @param candidates the pairs of individuals that may be the same person
	 * @return a finding for each pair, with the draft of the hypothesis that would record it
	 */
	static List<IdentityFinding> findings(final List<DuplicateFinderService.Candidate> candidates){
		return candidates.stream()
			.map(IdentityChecker::finding)
			.toList();
	}

	/** @return the confidence proposed for the hypothesis of a pair with that similarity score */
	static String confidence(final int score){
		if(score >= HIGH_SCORE)
			return CONFIDENCE_HIGH;
		return (score >= MEDIUM_SCORE? CONFIDENCE_MEDIUM: CONFIDENCE_LOW);
	}


	private static IdentityFinding finding(final DuplicateFinderService.Candidate candidate){
		final String leftId = candidate.left().id();
		final String rightId = candidate.right().id();
		final String left = candidate.left().displayName();
		final String right = candidate.right().displayName();
		final int score = candidate.score();
		final String confidence = confidence(score);

		final String similarities = candidate.matches().stream()
			.map(m -> m.feature() + ": " + m.detail() + " (+" + m.score() + ")")
			.collect(Collectors.joining("; "));
		final String differences = String.join("; ", candidate.differences());
		final String why = (similarities.isEmpty()? "-": similarities);
		final String differ = (differences.isEmpty()? "-": differences);

		//the key is the pair: ignoring a candidate is about the two records, not about how well they score today
		final Diagnostic diagnostic = new Diagnostic((score >= HIGH_SCORE? Diagnostic.Severity.WARNING: Diagnostic.Severity.INFORMATION),
			AssistantCategory.IDENTITY, I18N.tf("identity.possible_duplicate.title", left, right, score),
			I18N.tf("identity.possible_duplicate.explanation", left, right, score, I18N.t("enum.confidence." + confidence), why, differ),
			List.of(leftId, rightId), null, DiagnosticKeys.of("identity.possible_duplicate", List.of(leftId, rightId)),
			List.of(), score);
		final String rationale = I18N.tf("identity.rationale", score, why, differ);
		return new IdentityFinding(diagnostic, new IdentityDraft(leftId, rightId, rationale, confidence));
	}

}
