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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.io.model.RecordTags;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.preferences.TagStore;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.tree.GenealogyRepository;
import io.github.mtrevisan.familylegacy.tree.OrderPreferences;
import io.github.mtrevisan.familylegacy.tree.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContexts;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDispatcher;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.Bookmark;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkStore;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkType;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.ChronomapWorkspace;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionType;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.FindRecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.JumpToIndividualDialog;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.AppIcon;
import io.github.mtrevisan.familylegacy.ui.helpers.ClippedTextTooltips;
import io.github.mtrevisan.familylegacy.ui.helpers.DialogEscapeSupport;
import io.github.mtrevisan.familylegacy.ui.helpers.DialogPlacement;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.StatusMessages;
import io.github.mtrevisan.familylegacy.ui.preferences.AppearanceManager;
import io.github.mtrevisan.familylegacy.ui.tools.files.FileMenuController;
import io.github.mtrevisan.familylegacy.ui.tools.files.RecentFilesManager;
import io.github.mtrevisan.familylegacy.ui.tools.research.TasksPanel;
import org.apache.commons.lang3.StringUtils;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;


/**
 * Main application frame.
 */
public class FamilyLegacyFrame extends JFrame{

	/** Initial width of the switcher side of the split pane. */
	private static final int SWITCHER_WIDTH = 1000;
	/** Initial width of the dossier side of the split pane. */
	private static final int DOSSIER_WIDTH = 400;
	/** Initial height of the tasks panel, below the views. */
	private static final int TASKS_HEIGHT = 180;
	private static final int TASKS_WIDTH = 460;


	private final TreeType treeType;

	private FLEFModel model;
	private final ModelHistory history;
	/** The Research Assistant, if the application has one: it is found as a service, so that the window does not depend on it. */
	private final ResearchAssistantPort assistant = ResearchAssistantPort.find()
		.orElse(null);
	private RecentChangesDialog recentChangesDialog;
	private ProjectionSwitcherPanel switcher;
	private final BookmarkStore bookmarkStore = new BookmarkStore();
	/** Where the free tags of the records are kept, for each file (they are not in the file: see {@link RecordTags}). */
	private final TagStore tagStore = new TagStore();
	private final FileMenuController fileController;
	private Runnable terminator = () -> System.exit(0);
	private final JSplitPane split;
	/** The research tasks still to be done, below the views (see {@link #setTasksPanelVisible(boolean)}). */
	private final TasksPanel tasksPanel;
	/** The views (with the dossier) above the tasks panel; {@code null} while the panel is hidden. */
	private JSplitPane tasksSplit;

	private final JToolBar toolBar;

	/** Whether the window covers the whole screen. */
	private boolean fullScreen;
	/** Where the window was before going in full screen. */
	private Rectangle windowedBounds;
	/** Whether the window was maximized before going in full screen. */
	private int windowedState = NORMAL;

	private static final String APP_TITLE = "Family Legacy";

	private final ToolBarBuilder toolBarBuilder = new ToolBarBuilder();
	private final DossierManager dossierManager;


	public FamilyLegacyFrame(final TreeType treeType, final FLEFModel model){
		super(APP_TITLE);
		setIconImages(AppIcon.windowImages());

		this.treeType = treeType;

		this.model = model;
		if(assistant != null){
			assistant.install(this::model, this::readOnlyModel, this::currentFilePath, this::currentSelectionId);
			//the assistant can show the individual of a finding in the view
			assistant.installRecordViewer(this::loadRoot);
		}
		DialogEscapeSupport.install();
		ClippedTextTooltips.install();
		DialogPlacement.install();
		history = new ModelHistory(model);
		switcher = new ProjectionSwitcherPanel(treeType, model);
		fileController = new FileMenuController(this, this::model, this::replaceModel, history);
		//there are no unsaved changes: every change of the model is written to the file by itself
		history.addChangeListener(fileController::documentChanged);
		//the tags go with the file: they are written when they change, and follow the document when it is saved as another file
		bindTags(model);
		fileController.setSameDocumentFileListener(this::documentFileChanged);
		//the title says which file is open
		fileController.setCurrentFileListener(file -> setTitle(file != null? file.getName() + " - " + APP_TITLE: APP_TITLE));

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, switcher, null);
		split.setResizeWeight(1.);
		split.setDividerLocation(SWITCHER_WIDTH);
		split.setContinuousLayout(true);

