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
package io.github.mtrevisan.familylegacy.ui.dialogs.help;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Dynamic dialog that lists every registered keyboard shortcut from ShortcutRegistry.
 */
public final class KeyboardShortcutsDialog extends JDialog{

	private KeyboardShortcutsDialog(final Window owner){
		super(owner, I18N.t("dialog.keyboard.shortcuts.title"), ModalityType.APPLICATION_MODAL);

		GUIHelper.installEscapeToClose(this);

		//the same keys can be registered by several actions of the same menu (e.g. the jump-to item changes its label): list them once
		final Set<String> seen = new HashSet<>();
		final List<ShortcutRegistry.ShortcutDefinition> shortcuts = ShortcutRegistry.getAllShortcuts().stream()
			.filter(s -> seen.add(s.category() + '|' + s.displayKeys() + '|' + s.context()))
			.sorted(Comparator.comparing(ShortcutRegistry.ShortcutDefinition::category)
				.thenComparing(ShortcutRegistry.ShortcutDefinition::action))
			.toList();

		final String[] columns = {
			I18N.t("dialog.keyboard.shortcuts.table.category"),
			I18N.t("dialog.keyboard.shortcuts.table.action"),
			I18N.t("dialog.keyboard.shortcuts.table.shortcut"),
			I18N.t("dialog.keyboard.shortcuts.table.context")
		};
		final Object[][] rows = new Object[shortcuts.size()][];
		for(int i = 0; i < shortcuts.size(); i ++){
			final ShortcutRegistry.ShortcutDefinition s = shortcuts.get(i);
			rows[i] = new Object[]{s.category(), s.action(), s.displayKeys(),
				(s.context() != null? s.context(): I18N.t("shortcut.context.global"))};
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
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(140);
		columnModel.getColumn(1).setPreferredWidth(320);
		columnModel.getColumn(2).setPreferredWidth(140);
		columnModel.getColumn(3).setPreferredWidth(160);

		final DefaultTableCellRenderer leftAligned = new DefaultTableCellRenderer(){
			@Override
			public Component getTableCellRendererComponent(final JTable t, final Object value,
				final boolean isSelected, final boolean hasFocus, final int row, final int column){
				super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
				setHorizontalAlignment(LEFT);
				return this;
			}
		};
		columnModel.getColumn(2).setCellRenderer(leftAligned);
		columnModel.getColumn(3).setCellRenderer(leftAligned);

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(800, 520));


		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

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
