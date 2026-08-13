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
package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Utilities for installing UI behaviors (popup menus, double‑click, keyboard shortcuts).
 */
public final class GUIHelper{

	public static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	public static final KeyStroke INSERT_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);
	public static final KeyStroke DELETE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);

	public static final KeyStroke CTRL_UP_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK);
	public static final KeyStroke CTRL_DOWN_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK);

	private static final Color COLOR_BACKGROUND = UIManager.getColor("TextField.background");
	private static final Color COLOR_FOREGROUND_ENABLED = UIManager.getColor("TextField.foreground");
	private static final Color COLOR_FOREGROUND_DISABLED = UIManager.getColor("Label.disabledForeground");


	private static final String PLACEHOLDER_LIST = "(no items)";
	private static final String PLACEHOLDER_TEXT = "(right-click to set)";
	private static final String TOOLTIP_TEXT = "Right-click for actions, double‑click to edit";

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
	 * Decorates or creates a JList that paints a placeholder when empty.
	 */
	public static <E> JList<E> createList(final ListModel<E> model){
		return new JList<>(model){
			@Serial
			private static final long serialVersionUID = 1004864634885107966L;

			@Override
			protected void paintComponent(final Graphics g){
				super.paintComponent(g);

				// If the list is empty, draw the placeholder text directly on the JList graph.
				if(getModel().getSize() == 0){
					final Graphics2D g2 = (Graphics2D)g.create();
					try{
						g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
						g2.setColor(COLOR_FOREGROUND_DISABLED);
						g2.setFont(getFont());

						final FontMetrics fm = g2.getFontMetrics();
						g2.drawString(PLACEHOLDER_LIST, 2, fm.getAscent() + 3);
					}
					finally{
						g2.dispose();
					}
				}
			}
		};
	}


	/**
	 * Installs behavior with full control over the popup menu structure.
	 * <p>
	 * The menu is built using a {@link MenuBuilder} that lets you specify the exact
	 * sequence of items, separators, and their enabled state. The popup is re‑created
	 * each time it is shown, so enabled states are always current.
	 *
	 * @param component	The component to enhance.
	 * @param doubleClickAction	Action invoked on double‑click (may be {@code null}).
	 * @param keyInsertAction	Action invoked by the INSERT key (may be {@code null}).
	 * @param keyDeleteAction	Action invoked by the DELETE key (may be {@code null}).
	 * @param menuBuilder	Consumer that defines the popup menu structure.
	 */
	public static void installBehavior(final JComponent component,
			final Runnable doubleClickAction, final Runnable keyInsertAction, final Runnable keyDeleteAction,
			final Consumer<MenuBuilder> menuBuilder){
		if(component instanceof JTextComponent field)
			field.setEditable(false);
		component.setBackground(COLOR_BACKGROUND);
		component.setToolTipText(TOOLTIP_TEXT);
		if(component instanceof JList<?> list)
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Collect the menu entries from the builder
		final Supplier<Boolean> hasSelection = buildSelectionSupplier(component);
		final MenuBuilder builder = new MenuBuilder(hasSelection);
		menuBuilder.accept(builder);
		final List<MenuEntry> entries = builder.getEntries();

		// Mouse listener for popup trigger and double‑click
		component.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent me){
				if(me.getClickCount() == 2 && doubleClickAction != null && hasSelection.get())
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
		if(keyDeleteAction != null){
			addKeyboardShortcut(component, KeyEvent.VK_DELETE, "delete-action", () -> {
				if(hasSelection.get())
					keyDeleteAction.run();
			});
		}
	}

	private static Supplier<Boolean> buildSelectionSupplier(final JComponent component){
		if(component instanceof JList<?> list)
			return () -> (list.getSelectedIndex() != -1);

		if(component instanceof JTextComponent textComp)
			return () -> (StringUtils.isNotBlank(textComp.getText()) && !isPlaceholder(textComp.getText()));

		if(component instanceof JButton button)
			return () -> {
				final Icon icon = button.getIcon();
				return (icon != null && icon != PreferredImagePanel.PLACEHOLDER_ICON);
			};

		// For other components, no meaningful selection; default to false
		return () -> false;
	}

	public static boolean isPlaceholder(final String text){
		return PLACEHOLDER_TEXT.equals(text);
	}


	public static String limitTextLength(final String text){
		return (text.length() > 50? text.substring(0, 49) + "…": text);
	}

	public static void updateDisplay(final JTextComponent component, final Supplier<Boolean> hasData,
		final Supplier<String> getText, final Consumer<String> setText){
		if(hasData.get()){
			setText.accept(getText.get());
			component.setForeground(COLOR_FOREGROUND_ENABLED);
		}
		else{
			setText.accept(PLACEHOLDER_TEXT);
			component.setForeground(COLOR_FOREGROUND_DISABLED);
		}
	}

	public static void updateDisplay(final JTextComponent component, final Supplier<Boolean> hasData,
		final Supplier<String> getText){
		if(hasData.get()){
			component.setText(getText.get());
			component.setForeground(COLOR_FOREGROUND_ENABLED);
		}
		else{
			component.setText(PLACEHOLDER_TEXT);
			component.setForeground(COLOR_FOREGROUND_DISABLED);
		}
	}

	private static void addKeyboardShortcut(final JComponent component, final int virtualKey, final String actionMapKey,
		final Runnable action){
		component.getInputMap()
			.put(KeyStroke.getKeyStroke(virtualKey, 0), actionMapKey);
		component.getActionMap()
			.put(actionMapKey, new AbstractAction(){
				@Serial
				private static final long serialVersionUID = 3859254441434336995L;

				@Override
				public void actionPerformed(final ActionEvent ae){
					action.run();
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


	/**
	 * Displays the JPopupMenu associated with the specified component, positioning it directly beneath its lower edge.
	 *
	 * @param component	The component source.
	 */
	private static void showPopupMenu(final JComponent component){
		if(component != null)
			showPopupMenu(component, 0, component.getHeight());
	}

	/**
	 * Displays the JPopupMenu associated with the specified component.
	 *
	 * @param component	The component source.
	 * @param x	The X position relative to the component where the menu should be shown.
	 * @param y	The Y position relative to the component where the menu should be shown.
	 */
	private static void showPopupMenu(final JComponent component, final int x, final int y){
		if(component == null || !component.isEnabled())
			return;

		final JPopupMenu popup = component.getComponentPopupMenu();
		if(popup != null)
			popup.show(component, x, y);
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


	public static void showValidationErrorAndFocus(final Component parentComponent, final String message,
		final JTabbedPane tabbedPane, final JPanel tabbedPanel, final JComponent component){
		JOptionPane.showMessageDialog(parentComponent,
			message,
			"Validation Error", JOptionPane.ERROR_MESSAGE);

		if(tabbedPane != null && tabbedPanel != null){
			tabbedPane.requestFocusInWindow();
			tabbedPane.setSelectedComponent(tabbedPanel);
		}
		SwingUtilities.invokeLater(component::requestFocusInWindow);
	}


	public static JPanel createButtonPanel(final JRootPane rootPane, final Runnable save, final Runnable cancel){
		final JButton saveButton = new JButton("Save");
		final JButton cancelButton = new JButton("Cancel");

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);

		saveButton.addActionListener(e -> save.run());

		rootPane.setDefaultButton(saveButton);

		final Action escapeAction = new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 8267350842047854519L;

			@Override
			public void actionPerformed(final ActionEvent e){
				cancel.run();
			}
		};
		cancelButton.addActionListener(escapeAction);
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "escape");
		rootPane.getActionMap().put("escape", escapeAction);

		return buttonPanel;
	}


	/**
	 * Immutable record representing a menu entry.
	 */
	private record MenuEntry(String label, Runnable action, Supplier<Boolean> enabledCondition, boolean isSeparator){
		private static MenuEntry createEntry(final String label, final Runnable action,
			final Supplier<Boolean> enabledCondition){
			return new MenuEntry(label, action, enabledCondition, false);
		}

		private static MenuEntry createSeparator(){
			return new MenuEntry(null, null, null, true);
		}
	}


	/**
	 * Launches a standalone test window for a record dialog, replacing the identical
	 * {@code UIManager.setLookAndFeel(...) + SwingUtilities.invokeLater(...)} boilerplate that used to be duplicated
	 * in every dialog's {@code public static void main(String[])}. Usage:
	 * <pre>
	 * public static void main(final String[] args){
	 *     GUIHelper.launch(NoteRecordDialog::createNew);
	 * }
	 * </pre>
	 *
	 * @param dialogFactory	Reference to the dialog's own {@code createNew(Dialog, FLEFModel)} factory method.
	 */
	public static void launch(final CreateNewFunction dialogFactory){
		launch(dialogFactory, model -> {});
	}

	public static void launch(final EditFunction dialogFactory, final FLEFRecord record){
		final CreateNewFunction createNewFn = (dialog, model) -> dialogFactory.apply(dialog, model, record);
		launch(createNewFn, model -> {});
	}

	public static void launch(final EditFunction dialogFactory, final Consumer<FLEFModel> modelFiller,
			final FLEFRecord record){
		final CreateNewFunction createNewFn = (dialog, model) -> dialogFactory.apply(dialog, model, record);
		launch(createNewFn, modelFiller);
	}

	public static void launch(final CreateNewFunction dialogFactory, final Consumer<FLEFModel> modelFiller){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		modelFiller.accept(model);

		SwingUtilities.invokeLater(() -> {
			final JDialog dialog = dialogFactory.apply(null, model);
			dialog.setVisible(true);
		});
	}

}
