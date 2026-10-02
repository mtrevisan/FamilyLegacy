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
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import org.apache.commons.lang3.StringUtils;

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
public final class CulturalNormRootSection implements SectionBuilder{

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
			FLEFRecordHelper.getChildValue(ctx.root, CulturalNormReader.TAG_TITLE));
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.sections().culturalNormOf(), ReportFormatters.escape(title))));

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
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().culturalNormRuleType(),
			FLEFRecordHelper.getChildValue(ctx.root, CulturalNormReader.TAG_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().culturalNormValidFrom(),
			GenealogicalDateHelper.formatDateStructure(ctx.root, CulturalNormReader.TAG_VALID_FROM,
				ctx.labels, contextLabels));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().culturalNormValidTo(),
			GenealogicalDateHelper.formatDateStructure(ctx.root, CulturalNormReader.TAG_VALID_TO,
				ctx.labels, contextLabels));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().culturalNormPlace(),
			FLEFRecordHelper.extractPlace(ctx.root, ctx.model));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeInvocations(final List<ReportSection> out){
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord rec : ctx.model.getRecords()){
			for(final FLEFRecord cn : collectDescendantsWithTag(rec, CulturalNormHandler.TYPE)){
				if(!ctx.root.getId().equals(cn.getValue()))
					continue;
				final String label = describeOwner(rec);
				if(label != null && !items.contains(label))
					items.add(label);
			}
		}
		if(items.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().culturalNormInvocations()));
		out.add(new ReportSection.BulletList(items.stream()
			.map(ReportFormatters::escape).toList()));
	}

	public static List<FLEFRecord> collectDescendantsWithTag(final FLEFRecord rec, final String tag){
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
		final String type = FLEFRecordHelper.getChildValue(rec, CulturalNormReader.TAG_TYPE);
		final String id = rec.getId();
		if(type != null)
			return type + StringUtils.SPACE + ReportFormatters.orEmpty(id);
		return ReportFormatters.orEmpty(id);
	}

}
