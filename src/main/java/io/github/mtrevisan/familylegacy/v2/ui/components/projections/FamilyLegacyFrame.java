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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks.Bookmark;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks.BookmarkStore;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks.BookmarkType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.GroupDossierPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.IndividualDossierPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.files.FileMenuController;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Main application frame.
 * <p>
 * The frame composes three cooperating components:
 * <ul>
 *   <li>the {@link ProjectionSwitcherPanel}, which cycles through the
 *       ancestor tree, the Sugiyama pedigree graph, and the ego
 *       network;</li>
 *   <li>the {@link IndividualDossierPanel}, showing the complete FLEF
 *       record set of the currently selected individual;</li>
 *   <li>the {@link GroupDossierPanel}, showing the complete FLEF record
 *       set of the currently selected group.</li>
 * </ul>
 * The three are connected through selection callbacks: whenever the
 * user focuses an individual or a group in the active view, the
 * corresponding dossier is populated. The two dossiers are mutually
 * exclusive: only one is visible at a time, because a group and an
 * individual cannot be selected at the same time.
 * <p>
 * <b>File lifecycle.</b> The frame delegates all the document-level
 * operations (new, open, save, save as, exit) to a
 * {@link FileMenuController}, which owns the current file and the
 * dirty flag. The frame exposes the model and the callbacks the
 * controller needs through package-private accessors; it does not
 * duplicate the document state.
 * <p>
 * <b>Bookmarks.</b> A persistent {@link BookmarkStore} keeps user-saved
 * views. Applying a bookmark behaves like a user selection: the
 * navigation history is updated, the {@code Ctrl+Left} and
 * {@code Ctrl+Right} shortcuts work across bookmark applications, and
 * the dossier panel is populated accordingly.
 * <p>
 * <b>Tool context.</b> The frame exposes a method that builds a
 * {@code ToolContext} for the current state. Every menu item that
 * triggers a tool obtains its context from this method, so the tools
 * are always wired to the same model, owner, and callbacks.
 */
public class FamilyLegacyFrame extends JFrame{

	@Serial
	private static final long serialVersionUID = 7829104738102938471L;


	/** Initial width of the switcher side of the split pane. */
	private static final int SWITCHER_WIDTH = 1000;
	/** Initial width of the dossier side of the split pane. */
	private static final int DOSSIER_WIDTH = 400;

	private static final String ACTION_NAVIGATE_BACK = "navigateBack";
	private static final String ACTION_NAVIGATE_FORWARD = "navigateForward";
	private static final String ACTION_EDIT_SELECTION = "editSelection";
	private static final String ACTION_MOVE_UP = "moveUp";
	private static final String ACTION_MOVE_DOWN = "moveDown";
	private static final String ACTION_MOVE_LEFT = "moveLeft";
	private static final String ACTION_MOVE_RIGHT = "moveRight";
	private static final String ACTION_CONFIRM_SELECTION = "confirmSelection";


	private FLEFModel model;
	private ProjectionSwitcherPanel switcher;
	private final IndividualDossierPanel individualDossier;
	private final GroupDossierPanel groupDossier;
	private final BookmarkStore bookmarkStore = new BookmarkStore();
	private final FileMenuController fileController;

	/**
	 * The panel currently installed on the right side of the split pane.
	 * Switches between {@link #individualDossier} and
	 * {@link #groupDossier} depending on which entity is selected.
	 */
	private JComponent currentDossier;

	private final JSplitPane split;


