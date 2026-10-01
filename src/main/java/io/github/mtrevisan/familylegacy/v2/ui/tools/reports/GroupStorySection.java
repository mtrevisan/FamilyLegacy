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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Builds the life story of a group: name, type, preferred image, members,
 * attributes, parent/child groups, events in which the group participates,
 * notes, citations, media and audit trail.
 *
 * <p>This section is emitted only for group roots.</p>
 */
final class GroupStorySection implements SectionBuilder{

	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_TYPE = "type";
	private static final String TAG_URI = "uri";
	private static final String TAG_ROLE = "role";
	private static final String TAG_STATUS = "status";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_AGENCY = "agency";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	GroupStorySection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isGroupRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		final String who = ReportFormatters.escape(ctx.displayText(ctx.root));

		out.add(new ReportSection.Heading(1, String.format(ctx.labels.narrative().groupLifeOf(), who)));

		writeNarrative(out);
		writePreferredImage(out);
		writePersonalData(out);
		if(ctx.config.groupMembers())
			writeMembers(out);
		if(ctx.config.groupAttributes())
			writeAttributes(out);
		if(ctx.config.groupSubgroups())
			writeSubgroups(out);
		writeEvents(out);
		writeNotes(out);
		writeCitedSourceTitles(out);
		writeAudit(out);

