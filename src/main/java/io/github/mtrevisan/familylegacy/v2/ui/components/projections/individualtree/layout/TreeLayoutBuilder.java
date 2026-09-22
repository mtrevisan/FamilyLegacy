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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.ComponentOrientation;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;


/**
 * Utility class responsible for building the genealogical tree layout.
 * <p>
 * The layout reserves one cell per ancestor slot, from the root down to
 * the configured maximum number of generations. Every cell is filled with
 * a {@link PartnersPanel}: a real one when a {@link TreeNode} exists, or a
 * placeholder one when it does not.
 * <p>
 * The placeholder panels exist to guarantee that every generation row has
 * at least one panel of the same size as the real panels. Without them, a
 * row with no ancestors would collapse to zero height, shifting the root
 * individual upward. By filling every slot, the row height is always the
 * panel height and the root stays at the same vertical position
 * regardless of how many generations of ancestors are actually present in
 * the data.
 * <p>
 * The children slot (below the root in vertical layout, to the left in
 * horizontal layout) is intentionally left at its natural size: it does
 * not influence the position of the root individual and may legitimately
 * grow to fit its content (e.g. a large number of siblings).
 */
public final class TreeLayoutBuilder{

	public static final int GENERATION_SEPARATOR_SIZE = 16;


	private TreeLayoutBuilder(){}


	/**
	 * Helper record to hold node layout metadata during BFS traversal.
	 *
	 * @param dimension	Column index if the genealogical tree is in horizontal, row otherwise.
	 */
	private record LayoutNodeItem(TreeNode node, int depth, int dimension, int span){}


	/**
	 * Builds the dynamic layout using cell placement.
	 * Each node represents a couple (individual + partner).
	 *
	 * @param mainPanel      the main layout panel
	 * @param rootNode       the root node of the tree
	 * @param showPartner    whether to show partner details
	 * @param maxAncestors   the maximum number of generations to display
	 * @param model          the FLEF model
	 * @param nodeToPanelMap map to store the node-to-panel association (will be populated)
	 * @param listener       the individual listener
	 * @param popupFactory   the popup menu factory
	 * @param treeLayout     the tree orientation layout
	 * @return a LayoutResult containing the children panel
	 */
	static SiblingsPanel buildLayout(final JPanel mainPanel, final TreeNode rootNode, final boolean showPartner,
			final int maxAncestors, final FLEFModel model, final Map<TreeNode, PartnersPanel> nodeToPanelMap,
			final IndividualListener listener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory, final TreeLayout treeLayout){
		final int maxDepth = (rootNode != null? calculateSubtreeMaxDepth(rootNode, maxAncestors): 0);
		// Calculate total leaf units dynamically according to present ancestors
		final int maxLeafUnits = (rootNode != null? calculateSubtreeLeafUnits(rootNode, 0, maxDepth): 1);

		final boolean isVertical = (treeLayout == TreeLayout.VERTICAL);
		final String mainPanelConstraints = (isVertical? "debug,ins 0": "debug,ins 10");
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

			final TreeNode node = item.node;
			final int depth = item.depth;
			final int dimension = item.dimension;
			final int span = item.span;

			if(node == null)
				continue;

			// Assign full span (maxLeafUnits) to root depth (depth 0) to keep parents centered,
			// otherwise use the calculated span for ancestor generations
			final String cellConstraints = (isVertical
				? "cell " + dimension + StringUtils.SPACE + (maxDepth - depth) + ",span " + span + " 1,align center"
				: "cell " + (depth + 1) + StringUtils.SPACE + dimension + ",span 1 " + span + ",align center");

			// Create the panel for this slot
			final BoxPanelType boxPanelType = (depth == 0? BoxPanelType.PRIMARY: BoxPanelType.SECONDARY);
			final PartnersPanel partnerPanel = createPanelForNode(node, boxPanelType, treeLayout, model, listener,
				popupFactory, false);

			nodeToPanelMap.put(node, partnerPanel);

			// Add the panel at the computed cell
			mainPanel.add(partnerPanel, cellConstraints);

			// Expand only existing parent nodes
			if(depth < maxDepth){
				final int nextDepth = depth + 1;
				final int halfSpan = span >> 1;

				// Push MOTHER first, then FATHER, so that FATHER is processed first (LIFO order)
				final TreeNode motherNode = node.getMother();
				stack.push(new LayoutNodeItem(motherNode, nextDepth, dimension + halfSpan, halfSpan));

				final TreeNode fatherNode = node.getFather();
				stack.push(new LayoutNodeItem(fatherNode, nextDepth, dimension, halfSpan));
			}
		}

