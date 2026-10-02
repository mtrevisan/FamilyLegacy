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
import java.util.Comparator;
import java.util.List;


/**
 * Builds a global chronological timeline of every event semantically tied
 * to the root — individual events, group events, historic events
 * (context impacts). The timeline is optional and complementary to the
 * thematic sections.
 */
final class TimelineSection implements SectionBuilder{

	private static final String TAG_TYPE = "type";
	private static final String TAG_TITLE = "title";


	private final ReportContext ctx;


	TimelineSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.timeline())
			return List.of();

		final List<FLEFRecord> events = collectEvents();
		if(events.isEmpty())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.sections().timeline()));

		final List<List<String>> rows = new ArrayList<>(events.size());
		for(final FLEFRecord e : events){
			final Integer year = GenealogicalDateHelper.yearOrNull(e);
			final String date = ReportFormatters.orEmpty(
				GenealogicalDateHelper.formatEventDate(e, ctx.labels,
					ctx.contextLabelResolver()));
			final String type = ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(e, TAG_TYPE));
			final String title = ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(e, TAG_TITLE));
			final String place = ReportFormatters.orEmpty(
				FLEFRecordHelper.extractPlace(e, ctx.model));
			rows.add(List.of(
				String.valueOf(year != null? year: StringUtils.EMPTY),
				ReportFormatters.escape(date),
				ReportFormatters.escape(type),
				ReportFormatters.escape(title),
				ReportFormatters.escape(place)));
		}
		out.add(new ReportSection.Table(
			List.of(ctx.labels.sections().year(), ctx.labels.sections().date(), ctx.labels.sections().type(),
				ctx.labels.sections().title(), ctx.labels.sections().place()),
			rows));
		return out;
	}


	private List<FLEFRecord> collectEvents(){
		final List<FLEFRecord> out = new ArrayList<>();
		switch(ctx.rootKind()){
			case INDIVIDUAL -> out.addAll(ctx.index.eventsOf(ctx.root));
			case GROUP -> out.addAll(ctx.index.eventsOfGroup(ctx.root));
			case PLACE -> {
				out.addAll(ctx.index.eventsAtPlace(ctx.root));
				out.addAll(ctx.index.eventsByPlace(ctx.root));
			}
			default -> { /* no timeline for other roots */ }
		}

		// Add historic events connected via context_impact.
		for(final FLEFRecord ci : ctx.index.contextImpactsFor(ctx.relatedRecordIds())){
			final FLEFRecord contextNode = FLEFRecordHelper.findChild(ci, "context");
			if(contextNode == null)
				continue;
			final FLEFRecord ref = contextNode.getTheOnlyChild();
			if(ref == null)
				continue;
			if(!"historic_event".equalsIgnoreCase(ref.getTag()))
				continue;
			final FLEFRecord rec = ctx.visible(ctx.model.getRecordById(ref.getValue()));
			if(rec != null && !out.contains(rec))
				out.add(rec);
		}

		out.sort(Comparator.comparingInt(GenealogicalDateHelper::yearOrMax));
		return out;
	}

}
