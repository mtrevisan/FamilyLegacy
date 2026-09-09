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

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.v2.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.v2.io.grammar.FLEFGrammarValidator;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ImagePreviewAccessory;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

	public static final KeyStroke UNDO_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK);
	public static final KeyStroke MAC_UNDO_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	public static final KeyStroke REDO_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK);

	public static final KeyStroke CTRL_L_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK);

	private static final Color COLOR_BACKGROUND = UIManager.getColor("TextField.background");
	public static final Color COLOR_FOREGROUND_ENABLED = UIManager.getColor("TextField.foreground");
	public static final Color COLOR_FOREGROUND_DISABLED = UIManager.getColor("Label.disabledForeground");


	private static final String PLACEHOLDER_LIST = "(no items)";
	private static final String PLACEHOLDER_TEXT = "(right-click to set)";
	private static final String TOOLTIP_TEXT = "Right-click for actions, double‑click to edit";
	private static final String TOOLTIP_DUAL_ACTION_TEXT = "Right-click for actions, double‑click to edit citation, shift+double-click to edit record";

	private static final String ELLIPSIS = "…";

	public static final String PROPERTY_ASSOCIATED_LABEL = "__associatedLabel";


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
	 * @param doubleClickAction	Action invoked on double‑click (it may be {@code null}).
	 * @param keyInsertAction	Action invoked by the INSERT key (it may be {@code null}).
	 * @param keyDeleteAction	Action invoked by the DELETE key (it may be {@code null}).
	 * @param menuBuilder	Consumer that defines the popup menu structure.
	 */
	public static void installBehavior(final JComponent component,
			final Runnable doubleClickAction, final Runnable shiftDoubleClickAction,
			final Runnable keyInsertAction, final Runnable keyDeleteAction,
			final Consumer<MenuBuilder> menuBuilder){
		component.setBackground(COLOR_BACKGROUND);
		component.setToolTipText(shiftDoubleClickAction == null? TOOLTIP_TEXT: TOOLTIP_DUAL_ACTION_TEXT);
		if(component instanceof JTextComponent field)
			field.setEditable(false);
		else if(component instanceof JList<?> list)
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Collect the menu entries from the builder
		final Supplier<Boolean> hasSelection = buildSelectionSupplier(component);
		final MenuBuilder builder = new MenuBuilder(hasSelection);
		menuBuilder.accept(builder);
		final List<MenuEntry> entries = builder.getEntries();

		// Mouse listener for popup trigger and double‑click
		component.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && hasSelection.get()){
					if(shiftDoubleClickAction != null && e.isShiftDown())
						shiftDoubleClickAction.run();
					else if(doubleClickAction != null)
						doubleClickAction.run();
				}
				else if(e.isPopupTrigger())
					showPopup(e);
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

		if(shiftDoubleClickAction != null){
			if(component instanceof JList<?> list)
				// Global Shift listener to change cursor even without focus
				DualActionListEnhancer.install(list);
			else if(component instanceof JTextField field)
				// Global Shift listener to change cursor even without focus
				DualActionTextFieldEnhancer.install(field);
		}

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

		if(component instanceof JTextComponent)
			return () -> true;

		if(component instanceof JButton button)
			return () -> {
				final Icon icon = button.getIcon();
				return (icon != null && icon != PreferredImagePanel.PLACEHOLDER_ICON);
			};

		// For other components, no meaningful selection; default to false
		return () -> false;
	}

	public static String getText(final String value){
		if(isPlaceholder(value))
			return null;

		return (value != null? value.trim(): null);
	}

	private static boolean isPlaceholder(final String text){
		return PLACEHOLDER_TEXT.equals(text);
	}

	public static void setText(final String value, final JTextComponent component, final Consumer<String> setText){
		if(StringUtils.isNotEmpty(value)){
			setText.accept(value);
			component.setForeground(COLOR_FOREGROUND_ENABLED);
		}
		else{
			setText.accept(PLACEHOLDER_TEXT);
			component.setForeground(COLOR_FOREGROUND_DISABLED);
		}

		if(component.isShowing()){
			component.revalidate();
			component.repaint();
		}
	}


	public static String limitTextLength(final String text){
		return (text.length() > 50? text.substring(0, 49) + ELLIPSIS: text);
	}

	public static void updateDisplay(final JTextComponent component, final Supplier<Boolean> hasData,
			final Supplier<String> getText, final Consumer<String> setText){
		if(!component.isShowing())
			return;

		if(hasData.get()){
			setText.accept(getText.get());
			component.setForeground(COLOR_FOREGROUND_ENABLED);
		}
		else{
			setText.accept(PLACEHOLDER_TEXT);
			component.setForeground(COLOR_FOREGROUND_DISABLED);
		}

		component.revalidate();
		component.repaint();
	}

	public static void updateDisplay(final JTextComponent component, final Supplier<Boolean> hasData,
			final Supplier<String> getText){
		SwingUtilities.invokeLater(() -> {
			final Timer timer = new Timer(100, e -> {
				if(hasData.get()){
					component.setText(getText.get());
					component.setForeground(COLOR_FOREGROUND_ENABLED);
				}
				else{
					if(component instanceof BoundTextField btf)
						btf.forceSetText(PLACEHOLDER_TEXT);
					else
						component.setText(PLACEHOLDER_TEXT);
					component.setForeground(COLOR_FOREGROUND_DISABLED);
				}
			});
			timer.setRepeats(false);
			timer.start();
		});
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
		for(final MenuEntry entry : entries){
			if(entry.isSeparator)
				popup.addSeparator();
			else{
				final JMenuItem item = new JMenuItem(entry.label);
				item.addActionListener(ev -> entry.action.run());
				item.setEnabled(entry.enabledCondition.get());
				popup.add(item);
			}
		}
		return popup;
	}


	/**
	 * Creates a panel with a two‑column layout using MigLayout.
	 * Each row consists of a label and a field.
	 * The layout uses {@code wrap 1} to force each component onto its own row,
	 * and {@code split 2} to place label and field on the same row.
	 *
	 * @param rowConstraints	The row constraints (e.g. "[]10[]")
	 * @return	A new JPanel with the specified layout
	 */
	public static JPanel createLabelFieldPanel(final int insets, final String rowConstraints){
		return new JPanel(createLabelFieldLayout(insets, rowConstraints));
	}

	public static MigLayout createLabelFieldLayout(final int insets, final String rowConstraints){
		return new MigLayout("ins " + insets + ",hidemode 3,fillx,top,wrap 2",
			"[right]rel[grow,fill]",
			rowConstraints);
	}

	/**
	 * Adds a labeled field to a panel that uses the two‑column layout.
	 * The label is added with {@code split 2} and the field with {@code growx,pushx}.
	 *
	 * @param container	The panel (must have been created with {@link #createLabelFieldPanel})
	 * @param labelText	The text for the label
	 * @param field	The field component
	 */
	public static void addLabeledComponent(final Container container, final String labelText, final JComponent field){
		final JLabel label = new JLabel(labelText);
		label.setLabelFor(field);
		field.putClientProperty(PROPERTY_ASSOCIATED_LABEL, label);

		container.add(label, "align label");
		container.add((field instanceof JTextArea? createScrollPane(field): field), "growx");
	}

	/**
	 * Adds a labeled field to a panel that uses the two‑column layout.
	 * The label is added with {@code split 2} and the field with {@code growx,pushx}.
	 *
	 * @param container	The panel (must have been created with {@link #createLabelFieldPanel})
	 * @param field	The field component
	 */
	public static void addComponent(final Container container, final JComponent field){
		container.add((field instanceof JTextArea? createScrollPane(field): field), "span 2,growx");
	}

	public static void setComponentVisible(final JComponent field, final boolean visible){
		final JLabel label = (JLabel)field.getClientProperty(PROPERTY_ASSOCIATED_LABEL);
		if(label != null)
			label.setVisible(visible);
		field.setVisible(visible);

		final Container parent = field.getParent();
		if(parent != null && parent.isShowing()){
			parent.revalidate();
			parent.repaint();
		}
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


	public static JPanel createButtonPanel(final JDialog dialog, final Runnable save, final Runnable cancel){
		return createButtonPanel(dialog, "Save", save, "Cancel", cancel);
	}

	public static JPanel createButtonPanel(final JDialog dialog, final String saveLabel, final Runnable save,
			final String cancelLabel, final Runnable cancel){
		final JButton saveButton = new JButton(saveLabel);
		final JButton cancelButton = new JButton(cancelLabel);

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);

		saveButton.addActionListener(e -> save.run());

		final JRootPane rootPane = dialog.getRootPane();
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
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(ESCAPE_STROKE, "escape");
		rootPane.getActionMap()
			.put("escape", escapeAction);

		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(final WindowEvent e){
				cancel.run();
			}
		});

		return buttonPanel;
	}

	public static JPanel createNewSelectCancelButtonPanel(final JRootPane rootPane, final Runnable createNew,
			final Runnable select, final Runnable cancel){
		final JButton createNewButton = new JButton("Create New…");
		final JButton selectButton = new JButton("Select");
		final JButton cancelButton = new JButton("Cancel");

		final JPanel buttonPanel = new JPanel(new MigLayout("ins 0,fillx", "[left][grow,fill][right]", "[]"));
		buttonPanel.add(createNewButton, "cell 0 0,left");
		buttonPanel.add(selectButton, "cell 2 0,split 2,right");
		buttonPanel.add(cancelButton, "cell 2 0,right");

		createNewButton.addActionListener(e -> createNew.run());
		selectButton.addActionListener(e -> select.run());

		final Action escapeAction = new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -2257752682016633238L;

			@Override
			public void actionPerformed(final ActionEvent e){
				cancel.run();
			}
		};
		cancelButton.addActionListener(escapeAction);
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(ESCAPE_STROKE, "escape");
		rootPane.getActionMap()
			.put("escape", escapeAction);

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


	public static void drawX(final Graphics2D graphics2D, final Point enterPoint){
		graphics2D.setColor(Color.RED);
		graphics2D.drawLine(enterPoint.x - 10, enterPoint.y - 10,
			enterPoint.x + 10, enterPoint.y + 10);
		graphics2D.drawLine(enterPoint.x + 10, enterPoint.y - 10,
			enterPoint.x - 10, enterPoint.y + 10);
		graphics2D.setColor(Color.BLACK);
	}


	public static <T> void setupReorderingShortcuts(final JList<T> list, final DefaultListModel<T> listModel){
		final InputMap inputMap = list.getInputMap(JComponent.WHEN_FOCUSED);
		final ActionMap actionMap = list.getActionMap();

		// Keybindings: CTRL + UP / CTRL + DOWN
		inputMap.put(GUIHelper.CTRL_UP_STROKE, "moveUp");
		inputMap.put(GUIHelper.CTRL_DOWN_STROKE, "moveDown");

		actionMap.put("moveUp", new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 1557398991645410075L;

			@Override
			public void actionPerformed(final ActionEvent e){
				moveSelectedItemUp(list, listModel);
			}
		});

		actionMap.put("moveDown", new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -8473405355379714143L;

			@Override
			public void actionPerformed(final ActionEvent e){
				moveSelectedItemDown(list, listModel);
			}
		});
	}

	/**
	 * Moves the currently selected item up by one position.
	 */
	public static <T> void moveSelectedItemUp(final JList<T> list, final DefaultListModel<T> listModel){
		final int idx = list.getSelectedIndex();
		if(idx > 0)
			swapItems(list, listModel, idx, idx - 1);
	}

	/**
	 * Moves the currently selected item down by one position.
	 */
	public static <T> void moveSelectedItemDown(final JList<T> list, final DefaultListModel<T> listModel){
		final int idx = list.getSelectedIndex();
		if(idx >= 0 && idx < listModel.size() - 1)
			swapItems(list, listModel, idx, idx + 1);
	}

	private static <T> void swapItems(final JList<T> list, final DefaultListModel<T> listModel, final int index1,
			final int index2){
		// Swap in underlying items list
		final T tempRecord = listModel.get(index1);
		listModel.set(index1, listModel.get(index2));
		listModel.set(index2, tempRecord);

		// Swap in GUI model
		final T tempDisplay = listModel.get(index1);
		listModel.set(index1, listModel.get(index2));
		listModel.set(index2, tempDisplay);

		// Keep the moved item selected and visible
		list.setSelectedIndex(index2);
		list.ensureIndexIsVisible(index2);
	}

	public static <E, T> void setupDragAndDrop(final JList<E> list, final DefaultListModel<T> listModel){
		list.setDragEnabled(true);
		list.setDropMode(DropMode.INSERT);

		// Ensure selection on mouse press (if none selected)
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(list.getSelectedIndex() < 0 && SwingUtilities.isLeftMouseButton(e)){
					final int index = list.locationToIndex(e.getPoint());
					if(index >= 0)
						list.setSelectedIndex(index);
				}
			}
		});

		list.setTransferHandler(new TransferHandler(){
			@Serial
			private static final long serialVersionUID = 5054623014198954269L;

			private int sourceIndex = -1;


			@Override
			public int getSourceActions(final JComponent c){
				return MOVE;
			}

			@Override
			protected Transferable createTransferable(final JComponent c){
				sourceIndex = list.getSelectedIndex();
				if(sourceIndex < 0)
					return null;

				// Export the index as a string; only the originator will use it
				return new StringSelection(String.valueOf(sourceIndex));
			}

			@Override
			public boolean canImport(final TransferSupport support){
				// Accept drops from the same list only
				if(!(support.getComponent() instanceof JList))
					return false;

				// Only accept string data (our own format)
				return support.isDataFlavorSupported(DataFlavor.stringFlavor);
			}

			@Override
			public boolean importData(final TransferSupport support){
				if(!support.isDrop())
					return false;
				// Only accept drops from the same list
				if(!(support.getComponent() instanceof JList))
					return false;

				final JList.DropLocation dropLocation = (JList.DropLocation)support.getDropLocation();
				// index where the item would be inserted
				final int dropIndex = dropLocation.getIndex();
				if(dropIndex == -1)
					return false;

				// If sourceIndex is invalid, abort
				if(sourceIndex < 0 || sourceIndex >= listModel.size())
					return false;

				// Adjust drop index when moving downwards: after removing the source,
				// the target index shifts by one.
				final int targetIndex = (dropIndex > sourceIndex? dropIndex - 1: dropIndex);
				if(targetIndex == sourceIndex)
					return false;

				// Perform the move
				moveItem(listModel, sourceIndex, targetIndex);
				list.setSelectedIndex(targetIndex);
				list.ensureIndexIsVisible(targetIndex);
				return true;
			}

			@Override
			protected void exportDone(final JComponent source, final Transferable data, final int action){
				// Nothing else to clean up
			}
		});
	}

	/**
	 * Moves an item from one index to another within the list.
	 * Updates both the display model and the underlying items list.
	 */
	private static <T> void moveItem(final DefaultListModel<T> listModel, final int fromIndex,
			final int toIndex){
		if(fromIndex == toIndex)
			return;

		// Remove from old position
		final T item = listModel.remove(fromIndex);
		final T display = listModel.remove(fromIndex);

		// Insert at new position
		listModel.add(toIndex, item);
		listModel.add(toIndex, display);
	}


	public static File selectImageFile(final Component parentComponent, final String currentUriPath){
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select Image File");
		final String[] extensions = ImageIO.getReaderFileSuffixes();
		final String description = "Supported Images (" + String.join(", ", extensions) + ")";
		fileChooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
		fileChooser.setAccessory(new ImagePreviewAccessory(fileChooser));

		if(StringUtils.isNotEmpty(currentUriPath)){
			final String dirPath = FilenameUtils.getFullPath(currentUriPath);
			if(StringUtils.isNotEmpty(dirPath)){
				final File parentDir = new File(dirPath);
				if(parentDir.exists() && parentDir.isDirectory())
					fileChooser.setCurrentDirectory(parentDir);
			}
		}

		final int userSelection = fileChooser.showOpenDialog(parentComponent);
		if(userSelection != JFileChooser.APPROVE_OPTION)
			return null;

		final File selectedFile = fileChooser.getSelectedFile();
		if(selectedFile == null || !selectedFile.exists()){
			JOptionPane.showMessageDialog(parentComponent,
				"Selected file does not exist.",
				"Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}

		return selectedFile;
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
		final FLEFModel model = new FLEFModel();

		launch(dialogFactory, model);
	}

	public static void launch(final EditFunction dialogFactory, final FLEFRecord record){
		final FLEFModel model = new FLEFModel();

		final CreateNewFunction createNewFn = (dialog, model2) -> dialogFactory.apply(dialog, model, record);
		launch(createNewFn, model);
	}

	public static void launch(final EditFunction dialogFactory, final Consumer<FLEFModel> modelFiller,
			final FLEFRecord record){
		final FLEFModel model = new FLEFModel();
		modelFiller.accept(model);

		final CreateNewFunction createNewFn = (dialog, model2) -> dialogFactory.apply(dialog, model, record);
		launch(createNewFn, model);
	}

	public static void launch(final EditFunction dialogFactory, final String modelUri, final String recordId)
			throws IOException{
		final String content;
		try(final InputStream is = GUIHelper.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		validate(model);

		final FLEFRecord record = model.getRecordById(recordId);

		final CreateNewFunction createNewFn = (dialog, model2) -> dialogFactory.apply(dialog, model, record);
		launch(createNewFn, model);
	}

	private static void validate(FLEFModel model) throws IOException{
		final Path path = Paths.get("src/main/resources/gedg/flef_0.1.2.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);
		for(final String warning : grammar.getParseWarnings())
			System.err.println(warning);

		final FLEFGrammarValidator.ValidationResult validationResult = FLEFGrammarValidator.validate(grammar);
		for(final String error : validationResult.errors())
			System.err.println(error);
		for(final String warning : validationResult.warnings())
			System.err.println(warning);

		final FLEFValidator validator = new FLEFValidator(grammar);
		final List<String> errorsSchema = validator.validateSchema(model);
		for(final String error : errorsSchema)
			System.err.println(error);

		final List<String> errorsIntegrity = validator.validateIntegrity(model);
		for(final String error : errorsIntegrity)
			System.err.println(error);
	}

	public static void launch(final CreateNewFunction dialogFactory, final FLEFModel model){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		SwingUtilities.invokeLater(() -> {
			final BaseRecordDialog dialog = dialogFactory.apply(null, model);
			dialog.setVisible(true);

			System.out.println(FLEFWriter.createCompact().writeToString(model));
		});
	}

}
