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
package io.github.mtrevisan.familylegacy.v2.ui.components.grouptree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;


/**
 * Utility class responsible for building non-biological group tree dynamic layouts.
 */
public final class GroupIndividualTreeLayoutBuilder{

	public static final int GENERATION_SEPARATOR_SIZE = 16;


	private GroupIndividualTreeLayoutBuilder(){}


	/**
	 * Helper record to hold node layout metadata during BFS/DFS traversal.
	 *
	 * @param dimension	Column index if the genealogical tree is in horizontal, row otherwise.
	 */
	private record LayoutNodeItem(GroupTreeNode node, int depth, int dimension, int span){}

	/**
	 * Result of building the tree layout.
	 *
	 * @param childrenPanel     the children panel
	 */
	public record LayoutResult(SiblingsPanel childrenPanel){}


	/**
	 * Builds the dynamic layout using cell placement for N-ary group structures.
	 *
	 * @param mainPanel      the main layout panel
	 * @param rootNode       the root node of the tree
	 * @param maxGenerations the maximum number of generations to display
	 * @param model          the FLEF model
	 * @param nodeToPanelMap map to store node-to-panel association
	 * @param listener       the individual listener
	 * @param treeLayout     the orientation layout (VERTICAL or HORIZONTAL)
	 * @return a LayoutResult containing the main panel, children panel, and its scroll pane
	 */
	static LayoutResult buildLayout(final JPanel mainPanel, final GroupTreeNode rootNode, final int maxGenerations,
			final FLEFModel model, final Map<GroupTreeNode, JPanel> nodeToPanelMap, final IndividualListener listener,
			final TreeLayout treeLayout){
		final int ancestorLevels = Math.max(1, maxGenerations - 1);
		final int maxDepth = ancestorLevels - 1;
		final int maxLeafUnits = 1 << maxDepth;

		final boolean isVertical = (treeLayout == TreeLayout.VERTICAL);
		final String mainPanelConstraints = (isVertical? "ins 0": "ins 10");
		final String primaryConstraints = buildPrimaryConstraints(maxDepth);
		final String secondaryConstraints = buildSecondaryConstraints(maxLeafUnits);
		if(isVertical)
			mainPanel.setLayout(new MigLayout(mainPanelConstraints, secondaryConstraints, primaryConstraints));
		else
			mainPanel.setLayout(new MigLayout(mainPanelConstraints, primaryConstraints, secondaryConstraints));

		final Deque<LayoutNodeItem> stack = new ArrayDeque<>();
		stack.push(new LayoutNodeItem(rootNode, 0, 0, maxLeafUnits));
		while(!stack.isEmpty()){
			final LayoutNodeItem item = stack.pop();

			final GroupTreeNode node = item.node;
			final int depth = item.depth;
			final int dimension = item.dimension;
			final int span = item.span;

			// PRUNING RULE: Do not render upper empty ancestor slots if node is null
			if(node == null && depth > 0)
				continue;

			// Create a panel for this node (or an empty placeholder if node is null)
			final JPanel nodePanel = createPanelForNode(node,
				(depth == 0? BoxPanelType.PRIMARY: BoxPanelType.SECONDARY), treeLayout, model, listener);

			if(node != null)
				nodeToPanelMap.put(node, nodePanel);

			// Add the panel at the computed cell
			final String cellConstraints = (isVertical
				? "cell " + dimension + " " + (maxDepth - depth) + ",span " + span + ",grow"
				: "cell " + (depth + 1) + " " + dimension + ",span 1 " + span + ",grow");
			mainPanel.add(nodePanel, cellConstraints);

			// Push parents onto stack ONLY if the current node exists
			if(depth < maxDepth && node != null){
				final int nextDepth = depth + 1;
				final int halfSpan = span >> 1;

				final List<GroupTreeNode> parents = node.getParents();
				if(!parents.isEmpty()){
					int currentDim = dimension;
					for(int i = parents.size() - 1; i >= 0; i --){
						final GroupTreeNode parentNode = parents.get(i);
						stack.push(new LayoutNodeItem(parentNode, nextDepth, currentDim, halfSpan));
						currentDim += halfSpan;
					}
				}
			}
		}

//		// Extract root parent records for children panel
//		final FLEFRecord father = (rootNode != null && rootNode.getFather() != null
//			? rootNode.getFather().getIndividual()
//			: null);
//		final FLEFRecord mother = (rootNode != null && rootNode.getMother() != null
//			? rootNode.getMother().getIndividual()
//			: null);
//		if(rootNode != null && !showPartner){
//			final SiblingsData rootSiblingsData = SiblingsData.createSingleChild(rootNode.getIndividualData());
//			rootNode.setPartnerAndBiologicalChildren(null, null, rootSiblingsData);
//		}
//
//		// Add children (below/left)
//		final SiblingsPanel childrenPanel = createChildrenPanel(father, mother, model, listener, rootNode, showPartner,
//			treeLayout);
//		final JScrollPane childrenScrollPane = createChildrenScrollPane(childrenPanel, treeLayout);
//		final String childrenCellConstraints = (isVertical
//			? "cell 0 " + (maxDepth + 1) + ",span " + maxLeafUnits + ",center"
//			: "cell 0 0,span 1 " + maxLeafUnits + ",growy,center");
//		mainPanel.add(childrenScrollPane, childrenCellConstraints);
//
//		return new LayoutResult(childrenPanel);
		return new LayoutResult(null);
	}