		return out;
	}


	/* ----- Narrative ------------------------------------------------------- */

	private void writeNarrative(final List<ReportSection> out){
		final String narrative = new GroupNarrator(ctx.model, ctx.index, ctx.labels)
			.narrate(ctx.root);
		if(!narrative.isBlank())
			out.add(new ReportSection.Paragraph(narrative));
	}


	/* ----- Preferred image ------------------------------------------------- */

	private void writePreferredImage(final List<ReportSection> out){
		final FLEFRecord preferred = FLEFRecordHelper.findChild(ctx.root, "preferred_image");
		if(preferred == null)
			return;
		final String uri = FLEFRecordHelper.getChildValue(preferred, TAG_URI);
		if(uri == null)
			return;
		final String caption = ReportFormatters.imageCaption(
			preferred, ctx.labels.sections().preferredImage(), ctx.labels);
		try{
			out.add(new ReportSection.Image(Path.of(uri), caption));
		}
		catch(final Exception ignored){
			out.add(new ReportSection.Paragraph("[" + caption + ": " + uri + "]"));
		}
	}


	/* ----- Personal data --------------------------------------------------- */

	private void writePersonalData(final List<ReportSection> out){
		out.add(new ReportSection.Heading(2, ctx.labels.narrative().groupData()));
		final List<String> rows = new ArrayList<>();
		rows.add("**" + ctx.labels.sections().id() + ":** "
			+ ReportFormatters.escape(ReportFormatters.orEmpty(ctx.root.getId())));

		final String type = FLEFRecordHelper.getChildValue(ctx.root, TAG_TYPE);
		if(type != null && !type.isBlank())
			rows.add("**" + ctx.labels.sections().groupType() + ":** " + ReportFormatters.escape(type));

		for(final FLEFRecord nameRec : FLEFRecordHelper.findChildren(ctx.root, TAG_NAME)){
			final String nameType = FLEFRecordHelper.getChildValue(nameRec, TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(nameRec, TAG_VALUE);
			if(value == null || value.isBlank())
				continue;
			final StringBuilder line = new StringBuilder();
			line.append("**").append(ctx.labels.sections().name());
			if(nameType != null && !nameType.isBlank())
				line.append(" (").append(ReportFormatters.escape(nameType)).append(")");
			line.append(":** ").append(ReportFormatters.escape(value));
			rows.add(line.toString());
		}
		out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Members --------------------------------------------------------- */

	private void writeMembers(final List<ReportSection> out){
		final List<FLEFRecord> memberships = ctx.index.membersOf(ctx.root);
		if(memberships.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.narrative().groupMembers()));

		final List<List<String>> rows = new ArrayList<>();
		for(final FLEFRecord m : memberships){
			final String memberId = memberIndividualId(m);
			final FLEFRecord member = (memberId != null
				? ctx.visible(ctx.model.getRecordById(memberId)): null);
			if(member == null)
				continue;

			rows.add(List.of(
				ReportFormatters.escape(ctx.displayText(member)),
				ReportFormatters.escape(ReportFormatters.orEmpty(
					FLEFRecordHelper.getChildValue(m, TAG_ROLE))),
				ReportFormatters.escape(ReportFormatters.orEmpty(
					ReportFormatters.enumLabel(
						FLEFRecordHelper.getChildValue(m, TAG_STATUS)))),
				ReportFormatters.escape(ReportFormatters.orEmpty(
					GenealogicalDateHelper.formatDateStructure(
						m, TAG_VALID_FROM, ctx.labels, contextLabels))),
				ReportFormatters.escape(ReportFormatters.orEmpty(
					GenealogicalDateHelper.formatDateStructure(
						m, TAG_VALID_TO, ctx.labels, contextLabels)))
			));
		}

		if(rows.isEmpty())
			return;

		out.add(new ReportSection.Table(
			List.of(ctx.labels.sections().person(), ctx.labels.sections().role(), ctx.labels.sections().status(),
				ctx.labels.sections().from(), ctx.labels.sections().to()),
			rows));

		// Full details of each membership (notes/sources/evidence/audit).
		if(ctx.config.notes() || ctx.config.sources() || ctx.config.evidence() || ctx.config.audit()){
			for(final FLEFRecord m : memberships){
				final String mid = memberIndividualId(m);
				final FLEFRecord member = (mid != null? ctx.visible(ctx.model.getRecordById(mid)): null);
				if(member == null)
					continue;

				final List<ReportSection> extras = new ArrayList<>();
				if(ctx.config.notes())
					extras.addAll(ctx.citations().notes(m));
				if(ctx.config.sources())
					extras.addAll(ctx.citations().citations(m));
				if(ctx.config.evidence())
					ctx.citations().addEvidence(m, extras);
				extras.addAll(ctx.citations().audit(m));
				if(!extras.isEmpty()){
					out.add(new ReportSection.Heading(3,
						ReportFormatters.escape(ctx.displayText(member))));
					out.addAll(extras);
				}
			}
		}
	}


	/* ----- Attributes ------------------------------------------------------ */

	private void writeAttributes(final List<ReportSection> out){
		final List<FLEFRecord> attrs = ctx.index.attributesOfGroup(ctx.root);
		if(attrs.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().groupAttributes()));
		final List<List<String>> rows = new ArrayList<>();
		for(final FLEFRecord a : attrs)
			rows.add(List.of(
				ReportFormatters.esc(FLEFRecordHelper.getChildValue(a, TAG_TYPE)),
				ReportFormatters.esc(FLEFRecordHelper.getChildValue(a, TAG_VALUE)),
				ReportFormatters.esc(ReportFormatters.orEmpty(
					GenealogicalDateHelper.formatDateStructure(
						a, TAG_VALID_FROM, ctx.labels, contextLabels))),
				ReportFormatters.esc(ReportFormatters.orEmpty(
					GenealogicalDateHelper.formatDateStructure(
						a, TAG_VALID_TO, ctx.labels, contextLabels))),
				ReportFormatters.esc(ReportFormatters.orEmpty(
					ReportFormatters.resolvePlaceName(ctx.model, a)))));

		out.add(new ReportSection.Table(
			List.of(ctx.labels.sections().type(), ctx.labels.sections().value(),
				ctx.labels.sections().from(), ctx.labels.sections().to(), ctx.labels.sections().place()),
			rows));
	}


	/* ----- Subgroups ------------------------------------------------------- */

	private void writeSubgroups(final List<ReportSection> out){
		final List<FLEFRecord> parents = ctx.index.parentGroupsOf(ctx.root);
		final List<FLEFRecord> children = ctx.index.childGroupsOf(ctx.root);
		if(parents.isEmpty() && children.isEmpty())
			return;

		if(!parents.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.sections().groupParentGroups()));
			for(final FLEFRecord rel : parents)
				writeGroupRelation(out, rel, false);
		}
		if(!children.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.sections().groupChildGroups()));
			for(final FLEFRecord rel : children)
				writeGroupRelation(out, rel, true);
		}
	}

	private void writeGroupRelation(final List<ReportSection> out, final FLEFRecord rel,
		final boolean useSubject){
		final String counterpartId = (useSubject
			? FLEFRecordHelper.getChildValue(rel, "subject.group")
			: FLEFRecordHelper.getChildValue(rel, "target.group"));
		final FLEFRecord counterpart = (counterpartId != null
			? ctx.visible(ctx.model.getRecordById(counterpartId)): null);
		final String label = (counterpart != null? ctx.displayText(counterpart): counterpartId);
		if(label == null || label.isBlank())
			return;

		out.add(new ReportSection.Heading(3, ReportFormatters.escape(label)));

		final String from = GenealogicalDateHelper.formatDateStructure(
			rel, TAG_VALID_FROM, ctx.labels, contextLabels);
		final String to = GenealogicalDateHelper.formatDateStructure(
			rel, TAG_VALID_TO, ctx.labels, contextLabels);
		final List<String> meta = new ArrayList<>();
		if(from != null)
			meta.add("**" + ctx.labels.sections().validFrom() + ":** " + ReportFormatters.escape(from));
		if(to != null)
			meta.add("**" + ctx.labels.sections().validTo() + ":** " + ReportFormatters.escape(to));
		if(!meta.isEmpty())
			out.add(new ReportSection.BulletList(meta));

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(rel));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(rel));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(rel, out);
		out.addAll(ctx.citations().audit(rel));
	}


	/* ----- Events ---------------------------------------------------------- */

	private void writeEvents(final List<ReportSection> out){
		final List<FLEFRecord> events = ctx.index.eventsOfGroup(ctx.root);
		out.add(new ReportSection.Heading(2, ctx.labels.sections().groupEvents()));
		if(events.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.sections().empty()));
			return;
		}
		for(final FLEFRecord evt : events)
			writeEvent(out, evt);
	}

	private void writeEvent(final List<ReportSection> out, final FLEFRecord evt){
		final String type = ReportFormatters.escape(ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(evt, TAG_TYPE)));
		final String date = ReportFormatters.escape(ReportFormatters.orEmpty(
			GenealogicalDateHelper.formatEventDate(evt, ctx.labels, contextLabels)));
		out.add(new ReportSection.Heading(3,
			(type.isEmpty()? "Event": type) + (date.isEmpty()? StringUtils.EMPTY: " — " + date)));

		final String place = FLEFRecordHelper.extractPlace(evt, ctx.model);
		if(place != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().place() + ":** " + ReportFormatters.escape(place)));

		final String agency = FLEFRecordHelper.getChildValue(evt, TAG_AGENCY);
		if(agency != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().agency() + ":** " + ReportFormatters.escape(agency)));

		final String descr = FLEFRecordHelper.getChildValue(evt, TAG_DESCRIPTION);
		if(descr != null)
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(descr)));

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(evt));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(evt));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(evt, out);
		out.addAll(ctx.citations().audit(evt));
	}


	/* ----- Notes / cited source titles / audit ---------------------------- */

	private void writeNotes(final List<ReportSection> out){
		if(!ctx.config.notes())
			return;
		final List<ReportSection> n = ctx.citations().notes(ctx.root);
		if(!n.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.notes()));
			out.addAll(n);
		}
	}

	private void writeCitedSourceTitles(final List<ReportSection> out){
		if(!ctx.config.sources())
			return;
		final List<String> ids = new ArrayList<>(SourceCollector.collectForGroup(ctx));
		if(ids.isEmpty())
			return;
		out.add(new ReportSection.Heading(2, ctx.labels.sections().citations()));
		out.add(new ReportSection.BulletList(ids.stream()
			.map(ctx::sourceTitle)
			.toList()));
	}

	private void writeAudit(final List<ReportSection> out){
		final List<ReportSection> audit = ctx.citations().audit(ctx.root);
		if(!audit.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.sections().audit()));
			out.addAll(audit);
		}
	}


	/* ----- Helpers --------------------------------------------------------- */

	private static String memberIndividualId(final FLEFRecord rel){
		final String direct = FLEFRecordHelper.getChildValue(rel, "subject.individual");
		if(direct != null)
			return direct;
		final FLEFRecord subjNode = FLEFRecordHelper.findChild(rel, "subject");
		if(subjNode == null)
			return null;
		final FLEFRecord ref = subjNode.getTheOnlyChild();
		if(ref == null)
			return null;
		return (IndividualHandler.TYPE.equalsIgnoreCase(ref.getTag())? ref.getValue(): null);
	}

}
