package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main section for a conclusion report (root = {@code conclusion} record).
 *
 * <p>Renders the issue, proof status and narrative, then:
 * <ul>
 *   <li>every resolved target, with the {@code preferred} one marked;</li>
 *   <li>the research questions that produced this conclusion;</li>
 *   <li>sources, evidence, audit and privacy.</li>
 * </ul>
 */
final class ConclusionRootSection implements SectionBuilder{

	private static final String TAG_ISSUE = "issue";
	private static final String TAG_PROOF = "proof_status";
	private static final String TAG_NARRATIVE = "narrative";
	private static final String TAG_RESOLVES = "resolves";
	private static final String TAG_PREFERRED = "preferred";
	private static final String TAG_RESEARCH = "research";
	private static final String TAG_TITLE = "title";
	private static final String TAG_VOID = "void";


	private final ReportContext ctx;


	ConclusionRootSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isConclusionRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.conclusionOf(),
			ReportFormatters.escape(ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(ctx.root, TAG_ISSUE))))));

		writeBasicInfo(out);
		writeResolves(out);
		writeLinkedQuestions(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ctx.root));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(ctx.root, out);
		out.addAll(ctx.citations().audit(ctx.root));
		out.addAll(ctx.citations().privacyInfo(ctx.root));
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final String proof = FLEFRecordHelper.getChildValue(ctx.root, TAG_PROOF);
		if(proof != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.researchProofStatus() + ":** "
					+ ReportFormatters.escape(ReportFormatters.enumLabel(proof))));

		final String narrative = FLEFRecordHelper.getChildValue(ctx.root, TAG_NARRATIVE);
		if(narrative != null)
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(narrative)));
	}


	private void writeResolves(final List<ReportSection> out){
		final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(ctx.root, TAG_RESOLVES);
		if(resolves.isEmpty()) return;

		final String preferredLabel = describeOneOf(
			FLEFRecordHelper.findChild(ctx.root, TAG_PREFERRED));

		out.add(new ReportSection.Heading(2, ctx.labels.researchResolves()));
		final List<String> items = new ArrayList<>();
		for(final FLEFRecord r : resolves){
			final String label = describeOneOf(r);
			if(label == null) continue;
			final boolean preferred = (preferredLabel != null && preferredLabel.equals(label));
			items.add(ReportFormatters.escape(label)
				+ (preferred? "  ← *" + ctx.labels.researchPreferred() + "*": ""));
		}
		if(!items.isEmpty())
			out.add(new ReportSection.BulletList(items));
	}


	private void writeLinkedQuestions(final List<ReportSection> out){
		final List<FLEFRecord> links = FLEFRecordHelper.findChildren(ctx.root, TAG_RESEARCH);
		if(links.isEmpty()) return;

		final List<String> items = new ArrayList<>();
		for(final FLEFRecord r : links){
			final String qid = r.getValue();
			if(qid == null) continue;
			final FLEFRecord q = ctx.visible(ctx.model.getRecordById(qid));
			if(q == null) continue;
			final String title = FLEFRecordHelper.getChildValue(q, TAG_TITLE);
			items.add(ReportFormatters.escape(
				title != null && !title.isBlank()? title.trim(): qid));
		}
		if(items.isEmpty()) return;

		out.add(new ReportSection.Heading(2, ctx.labels.researchLinkedQuestions()));
		out.add(new ReportSection.BulletList(items));
	}


	private String describeOneOf(final FLEFRecord field){
		if(field == null) return null;
		final FLEFRecord ref = field.getTheOnlyChild();
		if(ref == null || TAG_VOID.equalsIgnoreCase(ref.getTag())) return null;
		final String tag = ref.getTag();
		final String id = ref.getValue();
		if(id == null || id.isBlank()) return null;
		final FLEFRecord rec = ctx.model.getRecordById(id);
		if(rec == null || !ctx.isVisible(rec))
			return tag + " " + id;
		return ctx.displayText(rec);
	}

}
