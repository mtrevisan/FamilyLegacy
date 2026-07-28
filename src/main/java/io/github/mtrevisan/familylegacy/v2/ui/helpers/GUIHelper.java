package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Utilities for installing UI behaviors (popup menus, double‑click, keyboard shortcuts).
 * <p>
 * The main entry point is {@link #installBehavior(JComponent, Supplier, Runnable, Runnable, Runnable, Consumer)},
 * which lets you define the popup menu structure exactly via a {@link MenuBuilder}.
 * You can specify any number of items, separators, and custom enable conditions.
 * <p>
 * A legacy overload is provided for backward compatibility, but it is just a wrapper
 * that builds a standard menu using the same builder.
 */
public final class GUIHelper{

	private GUIHelper(){}


	public static JScrollPane createScrollPane(final JList<?> list){
		final JScrollPane scrollPane = new JScrollPane(new ScrollableContainerHost(list,
			ScrollableContainerHost.ScrollType.VERTICAL));
		scrollPane.setPreferredSize(list.getPreferredScrollableViewportSize());
		return scrollPane;
	}

	public static JScrollPane createScrollPane(final JTextComponent area){
		return new JScrollPane(new ScrollableContainerHost(area,
			ScrollableContainerHost.ScrollType.VERTICAL));
	}

	public static JScrollPane createScrollPane(final JComponent area){
		return new JScrollPane(new ScrollableContainerHost(area,
			ScrollableContainerHost.ScrollType.VERTICAL));
	}


	/**
	 * Installs behavior with full control over the popup menu structure.
	 * <p>
	 * The menu is built using a {@link MenuBuilder} that lets you specify the exact
	 * sequence of items, separators, and their enabled state. The popup is re‑created
	 * each time it is shown, so enabled states are always current.
	 *
	 * @param component	The component to enhance.
	 * @param hasSelection	Supplier that tells whether an item is selected.
	 * @param doubleClickAction	Action invoked on double‑click (may be {@code null}).
	 * @param keyInsertAction	Action invoked by the INSERT key (may be {@code null}).
	 * @param keyDeleteAction	Action invoked by the DELETE key (may be {@code null}).
	 * @param menuBuilder	Consumer that defines the popup menu structure.
	 */
	public static void installBehavior(final JComponent component, final Supplier<Boolean> hasSelection,
			final Runnable doubleClickAction, final Runnable keyInsertAction, final Runnable keyDeleteAction,
			final Consumer<MenuBuilder> menuBuilder){
		// Collect the menu entries from the builder
		final MenuBuilder builder = new MenuBuilder(hasSelection);
		menuBuilder.accept(builder);
		final List<MenuEntry> entries = builder.getEntries();

		// Mouse listener for popup trigger and double‑click
		component.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent me){
				if(me.getClickCount() == 2 && doubleClickAction != null
						&& (!(component instanceof JList<?> list) || list.getSelectedIndex() >= 0))
					doubleClickAction.run();
			}

			@Override
			public void mousePressed(final MouseEvent me){
				if(me.isPopupTrigger())
					showPopup(me);
			}

			@Override
			public void mouseReleased(final MouseEvent me){
				if(me.isPopupTrigger())
					showPopup(me);
			}

			private void showPopup(final MouseEvent me){
				// Ensure the clicked item gets selected
				if(component instanceof JList<?> list){
					final int index = list.locationToIndex(me.getPoint());
					if(index >= 0 && !list.isSelectedIndex(index))
						list.setSelectedIndex(index);
				}
				// Build the popup from scratch (enabled states are evaluated now)
				final JPopupMenu popup = buildPopup(entries);
				popup.show(component, me.getX(), me.getY());
			}
		});

		// Keyboard shortcuts
		if(keyInsertAction != null)
			addKeyboardShortcut(component, KeyEvent.VK_INSERT, "insert-action", keyInsertAction);
		if(keyDeleteAction != null && component instanceof JList)
			addKeyboardShortcut(component, KeyEvent.VK_DELETE, "delete-action", () -> {
				if(hasSelection.get())
					keyDeleteAction.run();
			});
	}

	private static void addKeyboardShortcut(final JComponent component, final int virtualKey, final String actionMapKey,
			final Runnable action){
		component.getInputMap()
			.put(KeyStroke.getKeyStroke(virtualKey, 0), actionMapKey);
		component.getActionMap()
			.put(actionMapKey, new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent ae){
					action.run();
				}
			});
	}

	/**
	 * It builds a standard menu with "Create New...", "Add Existing...",
	 * "Edit...", "Clear", "Notes..." (where "Edit", "Clear", and "Notes"
	 * are selection‑sensitive). This is just a convenience wrapper around
	 * the builder version.
	 */
	public static void installStandardBehavior(final JComponent component, final Supplier<Boolean> hasSelection,
			final Runnable createNewAction, final Runnable addAction, final Runnable editAction,
			final Runnable clearAction, final Runnable notesAction){
		installBehavior(component, hasSelection, editAction, createNewAction, clearAction,
			builder -> {
				if(createNewAction != null)
					builder.item("Create New...", createNewAction);
				if(addAction != null)
					builder.item("Add Existing...", addAction);
				if(editAction != null || clearAction != null || notesAction != null)
					builder.separator();
				if(editAction != null)
					builder.selectionSensitiveItem("Edit...", editAction);
				if(clearAction != null)
					builder.selectionSensitiveItem("Clear", clearAction);
				if(notesAction != null){
					if(editAction != null || clearAction != null)
						builder.separator();

					builder.selectionSensitiveItem("Notes...", notesAction);
				}
			});
	}


	private static JPopupMenu buildPopup(final List<MenuEntry> entries){
		final JPopupMenu popup = new JPopupMenu();
		for(final MenuEntry e : entries){
			if(e.isSeparator)
				popup.addSeparator();
			else{
				final JMenuItem item = new JMenuItem(e.label);
				item.addActionListener(ev -> e.action.run());
				item.setEnabled(e.enabledCondition.get());
				popup.add(item);
			}
		}
		return popup;
	}


	/**
	 * Builder that collects menu entries.
	 * <p>
	 * You can call {@link #item(String, Runnable)} to add a plain item,
	 * {@link #selectionSensitiveItem(String, Runnable)} for an item that is enabled only when there is a selection, or
	 * {@link #item(String, Runnable, Supplier)} for a fully custom enable condition.
	 * {@link #separator()} inserts a separator.
	 */
	public static final class MenuBuilder{

		private final Supplier<Boolean> hasSelection;
		private final List<MenuEntry> entries = new ArrayList<>();


		private MenuBuilder(final Supplier<Boolean> hasSelection){
			this.hasSelection = hasSelection;
		}

		/**
		 * Adds a menu item that is always enabled.
		 *
		 * @param label	The item label.
		 * @param action	The action to run when clicked.
		 * @return	This builder.
		 */
		public MenuBuilder item(final String label, final Runnable action){
			entries.add(MenuEntry.createEntry(label, action, () -> true));
			return this;
		}

		/**
		 * Adds a menu item that is enabled only when {@link #hasSelection} is {@code true}.
		 *
		 * @param label	The item label.
		 * @param action	The action to run when clicked.
		 * @return	This builder.
		 */
		public MenuBuilder selectionSensitiveItem(final String label, final Runnable action){
			entries.add(MenuEntry.createEntry(label, action, hasSelection));
			return this;
		}

		/**
		 * Adds a menu item with a custom enable condition.
		 *
		 * @param label	The item label.
		 * @param action	The action to run when clicked.
		 * @param enabledCondition	Supplier that returns {@code true} when the item should be enabled.
		 * @return	This builder.
		 */
		public MenuBuilder item(final String label, final Runnable action, final Supplier<Boolean> enabledCondition){
			entries.add(MenuEntry.createEntry(label, action, enabledCondition));
			return this;
		}

		/**
		 * Adds a separator.
		 *
		 * @return	This builder.
		 */
		public MenuBuilder separator(){
			entries.add(MenuEntry.createSeparator());
			return this;
		}

		private List<MenuEntry> getEntries(){
			return entries;
		}
	}



	/**
	 * Returns the parent frame of this panel.
	 *
	 * @return the parent frame, or {@code null} if not found
	 */
	public static Frame getParentFrame(final Dialog dialog){
		Container parent = dialog.getParent();
		while(parent != null && !(parent instanceof Frame))
			parent = parent.getParent();
		return (Frame)parent;
	}


	public static void showValidationErrorAndFocus(final Component parentComponent, final String message,
			final JTabbedPane tabbedPane, final JPanel panel, final JComponent component){
		JOptionPane.showMessageDialog(parentComponent,
			message,
			"Validation Error", JOptionPane.ERROR_MESSAGE);

		if(tabbedPane != null && panel != null)
			tabbedPane.setSelectedComponent(panel);
		SwingUtilities.invokeLater(component::requestFocusInWindow);
	}


	/**
	 * Immutable record representing a menu entry.
	 */
	private record MenuEntry(String label, Runnable action, Supplier<Boolean> enabledCondition, boolean isSeparator){
		private static MenuEntry createEntry(String label, Runnable action, Supplier<Boolean> enabledCondition){
			return new MenuEntry(label, action, enabledCondition, false);
		}

		private static MenuEntry createSeparator(){
			return new MenuEntry(null, null, null, true);
		}
	}

}