		// Extract root parent records for the children panel
		final FLEFRecord father = (rootNode != null && rootNode.getFather() != null
			? rootNode.getFather().getIndividual()
			: null);
		final FLEFRecord mother = (rootNode != null && rootNode.getMother() != null
			? rootNode.getMother().getIndividual()
			: null);
		if(rootNode != null && !showPartner){
			final SiblingsData rootSiblingsData = SiblingsData.createSingleChild(rootNode.getIndividualData());
			rootNode.setPartnerAndBiologicalChildren(null, null, rootSiblingsData);
		}

		// Add the children panel at the end of the primary axis
		final SiblingsPanel childrenPanel = createChildrenPanel(father, mother, model, listener, popupFactory, rootNode,
			showPartner, treeLayout);
		final JScrollPane childrenScrollPane = createChildrenScrollPane(childrenPanel, treeLayout);
		final String childrenCellConstraints = (isVertical
			? "cell 0 " + (maxDepth + 1) + ",span " + maxLeafUnits + ",align center,wmax pref"
			: "cell 0 0,span 1 " + maxLeafUnits + ",align center,hmax pref");
		mainPanel.add(childrenScrollPane, childrenCellConstraints);

		return childrenPanel;
	}

	/**
	 * Calculates the maximum actual depth of the subtree up to {@code maxAncestors}.
	 *
	 * @param rootNode     the root node of the subtree
	 * @param maxAncestors the maximum configured generations
	 * @return actual maximum depth present in the subtree (0 if only root exists)
	 */
	private static int calculateSubtreeMaxDepth(final TreeNode rootNode, final int maxAncestors){
		if(rootNode == null)
			return 0;

		final Deque<TreeNode> stack = new ArrayDeque<>();
		final Map<TreeNode, Integer> depthMap = new HashMap<>();

		stack.push(rootNode);
		depthMap.put(rootNode, 0);

		int maxDepth = 0;

		while(!stack.isEmpty()){
			final TreeNode current = stack.pop();
			final int currentDepth = depthMap.get(current);

			if(currentDepth > maxDepth)
				maxDepth = currentDepth;

			if(currentDepth < maxAncestors){
				final TreeNode father = current.getFather();
				if(father != null){
					stack.push(father);
					depthMap.put(father, currentDepth + 1);
				}

				final TreeNode mother = current.getMother();
				if(mother != null){
					stack.push(mother);
					depthMap.put(mother, currentDepth + 1);
				}
			}
		}

		return maxDepth;
	}

	/**
	 * Calculates the number of leaf units occupied by a subtree up to {@code maxDepth}.
	 *
	 * @param rootNode     the root node of the subtree
	 * @param startDepth   the starting depth in the tree
	 * @param maxDepth     the maximum depth to inspect
	 * @return total leaf units count for the subtree
	 */
	private static int calculateSubtreeLeafUnits(final TreeNode rootNode, final int startDepth, final int maxDepth){
		if(rootNode == null)
			return 0;

		final Deque<TreeNode> stack = new ArrayDeque<>();
		final Map<TreeNode, Integer> leafUnitsMap = new HashMap<>();

		// Helper stack to process nodes in post-order (children before parents)
		final Deque<TreeNode> postOrderStack = new ArrayDeque<>();
		final Map<TreeNode, Integer> depthMap = new HashMap<>();
		stack.push(rootNode);
		depthMap.put(rootNode, startDepth);
		while(!stack.isEmpty()){
			final TreeNode current = stack.pop();
			final int currentDepth = depthMap.get(current);

			postOrderStack.push(current);

			if(currentDepth < maxDepth){
				final TreeNode father = current.getFather();
				if(father != null){
					stack.push(father);
					depthMap.put(father, currentDepth + 1);
				}

				final TreeNode mother = current.getMother();
				if(mother != null){
					stack.push(mother);
					depthMap.put(mother, currentDepth + 1);
				}
			}
		}

		while(!postOrderStack.isEmpty()){
			final TreeNode current = postOrderStack.pop();
			final int currentDepth = depthMap.get(current);

			if(currentDepth >= maxDepth){
				leafUnitsMap.put(current, 1);

				continue;
			}

			final int fatherUnits = leafUnitsMap.getOrDefault(current.getFather(), 0);
			final int motherUnits = leafUnitsMap.getOrDefault(current.getMother(), 0);
			final int total = fatherUnits + motherUnits;

			leafUnitsMap.put(current, total == 0? 1: total);
		}

		return leafUnitsMap.getOrDefault(rootNode, 0);
	}

	/**
	 * Builds the constraint string for the primary axis (generation axis):
	 * rows in vertical layout, columns in horizontal layout.
	 * <p>
	 * Every ancestor slot uses the plain {@code []} constraint (natural
	 * size), because every cell is guaranteed to contain a panel. The
	 * row height is therefore always equal to the panel height, and the
	 * root stays at the same vertical position regardless of how many
	 * generations of ancestors are present.
	 * <p>
	 * The children slot is also natural: it does not influence the
	 * position of the root and may legitimately grow to fit its content.
	 *
	 * @param maxDepth the maximum ancestor depth (0 = root only)
	 * @return the constraint string
	 */
	private static String buildPrimaryConstraints(final int maxDepth){
		final StringBuilder constraints = new StringBuilder();
		for(int i = 0; i <= maxDepth; i ++){
			if(i > 0)
				constraints.append(GENERATION_SEPARATOR_SIZE);
			constraints.append("[]");
		}

		// Append children row constraint without trailing separator
		constraints.append(GENERATION_SEPARATOR_SIZE)
			.append("[]");
		return constraints.toString();
	}

	/**
	 * Builds secondary constraints ensuring uniform column/row sizes using Size Groups (sg col).
	 *
	 * @param totalLeafUnits the total number of allocated leaf units
	 * @return the secondary constraints string
	 */
	private static String buildSecondaryConstraints(final int totalLeafUnits){
		final StringBuilder constraints = new StringBuilder();
		for(int i = 0; i < totalLeafUnits; i ++){
			if(i > 0)
				constraints.append(PartnersPanel.GROUP_SEPARATION);
			constraints.append("[grow,center]");
		}
		return constraints.toString();
	}

	private static PartnersPanel createEmptyPanelForNode(final BoxPanelType type, final TreeLayout treeLayout){
		return PartnersPanel.createEmpty(type, treeLayout);
	}

	public static PartnersPanel createPanelForNode(final TreeNode node, final BoxPanelType type,
			final TreeLayout treeLayout, final FLEFModel model, final IndividualListener listener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory,
			final boolean suppressCollapseBadge){
		final PartnersPanel panel = PartnersPanel.create(type, treeLayout, model)
			.withListener(listener, popupFactory)
			.withSuppressCollapseBadge(suppressCollapseBadge);

		if(node != null){
			final TreeNode father = node.getFather();
			final TreeNode mother = node.getMother();
			final TreeNode fatherFather = (father != null? father.getFather(): null);
			final TreeNode fatherMother = (father != null? father.getMother(): null);
			final TreeNode motherFather = (mother != null? mother.getFather(): null);
			final TreeNode motherMother = (mother != null? mother.getMother(): null);

			panel.getFatherPanel()
				.withParent(
					(fatherFather != null? fatherFather.getIndividual(): null),
					(fatherMother != null? fatherMother.getIndividual(): null));
			panel.getMotherPanel()
				.withParent(
					(motherFather != null? motherFather.getIndividual(): null),
					(motherMother != null? motherMother.getIndividual(): null));

			final IndividualData fatherData = (father != null? father.getIndividualData(): null);
			final IndividualData motherData = (mother != null? mother.getIndividualData(): null);
			panel.withBiologicalParents(fatherData, motherData);
		}

		return panel;
	}

	private static SiblingsPanel createChildrenPanel(final FLEFRecord father, final FLEFRecord mother,
			final FLEFModel model, final IndividualListener listener,
			final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory, final TreeNode rootNode,
			final boolean showPartner, final TreeLayout treeLayout){
		final SiblingsPanel panel = SiblingsPanel.create(father, mother, BoxPanelType.SECONDARY, model, showPartner,
				treeLayout)
			.withListener(listener, popupFactory);
		if(rootNode != null)
			panel.withSiblingsData(rootNode.getBiologicalChildrenData());
		return panel;
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
