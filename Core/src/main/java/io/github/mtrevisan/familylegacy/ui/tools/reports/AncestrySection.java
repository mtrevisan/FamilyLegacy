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

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the paternal and maternal ancestry sections. Each is emitted only
 * when enabled, rendering an explicit message when empty.
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

	private void append(final List<ReportSection> out, final String title, final List<FLEFRecord> ancestors){
		out.add(new ReportSection.Heading(1, title));
		if(ancestors.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.sections().noAncestors()));
			return;
		}
		for(final FLEFRecord a : ancestors){
			out.add(new ReportSection.Paragraph(
				"  " + ReportFormatters.escape(ctx.displayText(a))));
		}
	}

}
