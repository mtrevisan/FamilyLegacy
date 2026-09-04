package io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.Side;
import io.github.mtrevisan.familylegacy.v2.ui.components.siblings.SiblingsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Function;


/**
 * Generalized panel responsible for rendering an N-generation genealogical tree layout dynamically.
 */
public class BiologicalTreePanel extends JPanel implements BiologicalTreeChangeListener, IndividualListener{

	@Serial
	private static final long serialVersionUID = 9011391311012465249L;


	private static final Logger LOGGER = LoggerFactory.getLogger(BiologicalTreePanel.class);


	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);
	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;

	private static final int GENERATION_SEPARATOR_SIZE = 36;

	private static final String TAG_TYPE = "type";
	private static final String TAG_TARGET = "target";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_SEX = "sex";

	private static final String ENUM_TYPE_ENDS_WITH_CHILD = "child";
	private static final String ENUM_SEX_FEMALE = "female";


	/**
	 * Helper record to hold node layout metadata during BFS traversal.
	 */
	private record LayoutNodeItem(AncestorNode node, int depth, int col, int span){}


	private final FLEFModel model;
	private final BiologicalTreeService treeService;
	private final AncestorTreeMutator treeMutator;

	private String currentRootIndividualId;
	private AncestorNode rootNode;
	private int currentMaxGenerations;

	// Map associating each AncestorNode with its UI BiologicalParentsPanel
	private final Map<AncestorNode, PartnersPanel> nodeToPanelMap = new HashMap<>();

	// Children block (Generation 1)
	private SiblingsPanel childrenPanel;
	private JScrollPane childrenScrollPane;

	private IndividualPanel selectedPanel;


	public BiologicalTreePanel(final FLEFModel model){
		this.model = model;
		this.treeService = new BiologicalTreeService(model);
		this.treeMutator = new AncestorTreeMutator(model, treeService, this);

		setBackground(BACKGROUND_COLOR_APPLICATION);
		setOpaque(true);
	}

	public void loadTree(final String rootIndividualId, final int maxGenerations){
		this.currentRootIndividualId = rootIndividualId;
		this.currentMaxGenerations = maxGenerations;

		refreshTree();
	}

	private void refreshTree(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::refreshTree);

			return;
		}

		// Build tree hierarchy from model
		final AncestorNode rootIndividualNode = treeService.buildAncestorTree(currentRootIndividualId, currentMaxGenerations - 1 );

		// Clear previous UI sub-components
		removeAll();
		nodeToPanelMap.clear();

		// Render node components and bind click listeners
		if(rootIndividualNode != null){
			final IndividualData partnerData = rootIndividualNode.getPartnerData();
			final String partnerId = (partnerData != null? partnerData.getIndividualId(): null);
			final AncestorNode partnerNode = treeService.buildAncestorTree(partnerId, currentMaxGenerations - 1);
			final SexType sex = rootIndividualNode.getIndividualData().getIndividualSex();

			rootNode = new AncestorNode(rootIndividualNode.getBiologicalChildrenData());
			if(sex == SexType.FEMALE){
				rootNode.setFather(partnerNode);
				rootNode.setMother(rootIndividualNode);
			}
			else{
				rootNode.setFather(rootIndividualNode);
				rootNode.setMother(partnerNode);
			}

			buildLayout();
		}

		// Force Swing repaint and recalculate layout
		revalidate();
		repaint();
	}

	/**
	 * Builds the dynamic layout using cell placement.
	 * Each node represents a couple (individual + partner).
	 */
	private void buildLayout(){
		final int ancestorLevels = Math.max(1, currentMaxGenerations - 1);
		final int maxDepth = ancestorLevels - 1;
		final int maxLeafColumns = 1 << maxDepth;

		// Constructing MigLayout constraints
		final StringBuilder colConstraints = new StringBuilder();
		for(int i = 0; i < maxLeafColumns; i ++){
			if(i > 0)
				colConstraints.append(PartnersPanel.GROUP_SEPARATION);
			colConstraints.append("[grow,center]");
		}

		// Rows: ascending levels (excluding root) + root
		final StringBuilder rowConstraints = new StringBuilder();
		for(int i = 0; i <= maxDepth; i ++){
			if(i > 0)
				rowConstraints.append(GENERATION_SEPARATOR_SIZE);
			rowConstraints.append("[]");
		}
		// children row
		rowConstraints.append(GENERATION_SEPARATOR_SIZE).append("[]");

		setLayout(new MigLayout("ins 0", colConstraints.toString(), rowConstraints.toString()));

		final Deque<LayoutNodeItem> stack = new ArrayDeque<>();
		stack.push(new LayoutNodeItem(rootNode, 0, 0, maxLeafColumns));
		while(!stack.isEmpty()){
			final LayoutNodeItem item = stack.pop();

			final AncestorNode node = item.node;
			final int depth = item.depth;
			final int col = item.col;
			final int span = item.span;

			final int row = maxDepth - depth;

			// PRUNING RULE: Do not render upper empty ancestor slots if node is null
			if(node == null && depth > 0)
				continue;

			// Create a panel for this node (or an empty placeholder if node is null)
			final PartnersPanel panel = createPanelForNode(node,
				(depth == 0? BoxPanelType.PRIMARY: BoxPanelType.SECONDARY));
			if(node != null)
				nodeToPanelMap.put(node, panel);

			// Add the panel at the computed cell
			add(panel, "cell " + col + " " + row + ", span " + span + ", grow");

			// Push parents onto stack ONLY if the current node exists
			// NOTE: Push MOTHER first, then FATHER, so that FATHER is processed first (LIFO order).
			if(depth < maxDepth && node != null){
				final int nextDepth = depth + 1;
				final int halfSpan = span >> 1;

				final AncestorNode motherNode = node.getMother();
				stack.push(new LayoutNodeItem(motherNode, nextDepth, col + halfSpan, halfSpan));

				final AncestorNode fatherNode = node.getFather();
				stack.push(new LayoutNodeItem(fatherNode, nextDepth, col, halfSpan));
			}
		}

		// Extract root parent records for children panel
		final FLEFRecord father = (rootNode != null && rootNode.getFather() != null
			? rootNode.getFather().getIndividual()
			: null);
		final FLEFRecord mother = (rootNode != null && rootNode.getMother() != null
			? rootNode.getMother().getIndividual()
			: null);

		// Add children (below)
		childrenPanel = createChildrenPanel(father, mother);
		childrenScrollPane = createChildrenScrollPane(childrenPanel);
		add(childrenScrollPane, "cell 0 " + (maxDepth + 1) + ",span " + maxLeafColumns + ",center");
	}

	private PartnersPanel createPanelForNode(final AncestorNode node, final BoxPanelType type){
		final PartnersPanel panel = PartnersPanel.create(type, model)
			.withListener(this);
		panel.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				final String clickedId = node.getIndividualId();
				if(clickedId != null)
					treeMutator.navigateToRoot(clickedId);
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
				.withParent((fatherFather != null? fatherFather.getIndividual(): null),
					(fatherMother != null? fatherMother.getIndividual(): null));
			panel.getMotherPanel()
				.withParent((motherFather != null? motherFather.getIndividual(): null),
					(motherMother != null? motherMother.getIndividual(): null));

			final AncestorNode father = node.getFather();
			final AncestorNode mother = node.getMother();
			final IndividualData fatherData = (father != null? father.getIndividualData(): null);
			final IndividualData motherData = (mother != null? mother.getIndividualData(): null);
			panel.withBiologicalParents(fatherData, motherData);
		}
		return panel;
	}

	private SiblingsPanel createChildrenPanel(final FLEFRecord father, final FLEFRecord mother){
		final SiblingsPanel panel = SiblingsPanel.create(father, mother, BoxPanelType.SECONDARY, model)
			.withListener(this);
		if(rootNode != null)
			panel.withSiblingsData(rootNode.getBiologicalChildrenData());
		return panel;
	}

	private JScrollPane createChildrenScrollPane(final JPanel content){
		final JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setOpaque(false);
		scrollPane.getViewport()
			.setOpaque(false);
		scrollPane.setBorder(null);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		// Dynamically compute horizontal scrollbar height to avoid overlapping children panels
		final JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
		final int scrollBarHeight = horizontalScrollBar.getPreferredSize()
			.height;
		content.setBorder(BorderFactory.createEmptyBorder(0, 0, scrollBarHeight, 0));

		return scrollPane;
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D g2){
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setColor(CONNECTION_LINE_COLOR);
			g2.setStroke(PartnersPanel.CONNECTION_STROKE);

			drawTreeConnections(g2);
			drawChildrenConnections(g2);
		}
	}

	/**
	 * Iteratively traverses all tree nodes using a Queue (BFS) to draw connection lines between parents and children.
	 */
	private void drawTreeConnections(final Graphics2D g2){
		if(rootNode == null)
			return;

		final Queue<AncestorNode> queue = new ArrayDeque<>();
		queue.add(rootNode);
		while(!queue.isEmpty()){
			final AncestorNode node = queue.poll();
			final PartnersPanel nodePanel = nodeToPanelMap.get(node);

			final AncestorNode father = node.getFather();
			if(father != null){
				final PartnersPanel fatherPanel = nodeToPanelMap.get(father);
				if(fatherPanel != null && nodePanel != null){
					Point enter = nodePanel.getPaintingFatherEnterPoint();
					enter = SwingUtilities.convertPoint(nodePanel, enter, this);
					connectParentToChild(fatherPanel, enter, g2);
				}
				queue.add(father);
			}

			final AncestorNode mother = node.getMother();
			if(mother != null){
				final PartnersPanel motherPanel = nodeToPanelMap.get(mother);
				if(motherPanel != null && nodePanel != null){
					Point enter = nodePanel.getPaintingMotherEnterPoint();
					enter = SwingUtilities.convertPoint(nodePanel, enter, this);
					connectParentToChild(motherPanel, enter, g2);
				}
				queue.add(mother);
			}
		}
	}

	private void connectParentToChild(final PartnersPanel parentGroupPanel, final Point childEnterPoint,
			final Graphics2D g2){
		Point parentExit = parentGroupPanel.getPaintingExitPoint();
		parentExit = SwingUtilities.convertPoint(parentGroupPanel, parentExit, this);

		// Vertical line extending out from parent group
		final int midY = (childEnterPoint.y + parentExit.y + PartnersPanel.GROUP_EXITING_HEIGHT) / 2;
		g2.drawLine(parentExit.x, parentExit.y,
			parentExit.x, midY);

		// Vertical line entering into child panel
		g2.drawLine(childEnterPoint.x, childEnterPoint.y,
			childEnterPoint.x, midY);

		// Horizontal connecting line
		g2.drawLine(parentExit.x, midY,
			childEnterPoint.x, midY);
	}

	private void drawChildrenConnections(final Graphics2D g2){
		if(rootNode == null || childrenPanel == null)
			return;

		final PartnersPanel homePanel = nodeToPanelMap.get(rootNode);
		final Point[] childEnterPoints = childrenPanel.getPaintingEnterPoints();

		if(homePanel != null && childEnterPoints.length > 0){
			Point homeExit = homePanel.getPaintingExitPoint();
			homeExit = SwingUtilities.convertPoint(homePanel, homeExit, this);

			// Vertical line exiting home group
			final int connectY = childrenScrollPane.getY() + childEnterPoints[0].y - GENERATION_SEPARATOR_SIZE / 2;
			g2.drawLine(homeExit.x, homeExit.y,
				homeExit.x, connectY);
		}
	}


	@Override
	public void onTreeStructureChanged(final String rootIndividualId){
		// Run UI updates on the Swing Event Dispatch Thread
		SwingUtilities.invokeLater(() -> loadTree(rootIndividualId, currentMaxGenerations));
	}


	// NOTE: Operation handled ONLY by UI/Panel
	@Override
	public void onIndividualEdit(final FLEFRecord individual){
		if(individual == null)
			return;

		final FLEFRecord editedIndividual = showEditIndividualDialog(individual);
		if(editedIndividual != null){
			LOGGER.debug("Individual edited: {}", editedIndividual.getId());

			// Invalidate indices to clear cached IndividualData/Events and rebuild UI
			treeMutator.editIndividual(editedIndividual, getRootIndividualId());
		}
	}


	// NOTE: Operation delegated to AncestorTreeMutator
	@Override
	public void onIndividualSelected(final FLEFRecord individual){
		if(individual != null && individual.getId() != null)
			treeMutator.navigateToRoot(individual.getId());
	}

	// NOTE: Operation delegated to AncestorTreeMutator
	@Override
	public void onIndividualRemove(final FLEFRecord individual){
		if(individual == null)
			return;

		final int confirm = JOptionPane.showConfirmDialog(
			this,
			"Are you sure you want to remove individual "
				+ IndividualHandler.getInstance().getDisplayText(individual, model) + "?",
			"Confirm Removal",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if(confirm == JOptionPane.YES_OPTION){
			LOGGER.debug("Individual remove {}", individual.getId());

			// Pass the current root individual ID to allow fallback logic if root is deleted
			treeMutator.removeIndividual(individual, getRootIndividualId());
		}
	}

	@Override
	public void onPanelSelected(final IndividualPanel panel){
		selectedPanel = panel;
	}

	@Override
	public void onIndividualAddOrLink(final IndividualOperation operation, final FLEFRecord father,
			final FLEFRecord mother){
		final String rootIndividualId = getRootIndividualId();
		final Function<SexType, FLEFRecord> fnOperation = (operation == IndividualOperation.ADD
			? this::showCreateIndividualDialog
			: this::showSearchIndividualDialog);

		final Component parent = SiblingsPanel.findContainingSiblingsPanel(selectedPanel.getParent());
		final boolean addChild = (parent != null);

		if(addChild){
			// add child
			final String fatherId = (father != null? father.getId(): null);
			final String motherId = (mother != null? mother.getId(): null);
			final FLEFRecord newChild = fnOperation.apply(null);
			if(newChild == null)
				return;

			treeMutator.addChildToParents(fatherId, motherId, newChild);

			// Invalidate cache and refresh
			treeMutator.invalidateAndNotifyTreeChanged(rootIndividualId);

			return;
		}


		final PartnersPanel partnerPanel = PartnersPanel.findContainingPartnersPanel(selectedPanel.getParent());
		if(partnerPanel == null)
			return;
		final boolean addParent = partnerPanel.isEmpty();
		final Side side = partnerPanel.getSideOf(selectedPanel);

		final FLEFRecord newIndividual = fnOperation.apply(side == Side.LEFT? SexType.MALE: SexType.FEMALE);
		if(newIndividual == null)
			return;

		if(addParent){
			// add parent

			// Retrieve the child ID from the AncestorNode that owns this PartnersPanel.
			// The nodeToPanelMap maps each node to its UI panel.
			final String childId = nodeToPanelMap.entrySet().stream()
				.filter(entry -> entry.getValue() == partnerPanel)
				.findFirst()
				.map(Map.Entry::getKey)
				.map(AncestorNode::getIndividualId)
				.orElse(null);

			// Establish the parent-child relationship in the model
			treeMutator.addParentToChild(childId, newIndividual);
		}
		else{
			// add partner
			// Determine which side we are adding (male on the left, female on the right)
			final boolean isFemale = (side == Side.RIGHT);
			final String partnerId = (isFemale? partnerPanel.getFatherPanel(): partnerPanel.getMotherPanel())
				.getData()
				.getIndividualId();

			// Before adding, find existing children of the partnerId
			final List<FLEFRecord> children = findChildren(partnerId);

			treeMutator.addPartnerToIndividual(partnerId, newIndividual);

			// For each child, add relationship to new partner
			for(final FLEFRecord child : children)
				treeMutator.addParentToChild(child.getId(), newIndividual);
		}

		// Invalidate cache and refresh
		treeMutator.invalidateAndNotifyTreeChanged(rootIndividualId);
	}

	/**
	 * Opens a dialog allowing the user to select which relationships of the given individual to unlink.
	 */
	@Override
	public void showUnlinkDialog(final FLEFRecord individual){
		if(individual == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final UnlinkRelationshipsDialog dialog = new UnlinkRelationshipsDialog(owner, model, individual.getId());
		dialog.setVisible(true);

		final List<String> selectedIds = dialog.getSelectedRelationshipIds();
		if(selectedIds.isEmpty())
			return;

		// Confirm with the user
		final int confirm = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to remove the selected relationships?",
			"Confirm Unlink",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm == JOptionPane.YES_OPTION){
			treeMutator.removeRelationships(selectedIds);

			treeMutator.invalidateAndNotifyTreeChanged(getRootIndividualId());
		}
	}

	/**
	 * Finds all children of the given individual.
	 * Searches for relationship records where the object is the individual
	 * and the type is one of the parent‑child relationship types.
	 *
	 * @param individualId the ID of the parent (without @)
	 * @return a list of FLEFRecord objects representing the children
	 */
	private List<FLEFRecord> findChildren(final String individualId){
		final List<FLEFRecord> children = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(!type.endsWith(ENUM_TYPE_ENDS_WITH_CHILD))
				continue;

			// The target must be the individual
			final String objectId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			if(!individualId.equals(objectId))
				continue;

			// The subject is the child
			final String childId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			if(childId == null)
				continue;

			final FLEFRecord child = model.getRecordById(childId);
			if(child != null)
				children.add(child);
		}
		return children;
	}


	private FLEFRecord showEditIndividualDialog(final FLEFRecord individual){
		final Window window = SwingUtilities.getWindowAncestor(this);
		final Dialog parent = (window instanceof Dialog dialog? dialog: null);

		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createEditDialog(parent, model, individual);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	private FLEFRecord showCreateIndividualDialog(final SexType sex){
		final Window window = SwingUtilities.getWindowAncestor(this);
		final Dialog parent = (window instanceof Dialog dialog? dialog: null);

		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createNewDialog(parent, model);
		dialog.witSex(sex);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	private FLEFRecord showSearchIndividualDialog(final SexType sex){
		final Window window = SwingUtilities.getWindowAncestor(this);
		final Dialog parent = (window instanceof Dialog dialog? dialog: null);

		final FLEFRecord[] result = {null};
		final Function<FLEFRecord, Boolean> fnFilter = record -> (sex == null || ENUM_SEX_FEMALE.equals(FLEFRecordHelper.getChildValue(record, TAG_SEX)) ^ (sex == SexType.MALE));
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model, fnFilter,
			IndividualHandler.class);
		dialog.addPropertyChangeListener(MultiTypeSelectionDialog.PROPERTY_TYPE_SELECTED,
			e -> result[0] = dialog.getSelectedRecord());
		dialog.setVisible(true);

		return result[0];
	}


	@Override
	public void onIndividualMove(final FLEFRecord individual){
		if(individual == null)
			return;

		LOGGER.debug("Moving individual {} to clipboard", individual.getId());

		RelationClipboard.getInstance()
			.setRecord(individual);
	}

	@Override
	public void onIndividualPaste(final FLEFRecord father, final FLEFRecord mother){
		final RelationClipboard clipboard = RelationClipboard.getInstance();
		if(!clipboard.hasRecord())
			return;

		final FLEFRecord source = clipboard.getRecord();

		final String rootIndividualId = getRootIndividualId();

		final Component parent = SiblingsPanel.findContainingSiblingsPanel(selectedPanel.getParent());
		final boolean addChild = (parent != null);

		// Disconnect the individual from all relationships
		final List<String> relationshipIds = extractRelationships(source.getId());
		treeMutator.removeRelationships(relationshipIds);

		final String fatherId = (father != null? father.getId(): null);
		final String motherId = (mother != null? mother.getId(): null);

		if(addChild)
			// Paste as child of the provided parents
			treeMutator.addChildToParents(fatherId, motherId, source);
		else{
			final PartnersPanel partnerPanel = PartnersPanel.findContainingPartnersPanel(selectedPanel.getParent());

			if(partnerPanel == null){
				clipboard.clear();

				// Invalidate cache and refresh
				treeMutator.invalidateAndNotifyTreeChanged(rootIndividualId);

				return;
			}

			final boolean addParent = partnerPanel.isEmpty();
			final Side side = partnerPanel.getSideOf(selectedPanel);

			if(addParent){
				// Paste as parent of the child associated with this PartnersPanel
				final String childId = nodeToPanelMap.entrySet().stream()
					.filter(entry -> entry.getValue() == partnerPanel)
					.findFirst()
					.map(Map.Entry::getKey)
					.map(AncestorNode::getIndividualId)
					.orElse(null);
				if(childId != null)
					treeMutator.addParentToChild(childId, source);
			}
			else{
				// Paste as a partner of the existing individual
				final boolean isFemale = (side == Side.RIGHT);
				final IndividualData partnerData = (isFemale? partnerPanel.getFatherPanel(): partnerPanel.getMotherPanel())
					.getData();
				final String partnerId = partnerData.getIndividualId();
				if(partnerId != null){
					final boolean isPartnerFemale = (partnerData.getIndividualSex() == SexType.FEMALE);
					final boolean isSourceFemale = ENUM_SEX_FEMALE.equals(FLEFRecordHelper.getChildValue(source, TAG_SEX));
					if(isPartnerFemale ^ isSourceFemale)
						treeMutator.addPartnerToIndividual(partnerId, source);
				}
			}
		}

		// Clear the clipboard and update the tree
		clipboard.clear();

		treeMutator.invalidateAndNotifyTreeChanged(rootIndividualId);
	}

	/**
	 * Extracts relationships from the model and groups them into parents, partners, and children.
	 */
	private List<String> extractRelationships(final String individualId){
		final List<String> result = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null)
				continue;

			final String subjectId = relationship.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
			final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
			final boolean involves = (individualId.equals(subjectId) || individualId.equals(targetId));
			if(involves)
				result.add(relationship.getId());
		}
		return result;
	}


	/**
	 * Retrieves the ID of the current root individual rendered in the tree.
	 *
	 * @return the record ID of the root individual, or {@code null} if no tree is loaded.
	 */
	public String getRootIndividualId(){
		if(rootNode == null || rootNode.getFather() == null)
			return null;

		return rootNode.getFather()
			.getIndividual()
			.getId();
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";
		final String rootIndividualId = "I1";
		final int maxGenerations = 4;

		final String content;
		try(final InputStream is = BiologicalTreePanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final BiologicalTreePanel panel = new BiologicalTreePanel(model);
			panel.loadTree(rootIndividualId, maxGenerations);

			final JFrame frame = new JFrame();
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
