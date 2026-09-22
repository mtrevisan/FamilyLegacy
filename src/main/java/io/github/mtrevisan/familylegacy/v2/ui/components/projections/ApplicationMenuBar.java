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

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks.BookmarkMenu;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.help.DiagnosticsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.help.HelpViewerDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.help.KeyboardShortcutsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;
import io.github.mtrevisan.familylegacy.v2.ui.tools.events.EventToolRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.tools.files.FileMenuController;
import io.github.mtrevisan.familylegacy.v2.ui.tools.files.FileToolRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.tools.groups.GroupToolRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.IndividualToolRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.tools.places.PlaceToolRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.tools.research.ResearchToolRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.tools.sources.SourceToolRegistry;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


/**
 * Builds the full menu bar of the application and wires each item to the
 * corresponding operation.
 * <p>
 * The class exists to keep {@link FamilyLegacyFrame} focused on its
 * responsibilities (managing the split pane, the dossiers, and the
 * projections). Menu construction is a self-contained concern with its
 * own vocabulary (mnemonics, accelerators, separators, dynamic enable
 * state) and it belongs in a dedicated collaborator.
 * <p>
 * <b>Structure.</b> Each top-level menu has its own factory method. The
 * menus that are backed by a registry of tools ({@link FileToolRegistry},
 * {@link IndividualToolRegistry}, {@link GroupToolRegistry},
 * {@link PlaceToolRegistry}, {@link SourceToolRegistry},
 * {@link EventToolRegistry}, {@link ResearchToolRegistry}) iterate over
 * the registry and produce one item per tool. The menus that are not
 * (File, Edit, View, Navigate, Bookmarks, Help) contain their items
 * inline, because their vocabulary is small and does not benefit from a
 * registry.
 * <p>
 * <b>File menu.</b> The File menu is the only menu with shared state:
 * the current file and the dirty flag, owned by a
 * {@link FileMenuController}. Every item in the menu delegates to that
 * controller.
 * <p>
 * <b>Tool context.</b> Every menu item that triggers a tool obtains its
 * context from {@link FamilyLegacyFrame#createToolContext()}, so the
 * tools are always wired to the same model, owner, and callbacks.
 */
final class ApplicationMenuBar{

	private final FamilyLegacyFrame frame;


	ApplicationMenuBar(final FamilyLegacyFrame frame){
		this.frame = frame;
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	JMenuBar build(){
		final JMenuBar bar = new JMenuBar();
		bar.add(createFileMenu());
		//TODO remove comment
//		bar.add(createEditMenu());
		bar.add(createViewMenu());
		bar.add(createIndividualMenu());
		bar.add(createGroupMenu());
		bar.add(createPlaceMenu());
		bar.add(createSourceMenu());
		bar.add(createEventMenu());
		bar.add(createResearchMenu());
		//TODO remove comment
//		bar.add(createToolsMenu());
		bar.add(createNavigateMenu());
		bar.add(createBookmarkMenu());
		bar.add(createHelpMenu());
		return bar;
	}


	/* ======================================================================
	 *                          File
	 * ====================================================================== */

	private JMenu createFileMenu(){
		final JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);

		final FileMenuController fileController = frame.fileController();

		final JMenuItem newFile = new JMenuItem("New File…", KeyEvent.VK_N);
		newFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutMask()));
		newFile.addActionListener(e -> fileController.newFile());
		menu.add(newFile);

		final JMenuItem openFile = new JMenuItem("Open File…", KeyEvent.VK_O);
		openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutMask()));
		openFile.addActionListener(e -> fileController.openFile());
		menu.add(openFile);

		menu.add(createRecentFilesMenu(fileController));

		menu.add(new JSeparator());

		final JMenuItem save = new JMenuItem("Save", KeyEvent.VK_S);
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutMask()));
		save.addActionListener(e -> fileController.save());
		menu.add(save);

		final JMenuItem saveAs = new JMenuItem("Save As…", KeyEvent.VK_A);
		saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
			menuShortcutMask() | InputEvent.SHIFT_DOWN_MASK));
		saveAs.addActionListener(e -> fileController.saveAs());
		menu.add(saveAs);

		menu.add(new JSeparator());

		final JMenu importMenu = new JMenu("Import");
		importMenu.setMnemonic(KeyEvent.VK_I);
		for(final ToolOperation tool : FileToolRegistry.importTools())
			importMenu.add(toolItem(tool, 0));
		menu.add(importMenu);

