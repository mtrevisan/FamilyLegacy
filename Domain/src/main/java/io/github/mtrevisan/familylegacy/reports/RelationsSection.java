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
import io.github.mtrevisan.familylegacy.reports.index.KinshipResolver;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Builds the direct-relations and indirect-relations sections.
 *
 * <p>Direct relations are the root and its descendants, one table for each generation (the eldest first), each person with the years
 * of birth and death, the kinship term from the root and the parents. A person who descends from the root through more than one line
 * is listed once, in the first generation he or she is reached. Indirect relations list the spouses and the associates of the direct
 * relations, together with the kinship term of the direct relation they hang off.</p>
 */
public final class RelationsSection implements SectionBuilder{

	private final ReportContext ctx;
	private final TreeText text;


	public RelationsSection(final ReportContext ctx){
		this.ctx = ctx;
		this.text = new TreeText(ctx);
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
		out.add(new ReportSection.Paragraph(ctx.labels.sections().directRelationsNote()));

		final Set<String> seen = new HashSet<>(Set.of(ctx.root.getId()));
		List<FLEFRecord> generation = List.of(ctx.root);
		for(int level = 0; !generation.isEmpty(); level ++){
			out.add(new ReportSection.Heading(2, (level == 0
				? StringUtils.capitalize(StringUtils.strip(ctx.labels.sections().subject(), "()"))
				: ctx.labels.sections().generationN(ctx.formatInt(level)) + " (" + ctx.formatInt(generation.size()) + ")")));
			out.add(generationTable(generation));

			final List<FLEFRecord> next = new ArrayList<>();
			for(final FLEFRecord person : generation)
				for(final FLEFRecord child : text.childrenOf(person))
					if(seen.add(child.getId()))
						next.add(child);
			generation = next;
		}
		return out;
	}

	private ReportSection generationTable(final List<FLEFRecord> generation){
		final List<List<String>> rows = new ArrayList<>(generation.size());
		for(final FLEFRecord person : generation){
			final boolean isRoot = Objects.equals(person.getId(), ctx.root.getId());
			final List<String> parents = new ArrayList<>();
			for(final FLEFRecord parent : ctx.index.parentsOf(person))
				parents.add(ReportFormatters.escape(text.label(parent)));
			rows.add(List.of(
				ReportFormatters.escape(text.label(person)),
				(isRoot? StringUtils.EMPTY: ReportFormatters.escape(ReportFormatters.orEmpty(kinshipOf(person)))),
				String.join(", ", parents)));
		}
		return new ReportSection.Table(List.of(ctx.labels.sections().person(), ctx.labels.sections().relation(),
			ctx.labels.sections().roleChild() + StringUtils.SPACE + ctx.labels.sections().of()), rows);
	}

	/** @return	The kinship term of the person from the root; a descendant through an adoptive, foster or other legal link is not a blood relative: the legal term is shown, marked as such; {@code null} if there is none. */
	private String kinshipOf(final FLEFRecord person){
		final String blood = ctx.kinship().shortTerm(person.getId(), ctx.root.getId());
		if(ReportFormatters.isUsefulKinshipTerm(blood))
			return blood;

		final String legal = ctx.legalKinship().shortTerm(person.getId(), ctx.root.getId());
		return (ReportFormatters.isUsefulKinshipTerm(legal)? ctx.labels.sections().kinshipLegal(legal): null);
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
			final String viaLabel = ReportFormatters.esc(text.label(ir.via()))
				+ (ReportFormatters.isUsefulKinshipTerm(viaKin)? " (" + viaKin + ")": StringUtils.EMPTY);
			rows.add(List.of(
				ReportFormatters.esc(text.label(ir.individual())),
				kind,
				viaLabel));
		}

		out.add(new ReportSection.Table(
			List.of(ctx.labels.sections().person(), ctx.labels.sections().relation(), ctx.labels.sections().of()), rows));
		return out;
	}

}
