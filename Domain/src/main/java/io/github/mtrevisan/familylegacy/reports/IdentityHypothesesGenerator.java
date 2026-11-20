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
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Generates the "Identity Hypotheses" report of the whole file: every identity hypothesis with where it stands (open, accepted,
 * rejected, as told by the most recent conclusion that evaluates it), the records involved, the confidence, the rationale, the citations
 * that support it and the ones that contradict it, the research questions that deal with it and the conclusions that evaluate it.
 * <p>
 * It is a research management report and a quality-control report: the open hypotheses are the possible duplicates still to be
 * decided. The hypotheses that concern a record hidden by the privacy policy of the report are left out.
 * </p>
 */
public final class IdentityHypothesesGenerator{

	private final FLEFModel model;
	private final ReportConfig config;


	public IdentityHypothesesGenerator(final FLEFModel model, final ReportConfig config){
		this.model = model;
		this.config = config;
	}


	/** A hypothesis with what is needed to list it. */
	private record Entry(FLEFRecord hypothesis, List<FLEFRecord> candidates, String candidateTag, HypothesisStatus status,
			FLEFRecord deciding, List<FLEFRecord> conclusions){}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportLabels labels = new ReportLabels(config.language(), config.dateFormat());
		final String title = labels.sections().hypothesesTitle();
		final String language = labels.language().locale().toLanguageTag();

		final List<FLEFRecord> all = model.getRecords();
		if(all.isEmpty())
			return new ReportDocument(title, null, List.of(new ReportSection.Heading(1, title),
				new ReportSection.Paragraph(labels.sections().empty())), language, labels.sections().contents());

		final ReportContext ctx = ReportContext.build(model, all.getFirst(), config);
		listener.onProgress(10, null);

		final Map<String, List<FLEFRecord>> conclusionsByHypothesis = conclusionsByHypothesis();
		final List<Entry> visible = new ArrayList<>();
		for(final FLEFRecord hypothesis : model.getRecordsByType(IdentityHypothesisReader.TYPE)){
			final Entry entry = entry(ctx, hypothesis, conclusionsByHypothesis.getOrDefault(hypothesis.getId(), List.of()));
			if(entry != null)
				visible.add(entry);
		}
		listener.onProgress(40, null);

		final Map<HypothesisStatus, Integer> counts = new EnumMap<>(HypothesisStatus.class);
		for(final Entry entry : visible)
			counts.merge(entry.status(), 1, Integer::sum);
		final List<Entry> shown = visible.stream()
			.filter(entry -> config.hypothesisFilter().accepts(entry.status()))
			.filter(entry -> config.hypothesisRecordType().accepts(entry.candidateTag()))
			.toList();

