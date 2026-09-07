package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsData;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.ComponentOrientation;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;


/**
 * Utility class responsible for building the genealogical tree layout.
 */
public final class TreeLayoutBuilder{

	public static final int GENERATION_SEPARATOR_SIZE = 16;


	private TreeLayoutBuilder(){}


	/**
	 * Helper record to hold node layout metadata during BFS traversal.
	 *
	 * @param dimension	Column index if the genealogical tree is in horizontal, row otherwise.
	 */
	private record LayoutNodeItem(AncestorNode node, int depth, int dimension, int span){}

	/**
	 * Result of building the tree layout.
	 *
	 * @param childrenPanel     the children panel
	 */
	public record LayoutResult(SiblingsPanel childrenPanel){}


	/**
	 * Builds the dynamic horizontal layout using cell placement.
	 * Each node represents a couple (individual + partner).
	 *
	 * @param mainPanel      the main layout panel
	 * @param rootNode       the root node of the tree
	 * @param maxGenerations the maximum number of generations to display
	 * @param model          the FLEF model
	 * @param nodeToPanelMap       map to store the node-to-panel association (will be populated)
	 * @param listener       the individual listener
	 * @param mutator        the tree mutator for navigation
	 * @return a LayoutResult containing the main panel, children panel, and its scroll pane
	 */
	static LayoutResult buildLayout(final JPanel mainPanel, final AncestorNode rootNode, final boolean showPartner,
			final int maxGenerations, final FLEFModel model, final Map<AncestorNode, PartnersPanel> nodeToPanelMap,
			final IndividualListener listener, final AncestorTreeMutator mutator, final TreeLayout treeLayout){
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

			final AncestorNode node = item.node;
			final int depth = item.depth;
			final int dimension = item.dimension;
			final int span = item.span;

			// PRUNING RULE: Do not render upper empty ancestor slots if node is null
			if(node == null && depth > 0)
				continue;

			// Create a panel for this node (or an empty placeholder if node is null)
			final PartnersPanel partnerPanel = createPanelForNode(node,
				(depth == 0? BoxPanelType.PRIMARY: BoxPanelType.SECONDARY), treeLayout, model, listener, mutator);

			if(node != null)
				nodeToPanelMap.put(node, partnerPanel);

			// Add the panel at the computed cell
			final String cellConstraints = (isVertical
				? "cell " + dimension + " " + (maxDepth - depth) + ",span " + span + ",grow"
				: "cell " + (depth + 1) + " " + dimension + ",span 1 " + span + ",grow");
			mainPanel.add(partnerPanel, cellConstraints);

			// Push parents onto stack ONLY if the current node exists
			// Push MOTHER first, then FATHER, so that FATHER is processed first (LIFO order).
			if(depth < maxDepth && node != null){
				final int nextDepth = depth + 1;
				final int halfSpan = span >> 1;

				final AncestorNode motherNode = node.getMother();
				stack.push(new LayoutNodeItem(motherNode, nextDepth, dimension + halfSpan, halfSpan));

				final AncestorNode fatherNode = node.getFather();
				stack.push(new LayoutNodeItem(fatherNode, nextDepth, dimension, halfSpan));
			}
		}

		// Extract root parent records for children panel
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

		// Add children (below/left)
		final SiblingsPanel childrenPanel = createChildrenPanel(father, mother, model, listener, rootNode, showPartner,
			treeLayout);
		final JScrollPane childrenScrollPane = createChildrenScrollPane(childrenPanel, treeLayout);
		final String childrenCellConstraints = (isVertical
			? "cell 0 " + (maxDepth + 1) + ",span " + maxLeafUnits + ",center"
			: "cell 0 0,span 1 " + maxLeafUnits + ",growy,center");
		mainPanel.add(childrenScrollPane, childrenCellConstraints);

		return new LayoutResult(childrenPanel);
	}

	private static String buildPrimaryConstraints(final int maxDepth){
		final StringBuilder secondaryConstraints = new StringBuilder();
		for(int i = 0; i <= maxDepth; i ++){
			if(i > 0)
				secondaryConstraints.append(GENERATION_SEPARATOR_SIZE);
			secondaryConstraints.append("[]");
		}

		// children row
		secondaryConstraints.append(GENERATION_SEPARATOR_SIZE)
			.append("[]");
		return secondaryConstraints.toString();
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

	private static PartnersPanel createPanelForNode(final AncestorNode node, final BoxPanelType type,
			final TreeLayout treeLayout, final FLEFModel model, final IndividualListener listener, final AncestorTreeMutator mutator){
		final PartnersPanel panel = PartnersPanel.create(type, treeLayout, model)
			.withListener(listener);
		panel.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				final String clickedId = node.getIndividualId();
				if(clickedId != null)
					mutator.navigateToRoot(clickedId);
			}
		});

		if(node != null){
			final AncestorNode fatherNode = node.getFather();
			final AncestorNode motherNode = node.getMother();
			final AncestorNode fatherFather = (fatherNode != null? fatherNode.getFather(): null);
			final AncestorNode fatherMother = (fatherNode != null? fatherNode.getMother(): null);
			final AncestorNode motherFather = (motherNode != null? motherNode.getFather(): null);
			final AncestorNode motherMother = (motherNode != null? motherNode.getMother(): null);

			panel.getFatherPanel()
				.withParent(
					(fatherFather != null? fatherFather.getIndividual(): null),
					(fatherMother != null? fatherMother.getIndividual(): null));
			panel.getMotherPanel()
				.withParent(
					(motherFather != null? motherFather.getIndividual(): null),
					(motherMother != null? motherMother.getIndividual(): null));

			final AncestorNode father = node.getFather();
			final AncestorNode mother = node.getMother();
			final IndividualData fatherData = (father != null? father.getIndividualData(): null);
			final IndividualData motherData = (mother != null? mother.getIndividualData(): null);
			panel.withBiologicalParents(fatherData, motherData);
		}

		return panel;
	}

	private static SiblingsPanel createChildrenPanel(final FLEFRecord father, final FLEFRecord mother,
			final FLEFModel model, final IndividualListener listener, final AncestorNode rootNode,
			final boolean showPartner, final TreeLayout treeLayout){
		final SiblingsPanel panel = SiblingsPanel.create(father, mother, BoxPanelType.SECONDARY, model, showPartner,
				treeLayout)
			.withListener(listener);
		if(rootNode != null)
			panel.withSiblingsData(rootNode.getBiologicalChildrenData());
		return panel;
	}

	private static JScrollPane createChildrenScrollPane(final JPanel content, final TreeLayout treeLayout){
		final JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setOpaque(false);
		scrollPane.getViewport()
			.setOpaque(false);
		scrollPane.setBorder(null);
		scrollPane.setVerticalScrollBarPolicy(treeLayout == TreeLayout.VERTICAL
			? ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
			: ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(treeLayout == TreeLayout.VERTICAL
			? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
			: ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		// Dynamically compute horizontal scrollbar height to avoid overlapping children panels
		if(treeLayout == TreeLayout.VERTICAL){
			final JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
			final int scrollBarHeight = scrollBar.getPreferredSize()
				.height;
			content.setBorder(BorderFactory.createEmptyBorder(0, 0, scrollBarHeight, 0));
		}
		else{
			scrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
			final JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
			final int scrollBarWidth = scrollBar.getPreferredSize()
				.width;
			content.setBorder(BorderFactory.createEmptyBorder(0, scrollBarWidth, 0, 0));
		}

		return scrollPane;
	}

}
