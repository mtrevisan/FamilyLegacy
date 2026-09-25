package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.List;


/**
 * Builds the descendants tree as an indented monospace block.
 */
final class DescendantsSection implements SectionBuilder{

	private final ReportContext ctx;


	DescendantsSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.descendants())
			return List.of();
		final StringBuilder sb = new StringBuilder();
		write(sb, ctx.root, 0);
		return List.of(
			new ReportSection.Heading(1, ctx.labels.descendants()),
			new ReportSection.Paragraph(sb.toString()));
	}


	private void write(final StringBuilder sb, final FLEFRecord parent, final int depth){
		sb.append("  ".repeat(depth))
			.append(ReportFormatters.escape(ctx.displayText(parent)))
			.append('\n');
		for(final FLEFRecord child : ctx.index.childrenOf(parent))
			write(sb, child, depth + 1);
	}

}
