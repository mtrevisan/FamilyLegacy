package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


/**
 * Builds the Places section.
 *
 * <p>For every place touched by the root individual (via events or
 * attributes), the section renders:</p>
 * <ul>
 *   <li>the primary name (preferring an {@code official} variant when
 *       present);</li>
 *   <li>additional names with their type and locale, plus phonetic and
 *       transcription variants;</li>
 *   <li>the place type and geographic coordinates;</li>
 *   <li>every {@code place_relationship} in which the place is the
 *       {@code subject} (administrative, geographic, ecclesiastical,
 *       judicial, cadastral jurisdiction), with validity window, sources,
 *       notes and evidence;</li>
 *   <li>every citation of the place found in the individual's events and
 *       attributes, with the source wording and the citing record;</li>
 *   <li>notes, sources, evidence and audit of the place record itself.</li>
 * </ul>
 */
final class PlacesSection implements SectionBuilder{

	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_TYPE = "type";
	private static final String TAG_LOCALE = "locale";
	private static final String TAG_VARIANT = "variant";
	private static final String TAG_PLACE = "place";
	private static final String TAG_MAP = "map";
	private static final String TAG_COORDINATES = "coordinates";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_ORIGINAL = "original_text";

	private static final String NAME_TYPE_OFFICIAL = "official";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	PlacesSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	@Override
	public List<ReportSection> build(){
		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.places()));

		final Map<String, PlaceEntry> entries = collectPlaceEntries();
		if(entries.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.empty()));
			return out;
		}

		for(final PlaceEntry entry : entries.values())
			appendPlace(out, entry);
		return out;
	}


	/* ======================================================================
	 *                          Collection
	 * ====================================================================== */

	private Map<String, PlaceEntry> collectPlaceEntries(){
		final Map<String, PlaceEntry> out = new LinkedHashMap<>();
		for(final FLEFRecord evt : ctx.index.eventsOf(ctx.root))
			collectPlace(evt, out);
		for(final FLEFRecord attr : ctx.index.attributesOf(ctx.root))
			collectPlace(attr, out);
		return out;
	}

	private void collectPlace(final FLEFRecord rec, final Map<String, PlaceEntry> out){
		final FLEFRecord citation = FLEFRecordHelper.findChild(rec, TAG_PLACE);
		if(citation == null)
			return;

		final String pid = FLEFRecordHelper.getChildValue(citation, TAG_PLACE);
		if(pid == null)
			return;

		final FLEFRecord place = ctx.visible(ctx.model.getRecordById(pid));
		if(place == null)
			return;

		out.computeIfAbsent(pid, k -> new PlaceEntry(place))
			.citations.add(new CitationRef(citation, rec));
	}


	/* ======================================================================
	 *                          Per-place rendering
	 * ====================================================================== */

	private void appendPlace(final List<ReportSection> out, final PlaceEntry entry){
		final FLEFRecord place = entry.place();
		final FLEFRecord primary = primaryNameNode(place);

		out.add(new ReportSection.Heading(2,
			ReportFormatters.escape(nameValue(primary, place))));

		appendBasicInfo(out, place);
		appendNames(out, place, primary);
		appendJurisdictions(out, place);
		appendContainedPlaces(out, place);
		appendCitations(out, entry);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(place));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(place));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(place, out);
		out.addAll(ctx.citations().audit(place));
	}


	private void appendBasicInfo(final List<ReportSection> out, final FLEFRecord place){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.placeType(),
			FLEFRecordHelper.getChildValue(place, TAG_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.placeCoordinates(),
			FLEFRecordHelper.getChildValue(place, TAG_MAP + "." + TAG_COORDINATES));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/**
	 * Renders every {@code name} except the one that already serves as the
	 * section heading. The comparison is by node identity, so it works
	 * regardless of the position of the chosen primary name in the list.
	 */
	private void appendNames(final List<ReportSection> out, final FLEFRecord place,
		final FLEFRecord primary){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(place, TAG_NAME);
		final List<String> rows = new ArrayList<>();

		for(final FLEFRecord nameNode : names){
			final String type = FLEFRecordHelper.getChildValue(nameNode, TAG_TYPE);
			final String value = FLEFRecordHelper.getChildValue(nameNode, TAG_VALUE);
			final String locale = FLEFRecordHelper.getChildValue(nameNode, TAG_LOCALE);

			if(nameNode != primary){
				final StringBuilder line = new StringBuilder();
				line.append("**").append(ctx.labels.name());
				if(type != null && !type.isBlank())
					line.append(" (").append(ReportFormatters.escape(type)).append(")");
				line.append(":** ").append(ReportFormatters.escape(ReportFormatters.orEmpty(value)));
				if(locale != null && !locale.isBlank())
					line.append(" — *").append(ReportFormatters.escape(locale)).append('*');
				rows.add(line.toString());
			}

			for(final FLEFRecord variant : FLEFRecordHelper.findChildren(nameNode, TAG_VARIANT)){
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

	private void appendJurisdictions(final List<ReportSection> out, final FLEFRecord place){
		final List<FLEFRecord> rels = ctx.index.placeRelationshipsAsSubject(place);
		if(rels.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.placeJurisdictions()));
		for(final FLEFRecord rel : rels)
			appendJurisdiction(out, rel);
	}

	private void appendJurisdiction(final List<ReportSection> out, final FLEFRecord rel){
		final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
		final String targetId = FLEFRecordHelper.getChildValue(rel, "target.place");
		final FLEFRecord target = (targetId != null? ctx.visible(ctx.model.getRecordById(targetId)): null);
		final String targetName = (target != null? nameValue(primaryNameNode(target), target): targetId);

		final StringBuilder heading = new StringBuilder();
		final String relLabel = ReportFormatters.orEmpty(ReportFormatters.enumLabel(type));
		heading.append(relLabel);
		if(targetName != null && !targetName.isBlank())
			heading.append(' ').append(targetName);
		out.add(new ReportSection.Heading(4, ReportFormatters.escape(heading.toString())));

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


	/**
	 * Lists the place-relationships in which the place is the {@code target},
	 * i.e. the subordinate places it contains: municipalities in a province,
	 * parishes in a diocese, and so on. Mirrors {@link #appendJurisdictions}.
	 */
	private void appendContainedPlaces(final List<ReportSection> out, final FLEFRecord place){
		final List<FLEFRecord> rels = ctx.index.placeRelationshipsAsTarget(place);
		if(rels.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.placeContainedPlaces()));
		for(final FLEFRecord rel : rels)
			appendContainedPlace(out, rel);
	}

	private void appendContainedPlace(final List<ReportSection> out, final FLEFRecord rel){
		final String type = FLEFRecordHelper.getChildValue(rel, TAG_TYPE);
		final String subjectId = FLEFRecordHelper.getChildValue(rel, "subject.place");
		final FLEFRecord subject = (subjectId != null
			? ctx.visible(ctx.model.getRecordById(subjectId)): null);
		final String subjectName = (subject != null
			? nameValue(primaryNameNode(subject), subject): subjectId);

		final StringBuilder heading = new StringBuilder();
		final String relLabel = ReportFormatters.orEmpty(ReportFormatters.enumLabel(type));
		heading.append(relLabel);
		if(subjectName != null && !subjectName.isBlank())
			heading.append(' ').append(subjectName);
		out.add(new ReportSection.Heading(4, ReportFormatters.escape(heading.toString())));

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


	/* ----- Citations ------------------------------------------------------- */

	private void appendCitations(final List<ReportSection> out, final PlaceEntry entry){
		final List<List<ReportSection>> blocks = new ArrayList<>();

		for(final CitationRef ref : entry.citations()){
			final List<ReportSection> block = new ArrayList<>();

			final String original = FLEFRecordHelper.getChildValue(ref.citation(), TAG_ORIGINAL);
			if(original != null && !original.isBlank())
				block.add(new ReportSection.Paragraph(
					"**" + ctx.labels.placeOriginalText() + ":** "
						+ ReportFormatters.escape(original.trim())));

			// The citing record — event or attribute — for traceability.
			final String ownerLabel = describeOwner(ref.owner());
			if(ownerLabel != null)
				block.add(new ReportSection.Paragraph(
					"*" + ctx.labels.placeCitedBy() + ":* " + ReportFormatters.escape(ownerLabel)));

			if(ctx.config.sources())
				block.addAll(ctx.citations().citations(ref.citation()));
			if(ctx.config.evidence())
				ctx.citations().addEvidence(ref.citation(), block);

			if(!block.isEmpty())
				blocks.add(block);
		}

		if(blocks.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.placeCitations()));
		for(final List<ReportSection> block : blocks)
			out.addAll(block);
	}

	/**
	 * Returns a short description of the record that cites the place. For
	 * events, the event type plus the date; for attributes, the attribute
	 * type plus the {@code valid_from} date (attributes have no {@code date}
	 * field).
	 */
	private String describeOwner(final FLEFRecord owner){
		if(owner == null)
			return null;
		final String type = FLEFRecordHelper.getChildValue(owner, TAG_TYPE);

		// Events carry "date"; attributes carry "valid_from".
		String date = FLEFRecordHelper.extractDate(owner);
		if(date == null)
			date = GenealogicalDateHelper.formatDateStructure(
				owner, TAG_VALID_FROM, ctx.labels, contextLabels);

		final StringBuilder sb = new StringBuilder();
		if(type != null && !type.isBlank())
			sb.append(type);
		if(date != null && !date.isBlank()){
			if(sb.length() > 0)
				sb.append(" — ");
			sb.append(date);
		}
		return (sb.length() > 0? sb.toString(): null);
	}


	/* ======================================================================
	 *                          Name helpers
	 * ====================================================================== */

	/**
	 * Returns the {@code name} node to be used as the section heading: the
	 * first {@code official} name when available, otherwise the first
	 * non-blank name, otherwise {@code null}.
	 */
	private static FLEFRecord primaryNameNode(final FLEFRecord place){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(place, TAG_NAME);
		if(names.isEmpty())
			return null;

		for(final FLEFRecord n : names){
			final String type = FLEFRecordHelper.getChildValue(n, TAG_TYPE);
			if(NAME_TYPE_OFFICIAL.equalsIgnoreCase(type)){
				final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
				if(v != null && !v.isBlank())
					return n;
			}
		}
		for(final FLEFRecord n : names){
			final String v = FLEFRecordHelper.getChildValue(n, TAG_VALUE);
			if(v != null && !v.isBlank())
				return n;
		}
		return null;
	}

	/** Returns the trimmed display value of a name node, or the place ID as fallback. */
	private static String nameValue(final FLEFRecord nameNode, final FLEFRecord place){
		if(nameNode != null){
			final String v = FLEFRecordHelper.getChildValue(nameNode, TAG_VALUE);
			if(v != null && !v.isBlank())
				return v.trim();
		}
		return ReportFormatters.orEmpty(place.getId());
	}


	/* ======================================================================
	 *                          Data holders
	 * ====================================================================== */

	/**
	 * Accumulator for the citations of a single place. Declared as a plain
	 * class because records cannot hold additional mutable instance state.
	 */
	private static final class PlaceEntry{
		final FLEFRecord place;
		final List<CitationRef> citations = new ArrayList<>();

		PlaceEntry(final FLEFRecord place){
			this.place = Objects.requireNonNull(place);
		}

		FLEFRecord place(){
			return place;
		}

		List<CitationRef> citations(){
			return citations;
		}
	}

	private record CitationRef(FLEFRecord citation, FLEFRecord owner){
	}

}
