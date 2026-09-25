package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main section for a research-task report.
 */
final class ResearchTaskRootSection implements SectionBuilder{

	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_STATUS = "status";
	private static final String TAG_PRIORITY = "priority";
	private static final String TAG_DUE_DATE = "due_date";
	private static final String TAG_OUTCOME = "outcome";
	private static final String TAG_CREATED_BY = "created_by";
	private static final String TAG_QUESTION = "question";
	private static final String TAG_TITLE = "title";


	private final ReportContext ctx;


	ResearchTaskRootSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isResearchTaskRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.researchTaskOf(),
			ReportFormatters.escape(ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(ctx.root, TAG_DESCRIPTION))))));

		writeBasicInfo(out);
		writeQuestions(out);
		writeCreatedBy(out);

		out.addAll(ctx.citations().audit(ctx.root));
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(ctx.root, TAG_STATUS)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.researchTaskPriority(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(ctx.root, TAG_PRIORITY)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.researchTaskDueDate(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_DUE_DATE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.researchTaskOutcome(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_OUTCOME));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeQuestions(final List<ReportSection> out){
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(ctx.root, TAG_QUESTION)){
			final String id = qRef.getValue();
			if(id == null) continue;
			final FLEFRecord q = ctx.visible(ctx.model.getRecordById(id));
			if(q == null) continue;
			final String title = FLEFRecordHelper.getChildValue(q, TAG_TITLE);
			items.add(title != null && !title.isBlank()? title: id);
		}
		if(items.isEmpty()) return;
		out.add(new ReportSection.Heading(2, ctx.labels.researchLinkedQuestions()));
		out.add(new ReportSection.BulletList(items.stream()
			.map(ReportFormatters::escape).toList()));
	}


	private void writeCreatedBy(final List<ReportSection> out){
		final FLEFRecord createdBy = FLEFRecordHelper.findChild(ctx.root, TAG_CREATED_BY);
		if(createdBy == null) return;
		final FLEFRecord ref = createdBy.getTheOnlyChild();
		if(ref == null || ref.getValue() == null) return;
		final FLEFRecord activity = ctx.visible(ctx.model.getRecordById(ref.getValue()));
		if(activity == null) return;
		final String action = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(activity, "action"));
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.researchTaskCreatedBy() + ":** "
				+ ReportFormatters.escape(action)));
	}

}
