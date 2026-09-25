package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;


/**
 * Builds the life story of the root individual: narrative, preferred image,
 * personal data, events, attributes, relationships, notes, cited sources
 * and audit trail.
 *
 * <p>Cultural norms referenced by names and approximate dates are inlined
 * next to the value they qualify, so the reader sees the reasoning behind
 * each assertion.</p>
 */
final class LifeStorySection implements SectionBuilder{

	private static final String TAG_SEX = "sex";
	private static final String TAG_TYPE = "type";
	private static final String TAG_NAME = "name";
	private static final String TAG_CULTURAL_NORM = "cultural_norm";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_CAUSE = "cause";
	private static final String TAG_REASON = "reason";
	private static final String TAG_AGENCY = "agency";
	private static final String TAG_VALUE = "value";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_STATUS = "status";
	private static final String TAG_ROLE = "role";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_URI = "uri";

	private static final String TYPE_EVENT_PARTICIPATION = "event_participation";
	private static final String TYPE_RELATIONSHIP = "relationship";
	private static final String TYPE_INDIVIDUAL = "individual";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	LifeStorySection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		final List<ReportSection> out = new ArrayList<>();
		final String who = ReportFormatters.escape(ctx.displayText(ctx.root));

		out.add(new ReportSection.Heading(1, String.format(ctx.labels.lifeOf(), who)));

		writeNarrative(out);
		writePreferredImage(out);
		writePersonalData(out);
		writeEvents(out);
		writeAttributes(out);
		writeRelationships(out);
		writeNotes(out);
		writeCitedSourceTitles(out);
		writeAudit(out);

