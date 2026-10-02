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
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Builds the main section for a historic-event report.
 *
 * <p>Renders the event's title, type, date, place, then every context-impact
 * that references it, so the reader sees which genealogical assertions are
 * explained, influenced, constrained or caused by this historic event.
 */
final class HistoricEventRootSection implements SectionBuilder{

	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	HistoricEventRootSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isHistoricEventRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		final String title = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(ctx.root, HistoricEventReader.TAG_TITLE));
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.sections().historicEventOf(), ReportFormatters.escape(title))));

		writeBasicInfo(out);
		writeImpacts(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		out.addAll(ctx.citations().audit(ctx.root));
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().historicEventType(),
			FLEFRecordHelper.getChildValue(ctx.root, HistoricEventReader.TAG_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().date(),
			GenealogicalDateHelper.formatEventDate(ctx.root, ctx.labels, contextLabels));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().place(),
			FLEFRecordHelper.extractPlace(ctx.root, ctx.model));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeImpacts(final List<ReportSection> out){
		final List<FLEFRecord> impacts = ctx.index.contextImpactsFor(ctx.relatedRecordIds());
		if(impacts.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().historicEventImpacts()));
		for(final FLEFRecord ci : impacts){
			final String impactType = ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(ci, ContextImpactReader.TAG_IMPACT_TYPE));
			final String targetId = extractTargetId(ci);
			final String targetLabel = (targetId != null
				? describeTarget(targetId): "?");
			out.add(new ReportSection.Paragraph(
				"**" + ReportFormatters.escape(impactType) + ":** "
					+ ReportFormatters.escape(targetLabel)));

			final String rationale = FLEFRecordHelper.getChildValue(ci, ContextImpactReader.TAG_RATIONALE);
			if(rationale != null)
				out.add(new ReportSection.Paragraph(
					"  " + ReportFormatters.escape(rationale)));
		}
	}

	private String extractTargetId(final FLEFRecord ci){
		final FLEFRecord targetNode = FLEFRecordHelper.findChild(ci, ContextImpactReader.TAG_TARGET);
		if(targetNode == null)
			return null;
		final FLEFRecord ref = targetNode.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}

	private String describeTarget(final String id){
		final FLEFRecord rec = ctx.model.getRecordById(id);
		if(!ctx.isVisible(rec))
			return id;
		return ctx.displayText(rec);
	}

}
