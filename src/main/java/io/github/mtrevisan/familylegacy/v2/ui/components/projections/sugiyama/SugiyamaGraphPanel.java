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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.sugiyama;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.SpatialNavigation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeChangeListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeService;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ViewportPanSupport;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Sugiyama view of the pedigree.
 * <p>
 * Every vertex is a real {@link IndividualPanel} positioned at the
 * coordinates computed by {@link SugiyamaGraphLayout}. Two spouses
 * appear as two adjacent boxes, because the vertex is the individual,
 * not the couple. The edges are drawn behind the panels.
 * <p>
 * No selection state: single-click re-roots the graph on the clicked
 * individual. A selection callback can be installed through
 * {@link #withSelectionCallback(Consumer)} so that an enclosing container
 * (typically a side panel or a dossier) is notified of the currently
 * focused individual.
 */
public class SugiyamaGraphPanel extends JPanel implements TreeChangeListener{

	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES = new String[]{
		"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child"
	};


	private final FLEFModel model;
	private final TreeService treeService;

	private SugiyamaGraphLayout.Result layout;

	/** Centering offsets applied to the content when it is smaller than the viewport. */
	private int centeringOffsetX;
	private int centeringOffsetY;

	private final Canvas canvas;
	private final JLayer<Component> layer;
	private final JScrollPane scrollPane;

	private int currentMaxAncestors;
	private int currentMaxDescendants;

	/**
	 * Id of the individual currently highlighted by the keyboard cursor.
	 * <p>
	 * The Sugiyama view has no selection separate from the root, so the
	 * cursor is introduced here: arrow keys move it, and Enter re-roots
	 * the graph on it. When the cursor is {@code null}, the root is the
	 * implicit focus.
	 */
	private String focusId;

	private boolean showPartner = true;

	/**
	 * Optional callback invoked whenever the user focuses an individual
	 * in this view (single click on a node). The callback receives the
	 * individual id. Used by the enclosing frame to keep a dossier panel
	 * in sync with the current selection.
	 */
	private Consumer<String> selectionCallback;

	/** Id of the individual currently rendered as the root of the graph. */
	private String currentRootId;
	/** Optional callback invoked whenever the root changes due to user navigation. */
	private Consumer<String> navigationCallback;
	/** When {@code true}, {@link #loadGraph(String, int, int, boolean)} does not notify the navigation callback. */
	private boolean suppressNavigationNotification;


	public SugiyamaGraphPanel(final FLEFModel model, final TreeService treeService){
		this.model = Objects.requireNonNull(model);
		this.treeService = Objects.requireNonNull(treeService);

		this.canvas = new Canvas();
		this.layer = new JLayer<>(canvas);
		this.scrollPane = new JScrollPane(layer,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.scrollPane.setBorder(null);
		this.scrollPane.getVerticalScrollBar()
			.setUnitIncrement(16);
		this.scrollPane.getHorizontalScrollBar()
			.setUnitIncrement(16);

		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
		setPreferredSize(new Dimension(1000, 700));

		// Recompute the canvas size and the centering offsets whenever the
		// viewport is resized, so that the graph stays centered when it is
		// smaller than the visible area.
		scrollPane.getViewport().addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent e){
				updateCanvasSize();

				canvas.rebuild();
			}
		});
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public void loadGraph(final String rootId, final int maxAncestors, final int maxDescendants){
		loadGraph(rootId, maxAncestors, maxDescendants, showPartner);
	}

	public void loadGraph(final String rootId, final int maxAncestors, final int maxDescendants,
			final boolean showPartner){
		if(rootId == null)
			return;

		this.currentRootId = rootId;
		this.currentMaxAncestors = maxAncestors;
		this.currentMaxDescendants = maxDescendants;
		this.showPartner = showPartner;

		this.layout = SugiyamaGraphLayout.compute(model, rootId, treeService, maxAncestors, maxDescendants, showPartner);

		// Compute the canvas size and the centering offsets BEFORE placing
		// the panels, so the bounds applied in rebuild() and the translation
		// applied in paintComponent() use the same offsets.
		updateCanvasSize();
		canvas.rebuild();

		// (Re)install drag-to-pan on the newly created panels. The canvas itself
		// is persistent, so the install is a no-op there; the panels are
		// recreated on every rebuild and receive the adapter here.
		ViewportPanSupport.install(scrollPane, canvas);

		scrollPane.getViewport().setViewPosition(new Point(0, 0));

		if(!suppressNavigationNotification && navigationCallback != null)
			navigationCallback.accept(rootId);

		// The cursor is reset on every reload.
		focusId = rootId;
	}

	/**
	 * Registers a callback invoked whenever an individual is focused in
	 * this view. The callback receives the individual id.
	 * <p>
	 * The callback fires on every single click on a node, before the
	 * graph is re-rooted on that individual. This order allows an
	 * enclosing container to populate a detail panel with the newly
	 * selected individual before the graph is rebuilt around it.
	 *
	 * @param callback the callback; may be {@code null} to remove it
	 */
	public SugiyamaGraphPanel withSelectionCallback(final Consumer<String> callback){
		this.selectionCallback = callback;

		return this;
	}

	/**
	 * Returns the id of the current root individual rendered in the graph.
	 * <p>
	 * The primary source is the layout result, which carries the root id
	 * as part of the computed graph. The method returns {@code null} when
	 * no graph has been loaded yet.
	 *
	 * @return the id of the root individual, or {@code null}
	 */
	public String getRootIndividualId(){
		if(layout == null)
			return null;

		return layout.rootId();
	}

	/**
	 * Opens the edit dialog for the individual currently shown as the root
	 * of the graph.
	 * <p>
	 * Unlike the tree and the ego network, the Sugiyama view has no
	 * separate selection state: the focused individual is the root itself,
	 * which carries the red border. When no graph is loaded, the call is a
	 * no-op.
	 * <p>
	 * On a successful edit the graph is reloaded around the same root,
	 * through {@link #navigateTo(String, int, int, boolean)}, so the navigation
	 * history is not polluted by the refresh.
	 */
	public void editCurrentRoot(){
		if(layout == null)
			return;

		final String rootId = layout.rootId();
		if(rootId == null)
			return;

		final FLEFRecord record = model.getRecordById(rootId);
		if(record == null)
			return;

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		if(handler == null)
			return;

		final Window owner = SwingUtilities.getWindowAncestor(this);
		final BaseRecordDialog dialog = handler.createEditDialog(owner, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			navigateTo(rootId, currentMaxAncestors, currentMaxDescendants, showPartner);
	}

	/**
	 * Moves the keyboard cursor to the closest individual in the given
	 * direction.
	 * <p>
	 * The cursor is a visual highlight independent from the root: the
	 * root keeps its larger primary box, while the cursor carries the red
	 * border. When no cursor is set, the root is used as the starting
	 * point. When there is no neighbour in that direction, the call is a
	 * no-op.
	 *
	 * @param direction the direction to move; must not be {@code null}
	 */
	public void moveFocus(final SpatialNavigation.Direction direction){
		final Map<String, Rectangle> bounds = collectVisibleBounds();
		if(bounds.isEmpty())
			return;

		String current = focusId;
		if(current == null || !bounds.containsKey(current))
			current = (layout != null? layout.rootId(): null);
		if(current == null || !bounds.containsKey(current))
			return;

		final String next = SpatialNavigation.next(bounds, current, direction);
		if(next == null || next.equals(focusId))
			return;

		focusId = next;
		updateFocusVisuals();
	}

	/**
	 * Confirms the current keyboard cursor by re-rooting the graph on it.
	 * <p>
	 * Behaves like a click on a node: the graph is rebuilt around the
	 * cursor, and the navigation is pushed into the history through the
	 * navigation callback. When the cursor is already the root, or no
	 * cursor is set, the call is a no-op.
	 */
	public void confirmFocus(){
		if(focusId == null || layout == null)
			return;
		if(focusId.equals(layout.rootId()))
			return;

		loadGraph(focusId, currentMaxAncestors, currentMaxDescendants, showPartner);
	}

	/**
	 * Collects the on-screen bounds of every visible panel. The layout
	 * coordinates are not used directly, because the canvas applies a
	 * centering offset that would have to be replicated here; screen
	 * coordinates are simpler and consistent with the other views.
	 */
	private Map<String, Rectangle> collectVisibleBounds(){
		final Map<String, Rectangle> result = new LinkedHashMap<>();
		for(final Map.Entry<String, IndividualPanel> entry : canvas.visiblePanels().entrySet()){
			final IndividualPanel panel = entry.getValue();
			try{
				final Point p = panel.getLocationOnScreen();
				result.put(entry.getKey(), new Rectangle(p.x, p.y,
					panel.getWidth(), panel.getHeight()));
			}
			catch(final java.awt.IllegalComponentStateException ignored){
				// The panel is not showing; skip it.
			}
		}
		return result;
	}

	/**
	 * Updates the red border on every visible panel to reflect the
	 * current cursor. Called after {@link #moveFocus(SpatialNavigation.Direction)} without
	 * rebuilding the layout, so the visual update is cheap.
	 */
	private void updateFocusVisuals(){
		for(final Map.Entry<String, IndividualPanel> entry : canvas.visiblePanels().entrySet())
			entry.getValue().withSelected(entry.getKey().equals(focusId));

		canvas.repaint();
	}

	@Override
	public void onTreeStructureChanged(final String rootIndividualId){
		SwingUtilities.invokeLater(() -> loadGraph(rootIndividualId, currentMaxAncestors, currentMaxDescendants, showPartner));
	}

	public String getSelectedIndividualId(){
		return layout.rootId();
	}


	/* ======================================================================
	 *                          Canvas
	 * ====================================================================== */

	private final class Canvas extends JPanel{

		private final Map<String, IndividualPanel> visiblePanels = new LinkedHashMap<>();

		Canvas(){
			setLayout(null);
			setOpaque(true);
			setBackground(SugiyamaEdgeRouter.BACKGROUND);
		}

		void rebuild(){
			removeAll();
			visiblePanels.clear();

			if(layout == null)
				return;

			for(final Map.Entry<String, Rectangle> entry : layout.nodeBounds().entrySet()){
				final String id = entry.getKey();
				if(layout.dummyIds().contains(id))
					continue;

				final IndividualData data = layout.dataById().get(id);
				if(data == null || data.isEmpty())
					continue;

				final boolean isRoot = id.equals(layout.rootId());
				final boolean isFocus = (focusId != null? focusId.equals(id): isRoot);
				final IndividualPanel panel = IndividualPanel
					.create(BoxPanelType.SECONDARY, model)
					.withIndividualData(data)
					.withSelected(isFocus);

				// Apply the centering offsets, so that a graph smaller than the
				// viewport is drawn in the middle of the canvas.
				final Rectangle bounds = entry.getValue();
				panel.setBounds(
					bounds.x + centeringOffsetX,
					bounds.y + centeringOffsetY,
					bounds.width,
					bounds.height);

				attachListeners(panel, id);
				add(panel);
				visiblePanels.put(id, panel);
			}

			revalidate();
			repaint();
		}

		Map<String, IndividualPanel> visiblePanels(){
			return visiblePanels;
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);

			if(!(g instanceof Graphics2D g2) || layout == null)
				return;

			// Translate the drawing context by the centering offsets, so that
			// the edges follow the panels. The original transform is restored
			// at the end, so the rest of the painting pipeline is not affected.
			final AffineTransform original = g2.getTransform();
			try{
				g2.translate(centeringOffsetX, centeringOffsetY);
				SugiyamaEdgeRouter.drawEdges(g2, layout);
			}
			finally{
				g2.setTransform(original);
			}
		}

		private void attachListeners(final Component component, final String id){
			final MouseAdapter adapter = new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					if(!SwingUtilities.isLeftMouseButton(e))
						return;

					if(e.getClickCount() == 1){
						// Notify the enclosing container of the newly selected
						// individual, then re-root the graph on it.
						if(selectionCallback != null)
							selectionCallback.accept(id);

						loadGraph(id, currentMaxAncestors, currentMaxDescendants, showPartner);
					}
				}
			};
			component.addMouseListener(adapter);
			if(component instanceof Container c)
				for(final Component child : c.getComponents())
					attachListeners(child, id);
		}
	}


	/* ======================================================================
	 *                          Viewport interaction
	 * ====================================================================== */

	/**
	 * Sizes the canvas so that it is at least as large as the viewport, and
	 * computes the centering offsets. When the graph content is smaller than
	 * the viewport, the offsets center it; otherwise they are zero and the
	 * scroll pane behaves normally.
	 */
	private void updateCanvasSize(){
		if(layout == null)
			return;

		final Rectangle content = layout.contentBounds();
		final Dimension viewportSize = scrollPane.getViewport().getExtentSize();

		final int contentW = Math.max(1, (int)Math.ceil(content.getWidth()));
		final int contentH = Math.max(1, (int)Math.ceil(content.getHeight()));
		final int w = Math.max(contentW, viewportSize.width);
		final int h = Math.max(contentH, viewportSize.height);

		centeringOffsetX = (w - contentW) / 2;
		centeringOffsetY = (h - contentH) / 2;

		canvas.setPreferredSize(new Dimension(w, h));
		canvas.revalidate();
	}

	/**
	 * Registers a callback invoked whenever the root of the graph changes
	 * due to user navigation (single click on a node). Used by the
	 * enclosing container to record the navigation in the back/forward
	 * history.
	 *
	 * @param callback the callback; may be {@code null} to remove it
	 */
	public SugiyamaGraphPanel withNavigationCallback(final Consumer<String> callback){
		this.navigationCallback = callback;

		return this;
	}

	/**
	 * Loads the given individual as the new root without notifying the
	 * navigation callback. Used by the back/forward history to move the
	 * cursor without generating a new history entry.
	 *
	 * @param rootId       the individual id
	 * @param maxAncestors the maximum number of ancestor generations
	 * @param showPartner  whether to show partners and descendants
	 */
	public void navigateTo(final String rootId, final int maxAncestors, final int maxDescendants,
			final boolean showPartner){
		suppressNavigationNotification = true;
		try{
			loadGraph(rootId, maxAncestors, maxDescendants, showPartner);
		}
		finally{
			suppressNavigationNotification = false;
		}
	}

	/** Returns the id of the individual currently rendered as the root. */
	public String getCurrentRootId(){
		return currentRootId;
	}


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";
		final String rootIndividualId = "I1";
		final int maxAscendants = 2;
		final int maxDescendants = 2;

		final String content;
		try(final InputStream is = SugiyamaGraphPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final Predicate<String> relationshipTypeFilter = type -> ArrayUtils.contains(INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES,
				type.toLowerCase(Locale.ROOT));
			final TreeService treeService = new TreeService(relationshipTypeFilter, model);

			final SugiyamaGraphPanel panel = new SugiyamaGraphPanel(model, treeService);
			panel.loadGraph(rootIndividualId, maxAscendants, maxDescendants);

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
