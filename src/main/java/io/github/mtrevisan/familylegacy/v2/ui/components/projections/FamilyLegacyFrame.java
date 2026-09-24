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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks.Bookmark;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks.BookmarkStore;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks.BookmarkType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.GenealogyRepository;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.services.DossierManager;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.services.GlobalKeyboardController;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.services.ModelUtils;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.files.FileMenuController;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Main application frame.
 */
public class FamilyLegacyFrame extends JFrame{

	@Serial
	private static final long serialVersionUID = 7829104738102938471L;


	/** Initial width of the switcher side of the split pane. */
	private static final int SWITCHER_WIDTH = 1000;
	/** Initial width of the dossier side of the split pane. */
	private static final int DOSSIER_WIDTH = 400;

	private FLEFModel model;
	private ProjectionSwitcherPanel switcher;
	private final BookmarkStore bookmarkStore = new BookmarkStore();
	private final FileMenuController fileController;
	private final JSplitPane split;

	private final JToolBar toolBar;

	private final ToolBarBuilder toolBarBuilder = new ToolBarBuilder();
	private final DossierManager dossierManager;


	public FamilyLegacyFrame(final FLEFModel model){
		super("Family Legacy");

		this.model = Objects.requireNonNull(model);
		switcher = new ProjectionSwitcherPanel(model);
		fileController = new FileMenuController(this, this::model, this::replaceModel, () -> {});

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, switcher, null);
		split.setResizeWeight(1.0);
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
			this::editCurrentSelection,
			switcher::setProjection,
			dossierManager::setSidebarVisible,
			dossierManager.isSidebarVisible()
		);

		switcher.setProjectionChangeListener(toolBarBuilder::updateProjectionState);
		toolBarBuilder.updateProjectionState(switcher.getCurrentProjectionType());

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

	void editCurrentSelection(){
		switcher.editCurrentSelection();
	}

	String currentSelectionId(){
		return switcher.getSelectedEntityId();
	}

	Component currentView(){
		return switcher.getCurrentPanel();
	}

	void replaceModel(final FLEFModel newModel){
		if(newModel == null)
			return;

		SwingUtilities.invokeLater(() -> {
			model = newModel;

			switcher = new ProjectionSwitcherPanel(newModel);
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
			case TREE -> {
				bookmarkType = BookmarkType.TREE;
				rootId = switcher.getTreeGraphPanel().getRootIndividualId();
			}
			case GRAPH -> {
				bookmarkType = BookmarkType.GRAPH;
				rootId = switcher.getTreeGraphPanel().getRootIndividualId();
				props.put("showPartner", "true");
			}
			default -> {
				bookmarkType = BookmarkType.EGO_NETWORK;
				rootId = switcher.getEgoPanel().getCurrentEgoId();
			}
		}

		if(rootId == null || rootId.isBlank()){
			return null;
		}

		return new Bookmark(BookmarkStore.newId(), name, bookmarkType, rootId, props, System.currentTimeMillis());
	}

	void applyBookmark(final Bookmark bookmark){
		if(bookmark == null){
			return;
		}

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


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

//		final String modelUri = "/tests/TGMZ.flef";
		final String modelUri = "/tests/out.flef";
		final String rootIndividualId = "I1";

		final String content;
		try(final InputStream is = FamilyLegacyFrame.class.getResourceAsStream(modelUri)){
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
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
