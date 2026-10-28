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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.Bookmark;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkStore;
import io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks.BookmarkType;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.JumpToIndividualDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionType;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import io.github.mtrevisan.familylegacy.ui.components.projections.repository.GenealogyRepository;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.OrderPreferences;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.components.projections.services.DossierManager;
import io.github.mtrevisan.familylegacy.ui.components.projections.services.ModelUtils;
import io.github.mtrevisan.familylegacy.ui.helpers.DialogEscapeSupport;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.preferences.AppearanceManager;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContexts;
import io.github.mtrevisan.familylegacy.ui.tools.ToolDispatcher;
import io.github.mtrevisan.familylegacy.ui.tools.files.FileMenuController;
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
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Main application frame.
 */
public class FamilyLegacyFrame extends JFrame{

	/** Initial width of the switcher side of the split pane. */
	private static final int SWITCHER_WIDTH = 1000;
	/** Initial width of the dossier side of the split pane. */
	private static final int DOSSIER_WIDTH = 400;


	private final TreeType treeType;

	private FLEFModel model;
	private final ModelHistory history;
	/** The Research Assistant, if the application has one: it is found as a service, so that the window does not depend on it. */
	private final ResearchAssistantPort assistant = ResearchAssistantPort.find().orElse(null);
	private RecentChangesDialog recentChangesDialog;
	private ProjectionSwitcherPanel switcher;
	private final BookmarkStore bookmarkStore = new BookmarkStore();
	private final FileMenuController fileController;
	private final JSplitPane split;

	private final JToolBar toolBar;

	private final ToolBarBuilder toolBarBuilder = new ToolBarBuilder();
	private final DossierManager dossierManager;


	public FamilyLegacyFrame(final TreeType treeType, final FLEFModel model){
		super("Family Legacy");

		this.treeType = treeType;

		this.model = model;
		if(assistant != null)
			assistant.install(this::model, this::currentFilePath);
		DialogEscapeSupport.install();
		history = new ModelHistory(model);
		switcher = new ProjectionSwitcherPanel(treeType, model);
		fileController = new FileMenuController(this, this::model, this::replaceModel, () -> {});

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, switcher, null);
		split.setResizeWeight(1.);
		split.setDividerLocation(SWITCHER_WIDTH);
		split.setContinuousLayout(true);

		dossierManager = new DossierManager(split, model, SWITCHER_WIDTH);

