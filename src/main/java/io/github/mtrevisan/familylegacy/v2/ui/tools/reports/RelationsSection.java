package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Builds the direct-relations and indirect-relations sections.
 *
 * <p>Direct relations walk the descendant graph from the root, annotating
 * every individual with its kinship term. Indirect relations list
 * spouse/associate connections of direct relations, together with the kinship
 * term of the direct relation they hang off.</p>
 */
final class RelationsSection implements SectionBuilder{

	private static final String TAG_SEX = "sex";


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
			? "  — *" + ctx.labels.subject() + "*"
			: (ReportFormatters.isUsefulKinshipTerm(kinTerm)
				? "  — *" + ReportFormatters.escape(kinTerm) + "*": ""));

		out.add(new ReportSection.Heading(2,
			counter + ". " + ReportFormatters.escape(ctx.displayText(ind)) + suffix));

		final List<String> meta = new ArrayList<>();
		final String sex = Optional.ofNullable(FLEFRecordHelper.getChildValue(ind, TAG_SEX))
			.orElse(ctx.labels.sexUnknown());
		meta.add("**" + ctx.labels.sex() + ":** " + ReportFormatters.escape(sex));

		final List<FLEFRecord> parents = ctx.index.parentsOf(ind);
		if(!parents.isEmpty()){
			final List<String> p = new ArrayList<>(parents.size());
			for(final FLEFRecord parent : parents)
				p.add(ReportFormatters.escape(ctx.displayText(parent)));
			meta.add("**" + ctx.labels.childOf() + ":** " + String.join(", ", p));
		}
		out.add(new ReportSection.BulletList(meta));

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

		final List<RelationIndex.IndirectRelation> indirect =
			ctx.index.indirectRelationsDetailed(ctx.root);
		if(indirect.isEmpty()){
			out.add(new ReportSection.Paragraph(ctx.labels.noIndirectRelations()));
			return out;
		}

		final List<RelationIndex.IndirectRelation> sorted = new ArrayList<>(indirect);
		sorted.sort(Comparator.comparing(
			(final RelationIndex.IndirectRelation ir) ->
				ctx.displayText(ir.individual()).toLowerCase(ctx.labels.language().locale())));

		final List<List<String>> rows = new ArrayList<>(sorted.size());
		for(final RelationIndex.IndirectRelation ir : sorted){
			final String kind = (ir.kind() == RelationIndex.IndirectKind.SPOUSE
				? ctx.labels.relationSpouseOf()
				: ctx.labels.relationAssociateOf());
			final String viaKin = ctx.kinship().shortTerm(ir.via().getId(), ctx.root.getId());
			final String viaLabel = ReportFormatters.esc(ctx.displayText(ir.via()))
				+ (ReportFormatters.isUsefulKinshipTerm(viaKin)? " (" + viaKin + ")": "");
			rows.add(List.of(
				ReportFormatters.esc(ctx.displayText(ir.individual())),
				kind,
				viaLabel));
		}

		out.add(new ReportSection.Table(
			List.of(ctx.labels.person(), ctx.labels.relation(), ctx.labels.of()), rows));
		return out;
	}

}
