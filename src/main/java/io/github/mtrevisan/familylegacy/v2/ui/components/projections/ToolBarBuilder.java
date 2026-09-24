package io.github.mtrevisan.familylegacy.v2.ui.components.projections;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.help.ShortcutRegistry;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import java.util.function.Consumer;


/**
 * Encapsulates the construction and action bindings of the main application toolbar.
 */
public final class ToolBarBuilder{

	private final JToolBar toolBar = new JToolBar("Main Toolbar");
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
		btnBack.setToolTipText("Navigate Back (" + ShortcutRegistry.NAV_BACK.displayKeys() + ")");
		btnBack.addActionListener(e -> onBack.run());

		final JButton btnForward = new JButton("►");
		btnForward.setToolTipText("Navigate Forward (" + ShortcutRegistry.NAV_FORWARD.displayKeys() + ")");
		btnForward.addActionListener(e -> onForward.run());

		final JButton btnJump = new JButton("Jump To…");
		btnJump.setToolTipText("Jump to Individual/Group (" + ShortcutRegistry.NAV_JUMP_TO_INDIVIDUAL_OR_GROUP.displayKeys() + ")");
		btnJump.addActionListener(e -> onJump.run());

		final JButton btnEdit = new JButton("Edit");
		btnEdit.setToolTipText("Edit Current Selection (" + ShortcutRegistry.EDIT_SELECTION.displayKeys() + ")");
		btnEdit.addActionListener(e -> onEdit.run());

		toolBar.add(btnBack);
		toolBar.add(btnForward);
		toolBar.add(btnJump);
		toolBar.add(btnEdit);
		toolBar.addSeparator();

		// Projections
		btnTreeLayout = new JToggleButton("Tree");
		btnTreeLayout.setToolTipText("Ancestor Tree (" + ShortcutRegistry.VIEW_ANCESTOR_TREE.displayKeys() + ")");
		btnTreeLayout.setSelected(true);
		btnTreeLayout.addActionListener(e -> onProjectionSelect.accept(ProjectionType.TREE));

		btnGraphLayout = new JToggleButton("Sugiyama");
		btnGraphLayout.setToolTipText("Sugiyama Pedigree Graph (" + ShortcutRegistry.VIEW_SUGIYAMA_GRAPH.displayKeys() + ")");
		btnGraphLayout.addActionListener(e -> onProjectionSelect.accept(ProjectionType.GRAPH));

		btnEgoLayout = new JToggleButton("Ego Net");
		btnEgoLayout.setToolTipText("Ego Network (" + ShortcutRegistry.VIEW_EGO_NETWORK.displayKeys() + ")");
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
		btnToggleSidebar = new JToggleButton("Sidebar", initialSidebarVisible);
		btnToggleSidebar.setToolTipText("Toggle Dossier Sidebar Panel");
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
