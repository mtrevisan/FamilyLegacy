package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

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
		out.add(new ReportSection.Heading(1, ctx.labels.timeline()));

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
				String.valueOf(year != null? year: ""),
				ReportFormatters.escape(date),
				ReportFormatters.escape(type),
				ReportFormatters.escape(title),
				ReportFormatters.escape(place)));
		}
		out.add(new ReportSection.Table(
			List.of(ctx.labels.year(), ctx.labels.date(), ctx.labels.type(),
				ctx.labels.title(), ctx.labels.place()),
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
			if(contextNode == null) continue;
			final FLEFRecord ref = contextNode.getTheOnlyChild();
			if(ref == null) continue;
			if(!"historic_event".equalsIgnoreCase(ref.getTag())) continue;
			final FLEFRecord rec = ctx.visible(ctx.model.getRecordById(ref.getValue()));
			if(rec != null && !out.contains(rec))
				out.add(rec);
		}

		out.sort(Comparator.comparingInt(GenealogicalDateHelper::yearOrMax));
		return out;
	}

}
