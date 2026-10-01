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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index.KinshipResolver;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


/**
 * Builds the direct-relations and indirect-relations sections.
 *
 * <p>Direct relations walk the descendant graph from the root, annotating
 * every individual with its kinship term and explicit roles. Indirect relations
 * list spouse/associate connections of direct relations, together with the kinship
 * term of the direct relation they hang off and relevant context impacts.</p>
 */
final class RelationsSection implements SectionBuilder{

	private final ReportContext ctx;


	RelationsSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		final List<ReportSection> out = new ArrayList<>();
		if(ctx.config.directRelations())
			out.addAll(direct());
		if(ctx.config.indirectRelations())
			out.addAll(indirect());
		return out;
	}

	/* ----- Direct relations ------------------------------------------------ */

	private List<ReportSection> direct(){
		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.directRelations()));

		int counter = 0;
		for(final FLEFRecord ind : ctx.index.reachable(ctx.root)){
			counter++;
			out.addAll(directEntry(counter, ind));
		}
		return out;
	}

	private List<ReportSection> directEntry(final int counter, final FLEFRecord ind){
		final List<ReportSection> out = new ArrayList<>();
		final boolean isRoot = Objects.equals(ind.getId(), ctx.root.getId());
		final String kinTerm = (isRoot? null: ctx.kinship().shortTerm(ind.getId(), ctx.root.getId()));
		final String suffix = (isRoot
			? "  — *" + ctx.labels.sections().subject() + "*"
			: (ReportFormatters.isUsefulKinshipTerm(kinTerm)
				? "  — *" + ReportFormatters.escape(kinTerm) + "*": StringUtils.EMPTY));

		out.add(new ReportSection.Heading(2,
			counter + ". " + ReportFormatters.escape(ctx.displayText(ind)) + suffix));

		final List<String> meta = new ArrayList<>();
		final String sex = Optional.ofNullable(IndividualReader.extractRawSex(ind))
			.orElse(ctx.labels.sections().sexUnknown());
		meta.add("**" + ctx.labels.sections().sex() + ":** " + ReportFormatters.escape(sex));

		final List<FLEFRecord> parents = ctx.index.parentsOf(ind);
		if(!parents.isEmpty()){
			final List<String> p = new ArrayList<>(parents.size());
			for(final FLEFRecord parent : parents)
				p.add(ReportFormatters.escape(ctx.displayText(parent)));
			meta.add("**" + ctx.labels.sections().roleChild() + " " + ctx.labels.sections().of() + ":** " + String.join(", ", p));
		}
		out.add(new ReportSection.BulletList(meta));

		// Render contextual impacts attached to this specific direct relation
		appendContextImpactsForRecord(out, ind);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ind));
		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(ind));
		return out;
	}

	/* ----- Indirect relations --------------------------------------------- */

	private List<ReportSection> indirect(){
		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.indirectRelations()));

		final List<KinshipResolver.IndirectRelation> indirect = ctx.index.indirectRelationsDetailed(ctx.root);
		if(indirect.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.sections().noIndirectRelations()));
			return out;
		}

		final List<KinshipResolver.IndirectRelation> sorted = new ArrayList<>(indirect);
		sorted.sort(Comparator.comparing(
			(final KinshipResolver.IndirectRelation ir) ->
				ctx.displayText(ir.individual()).toLowerCase(ctx.labels.language().locale())));

		final List<List<String>> rows = new ArrayList<>(sorted.size());
		for(final KinshipResolver.IndirectRelation ir : sorted){
			final String kind = (ir.kind() == KinshipResolver.IndirectKind.SPOUSE
				? ctx.labels.sections().relationSpouseOf()
				: ctx.labels.sections().relationAssociateOf());
			final String viaKin = ctx.kinship().shortTerm(ir.via().getId(), ctx.root.getId());
			final String viaLabel = ReportFormatters.esc(ctx.displayText(ir.via()))
				+ (ReportFormatters.isUsefulKinshipTerm(viaKin)? " (" + viaKin + ")": StringUtils.EMPTY);
			rows.add(List.of(
				ReportFormatters.esc(ctx.displayText(ir.individual())),
				kind,
				viaLabel));
		}

		out.add(new ReportSection.Table(
			List.of(ctx.labels.sections().person(), ctx.labels.sections().relation(), ctx.labels.sections().of()), rows));
		return out;
	}

	/* ----- Context Impacts Helper ------------------------------------------ */

	/**
	 * Renders contextual historical or legal impacts linked to an individual record.
	 */
	private void appendContextImpactsForRecord(final List<ReportSection> out, final FLEFRecord ind){
		if(ind.getId() == null)
			return;
		final List<FLEFRecord> impacts = ctx.index.contextImpactsFor(Set.of(ind.getId()));
		if(impacts.isEmpty())
			return;

		for(final FLEFRecord imp : impacts){
			final String rationale = FLEFRecordHelper.getChildValue(imp, "rationale");
			if(rationale != null && !rationale.isBlank()){
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.sections().contextImpact() + ":** "
						+ ReportFormatters.escape(rationale.trim())));
			}
		}
	}

}
