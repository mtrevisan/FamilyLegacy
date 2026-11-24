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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.i18n.SectionLabelProvider;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.familylegacy.reports.index.ResearchIndex;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Generates the "Conflicts" report of the whole file: the conflicts between competing assertions (several births, deaths, parents,
 * relationships, attribute values, identities), the ones that no conclusion has settled yet and the ones that have been. For each
 * conflict it shows the competing assertions with their confidence and their supporting and contradicting citations, what the
 * conclusion decided (preferred assertion, proof status) and the research questions that deal with it.
 * <p>
 * The conflicts are found by a {@link ConflictFinder}; the ones that concern an individual hidden by the privacy policy of the report
 * are left out.
 * </p>
 */
public final class ConflictReportGenerator{

	private final FLEFModel model;
	private final ReportConfig config;
	private final ConflictFinder finder;


	/** @param finder	What finds the conflicts ({@code null}: none are found). */
	public ConflictReportGenerator(final FLEFModel model, final ReportConfig config, final ConflictFinder finder){
		this.model = model;
		this.config = config;
		this.finder = finder;
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportLabels labels = new ReportLabels(config.language(), config.dateFormat());
		final String title = labels.sections().conflictsTitle();
		final String language = labels.language().locale().toLanguageTag();

		final List<FLEFRecord> all = model.getRecords();
		if(all.isEmpty())
			return new ReportDocument(title, null, List.of(new ReportSection.Heading(1, title),
				new ReportSection.Paragraph(labels.sections().empty())), language, labels.sections().contents());

		final ReportContext ctx = ReportContext.build(model, all.getFirst(), config);
		listener.onProgress(5, null);

		final List<ReportConflict> visible = (finder != null? finder.find(model): List.<ReportConflict>of()).stream()
			.filter(conflict -> isVisible(ctx, conflict))
			.toList();
		listener.onProgress(60, null);

		final int unresolved = (int)visible.stream()
			.filter(conflict -> !conflict.resolved())
			.count();
		final List<ReportConflict> shown = visible.stream()
			.filter(conflict -> config.conflictFilter().accepts(conflict.resolved()))
			.filter(conflict -> config.conflictRecordType().accepts(conflict.kind()))
			.filter(config.conflictConfidence()::accepts)
			.toList();

		final Map<ReportConflict.Kind, List<ReportConflict>> byKind = new EnumMap<>(ReportConflict.Kind.class);
		for(final ReportConflict conflict : shown)
			byKind.computeIfAbsent(conflict.kind(), k -> new ArrayList<>())
				.add(conflict);

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, ctx.labels.sections().overview()));
		if(visible.isEmpty())
			sections.add(new ReportSection.Paragraph(ctx.labels.sections().conflictsNothing()));
		else{
			sections.add(new ReportSection.Paragraph(String.format(ctx.labels.sections().conflictsIntro(),
				ctx.formatInt(visible.size()), ctx.formatInt(unresolved), ctx.formatInt(visible.size() - unresolved))));
			if(config.conflictFilter() != ConflictFilter.ALL)
				sections.add(new ReportSection.Paragraph("*" + ctx.labels.sections().conflictsFilter(config.conflictFilter().name()) + "*"));
			if(config.conflictRecordType() != ConflictRecordType.ALL)
				sections.add(new ReportSection.Paragraph("*" + String.format(ctx.labels.sections().conflictsFilterType(),
					ctx.labels.sections().conflictsType(config.conflictRecordType().name())) + "*"));
			if(config.conflictConfidence() != ConflictConfidence.ALL)
				sections.add(new ReportSection.Paragraph("*" + String.format(ctx.labels.sections().conflictsFilterConfidence(),
					ctx.labels.sections().conflictsConfidence(config.conflictConfidence().name())) + "*"));
			if(shown.isEmpty())
				sections.add(new ReportSection.Paragraph(ctx.labels.sections().conflictsNoneShown()));
			else{
				sections.add(summary(ctx, byKind));
				final ResearchIndex research = new ResearchIndex(model, ctx::isVisible);
				int done = 0;
				for(final Map.Entry<ReportConflict.Kind, List<ReportConflict>> entry : byKind.entrySet()){
					sections.add(new ReportSection.Heading(2, ctx.labels.sections().conflictsKind(entry.getKey().key())
						+ " (" + ctx.formatInt(entry.getValue().size()) + ")"));
					for(final ReportConflict conflict : sorted(ctx, entry.getValue())){
						if(Thread.currentThread().isInterrupted())
							throw new RuntimeException("Report generation cancelled");

						describe(ctx, research, conflict, sections);
						listener.onProgress(60 + 40 * (++ done) / shown.size(), null);
					}
				}
			}
		}
		sections.add(new ReportSection.Paragraph("*" + ctx.labels.sections().conflictsNotes() + "*"));
		listener.onProgress(100, null);

		return new ReportDocument(title, null, sections, language, labels.sections().contents());
	}


	/** A conflict is shown only if every individual it concerns is. */
	private static boolean isVisible(final ReportContext ctx, final ReportConflict conflict){
		for(final String personId : conflict.personIds()){
			final FLEFRecord person = ctx.model.getRecordById(personId);
			if(person != null && !ctx.isVisible(person))
				return false;
		}
		return true;
	}

	/** The table of the kinds: how many conflicts, how many of them are not settled. */
	private static ReportSection summary(final ReportContext ctx, final Map<ReportConflict.Kind, List<ReportConflict>> byKind){
		final List<List<String>> rows = new ArrayList<>(byKind.size());
		for(final Map.Entry<ReportConflict.Kind, List<ReportConflict>> entry : byKind.entrySet()){
			final long unresolved = entry.getValue().stream()
				.filter(conflict -> !conflict.resolved())
				.count();
			rows.add(List.of(ctx.labels.sections().conflictsKind(entry.getKey().key()), ctx.formatInt(entry.getValue().size()),
				ctx.formatInt((int)unresolved), ctx.formatInt(entry.getValue().size() - (int)unresolved)));
		}
		return new ReportSection.Table(List.of(ctx.labels.sections().conflictsColumn("KIND"), ctx.labels.sections().conflictsColumn("TOTAL"),
			ctx.labels.sections().conflictsColumn("UNRESOLVED"), ctx.labels.sections().conflictsColumn("RESOLVED")), rows);
	}

	/** The conflicts by the individuals they concern, the unresolved first. */
	private List<ReportConflict> sorted(final ReportContext ctx, final List<ReportConflict> conflicts){
		final Comparator<String> collator = ReportCollation.of(ctx.labels.language().locale());
		final List<ReportConflict> out = new ArrayList<>(conflicts);
		out.sort(Comparator.comparing(ReportConflict::resolved)
			.thenComparing((final ReportConflict conflict) -> subject(ctx, conflict), collator)
			.thenComparing(conflict -> StringUtils.defaultString(conflict.assertions().isEmpty()? null: conflict.assertions().getFirst().recordId())));
		return out;
	}

	/** @return	The names of the individuals the conflict is about. */
	private String subject(final ReportContext ctx, final ReportConflict conflict){
		final List<String> names = new ArrayList<>(conflict.personIds().size());
		for(final String personId : conflict.personIds())
			names.add(display(ctx, personId));
		return String.join("; ", names);
	}

	private String display(final ReportContext ctx, final String recordId){
		if(recordId == null)
			return StringUtils.EMPTY;

		final FLEFRecord record = model.getRecordById(recordId);
		return (record != null? ctx.displayText(record): recordId);
	}


	/** One conflict: who and what is wrong, the status, the competing assertions and what is known of how it is going. */
	private void describe(final ReportContext ctx, final ResearchIndex research, final ReportConflict conflict, final List<ReportSection> out){
		final SectionLabelProvider labels = ctx.labels.sections();
		final ReportConflict.Settlement settlement = conflict.settlement();

		out.add(new ReportSection.Paragraph("**" + ReportFormatters.escape(subject(ctx, conflict)) + "**: "
			+ labels.conflictsProblem(conflict.problem().key())));
		out.add(new ReportSection.Paragraph(labels.conflictsStatusLabel() + ": " + status(ctx, conflict)));
		out.add(assertions(ctx, conflict));

		if(settlement != null){
			final FLEFRecord conclusion = model.getRecordById(settlement.conclusionId());
			out.add(new ReportSection.Paragraph(labels.conflictsConclusion() + ": "
				+ ReportFormatters.escape(conclusion != null? ctx.displayText(conclusion): settlement.conclusionId())));
			if(settlement.preferredId() != null)
				out.add(new ReportSection.Paragraph(labels.conflictsPreferred() + ": " + ReportFormatters.escape(preferredText(ctx, conflict))));
		}

		final List<String> questions = questions(ctx, research, conflict);
		if(!questions.isEmpty())
			out.add(new ReportSection.Paragraph(labels.conflictsQuestions() + ": " + String.join("; ", questions)));
	}

	/** @return	Where the conflict stands: not settled, or what the conclusion says. */
	private static String status(final ReportContext ctx, final ReportConflict conflict){
		final SectionLabelProvider labels = ctx.labels.sections();
		if(!conflict.resolved())
			return labels.conflictsStatus("UNRESOLVED");

		final String proofStatus = conflict.settlement().proofStatus();
		return (proofStatus != null? labels.conflictsStatus(proofStatus.toUpperCase(Locale.ROOT)): labels.conflictsStatus("PREFERRED"));
	}

	/** @return	What the preferred assertion asserts. */
	private String preferredText(final ReportContext ctx, final ReportConflict conflict){
		final String preferredId = conflict.settlement().preferredId();
		for(final ReportConflict.Assertion assertion : conflict.assertions())
			if(assertion.recordId().equals(preferredId))
				return assertionText(ctx, assertion);
		return display(ctx, preferredId);
	}

	private String assertionText(final ReportContext ctx, final ReportConflict.Assertion assertion){
		final String value = (assertion.value() != null && !assertion.value().isBlank()? assertion.value(): "?");
		return (assertion.placeId() != null? value + " @ " + display(ctx, assertion.placeId()): value);
	}

	/** The competing assertions, with how well each is backed (the preferred one is marked). */
	private ReportSection assertions(final ReportContext ctx, final ReportConflict conflict){
		final SectionLabelProvider labels = ctx.labels.sections();
		final String preferredId = (conflict.resolved()? conflict.settlement().preferredId(): null);
		final boolean withPlace = conflict.assertions().stream()
			.anyMatch(assertion -> assertion.placeId() != null);

		final List<String> headers = new ArrayList<>();
		headers.add(labels.conflictsColumn("RECORD"));
		headers.add(labels.conflictsColumn("ASSERTION"));
		if(withPlace)
			headers.add(labels.conflictsColumn("PLACE"));
		headers.add(labels.conflictsColumn("CONFIDENCE"));
		headers.add(labels.conflictsColumn("SUPPORTING"));
		headers.add(labels.conflictsColumn("CONTRADICTING"));
		if(preferredId != null)
			headers.add(labels.conflictsColumn("PREFERRED"));

		final List<List<String>> rows = new ArrayList<>(conflict.assertions().size());
		for(final ReportConflict.Assertion assertion : conflict.assertions()){
			final int[] citations = ConflictDashboard.citations(model, assertion);
			final List<String> row = new ArrayList<>();
			row.add(ReportFormatters.escape(assertion.recordId()));
			row.add(ReportFormatters.escape(assertion.value() != null && !assertion.value().isBlank()? assertion.value(): "?"));
			if(withPlace)
				row.add(ReportFormatters.escape(assertion.placeId() != null? display(ctx, assertion.placeId()): StringUtils.EMPTY));
			row.add(assertion.confidence() != null? labels.conflictsConfidence(assertion.confidence().toUpperCase(Locale.ROOT)): "-");
			row.add(ctx.formatInt(citations[0]));
			row.add(ctx.formatInt(citations[1]));
			if(preferredId != null)
				row.add(assertion.recordId().equals(preferredId)? labels.conflictsPreferred(): StringUtils.EMPTY);
			rows.add(row);
		}
		return new ReportSection.Table(headers, rows);
	}

	/** @return	The research questions the conclusion answers, and the ones about the individuals or the assertions. */
	private List<String> questions(final ReportContext ctx, final ResearchIndex research, final ReportConflict conflict){
		final Set<String> targets = new LinkedHashSet<>(conflict.personIds());
		for(final ReportConflict.Assertion assertion : conflict.assertions())
			targets.add(assertion.recordId());

		final Set<String> ids = new LinkedHashSet<>();
		if(conflict.resolved()){
			final FLEFRecord conclusion = model.getRecordById(conflict.settlement().conclusionId());
			if(conclusion != null)
				for(final FLEFRecord link : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESEARCH))
					if(link.getValue() != null)
						ids.add(link.getValue());
		}
		for(final FLEFRecord question : research.researchQuestionsFor(targets))
			ids.add(question.getId());

		final List<String> out = new ArrayList<>();
		for(final String id : ids){
			final FLEFRecord question = ctx.visible(model.getRecordById(id));
			if(question == null)
				continue;

			final String questionTitle = ResearchQuestionReader.extractTitle(question);
			out.add(ReportFormatters.escape((questionTitle != null && !questionTitle.isBlank()? questionTitle.strip(): id) + " [" + id + "]"));
		}
		return out;
	}

}
