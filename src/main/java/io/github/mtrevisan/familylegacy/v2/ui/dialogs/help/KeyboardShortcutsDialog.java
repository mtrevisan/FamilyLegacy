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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.help;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Comparator;
import java.util.List;


/**
 * Dynamic dialog that lists every registered keyboard shortcut from ShortcutRegistry.
 */
public final class KeyboardShortcutsDialog extends JDialog{

	private KeyboardShortcutsDialog(final Window owner){
		super(owner, "Keyboard Shortcuts", ModalityType.APPLICATION_MODAL);

		final List<ShortcutRegistry.ShortcutDefinition> shortcuts = ShortcutRegistry.getAllShortcuts().stream()
			.sorted(Comparator.comparing(ShortcutRegistry.ShortcutDefinition::category)
				.thenComparing(ShortcutRegistry.ShortcutDefinition::action))
			.toList();

		final String[] columns = {"Category", "Action", "Shortcut"};
		final Object[][] rows = new Object[shortcuts.size()][];
		for(int i = 0; i < shortcuts.size(); i ++){
			final ShortcutRegistry.ShortcutDefinition s = shortcuts.get(i);
			rows[i] = new Object[]{s.category(), s.action(), s.displayKeys()};
		}

		final DefaultTableModel model = new DefaultTableModel(rows, columns){
			@Override
			public boolean isCellEditable(final int row, final int column){
				return false;
			}
		};

		final JTable table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(140);
		table.getColumnModel().getColumn(1).setPreferredWidth(320);
		table.getColumnModel().getColumn(2).setPreferredWidth(180);

		final DefaultTableCellRenderer leftAligned = new DefaultTableCellRenderer(){
			@Override
			public Component getTableCellRendererComponent(final JTable t, final Object value,
				final boolean isSelected, final boolean hasFocus, final int row, final int column){
				super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
				setHorizontalAlignment(LEFT);
				return this;
			}
		};
		table.getColumnModel().getColumn(2).setCellRenderer(leftAligned);

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(700, 520));

		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(close);

		setLayout(new BorderLayout(8, 8));
		add(scroll, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}

	public static void show(final Window owner){
		final KeyboardShortcutsDialog dialog = new KeyboardShortcutsDialog(owner);
		dialog.setVisible(true);
	}

}