		// Event Bindings
		switcher.setSelectionCallback(dossierManager::showIndividualDossier);
		switcher.setGroupSelectionCallback(dossierManager::showGroupDossier);
		switcher.withNavigationListener(id -> dossierManager.syncDossier(id, model));

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
			dossierManager::setSidebarVisible,
			dossierManager.isSidebarVisible()
		);

		switcher.setProjectionChangeListener(toolBarBuilder::updateProjectionState);
		toolBarBuilder.updateProjectionState(switcher.getCurrentProjectionType());

		if(assistant != null){
			//how many findings the Research Assistant has, at the right end of the toolbar; a click opens it
			final ResearchAssistantPort.Indicator indicator = assistant.createIndicator(this, this::model, this::currentFilePath,
				FamilyLegacyFrame::isModalDialogShowing);
			toolBar.add(Box.createHorizontalGlue());
			toolBar.add(indicator.component());
			//the findings are computed again when the model changes (a step of the history, an undo, a redo, another file)
			history.addChangeListener(indicator::refreshSoon);
			indicator.refreshSoon();
		}

		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);

		setSize(SWITCHER_WIDTH + DOSSIER_WIDTH, 800);
		setMinimumSize(new Dimension(900, 500));
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		GlobalKeyboardController.install(this, switcher);

		final GenealogyRepository repository = switcher.getRepository();
		final JMenuBar menuBar = new ApplicationMenuBar(this)
			.build(repository);
		setJMenuBar(menuBar);

		PrivacyService.addListener(this::refreshPrivacy);
		//the order of children and partners is a view preference: changing it only rebuilds the views
		OrderPreferences.addListener(this::refreshPrivacy);
		installUndoCheckpoints();
	}

	/** Rebuilds the views after the privacy policy changed. */
	private void refreshPrivacy(){
		SwingUtilities.invokeLater(this::rebuildViews);
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
				else if(isModalDialogShowing())
					return;
				else if(event instanceof ActionEvent
						|| event instanceof MouseEvent mouseEvent && mouseEvent.getID() == MouseEvent.MOUSE_PRESSED
						|| event instanceof KeyEvent keyEvent && keyEvent.getID() == KeyEvent.KEY_PRESSED)
					history.checkpoint();
			}, AWTEvent.ACTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
	}

	private static boolean isModalDialogShowing(){
		for(final Window window : Window.getWindows())
			if(window instanceof JDialog dialog && dialog.isModal() && dialog.isShowing())
				return true;
		return false;
	}

	void undo(){
		final ModelHistory.Origin origin = history.peekUndoOrigin();
		if(origin == ModelHistory.Origin.DIALOG
				&& JOptionPane.showConfirmDialog(this, I18N.t("confirmation.undo.dialog.message"),
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

		recentChangesDialog = new RecentChangesDialog(this, this::model, history::recentChanges);
		recentChangesDialog.setVisible(true);
	}

	void redo(){
		if(history.redo())
			rebuildViews();
	}

	private void openJumpToDialog(){
		final ProjectionType projectionType = switcher.getCurrentProjectionType();
		final String id = JumpToIndividualDialog.showAndGet(this, model, projectionType);
		if(id != null)
			loadRoot(id);
	}

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
		return dossierManager.isSidebarVisible();
	}

	public void setSidebarVisible(final boolean visible){
		dossierManager.setSidebarVisible(visible);

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

	BookmarkStore bookmarkStore(){
		return bookmarkStore;
	}

	ProjectionSwitcherPanel switcher(){
		return switcher;
	}

	/** @return the path of the file being edited (what the findings ignored by the user are kept by), or {@code null} if it was never saved */
	private String currentFilePath(){
		final java.io.File file = fileController.currentFile();
		return (file != null? file.getAbsolutePath(): null);
	}

	FileMenuController fileController(){
		return fileController;
	}

	void editEntity(final String id){
		switcher.editEntity(id);
	}

	String currentSelectionId(){
		return switcher.getSelectedEntityId();
	}

	void replaceModel(final FLEFModel newModel){
		if(newModel == null)
			return;

		SwingUtilities.invokeLater(() -> {
			model = newModel;
			history.reset(newModel);

			switcher = new ProjectionSwitcherPanel(treeType, newModel);
			switcher.setSelectionCallback(dossierManager::showIndividualDossier);
			switcher.setGroupSelectionCallback(dossierManager::showGroupDossier);
			switcher.withNavigationListener(id -> dossierManager.syncDossier(id, newModel));
			switcher.setProjectionChangeListener(toolBarBuilder::updateProjectionState);

			dossierManager.rebuild(newModel);

			split.setLeftComponent(switcher);
			split.setDividerLocation(SWITCHER_WIDTH);

			split.revalidate();
			split.repaint();

			loadRoot(ModelUtils.findLowestIndividualId(newModel));
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
				rootId = switcher.getTreeGraphPanel().getRootIndividualId();
			}
			case SUGIYAMA_GRAPH -> {
				bookmarkType = BookmarkType.SUGIYAMA_GRAPH;
				rootId = switcher.getTreeGraphPanel().getRootIndividualId();
				props.put("showPartner", "true");
			}
			default -> {
				bookmarkType = BookmarkType.EGO_NETWORK;
				rootId = switcher.getEgoPanel().getCurrentEgoId();
			}
		}

		if(StringUtils.isEmpty(rootId)){
			return null;
		}

		return new Bookmark(BookmarkStore.newId(), name, bookmarkType, rootId, props, System.currentTimeMillis());
	}

	void applyBookmark(final Bookmark bookmark){
		if(bookmark == null){
			return;
		}

		final ProjectionType projection = switch(bookmark.type()){
			case BIOLOGICAL_TREE -> ProjectionType.BIOLOGICAL_TREE;
			case SUGIYAMA_GRAPH -> ProjectionType.SUGIYAMA_GRAPH;
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
		return ToolContexts.custom(model, currentSelectionId(), switcher.getSelectedPanel(),
			new ToolDispatcher(){
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
				public void openImported(final FLEFModel importedModel, final File source){
					fileController.openImported(importedModel, source);
				}

				@Override
				public void removeEntity(final String id){
					switcher.removeEntity(id);
				}

				@Override
				public ProjectionMutator getMutator(){
					return switcher.getMutator();
				}
			});
	}


	public static void main(final String[] args) throws IOException, URISyntaxException{
		final Path root = Paths.get(FamilyLegacyFrame.class
			.getProtectionDomain()
			.getCodeSource()
			.getLocation()
			.toURI());
		final Path path = root.resolve("gedg/flef_0.2.0.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);

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
		});
	}

}
