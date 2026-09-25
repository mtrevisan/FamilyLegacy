package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Builds the main section for a place report (root = {@code place} record).
 *
 * <p>Renders:</p>
 * <ul>
 *   <li>primary and secondary names, with type, locale and variants;</li>
 *   <li>place type and geographic coordinates;</li>
 *   <li>historical jurisdictions (both directions of {@code place_relationship});</li>
 *   <li>events that happened at the place;</li>
 *   <li>events in which the place is a participant;</li>
 *   <li>attributes recorded at the place;</li>
 *   <li>notes, sources, evidence and audit of the place itself.</li>
 * </ul>
 */
final class PlaceRootSection implements SectionBuilder{

	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_TYPE = "type";
	private static final String TAG_LOCALE = "locale";
	private static final String TAG_VARIANT = "variant";
	private static final String TAG_MAP = "map";
	private static final String TAG_COORDINATES = "coordinates";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_AGENCY = "agency";
	private static final String TAG_PLACE = "place";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	PlaceRootSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isPlaceRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.placeOf(),
			ReportFormatters.escape(primaryName(ctx.root)))));

		writeBasicInfo(out);
		writeNames(out);
		writeJurisdictions(out);
		writeEventsAtPlace(out);
		writeEventsByPlace(out);
		writeAttributesAtPlace(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(ctx.root, out);
		out.addAll(ctx.citations().audit(ctx.root));
		out.addAll(ctx.citations().privacyInfo(ctx.root));
		return out;
	}


	/* ----- Basic info ------------------------------------------------------ */

	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.placeType(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.placeCoordinates(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_MAP + "." + TAG_COORDINATES));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Names ----------------------------------------------------------- */

	private void writeNames(final List<ReportSection> out){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(ctx.root, TAG_NAME);
		if(names.isEmpty())
			return;

		final FLEFRecord primary = primaryNameNode(ctx.root);
		final List<String> rows = new ArrayList<>();
		for(final FLEFRecord n : names){
			final String type = FLEFRecordHelper.getChildValue(n, TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			final String locale = FLEFRecordHelper.getChildValue(n, TAG_LOCALE);
			if(value == null || value.isBlank())
				continue;
			if(n == primary && type == null && locale == null)
				continue;

			final StringBuilder line = new StringBuilder();
			line.append("**").append(ctx.labels.name());
			if(type != null && !type.isBlank())
				line.append(" (").append(ReportFormatters.escape(type)).append(")");
			line.append(":** ").append(ReportFormatters.escape(value));
			if(locale != null && !locale.isBlank())
				line.append(" — *").append(ReportFormatters.escape(locale)).append('*');
			rows.add(line.toString());

			for(final FLEFRecord variant : FLEFRecordHelper.findChildren(n, TAG_VARIANT)){
				final String v = ReportFormatters.renderNameVariant(variant);
				if(v != null)
					rows.add("  *" + ctx.labels.placeNameVariant() + ":* "
						+ ReportFormatters.escape(v));
			}
		}
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Jurisdictions --------------------------------------------------- */

	private void writeJurisdictions(final List<ReportSection> out){
		final List<FLEFRecord> parents = ctx.index.placeRelationshipsAsSubject(ctx.root);
		final List<FLEFRecord> children = ctx.index.placeRelationshipsAsTarget(ctx.root);
		if(parents.isEmpty() && children.isEmpty())
			return;

		if(!parents.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.placeJurisdictions()));
			for(final FLEFRecord rel : parents)
				writeRelation(out, rel, false, 3);
		}
		if(!children.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.placeContainedPlaces()));
			for(final FLEFRecord rel : children)
				writeRelation(out, rel, true, 3);
		}
	}

	private void writeRelation(final List<ReportSection> out, final FLEFRecord rel,
		final boolean useSubject, final int level){
		final String counterpartId = (useSubject
			? FLEFRecordHelper.getChildValue(rel, "subject.place")
			: FLEFRecordHelper.getChildValue(rel, "target.place"));
		final FLEFRecord counterpart = (counterpartId != null
			? ctx.visible(ctx.model.getRecordById(counterpartId)): null);
		final String label = (counterpart != null? primaryName(counterpart): counterpartId);
		if(label == null || label.isBlank())
			return;

		final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
		final String relLabel = ReportFormatters.orEmpty(ReportFormatters.enumLabel(type));
		out.add(new ReportSection.Heading(level,
			ReportFormatters.escape(relLabel) + " " + ReportFormatters.escape(label)));

		final String from = GenealogicalDateHelper.formatDateStructure(
			rel, TAG_VALID_FROM, ctx.labels, contextLabels);
		final String to = GenealogicalDateHelper.formatDateStructure(
			rel, TAG_VALID_TO, ctx.labels, contextLabels);
		final List<String> rows = new ArrayList<>();
		if(from != null)
			rows.add("**" + ctx.labels.validFrom() + ":** " + ReportFormatters.escape(from));
		if(to != null)
			rows.add("**" + ctx.labels.validTo() + ":** " + ReportFormatters.escape(to));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(rel));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(rel));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(rel, out);
		out.addAll(ctx.citations().audit(rel));
	}


	/* ----- Events at place ------------------------------------------------- */

	private void writeEventsAtPlace(final List<ReportSection> out){
		final List<FLEFRecord> events = ctx.index.eventsAtPlace(ctx.root);
		if(events.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.placeEvents()));
		for(final FLEFRecord evt : events)
			writeEvent(out, evt);
	}

	private void writeEventsByPlace(final List<ReportSection> out){
		final List<FLEFRecord> events = ctx.index.eventsByPlace(ctx.root);
		if(events.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.placeEventParticipations()));
		for(final FLEFRecord evt : events)
			writeEvent(out, evt);
	}

	private void writeEvent(final List<ReportSection> out, final FLEFRecord evt){
		final String type = ReportFormatters.escape(ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(evt, TAG_TYPE)));
		final String date = ReportFormatters.escape(ReportFormatters.orEmpty(
			GenealogicalDateHelper.formatEventDate(evt, ctx.labels, contextLabels)));
		out.add(new ReportSection.Heading(3,
			(type.isEmpty()? "Event": type) + (date.isEmpty()? "": " — " + date)));

		final String place = FLEFRecordHelper.extractPlace(evt, ctx.model);
		if(place != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.place() + ":** " + ReportFormatters.escape(place)));

		final String agency = FLEFRecordHelper.getChildValue(evt, TAG_AGENCY);
		if(agency != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.agency() + ":** " + ReportFormatters.escape(agency)));

		final String descr = FLEFRecordHelper.getChildValue(evt, TAG_DESCRIPTION);
		if(descr != null)
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(descr)));

		// Participants (may include individuals, groups or other places)
		writeParticipants(out, evt);
	}

	private void writeParticipants(final List<ReportSection> out, final FLEFRecord evt){
		final List<RelationIndex.Participant> participants = ctx.index.participantsOf(evt);
		if(participants.isEmpty())
			return;
		out.add(new ReportSection.Heading(4, ctx.labels.eventParticipants()));
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
	}


	/* ----- Attributes at place -------------------------------------------- */

	private void writeAttributesAtPlace(final List<ReportSection> out){
		final List<FLEFRecord> attrs = ctx.index.attributesAtPlace(ctx.root);
		if(attrs.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.placeAttributes()));
		for(final FLEFRecord a : attrs){
			final String type = ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(a, TAG_TYPE));
			final String value = ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(a, TAG_VALUE));
			out.add(new ReportSection.Paragraph(
				"**" + ReportFormatters.escape(type) + ":** "
					+ ReportFormatters.escape(value)));
		}
	}


	/* ----- Name helpers ---------------------------------------------------- */

	private static FLEFRecord primaryNameNode(final FLEFRecord place){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(place, TAG_NAME);
		if(names.isEmpty()) return null;
		for(final FLEFRecord n : names){
			final String type = FLEFRecordHelper.getChildValue(n, TAG_TYPE);
			if("official".equalsIgnoreCase(type)){
				final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
				if(v != null && !v.isBlank()) return n;
			}
		}
		for(final FLEFRecord n : names){
			final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			if(v != null && !v.isBlank()) return n;
		}
		return null;
	}

	private static String primaryName(final FLEFRecord place){
		final FLEFRecord n = primaryNameNode(place);
		if(n != null){
			final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			if(v != null && !v.isBlank()) return v.trim();
		}
		return ReportFormatters.orEmpty(place.getId());
	}

}
