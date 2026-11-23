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
package io.github.mtrevisan.familylegacy.assistant.report;

import io.github.mtrevisan.familylegacy.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.PlainAssistantContext;
import io.github.mtrevisan.familylegacy.assistant.chronology.ChronologyAssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.gap.GapChecker;
import io.github.mtrevisan.familylegacy.assistant.gap.GapDataLoader;
import io.github.mtrevisan.familylegacy.assistant.gap.GapFinding;
import io.github.mtrevisan.familylegacy.assistant.identity.IdentityChecker;
import io.github.mtrevisan.familylegacy.assistant.identity.IdentityFinding;
import io.github.mtrevisan.familylegacy.assistant.identity.IdentityOtherKinds;
import io.github.mtrevisan.familylegacy.assistant.source.SourceIdentifierChecker;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.DuplicateFinderService;
import io.github.mtrevisan.familylegacy.quality.IdentitySettings;
import io.github.mtrevisan.familylegacy.reports.ToVerifyAssistance;
import io.github.mtrevisan.familylegacy.reports.ToVerifyFinder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;


/**
 * Gives the To Verify report the findings of the assistants that look at the whole file: the research gaps, the chronology, the
 * identities (the individuals and the other kinds of record) and the identifiers of the sources that are wrong. It is a service, found
 * by the report: the reports are in a module this one depends on.
 * <p>
 * A finding is the title the assistant gives it (in the language of the application) against the first record it is about; it is left
 * out if any of its records is hidden, as the title may name it. The suggestions (the hints of what to look for in a source of a kind)
 * are not findings, and neither are the ones the user ignored in the assistant: the report is the whole list, as the assistant gives it.
 * </p>
 */
public final class AssistantToVerify implements ToVerifyAssistance{

	/** The share of the work of each part, by the time it takes (the comparison of the individuals is the slowest). */
	private static final double GAP_END = 0.05;
	private static final double CHRONOLOGY_END = 0.2;
	private static final double IDENTITY_END = 0.95;


	@Override
	public List<ToVerifyFinder.Finding> findings(final FLEFModel model, final Predicate<FLEFRecord> visible, final DoubleConsumer progress){
		final List<ToVerifyFinder.Finding> out = new ArrayList<>();
		if(model == null)
			return out;

		progress.accept(0.);
		research(model, visible, out);
		progress.accept(GAP_END);

		final double span = CHRONOLOGY_END - GAP_END;
		add(out, ToVerifyFinder.Kind.CHRONOLOGY, model, visible, new ChronologyAssistantContributor().diagnosticProviders().getFirst()
			.diagnose(new PlainAssistantContext(model), fraction -> progress.accept(GAP_END + span * fraction)));
		progress.accept(CHRONOLOGY_END);

		identities(model, visible, out, fraction -> progress.accept(CHRONOLOGY_END + (IDENTITY_END - CHRONOLOGY_END) * fraction));
		progress.accept(IDENTITY_END);

		sourceIdentifiers(model, visible, out);
		progress.accept(1.);
		return out;
	}


	private static void research(final FLEFModel model, final Predicate<FLEFRecord> visible, final List<ToVerifyFinder.Finding> out){
		add(out, ToVerifyFinder.Kind.RESEARCH_GAP, model, visible, GapChecker.findings(GapDataLoader.load(model, LocalDate.now()),
				AssistantTuning.load()).stream()
			.map(GapFinding::diagnostic)
			.toList());
	}

	private static void identities(final FLEFModel model, final Predicate<FLEFRecord> visible, final List<ToVerifyFinder.Finding> out,
			final DoubleConsumer progress){
		final IdentitySettings settings = IdentitySettings.load();
		final DuplicateFinderService.Options options = new DuplicateFinderService.Options();
		options.threshold = settings.threshold();
		options.maxResults = settings.maxPairs();
		//a pair that already has an identity hypothesis is not proposed again: the hypothesis has its own report
		options.skipExistingHypotheses = true;
		final List<DuplicateFinderService.Candidate> candidates = DuplicateFinderService.find(model, options, progress);

		add(out, ToVerifyFinder.Kind.IDENTITY, model, visible, IdentityChecker.findings(candidates, settings).stream()
			.map(IdentityFinding::diagnostic)
			.toList());
		add(out, ToVerifyFinder.Kind.IDENTITY, model, visible, IdentityOtherKinds.findings(model, settings).stream()
			.map(IdentityFinding::diagnostic)
			.toList());
	}

	/** Only what is wrong: the other findings of the identifiers of a source are hints (where to find the film, ...). */
	private static void sourceIdentifiers(final FLEFModel model, final Predicate<FLEFRecord> visible,
			final List<ToVerifyFinder.Finding> out){
		for(final FLEFRecord source : model.getRecordsByType(SourceReader.TYPE))
			add(out, ToVerifyFinder.Kind.SOURCE_IDENTIFIER, model, visible, SourceIdentifierChecker.findings(model, source.getId()).stream()
				.filter(diagnostic -> diagnostic.severity() != Diagnostic.Severity.INFORMATION)
				.toList());
	}


	private static void add(final List<ToVerifyFinder.Finding> out, final ToVerifyFinder.Kind kind, final FLEFModel model,
			final Predicate<FLEFRecord> visible, final List<Diagnostic> diagnostics){
		for(final Diagnostic diagnostic : diagnostics)
			if(isShown(diagnostic, model, visible))
				out.add(new ToVerifyFinder.Finding(kind, (diagnostic.recordIds().isEmpty()? null: diagnostic.recordIds().getFirst()),
					diagnostic.title()));
	}

	/** A finding is shown only if every record it is about is: its title may name any of them. */
	private static boolean isShown(final Diagnostic diagnostic, final FLEFModel model, final Predicate<FLEFRecord> visible){
		for(final String recordId : diagnostic.recordIds()){
			final FLEFRecord record = model.getRecordById(recordId);
			if(record != null && !visible.test(record))
				return false;
		}
		return true;
	}

}