		dossierManager = new DossierManager(split, model, SWITCHER_WIDTH);
		//a record edited from the dossier (a name, an event...) may be shown in the views too
		dossierManager.setOnRecordEdited(() -> SwingUtilities.invokeLater(this::refreshViewsAfterDossierEdit));

		tasksPanel = new TasksPanel(this::model, this::currentSelectionId);
		//a task saved, or its status changed, from the panel is a step of the history (and is written to the file)
		tasksPanel.setAfterChange(history::checkpoint);
		tasksPanel.setOnHide(() -> setTasksPanelVisible(false));
		tasksPanel.setDock(TasksPanel.Dock.of(AppPreferences.getInstance().tasksDock()));
		tasksPanel.setOnDock(this::setTasksDock);
		//the tasks are listed again when the model changes (a step of the history, an undo, a redo, another file)
		history.addChangeListener(() -> {
			if(tasksSplit != null)
				tasksPanel.refresh();
		});

		// Event Bindings
		bindSwitcher(switcher, model);

		toolBar = toolBarBuilder.build(
			() -> {
				if(switcher.canGoBack())
					switcher.navigateBack();
			},
			() -> {
				if(switcher.canGoForward())
					switcher.navigateForward();
			},
			this::openJumpToDialog,
			() -> editEntity(currentSelectionId()),
			switcher::setProjection,
			generations -> switcher.setGenerations(generations),
			dossierManager::setSidebarVisible,
			dossierManager.isSidebarVisible()
		);

		switcher.setProjectionChangeListener(toolBarBuilder::updateProjectionState);
		toolBarBuilder.updateProjectionState(switcher.getCurrentProjectionType());

