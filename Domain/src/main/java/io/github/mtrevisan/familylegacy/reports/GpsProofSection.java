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

import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Builds the main section of a Genealogical Proof Standard (GPS) report, whose root is a research question.
 * <p>
 * The report lays the question out as a proof argument, in the order the standard asks for:
 * </p>
 * <ol>
 *   <li><b>Research question</b>: the question, where it stands and what it is about;</li>
 *   <li><b>Evidence summary</b>: what was searched (the activities, with their results) and what was found (the citations, how many
 *       support and how many contradict, and how they are qualified: original or derived, primary or secondary, direct, indirect or
 *       negative);</li>
 *   <li><b>Conflicting evidence</b>: the conclusions that put competing assertions side by side, with the preferred one marked, and
 *       the citations that contradict;</li>
 *   <li><b>Analysis</b>: what the researcher observed in each activity;</li>
 *   <li><b>Reasoning</b>: why the conclusions were reached (the rationale of the question and the narrative of each conclusion);</li>
 *   <li><b>Conclusion</b>: the answer to the question and each conclusion with its proof status;</li>
 *   <li><b>Confidence</b>: the confidence in the answer and how the conclusions are distributed by proof status.</li>
 * </ol>
 * <p>
 * A part with nothing recorded says so, so that a gap in the proof is visible, not hidden. The sources, notes, evidence and audit of
 * the question follow, and then the common closing sections of the other reports.
 * </p>
 */
public final class GpsProofSection implements SectionBuilder{

	private static final String ENUM_SOURCE_TYPE = "enum.evidence.source_type";
	private static final String ENUM_INFORMATION_TYPE = "enum.evidence.information_type";
	private static final String ENUM_EVIDENCE_TYPE = "enum.evidence.evidence_type";


	private final ReportContext ctx;


