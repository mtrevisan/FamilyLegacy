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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main section for a research-question report.
 *
 * <p>Renders the question, its status, conclusion and confidence, then every
 * activity, task and conclusion linked to it, with the same level of detail
 * used by {@link ContextResearchSection}.</p>
 */
final class ResearchQuestionRootSection implements SectionBuilder{

	private static final String TAG_TITLE = "title";
	private static final String TAG_QUESTION = "question";
	private static final String TAG_STATUS = "status";
	private static final String TAG_CONCLUSION = "conclusion";
	private static final String TAG_CONCLUSION_CONFIDENCE = "conclusion_confidence";
	private static final String TAG_CLOSED_DATE = "closed_date";
	private static final String TAG_TARGET = "target";


	private final ReportContext ctx;


	ResearchQuestionRootSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isResearchQuestionRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();

		final String title = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(ctx.root, TAG_TITLE));
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.sections().researchQuestionOf(),
			ReportFormatters.escape(title))));

		writeQuestion(out);
		writeTargets(out);
		writeActivities(out);
		writeTasks(out);
		writeConclusions(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(ctx.root, out);
		out.addAll(ctx.citations().audit(ctx.root));
		out.addAll(ctx.citations().privacyInfo(ctx.root));
		return out;
	}


	private void writeQuestion(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().question(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_QUESTION));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(ctx.root, TAG_STATUS)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().conclusion(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_CONCLUSION));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchConclusionConfidence(),
			ReportFormatters.enumLabel(
				FLEFRecordHelper.getChildValue(ctx.root, TAG_CONCLUSION_CONFIDENCE)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchClosedDate(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_CLOSED_DATE));

		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		final String rationale = FLEFRecordHelper.getChildValue(ctx.root, "rationale");
		if(rationale != null)
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(rationale)));
	}


	private void writeTargets(final List<ReportSection> out){
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord t : FLEFRecordHelper.findChildren(ctx.root, TAG_TARGET)){
			final FLEFRecord ref = t.getTheOnlyChild();
			if(ref == null || FLEFRecord.TAG_VOID.equalsIgnoreCase(ref.getTag()))
				continue;

			final String id = ref.getValue();
			if(id == null)
				continue;

			final FLEFRecord rec = ctx.visible(ctx.model.getRecordById(id));
			items.add(rec != null? ctx.displayText(rec): id);
		}
		if(items.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().researchTargets()));
		out.add(new ReportSection.BulletList(items.stream()
			.map(ReportFormatters::escape).toList()));
	}


	private void writeActivities(final List<ReportSection> out){
		final List<FLEFRecord> acts = ctx.index.activitiesForQuestion(ctx.root);
		if(acts.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().researchActivities()));
		for(final FLEFRecord a : acts)
			writeActivity(out, a);
	}

	private void writeActivity(final List<ReportSection> out, final FLEFRecord a){
		final String type = ReportFormatters.enumLabel(
			FLEFRecordHelper.getChildValue(a, "activity_type"));
		final String action = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(a, "action"));
		final String heading = (type != null? type + " — ": StringUtils.EMPTY) + action;
		out.add(new ReportSection.Heading(3, ReportFormatters.escape(heading)));

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(a, "status")));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchResult(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(a, "result")));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		final String obs = FLEFRecordHelper.getChildValue(a, "observation");
		if(obs != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchObservation() + ":** "
					+ ReportFormatters.escape(obs)));

		final String concl = FLEFRecordHelper.getChildValue(a, TAG_CONCLUSION);
		if(concl != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().conclusion() + ":** " + ReportFormatters.escape(concl)));

		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(a));
		out.addAll(ctx.citations().audit(a));
	}


	private void writeTasks(final List<ReportSection> out){
		final List<FLEFRecord> tasks = ctx.index.tasksForQuestion(ctx.root);
		if(tasks.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().researchTasks()));
		for(final FLEFRecord t : tasks){
			out.add(new ReportSection.Heading(3, ReportFormatters.escape(
				ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(t, "description")))));

			final List<String> rows = new ArrayList<>();
			ReportFormatters.appendIfPresent(rows, ctx.labels.sections().status(),
				ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(t, "status")));
			ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchTaskPriority(),
				ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(t, "priority")));
			ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchTaskDueDate(),
				FLEFRecordHelper.getChildValue(t, "due_date"));
			if(!rows.isEmpty())
				out.add(new ReportSection.BulletList(rows));

			final String outcome = FLEFRecordHelper.getChildValue(t, "outcome");
			if(outcome != null)
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.sections().researchTaskOutcome() + ":** "
						+ ReportFormatters.escape(outcome)));
		}
	}


	private void writeConclusions(final List<ReportSection> out){
		final List<FLEFRecord> concl = ctx.index.conclusionsForQuestion(ctx.root);
		if(concl.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().conclusions()));
		for(final FLEFRecord c : concl){
			final String issue = FLEFRecordHelper.getChildValue(c, "issue");
			if(issue != null)
				out.add(new ReportSection.Heading(3,
					ctx.labels.sections().researchIssue() + ": " + ReportFormatters.escape(issue)));

			final String proof = FLEFRecordHelper.getChildValue(c, "proof_status");
			if(proof != null)
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.sections().researchProofStatus() + ":** "
						+ ReportFormatters.escape(ReportFormatters.enumLabel(proof))));

			final String narrative = FLEFRecordHelper.getChildValue(c, "narrative");
			if(narrative != null)
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.sections().researchNarrative() + ":** "
						+ ReportFormatters.escape(narrative)));

			if(ctx.config.sources())
				out.addAll(ctx.citations().citations(c));
		}
	}

}
