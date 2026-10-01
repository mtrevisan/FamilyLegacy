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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.IndividualTreeGraphListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayoutBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Places the {@link PartnersPanel}s of a Sugiyama layout on a Swing canvas.
 * <p>
 * The class is a thin orchestrator over four collaborators:
 * <ul>
 *   <li>{@link CoupleGrouper} decides who is paired with whom in each layer;</li>
 *   <li>{@link PartnersPanelFactory} creates the panels and the grandparent wiring;</li>
 *   <li>{@link ParentPanelIndex} keeps the reverse lookup and fills
 *       {@code nodeToPanelMap};</li>
 *   <li>{@link ChildrenStripBuilder} builds the bottom strip.</li>
 * </ul>
 * Each layer is wrapped in its own container and added as a single centered
 * cell to the outer grid, so narrower layers are centered under wider ones
 * instead of being left-aligned.
 * <p>
 * <b>Orientation.</b> In the vertical layout the layers are stacked with
 * the oldest ancestors on top and the children of the root couple at the
 * bottom. In the horizontal layout the same information is rotated 90°
 * counter-clockwise: the children strip is the leftmost column, followed
 * by the root generation, then parents, grandparents and so on to the right.
 * The layer order is therefore reversed for the horizontal case.
 */
public class CoordinateAssigner{

	private static final Logger LOGGER = LoggerFactory.getLogger(CoordinateAssigner.class);


	public static SiblingsPanel populateCanvas(final JPanel canvas, final TreeNode rootNode,
			final List<List<Graph.Node>> layers, final FLEFModel model, final Map<TreeNode, PartnersPanel> nodeToPanelMap,
			final IndividualTreeGraphListener treeListener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory, final TreeLayout treeLayout,
			final boolean showPartner){
		final Map<TreeNode, PartnersPanel> existingPanels = new HashMap<>(nodeToPanelMap);
		canvas.removeAll();

		final boolean isVertical = (treeLayout == TreeLayout.VERTICAL);
		final String layoutConstraints = (isVertical
			? "fillx,ins " + PartnersPanel.GROUP_SEPARATION + ",gap " + PartnersPanel.GROUP_SEPARATION + StringUtils.SPACE + TreeLayoutBuilder.GENERATION_SEPARATOR_SIZE
			: "filly,ins " + PartnersPanel.GROUP_SEPARATION + ",gap " + TreeLayoutBuilder.GENERATION_SEPARATOR_SIZE + StringUtils.SPACE + PartnersPanel.GROUP_SEPARATION);
		canvas.setLayout(new MigLayout(layoutConstraints, StringUtils.EMPTY, StringUtils.EMPTY));

		final Map<String, TreeNode> treeNodeById = buildTreeNodeIndex(layers);
		final List<List<TreeNode[]>> couplesPerLayer = CoupleGrouper.groupAll(layers, treeNodeById);
		// Vertical layout: oldest ancestors on top, root generation at the
		// bottom. Horizontal layout: children of the root couple on the left,
		// oldest ancestors on the right. The layer order is reversed for the
		// horizontal case so the root generation follows the children strip.
		final List<List<TreeNode[]>> couplesInCanvasOrder = (isVertical? couplesPerLayer: reverse(couplesPerLayer));

		final ParentPanelIndex parentIndex = new ParentPanelIndex();
		final PartnersPanelFactory panelFactory = new PartnersPanelFactory(model, treeLayout, treeListener, popupFactory);

		final SiblingsPanel siblingsPanel = ChildrenStripBuilder.build(rootNode, model, showPartner, treeLayout,
			treeListener, popupFactory);
		final boolean hasChildren = ChildrenStripBuilder.hasChildren(rootNode);

		// In vertical layout the children strip goes at the bottom, after
		// the layers; in horizontal layout it goes at the leftmost column,
		// before the layers. gridRow is used as the row index in vertical
		// layout and as the column index in horizontal layout.
		int gridRow = 0;
		if(!isVertical && hasChildren){
			addChildrenStrip(canvas, siblingsPanel, gridRow, isVertical, treeLayout);
			gridRow ++;
		}

		gridRow = buildLayerPanels(canvas, couplesInCanvasOrder, panelFactory, parentIndex, isVertical, gridRow,
			existingPanels, treeLayout);

		if(isVertical && hasChildren)
			addChildrenStrip(canvas, siblingsPanel, gridRow, isVertical, treeLayout);

		parentIndex.fillChildMappings(layers, nodeToPanelMap);
		parentIndex.fillRootMapping(rootNode, nodeToPanelMap);

		canvas.revalidate();
		canvas.repaint();

		return siblingsPanel;
	}


