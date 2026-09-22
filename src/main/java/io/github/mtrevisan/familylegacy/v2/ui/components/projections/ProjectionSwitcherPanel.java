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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.IndividualTreeGraphPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.GraphLayoutEngine;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayout;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.layout.TreeLayoutEngine;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph.EgoNetworkPanel;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Container that hosts three projection views (ancestor tree, Sugiyama
 * pedigree graph, ego network) and allows the user to cycle through them
 * with keyboard shortcuts.
 * <p>
 * <b>Shortcuts.</b> Four shortcuts are installed on the panel itself
 * with {@link JComponent#WHEN_IN_FOCUSED_WINDOW}:
 * <ul>
 *   <li>{@code Ctrl+E} cycles to the next projection;</li>
 *   <li>{@code Ctrl+1} switches to the ancestor tree;</li>
 *   <li>{@code Ctrl+2} switches to the Sugiyama graph;</li>
 *   <li>{@code Ctrl+3} switches to the ego network.</li>
 * </ul>
 * The same accelerators are also set on the View menu items, purely as
 * a visual hint. The panel bindings exist because menu accelerators can
 * be shadowed by component-level bindings or intercepted by the window
 * manager on some platforms; having both guarantees the switch always
 * fires.
 * <p>
 * <b>Switching.</b> The projection is swapped in place, with no
 * cross-fade: the new panel replaces the old one and a synchronous
 * recursive layout ensures the incoming projection has valid bounds
 * before the next paint. The asynchronous validation pass scheduled by
 * Swing would normally handle this, but on some look-and-feels it does
 * not reach the inner canvases before the first frame, leaving the
 * first screen blank; forcing the layout here makes the very first
 * frame identical to every subsequent one.
 */
public final class ProjectionSwitcherPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 4815718233118059642L;


	/** Default generation depth used when loading the tree or the graph. */
	private static final int DEFAULT_MAX_ANCESTORS = 2;

	/** Background color of the container. Matches the Sugiyama canvas. */
	private static final Color BACKGROUND = new Color(250, 249, 245);

	private static final String ACTION_PROJECTION_TREE = "projectionTree";
	private static final String ACTION_PROJECTION_SUGIYAMA = "projectionSugiyama";
	private static final String ACTION_PROJECTION_EGO = "projectionEgo";
	private static final TreeLayoutEngine TREE_LAYOUT_ENGINE = new TreeLayoutEngine();
	private static final GraphLayoutEngine GRAPH_LAYOUT_ENGINE = new GraphLayoutEngine();


	private final IndividualTreeGraphPanel treeGraphPanel;
	private final EgoNetworkPanel egoPanel;

	/** Shared browser-style navigation history across the three views. */
	private final NavigationHistory navigationHistory = new NavigationHistory();

	private JPanel currentPanel;

	/**
	 * Whether the first valid paint has already forced a synchronous
	 * recursive layout of the current projection's subtree.
	 */
	private boolean initialLayoutDone;

	/**
	 * Listener fired whenever the current root changes, regardless of
	 * the source. Used by the enclosing frame to keep the dossiers in
	 * sync with the active projection.
	 */
	private Consumer<String> navigationListener;

	/** Listener fired whenever the active projection changes. */
	private Consumer<ProjectionType> projectionChangeListener;


	public ProjectionSwitcherPanel(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.treeGraphPanel = new IndividualTreeGraphPanel(TreeType.BIOLOGICAL, TreeLayout.VERTICAL, TREE_LAYOUT_ENGINE,
				model)
			.withShowPartner();
		this.egoPanel = new EgoNetworkPanel(TreeLayout.VERTICAL, model);

		setOpaque(true);
		setBackground(BACKGROUND);

		setLayout(new BorderLayout());
		currentPanel = treeGraphPanel;
		add(treeGraphPanel, BorderLayout.CENTER);

		treeGraphPanel.withNavigationCallback(navigationHistory::push);
		egoPanel.withNavigationCallback(navigationHistory::push);

		setupSwitchShortcut(this);
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Loads the given individual as the root of the currently visible
	 * view. Only the current panel is populated: the other two are left
	 * untouched and will be synchronized on the next switch.
	 * <p>
	 * The call pushes the new root into the navigation history, exactly
	 * like a user click. Use {@link #navigateBack()} and
	 * {@link #navigateForward()} to move the cursor without pushing.
	 *
	 * @param individualId the id of the individual to use as root; may be
	 *                     {@code null} to leave the view unchanged
	 */
	public void loadRoot(final String individualId){
		if(individualId == null)
			return;

		if(currentPanel == treeGraphPanel)
			treeGraphPanel.load(individualId, DEFAULT_MAX_ANCESTORS);
		else
			egoPanel.load(individualId);

		notifyNavigation(individualId);
	}

	/**
	 * Returns the type of the currently visible projection.
	 *
	 * @return the active projection type, never {@code null}
	 */
	public ProjectionType getCurrentProjectionType(){
		if(currentPanel == treeGraphPanel)
			return (treeGraphPanel.getLayoutEngine() == TREE_LAYOUT_ENGINE? ProjectionType.TREE: ProjectionType.GRAPH);

		return ProjectionType.EGO_NETWORK;
	}

	/**
	 * Switches to the given projection and synchronizes the current root.
	 *
	 * @param type the projection to activate; must not be {@code null}
	 */
	public void setProjection(final ProjectionType type){
		if(type == null)
			return;

		final JPanel target = switch(type){
			case TREE -> {
				treeGraphPanel.setLayoutEngine(TREE_LAYOUT_ENGINE);
				yield treeGraphPanel;
			}
			case GRAPH -> {
				treeGraphPanel.setLayoutEngine(GRAPH_LAYOUT_ENGINE);
				yield treeGraphPanel;
			}
			case EGO_NETWORK -> egoPanel;
		};

		// Synchronize the target projection's root before swapping
		final String currentRootId = getSelectedEntityId();
		if(currentRootId != null){
			if(target == treeGraphPanel)
				treeGraphPanel.load(currentRootId, DEFAULT_MAX_ANCESTORS);
			else if(target == egoPanel)
				egoPanel.load(currentRootId);
		}

		swapPanels(target);

		notifyProjectionChanged(type);
	}

	private void notifyProjectionChanged(final ProjectionType type){
		if(projectionChangeListener != null)
			projectionChangeListener.accept(type);
	}

	/**
	 * Registers a callback invoked whenever an individual is selected in
	 * any of the hosted views.
	 *
	 * @param callback the callback; may be {@code null} to remove it
	 */
	public void setSelectionCallback(final Consumer<String> callback){
		treeGraphPanel.withSelectionCallback(callback);
		egoPanel.withSelectionCallback(callback);
	}

	/**
	 * Registers a callback invoked whenever a group is selected in any of
	 * the hosted views.
	 *
	 * @param callback the callback; may be {@code null} to remove it
	 */
	public void setGroupSelectionCallback(final Consumer<String> callback){
		egoPanel.withGroupSelectionCallback(callback);
	}

	/**
	 * Registers a listener invoked whenever the current root changes,
	 * regardless of the source.
	 *
	 * @param listener the listener; may be {@code null} to remove it
	 */
	public ProjectionSwitcherPanel withNavigationListener(final Consumer<String> listener){
		navigationListener = listener;

		return this;
	}

	/**
	 * Registers a listener invoked whenever the active projection changes.
	 *
	 * @param listener the listener; may be {@code null} to remove it
	 */
	public ProjectionSwitcherPanel setProjectionChangeListener(final Consumer<ProjectionType> listener){
		projectionChangeListener = listener;

		return this;
	}

	/**
	 * Opens the edit dialog for the currently selected entity in the
	 * active projection.
	 */
	public void editCurrentSelection(){
		if(currentPanel == treeGraphPanel)
			treeGraphPanel.editCurrentSelection();
		else
			egoPanel.editCurrentSelection();
	}

	/**
	 * Moves the visual selection of the currently active projection in the
	 * given direction.
	 *
	 * @param direction the direction to move; must not be {@code null}
	 */
	public void moveSelection(final SpatialNavigation.Direction direction){
		if(direction == null)
			return;

		if(currentPanel == treeGraphPanel)
			treeGraphPanel.moveSelection(direction);
		else
			egoPanel.moveSelection(direction);
	}

	/**
	 * Confirms the current selection of the active projection by making it
	 * the new root.
	 */
	public void confirmSelection(){
		final String id = getSelectedEntityId();
		if(currentPanel == treeGraphPanel)
			treeGraphPanel.rootConfirmSelection(id);
		else
			egoPanel.egoConfirmSelection(id);
	}

	public JPanel getCurrentPanel(){
		return currentPanel;
	}

	public IndividualTreeGraphPanel getTreeGraphPanel(){
		return treeGraphPanel;
	}

	public EgoNetworkPanel getEgoPanel(){
		return egoPanel;
	}

	/**
	 * Returns the id of the entity currently selected in the active
	 * projection, or {@code null} when nothing is selected.
	 * <p>
	 * The definition of "selected" depends on the view:
	 * <ul>
	 *   <li>tree: the individual highlighted with the red border, which may
	 *       differ from the root;</li>
	 *   <li>Sugiyama: the root of the graph, which is the only focus the
	 *       view has;</li>
	 *   <li>ego network: the individual or group highlighted with the red
	 *       border.</li>
	 * </ul>
	 */
	public String getSelectedEntityId(){
		if(currentPanel == treeGraphPanel)
			return treeGraphPanel.getSelectedIndividualId();
		return egoPanel.getSelectedEntityId();
	}


	/* ======================================================================
	 *                          Navigation history
	 * ====================================================================== */

	/** Returns {@code true} when there is at least one previous entry. */
	public boolean canGoBack(){
		return navigationHistory.canGoBack();
	}

	/** Returns {@code true} when there is at least one next entry. */
	public boolean canGoForward(){
		return navigationHistory.canGoForward();
	}

	/** Moves the navigation history one step back and applies the previous entry. */
	public void navigateBack(){
		final String id = navigationHistory.goBack();
		if(id == null)
			return;

		navigateCurrentViewTo(id);
		notifyNavigation(id);
	}

	/** Moves the navigation history one step forward and applies the next entry. */
	public void navigateForward(){
		final String id = navigationHistory.goForward();
		if(id == null)
			return;

		navigateCurrentViewTo(id);
		notifyNavigation(id);
	}

	/**
	 * Clears the shared navigation history. The three views keep their
	 * current content; only the timeline is reset, so {@code canGoBack()}
	 * and {@code canGoForward()} both return {@code false} afterwards.
	 */
	public void clearHistory(){
		navigationHistory.clear();
	}


	/* ======================================================================
	 *                          Painting
	 * ====================================================================== */

	/**
	 * Forces a synchronous recursive layout of the current projection
	 * the first time the container is painted with a valid size.
	 * <p>
	 * The asynchronous validation pass scheduled by Swing may not have
	 * reached the current projection's inner canvases by the time the
	 * first frame is painted; those canvases would have zero bounds and
	 * their {@code paintComponent} would exit immediately, leaving the
	 * first screen without the tree background and without the
	 * connection lines. Forcing the layout here makes the very first
	 * frame identical to every subsequent one.
	 */
	@Override
	public void paint(final Graphics g){
		if(!initialLayoutDone && getWidth() > 0 && getHeight() > 0){
			doLayout();
			if(currentPanel != null)
				layoutRecursively(currentPanel);
			initialLayoutDone = true;
		}
		super.paint(g);
	}


	/* ======================================================================
	 *                          Shortcuts
	 * ====================================================================== */

	private void setupSwitchShortcut(final JComponent component){
		final InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = component.getActionMap();

		final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		bindProjection(inputMap, actionMap,
			KeyStroke.getKeyStroke(KeyEvent.VK_1, mask),
			ProjectionType.TREE, ACTION_PROJECTION_TREE);
		bindProjection(inputMap, actionMap,
			KeyStroke.getKeyStroke(KeyEvent.VK_2, mask),
			ProjectionType.GRAPH, ACTION_PROJECTION_SUGIYAMA);
		bindProjection(inputMap, actionMap,
			KeyStroke.getKeyStroke(KeyEvent.VK_3, mask),
			ProjectionType.EGO_NETWORK, ACTION_PROJECTION_EGO);
	}

	private void bindProjection(final InputMap inputMap, final ActionMap actionMap,
		final KeyStroke stroke, final ProjectionType type, final String actionKey){
		inputMap.put(stroke, actionKey);
		actionMap.put(actionKey, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 8512390128374019283L;

			@Override
			public void actionPerformed(final ActionEvent e){
				setProjection(type);
			}
		});
	}


	/* ======================================================================
	 *                          Switching
	 * ====================================================================== */

	private void navigateCurrentViewTo(final String id){
		if(currentPanel == treeGraphPanel)
			treeGraphPanel.navigateTo(id);
		else
			egoPanel.navigateTo(id);
	}

	private void notifyNavigation(final String id){
		if(navigationListener != null && id != null)
			navigationListener.accept(id);
	}

	/**
	 * Replaces the visible projection with the given target and forces a
	 * synchronous recursive layout of the target's subtree, so the next
	 * paint sees the incoming projection at its final bounds. Without
	 * the forced layout, the tree canvas and its connection lines would
	 * not be drawn on the first frame after the switch, because the
	 * asynchronous validation pass scheduled by Swing might not have
	 * reached them yet.
	 */
	private void swapPanels(final JPanel target){
		removeAll();
		add(target, BorderLayout.CENTER);
		currentPanel = target;

		// Force the layout on the container and on the target's subtree,
		// so the very next paint finds every inner canvas at its final
		// bounds. Revalidate afterwards to notify the layout managers up
		// the tree that the structure has changed.
		doLayout();
		layoutRecursively(target);

		revalidate();
		repaint();
	}

	/**
	 * Recursively lays out a container and its visible descendants. This
	 * is the synchronous counterpart of the asynchronous validation pass
	 * that Swing schedules through {@code revalidate()}.
	 */
	private static void layoutRecursively(final Container container){
		container.doLayout();
		for(final Component child : container.getComponents()){
			if(child instanceof Container c && c.isVisible())
				layoutRecursively(c);
		}
	}


	/* ======================================================================
	 *                          Bootstrap
	 * ====================================================================== */

	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){
		}

		final String modelUri = "/tests/TGMZ.flef";
		final String individualId = "I1";

		final String content;
		try(final InputStream is = ProjectionSwitcherPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final ProjectionSwitcherPanel panel = new ProjectionSwitcherPanel(model);
			panel.loadRoot(individualId);

			final JFrame frame = new JFrame("Projection Switcher View");
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
