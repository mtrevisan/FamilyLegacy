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
package io.github.mtrevisan.familylegacy.ui.assistant.consanguinity;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReportText;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Turns the consanguinity report into the findings of the Research Assistant. It reports and explains: nothing is changed.
 * <p>
 * A couple whose partners are first cousins or closer (kinship of 1/16 or more) is a warning, the others (down to third cousins) are
 * information; the closest relatives come first. The most that are listed one by one is {@link #MAX_UNIONS}, the others are one
 * finding, so that a village where everybody is a third cousin of everybody does not fill the palette. The inbred individuals are one
 * finding, with the most inbred listed.
 * </p>
 */
final class ConsanguinityChecker{

	/** The couples listed one by one. */
	static final int MAX_UNIONS = 25;
	/** The records that a finding about many lists at most. */
	private static final int MAX_LISTED = 200;
	/** The individuals named in the explanation of the finding about the inbred ones. */
	private static final int MAX_NAMED = 10;
	/** The kinship coefficient from which a couple is a warning: first cousins (a convention). */
	static final double WARNING_KINSHIP = 1. / 16.;


	private ConsanguinityChecker(){}


	static List<Diagnostic> findings(final ConsanguinityReport report, final Function<String, String> names){
		final List<Diagnostic> findings = new ArrayList<>();

		final List<ConsanguinityReport.ConsanguineousUnion> unions = report.unions();
		for(final ConsanguinityReport.ConsanguineousUnion union : unions.subList(0, Math.min(MAX_UNIONS, unions.size())))
			findings.add(union(union, names));
		if(unions.size() > MAX_UNIONS)
			findings.add(otherUnions(unions.subList(MAX_UNIONS, unions.size())));

		if(!report.inbredIndividuals().isEmpty())
			findings.add(inbred(report));
		return findings;
	}


	private static Diagnostic union(final ConsanguinityReport.ConsanguineousUnion union, final Function<String, String> names){
		final List<String> recordIds = new ArrayList<>(List.of(union.firstId(), union.secondId()));
		recordIds.addAll(union.relationshipIds());

		final String children = (union.childIds().isEmpty()
			? I18N.t("consanguinity.union.no_children")
			: I18N.tf("consanguinity.union.children", String.valueOf(union.childIds().size())));
		final String relatedAs = (union.term().isEmpty()? I18N.t("consanguinity.union.related"): union.term());
		final String ancestors = (union.commonAncestors().isEmpty()
			? ""
			: ConsanguinityReportText.ancestors(union.commonAncestors(), names));
		final String coefficient = ConsanguinityReportText.decimal(union.relationship());
		final String inbreeding = ConsanguinityReportText.percent(union.kinship());

		final Diagnostic.Severity severity = (union.kinship() >= WARNING_KINSHIP? Diagnostic.Severity.WARNING: Diagnostic.Severity.INFORMATION);
		final Object[] arguments = {union.firstName(), union.secondName(), relatedAs, ancestors, coefficient, inbreeding, children};
		return new Diagnostic(severity, AssistantCategory.RELATIONSHIPS,
			I18N.tf("consanguinity.union.title", arguments), I18N.tf("consanguinity.union.explanation", arguments),
			recordIds, null,
			DiagnosticKeys.of("consanguinity.union", recordIds, coefficient, union.childIds().size()))
			.withRank(union.kinship());
	}

	private static Diagnostic otherUnions(final List<ConsanguinityReport.ConsanguineousUnion> others){
		final List<String> recordIds = new ArrayList<>();
		for(final ConsanguinityReport.ConsanguineousUnion union : others){
			if(recordIds.size() >= MAX_LISTED)
				break;

			recordIds.add(union.firstId());
			recordIds.add(union.secondId());
		}
		final Object[] arguments = {String.valueOf(others.size()), String.valueOf(MAX_UNIONS),
			ConsanguinityReportText.decimal(others.getFirst().relationship())};
		return new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RELATIONSHIPS,
			I18N.tf("consanguinity.other_unions.title", arguments), I18N.tf("consanguinity.other_unions.explanation", arguments),
			recordIds, null, DiagnosticKeys.of("consanguinity.other_unions", List.of(), others.size()))
			.withRank(others.getFirst().kinship());
	}

	private static Diagnostic inbred(final ConsanguinityReport report){
		final List<ConsanguinityReport.InbredIndividual> individuals = report.inbredIndividuals();
		final StringBuilder named = new StringBuilder();
		final List<String> recordIds = new ArrayList<>();
		for(int i = 0; i < individuals.size() && recordIds.size() < MAX_LISTED; i ++){
			final ConsanguinityReport.InbredIndividual individual = individuals.get(i);
			recordIds.add(individual.id());
			if(i < MAX_NAMED){
				if(i > 0)
					named.append("; ");
				named.append(individual.name())
					.append(" (")
					.append(ConsanguinityReportText.percent(individual.inbreeding()))
					.append(')');
			}
		}
		final Object[] arguments = {String.valueOf(individuals.size()), ConsanguinityReportText.percent(report.minimumInbreeding()),
			named.toString()};
		return new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RELATIONSHIPS,
			I18N.tf("consanguinity.inbred.title", arguments), I18N.tf("consanguinity.inbred.explanation", arguments),
			recordIds, null, DiagnosticKeys.of("consanguinity.inbred", List.of(), individuals.size()))
			.withRank(individuals.getFirst().inbreeding());
	}

}
