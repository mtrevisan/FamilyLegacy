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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.ComponentOrientation;
import java.util.List;


/**
 * Builds the bottom strip that shows the children of the root couple,
 * and wraps it in a scroll pane suitable for the tree orientation.
 */
final class ChildrenStripBuilder{

	private ChildrenStripBuilder(){}


	/**
	 * Builds the {@link SiblingsPanel} that shows the children of the root
	 * couple. The returned panel is always created, even when the root has
	 * no children: use {@link #hasChildren(TreeNode)} to decide whether to
	 * add it to the canvas.
	 */
	static SiblingsPanel build(final TreeNode rootNode, final FLEFModel model, final boolean showPartner,
			final TreeLayout treeLayout, final IndividualTreeGraphListener treeListener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory){
		final FLEFRecord father = extractParentBySex(rootNode, SexType.MALE);
		final FLEFRecord mother = extractParentBySex(rootNode, SexType.FEMALE);

		final BoxPanelType boxType = BoxPanelType.SECONDARY;
		final SiblingsPanel siblingsPanel = SiblingsPanel.create(father, mother, boxType, model, showPartner, treeLayout)
			.withListener(treeListener, popupFactory);

		if(rootNode != null)
			siblingsPanel.withSiblingsData(rootNode.getBiologicalChildrenData());

		return siblingsPanel;
	}

	static boolean hasChildren(final TreeNode rootNode){
		if(rootNode == null || rootNode.getBiologicalChildrenData() == null)
			return false;

		final List<IndividualData> children = rootNode.getBiologicalChildrenData()
			.getSiblings();
		return (children != null && !children.isEmpty());
	}

	/**
	 * Wraps the strip in a scroll pane whose scrollbar policies and padding
	 * match the tree orientation.
	 */
	static JScrollPane wrap(final JPanel content, final TreeLayout treeLayout){
		final JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setBorder(null);

		scrollPane.setVerticalScrollBarPolicy(treeLayout == TreeLayout.VERTICAL
			? ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
			: ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(treeLayout == TreeLayout.VERTICAL
			? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
			: ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		if(treeLayout == TreeLayout.VERTICAL){
			final JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
			final int scrollBarHeight = scrollBar.getPreferredSize().height;
			content.setBorder(BorderFactory.createEmptyBorder(0, 0, scrollBarHeight, 0));
		}
		else{
			scrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
			final JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
			final int scrollBarWidth = scrollBar.getPreferredSize().width;
			content.setBorder(BorderFactory.createEmptyBorder(0, scrollBarWidth, 0, 0));
		}

		return scrollPane;
	}


	/* ======================================================================
	 *                          Internal
	 * ====================================================================== */

	/**
	 * Extracts the parent record of the given sex from the root node,
	 * checking first the node's own individual data and then its partner.
	 * The partner takes precedence, matching the original behaviour.
	 */
	private static FLEFRecord extractParentBySex(final TreeNode rootNode, final SexType sex){
		if(rootNode == null)
			return null;

		FLEFRecord result = null;
		final IndividualData individualData = rootNode.getIndividualData();
		if(individualData != null && individualData.getSex() == sex)
			result = individualData.getIndividual();
		final IndividualData partnerData = rootNode.getPartnerData();
		if(partnerData != null && partnerData.getSex() == sex)
			result = partnerData.getIndividual();
		return result;
	}

}
