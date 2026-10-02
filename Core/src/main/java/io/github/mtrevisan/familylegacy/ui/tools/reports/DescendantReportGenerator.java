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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.tools.reports.index.RelationIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Produces a "Descendants of X" report: a pedigree-tree preview, followed
 * by one self-contained sub-report per person in the direct line (and, when
 * the user requests it, per collateral relative).
 */
public final class DescendantReportGenerator{

	private final FLEFModel model;
	private final FLEFRecord root;
	private final ReportConfig config;


	public DescendantReportGenerator(final FLEFModel model, final FLEFRecord root,
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

		final List<FLEFRecord> direct = index.descendantsByGeneration(root);
		final List<FLEFRecord> all;
		final List<FLEFRecord> collaterals;
		if(config.collateralScope() == CollateralScope.NONE){
			all = direct;
			collaterals = List.of();
		}
		else{
			all = index.descendantsWithCollaterals(root, config.collateralScope());
			collaterals = new ArrayList<>(all);
			collaterals.removeAll(direct);
		}

		final ReportContext ctx = ReportContext.build(model, root, config, index);
		final String title = String.format(ctx.labels.sections().ancestorReportTitle(),
			ctx.displayText(root));

		final PedigreeTreeSection tree = new PedigreeTreeSection(ctx, direct, collaterals,
			PedigreeTreeSection.Direction.DESCENDANTS);

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
