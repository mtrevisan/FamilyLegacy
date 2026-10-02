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

import io.github.mtrevisan.familylegacy.ui.dialogs.help.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import java.util.function.Consumer;


/**
 * Encapsulates the construction and action bindings of the main application toolbar.
 */
public final class ToolBarBuilder{

	private final JToolBar toolBar = new JToolBar(I18N.t("toolbar.title"));
	private JToggleButton btnTreeLayout;
	private JToggleButton btnGraphLayout;
	private JToggleButton btnEgoLayout;
	private JToggleButton btnToggleSidebar;


	public JToolBar build(final Runnable onBack, final Runnable onForward, final Runnable onJump, final Runnable onEdit,
			final Consumer<ProjectionType> onProjectionSelect, final Consumer<Boolean> onSidebarToggle,
			final boolean initialSidebarVisible){
		toolBar.setFloatable(false);

		// Navigation
		final JButton btnBack = new JButton("◄");
		btnBack.setToolTipText(I18N.t("toolbar.navigate.back") + " (" + ShortcutRegistry.NAV_BACK.displayKeys() + ")");
		btnBack.addActionListener(e -> onBack.run());

		final JButton btnForward = new JButton("►");
		btnForward.setToolTipText(I18N.t("toolbar.navigate.forward") + " (" + ShortcutRegistry.NAV_FORWARD.displayKeys() + ")");
		btnForward.addActionListener(e -> onForward.run());

		final JButton btnJump = new JButton("Jump To…");
		btnJump.setToolTipText(I18N.t("toolbar.jump.to") + " (" + ShortcutRegistry.NAV_JUMP_TO_INDIVIDUAL_OR_GROUP.displayKeys() + ")");
		btnJump.addActionListener(e -> onJump.run());

		final JButton btnEdit = new JButton(I18N.t("button.edit"));
		btnEdit.setToolTipText(I18N.t("toolbar.edit.selection") + " (" + ShortcutRegistry.EDIT_SELECTION_INDIVIDUAL.displayKeys() + ")");
		btnEdit.addActionListener(e -> onEdit.run());

		toolBar.add(btnBack);
		toolBar.add(btnForward);
		toolBar.add(btnJump);
		toolBar.add(btnEdit);
		toolBar.addSeparator();

		// Projections
		btnTreeLayout = new JToggleButton(I18N.t("toolbar.projection.tree.layout"));
		btnTreeLayout.setToolTipText(I18N.t("toolbar.projection.tree.layout.tooltip") + " (" + ShortcutRegistry.VIEW_ANCESTOR_TREE.displayKeys() + ")");
		btnTreeLayout.setSelected(true);
		btnTreeLayout.addActionListener(e -> onProjectionSelect.accept(ProjectionType.TREE));

		btnGraphLayout = new JToggleButton(I18N.t("toolbar.projection.sugiyama.graph"));
		btnGraphLayout.setToolTipText(I18N.t("toolbar.projection.sugiyama.graph.tooltip") + " (" + ShortcutRegistry.VIEW_SUGIYAMA_GRAPH.displayKeys() + ")");
		btnGraphLayout.addActionListener(e -> onProjectionSelect.accept(ProjectionType.GRAPH));

		btnEgoLayout = new JToggleButton(I18N.t("toolbar.projection.ego.network"));
		btnEgoLayout.setToolTipText(I18N.t("toolbar.projection.ego.network.tooltip") + " (" + ShortcutRegistry.VIEW_EGO_NETWORK.displayKeys() + ")");
		btnEgoLayout.addActionListener(e -> onProjectionSelect.accept(ProjectionType.EGO_NETWORK));

		final ButtonGroup projectionGroup = new ButtonGroup();
		projectionGroup.add(btnTreeLayout);
		projectionGroup.add(btnGraphLayout);
		projectionGroup.add(btnEgoLayout);

		toolBar.add(btnTreeLayout);
		toolBar.add(btnGraphLayout);
		toolBar.add(btnEgoLayout);
		toolBar.addSeparator();

		// Sidebar Toggle
		btnToggleSidebar = new JToggleButton(I18N.t("toolbar.sidebar"), initialSidebarVisible);
		btnToggleSidebar.setToolTipText(I18N.t("toolbar.sidebar.tooltip"));
		btnToggleSidebar.addActionListener(e -> onSidebarToggle.accept(btnToggleSidebar.isSelected()));

		toolBar.add(btnToggleSidebar);

		return toolBar;
	}

	public void updateProjectionState(final ProjectionType type){
		switch(type){
			case TREE -> btnTreeLayout.setSelected(true);
			case GRAPH -> btnGraphLayout.setSelected(true);
			case EGO_NETWORK -> btnEgoLayout.setSelected(true);
		}
	}

}