		//what a tool did (a quick entry: what it created), at the right end of the toolbar, for a few seconds
		final StatusMessageView statusMessage = new StatusMessageView();
		StatusMessages.shared()
			.setListener(statusMessage::show);
		//whether what is in memory is on the file
		final SaveStateView saveState = new SaveStateView();
		fileController.setSaveStateListener(saveState::show);
		if(assistant == null){
			toolBar.add(Box.createHorizontalGlue());
			toolBar.add(statusMessage);
			toolBar.add(saveState);
		}
		if(assistant != null){
			//how many findings the Research Assistant has, at the right end of the toolbar; a click opens it
			final ResearchAssistantPort.Indicator indicator = assistant.createIndicator(this, this::model, this::readOnlyModel,
				this::currentFilePath, FamilyLegacyFrame::isModalDialogShowing);
			toolBar.add(Box.createHorizontalGlue());
			toolBar.add(statusMessage);
			toolBar.add(saveState);
			toolBar.add(indicator.component());
			//the findings are computed again when the model changes (a step of the history, an undo, a redo, another file)
			history.addChangeListener(indicator::refreshSoon);
			//the findings that are counted depend on the level of the interface
			FeatureVisibilityService.shared()
				.addListener(indicator::refreshSoon);
			indicator.refreshSoon();
		}

		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);
		applyTasksPanelVisibility();

		setSize(SWITCHER_WIDTH + DOSSIER_WIDTH, 800);
		setMinimumSize(new Dimension(900, 500));
		//the window is closed by exitApplication(), that asks what to do with the unsaved changes first
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(final WindowEvent e){
				exitApplication();
			}
		});
		setLocationRelativeTo(null);

		GlobalKeyboardController.install(this, () -> switcher);

		final GenealogyRepository repository = switcher.getRepository();
		final JMenuBar menuBar = new ApplicationMenuBar(this)
			.build(repository);
		setJMenuBar(menuBar);
		//the level of the interface decides which items the menus offer: they are built again when it changes
		FeatureVisibilityService.shared()
			.addListener(() -> SwingUtilities.invokeLater(() -> {
				setJMenuBar(new ApplicationMenuBar(this).build(switcher.getRepository()));
				//the tasks panel is part of the research, shown from the standard level
				applyTasksPanelVisibility();
				revalidate();
				repaint();
			}));

		PrivacyService.addListener(this::refreshPrivacy);
		//the order of children and partners is a view preference: changing it only rebuilds the views
		OrderPreferences.addListener(this::refreshPrivacy);
		installUndoCheckpoints();
	}

	/** Tells the views what to do when the user selects or reaches a record: show it in the dossier, list its tasks. */
	private void bindSwitcher(final ProjectionSwitcherPanel switcher, final FLEFModel model){
		switcher.setSelectionCallback(id -> {
			dossierManager.showIndividualDossier(id);
			tasksPanel.selectionChanged();
		});
		switcher.setGroupSelectionCallback(id -> {
			dossierManager.showGroupDossier(id);
			tasksPanel.selectionChanged();
		});
		switcher.withNavigationListener(id -> {
			dossierManager.syncDossier(id, model);
			tasksPanel.selectionChanged();
			rememberView(id);
		});
		switcher.setHistoryListener(() -> toolBarBuilder.updateNavigationState(switcher.canGoBack(), switcher.canGoForward()));
		toolBarBuilder.updateNavigationState(switcher.canGoBack(), switcher.canGoForward());
	}

	/** Rebuilds the views after the privacy policy changed. */
	private void refreshPrivacy(){
		SwingUtilities.invokeLater(this::rebuildViews);
	}

	/** Shows again the individual (or group) of the views, whose data were changed from the dossier. */
	private void refreshViewsAfterDossierEdit(){
		PrivacyService.invalidate();
		switcher.getRepository()
			.invalidateIndices();

		loadRoot(switcher.getCurrentRootId());
	}

	private void rebuildViews(){
		PrivacyService.invalidate();
		switcher.getRepository()
			.invalidateIndices();

		String rootId = switcher.getCurrentRootId();
		if(rootId == null || !model.hasRecord(rootId))
			rootId = ModelUtils.findLowestIndividualId(model);
		loadRoot(rootId);
		dossierManager.rebuild(model);
		//the privacy policy decides which tasks are listed
		if(tasksSplit != null)
			tasksPanel.refresh();
	}

	/**
	 * Detects the changes made to the model so far, so that each user action ends up in its own undo step.
	 * <p>
	 * The listener runs <em>before</em> the event is dispatched, hence the changes of the previous actions are recorded just before
	 * the next one starts; a closing dialog is the exception, as it is where a record dialog saves.
	 */
	private void installUndoCheckpoints(){
		Toolkit.getDefaultToolkit()
			.addAWTEventListener(event -> {
				//nested dialogs (e.g. name -> name part) save into the model one after the other: they make a single step, recorded
				//when the last modal dialog closes
				if(event instanceof WindowEvent windowEvent){
					if(windowEvent.getID() == WindowEvent.WINDOW_CLOSED && windowEvent.getWindow() instanceof JDialog
							&& !isModalDialogShowing())
						history.checkpoint(ModelHistory.Origin.DIALOG);
				}
				else if(!isModalDialogShowing()){
					if(event instanceof ActionEvent
							|| event instanceof MouseEvent mouseEvent && mouseEvent.getID() == MouseEvent.MOUSE_PRESSED
							|| event instanceof KeyEvent keyEvent && keyEvent.getID() == KeyEvent.KEY_PRESSED)
						history.checkpoint();
				}
			}, AWTEvent.ACTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
	}

	private static boolean isModalDialogShowing(){
		for(final Window window : Window.getWindows())
			if(window instanceof JDialog dialog && dialog.isModal() && dialog.isShowing())
				return true;
		return false;
	}

	/**
	 * Closes the application, as the Exit command and the close button of the window do: the user is asked what to do with the
	 * unsaved changes, and can cancel.
	 */
	void exitApplication(){
		if(fileController.exit())
			terminator.run();
	}

	/**
	 * Closes the application as {@link #exitApplication()} does, and runs the action just before the process ends (it is what starts the
	 * installer of an update).
	 *
	 * @param beforeExit	The action; returns whether the application can end (if not, it stays open).
	 */
	void exitApplicationAndRun(final BooleanSupplier beforeExit){
		if(fileController.exit() && beforeExit.getAsBoolean())
			terminator.run();
	}

	/** Replaces what ends the process once the window is closed (the tests do not end theirs). */
	void setTerminator(final Runnable terminator){
		this.terminator = terminator;
	}

	void undo(){
		final ModelHistory.Origin origin = history.peekUndoOrigin();
		if(origin == ModelHistory.Origin.DIALOG
				&& MessageDialogs.confirm(this, I18N.t("confirmation.undo.dialog.message"),
				I18N.t("confirmation.undo.dialog.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
				!= JOptionPane.YES_OPTION)
			return;

		if(history.undo())
			rebuildViews();
	}

	void showRecentChanges(){
		if(recentChangesDialog != null && recentChangesDialog.isDisplayable()){
			recentChangesDialog.toFront();
			recentChangesDialog.refresh();
			return;
		}

		recentChangesDialog = new RecentChangesDialog(this, this::model, history, this::rebuildViews);
		recentChangesDialog.setVisible(true);
	}

	boolean canUndo(){
		return history.canUndo();
	}

	boolean canRedo(){
		return history.canRedo();
	}

	/** Told every time the history changes (a step recorded, an undo, a redo, another model). */
	void addHistoryListener(final Runnable listener){
		history.addChangeListener(listener);
	}

	void redo(){
		if(history.redo())
			rebuildViews();
	}

	private void openJumpToDialog(){
		final ProjectionType projectionType = switcher.getCurrentProjectionType();
		final String id = JumpToIndividualDialog.showAndGet(this, model, projectionType == ProjectionType.EGO_NETWORK);
		if(id == null)
			return;

		final ChronomapWorkspace.JumpResult result = switcher.jumpInChronomap(id);
		if(result == null || result == ChronomapWorkspace.JumpResult.OK){
			//not in the chronomap: re-root the view; in the chronomap, the jump has been done
			if(result == null)
				loadRoot(id);
			return;
		}

		final String[] options = {I18N.t("button.ok"), I18N.t("chronomap.jump.show_anyway")};
		final String message = I18N.t(result == ChronomapWorkspace.JumpResult.NO_POSITION? "chronomap.jump.no_position":
			"chronomap.jump.no_time");
		final int choice = MessageDialogs.option(this, message, I18N.t("chronomap.jump.title"), JOptionPane.DEFAULT_OPTION,
			JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
		if(choice == 1)
			loadRoot(id);
	}

	/**
	 * Finds a record of any type (Ctrl+F). An individual is shown in the views, as Ctrl+J does, and so is a group in the ego network;
	 * any other record is opened in its editor, and the views are built again if it is saved.
	 */
	void openFindDialog(){
		final FindRecordDialog.Found found = FindRecordDialog.showAndGet(this, model);
		if(found == null)
			return;

		final FLEFRecord record = found.record();
		final boolean ego = (switcher.getCurrentProjectionType() == ProjectionType.EGO_NETWORK);
		if(IndividualHandler.isOfType(record) || ego && GroupHandler.isOfType(record)){
			loadRoot(record.getId());
			return;
		}

		final RecordDialog dialog = found.handler()
			.createEditDialog(this, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			SwingUtilities.invokeLater(this::rebuildViews);
	}

	/** @return	Whether the window covers the whole screen (see {@link #setFullScreen(boolean)}). */
	public boolean isFullScreen(){
		return fullScreen;
	}

	public void toggleFullScreen(){
		setFullScreen(!isFullScreen());
	}

	/**
	 * Puts the window in full screen on the screen it is on, or brings it back as it was.
	 * <p>
	 * In full screen the window has no title bar and no borders, and covers the task bar; the menus stay. The decorations can be
	 * changed only while the window is not displayable, hence it is disposed and shown again: the windows it owns that are open
	 * (non-modal dialogs) are closed.
	 * <p>
	 * The window is only given the bounds of the screen, it is not made the full-screen window of the device
	 * ({@link GraphicsDevice#setFullScreenWindow(Window)}): on Windows the exclusive mode minimizes the window every time it loses
	 * the focus, even to one of its own dialogs, and gives it back right after; the window flickers (on and on while a dialog is
	 * open), showing what is behind it.
	 *
	 * @param fullScreen	Whether the window covers the whole screen.
	 */
	public void setFullScreen(final boolean fullScreen){
		if(fullScreen == isFullScreen())
			return;

		this.fullScreen = fullScreen;
		if(fullScreen){
			final Rectangle screenBounds = getGraphicsConfiguration().getBounds();
			windowedBounds = getBounds();
			windowedState = (getExtendedState() & ~ICONIFIED);

			dispose();
			setUndecorated(true);
			//a maximized window would cover only the area outside the task bar
			setExtendedState(NORMAL);
			setBounds(screenBounds);
			setVisible(true);
		}
		else{
			dispose();
			setUndecorated(false);
			setBounds(windowedBounds);
			setVisible(true);
			setExtendedState(windowedState);
		}
		validate();
		toFront();
		requestFocus();
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
		return dossierManager.isSidebarVisible();
	}

	/** @return	Whether the tasks panel can be shown: the research is part of the interface from the standard level. */
	public boolean isTasksPanelAvailable(){
		return FeatureVisibilityService.shared()
			.isVisible(UxLevel.STANDARD);
	}

	/** @return	Whether the tasks panel is shown below the views. */
	public boolean isTasksPanelVisible(){
		return (isTasksPanelAvailable() && AppPreferences.getInstance().tasksPanelVisible());
	}

	/** Shows or hides the tasks panel below the views; the choice is kept in the user preferences. */
	public void setTasksPanelVisible(final boolean visible){
		AppPreferences.getInstance()
			.setTasksPanelVisible(visible);
		applyTasksPanelVisibility();
	}

	/** Docks the tasks panel at a side of the window; the choice is kept in the user preferences. */
	public void setTasksDock(final TasksPanel.Dock dock){
		AppPreferences.getInstance()
			.setTasksDock(dock.name().toLowerCase(Locale.ROOT));
		tasksPanel.setDock(dock);
		detachTasksSplit();
		applyTasksPanelVisibility();
	}

	private void detachTasksSplit(){
		if(tasksSplit == null)
			return;

		remove(tasksSplit);
		tasksSplit.removeAll();
		tasksSplit = null;
		add(split, BorderLayout.CENTER);
	}

	/** Puts the tasks panel beside the views, or takes it away, as the preference and the level of the interface say. */
	private void applyTasksPanelVisibility(){
		final boolean visible = isTasksPanelVisible();
		if(visible && tasksSplit == null){
			remove(split);
			final TasksPanel.Dock dock = tasksPanel.getDock();
			final boolean first = (dock == TasksPanel.Dock.TOP || dock == TasksPanel.Dock.LEFT);
			tasksSplit = new JSplitPane((dock.isSide()? JSplitPane.HORIZONTAL_SPLIT: JSplitPane.VERTICAL_SPLIT),
				(first? tasksPanel: split), (first? split: tasksPanel));
			tasksSplit.setResizeWeight(first? 0.: 1.);
			tasksSplit.setContinuousLayout(true);
			tasksSplit.setBorder(null);
			placeTasksDivider(tasksSplit, dock);
			add(tasksSplit, BorderLayout.CENTER);
			tasksPanel.refresh();
		}
		else if(!visible)
			detachTasksSplit();
		revalidate();
		repaint();
	}

	/** Gives the tasks panel its initial size as soon as the split pane has one (the window may not be shown yet). */
	private static void placeTasksDivider(final JSplitPane splitPane, final TasksPanel.Dock dock){
		final boolean first = (dock == TasksPanel.Dock.TOP || dock == TasksPanel.Dock.LEFT);
		final int panelSize = (dock.isSide()? TASKS_WIDTH: TASKS_HEIGHT);
		final Runnable place = () -> {
			final int total = (dock.isSide()? splitPane.getWidth(): splitPane.getHeight());
			splitPane.setDividerLocation(first? panelSize: Math.max(0, total - splitPane.getDividerSize() - panelSize));
		};
		if(splitPane.getHeight() > 0 && splitPane.getWidth() > 0){
			SwingUtilities.invokeLater(place);
			return;
		}

		splitPane.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent e){
				if(splitPane.getHeight() <= 0 || splitPane.getWidth() <= 0)
					return;

				splitPane.removeComponentListener(this);
				place.run();
			}
		});
	}

	public void setSidebarVisible(final boolean visible){
		dossierManager.setSidebarVisible(visible);
		toolBarBuilder.updateSidebarState(visible);

		revalidate();
	}

	public void loadRoot(final String individualId){
		if(individualId != null)
			switcher.loadRoot(individualId);
	}

	/** @return the Research Assistant, if the application has one */
	ResearchAssistantPort assistant(){
		return assistant;
	}

	FLEFModel model(){
		return model;
	}

	/**
	 * @return a copy of the model as it is now that nobody changes, for what is read away from the event dispatch thread while the user
	 * 	may be editing (call it on that thread)
	 */
	private FLEFModel readOnlyModel(){
		return history.readOnlyView();
	}

	BookmarkStore bookmarkStore(){
		return bookmarkStore;
	}

	/** Gives the model the tags that were kept for its file, and writes them again at every change. */
	private void bindTags(final FLEFModel newModel){
		final RecordTags tags = newModel.tags();
		final String path = bookmarkFilePath();
		//the bookmarks are kept next to the file too
		bookmarkStore.bindDocument(path);
		if(path != null){
			tags.replaceAll(tagStore.load(path));
			//the tags of a record that is not in the file any more (it was deleted by other means) are forgotten
			tags.retainOnly(newModel::hasRecord);
		}
		tags.addChangeListener(this::saveTags);
	}

	/** The open document is now in another file (it was saved as): its tags are written next to it, and the bookmarks are the ones copied there. */
	private void documentFileChanged(){
		saveTags();
		bookmarkStore.bindDocument(bookmarkFilePath());
	}

	/** Remembers the person and the view next to the file, so that it opens there the next time. */
	private void rememberView(final String rootId){
		if(model != null && switcher != null)
			ViewMemory.save(bookmarkFilePath(), rootId, switcher.getCurrentProjectionType(), model);
	}

	/** Writes the tags of the open document under the path of its file (a document with no file has none yet: they are written when it has). */
	private void saveTags(){
		final String path = bookmarkFilePath();
		if(path != null && model != null)
			tagStore.save(path, model.tags());
	}

	ProjectionSwitcherPanel switcher(){
		return switcher;
	}

	/** @return the path of the file being edited (what the findings ignored by the user are kept by), or {@code null} if it was never saved */
	private String currentFilePath(){
		final File file = fileController.currentFile();
		return (file != null? file.getAbsolutePath(): null);
	}

	FileMenuController fileController(){
		return fileController;
	}

	/** Opens the given FLEF file, as the File &gt; Open command does (with the progress dialog and the Recent files list). */
	public void openFile(final File file){
		fileController.openFile(file);
	}

	/** Starts the guided tour of the first steps if it was never shown (it is offered once); call it on the event thread. */
	public void startGuidedTourOnce(){
		new GuidedTour(this)
			.startOnce();
	}

	/** Asks the file of the empty document the application starts with (see {@link FileMenuController#offerFileAtStartup()}); call it on the event thread. */
	public void offerFileAtStartup(){
		fileController.offerFileAtStartup();
	}

	/** Looks for a newer release, if the user wants it and it is time (see {@link AutomaticUpdatePolicy}); call it on the event thread. */
	public void checkForUpdatesAtStartup(){
		new UpdateFlow(this).startAutomatic();
	}

	void editEntity(final String id){
		switcher.editEntity(id);
	}

	String currentSelectionId(){
		return switcher.getSelectedEntityId();
	}

	void replaceModel(final FLEFModel newModel){
		replaceModel(newModel, false);
	}

	/**
	 * @param modified	{@code true} if the model is not the content of the file (a restored backup): it is written to the file.
	 */
	private void replaceModel(final FLEFModel newModel, final boolean modified){
		if(newModel == null)
			return;

		SwingUtilities.invokeLater(() -> {
			model = newModel;
			history.reset(newModel);
			bindTags(newModel);
			if(modified){
				history.markUnsaved();
				//what is in memory is not what is on the file: it is written to it
				fileController.documentChanged();
			}

			//what was shown the last time the file was used is read before the switcher starts to write what is shown now
			final ViewMemory.Start start = ViewMemory.read(bookmarkFilePath(), newModel);

			switcher = new ProjectionSwitcherPanel(treeType, newModel);
			bindSwitcher(switcher, newModel);
			switcher.setProjectionChangeListener(type -> {
				toolBarBuilder.updateProjectionState(type);
				rememberView(switcher.getCurrentRootId());
			});
			//the generations chosen in the toolbar are the ones of the new file too
			switcher.setGenerations(toolBarBuilder.generations());

			dossierManager.rebuild(newModel);

			split.setLeftComponent(switcher);
			split.setDividerLocation(SWITCHER_WIDTH);

			split.revalidate();
			split.repaint();

			if(start != null && start.projection() != null && start.projection() != switcher.getCurrentProjectionType())
				switcher.setProjection(start.projection());
			loadRoot(start != null? start.rootId(): ModelUtils.findLowestIndividualId(newModel));
		});
	}

	Bookmark captureCurrentState(final String name){
		final ProjectionType projection = switcher.getCurrentProjectionType();
		final Map<String, String> props = new LinkedHashMap<>();

		final BookmarkType bookmarkType;
		final String rootId;
		switch(projection){
			case BIOLOGICAL_TREE -> {
				bookmarkType = BookmarkType.BIOLOGICAL_TREE;
				rootId = switcher.getTreeGraphPanel()
					.getRootIndividualId();
			}
			case SUGIYAMA_GRAPH -> {
				bookmarkType = BookmarkType.SUGIYAMA_GRAPH;
				rootId = switcher.getTreeGraphPanel()
					.getRootIndividualId();
				props.put("showPartner", "true");
			}
			case CHRONOMAP -> {
				bookmarkType = BookmarkType.CHRONOMAP;
				rootId = switcher.getCurrentRootId();
			}
			default -> {
				bookmarkType = BookmarkType.EGO_NETWORK;
				rootId = switcher.getEgoPanel()
					.getCurrentEgoId();
			}
		}

		if(StringUtils.isEmpty(rootId)){
			return null;
		}

		//the bookmark is of the file it was made in: the identifiers of another file are other records. A document with no file has no
		//bookmarks: one made here would belong to none, and would never be listed
		final String file = bookmarkFilePath();
		if(file == null)
			return null;

		props.put(BookmarkStore.FILE_PROPERTY, file);

		return new Bookmark(BookmarkStore.newId(), name, bookmarkType, rootId, props, System.currentTimeMillis());
	}

	/** @return	The path the bookmarks are tied to (the file of the document, in its normal form), or {@code null} if it has none. */
	String bookmarkFilePath(){
		final File file = fileController.currentFile();
		return (file != null? RecentFilesManager.normalized(file).getAbsolutePath(): null);
	}

	void applyBookmark(final Bookmark bookmark){
		if(bookmark == null){
			return;
		}

		//the root can be gone (deleted since the bookmark was made)
		if(!bookmark.rootId().isEmpty() && !model.hasRecord(bookmark.rootId())){
			MessageDialogs.show(this, I18N.tf("dialog.bookmark.missing_root.message", bookmark.name()),
				I18N.t("dialog.bookmark.missing_root.title"), JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		final ProjectionType projection = switch(bookmark.type()){
			case BIOLOGICAL_TREE -> ProjectionType.BIOLOGICAL_TREE;
			case SUGIYAMA_GRAPH -> ProjectionType.SUGIYAMA_GRAPH;
			case EGO_NETWORK -> ProjectionType.EGO_NETWORK;
			case CHRONOMAP -> ProjectionType.CHRONOMAP;
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
		return ToolContexts.custom(model, currentSelectionId(), switcher.getSelectedPanel(),
			new ToolDispatcher(){
				@Override
				public boolean isEgoNetworkActive(){
					return (switcher.getCurrentProjectionType() == ProjectionType.EGO_NETWORK);
				}

				@Override
				public String[] getRootIds(){
					return switcher.getRootIds();
				}

				@Override
				public void editEntity(final String id){
					switcher.editEntity(id);
				}

				@Override
				public void loadRoot(final String id){
					FamilyLegacyFrame.this.loadRoot(id);
				}

				@Override
				public void replaceModel(final FLEFModel newModel){
					FamilyLegacyFrame.this.replaceModel(newModel);
				}

				@Override
				public void openFile(final File file){
					FamilyLegacyFrame.this.openFile(file);
				}

				@Override
				public String documentPath(){
					return bookmarkFilePath();
				}

				@Override
				public void restoreModel(final FLEFModel restoredModel){
					FamilyLegacyFrame.this.replaceModel(restoredModel, true);
				}

				@Override
				public boolean confirmDiscardChanges(){
					return fileController.confirmDiscardChanges();
				}

				@Override
				public void runTool(final ToolOperation tool){
					if(tool.isEnabled(createToolContext()))
						tool.run(createToolContext());
				}

				@Override
				public void jumpToIndividual(){
					//after the model of the import is shown: it is queued behind its replacement
					SwingUtilities.invokeLater(() -> {
						if(model != null && model.hasRecordsByType(IndividualReader.TYPE))
							openJumpToDialog();
					});
				}

				@Override
				public File openImported(final FLEFModel importedModel, final File source){
					return fileController.openImported(importedModel, source);
				}

				@Override
				public void removeEntity(final String id){
					switcher.removeEntity(id);
				}

				@Override
				public FLEFModel readOnlyModel(){
					return FamilyLegacyFrame.this.readOnlyModel();
				}

				@Override
				public ProjectionMutator getMutator(){
					return switcher.getMutator();
				}
			});
	}


	static void main(final String[] args) throws IOException{
		final String modelUri = "/tests/TGMZ.flef";
//		final String rootIndividualId = "I1";
		final String rootIndividualId = "I1430";

		final String content;
		try(final InputStream is = FamilyLegacyFrame.class.getResourceAsStream(modelUri)){
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			try{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch(final Exception ignored){}

			// Apply persisted language and font before any window is created.
			AppearanceManager.applySaved();

//			final TreeType treeType = TreeType.BIOLOGICAL;
			final TreeType treeType = TreeType.FAMILY;
			final FamilyLegacyFrame frame = new FamilyLegacyFrame(treeType, model);
			frame.loadRoot(rootIndividualId);
			frame.setVisible(true);

			//after the window is shown, so it does not interfere with the creation of the UI fonts
			SwingUtilities.invokeLater(AppearanceManager::preloadFontFamilies);
		});
	}

}
