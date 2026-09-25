package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * Builds the Cultural norms section.
 *
 * <p>Collects every {@code cultural_norm} reference reachable from the root
 * individual — from its {@code name} structures and from the
 * {@code approximate} qualifiers of its events — and renders the referenced
 * {@code cultural_norm} records with their title, rule type, validity window,
 * place, notes and citations.</p>
 *
 * <p>The section is emitted only when {@code config.culturalNorms()} is
 * enabled and at least one norm is reachable.</p>
 */
final class CulturalNormSection implements SectionBuilder{

	private static final String TAG_CULTURAL_NORM = "cultural_norm";
	private static final String TAG_TITLE = "title";
	private static final String TAG_RULE_TYPE = "rule_type";
	private static final String TAG_TYPE = "type";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	CulturalNormSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.culturalNorms())
			return List.of();

		final List<FLEFRecord> norms = collectNorms();
		if(norms.isEmpty())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.culturalNorms()));
		for(final FLEFRecord norm : norms)
			appendNorm(out, norm);
		return out;
	}


	/* ======================================================================
	 *                          Collection
	 * ====================================================================== */

	private List<FLEFRecord> collectNorms(){
		final Set<String> ids = new LinkedHashSet<>();

		// 1. The individual itself (names and any other cultural_norm
		//    reference attached to it).
		collectIdsFrom(ctx.root, ids);

		// 2. Every event the individual participates in.
		for(final FLEFRecord evt : ctx.index.eventsOf(ctx.root))
			collectIdsFrom(evt, ids);

		// 3. Every attribute of the individual.
		for(final FLEFRecord attr : ctx.index.attributesOf(ctx.root))
			collectIdsFrom(attr, ids);

		// Resolve, filter by privacy, preserve order.
		final List<FLEFRecord> out = new ArrayList<>(ids.size());
		for(final String id : ids){
			final FLEFRecord norm = ctx.visible(ctx.model.getRecordById(id));
			if(norm != null)
				out.add(norm);
		}
		return out;
	}

	/**
	 * Recursively walks {@code rec} and gathers the values of every child
	 * tagged {@code cultural_norm}, regardless of nesting depth. This covers
	 * all the branches where a cultural norm may be referenced:
	 * {@code name.cultural_norm},
	 * {@code date.value.point.full_date.approximate.cultural_norm},
	 * {@code date.value.bounded.not_before.decade.approximate.cultural_norm},
	 * and so on.
	 */
	private static void collectIdsFrom(final FLEFRecord rec, final Set<String> out){
		for(final FLEFRecord child : rec.getChildren()){
			if(TAG_CULTURAL_NORM.equalsIgnoreCase(child.getTag())){
				final String id = child.getValue();
				if(id != null && !id.isBlank())
					out.add(id);
			}
			collectIdsFrom(child, out);
		}
	}


	/* ======================================================================
	 *                          Rendering
	 * ====================================================================== */

	private void appendNorm(final List<ReportSection> out, final FLEFRecord norm){
		out.add(new ReportSection.Heading(2, titleOf(norm)));

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.culturalNormRuleType(),
			ruleTypeOf(norm));

		final String from = GenealogicalDateHelper.formatDateStructure(norm, TAG_VALID_FROM, ctx.labels, contextLabels);
		if(from != null)
			rows.add("**" + ctx.labels.culturalNormValidFrom() + ":** "
				+ ReportFormatters.escape(from));

		final String to = GenealogicalDateHelper.formatDateStructure(norm, TAG_VALID_TO, ctx.labels, contextLabels);
		if(to != null)
			rows.add("**" + ctx.labels.culturalNormValidTo() + ":** "
				+ ReportFormatters.escape(to));

		final String place = FLEFRecordHelper.extractPlace(norm, ctx.model);
		if(place != null)
			rows.add("**" + ctx.labels.culturalNormPlace() + ":** "
				+ ReportFormatters.escape(place));

		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(norm));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(norm));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(norm, out);
		out.addAll(ctx.citations().audit(norm));
	}


	private String titleOf(final FLEFRecord norm){
		final String title = FLEFRecordHelper.getChildValue(norm, TAG_TITLE);
		if(title != null && !title.isBlank())
			return ReportFormatters.escape(title.trim());

		// Fall back to the rule type, prettified.
		final String rule = ruleTypeOf(norm);
		if(rule != null)
			return ReportFormatters.escape(rule);

		return ReportFormatters.escape(ReportFormatters.orEmpty(norm.getId()));
	}

	private String ruleTypeOf(final FLEFRecord norm){
		String v = FLEFRecordHelper.getChildValue(norm, TAG_RULE_TYPE);
		if(v == null || v.isBlank())
			v = FLEFRecordHelper.getChildValue(norm, TAG_TYPE);
		if(v == null || v.isBlank())
			return null;
		return v.replace('_', ' ');
	}

}