//		final JMenu exportMenu = new JMenu("Export");
//		exportMenu.setMnemonic(KeyEvent.VK_E);
//		for(final ToolOperation tool : FileToolRegistry.exportTools())
//			exportMenu.add(toolItem(tool, 0));
//		menu.add(exportMenu);

		menu.add(new JSeparator());

		final JMenuItem properties = new JMenuItem("File Properties…", KeyEvent.VK_R);
		properties.addActionListener(e -> fileController.showProperties());
		menu.add(properties);

		menu.add(new JSeparator());

		final JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, menuShortcutMask()));
		exit.addActionListener(e -> fileController.exit());
		menu.add(exit);

		return menu;
	}

	/**
	 * Builds the "Open Recent" submenu from the recent files manager.
	 * <p>
	 * The submenu is rebuilt every time it is opened, so it reflects the
	 * current list without needing a listener on the manager. A "Clear
	 * list" entry is added at the bottom when the list is not empty.
	 */
	private JMenu createRecentFilesMenu(final FileMenuController fileController){
		final JMenu menu = new JMenu("Open Recent");
		menu.setMnemonic(KeyEvent.VK_R);

		menu.addMenuListener(new MenuListener(){
			@Override
			public void menuSelected(final MenuEvent e){
				menu.removeAll();

				final List<File> recent = fileController.recentFiles().list();
				if(recent.isEmpty()){
					final JMenuItem empty = new JMenuItem("(no recent files)");
					empty.setEnabled(false);
					menu.add(empty);
					return;
				}

				for(final File file : recent){
					final JMenuItem item = new JMenuItem(file.getName());
					item.setToolTipText(file.getAbsolutePath());
					item.addActionListener(a -> fileController.openFile(file));
					menu.add(item);
				}

				menu.addSeparator();
				final JMenuItem clear = new JMenuItem("Clear list");
				clear.addActionListener(a -> fileController.recentFiles().clear());
				menu.add(clear);
			}

			@Override public void menuDeselected(final MenuEvent e){}
			@Override public void menuCanceled(final MenuEvent e){}
		});

		return menu;
	}


	/* ======================================================================
	 *                          Edit
	 * ====================================================================== */

	private JMenu createEditMenu(){
		final JMenu menu = new JMenu("Edit");
		menu.setMnemonic(KeyEvent.VK_E);

		menu.add(accelerated("Undo", KeyEvent.VK_Z));
		menu.add(accelerated("Redo", KeyEvent.VK_Y));
		menu.add(new JSeparator());
		menu.add(accelerated("Cut", KeyEvent.VK_X));
		menu.add(accelerated("Copy", KeyEvent.VK_C));
		menu.add(accelerated("Paste", KeyEvent.VK_V));
		menu.add(accelerated("Delete", KeyEvent.VK_DELETE));
		menu.add(new JSeparator());
		menu.add(accelerated("Select All", KeyEvent.VK_A));
		menu.add(new JSeparator());
		menu.add(accelerated("Find…", KeyEvent.VK_F));
		menu.add(placeholder("Find Next", 0));
		menu.add(placeholder("Find Previous", 0));
		menu.add(placeholder("Replace…", 0));
		menu.add(new JSeparator());
		menu.add(placeholder("Preferences…", 0));

		return menu;
	}


	/* ======================================================================
	 *                          View
	 * ====================================================================== */

	private JMenu createViewMenu(){
		final JMenu menu = new JMenu("View");
		menu.setMnemonic(KeyEvent.VK_V);

		final JMenu projections = new JMenu("Projection");
		projections.setMnemonic(KeyEvent.VK_P);

		final JRadioButtonMenuItem treeItem = projectionItem(
			"Ancestor Tree", ProjectionType.TREE, KeyEvent.VK_1);
		final JRadioButtonMenuItem graphItem = projectionItem(
			"Sugiyama Graph", ProjectionType.GRAPH, KeyEvent.VK_2);
		final JRadioButtonMenuItem egoItem = projectionItem(
			"Ego Network", ProjectionType.EGO_NETWORK, KeyEvent.VK_3);

		final ButtonGroup projectionGroup = new ButtonGroup();
		projectionGroup.add(treeItem);
		projectionGroup.add(graphItem);
		projectionGroup.add(egoItem);

		projections.add(treeItem);
		projections.add(graphItem);
		projections.add(egoItem);

		// Refresh the selected radio button every time the submenu is
		// opened, so it reflects the active projection even when the
		// switch was triggered by Ctrl+1/2/3 or by the toolbar toggles.
		projections.addMenuListener(new MenuListener(){
			@Override
			public void menuSelected(final MenuEvent e){
				switch(frame.switcher().getCurrentProjectionType()){
					case TREE -> treeItem.setSelected(true);
					case GRAPH -> graphItem.setSelected(true);
					case EGO_NETWORK -> egoItem.setSelected(true);
				}
			}

			@Override public void menuDeselected(final MenuEvent e){}
			@Override public void menuCanceled(final MenuEvent e){}
		});

		menu.add(projections);

		menu.add(new JSeparator());

		final JCheckBoxMenuItem fullScreen = new JCheckBoxMenuItem("Full Screen");
		fullScreen.setMnemonic(KeyEvent.VK_F);
		fullScreen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
		fullScreen.setSelected(frame.isFullScreen());
		fullScreen.addActionListener(e -> frame.toggleFullScreen());
		menu.add(fullScreen);

		menu.add(new JSeparator());

		final JCheckBoxMenuItem showToolbar = new JCheckBoxMenuItem("Show Toolbar");
		showToolbar.setMnemonic(KeyEvent.VK_T);
		showToolbar.setSelected(frame.isToolbarVisible());
		showToolbar.addActionListener(e -> frame.setToolbarVisible(showToolbar.isSelected()));
		menu.add(showToolbar);

		final JCheckBoxMenuItem showSidebar = new JCheckBoxMenuItem("Show Sidebar");
		showSidebar.setMnemonic(KeyEvent.VK_B);
		showSidebar.setSelected(frame.isSidebarVisible());
		showSidebar.addActionListener(e -> frame.setSidebarVisible(showSidebar.isSelected()));
		menu.add(showSidebar);

		// Synchronize checkbox states when the menu is selected
		menu.addMenuListener(new MenuListener(){
			@Override
			public void menuSelected(final MenuEvent e){
				fullScreen.setSelected(frame.isFullScreen());
				showToolbar.setSelected(frame.isToolbarVisible());
				showSidebar.setSelected(frame.isSidebarVisible());
			}

			@Override
			public void menuDeselected(final MenuEvent e){}

			@Override
			public void menuCanceled(final MenuEvent e){}
		});

		return menu;
	}


	/* ======================================================================
	 *                          Individual
	 * ====================================================================== */

	private JMenu createIndividualMenu(){
		final JMenu menu = new JMenu("Individual");
		menu.setMnemonic(KeyEvent.VK_I);

		final List<ToolItemBinding> bindings = new ArrayList<>();

		addToolsToMenu(menu, IndividualToolRegistry.primaryTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, IndividualToolRegistry.relationshipTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, IndividualToolRegistry.advancedTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, IndividualToolRegistry.navigationTools(), bindings);

		bindDynamicEnablement(menu, bindings);

		return menu;
	}


	/* ======================================================================
	 *                          Group
	 * ====================================================================== */

	private JMenu createGroupMenu(){
		final JMenu menu = new JMenu("Group");
		menu.setMnemonic(KeyEvent.VK_G);

		final List<ToolItemBinding> bindings = new ArrayList<>();

		addToolsToMenu(menu, GroupToolRegistry.primaryTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, GroupToolRegistry.membershipTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, GroupToolRegistry.advancedTools(), bindings);

		bindDynamicEnablement(menu, bindings);

		return menu;
	}


	/* ======================================================================
	 *                          Place
	 * ====================================================================== */

	private JMenu createPlaceMenu(){
		final JMenu menu = new JMenu("Place");
		menu.setMnemonic(KeyEvent.VK_L);

		final List<ToolItemBinding> bindings = new ArrayList<>();

		addToolsToMenu(menu, PlaceToolRegistry.primaryTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, PlaceToolRegistry.geoAndNormalizationTools(), bindings);

		bindDynamicEnablement(menu, bindings);

		return menu;
	}


	/* ======================================================================
	 *                          Source
	 * ====================================================================== */

	private JMenu createSourceMenu(){
		final JMenu menu = new JMenu("Source");
		menu.setMnemonic(KeyEvent.VK_S);

		final List<ToolItemBinding> bindings = new ArrayList<>();

		addToolsToMenu(menu, SourceToolRegistry.primaryTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, SourceToolRegistry.repositoryTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, SourceToolRegistry.documentTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, SourceToolRegistry.citationTools(), bindings);

		bindDynamicEnablement(menu, bindings);

		return menu;
	}


	/* ======================================================================
	 *                          Event
	 * ====================================================================== */

	private JMenu createEventMenu(){
		final JMenu menu = new JMenu("Event");
		menu.setMnemonic(KeyEvent.VK_T);

		final List<ToolItemBinding> bindings = new ArrayList<>();

		addToolsToMenu(menu, EventToolRegistry.primaryTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, EventToolRegistry.referenceTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, EventToolRegistry.analysisTools(), bindings);

		bindDynamicEnablement(menu, bindings);

		return menu;
	}


	/* ======================================================================
	 *                          Research
	 * ====================================================================== */

	private JMenu createResearchMenu(){
		final JMenu menu = new JMenu("Research");
		menu.setMnemonic(KeyEvent.VK_R);

		final List<ToolItemBinding> bindings = new ArrayList<>();

		addToolsToMenu(menu, ResearchToolRegistry.planningTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, ResearchToolRegistry.analysisTools(), bindings);
		menu.add(new JSeparator());
		addToolsToMenu(menu, ResearchToolRegistry.reportTools(), bindings);

		bindDynamicEnablement(menu, bindings);

		return menu;
	}


	/* ======================================================================
	 *                          Tools
	 * ====================================================================== */

	private JMenu createToolsMenu(){
		final JMenu menu = new JMenu("Tools");
		menu.setMnemonic(KeyEvent.VK_O);

		menu.add(placeholder("Validate File…", 0));
		menu.add(placeholder("Check Consistency…", 0));
		menu.add(placeholder("Detect Duplicates…", 0));
		menu.add(new JSeparator());

		final JMenu reports = new JMenu("Reports");
		reports.setMnemonic(KeyEvent.VK_R);
		reports.add(placeholder("Ancestor Report…", 0));
		reports.add(placeholder("Descendant Report…", 0));
		reports.add(placeholder("Family Group Sheet…", 0));
		reports.add(placeholder("Individual Summary…", 0));
		reports.add(placeholder("Relationship Report…", 0));
		reports.add(placeholder("Bibliography…", 0));
		reports.add(placeholder("Research Progress…", 0));
		menu.add(reports);

		final JMenu charts = new JMenu("Charts");
		charts.setMnemonic(KeyEvent.VK_C);
		charts.add(placeholder("Ancestor Chart…", 0));
		charts.add(placeholder("Descendant Chart…", 0));
		charts.add(placeholder("Hourglass Chart…", 0));
		charts.add(placeholder("Fan Chart…", 0));
		charts.add(placeholder("Bowtie Chart…", 0));
		charts.add(placeholder("Relationship Chart…", 0));
		charts.add(placeholder("Map Chart…", 0));
		menu.add(charts);

		menu.add(new JSeparator());
		menu.add(placeholder("Statistics…", 0));
		menu.add(placeholder("Data Cleanup…", 0));
		menu.add(placeholder("Recompute Derived Data", 0));
		menu.add(new JSeparator());
		menu.add(placeholder("Backup…", 0));
		menu.add(placeholder("Restore from Backup…", 0));
		menu.add(placeholder("Compare Two Files…", 0));
		menu.add(new JSeparator());
		menu.add(placeholder("Plugins…", 0));

		return menu;
	}


	/* ======================================================================
	 *                          Navigate
	 * ====================================================================== */

	private JMenu createNavigateMenu(){
		final JMenu menu = new JMenu("Navigate");
		menu.setMnemonic(KeyEvent.VK_N);

		final JMenuItem back = new JMenuItem("Back", KeyEvent.VK_B);
		back.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, menuShortcutMask()));
		back.addActionListener(e -> {
			if(frame.switcher().canGoBack())
				frame.switcher().navigateBack();
		});
		menu.add(back);

		final JMenuItem forward = new JMenuItem("Forward", KeyEvent.VK_F);
		forward.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, menuShortcutMask()));
		forward.addActionListener(e -> {
			if(frame.switcher().canGoForward())
				frame.switcher().navigateForward();
		});
		menu.add(forward);

		menu.add(new JSeparator());

		final JMenuItem jumpTo = new JMenuItem("Jump to Individual…", KeyEvent.VK_J);
		jumpTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, menuShortcutMask()));
		jumpTo.addActionListener(e -> openJumpToDialog());
		menu.add(jumpTo);

		menu.add(new JSeparator());

		final JMenuItem clearHistory = new JMenuItem("Clear History", KeyEvent.VK_C);
		clearHistory.addActionListener(e -> {
			frame.switcher().clearHistory();
			JOptionPane.showMessageDialog(frame,
				"Navigation history has been cleared.",
				"History Cleared", JOptionPane.INFORMATION_MESSAGE);
		});
		menu.add(clearHistory);

		// The enabled state of Back, Forward and Clear History depends on
		// the current history. A MenuListener refreshes the state every
		// time the menu is opened, so no global history listener is needed.
		menu.addMenuListener(new MenuListener(){
			@Override
			public void menuSelected(final MenuEvent e){
				final boolean canGoBack = frame.switcher().canGoBack();
				final boolean canGoForward = frame.switcher().canGoForward();
				back.setEnabled(canGoBack);
				forward.setEnabled(canGoForward);
				clearHistory.setEnabled(canGoBack || canGoForward);

				// The jump-to dialog offers different record types
				// depending on the active projection.
				final boolean ego = frame.switcher().getCurrentProjectionType()
					== ProjectionType.EGO_NETWORK;
				jumpTo.setText(ego
					? "Jump to Individual or Group…"
					: "Jump to Individual…");
			}

			@Override public void menuDeselected(final MenuEvent e){}
			@Override public void menuCanceled(final MenuEvent e){}
		});

		// Initial state, consistent with an empty history.
		back.setEnabled(false);
		forward.setEnabled(false);
		clearHistory.setEnabled(false);

		return menu;
	}

	private void openJumpToDialog(){
		final String id = JumpToIndividualDialog.showAndGet(frame, frame.model(),
			frame.switcher().getCurrentProjectionType());
		if(id != null)
			frame.loadRoot(id);
	}


	/* ======================================================================
	 *                          Bookmarks
	 * ====================================================================== */

	private JMenu createBookmarkMenu(){
		final JMenu menu = new JMenu("Bookmarks");
		menu.setMnemonic(KeyEvent.VK_B);

		final BookmarkMenu bookmarkMenu = new BookmarkMenu(frame, frame.bookmarkStore(), frame::captureCurrentState,
			frame::applyBookmark);

		// Transfer the items of the existing BookmarkMenu into the new
		// JMenu, so the rest of the menu bar keeps a consistent structure.
		for(int i = 0; i < bookmarkMenu.getItemCount(); i++){
			final JMenuItem item = bookmarkMenu.getItem(i);
			if(item == null)
				menu.addSeparator();
			else
				menu.add(item);
		}
		return menu;
	}


	/* ======================================================================
	 *                          Help
	 * ====================================================================== */

	private JMenu createHelpMenu(){
		final JMenu menu = new JMenu("Help");
		menu.setMnemonic(KeyEvent.VK_H);

		final JMenuItem helpContents = new JMenuItem("Help Contents", KeyEvent.VK_H);
		helpContents.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		helpContents.addActionListener(e -> HelpViewerDialog.show(frame,
			"Help Contents", HelpContent.HELP_CONTENTS_HTML));
		menu.add(helpContents);

		final JMenuItem shortcuts = new JMenuItem("Keyboard Shortcuts…", KeyEvent.VK_K);
		shortcuts.addActionListener(e -> KeyboardShortcutsDialog.show(frame));
		menu.add(shortcuts);

		menu.add(new JSeparator());

		final JMenuItem userGuide = new JMenuItem("User Guide…", KeyEvent.VK_G);
		userGuide.addActionListener(e -> HelpViewerDialog.show(frame,
			"User Guide", HelpContent.USER_GUIDE_HTML));
		menu.add(userGuide);

		final JMenuItem onlineDocs = new JMenuItem("Online Documentation…", KeyEvent.VK_D);
		onlineDocs.addActionListener(e -> openUrl(HelpContent.DOCUMENTATION_URL));
		menu.add(onlineDocs);

		menu.add(new JSeparator());

		final JMenuItem checkUpdates = new JMenuItem("Check for Updates…", KeyEvent.VK_U);
		checkUpdates.addActionListener(e -> checkForUpdates());
		menu.add(checkUpdates);

		final JMenuItem reportBug = new JMenuItem("Report a Bug…", KeyEvent.VK_B);
		reportBug.addActionListener(e -> openUrl(HelpContent.ISSUE_TRACKER_URL));
		menu.add(reportBug);

		final JMenuItem diagnostics = new JMenuItem("Diagnostics…", KeyEvent.VK_I);
		diagnostics.addActionListener(e -> DiagnosticsDialog.show(frame, frame.model()));
		menu.add(diagnostics);

		menu.add(new JSeparator());

		final JMenuItem about = new JMenuItem("About Family Legacy", KeyEvent.VK_A);
		about.addActionListener(e -> showAboutDialog());
		menu.add(about);

		return menu;
	}


	/* ======================================================================
	 *                          Help actions
	 * ====================================================================== */

	private void openUrl(final String url){
		if(!Desktop.isDesktopSupported()){
			showUnsupportedBrowserMessage();
			return;
		}
		final Desktop desktop = Desktop.getDesktop();
		if(!desktop.isSupported(Desktop.Action.BROWSE)){
			showUnsupportedBrowserMessage();
			return;
		}
		try{
			desktop.browse(new URI(url));
		}
		catch(final URISyntaxException e){
			JOptionPane.showMessageDialog(frame,
				"Invalid URL: " + url,
				"Error", JOptionPane.ERROR_MESSAGE);
		}
		catch(final IOException e){
			JOptionPane.showMessageDialog(frame,
				"Unable to open the URL: " + url + "\n" + e.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void showUnsupportedBrowserMessage(){
		JOptionPane.showMessageDialog(frame,
			"Opening web pages is not supported on this platform.",
			"Not Supported", JOptionPane.WARNING_MESSAGE);
	}

	private void checkForUpdates(){
		final Object[] options = {"Open Releases Page", "Close"};
		final int choice = JOptionPane.showOptionDialog(frame,
			"<html>You are running <b>Family Legacy "
				+ HelpContent.APPLICATION_VERSION + "</b>.<br><br>"
				+ "Open the releases page to check whether a newer version is available?</html>",
			"Check for Updates",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.INFORMATION_MESSAGE,
			null, options, options[0]);
		if(choice == 0)
			openUrl(HelpContent.RELEASES_URL);
	}

	private void showAboutDialog(){
		final String message = """
			<html>
			<b>Family Legacy</b><br>
			Version %s<br><br>
			A modern genealogical data manager based on the<br>
			Family LEgacy Format (FLEF) protocol.<br><br>
			&copy; 2026 Mauro Trevisan<br>
			Distributed under the MIT License.
			</html>
			""".formatted(HelpContent.APPLICATION_VERSION);
		JOptionPane.showMessageDialog(frame, message, "About Family Legacy",
			JOptionPane.INFORMATION_MESSAGE);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private void addToolsToMenu(final JMenu menu, final List<ToolOperation> tools, final List<ToolItemBinding> bindings){
		for(final ToolOperation tool : tools){
			final JMenuItem item = toolItem(tool, 0);
			menu.add(item);

			bindings.add(new ToolItemBinding(item, tool));
		}
	}

	/** Returns the platform menu shortcut mask (Ctrl on Windows/Linux, Cmd on macOS). */
	private static int menuShortcutMask(){
		return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	}

	/**
	 * Creates a menu item bound to the given tool. The tool runs with a
	 * fresh context built from the frame's current state.
	 */
	private JMenuItem toolItem(final ToolOperation tool, final int mnemonic){
		final JMenuItem item = new JMenuItem(tool.getName(), mnemonic);
		item.addActionListener(e -> tool.run(frame.createToolContext()));
		item.setEnabled(tool.isEnabled(frame.createToolContext()));
		return item;
	}

	/**
	 * Binds a MenuListener to dynamically update the enabled state of all tool items
	 * based on ToolOperation#isEnabled(ToolContext).
	 */
	private void bindDynamicEnablement(final JMenu menu, final List<ToolItemBinding> bindings){
		menu.addMenuListener(new MenuListener(){
			@Override
			public void menuSelected(final MenuEvent e){
				final ToolContext context = frame.createToolContext();
				for(final ToolItemBinding binding : bindings)
					binding.item().setEnabled(binding.tool().isEnabled(context));
			}

			@Override
			public void menuDeselected(final MenuEvent e){}

			@Override
			public void menuCanceled(final MenuEvent e){}
		});
	}

	private record ToolItemBinding(JMenuItem item, ToolOperation tool){}

	/**
	 * Creates a menu item whose action shows a "not implemented yet"
	 * dialog. Used to expose the planned functionality without wiring it
	 * up.
	 */
	private JMenuItem placeholder(final String text, final int mnemonic){
		final JMenuItem item = new JMenuItem(text, mnemonic);
		item.addActionListener(e -> showNotImplemented(text.replace("…", StringUtils.EMPTY).trim()));
		return item;
	}

	/** Creates a menu item with a platform-appropriate accelerator. */
	private JMenuItem accelerated(final String text, final int keyCode){
		return accelerated(text, keyCode, 0);
	}

	private JMenuItem accelerated(final String text, final int keyCode, final int modifiers){
		final JMenuItem item = new JMenuItem(text);
		item.setAccelerator(KeyStroke.getKeyStroke(keyCode, menuShortcutMask() | modifiers));
		item.addActionListener(e -> showNotImplemented(text.replace("…", StringUtils.EMPTY).trim()));
		return item;
	}

	/** Creates a menu item that switches the active projection. */
	private JRadioButtonMenuItem projectionItem(final String text, final ProjectionType projection, final int keyCode){
		final JRadioButtonMenuItem item = new JRadioButtonMenuItem(text);
		item.setAccelerator(KeyStroke.getKeyStroke(keyCode, menuShortcutMask()));
		item.addActionListener(e -> frame.switcher().setProjection(projection));
		return item;
	}

	private void showNotImplemented(final String feature){
		JOptionPane.showMessageDialog(frame,
			"\"" + feature + "\" is not implemented yet.",
			"Feature Not Available", JOptionPane.INFORMATION_MESSAGE);
	}

}
