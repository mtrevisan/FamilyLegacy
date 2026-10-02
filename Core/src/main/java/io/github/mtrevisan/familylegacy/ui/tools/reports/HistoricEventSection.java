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
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * Builds the Historic events section.
 *
 * <p>Collects every {@code historic_event} referenced by a
 * {@code context_impact} whose target is semantically tied to the root
 * (see {@link ReportContext#relatedRecordIds()}), and renders each with its
 * type, title, date, place, note, sources, evidence and audit.</p>
 *
 * <p>The section is emitted only when {@code config.historicEvents()} is
 * enabled and at least one historic event is reachable.</p>
 */
final class HistoricEventSection implements SectionBuilder{

	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	HistoricEventSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.historicEvents())
			return List.of();

		final List<FLEFRecord> events = collectHistoricEvents();
		if(events.isEmpty())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.sections().historicEvents()));
		for(final FLEFRecord evt : events)
			appendHistoricEvent(out, evt);
		return out;
	}


	/* ======================================================================
	 *                          Collection
	 * ====================================================================== */

	private List<FLEFRecord> collectHistoricEvents(){
		final Set<String> ids = new LinkedHashSet<>();
		for(final FLEFRecord ci : ctx.index.contextImpactsFor(ctx.relatedRecordIds())){
			final FLEFRecord contextNode = FLEFRecordHelper.findChild(ci, ContextImpactReader.TAG_CONTEXT);
			if(contextNode == null)
				continue;
			final FLEFRecord ref = contextNode.getTheOnlyChild();
			if(ref == null || FLEFRecord.TAG_VOID.equalsIgnoreCase(ref.getTag()))
				continue;
			if(!HistoricEventHandler.TYPE.equalsIgnoreCase(ref.getTag()))
				continue;

			final String id = ref.getValue();
			if(id == null || id.isBlank())
				continue;
			ids.add(id);
		}

		final List<FLEFRecord> out = new ArrayList<>(ids.size());
		for(final String id : ids){
			final FLEFRecord rec = ctx.visible(ctx.model.getRecordById(id));
			if(rec != null)
				out.add(rec);
		}
		return out;
	}


	/* ======================================================================
	 *                          Rendering
	 * ====================================================================== */

	private void appendHistoricEvent(final List<ReportSection> out, final FLEFRecord evt){
		final String title = FLEFRecordHelper.getChildValue(evt, HistoricEventReader.TAG_TITLE);
		final String heading = (title != null && !title.isBlank()
			? title.trim()
			: ReportFormatters.orEmpty(evt.getId()));
		out.add(new ReportSection.Heading(2, ReportFormatters.escape(heading)));

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().historicEventType(),
			FLEFRecordHelper.getChildValue(evt, HistoricEventReader.TAG_TYPE));

		final String date = GenealogicalDateHelper.formatEventDate(
			evt, ctx.labels, contextLabels);
		if(date != null)
			rows.add("**" + ctx.labels.sections().date() + ":** " + ReportFormatters.escape(date));

		final String place = FLEFRecordHelper.extractPlace(evt, ctx.model);
		if(place != null)
			rows.add("**" + ctx.labels.sections().place() + ":** " + ReportFormatters.escape(place));

		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(evt));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(evt));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(evt, out);
		out.addAll(ctx.citations().audit(evt));
	}

}
