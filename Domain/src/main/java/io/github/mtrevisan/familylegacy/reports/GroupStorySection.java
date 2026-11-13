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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;


/**
 * Builds the life story of a group: name, type, preferred image, members,
 * attributes, parent/child groups, events in which the group participates,
 * notes, citations, media and audit trail.
 *
 * <p>This section is emitted only for group roots.</p>
 */
public final class GroupStorySection implements SectionBuilder{

	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	public GroupStorySection(final ReportContext ctx){
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
		writePreferredImage(out, ctx.model);
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

	private void writePreferredImage(final List<ReportSection> out, final FLEFModel model){
		if(!ctx.config.includePictures())
			return;

		final FLEFRecord preferred = FLEFRecordHelper.findChild(ctx.root, GroupReader.TAG_PREFERRED_IMAGE);
		if(preferred == null)
			return;

		final String uri = GroupReader.extractPreferredImageUri(preferred, model);
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

		final String type = FLEFRecordHelper.getChildValue(ctx.root, GroupReader.TAG_TYPE);
		if(type != null && !type.isBlank())
			rows.add("**" + ctx.labels.sections().groupType() + ":** " + ReportFormatters.escape(type));

		for(final FLEFRecord nameRec : FLEFRecordHelper.findChildren(ctx.root, GroupReader.TAG_NAME)){
			final String nameType = FLEFRecordHelper.getChildValue(nameRec, NameReader.TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(nameRec, NameReader.TAG_VALUE);
			if(StringUtils.isEmpty(value))
				continue;
			final StringBuilder line = new StringBuilder();
			line.append("**").append(ctx.labels.sections().name());
			if(nameType != null && !nameType.isBlank())
				line.append(StringUtils.SPACE)
					.append('(')
					.append(ReportFormatters.escape(nameType))
					.append(')');
			line.append(":** ")
				.append(ReportFormatters.escape(value));
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

		//the parents first, as in the file, then the children from the eldest to the youngest
		final TreeText text = new TreeText(ctx);
		final List<FLEFRecord> parents = new ArrayList<>();
		final List<FLEFRecord> children = new ArrayList<>();
		for(final FLEFRecord m : memberships){
			final String memberId = memberIndividualId(m);
			final FLEFRecord member = (memberId != null? ctx.visible(ctx.model.getRecordById(memberId)): null);
			if(member == null)
				continue;

			(RelationshipReader.ENUM_TYPE_CHILD.equalsIgnoreCase(FLEFRecordHelper.getChildValue(m, RelationshipReader.TAG_ROLE))
				? children: parents).add(m);
		}
		children.sort(Comparator.comparing((final FLEFRecord m) -> ctx.model.getRecordById(memberIndividualId(m)), text.byBirth()));
		final List<FLEFRecord> ordered = new ArrayList<>(parents);
		ordered.addAll(children);

		final List<List<String>> rows = new ArrayList<>();
		boolean dated = false;
		for(final FLEFRecord m : ordered){
			final FLEFRecord member = ctx.model.getRecordById(memberIndividualId(m));
			final String from = ReportFormatters.orEmpty(
				GenealogicalDateHelper.formatDateStructure(m, RelationshipReader.TAG_VALID_FROM, ctx.labels, contextLabels));
			final String to = ReportFormatters.orEmpty(
				GenealogicalDateHelper.formatDateStructure(m, RelationshipReader.TAG_VALID_TO, ctx.labels, contextLabels));
			dated |= (!from.isEmpty() || !to.isEmpty());
			rows.add(List.of(
				ReportFormatters.escape(ctx.displayText(member)),
				ReportFormatters.orEmpty(text.years(member)),
				ReportFormatters.escape(ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(m, RelationshipReader.TAG_ROLE))),
				ReportFormatters.escape(from),
				ReportFormatters.escape(to)));
		}

		if(rows.isEmpty())
			return;

		//the columns of the period of the membership are there only if some membership has one
		final List<String> headers = new ArrayList<>(List.of(ctx.labels.sections().person(), ctx.labels.sections().columnYears(),
			ctx.labels.sections().role()));
		final List<List<String>> table = new ArrayList<>(rows.size());
		if(dated){
			headers.add(ctx.labels.sections().from());
			headers.add(ctx.labels.sections().to());
			table.addAll(rows);
		}
		else
			for(final List<String> row : rows)
				table.add(row.subList(0, 3));
		out.add(new ReportSection.Table(headers, table));

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
					ctx.citations()
						.addEvidence(m, extras);
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
				ReportFormatters.esc(FLEFRecordHelper.getChildValue(a, GroupAttributeReader.TAG_TYPE)),
				ReportFormatters.esc(FLEFRecordHelper.getChildValue(a, GroupAttributeReader.TAG_VALUE)),
				ReportFormatters.esc(ReportFormatters.orEmpty(
					GenealogicalDateHelper.formatDateStructure(
						a, GroupAttributeReader.TAG_VALID_FROM, ctx.labels, contextLabels))),
				ReportFormatters.esc(ReportFormatters.orEmpty(
					GenealogicalDateHelper.formatDateStructure(
						a, GroupAttributeReader.TAG_VALID_TO, ctx.labels, contextLabels))),
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
			? FLEFRecordHelper.getChildValue(rel, RelationshipReader.TAG_SUBJECT_GROUP)
			: FLEFRecordHelper.getChildValue(rel, RelationshipReader.TAG_TARGET_GROUP));
		final FLEFRecord counterpart = (counterpartId != null
			? ctx.visible(ctx.model.getRecordById(counterpartId)): null);
		final String label = (counterpart != null? ctx.displayText(counterpart): counterpartId);
		if(StringUtils.isEmpty(label))
			return;

		out.add(new ReportSection.Heading(3, ReportFormatters.escape(label)));

		final String from = GenealogicalDateHelper.formatDateStructure(
			rel, RelationshipReader.TAG_VALID_FROM, ctx.labels, contextLabels);
		final String to = GenealogicalDateHelper.formatDateStructure(
			rel, RelationshipReader.TAG_VALID_TO, ctx.labels, contextLabels);
		final List<String> meta = new ArrayList<>();
		ReportFormatters.appendValidity(meta, ctx.labels, from, to);
		if(!meta.isEmpty())
			out.add(new ReportSection.BulletList(meta));

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(rel));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(rel));
		if(ctx.config.evidence())
			ctx.citations()
				.addEvidence(rel, out);
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
		for(final FLEFRecord evt : IndividualLifeStorySection.chronological(events))
			writeEvent(out, evt);
	}

	private void writeEvent(final List<ReportSection> out, final FLEFRecord evt){
		final String type = ReportFormatters.escape(ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(evt, EventReader.TAG_TYPE)));
		final String date = ReportFormatters.escape(ReportFormatters.orEmpty(
			GenealogicalDateHelper.formatEventDate(evt, ctx.labels, contextLabels)));
		out.add(new ReportSection.Heading(3,
			(type.isEmpty()? ctx.labels.sections().eventGeneric(): type) + (date.isEmpty()? StringUtils.EMPTY: " — " + date)));

		final List<String> details = new ArrayList<>();
		final String place = PlaceCitationReader.extractPlace(evt, ctx.model);
		if(place != null)
			details.add("**" + ctx.labels.sections().place() + ":** " + ReportFormatters.escape(place));

		final String descr = FLEFRecordHelper.getChildValue(evt, EventReader.TAG_DESCRIPTION);
		if(descr != null && !descr.isBlank())
			details.add("**" + ctx.labels.sections().description() + ":** " + ReportFormatters.escape(descr));
		if(!details.isEmpty())
			out.add(new ReportSection.BulletList(details));

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(evt));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(evt));
		if(ctx.config.evidence())
			ctx.citations()
				.addEvidence(evt, out);
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

	static String memberIndividualId(final FLEFRecord rel){
		final String direct = FLEFRecordHelper.getChildValue(rel, RelationshipReader.TAG_SUBJECT_INDIVIDUAL);
		if(direct != null)
			return direct;
		final FLEFRecord subjNode = FLEFRecordHelper.findChild(rel, RelationshipReader.TAG_SUBJECT);
		if(subjNode == null)
			return null;
		final FLEFRecord ref = subjNode.getTheOnlyChild();
		if(ref == null)
			return null;
		return (IndividualReader.isOfType(ref)? ref.getValue(): null);
	}

}
