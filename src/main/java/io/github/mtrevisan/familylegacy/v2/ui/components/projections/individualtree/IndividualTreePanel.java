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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityTreePopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.kinship.KinshipDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.lifespan.MultiLifespanStripPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree.PedigreeCollapse;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree.PedigreeCollapseDetector;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree.PedigreeCollapseDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree.PedigreePath;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.RelationshipOperationCoordinator;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.Side;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Generalized panel responsible for rendering an N-generation genealogical
 * tree layout dynamically.
 * <p>
 * The class is a thin orchestrator: it owns the root state, delegates
 * layout and rendering to {@link TreeLayoutBuilder} and {@link TreeRenderer},
 * delegates mutations to {@link TreeMutator} (through
 * {@link io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.RelationshipOperationCoordinator}) and delegates all dialogs to
 * {@link IndividualDialogProvider}. This keeps the panel focused on
 * composition and on the Swing event flow.
 */
public class IndividualTreePanel extends JPanel implements TreeChangeListener, IndividualListener{

	@Serial
	private static final long serialVersionUID = 9011391311012465249L;


	private static final Logger LOGGER = LoggerFactory.getLogger(IndividualTreePanel.class);


	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);
	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;

	private static final String ENUM_TYPE_BIOLOGICAL_CHILD = "biological_child";
	private static final String ENUM_TYPE_ADOPTIVE_CHILD = "adoptive_child";
	private static final String ENUM_TYPE_STEP_CHILD = "step_child";
	private static final String ENUM_TYPE_FOSTER_CHILD = "foster_child";
	private static final String ENUM_TYPE_GUARDED_CHILD = "guarded_child";

	private static final String ACTION_OPEN_KINSHIP_DIALOG = "openKinshipDialog";
	private static final String ACTION_TOGGLE_TREE_LAYOUT = "toggleTreeLayout";
	private static final String ACTION_SHOW_PEDIGREE_COLLAPSE = "showPedigreeCollapse";
	private static final String ACTION_TOGGLE_LIFESPANS_STRIP = "toggleLifespansStrip";


	private final String[] allowedRelationshipTypes;
	private TreeLayout treeLayout;
	private boolean showPartner;

	private final FLEFModel model;
	private final TreeService treeService;
	private final TreeMutator treeMutator;

	private final IndividualDialogProvider dialogProvider;
	private final RelationshipOperationCoordinator operationCoordinator;

	private String currentRootIndividualId;
	private TreeNode rootNode;
	private int currentMaxGenerations;

	// Map associating each TreeNode with its UI PartnersPanel
	private final Map<TreeNode, PartnersPanel> nodeToPanelMap = new LinkedHashMap<>();

	// Children block (Generation 1)
	private SiblingsPanel childrenPanel;

	/** Canvas that hosts the tree layout. */
	private final JPanel treeCanvas = new TreeCanvas();
	/** Collapsible strip showing the lifespans of the visible individuals. */
	private final MultiLifespanStripPanel lifespansStrip;
	/** Toggle bar always visible at the bottom. */
	private final ToggleBar toggleBar = new ToggleBar();
	/** Whether the lifespan strip is currently expanded. */
	private boolean stripVisible;

	/** Collapses detected in the current tree, keyed by individual id. */
	private Map<String, PedigreeCollapse> collapsesByIndividualId = Map.of();

	private JPanel selectedPanel;


	public IndividualTreePanel(final TreeType treeType, final TreeLayout treeLayout, final FLEFModel model){
		this.allowedRelationshipTypes = computeAllowedRelationshipTypes(treeType);
		this.treeLayout = treeLayout;

		this.model = model;

		final Predicate<String> relationshipTypeFilter = type -> ArrayUtils.contains(allowedRelationshipTypes,
			type.toLowerCase(Locale.ROOT));
		this.treeService = new TreeService(relationshipTypeFilter, model);
		this.treeMutator = new TreeMutator(relationshipTypeFilter, model, treeService, this);

		this.dialogProvider = new IndividualDialogProvider(model);
		this.operationCoordinator = new RelationshipOperationCoordinator(model, treeMutator,
			allowedRelationshipTypes);

		setBackground(BACKGROUND_COLOR_APPLICATION);
		setOpaque(true);


		setupLayoutShortcut(this);
		setupKinshipShortcut(this);
		setupCollapseShortcut(this);

		// The tree canvas must already be configured with the tree layout, so
		// the strip is initialized after the initial refresh (which happens on
		// the first loadTree call).
		this.lifespansStrip = new MultiLifespanStripPanel(model);

	// The panel uses BorderLayout: center = tree canvas, south = bottom bar
	// (toggle header + strip). This is the only structural change needed to
	// accommodate the strip without disturbing the tree layout.
		setLayout(new BorderLayout());
		add(treeCanvas, BorderLayout.CENTER);

		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(toggleBar, BorderLayout.NORTH);
		bottom.add(lifespansStrip, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		lifespansStrip.setVisible(false);

		setupStripShortcut(this);
	}


	public IndividualTreePanel withShowPartner(){
		showPartner = true;

		return this;
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
		final TreeNode rootIndividualNode = treeService.buildTree(currentRootIndividualId, showPartner,
			currentMaxGenerations - 1);

		// Clear previous UI sub-components
		treeCanvas.removeAll();
		nodeToPanelMap.clear();

		rootNode = rootIndividualNode;
		if(rootIndividualNode != null && showPartner){
			final IndividualData partnerData = rootIndividualNode.getPartnerData();
			final String partnerId = (partnerData != null? partnerData.getId(): null);
			final TreeNode partnerNode = treeService.buildTree(partnerId, showPartner,
				currentMaxGenerations - 1);
			final SexType sex = rootIndividualNode.getIndividualData()
				.getSex();

			rootNode = new TreeNode(rootIndividualNode.getBiologicalChildrenData());
			if(sex == SexType.FEMALE){
				rootNode.setFather(partnerNode);
				rootNode.setMother(rootIndividualNode);
			}
			else{
				rootNode.setFather(rootIndividualNode);
				rootNode.setMother(partnerNode);
			}
		}

		// Detect pedigree collapses before building the layout.
		final List<PedigreeCollapse> collapses = PedigreeCollapseDetector.detect(rootNode, model);
		collapsesByIndividualId = collapses.stream()
			.collect(Collectors.toMap(PedigreeCollapse::individualId, Function.identity()));

		buildLayout();
		applyCollapseBadges();
		updateStripIndividuals();

		// Force Swing recalculate layout and repaint (including parent context if available)
		treeCanvas.revalidate();
		treeCanvas.repaint();
		revalidate();
		repaint();
		if(getParent() != null){
			getParent().revalidate();
			getParent().repaint();
		}
	}

	/**
	 * Builds the dynamic layout using cell placement.
	 * Each node represents a couple (individual and partner).
	 */
	private void buildLayout(){
		final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory = new EntityTreePopupMenuFactory();
		final TreeLayoutBuilder.LayoutResult result = TreeLayoutBuilder.buildLayout(treeCanvas,
			rootNode, showPartner, currentMaxGenerations, model, nodeToPanelMap, this, popupFactory, treeMutator,
			treeLayout);

		childrenPanel = result.childrenPanel();
	}

	/**
	 * Walks the map of node-to-panel associations and applies the
	 * pedigree-collapse badge to every individual panel whose individual
	 * appears more than once in the tree.
	 * <p>
	 * The map is keyed by {@link TreeNode}, and each {@link PartnersPanel}
	 * shows the two parents of the node (the {@code fatherPanel} on the left
	 * and the {@code motherPanel} on the right). The method checks both slots
	 * independently: the badge can appear on one, the other, or both.
	 */
	private void applyCollapseBadges(){
		for(final Map.Entry<TreeNode, PartnersPanel> entry : nodeToPanelMap.entrySet()){
			final TreeNode node = entry.getKey();
			final PartnersPanel panel = entry.getValue();

			applyCollapseBadgeToSlot(panel.getFatherPanel(),
				(node.getFather() != null? node.getFather().getIndividualId(): null));
			applyCollapseBadgeToSlot(panel.getMotherPanel(),
				(node.getMother() != null? node.getMother().getIndividualId(): null));
		}
	}

	private void applyCollapseBadgeToSlot(final IndividualPanel slot, final String individualId){
		if(slot == null || individualId == null)
			return;

		final PedigreeCollapse collapse = collapsesByIndividualId.get(individualId);
		if(collapse == null)
			return;

		slot.withCollapseInfo(collapse.occurrenceCount(), buildCollapseTooltip(collapse));
	}

	private static String buildCollapseTooltip(final PedigreeCollapse collapse){
		final StringBuilder sb = new StringBuilder("<html><b>Pedigree collapse</b><br>");
		sb.append("This individual appears ")
			.append(collapse.occurrenceCount())
			.append(" times in the tree.<br><br>");
		sb.append("<b>Paths from the root:</b><br>");
		for(final PedigreePath path : collapse.paths())
			sb.append("&nbsp;&nbsp;").append(path.code())
				.append(" — ").append(path.describe())
				.append("<br>");
		sb.append("</html>");
		return sb.toString();
	}


	@Override
	public void onTreeStructureChanged(final String rootIndividualId){
		SwingUtilities.invokeLater(() -> loadTree(rootIndividualId, currentMaxGenerations));
	}


	@Override
	public void onEntityEdit(final FLEFRecord individual){
		if(individual == null)
			return;

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final FLEFRecord editedIndividual = dialogProvider.showEditDialog(parent, individual);
		if(editedIndividual != null){
			LOGGER.debug("Individual edited: {}", editedIndividual.getId());

			// Invalidate indices to clear cached IndividualData/Events and rebuild UI
			treeMutator.invalidateAndNotifyTreeChanged(getRootIndividualId());
		}
	}


	@Override
	public void onEntitySelected(final FLEFRecord individual){
		if(individual != null && individual.getId() != null)
			treeMutator.navigateToRoot(individual.getId());
	}

	@Override
	public void onEntityRemove(final FLEFRecord individual){
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
	public void onEntitySelected(final JPanel panel){
		selectedPanel = panel;
	}

	@Override
	public void onIndividualAddOrConnect(final TreeOperation operation){
		final Function<SexType, FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateIndividualDialog
			: this::showSearchIndividualDialog);

		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap);
		if(ctx == null || ctx.isChildContext() || !ctx.hasPartnerPanel())
			return;

		final SexType sex = (ctx.side == Side.LEFT? SexType.MALE: SexType.FEMALE);
		final FLEFRecord individual = fnOperation.apply(sex);
		if(individual == null)
			return;

		performParentRelationOperation(individual, ctx, false, getRootIndividualId());
	}

	@Override
	public void onChildAddOrConnect(final TreeOperation operation){
		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap);
		if(ctx == null || ctx.isChildContext() || !ctx.hasPartnerPanel())
			return;

		final Function<SexType, FLEFRecord> fnOperation = (operation == TreeOperation.ADD
			? this::showCreateIndividualDialog
			: this::showSearchIndividualDialog);
		final FLEFRecord child = fnOperation.apply(null);
		if(child == null)
			return;

		final IndividualPanel fatherPanel = ctx.partnerPanel.getFatherPanel();
		final IndividualPanel motherPanel = ctx.partnerPanel.getMotherPanel();
		final IndividualData fatherData = (fatherPanel != null? fatherPanel.getData(): null);
		final IndividualData motherData = (motherPanel != null? motherPanel.getData(): null);
		final FLEFRecord targetFather = (fatherData != null? fatherData.getIndividual(): null);
		final FLEFRecord targetMother = (motherData != null? motherData.getIndividual(): null);
		if(targetFather == null && targetMother == null)
			return;

		performChildRelationOperation(child, targetFather, targetMother, false, getRootIndividualId());

		onEntitySelected(targetFather != null? targetFather: targetMother);
	}


	/**
	 * Opens a dialog allowing the user to select which relationships of the given individual to unlink.
	 */
	@Override
	public void onEntityUnlink(final FLEFRecord individual){
		if(individual == null || selectedPanel == null)
			return;

		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap);
		if(ctx == null || !ctx.hasPartnerPanel())
			return;

		final IndividualPanel parentPanel = (ctx.side == Side.LEFT
			? ctx.partnerPanel.getFatherPanel()
			: ctx.partnerPanel.getMotherPanel());
		if(parentPanel == null)
			return;

		final FLEFRecord fatherRecord = parentPanel.getFather();
		final FLEFRecord motherRecord = parentPanel.getMother();
		final String targetFatherId = (fatherRecord != null? fatherRecord.getId(): null);
		final String targetMotherId = (motherRecord != null? motherRecord.getId(): null);

		final Window parent = SwingUtilities.getWindowAncestor(this);
		final Set<String> parentsIds = new LinkedHashSet<>();
		if(targetFatherId != null)
			parentsIds.add(targetFatherId);
		if(targetMotherId != null)
			parentsIds.add(targetMotherId);
		final Set<String> childrenIds = new LinkedHashSet<>(ctx.childrenId);
		final UnlinkRelationshipsDialog dialog = new UnlinkRelationshipsDialog(parent, model, individual.getId(),
			parentsIds, Collections.emptySet(), Collections.emptySet(), childrenIds);
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


	@Override
	public void onEntityRelocate(final FLEFRecord individual){
		if(individual == null)
			return;

		LOGGER.debug("Relocate individual {} to clipboard", individual.getId());

		RelationClipboard.getInstance()
			.setRecord(individual);
	}

	@Override
	public void onIndividualPaste(final FLEFRecord father, final FLEFRecord mother){
		final RelationClipboard clipboard = RelationClipboard.getInstance();
		if(!clipboard.hasRecord())
			return;

		final FLEFRecord source = clipboard.getRecord();

		// If father/mother details are present, paste as child; otherwise determine the context and paste as parent.
		if(father != null || mother != null)
			performChildRelationOperation(source, father, mother, true, getRootIndividualId());
		else{
			final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel, nodeToPanelMap);
			if(ctx != null && ctx.isParentContext())
				performParentRelationOperation(source, ctx, true, getRootIndividualId());
		}

		clipboard.clear();
	}


	/**
	 * Associates or creates a CHILD with respect to the given parents.
	 */
	private void performChildRelationOperation(final FLEFRecord child, final FLEFRecord father, final FLEFRecord mother,
			final boolean isPaste, final String rootId){
		if(child == null)
			return;

		if(isPaste){
			final List<String> relationshipIds = treeService.getRelationshipIdsForIndividual(child.getId());
			treeMutator.removeRelationships(relationshipIds);
		}

		final Window parent = SwingUtilities.getWindowAncestor(this);
		operationCoordinator.performChildOperation(parent, child, father, mother);

		treeMutator.invalidateAndNotifyTreeChanged(rootId);
	}

	/**
	 * Associates or creates a PARENT/PARTNER.
	 */
	private void performParentRelationOperation(final FLEFRecord individual, final TreeContextHelper.Context ctx,
		final boolean isPaste, final String rootId){
		if(individual == null || ctx == null)
			return;

		if(isPaste){
			final List<String> relIds = treeService.getRelationshipIdsForIndividual(individual.getId());
			treeMutator.removeRelationships(relIds);
		}

		final Window parent = SwingUtilities.getWindowAncestor(this);
		operationCoordinator.performParentOperation(parent, individual, ctx);

		treeMutator.invalidateAndNotifyTreeChanged(rootId);
	}


	/* ======================================================================
	 *                          Dialog shortcuts
	 * ====================================================================== */

	private FLEFRecord showCreateIndividualDialog(final SexType sex){
		return dialogProvider.showCreateDialog(SwingUtilities.getWindowAncestor(this), sex);
	}

	private FLEFRecord showSearchIndividualDialog(final SexType sex){
		return dialogProvider.showSearchDialog(SwingUtilities.getWindowAncestor(this), sex);
	}


	/* ======================================================================
	 *                          Root id
	 * ====================================================================== */

	/**
	 * Returns the id of the current root individual rendered in the tree.
	 * <p>
	 * In couple-container mode (when {@code showPartner} is enabled and the
	 * tree was rebuilt around a couple), the root node has no individual of
	 * its own: the actual root individual is held by the father slot. This
	 * method resolves both cases transparently.
	 *
	 * @return the id of the root individual, or {@code null} if no tree is
	 * loaded
	 */
	public String getRootIndividualId(){
		if(currentRootIndividualId != null)
			return currentRootIndividualId;

		if(rootNode == null)
			return null;

		// Couple-container node: the individual lives in the father slot.
		final String ownId = rootNode.getIndividualId();
		if(ownId != null)
			return ownId;

		final TreeNode father = rootNode.getFather();
		if(father != null && father.getIndividual() != null)
			return father.getIndividual()
				.getId();

		return null;
	}


	/* ======================================================================
	 *                          Layout shortcut
	 * ====================================================================== */

	public void setupLayoutShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_L_STROKE, ACTION_TOGGLE_TREE_LAYOUT);
		actionMap.put(ACTION_TOGGLE_TREE_LAYOUT, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -2768341528368853112L;

			@Override
			public void actionPerformed(final ActionEvent e){
				toggleLayout();
			}
		});
	}

	private void toggleLayout(){
		this.treeLayout = (this.treeLayout == TreeLayout.VERTICAL
			? TreeLayout.HORIZONTAL
			: TreeLayout.VERTICAL);

		refreshTree();

		final Window window = SwingUtilities.getWindowAncestor(this);
		if(window != null){
			window.pack();
			window.setLocationRelativeTo(null);
		}
	}

	/**
	 * Installs the Ctrl+K shortcut that opens the kinship dialog.
	 *
	 * @param component the component that receives the shortcut
	 */
	public void setupKinshipShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_K_STROKE, ACTION_OPEN_KINSHIP_DIALOG);
		actionMap.put(ACTION_OPEN_KINSHIP_DIALOG, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -1043238887053155832L;

			@Override
			public void actionPerformed(final ActionEvent e){
				openKinshipDialog();
			}
		});
	}

	/**
	 * Opens the kinship dialog with the current root pre-selected as the
	 * first individual. Uses the tree service to walk the ancestor graph.
	 */
	private void openKinshipDialog(){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final FLEFRecord initialA = (currentRootIndividualId != null
			? model.getRecordById(currentRootIndividualId)
			: null);
		final KinshipDialog dialog = new KinshipDialog(parent, model, treeService, initialA, null);
		dialog.setVisible(true);
	}

	/**
	 * Installs the {@code Ctrl+P} shortcut that opens the pedigree-collapse
	 * report dialog.
	 *
	 * @param component the component that receives the shortcut
	 */
	public void setupCollapseShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_P_STROKE, ACTION_SHOW_PEDIGREE_COLLAPSE);
		actionMap.put(ACTION_SHOW_PEDIGREE_COLLAPSE, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 1152736519295769067L;

			@Override
			public void actionPerformed(final ActionEvent e){
				showPedigreeCollapseDialog();
			}
		});
	}

	private void showPedigreeCollapseDialog(){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final List<PedigreeCollapse> collapses = List.copyOf(collapsesByIndividualId.values());
		final PedigreeCollapseDialog dialog = new PedigreeCollapseDialog(parent, collapses);
		dialog.setVisible(true);
	}

	/**
	 * Installs the {@code Ctrl+T} shortcut that toggles the lifespan strip.
	 *
	 * @param component the component that receives the shortcut
	 */
	public void setupStripShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		inputMap.put(GUIHelper.CTRL_T_STROKE, ACTION_TOGGLE_LIFESPANS_STRIP);
		actionMap.put(ACTION_TOGGLE_LIFESPANS_STRIP, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -2837918204710294817L;

			@Override
			public void actionPerformed(final ActionEvent e){
				toggleStrip();
			}
		});
	}

	/**
	 * Toggles the visibility of the lifespan strip. Called both by the
	 * keyboard shortcut and by the mouse click on the toggle bar.
	 */
	private void toggleStrip(){
		stripVisible = !stripVisible;
		lifespansStrip.setVisible(stripVisible);

		toggleBar.repaint();
		revalidate();
		repaint();
	}

	/**
	 * Collects the ids of every individual currently displayed in the tree
	 * and passes them to the lifespan strip.
	 * <p>
	 * The collection is performed by walking the whole component hierarchy
	 * under {@link #treeCanvas} and picking up every {@link IndividualPanel}
	 * that has data. This captures:
	 * <ul>
	 *   <li>the root individual and its partner;</li>
	 *   <li>every ancestor (father and mother of each couple node);</li>
	 *   <li>the partners shown next to each ancestor, when the
	 *       {@code showPartner} flag is enabled;</li>
	 *   <li>every sibling displayed in the children panel.</li>
	 * </ul>
	 * Walking the component tree rather than the {@link TreeNode} map is the
	 * only way to include the partners, which are not represented as separate
	 * tree nodes but only as panels inside a {@code PartnersPanel}.
	 * Duplicates are removed automatically by the {@link Set}.
	 */
	private void updateStripIndividuals(){
		final Set<String> ids = new LinkedHashSet<>();
		collectIndividualIds(treeCanvas, ids);
		lifespansStrip.setIndividuals(ids);
	}

	/**
	 * Recursively collects the ids of every {@link IndividualPanel} with
	 * non-empty data found in the subtree rooted at the given component.
	 *
	 * @param component the root of the subtree to walk
	 * @param ids       the accumulator set
	 */
	private static void collectIndividualIds(final Component component, final Set<String> ids){
		if(component instanceof IndividualPanel panel){
			final IndividualData data = panel.getData();
			if(data != null && !data.isEmpty() && data.getId() != null)
				ids.add(data.getId());
		}
		if(component instanceof Container container)
			for(final Component child : container.getComponents())
				collectIndividualIds(child, ids);
	}

	/**
	 * Thin header bar at the bottom of the panel. Clicking it toggles the
	 * lifespan strip.
	 */
	private final class ToggleBar extends JPanel{

		@Serial
		private static final long serialVersionUID = 5172038401928374591L;

		private static final int HEIGHT = 20;
		private static final Color BG = new Color(238, 234, 226);
		private static final Color BG_HOVER = new Color(230, 225, 214);
		private static final Color BORDER = new Color(210, 205, 195);
		private static final Color TEXT = new Color(70, 60, 40);
		private static final Font FONT = new Font("Tahoma", Font.PLAIN, 11);

		private boolean hovered;


		ToggleBar(){
			setPreferredSize(new Dimension(0, HEIGHT));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					toggleStrip();
				}

				@Override
				public void mouseEntered(final MouseEvent e){
					hovered = true;
					repaint();
				}

				@Override
				public void mouseExited(final MouseEvent e){
					hovered = false;
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);
			if(!(g instanceof Graphics2D g2))
				return;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setColor(hovered? BG_HOVER: BG);
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.setColor(BORDER);
			g2.drawLine(0, 0, getWidth(), 0);

			// Arrow: drawn with a Path2D rather than a font glyph, because the
			// Unicode triangle characters (U+25B6, U+25BC) are not present in
			// every font on every platform and may render as replacement glyphs.
			final int arrowSize = 8;
			final int arrowX = 10;
			final int arrowY = (getHeight() - arrowSize) / 2;
			drawArrow(g2, arrowX, arrowY, arrowSize, stripVisible);

			// Label.
			final String label = "Lifespans";
			g2.setFont(FONT);
			g2.setColor(TEXT);
			final FontMetrics fm = g2.getFontMetrics();
			g2.drawString(label, arrowX + arrowSize + 6,
				(getHeight() + fm.getAscent()) / 2 - 2);
		}

		/**
		 * Draws a solid triangle arrow.
		 * <p>
		 * When {@code expanded} is {@code true}, the triangle points down
		 * (▼); otherwise it points right (▶). The triangle is drawn with a
		 * Path2D so that rendering does not depend on the presence of Unicode
		 * geometric-shape characters in the current font.
		 *
		 * @param g2       the graphics context
		 * @param x        the left edge of the triangle
		 * @param y        the top edge of the triangle
		 * @param size     the side length of the triangle
		 * @param expanded whether the triangle points down (expanded) or right
		 */
		private static void drawArrow(final Graphics2D g2, final int x, final int y, final int size,
				final boolean expanded){
			final java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
			if(expanded){
				// Downward triangle: top-left, top-right, bottom-center.
				path.moveTo(x, y);
				path.lineTo(x + size, y);
				path.lineTo(x + size / 2.0, y + size);
			}
			else{
				// Rightward triangle: top-left, bottom-left, right-center.
				path.moveTo(x, y);
				path.lineTo(x, y + size);
				path.lineTo(x + size, y + size / 2.0);
			}
			path.closePath();

			g2.setColor(TEXT);
			g2.fill(path);
		}

	}

	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static String[] computeAllowedRelationshipTypes(final TreeType treeType){
		return switch(treeType){
			case BIOLOGICAL -> new String[]{ENUM_TYPE_BIOLOGICAL_CHILD};
			case FAMILY -> new String[]{ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD,
				ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD};
		};
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
		try(final InputStream is = IndividualTreePanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final IndividualTreePanel panel = new IndividualTreePanel(TreeType.BIOLOGICAL, TreeLayout.VERTICAL, model)
//			final IndividualTreePanel panel = new IndividualTreePanel(TreeType.FAMILY, TreeLayout.VERTICAL, model)
				.withShowPartner();
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


	/**
	 * Sub-panel that hosts the tree layout and draws the connection lines.
	 * <p>
	 * The tree content is no longer hosted directly by
	 * {@link IndividualTreePanel}, because the panel now also contains the
	 * bottom strip. The canvas is the single child of the center region and
	 * inherits all the painting responsibilities that used to belong to the
	 * outer panel.
	 */
	private final class TreeCanvas extends JPanel{

		@Serial
		private static final long serialVersionUID = -4019283750192847103L;


		TreeCanvas(){
			setBackground(BACKGROUND_COLOR_APPLICATION);
			setOpaque(true);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);

			if(!(g instanceof Graphics2D g2))
				return;

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setColor(CONNECTION_LINE_COLOR);
			g2.setStroke(PartnersPanel.CONNECTION_STROKE);

			TreeRenderer.drawTree(g2, treeLayout, rootNode, nodeToPanelMap, childrenPanel, this);
		}

	}

}
