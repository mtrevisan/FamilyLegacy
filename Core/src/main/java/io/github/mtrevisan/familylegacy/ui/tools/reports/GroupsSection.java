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
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


/**
 * Builds the Groups section.
 *
 * <p>For every group the root individual is a member of ({@code group_member}
 * relationship), the section renders:</p>
 * <ul>
 *   <li>the group name (preferring an {@code official} variant when present)
 *       and type;</li>
 *   <li>the membership details — role, status, validity window, notes,
 *       sources, evidence and audit of the relationship itself;</li>
 *   <li>the group's attributes ({@code residence}, {@code member_count},
 *       {@code social_class}, {@code wealth}, {@code religion}, ...);</li>
 *   <li>parent and child groups ({@code part_of} relationships), with validity
 *       and provenance;</li>
 *   <li>events the group participates in;</li>
 *   <li>notes, sources, evidence and audit of the group record itself.</li>
 * </ul>
 *
 * <p>The section is emitted only when {@code config.groups()} is enabled and
 * at least one membership is reachable.</p>
 */
final class GroupsSection implements SectionBuilder{

	/* ======================================================================
	 *                          Tags
	 * ====================================================================== */

	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_TYPE = "type";
	private static final String TAG_ROLE = "role";
	private static final String TAG_STATUS = "status";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_TARGET = "target";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_AGENCY = "agency";

