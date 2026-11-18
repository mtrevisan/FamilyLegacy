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

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Builds the descendants tree as an indented monospace block: each person with the years of birth and death and the spouses, the
 * children from the eldest to the youngest. A person that is already in the tree (two branches that meet again) is not drawn a second
 * time, with the children and all: the line says to see above.
 */
public final class DescendantsSection implements SectionBuilder{

	private final ReportContext ctx;
	private final TreeText text;


	public DescendantsSection(final ReportContext ctx){
		this.ctx = ctx;
		this.text = new TreeText(ctx);
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.descendants())
			return List.of();
		final StringBuilder sb = new StringBuilder();
		write(sb, ctx.root, 0, new HashSet<>(), new HashSet<>());
		return List.of(
			new ReportSection.Heading(1, ctx.labels.descendants()),
			new ReportSection.Paragraph(sb.toString()));
	}


	private void write(final StringBuilder sb, final FLEFRecord parent, final int depth, final Set<String> path, final Set<String> drawn){
		sb.repeat("  ", depth)
			.append(ReportFormatters.escape(text.label(parent)));
		for(final FLEFRecord spouse : ctx.index.spousesOf(parent))
			sb.append(" + ")
				.append(ReportFormatters.escape(text.label(spouse)));

		final List<FLEFRecord> children = text.childrenOf(parent);
		//a person that is drawn already has the children below the first line, and a circle of parentage (an error of the file) must
		//not be followed forever
		if(!drawn.add(parent.getId()) || path.contains(parent.getId())){
			if(!children.isEmpty())
				sb.append(" (").append(ctx.labels.sections().treeSeeAbove()).append(')');
			sb.append(StringUtils.LF);
			return;
		}
		sb.append(StringUtils.LF);

		path.add(parent.getId());
		for(final FLEFRecord child : children)
			write(sb, child, depth + 1, path, drawn);
		path.remove(parent.getId());
	}

}
