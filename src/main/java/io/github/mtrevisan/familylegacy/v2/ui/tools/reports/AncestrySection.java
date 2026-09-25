package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the paternal and maternal ancestry sections. Each is emitted only
 * when enabled and non-empty.
 */
final class AncestrySection implements SectionBuilder{

	private final ReportContext ctx;


	AncestrySection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		final List<ReportSection> out = new ArrayList<>();
		if(ctx.config.paternalAncestry())
			append(out, ctx.labels.paternalAncestry(), ctx.index.paternalAncestors(ctx.root));
		if(ctx.config.maternalAncestry())
			append(out, ctx.labels.maternalAncestry(), ctx.index.maternalAncestors(ctx.root));
		return out;
	}


	private void append(final List<ReportSection> out, final String title,
		final List<FLEFRecord> ancestors){
		if(ancestors.isEmpty())
			return;
		out.add(new ReportSection.Heading(1, title));
		for(final FLEFRecord a : ancestors)
			out.add(new ReportSection.Paragraph(
				"  " + ReportFormatters.escape(ctx.displayText(a))));
	}

}