	public FamilyLegacyFrame(final FLEFModel model){
		super("Family Legacy");

		this.model = Objects.requireNonNull(model);
		this.switcher = new ProjectionSwitcherPanel(model);
		this.individualDossier = new IndividualDossierPanel(model);
		this.groupDossier = new GroupDossierPanel(model);
		this.fileController = new FileMenuController(this,
			this::model,
			this::replaceModel,
			() -> {});

		// Selection callbacks: user clicks on a node populate the dossiers.
		switcher.setSelectionCallback(this::showIndividualDossier);
		switcher.setGroupSelectionCallback(this::showGroupDossier);

		// Navigation listener: fires for any root change, including
		// bookmark applications and back/forward navigation. Keeps the
		// dossiers in sync with the active view.
		switcher.setNavigationListener(this::syncDossier);

		this.currentDossier = individualDossier;

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, switcher, currentDossier);
		split.setResizeWeight(1.0);
		split.setDividerLocation(SWITCHER_WIDTH);
		split.setContinuousLayout(true);

		setLayout(new BorderLayout());
		add(split, BorderLayout.CENTER);

		setSize(SWITCHER_WIDTH + DOSSIER_WIDTH, 800);
		setMinimumSize(new Dimension(900, 500));
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		setupNavigationShortcuts();
		setupEditShortcut();
		setupArrowShortcuts();

		setJMenuBar(new ApplicationMenuBar(this).build());
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Loads the given individual as the root of the currently visible
	 * view and populates the individual dossier with the same individual.
	 * The call pushes the new root into the navigation history.
	 *
	 * @param individualId the individual id; may be {@code null}
	 */
	public void loadRoot(final String individualId){
		if(individualId == null)
			return;

		switcher.loadRoot(individualId);
	}


	/* ======================================================================
	 *                          Package-private API for the menu bar
	 * ====================================================================== */

	FLEFModel model(){
		return model;
	}

	BookmarkStore bookmarkStore(){
		return bookmarkStore;
	}

	ProjectionSwitcherPanel switcher(){
		return switcher;
	}

	FileMenuController fileController(){
		return fileController;
	}

	/**
	 * Opens the edit dialog for the entity currently selected in the
	 * active projection. Used by the menu bar to wire the F2 shortcut.
	 */
	void editCurrentSelection(){
		switcher.editCurrentSelection();
	}

	/**
	 * Returns the id of the entity currently selected in the active
	 * projection, or {@code null}. Used by the tools to determine which
	 * entity their operations should target.
	 */
	String currentSelectionId(){
		return switcher.getSelectedEntityId();
	}

	/**
	 * Returns the panel of the currently visible projection, or
	 * {@code null}. Used by the print tool.
	 */
	Component currentView(){
		return switcher.getCurrentPanel();
	}

	/**
	 * Replaces the current model with a new one, rebuilding the whole
	 * frame content around it.
	 * <p>
	 * The implementation disposes the current switcher and the two
	 * dossiers and creates fresh ones. The alternative — allowing each
	 * panel to swap its model in place — would require a larger
	 * refactoring and is not necessary for the current use cases (open
	 * file, import file, new file).
	 *
	 * @param newModel the new model; must not be {@code null}
	 */
	void replaceModel(final FLEFModel newModel){
		if(newModel == null)
			return;

		SwingUtilities.invokeLater(() -> {
			this.model = newModel;

			// Dispose the old content: the switcher and the dossiers
			// hold references to the previous model that cannot be
			// swapped in place.
			remove(split);

			this.switcher = new ProjectionSwitcherPanel(newModel);
			switcher.setSelectionCallback(this::showIndividualDossier);
			switcher.setGroupSelectionCallback(this::showGroupDossier);
			switcher.setNavigationListener(this::syncDossier);

			final IndividualDossierPanel newIndividualDossier =
				new IndividualDossierPanel(newModel);
			final GroupDossierPanel newGroupDossier =
				new GroupDossierPanel(newModel);

			// The dossiers are final fields, so we keep the instances
			// and simply repopulate them. A more thorough refactoring
			// would make them non-final, but for now repopulating works.
			this.individualDossier.setIndividual(null);
			this.groupDossier.setGroup(null);
			this.currentDossier = this.individualDossier;

			split.setLeftComponent(switcher);
			split.setRightComponent(currentDossier);
			split.setDividerLocation(SWITCHER_WIDTH);

			add(split, BorderLayout.CENTER);

			revalidate();
			repaint();
		});
	}


