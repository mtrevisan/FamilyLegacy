package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Builds the main section of an event report.
 *
 * <p>Renders the event's type, date, place, agency, cause and description,
 * then lists every participant together with its role and its kind
 * (individual / group / place). Notes, citations, evidence and audit of the
 * event record are appended at the bottom.</p>
 */
final class EventRootSection implements SectionBuilder{

	private static final String TAG_TYPE = "type";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_AGENCY = "agency";
	private static final String TAG_CAUSE = "cause";
	private static final String TAG_REASON = "reason";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	EventRootSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isEventRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();

		final String type = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(ctx.root, TAG_TYPE));
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.eventOf(),
			ReportFormatters.escape(type))));

		writeBasicInfo(out);
		writeParticipants(out);

		if(ctx.config.notes()){
			final List<ReportSection> n = ctx.citations().notes(ctx.root);
			if(!n.isEmpty()){
				out.add(new ReportSection.Heading(2, ctx.labels.notes()));
				out.addAll(n);
			}
		}
		if(ctx.config.sources()){
			final List<ReportSection> c = ctx.citations().citations(ctx.root);
			if(!c.isEmpty()){
				out.add(new ReportSection.Heading(2, ctx.labels.citations()));
				out.addAll(c);
			}
		}
		if(ctx.config.evidence())
			ctx.citations().addEvidence(ctx.root, out);
		final List<ReportSection> audit = ctx.citations().audit(ctx.root);
		if(!audit.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.audit()));
			out.addAll(audit);
		}
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();

		final String date = GenealogicalDateHelper.formatEventDate(
			ctx.root, ctx.labels, contextLabels);
		if(date != null)
			rows.add("**" + ctx.labels.date() + ":** " + ReportFormatters.escape(date));

		final String place = FLEFRecordHelper.extractPlace(ctx.root, ctx.model);
		if(place != null)
			rows.add("**" + ctx.labels.place() + ":** " + ReportFormatters.escape(place));

		final String agency = FLEFRecordHelper.getChildValue(ctx.root, TAG_AGENCY);
		if(agency != null)
			rows.add("**" + ctx.labels.agency() + ":** " + ReportFormatters.escape(agency));

		String cause = FLEFRecordHelper.getChildValue(ctx.root, TAG_CAUSE + "." + TAG_REASON);
		if(cause == null)
			cause = FLEFRecordHelper.getChildValue(ctx.root, TAG_CAUSE);
		if(cause != null)
			rows.add("**" + ctx.labels.cause() + ":** " + ReportFormatters.escape(cause));

		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		final String descr = FLEFRecordHelper.getChildValue(ctx.root, TAG_DESCRIPTION);
		if(descr != null && !descr.isBlank())
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(descr)));
	}


	private void writeParticipants(final List<ReportSection> out){
		final List<RelationIndex.Participant> participants = ctx.index.participantsOf(ctx.root);
		if(participants.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.eventParticipants()));

		final List<List<String>> rows = new ArrayList<>(participants.size());
		for(final RelationIndex.Participant p : participants){
			final String role = (p.role() != null && !p.role().isBlank()? p.role(): "—");
			rows.add(List.of(
				ReportFormatters.escape(role),
				ReportFormatters.escape(ctx.displayText(p.record())),
				ReportFormatters.escape(ReportFormatters.orEmpty(
					ReportFormatters.enumLabel(p.kind())))));
		}
		out.add(new ReportSection.Table(
			List.of(ctx.labels.role(), ctx.labels.person(), ctx.labels.kind()), rows));

		// Participation details (source, note, evidence, audit) for each
		// participant, shown as a sub-block.
		for(final RelationIndex.Participant p : participants){
			final List<ReportSection> details =
				ctx.citations().participationDetails(p.participation());
			if(!details.isEmpty()){
				out.add(new ReportSection.Heading(3, ctx.displayText(p.record())));
				out.addAll(details);
			}
		}
	}

}