	private static String buildPrimaryConstraints(final int maxDepth){
		final StringBuilder constraints = new StringBuilder();
		for(int i = 0; i < maxDepth; i ++){
			if(i > 0)
				constraints.append(GENERATION_SEPARATOR_SIZE);
			constraints.append("[]");
		}

//		// children row
//		constraints.append(GENERATION_SEPARATOR_SIZE)
//			.append("[]");
		return constraints.toString();
	}

	private static String buildSecondaryConstraints(final int maxLeafUnits){
		final StringBuilder constraints = new StringBuilder();
		for(int i = 0; i < maxLeafUnits; i ++){
			if(i > 0)
				constraints.append(PartnersPanel.GROUP_SEPARATION);
			constraints.append("[grow,center]");
		}
		return constraints.toString();
	}

	private static JPanel createPanelForNode(final GroupTreeNode node, final BoxPanelType type,
			final TreeLayout treeLayout, final FLEFModel model, final IndividualListener listener){
		final PartnersPanel panel = PartnersPanel.create(type, treeLayout, model)
			.withListener(listener);
		panel.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					if(listener != null && node.getRecord() != null)
						listener.onIndividualSelected(node.getRecord());
				}
			}
		});

		if(node != null){
			//FIXME
			final IndividualData nodeData = IndividualData.create(node.getRecord(), null, model);
			panel.withBiologicalParents(nodeData, null);

//			final AncestorNode fatherNode = node.getFather();
//			final AncestorNode motherNode = node.getMother();
//			final AncestorNode fatherFather = (fatherNode != null? fatherNode.getFather(): null);
//			final AncestorNode fatherMother = (fatherNode != null? fatherNode.getMother(): null);
//			final AncestorNode motherFather = (motherNode != null? motherNode.getFather(): null);
//			final AncestorNode motherMother = (motherNode != null? motherNode.getMother(): null);
//
//			panel.getFatherPanel()
//				.withParent(
//					(fatherFather != null? fatherFather.getIndividual(): null),
//					(fatherMother != null? fatherMother.getIndividual(): null));
//			panel.getMotherPanel()
//				.withParent(
//					(motherFather != null? motherFather.getIndividual(): null),
//					(motherMother != null? motherMother.getIndividual(): null));
//
//			final AncestorNode father = node.getFather();
//			final AncestorNode mother = node.getMother();
//			final IndividualData fatherData = (father != null? father.getIndividualData(): null);
//			final IndividualData motherData = (mother != null? mother.getIndividualData(): null);
//			panel.withBiologicalParents(fatherData, motherData);
		}

		return panel;
	}

//	private static SiblingsPanel createChildrenPanel(final FLEFRecord father, final FLEFRecord mother,
//		final FLEFModel model, final IndividualListener listener, final AncestorNode rootNode,
//		final boolean showPartner, final TreeLayout treeLayout){
//		final SiblingsPanel panel = SiblingsPanel.create(father, mother, BoxPanelType.SECONDARY, model, showPartner,
//				treeLayout)
//			.withListener(listener);
//		if(rootNode != null)
//			panel.withSiblingsData(rootNode.getBiologicalChildrenData());
//		return panel;
//	}

//	private static JScrollPane createChildrenScrollPane(final JPanel content, final TreeLayout treeLayout){
//		final JScrollPane scrollPane = new JScrollPane(content);
//		scrollPane.setOpaque(false);
//		scrollPane.getViewport()
//			.setOpaque(false);
//		scrollPane.setBorder(null);
//		scrollPane.setVerticalScrollBarPolicy(treeLayout == TreeLayout.VERTICAL
//			? ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
//			: ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
//		scrollPane.setHorizontalScrollBarPolicy(treeLayout == TreeLayout.VERTICAL
//			? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
//			: ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//
//		// Dynamically compute horizontal scrollbar height to avoid overlapping children panels
//		if(treeLayout == TreeLayout.VERTICAL){
//			final JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
//			final int scrollBarHeight = scrollBar.getPreferredSize()
//				.height;
//			content.setBorder(BorderFactory.createEmptyBorder(0, 0, scrollBarHeight, 0));
//		}
//		else{
//			scrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
//			final JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
//			final int scrollBarWidth = scrollBar.getPreferredSize()
//				.width;
//			content.setBorder(BorderFactory.createEmptyBorder(0, scrollBarWidth, 0, 0));
//		}
//
//		return scrollPane;
//	}

}
