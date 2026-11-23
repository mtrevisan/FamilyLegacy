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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Assertion;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Citation;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Finding;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Inspection;

import java.util.ArrayList;
import java.util.List;


/**
 * Writes an {@link Inspection} in the language of the user: the evidence used, the reasoning chain, the confidence, the alternative
 * interpretations and the conflicting evidence, plus the remarks on the reasoning.
 */
public final class InferenceInspectionText{

	private static final String INDENT = "    ";


	private InferenceInspectionText(){}


	public static String render(final Inspection inspection){
		final StringBuilder text = new StringBuilder();
		heading(text, I18N.tf("text.inference.title", inspection.conclusionId(), orDash(inspection.issue())));
		line(text, I18N.tf("text.inference.status", status(inspection.proofStatus())));
		line(text, I18N.tf("text.inference.confidence", confidence(inspection.confidence())));

		heading(text, I18N.t("text.inference.why"));
		if(inspection.questions().isEmpty())
			line(text, I18N.t("text.inference.why.none"));
		for(final String question : inspection.questions())
			line(text, "- " + question);

		heading(text, I18N.t("text.inference.how"));
		int step = 1;
		for(final String chain : chain(inspection))
			line(text, (step ++) + ". " + chain);

		heading(text, I18N.t("text.inference.evidence"));
		if(inspection.assertions().isEmpty())
			line(text, I18N.t("text.inference.evidence.none"));
		for(final Assertion assertion : inspection.assertions())
			assertion(text, assertion);
		if(!inspection.methodology().isEmpty()){
			line(text, I18N.t("text.inference.methodology"));
			for(final Citation citation : inspection.methodology())
				line(text, INDENT + "- " + citation(citation));
		}

		heading(text, I18N.t("text.inference.alternatives"));
		if(inspection.alternatives().isEmpty())
			line(text, I18N.t("text.inference.alternatives.none"));
		for(final Assertion alternative : inspection.alternatives())
			line(text, "- " + I18N.tf("text.inference.alternative", alternative.label(), alternative.weight(),
				confidence(alternative.confidence()), alternative.supporting().size(), alternative.opposing().size()));

		heading(text, I18N.t("text.inference.conflicts"));
		final List<String> conflicts = conflicts(inspection);
		if(conflicts.isEmpty())
			line(text, I18N.t("text.inference.conflicts.none"));
		for(final String conflict : conflicts)
			line(text, "- " + conflict);

		heading(text, I18N.t("text.inference.checks"));
		if(inspection.findings().isEmpty())
			line(text, I18N.t("text.inference.checks.none"));
		for(final Finding finding : inspection.findings())
			line(text, "- " + (finding.subject() != null
				? I18N.tf("text.inference.finding." + finding.kind().name().toLowerCase(java.util.Locale.ROOT), finding.subject())
				: I18N.t("text.inference.finding." + finding.kind().name().toLowerCase(java.util.Locale.ROOT))));
		return text.toString();
	}

	/** The reasoning chain: the steps that lead from the evidence to the conclusion, each one read from the records. */
	static List<String> chain(final Inspection inspection){
		final List<String> steps = new ArrayList<>();
		steps.add(inspection.questions().isEmpty()
			? I18N.t("text.inference.chain.question.none")
			: I18N.tf("text.inference.chain.question", String.join("; ", inspection.questions())));
		steps.add(I18N.tf("text.inference.chain.competing", inspection.assertions().size()));
		for(final Assertion assertion : inspection.assertions())
			steps.add(I18N.tf("text.inference.chain.weighed", assertion.label(), assertion.supporting().size(), assertion.opposing().size(),
				assertion.weight()));

		final Assertion preferred = inspection.preferred();
		if(preferred == null)
			steps.add(I18N.t("text.inference.chain.preferred.none"));
		else{
			final int best = inspection.alternatives().stream()
				.mapToInt(Assertion::weight)
				.max()
				.orElse(Integer.MIN_VALUE);
			final String how = (inspection.alternatives().isEmpty()? "alone": (preferred.weight() > best? "heavier": (preferred.weight() == best? "equal": "lighter")));
			steps.add(I18N.tf("text.inference.chain.preferred." + how, preferred.label(), preferred.weight()));
		}
		steps.add(I18N.tf("text.inference.chain.status", status(inspection.proofStatus())));
		steps.add(inspection.narrative() == null
			? I18N.t("text.inference.chain.narrative.none")
			: I18N.tf("text.inference.chain.narrative", inspection.narrative()));
		return steps;
	}

	/** The evidence that goes against the conclusion, or that keeps another reading alive. */
	static List<String> conflicts(final Inspection inspection){
		final List<String> conflicts = new ArrayList<>();
		final Assertion preferred = inspection.preferred();
		if(preferred != null)
			for(final Citation citation : preferred.opposing())
				conflicts.add(I18N.tf("text.inference.conflict.against", preferred.label(), citation(citation)));
		for(final Assertion alternative : inspection.alternatives())
			for(final Citation citation : alternative.supporting())
				conflicts.add(I18N.tf("text.inference.conflict.rival", alternative.label(), citation(citation)));
		return conflicts;
	}

	private static void assertion(final StringBuilder text, final Assertion assertion){
		line(text, (assertion.preferred()? "* ": "- ") + I18N.tf(assertion.preferred()
			? "text.inference.assertion.preferred": "text.inference.assertion", assertion.label(), assertion.weight(),
			confidence(assertion.confidence())));
		if(!assertion.known())
			line(text, INDENT + I18N.t("text.inference.assertion.unknown"));
		else if(assertion.citations().isEmpty())
			line(text, INDENT + I18N.t("text.inference.assertion.uncited"));
		for(final Citation citation : assertion.citations())
			line(text, INDENT + (citation.opposing()? "- ": "+ ") + citation(citation));
	}

	private static String citation(final Citation citation){
		final List<String> qualifiers = new ArrayList<>();
		if(citation.contradicting())
			qualifiers.add(I18N.t("text.inference.citation.contradicts"));
		qualifier(qualifiers, "enum.evidence.source_type.", citation.sourceType());
		qualifier(qualifiers, "enum.evidence.information_type.", citation.informationType());
		qualifier(qualifiers, "enum.evidence.evidence_type.", citation.evidenceType());
		final StringBuilder text = new StringBuilder(citation.source());
		if(citation.locator() != null)
			text.append(", ").append(citation.locator());
		if(!qualifiers.isEmpty())
			text.append(" (").append(String.join(", ", qualifiers)).append(')');
		text.append(" [").append(citation.weight() > 0? "+": "").append(citation.weight()).append(']');
		return text.toString();
	}

	private static void qualifier(final List<String> qualifiers, final String prefix, final String value){
		if(value != null)
			qualifiers.add(I18N.t(prefix + value, value));
	}

	private static String status(final String status){
		return (status == null || status.isBlank()
			? I18N.t("text.proof_summary.not_set")
			: I18N.t("enum.proof_status." + status, status));
	}

	private static String confidence(final String confidence){
		return (confidence == null || confidence.isBlank()
			? I18N.t("text.inference.confidence.none")
			: I18N.t("enum.confidence." + confidence, confidence));
	}

	private static String orDash(final String text){
		return (text != null && !text.isBlank()? text: "-");
	}

	private static void heading(final StringBuilder text, final String heading){
		if(!text.isEmpty())
			text.append('\n');
		text.append(heading).append('\n');
	}

	private static void line(final StringBuilder text, final String line){
		text.append(line).append('\n');
	}

}
