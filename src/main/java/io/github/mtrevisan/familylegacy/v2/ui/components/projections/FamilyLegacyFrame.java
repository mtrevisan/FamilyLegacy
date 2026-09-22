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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.files.FileMenuController;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
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


	private FLEFModel model;
	private ProjectionSwitcherPanel switcher;
	private IndividualDossierPanel individualDossier;
	private GroupDossierPanel groupDossier;
	private final BookmarkStore bookmarkStore = new BookmarkStore();
	private final FileMenuController fileController;

	/**
	 * The panel currently installed on the right side of the split pane.
	 * Switches between {@link #individualDossier} and
	 * {@link #groupDossier} depending on which entity is selected.
	 */
	private JComponent currentDossier;

	private final JSplitPane split;

	private final JToolBar toolBar;

	private JToggleButton btnToggleSidebar;
	private JToggleButton btnTreeLayout;
	private JToggleButton btnGraphLayout;
	private JToggleButton btnEgoLayout;
	private JButton btnBack;
	private JButton btnForward;

	private boolean sidebarVisible = true;


	public FamilyLegacyFrame(final FLEFModel model){
		super("Family Legacy");

		this.model = Objects.requireNonNull(model);
		switcher = new ProjectionSwitcherPanel(model);
		individualDossier = new IndividualDossierPanel(model);
		groupDossier = new GroupDossierPanel(model);
		fileController = new FileMenuController(this,
			this::model,
			this::replaceModel,
			() -> {});

		// Selection callbacks: user clicks on a node populate the dossiers.
		switcher.setSelectionCallback(this::showIndividualDossier);
		switcher.setGroupSelectionCallback(this::showGroupDossier);

		// Navigation listener: fires for any root change, including
		// bookmark applications and back/forward navigation. Keeps the
		// dossiers in sync with the active view.
		switcher.withNavigationListener(this::syncDossier);

		currentDossier = individualDossier;

		toolBar = createToolBar();

		// Keep the toolbar radio buttons in sync with the active projection,
		// regardless of whether the change came from the toolbar, a keyboard
		// shortcut (Ctrl+1/2/3) or a bookmark application.
		switcher.setProjectionChangeListener(this::onProjectionChanged);
		onProjectionChanged(switcher.getCurrentProjectionType());

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, switcher, currentDossier);
		split.setResizeWeight(1.0);
		split.setDividerLocation(SWITCHER_WIDTH);
		split.setContinuousLayout(true);

		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
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
	 *                          Toolbar
	 * ====================================================================== */

	private JToolBar createToolBar(){
		final JToolBar tb = new JToolBar("Main Toolbar");
		tb.setFloatable(false);

		// File operations
		final JButton btnNew = new JButton("New");
		btnNew.setToolTipText("New File (Ctrl+N)");
		btnNew.addActionListener(e -> fileController.newFile());

		final JButton btnOpen = new JButton("Open");
		btnOpen.setToolTipText("Open File (Ctrl+O)");
		btnOpen.addActionListener(e -> fileController.openFile());

		final JButton btnSave = new JButton("Save");
		btnSave.setToolTipText("Save File (Ctrl+S)");
		btnSave.addActionListener(e -> fileController.save());

		tb.add(btnNew);
		tb.add(btnOpen);
		tb.add(btnSave);
		tb.addSeparator();

		// Navigation
		btnBack = new JButton("◄");
		btnBack.setToolTipText("Navigate Back (Ctrl+Left)");
		btnBack.addActionListener(e -> {
			if(switcher.canGoBack())
				switcher.navigateBack();
		});

		btnForward = new JButton("►");
		btnForward.setToolTipText("Navigate Forward (Ctrl+Right)");
		btnForward.addActionListener(e -> {
			if(switcher.canGoForward())
				switcher.navigateForward();
		});

		final JButton btnJump = new JButton("Jump To…");
		btnJump.setToolTipText("Jump to Individual or Group (Ctrl+J)");
		btnJump.addActionListener(e -> openJumpToDialog());

		final JButton btnEdit = new JButton("Edit");
		btnEdit.setToolTipText("Edit Current Selection (F2)");
		btnEdit.addActionListener(e -> editCurrentSelection());

		tb.add(btnBack);
		tb.add(btnForward);
		tb.add(btnJump);
		tb.add(btnEdit);
		tb.addSeparator();

		// Projection Switcher Buttons
		btnTreeLayout = new JToggleButton("Tree");
		btnTreeLayout.setToolTipText("Ancestor Tree (Ctrl+1)");
		btnTreeLayout.setSelected(true);
		btnTreeLayout.addActionListener(e -> switcher.setProjection(ProjectionType.TREE));

		btnGraphLayout = new JToggleButton("Sugiyama");
		btnGraphLayout.setToolTipText("Sugiyama Pedigree Graph (Ctrl+2)");
		btnGraphLayout.addActionListener(e -> switcher.setProjection(ProjectionType.GRAPH));

		btnEgoLayout = new JToggleButton("Ego Net");
		btnEgoLayout.setToolTipText("Ego Network (Ctrl+3)");
		btnEgoLayout.addActionListener(e -> switcher.setProjection(ProjectionType.EGO_NETWORK));

		final ButtonGroup projectionGroup = new ButtonGroup();
		projectionGroup.add(btnTreeLayout);
		projectionGroup.add(btnGraphLayout);
		projectionGroup.add(btnEgoLayout);

		tb.add(btnTreeLayout);
		tb.add(btnGraphLayout);
		tb.add(btnEgoLayout);
		tb.addSeparator();

		// Layout & View Toggles
		btnToggleSidebar = new JToggleButton("Sidebar", sidebarVisible);
		btnToggleSidebar.setToolTipText("Toggle Dossier Sidebar Panel");
		btnToggleSidebar.addActionListener(e -> setSidebarVisible(btnToggleSidebar.isSelected()));

		tb.add(btnToggleSidebar);

		return tb;
	}

	private void openJumpToDialog(){
		final String id = JumpToIndividualDialog.showAndGet(
			this,
			model(),
			switcher.getCurrentProjectionType());
		if(id != null)
			loadRoot(id);
	}

	/**
	 * Updates the toolbar's projection toggles to reflect the given
	 * projection. Called from the switcher's projection listener, so it
	 * also fires for Ctrl+1/2/3 and for bookmark applications.
	 */
	private void onProjectionChanged(final ProjectionType type){
		switch(type){
			case TREE -> btnTreeLayout.setSelected(true);
			case GRAPH -> btnGraphLayout.setSelected(true);
			case EGO_NETWORK -> btnEgoLayout.setSelected(true);
		}
	}

	/* ======================================================================
	 *                          View Visibility API
	 * ====================================================================== */

	public boolean isFullScreen(){
		return ((getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH);
	}

	public void toggleFullScreen(){
		setExtendedState(isFullScreen()? NORMAL: MAXIMIZED_BOTH);
	}

	public boolean isToolbarVisible(){
		return (toolBar != null && toolBar.isVisible());
	}

	public void setToolbarVisible(final boolean visible){
		if(toolBar != null){
			toolBar.setVisible(visible);

			revalidate();
		}
	}

	public boolean isSidebarVisible(){
		return sidebarVisible;
	}

	public void setSidebarVisible(final boolean visible){
		sidebarVisible = visible;
		if(visible){
			split.setRightComponent(currentDossier);
			split.setDividerLocation(SWITCHER_WIDTH);
		}
		else
			split.setRightComponent(null);

		revalidate();
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

			this.switcher = new ProjectionSwitcherPanel(newModel);
			switcher.setSelectionCallback(this::showIndividualDossier);
			switcher.setGroupSelectionCallback(this::showGroupDossier);
			switcher.withNavigationListener(this::syncDossier);
			switcher.setProjectionChangeListener(this::onProjectionChanged);

			this.individualDossier = new IndividualDossierPanel(newModel);
			this.groupDossier = new GroupDossierPanel(newModel);
			this.currentDossier = this.individualDossier;

			remove(split);
			split.setLeftComponent(switcher);
			split.setRightComponent(currentDossier);
			split.setDividerLocation(SWITCHER_WIDTH);
			add(split, BorderLayout.CENTER);
			split.revalidate();
			split.repaint();

			loadRoot(lowestIndividualId(newModel));
		});
	}

	/**
	 * Returns the id of the individual with the lowest numeric suffix,
	 * or {@code null} when the model contains no individuals.
	 * <p>
	 * IDs are compared by their numeric part, not lexicographically:
	 * {@code I2} is lower than {@code I10}.
	 */
	private static String lowestIndividualId(final FLEFModel model){
		String bestId = null;
		long bestNumber = Long.MAX_VALUE;

		for(final FLEFRecord record : model.getRecords()){
			if(!IndividualHandler.TYPE.equalsIgnoreCase(record.getTag()))
				continue;

			final String id = record.getId();
			if(id == null)
				continue;

			final long number = numericSuffix(id);
			if(number < bestNumber){
				bestNumber = number;
				bestId = id;
			}
		}
		return bestId;
	}

	/**
	 * Extracts the trailing digits from an id such as {@code I42} → {@code 42}.
	 * When the id has no trailing digits, returns {@link Long#MAX_VALUE} so
	 * such ids sort last.
	 */
	private static long numericSuffix(final String id){
		int i = id.length();
		while(i > 0 && Character.isDigit(id.charAt(i - 1)))
			i --;

		if(i == id.length())
			return Long.MAX_VALUE;

		try{
			return Long.parseLong(id.substring(i));
		}
		catch(final NumberFormatException ex){
			return Long.MAX_VALUE;
		}
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

		currentDossier = target;
		if(sidebarVisible){
			final int divider = split.getDividerLocation();
			split.setRightComponent(target);
			split.setDividerLocation(divider);
		}
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
	 * Installs a global KeyEventDispatcher for spatial navigation arrow shortcuts.
	 * This bypasses component-level InputMap consumption (e.g. JScrollPane).
	 */
	private void setupArrowShortcuts(){
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(e -> {
				// Handle only KEY_PRESSED events when this frame is the active window
				if(e.getID() != KeyEvent.KEY_PRESSED || !isFocused())
					return false;

				// Do not swallow arrow/enter keys when any modifier is held:
				// Ctrl+Left / Ctrl+Right must reach the InputMap bindings for
				// navigateBack / navigateForward, and Shift/Alt combinations
				// may be used by other components (text selection, OS gestures).
				// On macOS the menu shortcut is Cmd, which maps to META_DOWN_MASK.
				final int modifiers = e.getModifiersEx();
				if((modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK
						| InputEvent.SHIFT_DOWN_MASK)) != 0)
					return false;

				// Do not intercept arrow keys if a text component is currently focused
				final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
					.getFocusOwner();
				if(focusOwner instanceof JTextComponent)
					return false;

				final int keyCode = e.getKeyCode();
				switch(keyCode){
					case KeyEvent.VK_UP -> {
						switcher.moveSelection(SpatialNavigation.Direction.UP);
						return true;
					}
					case KeyEvent.VK_DOWN -> {
						switcher.moveSelection(SpatialNavigation.Direction.DOWN);
						return true;
					}
					case KeyEvent.VK_LEFT -> {
						switcher.moveSelection(SpatialNavigation.Direction.LEFT);
						return true;
					}
					case KeyEvent.VK_RIGHT -> {
						switcher.moveSelection(SpatialNavigation.Direction.RIGHT);
						return true;
					}
					case KeyEvent.VK_ENTER -> {
						switcher.confirmSelection();
						return true;
					}
					default -> {
						return false;
					}
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
				rootId = switcher.getTreeGraphPanel()
					.getRootIndividualId();
			}
			case GRAPH -> {
				bookmarkType = BookmarkType.GRAPH;
				rootId = switcher.getTreeGraphPanel()
					.getRootIndividualId();
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
			case GRAPH -> ProjectionType.GRAPH;
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

//		final String modelUri = "/tests/TGMZ.flef";
		final String modelUri = "/tests/out.flef";
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
