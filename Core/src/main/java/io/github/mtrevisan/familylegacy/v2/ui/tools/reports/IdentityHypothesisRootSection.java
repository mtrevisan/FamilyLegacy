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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main section for an identity-hypothesis report.
 *
 * <p>Renders the two candidates, the comment, then every conclusion that
 * evaluates the hypothesis.
 */
final class IdentityHypothesisRootSection implements SectionBuilder{

	private static final String TAG_IDENTITY = "identity";
	private static final String TAG_COMMENT = "comment";
	private static final String TAG_RESOLVES = "resolves";
	private static final String TAG_ISSUE = "issue";
	private static final String TAG_PROOF = "proof_status";
	private static final String TAG_NARRATIVE = "narrative";

	private static final String TYPE_CONCLUSION = "conclusion";


	private final ReportContext ctx;


	IdentityHypothesisRootSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isIdentityHypothesisRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.sections().identityHypothesis()));

		writeCandidates(out);
		writeConclusions(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(ctx.root, out);
		out.addAll(ctx.citations().audit(ctx.root));
		return out;
	}


	private void writeCandidates(final List<ReportSection> out){
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord cand : FLEFRecordHelper.findChildren(ctx.root, TAG_IDENTITY)){
			final FLEFRecord ref = cand.getTheOnlyChild();
			if(ref == null || FLEFRecord.TAG_VOID.equalsIgnoreCase(ref.getTag()))
				continue;
			final String id = ref.getValue();
			if(id == null)
				continue;
			final FLEFRecord rec = ctx.visible(ctx.model.getRecordById(id));
			items.add(rec != null? ctx.displayText(rec): id);
		}
		if(items.isEmpty())
			return;
		out.add(new ReportSection.Paragraph(
			"**" + ctx.labels.sections().identityCandidates() + ":** "
				+ ReportFormatters.escape(String.join(" ↔ ", items))));

		final String comment = FLEFRecordHelper.getChildValue(ctx.root, TAG_COMMENT);
		if(comment != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().identityComment() + ":** "
					+ ReportFormatters.escape(comment)));
	}


	private void writeConclusions(final List<ReportSection> out){
		final List<FLEFRecord> conclusions = new ArrayList<>();
		for(final FLEFRecord c : ctx.visibleRecordsByType(TYPE_CONCLUSION)){
			for(final FLEFRecord t : FLEFRecordHelper.findChildren(c, TAG_RESOLVES)){
				final FLEFRecord ref = t.getTheOnlyChild();
				if(ref != null && ctx.root.getId().equals(ref.getValue())){
					conclusions.add(c);
					break;
				}
			}
		}
		if(conclusions.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sections().conclusions()));
		for(final FLEFRecord c : conclusions){
			out.add(new ReportSection.Heading(3, ReportFormatters.escape(
				ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(c, TAG_ISSUE)))));
			final String proof = FLEFRecordHelper.getChildValue(c, TAG_PROOF);
			if(proof != null)
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.sections().researchProofStatus() + ":** "
						+ ReportFormatters.escape(ReportFormatters.enumLabel(proof))));
			final String narrative = FLEFRecordHelper.getChildValue(c, TAG_NARRATIVE);
			if(narrative != null)
				out.add(new ReportSection.Paragraph(ReportFormatters.escape(narrative)));
		}
	}

}
