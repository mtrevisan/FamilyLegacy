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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main section for a research-activity report.
 *
 * <p>Renders the activity type, action, status, result, search scope,
 * observation and conclusion, then lists the questions it contributes to,
 * the task it generated and the parent activity it follows.
 */
final class ResearchActivityRootSection implements SectionBuilder{

	private static final String TAG_ACTIVITY_TYPE = "activity_type";
	private static final String TAG_ACTION = "action";
	private static final String TAG_STATUS = "status";
	private static final String TAG_RESULT = "result";
	private static final String TAG_SCOPE_TYPE = "search_scope.type";
	private static final String TAG_SCOPE_DETAIL = "search_scope.detail";
	private static final String TAG_OBSERVATION = "observation";
	private static final String TAG_CONCLUSION = "conclusion";
	private static final String TAG_QUESTION = "question";
	private static final String TAG_PARENT = "parent_activity";
	private static final String TAG_TITLE = "title";
	private static final String TAG_DESCRIPTION = "description";


	private final ReportContext ctx;


	ResearchActivityRootSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isResearchActivityRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		final String type = ReportFormatters.enumLabel(
			FLEFRecordHelper.getChildValue(ctx.root, TAG_ACTIVITY_TYPE));
		final String action = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(ctx.root, TAG_ACTION));
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.sections().researchActivityOf(),
			(type != null? type + " — ": StringUtils.EMPTY) + action)));

		writeBasicInfo(out);
		writeQuestions(out);
		writeParent(out);
		writeTasks(out);
		writeSourcesAndAudit(out);
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(ctx.root, TAG_STATUS)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchResult(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(ctx.root, TAG_RESULT)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchSearchScope(),
			describeScope());
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchObservation(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_OBSERVATION));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().conclusion(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_CONCLUSION));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private String describeScope(){
		final String type = FLEFRecordHelper.getChildValue(ctx.root, TAG_SCOPE_TYPE);
		final String detail = FLEFRecordHelper.getChildValue(ctx.root, TAG_SCOPE_DETAIL);
		if(type == null && detail == null)
			return null;
		final StringBuilder sb = new StringBuilder();
		if(type != null) sb.append(ReportFormatters.enumLabel(type));
		if(detail != null){
			if(!sb.isEmpty()) sb.append(" — ");
			sb.append(detail);
		}
		return sb.toString();
	}


	private void writeQuestions(final List<ReportSection> out){
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(ctx.root, TAG_QUESTION)){
			final String id = qRef.getValue();
			if(id == null)
				continue;
			final FLEFRecord q = ctx.visible(ctx.model.getRecordById(id));
			if(q == null)
				continue;
			final String title = FLEFRecordHelper.getChildValue(q, TAG_TITLE);
			items.add(title != null && !title.isBlank()? title: id);
		}
		if(items.isEmpty())
			return;
		out.add(new ReportSection.Heading(2, ctx.labels.sections().researchLinkedQuestions()));
		out.add(new ReportSection.BulletList(items.stream()
			.map(ReportFormatters::escape).toList()));
	}


	private void writeParent(final List<ReportSection> out){
		final FLEFRecord parentNode = FLEFRecordHelper.findChild(ctx.root, TAG_PARENT);
		if(parentNode == null)
			return;
		final FLEFRecord ref = parentNode.getTheOnlyChild();
		if(ref == null || ref.getValue() == null)
			return;
		final FLEFRecord parent = ctx.visible(ctx.model.getRecordById(ref.getValue()));
		if(parent == null)
			return;
		final String type = ReportFormatters.enumLabel(
			FLEFRecordHelper.getChildValue(parent, TAG_ACTIVITY_TYPE));
		final String action = FLEFRecordHelper.getChildValue(parent, TAG_ACTION);
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.sections().researchParentActivity() + ":** "
				+ (type != null? type + " — ": StringUtils.EMPTY) + action));
	}


	private void writeTasks(final List<ReportSection> out){
		final List<FLEFRecord> tasks = ctx.index.tasksForActivity(ctx.root);
		if(tasks.isEmpty())
			return;
		out.add(new ReportSection.Heading(2, ctx.labels.sections().researchTasks()));
		for(final FLEFRecord t : tasks)
			out.add(new ReportSection.Paragraph("• " + ReportFormatters.escape(
				ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(t, TAG_DESCRIPTION)))));
	}


	private void writeSourcesAndAudit(final List<ReportSection> out){
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(ctx.root, out);
		out.addAll(ctx.citations().audit(ctx.root));
	}

}
