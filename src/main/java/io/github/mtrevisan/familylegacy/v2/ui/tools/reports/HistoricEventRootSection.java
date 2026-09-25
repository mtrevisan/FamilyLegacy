package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

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

	private static final String TAG_TITLE = "title";
	private static final String TAG_TYPE = "type";
	private static final String TAG_CONTEXT = "context";
	private static final String TAG_IMPACT = "impact_type";
	private static final String TAG_RATIONALE = "rationale";
	private static final String TAG_TARGET = "target";


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
			FLEFRecordHelper.getChildValue(ctx.root, TAG_TITLE));
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.historicEventOf(), ReportFormatters.escape(title))));

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
		ReportFormatters.appendIfPresent(rows, ctx.labels.historicEventType(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.date(),
			GenealogicalDateHelper.formatEventDate(ctx.root, ctx.labels, contextLabels));
		ReportFormatters.appendIfPresent(rows, ctx.labels.place(),
			FLEFRecordHelper.extractPlace(ctx.root, ctx.model));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeImpacts(final List<ReportSection> out){
		final List<FLEFRecord> impacts = ctx.index.contextImpactsFor(ctx.relatedRecordIds());
		if(impacts.isEmpty()) return;

		out.add(new ReportSection.Heading(2, ctx.labels.historicEventImpacts()));
		for(final FLEFRecord ci : impacts){
			final String impactType = ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(ci, TAG_IMPACT));
			final String targetId = extractTargetId(ci);
			final String targetLabel = (targetId != null
				? describeTarget(targetId): "?");
			out.add(new ReportSection.Paragraph(
				"**" + ReportFormatters.escape(impactType) + ":** "
					+ ReportFormatters.escape(targetLabel)));

			final String rationale = FLEFRecordHelper.getChildValue(ci, TAG_RATIONALE);
			if(rationale != null)
				out.add(new ReportSection.Paragraph(
					"  " + ReportFormatters.escape(rationale)));
		}
	}

	private String extractTargetId(final FLEFRecord ci){
		final FLEFRecord targetNode = FLEFRecordHelper.findChild(ci, TAG_TARGET);
		if(targetNode == null) return null;
		final FLEFRecord ref = targetNode.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}

	private String describeTarget(final String id){
		final FLEFRecord rec = ctx.model.getRecordById(id);
		if(rec == null || !ctx.isVisible(rec)) return id;
		return ctx.displayText(rec);
	}

}