	/* ======================================================================
	 *                          Internal helpers
	 * ====================================================================== */

	private static Map<String, TreeNode> buildTreeNodeIndex(final List<List<Graph.Node>> layers){
		final Map<String, TreeNode> treeNodeById = new HashMap<>();
		for(final List<Graph.Node> layer : layers)
			for(final Graph.Node node : layer)
				if(node.getTreeNode() != null && node.getId() != null)
					treeNodeById.put(node.getId(), node.getTreeNode());
		return treeNodeById;
	}

	/** Returns a new list with the elements of the source in reverse order. */
	private static <T> List<T> reverse(final List<T> source){
		final List<T> reversed = new ArrayList<>(source.size());
		for(int i = source.size() - 1; i >= 0; i --)
			reversed.add(source.get(i));
		return reversed;
	}

	/**
	 * Builds the {@link PartnersPanel}s, wraps each layer in a container so
	 * it can be centered as a block, and adds the containers to the canvas.
	 *
	 * @param startGridRow the row index (vertical) or column index
	 *                     (horizontal) of the first layer to place
	 * @return the first free grid row (or column) after the last non-empty layer
	 */
	private static int buildLayerPanels(final JPanel canvas, final List<List<TreeNode[]>> couplesPerLayer,
			final PartnersPanelFactory panelFactory, final ParentPanelIndex parentIndex, final boolean isVertical,
			final int startGridRow, final Map<TreeNode, PartnersPanel> existingPanels, final TreeLayout treeLayout){
		final String layerLayout = (isVertical? "ins 0,gapx 40,flowx": "ins 0,gapy 40,flowy");
		final Map<String, PartnersPanel> byCoupleKey = new HashMap<>();

		int gridRow = startGridRow;
		final int layerCouples = couplesPerLayer.size();
		for(int layerIndex = 0; layerIndex < layerCouples; layerIndex ++){
			final List<TreeNode[]> couples = couplesPerLayer.get(layerIndex);
			if(couples.isEmpty())
				continue;

			// Wrap the couples of this layer in a container so that the
			// layer is centered as a block. Without this wrapper, layers
			// with fewer couples than the widest one were laid out starting
			// from the leftmost column, breaking the visual alignment with
			// the wider layers.
			final JPanel layerPanel = new JPanel(new MigLayout(layerLayout));
			layerPanel.setOpaque(false);

			for(int i = 0; i < couples.size(); i ++){
				final TreeNode[] couple = couples.get(i);
				final String key = CoupleGrouper.coupleKey(couple[0], couple[1]);

				PartnersPanel panel = byCoupleKey.get(key);
				if(panel == null){
					TreeNode matchNode = (couple[0] != null? couple[0]: couple[1]);
					if(matchNode != null && existingPanels.containsKey(matchNode))
						panel = existingPanels.get(matchNode).withTreeLayout(treeLayout);
					else{
						final int trueLayerIndex = (isVertical? layerIndex: layerCouples - layerIndex - 1);
						panel = panelFactory.create(couple[0], couple[1], trueLayerIndex);
					}
					byCoupleKey.put(key, panel);
					parentIndex.index(panel);
				}

				LOGGER.debug("add partner panel {} to layer {} couple index {}", panel, layerIndex, i);

				layerPanel.add(panel);
			}

			// Add the layer wrapper as a single centered cell in the outer
			// grid. The row index is used only in the vertical orientation;
			// in the horizontal orientation, the same value becomes the
			// column index and rows advance naturally.
			final String cellConstraints = (isVertical
				? "cell 0 " + gridRow + ",align center"
				: "cell " + gridRow + " 0,align center");

			LOGGER.debug("add layer {} wrapper with constraints {}", layerIndex, cellConstraints);

			canvas.add(layerPanel, cellConstraints);

			gridRow ++;
		}
		return gridRow;
	}

	private static void addChildrenStrip(final JPanel canvas, final SiblingsPanel siblingsPanel,
			final int gridRow, final boolean isVertical, final TreeLayout treeLayout){
		final JScrollPane strip = ChildrenStripBuilder.wrap(siblingsPanel, treeLayout);
		final String cellConstraints = (isVertical
			? "cell 0 " + gridRow + ",align center,wmax pref"
			: "cell " + gridRow + " 0,align center,hmax pref");

		LOGGER.debug("add children with constraints {}", cellConstraints);

		canvas.add(strip, cellConstraints);
	}

}
