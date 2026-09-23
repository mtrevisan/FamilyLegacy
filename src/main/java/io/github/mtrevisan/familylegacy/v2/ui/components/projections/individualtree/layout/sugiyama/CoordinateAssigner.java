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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.ComponentOrientation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CoordinateAssigner{

	private static final Logger LOGGER = LoggerFactory.getLogger(CoordinateAssigner.class);


	public static SiblingsPanel populateCanvas(final JPanel canvas, final List<List<Graph.Node>> layers,
			final FLEFModel model, final Map<TreeNode, PartnersPanel> nodeToPanelMap,
			final IndividualTreeGraphListener treeListener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory, final TreeLayout treeLayout,
			final boolean showPartner){
		canvas.removeAll();

		final boolean isVertical = (treeLayout == TreeLayout.VERTICAL);
		final String layoutConstraints = (isVertical? "fillx,ins 20,gap 20 40": "filly,ins 20,gap 40 20");
		canvas.setLayout(new MigLayout("debug," + layoutConstraints, StringUtils.EMPTY, StringUtils.EMPTY));

		final int totalLayers = layers.size();

		// Calculate max pair capacity based on the widest layer
		int maxPairCapacity = 1;
		for(final List<Graph.Node> layer : layers)
			if(layer != null){
				final int pairCount = Math.max(1, (layer.size() + 1) / 2);
				if(pairCount > maxPairCapacity)
					maxPairCapacity = pairCount;
			}
		final int maxGridColumns = maxPairCapacity;

		TreeNode rootNode = null;
		final Map<String, PartnersPanel> processedParentKeys = new HashMap<>();
		int gridRow = 0;
		// 1. Pass: Build PartnersPanels, place on canvas grid, and populate processedParentKeys
		for(int layerIndex = 0; layerIndex < totalLayers; layerIndex ++){
			final List<Graph.Node> layerNodes = layers.get(layerIndex);
			if(layerNodes == null || layerNodes.isEmpty())
				continue;

			for(final Graph.Node node : layerNodes){
				if(node.isDummy())
					continue;

				final TreeNode treeNode = node.getTreeNode();
				if(treeNode != null && treeNode.getGeneration() == 0){
					rootNode = treeNode;

					break;
				}
			}

			final int rawCount = layerNodes.size();
			final int pairCount = Math.max(1, (rawCount + 1) / 2);
			final int slotWidth = Math.max(1, maxGridColumns / pairCount);
			boolean rowHasPanels = false;
			for(int i = 0; i < rawCount; i += 2){
				final Graph.Node node1 = layerNodes.get(i);
				final Graph.Node node2 = (i + 1 < rawCount? layerNodes.get(i + 1): null);

				final TreeNode tn1 = (!node1.isDummy()? node1.getTreeNode(): null);
				final TreeNode tn2 = (node2 != null && !node2.isDummy()? node2.getTreeNode(): null);
				if(tn1 == null && tn2 == null)
					continue;

				// Identify father (Male) and mother (Female) explicitly
				TreeNode fatherTn = null;
				TreeNode motherTn = null;
				if(tn1 != null && tn1.getIndividualData() != null){
					if(tn1.getIndividualData().getSex() == SexType.MALE)
						fatherTn = tn1;
					else if(tn1.getIndividualData().getSex() == SexType.FEMALE)
						motherTn = tn1;
				}
				if(tn2 != null && tn2.getIndividualData() != null){
					if(tn2.getIndividualData().getSex() == SexType.MALE)
						fatherTn = tn2;
					else if(tn2.getIndividualData().getSex() == SexType.FEMALE)
						motherTn = tn2;
				}

				final String parentKey = getParentGroupKey(fatherTn, motherTn);

				PartnersPanel panel = processedParentKeys.get(parentKey);
				if(panel == null){
					final BoxPanelType boxType = (fatherTn != null && fatherTn.getGeneration() == 0
							|| motherTn != null && motherTn.getGeneration() == 0
						? BoxPanelType.PRIMARY
						: BoxPanelType.SECONDARY);

					final IndividualData fatherData = (fatherTn != null? fatherTn.getIndividualData(): null);
					final IndividualData motherData = (motherTn != null? motherTn.getIndividualData(): null);

					// Create panel for this pair (this is the Parents Panel for their children)
					panel = PartnersPanel.create(boxType, treeLayout, model)
						.withBiologicalParents(fatherData, motherData)
						.withListener(treeListener, popupFactory)
						.withSuppressCollapseBadge(true);

					// Wire upper connections to grandparents
					final TreeNode fatherFather = (fatherTn != null? fatherTn.getFather(): null);
					final TreeNode fatherMother = (fatherTn != null? fatherTn.getMother(): null);
					final TreeNode motherFather = (motherTn != null? motherTn.getFather(): null);
					final TreeNode motherMother = (motherTn != null? motherTn.getMother(): null);
					panel.getFatherPanel().withParent(
						(fatherFather != null? fatherFather.getIndividual(): null),
						(fatherMother != null? fatherMother.getIndividual(): null)
					);
					panel.getMotherPanel().withParent(
						(motherFather != null? motherFather.getIndividual(): null),
						(motherMother != null? motherMother.getIndividual(): null)
					);

					processedParentKeys.put(parentKey, panel);
				}

				final int pairIndex = i / 2;
				final int startCol = pairIndex * slotWidth;
				final String cellConstraints = isVertical
					? "cell " + startCol + StringUtils.SPACE + gridRow + StringUtils.SPACE + slotWidth + " 1,align center"
					: "cell " + gridRow + StringUtils.SPACE + startCol + " 1 " + slotWidth + ",align center";

				LOGGER.debug("add partner panel {} with constraints {}, layer {}, gridRow {}, pairIndex {}",
					panel.toString(), cellConstraints, layerIndex, gridRow, pairIndex);

				canvas.add(panel, cellConstraints);

				rowHasPanels = true;
			}

			if(rowHasPanels)
				gridRow ++;
		}

		// 2. Pass: Scan layers backwards (bottom to top) to populate nodeToPanelMap (Key: Child TreeNode -> Value: Parents PartnersPanel)
		for(int layerIndex = totalLayers - 1; layerIndex >= 0; layerIndex --){
			final List<Graph.Node> layerNodes = layers.get(layerIndex);
			if(layerNodes == null || layerNodes.isEmpty())
				continue;

			for(final Graph.Node node : layerNodes){
				if(node == null || node.isDummy() || node.getTreeNode() == null)
					continue;

				final TreeNode childNode = node.getTreeNode();
				final String parentGroupKey = getParentGroupKey(childNode.getFather(), childNode.getMother());
				final PartnersPanel parentPanel = processedParentKeys.get(parentGroupKey);
				if(parentPanel != null)
					nodeToPanelMap.put(childNode, parentPanel);
			}
		}

		//TODO ?
//		final TreeNode rootIndividualNode = layers.getLast()
//			.getFirst()
//			.getTreeNode();
//		final TreeNode rootPartnerNode = layers.getLast()
//			.getLast()
//			.getTreeNode();
//		final TreeNode rootNode2 = new TreeNode(rootIndividualNode.getBiologicalChildrenData());
//		final String parentGroupKey = getParentGroupKey(rootIndividualNode, rootPartnerNode);
//		final PartnersPanel parentPanel = processedParentKeys.get(parentGroupKey);
//		if(parentPanel != null)
//			nodeToPanelMap.put(rootNode2, parentPanel);

		if(rootNode == null)
			for(final List<Graph.Node> layer : layers){
				for(final Graph.Node node : layer)
					if(!node.isDummy() && node.getTreeNode() != null && node.getTreeNode().getGeneration() == 0){
						rootNode = node.getTreeNode();

						break;
					}
				if(rootNode != null)
					break;
			}

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

		boolean hasChildren = false;
		if(rootNode != null){
			siblingsPanel.withSiblingsData(rootNode.getBiologicalChildrenData());
			final List<IndividualData> children = (rootNode.getBiologicalChildrenData() != null
				? rootNode.getBiologicalChildrenData().getSiblings()
				: null);
			hasChildren = (children != null && !children.isEmpty());
		}

		if(hasChildren){
			final JScrollPane childrenScrollPane = createChildrenScrollPane(siblingsPanel, treeLayout);
			final String cellConstraints = (isVertical
				? "cell 0 " + gridRow + ",span " + maxGridColumns + ",align center,wmax pref"
				: "cell " + gridRow + " 0,span 1 " + maxGridColumns + ",align center,hmax pref");

			LOGGER.debug("add children with constraints {}", cellConstraints);

			canvas.add(childrenScrollPane, cellConstraints);
		}

		canvas.revalidate();
		canvas.repaint();

		return siblingsPanel;
	}

	private static String getParentGroupKey(final TreeNode fatherNode, final TreeNode motherNode){
		return (fatherNode != null? fatherNode.getIndividualId(): StringUtils.EMPTY)
			+ "+"
			+ (motherNode != null? motherNode.getIndividualId(): StringUtils.EMPTY);
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