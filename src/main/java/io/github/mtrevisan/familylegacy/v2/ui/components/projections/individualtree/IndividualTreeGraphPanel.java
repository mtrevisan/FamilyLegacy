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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.JumpToIndividualDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.ProjectionType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.SpatialNavigation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityPopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.EntityTreePopupMenuFactory;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.GraphLayoutEngine;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.LayoutEngine;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayoutBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.CollapsibleBar;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.kinship.KinshipDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.lifespan.MultiLifespanStripPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.RelationshipOperationCoordinator;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ViewportPanSupport;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Generalized panel responsible for rendering an N-generation genealogical
 * tree layout dynamically.
 * <p>
 * The panel is a thin orchestrator: it owns the root state, delegates
 * layout and rendering to {@link TreeLayoutBuilder} and
 * {@link LayoutEngine}, and delegates the other concerns to dedicated
 * collaborators:
 * <ul>
 *   <li>{@link IndividualTreeGraphListener} handles the interaction events
 *       (edit, remove, add, unlink, paste);</li>
 *   <li>{@link TreeSelectionController} manages the visual selection
 *       and the spatial navigation;</li>
 *   <li>{@link PedigreeCollapseController} detects collapses and
 *       decorates the affected panels;</li>
 *   <li>{@link LifespanStripController} manages the bottom strip;</li>
 *   <li>{@link TreeShortcutInstaller} installs the keyboard shortcuts.</li>
 * </ul>
 * The panel keeps only the wiring, the root lifecycle, and the UI
 * composition.
 */
public class IndividualTreeGraphPanel extends JPanel implements TreeChangeListener{

	@Serial
	private static final long serialVersionUID = 9011391311012465249L;


	private static final Color BACKGROUND_COLOR = new Color(242, 238, 228);
	private static final Color CONNECTION_LINE_COLOR = Color.BLACK;

	private static final String ENUM_TYPE_BIOLOGICAL_CHILD = "biological_child";
	private static final String ENUM_TYPE_ADOPTIVE_CHILD = "adoptive_child";
	private static final String ENUM_TYPE_STEP_CHILD = "step_child";
	private static final String ENUM_TYPE_FOSTER_CHILD = "foster_child";
	private static final String ENUM_TYPE_GUARDED_CHILD = "guarded_child";


	private final FLEFModel model;
	private final TreeService treeService;

	/** Interaction listener installed on every created panel. */
	private final IndividualTreeGraphListener treeListener;
	/** Visual selection and spatial navigation. */
	private final TreeSelectionController selection;
	/** Pedigree collapse detection and badge application. */
	private final PedigreeCollapseController collapses;
	/** Bottom lifespan strip. */
	private final LifespanStripController lifespanStrip;

	private TreeLayout treeLayout;
	private LayoutEngine layoutEngine;
	private boolean showPartner;

	private String currentRootIndividualId;
	private int currentMaxAncestors;
	private TreeNode rootNode;

	private final Map<TreeNode, PartnersPanel> nodeToPanelMap = new LinkedHashMap<>();
	private SiblingsPanel childrenPanel;

	private Path2D cachedTreePath;

	private final JPanel treeCanvas = new TreeCanvas();
	private final JPanel centeringWrapper = new JPanel(new GridBagLayout());
	private final JScrollPane treeScroll;

	private Consumer<String> selectionCallback;
	private Consumer<String> navigationCallback;
	private boolean suppressNavigationNotification;


