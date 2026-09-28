package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index.RelationIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Produces an "Ancestors of X" report: a pedigree-tree preview, followed by
 * one self-contained sub-report per person in the direct line (and, when
 * the user requests it, per collateral relative).
 */
public final class AncestorReportGenerator{

	private final FLEFModel model;
	private final FLEFRecord root;
	private final ReportConfig config;


	public AncestorReportGenerator(final FLEFModel model, final FLEFRecord root,
		final ReportConfig config){
		this.model = Objects.requireNonNull(model);
		this.root = Objects.requireNonNull(root);
		this.config = Objects.requireNonNull(config);
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final RelationIndex index = RelationIndex.build(model);

		final List<FLEFRecord> direct = index.ancestorsByGeneration(root);
		final List<FLEFRecord> all;
		final List<FLEFRecord> collaterals;
		if(config.collateralScope() == CollateralScope.NONE){
			all = direct;
			collaterals = List.of();
		}
		else{
			all = index.ancestorsWithCollaterals(root, config.collateralScope());
			collaterals = new ArrayList<>(all);
			collaterals.removeAll(direct);
		}

		final ReportContext ctx = ReportContext.build(model, root, config, index);
		final String title = String.format(ctx.labels.sections().ancestorReportTitle(),
			ctx.displayText(root));

		final PedigreeTreeSection tree = new PedigreeTreeSection(ctx, direct, collaterals,
			PedigreeTreeSection.Direction.ANCESTORS);

		final ReportConfig perPerson = perPersonConfig(config);
		return new MultiPersonReportGenerator(model, all, perPerson, title, List.of(tree))
			.generate(listener);
	}


	private static ReportConfig perPersonConfig(final ReportConfig base){
		return new ReportConfig(
			base.language(),
			base.privacyPolicy(),
			base.showPrivacyDetails(),
			base.audit(),
			base.header(),
			/*introduction*/          true,
			/*paternalAncestry*/      false,
			/*maternalAncestry*/      false,
			/*descendants*/           false,
			/*directRelations*/       false,
			/*indirectRelations*/     false,
			base.collateralScope(),
			base.notes(),
			base.sources(),
			base.media(),
			base.evidence(),
			base.contextAndResearch(),
			base.culturalNorms(),
			base.groups(),
			base.groupMembers(),
			base.groupSubgroups(),
			base.groupAttributes(),
			base.historicEvents(),
			base.repositories(),
			base.eventFullParticipants(),
			base.dateProvenance(),
			base.coverageReport(),
			base.timeline(),
			/*indexIndividuals*/      false,
			/*indexPlaces*/           base.indexPlaces(),
			base.includeDescriptions(),
			base.includePictures());
	}

}