		return out;
	}


	/* ----- Narrative ------------------------------------------------------- */

	private void writeNarrative(final List<ReportSection> out){
		final String narrative = new LifeNarrator(ctx.model, ctx.index, ctx.labels)
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
			preferred, ctx.labels.preferredImage(), ctx.labels);
		try{
			out.add(new ReportSection.Image(Path.of(uri), caption));
		}
		catch(final Exception ignored){
			out.add(new ReportSection.Paragraph(
				"[" + caption + ": " + uri + "]"));
		}
	}


	/* ----- Personal data --------------------------------------------------- */

	private void writePersonalData(final List<ReportSection> out){
		out.add(new ReportSection.Heading(2, ctx.labels.personalData()));
		final List<String> personal = new ArrayList<>();
		personal.add("**" + ctx.labels.id() + ":** "
			+ ReportFormatters.escape(ReportFormatters.orEmpty(ctx.root.getId())));
		personal.add("**" + ctx.labels.sex() + ":** " + ReportFormatters.escape(
			Optional.ofNullable(FLEFRecordHelper.getChildValue(ctx.root, TAG_SEX))
				.orElse(ctx.labels.sexUnknown())));

		for(final FLEFRecord nameRec : FLEFRecordHelper.findChildren(ctx.root, TAG_NAME)){
			final String nameType = FLEFRecordHelper.getChildValue(nameRec, TAG_TYPE);
			final String n = ReportFormatters.buildName(nameRec);
			if(n.isBlank())
				continue;

			final StringBuilder line = new StringBuilder();
			line.append("**").append(ctx.labels.name());
			if(nameType != null)
				line.append(" (").append(ReportFormatters.escape(nameType)).append(")");
			line.append(":** ").append(ReportFormatters.escape(n));

			// Cultural norms attached to the name.
			final List<String> norms = nameNormTitles(nameRec);
			if(!norms.isEmpty())
				line.append(" — *").append(ctx.labels.nameCulturalNorm())
					.append(":* ")
					.append(ReportFormatters.escape(String.join(", ", norms)));

			personal.add(line.toString());


			// Phonetic and transcription variants of each part.
			for(final String variant : partVariants(nameRec))
				personal.add("  " + variant);
		}
		out.add(new ReportSection.BulletList(personal));

		// Provenance of each name (item 3): sources and notes attached to the
		// name structure itself.
		if(ctx.config.notes() || ctx.config.sources()){
			for(final FLEFRecord nameRec : FLEFRecordHelper.findChildren(ctx.root, TAG_NAME)){
				final List<ReportSection> provenance = new ArrayList<>();
				if(ctx.config.notes())
					provenance.addAll(ctx.citations().notes(nameRec));
				if(ctx.config.sources())
					provenance.addAll(ctx.citations().citations(nameRec));
				if(provenance.isEmpty())
					continue;
				final String nameType = FLEFRecordHelper.getChildValue(nameRec, TAG_TYPE);
				out.add(new ReportSection.Heading(3,
					ctx.labels.name() + (nameType != null? " (" + nameType + ")": "")));
				out.addAll(provenance);
			}
		}
	}

	/**
	 * Extracts every {@code variant} attached to a {@code part} of the given
	 * {@code name} structure, formatted as {@code *<part type>:* v1, v2, ...}.
	 */
	private List<String> partVariants(final FLEFRecord nameRec){
		final List<String> out = new ArrayList<>();
		for(final FLEFRecord part : nameRec.getChildren()){
			if(!"part".equalsIgnoreCase(part.getTag()))
				continue;
			final List<String> vs = new ArrayList<>();
			for(final FLEFRecord variant : FLEFRecordHelper.findChildren(part, "variant")){
				final String v = ReportFormatters.renderNameVariant(variant);
				if(v != null)
					vs.add(v);
			}
			if(vs.isEmpty())
				continue;
			final String partType = FLEFRecordHelper.getChildValue(part, "type");
			final StringBuilder sb = new StringBuilder("  *");
			sb.append(ReportFormatters.escape(ReportFormatters.orEmpty(partType)))
				.append(":* ").append(ReportFormatters.escape(String.join(", ", vs)));
			out.add(sb.toString());
		}
		return out;
	}

	/**
	 * Resolves the titles of every {@code cultural_norm} reference directly
	 * attached to a {@code name} structure. Order is preserved.
	 */
	private List<String> nameNormTitles(final FLEFRecord nameRec){
		final List<String> out = new ArrayList<>();
		for(final FLEFRecord cn : FLEFRecordHelper.findChildren(nameRec, TAG_CULTURAL_NORM)){
			final String id = cn.getValue();
			if(id == null || id.isBlank())
				continue;
			final String label = ctx.resolveContextLabel(id);
			if(label != null && !label.isBlank())
				out.add(label);
		}
		return out;
	}


	/* ----- Events ---------------------------------------------------------- */

	private void writeEvents(final List<ReportSection> out){
		final List<FLEFRecord> events = ctx.index.eventsOf(ctx.root);
		out.add(new ReportSection.Heading(2, ctx.labels.lifeEvents()));
		if(events.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.empty()));
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

		String cause = FLEFRecordHelper.getChildValue(evt, TAG_CAUSE + "." + TAG_REASON);
		if(cause == null)
			cause = FLEFRecordHelper.getChildValue(evt, TAG_CAUSE);
		if(cause != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.cause() + ":** " + ReportFormatters.escape(cause)));

		final String role = roleOfInEvent(ctx.root, evt);
		if(role != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.role() + ":** " + ReportFormatters.escape(role)));

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(evt));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(evt));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(evt, out);
		out.addAll(ctx.citations().audit(evt));
		if(ctx.config.eventFullParticipants())
			appendOtherParticipants(out, evt);
	}


	/**
	 * Renders the other participants of the event, together with their role.
	 * Only emitted when {@code eventFullParticipants} is enabled. The root
	 * individual itself is excluded from the list.
	 */
	private void appendOtherParticipants(final List<ReportSection> out, final FLEFRecord evt){
		final List<RelationIndex.Participant> participants = ctx.index.participantsOf(evt);
		if(participants.isEmpty())
			return;
		final List<String> items = new ArrayList<>();
		for(final RelationIndex.Participant p : participants){
			if(Objects.equals(p.record().getId(), ctx.root.getId()))
				continue;
			final String role = (p.role() != null && !p.role().isBlank()
				? " — *" + ReportFormatters.escape(p.role()) + "*": "");
			items.add(ReportFormatters.escape(ctx.displayText(p.record()))
				+ " [" + ReportFormatters.escape(ReportFormatters.orEmpty(
				ReportFormatters.enumLabel(p.kind()))) + "]" + role);
		}
		if(!items.isEmpty()){
			out.add(new ReportSection.Heading(4, ctx.labels.eventOtherParticipants()));
			out.add(new ReportSection.BulletList(items));

			// Participation details (source, note, evidence, audit).
			for(final RelationIndex.Participant p : participants){
				if(Objects.equals(p.record().getId(), ctx.root.getId()))
					continue;
				final List<ReportSection> details =
					ctx.citations().participationDetails(p.participation());
				if(!details.isEmpty()){
					out.add(new ReportSection.Heading(5,
						ctx.displayText(p.record())));
					out.addAll(details);
				}
			}
		}
	}

	private String roleOfInEvent(final FLEFRecord individual, final FLEFRecord event){
		final String evtId = event.getId();
		if(evtId == null)
			return null;
		for(final FLEFRecord ep : ctx.visibleRecordsByType(TYPE_EVENT_PARTICIPATION)){
			final String eid = FLEFRecordHelper.getChildValue(ep, TAG_EVENT);
			if(!Objects.equals(evtId, eid))
				continue;
			final FLEFRecord pf = FLEFRecordHelper.findChild(ep, TAG_PARTICIPANT);
			if(pf == null)
				continue;
			final FLEFRecord ref = pf.getTheOnlyChild();
			if(ref == null || !TYPE_INDIVIDUAL.equalsIgnoreCase(ref.getTag()))
				continue;
			if(!Objects.equals(individual.getId(), ref.getValue()))
				continue;
			return FLEFRecordHelper.getChildValue(ep, TAG_ROLE);
		}
		return null;
	}


	/* ----- Attributes ------------------------------------------------------ */

	private void writeAttributes(final List<ReportSection> out){
		final List<FLEFRecord> attrs = ctx.index.attributesOf(ctx.root);
		if(attrs.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.attributes()));

		final List<List<String>> rows = new ArrayList<>();
		for(final FLEFRecord a : attrs)
			rows.add(List.of(
				ReportFormatters.esc(FLEFRecordHelper.getChildValue(a, TAG_TYPE)),
				ReportFormatters.esc(FLEFRecordHelper.getChildValue(a, TAG_VALUE)),
				ReportFormatters.esc(GenealogicalDateHelper.formatDateStructure(
					a, TAG_VALID_FROM, ctx.labels, contextLabels)),
				ReportFormatters.esc(GenealogicalDateHelper.formatDateStructure(
					a, TAG_VALID_TO, ctx.labels, contextLabels)),
				ReportFormatters.esc(ReportFormatters.resolvePlaceName(ctx.model, a))));

		out.add(new ReportSection.Table(
			List.of(ctx.labels.type(), ctx.labels.value(),
				ctx.labels.from(), ctx.labels.to(), ctx.labels.place()),
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
					out.add(new ReportSection.Heading(3,
						ctx.labels.type() + ": " + ReportFormatters.escape(
							ReportFormatters.orEmpty(
								FLEFRecordHelper.getChildValue(a, TAG_TYPE)))));
					out.addAll(extras);
				}
			}
		}
	}


	/* ----- Relationships --------------------------------------------------- */

	private void writeRelationships(final List<ReportSection> out){
		out.add(new ReportSection.Heading(2, ctx.labels.relationships()));

		final List<FLEFRecord> declared = new ArrayList<>();
		for(final FLEFRecord rel : ctx.visibleRecordsByType(TYPE_RELATIONSHIP)){
			final String subj = rel.extractReferencedId(TAG_SUBJECT, TYPE_INDIVIDUAL);
			if(Objects.equals(ctx.root.getId(), subj))
				declared.add(rel);
		}
		if(declared.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.empty()));
			return;
		}

		for(final FLEFRecord rel : declared)
			writeRelationship(out, rel);

		// Group memberships of the individual.
		final List<FLEFRecord> memberships = ctx.index.groupMembershipsOf(ctx.root);
		if(!memberships.isEmpty()){
			out.add(new ReportSection.Heading(3, ctx.labels.groupMemberships()));
			final List<String> items = new ArrayList<>();
			for(final FLEFRecord m : memberships){
				final String groupId = memberGroupId(m);
				final FLEFRecord group = (groupId != null
					? ctx.visible(ctx.model.getRecordById(groupId)): null);
				if(group == null)
					continue;
				final String name = ctx.displayText(group);
				final String role = FLEFRecordHelper.getChildValue(m, TAG_ROLE);
				items.add(ReportFormatters.escape(name)
					+ (role != null && !role.isBlank()
					? " — *" + ReportFormatters.escape(role) + "*": ""));
			}
			if(!items.isEmpty())
				out.add(new ReportSection.BulletList(items));
		}
	}

	/** Extracts the referenced group ID from the {@code target} branch of a {@code group_member}. */
	private static String memberGroupId(final FLEFRecord rel){
		final String direct = FLEFRecordHelper.getChildValue(rel, "target.group");
		if(direct != null)
			return direct;
		final FLEFRecord targetNode = FLEFRecordHelper.findChild(rel, "target");
		if(targetNode == null)
			return null;
		final FLEFRecord ref = targetNode.getTheOnlyChild();
		if(ref == null)
			return null;
		return ("group".equalsIgnoreCase(ref.getTag())? ref.getValue(): null);
	}


	private void writeRelationship(final List<ReportSection> out, final FLEFRecord rel){
		final String relType = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(rel, TAG_TYPE));
		final String role = FLEFRecordHelper.getChildValue(rel, TAG_ROLE);
		final String status = FLEFRecordHelper.getChildValue(rel, TAG_STATUS);
		final String from = GenealogicalDateHelper.formatDateStructure(
			rel, TAG_VALID_FROM, ctx.labels, contextLabels);
		final String to = GenealogicalDateHelper.formatDateStructure(
			rel, TAG_VALID_TO, ctx.labels, contextLabels);
		final String targId = rel.extractReferencedId(TAG_TARGET, TYPE_INDIVIDUAL);
		final FLEFRecord target = (targId != null? ctx.model.getRecordById(targId): null);
		final String targetLabel = (target != null? ctx.displayText(target)
			: ReportFormatters.orEmpty(targId));

		out.add(new ReportSection.Heading(3,
			ReportFormatters.escape(relType)
				+ (role != null? " / " + ReportFormatters.escape(role): "")
				+ ": " + ReportFormatters.escape(targetLabel)));

		final List<String> meta = new ArrayList<>();
		if(status != null)
			meta.add("**" + ctx.labels.relationshipStatus() + ":** "
				+ ReportFormatters.escape(status));
		if(from != null)
			meta.add("**" + ctx.labels.validFrom() + ":** " + ReportFormatters.escape(from));
		if(to != null)
			meta.add("**" + ctx.labels.validTo() + ":** " + ReportFormatters.escape(to));
		if(target != null && ctx.isVisible(target)){
			final String kinTerm = ctx.kinship().shortTerm(target.getId(), ctx.root.getId());
			if(ReportFormatters.isUsefulKinshipTerm(kinTerm))
				meta.add("*" + ReportFormatters.escape(kinTerm) + "*");
		}
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
		final List<String> ids = new ArrayList<>(SourceCollector.collect(ctx));
		if(ids.isEmpty())
			return;
		out.add(new ReportSection.Heading(2, ctx.labels.citations()));
		out.add(new ReportSection.BulletList(ids.stream()
			.map(ctx::sourceTitle)
			.toList()));
	}


	private void writeAudit(final List<ReportSection> out){
		final List<ReportSection> audit = ctx.citations().audit(ctx.root);
		if(!audit.isEmpty()){
			out.add(new ReportSection.Heading(2, ctx.labels.audit()));
			out.addAll(audit);
		}
	}

}
