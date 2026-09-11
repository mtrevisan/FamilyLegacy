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
package io.github.mtrevisan.familylegacy.v2.ui.bindings;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.DualActionListEnhancer;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.DualActionTextFieldEnhancer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class BindingsHelper{

	private static final Color COLOR_BACKGROUND = UIManager.getColor("TextField.background");
	public static final Color COLOR_FOREGROUND_ENABLED = UIManager.getColor("TextField.foreground");
	public static final Color COLOR_FOREGROUND_DISABLED = UIManager.getColor("Label.disabledForeground");

	private static final String PLACEHOLDER_LIST = "(no items)";
	private static final String PLACEHOLDER_TEXT = "(right-click to set)";
	private static final String TOOLTIP_TEXT = "Right-click for actions, double‑click to edit";
	private static final String TOOLTIP_DUAL_ACTION_TEXT = "Right-click for actions, double‑click to edit citation, shift+double-click to edit record";


	private BindingsHelper(){}


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
	public static void installBehavior(final JComponent component, final Runnable doubleClickAction,
			final Runnable shiftDoubleClickAction, final Runnable keyInsertAction, final Runnable keyDeleteAction,
			final Consumer<MenuBuilder> menuBuilder){
		final Supplier<Boolean> hasSelection = buildSelectionSupplier(component);
		installBehavior(component, hasSelection, doubleClickAction, shiftDoubleClickAction, keyInsertAction,
			keyDeleteAction, menuBuilder);
	}

	/**
	 * Installs behavior with full control over the popup menu structure and custom selection supplier.
	 *
	 * @param component	The component to enhance.
	 * @param hasSelection	A custom supplier for selection state.
	 * @param doubleClickAction	Action invoked on double‑click (it may be {@code null}).
	 * @param keyInsertAction	Action invoked by the INSERT key (it may be {@code null}).
	 * @param keyDeleteAction	Action invoked by the DELETE key (it may be {@code null}).
	 * @param menuBuilder	Consumer that defines the popup menu structure.
	 */
	public static void installBehavior(final JComponent component, final Supplier<Boolean> hasSelection,
			final Runnable doubleClickAction, final Runnable shiftDoubleClickAction, final Runnable keyInsertAction,
			final Runnable keyDeleteAction, final Consumer<MenuBuilder> menuBuilder){
		component.setBackground(COLOR_BACKGROUND);
		component.setToolTipText(shiftDoubleClickAction == null? TOOLTIP_TEXT: TOOLTIP_DUAL_ACTION_TEXT);
		if(component instanceof JTextComponent field)
			field.setEditable(false);
		else if(component instanceof JList<?> list)
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Collect the menu entries from the builder
		final MenuBuilder builder = new MenuBuilder(hasSelection);
		menuBuilder.accept(builder);
		final List<MenuBuilder.MenuEntry> entries = builder.getEntries();

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

	private static JPopupMenu buildPopup(final List<MenuBuilder.MenuEntry> entries){
		final JPopupMenu popup = new JPopupMenu();
		for(final MenuBuilder.MenuEntry entry : entries){
			if(entry.isSeparator())
				popup.addSeparator();
			else{
				final JMenuItem item = new JMenuItem(entry.label());
				item.addActionListener(ev -> entry.action().run());
				item.setEnabled(entry.enabledCondition().get());
				popup.add(item);
			}
		}
		return popup;
	}

	private static Supplier<Boolean> buildSelectionSupplier(final JComponent component){
		if(component instanceof JList<?> list)
			return () -> (list.getSelectedIndex() != -1);

		if(component instanceof JTextComponent)
			return () -> true;

		if(component instanceof JButton button)
			return () -> {
				final Icon icon = button.getIcon();
				return (icon != null);
			};

		// For other components, no meaningful selection; default to false
		return () -> false;
	}

	private static boolean isPlaceholder(final String text){
		return PLACEHOLDER_TEXT.equals(text);
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


	public static String getText(final String value){
		if(isPlaceholder(value))
			return null;

		return (value != null? value.trim(): null);
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

}