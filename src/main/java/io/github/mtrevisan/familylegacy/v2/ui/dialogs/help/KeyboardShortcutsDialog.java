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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Read-only dialog that lists every keyboard shortcut installed by the
 * application, grouped by category.
 * <p>
 * The list is hardcoded here rather than extracted from the Swing
 * {@code InputMap}s of the individual panels. Extracting it at runtime
 * would require walking the component hierarchy and reverse-mapping
 * keystrokes to human-readable action names, which is fragile and
 * locale-dependent. Keeping a curated list has the added benefit of
 * documenting the shortcuts in the language of the user interface.
 */
public final class KeyboardShortcutsDialog extends JDialog{

	private record Shortcut(String category, String action, String keys){
	}


	private static final List<Shortcut> SHORTCUTS = buildShortcutList();


	private KeyboardShortcutsDialog(final Window owner){
		super(owner, "Keyboard Shortcuts", ModalityType.APPLICATION_MODAL);

		final String[] columns = {"Category", "Action", "Shortcut"};
		final Object[][] rows = new Object[SHORTCUTS.size()][];
		for(int i = 0; i < SHORTCUTS.size(); i++){
			final Shortcut s = SHORTCUTS.get(i);
			rows[i] = new Object[]{s.category(), s.action(), s.keys()};
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

		// Keep the shortcuts left-aligned; the default cell renderer
		// centers them, which looks odd in a two-column layout.
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


	/**
	 * Opens the dialog.
	 *
	 * @param owner the parent window; may be {@code null}
	 */
	public static void show(final Window owner){
		final KeyboardShortcutsDialog dialog = new KeyboardShortcutsDialog(owner);
		dialog.setVisible(true);
	}


	/* ======================================================================
	 *                          Shortcut catalogue
	 * ====================================================================== */

	private static List<Shortcut> buildShortcutList(){
		final List<Shortcut> list = new ArrayList<>();

		// File
		list.add(new Shortcut("File", "New file", "Ctrl+N"));
		list.add(new Shortcut("File", "Open file", "Ctrl+O"));
		list.add(new Shortcut("File", "Save", "Ctrl+S"));
		list.add(new Shortcut("File", "Save as", "Ctrl+Shift+S"));
		list.add(new Shortcut("File", "Print", "Ctrl+P"));
		list.add(new Shortcut("File", "Exit", "Ctrl+Q"));

		// Edit
		list.add(new Shortcut("Edit", "Undo", "Ctrl+Z"));
		list.add(new Shortcut("Edit", "Redo", "Ctrl+Y"));
		list.add(new Shortcut("Edit", "Cut", "Ctrl+X"));
		list.add(new Shortcut("Edit", "Copy", "Ctrl+C"));
		list.add(new Shortcut("Edit", "Paste", "Ctrl+V"));
		list.add(new Shortcut("Edit", "Delete", "Del"));
		list.add(new Shortcut("Edit", "Select all", "Ctrl+A"));
		list.add(new Shortcut("Edit", "Find", "Ctrl+F"));
		list.add(new Shortcut("Edit", "Edit current selection", "F2"));

		// View
		list.add(new Shortcut("View", "Ancestor tree", "Ctrl+1"));
		list.add(new Shortcut("View", "Sugiyama graph", "Ctrl+2"));
		list.add(new Shortcut("View", "Ego network", "Ctrl+3"));
		list.add(new Shortcut("View", "Switch projection", "Ctrl+E"));
		list.add(new Shortcut("View", "Zoom in", "Ctrl++"));
		list.add(new Shortcut("View", "Zoom out", "Ctrl+-"));
		list.add(new Shortcut("View", "Zoom reset", "Ctrl+0"));
		list.add(new Shortcut("View", "Full screen", "F11"));

		// Navigate
		list.add(new Shortcut("Navigate", "Back", "Ctrl+←"));
		list.add(new Shortcut("Navigate", "Forward", "Ctrl+→"));
		list.add(new Shortcut("Navigate", "Jump to individual", "Ctrl+J"));

		// Spatial navigation
		list.add(new Shortcut("Selection", "Move up", "↑"));
		list.add(new Shortcut("Selection", "Move down", "↓"));
		list.add(new Shortcut("Selection", "Move left", "←"));
		list.add(new Shortcut("Selection", "Move right", "→"));
		list.add(new Shortcut("Selection", "Confirm selection (set as root)", "Enter"));

		// Ancestor tree
		list.add(new Shortcut("Ancestor Tree", "Toggle layout (vertical / horizontal)", "Ctrl+L"));
		list.add(new Shortcut("Ancestor Tree", "Kinship calculator", "Ctrl+K"));
		list.add(new Shortcut("Ancestor Tree", "Pedigree collapse report", "Ctrl+P"));
		list.add(new Shortcut("Ancestor Tree", "Toggle lifespan strip", "Ctrl+T"));

		// Sugiyama graph
		list.add(new Shortcut("Sugiyama Graph", "Toggle Sugiyama view", "Ctrl+G"));
		list.add(new Shortcut("Sugiyama Graph", "Zoom anchored at cursor", "Ctrl+wheel"));
		list.add(new Shortcut("Sugiyama Graph", "Scroll vertically", "wheel"));
		list.add(new Shortcut("Sugiyama Graph", "Scroll horizontally", "Shift+wheel"));
		list.add(new Shortcut("Sugiyama Graph", "Pan", "left drag"));

		// Help
		list.add(new Shortcut("Help", "Help contents", "F1"));

		list.sort(Comparator
			.comparing(Shortcut::category)
			.thenComparing(Shortcut::action));

		return List.copyOf(list);
	}

}
