package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Builds the introduction for a group report. Mirrors
 * {@link IntroductionSection} but with statistics that make sense for a
 * group: number of members, subgroups, attributes, events and cited sources.
 *
 * <p>Emitted only when {@code config.introduction()} is enabled.</p>
 */
final class GroupIntroductionSection implements SectionBuilder{

	private static final String TAG_TYPE = "type";
	private static final String TYPE_FOUNDING = "founding";
	private static final String TYPE_DISSOLVED = "dissolved";


	private final ReportContext ctx;


	GroupIntroductionSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.introduction())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.introduction()));
		out.add(new ReportSection.Paragraph(String.format(
			ctx.labels.groupIntroductionBody(),
			"**" + ReportFormatters.escape(ctx.displayText(ctx.root)) + "**")));

		final List<FLEFRecord> events = ctx.index.eventsOfGroup(ctx.root);

		out.add(new ReportSection.Paragraph(ctx.labels.statistics()));
		out.add(new ReportSection.BulletList(List.of(
			String.format(ctx.labels.groupLifespan(), groupLifespanOf(events)),
			String.format(ctx.labels.eventsCount(), ctx.formatInt(events.size())),
			String.format(ctx.labels.sourcesCount(), ctx.formatInt(sourcesCount())),
			ctx.labels.groupCounts(
				ctx.index.membersOf(ctx.root).size(),
				ctx.index.childGroupsOf(ctx.root).size(),
				ctx.index.attributesOfGroup(ctx.root).size())
		)));
		return out;
	}


	private String groupLifespanOf(final List<FLEFRecord> events){
		Integer fromYear = null;
		Integer toYear = null;

		for(final FLEFRecord evt : events){
			final String t = FLEFRecordHelper.getChildValue(evt, TAG_TYPE);
			if(t == null)
				continue;
			final Integer y = GenealogicalDateHelper.yearOrNull(evt);
			if(y == null)
				continue;
			switch(t.toLowerCase(Locale.ROOT)){
				case TYPE_FOUNDING -> fromYear = y;
				case TYPE_DISSOLVED -> toYear = y;
				default -> { /* ignore */ }
			}
		}

		if(fromYear == null && toYear == null)
			return "—";

		final String span = (fromYear != null? fromYear.toString(): "?")
			+ "–"
			+ (toYear != null? toYear.toString(): "?");
		return span;
	}


	private int sourcesCount(){
		final List<String> ids = new ArrayList<>();
		SourceCollector.collectForGroup(ctx, ids);
		return ids.size();
	}

}
