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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayoutBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.ComponentOrientation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CoordinateAssigner{

	public static SiblingsPanel populateCanvas(final JPanel canvas, final List<List<Graph.Node>> layers,
			final Map<Graph.Node, List<Long>> ahnentafelMap, final FLEFModel model,
			final Map<TreeNode, PartnersPanel> nodeToPanelMap, final IndividualTreeGraphListener treeListener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory,
			final TreeLayout treeLayout, final boolean showPartner){
		canvas.removeAll();

		final boolean isVertical = (treeLayout == TreeLayout.VERTICAL);
		final String layoutConstraints = (isVertical? "fillx,ins 20,gap 20 40": "filly,ins 20,gap 40 20");
		canvas.setLayout(new MigLayout(layoutConstraints, StringUtils.EMPTY, StringUtils.EMPTY));

		final int totalLayers = layers.size();
		// Generation 0 is the subject (bottom layer), generation L increases upwards
		int generationLevel = totalLayers - 1;
		int maxTheoreticalSlots = 1 << generationLevel;
		int slotColumnSpan = 1;

		TreeNode rootNode = null;
		final Map<String, PartnersPanel> processedParentKeys = new HashMap<>();
		final Set<Long> visibleAhnentafelSlots = new HashSet<>();
		for(int layerIndex = 0; layerIndex < totalLayers; layerIndex ++){
			final List<Graph.Node> layerNodes = layers.get(layerIndex);

			// Track occupied column slots in the grid for this generation layer
			final Set<Integer> occupiedGridSlots = new HashSet<>();
			for(final Graph.Node node : layerNodes){
				if(node.isDummy())
					continue;

				final TreeNode tn = node.getTreeNode();
				if(tn == null)
					// Consanguineous ancestor already placed
					continue;

				if(tn.getGeneration() == 0)
					rootNode = tn;

				final String parentKey = getParentGroupKey(tn);
				if(parentKey != null && processedParentKeys.containsKey(parentKey)){
					final PartnersPanel panel = processedParentKeys.get(parentKey);
					nodeToPanelMap.put(tn, panel);

					continue;
				}

				final int parentLayerIndex = layerIndex - 1;
				if(parentLayerIndex >= 0){
					int minSlot = Integer.MAX_VALUE;
					int maxSlot = Integer.MIN_VALUE;

					for(final Graph.Node siblingNode : layerNodes){
						if(siblingNode.isDummy())
							continue;

						final TreeNode siblingTn = siblingNode.getTreeNode();
						if(siblingTn != null && isSameParentGroup(siblingTn, tn)){
							final List<Long> ahnentafels = ahnentafelMap.getOrDefault(siblingNode, List.of(1l));
							for(final Long ahn : ahnentafels){
								visibleAhnentafelSlots.add(ahn);

								final int slotIndex = (int)(ahn - (1l << generationLevel));
								minSlot = Math.min(minSlot, slotIndex);
								maxSlot = Math.max(maxSlot, slotIndex);
							}
						}
					}

					// Calculate grid column alignment
					final int startCol = minSlot * slotColumnSpan;
					final int colSpan = ((maxSlot - minSlot) + 1) * slotColumnSpan;
					final String cellConstraints = isVertical
						? "cell " + startCol + StringUtils.SPACE + parentLayerIndex + StringUtils.SPACE + colSpan + " 1,align center"
						: "cell " + parentLayerIndex + StringUtils.SPACE + startCol + " 1 " + colSpan + ",align center";

					PartnersPanel panel = nodeToPanelMap.get(tn);
					if(panel == null){
						final BoxPanelType boxType = (tn.getGeneration() == -1? BoxPanelType.PRIMARY: BoxPanelType.SECONDARY);
						panel = TreeLayoutBuilder.createPanelForNode(tn, boxType, treeLayout, model, treeListener,
							popupFactory, true);
						nodeToPanelMap.put(tn, panel);
					}

					canvas.add(panel, cellConstraints);

					if(parentKey != null)
						processedParentKeys.put(parentKey, panel);

					// Mark grid columns as occupied
					for(int s = minSlot; s <= maxSlot; s ++)
						occupiedGridSlots.add(s);
				}
			}

			// Render empty placeholder panels if parent slot is empty
			for(int slot = 0; slot < maxTheoreticalSlots; slot ++)
				if(!occupiedGridSlots.contains(slot)){
					final long currentAhnentafel = (1l << generationLevel) + slot;
					final long childAhnentafel = currentAhnentafel / 2;

					if(visibleAhnentafelSlots.contains(childAhnentafel)){
						visibleAhnentafelSlots.add(currentAhnentafel);

						final int startCol = slot * slotColumnSpan;
						final String cellConstraints = isVertical
							? "cell " + startCol + StringUtils.SPACE + layerIndex + StringUtils.SPACE + slotColumnSpan + " 1,align center"
							: "cell " + layerIndex + StringUtils.SPACE + startCol + " 1 " + slotColumnSpan + ",align center";

						final BoxPanelType boxType = (generationLevel == -1? BoxPanelType.PRIMARY: BoxPanelType.SECONDARY);
						final PartnersPanel emptyPanel = PartnersPanel.createEmpty(boxType, treeLayout);
						canvas.add(emptyPanel, cellConstraints);
					}
				}


			generationLevel --;
			maxTheoreticalSlots >>>= 1;
			slotColumnSpan <<= 1;
		}

		// Extract parent records for children/siblings panel
		final IndividualData individualData = (rootNode != null? rootNode.getIndividualData(): null);
		final IndividualData partnerData = (rootNode != null? rootNode.getPartnerData(): null);
		FLEFRecord father = null;
		FLEFRecord mother = null;
		if(individualData != null){
			if(individualData.getSex() == SexType.MALE)
				father = individualData.getIndividual();
			else if(individualData.getSex() == SexType.FEMALE)
				mother = individualData.getIndividual();
		}
		if(partnerData != null){
			if(partnerData.getSex() == SexType.MALE)
				father = partnerData.getIndividual();
			else if(partnerData.getSex() == SexType.FEMALE)
				mother = partnerData.getIndividual();
		}

		final SiblingsPanel siblingsPanel = SiblingsPanel.create(father, mother, BoxPanelType.SECONDARY, model,
				showPartner, treeLayout)
			.withListener(treeListener, popupFactory);
		if(rootNode != null)
			siblingsPanel.withSiblingsData(rootNode.getBiologicalChildrenData());

		final JScrollPane childrenScrollPane = createChildrenScrollPane(siblingsPanel, treeLayout);
		final int maxGridColumns = 1 << Math.max(totalLayers - 1, 0);
		final String childrenCellConstraints = isVertical
			? "cell 0 " + (totalLayers - 1) + ",span " + maxGridColumns + ",align center,wmax pref"
			: "cell " + (totalLayers - 1) + " 0,span 1 " + maxGridColumns + ",align center,hmax pref";

		canvas.add(childrenScrollPane, childrenCellConstraints);

		canvas.revalidate();
		canvas.repaint();

		return siblingsPanel;
	}

	private static String getParentGroupKey(final TreeNode node){
		final String fatherId = (node.getFather() != null && node.getFather().getIndividualData() != null
			? node.getFather().getIndividualData().getIndividual().getId()
			: "");
		final String motherId = (node.getMother() != null && node.getMother().getIndividualData() != null
			? node.getMother().getIndividualData().getIndividual().getId()
			: "");
		if(fatherId.isEmpty() && motherId.isEmpty())
			return null;
		return fatherId + "_" + motherId;
	}

	private static boolean isSameParentGroup(final TreeNode node1, final TreeNode node2){
		final String key1 = getParentGroupKey(node1);
		final String key2 = getParentGroupKey(node2);
		return key1 != null && key1.equals(key2);
	}

	private static JScrollPane createChildrenScrollPane(final JPanel content, final TreeLayout treeLayout){
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

		// Dynamically compute horizontal scrollbar height without forcing container expansion
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

}
