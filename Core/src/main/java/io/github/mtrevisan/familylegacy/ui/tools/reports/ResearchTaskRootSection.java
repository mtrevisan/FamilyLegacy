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
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main section for a research-task report.
 */
final class ResearchTaskRootSection implements SectionBuilder{

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
			ctx.labels.sections().researchTaskOf(),
			ReportFormatters.escape(ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(ctx.root, ResearchTaskReader.TAG_DESCRIPTION))))));

		writeBasicInfo(out);
		writeQuestions(out);
		writeCreatedBy(out);

		out.addAll(ctx.citations().audit(ctx.root));
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(ctx.root, ResearchTaskReader.TAG_STATUS)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchTaskPriority(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(ctx.root, ResearchTaskReader.TAG_PRIORITY)));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchTaskDueDate(),
			FLEFRecordHelper.getChildValue(ctx.root, ResearchTaskReader.TAG_DUE_DATE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().researchTaskOutcome(),
			FLEFRecordHelper.getChildValue(ctx.root, ResearchTaskReader.TAG_OUTCOME));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeQuestions(final List<ReportSection> out){
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(ctx.root, ResearchTaskReader.TAG_QUESTION)){
			final String id = qRef.getValue();
			if(id == null)
				continue;
			final FLEFRecord q = ctx.visible(ctx.model.getRecordById(id));
			if(q == null)
				continue;
			final String title = FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_TITLE);
			items.add(title != null && !title.isBlank()? title: id);
		}
		if(items.isEmpty())
			return;
		out.add(new ReportSection.Heading(2, ctx.labels.sections().researchLinkedQuestions()));
		out.add(new ReportSection.BulletList(items.stream()
			.map(ReportFormatters::escape).toList()));
	}


	private void writeCreatedBy(final List<ReportSection> out){
		final FLEFRecord createdBy = FLEFRecordHelper.findChild(ctx.root, ResearchTaskReader.TAG_CREATED_BY);
		if(createdBy == null)
			return;
		final FLEFRecord ref = createdBy.getTheOnlyChild();
		if(ref == null || ref.getValue() == null)
			return;
		final FLEFRecord activity = ctx.visible(ctx.model.getRecordById(ref.getValue()));
		if(activity == null)
			return;
		final String action = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(activity, "action"));
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.sections().researchTaskCreatedBy() + ":** "
				+ ReportFormatters.escape(action)));
	}

}
