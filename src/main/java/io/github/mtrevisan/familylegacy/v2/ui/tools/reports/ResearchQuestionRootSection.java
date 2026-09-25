package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


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
	private static final String TAG_RATIONALE = "rationale";
	private static final String TAG_CLOSED_DATE = "closed_date";
	private static final String TAG_TARGET = "target";
	private static final String TAG_VOID = "void";
	private static final String TAG_TYPE = "type";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	ResearchQuestionRootSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isResearchQuestionRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();

		final String title = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(ctx.root, TAG_TITLE));
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.researchQuestionOf(),
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
		ReportFormatters.appendIfPresent(rows, ctx.labels.question(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_QUESTION));
		ReportFormatters.appendIfPresent(rows, ctx.labels.status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(ctx.root, TAG_STATUS)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.conclusion(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_CONCLUSION));
		ReportFormatters.appendIfPresent(rows, ctx.labels.researchConclusionConfidence(),
			ReportFormatters.enumLabel(
				FLEFRecordHelper.getChildValue(ctx.root, TAG_CONCLUSION_CONFIDENCE)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.researchClosedDate(),
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
			if(ref == null || TAG_VOID.equalsIgnoreCase(ref.getTag()))
				continue;
			final String id = ref.getValue();
			if(id == null) continue;
			final FLEFRecord rec = ctx.visible(ctx.model.getRecordById(id));
			items.add(rec != null? ctx.displayText(rec): id);
		}
		if(items.isEmpty()) return;

		out.add(new ReportSection.Heading(2, ctx.labels.researchTargets()));
		out.add(new ReportSection.BulletList(items.stream()
			.map(ReportFormatters::escape).toList()));
	}


	private void writeActivities(final List<ReportSection> out){
		final List<FLEFRecord> acts = ctx.index.activitiesForQuestion(ctx.root);
		if(acts.isEmpty()) return;
		out.add(new ReportSection.Heading(2, ctx.labels.researchActivities()));
		for(final FLEFRecord a : acts)
			writeActivity(out, a);
	}

	private void writeActivity(final List<ReportSection> out, final FLEFRecord a){
		final String type = ReportFormatters.enumLabel(
			FLEFRecordHelper.getChildValue(a, "activity_type"));
		final String action = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(a, "action"));
		final String heading = (type != null? type + " — ": "") + action;
		out.add(new ReportSection.Heading(3, ReportFormatters.escape(heading)));

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(a, "status")));
		ReportFormatters.appendIfPresent(rows, ctx.labels.researchResult(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(a, "result")));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		final String obs = FLEFRecordHelper.getChildValue(a, "observation");
		if(obs != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.researchObservation() + ":** "
					+ ReportFormatters.escape(obs)));

		final String concl = FLEFRecordHelper.getChildValue(a, TAG_CONCLUSION);
		if(concl != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.conclusion() + ":** " + ReportFormatters.escape(concl)));

		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(a));
		out.addAll(ctx.citations().audit(a));
	}


	private void writeTasks(final List<ReportSection> out){
		final List<FLEFRecord> tasks = ctx.index.tasksForQuestion(ctx.root);
		if(tasks.isEmpty()) return;
		out.add(new ReportSection.Heading(2, ctx.labels.researchTasks()));
		for(final FLEFRecord t : tasks){
			out.add(new ReportSection.Heading(3, ReportFormatters.escape(
				ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(t, "description")))));

			final List<String> rows = new ArrayList<>();
			ReportFormatters.appendIfPresent(rows, ctx.labels.status(),
				ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(t, "status")));
			ReportFormatters.appendIfPresent(rows, ctx.labels.researchTaskPriority(),
				ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(t, "priority")));
			ReportFormatters.appendIfPresent(rows, ctx.labels.researchTaskDueDate(),
				FLEFRecordHelper.getChildValue(t, "due_date"));
			if(!rows.isEmpty())
				out.add(new ReportSection.BulletList(rows));

			final String outcome = FLEFRecordHelper.getChildValue(t, "outcome");
			if(outcome != null)
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.researchTaskOutcome() + ":** "
						+ ReportFormatters.escape(outcome)));
		}
	}


	private void writeConclusions(final List<ReportSection> out){
		final List<FLEFRecord> concl = ctx.index.conclusionsForQuestion(ctx.root);
		if(concl.isEmpty()) return;
		out.add(new ReportSection.Heading(2, ctx.labels.conclusions()));
		for(final FLEFRecord c : concl){
			out.add(new ReportSection.Heading(3, ReportFormatters.escape(
				ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(c, "issue")))));
			ReportFormatters.appendIfPresent(List.of(), ctx.labels.status(),
				FLEFRecordHelper.getChildValue(c, "proof_status"));

			final String proof = FLEFRecordHelper.getChildValue(c, "proof_status");
			if(proof != null)
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.researchProofStatus() + ":** "
						+ ReportFormatters.escape(ReportFormatters.enumLabel(proof))));

			final String narrative = FLEFRecordHelper.getChildValue(c, "narrative");
			if(narrative != null)
				out.add(new ReportSection.Paragraph(ReportFormatters.escape(narrative)));

			if(ctx.config.sources())
				out.addAll(ctx.citations().citations(c));
		}
	}

}