		final SectionLabelProvider text = ctx.labels.sections();
		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, text.overview()));
		if(visible.isEmpty())
			sections.add(new ReportSection.Paragraph(text.hypothesesNothing()));
		else{
			sections.add(new ReportSection.Paragraph(String.format(text.hypothesesIntro(), ctx.formatInt(visible.size()),
				ctx.formatInt(counts.getOrDefault(HypothesisStatus.OPEN, 0)), ctx.formatInt(counts.getOrDefault(HypothesisStatus.ACCEPTED, 0)),
				ctx.formatInt(counts.getOrDefault(HypothesisStatus.REJECTED, 0)))));
			if(config.hypothesisFilter() != HypothesisFilter.ALL)
				sections.add(new ReportSection.Paragraph("*" + text.hypothesesFilter(config.hypothesisFilter().name()) + "*"));
			if(config.hypothesisRecordType() != HypothesisRecordType.ALL)
				sections.add(new ReportSection.Paragraph("*" + String.format(text.hypothesesFilterType(),
					text.hypothesesType(config.hypothesisRecordType().name())) + "*"));
			if(shown.isEmpty())
				sections.add(new ReportSection.Paragraph(text.hypothesesNoneShown()));
			else{
				final ResearchIndex research = new ResearchIndex(model, ctx::isVisible);
				int done = 0;
				for(final HypothesisStatus status : HypothesisStatus.values()){
					final List<Entry> group = sorted(ctx, shown.stream()
						.filter(entry -> entry.status() == status)
						.toList());
					if(group.isEmpty())
						continue;

					sections.add(new ReportSection.Heading(2, text.hypothesesStatusHeading(status.name()) + " (" + ctx.formatInt(group.size()) + ")"));
					for(final Entry entry : group){
						if(Thread.currentThread().isInterrupted())
							throw new RuntimeException("Report generation cancelled");

						describe(ctx, research, entry, sections);
						listener.onProgress(40 + 60 * (++ done) / shown.size(), null);
					}
				}
			}
		}
		sections.add(new ReportSection.Paragraph("*" + text.hypothesesNotes() + "*"));
		listener.onProgress(100, null);

		return new ReportDocument(title, null, sections, language, labels.sections().contents());
	}


	/** @return	The conclusions that resolve each hypothesis (by the identifier of the hypothesis). */
	private Map<String, List<FLEFRecord>> conclusionsByHypothesis(){
		final Map<String, List<FLEFRecord>> out = new HashMap<>();
		for(final FLEFRecord conclusion : model.getRecordsByType(ConclusionReader.TYPE))
			for(final FLEFRecord resolves : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES)){
				final FLEFRecord target = resolves.getTheOnlyChild();
				if(target != null && target.getValue() != null)
					out.computeIfAbsent(target.getValue(), k -> new ArrayList<>())
						.add(conclusion);
			}
		return out;
	}

	/** @return	The hypothesis with its status, {@code null} if it concerns a record that the privacy policy hides. */
	private Entry entry(final ReportContext ctx, final FLEFRecord hypothesis, final List<FLEFRecord> conclusions){
		final List<FLEFRecord> candidates = new ArrayList<>(2);
		String candidateTag = null;
		for(final FLEFRecord identity : FLEFRecordHelper.findChildren(hypothesis, IdentityHypothesisReader.TAG_IDENTITY)){
			final FLEFRecord ref = identity.getTheOnlyChild();
			if(ref == null || ref.getValue() == null)
				continue;

			if(candidateTag == null)
				candidateTag = ref.getTag();
			final FLEFRecord candidate = model.getRecordById(ref.getValue());
			if(candidate != null){
				if(!ctx.isVisible(candidate))
					return null;
				candidates.add(candidate);
			}
		}

		final List<FLEFRecord> visibleConclusions = conclusions.stream()
			.filter(ctx::isVisible)
			.toList();
		FLEFRecord deciding = null;
		String decidingDate = null;
		for(final FLEFRecord conclusion : visibleConclusions){
			if(ConclusionReader.extractProofStatus(conclusion) == null)
				continue;

			final String date = latestAuditDate(conclusion);
			if(deciding == null || (date != null && (decidingDate == null || date.compareTo(decidingDate) >= 0))){
				deciding = conclusion;
				decidingDate = date;
			}
		}
		final HypothesisStatus status = HypothesisStatus.of(deciding != null? ConclusionReader.extractProofStatus(deciding): null);
		return new Entry(hypothesis, candidates, candidateTag, status, deciding, visibleConclusions);
	}

	/** @return	The latest of the creation and update dates of the audit (the ISO instants compare as text), {@code null} if it has none. */
	private static String latestAuditDate(final FLEFRecord record){
		String latest = FLEFRecordHelper.getChildValue(record, AuditReader.TAG_AUDIT + "." + AuditReader.TAG_CREATION_DATE);
		for(final FLEFRecord update : FLEFRecordHelper.findChildren(record, AuditReader.TAG_AUDIT + "." + AuditReader.TAG_UPDATE)){
			final String date = FLEFRecordHelper.getChildValue(update, AuditReader.TAG_DATE);
			if(date != null && (latest == null || date.compareTo(latest) > 0))
				latest = date;
		}
		return latest;
	}

	private List<Entry> sorted(final ReportContext ctx, final List<Entry> entries){
		final Comparator<String> collator = ReportCollation.of(ctx.labels.language().locale());
		final List<Entry> out = new ArrayList<>(entries);
		out.sort(Comparator.comparing((final Entry entry) -> candidatesText(ctx, entry), collator)
			.thenComparing(entry -> StringUtils.defaultString(entry.hypothesis().getId())));
		return out;
	}

	private static String candidatesText(final ReportContext ctx, final Entry entry){
		final List<String> names = new ArrayList<>(entry.candidates().size());
		for(final FLEFRecord candidate : entry.candidates())
			names.add(ctx.displayText(candidate));
		return String.join(" ↔ ", names);
	}


	/** One hypothesis: the records, where it stands and what is known of how it is going. */
	private void describe(final ReportContext ctx, final ResearchIndex research, final Entry entry, final List<ReportSection> out){
		final SectionLabelProvider text = ctx.labels.sections();
		final FLEFRecord hypothesis = entry.hypothesis();

		out.add(new ReportSection.Heading(3, ReportFormatters.escape(candidatesText(ctx, entry)) + " [" + hypothesis.getId() + "]"));

		final String proofStatus = (entry.deciding() != null? ConclusionReader.extractProofStatus(entry.deciding()): null);
		out.add(new ReportSection.Paragraph("**" + text.hypothesesLabel("STATUS") + ":** " + text.hypothesesStatus(entry.status().name())
			+ (proofStatus != null? " (" + ctx.labels.enumValue("enum.proof_status", proofStatus) + ")": StringUtils.EMPTY)));

		final String confidence = FLEFRecordHelper.getChildValue(hypothesis, IdentityHypothesisReader.TAG_CONFIDENCE);
		out.add(new ReportSection.Paragraph("**" + text.hypothesesLabel("CONFIDENCE") + ":** "
			+ (confidence != null && !confidence.isBlank()? ctx.labels.enumValue("enum.confidence", confidence): text.hypothesesLabel("NOT_STATED"))));

		final String rationale = IdentityHypothesisReader.extractRationale(hypothesis);
		if(rationale != null && !rationale.isBlank())
			out.add(new ReportSection.Paragraph("**" + text.hypothesesLabel("RATIONALE") + ":** " + ReportFormatters.escape(rationale.strip())));

		final List<String> supporting = new ArrayList<>();
		final List<String> contradicting = new ArrayList<>();
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(hypothesis, IdentityHypothesisReader.TAG_SOURCE)){
			final String sourceId = SourceCitationReader.extractSource(citation);
			if(sourceId == null)
				continue;

			final String label = ReportFormatters.escape(ctx.sourceTitle(sourceId));
			(SourceCitationReader.isContradicting(citation)? contradicting: supporting).add(label);
		}
		out.add(new ReportSection.Paragraph("**" + text.hypothesesLabel("SUPPORTING") + ":** "
			+ (supporting.isEmpty()? text.hypothesesLabel("NONE"): String.join("; ", supporting))));
		out.add(new ReportSection.Paragraph("**" + text.hypothesesLabel("CONTRADICTING") + ":** "
			+ (contradicting.isEmpty()? text.hypothesesLabel("NONE"): String.join("; ", contradicting))));

		final List<String> questions = questions(ctx, research, entry);
		if(!questions.isEmpty())
			out.add(new ReportSection.Paragraph("**" + text.hypothesesLabel("QUESTIONS") + ":** " + String.join("; ", questions)));

		if(!entry.conclusions().isEmpty()){
			final List<String> items = new ArrayList<>();
			for(final FLEFRecord conclusion : entry.conclusions()){
				final String status = ConclusionReader.extractProofStatus(conclusion);
				items.add(ReportFormatters.escape(ctx.displayText(conclusion)) + " [" + conclusion.getId() + "]"
					+ (status != null? " (" + ctx.labels.enumValue("enum.proof_status", status) + ")": StringUtils.EMPTY));
			}
			out.add(new ReportSection.Paragraph("**" + text.hypothesesLabel("CONCLUSIONS") + ":** " + String.join("; ", items)));
		}
	}

	/** @return	The research questions about the hypothesis or its records, and the ones that its conclusions answer. */
	private List<String> questions(final ReportContext ctx, final ResearchIndex research, final Entry entry){
		final Set<String> targets = new LinkedHashSet<>();
		targets.add(entry.hypothesis().getId());
		for(final FLEFRecord candidate : entry.candidates())
			targets.add(candidate.getId());

		final Set<String> ids = new LinkedHashSet<>();
		for(final FLEFRecord conclusion : entry.conclusions())
			for(final FLEFRecord link : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESEARCH))
				if(link.getValue() != null)
					ids.add(link.getValue());
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
