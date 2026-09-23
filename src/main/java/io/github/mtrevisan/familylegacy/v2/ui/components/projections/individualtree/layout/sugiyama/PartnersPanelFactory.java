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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.sugiyama;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.IndividualTreeGraphListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;


/**
 * Creates {@link PartnersPanel}s for the Sugiyama layout, wiring the upper
 * connections to the grandparents.
 * <p>
 * The factory holds the collaborators that are fixed for a single layout
 * pass (model, orientation, tree listener, popup factory) so that the
 * caller only needs to supply the two {@link TreeNode}s of the couple.
 */
final class PartnersPanelFactory{

	private final FLEFModel model;
	private final TreeLayout treeLayout;
	private final IndividualTreeGraphListener treeListener;
	private final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory;


	PartnersPanelFactory(final FLEFModel model, final TreeLayout treeLayout,
			final IndividualTreeGraphListener treeListener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory){
		this.model = model;
		this.treeLayout = treeLayout;
		this.treeListener = treeListener;
		this.popupFactory = popupFactory;
	}


	PartnersPanel create(final TreeNode fatherTn, final TreeNode motherTn){
		final BoxPanelType boxType = (isRootGeneration(fatherTn) || isRootGeneration(motherTn)
			? BoxPanelType.PRIMARY
			: BoxPanelType.SECONDARY);

		final IndividualData fatherData = (fatherTn != null? fatherTn.getIndividualData(): null);
		final IndividualData motherData = (motherTn != null? motherTn.getIndividualData(): null);
		final PartnersPanel panel = PartnersPanel.create(boxType, treeLayout, model)
			.withBiologicalParents(fatherData, motherData)
			.withListener(treeListener, popupFactory)
			.withSuppressCollapseBadge(true);

		wireGrandparents(panel, fatherTn, motherTn);

		return panel;
	}


	/* ======================================================================
	 *                          Internal
	 * ====================================================================== */

	private static void wireGrandparents(final PartnersPanel panel, final TreeNode fatherTn, final TreeNode motherTn){
		panel.getFatherPanel().withParent(
			individual(fatherTn != null? fatherTn.getFather(): null),
			individual(fatherTn != null? fatherTn.getMother(): null));
		panel.getMotherPanel().withParent(
			individual(motherTn != null? motherTn.getFather(): null),
			individual(motherTn != null? motherTn.getMother(): null));
	}

	private static boolean isRootGeneration(final TreeNode tn){
		return (tn != null && tn.getGeneration() == 0);
	}

	private static FLEFRecord individual(final TreeNode tn){
		return (tn != null? tn.getIndividual(): null);
	}

}