	private static final String NAME_TYPE_OFFICIAL = "official";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	GroupsSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	@Override
	public List<ReportSection> build(){
		if(!ctx.config.groups())
			return List.of();

		final Map<String, MembershipEntry> entries = collectMemberships();
		if(entries.isEmpty())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.sections().groups()));
		for(final MembershipEntry entry : entries.values())
			appendGroup(out, entry);
		return out;
	}


	/* ======================================================================
	 *                          Collection
	 * ====================================================================== */

	private Map<String, MembershipEntry> collectMemberships(){
		final Map<String, MembershipEntry> out = new LinkedHashMap<>();

		for(final FLEFRecord membership : ctx.index.groupMembershipsOf(ctx.root)){
			final String groupId = memberGroupId(membership);
			if(groupId == null)
				continue;
			final FLEFRecord group = ctx.visible(ctx.model.getRecordById(groupId));
			if(group == null)
				continue;
			out.computeIfAbsent(groupId, k -> new MembershipEntry(group))
				.memberships.add(membership);
		}
		return out;
	}

	/** Extracts the referenced group ID from the {@code target} branch of a {@code group_member}. */
	private static String memberGroupId(final FLEFRecord rel){
		final String direct = FLEFRecordHelper.getChildValue(rel, "target.group");
		if(direct != null)
			return direct;
		final FLEFRecord targetNode = FLEFRecordHelper.findChild(rel, TAG_TARGET);
		if(targetNode == null)
			return null;
		final FLEFRecord ref = targetNode.getTheOnlyChild();
		if(ref == null)
			return null;
		return ("group".equalsIgnoreCase(ref.getTag())? ref.getValue(): null);
	}


	/* ======================================================================
	 *                          Per-group rendering
	 * ====================================================================== */

	private void appendGroup(final List<ReportSection> out, final MembershipEntry entry){
		final FLEFRecord group = entry.group();

		out.add(new ReportSection.Heading(2, nameOf(group)));

		appendBasicInfo(out, group);
		appendMemberships(out, entry);
		appendGroupAttributes(out, group);
		appendParentGroups(out, group);
		appendChildGroups(out, group);
		appendGroupEvents(out, group);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(group));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(group));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(group, out);
		out.addAll(ctx.citations().audit(group));
	}


	private void appendBasicInfo(final List<ReportSection> out, final FLEFRecord group){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().groupType(),
			FLEFRecordHelper.getChildValue(group, TAG_TYPE));

		// Additional names (all but the primary heading).
		final FLEFRecord primary = primaryNameNode(group);
		for(final FLEFRecord n : FLEFRecordHelper.findChildren(group, TAG_NAME)){
			if(n == primary)
				continue;
			final String type = FLEFRecordHelper.getChildValue(n, NameReader.TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
			if(value == null || value.isBlank())
				continue;
			final StringBuilder line = new StringBuilder();
			line.append("**").append(ctx.labels.sections().name());
			if(type != null && !type.isBlank())
				line.append(" (").append(ReportFormatters.escape(type)).append(")");
			line.append(":** ").append(ReportFormatters.escape(value));
			rows.add(line.toString());
		}

		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Memberships ----------------------------------------------------- */

	private void appendMemberships(final List<ReportSection> out, final MembershipEntry entry){
		out.add(new ReportSection.Heading(3, ctx.labels.sections().groupMemberships()));

		for(final FLEFRecord membership : entry.memberships()){
			final List<String> meta = new ArrayList<>();
			ReportFormatters.appendIfPresent(meta, ctx.labels.sections().role(),
				FLEFRecordHelper.getChildValue(membership, TAG_ROLE));
			ReportFormatters.appendIfPresent(meta, ctx.labels.sections().status(),
				ReportFormatters.enumLabel(
					FLEFRecordHelper.getChildValue(membership, TAG_STATUS)));

			final String from = GenealogicalDateHelper.formatDateStructure(
				membership, TAG_VALID_FROM, ctx.labels, contextLabels);
			if(from != null)
				meta.add("**" + ctx.labels.sections().validFrom() + ":** " + ReportFormatters.escape(from));
			final String to = GenealogicalDateHelper.formatDateStructure(
				membership, TAG_VALID_TO, ctx.labels, contextLabels);
			if(to != null)
				meta.add("**" + ctx.labels.sections().validTo() + ":** " + ReportFormatters.escape(to));

			if(!meta.isEmpty())
				out.add(new ReportSection.BulletList(meta));

			if(ctx.config.notes())
				out.addAll(ctx.citations().notes(membership));
			if(ctx.config.sources())
				out.addAll(ctx.citations().citations(membership));
			if(ctx.config.evidence())
				ctx.citations().addEvidence(membership, out);
			out.addAll(ctx.citations().audit(membership));
		}
	}


	/* ----- Attributes ------------------------------------------------------ */

	private void appendGroupAttributes(final List<ReportSection> out, final FLEFRecord group){
		final List<FLEFRecord> attrs = ctx.index.attributesOfGroup(group);
		if(attrs.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.sections().groupAttributes()));
		final List<List<String>> rows = new ArrayList<>();
		for(final FLEFRecord a : attrs){
			rows.add(List.of(
				ReportFormatters.esc(FLEFRecordHelper.getChildValue(a, TAG_TYPE)),
				ReportFormatters.esc(FLEFRecordHelper.getChildValue(a, TAG_VALUE)),
				ReportFormatters.esc(GenealogicalDateHelper.formatDateStructure(
					a, TAG_VALID_FROM, ctx.labels, contextLabels)),
				ReportFormatters.esc(GenealogicalDateHelper.formatDateStructure(
					a, TAG_VALID_TO, ctx.labels, contextLabels)),
				ReportFormatters.esc(ReportFormatters.resolvePlaceName(ctx.model, a))));
		}
		out.add(new ReportSection.Table(
			List.of(ctx.labels.sections().type(), ctx.labels.sections().value(),
				ctx.labels.sections().from(), ctx.labels.sections().to(), ctx.labels.sections().place()),
			rows));

		if(ctx.config.notes() || ctx.config.sources() || ctx.config.evidence() || ctx.config.audit()){
			for(final FLEFRecord a : attrs){
				final List<ReportSection> extras = new ArrayList<>();
				if(ctx.config.notes())
					extras.addAll(ctx.citations().notes(a));
				if(ctx.config.sources())
					extras.addAll(ctx.citations().citations(a));
				if(ctx.config.evidence())
					ctx.citations().addEvidence(a, extras);
				extras.addAll(ctx.citations().audit(a));
				if(!extras.isEmpty()){
					out.add(new ReportSection.Heading(4, ctx.labels.sections().type() + ": "
						+ ReportFormatters.escape(ReportFormatters.orEmpty(
						FLEFRecordHelper.getChildValue(a, TAG_TYPE)))));
					out.addAll(extras);
				}
			}
		}
	}


	/* ----- Part_of hierarchy ---------------------------------------------- */

	private void appendParentGroups(final List<ReportSection> out, final FLEFRecord group){
		final List<FLEFRecord> rels = ctx.index.parentGroupsOf(group);
		if(rels.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.sections().groupParentGroups()));
		for(final FLEFRecord rel : rels)
			appendGroupRelation(out, rel, false);
	}

	private void appendChildGroups(final List<ReportSection> out, final FLEFRecord group){
		final List<FLEFRecord> rels = ctx.index.childGroupsOf(group);
		if(rels.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.sections().groupChildGroups()));
		for(final FLEFRecord rel : rels)
			appendGroupRelation(out, rel, true);
	}

	/**
	 * Renders a single {@code part_of} relationship. When {@code useSubject}
	 * is {@code true}, the subject group is the relevant one (child groups
	 * of a parent); otherwise the target group is the relevant one (parent
	 * groups of a child).
	 */
	private void appendGroupRelation(final List<ReportSection> out, final FLEFRecord rel,
		final boolean useSubject){
		final String counterpartId = (useSubject
			? FLEFRecordHelper.getChildValue(rel, "subject.group")
			: FLEFRecordHelper.getChildValue(rel, "target.group"));
		final FLEFRecord counterpart = (counterpartId != null
			? ctx.visible(ctx.model.getRecordById(counterpartId)): null);
		final String label = (counterpart != null? nameOf(counterpart): counterpartId);
		if(label == null || label.isBlank())
			return;

		out.add(new ReportSection.Heading(4, ReportFormatters.escape(label)));

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

	private void appendGroupEvents(final List<ReportSection> out, final FLEFRecord group){
		final List<FLEFRecord> events = ctx.index.eventsOfGroup(group);
		if(events.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.sections().groupEvents()));
		for(final FLEFRecord evt : events){
			final String type = ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(evt, TAG_TYPE));
			final String date = ReportFormatters.orEmpty(
				GenealogicalDateHelper.formatEventDate(evt, ctx.labels, contextLabels));
			final String heading = (type.isEmpty()? "Event": type)
				+ (date.isEmpty()? StringUtils.EMPTY: " — " + date);
			out.add(new ReportSection.Heading(4, ReportFormatters.escape(heading)));

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
		}
	}


	/* ======================================================================
	 *                          Name helpers
	 * ====================================================================== */

	private FLEFRecord primaryNameNode(final FLEFRecord group){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(group, TAG_NAME);
		if(names.isEmpty())
			return null;
		for(final FLEFRecord n : names){
			final String type = FLEFRecordHelper.getChildValue(n, NameReader.TAG_TYPE);
			if(NAME_TYPE_OFFICIAL.equalsIgnoreCase(type)){
				final String v = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
				if(v != null && !v.isBlank())
					return n;
			}
		}
		for(final FLEFRecord n : names){
			final String v = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
			if(v != null && !v.isBlank())
				return n;
		}
		return null;
	}

	private String nameOf(final FLEFRecord group){
		final FLEFRecord primary = primaryNameNode(group);
		if(primary != null){
			final String v = FLEFRecordHelper.getChildValue(primary, TAG_VALUE);
			if(v != null && !v.isBlank())
				return ReportFormatters.escape(v.trim());
		}
		return ReportFormatters.escape(ReportFormatters.orEmpty(group.getId()));
	}


	/* ======================================================================
	 *                          Data holders
	 * ====================================================================== */

	private static final class MembershipEntry{
		final FLEFRecord group;
		final List<FLEFRecord> memberships = new ArrayList<>();

		MembershipEntry(final FLEFRecord group){
			this.group = group;
		}

		FLEFRecord group(){
			return group;
		}

		List<FLEFRecord> memberships(){
			return memberships;
		}
	}

}