	public GpsProofSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isResearchQuestionRoot())
			return List.of();

		final List<FLEFRecord> activities = ctx.index.activitiesForQuestion(ctx.root);
		final List<FLEFRecord> conclusions = ctx.index.conclusionsForQuestion(ctx.root);
		final List<FLEFRecord> citations = citationsOf(activities, conclusions);

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, String.format(ctx.labels.sections().gpsProofOf(),
			ReportFormatters.escape(ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(ctx.root, ResearchQuestionReader.TAG_TITLE))))));

		writeQuestion(out);
		writeEvidenceSummary(out, activities, citations);
		writeConflictingEvidence(out, conclusions, citations);
		writeAnalysis(out, activities);
		writeReasoning(out, conclusions);
		writeConclusion(out, conclusions);
		writeConfidence(out, conclusions);

		if(ctx.writesNotes())
			out.addAll(ctx.citations().rootNotes(ctx.root));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		if(ctx.config.evidence())
			ctx.citations()
				.addEvidence(ctx.root, out);
		out.addAll(ctx.citations().audit(ctx.root));
		out.addAll(ctx.citations().privacyInfo(ctx.root));
		return out;
	}


	/* ======================================================================
	 *                          The parts of the proof
	 * ====================================================================== */

	private void writeQuestion(final List<ReportSection> out){
		out.add(new ReportSection.Heading(2, ctx.labels.sections().gpsResearchQuestion()));

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().question(),
			FLEFRecordHelper.getChildValue(ctx.root, ResearchQuestionReader.TAG_QUESTION));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().status(),
			ctx.labels.enumValue("enum.research_question.status", FLEFRecordHelper.getChildValue(ctx.root, ResearchQuestionReader.TAG_STATUS)));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		final List<String> targets = new ArrayList<>();
		for(final FLEFRecord target : FLEFRecordHelper.findChildren(ctx.root, ResearchQuestionReader.TAG_TARGET)){
			final FLEFRecord ref = target.getTheOnlyChild();
			if(ref == null || StringUtils.isEmpty(ref.getValue()))
				continue;

			final FLEFRecord record = ctx.visible(ctx.model.getRecordById(ref.getValue()));
			targets.add(ReportFormatters.escape(record != null? ctx.displayText(record): ref.getValue()));
		}
		if(!targets.isEmpty()){
			out.add(new ReportSection.Paragraph("**" + ctx.labels.sections().researchTargets() + ":**"));
			out.add(new ReportSection.BulletList(targets));
		}
	}

	private void writeEvidenceSummary(final List<ReportSection> out, final List<FLEFRecord> activities, final List<FLEFRecord> citations){
		out.add(new ReportSection.Heading(2, ctx.labels.sections().gpsEvidenceSummary()));
		if(activities.isEmpty() && citations.isEmpty()){
			out.add(nothingRecorded());
			return;
		}

		if(!activities.isEmpty()){
			final List<String> items = new ArrayList<>();
			for(final FLEFRecord activity : activities)
				items.add(ReportFormatters.escape(activitySummary(activity)));
			out.add(new ReportSection.Paragraph("**" + ctx.labels.sections().researchActivities() + ":**"));
			out.add(new ReportSection.BulletList(items));
		}

		if(!citations.isEmpty()){
			final long contradicting = citations.stream()
				.filter(SourceCitationReader::isContradicting)
				.count();
			final List<String> rows = new ArrayList<>();
			rows.add(String.format(ctx.labels.sections().gpsCitationsTally(), citations.size(), citations.size() - contradicting, contradicting));
			ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceSourceType(),
				tally(citations, EvidenceQualifiersReader.TAG_SOURCE_TYPE, ENUM_SOURCE_TYPE));
			ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceInformationType(),
				tally(citations, EvidenceQualifiersReader.TAG_INFORMATION_TYPE, ENUM_INFORMATION_TYPE));
			ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceType(),
				tally(citations, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, ENUM_EVIDENCE_TYPE));
			out.add(new ReportSection.BulletList(rows));
		}

		//(the citations themselves, as in the other reports)
		if(ctx.config.sources())
			for(final FLEFRecord activity : activities)
				out.addAll(ctx.citations().citations(activity));
	}

	private void writeConflictingEvidence(final List<ReportSection> out, final List<FLEFRecord> conclusions, final List<FLEFRecord> citations){
		out.add(new ReportSection.Heading(2, ctx.labels.sections().gpsConflictingEvidence()));

		boolean any = false;
		final ContextResearchSection research = new ContextResearchSection(ctx);
		for(final FLEFRecord conclusion : conclusions){
			final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES);
			final boolean conflicting = ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE.equals(
				ConclusionReader.extractProofStatus(conclusion));
			if(!conflicting && resolves.size() < 2)
				continue;

			any = true;
			out.add(new ReportSection.Heading(3, issueOf(conclusion)));
			final String preferred = research.describeOneOf(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED));
			final List<String> items = new ArrayList<>();
			for(final FLEFRecord resolved : resolves){
				final String label = research.describeOneOf(resolved);
				if(label != null)
					items.add(ReportFormatters.escape(label)
						+ (label.equals(preferred)? "  ← *" + ctx.labels.sections().researchPreferred() + "*": StringUtils.EMPTY));
			}
			if(!items.isEmpty())
				out.add(new ReportSection.BulletList(items));
		}

		final long contradicting = citations.stream()
			.filter(SourceCitationReader::isContradicting)
			.count();
		if(contradicting > 0){
			any = true;
			out.add(new ReportSection.Paragraph(String.format(ctx.labels.sections().gpsContradictingCitations(), contradicting)));
		}
		if(!any)
			out.add(nothingRecorded());
	}

	private void writeAnalysis(final List<ReportSection> out, final List<FLEFRecord> activities){
		out.add(new ReportSection.Heading(2, ctx.labels.sections().gpsAnalysis()));

		boolean any = false;
		for(final FLEFRecord activity : activities){
			final String observation = FLEFRecordHelper.getChildValue(activity, ResearchActivityReader.TAG_OBSERVATION);
			final String conclusion = FLEFRecordHelper.getChildValue(activity, ResearchActivityReader.TAG_CONCLUSION);
			if(StringUtils.isBlank(observation) && StringUtils.isBlank(conclusion))
				continue;

			any = true;
			out.add(new ReportSection.Heading(3, ReportFormatters.escape(activitySummary(activity))));
			if(StringUtils.isNotBlank(observation))
				out.add(new ReportSection.Paragraph("**" + ctx.labels.sections().researchObservation() + ":** "
					+ ReportFormatters.escape(observation)));
			if(StringUtils.isNotBlank(conclusion))
				out.add(new ReportSection.Paragraph("**" + ctx.labels.sections().conclusion() + ":** " + ReportFormatters.escape(conclusion)));
		}
		if(!any)
			out.add(nothingRecorded());
	}

	private void writeReasoning(final List<ReportSection> out, final List<FLEFRecord> conclusions){
		out.add(new ReportSection.Heading(2, ctx.labels.sections().gpsReasoning()));

		boolean any = false;
		final String rationale = FLEFRecordHelper.getChildValue(ctx.root, ResearchQuestionReader.TAG_RATIONALE);
		if(StringUtils.isNotBlank(rationale)){
			any = true;
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(rationale)));
		}
		for(final FLEFRecord conclusion : conclusions){
			final String narrative = ConclusionReader.extractNarrative(conclusion);
			if(StringUtils.isBlank(narrative))
				continue;

			any = true;
			out.add(new ReportSection.Heading(3, issueOf(conclusion)));
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(narrative)));
			if(ctx.config.sources())
				out.addAll(ctx.citations().citations(conclusion));
		}
		if(!any)
			out.add(nothingRecorded());
	}

	private void writeConclusion(final List<ReportSection> out, final List<FLEFRecord> conclusions){
		out.add(new ReportSection.Heading(2, ctx.labels.sections().conclusion()));

		boolean any = false;
		final String answer = FLEFRecordHelper.getChildValue(ctx.root, ResearchQuestionReader.TAG_CONCLUSION);
		if(StringUtils.isNotBlank(answer)){
			any = true;
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(answer)));
		}

		final ContextResearchSection research = new ContextResearchSection(ctx);
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord conclusion : conclusions){
			final StringBuilder item = new StringBuilder(ReportFormatters.escape(issueOf(conclusion)));
			final String preferred = research.describeOneOf(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED));
			if(preferred != null)
				item.append(": ").append(ReportFormatters.escape(preferred));
			final String proof = ctx.labels.enumValue("enum.proof_status", ConclusionReader.extractProofStatus(conclusion));
			if(proof != null)
				item.append(" (").append(ReportFormatters.escape(proof)).append(')');
			items.add(item.toString());
		}
		if(!items.isEmpty()){
			any = true;
			out.add(new ReportSection.BulletList(items));
		}
		if(!any)
			out.add(nothingRecorded());
	}

	private void writeConfidence(final List<ReportSection> out, final List<FLEFRecord> conclusions){
		out.add(new ReportSection.Heading(2, ctx.labels.sections().researchConclusionConfidence()));

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchConclusionConfidence(),
			ctx.labels.enumValue("enum.confidence", FLEFRecordHelper.getChildValue(ctx.root, ResearchQuestionReader.TAG_CONCLUSION_CONFIDENCE)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchClosedDate(),
			FLEFRecordHelper.getChildValue(ctx.root, ResearchQuestionReader.TAG_CLOSED_DATE));

		//the proof statuses, in the order of the protocol, then the ones not declared
		final Map<String, Integer> byStatus = new LinkedHashMap<>();
		for(final String status : ConclusionReader.PROOF_STATUSES)
			byStatus.put(status, 0);
		for(final FLEFRecord conclusion : conclusions){
			final String status = StringUtils.defaultIfBlank(ConclusionReader.extractProofStatus(conclusion), StringUtils.EMPTY);
			byStatus.merge(status.toLowerCase(Locale.ROOT), 1, Integer::sum);
		}
		final List<String> distribution = new ArrayList<>();
		for(final Map.Entry<String, Integer> entry : byStatus.entrySet())
			if(entry.getValue() > 0)
				distribution.add(entry.getValue() + StringUtils.SPACE
					+ StringUtils.defaultIfBlank(ctx.labels.enumValue("enum.proof_status", entry.getKey()), ctx.labels.sections().gpsNoProofStatus()));
		if(!distribution.isEmpty())
			rows.add("**" + ctx.labels.sections().researchProofStatus() + ":** " + String.join(", ", distribution));

		if(rows.isEmpty())
			out.add(nothingRecorded());
		else
			out.add(new ReportSection.BulletList(rows.stream()
				.map(ReportFormatters::escape)
				.toList()));
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private ReportSection nothingRecorded(){
		return new ReportSection.Paragraph("*" + ctx.labels.sections().gpsNothingRecorded() + "*");
	}

	private static String issueOf(final FLEFRecord conclusion){
		return StringUtils.defaultIfBlank(ConclusionReader.extractIssues(conclusion), conclusion.getId());
	}

	/** The activity in one line: its type, what was done and the result. */
	private String activitySummary(final FLEFRecord activity){
		final StringBuilder sb = new StringBuilder();
		final String type = ctx.labels.enumValue("enum.research_activity.type",
			FLEFRecordHelper.getChildValue(activity, ResearchActivityReader.TAG_ACTIVITY_TYPE));
		final String action = FLEFRecordHelper.getChildValue(activity, ResearchActivityReader.TAG_ACTION);
		if(type != null)
			sb.append(type);
		if(StringUtils.isNotBlank(action)){
			if(!sb.isEmpty())
				sb.append(" — ");
			sb.append(action);
		}
		if(sb.isEmpty())
			sb.append(ReportFormatters.orEmpty(activity.getId()));
		final String result = ctx.labels.enumValue("enum.research_activity.result",
			FLEFRecordHelper.getChildValue(activity, ResearchActivityReader.TAG_RESULT));
		if(result != null)
			sb.append(": ").append(result);
		return sb.toString();
	}

	/** The citations of the activities and of the conclusions of the question. */
	private static List<FLEFRecord> citationsOf(final List<FLEFRecord> activities, final List<FLEFRecord> conclusions){
		final List<FLEFRecord> citations = new ArrayList<>();
		for(final FLEFRecord activity : activities)
			citations.addAll(FLEFRecordHelper.findChildren(activity, ResearchActivityReader.TAG_SOURCE));
		for(final FLEFRecord conclusion : conclusions)
			citations.addAll(FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_SOURCE));
		return citations;
	}

	/** @return	How many citations have each value of a qualifier ("3 primary, 1 secondary"); {@code null} if none has it. */
	private String tally(final List<FLEFRecord> citations, final String qualifier, final String enumPrefix){
		final Map<String, Integer> counts = new LinkedHashMap<>();
		for(final FLEFRecord citation : citations){
			final FLEFRecord evidence = FLEFRecordHelper.findChild(citation, SourceCitationReader.TAG_EVIDENCE);
			final String value = (evidence != null? FLEFRecordHelper.getChildValue(evidence, qualifier): null);
			if(StringUtils.isNotBlank(value))
				counts.merge(value.trim(), 1, Integer::sum);
		}
		if(counts.isEmpty())
			return null;

		final List<String> parts = new ArrayList<>();
		for(final Map.Entry<String, Integer> entry : counts.entrySet())
			parts.add(entry.getValue() + StringUtils.SPACE + ctx.labels.enumValue(enumPrefix, entry.getKey()));
		return String.join(", ", parts);
	}

}
