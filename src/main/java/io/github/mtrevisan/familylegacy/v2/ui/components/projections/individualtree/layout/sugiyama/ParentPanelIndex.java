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

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.TreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Reverse index from a parent individual id to the {@link PartnersPanel}
 * that displays it as a parent, and the routines that use it to fill
 * {@code nodeToPanelMap} (child {@link TreeNode} → panel of its parents).
 * <p>
 * Using an index instead of the couple string key avoids breaking when a
 * panel is created from a partial couple, i.e. a single parent whose
 * partner is not in the same layer.
 */
final class ParentPanelIndex{

	private final Map<String, PartnersPanel> byParentId = new HashMap<>();


	/** Indexes both parents of the panel (up to two entries). */
	void index(final PartnersPanel panel){
		indexParent(panel.getFatherPanel(), panel);
		indexParent(panel.getMotherPanel(), panel);
	}

	/**
	 * Fills {@code nodeToPanelMap} by scanning the layers bottom-to-top
	 * and mapping each child TreeNode to the panel of its parents. Prefers
	 * the father; falls back to the mother when the father is not present
	 * in any panel (e.g., only the mother is loaded).
	 */
	void fillChildMappings(final List<List<Graph.Node>> layers, final Map<TreeNode, PartnersPanel> nodeToPanelMap){
		for(int layerIndex = layers.size() - 1; layerIndex >= 0; layerIndex --){
			final List<Graph.Node> layerNodes = layers.get(layerIndex);
			if(layerNodes == null || layerNodes.isEmpty())
				continue;

			for(final Graph.Node node : layerNodes){
				if(node == null || node.isDummy() || node.getTreeNode() == null)
					continue;

				final PartnersPanel parentPanel = panelFor(node.getTreeNode());
				if(parentPanel != null)
					nodeToPanelMap.put(node.getTreeNode(), parentPanel);
			}
		}
	}

	/**
	 * Maps the given root TreeNode to the panel of the root couple.
	 * <p>
	 * The root may be a synthetic container when {@code showPartner} is
	 * true: it wraps the biological children of the root couple and has
	 * no {@code individualId} of its own. The actual root individuals are
	 * its father and mother. When the root is a plain individual, its own
	 * id is used as a fallback.
	 */
	void fillRootMapping(final TreeNode rootNode, final Map<TreeNode, PartnersPanel> nodeToPanelMap){
		if(rootNode == null)
			return;

		PartnersPanel rootPanel = null;
		if(rootNode.getFather() != null)
			rootPanel = byParentId.get(rootNode.getFather().getIndividualId());
		if(rootPanel == null && rootNode.getMother() != null)
			rootPanel = byParentId.get(rootNode.getMother().getIndividualId());
		if(rootPanel == null && rootNode.getIndividualId() != null)
			rootPanel = byParentId.get(rootNode.getIndividualId());

		if(rootPanel != null)
			nodeToPanelMap.put(rootNode, rootPanel);
	}


	/* ======================================================================
	 *                          Internal
	 * ====================================================================== */

	private void indexParent(final IndividualPanel parentPanel, final PartnersPanel panel){
		if(parentPanel == null)
			return;

		final IndividualData data = parentPanel.getData();
		if(data != null && data.getId() != null)
			byParentId.put(data.getId(), panel);
	}

	private PartnersPanel panelFor(final TreeNode childNode){
		PartnersPanel parentPanel = null;
		if(childNode.getFather() != null)
			parentPanel = byParentId.get(childNode.getFather().getIndividualId());
		if(parentPanel == null && childNode.getMother() != null)
			parentPanel = byParentId.get(childNode.getMother().getIndividualId());
		return parentPanel;
	}

}
