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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index.EventIndex;

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
			ctx.labels.sections().eventOf(),
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
				out.add(new ReportSection.Heading(2, ctx.labels.sections().citations()));
				out.addAll(c);
			}
		}
		if(ctx.config.evidence())
			ctx.citations().addEvidence(ctx.root, out);
		final List<ReportSection> audit = ctx.citations().audit(ctx.root);
		if(!audit.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.sections().audit()));
			out.addAll(audit);
		}
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();

		final String date = GenealogicalDateHelper.formatEventDate(
			ctx.root, ctx.labels, contextLabels);
		if(date != null)
			rows.add("**" + ctx.labels.sections().date() + ":** " + ReportFormatters.escape(date));

		final String place = FLEFRecordHelper.extractPlace(ctx.root, ctx.model);
		if(place != null)
			rows.add("**" + ctx.labels.sections().place() + ":** " + ReportFormatters.escape(place));

		final String agency = FLEFRecordHelper.getChildValue(ctx.root, TAG_AGENCY);
		if(agency != null)
			rows.add("**" + ctx.labels.sections().agency() + ":** " + ReportFormatters.escape(agency));

		String cause = FLEFRecordHelper.getChildValue(ctx.root, TAG_CAUSE + "." + TAG_REASON);
		if(cause == null)
			cause = FLEFRecordHelper.getChildValue(ctx.root, TAG_CAUSE);
		if(cause != null)
			rows.add("**" + ctx.labels.sections().cause() + ":** " + ReportFormatters.escape(cause));

		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		final String descr = FLEFRecordHelper.getChildValue(ctx.root, TAG_DESCRIPTION);
		if(descr != null && !descr.isBlank())
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(descr)));
	}


	private void writeParticipants(final List<ReportSection> out){
		List<EventIndex.Participant> participants = ctx.index.participantsOf(ctx.root);
		if(participants.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().eventParticipants()));

		final List<List<String>> rows = new ArrayList<>(participants.size());
		for(final EventIndex.Participant p : participants){
			final String role = (p.role() != null && !p.role().isBlank()? p.role(): "—");
			rows.add(List.of(
				ReportFormatters.escape(role),
				ReportFormatters.escape(ctx.displayText(p.record())),
				ReportFormatters.escape(ReportFormatters.orEmpty(
					ReportFormatters.enumLabel(p.kind())))));
		}
		out.add(new ReportSection.Table(
			List.of(ctx.labels.sections().role(), ctx.labels.sections().person(), ctx.labels.sections().kind()), rows));

		// Participation details (source, note, evidence, audit) for each
		// participant, shown as a sub-block.
		for(final EventIndex.Participant p : participants){
			final List<ReportSection> details =
				ctx.citations().participationDetails(p.participation());
			if(!details.isEmpty()){
				out.add(new ReportSection.Heading(3, ctx.displayText(p.record())));
				out.addAll(details);
			}
		}
	}

}