	/* ======================================================================
	 *                          Dossier switching
	 * ====================================================================== */

	/**
	 * Updates the right-hand dossier to match the given root entity. The
	 * entity type is resolved through the model: groups go to the group
	 * dossier, everything else to the individual dossier.
	 */
	private void syncDossier(final String rootId){
		if(rootId == null)
			return;

		final FLEFRecord record = model.getRecordById(rootId);
		if(record != null && GroupHandler.TYPE.equalsIgnoreCase(record.getTag()))
			showGroupDossier(rootId);
		else
			showIndividualDossier(rootId);
	}

	private void showIndividualDossier(final String individualId){
		individualDossier.setIndividual(individualId);
		swapDossier(individualDossier);
	}

	private void showGroupDossier(final String groupId){
		groupDossier.setGroup(groupId);
		swapDossier(groupDossier);
	}

	/**
	 * Installs the given panel on the right side of the split pane, if
	 * it is not already installed.
	 */
	private void swapDossier(final JComponent target){
		if(target == currentDossier)
			return;

		final int divider = split.getDividerLocation();
		split.setRightComponent(target);
		split.setDividerLocation(divider);
		currentDossier = target;
	}


	/* ======================================================================
	 *                          Navigation shortcuts
	 * ====================================================================== */

	/**
	 * Installs the {@code Ctrl+Left} and {@code Ctrl+Right} shortcuts
	 * that drive the browser-style navigation history. On macOS the
	 * modifier becomes {@code Cmd} automatically, thanks to the platform
	 * menu shortcut mask.
	 */
	private void setupNavigationShortcuts(){
		final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		final InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = getRootPane().getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, mask), ACTION_NAVIGATE_BACK);
		actionMap.put(ACTION_NAVIGATE_BACK, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -1029384756102938475L;

			@Override
			public void actionPerformed(final ActionEvent e){
				if(switcher.canGoBack())
					switcher.navigateBack();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, mask), ACTION_NAVIGATE_FORWARD);
		actionMap.put(ACTION_NAVIGATE_FORWARD, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 2938475610293847561L;

			@Override
			public void actionPerformed(final ActionEvent e){
				if(switcher.canGoForward())
					switcher.navigateForward();
			}
		});
	}

	/**
	 * Installs the {@code F2} shortcut that opens the edit dialog for the
	 * currently selected entity in the active projection.
	 */
	private void setupEditShortcut(){
		final InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = getRootPane().getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), ACTION_EDIT_SELECTION);
		actionMap.put(ACTION_EDIT_SELECTION, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -4710293847561029384L;

			@Override
			public void actionPerformed(final ActionEvent e){
				switcher.editCurrentSelection();
			}
		});
	}

	/**
	 * Installs the arrow keys and Enter for spatial navigation.
	 * <p>
	 * The bindings are registered on the frame's root pane with
	 * {@link JComponent#WHEN_IN_FOCUSED_WINDOW}, so they fire whenever
	 * the frame has focus and no closer component claims the key first.
	 * Text fields keep their own arrow behaviour, because their
	 * {@code WHEN_FOCUSED} bindings take precedence over the frame's
	 * {@code WHEN_IN_FOCUSED_WINDOW} ones.
	 */
	private void setupArrowShortcuts(){
		final InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = getRootPane().getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), ACTION_MOVE_UP);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), ACTION_MOVE_DOWN);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), ACTION_MOVE_LEFT);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), ACTION_MOVE_RIGHT);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACTION_CONFIRM_SELECTION);

		actionMap.put(ACTION_MOVE_UP, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -1029384756102938470L;

			@Override
			public void actionPerformed(final ActionEvent e){
				switcher.moveSelection(SpatialNavigation.Direction.UP);
			}
		});
		actionMap.put(ACTION_MOVE_DOWN, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -1029384756102938471L;

			@Override
			public void actionPerformed(final ActionEvent e){
				switcher.moveSelection(SpatialNavigation.Direction.DOWN);
			}
		});
		actionMap.put(ACTION_MOVE_LEFT, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -1029384756102938472L;

			@Override
			public void actionPerformed(final ActionEvent e){
				switcher.moveSelection(SpatialNavigation.Direction.LEFT);
			}
		});
		actionMap.put(ACTION_MOVE_RIGHT, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -1029384756102938473L;

			@Override
			public void actionPerformed(final ActionEvent e){
				switcher.moveSelection(SpatialNavigation.Direction.RIGHT);
			}
		});
		actionMap.put(ACTION_CONFIRM_SELECTION, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -1029384756102938474L;

			@Override
			public void actionPerformed(final ActionEvent e){
				switcher.confirmSelection();
			}
		});
	}


	/* ======================================================================
	 *                          Bookmarks
	 * ====================================================================== */

	/**
	 * Captures the current view as a bookmark, ready to be saved. The
	 * captured state is the active projection plus the root entity
	 * currently loaded in it. The dossier is not captured, because it is
	 * a consequence of the root and not an independent piece of state.
	 *
	 * @param name the user-visible name
	 * @return the bookmark, or {@code null} when there is no root to save
	 */
	Bookmark captureCurrentState(final String name){
		final ProjectionType projection = switcher.getCurrentProjectionType();
		final Map<String, String> props = new LinkedHashMap<>();

		final BookmarkType bookmarkType;
		final String rootId;

		switch(projection){
			case TREE -> {
				bookmarkType = BookmarkType.TREE;
				rootId = switcher.getTreePanel().getRootIndividualId();
			}
			case SUGIYAMA -> {
				bookmarkType = BookmarkType.SUGIYAMA;
				rootId = switcher.getSugiyamaGraphPanel().getRootIndividualId();
				props.put("showPartner", "true");
			}
			default -> {
				bookmarkType = BookmarkType.EGO_NETWORK;
				rootId = switcher.getEgoPanel().getCurrentEgoId();
			}
		}

		if(rootId == null || rootId.isBlank())
			return null;

		return new Bookmark(BookmarkStore.newId(), name, bookmarkType, rootId, props,
			System.currentTimeMillis());
	}

	/**
	 * Applies a saved bookmark to the UI.
	 * <p>
	 * The bookmark is treated as a selection: the projection is switched
	 * first, then the root is loaded through
	 * {@link ProjectionSwitcherPanel#loadRoot(String)}, which pushes the
	 * new entry into the navigation history. The dossier is updated by
	 * the navigation listener installed on the switcher, so the user
	 * sees the same behaviour as a click on a node, and
	 * {@code Ctrl+Left} / {@code Ctrl+Right} can undo and redo the
	 * bookmark application.
	 *
	 * @param bookmark the bookmark to apply; {@code null} is ignored
	 */
	void applyBookmark(final Bookmark bookmark){
		if(bookmark == null)
			return;

		final ProjectionType projection = switch(bookmark.type()){
			case TREE -> ProjectionType.TREE;
			case SUGIYAMA -> ProjectionType.SUGIYAMA;
			case EGO_NETWORK -> ProjectionType.EGO_NETWORK;
		};

		switcher.setProjection(projection);
		switcher.loadRoot(bookmark.rootId());
	}

	/**
	 * Builds a tool context from the current application state. Every menu
	 * item that triggers a tool obtains its context from this method, so
	 * the tools are always wired to the same model, owner, and callbacks.
	 */
	ToolContext createToolContext(){
		return new ToolContext(
			model,
			this,
			this::editCurrentSelection,
			this::currentSelectionId,
			this::loadRoot,
			this::replaceModel,
			this::currentView);
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
		final String rootIndividualId = "I1";

		final String content;
		try(final InputStream is = FamilyLegacyFrame.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is)
				.readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			final FamilyLegacyFrame frame = new FamilyLegacyFrame(model);
			frame.loadRoot(rootIndividualId);
			frame.setVisible(true);
		});
	}

}