	public IndividualTreeGraphPanel(final TreeType treeType, final TreeLayout treeLayout,
			final LayoutEngine layoutEngine, final FLEFModel model){
		this.treeLayout = treeLayout;
		this.layoutEngine = layoutEngine;
		this.model = model;

		final String[] allowedTypes = computeAllowedRelationshipTypes(treeType);
		final Predicate<String> typeFilter = type ->
			ArrayUtils.contains(allowedTypes, type.toLowerCase(Locale.ROOT));
		treeService = new TreeService(typeFilter, model);
		final TreeMutator treeMutator = new TreeMutator(typeFilter, model, treeService, this);
		final IndividualDialogProvider dialogProvider = new IndividualDialogProvider(model);
		final RelationshipOperationCoordinator operationCoordinator = new RelationshipOperationCoordinator(model,
			treeMutator, allowedTypes);

		selection = new TreeSelectionController(
			this::notifySelection,
			this::rootConfirmSelection);
		collapses = new PedigreeCollapseController(model);

		final MultiLifespanStripPanel strip = new MultiLifespanStripPanel(model);
		final CollapsibleBar toggleBar = new CollapsibleBar("Lifespans");
		lifespanStrip = new LifespanStripController(strip, toggleBar, this::revalidate);
		toggleBar.withListener(lifespanStrip::toggle);

		treeListener = new IndividualTreeGraphListener(model, treeService, treeMutator,
			dialogProvider, operationCoordinator, this,
			this::getRootIndividualId,
			() -> nodeToPanelMap,
			this::notifySelection);

		setBackground(BACKGROUND_COLOR);
		setOpaque(true);
		setLayout(new BorderLayout());

		centeringWrapper.setBackground(BACKGROUND_COLOR);
		centeringWrapper.setOpaque(true);
		centeringWrapper.add(treeCanvas);

		treeScroll = new JScrollPane(centeringWrapper,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treeScroll.setBorder(null);
		final JViewport viewport = treeScroll.getViewport();
		viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		viewport.setBackground(BACKGROUND_COLOR);
		treeScroll.getVerticalScrollBar()
			.setUnitIncrement(16);
		treeScroll.getHorizontalScrollBar()
			.setUnitIncrement(16);
		add(treeScroll, BorderLayout.CENTER);

		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(toggleBar, BorderLayout.NORTH);
		bottom.add(strip, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		TreeShortcutInstaller.installAll(this,
			this::toggleLayout,
			this::openKinshipDialog,
			() -> collapses.showDialog(SwingUtilities.getWindowAncestor(this)),
			lifespanStrip::toggle);
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public IndividualTreeGraphPanel withLayoutEngine(final LayoutEngine layoutEngine){
		this.layoutEngine = layoutEngine;

		refreshTree();

		return this;
	}

	public IndividualTreeGraphPanel withShowPartner(){
		showPartner = true;

		return this;
	}

	public IndividualTreeGraphPanel withSelectionCallback(final Consumer<String> callback){
		selectionCallback = callback;

		return this;
	}

	public IndividualTreeGraphPanel withNavigationCallback(final Consumer<String> callback){
		navigationCallback = callback;

		return this;
	}

	/** Loads the given root without notifying the navigation callback. */
	public void navigateTo(final String individualId){
		suppressNavigationNotification = true;
		try{
			load(individualId, currentMaxAncestors);
		}
		finally{
			suppressNavigationNotification = false;
		}
	}

	public void load(final String rootIndividualId, final int maxAncestors){
		currentRootIndividualId = rootIndividualId;
		currentMaxAncestors = maxAncestors;

		refreshTree();

		if(!suppressNavigationNotification && navigationCallback != null && rootIndividualId != null)
			navigationCallback.accept(rootIndividualId);
	}

	public String getRootIndividualId(){
		if(currentRootIndividualId != null)
			return currentRootIndividualId;

		if(rootNode == null)
			return null;

		final String ownId = rootNode.getIndividualId();
		if(ownId != null)
			return ownId;

		final TreeNode father = rootNode.getFather();
		return (father != null && father.getIndividual() != null? father.getIndividual().getId(): null);
	}

	public String getSelectedIndividualId(){
		return selection.selectedId();
	}

	/** Opens the editor for the currently selected individual. */
	public void editCurrentSelection(){
		final String id = selection.selectedId();
		if(id == null)
			return;

		final FLEFRecord record = model.getRecordById(id);
		if(record != null)
			treeListener.onEntityEdit(record);
	}

	/** Moves the visual selection in the given direction. */
	public void moveSelection(final SpatialNavigation.Direction direction){
		selection.move(direction, treeCanvas, currentRootIndividualId);
	}

	/** Confirms the selection, re-rooting the tree on it. */
	public void rootConfirmSelection(final String selectedId){
		if(selectedId == null)
			return;

		onTreeStructureChanged(selectedId);
	}

	@Override
	public void onTreeStructureChanged(final String rootIndividualId){
		if(rootIndividualId == null){
			// The deleted individual was the root and had no usable
			// relative: ask the user to pick a new root through the
			// Jump to Individual dialog. Deferred so the tree canvas has
			// time to clear the old root before the modal dialog opens.
			SwingUtilities.invokeLater(this::openJumpToDialogAfterDeletion);

			return;
		}

		SwingUtilities.invokeLater(() -> load(rootIndividualId, currentMaxAncestors));
	}

	/**
	 * Opens the Jump to Individual dialog after a deletion that left no
	 * fallback root, and loads the chosen individual. Does nothing when
	 * the user cancels.
	 */
	private void openJumpToDialogAfterDeletion(){
		final Window owner = SwingUtilities.getWindowAncestor(this);
		// Here the panel is either the tree or the Sugiyama graph; both
		// offer only individuals, so ProjectionType.TREE is fine.
		final String chosen = JumpToIndividualDialog.showAndGet(owner, model, ProjectionType.TREE);
		if(chosen != null)
			load(chosen, currentMaxAncestors);
	}

	public static String[] computeAllowedRelationshipTypes(final TreeType treeType){
		return switch(treeType){
			case BIOLOGICAL -> new String[]{ENUM_TYPE_BIOLOGICAL_CHILD};
			case FAMILY -> new String[]{
				ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD,
				ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD,
				ENUM_TYPE_STEP_CHILD};
		};
	}


	/* ======================================================================
	 *                          Tree lifecycle
	 * ====================================================================== */

	private void refreshTree(){
		if(!SwingUtilities.isEventDispatchThread()){
			SwingUtilities.invokeLater(this::refreshTree);

			return;
		}

		final TreeNode rootIndividualNode = treeService.buildTree(currentRootIndividualId, showPartner,
			currentMaxAncestors);

		centeringWrapper.setVisible(false);

		try{
			nodeToPanelMap.clear();
			treeCanvas.removeAll();
			selection.clear();

			rootNode = rootIndividualNode;
			if(rootIndividualNode != null && showPartner){
				final IndividualData partnerData = rootIndividualNode.getPartnerData();
				final String partnerId = (partnerData != null? partnerData.getId(): null);
				final TreeNode partnerNode = treeService.buildTree(partnerId, showPartner, currentMaxAncestors);
				final SexType sex = rootIndividualNode.getIndividualData().getSex();

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

			collapses.detect(rootNode);
			buildLayout();
			// Force the wrapper to recompute its preferred size (which includes the
			// canvas), so the scroll pane updates its view size and shows scrollbars
			// when needed. The canvas is either stretched (content smaller than the
			// viewport) or scrolled (content larger), never clipped.
			centeringWrapper.doLayout();
			treeScroll.revalidate();
			collapses.apply(nodeToPanelMap);

			selection.updateTree(nodeToPanelMap, childrenPanel);
			if(selection.selectedId() == null && currentRootIndividualId != null)
				selection.setSelectedId(currentRootIndividualId);
			selection.apply();

			lifespanStrip.update(treeCanvas);

			// Pan support: idempotent on the persistent parts of the
			// hierarchy, fresh on the panels that were just recreated.
			ViewportPanSupport.install(treeScroll, centeringWrapper,
				c -> (c instanceof JScrollPane && c != treeScroll));
		}
		finally{
			centeringWrapper.setVisible(true);
		}

		treeCanvas.revalidate();
		treeCanvas.repaint();
		revalidate();
		repaint();

		SwingUtilities.invokeLater(() -> {
			cachedTreePath = layoutEngine.buildTreePath(treeLayout, rootNode, nodeToPanelMap, childrenPanel, treeCanvas);
			treeCanvas.repaint();
		});
	}

	private void buildLayout(){
		final EntityPopupMenuFactory<IndividualPanel, IndividualListener> popupFactory = new EntityTreePopupMenuFactory();
		childrenPanel = layoutEngine.buildLayout(treeCanvas, rootNode, showPartner, currentMaxAncestors, model,
			nodeToPanelMap, treeListener, popupFactory, treeLayout);
	}

	private void notifySelection(final String id){
		if(id != null){
			selection.setSelectedId(id);
			selection.apply();
		}
		if(selectionCallback != null && id != null)
			selectionCallback.accept(id);
	}


	/* ======================================================================
	 *                          Actions
	 * ====================================================================== */

	private void toggleLayout(){
		treeLayout = (treeLayout == TreeLayout.VERTICAL? TreeLayout.HORIZONTAL: TreeLayout.VERTICAL);
		refreshTree();

		final Window window = SwingUtilities.getWindowAncestor(this);
		if(window != null){
			window.pack();
			window.setLocationRelativeTo(null);
		}
	}

	private void openKinshipDialog(){
		final Window parent = SwingUtilities.getWindowAncestor(this);
		final FLEFRecord initialA = (currentRootIndividualId != null? model.getRecordById(currentRootIndividualId): null);
		final KinshipDialog dialog = new KinshipDialog(parent, model, treeService, initialA, null);
		dialog.setVisible(true);
	}

	public LayoutEngine getLayoutEngine(){
		return layoutEngine;
	}

	public void setLayoutEngine(final LayoutEngine layoutEngine){
		this.layoutEngine = layoutEngine;
	}


	/* ======================================================================
	 *                          Canvas
	 * ====================================================================== */

	private final class TreeCanvas extends JPanel{

		@Serial
		private static final long serialVersionUID = -4019283750192847103L;


		TreeCanvas(){
			setBackground(BACKGROUND_COLOR);
			setOpaque(true);
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);

			if(!(g instanceof Graphics2D g2))
				return;

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			g2.setColor(CONNECTION_LINE_COLOR);
			g2.setStroke(PartnersPanel.CONNECTION_STROKE);

			if(cachedTreePath != null)
				g2.draw(cachedTreePath);
		}

	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

//		final String modelUri = "/tests/TGMZ.flef";
//		final String rootIndividualId = "I1";
		final String modelUri = "/tests/out.flef";
		final String rootIndividualId = "I4321";
		final int maxAncestors = 2;

		final String content;
		try(final InputStream is = IndividualTreeGraphPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
//			final LayoutEngine layoutEngine = new TreeLayoutEngine();
			final LayoutEngine layoutEngine = new GraphLayoutEngine();
			final IndividualTreeGraphPanel panel = new IndividualTreeGraphPanel(TreeType.BIOLOGICAL, TreeLayout.VERTICAL,
					layoutEngine, model)
				.withShowPartner();
			panel.load(rootIndividualId, maxAncestors);

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
