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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks;

import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.io.Serial;
import java.util.List;


/**
 * Modal dialog that lets the user rename or delete saved bookmarks.
 */
public final class BookmarkManagerDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -8102938475610293847L;


	private final BookmarkStore store;
	private final DefaultListModel<Bookmark> model = new DefaultListModel<>();
	private final JList<Bookmark> list = new JList<>(model);


	public BookmarkManagerDialog(final Window owner, final BookmarkStore store){
		super(owner, "Manage bookmarks", ModalityType.APPLICATION_MODAL);

		this.store = store;

		buildUI();
		reload();

		setSize(480, 360);
		setLocationRelativeTo(owner);
	}


	private void buildUI(){
		setLayout(new BorderLayout());

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer((l, value, index, isSelected, cellHasFocus) -> {
			final JPanel row = new JPanel(new MigLayout("ins 4,gapx 8,fillx", "[grow][right]", "[]0[]"));
			row.setOpaque(true);
			row.setBackground(isSelected? l.getSelectionBackground(): l.getBackground());

			final JLabel title = new JLabel(value.name());
			title.setFont(l.getFont().deriveFont(Font.BOLD));
			title.setForeground(isSelected? l.getSelectionForeground(): l.getForeground());

			final JLabel subtitle = new JLabel(value.type().displayName() + " — " + value.rootId());
			subtitle.setFont(l.getFont().deriveFont(Font.PLAIN, l.getFont().getSize() - 1f));
			subtitle.setForeground(isSelected? l.getSelectionForeground(): Color.GRAY);

			row.add(title, "growx,wrap");
			row.add(subtitle, "growx");
			return row;
		});

		final JScrollPane scroll = new JScrollPane(list);
		add(scroll, BorderLayout.CENTER);

		final JPanel buttons = new JPanel(new MigLayout("ins 6,gapx 6", "[]8[]8[grow,fill][]8[]", "[]"));
		final JButton rename = new JButton("Rename…");
		final JButton delete = new JButton("Delete");
		final JButton close = new JButton("Close");

		rename.addActionListener(e -> onRename());
		delete.addActionListener(e -> onDelete());
		close.addActionListener(e -> dispose());

		buttons.add(rename);
		buttons.add(delete);
		buttons.add(new JPanel(), "growx");
		buttons.add(close);

		add(buttons, BorderLayout.SOUTH);
	}


	private void reload(){
		model.clear();
		final List<Bookmark> all = store.all();
		for(final Bookmark b : all)
			model.addElement(b);
	}

	private void onRename(){
		final Bookmark selected = list.getSelectedValue();
		if(selected == null)
			return;

		final String newName = JOptionPane.showInputDialog(this, "New name:", selected.name());
		if(newName == null || newName.isBlank())
			return;

		store.rename(selected.id(), newName.trim());
		reload();
	}

	private void onDelete(){
		final Bookmark selected = list.getSelectedValue();
		if(selected == null)
			return;

		final int confirm = JOptionPane.showConfirmDialog(this,
			"Delete bookmark \"" + selected.name() + "\"?",
			"Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		store.remove(selected.id());
		reload();
	}

}
