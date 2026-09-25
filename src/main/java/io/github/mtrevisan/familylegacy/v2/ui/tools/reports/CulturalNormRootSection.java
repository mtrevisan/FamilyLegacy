package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Builds the main section for a cultural-norm report.
 *
 * <p>Renders the norm's title, rule type, validity window and place, then
 * lists every name and every approximate date that invokes this norm across
 * the model, so the reader can see which genealogical assertions rely on it.
 */
final class CulturalNormRootSection implements SectionBuilder{

	private static final String TAG_TITLE = "title";
	private static final String TAG_RULE_TYPE = "rule_type";
	private static final String TAG_TYPE = "type";
	private static final String TAG_VALID_FROM = "valid_from";
	private static final String TAG_VALID_TO = "valid_to";
	private static final String TAG_CULTURAL_NORM = "cultural_norm";


	private final ReportContext ctx;
	private final Function<String, String> contextLabels;


	CulturalNormRootSection(final ReportContext ctx){
		this.ctx = ctx;
		this.contextLabels = ctx.contextLabelResolver();
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isCulturalNormRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		final String title = ReportFormatters.orEmpty(
			FLEFRecordHelper.getChildValue(ctx.root, TAG_TITLE));
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.culturalNormOf(), ReportFormatters.escape(title))));

		writeBasicInfo(out);
		writeInvocations(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		out.addAll(ctx.citations().audit(ctx.root));
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.culturalNormRuleType(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_RULE_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.culturalNormValidFrom(),
			GenealogicalDateHelper.formatDateStructure(ctx.root, TAG_VALID_FROM,
				ctx.labels, contextLabels));
		ReportFormatters.appendIfPresent(rows, ctx.labels.culturalNormValidTo(),
			GenealogicalDateHelper.formatDateStructure(ctx.root, TAG_VALID_TO,
				ctx.labels, contextLabels));
		ReportFormatters.appendIfPresent(rows, ctx.labels.culturalNormPlace(),
			FLEFRecordHelper.extractPlace(ctx.root, ctx.model));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeInvocations(final List<ReportSection> out){
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord rec : ctx.model.getRecords()){
			for(final FLEFRecord cn : collectDescendantsWithTag(rec, TAG_CULTURAL_NORM)){
				if(!ctx.root.getId().equals(cn.getValue()))
					continue;
				final String label = describeOwner(rec);
				if(label != null && !items.contains(label))
					items.add(label);
			}
		}
		if(items.isEmpty()) return;

		out.add(new ReportSection.Heading(2, ctx.labels.culturalNormInvocations()));
		out.add(new ReportSection.BulletList(items.stream()
			.map(ReportFormatters::escape).toList()));
	}

	private static List<FLEFRecord> collectDescendantsWithTag(final FLEFRecord rec, final String tag){
		final List<FLEFRecord> out = new ArrayList<>();
		collectDescendantsWithTag(rec, tag, out);
		return out;
	}

	private static void collectDescendantsWithTag(final FLEFRecord rec, final String tag,
		final List<FLEFRecord> out){
		for(final FLEFRecord child : rec.getChildren()){
			if(tag.equalsIgnoreCase(child.getTag()))
				out.add(child);
			collectDescendantsWithTag(child, tag, out);
		}
	}

	private String describeOwner(final FLEFRecord rec){
		if(rec == ctx.root)
			return null;
		final String type = FLEFRecordHelper.getChildValue(rec, TAG_TYPE);
		final String id = rec.getId();
		if(type != null)
			return type + " " + ReportFormatters.orEmpty(id);
		return ReportFormatters.orEmpty(id);
	}

}
